/*
 * Author: AntiOblivionis
 * QQ: 319641317
 * Github: https://github.com/GoldenglowSusie/
 * Bilibili: Rhodes Island T0 Thuật sư điều khiển cơ giới Chengshan
 *
 * Chief Tester: Ximuze
 *
 * Co-developed with AI assistants:
 * - Cursor
 * - Claude-4.5-Sonnet
 * - GPT-5
 * - Gemini-2.5-Pro
 */

package com.tgwgroup.MiRearScreenSwitcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import rikka.shizuku.Shizuku;

/**
 * thông báodịch vụ lắng nghe
 * lắng nghehệ thốngthông báo, sẽchọntrongứng dụngthông báohiển thịđếnmàn hình sau
 */
public class NotificationService extends NotificationListenerService {
    private static final String TAG = "NotificationService";
    private static final int NOTIFICATION_ID = 1001; // và Service khác cùng dùng ID
    
    private Set<String> selectedApps = new HashSet<>();
    private boolean privacyHideTitle = false; // V3.2: ẩnchế độ - ẩntiêu đề
    private boolean privacyHideContent = false; // V3.2: ẩnchế độ - ẩntrongchứa
    private boolean followDndMode = true; // theohệ thốngKhông làm phiềnchế độ（mặc địnhbật）
    private boolean onlyWhenLocked = false; // úp màn hìnhtaythời gianthông báo（mặc địnhđóng）
    private boolean notificationDarkMode = false; // thông báochế độ tối（mặc địnhđóng）
    private boolean serviceEnabled = false; // dịch vụcóbật
    private ITaskService taskService; // tựTaskServiceinstance
    private SharedPreferences prefs;
    private PowerManager.WakeLock wakeLock;
    
    // màn hình chínhcảm biến tiệm cậntươngkey
    private SensorManager sensorManager;
    private Sensor mainProximitySensor; // màn hình chínhcảm biến tiệm cận
    private boolean isMainScreenCovered = false; // màn hình chínhcóbị
    
    // instance static, chongoàibộ phậntruy cập
    private static NotificationService instance;
    
    public static ITaskService getTaskService() {
        return instance != null ? instance.taskService : null;
    }
    
