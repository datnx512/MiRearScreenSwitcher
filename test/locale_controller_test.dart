import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:mrss/services/locale_controller.dart';

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  group('LocaleController', () {
    test('initial locale is Vietnamese', () {
      final controller = LocaleController();
      expect(controller.locale.languageCode, 'vi');
      expect(controller.currentCode, 'vi');
    });

    test('setLocale("en") changes to English', () async {
      final controller = LocaleController();
      await controller.setLocale('en');

      expect(controller.locale.languageCode, 'en');
      expect(controller.currentCode, 'en');
    });

    test('setLocale("zh_CN") changes to Chinese Simplified', () async {
      final controller = LocaleController();
      await controller.setLocale('zh_CN');

      expect(controller.locale.languageCode, 'zh');
      expect(controller.locale.countryCode, 'CN');
      expect(controller.currentCode, 'zh_CN');
    });

    test('setLocale("zh_TW") changes to Chinese Traditional', () async {
      final controller = LocaleController();
      await controller.setLocale('zh_TW');

      expect(controller.locale.languageCode, 'zh');
      expect(controller.locale.countryCode, 'TW');
      expect(controller.currentCode, 'zh_TW');
    });

    test('currentCode returns correct string for each locale', () async {
      final controller = LocaleController();

      expect(controller.currentCode, 'vi');

      await controller.setLocale('en');
      expect(controller.currentCode, 'en');

      await controller.setLocale('zh_CN');
      expect(controller.currentCode, 'zh_CN');

      await controller.setLocale('zh_TW');
      expect(controller.currentCode, 'zh_TW');

      await controller.setLocale('vi');
      expect(controller.currentCode, 'vi');
    });

    test('currentName returns display name for each locale', () async {
      final controller = LocaleController();

      expect(controller.currentName, 'Tiếng Việt');

      await controller.setLocale('en');
      expect(controller.currentName, 'English');

      await controller.setLocale('zh_CN');
      expect(controller.currentName, '简体中文');

      await controller.setLocale('zh_TW');
      expect(controller.currentName, '繁體中文');
    });

    test('load() restores Vietnamese when nothing is saved', () async {
      final controller = LocaleController();
      await controller.load();
      expect(controller.locale.languageCode, 'vi');
    });

    test('load() restores saved locale', () async {
      SharedPreferences.setMockInitialValues({'app_locale': 'en'});
      final controller = LocaleController();
      await controller.load();
      expect(controller.locale.languageCode, 'en');
      expect(controller.currentCode, 'en');
    });

    test('load() restores saved Chinese Simplified', () async {
      SharedPreferences.setMockInitialValues({'app_locale': 'zh_CN'});
      final controller = LocaleController();
      await controller.load();
      expect(controller.locale.languageCode, 'zh');
      expect(controller.locale.countryCode, 'CN');
      expect(controller.currentCode, 'zh_CN');
    });

    test('setLocale() persists the code', () async {
      final controller = LocaleController();
      await controller.setLocale('zh_TW');

      final prefs = await SharedPreferences.getInstance();
      expect(prefs.getString('app_locale'), 'zh_TW');
    });

    test('notifies listeners on setLocale()', () async {
      final controller = LocaleController();
      var notified = false;
      controller.addListener(() => notified = true);

      await controller.setLocale('en');
      expect(notified, isTrue);
    });

    test('supportedLocales contains all 4 locales', () {
      expect(LocaleController.supportedLocales.length, 4);
      expect(LocaleController.supportedLocales.keys, contains('vi'));
      expect(LocaleController.supportedLocales.keys, contains('en'));
      expect(LocaleController.supportedLocales.keys, contains('zh_CN'));
      expect(LocaleController.supportedLocales.keys, contains('zh_TW'));
    });
  });
}
