# Kế hoạch nâng cấp MRSS v3.5+
*Mục tiêu: Biến MRSS từ công cụ chuyển màn hình thành trung tâm điều khiển màn hình sau*

## Hiện trạng v3.4.1

### Đã có
- Chuyển app sang màn hình sau (Quick Settings Tile)
- Chụp màn hình sau, quay màn hình sau
- Hoạt ảnh sạc 3D
- Thông báo màn hình sau (NotificationListenerService)
- DPI / Rotation / Proximity / Keep-awake
- Dark Mode, Language Picker, Onboarding, Profiles, Backup
- 51 tests, flutter analyze 0 issues

### Đang thiếu
- Không có widget clock/đồng hồ cho màn hình sau
- Không có控制 nhạc khi app đang ở màn hình sau
- Profiles chưa wire vào UI (chỉ có model)
- Không có auto-switch theo sự kiện (mở nắp, sạc, giờ)
- Không có thống kê sử dụng
- Backup/Restore chưa có nút trong UI
- Không có tùy chỉnh hoạt ảnh sạc

---

## Phase 5: Hoàn thiện UI hiện tại (1-2 ngày)

### 5.1 Wire App Profiles vào HomePage
- Thêm horizontal scrollable profile chips ở đầu trang
- Nhấn chip → áp dụng profile (DPI + rotation + toggles)
- Nhấn giữ chip → xóa/sửa profile
- Nút "+" để tạo profile từ cấu hình hiện tại
- **Files:** `screens/home_page.dart`, `models/app_profile.dart`
- **Verify:** Tạo profile → switch → verify settings thay đổi

### 5.2 Wire Backup/Restore vào UI
- Thêm mục "Sao lưu" và "Khôi phục" trong Settings
- Backup → lưu JSON ra Downloads
- Restore → chọn JSON từ file picker
- **Files:** `screens/settings_page.dart` (mới), `services/backup_service.dart`
- **Verify:** Backup → clear → restore → settings khôi phục

### 5.3 Trang Settings riêng
- Tách Dark Mode, Language, Backup, About ra trang Settings riêng
- HomePage chỉ giữ các toggle hiển thị
- **Files:** `screens/settings_page.dart` (mới), `screens/home_page.dart`
- **Verify:** Navigate HomePage → Settings → các tùy chọn hoạt động

---

## Phase 6: Tính năng màn hình sau mới (3-5 ngày)

### 6.1 Đồng hồ màn hình sau 🕐
- Hiển thị đồng hồ lớn trên màn hình sau khi không có app nào
- Chọn style: Digital / Analog / Minimalist
- Hiển thị ngày, pin, thông báo gần nhất
- **Java:** `RearScreenClockActivity.java` (mới) — overlay activity
- **Dart:** `screens/clock_settings_page.dart` (mới)
- **Shell:** Không cần — dùng WindowManager overlay
- **Verify:** Bật đồng hồ → màn hình sau hiển thị giờ

### 6.2 Media Controller màn hình sau 🎵
- Khi app nhạc đang chạy, hiện điều khiển trên màn hình sau
- Play/Pause/Next/Previous + thanh tiến trình
- Hiển thị tên bài + album art
- **Java:** `RearScreenMediaActivity.java` (mới) — MediaSessionManager listener
- **Dart:** `screens/media_settings_page.dart` (mới)
- **Verify:** Mở Spotify → màn hình sau hiện media controller

### 6.3 Custom charging animation 🎨
- Cho phép chọn kiểu hoạt ảnh sạc: Lightning / Wave / Pulse / Minimal
- Tùy chỉnh màu sắc gradient
- Tốc độ animation
- **Java:** `LightningShapeView.java` (sửa), `ChargingService.java` (sửa)
- **Dart:** `screens/charging_settings_page.dart` (mới)
- **Verify:** Đổi style → cắm sạc → animation mới hiển thị

### 6.4 Auto-switch rules ⚡
- Tự động chuyển app sang màn hình sau theo điều kiện:
  - Khi mở nắp/đóng nắp (phát hiện qua sensor)
  - Khi cắm sạc
  - Khi theo giờ (vd: 22:00 → tự bật Đọc sách)
  - Khi mở app cụ thể (vd: mở Kindle → tự chuyển sau)
- **Java:** `AutoSwitchService.java` (mới) — lắng nghe events
- **Dart:** `screens/auto_switch_rules_page.dart` (mới), `models/switch_rule.dart` (mới)
- **Verify:** Tạo rule "khi sạc → chuyển Kindle sang sau" → cắm sạc → Kindle hiện sau

---

## Phase 7: Thông minh hơn (3-5 ngày)

### 7.1 Notification widgets màn hình sau 📢
- Thay vì chỉ text, hiển thị notification dạng widget:
  - Notification lớn + nút action (Reply, Mark as read)
  - Stack notifications (vuốt để xem tiếp)
  - Notification priority (quan trọng hiện trước)
- **Java:** `RearScreenNotificationActivity.java` (sửa đáng kể)
- **Dart:** `screens/notification_settings_page.dart` (sửa)
- **Verify:** Nhận nhiều notifications → stack hiển thị + vuốt

