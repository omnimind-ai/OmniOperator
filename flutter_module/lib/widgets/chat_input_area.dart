// lib/widgets/chat_input_area.dart

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../l10n/app_strings.dart';

class ChatInputArea extends StatefulWidget {
  final TextEditingController controller;
  final FocusNode focusNode;
  final bool isProcessing;
  final bool isDevServerRunning;
  final Function(String) onSendMessage;
  final VoidCallback onStartDevServer;
  final VoidCallback onStopDevServer;

  const ChatInputArea({
    super.key,
    required this.controller,
    required this.focusNode,
    required this.isProcessing,
    required this.isDevServerRunning,
    required this.onSendMessage,
    required this.onStartDevServer,
    required this.onStopDevServer,
  });

  @override
  State<ChatInputArea> createState() => _ChatInputAreaState();
}

class _ChatInputAreaState extends State<ChatInputArea> {
  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onStateChanged);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onStateChanged);
    super.dispose();
  }

  void _onStateChanged() {
    if (mounted) {
      setState(() {});
    }
  }

  Widget _buildDevServerChip({required bool compactLabels}) {
    final strings = AppStringsScope.of(context);
    final String label = strings.devServerStopLabel(compact: compactLabels);
    if (widget.isDevServerRunning) {
      return InkWell(
        onTap:
            widget.isProcessing
                ? null
                : () {
                  HapticFeedback.lightImpact();
                  widget.onStopDevServer();
                },
        borderRadius: BorderRadius.circular(20),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          decoration: BoxDecoration(
            color: Colors.grey.shade100,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: Colors.grey.shade300, width: 1),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.stop_circle_outlined,
                size: 16,
                color: Colors.grey.shade700,
              ),
              const SizedBox(width: 6),
              Text(
                label,
                style: const TextStyle(
                  color: Colors.black87,
                  fontWeight: FontWeight.w500,
                  fontSize: 12,
                ),
              ),
            ],
          ),
        ),
      );
    }

    final String startLabel = strings.devServerStartLabel(compact: compactLabels);
    return InkWell(
      // Updated onTap to include haptic feedback
      onTap:
          widget.isProcessing
              ? null
              : () {
                HapticFeedback.lightImpact();
                widget.onStartDevServer();
              },
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: Colors.grey.shade100,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: Colors.grey.shade300, width: 1),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.rocket_launch_outlined,
              size: 16,
              color: Colors.grey.shade700,
            ),
            const SizedBox(width: 6),
            Text(
              startLabel,
              style: const TextStyle(
                color: Colors.black87,
                fontWeight: FontWeight.w500,
                fontSize: 12,
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final strings = AppStringsScope.of(context);
    final hasText = widget.controller.text.trim().isNotEmpty;
    final bool compactLabels =
        MediaQuery.of(context).size.width < 360 ||
        MediaQuery.of(context).textScaleFactor > 1.1;

    IconData icon;
    VoidCallback? onPressed;
    Color backgroundColor;
    String tooltip;

    // Updated logic to wrap callbacks with HapticFeedback
    if (widget.isDevServerRunning) {
      icon = Icons.lock_outline;
      onPressed = null;
      backgroundColor = Colors.grey.shade400;
      tooltip = strings.devServerDesktopOnly;
    } else if (widget.isProcessing) {
      icon = Icons.hourglass_empty_rounded;
      onPressed = null;
      backgroundColor = Colors.grey.shade400;
      tooltip = strings.processingTooltip;
    } else if (hasText) {
      icon = Icons.arrow_upward_rounded;
      onPressed = () {
        HapticFeedback.lightImpact();
        widget.onSendMessage(widget.controller.text);
      };
      backgroundColor = theme.primaryColor;
      tooltip = strings.sendTooltip;
    } else {
      icon = Icons.arrow_upward_rounded;
      onPressed = null;
      backgroundColor = Colors.grey.shade400;
      tooltip = strings.sendTooltip;
    }

    // This outer container now only provides padding and is transparent.
    return Container(
      padding: const EdgeInsets.fromLTRB(8.0, 8.0, 8.0, 12.0),
      child: SafeArea(
        child: Container(
          padding: const EdgeInsets.fromLTRB(16, 4, 8, 4),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(28.0),
            // The shadow gives the "floating" effect.
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.06),
                blurRadius: 10,
                offset: const Offset(0, 2),
              ),
            ],
            border: Border.all(color: Colors.grey.shade200),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Stack(
                children: [
                  TextField(
                    controller: widget.controller,
                    focusNode: widget.focusNode,
                    readOnly: widget.isProcessing || widget.isDevServerRunning,
                    keyboardType: TextInputType.multiline,
                    minLines: 1,
                    maxLines: 5,
                    textCapitalization: TextCapitalization.sentences,
                    style: const TextStyle(fontSize: 16.0, height: 1.5),
                    decoration: InputDecoration(
                      hintText: strings.tryHint,
                      hintStyle: TextStyle(
                        fontSize: 16.0,
                        color: Colors.grey.shade400,
                      ),
                      border: InputBorder.none,
                      contentPadding: const EdgeInsets.symmetric(vertical: 4),
                    ),
                  ),
                ],
              ),
              Row(
                children: [
                  Expanded(
                    child: SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: Row(
                        children: [
                          _buildDevServerChip(compactLabels: compactLabels),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton(
                    icon: Icon(
                      Icons.attach_file_rounded,
                      color: Colors.grey.shade600,
                    ),
                    onPressed: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text(strings.attachmentNotImplemented),
                        ),
                      );
                    },
                    tooltip: strings.attachFileTooltip,
                  ),
                  SizedBox(
                    width: 40,
                    height: 40,
                    child: IconButton(
                      tooltip: tooltip,
                      style: ButtonStyle(
                        backgroundColor: MaterialStateProperty.all(
                          backgroundColor,
                        ),
                        shape: MaterialStateProperty.all(const CircleBorder()),
                      ),
                      icon: AnimatedSwitcher(
                        duration: const Duration(milliseconds: 200),
                        transitionBuilder: (child, animation) {
                          return ScaleTransition(
                            scale: animation,
                            child: child,
                          );
                        },
                        child: Icon(
                          icon,
                          key: ValueKey<IconData>(icon),
                          color: Colors.white,
                          size: 20,
                        ),
                      ),
                      onPressed:
                          onPressed, // Use the updated onPressed callback
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
