import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:mrss/services/theme_controller.dart';

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  group('ThemeController', () {
    test('initial mode is ThemeMode.system', () {
      final controller = ThemeController();
      expect(controller.mode, ThemeMode.system);
    });

    test('isDark and isLight are false for system mode', () {
      final controller = ThemeController();
      expect(controller.isDark, isFalse);
      expect(controller.isLight, isFalse);
    });

    test('toggle() switches light → dark → light', () async {
      final controller = ThemeController();

      // system → dark (first toggle goes to dark since not currently dark)
      await controller.toggle();
      expect(controller.mode, ThemeMode.dark);
      expect(controller.isDark, isTrue);
      expect(controller.isLight, isFalse);

      // dark → light
      await controller.toggle();
      expect(controller.mode, ThemeMode.light);
      expect(controller.isLight, isTrue);
      expect(controller.isDark, isFalse);
    });

    test('setMode() sets the correct mode', () async {
      final controller = ThemeController();

      await controller.setMode(ThemeMode.dark);
      expect(controller.mode, ThemeMode.dark);
      expect(controller.isDark, isTrue);

      await controller.setMode(ThemeMode.light);
      expect(controller.mode, ThemeMode.light);
      expect(controller.isLight, isTrue);

      await controller.setMode(ThemeMode.system);
      expect(controller.mode, ThemeMode.system);
      expect(controller.isDark, isFalse);
      expect(controller.isLight, isFalse);
    });

    test('currentGradient returns darkGradient in dark mode', () {
      final controller = ThemeController();
      controller.setMode(ThemeMode.dark);
      expect(controller.currentGradient.colors, ThemeController.darkGradientColors);
    });

    test('currentGradient returns lightGradient in light mode', () {
      final controller = ThemeController();
      controller.setMode(ThemeMode.light);
      expect(controller.currentGradient.colors, ThemeController.lightGradientColors);
    });

    test('load() restores system mode when nothing is saved', () async {
      final controller = ThemeController();
      await controller.load();
      expect(controller.mode, ThemeMode.system);
    });

    test('load() restores dark mode when "dark" is saved', () async {
      SharedPreferences.setMockInitialValues({'theme_mode': 'dark'});
      final controller = ThemeController();
      await controller.load();
      expect(controller.mode, ThemeMode.dark);
      expect(controller.isDark, isTrue);
    });

    test('load() restores light mode when "light" is saved', () async {
      SharedPreferences.setMockInitialValues({'theme_mode': 'light'});
      final controller = ThemeController();
      await controller.load();
      expect(controller.mode, ThemeMode.light);
      expect(controller.isLight, isTrue);
    });

    test('toggle() persists the new mode', () async {
      final controller = ThemeController();
      await controller.toggle();

      final prefs = await SharedPreferences.getInstance();
      expect(prefs.getString('theme_mode'), 'dark');
    });

    test('setMode() persists the value', () async {
      final controller = ThemeController();
      await controller.setMode(ThemeMode.light);

      final prefs = await SharedPreferences.getInstance();
      expect(prefs.getString('theme_mode'), 'light');
    });

    test('notifies listeners on toggle()', () async {
      final controller = ThemeController();
      var notified = false;
      controller.addListener(() => notified = true);

      await controller.toggle();
      expect(notified, isTrue);
    });

    test('notifies listeners on setMode()', () async {
      final controller = ThemeController();
      var notified = false;
      controller.addListener(() => notified = true);

      await controller.setMode(ThemeMode.dark);
      expect(notified, isTrue);
    });
  });
}
