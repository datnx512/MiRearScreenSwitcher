# Changelog

Tất cả thay đổi đáng chú ý của dự án MiRearScreenSwitcher.

## [Unreleased] — v3.3.0

### Thêm
- **Dark Mode**: Chế độ tối/sáng với gradient riêng, lưu preference
- **Language Picker**: Chọn ngôn ngữ in-app (Tiếng Việt, English, 简体中文, 繁體中文)
- **Onboarding**: Hướng dẫn 3 trang lần đầu khởi động (Shizuku + Quick Settings Tile)
- **Profiles**: 4 hồ sơ cài đặt (Mặc định, Đọc sách, Xem video, Tiết kiệm pin)
- **Backup/Restore**: Export/Import cài đặt ra JSON
- **Locale Việt**: 107 chuỗi Flutter + 19 chuỗi Android
- **Unit Tests**: 51 tests (app_profile, theme, locale, backup, widget)
- **ARCHITECTURE.md**: Sơ đồ kiến trúc + luồng dữ liệu
- **README_VI.md**: Tài liệu tiếng Việt
- **CI/CD**: Thêm `flutter analyze` + `flutter test` trước build

### Sửa
- Tách `main.dart` từ 3,736 dòng monolith → 7 file module (71 + 1,217 + 456 + 383 + 270 + 242)
- Sửa 12 empty catch blocks trong Java → thêm `Log.e(TAG, msg, e)`
- Thay 66 `print()` → `debugPrint()` trong Dart
- Thay tất cả `withOpacity()` → `withValues(alpha:)` (Flutter 3.29+)
- Dịch comment Java tiếng Trung → tiếng Việt (22 files)
- Xóa IDE artifacts (`.VSCodeCounter/`)
- Hardcoded `tooltip: '重启软件'` → i18n
- SDK constraint: `^3.8.1` → `^3.7.0` (tương thích server)

### Build
- `flutter analyze`: 0 issues ✅
- `flutter test`: 51/51 pass ✅
- `flutter build apk --debug`: thành công (186MB) ✅

## [3.2.0] — 2025-12-06 (upstream)

### Thêm
- Thông báo khi màn hình chính bị che
- Loại bỏ thông báo khi khóa màn hình

## [3.1.0] — 2025-10-27 (upstream)

### Thêm
- Hoạt ảnh sạc: icon tia sét
- Bỏ giới hạn 60s thời gian tự động xóa thông báo

### Sửa
- Lưu trạng thái công tắc thông báo sau重启
- Đồng bộ trạng thái dịch vụ thông báo

## [3.0.0] — 2025-10-16 (upstream)

### Thêm
- Giấy phép GPL-3.0
- Hoạt ảnh sạc 3D + hiệu ứng trọng lực
- Thông báo màn hình sau
- Quay màn hình sau
- URI Protocol (`mrss://`)
- Giao diện Material 3 + siêu椭圆
