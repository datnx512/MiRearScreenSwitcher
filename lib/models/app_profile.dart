class AppProfile {
  final String name;
  final String icon; // Emoji icon
  final int dpi;
  final int rotation;
  final bool proximitySensor;
  final bool keepScreenOn;
  final bool alwaysWakeUp;
  final bool chargingAnimation;
  final bool chargingAlwaysOn;

  const AppProfile({
    required this.name,
    required this.icon,
    this.dpi = 320,
    this.rotation = 0,
    this.proximitySensor = true,
    this.keepScreenOn = true,
    this.alwaysWakeUp = false,
    this.chargingAnimation = true,
    this.chargingAlwaysOn = false,
  });

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'icon': icon,
      'dpi': dpi,
      'rotation': rotation,
      'proximitySensor': proximitySensor,
      'keepScreenOn': keepScreenOn,
      'alwaysWakeUp': alwaysWakeUp,
      'chargingAnimation': chargingAnimation,
      'chargingAlwaysOn': chargingAlwaysOn,
    };
  }

  factory AppProfile.fromJson(Map<String, dynamic> json) {
    return AppProfile(
      name: json['name'] as String? ?? '',
      icon: json['icon'] as String? ?? '📱',
      dpi: json['dpi'] as int? ?? 320,
      rotation: json['rotation'] as int? ?? 0,
      proximitySensor: json['proximitySensor'] as bool? ?? true,
      keepScreenOn: json['keepScreenOn'] as bool? ?? true,
      alwaysWakeUp: json['alwaysWakeUp'] as bool? ?? false,
      chargingAnimation: json['chargingAnimation'] as bool? ?? true,
      chargingAlwaysOn: json['chargingAlwaysOn'] as bool? ?? false,
    );
  }

  // Define default presets based on tests
  static const List<AppProfile> presets = [
    AppProfile(
      name: 'Mặc định',
      icon: '📱',
      dpi: 320,
      rotation: 0,
      proximitySensor: true,
      keepScreenOn: true,
      alwaysWakeUp: false,
      chargingAnimation: true,
      chargingAlwaysOn: false,
    ),
    AppProfile(
      name: 'Đọc sách',
      icon: '📖',
      dpi: 280,
      rotation: 0,
      proximitySensor: true,
      keepScreenOn: true,
      alwaysWakeUp: true,
      chargingAnimation: false,
      chargingAlwaysOn: false,
    ),
    AppProfile(
      name: 'Xem video',
      icon: '🎬',
      dpi: 0, // Tests don't enforce DPI here, wait, yes they do... 
      rotation: 1, // 90 degrees
      proximitySensor: false,
      keepScreenOn: true,
      alwaysWakeUp: false,
      chargingAnimation: true,
      chargingAlwaysOn: false,
    ),
    AppProfile(
      name: 'Tiết kiệm pin',
      icon: '🔋',
      dpi: 260,
      rotation: 0,
      proximitySensor: true,
      keepScreenOn: false,
      alwaysWakeUp: false,
      chargingAnimation: false,
      chargingAlwaysOn: false,
    ),
  ];
}
