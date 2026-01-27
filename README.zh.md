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
  <a href="#功能">功能特性</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#api-参考">API 参考</a>
</p>

## 最新动态 🔥

- [2026.01.27] 🎉 **抢先支持 Clawdbot 插件接入**：通过 OmniOperator DevServer 为 Clawdbot 补齐 Android GUI 操作能力 👉 `integrations/clawdbot/omni-operator/README.md`

OmniOperator 是一款 Android 应用程序，旨在通过 HTTP 请求实现对 Android 设备的远程控制与交互。它在设备上运行一个本地代理和开发服务器（DevServer），充当外部指令与设备系统之间的桥梁。

项目内置了一个基于 Web 的 "DevServer Playground"，处于同一网络环境下的浏览器即可直接访问。该界面提供了便捷的设备调试、命令执行及状态检查功能。

![Omni Operator Demo](./demo.gif)

## Integrations / 生态集成

- **Clawdbot Plugin (早期/官方首要集成)**
  - **核心价值**：为 Clawdbot 赋予 Android GUI 的可视化操作能力。
  - **适用场景**：建议仅在局域网或受信任的网络环境中使用，请避免将 DevServer 直接暴露于公网。
  - **快速入口**：`integrations/clawdbot/omni-operator/README.md`
  - **演示**：可参考上方的 `./demo.gif`（后续将补充“聊天指令→手机执行→回传截图/反馈”的完整演示）。
  - **与 OpenOmniCloud 的关系**：OmniOperator 定位为 OpenOmniCloud 的设备端**执行层**，同时也作为开放组件供 Clawdbot 等第三方生态集成。

## 功能特性

**核心 Android 应用：**
* **设备内 HTTP 代理：** 作为网关，负责拦截、代理及转发相关的网络请求。
* **设备内 Dev Server：** 托管 Playground Web 界面并提供核心设备操作 API。
* **远程设备操作：** 支持通过 HTTP 接口执行屏幕点击（坐标映射）、应用启动等系统级操作。

**DevServer Playground (Web UI)：**
* **交互式屏幕镜像：**
    * 实时获取并渲染当前设备屏幕截图 (`/captureScreenshotImage`)。
    * 支持点击截图直接获取精确的像素坐标。
    * 获取的坐标可直接填入终端命令中，实现快速调试。
* **UI XML 检查器：**
    * 提取并展示当前 UI 层级的 XML 结构 (`/captureScreenshotXml`)。
    * 集成 Monaco Editor，提供代码高亮与折叠功能，便于分析 UI 树。
* **集成终端 (xterm.js)：**
    * **内置指令**：支持 `screenshot` (截图)、`xmlshot` (获取XML)、`help` (帮助)、`clear` (清屏)、`apidoc` (文档) 等快捷指令。
    * **动态控制**：直接执行设备控制命令，如 `tapCoordinate <x> <y>` (点击坐标) 或 `launchApplication <packageName>` (启动应用)。
    * **增强体验**：支持 Tab 自动补全、命令历史回溯以及 Ctrl+U 清除当前行。
* **API 文档集成：** 输入 `apidoc` 命令即可快速跳转至 `/redoc` 查看完整接口定义。

## 快速开始

1.  安装 APK 或从源码编译（详见下文）。
2.  在 Android 设备上启动 **OmniOperator** 并点击 **Start DevServer**。
3.  记录应用界面上显示的 IP 地址与端口号（例如 `http://192.168.1.5:8080`）。
4.  在同一局域网内的电脑或手机浏览器中输入该地址，进入 Playground。
5.  在终端尝试输入 `help`、`screenshot`、`xmlshot` 或 `apidoc` 开始探索。

## 环境要求

- **Android Studio** + **Android SDK**（需包含 platform tools / adb）。
- **JDK 17**（推荐用于现代 Android 项目构建）。
- **Flutter SDK**（用于构建 Web Playground 模块）。
- **Android 设备**（真机）或可联网的**模拟器**。

## 安装与配置 (Android 应用)

1.  **获取 APK：**
    * 直接下载发布的 `app-release.apk`。
    * 或者从源码自行构建生成。
