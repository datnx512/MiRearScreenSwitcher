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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;

import rikka.shizuku.Shizuku;

/**
 * dịch vụ lắng nghe trạng thái sạc
 * lắng nghe sự kiện kết nối nguồn, sau khi cắm điện ở màn hình sau hiển thị hoạt ảnh pin sạc
 */
public class ChargingService extends Service {
    private static final String TAG = "ChargingService";
    private SharedPreferences prefs;
    private ITaskService taskService;
    private PowerManager.WakeLock wakeLock;
    
    // instance static, cho RearScreenChargingActivity truy cập
    private static ChargingService instance;
    
    // ngăn kích hoạt lặphoạt ảnh（thời gian chờ）
    private long lastChargingAnimationTime = 0;
    private static final long CHARGING_ANIMATION_COOLDOWN_MS = 6000; // 6giâythời gian chờ
    
    // V3.5: chế độ hoạt ảnh sạc giữ sáng
    private boolean chargingAlwaysOnEnabled = false;
    private Handler wakeupHandler;
    private Runnable wakeupRunnable;
    private boolean isWakeupRunning = false;
    
    public static ITaskService getTaskService() {
        return instance != null ? instance.taskService : null;
    }
    
    private final Shizuku.UserServiceArgs serviceArgs = 
        new Shizuku.UserServiceArgs(new ComponentName("com.tgwgroup.MiRearScreenSwitcher", TaskService.class.getName()))
            .daemon(false)
            .processNameSuffix("charging_task_service")
            .debuggable(false)
            .version(1);
    
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
    
