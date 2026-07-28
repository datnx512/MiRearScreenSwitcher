import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Controls app-wide theme (light/dark) with persistence.
class ThemeController extends ChangeNotifier {
  static const _key = 'theme_mode';
  ThemeMode _mode = ThemeMode.system;

  ThemeMode get mode => _mode;

  bool get isDark => _mode == ThemeMode.dark;
  bool get isLight => _mode == ThemeMode.light;

  /// Brand gradient for light mode
  static const List<Color> lightGradientColors = [
    Color(0xFFFF9D88),
    Color(0xFFFFB5C5),
    Color(0xFFE0B5DC),
    Color(0xFFA8C5E5),
  ];

  /// Brand gradient for dark mode — deeper, more saturated
  static const List<Color> darkGradientColors = [
    Color(0xFF5D3B2E),
    Color(0xFF6B3B4A),
    Color(0xFF5C4163),
    Color(0xFF2E4A6B),
  ];

  static const LinearGradient lightGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: lightGradientColors,
  );

  static const LinearGradient darkGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: darkGradientColors,
  );

  /// Get the appropriate gradient based on dark mode state
  LinearGradient get currentGradient => isDark ? darkGradient : lightGradient;

  /// Get text color based on theme
  Color get textColor => isDark ? const Color(0xDDFFFFFF) : Colors.black87;
  Color get subTextColor => isDark ? const Color(0x99FFFFFF) : Colors.black54;
  Color get hintTextColor => isDark ? const Color(0x61FFFFFF) : Colors.black38;
  Color get cardColor =>
      isDark ? Colors.white.withValues(alpha: 0.10) : Colors.white.withValues(alpha: 0.25);
  Color get borderColor =>
      isDark ? Colors.white.withValues(alpha: 0.30) : Colors.white.withValues(alpha: 0.50);

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getString(_key);
    if (saved == 'dark') {
      _mode = ThemeMode.dark;
    } else if (saved == 'light') {
      _mode = ThemeMode.light;
    } else {
      _mode = ThemeMode.system;
    }
    notifyListeners();
  }

  Future<void> toggle() async {
    _mode = isDark ? ThemeMode.light : ThemeMode.dark;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_key, _mode == ThemeMode.dark ? 'dark' : 'light');
  }

  Future<void> setMode(ThemeMode mode) async {
    _mode = mode;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    String value;
    switch (mode) {
      case ThemeMode.dark:
        value = 'dark';
        break;
      case ThemeMode.light:
        value = 'light';
        break;
      case ThemeMode.system:
        value = 'system';
        break;
    }
    await prefs.setString(_key, value);
  }
}