2.  **安装：** 将 APK 安装至您的 Android 设备。
3.  **启动：** 打开 **OmniOperator** 应用。
4.  **运行服务：** 点击 **Start DevServer** 按钮。
    * *注意：为了获得完整的控制能力，系统可能会提示您开启无障碍服务权限。*
5.  **连接：** 应用将显示 DevServer 的运行 IP 和端口，在同网络下的浏览器中访问该地址即可。

## 源码构建指南

如果您希望从源码自行编译，请按以下步骤操作：

1.  **初始化 Flutter 模块：**
    ```bash
    cd flutter_module
    flutter pub get
    ```
2.  **构建 Android 应用：**
    ```bash
    ./gradlew assembleDebug
    ```
3.  **安装至设备：**
    ```bash
    ./gradlew :app:installDebug
    ```
4.  **启动：** 在设备上打开 **OmniOperator** 并启动 DevServer。

## API 参考

您可以在 Playground 的终端中输入 `apidoc` 查看常用命令，或访问 `http://<device-ip>:<port>/redoc` 获取完整的 Swagger/OpenAPI 文档。

## 兼容性与权限说明

- **平台**：Android（支持真机与模拟器）。
- **网络**：控制端（浏览器/脚本）需与 Android 设备处于同一局域网（LAN）。
- **权限**：为了实现点击、滑动等操作，应用需要获取**无障碍服务 (Accessibility Service)** 权限。

## 安全提示

> [!WARNING]
> * **DevServer 设计用于受信任的本地网络环境。**
> * **请务必避免将设备的 IP/端口暴露在公网上。**
> * **不使用时，建议在应用内关闭 DevServer。**

## 项目结构

```text
.
├── app/                 # Android 原生应用源码 (Kotlin)
├── flutter_module/      # Web Playground 源码 (Flutter)
└── docs/                # 补充文档与指南

```

## 架构设计理念

OmniOperator 是 **OpenOmniCloud** 体系中的设备侧**执行层**。它对外提供标准化的 HTTP 接口，并承载了一个轻量级的 DevServer Playground 用于人工调试与控制。

**设计原则**
目前的实现严格专注于“执行”这一核心职责，并遵循以下两条基本原则：

1. **被动运行 (Passive Execution)**
OmniOperator 仅在接收到明确的 API 请求时才执行动作。它本身不包含任何自主决策机制或目标驱动的逻辑。
2. **无状态 (Statelessness)**
服务不维护任何持久化的业务状态。所有的上下文信息（Context）都必须由调用方（如 OpenOmniCloud 或人工操作者）负责管理和维护。

**设计收益：**

* **职责清晰**：状态与逻辑完全交由控制方（OpenOmniCloud/人）管理。
* **可预测性**：设备行为完全取决于输入指令，无隐含副作用。
* **易扩展**：支持多个控制器复用同一设备资源。
* **易集成**：作为纯粹的执行单元，能够轻松接入 OpenOmniCloud 或其他自动化系统。

OmniOperator 的 API 构成了设备执行层与高层控制逻辑之间的清晰边界，这使得它既适合开发者进行手动调试，也是构建自动化系统的理想基石。

## 参与贡献

我们非常欢迎社区的贡献！为了保持项目的一致性，请遵循以下指南。

### 分支策略

* 对于非微小的改动，请创建独立的 `feature` 分支。
* 所有 Pull Request (PR) 请提交至仓库的默认分支。

### 代码风格

本项目使用 **Ktlint** 维护代码风格。在提交代码前，请运行以下命令进行检查：

```bash
./gradlew ktlintCheck

```

强烈建议在您的 IDE 中安装 Ktlint 插件，以便在编码时自动格式化。

### 构建说明

请确保已安装 **Flutter SDK**。在构建 Android 项目前，需先初始化 Flutter 依赖：

```bash
cd flutter_module
flutter pub get

```

依赖准备就绪后，即可使用 Android Studio 或 Gradle 命令行进行构建。

### 日志规范

请参考 [日志管理规范](https://www.google.com/search?q=docs/log_management.md) 以确保日志格式的统一性。

### 支持与反馈

如需查看 FAQ 或进行故障排查，请查阅 `docs/` 目录下的文档。如有未解决的问题，欢迎提交 Issue。
