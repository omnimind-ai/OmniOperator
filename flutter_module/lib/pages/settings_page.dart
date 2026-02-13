// lib/pages/settings_page.dart

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../l10n/app_strings.dart';
import '../services/app_language_controller.dart';
import '../services/platform_channel.dart';

class SettingsPage extends StatefulWidget {
  final bool isCompanionModeEnabled;
  final ValueChanged<bool> onCompanionModeChanged;
  final AppLanguageController languageController;

  const SettingsPage({
    super.key,
    required this.isCompanionModeEnabled,
    required this.onCompanionModeChanged,
    required this.languageController,
  });

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _serverIpController;
  late final TextEditingController _socketAuthTokenController;
  late final TextEditingController _devServerApiKeyController;
  late final TextEditingController _screenshotQualityController;
  late final TextEditingController _screenshotScaleController;
  // Use a consistent key across the app.
  static const String _serverIpKey = 'server_ip_address';
  static const String _socketAuthEnabledKey = 'socket_auth_enabled';
  static const String _socketAuthTokenKey = 'socket_auth_token';
  static const String _devServerAuthEnabledKey = 'dev_server_auth_enabled';
  static const String _devServerApiKeyKey = 'dev_server_api_key';
  static const String _screenshotQualityKey = 'screenshot_jpeg_quality';
  static const String _screenshotResizeEnabledKey = 'screenshot_resize_enabled';
  static const String _screenshotScalePercentKey = 'screenshot_scale_percent';
  static const int _defaultScreenshotQuality = 50;
  static const int _defaultScreenshotScalePercent = 100;
  Timer? _serverDebounceTimer;
  Timer? _authDebounceTimer;
  Timer? _advancedDebounceTimer;
  late bool _isCompanionModeEnabled;
  late AppLanguage _selectedLanguage;
  bool _socketAuthEnabled = false;
  bool _devServerAuthEnabled = false;
  bool _obscureSocketToken = true;
  bool _obscureDevServerKey = true;
  int _screenshotQuality = _defaultScreenshotQuality;
  bool _screenshotResizeEnabled = false;
  int _screenshotScalePercent = _defaultScreenshotScalePercent;

  @override
  void initState() {
    super.initState();
    _serverIpController = TextEditingController();
    _socketAuthTokenController = TextEditingController();
    _devServerApiKeyController = TextEditingController();
    _screenshotQualityController = TextEditingController(
      text: _defaultScreenshotQuality.toString(),
    );
    _screenshotScaleController = TextEditingController(
      text: _defaultScreenshotScalePercent.toString(),
    );
    _isCompanionModeEnabled = widget.isCompanionModeEnabled;
    _selectedLanguage = widget.languageController.language;

    // Load saved values first, then attach listeners to avoid
    // triggering auto-save during initial load.
    _initSettings();
  }

  Future<void> _initSettings() async {
    await Future.wait([
      _loadServerAddress(),
      _loadAuthSettings(),
      _loadAdvancedSettings(),
    ]);
    if (!mounted) return;
    _serverIpController.addListener(_onServerAddressChanged);
    _socketAuthTokenController.addListener(_onAuthSettingChanged);
    _devServerApiKeyController.addListener(_onAuthSettingChanged);
  }

  @override
  void dispose() {
    _serverDebounceTimer?.cancel();
    _authDebounceTimer?.cancel();
    _advancedDebounceTimer?.cancel();
    _serverIpController.removeListener(_onServerAddressChanged);
    _socketAuthTokenController.removeListener(_onAuthSettingChanged);
    _devServerApiKeyController.removeListener(_onAuthSettingChanged);
    _serverIpController.dispose();
    _socketAuthTokenController.dispose();
    _devServerApiKeyController.dispose();
    _screenshotQualityController.dispose();
    _screenshotScaleController.dispose();
    super.dispose();
  }

  void _toggleCompanionMode(bool enabled) {
    if (_isCompanionModeEnabled == enabled) return;
    setState(() {
      _isCompanionModeEnabled = enabled;
    });
    widget.onCompanionModeChanged(enabled);
  }

  Future<void> _changeLanguage(AppLanguage language) async {
    if (_selectedLanguage == language) return;
    setState(() {
      _selectedLanguage = language;
    });
    await widget.languageController.setLanguage(language);
  }

