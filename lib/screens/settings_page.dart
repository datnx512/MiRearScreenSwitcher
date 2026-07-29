import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:ui';
import '../l10n/app_localizations.dart';
import '../services/theme_controller.dart';
import '../services/locale_controller.dart';
import '../services/backup_service.dart';
import '../services/update_checker.dart';
import '../widgets/squircle.dart';
import 'charging_settings_page.dart';
import 'auto_switch_rules_page.dart';

/// Read app version+build from pubspec.yaml at compile time.
/// Uses package_config.json resolution — no extra dependency needed.
const _appVersion = String.fromEnvironment(
  'APP_VERSION',
  defaultValue: '3.6.3',
);
const _appBuild = String.fromEnvironment(
  'APP_BUILD',
  defaultValue: '15',
);

/// Settings page with appearance, language, backup/restore, and about sections.
class SettingsPage extends StatefulWidget {
  final ThemeController themeController;
  final LocaleController localeController;

  const SettingsPage({
    super.key,
    required this.themeController,
    required this.localeController,
  });

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
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
        title: Text(
          l.translate('settings'),
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
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
                // === Giao diện (Appearance) ===
                _buildSectionTitle('🎨 Giao diện'),
                const SizedBox(height: 12),
                _buildGlassCard(
                  children: [
                    _buildToggleRow(
                      title: tc.isDark ? 'Chế độ tối' : 'Chế độ sáng',
                      subtitle: 'Bật/tắt chế độ tối',
                      value: tc.isDark,
                      onChanged: (_) => tc.toggle(),
                    ),
                  ],
                ),
                const SizedBox(height: 24),

                // === Ngôn ngữ (Language) ===
                _buildSectionTitle('🌐 Ngôn ngữ'),
                const SizedBox(height: 12),
                _buildGlassCard(
                  children: [
                    ...LocaleController.supportedLocales.entries.map((entry) {
                      final isSelected =
                          widget.localeController.currentCode == entry.key;
                      return _buildLocaleRow(
                        entry.value,
                        entry.key,
                        isSelected,
                      );
                    }),
                  ],
                ),
                const SizedBox(height: 24),

                // === Tính năng màn hình sau ===
                _buildSectionTitle('📱 Tính năng màn hình sau'),
                const SizedBox(height: 12),
                _buildGlassCard(
                  children: [
                    _buildNavRow(
                      icon: '🕐',
                      title: 'Đồng hồ màn hình sau',
                      subtitle: 'Hiển thị giờ + ngày + pin',
                      onTap: () async {
                        try {
                          const platform = MethodChannel('com.display.switcher/task');
                          await platform.invokeMethod('showRearClock');
                          if (context.mounted) {
                            _showSnackBar('Đã mở đồng hồ trên màn hình sau');
                          }
                        } catch (e) {
                          _showSnackBar('Không thể mở đồng hồ: $e');
                        }
                      },
                    ),
                    const SizedBox(height: 8),
                    _buildNavRow(
                      icon: '🎵',
                      title: 'Điều khiển nhạc',
                      subtitle: 'Play/Pause/Next trên màn hình sau',
                      onTap: () async {
                        try {
                          const platform = MethodChannel('com.display.switcher/task');
                          await platform.invokeMethod('showRearMedia');
                          if (context.mounted) {
                            _showSnackBar('Đã mở điều khiển nhạc trên màn hình sau');
                          }
                        } catch (e) {
                          _showSnackBar('Không thể mở media: $e');
                        }
                      },
                    ),
                    const SizedBox(height: 8),
                    _buildNavRow(
                      icon: '⚡',
                      title: 'Tùy chỉnh hoạt ảnh sạc',
                      subtitle: '4 kiểu: Tia sét, Sóng, Nhịp, Tối giản',
                      onTap: () => Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const ChargingSettingsPage(),
                        ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    _buildNavRow(
                      icon: '🔄',
                      title: 'Tự động chuyển',
                      subtitle: 'Tự chuyển app khi sạc/mở app/bật màn hình',
                      onTap: () => Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const AutoSwitchRulesPage(),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 24),

                // === Sao lưu (Backup) ===
                _buildSectionTitle('💾 Sao lưu'),
                const SizedBox(height: 12),
                _buildGlassCard(
                  children: [
                    Text(
                      'Xuất hoặc nhập cài đặt ứng dụng dưới dạng JSON.',
                      style: TextStyle(
                        fontSize: 13,
                        color: Colors.black54,
                      ),
                    ),
                    const SizedBox(height: 16),
                    _buildActionButton(
                      label: 'Sao lưu cài đặt',
                      icon: Icons.upload,
                      onTap: _exportSettings,
                    ),
                    const SizedBox(height: 12),
                    _buildActionButton(
                      label: 'Khôi phục cài đặt',
                      icon: Icons.download,
                      onTap: _showRestoreDialog,
                    ),
                  ],
                ),
                const SizedBox(height: 24),

                // === Giới thiệu (About) ===
                _buildSectionTitle('ℹ️ Giới thiệu'),
                const SizedBox(height: 12),
                _buildGlassCard(
                  children: [
                    Row(
                      children: [
                        const Text(
                          '📱',
                          style: TextStyle(fontSize: 24),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Mi Rear Screen Switcher',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.black87,
                                ),
                              ),
                              Text(
                                'Phiên bản $_appVersion (build $_appBuild)',
                                style: TextStyle(
                                  fontSize: 13,
                                  color: Colors.black54,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    _buildNavRow(
                      icon: '🔄',
                      title: 'Kiểm tra cập nhật',
                      subtitle: 'Kiểm tra bản mới trên GitHub',
                      onTap: _checkUpdate,
                    ),
                    const SizedBox(height: 16),
                    const Divider(color: Colors.black26, height: 1),
                    const SizedBox(height: 16),
                    Text(
                      'Tác giả:',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: Colors.black87,
                      ),
                    ),
                    const SizedBox(height: 8),
                    _buildInfoRow('👨‍💻', l.translate('author_anti')),
                    const SizedBox(height: 8),
                    _buildInfoRow('🧪', l.translate('author_xmz')),
                    const SizedBox(height: 16),
                    const Divider(color: Colors.black26, height: 1),
                    const SizedBox(height: 16),
                    Text(
                      'Ứng dụng chuyển đổi ứng dụng giữa màn hình chính và màn hình sau trên thiết bị gập Xiaomi. '
                      'Cần Shizuku để hoạt động.',
                      style: TextStyle(
                        fontSize: 13,
                        color: Colors.black54,
                        height: 1.4,
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

  // === UI Builder helpers ===

  Widget _buildSectionTitle(String title) {
    return Padding(
      padding: const EdgeInsets.only(left: 4),
      child: Text(
        title,
        style: TextStyle(
          fontSize: 18,
          fontWeight: FontWeight.bold,
          color: Colors.white,
        ),
      ),
    );
  }

  Widget _buildGlassCard({required List<Widget> children}) {
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
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
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
    String? subtitle,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: () => onChanged(!value),
        borderRadius: BorderRadius.circular(SquircleRadii.small),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 8.0, horizontal: 4.0),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: Colors.black87,
                      ),
                    ),
                    if (subtitle != null) ...[
                      const SizedBox(height: 4),
                      Text(
                        subtitle,
                        style: const TextStyle(
                          fontSize: 12,
                          color: Colors.black54,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Switch(
                value: value,
                onChanged: onChanged,
                activeColor: const Color(0xFFFF9D88),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildLocaleRow(String name, String code, bool isSelected) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: () => widget.localeController.setLocale(code),
        borderRadius: BorderRadius.circular(SquircleRadii.small),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 10.0, horizontal: 4.0),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  name,
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight:
                        isSelected ? FontWeight.bold : FontWeight.normal,
                    color: Colors.black87,
                  ),
                ),
              ),
              if (isSelected)
                const Icon(Icons.check, size: 20, color: Color(0xFFFF9D88)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildActionButton({
    required String label,
    required IconData icon,
    required VoidCallback onTap,
  }) {
    return SizedBox(
      width: double.infinity,
      child: ClipPath(
        clipper: const SquircleClipper(cornerRadius: SquircleRadii.small),
        child: Container(
          decoration: const BoxDecoration(gradient: kBrandGradient),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: onTap,
              splashColor: Colors.white.withValues(alpha: 0.3),
              highlightColor: Colors.white.withValues(alpha: 0.2),
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 14),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(icon, color: Colors.white, size: 20),
                    const SizedBox(width: 8),
                    Text(
                      label,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildInfoRow(String emoji, String text) {
    return Row(
      children: [
        Text(emoji, style: const TextStyle(fontSize: 18)),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: const TextStyle(
              fontSize: 14,
              color: Colors.black87,
            ),
          ),
        ),
      ],
    );
  }

  // === Action handlers ===

  void _showSnackBar(String message) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(message)),
      );
    }
  }

  Widget _buildNavRow({
    required String icon,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(SquircleRadii.small),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 4),
          child: Row(
            children: [
              Text(icon, style: const TextStyle(fontSize: 22)),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        color: Colors.black87,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        fontSize: 12,
                        color: Colors.black54,
                      ),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: Colors.black38),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _checkUpdate() async {
    _showSnackBar('Đang kiểm tra cập nhật...');
    try {
      // Lấy version hiện tại từ package_info_plus
      final currentVersion = await _getAppVersion();
      final update = await UpdateChecker.check(currentVersion);
      if (update == null) {
        _showSnackBar('Bạn đang dùng phiên bản mới nhất (v$currentVersion)');
      } else {
        if (mounted) {
          showDialog(
            context: context,
            builder: (context) => AlertDialog(
              title: Text('Có bản cập nhật v${update.version}'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Kích thước: ${update.sizeLabel}'),
                  const SizedBox(height: 8),
                  if (update.releaseNotes.isNotEmpty)
                    ConstrainedBox(
                      constraints: const BoxConstraints(maxHeight: 200),
                      child: SingleChildScrollView(
                        child: Text(update.releaseNotes),
                      ),
                    ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Để sau'),
                ),
                TextButton(
                  onPressed: () {
                    Navigator.pop(context);
                    _openDownloadUrl(update.releaseUrl);
                  },
                  child: const Text('Tải xuống'),
                ),
              ],
            ),
          );
        }
      }
    } catch (e) {
      _showSnackBar('Kiểm tra cập nhật thất bại: $e');
    }
  }

  Future<String> _getAppVersion() async {
    return _appVersion;
  }

  void _openDownloadUrl(String url) {
    try {
      final platform = const MethodChannel('com.display.switcher/task');
      platform.invokeMethod('openUrl', {'url': url});
    } catch (_) {
      _showSnackBar('Mở link: $url');
    }
  }

  Future<void> _exportSettings() async {
    try {
      final json = await BackupService.exportSettings();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Đã xuất cài đặt (${json.length} bytes)'),
            duration: const Duration(seconds: 3),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi xuất cài đặt: $e')),
        );
      }
    }
  }

  Future<void> _showRestoreDialog() async {
    final controller = TextEditingController();
    final result = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Khôi phục cài đặt'),
        content: TextField(
          controller: controller,
          maxLines: 10,
          decoration: const InputDecoration(
            labelText: 'Dán JSON tại đây',
            hintText: '{"theme_mode": "dark", ...}',
            border: OutlineInputBorder(),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Hủy'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('Khôi phục'),
          ),
        ],
      ),
    );
    controller.dispose();

    if (result == null || result.trim().isEmpty) return;

    final success = await BackupService.importSettings(result);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            success
                ? 'Đã khôi phục cài đặt thành công'
                : 'Khôi phục thất bại — JSON không hợp lệ',
          ),
        ),
      );
    }
  }
}
