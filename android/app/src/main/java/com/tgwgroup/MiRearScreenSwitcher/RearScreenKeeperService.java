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
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import rikka.shizuku.Shizuku;

/**
 * foregroundService - giữmàn hình saugiữ sáng
 * 
 * làngười dùngServicemàkhônglàActivity：
 * - Activitygiải phápthất bại3lần（FLAG_NOT_FOCUSABLE、màn hìnhngoài、alpha=0đềusẽbịonStop）
 * - ServicekhôngsẽbịonPause/onStop, hệ thốngrấtkillforegroundService
 * - có thểtrực tiếpcóWakeLockgiữmàn hìnhgiữ sáng
 * 
 * chú ý：WakeLockcó thểsẽchocáimàn hìnhđềugiữgiữ sáng（không cóphápchắc chắnchắc chắndisplay）
 */
public class RearScreenKeeperService extends Service implements SensorEventListener {
    private static final String TAG = "RearScreenKeeperService";
    private static final String CHANNEL_ID = "rear_screen_keeper";
    private static final int NOTIFICATION_ID = 10001;

    private static RearScreenKeeperService instance = null;
    private PowerManager.WakeLock wakeLock;
    private Handler handler;
    private ITaskService taskService = null;

    // V12.3: khởi tạokill processchiến lược - chỉkill1lần, khôngliên tụcgiám sát
    private static final int INITIAL_KILL_COUNT = 1; // khởi tạokill1lần
    private static final long KILL_INTERVAL_MS = 200; // mỗi lầnkhoảng200ms

    // V12.1: cảm biến tiệm cậnlắng nghe
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private boolean isProximityCovered = false;
    private long lastProximityTime = 0;
    private static final long PROXIMITY_DEBOUNCE_MS = 1500; // chống rungđộng：1500mstrongkết nốitiếp tụcmớikích hoạt（thấp）

    // V2.2: tiệm cậncông tắc cảm biếntrạng thái
    private boolean proximitySensorEnabled = true; // mặc địnhbật

    // V14.5: lắng ngheứng dụngcóthủ côngchuyểnquaymàn hình chính
    private static final long CHECK_TASK_INTERVAL_MS = 2000; // 2giâykiểm tramột lần
    private String monitoredTaskInfo = null; // thức: "packageName:taskId"

    // V2.3: tạm thờitạm dừng giám sát（hoạt ảnh sạchiển thịtrong thời gian）
    private boolean monitoringPaused = false;

    // V2.4: liên tụcđánh thứcmàn hình sau（ngăn chặntự độngtắt màn hình）
    private static final long WAKEUP_INTERVAL_MS = 100; // liên tục gửi, 0.1 giâyđánh thứcmột lần（đốitắt màn hìnhkhông cócảm）
    private boolean keepScreenOnEnabled = true; // mặc địnhbậtmàn hình saugiữ sáng

    public static void pauseMonitoring() {
        if (instance != null) {
            instance.monitoringPaused = true;

            // ✅ sởcópendingkiểm travụ
            if (instance.handler != null) {
                instance.handler.removeCallbacks(instance.checkTaskRunnable);
                Log.d(TAG, "⏸️ Monitoring paused, all checks cancelled");
            } else {
                Log.d(TAG, "⏸️ Monitoring paused");
            }
        }
    }

