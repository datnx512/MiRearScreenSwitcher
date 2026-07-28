import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';

/// Lightweight logger that writes to file + debugPrint.
/// Logs survive app restarts for troubleshooting.
class AppLogger {
  static final AppLogger _instance = AppLogger._();
  factory AppLogger() => _instance;
  AppLogger._();

  static const int _maxFileSize = 2 * 1024 * 1024; // 2MB
  static const String _logFileName = 'mrss_debug.log';

  File? _logFile;
  bool _initialized = false;

  Future<void> init() async {
    if (_initialized) return;
    try {
      final dir = await getApplicationDocumentsDirectory();
      _logFile = File('${dir.path}/$_logFileName');
      _initialized = true;
      // Rotate log if too large
      if (_logFile!.existsSync()) {
        final size = await _logFile!.length();
        if (size > _maxFileSize) {
          final oldContent = await _logFile!.readAsString();
          final lines = oldContent.split('\n');
          final keepLines = lines.sublist(lines.length ~/ 2);
          await _logFile!.writeAsString(keepLines.join('\n'));
        }
      }
      log('═══════ MRSS Log Started ═══════');
    } catch (e) {
      debugPrint('Logger init failed: $e');
    }
  }

  void log(String message, {String level = 'INFO'}) {
    final timestamp = DateTime.now().toIso8601String();
    final line = '[$timestamp] [$level] $message';
    debugPrint(line);
    _writeToFile(line);
  }

  void error(String message, [Object? error, StackTrace? stackTrace]) {
    final timestamp = DateTime.now().toIso8601String();
    var line = '[$timestamp] [ERROR] $message';
    if (error != null) line += '\n  Error: $error';
    if (stackTrace != null) line += '\n  Stack: $stackTrace';
    debugPrint(line);
    _writeToFile(line);
  }

  void warning(String message) {
    log(message, level: 'WARN');
  }

  void _writeToFile(String line) {
    if (_logFile == null) return;
    try {
      _logFile!.writeAsStringSync('$line\n', mode: FileMode.append);
    } catch (_) {
      // Silent fail — logging should never crash the app
    }
  }

  /// Get the full log content for export
  Future<String> getLogContent() async {
    if (_logFile == null || !_logFile!.existsSync()) return '';
    return _logFile!.readAsString();
  }

  /// Get log file path for sharing
  Future<String?> getLogPath() async {
    return _logFile?.path;
  }

  /// Clear all logs
  Future<void> clearLogs() async {
    if (_logFile != null && _logFile!.existsSync()) {
      await _logFile!.writeAsString('');
    }
    log('Logs cleared');
  }
}
