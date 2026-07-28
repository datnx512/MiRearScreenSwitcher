import 'package:flutter_test/flutter_test.dart';
import 'package:mrss/models/app_profile.dart';

void main() {
  group('AppProfile', () {
    test('fromJson() parses valid JSON with all fields', () {
      final json = <String, dynamic>{
        'name': 'Custom',
        'icon': '🎮',
        'dpi': 280,
        'rotation': 2,
        'proximitySensor': false,
        'keepScreenOn': false,
        'alwaysWakeUp': true,
        'chargingAnimation': false,
        'chargingAlwaysOn': true,
      };

      final profile = AppProfile.fromJson(json);

      expect(profile.name, 'Custom');
      expect(profile.icon, '🎮');
      expect(profile.dpi, 280);
      expect(profile.rotation, 2);
      expect(profile.proximitySensor, isFalse);
      expect(profile.keepScreenOn, isFalse);
      expect(profile.alwaysWakeUp, isTrue);
      expect(profile.chargingAnimation, isFalse);
      expect(profile.chargingAlwaysOn, isTrue);
    });

    test('fromJson() applies defaults for missing optional fields', () {
      final profile = AppProfile.fromJson({'name': 'Minimal'});

      expect(profile.name, 'Minimal');
      expect(profile.icon, '📱');
      expect(profile.dpi, 320);
      expect(profile.rotation, 0);
      expect(profile.proximitySensor, isTrue);
      expect(profile.keepScreenOn, isTrue);
      expect(profile.alwaysWakeUp, isFalse);
      expect(profile.chargingAnimation, isTrue);
      expect(profile.chargingAlwaysOn, isFalse);
    });

    test('toJson() round-trips through fromJson()', () {
      final original = AppProfile(
        name: 'RoundTrip',
        icon: '🚀',
        dpi: 300,
        rotation: 3,
        proximitySensor: false,
        keepScreenOn: true,
        alwaysWakeUp: true,
        chargingAnimation: false,
        chargingAlwaysOn: true,
      );

      final json = original.toJson();
      final restored = AppProfile.fromJson(json);

      expect(restored.name, original.name);
      expect(restored.icon, original.icon);
      expect(restored.dpi, original.dpi);
      expect(restored.rotation, original.rotation);
      expect(restored.proximitySensor, original.proximitySensor);
      expect(restored.keepScreenOn, original.keepScreenOn);
      expect(restored.alwaysWakeUp, original.alwaysWakeUp);
      expect(restored.chargingAnimation, original.chargingAnimation);
      expect(restored.chargingAlwaysOn, original.chargingAlwaysOn);
    });

    test('toJson() contains all expected keys', () {
      final profile = AppProfile(
        name: 'Keys',
        icon: '🔑',
        dpi: 320,
        rotation: 0,
        proximitySensor: true,
        keepScreenOn: true,
        alwaysWakeUp: false,
        chargingAnimation: true,
        chargingAlwaysOn: false,
      );
      final json = profile.toJson();

      expect(json.keys, containsAll(<String>[
        'name',
        'icon',
        'dpi',
        'rotation',
        'proximitySensor',
        'keepScreenOn',
        'alwaysWakeUp',
        'chargingAnimation',
        'chargingAlwaysOn',
      ]));
    });

    group('presets', () {
      test('has exactly 4 presets', () {
        expect(AppProfile.presets.length, 4);
      });

      test('preset names match the expected Vietnamese labels', () {
        final names = AppProfile.presets.map((p) => p.name).toList();
        expect(names, ['Mặc định', 'Đọc sách', 'Xem video', 'Tiết kiệm pin']);
      });

      test('default preset has expected values', () {
        final preset = AppProfile.presets[0];
        expect(preset.name, 'Mặc định');
        expect(preset.icon, '📱');
        expect(preset.dpi, 320);
        expect(preset.rotation, 0);
        expect(preset.proximitySensor, isTrue);
        expect(preset.keepScreenOn, isTrue);
        expect(preset.alwaysWakeUp, isFalse);
        expect(preset.chargingAnimation, isTrue);
        expect(preset.chargingAlwaysOn, isFalse);
      });

      test('reading preset has lower DPI and no charging animation', () {
        final preset = AppProfile.presets[1];
        expect(preset.name, 'Đọc sách');
        expect(preset.icon, '📖');
        expect(preset.dpi, 280);
        expect(preset.chargingAnimation, isFalse);
      });

      test('video preset has 90° rotation and proximity off', () {
        final preset = AppProfile.presets[2];
        expect(preset.name, 'Xem video');
        expect(preset.icon, '🎬');
        expect(preset.rotation, 1);
        expect(preset.proximitySensor, isFalse);
        expect(preset.keepScreenOn, isTrue);
      });

      test('battery-saver preset has lowest DPI and keep-screen off', () {
        final preset = AppProfile.presets[3];
        expect(preset.name, 'Tiết kiệm pin');
        expect(preset.icon, '🔋');
        expect(preset.dpi, 260);
        expect(preset.keepScreenOn, isFalse);
        expect(preset.chargingAnimation, isFalse);
      });
    });
  });
}