    public static void resumeMonitoring() {
        if (instance != null) {
            instance.monitoringPaused = false;
            Log.d(TAG, "▶️ Monitoring resumed");

            // ✅ trễ5giâysaumớibắt đầukiểm tra, app castthời giankhôi phụcđếnforeground
            if (instance.handler != null) {
                instance.handler.removeCallbacks(instance.checkTaskRunnable);
                instance.handler.postDelayed(instance.checkTaskRunnable, 5000);
                Log.d(TAG, "⏰ Next check scheduled in 5 seconds");
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // tạothông báođạo
        createNotificationChannel();

        // tạoHandlerdùng chochắc chắnthời gianvụ
        handler = new Handler(Looper.getMainLooper());

        // V2.2: từSharedPreferenceskhôi phụccông tắc cảm biếntrạng thái
        loadProximitySensorSetting();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // V14.6: xử lýnhấpthông báotrả vềmàn hình chínhsự kiện
        if (intent != null && "ACTION_RETURN_TO_MAIN".equals(intent.getAction())) {

            // sẽgiám sátvụchuyểnquaymàn hình chính
            if (monitoredTaskInfo != null && monitoredTaskInfo.contains(":") && taskService != null) {
                try {
                    String[] parts = monitoredTaskInfo.split(":");
                    String packageName = parts[0];
                    int taskId = Integer.parseInt(parts[1]);

                    // lấy tên ứng dụng
                    String appName = getAppName(packageName);

                    taskService.moveTaskToDisplay(taskId, 0);

                    // trướcgỡ bỏthông báo foreground
                    stopForeground(Service.STOP_FOREGROUND_REMOVE);

                    // hiển thị Toast trễgợi ý
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        Toast.makeText(this, appName + " " + getString(R.string.toast_return_to_main), Toast.LENGTH_SHORT).show();
                    }, 100);

                    // dừngdịch vụ
                    stopSelf();
                    return START_NOT_STICKY;

                } catch (Exception e) {
                    Log.w(TAG, "Failed to return task to main", e);
                }
            }
        }

        // V2.2: xử lýtiệm cậncông tắc cảm biếncài đặt
        if (intent != null && "ACTION_SET_PROXIMITY_ENABLED".equals(intent.getAction())) {
            boolean enabled = intent.getBooleanExtra("enabled", true);
            proximitySensorEnabled = enabled;

            Log.d(TAG, "🔧 传感器开关状态已更新: " + enabled);

            // nếuđóngcảm biến, vàhiện tạiđanglắng nghe, thìhủy đăng kýlắng nghe
            if (!enabled && sensorManager != null && proximitySensor != null) {
                sensorManager.unregisterListener(this);
                Log.d(TAG, "⏸️ 传感器监听器已注销");
            }
            // nếumởcảm biến, vàhiện tạikhôngcólắng nghe, thìđăng kýlắng nghe
            else if (enabled && sensorManager != null && proximitySensor != null) {
                boolean registered = sensorManager.registerListener(this, proximitySensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
                if (registered) {
                    Log.d(TAG, "✅ 传感器监听器已注册");
                } else {
                    Log.w(TAG, "⚠ 传感器监听器注册失败");
                }
            }

            return START_STICKY;
        }

        // V2.5: xử lýmàn hình saucông tắc giữ sángcài đặt
        if (intent != null && "ACTION_SET_KEEP_SCREEN_ON_ENABLED".equals(intent.getAction())) {
            boolean enabled = intent.getBooleanExtra("enabled", true);
            keepScreenOnEnabled = enabled;

            Log.d(TAG, "🔆 背屏常亮开关已" + (enabled ? "开启" : "关闭"));

            // nếuđónggiữ sáng, dừnggửiWAKEUP
            if (!enabled && handler != null) {
                handler.removeCallbacks(wakeupRearScreenRunnable);
                Log.d(TAG, "⏸️ 背屏WAKEUP发送已停止");
            }
            // nếumởgiữ sáng, khởi độnggửiWAKEUP
            else if (enabled && handler != null) {
                handler.removeCallbacks(wakeupRearScreenRunnable);
                startRearScreenWakeup();
            }

            return START_STICKY;
        }

        try {
            // V14.7: trướctừIntentlấycầngiám sátthông tin task
            if (intent != null) {
                String newMonitoredTask = intent.getStringExtra("lastMovedTask");
                if (newMonitoredTask != null) {
                    monitoredTaskInfo = newMonitoredTask;
                }
            }

            // V2.5: từIntentlấymàn hình sautrạng thái công tắc giữ sáng
            if (intent != null) {
                keepScreenOnEnabled = intent.getBooleanExtra("keepScreenOnEnabled", true);
                Log.d(TAG, "🔆 背屏常亮开关状态: " + (keepScreenOnEnabled ? "开启" : "关闭"));
            }

            // V15.1: ngayhiển thịthông báo, khôngchờkhácthao tác
            Notification notification = buildNotification();
            startForeground(NOTIFICATION_ID, notification);

            // ởbackgroundthreadthời gianthao tác, khôngthông báohiển thị
            new Thread(() -> {
                // bindShizuku TaskService
                bindTaskService();

                // khởi tạocảm biến tiệm cận
                initProximitySensor();
            }).start();

            // 2. lấyWakeLockgiữmàn hìnhgiữ sáng
            if (wakeLock == null || !wakeLock.isHeld()) {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

                // sử dụngSCREEN_BRIGHT_WAKE_LOCKgiữmàn hìnhsáng
                // chú ý：nàysẽchomàn hìnhgiữsáng, nhưngcó thểkhông cóphápchắc chắnlàcáidisplay
                wakeLock = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK, // gỡ bỏACQUIRE_CAUSES_WAKEUPtránhđánh thứcmàn hình chính
                        "MRSS::RearScreenKeeper");

                // liên tụccóWakeLock（khôngcài đặttimeout）
                wakeLock.acquire();

            } else {
            }

            // 3. V12.2: khởi tạokill process（chỉkilllần, khôngliên tụcgiám sát）
            performInitialKills();

            // 4. V14.5: khởi độngchắc chắnkỳkiểm travụ
            if (monitoredTaskInfo != null) {
                startTaskMonitoring();
            }

            // 5. V2.5: khởi độngliên tụcđánh thứcmàn hình sau（0.5giây, theotrạng thái công tắc）
            startRearScreenWakeup();

        } catch (Exception e) {
            Log.e(TAG, "✗ Error starting service", e);
        }

        // START_STICKY: nếubịhệ thốngkill, sẽtự độngkhởi động lại
        return START_STICKY;
    }

