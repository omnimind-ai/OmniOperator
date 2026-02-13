import 'package:flutter/material.dart';

enum AppLanguage { zh, en }

extension AppLanguageCode on AppLanguage {
  String get code => this == AppLanguage.zh ? 'zh' : 'en';
}

class AppStringsScope extends InheritedWidget {
  final AppStrings strings;

  const AppStringsScope({
    super.key,
    required this.strings,
    required super.child,
  });

  static AppStrings of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<AppStringsScope>();
    assert(scope != null, 'AppStringsScope not found in context');
    return scope!.strings;
  }

  @override
  bool updateShouldNotify(AppStringsScope oldWidget) {
    return oldWidget.strings.language != strings.language;
  }
}

class AppStrings {
  final AppLanguage language;

  const AppStrings(this.language);

  bool get _isZh => language == AppLanguage.zh;

  String get appTitle => 'Omni DevServer';

  String get settingsTitle => _isZh ? '设置' : 'Settings';
  String get settingsTooltip => settingsTitle;

  String get usageTitle => _isZh ? '使用' : 'Usage';
  String get companionModeTitle => _isZh ? '陪伴模式' : 'Companion Mode';
  String get companionModeOn => _isZh ? '已开启' : 'On';
  String get companionModeOff => _isZh ? '已关闭' : 'Off';

  String get languageTitle => _isZh ? '语言' : 'Language';
  String get languageSubtitle =>
      _isZh ? '选择界面语言' : 'Choose the interface language.';
  String get languageChineseLabel => '中文';
  String get languageEnglishLabel => 'English';

  String get agentServerTitle =>
      _isZh ? 'Agent Server 配置' : 'Agent Server Configuration';
  String get agentServerSubtitle =>
      _isZh
          ? '输入设备要连接的 Agent Server IP 和端口。'
          : 'Enter the IP address and port of the Agent Server your device will connect to.';
  String get serverIpLabel => _isZh ? 'Server IP 地址' : 'Server IP Address';
  String get serverIpHint =>
      _isZh ? '例如：192.168.1.100:5000' : 'e.g., 192.168.1.100:5000';
  String get serverIpDialogHint =>
      _isZh ? '例如：example.local:5000' : 'example.local:5000';
  String get serverAddressEmpty =>
      _isZh ? '请输入服务器地址' : 'Please enter a server address';
  String get serverAddressNeedPort =>
      _isZh
          ? '请包含端口号（如：:5000）'
          : 'Please include the port number (e.g., :5000)';

  // --- Authentication settings ---
  String get authSectionTitle => _isZh ? '认证设置' : 'Authentication';
  String get authSectionSubtitle =>
      _isZh
          ? '为 Socket.IO 连接和 DevServer 配置可选的认证保护。默认不启用。'
          : 'Configure optional authentication for Socket.IO connections and DevServer. Disabled by default.';
  String get socketAuthTitle =>
      _isZh ? 'Socket.IO 认证' : 'Socket.IO Authentication';
  String get socketAuthSubtitle =>
      _isZh
          ? '启用后，连接 Agent Server 时将携带此 Token 进行身份验证。'
          : 'When enabled, this token is sent during the Socket.IO handshake for server-side verification.';
  String get socketAuthTokenLabel => _isZh ? 'Auth Token' : 'Auth Token';
  String get socketAuthTokenHint =>
      _isZh ? '输入 Socket.IO 认证 Token' : 'Enter Socket.IO auth token';
  String get devServerAuthTitle =>
      _isZh ? 'DevServer 认证' : 'DevServer Authentication';
  String get devServerAuthSubtitle =>
      _isZh
          ? '启用后，所有 DevServer API 请求必须携带此 API Key（通过 Authorization: Bearer <key> 请求头）。'
          : 'When enabled, all DevServer API requests must include this API Key via the "Authorization: Bearer <key>" header.';
  String get devServerApiKeyLabel => _isZh ? 'API Key' : 'API Key';
  String get devServerApiKeyHint =>
      _isZh ? '输入 DevServer API Key' : 'Enter DevServer API Key';

  // --- Advanced settings ---
  String get advancedSectionTitle => _isZh ? '高级' : 'Advanced';
  String get advancedSectionSubtitle =>
      _isZh
          ? '调整高级参数。仅在你明确了解影响时修改。'
          : 'Adjust advanced parameters. Change only if you understand the impact.';
  String get screenshotQualityTitle => _isZh ? '截图质量' : 'Screenshot Quality';
  String screenshotQualityValue(int value) =>
      _isZh ? 'JPEG 质量：$value' : 'JPEG quality: $value';

  String get tryHint =>
      _isZh ? '试试：“设置明天8点的闹钟”' : 'Try: "Set an alarm for 8am tomorrow"';
  String get processingTooltip => _isZh ? '处理中...' : 'Processing...';
  String get sendTooltip => _isZh ? '发送' : 'Send';
  String get attachFileTooltip => _isZh ? '添加文件' : 'Attach file';
  String get attachmentNotImplemented =>
      _isZh ? '暂不支持文件附件。' : 'File attachment not implemented.';

