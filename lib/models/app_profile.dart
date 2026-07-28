/// App profile model — saves a snapshot of display settings.
class AppProfile {
  final String name;
  final String icon;
  final int dpi;
  final int rotation; // 0=0°, 1=90°, 2=180°, 3=270°
  final bool proximitySensor;
  final bool keepScreenOn;
  final bool alwaysWakeUp;
  final bool chargingAnimation;
  final bool chargingAlwaysOn;

  const AppProfile({
    required this.name,
    required this.icon,
    required this.dpi,
    required this.rotation,
    required this.proximitySensor,
    required this.keepScreenOn,
    required this.alwaysWakeUp,
    required this.chargingAnimation,
    required this.chargingAlwaysOn,
  });

  Map<String, dynamic> toJson() => {
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

  factory AppProfile.fromJson(Map<String, dynamic> json) => AppProfile(
        name: json['name'] as String,
        icon: json['icon'] as String? ?? '📱',
        dpi: json['dpi'] as int? ?? 320,
        rotation: json['rotation'] as int? ?? 0,
        proximitySensor: json['proximitySensor'] as bool? ?? true,
        keepScreenOn: json['keepScreenOn'] as bool? ?? true,
        alwaysWakeUp: json['alwaysWakeUp'] as bool? ?? false,
        chargingAnimation: json['chargingAnimation'] as bool? ?? true,
        chargingAlwaysOn: json['chargingAlwaysOn'] as bool? ?? false,
      );

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
      alwaysWakeUp: false,
      chargingAnimation: false,
      chargingAlwaysOn: false,
    ),
    AppProfile(
      name: 'Xem video',
      icon: '🎬',
      dpi: 320,
      rotation: 1, // 90°
      proximitySensor: false,
      keepScreenOn: true,
      alwaysWakeUp: false,
      chargingAnimation: false,
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
