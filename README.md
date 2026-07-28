# MRSS — MiRearScreenSwitcher

Ứng dụng chuyển màn hình sau cho Xiaomi 17 Pro / 17 Pro Max. Không cần root, dùng Shizuku.

## Tính năng

- 🎯 Chuyển ứng dụng sang màn hình sau 1 chạm qua Quick Settings Tile
- 📸 Chụp màn hình sau
- 📹 Quay màn hình sau
- ⚡ Hiển thị hoạt ảnh sạc trên màn hình sau
- 📢 Hiển thị thông báo trên màn hình sau
- 🔄 Xoay màn hình sau (0°/90°/180°/270°)
- 📱 Điều chỉnh DPI màn hình sau
- 🤚 Phát hiện che màn hình sau
- 💡 Giữ màn hình sau luôn sáng
- 🌙 Chế độ tối / sáng
- 🌐 Đa ngôn ngữ: Tiếng Việt, English, 简体中文, 繁體中文
- 📋 Hồ sơ cài đặt (Mặc định, Đọc sách, Xem video, Tiết kiệm pin)
- 💾 Sao lưu/Khôi phục cài đặt

## Yêu cầu

1. **Thiết bị**: Xiaomi 17 Pro / 17 Pro Max (màn hình sau)
2. **Shizuku**: Cài đặt và khởi động Shizuku
   - Tải tại: [shizuku.rikka.app](https://shizuku.rikka.app/)
   - Khởi động qua ADB hoặc gỡ lỗi không dây

## Cài đặt

### Từ GitHub Releases
1. Tải APK mới nhất từ [Releases](https://github.com/datnx512/MiRearScreenSwitcher/releases)
2. Cài đặt APK
3. Mở MRSS, cấp quyền Shizuku

### Build từ source
```bash
flutter pub get
flutter build apk --release --split-per-abi --target-platform android-arm64
```
APK tại: `build/app/outputs/flutter-apk/app-arm64-v8a-release.apk`

## Cấu trúc dự án

```
lib/
├── main.dart                    # Entry point + MaterialApp
├── l10n/
│   ├── app_localizations.dart   # Locale delegate
│   └── app_strings.dart         # 107 chuỗi × 9 ngôn ngữ
├── models/
│   └── app_profile.dart          # Profile presets
├── screens/
│   ├── home_page.dart           # Trang chủ
│   ├── app_selection_page.dart   # Chọn ứng dụng thông báo
│   ├── notification_settings_page.dart
│   └── onboarding_page.dart     # Hướng dẫn lần đầu
├── services/
│   ├── theme_controller.dart    # Dark/Light mode
│   ├── locale_controller.dart   # Language picker
│   └── backup_service.dart      # Export/Import JSON
└── widgets/
    ├── squircle.dart            # Superellipse shapes
    └── gradient_widgets.dart    # Toggle, Checkbox, AppListItem

android/app/src/main/java/
├── MainActivity.java            # FlutterActivity + Shizuku
├── TaskService.java             # Shell commands (qua Shizuku)
├── SwitchToRearTileService.java # QS Tile: chuyển màn hình
├── RearScreenshotTileService.java
├── ChargingService.java         # Hoạt ảnh sạc
├── NotificationService.java     # Thông báo màn hình sau
└── ...
```

## Giấy phép

GPL-3.0 (từ V3.0.0 trở đi)

## Ghi nhận

- Tác giả gốc: [AntiOblivionis](https://github.com/GoldenglowSusie/)
- Fork và phát triển bởi: [datnx512](https://github.com/datnx512/)
