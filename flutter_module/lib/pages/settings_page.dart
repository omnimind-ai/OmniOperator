// lib/pages/settings_page.dart

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
  // Use a consistent key across the app.
  static const String _serverIpKey = 'server_ip_address';
  static const String _socketAuthEnabledKey = 'socket_auth_enabled';
  static const String _socketAuthTokenKey = 'socket_auth_token';
  static const String _devServerAuthEnabledKey = 'dev_server_auth_enabled';
  static const String _devServerApiKeyKey = 'dev_server_api_key';
  bool _isLoading = false;
  late bool _isCompanionModeEnabled;
  late AppLanguage _selectedLanguage;
  bool _socketAuthEnabled = false;
  bool _devServerAuthEnabled = false;
  bool _obscureSocketToken = true;
  bool _obscureDevServerKey = true;

  @override
  void initState() {
    super.initState();
    _serverIpController = TextEditingController();
    _socketAuthTokenController = TextEditingController();
    _devServerApiKeyController = TextEditingController();
    _loadServerAddress();
    _loadAuthSettings();
    _isCompanionModeEnabled = widget.isCompanionModeEnabled;
    _selectedLanguage = widget.languageController.language;
  }

  @override
  void dispose() {
    _serverIpController.dispose();
    _socketAuthTokenController.dispose();
    _devServerApiKeyController.dispose();
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
    final strings = AppStringsScope.of(context);
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

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(strings.authSettingsSaved),
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  // --- MODIFIED METHOD ---
  // This method is updated to fix the loading state and remove SnackBars.
  Future<void> _saveServerAddress() async {
    final strings = AppStringsScope.of(context);
    if (!(_formKey.currentState?.validate() ?? false)) {
      return; // Exit if form is not valid
    }

    setState(() => _isLoading = true);

    final ipAddress = _serverIpController.text.trim();

    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_serverIpKey, ipAddress);

      await platform.invokeMethod('sendServerAddress', {
        'serverAddress': ipAddress,
      });

      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(strings.serverAddressSaved(ipAddress)),
            duration: const Duration(seconds: 2),
          ),
        );
      }
    } on PlatformException catch (e) {
      debugPrint("Error saving server address: ${e.message}");

      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
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
              onChanged: _isLoading ? null : _toggleCompanionMode,
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
              onSelectionChanged:
                  _isLoading
                      ? null
                      : (selection) {
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
              enabled: !_isLoading,
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
            FilledButton.icon(
              onPressed: _isLoading ? null : _saveServerAddress,
              icon:
                  _isLoading
                      ? Container(
                        width: 20,
                        height: 20,
                        padding: const EdgeInsets.all(2.0),
                        child: const CircularProgressIndicator(
                          color: Colors.white,
                          strokeWidth: 3,
                        ),
                      )
                      : const Icon(Icons.save_rounded),
              label: Text(
                _isLoading ? strings.savingButton : strings.saveButton,
              ),
              style: FilledButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                textStyle: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
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
              onChanged:
                  _isLoading
                      ? null
                      : (val) => setState(() => _socketAuthEnabled = val),
            ),
            if (_socketAuthEnabled)
              TextFormField(
                controller: _socketAuthTokenController,
                enabled: !_isLoading,
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
              onChanged:
                  _isLoading
                      ? null
                      : (val) => setState(() => _devServerAuthEnabled = val),
            ),
            if (_devServerAuthEnabled)
              TextFormField(
                controller: _devServerApiKeyController,
                enabled: !_isLoading,
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
            const SizedBox(height: 24),
            FilledButton.icon(
              onPressed: _isLoading ? null : _saveAuthSettings,
              icon: const Icon(Icons.shield_rounded),
              label: Text(strings.saveButton),
              style: FilledButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                textStyle: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
