# 📋 Git 提交规范与应用更新日志识别指南 (Commit Convention)

为了保障 **余粮 (GrainLedger)** 项目的版本演进清晰可溯，并实现 **GitHub Releases 更新日志在应用内的全自动结构化智能解析与分类展示**，本项目制定了以下 Git Commit 提交规范。

---

## 📌 一、核心基本原则

1. **中文强制要求**：所有 Git Commit 的描述内容（`<subject>`）**必须 100% 使用中文**，严禁使用含混不清的纯英文或单一单词提交。
2. **规范格式约束**：所有提交信息必须严格遵循 [Conventional Commits](https://www.conventionalcommits.org/) 格式：
   ```text
   <type>(<scope>): <subject>
   # 或（无特定模块时）
   <type>: <subject>
   ```

---

## 🏷️ 二、Commit 类型 (Type) 详解

应用内置的更新检测服务将自动根据 `type` 对更新条目进行归类展示：

| Type 类型 | 说明 | 应用内对应展示分类 | 示例 |
| :--- | :--- | :--- | :--- |
| **`feat`** / `feature` | 新增功能、新交互或新能力 | **【新增特性】** | `feat(budget): 增加预算细项一键复制到下月功能` |
| **`fix`** / `bugfix` / `hotfix` | 修复缺陷或解决异常 Bug | **【问题修复】** | `fix(ui): 修复记账弹窗与编辑弹窗点击无反应问题` |
| **`perf`** | 性能优化、渲染提速或内存瘦身 | **【优化改进】** | `perf(core): 优化大数据量下月度流水树聚合计算性能` |
| **`refactor`** | 代码重构（不改变功能与外部表现） | **【优化改进】** | `refactor(updater): 重构结构化更新日志解析引擎` |
| **`style`** | 界面布局、动效、图标、主题等外观调整 | **【优化改进】** | `style(icon): 全量重构生成100%纯白不透明图标` |
| **`docs`** | 文档新增或修改（如 README、规则说明） | **【其他变更】** | `docs: 新增项目Git提交规范指南COMMIT_CONVENTION.md` |
| **`chore`** | 构建配置、依赖库升级、打包脚本等日常杂务 | **【其他变更】** | `chore(deps): 升级Compose BOM至最新稳定版本` |
| **`test`** | 增加或修复单元测试、自动化测试用例 | **【其他变更】** | `test(viewmodel): 增加单一数据源资金配平校验用例` |
| **`revert`** | 代码回滚或撤销历史提交 | **【其他变更】** | `revert: 回退因兼容性引入的OverlayDialog提交` |

---

## 🎯 三、推荐的作用域 (Scope)

作用域用以标明变更影响的核心子系统或模块，在应用内会被自动提取并格式化为 `[SCOPE]` 标签前缀（例如 `[UI]`, `[BUDGET]`）：

- `ui`: 通用组件、脚手架、页面版型与导航胶囊
- `dashboard`: 资产看板、资金池配平健康卡片
- `budget`: 预算规划、信封卡片与细项编辑
- `transactions`: 流水明细、日历聚合流水树
- `settings`: 设置中心、主题模式与本地数据备份
- `excel`: Apache POI Excel 导入与导出互通引擎
- `updater`: GitHub Releases 检查、双通道加速下载与安装器
- `icon`: 应用桌面自适应图标与矢量资源
- `theme`: AMOLED 深色模式、配色方案与圆角形状
- `data`: 数据库模型、DAO、Repository 与单一数据源

---

## 🤖 四、应用内更新日志智能识别机制

当发布新版本（Release）时，应用内部的 `AppUpdaterService` 和 `ParsedChangelog` 会自动对 Release Body 中的提交记录执行以下正则解析与分类渲染：

```kotlin
// 正则匹配规则
^(?:[-*]\s*)?(feat|feature|fix|bugfix|hotfix|perf|refactor|style|docs|chore|test|revert)(?:\(([^)]+)\))?[:：\s]\s*(.+)$
```

### 📱 界面渲染规则：
1. **冒号兼容**：自动兼容英文冒号 `:`、中文全角冒号 `：` 以及空格分隔。
2. **列表标记兼容**：兼容 Markdown 列表符号（如 `- feat:` 或 `* fix:`）。
3. **作用域标签自动提亮**：
   - 提交内容：`feat(ui): 增加通用页面容器AppPageScaffold`
   - 应用内展示：`• [UI] 增加通用页面容器AppPageScaffold`
4. **多分类模块化排版**：
   - 在【检查更新弹窗】与【版本发布历史卡片】中，条目会被自动分拣至 **【新增特性】**（蓝色）、**【问题修复】**（绿色）、**【优化改进】**（紫色）与 **【其他变更】**（灰色）模块中展示。

---

## 💡 五、正反示例对比

### ✅ 推荐示例 (Good)
```text
feat(budget): 新增预算细项算式公式直接计算支持
fix(icon): 修复自适应图标退出动画露出原生绿色底色
style(theme): 优化AMOLED纯黑模式下的高对比度卡片边框
perf(transactions): 优化流水按日归纳时的去重与排序性能
refactor(ui): 拆分UI组件至独立模块并规范分类
docs: 更新README文档中的构建与加速下载指南
```

### ❌ 错误示例 (Bad)
```text
update                    (无具体类型，无描述)
fix bug                   (未说明具体修复了什么问题，使用纯英文)
feat: add new feature     (未遵守中文提交规则)
修改了一些代码              (缺失类型前缀，无法被更新日志引擎识别)
```

---

## 🚀 六、快速参考模板

在执行 Git 提交时，可直接套用以下命令模板：

```bash
git add -A
git commit -m "<type>(<scope>): <中文简短说明>"
git push origin main
```