  String get permissionDialogTitle => _isZh ? '需要权限' : 'Permissions Required';
  String get permissionDialogDescription =>
      _isZh
          ? 'OmniBot 需要以下权限才能在后台正常工作。'
          : 'OmniBot needs the following permissions to work correctly in the background.';
  String get accessibilityPermissionTitle =>
      _isZh ? '无障碍服务' : 'Accessibility Service';
  String get accessibilityPermissionDescription =>
      _isZh ? '允许应用响应系统事件。' : 'Allows the app to respond to system events.';
  String get batteryPermissionTitle => _isZh ? '电池优化' : 'Battery Optimization';
  String get batteryPermissionDescription =>
      _isZh ? '防止系统停止应用。' : 'Prevents the system from stopping the app.';

  String get connectAgentTitle => _isZh ? '连接到 Agent' : 'Connect to Agent';
  String get connectAgentSkip => _isZh ? '跳过' : 'Skip';
  String get connectAgentAction => _isZh ? '连接' : 'Connect';

  String devServerStartLabel({required bool compact}) =>
      _isZh
          ? (compact ? '启动' : '启动 DevServer')
          : (compact ? 'Start' : 'Start DevServer');
  String devServerStopLabel({required bool compact}) =>
      _isZh
          ? (compact ? '停止' : '停止 DevServer')
          : (compact ? 'Stop' : 'Stop DevServer');
  String get devServerDesktopOnly =>
      _isZh
          ? 'DevServer 模式请在电脑端发送任务。'
          : 'DevServer mode: send tasks from the desktop.';

  List<String> get initialSuggestions =>
      _isZh
          ? const ['📷 打开相机并拍一张照片', '📅 创建明天上午的会议提醒', '🛫 查询北京飞上海的机票']
          : const [
            '📷 Open the camera and take a photo',
            '📅 Create a reminder for tomorrow morning',
            '🛫 Check flights from Beijing to Shanghai',
          ];

  String get welcomeMessage =>
      _isZh
          ? '**嗨，欢迎来到你的专属Mobile智能体！👋** \n\n🧠 想让它帮忙？切换到代理模式，说一声就搞定。\n\n🤗 只是想专注做事？陪伴模式会默默记录，事后给你总结回顾～\n\n开始体验吧！'
          : '**Hi, welcome to your personal mobile agent! 👋** \n\n🧠 Need help? Switch to Agent mode and just ask.\n\n🤗 Want to stay focused? Companion mode will quietly observe and summarize afterwards.\n\nLet\'s get started!';
  String get suggestionTitle => _isZh ? '你可以这样说：' : 'Try saying:';

  String get summaryInProgress =>
      _isZh ? '正在为您总结...' : 'Preparing your summary...';
  String get companionModeEntered =>
      _isZh
          ? '📝 已进入陪伴模式～\n\n请按自己的节奏开始操作，智能体会在一旁默默观察。\n\n完成后，记得点击 **"浏览完成"** 按钮哦！'
          : '📝 Companion mode is on~\n\nGo at your own pace. The agent will quietly observe.\n\nWhen you\'re done, tap **"Finish Browsing"**.';
  String get finishBrowsing => _isZh ? '浏览完成' : 'Finish Browsing';
  String understoodTask(String task) =>
      _isZh ? '明白！马上帮你搞定："$task"' : 'Got it! I\'ll handle: "$task"';
  String get connectFailure =>
      _isZh
          ? '❌ 连接Agent Server失败，请检查网络连接或在设置中更新IP地址。'
          : '❌ Failed to connect to the Agent Server. Check your network or update the IP address in Settings.';
  String get operationInProgress =>
      _isZh
          ? '⚠️ 当前有操作正在进行，请稍后再试。'
          : '⚠️ An operation is already in progress. Please try again later.';

  String get devServerStartUser =>
      _isZh ? '🚀 启动 DevServer' : '🚀 Start DevServer';
  String get devServerStarting =>
      _isZh ? '正在启动开发服务器...' : 'Starting the dev server...';
  String devServerStartSuccess(String address) =>
      _isZh
          ? '🚀 DevServer 在 http://$address 启动成功！\n\n请确保您的设备已连接到同一网络，并在浏览器中访问该地址。\n\n⚠️ DevServer 模式下请在电脑端发送任务，手机端发送已禁用。'
          : '🚀 DevServer started at http://$address!\n\nMake sure your device is on the same network and open the address in a browser.\n\n⚠️ In DevServer mode, send tasks from the desktop. Mobile sending is disabled.';
  String devServerStartFailure(String message) =>
      _isZh
          ? '❌ DevServer 启动失败: $message'
          : '❌ DevServer failed to start: $message';
  String get devServerStopUser =>
      _isZh ? '🛑 停止 DevServer' : '🛑 Stop DevServer';
  String get devServerStopping =>
      _isZh ? '正在停止开发服务器...' : 'Stopping the dev server...';
  String get devServerStopped =>
      _isZh ? '✅ DevServer 已停止。' : '✅ DevServer stopped.';
  String devServerStopFailure(String message) =>
      _isZh
          ? '❌ DevServer 停止失败: $message'
          : '❌ DevServer failed to stop: $message';

  String get emptyBotMessage =>
      _isZh ? '错误：没有消息内容。' : 'Error: No message content.';
  String platformError(String message) =>
      _isZh ? '❌ 发生错误：$message' : '❌ An error occurred: $message';
}