    /**
 * V15.2: khởi độngvụlắng nghe - kiểmđoứng dụngcóởforeground
 * giám sátbịcastđếnmàn hình sauứng dụng, nếukhôngởforeground（bịđónghoặcchuyển）, tự độngdừngdịch vụvàxóathông báo
 */
    private final Runnable checkTaskRunnable = new Runnable() {
        @Override
        public void run() {
            // V2.3: nếugiám sátđãtạm dừng（hoạt ảnh sạchiển thịtrong）, bỏ quabảnlầnkiểm tra
            if (monitoringPaused) {
                handler.postDelayed(this, CHECK_TASK_INTERVAL_MS);
                return;
            }

            if (monitoredTaskInfo != null && taskService != null) {
                try {
                    // V15.2: kiểm tra màn hình sau(displayId=1)ứng dụng foreground có còn ứng dụng chúng ta giám sát
                    String rearForegroundApp = taskService.getForegroundAppOnDisplay(1);

                    // V2.3: xếptrừhoạt ảnh sạc/thông báo hoạt ảnh（tạm thờichiếm dụngmàn hình sau, khôngứnggâyServicehủy）
                    if (rearForegroundApp != null && (rearForegroundApp.contains("RearScreenChargingActivity")
                            || rearForegroundApp.contains("RearScreenNotificationActivity"))) {
                        // hoạt ảnh sạcđanghiển thị, bỏ quabảnlầnkiểm tra
                        handler.postDelayed(this, CHECK_TASK_INTERVAL_MS);
                        return;
                    }

                    // nếu ứng dụng foreground màn hình sau không phải ứng dụng chúng ta giám sát, giải thíchnóbịđónghoặcchuyển
                    if (rearForegroundApp == null || !rearForegroundApp.equals(monitoredTaskInfo)) {
                        // ứng dụngkhôngởmàn hình sau foreground（bịđónghoặcchuyển）, dừngdịch vụ
                        stopForeground(Service.STOP_FOREGROUND_REMOVE);
                        stopSelf();
                        return;
                    }

                    // tiếp tụclắng nghe
                    handler.postDelayed(this, CHECK_TASK_INTERVAL_MS);

                } catch (Exception e) {
                    Log.w(TAG, "Task check failed: " + e.getMessage());
                    handler.postDelayed(this, CHECK_TASK_INTERVAL_MS);
                }
            } else {
                handler.postDelayed(this, CHECK_TASK_INTERVAL_MS);
            }
        }
    };

