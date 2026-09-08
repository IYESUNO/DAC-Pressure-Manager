# DAC Pressure Manager（原生 Android）

这是 [DAC-Pressure-Manager.html](https://apps.9527857.xyz/DAC-Pressure-Manager.html) 的离线优先原生 Android 重写。项目不包含
HTML、JavaScript、Capacitor 或 WebView；原始 HTML 位于相邻项目目录中，仅作为功能核对基准。
压力计算、档案、历史和界面日常使用均完全离线。只有用户在设置中主动点击“版本”时，
应用才会访问 GitHub Releases API 检查更新；冷启动和恢复前台不会联网。

## 开发环境

- Android Studio（直接打开本目录）
- JDK 17
- Gradle 8.14.3 / Android Gradle Plugin 8.13.2
- Kotlin 2.2.20
- `minSdk 24`，`compileSdk/targetSdk 36`

应用 ID 保持为 `com.iyes.dacpressuremanager`。界面使用 Jetpack Compose 和 Material 3；
结构化离线数据使用 Room 2.8.4。

界面自动跟随 Android 系统的明暗模式。应用内置 Roboto 可变字体，避免厂商主题或用户
替换系统字体后破坏仪表布局；字体许可见 `ROBOTO-OFL.txt`。

## 代码结构

```text
app/src/main/java/com/iyes/dacpressuremanager/
├── data/       Room、DAO、Repository 和持久化规则
├── domain/     模式、档案模型、压力公式及精确舍入
├── export/     CSV 生成、Storage Access Framework 与系统分享
├── update/     仅由用户主动触发的 GitHub Release 更新检查
└── ui/         ViewModel、单向状态、Compose 仪表盘、历史与设置弹窗
```

依赖由 `gradle/libs.versions.toml` 统一管理。`AppContainer` 负责手动注入数据库和
Repository，项目刻意不引入 Hilt、Retrofit、WorkManager、DataStore 或图片框架。

## 仓库与发布目标

本目录本身是独立 Git 仓库，`origin` 指向
`https://github.com/IYESUNO/DAC-Pressure-Manager.git`。Gradle 负责测试和构建；
正式发布使用既有签名材料生成并核验 Release APK，再通过 GitHub CLI 合并发布分支并
上传 GitHub Release。相邻的 `html-apps/`、`weight-record/` 和父工作区文件不属于
本仓库。

## 关键数据约定

- Reference、Measured 和历史压力都以百分之一为单位保存为 `Int`。
- Diamond 范围为 `1000.00–2999.99 cm⁻¹`，默认 `1333.00 cm⁻¹`。
- Ruby 范围为 `600.00–799.99 nm`，默认 `694.24 nm`。
- Diamond 使用 AK2006 公式，仅允许保存原始压力在 `0–310 GPa` 内的记录。
- Ruby 使用 `A=1904`、`B=7.665` 公式，并允许负压力。
- 舍入函数与 JavaScript `Math.round` 保持一致：精确半值向正无穷方向取整。
- Room 数据库版本为 1，schema 保存在 `app/schemas/`；以后修改表结构时必须新增迁移，
  不要直接覆盖既有 schema。

## 数据与导出

- 模式、活动档案、档案名称/顺序和数值变化都会自动保存。
- Diamond 与 Ruby 可分别选择 Default 加 31 个 Radix 配色；每种配色均按系统明暗主题
  使用对应的 12 阶角色色，并自动保存选择。
- UI 使用乐观状态即时响应，写入命令按操作顺序串行提交到 Room。
- 主仪表盘直接铺满可用屏幕，不使用外层卡片或阴影，也不做纵向滚动；Records 始终位于计数器右侧。
- 深色模式使用分层黑灰表面和高对比度浅色文字，保留 Diamond 蓝、Ruby 红及绿色数字屏。
- 档案长按排序使用 Compose 系统手势检测与列表位移动画：拖动时仅预览占位顺序，
  支持一次跨过多个档案，松手后才向 Room 提交一次最终顺序。
- 完整历史以覆盖主界面的弹窗显示，弹窗内部记录列表可滚动。
- 每种模式至少保留一个档案；每个档案最多保留 50 条历史。
- CSV 只导出当前档案，采用 UTF-8 BOM、CRLF、九列和旧到新顺序，并防护表格公式注入。
- “Save as”通过系统文件选择器保存，“Share”通过 `FileProvider` 打开系统分享面板。
- Manifest 只为用户主动点击“版本”后的 GitHub Release 检查声明网络权限，不声明宽泛
  存储权限；冷启动、计算、档案、历史和主题均不联网，并关闭系统备份和设备迁移。

## 构建与检查

在项目根目录运行：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

连接设备或启动模拟器后运行：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Debug APK 位于 `app/build/outputs/apk/debug/`。Release 构建启用 R8 和资源压缩；正式发布前
必须复用项目既有正式签名证书，不要生成替代证书，也不要把签名密钥提交到仓库。

## 后续修改原则

- UI 只读取 `MainUiState` / `HistoryUiState` 并派发 Action，业务规则不要放进 Composable。
- 数据修改统一经由 `DacRepository`，不要从 UI 直接访问 DAO。
- 新文案放入 `res/values/strings.xml`，以便以后本地化。
- 新增交互时保持至少 48dp 触控区域，并补充 TalkBack 语义和相应测试。
- 新依赖应先确认原生 API 无法合理实现，避免破坏轻量化目标。
