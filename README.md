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
  <a href="README.zh.md">Chinese Docs</a> •
  <a href="#features">Features</a> •
  <a href="#quickstart">Quickstart</a> •
  <a href="#api-reference">API Reference</a>
</p>

## Latest News 🔥

- [2026.01.27] 🎉 **Clawdbot Plugin integration landed**: control Android GUI via OmniOperator DevServer 👉 `integrations/clawdbot/omni-operator/README.md`

OmniOperator is an Android application designed to enable remote control and interaction with Android devices via HTTP requests. It runs a local proxy and a development server (DevServer) on the device, acting as a bridge between external commands and the Android system.

The project includes a web-based "DevServer Playground" that can be accessed from any browser on the same network, providing a convenient UI for device debugging, command execution, and health/status checks.

![Omni Operator Demo](./demo.gif)

## Integrations / Ecosystem Integrations

- **Clawdbot Plugin (Early/first-class integration)**
  - **Core value**: brings visual Android GUI control to Clawdbot.
  - **Scope**: use only on LAN / trusted networks; do not expose DevServer to the public internet.
  - **Quickstart**: `integrations/clawdbot/omni-operator/README.md`
  - **Demo**:
    <table>
      <tr>
        <td><a href="1.mp4">Demo 1 (MP4)</a></td>
        <td><a href="2.mp4">Demo 2 (MP4)</a></td>
      </tr>
    </table>
  - **Relationship with OpenOmniCloud**: OmniOperator is positioned as the on-device **execution layer** for OpenOmniCloud, and is also an open component for third-party ecosystem integrations (Clawdbot is among the first).

## Features

**Core Android Application:**
* **On-device HTTP proxy:** Acts as a gateway for intercepting, proxying, and forwarding related network requests.
* **On-device DevServer:** Hosts the Playground web UI and exposes core device operation APIs.
* **Remote device operations:** Supports system-level operations such as coordinate-mapped taps and launching applications via HTTP APIs.

**DevServer Playground (Web UI):**
* **Interactive screen mirroring:**
  * Fetch and render the current device screenshot (`/captureScreenshotImage`).
  * Click on the screenshot to get precise pixel coordinates.
  * Use the coordinates directly in terminal commands for rapid debugging.
* **UI XML inspector:**
  * Extract and display the current UI hierarchy as XML (`/captureScreenshotXml`).
  * Built-in Monaco Editor with syntax highlighting and folding for easier UI-tree inspection.
* **Integrated terminal (xterm.js):**
  * **Built-in commands**: `screenshot`, `xmlshot`, `help`, `clear`, `apidoc`, etc.
  * **Direct control**: run commands like `tapCoordinate <x> <y>` or `launchApplication <packageName>`.
  * **Quality of life**: tab completion, command history, and Ctrl+U to clear the current line.
* **API documentation**: run `apidoc` to jump to `/redoc` for full API definitions.

## Quickstart

1. Install the APK or build from source (see below).
2. Launch **OmniOperator** on your Android device and tap **Start DevServer**.
3. Copy the IP address and port shown in the app (e.g., `http://192.168.1.5:8080`).
4. Open the address in a browser on the same LAN to enter the Playground.
5. Try `help`, `screenshot`, `xmlshot`, or `apidoc` in the terminal to get started.

## Prerequisites

- **Android Studio** + **Android SDK** (with platform tools / adb).
- **JDK 17** (recommended for modern Android builds).
- **Flutter SDK** (for building the web Playground module).
- An Android **physical device** or a network-accessible **emulator**.

## Installation & Setup (Android Application)

1. **Get the APK:**
   * Download the published `app-release.apk`.
   * Or build it from source.
2. **Install** the APK on your Android device.
3. **Launch** **OmniOperator**.
4. **Start the service:** tap **Start DevServer**.
   * Note: the system may prompt you to enable Accessibility Service for full control capabilities.
5. **Connect:** the app will display the DevServer IP and port; open it in a browser on the same network.

## Build & Run (From Source)

If you want to build from source, follow these steps:

1. Initialize the Flutter module:
    ```bash
    cd flutter_module
    flutter pub get
    ```
2. Build the Android app:
    ```bash
    ./gradlew assembleDebug
    ```
3. Install to a connected device:
    ```bash
    ./gradlew :app:installDebug
    ```
4. Launch **OmniOperator** on the device and start the DevServer.

## API Reference

Use `apidoc` in the Playground terminal, or open `http://<device-ip>:<port>/redoc` to view the full Swagger/OpenAPI documentation.

## Compatibility & Permissions

- Target platform: Android (device or emulator).
- Network: the controller (browser/scripts) must be on the same LAN as the device.
- Permissions: Accessibility Service permission is required for actions like tap/scroll.

## Security Notes

> [!WARNING]
> *   The DevServer is intended for trusted local networks only.
> *   Avoid exposing the device IP/port to the public internet.
> *   Stop the DevServer when not in use.

## Project Layout

```text
.
├── app/                 # Android application source
├── flutter_module/      # Web Playground UI
└── docs/                # Additional documentation
```

## Architecture Design

OmniOperator is the on-device **execution layer** in the **OpenOmniCloud** stack. It exposes a standardized HTTP interface and ships with a lightweight DevServer Playground for manual debugging and control.

**Design principles**
The current implementation focuses strictly on “execution” and follows two fundamental principles:

1. **Passive execution**
   OmniOperator executes actions only when explicitly requested via API. It contains no autonomous decision-making or goal-driven logic.

2. **Statelessness**
   The service maintains no persistent business state. All context must be managed by the caller (e.g., OpenOmniCloud or a human operator).

**Benefits**
* **Clear responsibilities:** state and logic live entirely in the controller (OpenOmniCloud/human).
* **Predictability:** device behavior depends only on explicit inputs.
* **Extensibility:** multiple controllers can reuse the same device surface.
* **Easy integration:** as a pure execution unit, it plugs into OpenOmniCloud and other automation systems.

OmniOperator’s APIs form a clean boundary between device operations and higher-level control logic, making it suitable for both manual debugging (via the Playground) and OpenOmniCloud-driven automation.

## Contributing

We welcome contributions to OmniOperator. When contributing, please follow these guidelines to maintain consistency across the project.

### Branching Strategy

- Use a feature branch for non-trivial changes.
- Open a Pull Request against the repository's default branch.

### Code Style

We enforce consistent code style using **Ktlint**. Before committing your changes, please run:

```bash
./gradlew ktlintCheck
```

We recommend installing the Ktlint plugin in your IDE for automatic formatting support.

### Building the Project

To build the project locally, you need to install the **Flutter SDK** first.

Then, initialize the Flutter module dependencies:

```bash
cd flutter_module
flutter pub get
```

Once dependencies are installed, you can build the full project using Android Studio or the Gradle command line.

### Logging

Please follow our [log management guidelines](docs/log_management.md) to keep logging consistent across the project.

### Support

For Frequently Asked Questions (FAQs) and troubleshooting, see the documentation under `docs/` or file an issue describing your problem.
