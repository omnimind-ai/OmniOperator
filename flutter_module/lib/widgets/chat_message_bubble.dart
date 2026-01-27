// lib/widgets/chat_message_bubble.dart

import 'dart:async'; // Import 'dart:async' for Timer
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/chat_models.dart';
import 'typewriter_text.dart';
import 'typing_indicator.dart';

// --- START OF MODIFICATION ---
// A new private stateful widget to handle the individual animation of each suggestion chip.
class _AnimatedSuggestionChip extends StatefulWidget {
  final Widget child;
  final Duration delay;

  const _AnimatedSuggestionChip({required this.child, required this.delay});

  @override
  State<_AnimatedSuggestionChip> createState() =>
      _AnimatedSuggestionChipState();
}

class _AnimatedSuggestionChipState extends State<_AnimatedSuggestionChip> {
  bool _isVisible = false;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    // Start a timer with the given delay to trigger the animation.
    _timer = Timer(widget.delay, () {
      if (mounted) {
        setState(() {
          _isVisible = true;
        });
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // These two widgets create the "fade in downwards" effect.
    return AnimatedOpacity(
      opacity: _isVisible ? 1.0 : 0.0,
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeOut,
      child: AnimatedSlide(
        offset: _isVisible ? Offset.zero : const Offset(0.0, 0.5),
        duration: const Duration(milliseconds: 400),
        curve: Curves.easeOutCubic,
        child: widget.child,
      ),
    );
  }
}
// --- END OF MODIFICATION ---

class ChatMessageBubble extends StatefulWidget {
  const ChatMessageBubble({
    super.key,
    required this.message,
    required this.shouldAnimate,
    required this.isLatestBotMessage,
    this.onSuggestionTapped,
    this.onCharacterTyped,
    this.onAnimationCompleted,
    this.onSuggestionsDisplayed,
  });

  final Message message;
  final bool shouldAnimate;
  final bool isLatestBotMessage;
  final Function(String)? onSuggestionTapped;
  final VoidCallback? onCharacterTyped;
  final VoidCallback? onAnimationCompleted;
  final VoidCallback? onSuggestionsDisplayed;

  @override
  State<ChatMessageBubble> createState() => _ChatMessageBubbleState();
}

class _ChatMessageBubbleState extends State<ChatMessageBubble> {
  late bool _isAnimationComplete;

  @override
  void initState() {
    super.initState();
    _isAnimationComplete = !widget.shouldAnimate;
  }

  @override
  void didUpdateWidget(covariant ChatMessageBubble oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.message.id != oldWidget.message.id) {
      setState(() {
        _isAnimationComplete = !widget.shouldAnimate;
      });
    }
  }

  // --- START OF MODIFICATION ---
  // This method now builds the staggered animation.
  Widget _buildSuggestions(BuildContext context) {
    if (!_isAnimationComplete ||
        !widget.isLatestBotMessage ||
        widget.message.suggestions == null ||
        widget.message.suggestions!.isEmpty) {
      return const SizedBox.shrink(key: ValueKey('suggestions_empty'));
    }

    final theme = Theme.of(context);

    return Padding(
      key: const ValueKey('suggestions_visible'),
      padding: const EdgeInsets.only(top: 12.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (widget.message.suggestionTitle != null &&
              widget.message.suggestionTitle!.isNotEmpty)
            // The title also gets a gentle animation.
            _AnimatedSuggestionChip(
              delay: Duration.zero,
              child: Padding(
                padding: const EdgeInsets.only(bottom: 8.0),
                child: Text(
                  widget.message.suggestionTitle!,
                  style: TextStyle(
                    color: theme.colorScheme.onSurface.withOpacity(0.7),
                    fontSize: 14,
                  ),
                ),
              ),
            ),
          Wrap(
            spacing: 8.0,
            runSpacing: 8.0,
            // We use .asMap().entries.map to get the index for the delay.
            children:
                widget.message.suggestions!.asMap().entries.map((entry) {
                  final int index = entry.key;
                  final Suggestion suggestion = entry.value;

                  final suggestionChip = GestureDetector(
                    onTap:
                        () => widget.onSuggestionTapped?.call(suggestion.text),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        vertical: 8.0,
                        horizontal: 12.0,
                      ),
                      decoration: BoxDecoration(
                        color: theme.colorScheme.surface,
                        borderRadius: BorderRadius.circular(16.0),
                      ),
                      child: Text(
                        suggestion.text,
                        style: TextStyle(
                          color: theme.colorScheme.onSurface,
                          fontSize: 14,
                        ),
                      ),
                    ),
                  );

                  // Each chip is wrapped in our animator widget with a staggered delay.
                  return _AnimatedSuggestionChip(
                    // The delay increases for each subsequent chip.
                    // The title animation adds an offset, so we start from index + 1.
                    delay: Duration(milliseconds: 100 * (index + 1)),
                    child: suggestionChip,
                  );
                }).toList(),
          ),
        ],
      ),
    );
  }
  // --- END OF MODIFICATION ---

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isUser = widget.message.sender == Sender.user;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Column(
        crossAxisAlignment:
            isUser ? CrossAxisAlignment.end : CrossAxisAlignment.start,
        children: [
          Container(
            decoration: BoxDecoration(
              color: isUser ? null : theme.colorScheme.surface,
              gradient:
                  isUser
                      ? LinearGradient(
                        colors: [theme.primaryColor, const Color(0xFF6B9FFF)],
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                      )
                      : null,
              borderRadius: BorderRadius.circular(18.0),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.07),
                  blurRadius: 10,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            constraints: BoxConstraints(
              maxWidth: MediaQuery.of(context).size.width * 0.85,
            ),
            padding: const EdgeInsets.symmetric(
              vertical: 12.0,
              horizontal: 16.0,
            ),
            child:
                widget.message.isLoading
                    ? const TypingIndicator()
                    : Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        TypewriterText(
                          text: widget.message.text,
                          style: TextStyle(
                            color:
                                isUser
                                    ? theme.colorScheme.onPrimary
                                    : theme.colorScheme.onSurface,
                          ),
                          shouldAnimate: widget.shouldAnimate,
                          onCharacterTyped: widget.onCharacterTyped,
                          onAnimationCompleted: () {
                            if (mounted) {
                              setState(() {
                                _isAnimationComplete = true;
                              });
                            }
                            widget.onAnimationCompleted?.call();
                            if (widget.message.suggestions != null &&
                                widget.message.suggestions!.isNotEmpty) {
                              widget.onSuggestionsDisplayed?.call();
                            }
                          },
                        ),
                        if (widget.message.actionText != null)
                          Padding(
                            padding: const EdgeInsets.only(top: 10.0),
                            child: TextButton(
                              style: TextButton.styleFrom(
                                backgroundColor: theme.colorScheme.primary
                                    .withOpacity(0.1),
                                foregroundColor: theme.colorScheme.primary,
                                disabledBackgroundColor: Colors.grey
                                    .withOpacity(0.1),
                                disabledForegroundColor: Colors.grey
                                    .withOpacity(0.5),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(8.0),
                                ),
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 12,
                                  vertical: 8,
                                ),
                              ),
                              onPressed: widget.message.onAction,
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  const Icon(
                                    Icons.touch_app_outlined,
                                    size: 16,
                                  ),
                                  const SizedBox(width: 6),
                                  Text(
                                    widget.message.actionText!,
                                    style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                      ],
                    ),
          ),
          // This AnimatedSwitcher now primarily handles the exit animation.
          // The entrance animation is handled by the children themselves.
          AnimatedSwitcher(
            duration: const Duration(milliseconds: 350),
            switchInCurve: Curves.easeOutCubic,
            switchOutCurve: Curves.easeInCubic,
            transitionBuilder: (Widget child, Animation<double> animation) {
              final isExiting =
                  child.key == const ValueKey('suggestions_empty');

              // Apply slide animation only on exit
              final slideAnimation = Tween<Offset>(
                begin: isExiting ? Offset.zero : const Offset(0.0, -0.2),
                end: isExiting ? const Offset(0.0, -0.5) : Offset.zero,
              ).animate(animation);

              return FadeTransition(
                opacity: animation,
                child: SlideTransition(position: slideAnimation, child: child),
              );
            },
            child: _buildSuggestions(context),
          ),
          const SizedBox(height: 6),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8.0),
            child: Text(
              DateFormat.Hm().format(widget.message.timestamp),
              style: TextStyle(color: Colors.grey.shade500, fontSize: 11.0),
            ),
          ),
        ],
      ),
    );
  }
}
