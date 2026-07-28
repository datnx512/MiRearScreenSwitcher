/*
 * Author: AntiOblivionis
 * QQ: 319641317
 * Github: https://github.com/GoldenglowSusie/
 * Bilibili: Rhodes Island T0 Thuật sư điều khiển cơ giới Chengshan
 *
 * Chief Tester: �? *
 * Co-developed with AI assistants:
 * - Cursor
 * - Claude-4.5-Sonnet
 * - GPT-5
 * - Gemini-2.5-Pro
 */

package com.tgwgroup.MiRearScreenSwitcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * màn hình sauthông báohiển thịActivity
 * hiển thịứng dụngicon、tênvàthông báotrongchứa, tuyếntínhhoạt ảnhkết quả
 */
public class RearScreenNotificationActivity extends Activity {
    private static final String TAG = "RearScreenNotificationActivity";
    
    // instance static
    private static volatile RearScreenNotificationActivity currentInstance = null;
    
    private String packageName;
    private boolean contentInitialized = false;  // đánh dấutrongchứacóđãkhởi tạo
    
    // thông báo hoạt ảnhtrong thời gianliên tụcđánh thức vàkilllauncher
    private android.os.Handler wakeupHandler;
    private Runnable wakeupRunnable;
    private boolean isWakeupRunning = false;
    