    private void startTaskMonitoring() {
        if (monitoredTaskInfo != null && handler != null) {
            handler.postDelayed(checkTaskRunnable, CHECK_TASK_INTERVAL_MS);
        }
    }

    /**
 * V2.5: liên tụcđánh thứcmàn hình sauvụ - 0.5giâygửiWAKEUP, ngăn chặnmàn hình sautự độngtắt màn hình
 */
    private final Runnable wakeupRearScreenRunnable = new Runnable() {
        @Override
        public void run() {
            // kiểm tra trạng thái công tắc
            if (keepScreenOnEnabled && taskService != null) {
                try {
                    // vềmàn hình sau(displayId=1)gửiWAKEUPđánh thứcsố
                    taskService.executeShellCommand("input -d 1 keyevent KEYCODE_WAKEUP");
                    // Log.d(TAG, "✨ màn hình saugiữ sốngđánh thứcđãgửi"); // chú thíchbằngítlog
                } catch (Exception e) {
                    Log.w(TAG, "背屏唤醒失败: " + e.getMessage());
                }
            }

            // liên tục gửi, 0.5giâymột lần
            if (keepScreenOnEnabled) {
                handler.postDelayed(this, WAKEUP_INTERVAL_MS);
            }
        }
    };

    private void startRearScreenWakeup() {
        if (handler != null && keepScreenOnEnabled) {
            // ngaymột lầnđánh thức, sau đóbắt đầuliên tục gửi
            handler.post(wakeupRearScreenRunnable);
            Log.d(TAG, "⏰ 背屏持续唤醒已启动 (0.5秒间隔)");
        }
    }

