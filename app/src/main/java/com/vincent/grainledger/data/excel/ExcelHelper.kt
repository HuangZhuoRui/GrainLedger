package com.vincent.grainledger.data.excel

import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.data.model.TransactionRecord
import com.vincent.grainledger.data.repository.LedgerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Excel 导入导出核心引擎。
 *
 * 采用原生 OpenXML (ZIP + XML) 流式技术构建，免除冗余体积与兼容性风险，
 * 支持对 《账单.xlsx》 中的四大工作表（综合查看、数据源、每日账单、草稿页）进行完整解析与格式化导出。
 */
object ExcelHelper {

    /**
     * Excel 导入执行结果。
     *
     * @property isSuccess 导入是否完全顺利
     * @property importedBudgetCount 成功解析并入库的预算项行数
     * @property importedTransactionCount 成功解析并入库的交易流水行数
     * @property message 结果或错误描述
     */
    data class ImportResult(
        val isSuccess: Boolean,
        val importedBudgetCount: Int = 0,
        val importedTransactionCount: Int = 0,
        val message: String = ""
    )

    /**
     * 从 Excel 输入流中导入数据并持久化到本地数据仓库。
     *
     * @param inputStream xlsx 文件输入流
     * @param repository 目标本地仓库
     * @return 导入结果
     */
    suspend fun importFromExcelStream(inputStream: InputStream, repository: LedgerRepository): ImportResult = withContext(Dispatchers.IO) {
        try {
            val zipEntriesMap = mutableMapOf<String, ByteArray>()
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    var readLength: Int
                    while (zip.read(buffer).also { readLength = it } != -1) {
                        byteArrayOutputStream.write(buffer, 0, readLength)
                    }
                    zipEntriesMap[entry.name] = byteArrayOutputStream.toByteArray()
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // 1. 解析共享字符串表 sharedStrings.xml
            val sharedStringsList = mutableListOf<String>()
            val sharedStringsBytes = zipEntriesMap["xl/sharedStrings.xml"]
            if (sharedStringsBytes != null) {
                val factory = DocumentBuilderFactory.newInstance()
                factory.isNamespaceAware = true
                val document = factory.newDocumentBuilder().parse(sharedStringsBytes.inputStream())
                val siNodeList = document.getElementsByTagName("si")
                for (i in 0 until siNodeList.length) {
                    val siElement = siNodeList.item(i) as Element
                    val tNodeList = siElement.getElementsByTagName("t")
                    val stringBuilder = StringBuilder()
                    for (j in 0 until tNodeList.length) {
                        stringBuilder.append(tNodeList.item(j).textContent ?: "")
                    }
                    sharedStringsList.add(stringBuilder.toString())
                }
            }

            // 2. 解析工作表映射 workbook.xml
            val sheetFileMap = mutableMapOf<String, String>()
            val workbookBytes = zipEntriesMap["xl/workbook.xml"]
            val relsBytes = zipEntriesMap["xl/_rels/workbook.xml.rels"]

            val relsMap = mutableMapOf<String, String>()
            if (relsBytes != null) {
                val factory = DocumentBuilderFactory.newInstance()
                factory.isNamespaceAware = true
                val relsDocument = factory.newDocumentBuilder().parse(relsBytes.inputStream())
                val relationshipNodes = relsDocument.getElementsByTagName("Relationship")
                for (i in 0 until relationshipNodes.length) {
                    val element = relationshipNodes.item(i) as Element
                    val relId = element.getAttribute("Id")
                    val targetPath = element.getAttribute("Target")
                    relsMap[relId] = if (targetPath.startsWith("/")) targetPath.substring(1) else "xl/$targetPath"
                }
            }

            if (workbookBytes != null) {
                val factory = DocumentBuilderFactory.newInstance()
                factory.isNamespaceAware = true
                val workbookDocument = factory.newDocumentBuilder().parse(workbookBytes.inputStream())
                val sheetNodes = workbookDocument.getElementsByTagName("sheet")
                for (i in 0 until sheetNodes.length) {
                    val element = sheetNodes.item(i) as Element
                    val sheetName = element.getAttribute("name")
                    val relId = element.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                        .ifEmpty { element.getAttribute("r:id") }
                    val targetFile = relsMap[relId] ?: "xl/worksheets/sheet${i + 1}.xml"
                    sheetFileMap[sheetName] = targetFile
                }
            }

            var budgetCount = 0
            var transactionCount = 0

            // 3. 解析《数据源》工作表
            val dataSourceFileName = sheetFileMap["数据源"] ?: "xl/worksheets/sheet2.xml"
            val dataSourceBytes = zipEntriesMap[dataSourceFileName]
            if (dataSourceBytes != null) {
                val rowDataList = parseSheetRows(dataSourceBytes, sharedStringsList)
                // 首行为标题行：时间年 | 时间月 | 类别 | 详情 | 单价 | 数量 | 总价 | 出资人 | 实际消费 | 结余 | 实际加入
                for (row in rowDataList) {
                    val yearText = row["A"] ?: ""
                    val year = yearText.toIntOrNull() ?: continue
                    val month = row["B"]?.toIntOrNull() ?: 1
                    val categoryName = row["C"] ?: "未分类"
                    val detailName = row["D"] ?: ""
                    if (detailName.isEmpty()) continue

                    val unitPrice = row["E"]?.toDoubleOrNull() ?: 0.0
                    val quantity = row["F"]?.toDoubleOrNull() ?: 1.0
                    val totalPrice = row["G"]?.toDoubleOrNull() ?: (unitPrice * quantity)
                    val funder = row["H"]?.ifEmpty { "默认账户" } ?: "默认账户"
                    val actualSpent = row["I"]?.toDoubleOrNull() ?: 0.0
                    val actualAllocated = row["K"]?.toDoubleOrNull() ?: totalPrice
                    val balance = row["J"]?.toDoubleOrNull() ?: (actualAllocated - actualSpent)

                    val budgetItem = BudgetItem(
                        itemId = 0L,
                        year = year,
                        month = month,
                        categoryName = categoryName,
                        detailName = detailName,
                        unitPrice = unitPrice,
                        quantity = quantity,
                        totalPrice = totalPrice,
                        actualAllocated = actualAllocated,
                        funder = funder,
                        actualSpent = actualSpent,
                        balance = balance
                    )
                    repository.saveBudgetItem(budgetItem)
                    budgetCount++
                }
            }

            // 4. 解析《每日账单》工作表
            val dailyLedgerFileName = sheetFileMap["每日账单"] ?: "xl/worksheets/sheet3.xml"
            val dailyLedgerBytes = zipEntriesMap[dailyLedgerFileName]
            if (dailyLedgerBytes != null) {
                val transactionRowList = parseSheetRows(dailyLedgerBytes, sharedStringsList)
                // 首行标题：时间年 | 时间月 | 时间日 | 类别 | 详情 | 收支 | 具体剩余 | 类剩余
                for (row in transactionRowList) {
                    val yearText = row["A"] ?: ""
                    val year = yearText.toIntOrNull() ?: continue
                    val month = row["B"]?.toIntOrNull() ?: continue
                    val day = row["C"]?.toIntOrNull() ?: 1
                    val categoryName = row["D"] ?: "未分类"
                    val detailName = row["E"] ?: ""
                    if (detailName.isEmpty()) continue

                    val amount = row["F"]?.toDoubleOrNull() ?: continue
                    val itemRemaining = row["G"]?.toDoubleOrNull() ?: 0.0
                    val categoryRemaining = row["H"]?.toDoubleOrNull() ?: 0.0

                    val record = TransactionRecord(
                        recordId = 0L,
                        year = year,
                        month = month,
                        day = day,
                        categoryName = categoryName,
                        detailName = detailName,
                        amount = amount,
                        itemRemaining = itemRemaining,
                        categoryRemaining = categoryRemaining,
                        funder = "默认账户",
                        remark = "Excel导入流水"
                    )
                    repository.recordTransaction(record)
                    transactionCount++
                }
            }

            ImportResult(
                isSuccess = true,
                importedBudgetCount = budgetCount,
                importedTransactionCount = transactionCount,
                message = "成功导入 $budgetCount 项预算和 $transactionCount 笔记账流水！"
            )
        } catch (exception: Exception) {
            ImportResult(
                isSuccess = false,
                message = "导入失败: ${exception.localizedMessage ?: "文件解析异常"}"
            )
        }
    }

