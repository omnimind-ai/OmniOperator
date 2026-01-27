<p align="center">
  <img src="logo.png" alt="OmniOperator Logo" width="100%" />
</p>

<h1 align="center">OmniOperator 🛠️</h1>

<p align="center">
  <a href="https://flutter.dev">
    <img src="https://img.shields.io/badge/Flutter-3.x-02569B?logo=flutter" alt="Flutter 3.x">
  </a>
  <a href="https://www.android.com">
    <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android" alt="Platform Android">
  </a>
  <a href="https://m3.material.io/">
    <img src="https://img.shields.io/badge/Style-Material_You-7B1FA2?logo=materialdesign" alt="Style Material You">
  </a>
</p>

<p align="center">
  <a href="README.md">English Docs</a> •
  <a href="#功能">功能</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#api-参考">API 参考</a>
</p>

OmniOperator 是一个 Android 应用，支持通过 HTTP 请求远程控制和交互 Android 设备。它会在设备上启动本地代理与开发服务器。

项目包含一个基于 Web 的 “DevServer Playground”，同一网络内的浏览器即可访问，便于设备操作、命令执行与检查。

![Omni Operator Demo](./demo.gif)

## 功能

**核心 Android 应用：**
*   **设备内 HTTP 代理：** 负责代理与转发相关请求。
*   **设备内 Dev Server：** 提供 Playground 与设备操作 API。
*   **远程设备操作：** 通过 HTTP 请求进行点击坐标、打开应用等操作。

**DevServer Playground（Web UI）：**
*   **交互式截图视图：**
    *   获取并显示当前设备屏幕（`/captureScreenshotImage`）。
    *   点击截图获取准确像素坐标。
    *   坐标可直接用于终端命令。
*   **UI XML 检查器：**
    *   获取并显示当前 UI 层级的 XML（`/captureScreenshotXml`）。
    *   使用 Monaco Editor 提供语法高亮与折叠。
*   **集成终端（xterm.js）：**
    *   内置命令：`screenshot`、`xmlshot`、`help`、`clear`、`apidoc`。
    *   执行动态指令控制设备（如 `tapCoordinate <x> <y>`、`launchApplication <packageName>`）。
    *   支持 Tab 补全、历史命令、Ctrl+U 清行。
*   **API 文档入口：** 使用 `apidoc` 打开 `/redoc`。

## 快速开始

1. 安装 APK 或从源码构建（见下）。
2. 在设备上打开 **OmniOperator** 并 **Start DevServer**。
3. 记录应用显示的 IP/端口（例如 `http://192.168.1.5:8080`）。
4. 在同一网络中的浏览器打开该地址访问 Playground。
5. 使用 `help`、`screenshot`、`xmlshot`、`apidoc` 进行探索。

## 先决条件

- Android Studio + Android SDK（含 platform tools / adb）。
- JDK 17（建议用于现代 Android 构建）。
- Flutter SDK（用于 Web Playground 模块）。
- 实体 Android 设备或可联网的模拟器。

## 安装与配置（Android 应用）

1. 获取 APK。
    *   下载 `app-debug.apk`。
    *   或从源码构建生成 APK。
2. 安装到 Android 设备。
3. 在设备上打开 **OmniOperator**。
4. 在应用内选择 **Start DevServer**。
    *   为完整功能可能需要启用无障碍服务权限。
5. 应用将显示 DevServer 的 IP 和端口。
6. 在同一网络中的浏览器打开该地址。

## 从源码构建与运行

1. 初始化 Flutter 模块：
    ```bash
    cd flutter_module
    flutter pub get
    ```
2. 构建 Android 应用：
    ```bash
    ./gradlew assembleDebug
    ```
3. 安装到设备：
    ```bash
    ./gradlew :app:installDebug
    ```
4. 在设备上启动 **OmniOperator** 并开启 DevServer。

## API 参考

完整 API 列表和说明可在 Playground 终端使用 `apidoc`，或直接访问 `http://<device-ip>:<port>/redoc`。

## 兼容性与权限

- 平台：Android（设备或模拟器）。
- 网络：设备与浏览器需在同一局域网。
- 权限：部分控制能力需开启无障碍服务。

## 安全提示

> [!WARNING]
> *   DevServer 仅用于受信任的本地网络。
> *   避免将设备 IP/端口暴露到公网。
> *   不使用时请关闭 DevServer。

## 项目结构

```text
.
├── app/                 # Android 应用源码
├── flutter_module/      # Web Playground
└── docs/                # 补充文档
```

## 架构设计

OmniOperator 是 **OpenOmniCloud** 使用的设备侧执行层，提供设备操作的 HTTP 接口，并承载轻量的 DevServer Playground 用于调试与人工控制。

**设计原则**  
当前实现仅专注 OmniOperator，并遵循两条核心原则：

1. **被动运行**  
   仅在 API 请求触发时执行动作，不包含自主决策或目标驱动逻辑。

2. **无状态**  
   服务不保留持久状态；上下文需由调用方提供与管理（例如 OpenOmniCloud 或人工操作者）。

这些设计带来的好处：
- **职责清晰：** 状态由控制方（OpenOmniCloud 或人工操作者）管理
- **可预测性：** 行为仅由输入驱动
- **易扩展：** 多个控制器可复用同一设备
- **便于集成：** 作为纯执行层与 OpenOmniCloud 协作

OmniOperator 的 API 作为设备执行与高层控制之间的边界，既适合人类直接控制，也适用于 OpenOmniCloud 驱动的自动化。

## 参与贡献

欢迎贡献。请遵循以下指南以保持一致性。

### 分支策略

- 非小改动请使用 feature 分支。
- 提交 PR 到仓库默认分支。

### 代码风格

项目使用 **Ktlint** 进行代码风格检查，提交前请运行：

```bash
./gradlew ktlintCheck
```

建议在 IDE 中安装 Ktlint 插件以获得自动格式化支持。

### 构建说明

请先安装 **Flutter SDK**，然后初始化 Flutter 模块依赖：

```bash
cd flutter_module
flutter pub get
```

依赖完成后，可在 Android Studio 中构建，或使用 Gradle 命令行。

### 日志

请参考 [日志管理规范](docs/log_management.md) 以保持日志一致性。

### 支持

如需 FAQ 或排障，请查看 `docs/` 下文档或提交 issue。
