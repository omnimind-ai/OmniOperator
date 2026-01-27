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

OmniOperator is an Android application that empowers users to control and interact with their Android device remotely via HTTP requests. It achieves this by starting a local proxy and a development server directly on the device.

The project includes a web-based "DevServer Playground," accessible from any browser on the same network, allowing for intuitive device manipulation, command execution, and inspection.

![Omni Operator Demo](./demo.gif)

## Features

**Core Android Application:**
*   **On-Device HTTP Proxy:** Details of proxy functionality would be part of the Android app's core logic.
*   **On-Device Dev Server:** Hosts the Playground and exposes an API for device operations.
*   **Remote Device Operations:** Allows performing actions like tapping coordinates, opening applications, etc., via HTTP requests.

**DevServer Playground (Web UI):**
*   **Interactive Screenshot View:**
    *   Fetch and display the current device screen (`/captureScreenshotImage`).
    *   Click on the screenshot to get accurate pixel coordinates.
    *   Coordinates are displayed and can be easily used in terminal commands.
*   **UI XML Inspector:**
    *   Fetch and display the XML representation of the current UI hierarchy (`/captureScreenshotXml`).
    *   Uses the Monaco Editor for a rich XML viewing experience (syntax highlighting, folding).
*   **Integrated Terminal (xterm.js):**
    *   Execute built-in commands for playground interaction (`screenshot`, `xmlshot`, `help`, `clear`, `apidoc`).
    *   Execute dynamic, server-defined commands to control the Android device (e.g., `tapCoordinate <x> <y>`, `launchApplication <packageName>`).
    *   Tab completion for commands.
    *   Command history (Arrow Up).
    *   Clear current line (Ctrl+U).
*   **API Documentation Access:** A command (`apidoc`) to easily open the API documentation (hosted at `/redoc`).

## Quickstart

1. Install the APK or build from source (see below).
2. Open **OmniOperator** on the device and **Start DevServer**.
3. Note the IP/port shown in the app (for example: `http://192.168.1.5:8080`).
4. Open that address in a browser on the same network to access the Playground.
5. Use the terminal commands (`help`, `screenshot`, `xmlshot`, `apidoc`) to explore.

## Prerequisites

- Android Studio + Android SDK (with platform tools / adb).
- JDK 17 (recommended for modern Android builds).
- Flutter SDK (for the web playground module).
- A physical Android device or emulator with network access.

## Installation & Setup (Android Application)

1.  Obtain the APK.
    *   Download the latest `app-debug.apk` from the project's releases page.
    *   Or, build the Android project from the source code to generate the APK.
2.  Install on Android Device.
3.  Open the **OmniOperator** application on your Android device.
4.  Within the app, find the option to **Start DevServer**.
    *   AccessibilityService permissions might be required for full functionality.
5.  The app should display the IP address and port where the DevServer is running (e.g., `http://192.168.1.5:8080`).
6.  Open a web browser on your computer (or another device on the same network) and navigate to this address.

## Build & Run (From Source)

1.  Initialize the Flutter module:
    ```bash
    cd flutter_module
    flutter pub get
    ```
2.  Build the Android app:
    ```bash
    ./gradlew assembleDebug
    ```
3.  Install to a connected device:
    ```bash
    ./gradlew :app:installDebug
    ```
4.  Launch **OmniOperator** on the device and start the DevServer.

## API Reference

For a comprehensive list and details of all API endpoints, parameters, and responses, use the `apidoc` command in the Playground's terminal or directly navigate to `http://<device-ip>:<port>/redoc`.

## Compatibility & Permissions

- Target platform: Android (device or emulator).
- Network: the device and browser must be on the same LAN.
- Permissions: accessibility service may be required for full control actions.

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

OmniOperator is the device-side execution layer used by **OpenOmniCloud**. It exposes a clean HTTP interface for device operations and hosts a lightweight DevServer Playground for debugging and manual control.

**Design Principles**  
This implementation focuses exclusively on OmniOperator and follows two core architectural principles:

1. **Passive Operation**  
   OmniOperator operates strictly in a reactive mode - it performs actions only when explicitly requested through its API. It contains no autonomous behavior, decision-making logic, or goal-pursuing capabilities. This ensures predictable control and clear responsibility boundaries.

2. **Statelessness**  
   The service maintains zero persistent state between operations. All contextual information (task progress, operation history, or environmental observations) must be managed externally by the caller (for example OpenOmniCloud or a human operator). Each API request contains all necessary context to complete its operation.

These design choices yield important benefits:
- **Clear Separation of Concerns:** State management resides exclusively with controllers (human or agent)
- **Deterministic Behavior:** Operations depend solely on explicit inputs
- **Simplified Scaling:** Multiple controllers can interact with the same device
- **Agent Compatibility:** Complements OpenOmniCloud by providing pure execution capabilities

OmniOperator's API serves as the boundary between device-level operations and higher-order control, making it reusable for both direct human control (through the DevServer Playground) and OpenOmniCloud-driven automation.

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

Please follow our [log management guidelines](docs/log_management.md) to maintain consistent and meaningful logging across the project.

### Support

For Frequently Asked Questions (FAQs) and troubleshooting, see the documentation under `docs/` or file an issue describing your problem.