    // broadcast receiver：lắng nghecài đặttải lại
    private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.tgwgroup.MiRearScreenSwitcher.RELOAD_NOTIFICATION_SETTINGS".equals(intent.getAction())) {
                Log.d(TAG, "🔄 收到重新加载设置的广播");
                loadNotificationServiceSettings(); // tải lại trạng thái công tắc
                loadSettings(); // tải lạikháccài đặt
            }
        }
    };
    
    // Shizukudịch vụcấu hình
    private final Shizuku.UserServiceArgs serviceArgs = 
        new Shizuku.UserServiceArgs(new ComponentName("com.tgwgroup.MiRearScreenSwitcher", TaskService.class.getName()))
            .daemon(false)
            .processNameSuffix("notification_task_service")
            .debuggable(false)
            .version(1);
    
    // TaskService kết nối
    private final ServiceConnection taskServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "✓ TaskService connected");
            taskService = ITaskService.Stub.asInterface(binder);
            
            // khởi tạocache thông tin màn hình
            try {
                DisplayInfoCache.getInstance().initialize(taskService);
            } catch (Exception e) {
                Log.w(TAG, "初始化显示屏缓存失败: " + e.getMessage());
            }
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "✗ TaskService disconnected");
            taskService = null;
            // tự động kết nối lại
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (taskService == null) {
                    bindTaskService();
                }
            }, 1000);
        }
    };
    
    // Shizuku listener
    private final Shizuku.OnBinderReceivedListener binderReceivedListener = 
        () -> {
            Log.d(TAG, "Shizuku binder received");
            bindTaskService();
        };
    
    private final Shizuku.OnBinderDeadListener binderDeadListener = 
        () -> {
            Log.d(TAG, "Shizuku binder dead");
            taskService = null;
            // thử kết nối lại
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                bindTaskService();
            }, 1000);
        };
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🟢 NotificationService created");
        
        // lưu instance
        instance = this;
        
        // khởi tạoSharedPreferences
        prefs = getSharedPreferences("mrss_settings", Context.MODE_PRIVATE);
        
        // đăng kýbroadcast receiver（lắng nghethay đổi cài đặt）
        IntentFilter filter = new IntentFilter("com.tgwgroup.MiRearScreenSwitcher.RELOAD_NOTIFICATION_SETTINGS");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(settingsReceiver, filter);
        }
        Log.d(TAG, "✓ 广播接收器已注册");
        
        // thêm Shizuku listener
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        
        // bind TaskService
        bindTaskService();
        
        // V2.4: tảithông báodịch vụtrạng thái công tắc
        Log.d(TAG, "🔧 开始加载通知服务开关状态...");
        loadNotificationServiceSettings();
        Log.d(TAG, "🔧 通知服务开关状态加载完成: " + serviceEnabled);
        
        // khởi tạomàn hình chínhcảm biến tiệm cận
        initMainProximitySensor();
        
        // khởi độnglàforeground service, ngăn chặnbịhệ thốngkill
        startForeground(NOTIFICATION_ID, RearScreenKeeperService.createServiceNotification(this));
        Log.d(TAG, "✓ 前台服务已启动");
        
        loadSettings();
    }
    
    private void bindTaskService() {
        try {
            if (taskService != null) {
                Log.d(TAG, "TaskService already bound");
                return;
            }
            
            if (!Shizuku.pingBinder()) {
                Log.w(TAG, "Shizuku not available");
                return;
            }
            
            Log.d(TAG, "🔗 开始绑定TaskService...");
            Shizuku.bindUserService(serviceArgs, taskServiceConnection);
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind TaskService", e);
        }
    }
    
    /**
 * tảithông báodịch vụtrạng thái công tắc
 */
    private void loadNotificationServiceSettings() {
        try {
            Log.d(TAG, "🔧 开始读取FlutterSharedPreferences...");
            // từFlutterSharedPreferencestrạng thái công tắc
            SharedPreferences flutterPrefs = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE);
            Log.d(TAG, "🔧 FlutterSharedPreferences读取成功");
            
            serviceEnabled = flutterPrefs.getBoolean("flutter.notification_service_enabled", false);
            Log.d(TAG, "🔧 通知服务开关状态已恢复: " + serviceEnabled);
            
            // NotificationListenerServicehệ thốngquản lý, khôngcó thểthủ côngdừng
            // nếucông tắcđóng, dịch vụsẽnhưngkhôngxử lýthông báo
            if (!serviceEnabled) {
                Log.d(TAG, "⏸️ 通知服务已禁用，将忽略所有通知");
            } else {
                Log.d(TAG, "✅ 通知服务已启用，将处理通知");
            }
        } catch (Exception e) {
            Log.e(TAG, "✗ 加载通知服务设置失败", e);
            serviceEnabled = false; // mặc địnhđóng
        }
    }
    
    private void loadSettings() {
        try {
            selectedApps = prefs.getStringSet("notification_selected_apps", new HashSet<>());
            privacyHideTitle = prefs.getBoolean("notification_privacy_hide_title", false);
            privacyHideContent = prefs.getBoolean("notification_privacy_hide_content", false);
            followDndMode = prefs.getBoolean("notification_follow_dnd_mode", true);
            onlyWhenLocked = prefs.getBoolean("notification_only_when_locked", false);
            notificationDarkMode = prefs.getBoolean("notification_dark_mode", false);
            // chú ý：khôngởnàylàm lạicài đặt serviceEnabled, giữ loadNotificationServiceSettings() giá trị
            
            Log.d(TAG, "⚙️ 已加载设置");
            Log.d(TAG, "   - 启用状态: " + serviceEnabled + " (由loadNotificationServiceSettings设置)");
            Log.d(TAG, "   - 选中应用: " + selectedApps.size() + " 个");
            Log.d(TAG, "   - 隐藏标题: " + privacyHideTitle);
            Log.d(TAG, "   - 隐藏内容: " + privacyHideContent);
            
            if (!selectedApps.isEmpty()) {
                Log.d(TAG, "📋 选中应用列表: " + selectedApps.toString());
            } else {
                Log.w(TAG, "⚠️ 没有选中任何应用");
            }
        } catch (Exception e) {
            Log.e(TAG, "加载设置失败", e);
            selectedApps = new HashSet<>();
            // khôngởnàyreset serviceEnabled
        }
    }
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        
        // V2.4: mỗi lần nhận thông báotải lại trạng thái công tắc
        loadNotificationServiceSettings();
        
        // V2.4: nếuthông báodịch vụcông tắcđóng, khôngxử lýthông báo
        if (!serviceEnabled) {
            Log.d(TAG, "⏸️ 通知服务已禁用，忽略通知");
            return;
        }
        
        try {
            String packageName = sbn.getPackageName();
            Notification notification = sbn.getNotification();
            
            Log.d(TAG, "📢 收到通知: " + packageName);
            
            // thườngthông báo
            if ((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0) {
                Log.d(TAG, "⏭️ 忽略常驻通知: " + packageName);
                return;
            }
            
            // tựthông báo
            if (packageName.equals(getPackageName())) {
                Log.d(TAG, "⏭️ 忽略自己的通知");
                return;
            }
            
            // mỗi lầnđềutải lạicài đặt（đảm bảothựcthời gian）
            loadSettings();
            
            // kiểm tradịch vụcóbật
            if (!serviceEnabled) {
                Log.d(TAG, "⏭️ 通知服务未启用，跳过");
                return;
            }
            
            // kiểm trahệ thốngKhông làm phiềnchế độ
            if (followDndMode) {
                try {
                    android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null && nm.getCurrentInterruptionFilter() != android.app.NotificationManager.INTERRUPTION_FILTER_ALL) {
                        Log.d(TAG, "⏭️ 系统勿扰模式已开启，跳过通知动画");
                        return;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "检查勿扰模式失败: " + e.getMessage());
                }
            }
            
            // kiểm tracóúp màn hìnhtaythời gianthông báo（kiểmđomàn hình chínhcảm biến tiệm cận）
            if (onlyWhenLocked) {
                if (!isMainScreenCovered) {
                    Log.d(TAG, "⏭️ 主屏未被遮盖，仅倒扣手机通知模式已开启，跳过");
                    return;
                }
            }
            
            Log.d(TAG, "📋 当前选中应用数量: " + selectedApps.size());
            Log.d(TAG, "📋 选中应用列表: " + selectedApps.toString());
            
            // kiểm tracóởchọntrongtrong
            if (!selectedApps.contains(packageName)) {
                Log.d(TAG, "⏭️ 应用不在选中列表中: " + packageName);
                return;
            }
            
            Log.d(TAG, "✓ 应用在选中列表中: " + packageName);
            
            // nângthông báotrongchứa
            String title = notification.extras.getString(Notification.EXTRA_TITLE, "");
            String text = notification.extras.getString(Notification.EXTRA_TEXT, "");
            long when = notification.when;
            
            Log.d(TAG, "📝 通知标题: " + title);
            Log.d(TAG, "📝 通知内容: " + text);
            
            // V3.2: ẩnchế độxử lý（phân vùngphântiêu đềvàtrongchứa）
            if (privacyHideTitle) {
                Log.d(TAG, "🔒 隐藏通知标题");
                title = getString(R.string.privacy_mode_enabled);
            }
            if (privacyHideContent) {
                Log.d(TAG, "🔒 隐藏通知内容");
                text = getString(R.string.new_message_placeholder);
            }
            
            Log.d(TAG, "🚀 开始显示背屏通知: " + packageName);
            
            // thông báo animation manager: bắt đầuthông báo hoạt ảnh（trả vềhoạt ảnh cũ bị ngắt）
            RearAnimationManager.AnimationType oldAnim = RearAnimationManager.startAnimation(RearAnimationManager.AnimationType.NOTIFICATION);
            
            // nếu có hoạt ảnh cũ cần ngắt, gửi broadcast ngắt
            if (oldAnim == RearAnimationManager.AnimationType.CHARGING) {
                Log.d(TAG, "🔄 检测到充电动画正在播放，发送打断广播");
                
                // V3.5: kiểm trahoạt ảnh sạccólàchế độ giữ sáng
                boolean chargingAlwaysOn = prefs.getBoolean("charging_always_on_enabled", false);
                RearAnimationManager.markInterruptedChargingAsAlwaysOn(chargingAlwaysOn);
                
                RearAnimationManager.sendInterruptBroadcast(this, RearAnimationManager.AnimationType.CHARGING);
            } else if (oldAnim == RearAnimationManager.AnimationType.NOTIFICATION) {
                Log.d(TAG, "🔄 检测到通知动画正在播放，发送打断广播并重载");
                RearAnimationManager.sendInterruptBroadcast(this, RearAnimationManager.AnimationType.NOTIFICATION);
                
                // trễ600mssaukhởi động lạithông báo hoạt ảnh, đảm bảohoạt ảnh cũhoàn toàndừng（khóa màn hình+app castdướicầnhơnnhiềuthời gian）
                final String finalPackageName = packageName;
                final String finalTitle = title;
                final String finalText = text;
                final long finalWhen = when;
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    Log.d(TAG, "🔄 重载通知动画");
                    showNotificationOnRearScreen(finalPackageName, finalTitle, finalText, finalWhen);
                }, 600);
                return; // nângtrướctrả về, tránh lặp lạikhởi động
            }
            
            // kích hoạtmàn hình sauthông báohiển thị
            showNotificationOnRearScreen(packageName, title, text, when);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 处理通知时出错", e);
        }
    }
    
    private void showNotificationOnRearScreen(String packageName, String title, String text, long when) {
        // thamChargingServicethử lại
        if (taskService == null) {
            Log.w(TAG, "⚠️ TaskService未连接，尝试重新绑定...");
            bindTaskService();
            
            // trễ500mssauthử lại
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                showNotificationOnRearScreenDirect(packageName, title, text, when);
            }, 500);
        } else {
            showNotificationOnRearScreenDirect(packageName, title, text, when);
        }
    }
    
    private void showNotificationOnRearScreenDirect(String packageName, String title, String text, long when) {
        try {
            if (taskService == null) {
                Log.e(TAG, "❌ TaskService仍然不可用，放弃显示通知");
                return;
            }
            
            // ngắnthời giancụcbộ phậngiữ sống, tránhởkhóa màn hình/nặngtảidướibị
            acquireWakeLock(6000);
            Log.d(TAG, "🎯 准备启动Activity显示通知");
            
            // trạng thái khóa màn hìnhkiểm tra
            android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            boolean isLocked = km != null && km.isKeyguardLocked();
            
            // màn hình chính foregroundứng dụng（dùng chocùngtên packageforegroundgiữ）
            String mainForegroundApp = null;
            try {
                mainForegroundApp = taskService.getForegroundAppOnDisplay(0);
                Log.d(TAG, "📱 主屏前台应用: " + mainForegroundApp);
            } catch (Throwable t) {
                Log.w(TAG, "获取主屏前台应用失败: " + t.getMessage());
            }
            
            // V3.3: gỡ bỏđánh thứccode, tránh khi khóa màn hình chuyển đến giao diện mật khẩu
            
            try {
                // tạm dừng giám sát, ngăn chặnbịsaikill
                RearScreenKeeperService.pauseMonitoring();
            } catch (Throwable t) {
                Log.w(TAG, "pauseMonitoring failed: " + t.getMessage());
            }
            
            try {
                // tắtLauncher màn hình sau chính thức, tránh
                taskService.disableSubScreenLauncher();
            } catch (Throwable t) {
                Log.w(TAG, "disableSubScreenLauncher failed: " + t.getMessage());
            }
            
            // V3.3: gỡ bỏ wm dismiss-keyguard lệnh, tránh khi khóa màn hình chuyển đến giao diện mật khẩu
            
            // 2) theo trạng thái khóa màn hình và ứng dụng foreground chọn chiến lược khởi động
            String componentName = getPackageName() + "/" + RearScreenNotificationActivity.class.getName();
            
            // khi khóa màn hình và màn hình chính foreground là ứng dụng thuộc thông báo này, tránhmàn hình chính chiếm chỗchiến lược, sửalàtrực tiếpmàn hình saukhởi động, ngăn chặnhệ thống
            // chắc chắnphân phốitên package, tránhsaixét（như com.tencent.mm và com.tencent.mobileqq）
            boolean forceDirectRearDueToSameApp = false;
            if (isLocked && mainForegroundApp != null && !mainForegroundApp.isEmpty()) {
                // nângmàn hình chính foregroundứng dụngtên package（thứccó thểlà "com.example.app/com.example.app.MainActivity"）
                String foregroundPackage = mainForegroundApp;
                if (mainForegroundApp.contains("/")) {
                    foregroundPackage = mainForegroundApp.split("/")[0];
                }
                forceDirectRearDueToSameApp = foregroundPackage.equals(packageName);
                Log.d(TAG, String.format("🔍 锁屏同包检查: 主屏前台=[%s] vs 通知包名=[%s] -> %s",
                    foregroundPackage, packageName, forceDirectRearDueToSameApp ? "匹配(直接背屏)" : "不匹配(占位策略)"));
            }
            
            // ✅ thống nhấtchiến lược：không cókhóa màn hìnhvớikhông, đềukhởi động trực tiếp trên màn hình sau（tránhDPIkhôngphân phối）
            // khởi động trực tiếp trên màn hình saucó thể đảm bảo bố cục sử dụng đúngDPI（450）, tránhtừmàn hình chínhchuyểngây
            
            // đảm bảochế độ tốicài đặtlàtối ưumới
            notificationDarkMode = prefs.getBoolean("notification_dark_mode", false);
            Log.d(TAG, "🌙 当前暗夜模式设置: " + notificationDarkMode);
            
            String directCmd = String.format(
                "am start --display 1 -n %s --es packageName \"%s\" --es title \"%s\" --es text \"%s\" --el when %d --ez darkMode %b",
                componentName,
                packageName,
                title.replace("\"", "\\\""),
                text.replace("\"", "\\\""),
                when,
                notificationDarkMode
            );
            
            boolean started = false;
            // thử3lầnkhởi động trực tiếp, đảm bảothành công
            for (int retry = 0; retry < 3; retry++) {
                try {
                    taskService.executeShellCommand(directCmd);
                    Log.d(TAG, String.format("✓ %s，直接在背屏启动通知Activity (尝试%d)",
                        isLocked ? "锁屏状态" : "非锁屏状态", retry + 1));
                    try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    
                    // kiểm tracókhởi độngthành công
                    String check = taskService.executeShellCommandWithResult("am stack list | grep RearScreenNotificationActivity");
                    if (check != null && !check.trim().isEmpty()) {
                        started = true;
                        Log.d(TAG, "✓ 通知动画已在背屏启动");
                        break;
                    }
                } catch (Throwable t) {
                    Log.w(TAG, String.format("尝试%d失败: %s", retry + 1, t.getMessage()));
                }
            }
            
            // nếukhởi động trực tiếpthất bại, sử dụngbịngười dùngchiến lược（màn hình chính chiếm chỗ+chuyển）
            if (!started && isLocked) {
                Log.w(TAG, "⚠️ 直接背屏启动失败，回退到主屏占位+移动策略");
                
                // màn hình chính khởi động（Activity tựchiếm chỗ）
                String startOnMainCmd = String.format(
                    "am start -n %s --es packageName \"%s\" --es title \"%s\" --es text \"%s\" --el when %d --ez darkMode %b",
                    componentName,
                    packageName,
                    title.replace("\"", "\\\""),
                    text.replace("\"", "\\\""),
                    when,
                    notificationDarkMode
                );
                Log.d(TAG, "🔵 在主屏启动通知Activity（占位符）");
                taskService.executeShellCommand(startOnMainCmd);
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                
                // poll lấy taskId
                String notifTaskId = null;
                int attempts = 0;
                int maxAttempts = 60;
                while (notifTaskId == null && attempts < maxAttempts) {
                    try { Thread.sleep(40); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    String result = taskService.executeShellCommandWithResult("am stack list | grep RearScreenNotificationActivity");
                    if (result != null && !result.trim().isEmpty()) {
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("taskId=(\\d+)");
                        java.util.regex.Matcher matcher = pattern.matcher(result);
                        if (matcher.find()) {
                            notifTaskId = matcher.group(1);
                            Log.d(TAG, "🎯 找到通知taskId=" + notifTaskId);
                            break;
                        }
                    }
                    attempts++;
                }
                
                if (notifTaskId != null) {
                    // 4) chuyển đến màn hình sau
                    String moveCmd = "service call activity_task 50 i32 " + notifTaskId + " i32 1";
                    taskService.executeShellCommand(moveCmd);
                    try { Thread.sleep(60); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    
                    // 5) khi khóa màn hìnhđóng màn hình chính, tránhmàn hình chínhtiêu điểm
                    // chức năng màn hình chính ngủđã gỡ
                    Log.d(TAG, "🔒 锁屏状态，主屏已关闭");
                    
                    Log.d(TAG, "✓ 通知动画已移动到背屏");
                } else {
                    Log.e(TAG, "❌ 未能找到通知Activity的taskId，最后尝试直接在背屏启动");
                    try {
                        String fallbackCmd = String.format(
                            "am start --display 1 -n %s --es packageName \"%s\" --es title \"%s\" --es text \"%s\" --el when %d --ez darkMode %b",
                            componentName,
                            packageName,
                            title.replace("\"", "\\\""),
                            text.replace("\"", "\\\""),
                            when,
                            notificationDarkMode
                        );
                        taskService.executeShellCommand(fallbackCmd);
                        Log.d(TAG, "🟦 已尝试直接 --display 1 启动通知Activity（fallback）");
                    } catch (Throwable t) {
                        Log.w(TAG, "Fallback直接在背屏启动失败: " + t.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 显示背屏通知失败", e);
        } finally {
            releaseWakeLock();
        }
    }

    private void acquireWakeLock(long timeoutMs) {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                if (wakeLock == null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MRSS:NotificationWake");
                    wakeLock.setReferenceCounted(false);
                }
                if (!wakeLock.isHeld()) {
                    wakeLock.acquire(timeoutMs);
                    Log.d(TAG, "🔒 PARTIAL_WAKE_LOCK acquired for " + timeoutMs + "ms");
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to acquire wakelock: " + t.getMessage());
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "🔓 PARTIAL_WAKE_LOCK released");
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to release wakelock: " + t.getMessage());
        }
    }
    
    /**
 * khởi tạomàn hình chínhcảm biến tiệm cận（dùng chokiểmđoúp màn hìnhtay）
 */
    private void initMainProximitySensor() {
        try {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            
            if (sensorManager != null) {
                // lấysởcócảm biến
                java.util.List<Sensor> allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
                
                // tramàn hình chínhcảm biến tiệm cận（khônggói"Back"cảm biến tiệm cận）
                // ưutrướcchọn Wakeup phiên bản, nếukhôngcóthìchọn Non-wakeup phiên bản
                Sensor wakeupSensor = null;
                Sensor nonWakeupSensor = null;
                
                for (Sensor sensor : allSensors) {
                    String name = sensor.getName();
                    if (name.contains("Proximity") && !name.contains("Back")) {
                        // màn hình chínhcảm biến tiệm cận（khônggóiBack）
                        if (name.contains("Wakeup")) {
                            wakeupSensor = sensor;
                        } else {
                            nonWakeupSensor = sensor;
                        }
                    }
                }
                
                // ưutrướcsử dụng Wakeup phiên bản
                if (wakeupSensor != null) {
                    mainProximitySensor = wakeupSensor;
                } else if (nonWakeupSensor != null) {
                    mainProximitySensor = nonWakeupSensor;
                    Log.w(TAG, "→ Using NON-WAKEUP main proximity sensor");
                }
                
                // nếukhôngđếnmàn hình chínhcảm biến, quaylùiđếnmặc địnhcảm biến
                if (mainProximitySensor == null) {
                    Log.w(TAG, "⚠ Main proximity sensor not found, using default");
                    mainProximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                }
                
                if (mainProximitySensor != null) {
                    // đăng kýcảm biếnlistener
                    boolean registered = sensorManager.registerListener(
                            proximitySensorListener,
                            mainProximitySensor,
                            SensorManager.SENSOR_DELAY_NORMAL);
                    
                    if (registered) {
                        Log.d(TAG, "✅ 主屏接近传感器已注册");
                    } else {
                        Log.w(TAG, "⚠ Failed to register main proximity sensor");
                    }
                } else {
                    Log.w(TAG, "⚠ No main proximity sensor available");
                }
            } else {
                Log.w(TAG, "⚠ SensorManager not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "✗ Error initializing main proximity sensor", e);
        }
    }
    
    /**
 * hủy đăng kýmàn hình chínhcảm biến tiệm cận
 */
    private void unregisterMainProximitySensor() {
        try {
            if (sensorManager != null && proximitySensorListener != null) {
                sensorManager.unregisterListener(proximitySensorListener);
                Log.d(TAG, "✓ 主屏接近传感器已注销");
            }
        } catch (Exception e) {
            Log.e(TAG, "✗ Error unregistering main proximity sensor", e);
        }
    }
    
    /**
 * màn hình chínhcảm biến tiệm cậnlistener
 */
    private final SensorEventListener proximitySensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor == mainProximitySensor) {
                float distance = event.values[0];
                float maxRange = mainProximitySensor.getMaximumRange();
                
                // tất nhiêntiệm cận0（bị）thời giankích hoạt
                // nhỏởtối ưulớn20%là
                boolean isCovered = (distance < maxRange * 0.2f);
                
                isMainScreenCovered = isCovered;
                
                if (isCovered) {
                    Log.d(TAG, "📱 主屏接近传感器：被遮盖 (距离: " + distance + " cm)");
                } else {
                    Log.d(TAG, "📱 主屏接近传感器：未遮盖 (距离: " + distance + " cm)");
                }
            }
        }
        
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // không cầnxử lý
        }
    };
    
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "🔗 NotificationListener connected");
        loadSettings();
        Log.d(TAG, "✓ 通知监听器已就绪");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🔴 NotificationService destroyed");
        
        // hủy đăng kýbroadcast receiver
        try {
            unregisterReceiver(settingsReceiver);
            Log.d(TAG, "✓ 广播接收器已注销");
        } catch (Exception e) {
            Log.w(TAG, "Failed to unregister receiver", e);
        }
        
        // gỡ Shizuku listener
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener);
            Shizuku.removeBinderDeadListener(binderDeadListener);
        } catch (Exception e) {
            Log.w(TAG, "Failed to remove Shizuku listeners", e);
        }
        
        // unbind TaskService
        try {
            if (taskService != null) {
                Shizuku.unbindUserService(serviceArgs, taskServiceConnection, true);
                taskService = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to unbind TaskService", e);
        }
        
        // hủy đăng kýmàn hình chínhcảm biến tiệm cận
        unregisterMainProximitySensor();
        
        // xóainstance
        instance = null;
        
        stopForeground(true);
    }
}