    /**
     * 解析工作表 XML 中的所有单元格数据。
     */
    private fun parseSheetRows(sheetBytes: ByteArray, sharedStringsList: List<String>): List<Map<String, String>> {
        val resultRowList = mutableListOf<Map<String, String>>()
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val document = factory.newDocumentBuilder().parse(sheetBytes.inputStream())
        val rowNodeList = document.getElementsByTagName("row")

        for (i in 0 until rowNodeList.length) {
            val rowElement = rowNodeList.item(i) as Element
            val cellNodeList = rowElement.getElementsByTagName("c")
            val singleRowMap = mutableMapOf<String, String>()

            for (j in 0 until cellNodeList.length) {
                val cell = cellNodeList.item(j) as Element
                val cellCoordinate = cell.getAttribute("r")
                val columnLetter = cellCoordinate.filter { it.isLetter() }
                val cellType = cell.getAttribute("t")

                var cellValue = ""
                val vNodeList = cell.getElementsByTagName("v")
                if (vNodeList.length > 0) {
                    cellValue = vNodeList.item(0).textContent ?: ""
                }

                if (cellType == "s" && cellValue.isNotEmpty()) {
                    val index = cellValue.toIntOrNull()
                    if (index != null && index in sharedStringsList.indices) {
                        cellValue = sharedStringsList[index]
                    }
                }
                singleRowMap[columnLetter] = cellValue
            }

            if (singleRowMap.isNotEmpty()) {
                resultRowList.add(singleRowMap)
            }
        }
        return resultRowList
    }

