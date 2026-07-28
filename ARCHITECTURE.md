# Kiến trúc MRSS

## Tổng quan

```
┌─────────────────────────────────────────────────┐
│  Flutter UI Layer (Dart)                        │
│                                                 │
│  main.dart → MaterialApp                        │
│    ├── OnboardingPage (first-launch)            │
│    └── HomePage                                 │
│         ├── AppSelectionPage                    │
│         └── NotificationSettingsPage            │
│                                                 │
│  Services: ThemeController, LocaleController,    │
│            BackupService                         │
│  Models: AppProfile                              │
│  Widgets: Squircle, GradientToggle, etc.        │
└──────────────────┬──────────────────────────────┘
                   │ MethodChannel
                   ▼
┌─────────────────────────────────────────────────┐
│  Android Native Layer (Java)                   │
│                                                 │
│  MainActivity (FlutterActivity)                 │
│    ├── Shizuku binder lifecycle                 │
│    ├── Permission management                     │
│    └── MethodChannel handler                     │
│                                                 │
│  TaskService (chạy trong Shizuku process)       │
│    ├── Shell privilege: am, service call        │
│    ├── getCurrentForegroundApp()                 │
│    ├── switchToRearDisplay()                     │
│    └── setRearDpi(), setDisplayRotation()        │
│                                                 │
│  Quick Settings Tiles:                          │
│    ├── SwitchToRearTileService                  │
│    ├── RearScreenshotTileService                │
│    └── RearScreenRecordTileService              │
│                                                 │
│  Background Services:                           │
│    ├── RearScreenKeeperService (giữ sáng)       │
│    ├── ChargingService (hoạt ảnh sạc)           │
│    ├── NotificationService (thông báo)          │
│    ├── AlwaysWakeUpService                      │
│    └── ScreenRecordService                      │
│                                                 │
│  Activities:                                    │
│    ├── RearScreenChargingActivity               │
│    ├── RearScreenNotificationActivity           │
│    ├── RearScreenWakeActivity                   │
│    └── UriReceiverActivity (mrss:// protocol)   │
└─────────────────────────────────────────────────┘
```

## Luồng dữ liệu

1. **User toggle** → Flutter UI → MethodChannel → MainActivity → AIDL → TaskService (Shizuku) → Shell command
2. **Quick Settings Tile** → TileService → TaskService (AIDL) → Shell command
3. **Charging event** → BroadcastReceiver → ChargingService → RearScreenChargingActivity (overlay)
4. **Notification** → NotificationListenerService → RearScreenNotificationActivity (overlay)
5. **URI Protocol** (`mrss://`) → UriReceiverActivity → UriCommandService

## Shizuku Integration

- `ITaskService.aidl` định nghĩa interface AIDL
- `TaskService` chạy trong process Shizuku, có shell privilege
- `MainActivity` bind/unbind qua `Shizuku.UserServiceArgs`
- Tự động reconnect khi binder dead

## Theme System

- `ThemeController`: quản lý light/dark, persist vào SharedPreferences
- Brand gradient: 4 màu (coral → pink → purple → blue)
- Dark gradient: phiên bản đậm hơn
- `currentGradient` getter động theo theme

## Localization System

- `LocaleController`: quản lý locale, persist vào SharedPreferences
- 4 ngôn ngữ: vi, en, zh_CN, zh_TW (+ fr, de, es, ja, ko trong app_strings)
- `AppStrings` map: key → {locale → string}
- `translate(key)` resolves locale → string với fallback to English
