import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../widgets/squircle.dart';
import '../widgets/gradient_widgets.dart';

/// Auto-switch rules configuration page.
/// Users can configure rules to auto-switch apps to rear screen.
class AutoSwitchRulesPage extends StatefulWidget {
  const AutoSwitchRulesPage({super.key});

  @override
  State<AutoSwitchRulesPage> createState() => _AutoSwitchRulesPageState();
}

class _AutoSwitchRulesPageState extends State<AutoSwitchRulesPage> {
  static final platform = MethodChannel('com.display.switcher/task');

  bool _enableOnCharging = false;
  bool _enableOnAppOpen = false;
  bool _enableOnRearScreenOn = false;
  String _chargingTargetApp = '';
  String _appOpenTargetApp = '';
  String _rearScreenTargetApp = '';
  bool _serviceRunning = false;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _enableOnCharging = prefs.getBool('rule_charging_enabled') ?? false;
      _enableOnAppOpen = prefs.getBool('rule_app_open_enabled') ?? false;
      _enableOnRearScreenOn =
          prefs.getBool('rule_rear_screen_on_enabled') ?? false;
      _chargingTargetApp = prefs.getString('target_charging') ?? '';
      _appOpenTargetApp = prefs.getString('target_app_open') ?? '';
      _rearScreenTargetApp = prefs.getString('target_rear_screen_on') ?? '';
      _serviceRunning = prefs.getBool('auto_switch_running') ?? false;
    });
  }

  Future<void> _toggleService(bool enabled) async {
    try {
      if (enabled) {
        await platform.invokeMethod('startAutoSwitch');
      } else {
        await platform.invokeMethod('stopAutoSwitch');
      }
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('auto_switch_running', enabled);
      setState(() => _serviceRunning = enabled);
    } catch (e) {
      debugPrint('Failed to toggle auto-switch service: $e');
    }
  }

  Future<void> _saveRule(
    String ruleType,
    bool enabled,
    String targetApp,
  ) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('rule_${ruleType}_enabled', enabled);
    await prefs.setString('target_$ruleType', targetApp);
    try {
      await platform.invokeMethod('setAutoSwitchRule', {
        'ruleType': ruleType,
        'enabled': enabled,
        'targetApp': targetApp,
      });
    } catch (e) {
      debugPrint('Failed to save rule: $e');
    }
  }

  Widget _buildRuleCard({
    required String title,
    required String icon,
    required String description,
    required bool enabled,
    required String targetApp,
    required ValueChanged<bool> onToggle,
    required ValueChanged<String> onAppChanged,
  }) {
    return ClipPath(
      clipper: const SquircleClipper(cornerRadius: SquircleRadii.large),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.15),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(icon, style: const TextStyle(fontSize: 24)),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Colors.white.withValues(alpha: 0.95),
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        description,
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.white.withValues(alpha: 0.7),
                        ),
                      ),
                    ],
                  ),
                ),
                GradientToggle(value: enabled, onChanged: onToggle),
              ],
            ),
            if (enabled) ...[
              const SizedBox(height: 16),
              TextField(
                controller: TextEditingController(text: targetApp),
                style: TextStyle(color: Colors.white.withValues(alpha: 0.9)),
                decoration: InputDecoration(
                  labelText: 'Tên package ứng dụng',
                  labelStyle: TextStyle(
                      color: Colors.white.withValues(alpha: 0.6)),
                  hintText: 'vd: com.amazon.kindle',
                  hintStyle: TextStyle(
                      color: Colors.white.withValues(alpha: 0.4)),
                  enabledBorder: UnderlineInputBorder(
                    borderSide: BorderSide(
                        color: Colors.white.withValues(alpha: 0.3)),
                  ),
                  focusedBorder: UnderlineInputBorder(
                    borderSide: BorderSide(
                        color: Colors.white.withValues(alpha: 0.6)),
                  ),
                ),
                onChanged: onAppChanged,
              ),
            ],
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tự động chuyển'),
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
              // Service toggle
              ClipPath(
                clipper: const SquircleClipper(
                    cornerRadius: SquircleRadii.large),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 20, vertical: 16),
                  decoration: BoxDecoration(
                    color: _serviceRunning
                        ? Colors.white.withValues(alpha: 0.30)
                        : Colors.white.withValues(alpha: 0.15),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.auto_awesome,
                          color: Colors.white70, size: 24),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Bật tự động chuyển',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                                color: Colors.white.withValues(alpha: 0.95),
                              ),
                            ),
                            Text(
                              _serviceRunning
                                  ? 'Đang giám sát...'
                                  : 'Tắt',
                              style: TextStyle(
                                fontSize: 12,
                                color: Colors.white.withValues(alpha: 0.6),
                              ),
                            ),
                          ],
                        ),
                      ),
                      GradientToggle(
                        value: _serviceRunning,
                        onChanged: _toggleService,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 20),

              // Rule: On charging
              _buildRuleCard(
                title: 'Khi cắm sạc',
                icon: '🔌',
                description: 'Tự động chuyển app khi cắm cáp sạc',
                enabled: _enableOnCharging,
                targetApp: _chargingTargetApp,
                onToggle: (v) {
                  setState(() => _enableOnCharging = v);
                  _saveRule('charging', v, _chargingTargetApp);
                },
                onAppChanged: (v) {
                  _chargingTargetApp = v;
                  _saveRule('charging', _enableOnCharging, v);
                },
              ),
              const SizedBox(height: 16),

              // Rule: On app open
              _buildRuleCard(
                title: 'Khi mở app',
                icon: '📱',
                description: 'Tự động chuyển app cụ thể sang màn hình sau',
                enabled: _enableOnAppOpen,
                targetApp: _appOpenTargetApp,
                onToggle: (v) {
                  setState(() => _enableOnAppOpen = v);
                  _saveRule('app_open', v, _appOpenTargetApp);
                },
                onAppChanged: (v) {
                  _appOpenTargetApp = v;
                  _saveRule('app_open', _enableOnAppOpen, v);
                },
              ),
              const SizedBox(height: 16),

              // Rule: On rear screen on
              _buildRuleCard(
                title: 'Khi bật màn hình sau',
                icon: '📲',
                description: 'Chuyển app khi màn hình sau được bật',
                enabled: _enableOnRearScreenOn,
                targetApp: _rearScreenTargetApp,
                onToggle: (v) {
                  setState(() => _enableOnRearScreenOn = v);
                  _saveRule('rear_screen_on', v, _rearScreenTargetApp);
                },
                onAppChanged: (v) {
                  _rearScreenTargetApp = v;
                  _saveRule('rear_screen_on', _enableOnRearScreenOn, v);
                },
              ),
              const SizedBox(height: 20),

              // Info note
              ClipPath(
                clipper: const SquircleClipper(
                    cornerRadius: SquircleRadii.large),
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.orange.withValues(alpha: 0.15),
                    border: Border.all(
                      color: Colors.orange.withValues(alpha: 0.3),
                    ),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.info_outline,
                          color: Colors.orange, size: 20),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'Tự động chuyển cần Shizuku đang chạy. '
                          'Nhập tên package chính xác của ứng dụng.',
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.white.withValues(alpha: 0.8),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
