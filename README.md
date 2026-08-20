# 🌾 余粮 (GrainLedger)

<div align="center">

![GrainLedger Logo](app/src/main/ic_launcher-playstore.png)

**基于 Android & Jetpack Compose 构建的现代化 MIUI 风格预算信封与智能日常记账系统**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.08-brightgreen.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-orange.svg?logo=android)](https://www.android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/HuangZhuoRui/GrainLedger?include_prereleases&color=blue)](https://github.com/HuangZhuoRui/GrainLedger/releases)

[功能特性](#-功能特性) • [架构设计](#-架构设计) • [界面预览](#-界面设计与体验) • [快速开始](#-快速开始与构建) • [更新与下载](#-下载与更新) • [提交规范](COMMIT_CONVENTION.md)

</div>

---

## 📖 简介

**余粮 (GrainLedger)** 是一款专为个人与家庭财务规划打造的高颜值、现代化的 Android 记账与预算信封管理工具。

应用严格遵循 **单一数据源 (SSOT)** 原则，采用轻量现代的 **MIUI / HyperOS 通透设计语言**，将传统的“预算信封法”与智能“双剩余 / 资金池配平”模型相结合。无论是在日常快速记账、月度开支细项规划，还是与 Excel 账本互通导入导出，余粮都能为您提供行云流水般的操作体验。

---

## ✨ 功能特性

### 📊 1. 智能资产看板 (Dashboard)
- **月度资产总览**：实时汇总展示当月总预算额度、实际加入（注入资金）、实际已支出与账户总剩余。
- **资金池配平健康状态卡片**：自动校验草稿资金池总额与各分类预算实际分配总额是否平衡，防范漏记与资金分配缺口。
- **分类信封聚合展示**：按大类（如强制类、固定开支、日常自由等）直观呈现信封配额与消耗进度条。
- **快捷算式记账**：支持在输入框内直接输入计算公式（如 `38+15.5+6`），一键快速入账。

### 🗓️ 2. 预算信封规划 (Budget)
- **细项层级管理**：支持自定义单价、数量、周期与实际分配额度（资金注入）。
- **多账户与出处追踪**：记录每一笔细项的资金来源账户与备注说明。
- **即时增删改查**：响应式动态计算每一笔细项的实时剩余额度。

### 📝 3. 流水树与明细追踪 (Transactions)
- **日历聚合流水树**：按日期分组归纳每日收支明细与每日小计。
- **双向联动与即时撤销**：单笔流水可追溯至对应预算细项，支持一键删除与数据即时回滚联动。

### 📁 4. Excel 双向互通与数据备份 (Settings)
- **智能 Excel 互通**：支持导入自定义 Excel 预算表或将全量历史账本导出为标准 `.xlsx` 文件。
- **全量本地备份与恢复**：纯本地离线隐私保护，无需注册，数据随身带走。

### 🚀 5. 高速更新检查体系 (Updater)
- **多通道版本检查**：集成 GitHub Releases 官方接口与自建加速节点。
- **高速多通道下载**：内置直连与高速代理双通道下载，支持断点感知、实时速率测算与应用内一键无缝安装升级。

---

## 🎨 界面设计与体验

- **MIUI / HyperOS 质感设计**：深度适配优雅的圆角大卡片（Squircle）、高通透背景与悬浮胶囊导航栏。
- **全屏沉浸式状态栏**：全局适配 Edge-to-Edge 全屏显示，暗色/浅色模式自适应状态栏图标高对比度对比，无缝沉浸。
- **跨页全量连续平移动画**：底层采用 `HorizontalPager` 重构，支持手势滑动与跨页签平滑滑过（`A -> B -> C -> D`）。
- **深色模式无缝切换**：完美支持 AMOLED 纯黑沉浸模式与自适应深浅色动态切换。

---

## 🏗️ 架构与技术栈

本项目基于现代化 Android 架构最佳实践进行设计与构建：

| 层次 | 技术选型 | 说明 |
| :--- | :--- | :--- |
| **编程语言** | Kotlin 2.x | 强类型、协程与响应式流 |
| **UI 框架** | Jetpack Compose (BOM 2026) | 声明式响应式 UI 构建 |
| **设计系统** | MIUIX KMP UI Library | 高品质 MIUI / HyperOS 视觉组件库 |
| **架构模式** | MVI / MVVM + SSOT | 单一数据源驱动，一处更新全量响应 |
| **异步处理** | Kotlin Coroutines & StateFlow | 线程安全与响应式数据流传递 |
| **表格引擎** | Apache POI | Excel `.xlsx` 格式高兼容导入导出 |
| **代码混淆** | ProGuard / R8 | 生产级代码混淆与无用资源压缩瘦身 |

---

## 📦 快速开始与构建

### 1. 环境要求
- **Android Studio**: Ladybug / Meerkat (2024.2+) 或更高版本
- **JDK**: OpenJDK 17 或 OpenJDK 21
- **Android SDK**: Compile SDK 37 (Android 15+), Min SDK 33

### 2. 克隆仓库
```bash
git clone https://github.com/HuangZhuoRui/GrainLedger.git
cd GrainLedger
```

### 3. 编译构建
```bash
# 构建 Debug 版本 APK
./gradlew assembleDebug

# 构建 Release 签名与混淆版本 APK
./gradlew assembleRelease

# 运行单元测试
./gradlew testDebugUnitTest
```
构建产物输出路径：`app/build/outputs/apk/release/app-release.apk`

---

## 📥 下载与更新

您可以通过以下渠道下载最新版本的 GrainLedger APK：

- 🚀 **自建专线加速下载**：[高速镜像下载通道](https://update.vincenthzr.org:8443/download/HuangZhuoRui/GrainLedger/releases/download/v1.1.0/GrainLedger-v1.1.0.apk)
- ⚡ **GitHub Releases 原生直链**：[GitHub Releases 页面](https://github.com/HuangZhuoRui/GrainLedger/releases)

---

## 📄 开源许可证

```text
Copyright 2026 Vincent (HuangZhuoRui)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
