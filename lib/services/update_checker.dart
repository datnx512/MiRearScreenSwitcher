import 'dart:convert';
import 'package:http/http.dart' as http;

/// Checks GitHub Releases for newer versions.
class UpdateChecker {
  static const String _repoUrl =
      'https://api.github.com/repos/datnx512/MiRearScreenSwitcher/releases/latest';

  /// Check for updates. Returns UpdateInfo if newer version available, null otherwise.
  static Future<UpdateInfo?> check(String currentVersion) async {
    try {
      final response = await http.get(
        Uri.parse(_repoUrl),
        headers: {'Accept': 'application/vnd.github+json'},
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode != 200) return null;

      final data = jsonDecode(response.body) as Map<String, dynamic>;
      final tagName = data['tag_name'] as String? ?? '';
      final cleanTag = tagName.startsWith('v') ? tagName.substring(1) : tagName;

      if (_isNewer(cleanTag, currentVersion)) {
        final assets = data['assets'] as List<dynamic>?;
        String? downloadUrl;
        int? downloadSize;

        if (assets != null && assets.isNotEmpty) {
          final asset = assets[0] as Map<String, dynamic>;
          downloadUrl = asset['browser_download_url'] as String?;
          downloadSize = asset['size'] as int?;
        }

        return UpdateInfo(
          version: cleanTag,
          releaseUrl: data['html_url'] as String? ?? '',
          downloadUrl: downloadUrl,
          downloadSize: downloadSize,
          releaseNotes: data['body'] as String? ?? '',
          publishedAt: data['published_at'] as String? ?? '',
        );
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  /// Compare semantic versions. Returns true if remote > local.
  static bool _isNewer(String remote, String local) {
    final rParts = remote.split('.').map((e) => int.tryParse(e) ?? 0).toList();
    final lParts = local.split('.').map((e) => int.tryParse(e) ?? 0).toList();

    for (var i = 0; i < 3; i++) {
      final r = i < rParts.length ? rParts[i] : 0;
      final l = i < lParts.length ? lParts[i] : 0;
      if (r > l) return true;
      if (r < l) return false;
    }
    return false;
  }
}

class UpdateInfo {
  final String version;
  final String releaseUrl;
  final String? downloadUrl;
  final int? downloadSize;
  final String releaseNotes;
  final String publishedAt;

  const UpdateInfo({
    required this.version,
    required this.releaseUrl,
    this.downloadUrl,
    this.downloadSize,
    required this.releaseNotes,
    required this.publishedAt,
  });

  String get sizeLabel {
    if (downloadSize == null) return 'Unknown';
    final mb = downloadSize! / 1024 / 1024;
    return '${mb.toStringAsFixed(1)}MB';
  }
}