    // V3.5: broadcast receiver thay đổi cài đặt
    private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "收到设置变化广播");
            chargingAlwaysOnEnabled = prefs.getBoolean("charging_always_on_enabled", false);
            Log.d(TAG, "充电动画常亮: " + chargingAlwaysOnEnabled);
        }
    };
    
    // V3.5: broadcast receiver khôi phục hoạt ảnh sạc
    private BroadcastReceiver resumeChargingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.tgwgroup.MiRearScreenSwitcher.RESUME_CHARGING_ANIMATION".equals(intent.getAction())) {
                Log.d(TAG, "🔋 收到恢复充电动画广播，准备恢复");
                
                // lấy pin hiện tại
                int batteryLevel = getBatteryLevel(context);
                
                // sau khi trễ khởi động lại hoạt ảnh sạc
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        // thông báo animation manager: bắt đầu hoạt ảnh sạc
                        RearAnimationManager.startAnimation(RearAnimationManager.AnimationType.CHARGING);
                        
                        // khởi độnghoạt ảnh sạc
                        showChargingOnRearScreen(batteryLevel, false);
                        
                        // nếu chế độ giữ sáng bật, khởi động vòng lặp đánh thức
                        if (chargingAlwaysOnEnabled) {
                            Log.d(TAG, "💡 常亮模式开启，启动wakeup循环");
                            startWakeupAndUpdateLoop();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "恢复充电动画失败", e);
                    }
                }, 300);  // trễ 300ms, đảm bảo Activity thông báo hủy hoàn toàn
            }
        }
    };
    
    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                // kiểm tra trạng thái công tắc
                boolean enabled = prefs.getBoolean("charging_animation_enabled", true);
                if (!enabled) {
                    Log.d(TAG, "Charging animation disabled");
                    return;
                }
                
                // kiểm tra thời gian chờ (ngăn kích hoạt lặp)
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastChargingAnimationTime < CHARGING_ANIMATION_COOLDOWN_MS) {
                    Log.d(TAG, "⏸ Charging animation in cooldown, skipping");
                    return;
                }
                
                // kiểm tra trạng thái khóa màn hình
                android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                boolean isLocked = km != null && km.isKeyguardLocked();
                
                if (isLocked) {
                    Log.d(TAG, "🔓 Screen is locked, will show charging animation with screen sleep");
                } else {
                    Log.d(TAG, "🔓 Screen is unlocked, will show charging animation without screen sleep");
                }
                
                int batteryLevel = getBatteryLevel(context);
                Log.d(TAG, "🔌 Power connected, battery: " + batteryLevel + "%");
                
                // ghi thời điểm kích hoạt
                lastChargingAnimationTime = currentTime;
                
                // thông báo animation manager: bắt đầu hoạt ảnh sạc (trả về hoạt ảnh cũ bị ngắt)
                RearAnimationManager.AnimationType oldAnim = RearAnimationManager.startAnimation(RearAnimationManager.AnimationType.CHARGING);
                
                // nếu có hoạt ảnh cũ cần ngắt, gửi broadcast ngắt
                if (oldAnim == RearAnimationManager.AnimationType.NOTIFICATION) {
                    Log.d(TAG, "检测到通知动画正在播放，发送打断广播");
                    RearAnimationManager.sendInterruptBroadcast(ChargingService.this, RearAnimationManager.AnimationType.NOTIFICATION);
                }
                
                showChargingOnRearScreen(batteryLevel, isLocked);
                
                // V3.5: nếu đã bật hoạt ảnh sạc giữ sáng, khởi động vòng lặp đánh thức và cập nhật
                if (chargingAlwaysOnEnabled) {
                    Log.d(TAG, "充电动画常亮已开启，启动wakeup循环");
                    startWakeupAndUpdateLoop();
                }
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                // rút bộ sạc, hủy hoạt ảnh sạc ngay
                Log.d(TAG, "🔌 Power disconnected, finishing charging animation");
                
                // V3.5: dừng vòng lặp đánh thức
                stopWakeupLoop();
                
                finishChargingAnimation();
            }
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ChargingService created");
        
        // lưu instance
        instance = this;
        
        prefs = getSharedPreferences("mrss_settings", Context.MODE_PRIVATE);
        
        // thêm Shizuku listener
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);  // lắng nghe sự kiện rút điện
        registerReceiver(batteryReceiver, filter);
        
        // V3.5: đăng ký broadcast receiver thay đổi cài đặt
        IntentFilter settingsFilter = new IntentFilter("com.tgwgroup.MiRearScreenSwitcher.RELOAD_CHARGING_SETTINGS");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, settingsFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(settingsReceiver, settingsFilter);
        }
        
        // V3.5: đăng ký broadcast receiver khôi phục hoạt ảnh sạc
        IntentFilter resumeFilter = new IntentFilter("com.tgwgroup.MiRearScreenSwitcher.RESUME_CHARGING_ANIMATION");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(resumeChargingReceiver, resumeFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(resumeChargingReceiver, resumeFilter);
        }
        
        // V3.5: tải cài đặt hoạt ảnh sạc giữ sáng
        chargingAlwaysOnEnabled = prefs.getBoolean("charging_always_on_enabled", false);
        wakeupHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        
        // bind TaskService
        bindTaskService();
        
        // khởi động foreground servicegiữ sống (sử dụng thống nhất thông báo kernel service)
        startForeground(NOTIFICATION_ID, RearScreenKeeperService.createServiceNotification(this));
        Log.d(TAG, "✓ 前台服务已启动（使用内核服务通知）");
    }
    
    private static final int NOTIFICATION_ID = 1001; // và Service khác cùng dùng ID

    private void acquireWakeLock(long timeoutMs) {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                if (wakeLock == null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MRSS:ChargingWake");
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
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ChargingService started");
        
        // đảm bảo TaskService đã bind
        if (taskService == null) {
            bindTaskService();
        }
        
        return START_STICKY;
    }
    
    private void bindTaskService() {
        try {
            if (!Shizuku.pingBinder()) {
                Log.w(TAG, "Shizuku not available");
                return;
            }
            
            if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No Shizuku permission");
                return;
            }
            
            Shizuku.bindUserService(serviceArgs, taskServiceConnection);
            Log.d(TAG, "Binding TaskService...");
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind TaskService", e);
        }
    }
    
    private int getBatteryLevel(Context context) {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }
    
    /**
 * kết thúc hoạt ảnh sạc ngay
 */
    private void finishChargingAnimation() {
        try {
            // qua broadcast thông báo RearScreenChargingActivity kết thúc ngay
            Intent finishIntent = new Intent("com.tgwgroup.MiRearScreenSwitcher.FINISH_CHARGING_ANIMATION");
            finishIntent.setPackage(getPackageName());
            sendBroadcast(finishIntent);
                Log.d(TAG, "已发送结束充电动画的广播");
        } catch (Exception e) {
            Log.e(TAG, "Failed to finish charging animation", e);
        }
    }
    
    private void showChargingOnRearScreen(int level, boolean isLocked) {
        showChargingOnRearScreenWithRetry(level, isLocked, 0);
    }
    
    private void showChargingOnRearScreenWithRetry(int level, boolean isLocked, int retryCount) {
        if (taskService == null) {
            if (retryCount < 10) {  // tối đa thử lại 10 lần（tổng cộng 1 giây）
                Log.w(TAG, "TaskService not available, retry " + (retryCount + 1) + "/10");
                // thử lại trễ
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    showChargingOnRearScreenWithRetry(level, isLocked, retryCount + 1);
                }, 100);
                return;
            } else {
                Log.e(TAG, "TaskService still not available after 10 retries, aborting");
                return;
            }
        }
        
        acquireWakeLock(8000);
        try {
            // bước1: kiểm tra màn hình sau có ứng dụng cast
            String lastTask = SwitchToRearTileService.getLastMovedTask();
            int rearTaskId = -1;
            
            if (lastTask != null && lastTask.contains(":")) {
                try {
                    String rearForegroundApp = taskService.getForegroundAppOnDisplay(1);
                    
                    // nếuhiện tạimàn hình sau foregroundlàhoạt ảnh sạc, giải thíchtrênmột lầnhoạt ảnhvẫnkhônghủy hoàn toàn, sử dụnglastTask
                    if (rearForegroundApp != null && rearForegroundApp.contains("RearScreenChargingActivity")) {
                        Log.d(TAG, "充电动画正在显示，使用lastTask: " + lastTask);
                        String[] parts = lastTask.split(":");
                        rearTaskId = Integer.parseInt(parts[1]);
                    } else if (rearForegroundApp != null && rearForegroundApp.equals(lastTask)) {
                        // màn hình sauchắc chắnthựccó app castở
                        String[] parts = lastTask.split(":");
                        rearTaskId = Integer.parseInt(parts[1]);
                        Log.d(TAG, "背屏有投送app: " + lastTask);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "检查背屏app失败", e);
                }
            }
            
            // bước2: nếu cóapp cast, tạm dừng giám sát RearScreenKeeperService
            if (rearTaskId > 0) {
                RearScreenKeeperService.pauseMonitoring();
            }
            
            // bước3: tắt Launcher chính thức
            taskService.disableSubScreenLauncher();
            
            long startTime = System.currentTimeMillis();
            Log.d(TAG, String.format("[%tT.%tL] 开始启动充电动画", startTime, startTime));
            
            // V3.3: gỡ bỏ tất cả code đánh thức và mở khóa, tránh khi khóa màn hình chuyển đến giao diện mật khẩu
            
            // bước4: sử dụng chiến lược MRSN - tr tiên ở màn hình chính tàng hình khởi động, sau đóchuyển đến màn hình sau
            try {
                // 4.1: tr tiên ở màn hình chính khởi động（Activity sẽ tự ẩn ở onCreate）
                String componentName = getPackageName() + "/" + RearScreenChargingActivity.class.getName();
                String mainCmd = String.format(
                    "am start -n %s --ei batteryLevel %d --ei rearTaskId %d",
                    componentName,
                    level,
                    rearTaskId
                );
                
                Log.d(TAG, String.format("[%tT.%tL] 🔵 在主屏启动Activity", System.currentTimeMillis(), System.currentTimeMillis()));
                taskService.executeShellCommand(mainCmd);
                
                // 4.2: poll lấy taskId（tối đa 60 lần x 30ms = 1800ms, trong thời gian gửi lại lệnh）
                String chargingTaskId = null;
                int attempts = 0;
                int maxAttempts = 60;
                
                while (chargingTaskId == null && attempts < maxAttempts) {
                    Thread.sleep(30);
                    String result = taskService.executeShellCommandWithResult("am stack list | grep RearScreenChargingActivity");
                    if (result != null && !result.trim().isEmpty()) {
                        // parse taskId=XXX
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("taskId=(\\d+)");
                        java.util.regex.Matcher matcher = pattern.matcher(result);
                        if (matcher.find()) {
                            chargingTaskId = matcher.group(1);
                            Log.d(TAG, String.format("[%tT.%tL] 找到taskId=%s (尝试%d次)", 
                                System.currentTimeMillis(), System.currentTimeMillis(), chargingTaskId, attempts + 1));
                            break;
                        }
                    }
                    attempts++;
                    if (attempts == 20 || attempts == 40) { // giữa chừnggửi lại một hai lần lệnh khởi động
                        Log.d(TAG, String.format("[%tT.%tL] 重新发送主屏启动命令", System.currentTimeMillis(), System.currentTimeMillis()));
                        taskService.executeShellCommand(mainCmd);
                    }
                }
                
                if (chargingTaskId != null) {
                    // 4.3: chuyển đến màn hình sau
                    String moveCmd = "service call activity_task 50 i32 " + chargingTaskId + " i32 1";
                    taskService.executeShellCommand(moveCmd);
                    Thread.sleep(40); // chờ chuyển xong
                    
                    // 4.4: chỉ khi khóa màn hình đóng màn hình chính（khi sáng màn hình không cần đóng）
                    if (isLocked) {
                        // chức năng màn hình chính ngủđã gỡ
                        Log.d(TAG, String.format("[%tT.%tL] 锁屏状态，主屏已关闭", 
                            System.currentTimeMillis(), System.currentTimeMillis()));
                    } else {
                        Log.d(TAG, String.format("[%tT.%tL] 亮屏状态，保持主屏开启", 
                            System.currentTimeMillis(), System.currentTimeMillis()));
                    }
                    
                    long endTime = System.currentTimeMillis();
                    Log.d(TAG, String.format("[%tT.%tL] 充电动画已移动到背屏 (总耗时%dms)", 
                        endTime, endTime, endTime - startTime));
                } else {
                    Log.e(TAG, String.format("[%tT.%tL] 未能找到taskId, 尝试了%d次", 
                        System.currentTimeMillis(), System.currentTimeMillis(), attempts));
                }
            } catch (Exception e) {
                long errorTime = System.currentTimeMillis();
                Log.e(TAG, String.format("[%tT.%tL] 启动充电动画失败", errorTime, errorTime), e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing charging", e);
        } finally {
            releaseWakeLock();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // dừng foreground service
        stopForeground(true);
        
        // xóa instance static
        instance = null;
        
        // gỡ Shizuku listener
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener);
            Shizuku.removeBinderDeadListener(binderDeadListener);
        } catch (Exception e) {
            Log.e(TAG, "Error removing Shizuku listeners", e);
        }
        
        try {
            unregisterReceiver(batteryReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering battery receiver", e);
        }
        
        // V3.5: hủy đăng ký receiver thay đổi cài đặt
        try {
            unregisterReceiver(settingsReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering settings receiver", e);
        }
        
        // V3.5: hủy đăng ký receiver khôi phục hoạt ảnh sạc
        try {
            unregisterReceiver(resumeChargingReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering resume charging receiver", e);
        }
        
        // V3.5: dừng vòng lặp đánh thức
        stopWakeupLoop();
        
        // unbind TaskService
        try {
            if (taskService != null) {
                Shizuku.unbindUserService(serviceArgs, taskServiceConnection, true);
                taskService = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unbinding TaskService", e);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    // V3.5: khởi động vòng lặp đánh thức và cập nhật pin
    private void startWakeupAndUpdateLoop() {
        if (isWakeupRunning) {
            Log.w(TAG, "⚠️ Wakeup loop already running");
            return;
        }
        
        isWakeupRunning = true;
        
        wakeupRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isWakeupRunning) return;
                
                // kiểm tra trạng thái công tắc
                boolean enabled = prefs.getBoolean("charging_always_on_enabled", false);
                if (!enabled) {
                    Log.d(TAG, "充电动画常亮已关闭，停止循环");
                    stopWakeupLoop();
                    return;
                }
                
                // gửi lệnh wakeup
                try {
                    if (taskService != null) {
                        taskService.executeShellCommand("input -d 1 keyevent KEYCODE_WAKEUP");
                        Log.d(TAG, "✓ Wakeup sent");
                    } else {
                        Log.w(TAG, "⚠️ TaskService is null, skipping wakeup");
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "发送wakeup失败: " + t.getMessage());
                }
                
                // cập nhật hiển thị pin hoạt ảnh sạc
                try {
                    int batteryLevel = getBatteryLevel(getApplicationContext());
                    // trực tiếpgọi phương thức static cập nhật pin
                    RearScreenChargingActivity.updateBatteryLevelStatic(batteryLevel);
                    Log.d(TAG, "🔋 电量已直接更新: " + batteryLevel + "%");
                } catch (Exception e) {
                    Log.w(TAG, "更新电量失败: " + e.getMessage());
                }
                
                // sau 100ms tiếp tục
                wakeupHandler.postDelayed(this, 100);
            }
        };
        
        // bắt đầu ngay
        wakeupHandler.post(wakeupRunnable);
        Log.d(TAG, "✓ Wakeup and update loop started");
    }
    
    // V3.5: dừng vòng lặp đánh thức
    private void stopWakeupLoop() {
        isWakeupRunning = false;
        if (wakeupHandler != null && wakeupRunnable != null) {
            wakeupHandler.removeCallbacks(wakeupRunnable);
        }
        Log.d(TAG, "✓ Wakeup loop stopped");
    }
}

