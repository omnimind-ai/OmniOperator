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
  // Use a consistent key across the app.
  static const String _serverIpKey = 'server_ip_address';
  bool _isLoading = false;
  late bool _isCompanionModeEnabled;
  late AppLanguage _selectedLanguage;

  @override
  void initState() {
    super.initState();
    _serverIpController = TextEditingController();
    _loadServerAddress();
    _isCompanionModeEnabled = widget.isCompanionModeEnabled;
    _selectedLanguage = widget.languageController.language;
  }

  @override
  void dispose() {
    _serverIpController.dispose();
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
              label: Text(_isLoading ? strings.savingButton : strings.saveButton),
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
