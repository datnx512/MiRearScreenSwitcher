import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../widgets/squircle.dart';
import '../widgets/gradient_widgets.dart';

/// Charging animation style settings.
enum ChargingStyle {
  lightning('⚡', 'Tia sét', 'Hiệu ứng tia sét 3D + chất lỏng'),
  wave('🌊', 'Sóng', 'Sóng năng lượng động'),
  pulse('💓', 'Nhịp', 'Xung nhịp đơn giản, tối pin'),
  minimal('⚪', 'Tối giản', 'Chỉ hiển thị % pin');

  final String icon;
  final String name;
  final String description;
  const ChargingStyle(this.icon, this.name, this.description);
}

/// Settings page for charging animation customization.
class ChargingSettingsPage extends StatefulWidget {
  const ChargingSettingsPage({super.key});

  @override
  State<ChargingSettingsPage> createState() => _ChargingSettingsPageState();
}

class _ChargingSettingsPageState extends State<ChargingSettingsPage> {
  ChargingStyle _selectedStyle = ChargingStyle.lightning;
  double _animationSpeed = 1.0;
  bool _showBatteryPercent = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      final styleIndex = prefs.getInt('charging_style') ?? 0;
      _selectedStyle = ChargingStyle.values[styleIndex.clamp(0, 3)];
      _animationSpeed = prefs.getDouble('charging_speed') ?? 1.0;
      _showBatteryPercent = prefs.getBool('charging_show_percent') ?? true;
    });
  }

  Future<void> _saveStyle(ChargingStyle style) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('charging_style', style.index);
    setState(() => _selectedStyle = style);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Đã đổi kiểu hoạt ảnh: ${style.name}')),
      );
    }
  }

  Future<void> _saveSpeed(double speed) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setDouble('charging_speed', speed);
    setState(() => _animationSpeed = speed);
  }

  Future<void> _toggleBatteryPercent(bool value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('charging_show_percent', value);
    setState(() => _showBatteryPercent = value);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tùy chỉnh hoạt ảnh sạc'),
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
              // Style selector
              Text(
                'Kiểu hoạt ảnh',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.white.withValues(alpha: 0.9),
                ),
              ),
              const SizedBox(height: 12),
              ...ChargingStyle.values.map((style) {
                final isSelected = _selectedStyle == style;
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: GestureDetector(
                    onTap: () => _saveStyle(style),
                    child: ClipPath(
                      clipper: const SquircleClipper(
                          cornerRadius: SquircleRadii.large),
                      child: Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: isSelected
                              ? Colors.white.withValues(alpha: 0.35)
                              : Colors.white.withValues(alpha: 0.15),
                          border: Border.all(
                            color: isSelected
                                ? Colors.white.withValues(alpha: 0.6)
                                : Colors.white.withValues(alpha: 0.2),
                            width: isSelected ? 2 : 1,
                          ),
                        ),
                        child: Row(
                          children: [
                            Text(style.icon,
                                style: const TextStyle(fontSize: 32)),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    style.name,
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.bold,
                                      color: Colors.white
                                          .withValues(alpha: 0.95),
                                    ),
                                  ),
                                  const SizedBox(height: 2),
                                  Text(
                                    style.description,
                                    style: TextStyle(
                                      fontSize: 13,
                                      color: Colors.white
                                          .withValues(alpha: 0.7),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            if (isSelected)
                              Icon(Icons.check_circle,
                                  color: Colors.white.withValues(alpha: 0.9)),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              }),

              const SizedBox(height: 24),

              // Animation speed
              ClipPath(
                clipper: const SquircleClipper(
                    cornerRadius: SquircleRadii.large),
                child: Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.15),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Tốc độ hoạt ảnh',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w500,
                          color: Colors.white.withValues(alpha: 0.9),
                        ),
                      ),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          const Text('0.5x',
                              style: TextStyle(color: Colors.white70)),
                          Expanded(
                            child: Slider(
                              value: _animationSpeed,
                              min: 0.5,
                              max: 2.0,
                              divisions: 6,
                              label: '${_animationSpeed.toStringAsFixed(1)}x',
                              onChanged: _saveSpeed,
                            ),
                          ),
                          const Text('2.0x',
                              style: TextStyle(color: Colors.white70)),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Hiện tại: ${_animationSpeed.toStringAsFixed(1)}x',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.white.withValues(alpha: 0.6),
                        ),
                      ),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 16),

              // Show battery percent toggle
              ClipPath(
                clipper: const SquircleClipper(
                    cornerRadius: SquircleRadii.large),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 20, vertical: 16),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.15),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.battery_std,
                          color: Colors.white70, size: 20),
                      const SizedBox(width: 8),
                      Text(
                        'Hiển thị % pin',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w500,
                          color: Colors.white.withValues(alpha: 0.9),
                        ),
                      ),
                      const Spacer(),
                      GradientToggle(
                        value: _showBatteryPercent,
                        onChanged: _toggleBatteryPercent,
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
