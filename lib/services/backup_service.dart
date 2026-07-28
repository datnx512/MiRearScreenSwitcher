import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/app_profile.dart';

/// Backup and restore app settings to/from JSON.
class BackupService {
  /// All settings keys that should be backed up
  static const _settingsKeys = [
    'proximity_sensor_enabled',
    'charging_animation_enabled',
    'charging_always_on_enabled',
    'keep_screen_on_enabled',
    'always_wakeup_enabled',
    'notification_service_enabled',
    'notification_privacy_hide_title',
    'notification_privacy_hide_content',
    'notification_follow_dnd_mode',
    'notification_only_when_locked',
    'notification_dark_mode',
    'notification_duration',
    'theme_mode',
    'app_locale',
    'onboarding_completed',
  ];

  /// Export all settings to JSON string
  static Future<String> exportSettings() async {
    final prefs = await SharedPreferences.getInstance();
    final data = <String, dynamic>{};

    for (final key in _settingsKeys) {
      final value = prefs.get(key);
      if (value != null) {
        data[key] = value;
      }
    }

    // Also export selected notification apps
    // (handled via platform channel, not SharedPreferences)

    final json = const JsonEncoder.withIndent('  ').convert(data);
    return json;
  }

  /// Import settings from JSON string
  static Future<bool> importSettings(String jsonString) async {
    try {
      final data = jsonDecode(jsonString) as Map<String, dynamic>;
      final prefs = await SharedPreferences.getInstance();

      for (final key in _settingsKeys) {
        if (data.containsKey(key)) {
          final value = data[key];
          if (value is bool) {
            await prefs.setBool(key, value);
          } else if (value is int) {
            await prefs.setInt(key, value);
          } else if (value is String) {
            await prefs.setString(key, value);
          }
        }
      }

      return true;
    } catch (e) {
      return false;
    }
  }

  /// Export a profile to JSON
  static String exportProfile(AppProfile profile) {
    return const JsonEncoder.withIndent('  ').convert(profile.toJson());
  }

  /// Import a profile from JSON
  static AppProfile? importProfile(String jsonString) {
    try {
      final data = jsonDecode(jsonString) as Map<String, dynamic>;
      return AppProfile.fromJson(data);
    } catch (e) {
      return null;
    }
  }
}
