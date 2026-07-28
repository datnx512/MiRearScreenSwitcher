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

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import rikka.shizuku.Shizuku;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.display.switcher/task";
    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    
    // instance static, chokhácloạitruy cập
    private static MainActivity currentInstance;
    
    public static MainActivity getCurrentInstance() {
        return currentInstance;
    }
    
    private ITaskService taskService;
    private MethodChannel methodChannel;
    private final Shizuku.UserServiceArgs serviceArgs = 
        new Shizuku.UserServiceArgs(new ComponentName("com.tgwgroup.MiRearScreenSwitcher", TaskService.class.getName()))
            .daemon(false)
            .processNameSuffix("task_service")
            .debuggable(false)
            .version(1);
    
    // Shizuku listener（keyphím！）
    private final Shizuku.OnBinderReceivedListener binderReceivedListener = 
        () -> {
            bindTaskService();
        };
    
    private final Shizuku.OnBinderDeadListener binderDeadListener = 
        () -> {
            taskService = null;
            
            // khởi độngkết nối lạivụ
            scheduleReconnectTaskService();
        };
    
    /**
 * TaskServicekết nối lạivụ
 */
    private final Runnable reconnectTaskServiceRunnable = new Runnable() {
        @Override
        public void run() {
            if (taskService == null) {
                bindTaskService();
                
                // nếukết nối lại thất bại, 2giâysauthử lại lần nữa
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 30);
            } else {
            }
        }
    };
    
    /**
 * xếpTaskServicekết nối lại
 */
    private void scheduleReconnectTaskService() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(reconnectTaskServiceRunnable, 30);
    };
    
    private final Shizuku.OnRequestPermissionResultListener requestPermissionResultListener = 
        (requestCode, grantResult) -> {
            boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                bindTaskService();
            }
            // thông báoFlutterlàm mớitrạng thái
            if (methodChannel != null) {
                runOnUiThread(() -> {
                    methodChannel.invokeMethod("onShizukuPermissionChanged", granted);
                });
            }
        };
    
    private final ServiceConnection taskServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            taskService = ITaskService.Stub.asInterface(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            taskService = null;
        }
    };
    
    private void bindTaskService() {
        if (taskService != null) {
            return;
        }
        
        try {
            if (!Shizuku.pingBinder()) {
                Log.e(TAG, "Shizuku not available");
                return;
            }
            
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "No Shizuku permission");
                return;
            }
            Shizuku.bindUserService(serviceArgs, taskServiceConnection);
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind TaskService", e);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // lưu instance
        currentInstance = this;
        
        // thêm Shizuku listener（keyphím！sử dụngStickyphiên bản）
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener);
        
        // tự độngkiểm travàyêu cầuShizukuquyền
        checkAndRequestShizukuPermission();
        
        // xử lýthông báoIntent
        handleIncomingIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingIntent(intent);
    }
    
    /**
 * xử lýtựServicethông báoIntent
 */
    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        
        String action = intent.getAction();
        
        // xử lýthông báoIntent
        if ("SHOW_NOTIFICATION_ON_REAR_SCREEN".equals(action)) {
            String packageName = intent.getStringExtra("packageName");
            String title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");
            long when = intent.getLongExtra("when", System.currentTimeMillis());
            
            Log.d(TAG, "Received notification intent for: " + packageName);
            startNotificationOnRearScreen(packageName, title, text, when);
        }
    }
    
    /**
 * ởmàn hình saukhởi độngthông báohiển thịActivity
 */
    private void startNotificationOnRearScreen(String packageName, String title, String text, long when) {
        if (taskService == null) {
            Log.w(TAG, "TaskService not available for notification");
            return;
        }
        
        new Thread(() -> {
            try {
                // bước1: tắt Launcher chính thức
                taskService.disableSubScreenLauncher();
                
                // bước2: đánh thứcmàn hình sau
                taskService.executeShellCommand("input -d 1 keyevent KEYCODE_WAKEUP");
                Thread.sleep(50);
                
                // bước3: ởmàn hình chính khởi độngActivity
                String componentName = getPackageName() + "/" + RearScreenNotificationActivity.class.getName();
                String mainCmd = String.format(
                    "am start -n %s --es packageName \"%s\" --es title \"%s\" --es text \"%s\" --el when %d",
                    componentName, packageName,
                    title != null ? title.replace("\"", "'") : "",
                    text != null ? text.replace("\"", "'") : "",
                    when
                );
                taskService.executeShellCommand(mainCmd);
                
                // bước4: poll lấy taskId
                String notifTaskId = null;
                int attempts = 0;
                int maxAttempts = 20;
                
                while (notifTaskId == null && attempts < maxAttempts) {
                    Thread.sleep(30);
                    String result = taskService.executeShellCommandWithResult("am stack list | grep RearScreenNotificationActivity");
                    if (result != null && !result.trim().isEmpty()) {
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("taskId=(\\d+)");
                        java.util.regex.Matcher matcher = pattern.matcher(result);
                        if (matcher.find()) {
                            notifTaskId = matcher.group(1);
                            Log.d(TAG, "Found notification taskId=" + notifTaskId);
                            break;
                        }
                    }
                    attempts++;
                }
                
                if (notifTaskId != null) {
                    // bước5: chuyển đến màn hình sau
                    String moveCmd = "service call activity_task 50 i32 " + notifTaskId + " i32 1";
                    taskService.executeShellCommand(moveCmd);
                    Thread.sleep(40);
                    
                    // bước6: kiểm tracókhóa màn hình, chắc chắncóđóng màn hình chính
                    android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                    if (km != null && km.isKeyguardLocked()) {
                        // chức năng màn hình chính ngủđã gỡ
                        Log.d(TAG, "🔒 锁屏状态，主屏已关闭");
                    }
                    
                    Log.d(TAG, "✅ Notification animation started on rear screen");
                } else {
                    Log.e(TAG, "❌ Failed to find notification taskId");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to show notification on rear screen", e);
            }
        }).start();
    }
    
    /**
 * Shelllệnh（choRearScreenChargingActivitygọi）
 */
    public void executeShellCommand(String cmd) {
        if (taskService != null) {
            try {
                taskService.executeShellCommand(cmd);
            } catch (Exception e) {
                Log.e(TAG, "Failed to execute command: " + cmd, e);
            }
        }
    }
    
    private void checkAndRequestShizukuPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(0);
                } else {
                    bindTaskService();
                }
            } else {
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check Shizuku permission", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // xóa instance static
        currentInstance = null;
        
        Shizuku.removeBinderReceivedListener(binderReceivedListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener);
    }
    
    @Override
    public void configureFlutterEngine(FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        
        methodChannel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL);
        methodChannel.setMethodCallHandler((call, result) -> {
                switch (call.method) {
                    case "checkShizuku": {
                        try {
                            boolean isRunning = Shizuku.pingBinder();
                            boolean hasPermission = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
                            result.success(isRunning && hasPermission);
                        } catch (Exception e) {
                            result.success(false);
                        }
                        break;
                    }
                    
                    case "requestShizukuPermission": {
                        try {
                            Shizuku.requestPermission(0);
                            result.success(null);
                        } catch (Exception e) {
                            result.error("ERROR", e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "getShizukuInfo": {
                        try {
                            boolean isRunning = Shizuku.pingBinder();
                            boolean hasPermission = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
                            int uid = Shizuku.getUid();
                            int version = Shizuku.getVersion();
                            String info = "Running: " + isRunning + "\n" +
                                         "Permission: " + hasPermission + "\n" +
                                         "UID: " + uid + "\n" +
                                         "Version: " + version;
                            result.success(info);
                        } catch (Exception e) {
                            result.success("Error: " + e.getMessage());
                        }
                        break;
                    }
                    
                    case "getCurrentApp": {
                        if (taskService != null) {
                            try {
                                String currentApp = taskService.getCurrentForegroundApp();
                                result.success(currentApp);
                            } catch (Exception e) {
                                Log.e(TAG, "TaskService error: " + e.getMessage(), e);
                                result.success(null);
                            }
                        } else {
                            result.success(null);
                        }
                        break;
                    }
                    
                    case "requestNotificationPermission": {
                        // Android 13+ cầnyêu cầuthông báoquyền
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                                != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, 
                                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                                    NOTIFICATION_PERMISSION_REQUEST_CODE);
                                result.success(null);
                            } else {
                                result.success(null);
                            }
                        } else {
                            // Android 12bằngdướikhông cầnyêu cầuthông báoquyền
                            result.success(null);
                        }
                        break;
                    }
                    
                    case "getCurrentRearDpi": {
                        if (taskService != null) {
                            try {
                                int dpi = taskService.getCurrentRearDpi();
                                result.success(dpi);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to get rear DPI", e);
                                result.error("ERROR", e.getMessage(), null);
                            }
                        } else {
                            result.error("ERROR", "TaskService not available", null);
                        }
                        break;
                    }
                    
                    case "setRearDpi": {
                        if (taskService != null) {
                            try {
                                int dpi = (int) call.argument("dpi");
                                boolean success = taskService.setRearDpi(dpi);
                                result.success(success);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to set rear DPI", e);
                                result.error("ERROR", e.getMessage(), null);
                            }
                        } else {
                            result.error("ERROR", "TaskService not available", null);
                        }
                        break;
                    }
                    
                    case "resetRearDpi": {
                        if (taskService != null) {
                            try {
                                boolean success = taskService.resetRearDpi();
                                result.success(success);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to reset rear DPI", e);
                                result.error("ERROR", e.getMessage(), null);
                            }
                        } else {
                            result.error("ERROR", "TaskService not available", null);
                        }
                        break;
                    }
                    
                    case "openCoolApkProfile": {
                        try {
                            Intent intent = new Intent();
                            intent.setClassName("com.coolapk.market", "com.coolapk.market.view.AppLinkActivity");
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setData(android.net.Uri.parse("coolmarket://u/8158212"));
                            startActivity(intent);
                            result.success(null);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to open CoolApk profile", e);
                            result.error("ERROR", getString(R.string.error_install_coolapk), null);
                        }
                        break;
                    }
                    
                    case "openCoolApkProfileXmz": {
                        try {
                            Intent intent = new Intent();
                            intent.setClassName("com.coolapk.market", "com.coolapk.market.view.AppLinkActivity");
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setData(android.net.Uri.parse("coolmarket://u/4279097"));
                            startActivity(intent);
                            result.success(null);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to open CoolApk profile", e);
                            result.error("ERROR", getString(R.string.error_install_coolapk), null);
                        }
                        break;
                    }
                    
                    case "openTutorial": {
                        // mởvănsử dụngtrình
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(android.net.Uri.parse("https://docs.qq.com/doc/DVWxpT3hQdHNPR3Zy?dver="));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            result.success(null);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to open tutorial", e);
                            result.error("ERROR", getString(R.string.error_open_failed) + e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "openDonationPage": {
                        // mởủng hộtrang
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(android.net.Uri.parse("https://tgwgroup.ltd/2025/10/19/%e5%85%b3%e4%ba%8e%e6%89%93%e8%b5%8f/"));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            result.success(null);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to open donation page", e);
                            result.error("ERROR", getString(R.string.error_open_failed) + e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "openQQGroup": {
                        // mởMRSSnhóm chattrang
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(android.net.Uri.parse("https://tgwgroup.ltd/2025/10/21/%e5%85%b3%e4%ba%8emrss%e4%ba%a4%e6%b5%81%e7%be%a4/"));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            result.success(null);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to open QQ group page", e);
                            result.error("ERROR", getString(R.string.error_open_failed) + e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "ensureTaskServiceConnected": {
                        // đảm bảo TaskService kết nốibình thường
                        try {
                            if (taskService == null) {
                                // thửlàm lạibind
                                bindTaskService();
                            }
                            result.success(taskService != null);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to ensure TaskService connection", e);
                            result.error("ERROR", e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "setDisplayRotation": {
                        if (taskService != null) {
                            try {
                                int displayId = (int) call.argument("displayId");
                                int rotation = (int) call.argument("rotation");
                                boolean success = taskService.setDisplayRotation(displayId, rotation);
                                result.success(success);
                            } catch (Exception e) {
                                result.error("ERROR", e.getMessage(), null);
                            }
                        } else {
                            result.error("ERROR", "TaskService not available", null);
                        }
                        break;
                    }
                    
                    case "getDisplayRotation": {
                        if (taskService != null) {
                            try {
                                int displayId = (int) call.argument("displayId");
                                int rotation = taskService.getDisplayRotation(displayId);
                                result.success(rotation);
                            } catch (Exception e) {
                                result.error("ERROR", e.getMessage(), null);
                            }
                        } else {
                            result.error("ERROR", "TaskService not available", null);
                        }
                        break;
                    }
                    
                    case "returnRearAppAndRestart": {
                        // khởi động lạitrướctrướcquaymàn hình sauứng dụng
                        if (taskService != null) {
                            try {
                                // lấycuối cùngchuyểnthông tin task
                                String lastTask = SwitchToRearTileService.getLastMovedTask();
                                
                                if (lastTask != null && lastTask.contains(":")) {
                                    String[] parts = lastTask.split(":");
                                    int taskId = Integer.parseInt(parts[1]);
                                    
                                    // kiểm travụcóvẫnởmàn hình sau
                                    boolean onRear = taskService.isTaskOnDisplay(taskId, 1);
                                    
                                    if (onRear) {
                                        // quaymàn hình chính
                                        taskService.moveTaskToDisplay(taskId, 0);
                                        
                                        // khôi phụcLauncher chính thức
                                        taskService.enableSubScreenLauncher();
                                        
                                        result.success(true);
                                    } else {
                                        // khôngcóứng dụngởmàn hình sau
                                        result.success(false);
                                    }
                                } else {
                                    // khôngcóghi
                                    result.success(false);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to return rear app", e);
                                result.error("ERROR", e.getMessage(), null);
                            }
                        } else {
                            result.error("ERROR", "TaskService not available", null);
                        }
                        break;
                    }
                    
                    case "setProximitySensorEnabled": {
                        // V2.2: cài đặttiệm cậncông tắc cảm biến
                        boolean enabled = (boolean) call.argument("enabled");
                        
                        // thông báoRearScreenKeeperServicecập nhậttrạng thái
                        Intent intent = new Intent(this, RearScreenKeeperService.class);
                        intent.setAction("ACTION_SET_PROXIMITY_ENABLED");
                        intent.putExtra("enabled", enabled);
                        startService(intent);
                        
                        result.success(true);
                        break;
                    }
                    
                    case "setKeepScreenOnEnabled": {
                        // V2.5: cài đặtmàn hình saucông tắc giữ sáng
                        boolean enabled = (boolean) call.argument("enabled");
                        
                        // thông báoRearScreenKeeperServicecập nhậttrạng thái
                        Intent intent = new Intent(this, RearScreenKeeperService.class);
                        intent.setAction("ACTION_SET_KEEP_SCREEN_ON_ENABLED");
                        intent.putExtra("enabled", enabled);
                        startService(intent);
                        
                        result.success(true);
                        break;
                    }
                    
                    case "setAlwaysWakeUpEnabled": {
                        // V3.5: cài đặtkhi chưa cast ứng dụng giữ sángcông tắc
                        boolean enabled = (boolean) call.argument("enabled");
                        
                        SharedPreferences prefs = getSharedPreferences("mrss_settings", MODE_PRIVATE);
                        prefs.edit().putBoolean("always_wakeup_enabled", enabled).apply();
                        
                        Intent intent = new Intent(this, AlwaysWakeUpService.class);
                        if (enabled) {
                            startService(intent);
                            Log.d(TAG, "AlwaysWakeUpService started");
                        } else {
                            stopService(intent);
                            Log.d(TAG, "AlwaysWakeUpService stopped");
                        }
                        
                        result.success(true);
                        break;
                    }
                    
                    case "setChargingAlwaysOnEnabled": {
                        // V3.5: cài đặthoạt ảnh sạc giữ sángcông tắc
                        boolean enabled = (boolean) call.argument("enabled");
                        
                        SharedPreferences prefs = getSharedPreferences("mrss_settings", MODE_PRIVATE);
                        prefs.edit().putBoolean("charging_always_on_enabled", enabled).apply();
                        
                        // thông báoChargingServicetải lạicài đặt
                        sendBroadcast(new Intent("com.tgwgroup.MiRearScreenSwitcher.RELOAD_CHARGING_SETTINGS"));
                        
                        Log.d(TAG, "Charging always on set to: " + enabled);
                        result.success(true);
                        break;
                    }
                    
                    case "toggleChargingService": {
                        // V2.3: chuyểnhoạt ảnh sạcdịch vụ
                        boolean enabled = (boolean) call.argument("enabled");
                        
                        Intent intent = new Intent(this, ChargingService.class);
                        if (enabled) {
                            startService(intent);
                            Log.d(TAG, "ChargingService started");
                        } else {
                            stopService(intent);
                            Log.d(TAG, "ChargingService stopped");
                        }
                        
                        result.success(true);
                        break;
                    }
                    
                    case "startNotificationService": {
                        // V2.4: khởi độngthông báodịch vụ
                        Intent intent = new Intent(this, NotificationService.class);
                        startService(intent);
                        Log.d(TAG, "NotificationService started");
                        result.success(true);
                        break;
                    }
                    
                    case "toggleNotificationService": {
                        // V2.4: chuyểnthông báodịch vụ
                        boolean enabled = (boolean) call.argument("enabled");
                        
                        SharedPreferences prefs = getSharedPreferences("mrss_settings", MODE_PRIVATE);
                        prefs.edit()
                            .putBoolean("notification_service_enabled", enabled)
                            .apply();
                        
                        if (enabled) {
                            // bậtthời giankhởi độngdịch vụ
                            Intent intent = new Intent(this, NotificationService.class);
                            startService(intent);
                            Log.d(TAG, "NotificationService started");
                        } else {
                            // đóngthời giandừngdịch vụ
                            Intent intent = new Intent(this, NotificationService.class);
                            stopService(intent);
                            Log.d(TAG, "NotificationService stopped");
                        }
                        
                        Log.d(TAG, "Notification service enabled: " + enabled);
                        result.success(true);
                        break;
                    }
                    
                    case "checkNotificationListenerPermission": {
                        // V2.4: kiểm trathông báolắng nghequyền
                        boolean hasPermission = isNotificationListenerEnabled();
                        result.success(hasPermission);
                        break;
                    }
                    
                    case "openNotificationListenerSettings": {
                        // V2.4: mởthông báolắng nghecài đặt
                        try {
                            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            result.success(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to open notification settings", e);
                            result.error("ERROR", e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "checkQueryAllPackagesPermission": {
                        // V2.4: kiểm traQUERY_ALL_PACKAGESquyền
                        boolean hasPermission = checkSelfPermission("android.permission.QUERY_ALL_PACKAGES") == PackageManager.PERMISSION_GRANTED;
                        Log.d(TAG, "🔍 QUERY_ALL_PACKAGES permission check: " + hasPermission);
                        result.success(hasPermission);
                        break;
                    }
                    
                    case "requestQueryAllPackagesPermission": {
                        // V2.4: yêu cầuQUERY_ALL_PACKAGESquyền（chuyển đếnứng dụngtrang）
                        try {
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            result.success(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to open app settings", e);
                            result.error("ERROR", e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "getInstalledApps": {
                        // V2.4: lấyđãứng dụng（bất đồng bộ）
                        new Thread(() -> {
                            try {
                                // trướckiểm traquyền
                                boolean hasPermission = checkSelfPermission("android.permission.QUERY_ALL_PACKAGES") == PackageManager.PERMISSION_GRANTED;
                                if (!hasPermission) {
                                    Log.w(TAG, "⚠️ 没有QUERY_ALL_PACKAGES权限，应用列表可能不完整");
                                }
                                
                                java.util.List<java.util.Map<String, Object>> apps = getInstalledApps();
                                runOnUiThread(() -> result.success(apps));
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to get installed apps", e);
                                runOnUiThread(() -> result.error("ERROR", e.getMessage(), null));
                            }
                        }).start();
                        break;
                    }
                    
                    case "getSelectedNotificationApps": {
                        // V2.4: lấyđãchọnthông báoứng dụng
                        try {
                            SharedPreferences prefs = getSharedPreferences("mrss_settings", MODE_PRIVATE);
                            java.util.Set<String> selectedApps = prefs.getStringSet("notification_selected_apps", new java.util.HashSet<>());
                            result.success(new java.util.ArrayList<>(selectedApps));
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to get selected apps", e);
                            result.error("ERROR", e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "setSelectedNotificationApps": {
                        // V2.4: lưuchọnthông báoứng dụng
                        try {
                            @SuppressWarnings("unchecked")
                            java.util.List<String> selectedApps = (java.util.List<String>) call.arguments;
                            SharedPreferences prefs = getSharedPreferences("mrss_settings", MODE_PRIVATE);
                            prefs.edit()
                                .putStringSet("notification_selected_apps", new java.util.HashSet<>(selectedApps))
                                .apply();
                            Log.d(TAG, "Saved " + selectedApps.size() + " selected apps");
                            result.success(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to save selected apps", e);
                            result.error("ERROR", e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "setNotificationPrivacyHideTitle": {
                        // V3.2: cài đặtẩnthông báotiêu đề
                        try {
                            boolean enabled = (boolean) call.argument("enabled");
                            SharedPreferences prefs = getSharedPreferences("mrss_settings", MODE_PRIVATE);
                            prefs.edit()
                                .putBoolean("notification_privacy_hide_title", enabled)
                                .apply();
                            
                            // thông báoNotificationServicetải lạicài đặt
                            sendBroadcast(new Intent("com.tgwgroup.MiRearScreenSwitcher.RELOAD_NOTIFICATION_SETTINGS"));
                            
                            Log.d(TAG, "Privacy hide title set to: " + enabled);
                            result.success(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to set privacy hide title", e);
                            result.error("ERROR", e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "setNotificationPrivacyHideContent": {
                        // V3.2: cài đặtẩnthông báotrongchứa
                        try {
                            boolean enabled = (boolean) call.argument("enabled");
                            SharedPreferences prefs = getSharedPreferences("mrss_settings", MODE_PRIVATE);
                            prefs.edit()
                                .putBoolean("notification_privacy_hide_content", enabled)
                                .apply();
                            
                            // thông báoNotificationServicetải lạicài đặt
                            sendBroadcast(new Intent("com.tgwgroup.MiRearScreenSwitcher.RELOAD_NOTIFICATION_SETTINGS"));
                            
                            Log.d(TAG, "Privacy hide content set to: " + enabled);
                            result.success(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to set privacy hide content", e);
                            result.error("ERROR", e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "setFollowDndMode": {
                        // V3.0: cài đặttheohệ thốngKhông làm phiềnchế độ
                        try {
                            boolean enabled = (boolean) call.argument("enabled");
                            SharedPreferences prefs = getSharedPreferences("mrss_settings", MODE_PRIVATE);
                            prefs.edit()
                                .putBoolean("notification_follow_dnd_mode", enabled)
                                .apply();
                            Log.d(TAG, "Follow DND mode set to: " + enabled);
                            result.success(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to set follow DND mode", e);
                            result.error("ERROR", e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "setOnlyWhenLocked": {
                        // V3.0: cài đặtúp màn hìnhtaythời gianthông báo
                        try {
                            boolean enabled = (boolean) call.argument("enabled");
                            SharedPreferences prefs = getSharedPreferences("mrss_settings", MODE_PRIVATE);
                            prefs.edit()
                                .putBoolean("notification_only_when_locked", enabled)
                                .apply();
                            Log.d(TAG, "Only when upside down mode set to: " + enabled);
                            result.success(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to set only when upside down mode", e);
                            result.error("ERROR", e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "setNotificationDarkMode": {
                        // V3.1: cài đặtthông báochế độ tối
                        try {
                            boolean enabled = (boolean) call.argument("enabled");
                            SharedPreferences prefs = getSharedPreferences("mrss_settings", MODE_PRIVATE);
                            prefs.edit()
                                .putBoolean("notification_dark_mode", enabled)
                                .apply();
                            Log.d(TAG, "Notification dark mode set to: " + enabled);
                            result.success(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to set notification dark mode", e);
                            result.error("ERROR", e.getMessage(), null);
                        }
                        break;
                    }
                    
                    case "setNotificationDuration": {
                        // V3.4: cài đặtthông báotự độnghủythời gian
                        try {
                            int duration = (int) call.argument("duration");
                            SharedPreferences prefs = getSharedPreferences("mrss_settings", MODE_PRIVATE);
                            prefs.edit()
                                .putInt("notification_duration", duration)
                                .apply();
                            Log.d(TAG, "Notification duration set to: " + duration + " seconds");
                            result.success(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to set notification duration", e);
                            result.error("ERROR", e.getMessage(), null);
                        }
                        break;
                    }
                    
                    default:
                        result.notImplemented();
                }
            });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
            }
        }
    }
    
    /**
 * V2.4: kiểm trathông báodịch vụ lắng nghecóđãbật
 */
    private boolean isNotificationListenerEnabled() {
        String packageName = getPackageName();
        String flat = android.provider.Settings.Secure.getString(
            getContentResolver(),
            "enabled_notification_listeners"
        );
        
        if (flat == null || flat.isEmpty()) {
            return false;
        }
        
        String[] names = flat.split(":");
        for (String name : names) {
            android.content.ComponentName cn = android.content.ComponentName.unflattenFromString(name);
            if (cn != null && packageName.equals(cn.getPackageName())) {
                return true;
            }
        }
        return false;
    }
    
    /**
 * V2.4: lấyđãứng dụng
 */
    private java.util.List<java.util.Map<String, Object>> getInstalledApps() {
        java.util.List<java.util.Map<String, Object>> apps = new java.util.ArrayList<>();
        
        try {
            PackageManager pm = getPackageManager();
            java.util.List<android.content.pm.ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            
            Log.d(TAG, "Total packages found: " + packages.size());
            
            // sử dụngtênđơnchiến lược（người dùngứng dụng + nặngcầnhệ thốngứng dụng）
            java.util.Set<String> importantSystemApps = new java.util.HashSet<>();
            importantSystemApps.add("com.tencent.mm"); // 
            importantSystemApps.add("com.tencent.mobileqq"); // QQ
            importantSystemApps.add("com.coolapk.market"); // CoolApk
            importantSystemApps.add("com.sina.weibo"); // 
            importantSystemApps.add("com.taobao.taobao"); // 
            importantSystemApps.add("com.eg.android.AlipayGphone"); // 
            importantSystemApps.add("com.netease.cloudmusic"); // 
            importantSystemApps.add("com.ss.android.ugc.aweme"); // 
            importantSystemApps.add("com.bilibili.app.in"); // 
            importantSystemApps.add("com.android.mms"); // ngắn
            importantSystemApps.add("com.android.contacts"); // hệ
            
            for (android.content.pm.ApplicationInfo appInfo : packages) {
                // bỏ quatự
                if (appInfo.packageName.equals(getPackageName())) {
                    continue;
                }
                
                boolean isSystemApp = (appInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
                boolean isUserApp = !isSystemApp;
                boolean isImportantSystemApp = importantSystemApps.contains(appInfo.packageName);
                
                // chỉgóingười dùngứng dụnghoặcnặngcầnhệ thốngứng dụng
                if (!isUserApp && !isImportantSystemApp) {
                    continue;
                }
                
                java.util.Map<String, Object> app = new java.util.HashMap<>();
                app.put("appName", pm.getApplicationLabel(appInfo).toString());
                app.put("packageName", appInfo.packageName);
                app.put("isSystemApp", isSystemApp);  // V3.3: thêmhệ thốngứng dụngđánh dấuchí
                
                // lấy ứng dụngicon（toànđộ phân giải, khôngkhông）
                try {
                    Drawable icon = pm.getApplicationIcon(appInfo);
                    // sử dụngnguyênbắt đầuicon, khônggiới hạnlớnnhỏ
                    int iconSize = Math.max(icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
                    if (iconSize <= 0) iconSize = 192; // nếukhông cópháplấy, sử dụngmặc địnhcaođộ phân giải
                    
                    android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                        iconSize, iconSize, android.graphics.Bitmap.Config.ARGB_8888
                    );
                    android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                    icon.setBounds(0, 0, iconSize, iconSize);
                    icon.draw(canvas);
                    
                    java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream); // 100%lượng, không có
                    app.put("icon", stream.toByteArray());
                    
                    bitmap.recycle();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to get icon for " + appInfo.packageName);
                }
                
                apps.add(app);
            }
            
            // theotên ứng dụngsắp xếp
            apps.sort((a, b) -> {
                String nameA = (String) a.get("appName");
                String nameB = (String) b.get("appName");
                return nameA.compareToIgnoreCase(nameB);
            });
            
            Log.d(TAG, "Found " + apps.size() + " user apps");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to get installed apps", e);
        }
        
        return apps;
    }
}
