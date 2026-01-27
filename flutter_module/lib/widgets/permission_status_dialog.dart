// lib/widgets/permission_status_dialog.dart

import 'dart:async';
import 'package:flutter/material.dart';

import '../l10n/app_strings.dart';
import '../services/platform_channel.dart';

/// A dialog that shows the status of required permissions.
///
/// This widget is stateful and will automatically update its UI every second
/// to reflect the current permission status. It will also automatically
/// pop the navigation stack (close itself) once all required permissions are granted.
/// Users can tap on a permission row to navigate to the relevant system settings page.
class PermissionStatusDialog extends StatefulWidget {
  final bool initialIsAccessibilityEnabled;
  final bool initialIsBatteryOptimizationDisabled;
  final VoidCallback onOpenAccessibilitySettings;
  final VoidCallback onOpenBatterySettings;

  const PermissionStatusDialog({
    super.key,
    required this.initialIsAccessibilityEnabled,
    required this.initialIsBatteryOptimizationDisabled,
    required this.onOpenAccessibilitySettings,
    required this.onOpenBatterySettings,
  });

  @override
  State<PermissionStatusDialog> createState() => _PermissionStatusDialogState();
}

class _PermissionStatusDialogState extends State<PermissionStatusDialog> {
  late bool _isAccessibilityEnabled;
  late bool _isBatteryOptimizationDisabled;
  Timer? _pollingTimer;

  @override
  void initState() {
    super.initState();
    _isAccessibilityEnabled = widget.initialIsAccessibilityEnabled;
    _isBatteryOptimizationDisabled =
        widget.initialIsBatteryOptimizationDisabled;

    _pollingTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      _checkAndUpdateStatus();
    });
  }

  @override
  void dispose() {
    _pollingTimer?.cancel();
    super.dispose();
  }

  Future<void> _checkAndUpdateStatus() async {
    if (!mounted) return;

    final newAccessibilityStatus = await platform.invokeMethod(
      'isAccessibilityServiceEnabled',
    );
    await Future.delayed(const Duration(milliseconds: 300));
    final newBatteryStatus = await platform.invokeMethod(
      'isIgnoringBatteryOptimizations',
    );

    if (newAccessibilityStatus && newBatteryStatus) {
      if (mounted) Navigator.of(context).pop();
      return;
    }

    if (newAccessibilityStatus != _isAccessibilityEnabled ||
        newBatteryStatus != _isBatteryOptimizationDisabled) {
      if (mounted) {
        setState(() {
          _isAccessibilityEnabled = newAccessibilityStatus;
          _isBatteryOptimizationDisabled = newBatteryStatus;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final strings = AppStringsScope.of(context);
    return AlertDialog(
      icon: Icon(
        Icons.shield_moon_outlined,
        color: Theme.of(context).primaryColor,
        size: 36,
      ),
      title: Text(strings.permissionDialogTitle),
      // Use contentPadding to remove default padding so InkWell can go to the edge.
      contentPadding: const EdgeInsets.fromLTRB(24.0, 20.0, 24.0, 0),
      // Set actions to null or empty to remove the default button area padding.
      actions: const [],
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              strings.permissionDialogDescription,
              style: const TextStyle(fontSize: 14),
            ),
            const SizedBox(height: 16),
            _buildPermissionRow(
              context: context,
              title: strings.accessibilityPermissionTitle,
              description: strings.accessibilityPermissionDescription,
              isGranted: _isAccessibilityEnabled,
              onPressed: widget.onOpenAccessibilitySettings,
            ),
            const SizedBox(height: 8),
            _buildPermissionRow(
              context: context,
              title: strings.batteryPermissionTitle,
              description: strings.batteryPermissionDescription,
              isGranted: _isBatteryOptimizationDisabled,
              onPressed: widget.onOpenBatterySettings,
            ),
            // Add padding at the bottom to match the original dialog spacing.
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  /// Builds a single, tappable row for a permission, showing its status.
  Widget _buildPermissionRow({
    required BuildContext context,
    required String title,
    required String description,
    required bool isGranted,
    required VoidCallback onPressed,
  }) {
    final theme = Theme.of(context);
    return Material(
      color: Colors.transparent,
      child: InkWell(
        // The row is only tappable if the permission is not yet granted.
        onTap: isGranted ? null : onPressed,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 12.0, horizontal: 8.0),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              AnimatedSwitcher(
                duration: const Duration(milliseconds: 300),
                transitionBuilder:
                    (child, animation) =>
                        ScaleTransition(scale: animation, child: child),
                child: Icon(
                  isGranted ? Icons.check_circle_rounded : Icons.cancel_rounded,
                  key: ValueKey<bool>(isGranted),
                  color:
                      isGranted ? Colors.green.shade600 : Colors.red.shade400,
                  size: 28,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                    Text(
                      description,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: Colors.grey.shade600,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
