# GatherS · 截图智能管家

将散乱的截图转化为结构化信息资产的 Android 应用。基于 **Miuix**（Compose Multiplatform · MIUI 风格）组件库构建，全部识别与归纳均在本地完成，**不上传任何图片**。

## ✨ 功能

### 全息数据感知
- 自动扫描相册截图（MediaStore 监听，识别系统/各家 ROM 截图命名）
- 文件名语义解析：`Screenshot_2026-08-11-10-30-45.png` → 时间戳、设备代号、行为标注（编辑/裁剪/长截图…）
- 元数据多维提取：EXIF（ImageDescription / Software / DateTime）、分辨率、大小、模糊度检测

### 动态自适应分类
- 来源应用映射：目录/文件名 → 微信 / 淘宝 / 王者荣耀 / 系统截图…
- 多维度自动打标：内容标签（财务/凭证/攻略/聊天/游戏/错误）、状态标签（新截图/过期候选）、视觉标签（二维码/空白页）
- 智能摘要：本地启发式生成 10–20 字短摘要（如「微信财务截图 · 8月11日」）

### 高效清理中心
- 条件筛选：来源应用 × 内容标签 × 低价值/模糊，一键多选批量操作
- 内置回收站：清理前先复制副本，保留 30 天，支持一键还原、过期自动清理
- 高价值锁定：订单/支付/凭证类截图自动锁定，清理时跳过

### 可视化报告
- 周/月截图行为报告：总数、Top5 来源应用、14 天趋势、24 时段热力图
- 一键导出 Markdown 报告（分享到任意应用）

### 全自动智能管道（规则引擎）
- 内置 5 条智能规则：财务保护 / 临时凭证归类 / 错误清理 / 超期软清理 / 微信归类
- 规则可开关、即时生效

> 说明：视觉大模型 OCR / CLIP 等云端 AI 能力未接入（本地纯启发式实现），架构上预留了扩展点。

## 🛠 技术栈

| 组件 | 版本 |
| --- | --- |
| [Miuix](https://github.com/compose-miuix-ui/miuix) | 0.9.3 |
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.11.1 |
| Android Gradle Plugin | 9.3.1（AGP 9 内置 Kotlin） |
| Gradle | 9.6.1 |
| minSdk / targetSdk | 26 / 36 |

## 📦 构建

本项目**不在本地构建**，提交后由 GitHub Actions 自动编译：

1. 推送到 `main` 分支
2. 打开仓库 **Actions** 页签，等待 `Build APK` 工作流完成
3. 在最新的 run 页面底部 **Artifacts** 下载 `GatherS-debug-apk`

也可手动触发：Actions → Build APK → **Run workflow**。

本地构建（可选）：

```bash
./gradlew :app:assembleDebug
```

## 📁 项目结构

```
app/src/main/kotlin/com/gathers/app/
├── data/
│   ├── Screenshot.kt            # 截图数据模型
│   ├── ScreenshotRepository.kt  # MediaStore 扫描 + 索引 + 用户元数据
│   ├── FileNameParser.kt        # 文件名语义解析
│   ├── MetaExtractor.kt         # EXIF / 模糊度 / 空白检测
│   ├── Tagger.kt                # 分类打标 + 摘要
│   ├── TrashManager.kt          # 内置回收站（30 天）
│   ├── RuleEngine.kt            # 智能规则引擎
│   └── ReportGenerator.kt       # 周/月报告 + Markdown 导出
├── ui/
│   ├── App.kt                   # 权限 + 底部导航
│   ├── OverviewPage.kt          # 概览
│   ├── GalleryPage.kt           # 图库（筛选/多选/清理）
│   ├── DetailPage.kt            # 详情
│   ├── ReportPage.kt            # 报表
│   └── SettingsPage.kt          # 设置 + 回收站 + 规则
└── util/Formatters.kt           # 时间/大小格式化
```

## 📄 License

[Apache-2.0](LICENSE)
