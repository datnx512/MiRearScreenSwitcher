import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'l10n/app_localizations.dart';
import 'screens/home_page.dart';
import 'services/theme_controller.dart';
import 'services/locale_controller.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Colors.transparent,
      systemNavigationBarIconBrightness: Brightness.light,
    ),
  );
  SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

  runApp(const DisplaySwitcherApp());
}

class DisplaySwitcherApp extends StatefulWidget {
  const DisplaySwitcherApp({super.key});

  @override
  State<DisplaySwitcherApp> createState() => _DisplaySwitcherAppState();
}

class _DisplaySwitcherAppState extends State<DisplaySwitcherApp> {
  final _themeController = ThemeController();
  final _localeController = LocaleController();

  @override
  void initState() {
    super.initState();
    _themeController.load();
    _localeController.load();
    _themeController.addListener(() {
      if (mounted) setState(() {});
    });
    _localeController.addListener(() {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _themeController.dispose();
    _localeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MRSS',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
        brightness: Brightness.light,
      ),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.blue,
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
        brightness: Brightness.dark,
      ),
      themeMode: _themeController.mode,
      locale: _localeController.locale,
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
      localeResolutionCallback: (locale, supportedLocales) {
        // Use LocaleController's choice if set
        final controlled = _localeController.locale;
        for (var sl in supportedLocales) {
          if (sl.languageCode == controlled.languageCode &&
              sl.countryCode == controlled.countryCode) {
            return sl;
          }
        }
        // Fallback to system locale
        if (locale != null) {
          for (var sl in supportedLocales) {
            if (sl.languageCode == locale.languageCode &&
                sl.countryCode == locale.countryCode) {
              return sl;
            }
          }
          if (locale.languageCode == 'zh') {
            if (locale.countryCode == 'TW' || locale.countryCode == 'HK') {
              return const Locale('zh', 'TW');
            }
            return const Locale('zh', 'CN');
          }
        }
        return const Locale('vi', '');
      },
      home: HomePage(
        themeController: _themeController,
        localeController: _localeController,
      ),
    );
  }
}
