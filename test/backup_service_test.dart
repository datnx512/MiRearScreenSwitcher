import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:mrss/models/app_profile.dart';
import 'package:mrss/services/backup_service.dart';

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  group('BackupService', () {
    test('exportSettings() returns a valid JSON string', () async {
      final json = await BackupService.exportSettings();

      // Should be parseable as a Map.
      final data = jsonDecode(json) as Map<String, dynamic>;
      expect(data, isA<Map<String, dynamic>>());
    });

    test('exportSettings() includes saved values', () async {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('proximity_sensor_enabled', false);
      await prefs.setInt('notification_duration', 5);
      await prefs.setString('theme_mode', 'dark');

      final json = await BackupService.exportSettings();
      final data = jsonDecode(json) as Map<String, dynamic>;

      expect(data['proximity_sensor_enabled'], isFalse);
      expect(data['notification_duration'], 5);
      expect(data['theme_mode'], 'dark');
    });

    test('exportSettings() omits unset keys', () async {
      final json = await BackupService.exportSettings();
      final data = jsonDecode(json) as Map<String, dynamic>;

      // With empty SharedPreferences, no settings keys should be present.
      expect(data.isEmpty, isTrue);
    });

    test('importSettings() returns true for valid JSON', () async {
      const jsonString = '{"theme_mode": "dark", "app_locale": "en"}';
      final result = await BackupService.importSettings(jsonString);
      expect(result, isTrue);
    });

    test('importSettings() writes values to SharedPreferences', () async {
      const jsonString = '{"theme_mode": "dark", "notification_duration": 7, "keep_screen_on_enabled": false}';
      await BackupService.importSettings(jsonString);

      final prefs = await SharedPreferences.getInstance();
      expect(prefs.getString('theme_mode'), 'dark');
      expect(prefs.getInt('notification_duration'), 7);
      expect(prefs.getBool('keep_screen_on_enabled'), isFalse);
    });

    test('importSettings() returns false for invalid JSON', () async {
      final result = await BackupService.importSettings('not valid json {{{');
      expect(result, isFalse);
    });

    test('importSettings() returns false for JSON that is not a map', () async {
      final result = await BackupService.importSettings('[1, 2, 3]');
      expect(result, isFalse);
    });

    test('round-trip: export → import preserves values', () async {
      // Seed SharedPreferences with values across all supported types.
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('proximity_sensor_enabled', false);
      await prefs.setBool('keep_screen_on_enabled', true);
      await prefs.setInt('notification_duration', 10);
      await prefs.setString('theme_mode', 'light');
      await prefs.setString('app_locale', 'zh_CN');
      await prefs.setBool('onboarding_completed', true);

      // Export from the original prefs.
      final exported = await BackupService.exportSettings();

      // Wipe and re-import into a fresh SharedPreferences store.
      SharedPreferences.setMockInitialValues({});
      final importResult = await BackupService.importSettings(exported);
      expect(importResult, isTrue);

      final restored = await SharedPreferences.getInstance();
      expect(restored.getBool('proximity_sensor_enabled'), isFalse);
      expect(restored.getBool('keep_screen_on_enabled'), isTrue);
      expect(restored.getInt('notification_duration'), 10);
      expect(restored.getString('theme_mode'), 'light');
      expect(restored.getString('app_locale'), 'zh_CN');
      expect(restored.getBool('onboarding_completed'), isTrue);
    });

    test('round-trip: re-exporting after import yields identical JSON', () async {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('theme_mode', 'dark');
      await prefs.setInt('notification_duration', 3);

      final exported = await BackupService.exportSettings();

      SharedPreferences.setMockInitialValues({});
      await BackupService.importSettings(exported);
      final reExported = await BackupService.exportSettings();

      expect(reExported, exported);
    });
  });

  group('BackupService profile helpers', () {
    test('exportProfile() returns valid JSON of the profile', () {
      final profile = AppProfile.presets.first;
      final json = BackupService.exportProfile(profile);
      final data = jsonDecode(json) as Map<String, dynamic>;

      expect(data['name'], profile.name);
      expect(data['icon'], profile.icon);
      expect(data['dpi'], profile.dpi);
    });

    test('importProfile() returns a profile for valid JSON', () {
      final original = AppProfile.presets[1];
      final json = BackupService.exportProfile(original);
      final restored = BackupService.importProfile(json);

      expect(restored, isNotNull);
      expect(restored!.name, original.name);
      expect(restored.dpi, original.dpi);
      expect(restored.rotation, original.rotation);
    });

    test('importProfile() returns null for invalid JSON', () {
      final result = BackupService.importProfile('not json');
      expect(result, isNull);
    });
  });
}
