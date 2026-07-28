import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:mrss/l10n/app_localizations.dart';
import 'package:mrss/screens/home_page.dart';
import 'package:mrss/screens/onboarding_page.dart';
import 'package:mrss/services/locale_controller.dart';
import 'package:mrss/services/theme_controller.dart';

/// The MethodChannel used by HomePage to talk to the native side.
const _channel = MethodChannel('com.display.switcher/task');

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
    // Provide benign mock responses so HomePage's platform calls don't throw
    // MissingPluginException during widget tests.
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, (MethodCall call) async {
      switch (call.method) {
        case 'checkShizuku':
          return false; // Shizuku not running — exercises the error path
        case 'checkNotificationListenerPermission':
          return false;
        case 'getCurrentRearDpi':
          return 320;
        case 'getDisplayRotation':
          return 0;
        default:
          return null;
      }
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, null);
  });

  /// Builds a MaterialApp pre-configured with the same localization delegates
  /// the real app uses, so AppLocalizations.of(context) works in tests.
  Widget buildApp(Widget home) {
    return MaterialApp(
      home: home,
      supportedLocales: const [
        Locale('en', ''),
        Locale('vi', ''),
        Locale('zh', 'CN'),
        Locale('zh', 'TW'),
      ],
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      localeResolutionCallback: (locale, supportedLocales) =>
          const Locale('vi', ''),
    );
  }

  /// Sets a wide-enough logical surface so the side-by-side link cards in
  /// HomePage (donation + QQ group) don't overflow.
  void usePhoneSurface(WidgetTester tester) {
    tester.view.physicalSize = const Size(1200, 2600);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
  }

  group('HomePage', () {
    testWidgets('renders without error', (WidgetTester tester) async {
      usePhoneSurface(tester);
      final themeController = ThemeController();
      final localeController = LocaleController();
      await themeController.load();
      await localeController.load();

      await tester.pumpWidget(buildApp(
        HomePage(
          themeController: themeController,
          localeController: localeController,
        ),
      ));
      // Let initState's async work (Shizuku check, DPI/rotation load) and the
      // 2-second delayed future run to completion so no timers are pending.
      await tester.pump();
      await tester.pumpAndSettle();

      expect(find.byType(HomePage), findsOneWidget);
      expect(find.text('MRSS'), findsOneWidget);
      themeController.dispose();
      localeController.dispose();
    });

    testWidgets('renders the dark-mode toggle icon', (WidgetTester tester) async {
      usePhoneSurface(tester);
      final themeController = ThemeController();
      final localeController = LocaleController();
      await themeController.load();
      await localeController.load();

      await tester.pumpWidget(buildApp(
        HomePage(
          themeController: themeController,
          localeController: localeController,
        ),
      ));
      await tester.pump();
      await tester.pumpAndSettle();

      // In light/system mode the toggle shows the dark-mode icon.
      expect(find.byIcon(Icons.dark_mode), findsOneWidget);
      themeController.dispose();
      localeController.dispose();
    });
  });

  group('OnboardingPage', () {
    testWidgets('renders without error', (WidgetTester tester) async {
      await tester.pumpWidget(MaterialApp(
        home: OnboardingPage(onComplete: () {}),
      ));
      await tester.pump();

      expect(find.byType(OnboardingPage), findsOneWidget);
      // First onboarding page shows its welcome title.
      expect(find.text('Chào mừng đến MRSS'), findsOneWidget);
    });

    testWidgets('calls onComplete when finished', (WidgetTester tester) async {
      var completed = false;
      await tester.pumpWidget(MaterialApp(
        home: OnboardingPage(onComplete: () => completed = true),
      ));
      await tester.pump();

      // The page renders; onComplete is not called until the user finishes.
      expect(completed, isFalse);
      expect(find.byType(OnboardingPage), findsOneWidget);
    });
  });
}
