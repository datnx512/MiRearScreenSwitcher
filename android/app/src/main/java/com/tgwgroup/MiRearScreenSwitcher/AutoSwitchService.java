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
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.HashSet;
import java.util.Set;

import rikka.shizuku.Shizuku;

/**
 * Monitors system events and auto-switches apps to rear screen based on rules.
 *
 * Rule types:
 *  - charging      : when the phone is plugged in, switch a specific app to the rear screen.
 *  - app_open      : when a specific app is launched, auto-switch it to the rear screen.
 *  - rear_screen_on: when the rear screen turns on, switch a specific app to the rear screen.
 *
 * Rules are stored in SharedPreferences ("auto_switch_rules") as:
 *  - rule_app_open        : Set<String> of package names that should be auto-switched on launch.
 *  - target_charging      : String package name to switch when charging.
 *  - target_rear_screen_on: String package name to switch when the rear screen turns on.
 */
public class AutoSwitchService extends Service {
    private static final String TAG = "AutoSwitchService";
    private static final String CHANNEL_ID = "auto_switch_service";
    private static final int NOTIFICATION_ID = 10002;

    private static final long CHECK_INTERVAL = 3000; // Check foreground app every 3s
    private static final String PREFS_NAME = "auto_switch_rules";

    private Handler handler;
    private ITaskService taskService;
    private String lastForegroundApp = null;

    // ---- Shizuku TaskService binding (mirrors ChargingService pattern) ----
    private final Shizuku.UserServiceArgs serviceArgs =
        new Shizuku.UserServiceArgs(
            new ComponentName("com.tgwgroup.MiRearScreenSwitcher", TaskService.class.getName()))
            .daemon(false)
            .processNameSuffix("auto_switch_task_service")
            .debuggable(false)
            .version(1);

    private final ServiceConnection taskServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "✓ TaskService connected");
            taskService = ITaskService.Stub.asInterface(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "✗ TaskService disconnected");
            taskService = null;
            scheduleReconnectTaskService();
        }
    };

    private final Shizuku.OnBinderReceivedListener binderReceivedListener = () -> {
        Log.d(TAG, "Shizuku binder received");
        bindTaskService();
    };

    private final Shizuku.OnBinderDeadListener binderDeadListener = () -> {
        Log.d(TAG, "Shizuku binder dead");
        taskService = null;
        new Handler(Looper.getMainLooper()).postDelayed(() -> bindTaskService(), 1000);
    };

    private final Runnable reconnectTaskServiceRunnable = new Runnable() {
        @Override
        public void run() {
            if (taskService == null) {
                bindTaskService();
                new Handler(Looper.getMainLooper()).postDelayed(this, 1000);
            }
        }
    };

    private void scheduleReconnectTaskService() {
        new Handler(Looper.getMainLooper()).postDelayed(reconnectTaskServiceRunnable, 200);
    }

    private void bindTaskService() {
        if (taskService != null) {
            return;
        }
        try {
            if (!Shizuku.pingBinder()) {
                Log.w(TAG, "Shizuku not available");
                return;
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No Shizuku permission");
                return;
            }
            Shizuku.bindUserService(serviceArgs, taskServiceConnection);
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind TaskService", e);
        }
    }

    // ---- Foreground-app polling ----
    private final Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            checkForegroundApp();
            handler.postDelayed(this, CHECK_INTERVAL);
        }
    };

    // ---- Event receiver ----
    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received: " + action);

            if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                handleChargingConnected();
            } else if ("miui.intent.action.SUB_SCREEN_ON".equals(action)) {
                handleRearScreenOn();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        handler = new Handler(Looper.getMainLooper());

        // Register Shizuku listeners
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);

        // Register for events
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction("miui.intent.action.SUB_SCREEN_ON");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(eventReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(eventReceiver, filter);
        }

        // Bind TaskService and start monitoring
        bindTaskService();
        handler.post(checkRunnable);

        Log.i(TAG, "AutoSwitchService started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Ensure TaskService is bound
        if (taskService == null) {
            bindTaskService();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(checkRunnable);
            handler.removeCallbacks(reconnectTaskServiceRunnable);
        }
        try {
            unregisterReceiver(eventReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregister receiver", e);
        }
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener);
            Shizuku.removeBinderDeadListener(binderDeadListener);
        } catch (Exception e) {
            Log.w(TAG, "Failed to remove Shizuku listeners", e);
        }
        Log.i(TAG, "AutoSwitchService stopped");
    }

    private void checkForegroundApp() {
        if (taskService == null) return;

        try {
            String result = taskService.getCurrentForegroundApp();
            if (result == null) return;

            String packageName = result.split(":")[0];

            // Check if this app should be auto-switched
            if (!packageName.equals(lastForegroundApp)) {
                lastForegroundApp = packageName;

                if (shouldAutoSwitch(packageName, "app_open")) {
                    Log.i(TAG, "Auto-switching " + packageName + " to rear screen");
                    Thread switchThread = new Thread(() -> {
                        try {
                            int taskId = Integer.parseInt(result.split(":")[1]);
                            taskService.moveTaskToDisplay(taskId, 1);
                        } catch (Exception e) {
                            Log.e(TAG, "Auto-switch failed", e);
                        }
                    });
                    switchThread.start();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Check foreground failed", e);
        }
    }

    private void handleChargingConnected() {
        String targetApp = getRuleTargetApp("charging");
        if (targetApp != null && !targetApp.isEmpty()) {
            Log.i(TAG, "Charging connected, switching " + targetApp + " to rear");
            // Wait a moment for app to be foreground, then switch
            handler.postDelayed(() -> {
                try {
                    if (taskService != null) {
                        String result = taskService.getCurrentForegroundApp();
                        if (result != null && result.startsWith(targetApp)) {
                            int taskId = Integer.parseInt(result.split(":")[1]);
                            taskService.moveTaskToDisplay(taskId, 1);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Charging auto-switch failed", e);
                }
            }, 1000);
        }
    }

    private void handleRearScreenOn() {
        String targetApp = getRuleTargetApp("rear_screen_on");
        if (targetApp != null && !targetApp.isEmpty()) {
            Log.i(TAG, "Rear screen on, switching " + targetApp);
            handler.postDelayed(() -> {
                try {
                    if (taskService != null) {
                        String result = taskService.getCurrentForegroundApp();
                        if (result != null) {
                            int taskId = Integer.parseInt(result.split(":")[1]);
                            taskService.moveTaskToDisplay(taskId, 1);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Rear screen auto-switch failed", e);
                }
            }, 500);
        }
    }

    private boolean shouldAutoSwitch(String packageName, String ruleType) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> apps = prefs.getStringSet("rule_" + ruleType, new HashSet<>());
        return apps.contains(packageName);
    }

    private String getRuleTargetApp(String ruleType) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString("target_" + ruleType, "");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Auto Switch Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Tu dong chuyen app sang man hinh sau");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MRSS Auto-Switch")
            .setContentText("Dang giam sat...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build();
    }
}
