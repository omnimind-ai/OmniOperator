import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

class TypewriterText extends StatefulWidget {
  final String text;
  final TextStyle style;
  final bool shouldAnimate;
  final VoidCallback? onCharacterTyped;
  final VoidCallback? onAnimationCompleted;

  const TypewriterText({
    super.key,
    required this.text,
    required this.style,
    required this.shouldAnimate,
    this.onCharacterTyped,
    this.onAnimationCompleted,
  });

  @override
  State<TypewriterText> createState() => _TypewriterTextState();
}

class _TypewriterTextState extends State<TypewriterText> {
  String _displayedText = "";
  Timer? _timer;
  bool _isAnimationComplete = false;

  @override
  void initState() {
    super.initState();
    _isAnimationComplete = !widget.shouldAnimate;
    if (widget.shouldAnimate) {
      _startTyping();
    } else {
      _displayedText = widget.text;
    }
  }

  void _startTyping() {
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(milliseconds: 15), (timer) {
      if (_displayedText.length < widget.text.length) {
        if (mounted) {
          setState(() {
            _displayedText = widget.text.substring(
              0,
              _displayedText.length + 1,
            );
            widget.onCharacterTyped?.call();
          });
        }
      } else {
        _timer?.cancel();
        if (mounted) {
          setState(() {
            _isAnimationComplete = true;
          });
        }
        widget.onAnimationCompleted?.call();
      }
    });
  }

  @override
  void didUpdateWidget(covariant TypewriterText oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.text != widget.text) {
      _timer?.cancel();
      if (widget.shouldAnimate) {
        setState(() {
          _displayedText = "";
          _isAnimationComplete = false;
        });
        _startTyping();
      } else {
        setState(() {
          _displayedText = widget.text;
          _isAnimationComplete = true;
        });
      }
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Use a non-breaking space to prevent widget collapse when text is empty.
    final textToDisplay = _displayedText.isEmpty ? '\u200B' : _displayedText;

    return MarkdownBody(
      data: textToDisplay,
      selectable: true,
      // The key change is here, in the styleSheet customization.
      styleSheet: MarkdownStyleSheet.fromTheme(Theme.of(context)).copyWith(
        // General text styles
        p: widget.style.copyWith(height: 1.5),
        h1: widget.style.copyWith(fontSize: 24, fontWeight: FontWeight.bold),
        h2: widget.style.copyWith(fontSize: 20, fontWeight: FontWeight.bold),
        // Code styles
        code: widget.style.copyWith(
          fontFamily: 'monospace',
          backgroundColor: Theme.of(context).colorScheme.surfaceVariant,
          color: Theme.of(context).colorScheme.onSurfaceVariant,
        ),
        codeblockDecoration: BoxDecoration(
          color: Theme.of(context).colorScheme.surfaceVariant,
          borderRadius: BorderRadius.circular(8),
        ),
        // Blockquote styles
        blockquoteDecoration: BoxDecoration(
          color: Colors.grey.withOpacity(0.1),
          border: Border(
            left: BorderSide(
              color: Theme.of(context).colorScheme.primary.withOpacity(0.5),
              width: 4,
            ),
          ),
        ),
        // *** TABLE STYLING FOR HORIZONTAL SCROLLING ***
        // This forces columns to size to their content, preventing wrapping.
        tableColumnWidth: const IntrinsicColumnWidth(),
        // Add some padding to table cells for better spacing.
        tableCellsPadding: const EdgeInsets.all(6.0),
      ),
    );
  }
}