    /**
 * V12.3: khởi tạokill process - chỉkill1lần, khôngliên tụcgiám sát
 */
    private void performInitialKills() {

        for (int i = 0; i < INITIAL_KILL_COUNT; i++) {
            final int killNumber = i + 1;

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (taskService != null) {
                        try {
                            taskService.killLauncherProcess();
                        } catch (Exception e) {
                            Log.w(TAG, "⚠ Kill #" + killNumber + " failed: " + e.getMessage());
                        }
                    } else {
                        Log.w(TAG, "⚠ TaskService not available for kill #" + killNumber);
                    }

                    // lần cuốikillhoàn toànsaukết quả
                    if (killNumber == INITIAL_KILL_COUNT) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                            }
                        }, 100);
                    }
                }
            }, i * KILL_INTERVAL_MS);
        }
    }

    /**
 * Shizuku TaskService kết nốiquaygọi
 */
    private final ServiceConnection taskServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            taskService = ITaskService.Stub.asInterface(binder);

            // kết nối lạivụ（nếutồnở）
            if (handler != null) {
                handler.removeCallbacks(reconnectTaskServiceRunnable);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "⚠ TaskService disconnected - will attempt to reconnect");
            taskService = null;

            // khởi độngkết nối lạivụ
            scheduleReconnectTaskService();
        }
    };

    /**
 * TaskServicekết nối lạivụ
 */
    private final Runnable reconnectTaskServiceRunnable = new Runnable() {
        @Override
        public void run() {
            if (taskService == null) {
                bindTaskService();

                // nếukết nối lại thất bại, sau 1 giâythử lại lần nữa
                handler.postDelayed(this, 1000);
            } else {
            }
        }
    };

    /**
 * xếpTaskServicekết nối lại
 */
    private void scheduleReconnectTaskService() {
        if (handler != null) {
            handler.postDelayed(reconnectTaskServiceRunnable, 300);
        }
    };

    /**
 * bindShizuku TaskService
 */
    private void bindTaskService() {
        if (taskService != null) {
            return;
        }

        try {
            Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                    new ComponentName(getPackageName(), TaskService.class.getName()))
                    .daemon(false)
                    .processNameSuffix("task_service")
                    .debuggable(false)
                    .version(1);

            Shizuku.bindUserService(args, taskServiceConnection);
        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to bind TaskService", e);
        }
    }

    /**
 * unbind TaskService
 */
    private void unbindTaskService() {
        if (taskService != null) {
            try {
                Shizuku.unbindUserService(
                        new Shizuku.UserServiceArgs(
                                new ComponentName(getPackageName(), TaskService.class.getName()))
                                .daemon(false)
                                .processNameSuffix("task_service")
                                .debuggable(false)
                                .version(1),
                        taskServiceConnection,
                        true);
            } catch (Exception e) {
                Log.w(TAG, "Failed to unbind TaskService", e);
            }
            taskService = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.w(TAG, "═══════════════════════════════════════");
        Log.w(TAG, "⚠ Service onDestroy called");

        // ngaygỡ bỏthông báo foreground
        stopForeground(Service.STOP_FOREGROUND_REMOVE);

        // tronglýsởcóvụ
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        // V12.2: khôi phụcvàchủ độngđánh thứcLauncher
        if (taskService != null) {
            try {

                // 1. khôi phụcLauncher（unsuspend）
                taskService.enableSubScreenLauncher();

                // 2. ngắntrễ, đảm bảounsuspend
                Thread.sleep(300);

                // 3. chủ độngkhởi độngLauncherActivityđánh thứcnó

            } catch (Exception e) {
                Log.w(TAG, "Failed to restore launcher", e);
            }
        }

        // unbind TaskService
        unbindTaskService();

        // WakeLock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // hủy đăng kýcảm biến tiệm cận
        unregisterProximitySensor();

        instance = null;
        Log.w(TAG, "═══════════════════════════════════════");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // khôngbind
        return null;
    }

    /**
 * tạothông báođạo（Android 8.0+phảicần）
 */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notif_kernel_service),
                    NotificationManager.IMPORTANCE_LOW // thấpnặngcầntính, ítcan thiệp
            );
            channel.setDescription(getString(R.string.notif_channel_desc_subscreen));
            channel.setShowBadge(false); // không hiển thịđánh dấu
            channel.enableLights(false); // khônglấp lánhLED
            channel.enableVibration(false); // khôngrung

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);

        }
    }

    /**
 * lấy tên ứng dụng
 */
    private String getAppName(String packageName) {
        try {
            android.content.pm.PackageManager pm = getPackageManager();
            android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            CharSequence label = pm.getApplicationLabel(appInfo);
            if (label != null && label.length() > 0) {
                return label.toString();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get app name: " + e.getMessage());
        }
        return packageName; // thất bạithời giantrả vềtên package
    }

    /**
 * V2.4: tạothông dụngServicethông báo foreground（chonhiềucáiServicecùng dùng）
 */
    public static Notification createServiceNotification(Context context) {
        // tạothông báođạo
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_kernel_service),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(context.getString(R.string.notif_channel_desc_subscreen));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notif_kernel_service))
                .setContentText(context.getString(R.string.notif_mrss_running))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    /**
 * thông báo foreground
 */
    private Notification buildNotification() {
        // lấy tên ứng dụng
        String appName = getString(R.string.default_app_name);

        if (monitoredTaskInfo != null && monitoredTaskInfo.contains(":")) {
            String packageName = monitoredTaskInfo.split(":")[0];
            appName = getAppName(packageName);
        } else {
            Log.w(TAG, "⚠ Invalid monitored task info: " + monitoredTaskInfo);
        }

        // nhấpthông báochuyểnquaymàn hình chính
        Intent returnIntent = new Intent(this, RearScreenKeeperService.class);
        returnIntent.setAction("ACTION_RETURN_TO_MAIN");
        PendingIntent pendingIntent = PendingIntent.getService(
                this, 0, returnIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(appName + " " + getString(R.string.notif_running_on_rear))
                .setContentText(getString(R.string.notif_click_to_return, appName))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW) // thấpưutrước
                .setOngoing(true) // liên tụcthông báo, khôngcó thểtrượtxóa
                .setShowWhen(false) // không hiển thịthời gian
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    /**
 * kiểm traServicecóđang
 */
    public static boolean isRunning() {
        return instance != null;
    }

    /**
 * dừngService
 */
    public static void stop() {
        if (instance != null) {
            instance.stopSelf();
        }
    }

    // ========================================
    // cảm biến tiệm cậntươngkeyphương thức
    // ========================================

    /**
 * từSharedPreferencestảicông tắc cảm biếntrạng thái
 */
    private void loadProximitySensorSetting() {
        try {
            SharedPreferences prefs = getSharedPreferences("FlutterSharedPreferences", MODE_PRIVATE);
            proximitySensorEnabled = prefs.getBoolean("flutter.proximity_sensor_enabled", true);
            Log.d(TAG, "🔧 传感器开关状态已恢复: " + proximitySensorEnabled);
        } catch (Exception e) {
            Log.e(TAG, "✗ 加载传感器设置失败", e);
            proximitySensorEnabled = true; // mặc địnhbật
        }
    }

    /**
 * khởi tạocảm biến tiệm cận（màn hình saucảm biến tiệm cận）
 */
    private void initProximitySensor() {
        try {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            if (sensorManager != null) {
                // lấysởcócảm biến
                List<Sensor> allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

                // tramàn hình saucảm biến tiệm cận（têngói "Back" và "Proximity"）
                // ưutrướcchọn Wakeup phiên bản, nếukhôngcóthìchọn Non-wakeup phiên bản
                Sensor wakeupSensor = null;
                Sensor nonWakeupSensor = null;

                for (Sensor sensor : allSensors) {
                    String name = sensor.getName();
                    if (name.contains("Proximity") && name.contains("Back")) {
                        if (name.contains("Wakeup")) {
                            wakeupSensor = sensor;
                        } else {
                            nonWakeupSensor = sensor;
                        }
                    }
                }

                // ưutrướcsử dụng Wakeup phiên bản
                if (wakeupSensor != null) {
                    proximitySensor = wakeupSensor;
                } else if (nonWakeupSensor != null) {
                    proximitySensor = nonWakeupSensor;
                    Log.w(TAG, "→ Using NON-WAKEUP sensor (may not provide continuous data)");
                }

                // nếukhôngđếnmàn hình saucảm biến, quaylùiđếnmặc địnhcảm biến
                if (proximitySensor == null) {
                    Log.w(TAG, "⚠ Rear proximity sensor not found, using default");
                    proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                }

                if (proximitySensor != null) {
                    // V2.2: chỉ khicông tắc cảm biếnkhi bậtmớiđăng ký listener
                    if (proximitySensorEnabled) {
                        boolean registered = sensorManager.registerListener(
                                this,
                                proximitySensor,
                                SensorManager.SENSOR_DELAY_NORMAL);

                        if (registered) {
                            Log.d(TAG, "✅ 接近传感器已注册 (开关状态: " + proximitySensorEnabled + ")");
                        } else {
                            Log.w(TAG, "⚠ Failed to register proximity sensor");
                        }
                    } else {
                        Log.d(TAG, "⏸️ 接近传感器已禁用，跳过注册");
                    }
                } else {
                    Log.w(TAG, "⚠ No proximity sensor available");
                }
            } else {
                Log.w(TAG, "⚠ SensorManager not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "✗ Error initializing proximity sensor", e);
        }
    }

    /**
 * hủy đăng kýcảm biến tiệm cận
 */
    private void unregisterProximitySensor() {
        try {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        } catch (Exception e) {
            Log.e(TAG, "✗ Error unregistering proximity sensor", e);
        }
    }

    /**
 * cảm biếndữ liệuthay đổiquaygọi
 */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // V2.2: nếucảm biếnđãđóng, khôngxử lýsự kiện
        if (!proximitySensorEnabled) {
            return;
        }

        // kiểm tracólàtôicácmàn hình saucảm biến tiệm cận
        if (event.sensor == proximitySensor) {
            float distance = event.values[0];
            float maxRange = proximitySensor.getMaximumRange();

            // log - mỗi lầncảm biếnthay đổiđềughi

            // tất nhiêntiệm cận0（bị）thời giankích hoạt
            boolean isCovered = (distance < maxRange * 0.2f); // nhỏởtối ưulớn20%là

            long currentTime = System.currentTimeMillis();

            if (isCovered && !isProximityCovered) {
                // từchưađến
                isProximityCovered = true;
                lastProximityTime = currentTime;

                Log.w(TAG, "👋 PROXIMITY COVERED! Distance: " + distance + " cm");
                Log.w(TAG, "👋 Starting debounce timer (" + PROXIMITY_DEBOUNCE_MS + "ms)...");

                // chống rungđộng：trễkiểm tra
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isProximityCovered &&
                                (System.currentTimeMillis() - lastProximityTime >= PROXIMITY_DEBOUNCE_MS)) {
                            // chắc chắnnhậnsiêuquá trình500ms, kích hoạtquaymàn hình chính
                            Log.w(TAG, "👋 Debounce timer expired - triggering return to main display!");
                            handleProximityCovered();
                        } else {
                        }
                    }
                }, PROXIMITY_DEBOUNCE_MS);

            } else if (!isCovered && isProximityCovered) {
                // từđếnchưa
                isProximityCovered = false;
            } else if (isCovered && isProximityCovered) {
                // liên tụctrong
            } else {
                // liên tụcchưa
            }
        } else {
            // kháccảm biếndữ liệu, cũngghimộtdưới
        }
    }

    /**
 * cảm biếnthay đổiquaygọi（không cầnxử lý）
 */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // không cầnxử lý
    }

    /**
 * xử lýcảm biến tiệm cậnsự kiện - quaymàn hình chínhvàdừngService
 */
    private void handleProximityCovered() {
        Log.w(TAG, "═══════════════════════════════════════");
        Log.w(TAG, "🤚 PROXIMITY TRIGGER - Return to main display");
        Log.w(TAG, "═══════════════════════════════════════");

        try {
            if (taskService != null) {
                // lấycuối cùngchuyểnthông tin task
                String lastTask = SwitchToRearTileService.getLastMovedTask();

                if (lastTask != null && lastTask.contains(":")) {
                    String[] parts = lastTask.split(":");
                    String packageName = parts[0];
                    int taskId = Integer.parseInt(parts[1]);

                    // lấy tên ứng dụng
                    String appName = getAppName(packageName);

                    // quaymàn hình chính
                    boolean success = taskService.moveTaskToDisplay(taskId, 0);

                    if (success) {
                        // hiển thị Toast trễ
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            Toast.makeText(RearScreenKeeperService.this, appName + " " + getString(R.string.toast_return_to_main), Toast.LENGTH_SHORT).show();
                        }, 100);
                    } else {
                        Log.w(TAG, "⚠ Failed to return task (may already be on main display)");
                    }
                } else {
                    Log.w(TAG, "⚠ No active rear screen task found");
                }

                // trướcgỡ bỏthông báo foreground
                stopForeground(Service.STOP_FOREGROUND_REMOVE);

                // dừngService（sẽtự độngkhôi phụchệ thốngLauncher）
                stopSelf();

            } else {
                Log.w(TAG, "⚠ TaskService not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "✗ Error handling proximity event", e);
        }
    }
}
