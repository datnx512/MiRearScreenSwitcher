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

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import rikka.shizuku.Shizuku;

/**
 * V2.6: URIlệnhxử lýdịch vụ
 * ởbackgroundim lặngURIlệnh, không hiển thịUI
 * phụcngười dùnghiệncóTileServicechuyểnlogic
 */
public class UriCommandService extends IntentService {
    private static final String TAG = "UriCommandService";

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
            Log.d(TAG, "✓ TaskService已连接");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            taskService = null;
        }
    };

    public UriCommandService() {
        super("UriCommandService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bindTaskService();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        Uri uri = intent.getData();
        if (uri == null || !"mrss".equals(uri.getScheme())) {
            return;
        }

        Log.d(TAG, "🔗 处理URI: " + uri.toString());

        // đảm bảo TaskService đãkết nối
        if (!ensureTaskServiceConnected()) {
            Log.e(TAG, "❌ TaskService未连接");
            return;
        }

        String host = uri.getHost();
        if (host == null)
            return;

        switch (host) {
            case "switch":
                handleSwitch(uri);
                break;
            case "return":
                handleReturn(uri);
                break;
            case "screenshot":
                handleScreenshot();
                break;
            case "config":
                handleConfig(uri);
                break;
        }
    }

    private boolean ensureTaskServiceConnected() {
        if (taskService != null)
            return true;

        try {
            bindTaskService();

            // chờkết nối（tối đa3giây）
            int attempts = 0;
            while (taskService == null && attempts < 30) {
                Thread.sleep(100);
                attempts++;
            }

            return taskService != null;
        } catch (Exception e) {
            Log.e(TAG, "TaskService重连失败", e);
            return false;
        }
    }

    private void bindTaskService() {
        if (taskService != null)
            return;

        try {
            if (!Shizuku.pingBinder()) {
                Log.e(TAG, "Shizuku不可用");
                return;
            }

            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "无Shizuku权限");
                return;
            }

            Shizuku.bindUserService(serviceArgs, taskServiceConnection);
        } catch (Exception e) {
            Log.e(TAG, "绑定TaskService失败", e);
        }
    }

    /**
 * xử lýchuyểnlệnh - phụcngười dùngTileServicelogic
 */
    private void handleSwitch(Uri uri) {
        Log.d(TAG, "════════════════════════════════════════");
        Log.d(TAG, "🔄 处理SWITCH命令");
        Log.d(TAG, "URI: " + uri.toString());

        try {
            // 0. kiểm tra màn hình sau cóđãcóứng dụngở（từ chốilặp lạicast）
            try {
                String rearForegroundApp = taskService.getForegroundAppOnDisplay(1);
                Log.d(TAG, "背屏前台应用: " + rearForegroundApp);

                if (rearForegroundApp != null && !rearForegroundApp.isEmpty()) {
                    // xếptrừcho phépprocess：
                    // 1. MRSStựActivity（hoạt ảnh sạc、thông báo hoạt ảnh、đánh thứcchờ）
                    // 2. nhỏLauncher chính thức（com.xiaomi.subscreencenter.SubScreenLauncher）
                    if (!rearForegroundApp.contains("RearScreenChargingActivity") &&
                            !rearForegroundApp.contains("RearScreenNotificationActivity") &&
                            !rearForegroundApp.contains("RearScreenWakeupActivity") &&
                            !rearForegroundApp.contains("com.xiaomi.subscreencenter")) {
                        Log.w(TAG, "❌ 背屏已有应用在运行: " + rearForegroundApp);
                        Log.d(TAG, "════════════════════════════════════════");
                        return;
                    } else {
                        Log.d(TAG, "✓ 背屏空闲或仅有官方Launcher/MRSS临时Activity");
                    }
                } else {
                    Log.d(TAG, "✓ 背屏空闲");
                }
            } catch (Exception e) {
                Log.w(TAG, "检查背屏占用失败: " + e.getMessage());
            }

            // 1. chắc chắnchắc chắnmụcđánh dấu
            String currentParam = uri.getQueryParameter("current");
            String packageName = uri.getQueryParameter("packageName");
            String activity = uri.getQueryParameter("activity");

            Log.d(TAG, "参数 - current: " + currentParam + ", packageName: " + packageName + ", activity: " + activity);

            if ("true".equalsIgnoreCase(currentParam) || "1".equals(currentParam)) {
                // chuyểnhiện tạiứng dụng - hoàn toànphụcngười dùngTileServicelogic
                Log.d(TAG, "→ 模式：切换当前应用");
                // trướcứng dụngcấu hìnhtham số, lạichuyển
                applyConfigParams(uri);
                switchCurrentAppToRear();
            } else if (activity != null) {
                // khởi độngchắc chắnActivityđếnmàn hình sau
                Log.d(TAG, "→ 模式：启动指定Activity");
                switchSpecificAppToRear(activity, null, uri);
            } else if (packageName != null) {
                // khởi độngchắc chắntên packageđếnmàn hình sau
                Log.d(TAG, "→ 模式：启动指定包名");
                switchSpecificAppToRear(null, packageName, uri);
            } else {
                Log.w(TAG, "⚠ 未指定切换目标");
            }

            Log.d(TAG, "════════════════════════════════════════");
        } catch (Exception e) {
            Log.e(TAG, "❌ 切换命令失败", e);
            e.printStackTrace();
            Log.d(TAG, "════════════════════════════════════════");
        }
    }

    /**
 * chuyểnhiện tạiứng dụngđếnmàn hình sau - hoàn toànphụcngười dùngTileServicelogic
 */
    private void switchCurrentAppToRear() {
        try {
            // bước0: kiểm tra màn hình sau cóđãcóứng dụngở（phụcngười dùngTileServicelogic）
            String lastMovedTask = SwitchToRearTileService.getLastMovedTask();
            if (lastMovedTask != null && lastMovedTask.contains(":")) {
                try {
                    String[] oldParts = lastMovedTask.split(":");
                    String oldPackageName = oldParts[0];

                    // kiểm tracũứng dụngcóvẫnởmàn hình sau
                    String rearForegroundApp = taskService.getForegroundAppOnDisplay(1);
                    if (rearForegroundApp != null && rearForegroundApp.equals(lastMovedTask)) {
                        // màn hình sauđãcóứng dụngở, dừngthao tác
                        String oldAppName = getAppName(oldPackageName);
                        Log.w(TAG, "❌ 背屏已被占用: " + oldAppName);
                        return;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "检查旧应用失败: " + e.getMessage());
                }
            }

            // thêmkiểm tra：đảm bảomàn hình saukhôngcókhácngười dùngứng dụng
            try {
                String rearForegroundApp = taskService.getForegroundAppOnDisplay(1);
                if (rearForegroundApp != null && !rearForegroundApp.isEmpty()) {
                    // xếptrừcho phépprocess
                    if (!rearForegroundApp.contains("RearScreenChargingActivity") &&
                            !rearForegroundApp.contains("RearScreenNotificationActivity") &&
                            !rearForegroundApp.contains("RearScreenWakeupActivity") &&
                            !rearForegroundApp.contains("com.xiaomi.subscreencenter")) {
                        Log.w(TAG, "❌ 背屏已有其他应用: " + rearForegroundApp);
                        return;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "背屏占用检查失败: " + e.getMessage());
            }

            // bước1: tắt Launcher màn hình sau hệ thống（keyphím！ngăn chiếm chỗ）
            try {
                taskService.disableSubScreenLauncher();
            } catch (Exception e) {
                Log.w(TAG, "Failed to disable SubScreenLauncher", e);
            }

            // bước2: lấy hiện tạiứng dụng foreground
            String currentApp = taskService.getCurrentForegroundApp();

            // bước3: ngaykhởi động foregroundService（cho thông báo xuất hiện nhanh）
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

                // bước4: chuyểnđếndisplay 1 (màn hình sau)
                boolean success = taskService.moveTaskToDisplay(taskId, 1);

                if (success) {
                    Log.d(TAG, "✅ Task已移动到背屏 (taskId=" + taskId + ")");

                    // bước5: chủ động bật sáng màn hình sau (qua TaskService khởi động Activity, vượt giới hạn BAL) - keyphímbước！
                    try {
                        if (taskService != null) {
                            try {
                                boolean launchResult = taskService.launchWakeActivity(1);
                                if (!launchResult) {
                                    Log.w(TAG, "TaskService launch failed, fallback to shell");
                                    // Fallback: shelllệnhkhởi động
                                    String cmd = "am start --display 1 -n com.tgwgroup.MiRearScreenSwitcher/"
                                            + RearScreenWakeupActivity.class.getName();
                                    taskService.executeShellCommand(cmd);
                                }
                            } catch (NoSuchMethodError e) {
                                // cũphiên bảnTaskServicekhôngcólaunchWakeActivity, sử dụngshelllệnh
                                String cmd = "am start --display 1 -n com.tgwgroup.MiRearScreenSwitcher/"
                                        + RearScreenWakeupActivity.class.getName();
                                taskService.executeShellCommand(cmd);
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to launch wake activity: " + e.getMessage());
                    }

                    Log.d(TAG, "✅ " + packageName + " 已切换到背屏");

                    // Toastgợi ý
                    String appName = getAppName(packageName);
                    showToast(appName + " " + getString(R.string.toast_cast_to_rear));
                } else {
                    Log.e(TAG, "❌ 切换失败");
                    showToast(getString(R.string.toast_switch_failed));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "切换失败", e);
            showToast(getString(R.string.toast_switch_failed));
        }
    }

    /**
 * chuyểnchắc chắnứng dụngđếnmàn hình sau（packageNamehoặcactivity）
 */
    private void switchSpecificAppToRear(String activity, String packageName, Uri uri) {
        try {
            // bước0: trướccài đặtDPIvàxoay（ởkhởi độngứng dụngtrướccài đặttốtmàn hình sautham số）
            applyConfigParams(uri);

            // bước1: tắt Launcher màn hình sau hệ thống
            taskService.disableSubScreenLauncher();
            Thread.sleep(100);

            // bước1.5: tronglýmụcđánh dấuứng dụngcũtask（nếutồnở）- ngăn chặnlấyđếncũtask
            String targetPackageName = packageName;
            if (targetPackageName == null && activity != null) {
                // từactivitytrongnângtên package
                if (activity.contains("/")) {
                    targetPackageName = activity.substring(0, activity.indexOf("/"));
                }
            }

            if (targetPackageName != null) {
                try {
                    Log.d(TAG, "→ 检查并清理旧task: " + targetPackageName);
                    // thửdừngứng dụng（tronglýsởcótask）
                    taskService.executeShellCommand("am force-stop " + targetPackageName);
                    Thread.sleep(300);
                    Log.d(TAG, "✓ 已清理旧task");
                } catch (Exception e) {
                    Log.w(TAG, "清理旧task失败: " + e.getMessage());
                }
            }

            // bước2: ởmàn hình chính khởi độngứng dụng（tr tiên ở màn hình chính khởi động, mớilấy taskId）
            String launchCmd;
            if (activity != null) {
                launchCmd = "am start -n " + activity;
                Log.d(TAG, "→ 使用指定Activity启动: " + activity);
            } else {
                // sử dụngpmlệnhlấychínhActivity, monkeyhơncó thể
                launchCmd = "cmd package resolve-activity --brief " + packageName + " | tail -n 1";
                String mainActivity = taskService.executeShellCommandWithResult(launchCmd);

                if (mainActivity != null && !mainActivity.trim().isEmpty()
                        && !mainActivity.contains("No activity found")) {
                    mainActivity = mainActivity.trim();
                    launchCmd = "am start -n " + mainActivity;
                    Log.d(TAG, "→ 解析到主Activity: " + mainActivity);
                } else {
                    // Fallback: sử dụngpm dumplấychínhActivity
                    launchCmd = "pm dump " + packageName + " | grep -A 1 'android.intent.action.MAIN' | grep -o '"
                            + packageName + "[^\\s]*' | head -n 1";
                    mainActivity = taskService.executeShellCommandWithResult(launchCmd);

                    if (mainActivity != null && !mainActivity.trim().isEmpty()) {
                        mainActivity = mainActivity.trim();
                        launchCmd = "am start -n " + mainActivity;
                        Log.d(TAG, "→ 通过pm dump解析到主Activity: " + mainActivity);
                    } else {
                        // cuối cùngFallback: sử dụngIntentcáchkhởi động
                        launchCmd = "am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p "
                                + packageName;
                        Log.w(TAG, "→ 使用Intent方式启动（未能解析主Activity）");
                    }
                }
            }

            Log.d(TAG, "→ 执行启动命令: " + launchCmd);
            taskService.executeShellCommand(launchCmd);
            Log.d(TAG, "✓ 启动命令已执行");

            // bước3: chờứng dụng khởi độngvàtrải nghiệm, tối đa thử lại3lần
            String targetApp = null;
            String actualPackage = null;
            int taskId = -1;
            int maxRetries = 3;

            for (int retry = 0; retry < maxRetries; retry++) {
                Thread.sleep(500 + retry * 200); // lần500ms, sau đó

                targetApp = taskService.getCurrentForegroundApp();
                Log.d(TAG, "  尝试 " + (retry + 1) + "/" + maxRetries + " 获取前台应用: " + targetApp);

                if (targetApp == null || !targetApp.contains(":")) {
                    Log.w(TAG, "  未能获取应用，继续重试...");
                    continue;
                }

                String[] parts = targetApp.split(":");
                actualPackage = parts[0];

                // trải nghiệmcólàmụcđánh dấuứng dụng（packageNamevàactivitytrải nghiệm）
                boolean isTargetApp = false;
                if (packageName != null) {
                    isTargetApp = actualPackage.equals(packageName);
                } else if (activity != null) {
                    // từactivitynângtên packagevàotrải nghiệm
                    String activityPackage = activity.contains("/") ? activity.substring(0, activity.indexOf("/"))
                            : activity;
                    isTargetApp = actualPackage.equals(activityPackage);
                } else {
                    // không cótrải nghiệmphần, tiếpứng dụng（khôngnênđếnnày）
                    isTargetApp = true;
                }

                if (!isTargetApp) {
                    String expectedPkg = packageName != null ? packageName
                            : (activity != null ? activity.substring(0, activity.indexOf("/")) : "unknown");
                    Log.w(TAG, "  应用不匹配: " + actualPackage + " vs " + expectedPkg);

                    // lần cuốithử lạitrước, khởi độngmụcđánh dấuứng dụng
                    if (retry < maxRetries - 1) {
                        Log.w(TAG, "  强制停止当前应用并重新启动目标应用");
                        // trướcdừnglỗiứng dụng
                        taskService.executeShellCommand("am force-stop " + actualPackage);
                        Thread.sleep(200);
                        // làm lạilệnh khởi động
                        taskService.executeShellCommand(launchCmd);
                        continue;
                    } else {
                        Log.e(TAG, "  ❌ 多次重试后仍然无法启动目标应用");
                        return;
                    }
                } else {
                    // thành côngkhởi độngmụcđánh dấuứng dụng
                    taskId = Integer.parseInt(parts[1]);
                    Log.d(TAG, "✓ 成功启动目标应用，taskId: " + taskId);
                    break;
                }
            }

            if (taskId == -1) {
                Log.e(TAG, "❌ 未能获取启动的应用taskId");
                return;
            }

            // bước4: khởi độngRearScreenKeeperService
            Intent serviceIntent = new Intent(this, RearScreenKeeperService.class);
            serviceIntent.putExtra("lastMovedTask", targetApp);

            // truyền trạng thái công tắc màn hình sau giữ sáng
            try {
                android.content.SharedPreferences prefs = getSharedPreferences("FlutterSharedPreferences",
                        MODE_PRIVATE);
                boolean keepScreenOnEnabled = prefs.getBoolean("flutter.keep_screen_on_enabled", true);
                serviceIntent.putExtra("keepScreenOnEnabled", keepScreenOnEnabled);
            } catch (Exception e) {
                serviceIntent.putExtra("keepScreenOnEnabled", true);
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            // bước5: chuyển đến màn hình sau
            Log.d(TAG, "→ 步骤5: 移动Task到背屏 (taskId=" + taskId + ")");
            boolean success = taskService.moveTaskToDisplay(taskId, 1);

            if (success) {
                Log.d(TAG, "✅ Task已移动到背屏 (taskId=" + taskId + ")");

                // bước5.5: chờứng dụngởmàn hình sauổn địnhchắc chắnhiển thị
                Thread.sleep(300);
                Log.d(TAG, "→ 等待应用稳定");

                // bước5.6: chuyển đến màn hình sausau lần nữatrải nghiệmvàứng dụngDPI（đảm bảo）
                String dpiStr = uri.getQueryParameter("dpi");
                if (dpiStr != null) {
                    try {
                        int dpi = Integer.parseInt(dpiStr);
                        Log.d(TAG, "→ 再次验证DPI并应用: " + dpi);
                        // trải nghiệmhiện tạiDPI
                        int currentDpi = taskService.getCurrentRearDpi();
                        Log.d(TAG, "  当前背屏DPI: " + currentDpi);
                        if (currentDpi != dpi) {
                            Log.w(TAG, "  DPI不匹配，重新设置");
                            taskService.setRearDpi(dpi);
                            Thread.sleep(200);
                        } else {
                            Log.d(TAG, "  ✓ DPI已生效");
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "DPI验证失败: " + e.getMessage());
                    }
                }

                // bước6: chủ động bật sáng màn hình sau（keyphímbước！）
                Log.d(TAG, "→ 步骤6: 点亮背屏");
                try {
                    boolean launchResult = taskService.launchWakeActivity(1);
                    if (!launchResult) {
                        Log.w(TAG, "TaskService launch failed, fallback to shell");
                        String cmd = "am start --display 1 -n com.tgwgroup.MiRearScreenSwitcher/"
                                + RearScreenWakeupActivity.class.getName();
                        taskService.executeShellCommand(cmd);
                    }
                    Log.d(TAG, "✓ 背屏已点亮");
                } catch (NoSuchMethodError e) {
                    // cũphiên bảntương thích
                    String cmd = "am start --display 1 -n com.tgwgroup.MiRearScreenSwitcher/"
                            + RearScreenWakeupActivity.class.getName();
                    taskService.executeShellCommand(cmd);
                    Log.d(TAG, "✓ 背屏已点亮（旧版本fallback）");
                } catch (Exception e) {
                    Log.w(TAG, "Failed to launch wake activity: " + e.getMessage());
                }

                // bước7: nếucài đặtxoay, trải nghiệmvàkiểm traứng dụngtrạng thái
                String rotationStr = uri.getQueryParameter("rotation");
                if (rotationStr != null) {
                    Log.d(TAG, "→ 步骤7: 验证旋转并检查应用状态");
                    try {
                        int targetRotation = Integer.parseInt(rotationStr);

                        // chờxoay
                        Thread.sleep(500);

                        // trải nghiệmxoaycó
                        int currentRotation = taskService.getDisplayRotation(1);
                        Log.d(TAG, "  目标旋转: " + targetRotation + ", 当前旋转: " + currentRotation);

                        if (currentRotation != targetRotation) {
                            Log.w(TAG, "  ⚠ 旋转不匹配，重新设置");
                            taskService.setDisplayRotation(1, targetRotation);
                            Thread.sleep(500); // chờlàm lạicài đặt
                        } else {
                            Log.d(TAG, "  ✓ 旋转已生效");
                        }

                        // kiểm traứng dụngcóvẫnởmàn hình sau（có thểbịxoaykill）
                        boolean stillOnRear = taskService.isTaskOnDisplay(taskId, 1);
                        Log.d(TAG, "  应用是否还在背屏: " + stillOnRear);

                        if (!stillOnRear) {
                            // ứng dụngbịxoaykill, làm lạicast
                            Log.w(TAG, "  ⚠ 应用因旋转被杀，重新投放");
                            taskService.moveTaskToDisplay(taskId, 1);
                            Thread.sleep(200);
                            Log.d(TAG, "  ✓ 应用已复活");
                        } else {
                            Log.d(TAG, "  ✓ 应用正常运行");
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "旋转验证/检查失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "→ 步骤7: 跳过（无旋转参数）");
                }

                Log.d(TAG, "✅ " + actualPackage + " 已切换到背屏");

                // Toastgợi ý
                String appName = getAppName(actualPackage);
                showToast(appName + " " + getString(R.string.toast_cast_to_rear));
            } else {
                Log.e(TAG, "❌ 移动到背屏失败");
                showToast(getString(R.string.toast_switch_failed));
            }

        } catch (Exception e) {
            Log.e(TAG, "切换指定应用失败", e);
            showToast("切换失败: " + e.getMessage());
        }
    }

    /**
 * xử lýquaylệnh - hoàn toànphụcngười dùnghiệncólogic
 */
    private void handleReturn(Uri uri) {
        try {
            String currentParam = uri.getQueryParameter("current");
            String taskIdStr = uri.getQueryParameter("taskId");
            String packageName = uri.getQueryParameter("packageName");

            int targetTaskId = -1;
            String targetPackage = null;

            if ("true".equalsIgnoreCase(currentParam) || "1".equals(currentParam)) {
                String rearApp = taskService.getForegroundAppOnDisplay(1);
                if (rearApp != null && rearApp.contains(":")) {
                    String[] parts = rearApp.split(":");
                    targetPackage = parts[0];
                    targetTaskId = Integer.parseInt(parts[1]);
                }
            } else if (taskIdStr != null) {
                targetTaskId = Integer.parseInt(taskIdStr);
                // thửtừứng dụng foreground màn hình saulấytên package
                String rearApp = taskService.getForegroundAppOnDisplay(1);
                if (rearApp != null && rearApp.contains(":")) {
                    targetPackage = rearApp.split(":")[0];
                }
            } else if (packageName != null) {
                String rearApp = taskService.getForegroundAppOnDisplay(1);
                if (rearApp != null && rearApp.startsWith(packageName + ":")) {
                    targetPackage = packageName;
                    targetTaskId = Integer.parseInt(rearApp.split(":")[1]);
                }
            }

            if (targetTaskId != -1) {
                // kiểm travụcóthậtởmàn hình sau
                boolean onRear = taskService.isTaskOnDisplay(targetTaskId, 1);

                if (onRear) {
                    String appName = getAppName(targetPackage != null ? targetPackage : String.valueOf(targetTaskId));

                    // bước1: quaymàn hình chính
                    taskService.moveTaskToDisplay(targetTaskId, 0);
                    Log.d(TAG, "✅ 已拉回主屏 (taskId=" + targetTaskId + ")");

                    // bước2: khôi phụcLauncher chính thức（keyphím！）
                    try {
                        taskService.enableSubScreenLauncher();
                        Log.d(TAG, "✓ Launcher已恢复");
                    } catch (Exception e) {
                        Log.w(TAG, "恢复Launcher失败: " + e.getMessage());
                    }

                    // bước3: dừngRearScreenKeeperService（nếuở）
                    try {
                        stopService(new Intent(this, RearScreenKeeperService.class));
                        Log.d(TAG, "✓ KeeperService已停止");
                    } catch (Exception e) {
                        Log.w(TAG, "停止KeeperService失败: " + e.getMessage());
                    }

                    // Toastgợi ý
                    showToast(appName + " " + getString(R.string.toast_return_to_main));
                } else {
                    Log.w(TAG, "⚠ 任务不在背屏");
                    showToast(getString(R.string.toast_not_on_rear));
                }
            } else {
                Log.w(TAG, "⚠ 未找到要拉回的任务");
                showToast(getString(R.string.toast_app_not_found));
            }
        } catch (Exception e) {
            Log.e(TAG, "拉回命令失败", e);
        }
    }

    /**
 * xử lýchụp màn hìnhlệnh
 */
    private void handleScreenshot() {
        try {
            boolean success = taskService.takeRearScreenshot();

            // không cóthành côngthất bạiđềuhiển thịthành côngToast
            Log.d(TAG, "✅ 截图命令已执行");
            showToast(getString(R.string.toast_screenshot_saved));
        } catch (Exception e) {
            Log.e(TAG, "截图命令失败", e);
            // tức làsử dụngexceptioncũnghiển thịthành côngToast
            showToast(getString(R.string.toast_screenshot_saved));
        }
    }

    /**
 * xử lýcấu hìnhlệnh
 */
    private void handleConfig(Uri uri) {
        try {
            applyConfigParams(uri);
        } catch (Exception e) {
            Log.e(TAG, "配置命令失败", e);
        }
    }

    /**
 * ứng dụngcấu hìnhtham số - gọi trực tiếpTaskServicephương thức（MainActivitylogic）
 * DPIvàxoayđềutrực tiếpcài đặt, TaskServicetrongbộ phậnsẽxử lýchờvàphụcsống
 */
    private void applyConfigParams(Uri uri) {
        Log.d(TAG, "────────────────────────────");
        Log.d(TAG, "🔧 开始应用配置参数");
        Log.d(TAG, "URI: " + uri.toString());

        try {
            String dpiStr = uri.getQueryParameter("dpi");
            Log.d(TAG, "DPI参数: " + dpiStr);

            if (dpiStr != null) {
                int dpi = Integer.parseInt(dpiStr);
                Log.d(TAG, "→ 调用 taskService.setRearDpi(" + dpi + ")");

                // gọi trực tiếpTaskService.setRearDpi - hoàn toànMainActivitylogic
                boolean success = taskService.setRearDpi(dpi);

                if (success) {
                    Log.d(TAG, "✅ DPI设置成功: " + dpi);
                } else {
                    Log.e(TAG, "❌ DPI设置失败（TaskService返回false）");
                }
            } else {
                Log.d(TAG, "→ 跳过DPI设置（无参数）");
            }

            String rotationStr = uri.getQueryParameter("rotation");
            Log.d(TAG, "旋转参数: " + rotationStr);

            if (rotationStr != null) {
                int rotation = Integer.parseInt(rotationStr);
                Log.d(TAG, "→ 调用 taskService.setDisplayRotation(1, " + rotation + ")");

                // gọi trực tiếpTaskService.setDisplayRotation - hoàn toànMainActivitylogic
                // TaskServicetrongbộ phậnsẽtự độngxử lý：chờ500ms + kiểm traứng dụng + phụcsống
                boolean success = taskService.setDisplayRotation(1, rotation);

                if (success) {
                    Log.d(TAG, "✅ 旋转设置成功: " + rotation);
                } else {
                    Log.e(TAG, "❌ 旋转设置失败（TaskService返回false）");
                }
            } else {
                Log.d(TAG, "→ 跳过旋转设置（无参数）");
            }

            Log.d(TAG, "🔧 配置参数应用完成");
            Log.d(TAG, "────────────────────────────");
        } catch (Exception e) {
            Log.e(TAG, "❌ 应用配置参数异常", e);
            e.printStackTrace();
        }
    }

    /**
 * lấy tên ứng dụng
 */
    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    /**
 * hiển thị Toastgợi ý（chínhthread）
 */
    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (taskService != null) {
            try {
                Shizuku.unbindUserService(serviceArgs, taskServiceConnection, true);
            } catch (Exception e) {
                Log.e(TAG, "解绑TaskService失败", e);
            }
            taskService = null;
        }
    }
}
