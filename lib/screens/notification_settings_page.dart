import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../l10n/app_localizations.dart';
import '../widgets/squircle.dart';
import '../widgets/gradient_widgets.dart';

/// Notification settings page - configure privacy, DND, dark mode, auto-destroy.
class NotificationSettingsPage extends StatefulWidget {
  const NotificationSettingsPage({super.key});

  @override
  State<NotificationSettingsPage> createState() =>
      _NotificationSettingsPageState();
}

class _NotificationSettingsPageState extends State<NotificationSettingsPage> {
  static const platform = MethodChannel('com.display.switcher/task');

  bool _privacyHideTitle = false;
  bool _privacyHideContent = false;
  bool _followDndMode = true;
  bool _onlyWhenLocked = false;
  bool _notificationDarkMode = false;
  int _notificationDuration = 10;
  final TextEditingController _durationController = TextEditingController();
  final FocusNode _durationFocusNode = FocusNode();

  @override
  void initState() {
    super.initState();
    _loadAllSettings();
  }

  @override
  void dispose() {
    _durationController.dispose();
    _durationFocusNode.dispose();
    super.dispose();
  }

  Future<void> _loadAllSettings() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      setState(() {
        _privacyHideTitle =
            prefs.getBool('notification_privacy_hide_title') ?? false;
        _privacyHideContent =
            prefs.getBool('notification_privacy_hide_content') ?? false;
        _followDndMode = prefs.getBool('notification_follow_dnd_mode') ?? true;
        _onlyWhenLocked =
            prefs.getBool('notification_only_when_locked') ?? false;
        _notificationDarkMode =
            prefs.getBool('notification_dark_mode') ?? false;
        _notificationDuration = prefs.getInt('notification_duration') ?? 10;
        _durationController.text = _notificationDuration.toString();
      });
    } catch (e) {
      debugPrint('Failed to load notification settings: $e');
    }
  }

  Future<void> _togglePrivacyHideTitle(bool enabled) async {
    try {
      await platform.invokeMethod('setNotificationPrivacyHideTitle', {
        'enabled': enabled,
      });
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('notification_privacy_hide_title', enabled);
      setState(() {
        _privacyHideTitle = enabled;
      });
    } catch (e) {
      debugPrint('Failed to toggle hide title: $e');
    }
  }

  Future<void> _togglePrivacyHideContent(bool enabled) async {
    try {
      await platform.invokeMethod('setNotificationPrivacyHideContent', {
        'enabled': enabled,
      });
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('notification_privacy_hide_content', enabled);
      setState(() {
        _privacyHideContent = enabled;
      });
    } catch (e) {
      debugPrint('Failed to toggle hide content: $e');
    }
  }

  Future<void> _toggleFollowDndMode(bool enabled) async {
    try {
      await platform.invokeMethod('setFollowDndMode', {'enabled': enabled});
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('notification_follow_dnd_mode', enabled);
      setState(() {
        _followDndMode = enabled;
      });
    } catch (e) {
      debugPrint('Failed to toggle DND mode: $e');
    }
  }

  Future<void> _toggleOnlyWhenLocked(bool enabled) async {
    try {
      await platform.invokeMethod('setOnlyWhenLocked', {'enabled': enabled});
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('notification_only_when_locked', enabled);
      setState(() {
        _onlyWhenLocked = enabled;
      });
    } catch (e) {
      debugPrint('Failed to toggle only-when-locked: $e');
    }
  }

  Future<void> _toggleNotificationDarkMode(bool enabled) async {
    try {
      await platform.invokeMethod('setNotificationDarkMode', {
        'enabled': enabled,
      });
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('notification_dark_mode', enabled);
      setState(() {
        _notificationDarkMode = enabled;
      });
    } catch (e) {
      debugPrint('Failed to toggle notification dark mode: $e');
    }
  }

  Future<void> _setNotificationDuration(int seconds) async {
    try {
      await platform.invokeMethod('setNotificationDuration', {
        'duration': seconds,
      });
      final prefs = await SharedPreferences.getInstance();
      await prefs.setInt('notification_duration', seconds);
      setState(() {
        _notificationDuration = seconds;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              AppLocalizations.of(context)
                  .translate('toast_duration_set')
                  .replaceAll('{0}', '$seconds'),
            ),
          ),
        );
      }
    } catch (e) {
      debugPrint('Failed to set notification duration: $e');
    }
  }

  Widget _buildSettingsCard({required List<Widget> children}) {
    return CustomPaint(
      painter: const SquircleBorderPainter(
        radius: 32,
        color: Colors.white30,
        strokeWidth: 1.5,
      ),
      child: ClipPath(
        clipper: const SquircleClipper(cornerRadius: SquircleRadii.large),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 0, sigmaY: 0),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.25),
            ),
            child: Column(children: children),
          ),
        ),
      ),
    );
  }

  Widget _buildToggleRow({
    required IconData icon,
    required String label,
    required bool value,
    required ValueChanged<bool> onChanged,
  }) {
    return Row(
      children: [
        Icon(icon, size: 20, color: Colors.black54),
        const SizedBox(width: 8),
        Text(
          label,
          style: const TextStyle(
            fontSize: 14,
            color: Colors.black87,
            fontWeight: FontWeight.w500,
          ),
        ),
        const Spacer(),
        GradientToggle(value: value, onChanged: onChanged),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l.translate('notification_settings_title')),
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        elevation: 0,
        scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent,
        shadowColor: Colors.transparent,
      ),
      extendBodyBehindAppBar: true,
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: const BoxDecoration(gradient: kBrandGradient),
        child: SafeArea(
          child: ListView(
            padding: const EdgeInsets.all(20),
            children: [
              // Privacy settings card
              _buildSettingsCard(
                children: [
                  _buildToggleRow(
                    icon: Icons.lock_outline,
                    label: l.translate('hide_notification_title'),
                    value: _privacyHideTitle,
                    onChanged: _togglePrivacyHideTitle,
                  ),
                  const SizedBox(height: 12),
                  const Divider(color: Colors.black26, height: 1),
                  const SizedBox(height: 12),
                  _buildToggleRow(
                    icon: Icons.lock_outline,
                    label: l.translate('hide_notification_content'),
                    value: _privacyHideContent,
                    onChanged: _togglePrivacyHideContent,
                  ),
                ],
              ),
              const SizedBox(height: 20),
              // Follow system DND
              _buildSettingsCard(
                children: [
                  _buildToggleRow(
                    icon: Icons.notifications_paused,
                    label: l.translate('follow_system_dnd'),
                    value: _followDndMode,
                    onChanged: _toggleFollowDndMode,
                  ),
                ],
              ),
              const SizedBox(height: 20),
              // Only when upside down
              _buildSettingsCard(
                children: [
                  _buildToggleRow(
                    icon: Icons.flip_camera_android,
                    label: l.translate('only_when_locked'),
                    value: _onlyWhenLocked,
                    onChanged: _toggleOnlyWhenLocked,
                  ),
                ],
              ),
              const SizedBox(height: 20),
              // Notification dark mode
              _buildSettingsCard(
                children: [
                  _buildToggleRow(
                    icon: Icons.dark_mode,
                    label: l.translate('notification_dark_mode'),
                    value: _notificationDarkMode,
                    onChanged: _toggleNotificationDarkMode,
                  ),
                ],
              ),
              const SizedBox(height: 20),
              // Auto destroy time
              _buildSettingsCard(
                children: [
                  Row(
                    children: [
                      const Icon(Icons.timer_outlined,
                          size: 20, color: Colors.black54),
                      const SizedBox(width: 8),
                      Text(
                        l.translate('auto_destroy_time'),
                        style: const TextStyle(
                          fontSize: 14,
                          color: Colors.black87,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _durationController,
                          focusNode: _durationFocusNode,
                          keyboardType: TextInputType.number,
                          style: const TextStyle(color: Colors.black87),
                          decoration: InputDecoration(
                            labelText: l.translate('new_time_seconds'),
                            labelStyle: const TextStyle(color: Colors.black54),
                            hintText: l.translate('input_seconds'),
                            hintStyle: const TextStyle(color: Colors.black38),
                            border: OutlineInputBorder(
                              borderRadius: const BorderRadius.all(
                                Radius.circular(SquircleRadii.small),
                              ),
                              borderSide: BorderSide(color: Colors.black26),
                            ),
                            enabledBorder: OutlineInputBorder(
                              borderRadius: const BorderRadius.all(
                                Radius.circular(SquircleRadii.small),
                              ),
                              borderSide: BorderSide(color: Colors.black26),
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
                          decoration: const BoxDecoration(gradient: kBrandGradient),
                          child: ElevatedButton(
                            onPressed: () {
                              final seconds =
                                  int.tryParse(_durationController.text);
                              if (seconds != null && seconds > 0) {
                                _setNotificationDuration(seconds);
                              } else {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(
                                    content:
                                        Text(l.translate('input_valid_number')),
                                  ),
                                );
                              }
                            },
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.transparent,
                              foregroundColor: Colors.white,
                              shadowColor: Colors.transparent,
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 20, vertical: 12),
                            ),
                            child: Text(l.translate('confirm')),
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