  Future<void> _loadServerAddress() async {
    final prefs = await SharedPreferences.getInstance();
    final serverIp = prefs.getString(_serverIpKey) ?? '';
    if (mounted) {
      _serverIpController.text = serverIp;
    }
  }

  Future<void> _loadAuthSettings() async {
    final prefs = await SharedPreferences.getInstance();
    if (mounted) {
      setState(() {
        _socketAuthEnabled = prefs.getBool(_socketAuthEnabledKey) ?? false;
        _socketAuthTokenController.text =
            prefs.getString(_socketAuthTokenKey) ?? '';
        _devServerAuthEnabled =
            prefs.getBool(_devServerAuthEnabledKey) ?? false;
        _devServerApiKeyController.text =
            prefs.getString(_devServerApiKeyKey) ?? '';
      });
    }
  }

  Future<void> _saveAuthSettings() async {
    final prefs = await SharedPreferences.getInstance();

    await prefs.setBool(_socketAuthEnabledKey, _socketAuthEnabled);
    final socketToken =
        _socketAuthEnabled ? _socketAuthTokenController.text.trim() : '';
    await prefs.setString(_socketAuthTokenKey, socketToken);

    await prefs.setBool(_devServerAuthEnabledKey, _devServerAuthEnabled);
    final devServerKey =
        _devServerAuthEnabled ? _devServerApiKeyController.text.trim() : '';
    await prefs.setString(_devServerApiKeyKey, devServerKey);

    // Push values to native side via MethodChannel
    try {
      await platform.invokeMethod('setSocketAuthToken', {
        'token': _socketAuthEnabled ? socketToken : '',
      });
      await platform.invokeMethod('setDevServerApiKey', {
        'apiKey': _devServerAuthEnabled ? devServerKey : '',
      });
    } on PlatformException catch (e) {
      debugPrint('Error pushing auth settings to native: ${e.message}');
    }
  }

  Future<void> _loadAdvancedSettings() async {
    final prefs = await SharedPreferences.getInstance();
    final screenshotQuality =
        prefs.getInt(_screenshotQualityKey) ?? _defaultScreenshotQuality;
    final screenshotScalePercent =
        prefs.getInt(_screenshotScalePercentKey) ??
        _defaultScreenshotScalePercent;
    final resizeEnabled = prefs.getBool(_screenshotResizeEnabledKey) ?? false;
    final normalizedQuality = screenshotQuality.clamp(1, 100) as int;
    final normalizedScale = screenshotScalePercent.clamp(1, 100) as int;
    if (mounted) {
      setState(() {
        _screenshotQuality = normalizedQuality;
        _screenshotResizeEnabled = resizeEnabled;
        _screenshotScalePercent = normalizedScale;
      });
    }
    _screenshotQualityController.text = normalizedQuality.toString();
    _screenshotScaleController.text = normalizedScale.toString();
    await Future.wait([
      _pushScreenshotQualityToNative(normalizedQuality),
      _pushScreenshotResizeToNative(
        enabled: resizeEnabled,
        scalePercent: normalizedScale,
      ),
    ]);
  }

  Future<void> _saveAdvancedSettings() async {
    final prefs = await SharedPreferences.getInstance();
    final normalizedQuality = _screenshotQuality.clamp(1, 100) as int;
    final normalizedScale = _screenshotScalePercent.clamp(1, 100) as int;
    await prefs.setInt(_screenshotQualityKey, normalizedQuality);
    await prefs.setBool(_screenshotResizeEnabledKey, _screenshotResizeEnabled);
    await prefs.setInt(_screenshotScalePercentKey, normalizedScale);
    await Future.wait([
      _pushScreenshotQualityToNative(normalizedQuality),
      _pushScreenshotResizeToNative(
        enabled: _screenshotResizeEnabled,
        scalePercent: normalizedScale,
      ),
    ]);
  }

  Future<void> _pushScreenshotQualityToNative(int quality) async {
    try {
      await platform.invokeMethod('setScreenshotQuality', {
        'quality': quality.clamp(1, 100),
      });
    } on PlatformException catch (e) {
      debugPrint('Error pushing screenshot quality to native: ${e.message}');
    }
  }

  Future<void> _pushScreenshotResizeToNative({
    required bool enabled,
    required int scalePercent,
  }) async {
    try {
      await platform.invokeMethod('setScreenshotResize', {
        'enabled': enabled,
        'scalePercent': scalePercent.clamp(1, 100),
      });
    } on PlatformException catch (e) {
      debugPrint('Error pushing screenshot resize to native: ${e.message}');
    }
  }

