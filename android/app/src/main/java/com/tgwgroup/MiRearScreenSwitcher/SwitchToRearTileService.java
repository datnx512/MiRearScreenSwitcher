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

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;
import rikka.shizuku.Shizuku;

/**
 * Quick Settings Tile - chuyểnmàn hình sau
 * sau khi nhấpsẽhiện tạiứng dụng foregroundchuyểnđếnmàn hình sau
 */
public class SwitchToRearTileService extends TileService {
    private static final String TAG = "SwitchToRearTile";

    // biến static：lưucuối cùngchuyển đến màn hình sauthông tin task（dùng chocảm biến tiệm cậnkhôi phục）
    private static String lastMovedTask = null; // thức: "packageName:taskId"

    private ITaskService taskService;
    private final Shizuku.UserServiceArgs serviceArgs = new Shizuku.UserServiceArgs(
            new ComponentName("com.tgwgroup.MiRearScreenSwitcher", TaskService.class.getName()))
            .daemon(false)
            .processNameSuffix("task_service")
            .debuggable(false)
            .version(1);

    private final ServiceConnection taskServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            taskService = ITaskService.Stub.asInterface(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            taskService = null;
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
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 1000);
            }
        }
    };

    /**
 * xếpTaskServicekết nối lại
 */
    private void scheduleReconnectTaskService() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(reconnectTaskServiceRunnable, 200);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();

        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setSubtitle(null);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                tile.setStateDescription("");
            }
            tile.updateTile();
        }

        bindTaskService();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        unbindTaskService();
    }

    /**
 * staticphương thức：khôi phụcchắc chắnvụđếnmàn hình sau
 * RearScreenBroadcastReceiver gọi
 */
    public static void restoreTaskToRearDisplay(int taskId) {
        // phương thức này để trống, logic khôi phục thực tế do broadcast receiver trực tiếp khởi độngActivitykích hoạt
        // Activity sẽ tự động ứng dụngFLAG_KEEP_SCREEN_ON
    }

    /**
 * lấycuối cùngchuyển đến màn hình sauthông tin task
 * 
 * @return thức: "packageName:taskId", nếukhôngcóthìtrả vềnull
 */
    public static String getLastMovedTask() {
        return lastMovedTask;
    }

    @Override
    public void onClick() {
        super.onClick();
        switchCurrentAppToRearDisplay();
    }

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

    private void unbindTaskService() {
        if (taskService != null) {
            try {
                Shizuku.unbindUserService(serviceArgs, taskServiceConnection, true);
            } catch (Exception e) {
                Log.e(TAG, "Error unbinding TaskService", e);
            }
            taskService = null;
        }
    }

    private void switchCurrentAppToRearDisplay() {
        if (taskService == null) {
            Log.w(TAG, "TaskService not available!");
            showTemporaryFeedback(getString(R.string.toast_service_not_ready));

            // thửlàm lạibind
            bindTaskService();

            // thử lại trễ
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (taskService != null) {
                    performSwitch();
                } else {
                    showTemporaryFeedback(getString(R.string.toast_open_app_auth));
                }
            }, 1000);
            return;
        }

        performSwitch();
    }

    private void performSwitch() {
        // hiển thịtrongtrạng thái - giữnútngoài, chỉsửatiêu đề phụ
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_INACTIVE); // giữtắttrạng thái
            tile.setSubtitle(getString(R.string.tile_switching));
            // không hiển thị"đãbật"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                tile.setStateDescription("");
            }
            tile.updateTile();
        }

        try {
            // bước0: kiểm tra màn hình sau cóđãcóứng dụngở
            if (lastMovedTask != null && lastMovedTask.contains(":")) {
                try {
                    String[] oldParts = lastMovedTask.split(":");
                    String oldPackageName = oldParts[0];
                    int oldTaskId = Integer.parseInt(oldParts[1]);

                    // kiểm tracũứng dụngcóvẫnởmàn hình sau
                    String rearForegroundApp = taskService.getForegroundAppOnDisplay(1);
                    if (rearForegroundApp != null && rearForegroundApp.equals(lastMovedTask)) {
                        // màn hình sauđãcóứng dụngở, dừngthao tác
                        String oldAppName = getAppName(oldPackageName);

                        // thu gọn Control Center trước, Toast mới hiển thị
                        try {
                            taskService.collapseStatusBar();
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to collapse for toast: " + e.getMessage());
                        }

                        // hiển thị Toast trễ, đảm bảo Control Center đã thu gọn
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            Toast.makeText(this, getString(R.string.toast_please_switch_back, oldAppName),
                                    Toast.LENGTH_LONG).show();
                        }, 300);

                        showTemporaryFeedback(getString(R.string.toast_rear_occupied));
                        return;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to check previous app: " + e.getMessage());
                }
            }

            // bước1: tắt Launcher màn hình sau hệ thống（keyphím！ngăn chiếm chỗ）
            try {
                taskService.disableSubScreenLauncher();
            } catch (Exception e) {
                Log.w(TAG, "Failed to disable SubScreenLauncher", e);
            }

            // bước2: lấy hiện tạiứng dụng foreground
            String currentApp = taskService.getCurrentForegroundApp();

            // bước3: ngaykhởi động foregroundService（không trễ, cho thông báo xuất hiện nhanh）
            Intent serviceIntent = new Intent(this, RearScreenKeeperService.class);
            serviceIntent.putExtra("lastMovedTask", currentApp);

            // V2.5: truyền trạng thái công tắc màn hình sau giữ sáng
            try {
                android.content.SharedPreferences prefs = getSharedPreferences("FlutterSharedPreferences",
                        MODE_PRIVATE);
                boolean keepScreenOnEnabled = prefs.getBoolean("flutter.keep_screen_on_enabled", true);
                serviceIntent.putExtra("keepScreenOnEnabled", keepScreenOnEnabled);
            } catch (Exception e) {
                // mặc định là bật
                serviceIntent.putExtra("keepScreenOnEnabled", true);
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            if (currentApp != null && currentApp.contains(":")) {
                String[] parts = currentApp.split(":");
                String packageName = parts[0];
                int taskId = Integer.parseInt(parts[1]);

                // lấy tên ứng dụng
                String appName = getAppName(packageName);

                // bước4: chuyểnđếndisplay 1 (màn hình sau)
                boolean success = taskService.moveTaskToDisplay(taskId, 1);

                if (success) {
                    // lưucuối cùngchuyểnthông tin task（dùng chocảm biến tiệm cậnkhôi phục）
                    lastMovedTask = currentApp;

                    // tự động thu gọn Control Center（nâng trải nghiệm người dùng）
                    try {
                        new Thread(() -> {
                            try {
                                if (taskService != null) {
                                    taskService.collapseStatusBar();
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Failed to collapse: " + e.getMessage());
                            }
                        }).start();
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to start collapse thread: " + e.getMessage());
                    }

                    // hiển thị Toast trễ, đảm bảo Control Center đã thu gọn
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        Toast.makeText(this, appName + " " + getString(R.string.toast_cast_to_rear), Toast.LENGTH_SHORT)
                                .show();
                    }, 300);

                    // bước5: chủ động bật sáng màn hình sau (qua TaskService khởi động Activity, vượt giới hạn BAL)
                    try {
                        if (taskService != null) {
                            try {
                                boolean launchResult = taskService.launchWakeActivity(1);
                                if (!launchResult) {
                                    Log.w(TAG, "TaskService launch failed");
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "launchWakeActivity exception: " + e.getMessage());
                            }
                        } else {
                            Log.w(TAG, "TaskService not available");
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to launch wakeup activity", e);
                    }

                    showTemporaryFeedback(getString(R.string.toast_switched));
                } else {
                    // thu gọn Control Center trước
                    try {
                        taskService.collapseStatusBar();
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to collapse: " + e.getMessage());
                    }

                    // hiển thị Toast trễ
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        Toast.makeText(this, getString(R.string.toast_switch_failed), Toast.LENGTH_SHORT).show();
                    }, 300);

                    showTemporaryFeedback(getString(R.string.toast_failed));
                }
            } else {
                Log.w(TAG, "No foreground app found");
                showTemporaryFeedback(getString(R.string.toast_app_not_found_tile));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error switching app", e);
            showTemporaryFeedback(getString(R.string.toast_operation_failed));
        }
    }

    private void showTemporaryFeedback(String message) {
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setSubtitle(message);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                tile.setStateDescription("");
            }
            tile.updateTile();
        }

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Tile resetTile = getQsTile();
            if (resetTile != null) {
                resetTile.setState(Tile.STATE_INACTIVE);
                resetTile.setSubtitle(null);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    resetTile.setStateDescription("");
                }
                resetTile.updateTile();
            }
        }, 1500);
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
}
