import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../l10n/app_strings.dart';

class AppLanguageController extends ChangeNotifier {
  static const String _prefKey = 'app_language';

  AppLanguageController(this._language);

  AppLanguage _language;

  AppLanguage get language => _language;

  Future<void> setLanguage(AppLanguage language) async {
    if (_language == language) return;
    _language = language;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_prefKey, language.code);
  }

  static Future<AppLanguageController> load() async {
    final prefs = await SharedPreferences.getInstance();
    final code = prefs.getString(_prefKey);
    final language = code == AppLanguage.en.code ? AppLanguage.en : AppLanguage.zh;
    return AppLanguageController(language);
  }
}
