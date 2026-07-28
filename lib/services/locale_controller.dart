import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Manages app locale with persistence.
class LocaleController extends ChangeNotifier {
  static const _key = 'app_locale';
  Locale _locale = const Locale('vi', '');

  Locale get locale => _locale;

  /// All supported locales with display names
  static const Map<String, String> supportedLocales = {
    'vi': 'Tiếng Việt',
    'en': 'English',
    'zh_CN': '简体中文',
    'zh_TW': '繁體中文',
  };

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getString(_key);
    if (saved != null) {
      if (saved == 'en') {
        _locale = const Locale('en', '');
      } else if (saved == 'vi') {
        _locale = const Locale('vi', '');
      } else if (saved == 'zh_CN') {
        _locale = const Locale('zh', 'CN');
      } else if (saved == 'zh_TW') {
        _locale = const Locale('zh', 'TW');
      }
    }
    notifyListeners();
  }

  Future<void> setLocale(String code) async {
    switch (code) {
      case 'en':
        _locale = const Locale('en', '');
        break;
      case 'vi':
        _locale = const Locale('vi', '');
        break;
      case 'zh_CN':
        _locale = const Locale('zh', 'CN');
        break;
      case 'zh_TW':
        _locale = const Locale('zh', 'TW');
        break;
    }
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_key, code);
  }

  /// Get the current locale code for display
  String get currentCode {
    if (_locale.languageCode == 'zh') {
      if (_locale.countryCode == 'TW') return 'zh_TW';
      return 'zh_CN';
    }
    return _locale.languageCode;
  }

  /// Get display name for current locale
  String get currentName =>
      supportedLocales[currentCode] ?? 'Tiếng Việt';
}
