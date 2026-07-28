import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:typed_data';
import '../l10n/app_localizations.dart';
import '../widgets/squircle.dart';
import '../widgets/gradient_widgets.dart';
import 'notification_settings_page.dart';

/// App selection page - choose which apps can send notifications to the rear screen.
class AppSelectionPage extends StatefulWidget {
  const AppSelectionPage({super.key});

  @override
  State<AppSelectionPage> createState() => _AppSelectionPageState();
}

class _AppSelectionPageState extends State<AppSelectionPage> {
  static const platform = MethodChannel('com.display.switcher/task');

  List<Map<String, dynamic>> _apps = [];
  List<Map<String, dynamic>> _visibleApps = [];
  Set<String> _selectedApps = {};
  bool _isLoading = true;
  bool _includeSystemApps = false;
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _loadApps();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _startPermissionCheckLoop() async {
    debugPrint('Starting permission check loop');
    int checkAttempts = 0;

    while (checkAttempts < 30 && mounted) {
      await Future.delayed(const Duration(seconds: 1));
      if (!mounted) break;

      try {
        final bool granted = await platform.invokeMethod(
          'checkQueryAllPackagesPermission',
        );
        if (granted) {
          debugPrint('Permission granted, refreshing app list');
          if (mounted) {
            setState(() => _isLoading = true);
            await _loadAppsInternal();
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(
                    AppLocalizations.of(context)
                        .translate('permission_granted_refresh'),
                  ),
                ),
              );
            }
          }
          return;
        }
      } catch (e) {
        debugPrint('Permission check failed: $e');
      }
      checkAttempts++;
    }

    debugPrint('Permission check timed out (30s)');
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            AppLocalizations.of(context).translate('grant_permission_manual'),
          ),
        ),
      );
    }
  }

  Future<void> _loadAppsInternal() async {
    try {
      final List<dynamic> selectedApps =
          await platform.invokeMethod('getSelectedNotificationApps');
      _selectedApps = selectedApps.cast<String>().toSet();

      final List<dynamic> apps = await platform.invokeMethod('getInstalledApps');

      setState(() {
        _apps = apps.map((app) => Map<String, dynamic>.from(app)).toList();
        _isLoading = false;
      });

      _applyFilters();
      debugPrint('Loaded ${_apps.length} apps');
    } catch (e) {
      debugPrint('Failed to load app list: $e');
      setState(() => _isLoading = false);
    }
  }

  void _applyFilters() {
    final String q = _searchController.text.trim().toLowerCase();
    List<Map<String, dynamic>> filtered = _apps.where((app) {
      final String name = (app['appName'] ?? '').toString().toLowerCase();
      final String pkg = (app['packageName'] ?? '').toString().toLowerCase();
      final bool matchesQuery = q.isEmpty || name.contains(q) || pkg.contains(q);
      if (!_includeSystemApps && _isSystemApp(app)) {
        return false;
      }
      return matchesQuery;
    }).toList();

    filtered.sort((a, b) {
      final String pkgA = a['packageName'] ?? '';
      final String pkgB = b['packageName'] ?? '';
      final bool selectedA = _selectedApps.contains(pkgA);
      final bool selectedB = _selectedApps.contains(pkgB);
      if (selectedA && !selectedB) return -1;
      if (!selectedA && selectedB) return 1;
      final String nameA = (a['appName'] ?? '').toString().toLowerCase();
      final String nameB = (b['appName'] ?? '').toString().toLowerCase();
      return nameA.compareTo(nameB);
    });

    setState(() {
      _visibleApps = filtered;
    });
  }

  bool _isSystemApp(Map<String, dynamic> app) {
    final pkg = (app['packageName'] ?? '').toString();
    final dynamic flag1 = app['isSystem'];
    final dynamic flag2 = app['isSystemApp'];
    if (flag1 == true || flag2 == true) return true;
    return pkg.startsWith('com.android.') ||
        pkg.startsWith('com.google.android.') ||
        pkg.startsWith('android');
  }

  Future<void> _selectAllVisible() async {
    setState(() {
      for (final app in _visibleApps) {
        final String pkg = app['packageName'];
        _selectedApps.add(pkg);
      }
    });
    _applyFilters();
    try {
      await platform.invokeMethod(
        'setSelectedNotificationApps',
        _selectedApps.toList(),
      );
    } catch (e) {
      debugPrint('Failed to save select-all: $e');
    }
  }

  Future<void> _deselectAllVisible() async {
    setState(() {
      for (final app in _visibleApps) {
        final String pkg = app['packageName'];
        _selectedApps.remove(pkg);
      }
    });
    _applyFilters();
    try {
      await platform.invokeMethod(
        'setSelectedNotificationApps',
        _selectedApps.toList(),
      );
    } catch (e) {
      debugPrint('Failed to save deselect-all: $e');
    }
  }

  Future<void> _loadApps() async {
    setState(() => _isLoading = true);

    try {
      debugPrint('Checking QUERY_ALL_PACKAGES permission...');
      final bool hasPermission = await platform.invokeMethod(
        'checkQueryAllPackagesPermission',
      );
      debugPrint('Permission check result: $hasPermission');

      if (!hasPermission) {
        debugPrint('No QUERY_ALL_PACKAGES permission, showing dialog');
        setState(() => _isLoading = false);

        if (mounted) {
          final shouldOpenSettings = await showDialog<bool>(
            context: context,
            builder: (context) => AlertDialog(
              title: Text(
                AppLocalizations.of(context)
                    .translate('no_permission_dialog_title'),
              ),
              content: Text(
                AppLocalizations.of(context)
                    .translate('no_permission_dialog_content'),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context, false),
                  child: Text(AppLocalizations.of(context).translate('cancel')),
                ),
                TextButton(
                  onPressed: () => Navigator.pop(context, true),
                  child: Text(
                    AppLocalizations.of(context).translate('go_to_settings'),
                  ),
                ),
              ],
            ),
          );

          if (shouldOpenSettings == true) {
            await platform.invokeMethod('requestQueryAllPackagesPermission');
            _startPermissionCheckLoop();
          }
        }
        return;
      }

      await _loadAppsInternal();
    } catch (e) {
      debugPrint('Failed to load app list: $e');
      setState(() => _isLoading = false);
    }
  }

  Future<void> _toggleApp(String packageName, bool selected) async {
    setState(() {
      if (selected) {
        _selectedApps.add(packageName);
      } else {
        _selectedApps.remove(packageName);
      }
    });
    _applyFilters();
    try {
      await platform.invokeMethod(
        'setSelectedNotificationApps',
        _selectedApps.toList(),
      );
    } catch (e) {
      debugPrint('Failed to save selection: $e');
    }
  }

  Widget _buildGradientButton({
    required String label,
    required VoidCallback onTap,
  }) {
    return ClipPath(
      clipper: const SquircleClipper(cornerRadius: SquircleRadii.small),
      child: Container(
        decoration: const BoxDecoration(gradient: kBrandGradient),
        child: Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: onTap,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              child: Text(
                label,
                style: const TextStyle(color: Colors.white),
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

    return Scaffold(
      appBar: AppBar(
        title: Text(
          '${l.translate('select_app_title')} (${_selectedApps.length})',
        ),
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        elevation: 0,
        scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent,
        shadowColor: Colors.transparent,
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const NotificationSettingsPage(),
                ),
              );
            },
            tooltip: l.translate('notification_settings_tooltip'),
          ),
        ],
      ),
      extendBodyBehindAppBar: true,
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: const BoxDecoration(gradient: kBrandGradient),
        child: SafeArea(
          child: _isLoading
              ? const Center(
                  child: CircularProgressIndicator(color: Colors.white),
                )
              : Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    children: [
                      // Filter & batch actions card
                      CustomPaint(
                        painter: const SquircleBorderPainter(
                          radius: 32,
                          color: Colors.white30,
                          strokeWidth: 1.5,
                        ),
                        child: ClipPath(
                          clipper: const SquircleClipper(
                              cornerRadius: SquircleRadii.large),
                          child: BackdropFilter(
                            filter: ImageFilter.blur(sigmaX: 0, sigmaY: 0),
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 20, vertical: 12),
                              decoration: BoxDecoration(
                                color: Colors.white.withOpacity(0.25),
                              ),
                              child: Column(
                                children: [
                                  TextField(
                                    controller: _searchController,
                                    onChanged: (_) => _applyFilters(),
                                    style: const TextStyle(
                                        color: Colors.black87),
                                    decoration: InputDecoration(
                                      hintText: l.translate('search_hint'),
                                      hintStyle: const TextStyle(
                                          color: Colors.black45),
                                      prefixIcon: Icon(Icons.search,
                                          color: Colors.black54),
                                      border: OutlineInputBorder(
                                        borderRadius: const BorderRadius.all(
                                          Radius.circular(
                                              SquircleRadii.small),
                                        ),
                                        borderSide:
                                            BorderSide(color: Colors.black26),
                                      ),
                                      enabledBorder: OutlineInputBorder(
                                        borderRadius: const BorderRadius.all(
                                          Radius.circular(
                                              SquircleRadii.small),
                                        ),
                                        borderSide:
                                            BorderSide(color: Colors.black26),
                                      ),
                                      focusedBorder: OutlineInputBorder(
                                        borderRadius: const BorderRadius.all(
                                          Radius.circular(
                                              SquircleRadii.small),
                                        ),
                                        borderSide: const BorderSide(
                                            color: Colors.black54, width: 2),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(height: 10),
                                  Row(
                                    children: [
                                      _buildGradientButton(
                                        label: l.translate('select_all'),
                                        onTap: _selectAllVisible,
                                      ),
                                      const SizedBox(width: 8),
                                      _buildGradientButton(
                                        label: l.translate('deselect_all'),
                                        onTap: _deselectAllVisible,
                                      ),
                                      const Spacer(),
                                      Text(
                                        l.translate('show_system_apps'),
                                        style: const TextStyle(
                                          color: Colors.black87,
                                          fontSize: 11,
                                        ),
                                      ),
                                      const SizedBox(width: 6),
                                      GradientToggle(
                                        value: _includeSystemApps,
                                        onChanged: (v) {
                                          setState(
                                              () => _includeSystemApps = v);
                                          _applyFilters();
                                        },
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                      Expanded(
                        child: ListView.builder(
                          itemCount: _visibleApps.length,
                          padding: const EdgeInsets.symmetric(vertical: 8),
                          itemExtent: 72,
                          cacheExtent: 500,
                          addAutomaticKeepAlives: false,
                          addRepaintBoundaries: true,
                          physics: const ClampingScrollPhysics(),
                          itemBuilder: (context, index) {
                            final app = _visibleApps[index];
                            final String appName = app['appName'];
                            final String packageName = app['packageName'];
                            final Uint8List? iconBytes = app['icon'];
                            final bool isSelected =
                                _selectedApps.contains(packageName);
                            return AppListItem(
                              appName: appName,
                              packageName: packageName,
                              iconBytes: iconBytes,
                              isSelected: isSelected,
                              onToggle: () =>
                                  _toggleApp(packageName, !isSelected),
                            );
                          },
                        ),
                      ),
                    ],
                  ),
                ),
        ),
      ),
    );
  }
}
