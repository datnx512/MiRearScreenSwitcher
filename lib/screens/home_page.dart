import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:ui';
import 'package:shared_preferences/shared_preferences.dart';
import '../l10n/app_localizations.dart';
import '../services/theme_controller.dart';
import '../services/locale_controller.dart';
import '../widgets/squircle.dart';
import '../widgets/gradient_widgets.dart';
import 'app_selection_page.dart';
import '../models/app_profile.dart';

enum ShizukuStatus { checking, running, error }

/// Main home page with status, DPI settings, rotation, and feature toggles.
class HomePage extends StatefulWidget {
  final ThemeController themeController;
  final LocaleController localeController;

  const HomePage({
    super.key,
    required this.themeController,
    required this.localeController,
  });

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  static const platform = MethodChannel('com.display.switcher/task');

  ShizukuStatus _shizukuStatus = ShizukuStatus.checking;
  bool _shizukuRunning = false;
  String _customErrorTitle = '';
  bool _isLoading = false;
  bool _hasError = false;
  String _errorDetail = '';

  int _currentRearDpi = 0;
  bool _dpiLoading = true;
  final TextEditingController _dpiController = TextEditingController();
  final FocusNode _dpiFocusNode = FocusNode();

  int _currentRotation = 0;

  bool _proximitySensorEnabled = true;
  bool _chargingAnimationEnabled = true;
  bool _keepScreenOnEnabled = true;
  bool _alwaysWakeUpEnabled = false;
  bool _chargingAlwaysOnEnabled = false;
  bool _notificationEnabled = false;

  @override
  void initState() {
    super.initState();
    _checkShizuku();
    _loadSettings();
    _setupMethodCallHandler();
    _loadProximitySensorSetting();

    Future.delayed(const Duration(seconds: 2), () {
      _getCurrentRearDpi();
      _getCurrentRotation();
    });
  }

  @override
  void dispose() {
    _dpiController.dispose();
    _dpiFocusNode.dispose();
    super.dispose();
  }

  void _setupMethodCallHandler() {
    platform.setMethodCallHandler((call) async {
      if (call.method == 'onShizukuPermissionChanged') {
        final granted = call.arguments as bool;
        debugPrint('Shizuku permission changed: $granted');
        await _checkShizuku();
        if (granted) {
          _requestNotificationPermission();
        }
      }
    });
  }

  Future<void> _requestNotificationPermission() async {
    try {
      await platform.invokeMethod('requestNotificationPermission');
      debugPrint('Notification permission request sent');
    } catch (e) {
      debugPrint('Failed to request notification permission: $e');
    }
  }

  Future<void> _getCurrentRearDpi() async {
    setState(() => _dpiLoading = true);

    for (int i = 0; i < 5; i++) {
      try {
        final int dpi = await platform.invokeMethod('getCurrentRearDpi');
        setState(() {
          _currentRearDpi = dpi;
          _dpiController.text = dpi.toString();
          _dpiLoading = false;
        });
        debugPrint('Current rear DPI: $dpi');
        return;
      } catch (e) {
        debugPrint('Failed to get rear DPI (attempt ${i + 1}/5): $e');
        if (i < 4) {
          await Future.delayed(const Duration(seconds: 1));
        }
      }
    }

    setState(() {
      _dpiLoading = false;
      _currentRearDpi = 0;
    });
    debugPrint('Failed to get rear DPI after all retries');
  }

