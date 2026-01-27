// lib/models/chat_models.dart

import 'package:flutter/foundation.dart';

// --- Data Models for the Chat UI ---
enum Sender { user, bot }

class Message {
  final String id;
  final String text;
  final Sender sender;
  final bool isLoading;
  final DateTime timestamp;
  final String? actionText;
  final VoidCallback? onAction;
  final String? suggestionTitle; // <-- MODIFICATION: Add suggestion title
  final List<Suggestion>? suggestions; // <-- MODIFICATION: Add suggestions list

  Message({
    required this.text,
    required this.sender,
    this.isLoading = false,
    this.actionText,
    this.onAction,
    this.suggestionTitle, // <-- MODIFICATION: Add to constructor
    this.suggestions, // <-- MODIFICATION: Add to constructor
    String? id,
  }) : id =
           id ??
           '${DateTime.now().millisecondsSinceEpoch}-${text.hashCode}', // <-- NO CHANGE HERE, JUST FOR CONTEXT
       timestamp = DateTime.now();
}

// <-- MODIFICATION: Redefined Suggestion model for chat suggestions
class Suggestion {
  final String text;

  Suggestion({required this.text});
}
