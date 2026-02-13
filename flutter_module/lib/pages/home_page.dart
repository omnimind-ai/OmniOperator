// lib/pages/home_page.dart

import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../l10n/app_strings.dart';
import '../services/history_service.dart';
import '../services/app_language_controller.dart';
import '../services/platform_channel.dart';
import '../models/chat_models.dart';
import '../widgets/chat_message_bubble.dart';
import '../widgets/ip_address_dialog.dart';
import '../widgets/chat_input_area.dart';
import '../widgets/permission_status_dialog.dart';
import '../widgets/history_messages.dart';
import './settings_page.dart';

// Define the EventChannel for bot messages
const _botMessageEventChannel = EventChannel(
  'cn.com.omnimind.omnibot/bot_message_events',
);

class HomePage extends StatefulWidget {
  final AppLanguageController languageController;

  const HomePage({super.key, required this.languageController});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with WidgetsBindingObserver {
  final _textController = TextEditingController();
  final List<Message> _messages = [];
  bool _isProcessing = false;
  bool _isAwaitingSystemPermission = false;
  Timer? _permissionCheckTimer;
  bool _isPermissionDialogVisible = false;
  final ScrollController _scrollController = ScrollController();
  final Set<String> _completedAnimations = {};
  final GlobalKey<AnimatedListState> _listKey = GlobalKey<AnimatedListState>();
  final FocusNode _inputFocusNode = FocusNode();

  // --- STATE VARIABLES ---
  bool _isCompanionModeEnabled = false;
  bool _isDevServerRunning = false;
  StreamSubscription? _botMessageSubscription;

  List<String> _historyMessages = [];
  bool _didInitMessages = false;
  String? _welcomeMessageId;

  /// Handles scrolling to the bottom when the text input gains focus.
  void _onFocusChange() {
    if (_inputFocusNode.hasFocus) {
      Future.delayed(const Duration(milliseconds: 300), () {
        _scrollToBottom();
      });
    }
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _inputFocusNode.addListener(_onFocusChange);

    _permissionCheckTimer = Timer.periodic(const Duration(seconds: 2), (timer) {
      _checkPermissions();
    });
    _checkPermissions();

    _listenToBotMessages();

    _loadHistoryMessages();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      _checkInitialServerAddress();
    });
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final strings = AppStringsScope.of(context);
    if (_didInitMessages) {
      _updateWelcomeMessage(strings);
      return;
    }
    final initialSuggestions =
        strings.initialSuggestions
            .map((text) => Suggestion(text: text))
            .toList();
    final initialMessage = Message(
      text: strings.welcomeMessage,
      sender: Sender.bot,
      suggestionTitle: strings.suggestionTitle,
      suggestions: initialSuggestions,
    );
    _messages.add(initialMessage);
    _completedAnimations.add(initialMessage.id);
    _welcomeMessageId = initialMessage.id;
    _didInitMessages = true;
  }

  void _updateWelcomeMessage(AppStrings strings) {
    if (_welcomeMessageId == null) return;
    final index = _messages.indexWhere(
      (message) => message.id == _welcomeMessageId,
    );
    if (index == -1) return;
    final updatedSuggestions =
        strings.initialSuggestions
            .map((text) => Suggestion(text: text))
            .toList();
    _messages[index] = Message(
      id: _welcomeMessageId,
      text: strings.welcomeMessage,
      sender: Sender.bot,
      suggestionTitle: strings.suggestionTitle,
      suggestions: updatedSuggestions,
    );
  }

  Future<void> _loadHistoryMessages() async {
    try {
      final messages = await getHistoryMessages();
      if (mounted) {
        setState(() {
          _historyMessages = messages;
        });
      }
    } catch (e) {
      debugPrint('Error loading history messages: $e');
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _permissionCheckTimer?.cancel();
    _textController.dispose();
    _scrollController.dispose();
    _inputFocusNode.removeListener(_onFocusChange);
    _inputFocusNode.dispose();
    _botMessageSubscription?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    if (state == AppLifecycleState.resumed) {
      if (_isAwaitingSystemPermission) {
        setState(() {
          _isAwaitingSystemPermission = false;
        });
      }
      _checkPermissions();

      _loadHistoryMessages();
    }
  }

  void _onSuggestionTapped(String text) {
    setState(() {
      _textController.text = text;
      _textController.selection = TextSelection.fromPosition(
        TextPosition(offset: _textController.text.length),
      );
    });
    _inputFocusNode.requestFocus();
  }

  void _listenToBotMessages() {
    _botMessageSubscription = _botMessageEventChannel
        .receiveBroadcastStream()
        .listen(
          _onBotMessageReceived,
          onError: (error) {
            debugPrint("Bot message stream error: $error");
          },
        );
  }

  void _onBotMessageReceived(dynamic event) {
    if (!mounted || event is! Map) return;
    final strings = AppStringsScope.of(context);

    final String text = event['message'] as String? ?? strings.emptyBotMessage;
    final String? suggestionTitle = event['suggestionTitle'] as String?;
    final List<dynamic>? rawSuggestions =
        event['suggestions'] as List<dynamic>?;

    final List<Suggestion>? suggestions =
        rawSuggestions?.map((s) => Suggestion(text: s.toString())).toList();

    final newMessage = Message(
      text: text,
      sender: Sender.bot,
      suggestionTitle:
          (suggestionTitle != "No suggestions") ? suggestionTitle : null,
      suggestions: suggestions,
    );

    final bool shouldReplaceLast =
        _messages.isNotEmpty &&
        _messages.last.sender == Sender.bot &&
        _messages.last.text == strings.summaryInProgress;

    if (shouldReplaceLast) {
      final oldMessage = _messages.last;
      _completedAnimations.remove(oldMessage.id);

      setState(() {
        _messages[_messages.length - 1] = newMessage;
        _isProcessing = false;
      });
    } else {
      if (_isProcessing) {
        setState(() {
          _isProcessing = false;
        });
      }
      _addMessage(newMessage);
    }

    _scrollToBottom();
  }

  Future<void> _checkPermissions() async {
    if (_isAwaitingSystemPermission || !mounted) {
      return;
    }

    final bool isAccessibilityEnabled = await platform.invokeMethod(
      'isAccessibilityServiceEnabled',
    );
    final bool isIgnoringOptimizations = await platform.invokeMethod(
      'isIgnoringBatteryOptimizations',
    );

    final bool allPermissionsGranted =
        isAccessibilityEnabled && isIgnoringOptimizations;

    if (allPermissionsGranted) {
      if (_isPermissionDialogVisible) {
        Navigator.of(context).pop();
      }
    } else {
      if (!_isPermissionDialogVisible) {
        _showUnifiedPermissionDialog(
          isAccessibilityEnabled,
          isIgnoringOptimizations,
        );
      }
    }
  }

  void _showUnifiedPermissionDialog(
    bool isAccessibilityEnabled,
    bool isBatteryOptimizationDisabled,
  ) {
    setState(() => _isPermissionDialogVisible = true);

    showDialog(
      context: context,
      barrierDismissible: false,
      builder:
          (BuildContext dialogContext) => PermissionStatusDialog(
            initialIsAccessibilityEnabled: isAccessibilityEnabled,
            initialIsBatteryOptimizationDisabled: isBatteryOptimizationDisabled,
            onOpenAccessibilitySettings: () {
              Navigator.of(dialogContext).pop();
              setState(() => _isAwaitingSystemPermission = true);
              platform.invokeMethod('openAccessibilitySettings');
            },
            onOpenBatterySettings: () {
              Navigator.of(dialogContext).pop();
              setState(() => _isAwaitingSystemPermission = true);
              platform.invokeMethod('openBatteryOptimizationSettings');
            },
          ),
    ).then((_) {
      if (mounted) {
        setState(() => _isPermissionDialogVisible = false);
      }
    });
  }

  Future<void> _checkInitialServerAddress() async {
    final prefs = await SharedPreferences.getInstance();
    const String serverIpKey = 'server_ip_address';

    if (!prefs.containsKey(serverIpKey)) {
      _showInitialSetupDialog();
    } else {
      final String? serverAddress = prefs.getString(serverIpKey);
      if (serverAddress != null && serverAddress.isNotEmpty) {
        try {
          await platform.invokeMethod('sendServerAddress', {
            'serverAddress': serverAddress,
          });
        } on PlatformException {
          // It's okay if this fails on startup
        }
      }
    }

    // Restore persisted auth settings and push them to native.
    // This ensures auth config survives app restarts without
    // requiring the user to open the settings page first.
    await _restoreAuthSettings(prefs);
  }

  Future<void> _restoreAuthSettings(SharedPreferences prefs) async {
    final socketEnabled = prefs.getBool('socket_auth_enabled') ?? false;
    final socketToken = prefs.getString('socket_auth_token') ?? '';
    final devServerEnabled = prefs.getBool('dev_server_auth_enabled') ?? false;
    final devServerKey = prefs.getString('dev_server_api_key') ?? '';

    try {
      await platform.invokeMethod('setSocketAuthToken', {
        'token': socketEnabled ? socketToken : '',
      });
      await platform.invokeMethod('setDevServerApiKey', {
        'apiKey': devServerEnabled ? devServerKey : '',
      });
    } on PlatformException catch (e) {
      debugPrint('Error restoring auth settings: ${e.message}');
    }

    final screenshotQuality =
        (prefs.getInt('screenshot_jpeg_quality') ?? 50).clamp(1, 100) as int;
    try {
      await platform.invokeMethod('setScreenshotQuality', {
        'quality': screenshotQuality,
      });
    } on PlatformException catch (e) {
      debugPrint('Error restoring screenshot quality: ${e.message}');
    }
  }

  Future<void> _showInitialSetupDialog() async {
    final result = await showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) => const IpAddressDialog(),
    );

    if (!mounted || result == null) return;

    if (result.isNotEmpty) {
      try {
        await platform.invokeMethod('sendServerAddress', {
          'serverAddress': result,
        });
      } on PlatformException {
        // do nothing
      }
    }
  }

  void _scrollToBottom([bool isAnimated = true]) {
    Future.delayed(const Duration(milliseconds: 100), () {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: Duration(milliseconds: isAnimated ? 300 : 1),
          curve: Curves.easeOut,
        );
      }
    });
  }

  void _setCompanionMode(bool enabled) {
    if (_isCompanionModeEnabled == enabled) return;

    if (enabled) {
      platform.invokeMethod('toggleCompanionMode', {'enabled': true});
      platform.invokeMethod('toggleFinishedBrowsing', {'finished': false});
    } else {
      platform.invokeMethod('toggleCompanionMode', {'enabled': false});
    }

    setState(() {
      _isCompanionModeEnabled = enabled;
    });
  }

  void _toggleCompanionMode() {
    _setCompanionMode(!_isCompanionModeEnabled);
  }

  Future<void> _finishCompanionBrowsing() async {
    await platform.invokeMethod('toggleFinishedBrowsing', {'finished': true});
    final strings = AppStringsScope.of(context);

    if (mounted) {
      setState(() {
        final int messageIndex = _messages.lastIndexWhere(
          (m) => m.onAction != null,
        );
        if (messageIndex != -1) {
          final originalMessage = _messages[messageIndex];
          _messages[messageIndex] = Message(
            text: originalMessage.text,
            sender: originalMessage.sender,
            actionText: originalMessage.actionText,
            onAction: null,
          );
        }
      });
    }

    _addMessage(Message(text: strings.summaryInProgress, sender: Sender.bot));
  }

  Future<void> _sendMessage(String text) async {
    final trimmedText = text.trim();
    if (trimmedText.isEmpty || _isProcessing) return;
    final strings = AppStringsScope.of(context);
    if (_isDevServerRunning) {
      _addMessage(
        Message(text: '⚠️ ${strings.devServerDesktopOnly}', sender: Sender.bot),
      );
      _scrollToBottom();
      return;
    }

    await saveMessageToHistory(trimmedText);

    _inputFocusNode.unfocus();

    setState(() {
      _isProcessing = true;
      _textController.clear();
    });

    _addMessage(Message(text: trimmedText, sender: Sender.user));
    _addMessage(Message(text: '...', sender: Sender.bot, isLoading: true));

    final String messageToSend =
        _isCompanionModeEnabled ? "COMPANION_MODE $trimmedText" : trimmedText;

    try {
      final connectionStatus = await platform.invokeMethod('sendTextToNative', {
        'message': messageToSend,
      });

      _messages.removeLast();
      _listKey.currentState?.removeItem(
        _messages.length,
        (context, animation) => const SizedBox.shrink(),
      );

      if (connectionStatus == "SUCCESS") {
        if (_isCompanionModeEnabled) {
          _addMessage(
            Message(
              text: strings.companionModeEntered,
              sender: Sender.bot,
              actionText: strings.finishBrowsing,
              onAction: _finishCompanionBrowsing,
            ),
          );
        } else {
          final botResponse = strings.understoodTask(trimmedText);
          _addMessage(Message(text: botResponse, sender: Sender.bot));
          setState(() {
            _isProcessing = true;
          });
        }
      } else {
        String botResponse;
        if (connectionStatus == "FAILURE") {
          botResponse = strings.connectFailure;
        } else {
          botResponse = strings.operationInProgress;
        }
        _addMessage(Message(text: botResponse, sender: Sender.bot));
        setState(() {
          _isProcessing = false;
        });
      }

      _loadHistoryMessages();
    } on PlatformException catch (e) {
      _messages.removeLast();
      _listKey.currentState?.removeItem(
        _messages.length,
        (context, animation) => const SizedBox.shrink(),
      );

      final botResponse = strings.platformError(e.message ?? '');
      _addMessage(Message(text: botResponse, sender: Sender.bot));
      setState(() {
        _isProcessing = false;
      });
    }

    _scrollToBottom();
  }

  Future<void> _startDevServer() async {
    if (_isProcessing) return;
    final strings = AppStringsScope.of(context);

    _inputFocusNode.unfocus();

    setState(() => _isProcessing = true);

    _addMessage(Message(text: strings.devServerStartUser, sender: Sender.user));
    _addMessage(
      Message(
        text: strings.devServerStarting,
        sender: Sender.bot,
        isLoading: true,
      ),
    );

    String botResponse;
    try {
      String address = await platform.invokeMethod('startServer');
      botResponse = strings.devServerStartSuccess(address);
      if (mounted) {
        setState(() => _isDevServerRunning = true);
      }
    } on PlatformException catch (e) {
      botResponse = strings.devServerStartFailure(e.message ?? '');
    } finally {
      if (mounted) {
        setState(() => _isProcessing = false);
      }
    }

    setState(() {
      _messages.removeLast();
      _listKey.currentState?.removeItem(
        _messages.length,
        (context, animation) => const SizedBox.shrink(),
      );
      _addMessage(Message(text: botResponse, sender: Sender.bot));
    });
    _scrollToBottom();
  }

  Future<void> _stopDevServer() async {
    if (_isProcessing) return;
    final strings = AppStringsScope.of(context);

    _inputFocusNode.unfocus();
    setState(() => _isProcessing = true);

    _addMessage(Message(text: strings.devServerStopUser, sender: Sender.user));
    _addMessage(
      Message(
        text: strings.devServerStopping,
        sender: Sender.bot,
        isLoading: true,
      ),
    );

    String botResponse;
    try {
      await platform.invokeMethod('stopServer');
      botResponse = strings.devServerStopped;
      if (mounted) {
        setState(() => _isDevServerRunning = false);
      }
    } on PlatformException catch (e) {
      botResponse = strings.devServerStopFailure(e.message ?? '');
    } finally {
      if (mounted) {
        setState(() => _isProcessing = false);
      }
    }

    setState(() {
      _messages.removeLast();
      _listKey.currentState?.removeItem(
        _messages.length,
        (context, animation) => const SizedBox.shrink(),
      );
      _addMessage(Message(text: botResponse, sender: Sender.bot));
    });
    _scrollToBottom();
  }

  void _addMessage(Message message) {
    if (!mounted) return;
    setState(() {
      _messages.add(message);
      if (_listKey.currentState != null) {
        _listKey.currentState!.insertItem(
          _messages.length - 1,
          duration: const Duration(milliseconds: 500),
        );
      }
    });
    _scrollToBottom();
  }

  @override
  Widget build(BuildContext context) {
    final latestBotMessageIndex = _messages.lastIndexWhere(
      (m) => m.sender == Sender.bot,
    );
    final theme = Theme.of(context);
    final strings = AppStringsScope.of(context);

    return Scaffold(
      appBar: AppBar(
        backgroundColor: theme.scaffoldBackgroundColor,
        elevation: 0,
        centerTitle: true,
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('🤖', style: TextStyle(fontSize: 24)),
            const SizedBox(width: 8),
            Text(
              'OmniBot',
              style: TextStyle(
                color: theme.colorScheme.onSurface,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: Icon(
              Icons.settings_outlined,
              color: theme.colorScheme.onSurface.withOpacity(0.8),
            ),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder:
                      (context) => SettingsPage(
                        isCompanionModeEnabled: _isCompanionModeEnabled,
                        onCompanionModeChanged: _setCompanionMode,
                        languageController: widget.languageController,
                      ),
                ),
              );
            },
            tooltip: strings.settingsTooltip,
          ),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(1.0),
          child: Container(color: Colors.grey.withOpacity(0.2), height: 1.0),
        ),
      ),
      body: GestureDetector(
        onTap: () {
          _inputFocusNode.unfocus();
        },
        child: Stack(
          children: [
            NotificationListener<ScrollNotification>(
              onNotification: (scrollNotification) {
                if (scrollNotification is UserScrollNotification) {
                  if (_inputFocusNode.hasFocus) {
                    _inputFocusNode.unfocus();
                  }
                }
                return false;
              },
              child: AnimatedList(
                key: _listKey,
                controller: _scrollController,
                padding: const EdgeInsets.fromLTRB(16.0, 8.0, 16.0, 160.0),
                initialItemCount: _messages.length,
                itemBuilder: (context, index, animation) {
                  final message = _messages[index];
                  final isLatestBotMessage =
                      (message.sender == Sender.bot &&
                          index == latestBotMessageIndex);
                  final bool shouldAnimateTyping =
                      isLatestBotMessage &&
                      !_completedAnimations.contains(message.id) &&
                      message.onAction == null;

                  return SlideTransition(
                    position: Tween<Offset>(
                      begin: const Offset(0, 0.5),
                      end: Offset.zero,
                    ).animate(
                      CurvedAnimation(
                        parent: animation,
                        curve: Curves.easeOutQuart,
                      ),
                    ),
                    child: FadeTransition(
                      opacity: animation,
                      child: ChatMessageBubble(
                        key: ValueKey(message.id),
                        message: message,
                        shouldAnimate: shouldAnimateTyping,
                        isLatestBotMessage: isLatestBotMessage,
                        onSuggestionTapped: _onSuggestionTapped,
                        onSuggestionsDisplayed: _scrollToBottom,
                        onCharacterTyped:
                            shouldAnimateTyping
                                ? () => _scrollToBottom(false)
                                : null,
                        onAnimationCompleted:
                            () => _completedAnimations.add(message.id),
                      ),
                    ),
                  );
                },
              ),
            ),
            Align(
              alignment: Alignment.bottomCenter,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (_historyMessages.isNotEmpty)
                    HistoryMessages(
                      messages: _historyMessages,
                      onMessageTap: _onSuggestionTapped,
                    ),
                  ChatInputArea(
                    controller: _textController,
                    focusNode: _inputFocusNode,
                    isProcessing: _isProcessing,
                    isDevServerRunning: _isDevServerRunning,
                    onSendMessage: _sendMessage,
                    onStartDevServer: _startDevServer,
                    onStopDevServer: _stopDevServer,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
