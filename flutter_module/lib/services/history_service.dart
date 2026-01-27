// lib/services/history_service.dart

import 'package:shared_preferences/shared_preferences.dart';

const String _kHistoryKey = 'user_message_history';
const int _kMaxHistoryCount = 3;

/// Saves a user message to the persistent history.
///
/// This function adds the new message to the top of the history,
/// ensures no duplicates are present, and trims the list to the
/// maximum size of [_kMaxHistoryCount].
Future<void> saveMessageToHistory(String message) async {
  if (message.trim().isEmpty) return;

  final prefs = await SharedPreferences.getInstance();
  List<String> history = prefs.getStringList(_kHistoryKey) ?? [];

  // Remove the message if it already exists to avoid duplicates and move it to the top.
  history.remove(message);

  // Add the new message to the beginning of the list.
  history.insert(0, message);

  // Trim the list to the maximum allowed size.
  if (history.length > _kMaxHistoryCount) {
    history = history.sublist(0, _kMaxHistoryCount);
  }

  await prefs.setStringList(_kHistoryKey, history);
}

/// Retrieves the list of recent user messages from persistent storage.
Future<List<String>> getHistoryMessages() async {
  final prefs = await SharedPreferences.getInstance();
  return prefs.getStringList(_kHistoryKey) ?? [];
}
