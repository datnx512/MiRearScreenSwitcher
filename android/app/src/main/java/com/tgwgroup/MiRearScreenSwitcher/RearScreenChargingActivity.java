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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * màn hình sauhoạt ảnh sạcActivity
 * hiển thị sạcicon、pinphânvàvào, 5giâysautự độngđóngvàkhôi phụcapp casthoặcLauncher chính thức
 */
public class RearScreenChargingActivity extends Activity {
    private static final String TAG = "RearScreenChargingActivity";
    private int rearTaskId = -1;  // màn hình sauapp casttaskId, -1hiển thịkhôngcóapp cast
    private boolean autoFinishScheduled = false; // cóđãxếptự độnghủy
    
    // instance static, ngăn chặncũinstancecan thiệpmớiinstance
    private static volatile RearScreenChargingActivity currentInstance = null;
    private static volatile long currentInstanceCreateTime = 0;
    
    // staticpincập nhậtphương thức, choChargingServicegọi trực tiếp
    public static void updateBatteryLevelStatic(int newLevel) {
        if (currentInstance != null) {
            currentInstance.updateBatteryLevel(newLevel);
        }
    }
    
    // broadcast receiver：nhậnkết thúc ngaylệnhvàpincập nhật
    private android.content.BroadcastReceiver finishReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            String action = intent.getAction();
            if ("com.tgwgroup.MiRearScreenSwitcher.FINISH_CHARGING_ANIMATION".equals(action)) {
                Log.d(TAG, "🔌 收到拔电广播，立即销毁");
                finish();
            } else if ("com.tgwgroup.MiRearScreenSwitcher.INTERRUPT_CHARGING_ANIMATION".equals(action)) {
                Log.d(TAG, "🔄 收到打断广播（新动画来了），立即销毁但不恢复Launcher");
                // đánh dấulàbị ngắt, onDestroykhôngkhôi phụcLauncher
                finish();
            } else if ("com.tgwgroup.MiRearScreenSwitcher.UPDATE_CHARGING_BATTERY".equals(action)) {
                // V3.5: nhậnpincập nhật
                int newLevel = intent.getIntExtra("batteryLevel", -1);
                Log.d(TAG, "📡 收到电量更新广播: " + newLevel + "%");
                if (newLevel >= 0) {
                    updateBatteryLevel(newLevel);
                }
            }
        }
    };
    
    public RearScreenChargingActivity() {
        super();
        long time = System.currentTimeMillis();
        Log.d(TAG, String.format("[%tT.%tL] 🟢 构造函数被调用", time, time));
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long onCreateStartTime = System.currentTimeMillis();
        Log.d(TAG, String.format("[%tT.%tL] 🟡 onCreate开始", onCreateStartTime, onCreateStartTime));
        
        super.onCreate(savedInstanceState);
        
        // kiểm trahiện tạisởởmàn hình
        int displayId = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            displayId = getDisplay().getDisplayId();
        }
        Log.d(TAG, String.format("[%tT.%tL] 📍 当前displayId=%d", onCreateStartTime, onCreateStartTime, displayId));
        
        int level = getIntent().getIntExtra("batteryLevel", 0);
        rearTaskId = getIntent().getIntExtra("rearTaskId", -1);
        
        // ✅ nếuởmàn hình chính(displayId == 0), đềukhông, chờbịchuyển đến màn hình sau
        if (displayId == 0) {
            Log.d(TAG, String.format("[%tT.%tL] 💤 在主屏启动，保持透明占位符，等待移动", 
                onCreateStartTime, onCreateStartTime));
            return; // khôngcài đặttrongchứa, khôngthêmflags, chỉlàtrong suốtchiếm chỗ
        }
        
        // --- bằngdướicodechỉ ởmàn hình sau(displayId == 1) ---
        Log.d(TAG, String.format("[%tT.%tL] 🎯 在背屏执行，开始设置内容", onCreateStartTime, onCreateStartTime));
        
        // V3.3: giữgiữ sáng + khóa màn hìnhhiển thị
        getWindow().addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        );
        
        // phân phốimớiAPI：khi khóa màn hìnhhiển thị
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
        }
        
        // V3.5: tối ưukết xuấttínhcó thể（giảiDequeueBuffertimeout）
        getWindow().setFlags(
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );
        
        // V3.16: gỡ bỏ120Hzlàm lạicài đặt, hệ thống tự độngquản lýtốc độ làm mới
        
        // ⚠️ keyphím：ở setContentView trước đósử dụngmàn hình sauDPI！
        forceRearScreenDensityBeforeInflate();
        
        setContentView(R.layout.activity_rear_screen_charging);
        
        long afterSetContentViewTime = System.currentTimeMillis();
        Log.d(TAG, String.format("[%tT.%tL] 🟠 setContentView完成", 
            afterSetContentViewTime, afterSetContentViewTime));
        
        
        long afterGetIntentTime = System.currentTimeMillis();
        Log.d(TAG, String.format("[%tT.%tL] ⚡ Intent数据: Battery=%d%%, rearTaskId=%d", 
            afterGetIntentTime, afterGetIntentTime, level, rearTaskId));
        
        // V3.5: lấytoàn màn hìnhchất lỏngđồ
        LightningShapeView fullScreenLiquid = findViewById(R.id.full_screen_liquid);
        TextView batteryText = findViewById(R.id.battery_text);
        View chargingContainer = findViewById(R.id.charging_container);
        
        // cài đặttoàn màn hìnhchất lỏngchế độ
        fullScreenLiquid.setFullScreenMode(true);
        
        // ứng dụngtoànphân vùngmarginđếnpinsốchữ
        applySafeAreaToText(batteryText);
        
        // cài đặtpinvănchữ
        batteryText.setText(level + "%");
        
        // khởi độngtoàn màn hìnhchất lỏngsạchoạt ảnh（tuyếntính, từ0đếnpinphân）
        startFullScreenLiquidAnimation(fullScreenLiquid, level);
        
        // khởi độngpinsốchữhiện dầnhoạt ảnh
        startCenterTextAnimation(batteryText);
        
        long animationStartTime = System.currentTimeMillis();
        
        // V3.5: kiểm trasạccông tắc giữ sáng
        boolean chargingAlwaysOn = getSharedPreferences("mrss_settings", MODE_PRIVATE)
            .getBoolean("charging_always_on_enabled", false);
        
        if (chargingAlwaysOn) {
            Log.d(TAG, String.format("[%tT.%tL] 🎬 动画已启动，充电常亮模式，不自动关闭", 
                animationStartTime, animationStartTime));
        } else {
            Log.d(TAG, String.format("[%tT.%tL] 🎬 动画已启动，8秒后自动关闭", 
                animationStartTime, animationStartTime));
            // 8giâysautự độngđóng
            chargingContainer.postDelayed(this::finish, 8000);
        }
        autoFinishScheduled = true;
        
        long onCreateEndTime = System.currentTimeMillis();
        Log.d(TAG, String.format("[%tT.%tL] ✅ onCreate完成 (总耗时%dms)", 
            onCreateEndTime, onCreateEndTime, onCreateEndTime - onCreateStartTime));
        
        // đăng kýbroadcast receiver（lắng nghe rút điện、ngắtvàpincập nhậtsự kiện）
        android.content.IntentFilter finishFilter = new android.content.IntentFilter();
        finishFilter.addAction("com.tgwgroup.MiRearScreenSwitcher.FINISH_CHARGING_ANIMATION");
        finishFilter.addAction("com.tgwgroup.MiRearScreenSwitcher.INTERRUPT_CHARGING_ANIMATION");
        finishFilter.addAction("com.tgwgroup.MiRearScreenSwitcher.UPDATE_CHARGING_BATTERY");  // V3.5: lắng nghepincập nhật
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(finishReceiver, finishFilter, android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(finishReceiver, finishFilter);
        }
        
        // đăng kýLocalBroadcastManagerreceiver（lắng nghepincập nhật）
        // androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(finishReceiver, finishFilter);
        Log.d(TAG, String.format("[%tT.%tL] ✅ 已注册充电动画广播接收器", onCreateEndTime, onCreateEndTime));
        Log.d(TAG, "📡 广播接收器已注册，监听: FINISH_CHARGING_ANIMATION, INTERRUPT_CHARGING_ANIMATION, UPDATE_CHARGING_BATTERY");
        
        // cài đặtlàhiện tạiinstance
        currentInstance = this;
        currentInstanceCreateTime = onCreateEndTime;
        
        // đothửcodeđã gỡ
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        long resumeTime = System.currentTimeMillis();
        Log.d(TAG, String.format("[%tT.%tL] 🟢 onResume", resumeTime, resumeTime));
        
        // V3.3: lần nữađảm bảoWindow flags（giữgiữ sáng + khóa màn hìnhhiển thị）
        getWindow().addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        );
        
        // đảm bảokhóa màn hìnhhiển thịcài đặtliên tục
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
        }

        // ：nhânmàn hình chính chiếm chỗchưaxếptự độnghủy, thìởmàn hình sauresumethời gianxếp
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            int displayId = getDisplay() != null ? getDisplay().getDisplayId() : 0;
            if (displayId == 1 && !autoFinishScheduled) {
                // V3.5: kiểm trasạccông tắc giữ sáng
                boolean chargingAlwaysOn = getSharedPreferences("mrss_settings", MODE_PRIVATE)
                    .getBoolean("charging_always_on_enabled", false);
                
                if (!chargingAlwaysOn) {
                    Log.d(TAG, "⏱️ 未安排自动销毁，补偿安排5秒后finish");
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finish, 5000);
                } else {
                    Log.d(TAG, "💡 充电常亮模式，不自动销毁");
                }
                autoFinishScheduled = true;
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        long destroyTime = System.currentTimeMillis();
        Log.d(TAG, String.format("[%tT.%tL] 🔴 onDestroy被调用", destroyTime, destroyTime));
        
        // hủy đăng kýbroadcast receiver
        try {
            unregisterReceiver(finishReceiver);
            Log.d(TAG, String.format("[%tT.%tL] ✅ 已注销充电动画广播接收器", destroyTime, destroyTime));
        } catch (Exception e) {
            Log.w(TAG, "Failed to unregister finish receiver: " + e.getMessage());
        }
        
        // hủy đăng kýLocalBroadcastManagerreceiver
        // try {
        //     androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(finishReceiver);
        // Log.d(TAG, String.format("[%tT.%tL] ✅ đãhủy đăng kýLocalBroadcastManagerreceiver", destroyTime, destroyTime));
        // } catch (Exception e) {
        //     Log.w(TAG, "Failed to unregister LocalBroadcastManager receiver: " + e.getMessage());
        // }
        
        super.onDestroy();
        
        // kiểm tracólàhiện tạiinstance, ngăn chặncũinstancecan thiệpmớiinstance
        if (this != currentInstance) {
            Log.w(TAG, String.format("[%tT.%tL] ⚠️ 这是旧实例，跳过恢复操作", destroyTime, destroyTime));
            return;
        }
        
        // thông báo animation manager: hoạt ảnh sạckết thúc
        boolean shouldRestore = RearAnimationManager.endAnimation(RearAnimationManager.AnimationType.CHARGING);
        
        // chỉbình thườngkết thúcthời gianmớikhôi phụcLauncher, bị ngắtthời giankhôngkhôi phục
        if (!shouldRestore) {
            Log.d(TAG, String.format("[%tT.%tL] 🔄 充电动画被打断，跳过恢复Launcher", destroyTime, destroyTime));
            return;
        }
        
        // ởmàn hình saukhôi phụcapp casthoặcLauncher chính thức（tất nhiênởmàn hình sauthời gian）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            int currentDisplayId = getDisplay() != null ? getDisplay().getDisplayId() : 0;
            Log.d(TAG, String.format("[%tT.%tL] 📍 当前displayId=%d", destroyTime, destroyTime, currentDisplayId));
            
            if (currentDisplayId == 1) {
                final int finalTaskId = rearTaskId;
                
                // ởbackgroundthreadkhôi phụcthao tác, khôngonDestroy
                new Thread(() -> {
                    try {
                        // chờ50mschoActivity hoàn toànhủy
                        Thread.sleep(50);
                        
                        if (finalTaskId > 0) {
                            Log.d(TAG, "⚡ 恢复投送app (taskId=" + finalTaskId + ")");
                            restoreProjectedApp(finalTaskId);
                        } else {
                            Log.d(TAG, "⚡ 恢复官方Launcher");
                            restoreOfficialLauncher();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in restore thread", e);
                    }
                }).start();
            }
        }
    }
    
    private void restoreProjectedApp(int taskId) {
        try {
            // quaChargingServicelấyTaskServicevàkhôi phụcapp cast
            ITaskService taskService = ChargingService.getTaskService();
            if (taskService != null) {
                // bước1: trướctắt Launcher chính thức（ngăn chặnnómàn hình sau）
                taskService.disableSubScreenLauncher();
                
                // bước2: chờ200mschohệ thốngổn địnhchắc chắn（thêmtrễ）
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                    Log.e(TAG, "Sleep interrupted while waiting for system to stabilize", ignored);
                }
                
                // bước3: chuyểnapp castquayđếnmàn hình sau
                taskService.executeShellCommand(
                    "service call activity_task 50 i32 " + taskId + " i32 1"
                );
                
                // bước4: lạichờ200msđảm bảoappđãchuyển
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                    Log.e(TAG, "Sleep interrupted while ensuring app moved to rear screen", ignored);
                }
                
                // bước5: lần nữachắc chắnnhậnchuyển（nặnggiữ）
                taskService.executeShellCommand(
                    "service call activity_task 50 i32 " + taskId + " i32 1"
                );
                
                // bước6: chờ300mschoapphoàn toànhiển thị
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                    Log.e(TAG, "Sleep interrupted while waiting for app to fully display", ignored);
                }
                
                // bước7: khôngbậtLauncher chính thức（giữtắttrạng thái, choapp casttiếp tụctheomàn hình sau）
                // taskService.enableSubScreenLauncher(); // ❌ khôngcầnbật, ngược lạisẽmàn hình sau
                
                // bước8: khởi động lạiRearScreenKeeperServicegiám sátkhôi phụcapp
                restartKeeperService(taskId);
                
                Log.d(TAG, "✅ Projected app restored (taskId=" + taskId + ")");
            } else {
                Log.w(TAG, "TaskService not available from ChargingService");
                // quaylùiđếnMainActivity
                MainActivity mainActivity = MainActivity.getCurrentInstance();
                if (mainActivity != null) {
                    mainActivity.executeShellCommand(
                        "service call activity_task 50 i32 " + taskId + " i32 1"
                    );
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore projected app", e);
            // nếukhôi phụcapp castthất bại, khôi phụcgiám sátvàquaylùiđếnLauncher chính thức
            RearScreenKeeperService.resumeMonitoring();
            restoreOfficialLauncher();
        }
    }
    
    private void restartKeeperService(int taskId) {
        try {
            // lấytên packagevàtaskIdthông tin
            String lastTask = SwitchToRearTileService.getLastMovedTask();
            if (lastTask != null) {
                // khởi độngRearScreenKeeperService
                Intent serviceIntent = new Intent(this, RearScreenKeeperService.class);
                serviceIntent.putExtra("lastMovedTask", lastTask);
                
                // V2.5: truyền trạng thái công tắc màn hình sau giữ sáng
                try {
                    android.content.SharedPreferences prefs = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE);
                    boolean keepScreenOnEnabled = prefs.getBoolean("flutter.keep_screen_on_enabled", true);
                    serviceIntent.putExtra("keepScreenOnEnabled", keepScreenOnEnabled);
                } catch (Exception e) {
                    // mặc định là bật
                    serviceIntent.putExtra("keepScreenOnEnabled", true);
                }
                
                startService(serviceIntent);
                
                Log.d(TAG, "🔄 RearScreenKeeperService restarted for: " + lastTask);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart RearScreenKeeperService", e);
        }
    }
    
    private void restoreOfficialLauncher() {
        try {
            // quaChargingServicelấyTaskServicevàkhôi phụcLauncher chính thức
            ITaskService taskService = ChargingService.getTaskService();
            if (taskService != null) {
                taskService.executeShellCommand(
                    "am start --display 1 -n com.xiaomi.subscreencenter/.subscreenlauncher.SubScreenLauncherActivity"
                );
                Log.d(TAG, "✅ Official launcher restored");
            } else {
                Log.w(TAG, "TaskService not available from ChargingService");
                // quaylùiđếnMainActivity
                MainActivity mainActivity = MainActivity.getCurrentInstance();
                if (mainActivity != null) {
                    mainActivity.executeShellCommand(
                        "am start --display 1 -n com.xiaomi.subscreencenter/.subscreenlauncher.SubScreenLauncherActivity"
                    );
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore official launcher", e);
        }
    }
    
    /**
 * ởinflatebố cụctrước đósử dụngmàn hình sauDPI
 */
    private void forceRearScreenDensityBeforeInflate() {
        try {
            // từcachelấymàn hình sauDPI（phân phốisởcónhỏmàn hìnhthiết bị）
            RearDisplayHelper.RearDisplayInfo info = DisplayInfoCache.getInstance().getCachedInfo();
            int rearScreenDpi = info.densityDpi;
            
            // nếucachechưakhởi tạo, ngaydumpsyslấythậtthựcDPI
            if (rearScreenDpi <= 0) {
                Log.w(TAG, "⚠️ 背屏DPI未缓存，尝试实时获取");
                
                // thửlấyTaskService（thử lại）
                ITaskService taskService = null;
                for (int retry = 0; retry < 3; retry++) {
                    taskService = ChargingService.getTaskService();
                    if (taskService == null) {
                        taskService = NotificationService.getTaskService();
                    }
                    
                    if (taskService != null) {
                        break;
                    }
                    
                    Log.w(TAG, String.format("⏳ TaskService未连接，重试 %d/3", retry + 1));
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                if (taskService != null) {
                    try {
                        DisplayInfoCache.getInstance().initialize(taskService);
                        info = DisplayInfoCache.getInstance().getCachedInfo();
                        rearScreenDpi = info.densityDpi;
                        Log.d(TAG, "✅ 实时获取背屏DPI: " + rearScreenDpi);
                    } catch (Exception e) {
                        Log.e(TAG, "❌ 实时获取背屏DPI失败", e);
                        return;
                    }
                } else {
                    Log.e(TAG, "❌ TaskService重试3次后仍不可用，跳过DPI强制");
                    return;
                }
            }
            
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            int currentDpi = metrics.densityDpi;
            
            Log.d(TAG, String.format("🔧 inflate前 - 当前DPI=%d, 背屏DPI=%d", currentDpi, rearScreenDpi));
            
            metrics.densityDpi = rearScreenDpi;
            metrics.density = rearScreenDpi / 160f;
            metrics.scaledDensity = metrics.density;
            
            android.content.res.Configuration config = new android.content.res.Configuration(getResources().getConfiguration());
            config.densityDpi = rearScreenDpi;
            
            getResources().updateConfiguration(config, metrics);
            
            Log.d(TAG, String.format("✅ inflate前已强制应用背屏DPI: %d", metrics.densityDpi));
                
        } catch (Exception e) {
            Log.e(TAG, "❌ inflate前应用DPI失败", e);
        }
    }
    
    /**
 * V3.5: toàn màn hìnhchất lỏngsạchoạt ảnh（tuyếntính, từ0đếnmụcđánh dấupin）
 */
    private void startFullScreenLiquidAnimation(LightningShapeView liquidView, int targetLevel) {
        // mụcđánh dấusạcví dụ
        float targetFillLevel = targetLevel / 100f;
        
        // tạotuyếntínhsạchoạt ảnh（DecelerateInterpolator - kết quả）
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, targetFillLevel);
        animator.setDuration(2000); // 2giâysạchoạt ảnh
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator(2.5f));
        
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            liquidView.setFillLevel(animatedValue);
        });
        
        animator.start();
        Log.d(TAG, String.format("🌊 全屏液体填充动画已启动: 0%% → %d%%", targetLevel));
    }
    
    /**
 * V3.5: trongpinsốchữhiện dầnhoạt ảnh
 */
    private void startCenterTextAnimation(TextView textView) {
        textView.setAlpha(0f);
        textView.setScaleX(0.8f);
        textView.setScaleY(0.8f);
        
        textView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setStartDelay(600) // chất lỏngsạcbắt đầusau hiển thị
            .setInterpolator(new android.view.animation.DecelerateInterpolator(2.0f))
            .start();
    }
    
    /**
 * V3.5: cập nhật hiển thị pin（sạcchế độ giữ sángdướithựcthời giancập nhật）
 */
    private void updateBatteryLevel(int newLevel) {
        try {
            Log.d(TAG, "🔋 开始更新电量: " + newLevel + "%");
            LightningShapeView liquidView = findViewById(R.id.full_screen_liquid);
            TextView batteryText = findViewById(R.id.battery_text);
            
            if (liquidView != null && batteryText != null) {
                // cập nhậtchất lỏngsạc
                liquidView.setFillLevel(newLevel / 100f);
                // cập nhậtsốchữ
                batteryText.setText(newLevel + "%");
                Log.d(TAG, "🔋 电量已更新: " + newLevel + "%");
            } else {
                Log.w(TAG, "⚠️ 视图未找到，无法更新电量 - liquidView=" + (liquidView != null) + ", batteryText=" + (batteryText != null));
            }
        } catch (Exception e) {
            Log.w(TAG, "更新电量失败: " + e.getMessage());
        }
    }
    
    /**
 * V3.5: ứng dụngtoànphân vùngđếnpinsốchữ（đảm bảosốchữhiển thịởtoànphân vùngtrong）
 */
    private void applySafeAreaToText(TextView textView) {
        try {
            // từcachelấythông tin màn hình sau
            RearDisplayHelper.RearDisplayInfo info = DisplayInfoCache.getInstance().getCachedInfo();
            
            if (info == null) {
                Log.w(TAG, "⚠️ 背屏信息缓存为空");
                return;
            }
            
            if (!info.hasCutout()) {
                Log.d(TAG, "ℹ️ 背屏无Cutout，数字自动居中");
                return;
            }
            
            // cài đặtmarginchosốchữtrongởtoànphân vùng
            if (textView.getLayoutParams() instanceof android.widget.FrameLayout.LayoutParams) {
                android.widget.FrameLayout.LayoutParams params = 
                    (android.widget.FrameLayout.LayoutParams) textView.getLayoutParams();
                
                params.leftMargin = info.cutout.left;
                params.topMargin = info.cutout.top;
                params.rightMargin = info.cutout.right;
                params.bottomMargin = info.cutout.bottom;
                textView.setLayoutParams(params);
                
                Log.d(TAG, String.format("✅ 电量数字已应用安全区域: left=%d, top=%d, right=%d, bottom=%d",
                    info.cutout.left, info.cutout.top, info.cutout.right, info.cutout.bottom));
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 应用安全区域失败", e);
        }
    }
}

