// lib/widgets/ip_address_dialog.dart

import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../l10n/app_strings.dart';

class IpAddressDialog extends StatefulWidget {
  const IpAddressDialog({super.key});

  @override
  State<IpAddressDialog> createState() => _IpAddressDialogState();
}

class _IpAddressDialogState extends State<IpAddressDialog> {
  late final TextEditingController _dialogTextController;
  // This key must be consistent with the Settings page
  static const String _ipAddressKey = 'server_ip_address';

  @override
  void initState() {
    super.initState();
    _dialogTextController = TextEditingController();
  }

  Future<void> _saveAndSubmit() async {
    final ipAddress = _dialogTextController.text.trim();
    if (ipAddress.isNotEmpty) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_ipAddressKey, ipAddress);
    }
    if (mounted) {
      // Pop with the entered address so HomePage can use it
      Navigator.of(context).pop(ipAddress);
    }
  }

  @override
  void dispose() {
    _dialogTextController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final strings = AppStringsScope.of(context);
    return AlertDialog(
      icon: Icon(Icons.dns_rounded, color: theme.primaryColor, size: 28),
      title: Text(strings.connectAgentTitle),
      content: TextField(
        controller: _dialogTextController,
        autofocus: true,
        keyboardType: TextInputType.url,
        decoration: InputDecoration(
          labelText: strings.serverIpLabel,
          hintText: strings.serverIpDialogHint,
          prefixIcon: Icon(
            Icons.computer_rounded,
            color: theme.primaryColor.withOpacity(0.7),
          ),
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(12.0)),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12.0),
            borderSide: BorderSide(color: theme.primaryColor),
          ),
        ),
        onSubmitted: (_) => _saveAndSubmit(),
      ),
      actions: <Widget>[
        TextButton(
          child: Text(strings.connectAgentSkip),
          onPressed:
              () => Navigator.of(
                context,
              ).pop(''), // Pop with empty string if skipped
        ),
        FilledButton(
          onPressed: _saveAndSubmit,
          child: Text(strings.connectAgentAction),
        ),
      ],
    );
  }
}