  Future<void> _setRearDpi(int dpi) async {
    if (_isLoading) return;

    setState(() => _isLoading = true);

    try {
      await platform.invokeMethod('ensureTaskServiceConnected');
      await Future.delayed(const Duration(milliseconds: 500));
      await platform.invokeMethod('setRearDpi', {'dpi': dpi});
      await _getCurrentRearDpi();

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '${AppLocalizations.of(context).translate('toast_dpi_set')} $dpi',
            ),
          ),
        );
      }
    } catch (e) {
      debugPrint('Failed to set rear DPI: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '${AppLocalizations.of(context).translate('toast_set_failed')} $e. ${AppLocalizations.of(context).translate('toast_ensure_shizuku')}',
            ),
          ),
        );
      }
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _resetRearDpi() async {
    if (_isLoading) return;

    setState(() => _isLoading = true);

    try {
      await platform.invokeMethod('ensureTaskServiceConnected');
      await Future.delayed(const Duration(milliseconds: 500));
      await platform.invokeMethod('resetRearDpi');
      await _getCurrentRearDpi();

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content:
                Text(AppLocalizations.of(context).translate('toast_dpi_reset')),
          ),
        );
      }
    } catch (e) {
      debugPrint('Failed to reset rear DPI: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '${AppLocalizations.of(context).translate('toast_reset_failed')} $e. ${AppLocalizations.of(context).translate('toast_ensure_shizuku')}',
            ),
          ),
        );
      }
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _checkShizuku() async {
    setState(() {
      _shizukuStatus = ShizukuStatus.checking;
      _hasError = false;
      _errorDetail = '';
    });

    try {
      final result = await platform
          .invokeMethod('checkShizuku')
          .timeout(const Duration(seconds: 3));

      if (!mounted) return;

      setState(() {
        _shizukuRunning = result == true;
        _hasError = false;
        _errorDetail = '';

        if (_shizukuRunning) {
          _shizukuStatus = ShizukuStatus.running;
          _requestNotificationPermission();
        } else {
          _hasError = true;
          _shizukuStatus = ShizukuStatus.error;
          _customErrorTitle = '';
          _errorDetail =
              AppLocalizations.of(context).translate('shizuku_permission_denied');
          _getDetailedStatus();
        }
      });
    } catch (e) {
      if (!mounted) return;

      String errorType = '';
      String errorMsg = e.toString();

      if (errorMsg.contains('binder') || errorMsg.contains('Binder')) {
        errorType =
            AppLocalizations.of(context).translate('error_shizuku_communication');
        _errorDetail = AppLocalizations.of(context)
            .translate('error_shizuku_service_crashed');
      } else if (errorMsg.contains('permission') ||
          errorMsg.contains('Permission')) {
        errorType =
            AppLocalizations.of(context).translate('error_permission_denied');
        _errorDetail =
            AppLocalizations.of(context).translate('error_grant_in_shizuku');
      } else if (errorMsg.contains('RemoteException')) {
        errorType =
            AppLocalizations.of(context).translate('error_service_call_failed');
        _errorDetail = AppLocalizations.of(context)
            .translate('error_task_service_no_response');
      } else if (errorMsg.contains('TimeoutException')) {
        errorType =
            AppLocalizations.of(context).translate('error_check_timeout');
        _errorDetail =
            AppLocalizations.of(context).translate('error_shizuku_timeout');
      } else {
        errorType = AppLocalizations.of(context).translate('error_unknown');
        _errorDetail =
            errorMsg.length > 50 ? '${errorMsg.substring(0, 50)}...' : errorMsg;
      }
      setState(() {
        _shizukuRunning = false;
        _hasError = true;
        _shizukuStatus = ShizukuStatus.error;
        _customErrorTitle = errorType;
      });
    }
  }

  Future<void> _getDetailedStatus() async {
    try {
      final info = await platform.invokeMethod('getShizukuInfo');
      setState(() {
        _errorDetail = info.toString();
      });
    } catch (e) {
      debugPrint('Failed to get detailed status: $e');
    }
  }

  Future<void> _applyProfile(AppProfile profile) async {
    if (_isLoading) return;
    setState(() => _isLoading = true);

    try {
      setState(() {
        _proximitySensorEnabled = profile.proximitySensor;
        _keepScreenOnEnabled = profile.keepScreenOn;
        _alwaysWakeUpEnabled = profile.alwaysWakeUp;
        _chargingAnimationEnabled = profile.chargingAnimation;
        _chargingAlwaysOnEnabled = profile.chargingAlwaysOn;
        // notificationEnabled isn't in AppProfile tests, so we leave it as is or default
      });

      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('proximity_sensor_enabled', _proximitySensorEnabled);
      await prefs.setBool('keep_screen_on_enabled', _keepScreenOnEnabled);
      await prefs.setBool('always_wakeup_enabled', _alwaysWakeUpEnabled);
      await prefs.setBool('charging_animation_enabled', _chargingAnimationEnabled);
      await prefs.setBool('charging_always_on_enabled', _chargingAlwaysOnEnabled);
      await prefs.setBool('notification_service_enabled', _notificationEnabled);

      if (_shizukuRunning) {
        await platform.invokeMethod('ensureTaskServiceConnected');
        await Future.delayed(const Duration(milliseconds: 500));

        await platform.invokeMethod('setProximitySensorEnabled', {'enabled': _proximitySensorEnabled});
        await platform.invokeMethod('setKeepScreenOnEnabled', {'enabled': _keepScreenOnEnabled});
        await platform.invokeMethod('setAlwaysWakeUpEnabled', {'enabled': _alwaysWakeUpEnabled});
        await platform.invokeMethod('setChargingAlwaysOnEnabled', {'enabled': _chargingAlwaysOnEnabled});
        await platform.invokeMethod('toggleChargingService', {'enabled': _chargingAnimationEnabled});
        await platform.invokeMethod('toggleNotificationService', {'enabled': _notificationEnabled});

        if (profile.dpi > 0) {
          await platform.invokeMethod('setRearDpi', {'dpi': profile.dpi});
        } else {
          await platform.invokeMethod('resetRearDpi');
        }
        await _getCurrentRearDpi();

        final rotResult = await platform.invokeMethod('setDisplayRotation', {
          'displayId': 1,
          'rotation': profile.rotation,
        });
        if (rotResult == true) {
           _currentRotation = profile.rotation;
        }
      }

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Đã áp dụng cấu hình: ${profile.name}')),
        );
      }
    } catch (e) {
      debugPrint('Failed to apply profile: $e');
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _restartApp() async {
    if (_isLoading) return;
    setState(() => _isLoading = true);

    try {
      await platform.invokeMethod('ensureTaskServiceConnected');
      await Future.delayed(const Duration(milliseconds: 500));
      await platform.invokeMethod('returnRearAppAndRestart');
    } catch (e) {
      debugPrint('Restart error: $e');
    }
    SystemNavigator.pop();
  }

  Future<void> _loadSettings() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      setState(() {
        _proximitySensorEnabled =
            prefs.getBool('proximity_sensor_enabled') ?? true;
        _chargingAnimationEnabled =
            prefs.getBool('charging_animation_enabled') ?? true;
        _chargingAlwaysOnEnabled =
            prefs.getBool('charging_always_on_enabled') ?? false;
        _keepScreenOnEnabled =
            prefs.getBool('keep_screen_on_enabled') ?? true;
        _alwaysWakeUpEnabled =
            prefs.getBool('always_wakeup_enabled') ?? false;
        _notificationEnabled =
            prefs.getBool('notification_service_enabled') ?? false;
      });

      if (_chargingAnimationEnabled) {
        _startChargingService();
      }
      _checkNotificationPermission();
      if (_notificationEnabled) {
        _startNotificationService();
      }
    } catch (e) {
      debugPrint('Failed to load settings: $e');
    }
  }

  Future<void> _loadProximitySensorSetting() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      setState(() {
        _proximitySensorEnabled =
            prefs.getBool('proximity_sensor_enabled') ?? true;
      });
    } catch (e) {
      debugPrint('Failed to load proximity sensor setting: $e');
    }
  }

  Future<void> _checkNotificationPermission() async {
    try {
      final bool hasPermission =
          await platform.invokeMethod('checkNotificationListenerPermission');
      debugPrint('Notification listener permission: $hasPermission');
    } catch (e) {
      debugPrint('Failed to check notification permission: $e');
    }
  }

  Future<void> _startNotificationService() async {
    try {
      await platform.invokeMethod('startNotificationService');
      debugPrint('NotificationService started');
    } catch (e) {
      debugPrint('Failed to start NotificationService: $e');
    }
  }

  Future<void> _toggleNotificationService(bool enabled) async {
    if (enabled) {
      final bool hasPermission = await platform.invokeMethod(
        'checkNotificationListenerPermission',
      );
      if (!hasPermission) {
        await platform.invokeMethod('openNotificationListenerSettings');
        return;
      }
    }

    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('notification_service_enabled', enabled);
      await platform.invokeMethod('toggleNotificationService', {
        'enabled': enabled,
      });
      if (enabled) {
        await _startNotificationService();
      }
      setState(() => _notificationEnabled = enabled);
    } catch (e) {
      debugPrint('Failed to toggle notification service: $e');
      setState(() => _notificationEnabled = !enabled);
    }
  }

  Future<void> _openAppSelectionPage() async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const AppSelectionPage()),
    );
  }

  Future<void> _toggleProximitySensor(bool enabled) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('proximity_sensor_enabled', enabled);
      await platform.invokeMethod('setProximitySensorEnabled', {
        'enabled': enabled,
      });
      setState(() => _proximitySensorEnabled = enabled);
    } catch (e) {
      debugPrint('Failed to toggle proximity sensor: $e');
      setState(() => _proximitySensorEnabled = !enabled);
    }
  }

  Future<void> _toggleChargingAnimation(bool enabled) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('charging_animation_enabled', enabled);
      await platform.invokeMethod('toggleChargingService', {
        'enabled': enabled,
      });
      setState(() => _chargingAnimationEnabled = enabled);
    } catch (e) {
      debugPrint('Failed to toggle charging animation: $e');
      setState(() => _chargingAnimationEnabled = !enabled);
    }
  }

  Future<void> _startChargingService() async {
    try {
      await platform.invokeMethod('toggleChargingService', {'enabled': true});
    } catch (e) {
      debugPrint('Failed to start charging service: $e');
    }
  }

  Future<void> _toggleKeepScreenOn(bool enabled) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('keep_screen_on_enabled', enabled);

      if (enabled && _alwaysWakeUpEnabled) {
        await prefs.setBool('always_wakeup_enabled', false);
        await platform.invokeMethod('setAlwaysWakeUpEnabled', {
          'enabled': false,
        });
      }

      await platform.invokeMethod('setKeepScreenOnEnabled', {
        'enabled': enabled,
      });

      setState(() {
        _keepScreenOnEnabled = enabled;
        if (enabled) _alwaysWakeUpEnabled = false;
      });
    } catch (e) {
      debugPrint('Failed to toggle keep screen on: $e');
      setState(() => _keepScreenOnEnabled = !enabled);
    }
  }

  Future<void> _toggleAlwaysWakeUp(bool enabled) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('always_wakeup_enabled', enabled);

      if (enabled && _keepScreenOnEnabled) {
        await prefs.setBool('keep_screen_on_enabled', false);
        await platform.invokeMethod('setKeepScreenOnEnabled', {
          'enabled': false,
        });
      }

      await platform.invokeMethod('setAlwaysWakeUpEnabled', {
        'enabled': enabled,
      });

      setState(() {
        _alwaysWakeUpEnabled = enabled;
        if (enabled) _keepScreenOnEnabled = false;
      });
    } catch (e) {
      debugPrint('Failed to toggle always wake up: $e');
      setState(() => _alwaysWakeUpEnabled = !enabled);
    }
  }

  Future<void> _toggleChargingAlwaysOn(bool enabled) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('charging_always_on_enabled', enabled);
      await platform.invokeMethod('setChargingAlwaysOnEnabled', {
        'enabled': enabled,
      });
      setState(() => _chargingAlwaysOnEnabled = enabled);
    } catch (e) {
      debugPrint('Failed to toggle charging always on: $e');
      setState(() => _chargingAlwaysOnEnabled = !enabled);
    }
  }

  String _getDisplayStatus(BuildContext context) {
    switch (_shizukuStatus) {
      case ShizukuStatus.checking:
        return AppLocalizations.of(context).translate('check_shizuku');
      case ShizukuStatus.running:
        return AppLocalizations.of(context).translate('status_ready');
      case ShizukuStatus.error:
        return _customErrorTitle.isNotEmpty
            ? _customErrorTitle
            : AppLocalizations.of(context).translate('permission_required');
    }
  }

  // === UI Builder helpers ===

  Widget _buildProfileSelector(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 8),
          child: Text(
            'Hồ sơ cài đặt',
            style: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: Colors.white.withValues(alpha: 0.9),
            ),
          ),
        ),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          physics: const BouncingScrollPhysics(),
          clipBehavior: Clip.none,
          child: Row(
            children: AppProfile.presets.map((profile) {
              return Padding(
                padding: const EdgeInsets.only(right: 12.0),
                child: CustomPaint(
                  painter: SquircleBorderPainter(
                    radius: SquircleRadii.medium,
                    color: Colors.white.withValues(alpha: 0.5),
                    strokeWidth: 1.5,
                  ),
                  child: ClipPath(
                    clipper: const SquircleClipper(cornerRadius: SquircleRadii.medium),
                    child: BackdropFilter(
                      filter: ImageFilter.blur(sigmaX: 0, sigmaY: 0),
                      child: Material(
                        color: Colors.transparent,
                        child: InkWell(
                          onTap: (_isLoading || !_shizukuRunning) ? null : () => _applyProfile(profile),
                          splashColor: Colors.white.withValues(alpha: 0.3),
                          highlightColor: Colors.white.withValues(alpha: 0.2),
                          child: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                            decoration: BoxDecoration(
                              color: Colors.white.withValues(alpha: 0.25),
                            ),
                            child: Row(
                              children: [
                                Text(profile.icon, style: const TextStyle(fontSize: 20)),
                                const SizedBox(width: 8),
                                Text(
                                  profile.name,
                                  style: const TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.w600,
                                    color: Colors.black87,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ),
      ],
    );
  }

  void _showLanguagePicker(BuildContext context) {
    final lc = widget.localeController;
    showDialog(
      context: context,
      builder: (context) => SimpleDialog(
        title: const Text('Chọn ngôn ngữ'),
        children: LocaleController.supportedLocales.entries.map((entry) {
          return RadioListTile<String>(
            value: entry.key,
            groupValue: lc.currentCode,
            title: Text(entry.value),
            onChanged: (value) {
              if (value != null) {
                lc.setLocale(value);
              }
              Navigator.pop(context);
            },
          );
        }).toList(),
      ),
    );
  }

  Widget _buildGlassCard({
    required List<Widget> children,
    EdgeInsets padding = const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
  }) {
    return CustomPaint(
      painter: SquircleBorderPainter(
        radius: SquircleRadii.large,
        color: Colors.white.withValues(alpha: 0.5),
        strokeWidth: 1.5,
      ),
      child: ClipPath(
        clipper: const SquircleClipper(cornerRadius: SquircleRadii.large),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 0, sigmaY: 0),
          child: Container(
            padding: padding,
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.25),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: children,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildToggleRow({
    required String title,
    required bool value,
    required ValueChanged<bool> onChanged,
    bool showWarning = false,
    Widget? trailingExtra,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: () => onChanged(!value),
        borderRadius: BorderRadius.circular(SquircleRadii.small),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 12.0, horizontal: 8.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      title,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: Colors.black87,
                      ),
                    ),
                  ),
                  if (trailingExtra != null) trailingExtra,
                  const SizedBox(width: 8),
                  GradientToggle(value: value, onChanged: onChanged),
                ],
              ),
              if (showWarning) ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.orange.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(SquircleRadii.small),
                    border: Border.all(
                      color: Colors.orange.withValues(alpha: 0.4),
                      width: 1,
                    ),
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          AppLocalizations.of(context).translate('warning_burn_in'),
                          style: const TextStyle(
                            fontSize: 12,
                            color: Colors.black87,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildRotationButton(String label, int rotation) {
    bool isSelected = _currentRotation == rotation;
    return SizedBox(
      width: 50,
      height: 32,
      child: ClipPath(
        clipper: const SquircleClipper(cornerRadius: SquircleRadii.small),
        child: Container(
          decoration: BoxDecoration(
            gradient: isSelected ? kBrandGradient : null,
            color: isSelected ? null : Colors.white70,
          ),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: (_isLoading || _dpiLoading)
                  ? null
                  : () => _setRotation(rotation),
              child: Center(
                child: Text(
                  label,
                  style: TextStyle(
                    fontSize: 12,
                    color: isSelected ? Colors.white : Colors.black54,
                    fontWeight:
                        isSelected ? FontWeight.w500 : FontWeight.normal,
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _getCurrentRotation() async {
    try {
      final rotation = await platform.invokeMethod('getDisplayRotation', {
        'displayId': 1,
      });
      if (rotation != null && rotation >= 0) {
        setState(() => _currentRotation = rotation);
      }
    } catch (e) {
      debugPrint('Failed to get rotation: $e');
    }
  }

  Future<void> _setRotation(int rotation) async {
    if (!_shizukuRunning) {
      debugPrint('Shizuku not running, cannot set rotation');
      return;
    }
    if (_isLoading) return;

    setState(() => _isLoading = true);

    try {
      await platform.invokeMethod('ensureTaskServiceConnected');
      await Future.delayed(const Duration(milliseconds: 500));

      final result = await platform.invokeMethod('setDisplayRotation', {
        'displayId': 1,
        'rotation': rotation,
      });

      if (result == true) {
        setState(() => _currentRotation = rotation);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                '${AppLocalizations.of(context).translate('toast_rotation_set')} ${rotation * 90}°',
              ),
              duration: const Duration(seconds: 1),
            ),
          );
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                AppLocalizations.of(context).translate('toast_rotation_failed'),
              ),
            ),
          );
        }
      }
    } catch (e) {
      debugPrint('Rotation error: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '${AppLocalizations.of(context).translate('toast_error')} $e',
            ),
          ),
        );
      }
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Widget _buildLinkCard({
    required String emoji,
    required String label,
    required VoidCallback onTap,
  }) {
    return CustomPaint(
      painter: SquircleBorderPainter(
        radius: SquircleRadii.large,
        color: Colors.white.withValues(alpha: 0.5),
        strokeWidth: 1.5,
      ),
      child: ClipPath(
        clipper: const SquircleClipper(cornerRadius: SquircleRadii.large),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 0, sigmaY: 0),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: onTap,
              splashColor: Colors.white.withValues(alpha: 0.3),
              highlightColor: Colors.white.withValues(alpha: 0.2),
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.25),
                ),
                padding: const EdgeInsets.symmetric(
                    vertical: 12, horizontal: 16),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(emoji, style: const TextStyle(fontSize: 20)),
                    const SizedBox(width: 8),
                    Text(
                      label,
                      style: const TextStyle(
                        color: Colors.black87,
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    const SizedBox(width: 4),
                    const Icon(Icons.open_in_new,
                        size: 16, color: Colors.black54),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);
    final tc = widget.themeController;
    final gradient = tc.currentGradient;

    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        elevation: 0,
        scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent,
        shadowColor: Colors.transparent,
        title:
            const Text('MRSS', style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          IconButton(
            icon: const Icon(Icons.language),
            onPressed: () => _showLanguagePicker(context),
            tooltip: widget.localeController.currentName,
          ),
          IconButton(
            icon: Icon(tc.isDark ? Icons.light_mode : Icons.dark_mode),
            onPressed: () => tc.toggle(),
            tooltip: tc.isDark ? 'Chế độ sáng' : 'Chế độ tối',
          ),
          IconButton(
            icon: const Icon(Icons.restart_alt),
            onPressed: _restartApp,
            tooltip: l.translate('restart_app'),
          ),
        ],
      ),
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: BoxDecoration(gradient: gradient),
        child: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            physics: const BouncingScrollPhysics(),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Status card
                _buildGlassCard(
                  padding: const EdgeInsets.all(16),
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          _shizukuRunning
                              ? Icons.check_circle
                              : (_hasError
                                  ? Icons.error_outline
                                  : Icons.warning_rounded),
                          size: 28,
                          color: _shizukuRunning
                              ? Colors.green
                              : (_hasError ? Colors.red : Colors.orange),
                        ),
                        const SizedBox(width: 10),
                        Text(
                          _getDisplayStatus(context),
                          style: const TextStyle(
                            fontSize: 16,
                            color: Colors.black87,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                    if (_hasError && _errorDetail.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      Text(
                        _errorDetail,
                        style: const TextStyle(
                          fontSize: 12,
                          color: Colors.black54,
                          height: 1.3,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: 20),

                // Profile selector
                _buildProfileSelector(context),
                const SizedBox(height: 20),

                // DPI settings card
                _buildGlassCard(
                  padding: const EdgeInsets.all(20),
                  children: [
                    Row(
                      children: [
                        Text(
                          l.translate('dpi_settings'),
                          style: Theme.of(context)
                              .textTheme
                              .titleMedium
                              ?.copyWith(
                                color: Colors.black87,
                                fontWeight: FontWeight.bold,
                              ),
                        ),
                        if (_dpiLoading) ...[
                          const SizedBox(width: 12),
                          const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              valueColor: AlwaysStoppedAnimation<Color>(
                                  Colors.black54),
                            ),
                          ),
                        ],
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _dpiLoading
                          ? l.translate('checking_dpi')
                          : '${l.translate('current_dpi').replaceAll('%d', _currentRearDpi.toString())}  ${l.translate('recommended_range')}',
                      style: const TextStyle(
                          color: Colors.black54, fontSize: 14),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _dpiController,
                            focusNode: _dpiFocusNode,
                            enabled: !_dpiLoading && !_isLoading,
                            keyboardType: TextInputType.number,
                            style: const TextStyle(color: Colors.black87),
                            decoration: InputDecoration(
                              labelText: l.translate('new_dpi'),
                              labelStyle:
                                  const TextStyle(color: Colors.black54),
                              hintText: l.translate('input_number'),
                              hintStyle:
                                  const TextStyle(color: Colors.black38),
                              border: OutlineInputBorder(
                                borderRadius: const BorderRadius.all(
                                  Radius.circular(SquircleRadii.small),
                                ),
                                borderSide:
                                    BorderSide(color: Colors.black26),
                              ),
                              enabledBorder: OutlineInputBorder(
                                borderRadius: const BorderRadius.all(
                                  Radius.circular(SquircleRadii.small),
                                ),
                                borderSide:
                                    BorderSide(color: Colors.black26),
                              ),
                              focusedBorder: OutlineInputBorder(
                                borderRadius: const BorderRadius.all(
                                  Radius.circular(SquircleRadii.small),
                                ),
                                borderSide: const BorderSide(
                                    color: Colors.black54, width: 2),
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(width: 12),
                        ClipPath(
                          clipper: const SquircleClipper(
                              cornerRadius: SquircleRadii.small),
                          child: Container(
                            decoration: const BoxDecoration(
                                gradient: kBrandGradient),
                            child: ElevatedButton(
                              onPressed: (_isLoading || _dpiLoading)
                                  ? null
                                  : () {
                                      final dpi = int.tryParse(
                                          _dpiController.text);
                                      if (dpi != null && dpi > 0) {
                                        _setRearDpi(dpi);
                                      } else {
                                        ScaffoldMessenger.of(context)
                                            .showSnackBar(
                                          SnackBar(
                                            content: Text(
                                                l.translate('input_number')),
                                          ),
                                        );
                                      }
                                    },
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.transparent,
                                foregroundColor: Colors.white,
                                shadowColor: Colors.transparent,
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 20, vertical: 16),
                              ),
                              child: Text(l.translate('set_dpi')),
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    SizedBox(
                      width: double.infinity,
                      child: CustomPaint(
                        painter: SquircleBorderPainter(
                          radius: SquircleRadii.small,
                          color: Colors.black26,
                          strokeWidth: 1,
                        ),
                        child: ClipPath(
                          clipper: const SquircleClipper(
                              cornerRadius: SquircleRadii.small),
                          child: Material(
                            color: Colors.transparent,
                            child: InkWell(
                              onTap: (_isLoading || _dpiLoading)
                                  ? null
                                  : _resetRearDpi,
                              child: const Padding(
                                padding: EdgeInsets.symmetric(vertical: 12),
                                child: Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.center,
                                  children: [
                                    Icon(Icons.restore,
                                        color: Colors.black87, size: 20),
                                    SizedBox(width: 8),
                                    Text(
                                      'Khôi phục DPI mặc định',
                                      style: TextStyle(
                                          color: Colors.black87,
                                          fontSize: 14),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    const Divider(color: Colors.black26, height: 1),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Text(
                          l.translate('rotation_title'),
                          style: const TextStyle(
                            fontSize: 13,
                            color: Colors.black87,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        const Spacer(),
                        _buildRotationButton('0°', 0),
                        const SizedBox(width: 6),
                        _buildRotationButton('90°', 1),
                        const SizedBox(width: 6),
                        _buildRotationButton('180°', 2),
                        const SizedBox(width: 6),
                        _buildRotationButton('270°', 3),
                      ],
                    ),
                  ],
                ),
                const SizedBox(height: 20),

                // Features Group
                _buildGlassCard(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  children: [
                    _buildToggleRow(
                      title: l.translate('rear_cover_detection_title'),
                      value: _proximitySensorEnabled,
                      onChanged: _toggleProximitySensor,
                    ),
                    const Divider(color: Colors.black12, height: 1, indent: 8, endIndent: 8),
                    _buildToggleRow(
                      title: l.translate('rear_screen_always_on_title'),
                      value: _keepScreenOnEnabled,
                      onChanged: _toggleKeepScreenOn,
                    ),
                    const Divider(color: Colors.black12, height: 1, indent: 8, endIndent: 8),
                    _buildToggleRow(
                      title: l.translate('always_wake_up_title'),
                      value: _alwaysWakeUpEnabled,
                      onChanged: _toggleAlwaysWakeUp,
                      showWarning: _alwaysWakeUpEnabled,
                    ),
                    const Divider(color: Colors.black12, height: 1, indent: 8, endIndent: 8),
                    _buildToggleRow(
                      title: l.translate('charging_animation_title'),
                      value: _chargingAnimationEnabled,
                      onChanged: _toggleChargingAnimation,
                    ),
                    const Divider(color: Colors.black12, height: 1, indent: 8, endIndent: 8),
                    _buildToggleRow(
                      title: l.translate('charging_always_on_title'),
                      value: _chargingAlwaysOnEnabled,
                      onChanged: _toggleChargingAlwaysOn,
                      showWarning: _chargingAlwaysOnEnabled,
                    ),
                    const Divider(color: Colors.black12, height: 1, indent: 8, endIndent: 8),
                    _buildToggleRow(
                      title: l.translate('notification_service_title'),
                      value: _notificationEnabled,
                      onChanged: _toggleNotificationService,
                      trailingExtra: IconButton(
                        icon: const Icon(Icons.menu, size: 24),
                        color: Colors.black87,
                        onPressed: _openAppSelectionPage,
                        tooltip: l.translate('select_apps'),
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),

                // Tutorial link
                _buildLinkCard(
                  emoji: '📖',
                  label: l.translate('tutorial'),
                  onTap: () async {
                    try {
                      await platform.invokeMethod('openTutorial');
                    } catch (e) {
                      debugPrint('Failed to open tutorial: $e');
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(l.translate('open_failed')),
                          ),
                        );
                      }
                    }
                  },
                ),
                const SizedBox(height: 16),

                // Author link
                _buildLinkCard(
                  emoji: '👨‍💻',
                  label: l.translate('author_anti'),
                  onTap: () async {
                    try {
                      await platform.invokeMethod('openCoolApkProfile');
                    } catch (e) {
                      debugPrint('Failed to open CoolApk profile: $e');
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(l.translate('install_coolapk')),
                          ),
                        );
                      }
                    }
                  },
                ),
                const SizedBox(height: 16),

                // Tester link
                _buildLinkCard(
                  emoji: '🧪',
                  label: l.translate('author_xmz'),
                  onTap: () async {
                    try {
                      await platform.invokeMethod('openCoolApkProfileXmz');
                    } catch (e) {
                      debugPrint('Failed to open CoolApk profile: $e');
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(l.translate('install_coolapk')),
                          ),
                        );
                      }
                    }
                  },
                ),
                const SizedBox(height: 16),

                // Donation + QQ group
                Row(
                  children: [
                    Expanded(
                      child: _buildLinkCard(
                        emoji: '☕',
                        label: l.translate('buy_coffee'),
                        onTap: () async {
                          try {
                            await platform.invokeMethod('openDonationPage');
                          } catch (e) {
                            debugPrint('Failed to open donation: $e');
                            if (context.mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text(l.translate('open_failed')),
                                ),
                              );
                            }
                          }
                        },
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: _buildLinkCard(
                        emoji: '💬',
                        label: l.translate('qq_group_title'),
                        onTap: () async {
                          try {
                            await platform.invokeMethod('openQQGroup');
                          } catch (e) {
                            debugPrint('Failed to open QQ group: $e');
                            if (context.mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text(l.translate('open_failed')),
                                ),
                              );
                            }
                          }
                        },
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
