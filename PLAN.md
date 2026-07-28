# Kế hoạch phát triển MiRearScreenSwitcher (MRSS)
*Dành riêng cho Xiaomi 17 Pro / 17 Pro Max*

## Tổng quan

MRSS là ứng dụng Flutter cho Xiaomi 17 Pro/Pro Max, cho phép chuyển ứng dụng sang màn hình sau 1 chạm qua Quick Settings Tile, không cần root (dùng Shizuku). Fork này tiếp tục phát triển với focus: **việt hóa toàn diện, nâng cao chất lượng code, thêm tính năng mới**.

## Trạng thái hiện tại

### Đã hoàn thành (session 1)
- ✅ Refactor `main.dart`: 3,736 dòng → 7 file (71 + 1,217 + 456 + 383 + 270 + 242 dòng)
- ✅ Việt hóa: 107 chuỗi Flutter `vi` + 19 chuỗi Android `values-vi/strings.xml`
- ✅ Sửa 12 empty catch blocks trong Java → thêm `Log.e()`
- ✅ Xóa IDE artifacts (`.VSCodeCounter/`), cập nhật `.gitignore`
- ✅ Thay 66 `print()` → `debugPrint()`
- ✅ Tạo `test/widget_test.dart` skeleton

### Chưa làm
- ❌ Chưa commit changes
- ❌ Chưa có Flutter SDK để build verify
- ❌ 785 hardcoded Chinese strings trong Java layer
- ❌ 0% test coverage
- ❌ Java files vẫn còn comment tiếng Trung

## Task List

### Phase 1: Stabilize & Commit
*Mục tiêu: Commit code, cài Flutter SDK, build pass*

- [ ] **Task 1.1**: Commit toàn bộ changes hiện tại
  - Stage tất cả: refactor + vi locale + Java fixes + cleanup
  - Commit: `refactor: split main.dart, add Vietnamese locale, fix Java catch blocks`

- [ ] **Task 1.2**: Cài Flutter SDK trên server
  - Download Flutter stable SDK
  - Add to PATH
  - Verify: `flutter --version`

- [ ] **Task 1.3**: Build verify lần đầu
  - `flutter pub get`
  - `flutter analyze` — fix warnings
  - `flutter build apk --debug`
  - Verify: APK tồn tại

- [ ] **Task 1.4**: Cập nhật CI/CD
  - Thêm `flutter analyze` + `flutter test` trước build trong workflow
  - Verify: CI config valid

### Checkpoint 1: Build pass
- [ ] `flutter analyze` 0 errors
- [ ] `flutter build apk --debug` thành công
- [ ] Đã commit và push

### Phase 2: Code Quality
*Mục tiêu: Dọn dẹp nợ kỹ thuật, thêm tests, dịch comment*

- [ ] **Task 2.1**: Dịch comment Java sang tiếng Việt
  - 22 Java files, ~300+ comment tiếng Trung
  - `// 背屏DPI相关` → `// DPI màn hình sau`
  - Giữ technical terms: Shizuku, AIDL, WakeLock
  - Verify: `grep -rn '//.*[^\x00-\x7F]' android/` → 0

- [ ] **Task 2.2**: Hardcoded Chinese strings trong Java → strings.xml
  - 785 chuỗi tiếng Trung trong Java layer
  - Ưu tiên: Toast, error, notification text
  - Thêm vào `values/strings.xml` + `values-vi/strings.xml`
  - Verify: hardcoded strings giảm >80%

- [ ] **Task 2.3**: Viết unit tests cho TaskService
  - Test `getCurrentForegroundApp()` parsing
  - Test DPI get/set/reset
  - Test rotation commands
  - File: `test/task_service_test.dart`

- [ ] **Task 2.4**: Viết widget tests cho HomePage
  - Test status card (running/error/checking)
  - Test DPI input validation
  - Test rotation button state
  - File: `test/home_page_test.dart`

- [ ] **Task 2.5**: Viết widget tests cho AppSelectionPage
  - Test app list loads
  - Test search filter
  - Test select/deselect all
  - File: `test/app_selection_page_test.dart`

### Checkpoint 2: Code Quality
- [ ] 0 comment tiếng Trung trong code
- [ ] Hardcoded strings giảm >80%
- [ ] Test coverage >30%
- [ ] `flutter analyze` 0 warnings
- [ ] `flutter test` all pass

### Phase 3: Tính năng mới
*Mục tiêu: Thêm tính năng thực dụng cho người dùng*

- [ ] **Task 3.1**: Dark Mode toggle
  - `ThemeData.dark()` + gradient tối
  - Lưu preference SharedPreferences
  - Material 3 dynamic color
  - Files: `main.dart`, `screens/home_page.dart`, new `theme_controller.dart`

- [ ] **Task 3.2**: Language picker in-app
  - Dropdown chọn VI/EN/ZH-CN/ZH-TW
  - Override system locale
  - File: new `screens/settings_page.dart`

- [ ] **Task 3.3**: Onboarding tutorial
  - First-launch guide: Shizuku, Quick Settings Tile
  - 3-4 trang swipeable
  - Lưu `onboarding_completed` flag
  - File: new `screens/onboarding_page.dart`

- [ ] **Task 3.4**: Widget profiles/presets
  - Lưu cấu hình DPI + rotation + features thành profile
  - Quick switch (vd: "Đọc sách", "Xem video", "Mặc định")
  - File: new `models/profile.dart`

- [ ] **Task 3.5**: Backup & Restore settings
  - Export settings → JSON
  - Import từ JSON
  - File: new `services/backup_service.dart`

### Checkpoint 3: Features
- [ ] Dark mode hoạt động
- [ ] Language picker hoạt động
- [ ] Onboarding hiển thị first-launch
- [ ] Profiles tạo/switch/xóa được
- [ ] Backup/Restore round-trip OK

### Phase 4: Polish & Release
*Mục tiêu: Hoàn thiện, sẵn sàng release*

- [ ] **Task 4.1**: Performance optimization
  - Profile với Flutter DevTools
  - Optimize ListView.builder
  - Reduce rebuilds với const widgets
  - Verify: 60fps scrolling

- [ ] **Task 4.2**: Accessibility
  - Semantics labels cho icon buttons
  - Contrast ratio AA
  - Support TalkBack
  - Verify: accessibility checks pass

- [ ] **Task 4.3**: Error handling & crash reporting
  - Wrap tất cả platform calls
  - Error boundary widget
  - Verify: simulate errors → graceful handling

- [ ] **Task 4.4**: Cập nhật docs
  - README tiếng Việt
  - CONTRIBUTING.md
  - ARCHITECTURE.md (diagram + module map)
  - CHANGELOG.md

- [ ] **Task 4.5**: Release build
  - Bump version: 3.3.0+10
  - Build release APK
  - Tag git: `v3.3.0`

### Checkpoint 4: Release Ready
- [ ] Performance OK
- [ ] Accessibility AA
- [ ] Error handling comprehensive
- [ ] Docs cập nhật
- [ ] Release APK built, git tagged

## Rủi ro & Giảm thiểu

| Rủi ro | Mức | Giải pháp |
|---|---|---|
| Shizuku API thay đổi | TB | Pin version, test với nhiều bản |
| Xiaomi khóa quyền shell | Cao | Theo dõi MIUI/HyperOS updates |
| Không có thiết bị test | Cao | Emulator + community feedback |

## Open Questions
- [ ] Có nên thêm root-based fallback khi Shizuku không khả dụng?
- [ ] Publish lên Google Play hay chỉ GitHub Releases?
- [ ] Có cần thêm tính năng ghi log/debug cho troubleshooting?