### 7.2 Thống kê sử dụng 📊
- Thống kê thời gian dùng màn hình sau theo app
- Thống kê số lần switch, số notification
- Biểu đồ theo ngày/tuần
- **Dart:** `screens/usage_stats_page.dart` (mới), `services/usage_tracker.dart` (mới)
- **Java:** Thêm logging trong `TaskService.java`
- **Verify:** Dùng app 1 ngày → mở thống kê → thấy data

### 7.3 Smart suggestions 💡
- Gợi ý profile dựa trên thói quen sử dụng
- "Bạn hay dùng Kindle ở màn hình sau vào buổi tối" → gợi ý tạo profile tự động
- Phân tích usage data → đề xuất rules
- **Dart:** `services/suggestion_engine.dart` (mới)
- **Verify:** Dùng 3 ngày → hiện suggestion

---

## Phase 8: Cải thiện kỹ thuật (2-3 ngày)

### 8.1 Root fallback mode
- Khi Shizuku không khả dụng, kiểm tra root
- Nếu có root → chạy shell commands trực tiếp qua `su`
- Toggle trong Settings: "Root mode (không cần Shizuku)"
- **Java:** `RootService.java` (mới) — thay thế TaskService khi root
- **Dart:** `services/root_detector.dart` (mới)
- **Verify:** Tắt Shizuku → bật Root mode → vẫn chuyển màn hình được

### 8.2 Crash reporting & debug log
- Ghi log vào file khi có lỗi
- Button "Gửi log" → export log ra file
- Optional: Firebase Crashlytics (opt-in)
- **Dart:** `services/logger.dart` (mới)
- **Java:** `DebugLogger.java` (mới)
- **Verify:** Gây lỗi → mở log → thấy stack trace

### 8.3 Performance: reduce rebuilds
- Dùng `const` constructors everywhere
- Tách `Consumer` widgets cho controllers
- Profile với Flutter DevTools
- Mục tiêu: 60fps scrolling, <100ms cold start
- **Verify:** DevTools timeline ≤ 16ms/frame

---

## Phase 9: Polish & Distribution (1-2 ngày)

### 9.1 App icon & splash screen
- Custom app icon (thay小米icon)
- Splash screen với brand gradient
- Adaptive icon (foreground + background)
- **Files:** `android/app/src/main/res/mipmap-*`, `drawable/launch_background.xml`

### 9.2 In-app update checker
- Kiểm tra GitHub Releases có bản mới không
- Hiển thị dialog "Có bản cập nhật v3.5.0"
- Nút "Tải xuống" → mở browser
- **Dart:** `services/update_checker.dart` (mới)

### 9.3 Google Play preparation (optional)
- Setup signing key
- Store listing (screenshots, description)
- Privacy policy URL
- Content rating questionnaire

---

## Tóm tắt ưu tiên

| Phase | Tính năng | Mức ưu tiên | Thời gian |
|---|---|---|---|
| 5.1 | Wire Profiles vào UI | 🔴 Cao | 4h |
| 5.2 | Wire Backup/Restore vào UI | 🔴 Cao | 2h |
| 5.3 | Trang Settings riêng | 🔴 Cao | 3h |
| 6.1 | Đồng hồ màn hình sau | 🟡 TB | 1 ngày |
| 6.2 | Media Controller | 🟡 TB | 1 ngày |
| 6.3 | Custom charging animation | 🟡 TB | 1 ngày |
| 6.4 | Auto-switch rules | 🟢 Thấp | 1 ngày |
| 7.1 | Notification widgets | 🟢 Thấp | 1 ngày |
| 7.2 | Thống kê sử dụng | 🟢 Thấp | 1 ngày |
| 7.3 | Smart suggestions | 🟢 Thấp | 1 ngày |
| 8.1 | Root fallback | 🟡 TB | 1 ngày |
| 8.2 | Crash reporting | 🟡 TB | 4h |
| 8.3 | Performance | 🟢 Thấp | 4h |
| 9.1 | App icon & splash | 🟡 TB | 4h |
| 9.2 | Update checker | 🟢 Thấp | 2h |
| 9.3 | Google Play | ⚪ Optional | 1 ngày |

## Rủi ro

| Rủi ro | Mức | Giải pháp |
|---|---|---|
| Xiaomi chặn overlay activity | Cao | Dùng TYPE_APPLICATION_OVERLAY + miui.rear.policy meta-data |
| MediaSessionManager không hoạt động trên MIUI | TB | Test trên thiết bị thật, fallback read NotificationListener |
| Root mode bị Magisk hide | Thấp | Detect su binary + request root via libsu |
| Auto-switch tốn pin | TB | Chỉ poll khi màn hình sau ON, dùng BroadcastReceiver thay vì polling |

## Open Questions
- [ ] Đồng hồ màn hình sau: overlay activity hay widget? → **Activity (overlay)**
- [ ] Media controller: dùng MediaSessionManager hay parse NotificationListener? → **MediaSessionManager trước, fallback sau**
- [ ] Root mode: dùng libsu hay Runtime.exec("su")? → **Runtime.exec đơn giản hơn**
- [ ] Có nên thêm widget clock cho màn hình khóa? → **Chờ feedback**