    // broadcast receiver：nhậnngắtlệnh
    private android.content.BroadcastReceiver interruptReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            if ("com.tgwgroup.MiRearScreenSwitcher.INTERRUPT_NOTIFICATION_ANIMATION".equals(intent.getAction())) {
                Log.d(TAG, "🔄 收到打断广播（新动画来了），立即销毁但不恢复Launcher");
                finish();
            }
        }
    };
    
    public RearScreenNotificationActivity() {
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
        
        // lấyIntentdữ liệu
        packageName = getIntent().getStringExtra("packageName");
        String title = getIntent().getStringExtra("title");
        String text = getIntent().getStringExtra("text");
        long when = getIntent().getLongExtra("when", System.currentTimeMillis());
        boolean darkMode = getIntent().getBooleanExtra("darkMode", false);
        
        // ⚠️ keyphím：ở setContentView trước đósử dụngmàn hình sauDPI
        forceRearScreenDensityBeforeInflate();
        
        // ✅ thống nhấtcài đặtbố cục, đảm bảo màn hình chính chiếm chỗ xong chuyển đến màn hình sau cũng hiển thị bình thường
        setContentView(R.layout.activity_rear_screen_notification);
        
        // ứng dụngchế độ tốihoặcthườngbố cụcgọi
        if (darkMode) {
            applyDarkMode();
        } else {
            applyRegularLayout();
        }
        
        // V3.2: giữgiữ sáng + khóa màn hìnhhiển thị
        getWindow().addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        );
        
        // phân phốimớiAPI：khi khóa màn hìnhhiển thị
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
        }
        
        // cài đặtcửa sổsau, ngăn chặntrênquaymặtthời gianra
        getWindow().setBackgroundDrawableResource(R.drawable.bg_gradient_rear_screen);
        
        // nếuởmàn hình chính khởi động, chỉlàchiếm chỗ, trướcẩntrongchứa, chờchuyển đến màn hình sau
        if (displayId == 0) {
            Log.d(TAG, String.format("[%tT.%tL] 💤 在主屏启动(占位)，隐藏内容等待移动", 
                onCreateStartTime, onCreateStartTime));
            View container = findViewById(R.id.notification_container);
            container.setVisibility(View.INVISIBLE);
            // đánh dấutrongchứachưakhởi tạo, chờonResumeởmàn hình sauthời giankhởi tạo
            contentInitialized = false;
            Log.d(TAG, String.format("[%tT.%tL] ⏸️ 占位符模式，contentInitialized=false", 
                onCreateStartTime, onCreateStartTime));
            
            // đăng kýbroadcast receiver（tức làsử dụnglàchiếm chỗcũngcầnđăng ký）
            android.content.IntentFilter interruptFilter = new android.content.IntentFilter("com.tgwgroup.MiRearScreenSwitcher.INTERRUPT_NOTIFICATION_ANIMATION");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(interruptReceiver, interruptFilter, android.content.Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(interruptReceiver, interruptFilter);
            }
            currentInstance = this;
            return;
        }
        
        // --- bằngdướicodechỉ ởmàn hình sau ---
        Log.d(TAG, String.format("[%tT.%tL] 🎯 在背屏执行，开始设置内容", onCreateStartTime, onCreateStartTime));
        
        contentInitialized = true;
        
        // lấythông tin màn hình sauvàứng dụngtoànphân vùngphân phối
        applySafeAreaPadding();
        
        // lấyđồ
        ImageView appIconCenter = findViewById(R.id.app_icon_center);
        ImageView appIconSmall = findViewById(R.id.app_icon_small);
        TextView appNameText = findViewById(R.id.app_name);
        TextView notificationTitle = findViewById(R.id.notification_title);
        TextView notificationContent = findViewById(R.id.notification_content);
        View container = findViewById(R.id.notification_container);
        View appNameContainer = findViewById(R.id.app_name_container);
        View contentContainer = findViewById(R.id.notification_content_container);
        
        // tảiứng dụngthông tin
        try {
            PackageManager pm = getPackageManager();
            android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            String appName = pm.getApplicationLabel(appInfo).toString();
            Drawable icon = pm.getApplicationIcon(packageName);
            
            // cài đặticonvàtên ứng dụng
            appIconCenter.setImageDrawable(icon);
            appIconSmall.setImageDrawable(icon);
            appNameText.setText(appName);
            
            // cài đặtthông báotiêu đềvàtrongchứa
            if (title != null && !title.isEmpty()) {
                notificationTitle.setText(title);
                notificationTitle.setVisibility(View.VISIBLE);
            } else {
                notificationTitle.setVisibility(View.GONE);
            }
            
            if (text != null && !text.isEmpty()) {
                notificationContent.setText(text);
                notificationContent.setVisibility(View.VISIBLE);
            } else {
                notificationContent.setVisibility(View.GONE);
            }
            
            // nếutiêu đềlàtrống, ẩngiữa
            if (title == null || title.isEmpty()) {
                notificationContent.setPadding(
                    notificationContent.getPaddingLeft(),
                    0,
                    notificationContent.getPaddingRight(),
                    notificationContent.getPaddingBottom()
                );
            }
            
            Log.d(TAG, String.format("[%tT.%tL] 📱 通知: %s - %s: %s", 
                onCreateStartTime, onCreateStartTime, appName, title, text));
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to load app info", e);
            appNameText.setText(packageName);
            notificationTitle.setText(title);
            notificationContent.setText(text);
        }
        
        // bắt đầuhoạt ảnh
        startNotificationAnimation(appIconCenter, appNameContainer, contentContainer);
        
        // nhấpchuyển đếnứng dụng（ưutr tiên ởmàn hình saukhởi động）
        container.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            Log.d(TAG, String.format("[%tT.%tL] 👆 收到点击，准备跳转 package=%s", clickTime, clickTime, packageName));
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                Log.d(TAG, String.format("[%tT.%tL] 🔍 getLaunchIntentForPackage -> %s", clickTime, clickTime, (launchIntent == null ? "null" : String.valueOf(launchIntent.getComponent()))));
                if (launchIntent == null) {
                    Log.w(TAG, String.format("[%tT.%tL] ⚠️ 无法获取启动Intent: %s", clickTime, clickTime, packageName));
                    finish();
                    return;
                }
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                boolean started = false;
                // thửtrực tiếpởmàn hình chính khởi động（ActivityOptions -> display=0）
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        android.app.ActivityOptions opts = android.app.ActivityOptions.makeBasic();
                        // setLaunchDisplayId ởmột phầnROMcó thểngười dùng；khôngcó thểngười dùngsẽexception, vàovàofallback
                        java.lang.reflect.Method m = android.app.ActivityOptions.class.getMethod("setLaunchDisplayId", int.class);
                        m.invoke(opts, 0);
                        Log.d(TAG, String.format("[%tT.%tL] 🚀 尝试ActivityOptions在display=0(主屏)启动", clickTime, clickTime));
                        // bật sángmàn hình chính
                        try {
                            ITaskService tsWake = NotificationService.getTaskService();
                            if (tsWake != null) {
                                tsWake.executeShellCommand("// 主屏唤醒功能已移除");
                                Log.d(TAG, String.format("[%tT.%tL] ✓ 已唤醒主屏", clickTime, clickTime));
                            }
                        } catch (Throwable ignored) {
                            Log.e(TAG, "Failed to wake main screen via TaskService", ignored);
                        }
                        startActivity(launchIntent, opts.toBundle());
                        started = true;
                        Log.d(TAG, String.format("[%tT.%tL] ✓ ActivityOptions主屏启动成功", clickTime, clickTime));
                    }
                } catch (Throwable t) {
                    Log.w(TAG, String.format("[%tT.%tL] 🔁 ActivityOptions不可用，转fallback: %s", clickTime, clickTime, t.getMessage()));
                }

                if (!started) {
                    // quaylùi：sử dụngshellởmàn hình chính khởi động
                    try {
                        String component = null;
                        if (launchIntent.getComponent() != null) {
                            component = launchIntent.getComponent().flattenToShortString();
                        }
                        if (component == null) {
                            // parsemặc địnhLAUNCHER Activity
                            android.content.pm.PackageManager pm = getPackageManager();
                            Intent resolve = new Intent(Intent.ACTION_MAIN);
                            resolve.addCategory(Intent.CATEGORY_LAUNCHER);
                            resolve.setPackage(packageName);
                            android.content.pm.ResolveInfo ri = pm.resolveActivity(resolve, 0);
                            if (ri != null && ri.activityInfo != null) {
                                component = ri.activityInfo.packageName + "/" + ri.activityInfo.name;
                            }
                        }
                        Log.d(TAG, String.format("[%tT.%tL] 🧭 解析到组件: %s", clickTime, clickTime, String.valueOf(component)));
                        if (component != null) {
                            ITaskService ts = NotificationService.getTaskService();
                            if (ts != null) {
                                // trướcđánh thứcmàn hình chính
                                try {
                                    ts.executeShellCommand("// 主屏唤醒功能已移除");
                                    Log.d(TAG, String.format("[%tT.%tL] ✓ 已唤醒主屏", clickTime, clickTime));
                                } catch (Throwable ignored) {
                                    Log.e(TAG, "Failed to wake main screen via TaskService fallback", ignored);
                                }
                                String cmd = "am start --display 0 -n " + component;
                                boolean ok = ts.executeShellCommand(cmd);
                                if (!ok) {
                                    // cóROMkhông --display 0, lùihóalàmặc địnhhiển thị
                                    cmd = "am start -n " + component;
                                    ok = ts.executeShellCommand(cmd);
                                    Log.d(TAG, String.format("[%tT.%tL] 🔁 改为默认显示启动，结果=%s", clickTime, clickTime, ok));
                                }
                                Log.d(TAG, String.format("[%tT.%tL] ✓ Fallback shell主屏启动%s 组件=%s", clickTime, clickTime, ok ? "成功" : "失败", component));
                                started = ok;
                            } else {
                                Log.w(TAG, String.format("[%tT.%tL] ⚠️ TaskService为空，无法shell启动", clickTime, clickTime));
                            }
                        }
                    } catch (Throwable t) {
                        Log.w(TAG, String.format("[%tT.%tL] Fallback shell start 异常: %s", clickTime, clickTime, t.getMessage()));
                    }
                }

                Log.d(TAG, String.format("[%tT.%tL] 🧹 结束通知Activity，started=%s", clickTime, clickTime, started));
                // không cónhưkết thúchiện tạiActivity thông báo
                finish();
            } catch (Exception e) {
                Log.e(TAG, String.format("[%tT.%tL] ❌ 启动失败: %s", clickTime, clickTime, e.getMessage()), e);
                finish();
            }
        });
        
        // V3.4: theocài đặtthời giantự độngđóng
        int duration = getSharedPreferences("mrss_settings", MODE_PRIVATE).getInt("notification_duration", 10);
        container.postDelayed(this::finish, duration * 1000L);
        
        long onCreateEndTime = System.currentTimeMillis();
        Log.d(TAG, String.format("[%tT.%tL] ✓ onCreate完成 (总耗时%dms)", 
            onCreateEndTime, onCreateEndTime, onCreateEndTime - onCreateStartTime));
        
        // đăng kýbroadcast receiver（lắng nghengắtsự kiện）
        android.content.IntentFilter interruptFilter = new android.content.IntentFilter("com.tgwgroup.MiRearScreenSwitcher.INTERRUPT_NOTIFICATION_ANIMATION");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(interruptReceiver, interruptFilter, android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(interruptReceiver, interruptFilter);
        }
        Log.d(TAG, String.format("[%tT.%tL] ✓ 已注册通知动画广播接收器", onCreateEndTime, onCreateEndTime));
        
        // cài đặtlàhiện tạiinstance
        currentInstance = this;
        
        // khởi tạowakeupvòng lặp
        wakeupHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        
        // nếuởmàn hình sau, khởi động ngaywakeupvòng lặp
        if (displayId == 1) {
            startWakeupAndKillLoop();
        }
    }
    
    /**
 * thông báo hoạt ảnh
 * 1. lớnicontừtrong
 * 2. tên ứng dụngcontainerhiện dần
 * 3. thông báotrongchứacontainerhiện dần
 */
    private void startNotificationAnimation(ImageView iconCenter, View appNameContainer, View contentContainer) {
        // khởi tạotrạng thái
        appNameContainer.setAlpha(0f);
        appNameContainer.setScaleX(0.9f);
        appNameContainer.setScaleY(0.9f);
        contentContainer.setAlpha(0f);
        contentContainer.setTranslationY(30f);
        
        // bậtphầnthêm
        iconCenter.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        appNameContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        contentContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // giai đoạn1: trongiconlớn (0-300ms)
        iconCenter.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(300)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .withEndAction(() -> {
                // giai đoạn1.5: dừnggiữhiển thị (300-800ms) - thêm500msdừnggiữthời gian
                iconCenter.postDelayed(() -> {
                    // giai đoạn2: trongiconnhỏẩn dần (800-1000ms)
                    iconCenter.animate()
                        .scaleX(0.5f)
                        .scaleY(0.5f)
                        .alpha(0f)
                        .setDuration(200)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> {
                            iconCenter.setVisibility(View.GONE);
                            iconCenter.setLayerType(View.LAYER_TYPE_NONE, null);
                        })
                        .start();
                    
                    // giai đoạn3: tên ứng dụngcontainerhiện dầnvà(800-1050ms)
                    appNameContainer.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(250)
                        .setStartDelay(50)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> {
                            appNameContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                        })
                        .start();
                    
                    // giai đoạn4: thông báotrongchứacontainertừdướicáchvào (950-1250ms)
                    contentContainer.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setStartDelay(150)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> {
                            contentContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                        })
                        .start();
                }, 500); // dừnggiữ500ms
            })
            .start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        long resumeTime = System.currentTimeMillis();
        Log.d(TAG, String.format("[%tT.%tL] 🟢 onResume", resumeTime, resumeTime));
        
        // V3.2: lần nữađảm bảoWindow flags（giữgiữ sáng + khóa màn hìnhhiển thị）
        getWindow().addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        );
        
        // đảm bảokhóa màn hìnhhiển thịcài đặtliên tục
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
        }
        
        // đảm bảoHandlerđãkhởi tạo
        if (wakeupHandler == null) {
            wakeupHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        }
        
        // kiểm tracóđãtừmàn hình chínhchuyển đến màn hình sau（chiếm chỗlàthực tếhiển thị）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            int currentDisplayId = getDisplay() != null ? getDisplay().getDisplayId() : 0;
            View container = findViewById(R.id.notification_container);
            
            Log.d(TAG, String.format("[%tT.%tL] 🔍 检查移动: displayId=%d, contentInitialized=%s, container=%s", 
                resumeTime, resumeTime, currentDisplayId, contentInitialized, (container != null ? "存在" : "null")));
            
            // nếuhiện tạiởmàn hình sauvàtrongchứavẫnchưakhởi tạo（giải thíchlàtừmàn hình chínhchuyểnquá trình）
            if (currentDisplayId == 1 && !contentInitialized && container != null) {
                Log.d(TAG, String.format("[%tT.%tL] 🔄 检测到从主屏移动到背屏，初始化内容", resumeTime, resumeTime));
                
                // hiển thịtrongchứa
                container.setVisibility(View.VISIBLE);
                
                // lấyIntentdữ liệuvàkhởi tạotrongchứa
                String title = getIntent().getStringExtra("title");
                String text = getIntent().getStringExtra("text");
                
                // ứng dụngtoànphân vùngphân phối（chỉgọi một lần）
                applySafeAreaPadding();
                contentInitialized = true;
                
                // lấyđồ
                ImageView appIconCenter = findViewById(R.id.app_icon_center);
                ImageView appIconSmall = findViewById(R.id.app_icon_small);
                TextView appNameText = findViewById(R.id.app_name);
                TextView notificationTitle = findViewById(R.id.notification_title);
                TextView notificationContent = findViewById(R.id.notification_content);
                View appNameContainer = findViewById(R.id.app_name_container);
                View contentContainer = findViewById(R.id.notification_content_container);
                
                // tảiứng dụngthông tin
                try {
                    PackageManager pm = getPackageManager();
                    android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    String appName = pm.getApplicationLabel(appInfo).toString();
                    Drawable icon = pm.getApplicationIcon(packageName);
                    
                    // cài đặticonvàtên ứng dụng
                    appIconCenter.setImageDrawable(icon);
                    appIconSmall.setImageDrawable(icon);
                    appNameText.setText(appName);
                    
                    // cài đặtthông báotiêu đềvàtrongchứa
                    if (title != null && !title.isEmpty()) {
                        notificationTitle.setText(title);
                        notificationTitle.setVisibility(View.VISIBLE);
                    } else {
                        notificationTitle.setVisibility(View.GONE);
                    }
                    
                    if (text != null && !text.isEmpty()) {
                        notificationContent.setText(text);
                        notificationContent.setVisibility(View.VISIBLE);
                    } else {
                        notificationContent.setVisibility(View.GONE);
                    }
                    
                    // nếutiêu đềlàtrống, ẩngiữa
                    if (title == null || title.isEmpty()) {
                        notificationContent.setPadding(
                            notificationContent.getPaddingLeft(),
                            0,
                            notificationContent.getPaddingRight(),
                            notificationContent.getPaddingBottom()
                        );
                    }
                    
                    Log.d(TAG, String.format("[%tT.%tL] 📱 通知: %s - %s: %s", 
                        resumeTime, resumeTime, appName, title, text));
                    
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load app info in onResume", e);
                    appNameText.setText(packageName);
                    if (title != null) notificationTitle.setText(title);
                    if (text != null) notificationContent.setText(text);
                }
                
                // khởi độnghoạt ảnh
                startNotificationAnimation(appIconCenter, appNameContainer, contentContainer);
                
                // cài đặtnhấpsự kiệnvàtự độngđóng
                container.setOnClickListener(v -> {
                    long clickTime = System.currentTimeMillis();
                    Log.d(TAG, String.format("[%tT.%tL] 👆 收到点击，准备跳转 package=%s", clickTime, clickTime, packageName));
                    try {
                        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                        Log.d(TAG, String.format("[%tT.%tL] 🔍 getLaunchIntentForPackage -> %s", clickTime, clickTime, (launchIntent == null ? "null" : String.valueOf(launchIntent.getComponent()))));
                        if (launchIntent == null) {
                            Log.w(TAG, String.format("[%tT.%tL] ⚠️ 无法获取启动Intent: %s", clickTime, clickTime, packageName));
                            finish();
                            return;
                        }
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                        boolean started = false;
                        // thửtrực tiếpởmàn hình chính khởi động（ActivityOptions -> display=0）
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                android.app.ActivityOptions opts = android.app.ActivityOptions.makeBasic();
                                java.lang.reflect.Method m = android.app.ActivityOptions.class.getMethod("setLaunchDisplayId", int.class);
                                m.invoke(opts, 0);
                                Log.d(TAG, String.format("[%tT.%tL] 🚀 尝试ActivityOptions在display=0(主屏)启动", clickTime, clickTime));
                                // bật sángmàn hình chính
                                try {
                                    ITaskService tsWake = NotificationService.getTaskService();
                                    if (tsWake != null) {
                                        tsWake.executeShellCommand("// 主屏唤醒功能已移除");
                                        Log.d(TAG, String.format("[%tT.%tL] ✓ 已唤醒主屏", clickTime, clickTime));
                                    }
                                } catch (Throwable ignored) {
                                    Log.e(TAG, "Failed to wake main screen via TaskService", ignored);
                                }
                                startActivity(launchIntent, opts.toBundle());
                                started = true;
                                Log.d(TAG, String.format("[%tT.%tL] ✓ ActivityOptions主屏启动成功", clickTime, clickTime));
                            }
                        } catch (Throwable t) {
                            Log.w(TAG, String.format("[%tT.%tL] 🔁 ActivityOptions不可用，转fallback: %s", clickTime, clickTime, t.getMessage()));
                        }

                        if (!started) {
                            // quaylùi：sử dụngshellởmàn hình chính khởi động
                            try {
                                String component = null;
                                if (launchIntent.getComponent() != null) {
                                    component = launchIntent.getComponent().flattenToShortString();
                                }
                                if (component == null) {
                                    // parsemặc địnhLAUNCHER Activity
                                    android.content.pm.PackageManager pm = getPackageManager();
                                    Intent resolve = new Intent(Intent.ACTION_MAIN);
                                    resolve.addCategory(Intent.CATEGORY_LAUNCHER);
                                    resolve.setPackage(packageName);
                                    android.content.pm.ResolveInfo ri = pm.resolveActivity(resolve, 0);
                                    if (ri != null && ri.activityInfo != null) {
                                        component = ri.activityInfo.packageName + "/" + ri.activityInfo.name;
                                    }
                                }
                                Log.d(TAG, String.format("[%tT.%tL] 🧭 解析到组件: %s", clickTime, clickTime, String.valueOf(component)));
                                if (component != null) {
                                    ITaskService ts = NotificationService.getTaskService();
                                    if (ts != null) {
                                        // trướcđánh thứcmàn hình chính
                                        try {
                                            ts.executeShellCommand("// 主屏唤醒功能已移除");
                                            Log.d(TAG, String.format("[%tT.%tL] ✓ 已唤醒主屏", clickTime, clickTime));
                                        } catch (Throwable ignored) {
                                            Log.e(TAG, "Failed to wake main screen via TaskService fallback", ignored);
                                        }
                                        String cmd = "am start --display 0 -n " + component;
                                        boolean ok = ts.executeShellCommand(cmd);
                                        if (!ok) {
                                            cmd = "am start -n " + component;
                                            ok = ts.executeShellCommand(cmd);
                                            Log.d(TAG, String.format("[%tT.%tL] 🔁 改为默认显示启动，结果=%s", clickTime, clickTime, ok));
                                        }
                                        Log.d(TAG, String.format("[%tT.%tL] ✓ Fallback shell主屏启动%s 组件=%s", clickTime, clickTime, ok ? "成功" : "失败", component));
                                        started = ok;
                                    } else {
                                        Log.w(TAG, String.format("[%tT.%tL] ⚠️ TaskService为空，无法shell启动", clickTime, clickTime));
                                    }
                                }
                            } catch (Throwable t) {
                                Log.w(TAG, String.format("[%tT.%tL] Fallback shell start 异常: %s", clickTime, clickTime, t.getMessage()));
                            }
                        }

                        Log.d(TAG, String.format("[%tT.%tL] 🧹 结束通知Activity，started=%s", clickTime, clickTime, started));
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, String.format("[%tT.%tL] ❌ 启动失败: %s", clickTime, clickTime, e.getMessage()), e);
                        finish();
                    }
                });
                
                // V3.4: theocài đặtthời giantự độngđóng
                int duration = getSharedPreferences("mrss_settings", MODE_PRIVATE).getInt("notification_duration", 10);
                container.postDelayed(this::finish, duration * 1000L);
                
                // khởi độngwakeupvòng lặp
                startWakeupAndKillLoop();
                
                Log.d(TAG, String.format("[%tT.%tL] ✓ 移动后初始化完成", resumeTime, resumeTime));
            } else if (currentDisplayId == 1 && !isWakeupRunning) {
                // nếuđãởmàn hình saunhưngvòng lặpchưakhởi động, cũngkhởi động（ngăn chặn）
                startWakeupAndKillLoop();
            }
        }
    }
    
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        long configTime = System.currentTimeMillis();
        
        // ghicấu hìnhthay đổi（dùng chodebug）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            int currentDisplayId = getDisplay() != null ? getDisplay().getDisplayId() : 0;
            int densityDpi = newConfig.densityDpi;
            
            Log.d(TAG, String.format("[%tT.%tL] ⚙️ 配置变化: displayId=%d, densityDpi=%d", 
                configTime, configTime, currentDisplayId, densityDpi));
        }
    }
    
    @Override
    protected void onDestroy() {
        long destroyTime = System.currentTimeMillis();
        Log.d(TAG, String.format("[%tT.%tL] 🔴 onDestroy被调用", destroyTime, destroyTime));
        
        // dừngwakeupvòng lặp
        stopWakeupLoop();
        
        // hủy đăng kýbroadcast receiver
        try {
            unregisterReceiver(interruptReceiver);
            Log.d(TAG, String.format("[%tT.%tL] ✓ 已注销通知动画广播接收器", destroyTime, destroyTime));
        } catch (Exception e) {
            Log.w(TAG, "Failed to unregister interrupt receiver: " + e.getMessage());
        }
        
        super.onDestroy();
        
        // kiểm tracólàhiện tạiinstance
        if (this != currentInstance) {
            Log.w(TAG, String.format("[%tT.%tL] ⚠️ 这是旧实例，跳过恢复操作", destroyTime, destroyTime));
            return;
        }
        
        // thông báo animation manager: thông báo hoạt ảnhkết thúc
        boolean shouldRestore = RearAnimationManager.endAnimation(RearAnimationManager.AnimationType.NOTIFICATION);
        
        // chỉbình thườngkết thúcthời gianmớikhôi phụcLauncher, bị ngắtthời giankhôngkhôi phục
        if (!shouldRestore) {
            Log.d(TAG, String.format("[%tT.%tL] 🔄 通知动画被打断，跳过恢复Launcher", destroyTime, destroyTime));
            return;
        }
        
        // V3.5: kiểm tracó cầnkhôi phục hoạt ảnh sạc（chế độ giữ sáng）
        if (RearAnimationManager.shouldResumeChargingAnimation()) {
            Log.d(TAG, String.format("[%tT.%tL] 🔋 检测到充电动画常亮模式，发送恢复广播", destroyTime, destroyTime));
            
            // gửikhôi phục hoạt ảnh sạcbroadcast
            android.content.Intent resumeIntent = new android.content.Intent("com.tgwgroup.MiRearScreenSwitcher.RESUME_CHARGING_ANIMATION");
            resumeIntent.setPackage(getPackageName());
            sendBroadcast(resumeIntent);
            
            // xóađánh dấu
            RearAnimationManager.clearChargingAlwaysOnFlag();
            return;  // khôngkhôi phụcLauncher chính thức
        }
        
        // ởmàn hình saukhôi phụcLauncher chính thức
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            int currentDisplayId = getDisplay() != null ? getDisplay().getDisplayId() : 0;
            Log.d(TAG, String.format("[%tT.%tL] 📍 当前displayId=%d", destroyTime, destroyTime, currentDisplayId));
            
            if (currentDisplayId == 1) {
                new Thread(() -> {
                    try {
                        // quaChargingServicelấyTaskService（NotificationServicecũngbind）
                        ITaskService taskService = ChargingService.getTaskService();
                        if (taskService != null) {
                            taskService.executeShellCommand(
                                "am start --display 1 -n com.xiaomi.mirror/.SubscreenLauncher"
                            );
                            Log.d(TAG, "✓ Official launcher restored");
                        } else {
                            Log.w(TAG, "TaskService not available");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to restore launcher", e);
                    }
                }).start();
            }
        }
    }
    
    @Override
    public void finish() {
        super.finish();
        // tắtchuyểnhoạt ảnh
        overridePendingTransition(0, 0);
    }
    
    /**
 * ởinflatebố cụctrước đósử dụngmàn hình sauDPI（tối ưukeyphím！）
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
                    taskService = NotificationService.getTaskService();
                    if (taskService == null) {
                        taskService = ChargingService.getTaskService();
                    }
                    
                    if (taskService != null) {
                        break;
                    }
                    
                    Log.w(TAG, String.format("⚠️ TaskService未连接，重试 %d/3", retry + 1));
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
                        Log.d(TAG, "✓ 实时获取背屏DPI: " + rearScreenDpi);
                    } catch (Exception e) {
                        Log.e(TAG, "❌ 实时获取背屏DPI失败", e);
                        return;
                    }
                } else {
                    Log.e(TAG, "❌ TaskService重试3次后仍不可用，跳过DPI强制");
                    return;
                }
            }
            
            // lấy hiện tại DisplayMetrics
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            int currentDpi = metrics.densityDpi;
            
            Log.d(TAG, String.format("🔧 inflate前 - 当前DPI=%d, 背屏DPI=%d", currentDpi, rearScreenDpi));
            
            // sửalàmàn hình sauDPI
            metrics.densityDpi = rearScreenDpi;
            metrics.density = rearScreenDpi / 160f;
            metrics.scaledDensity = metrics.density;
            
            // cùngthời giansửa Configuration
            android.content.res.Configuration config = new android.content.res.Configuration(getResources().getConfiguration());
            config.densityDpi = rearScreenDpi;
            
            // ứng dụngmớicấu hình
            getResources().updateConfiguration(config, metrics);
            
            Log.d(TAG, String.format("✓ inflate前已强制应用背屏DPI: %d (density=%.2f)", 
                metrics.densityDpi, metrics.density));
                
        } catch (Exception e) {
            Log.e(TAG, "❌ inflate前应用DPI失败", e);
        }
    }
    
    /**
 * sử dụngmàn hình sauDPI（hệ thốngDPI）
 */
    private void applyRearScreenDensity() {
        try {
            // lấymàn hình sauDPI
            RearDisplayHelper.RearDisplayInfo info = DisplayInfoCache.getInstance().getCachedInfo();
            int rearScreenDpi = info.densityDpi;
            
            // lấy hiện tại DisplayMetrics
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            int currentDpi = metrics.densityDpi;
            
            Log.d(TAG, String.format("🔧 当前DPI=%d, 背屏DPI=%d", currentDpi, rearScreenDpi));
            
            // nếuhiện tạiDPIvớimàn hình sauDPIkhôngmột, sửa
            if (currentDpi != rearScreenDpi) {
                Log.d(TAG, String.format("⚠️ DPI不匹配！强制使用背屏DPI: %d", rearScreenDpi));
                
                // sửa DisplayMetrics
                metrics.densityDpi = rearScreenDpi;
                metrics.density = rearScreenDpi / 160f;
                metrics.scaledDensity = metrics.density;
                
                // cùngthời giansửa Configuration
                android.content.res.Configuration config = getResources().getConfiguration();
                config.densityDpi = rearScreenDpi;
                
                // cập nhật Resources
                getResources().updateConfiguration(config, metrics);
                
                Log.d(TAG, String.format("✓ 已强制应用背屏DPI: %d, density=%.2f", 
                    metrics.densityDpi, metrics.density));
            } else {
                Log.d(TAG, "✓ DPI已匹配，无需调整");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 应用背屏DPI失败", e);
        }
    }
    
    /**
 * ứng dụngtoànphân vùngphân phối（bắt đầuCutout）
 */
    private void applySafeAreaPadding() {
        try {
            // từcachelấythông tin màn hình sau
            RearDisplayHelper.RearDisplayInfo info = DisplayInfoCache.getInstance().getCachedInfo();
            
            // nếukhôngcócutout, không cầnthêmxử lý
            if (!info.hasCutout()) {
                Log.d(TAG, "ℹ️ 背屏无Cutout，无需调整布局");
                return;
            }
            
            // lấytrongchứabố cụccontainer（RelativeLayout with id=notification_container）
            android.view.View contentLayout = findViewById(R.id.notification_container);
            if (contentLayout != null && contentLayout.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams params = 
                    (android.view.ViewGroup.MarginLayoutParams) contentLayout.getLayoutParams();
                
                // kiểm tracóđãcài đặtquá trìnhmargin（tránh lặp lạicài đặt）
                if (params.leftMargin == info.cutout.left && 
                    params.topMargin == info.cutout.top && 
                    params.rightMargin == info.cutout.right && 
                    params.bottomMargin == info.cutout.bottom) {
                    Log.d(TAG, "ℹ️ 安全区域margin已设置，跳过");
                    return;
                }
                
                // cài đặtmargin（bắt đầucutoutphân vùng）, saudải màusẽsạccutoutphân vùng
                params.leftMargin = info.cutout.left;
                params.topMargin = info.cutout.top;
                params.rightMargin = info.cutout.right;
                params.bottomMargin = info.cutout.bottom;
                contentLayout.setLayoutParams(params);
                
                Log.d(TAG, String.format("✓ 已应用安全区域margin: left=%d, top=%d, right=%d, bottom=%d",
                    info.cutout.left, info.cutout.top, info.cutout.right, info.cutout.bottom));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 应用安全区域失败", e);
        }
    }
    
    /**
 * ứng dụngchế độ tối
 * hiển thịđensau, trừ, vănchữsửalà, gọibố cục
 */
    private void applyDarkMode() {
        try {
            Log.d(TAG, "🌙 应用暗夜模式");
            
            // hiển thịđensau
            View darkBackground = findViewById(R.id.dark_mode_background);
            if (darkBackground != null) {
                darkBackground.setVisibility(View.VISIBLE);
                Log.d(TAG, "✓ 黑色背景层已显示");
            }
            
            // lấycơ thểthông báocontainer - đảm bảotrong
            View wrapperContainer = findViewById(R.id.notification_wrapper);
            if (wrapperContainer != null) {
                Log.d(TAG, "✓ 整体通知容器已获取，保持垂直居中");
            }
            
            // lấycontainer - gỡ bỏkết quả, cài đặtlàtrong suốt
            View contentContainer = findViewById(R.id.notification_content_container);
            if (contentContainer != null) {
                contentContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                
                // gỡ bỏcontainerpaddinggiới hạn, chovănchữcó thểđếnmàn hình
                contentContainer.setPadding(0, 0, 0, 0);
                Log.d(TAG, "✓ 毛玻璃容器已移除，布局已调整");
            }
            
            // lấy tên ứng dụngvàiconcontainer - gọicấu hìnhvớitiêu đềtrongchứađối
            View appNameContainer = findViewById(R.id.app_name_container);
            if (appNameContainer != null) {
                android.widget.LinearLayout.LayoutParams containerParams = (android.widget.LinearLayout.LayoutParams) appNameContainer.getLayoutParams();
                containerParams.leftMargin = 0; // vớitiêu đềtrongchứatráiđối
                appNameContainer.setLayoutParams(containerParams);
                Log.d(TAG, "✓ 应用名容器已调整，与标题内容左侧对齐");
            }
            
            // lấy tên ứng dụng - sửalà
            TextView appName = findViewById(R.id.app_name);
            if (appName != null) {
                appName.setTextColor(android.graphics.Color.WHITE);
                Log.d(TAG, "✓ 应用名称已设为白色");
            }
            
            // lấythông báotiêu đề - sửalà, giới hạn1, gọigiữa
            TextView notificationTitle = findViewById(R.id.notification_title);
            if (notificationTitle != null) {
                notificationTitle.setTextColor(android.graphics.Color.WHITE);
                notificationTitle.setMaxLines(1); // chế độ tốisửalà1
                
                // gọitiêu đềmargin, vớiiconđếntiêu đềgiữagiữmột
                android.widget.LinearLayout.LayoutParams titleParams = (android.widget.LinearLayout.LayoutParams) notificationTitle.getLayoutParams();
                titleParams.topMargin = 8; // vớiiconđếntiêu đềgiữamột
                notificationTitle.setLayoutParams(titleParams);
                Log.d(TAG, "✓ 通知标题已设为白色，限制1行，间距已调整");
            }
            
            // lấythông báotrongchứa - sửalà, giới hạn6, gọigiữa
            TextView notificationContent = findViewById(R.id.notification_content);
            if (notificationContent != null) {
                notificationContent.setTextColor(android.graphics.Color.WHITE);
                notificationContent.setMaxLines(6); // chế độ tốisửalà6
                
                // gọitrongchứamargin, vớitiêu đềđếntrongchứagiữagiữmột
                android.widget.LinearLayout.LayoutParams contentParams = (android.widget.LinearLayout.LayoutParams) notificationContent.getLayoutParams();
                contentParams.topMargin = 8; // vớitiêu đềđếntrongchứagiữamột
                notificationContent.setLayoutParams(contentParams);
                Log.d(TAG, "✓ 通知内容已设为白色，限制6行，间距已调整");
            }
            
            Log.d(TAG, "✓ 暗夜模式已应用 - 全黑背景，毛玻璃已移除，文字已变白，布局已调整");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 应用暗夜模式失败", e);
        }
    }
    
    /**
 * ứng dụngthườngbố cụcgọi
 * sử dụngchế độ tốibố cụcgọi, nhưnggiữnguyêncómàu sắc
 */
    private void applyRegularLayout() {
        try {
            Log.d(TAG, "🎨 应用常规布局调整");
            
            // lấycơ thểthông báocontainer - đảm bảotrong
            View wrapperContainer = findViewById(R.id.notification_wrapper);
            if (wrapperContainer != null) {
                Log.d(TAG, "✓ 整体通知容器已获取，保持垂直居中");
            }
            
            // lấycontainer - gỡ bỏkết quả, cài đặtlàtrong suốt
            View contentContainer = findViewById(R.id.notification_content_container);
            if (contentContainer != null) {
                // gỡ bỏkết quả, cài đặtlàtrong suốt
                contentContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                
                // gỡ bỏcontainerpaddinggiới hạn, chovănchữcó thểđếnmàn hình
                contentContainer.setPadding(0, 0, 0, 0);
                Log.d(TAG, "✓ 常规布局容器已调整，毛玻璃已移除");
            }
            
            // lấy tên ứng dụngvàiconcontainer - gọicấu hìnhvớitiêu đềtrongchứađối
            View appNameContainer = findViewById(R.id.app_name_container);
            if (appNameContainer != null) {
                android.widget.LinearLayout.LayoutParams containerParams = (android.widget.LinearLayout.LayoutParams) appNameContainer.getLayoutParams();
                containerParams.leftMargin = 0; // vớitiêu đềtrongchứatráiđối
                appNameContainer.setLayoutParams(containerParams);
                Log.d(TAG, "✓ 应用名容器已调整，与标题内容左侧对齐");
            }
            
            // lấy tên ứng dụng - sửalàvàthêm
            TextView appName = findViewById(R.id.app_name);
            if (appName != null) {
                appName.setTextColor(android.graphics.Color.WHITE);
                appName.setShadowLayer(3, 0, 1, android.graphics.Color.parseColor("#40000000"));
                Log.d(TAG, "✓ 应用名称已设为白色，添加阴影");
            }
            
            // lấythông báotiêu đề - sửalàvàthêm, gọigiữavàsố
            TextView notificationTitle = findViewById(R.id.notification_title);
            if (notificationTitle != null) {
                // sửalàvănchữvàthêm
                notificationTitle.setTextColor(android.graphics.Color.WHITE);
                notificationTitle.setShadowLayer(3, 0, 1, android.graphics.Color.parseColor("#40000000"));
                notificationTitle.setMaxLines(1); // giới hạn1
                
                // gọitiêu đềmargin, vớiiconđếntiêu đềgiữagiữmột
                android.widget.LinearLayout.LayoutParams titleParams = (android.widget.LinearLayout.LayoutParams) notificationTitle.getLayoutParams();
                titleParams.topMargin = 8; // vớiiconđếntiêu đềgiữamột
                notificationTitle.setLayoutParams(titleParams);
                Log.d(TAG, "✓ 通知标题已设为白色，添加阴影，间距已调整");
            }
            
            // lấythông báotrongchứa - gọigiữavàsố, giữnguyêncómàu sắc
            TextView notificationContent = findViewById(R.id.notification_content);
            if (notificationContent != null) {
                // sửalàvănchữvàthêm
                notificationContent.setTextColor(android.graphics.Color.WHITE);
                notificationContent.setShadowLayer(3, 0, 1, android.graphics.Color.parseColor("#40000000"));
                notificationContent.setMaxLines(6); // giới hạn6
                
                // gọitrongchứamargin, vớitiêu đềđếntrongchứagiữagiữmột
                android.widget.LinearLayout.LayoutParams contentParams = (android.widget.LinearLayout.LayoutParams) notificationContent.getLayoutParams();
                contentParams.topMargin = 8; // vớitiêu đềđếntrongchứagiữamột
                notificationContent.setLayoutParams(contentParams);
                Log.d(TAG, "✓ 通知内容已设为白色，添加阴影，间距已调整");
            }
            
            Log.d(TAG, "✓ 常规布局已调整 - 毛玻璃已移除，文字已变白并添加阴影");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 应用常规布局调整失败", e);
        }
    }
    
    /**
 * khởi độngthông báo hoạt ảnhtrong thời gianvòng lặp đánh thức
 */
    private void startWakeupAndKillLoop() {
        if (isWakeupRunning) {
            Log.w(TAG, "⚠️ Wakeup loop already running");
            return;
        }
        
        isWakeupRunning = true;
        
        wakeupRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isWakeupRunning) return;
                
                // kiểm traActivitycóvẫnở
                if (isFinishing()) {
                    stopWakeupLoop();
                    return;
                }
                
                // lấyTaskService
                ITaskService taskService = NotificationService.getTaskService();
                
                if (taskService != null) {
                    // gửi lệnh wakeup
                    try {
                        taskService.executeShellCommand("input -d 1 keyevent KEYCODE_WAKEUP");
                        Log.d(TAG, "✓ Wakeup sent");
                    } catch (Throwable t) {
                        Log.w(TAG, "发送wakeup失败: " + t.getMessage());
                    }
                } else {
                    Log.w(TAG, "⚠️ TaskService is null, skipping wakeup");
                }
                
                // sau 100ms tiếp tục
                if (wakeupHandler != null) {
                    wakeupHandler.postDelayed(this, 100);
                }
            }
        };
        
        // bắt đầu ngay
        if (wakeupHandler != null) {
            wakeupHandler.post(wakeupRunnable);
            Log.d(TAG, "✓ Wakeup loop started");
        }
    }
    
    /**
 * dừng vòng lặp đánh thức
 */
    private void stopWakeupLoop() {
        isWakeupRunning = false;
        if (wakeupHandler != null && wakeupRunnable != null) {
            wakeupHandler.removeCallbacks(wakeupRunnable);
        }
        Log.d(TAG, "✓ Wakeup loop stopped");
    }
}