  Future<void> _saveServerAddress() async {
    if (!(_formKey.currentState?.validate() ?? false)) {
      return; // Exit if form is not valid
    }

    final ipAddress = _serverIpController.text.trim();

    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_serverIpKey, ipAddress);

      await platform.invokeMethod('sendServerAddress', {
        'serverAddress': ipAddress,
      });
    } on PlatformException catch (e) {
      debugPrint("Error saving server address: ${e.message}");
    }
  }

  void _onServerAddressChanged() {
    _serverDebounceTimer?.cancel();
    _serverDebounceTimer = Timer(const Duration(milliseconds: 800), () {
      _saveServerAddress();
    });
  }

  void _onAuthSettingChanged() {
    _authDebounceTimer?.cancel();
    _authDebounceTimer = Timer(const Duration(milliseconds: 800), () {
      _saveAuthSettings();
    });
  }

  void _onScreenshotQualityChanged(double value) {
    final normalized = value.round().clamp(1, 100) as int;
    setState(() {
      _screenshotQuality = normalized;
    });
    if (_screenshotQualityController.text != normalized.toString()) {
      _screenshotQualityController.text = normalized.toString();
    }
    _advancedDebounceTimer?.cancel();
    _advancedDebounceTimer = Timer(const Duration(milliseconds: 300), () {
      _saveAdvancedSettings();
    });
  }

  void _onScreenshotQualityTextSubmitted(String value) {
    final parsed = int.tryParse(value.trim());
    final normalized = (parsed ?? _screenshotQuality).clamp(1, 100) as int;
    setState(() {
      _screenshotQuality = normalized;
    });
    _screenshotQualityController.text = normalized.toString();
    _saveAdvancedSettings();
  }

  void _onScreenshotScaleChanged(double value) {
    final normalized = value.round().clamp(1, 100) as int;
    setState(() {
      _screenshotScalePercent = normalized;
    });
    if (_screenshotScaleController.text != normalized.toString()) {
      _screenshotScaleController.text = normalized.toString();
    }
    _advancedDebounceTimer?.cancel();
    _advancedDebounceTimer = Timer(const Duration(milliseconds: 300), () {
      _saveAdvancedSettings();
    });
  }

  void _onScreenshotScaleTextSubmitted(String value) {
    final parsed = int.tryParse(value.trim());
    final normalized = (parsed ?? _screenshotScalePercent).clamp(1, 100) as int;
    setState(() {
      _screenshotScalePercent = normalized;
    });
    _screenshotScaleController.text = normalized.toString();
    _saveAdvancedSettings();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final strings = AppStringsScope.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(strings.settingsTitle),
        backgroundColor: theme.scaffoldBackgroundColor,
        elevation: 0,
        foregroundColor: theme.colorScheme.onSurface,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(1.0),
          child: Container(color: Colors.grey.withOpacity(0.2), height: 1.0),
        ),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16.0),
          children: [
            Text(
              strings.usageTitle,
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w500,
              ),
            ),
            const SizedBox(height: 8),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(strings.companionModeTitle),
              subtitle: Text(
                _isCompanionModeEnabled
                    ? strings.companionModeOn
                    : strings.companionModeOff,
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onSurface.withOpacity(0.7),
                ),
              ),
              value: _isCompanionModeEnabled,
              onChanged: _toggleCompanionMode,
            ),
            const SizedBox(height: 24),
            Text(
              strings.languageTitle,
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w500,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              strings.languageSubtitle,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurface.withOpacity(0.7),
              ),
            ),
            const SizedBox(height: 16),
            SegmentedButton<AppLanguage>(
              segments: [
                ButtonSegment(
                  value: AppLanguage.zh,
                  label: Text(strings.languageChineseLabel),
                ),
                ButtonSegment(
                  value: AppLanguage.en,
                  label: Text(strings.languageEnglishLabel),
                ),
              ],
              selected: {_selectedLanguage},
              onSelectionChanged: (selection) {
                _changeLanguage(selection.first);
              },
            ),
            const SizedBox(height: 24),
            Text(
              strings.agentServerTitle,
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w500,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              strings.agentServerSubtitle,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurface.withOpacity(0.7),
              ),
            ),
            const SizedBox(height: 24),
            TextFormField(
              controller: _serverIpController,
              decoration: InputDecoration(
                labelText: strings.serverIpLabel,
                hintText: strings.serverIpHint,
                prefixIcon: Icon(
                  Icons.dns_rounded,
                  color: theme.primaryColor.withOpacity(0.8),
                ),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12.0),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12.0),
                  borderSide: BorderSide(color: theme.primaryColor),
                ),
              ),
              keyboardType: TextInputType.url,
              autovalidateMode: AutovalidateMode.onUserInteraction,
              validator: (value) {
                if (value == null || value.trim().isEmpty) {
                  return strings.serverAddressEmpty;
                }
                if (!value.contains(':')) {
                  return strings.serverAddressNeedPort;
                }
                return null;
              },
            ),
            const SizedBox(height: 32),
            Divider(color: Colors.grey.withOpacity(0.3)),
            const SizedBox(height: 16),
            // --- Authentication Section ---
            Text(
              strings.authSectionTitle,
              style: theme.textTheme.titleLarge?.copyWith(
                fontWeight: FontWeight.w500,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              strings.authSectionSubtitle,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurface.withOpacity(0.7),
              ),
            ),
            const SizedBox(height: 16),
            // Socket.IO Auth
            Text(
              strings.socketAuthTitle,
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w500,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              strings.socketAuthSubtitle,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurface.withOpacity(0.6),
              ),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(strings.socketAuthTitle),
              value: _socketAuthEnabled,
              onChanged: (val) {
                setState(() => _socketAuthEnabled = val);
                _saveAuthSettings();
              },
            ),
            if (_socketAuthEnabled)
              TextFormField(
                controller: _socketAuthTokenController,
                obscureText: _obscureSocketToken,
                decoration: InputDecoration(
                  labelText: strings.socketAuthTokenLabel,
                  hintText: strings.socketAuthTokenHint,
                  prefixIcon: Icon(
                    Icons.key_rounded,
                    color: theme.primaryColor.withOpacity(0.8),
                  ),
                  suffixIcon: IconButton(
                    icon: Icon(
                      _obscureSocketToken
                          ? Icons.visibility_off
                          : Icons.visibility,
                    ),
                    onPressed:
                        () => setState(
                          () => _obscureSocketToken = !_obscureSocketToken,
                        ),
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12.0),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12.0),
                    borderSide: BorderSide(color: theme.primaryColor),
                  ),
                ),
              ),
            const SizedBox(height: 24),
            // DevServer Auth
            Text(
              strings.devServerAuthTitle,
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w500,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              strings.devServerAuthSubtitle,
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurface.withOpacity(0.6),
              ),
            ),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(strings.devServerAuthTitle),
              value: _devServerAuthEnabled,
              onChanged: (val) {
                setState(() => _devServerAuthEnabled = val);
                _saveAuthSettings();
              },
            ),
            if (_devServerAuthEnabled)
              TextFormField(
                controller: _devServerApiKeyController,
                obscureText: _obscureDevServerKey,
                decoration: InputDecoration(
                  labelText: strings.devServerApiKeyLabel,
                  hintText: strings.devServerApiKeyHint,
                  prefixIcon: Icon(
                    Icons.vpn_key_rounded,
                    color: theme.primaryColor.withOpacity(0.8),
                  ),
                  suffixIcon: IconButton(
                    icon: Icon(
                      _obscureDevServerKey
                          ? Icons.visibility_off
                          : Icons.visibility,
                    ),
                    onPressed:
                        () => setState(
                          () => _obscureDevServerKey = !_obscureDevServerKey,
                        ),
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12.0),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12.0),
                    borderSide: BorderSide(color: theme.primaryColor),
                  ),
                ),
              ),
            const SizedBox(height: 32),
            Divider(color: Colors.grey.withOpacity(0.3)),
            const SizedBox(height: 16),
            ExpansionTile(
              tilePadding: EdgeInsets.zero,
              initiallyExpanded: false,
              title: Text(
                strings.advancedSectionTitle,
                style: theme.textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.w500,
                ),
              ),
              subtitle: Text(
                strings.advancedSectionSubtitle,
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.onSurface.withOpacity(0.7),
                ),
              ),
              children: [
                const SizedBox(height: 8),
                Text(
                  strings.screenshotQualityTitle,
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  strings.screenshotQualityValue(_screenshotQuality),
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.onSurface.withOpacity(0.6),
                  ),
                ),
                if (_screenshotQuality < 30) ...[
                  const SizedBox(height: 8),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(
                        Icons.warning_amber_rounded,
                        size: 18,
                        color: theme.colorScheme.error,
                      ),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          strings.screenshotQualityTooLowWarning,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.error,
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
                Slider(
                  min: 1,
                  max: 100,
                  divisions: 99,
                  value: _screenshotQuality.toDouble(),
                  label: _screenshotQuality.toString(),
                  onChanged: _onScreenshotQualityChanged,
                ),
                TextFormField(
                  controller: _screenshotQualityController,
                  decoration: InputDecoration(
                    labelText: strings.screenshotQualityInputLabel,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12.0),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12.0),
                      borderSide: BorderSide(color: theme.primaryColor),
                    ),
                  ),
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: false,
                    signed: false,
                  ),
                  inputFormatters: [
                    FilteringTextInputFormatter.digitsOnly,
                    LengthLimitingTextInputFormatter(3),
                  ],
                  onChanged: (value) {
                    final parsed = int.tryParse(value);
                    if (parsed == null) return;
                    final normalized = parsed.clamp(1, 100) as int;
                    if (normalized == _screenshotQuality) return;
                    setState(() {
                      _screenshotQuality = normalized;
                    });
                    _advancedDebounceTimer?.cancel();
                    _advancedDebounceTimer = Timer(
                      const Duration(milliseconds: 300),
                      () {
                        _saveAdvancedSettings();
                      },
                    );
                  },
                  onFieldSubmitted: _onScreenshotQualityTextSubmitted,
                  onEditingComplete: () {
                    _onScreenshotQualityTextSubmitted(
                      _screenshotQualityController.text,
                    );
                    FocusScope.of(context).unfocus();
                  },
                ),
                const SizedBox(height: 24),
                Text(
                  strings.screenshotResizeTitle,
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  strings.screenshotResizeSubtitle,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.onSurface.withOpacity(0.6),
                  ),
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(strings.screenshotResizeTitle),
                  value: _screenshotResizeEnabled,
                  onChanged: (enabled) {
                    setState(() {
                      _screenshotResizeEnabled = enabled;
                    });
                    _saveAdvancedSettings();
                  },
                ),
                if (_screenshotResizeEnabled) ...[
                  Text(
                    strings.screenshotResizeValue(_screenshotScalePercent),
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurface.withOpacity(0.6),
                    ),
                  ),
                  if (_screenshotScalePercent < 30) ...[
                    const SizedBox(height: 8),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(
                          Icons.warning_amber_rounded,
                          size: 18,
                          color: theme.colorScheme.error,
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            strings.screenshotResizeTooLowWarning,
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.error,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                  Slider(
                    min: 1,
                    max: 100,
                    divisions: 99,
                    value: _screenshotScalePercent.toDouble(),
                    label: _screenshotScalePercent.toString(),
                    onChanged: _onScreenshotScaleChanged,
                  ),
                  TextFormField(
                    controller: _screenshotScaleController,
                    decoration: InputDecoration(
                      labelText: strings.screenshotResizeInputLabel,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12.0),
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12.0),
                        borderSide: BorderSide(color: theme.primaryColor),
                      ),
                    ),
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: false,
                      signed: false,
                    ),
                    inputFormatters: [
                      FilteringTextInputFormatter.digitsOnly,
                      LengthLimitingTextInputFormatter(3),
                    ],
                    onChanged: (value) {
                      final parsed = int.tryParse(value);
                      if (parsed == null) return;
                      final normalized = parsed.clamp(1, 100) as int;
                      if (normalized == _screenshotScalePercent) return;
                      setState(() {
                        _screenshotScalePercent = normalized;
                      });
                      _advancedDebounceTimer?.cancel();
                      _advancedDebounceTimer = Timer(
                        const Duration(milliseconds: 300),
                        () {
                          _saveAdvancedSettings();
                        },
                      );
                    },
                    onFieldSubmitted: _onScreenshotScaleTextSubmitted,
                    onEditingComplete: () {
                      _onScreenshotScaleTextSubmitted(
                        _screenshotScaleController.text,
                      );
                      FocusScope.of(context).unfocus();
                    },
                  ),
                ],
              ],
            ),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}