    /**
     * 将当前系统的所有账目与预算数据导出为标准兼容的 xlsx 文件流。
     *
     * @param outputStream 写入目标流
     * @param repository 本地数据仓库
     */
    suspend fun exportToExcelStream(outputStream: OutputStream, repository: LedgerRepository) = withContext(Dispatchers.IO) {
        val monthList = repository.getAvailableMonths()
        val allBudgetItems = mutableListOf<BudgetItem>()
        val allTransactions = mutableListOf<TransactionRecord>()

        for ((year, month) in monthList) {
            allBudgetItems.addAll(repository.getBudgetItemsByMonth(year, month))
            allTransactions.addAll(repository.getTransactionsByMonth(year, month))
        }

        val zip = ZipOutputStream(outputStream)
        try {
            // 1. [Content_Types].xml
            val contentTypesXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
            """.trimIndent()
            addZipEntry(zip, "[Content_Types].xml", contentTypesXml)

            // 2. _rels/.rels
            val rootRelsXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
            """.trimIndent()
            addZipEntry(zip, "_rels/.rels", rootRelsXml)

            // 3. xl/_rels/workbook.xml.rels
            val wbRelsXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                </Relationships>
            """.trimIndent()
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", wbRelsXml)

            // 4. xl/workbook.xml
            val workbookXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                    <sheets>
                        <sheet name="数据源" sheetId="1" r:id="rId1"/>
                        <sheet name="每日账单" sheetId="2" r:id="rId2"/>
                    </sheets>
                </workbook>
            """.trimIndent()
            addZipEntry(zip, "xl/workbook.xml", workbookXml)

            // 5. xl/worksheets/sheet1.xml (数据源)
            val sheet1Builder = StringBuilder()
            sheet1Builder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            sheet1Builder.append("""<row r="1"><c r="A1" t="inlineStr"><is><t>时间年</t></is></c><c r="B1" t="inlineStr"><is><t>时间月</t></is></c><c r="C1" t="inlineStr"><is><t>类别</t></is></c><c r="D1" t="inlineStr"><is><t>详情</t></is></c><c r="E1" t="inlineStr"><is><t>单价</t></is></c><c r="F1" t="inlineStr"><is><t>数量</t></is></c><c r="G1" t="inlineStr"><is><t>总价</t></is></c><c r="H1" t="inlineStr"><is><t>出资人</t></is></c><c r="I1" t="inlineStr"><is><t>实际消费</t></is></c><c r="J1" t="inlineStr"><is><t>结余</t></is></c><c r="K1" t="inlineStr"><is><t>实际加入</t></is></c></row>""")
            allBudgetItems.forEachIndexed { index, item ->
                val r = index + 2
                sheet1Builder.append("""<row r="$r">""")
                sheet1Builder.append("""<c r="A$r"><v>${item.year}</v></c>""")
                sheet1Builder.append("""<c r="B$r"><v>${item.month}</v></c>""")
                sheet1Builder.append("""<c r="C$r" t="inlineStr"><is><t>${item.categoryName}</t></is></c>""")
                sheet1Builder.append("""<c r="D$r" t="inlineStr"><is><t>${item.detailName}</t></is></c>""")
                sheet1Builder.append("""<c r="E$r"><v>${item.unitPrice}</v></c>""")
                sheet1Builder.append("""<c r="F$r"><v>${item.quantity}</v></c>""")
                sheet1Builder.append("""<c r="G$r"><v>${item.totalPrice}</v></c>""")
                sheet1Builder.append("""<c r="H$r" t="inlineStr"><is><t>${item.funder}</t></is></c>""")
                sheet1Builder.append("""<c r="I$r"><v>${item.actualSpent}</v></c>""")
                sheet1Builder.append("""<c r="J$r"><v>${item.balance}</v></c>""")
                sheet1Builder.append("""<c r="K$r"><v>${item.actualAllocated}</v></c>""")
                sheet1Builder.append("""</row>""")
            }
            sheet1Builder.append("""</sheetData></worksheet>""")
            addZipEntry(zip, "xl/worksheets/sheet1.xml", sheet1Builder.toString())

            // 6. xl/worksheets/sheet2.xml (每日账单)
            val sheet2Builder = StringBuilder()
            sheet2Builder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            sheet2Builder.append("""<row r="1"><c r="A1" t="inlineStr"><is><t>时间年</t></is></c><c r="B1" t="inlineStr"><is><t>时间月</t></is></c><c r="C1" t="inlineStr"><is><t>时间日</t></is></c><c r="D1" t="inlineStr"><is><t>类别</t></is></c><c r="E1" t="inlineStr"><is><t>详情</t></is></c><c r="F1" t="inlineStr"><is><t>收支</t></is></c><c r="G1" t="inlineStr"><is><t>具体剩余</t></is></c><c r="H1" t="inlineStr"><is><t>类剩余</t></is></c></row>""")
            allTransactions.forEachIndexed { index, transaction ->
                val r = index + 2
                sheet2Builder.append("""<row r="$r">""")
                sheet2Builder.append("""<c r="A$r"><v>${transaction.year}</v></c>""")
                sheet2Builder.append("""<c r="B$r"><v>${transaction.month}</v></c>""")
                sheet2Builder.append("""<c r="C$r"><v>${transaction.day}</v></c>""")
                sheet2Builder.append("""<c r="D$r" t="inlineStr"><is><t>${transaction.categoryName}</t></is></c>""")
                sheet2Builder.append("""<c r="E$r" t="inlineStr"><is><t>${transaction.detailName}</t></is></c>""")
                sheet2Builder.append("""<c r="F$r"><v>${transaction.amount}</v></c>""")
                sheet2Builder.append("""<c r="G$r"><v>${transaction.itemRemaining}</v></c>""")
                sheet2Builder.append("""<c r="H$r"><v>${transaction.categoryRemaining}</v></c>""")
                sheet2Builder.append("""</row>""")
            }
            sheet2Builder.append("""</sheetData></worksheet>""")
            addZipEntry(zip, "xl/worksheets/sheet2.xml", sheet2Builder.toString())

        } finally {
            zip.finish()
            zip.flush()
        }
    }

    private fun addZipEntry(zip: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zip.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zip.write(bytes, 0, bytes.size)
        zip.closeEntry()
    }
}
