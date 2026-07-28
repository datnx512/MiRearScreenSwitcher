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
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import rikka.shizuku.Shizuku;

/**
 * màn hình sauquay màn hìnhdịch vụ
 * chức năng：
 * 1. hiển thịoverlay window（quay/dừngnút+nút đóng）
 * 2. quaymàn hình saumàn hình（screenrecord --display-id 1）
 * 3. foregroundServicegiữ sống
 */
public class ScreenRecordService extends Service {
    private static final String TAG = "ScreenRecordService";
    private static final String CHANNEL_ID = "rear_screen_keeper"; // sử dụngMRSSkernel servicethông quađạo
    private static final int NOTIFICATION_ID = 10004; // tránhvớiKeeperService
    
    private static ScreenRecordService instance = null;
    private WindowManager windowManager;
    private View floatingView;
    private boolean isRecording = false;
    private String currentVideoPath;
    private int recordPid = -1; // quay màn hìnhprocessID
    private Handler wakeupHandler = new Handler(android.os.Looper.getMainLooper());
    private static final long WAKEUP_INTERVAL_MS = 100; // 100msđánh thứcmột lầnmàn hình sau
    
    // TaskService
    private ITaskService taskService;
    private final Shizuku.UserServiceArgs serviceArgs = 
        new Shizuku.UserServiceArgs(new ComponentName("com.tgwgroup.MiRearScreenSwitcher", TaskService.class.getName()))
            .daemon(false)
            .processNameSuffix("task_service")
            .debuggable(false)
            .version(1);
    
    private final ServiceConnection taskServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            taskService = ITaskService.Stub.asInterface(binder);
            Log.d(TAG, "✓ TaskService connected");
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            taskService = null;
        }
    };
    
    public static boolean isRunning() {
        return instance != null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "📹 ScreenRecordService onCreate");
        
        // tạothông báođạo
        createNotificationChannel();
        
        // bind TaskService
        bindTaskService();
        
        // khởi độngthông báo foreground
        startForeground(NOTIFICATION_ID, buildNotification());
        Log.d(TAG, "✓ 前台Service已启动");
        
        // hiển thịoverlay window
        try {
            showFloatingWindow();
        } catch (Exception e) {
            Log.e(TAG, "❌ 显示悬浮窗失败", e);
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.toast_show_overlay_failed) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        
        Log.d(TAG, "═══════════════════════════════════════");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // bịkillsautự độngkhởi động lại
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void bindTaskService() {
        if (taskService != null) {
            Log.d(TAG, "TaskService已连接，跳过绑定");
            return;
        }
        
        try {
            if (!Shizuku.pingBinder()) {
                Log.e(TAG, "❌ Shizuku不可用");
                return;
            }
            
            if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "❌ 无Shizuku权限");
                return;
            }
            
            Log.d(TAG, "→ 正在绑定TaskService...");
            Shizuku.bindUserService(serviceArgs, taskServiceConnection);
        } catch (Exception e) {
            Log.e(TAG, "❌ 绑定TaskService失败", e);
            e.printStackTrace();
        }
    }
    
    private void createNotificationChannel() {
        // khôngtạomớithông quađạo, sử dụngMRSSkernel servicethông quađạo（đãtồnở）
    }
    
    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );
        
        // thống nhấtsử dụngMRSSkernel servicethông báothức
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_kernel_service))
            .setContentText(getString(R.string.notif_mrss_running))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
    
    /**
 * hiển thịoverlay window
 */
    private void showFloatingWindow() {
        Log.d(TAG, "→ 准备显示悬浮窗");
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) {
            Log.e(TAG, "❌ 无法获取WindowManager");
            return;
        }
        Log.d(TAG, "✓ WindowManager已获取");
        
        // tạooverlay windowbố cục
        Log.d(TAG, "→ 创建悬浮窗视图");
        floatingView = createFloatingView();
        if (floatingView == null) {
            Log.e(TAG, "❌ 创建视图失败");
            return;
        }
        Log.d(TAG, "✓ 视图已创建");
        
        // cài đặtoverlay windowtham số
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 20;
        params.y = 200;
        
        Log.d(TAG, "→ 参数设置完成，准备添加视图");
        Log.d(TAG, "  TYPE: " + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? "TYPE_APPLICATION_OVERLAY" : "TYPE_PHONE"));
        
        try {
            windowManager.addView(floatingView, params);
            Log.d(TAG, "✅ 悬浮窗已成功添加到WindowManager");
        } catch (Exception e) {
            Log.e(TAG, "❌ 添加悬浮窗失败", e);
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
 * tạooverlay windowđồ
 */
    private View createFloatingView() {
        Log.d(TAG, "→ 开始创建悬浮窗布局");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(16, 16, 16, 16);
        layout.setGravity(android.view.Gravity.CENTER); // trêndướitráiphảitrong
        
        Log.d(TAG, "✓ LinearLayout已创建");
        
        // sau - dải màu（và khácUImột）
        GradientDrawable background = new GradientDrawable();
        background.setOrientation(GradientDrawable.Orientation.TL_BR);
        background.setColors(new int[]{
            0xE0FF9D88,  // （88%khôngtrong suốt）
            0xE0FFB5C5,  // （88%khôngtrong suốt）
            0xE0E0B5DC,  // （88%khôngtrong suốt）
            0xE0A8C5E5   // （88%khôngtrong suốt）
        });
        background.setCornerRadius(60);
        layout.setBackground(background);
        
        // nút đóng（×）- trướcsáng
        final android.widget.TextView closeButton = new android.widget.TextView(this);
        closeButton.setText("×");
        closeButton.setTextColor(Color.WHITE);
        closeButton.setTextSize(32);
        closeButton.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        closeParams.gravity = android.view.Gravity.CENTER; // trêndướitrong
        closeParams.leftMargin = 24;
        closeButton.setLayoutParams(closeParams);
        
        closeButton.setOnClickListener(v -> {
            // quaytrongkhôngcho phépđóng
            if (isRecording) {
                Toast.makeText(this, getString(R.string.toast_stop_recording_first), Toast.LENGTH_SHORT).show();
                return;
            }
            // dừngdịch vụ（đóngoverlay window）
            stopSelf();
        });
        
        // quay/dừngnút（, ）
        final View recordButton = new View(this);
        int buttonSize = 120;
        LinearLayout.LayoutParams recordParams = new LinearLayout.LayoutParams(buttonSize, buttonSize);
        recordParams.gravity = android.view.Gravity.CENTER; // trêndướitrong
        recordButton.setLayoutParams(recordParams);
        
        // khởi tạotrạng thái：quaynút（thực）
        updateRecordButtonState(recordButton, false);
        
        // nhấpsự kiện
        recordButton.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
                updateRecordButtonState(recordButton, true);
                // quaythời gianẩnnút đóng
                closeButton.setVisibility(View.GONE);
            } else {
                stopRecordingInternal(recordButton, closeButton);
                updateRecordButtonState(recordButton, false);
                // chú ý：nút đóng sẽ ở khi dừng quay xong mới hiển thị（ởstopRecordingInternalToastquaygọitrong）
            }
        });
        
        layout.addView(recordButton);
        layout.addView(closeButton);
        
        Log.d(TAG, "✓ 按钮已添加到布局");
        
        // độngchức năng
        final WindowManager.LayoutParams[] params = new WindowManager.LayoutParams[1];
        layout.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (params[0] == null) {
                    params[0] = (WindowManager.LayoutParams) floatingView.getLayoutParams();
                }
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params[0].x;
                        initialY = params[0].y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        params[0].x = initialX + (int) (initialTouchX - event.getRawX());
                        params[0].y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params[0]);
                        return true;
                }
                return false;
            }
        });
        
        Log.d(TAG, "✓ 悬浮窗布局创建完成");
        return layout;
    }
    
    /**
 * cập nhậtquaynúttrạng thái
 */
    private void updateRecordButtonState(View button, boolean recording) {
        GradientDrawable drawable = new GradientDrawable();
        
        if (recording) {
            // dừngtrạng thái：cách
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(20);
            drawable.setColor(Color.RED);
            drawable.setSize(60, 60); // cáchtrongbộ phậnnhỏ
        } else {
            // quaytrạng thái：
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(Color.RED);
        }
        
        drawable.setStroke(6, Color.WHITE); // khung
        button.setBackground(drawable);
    }
    
    /**
 * đảm bảo TaskService kết nối
 */
    private boolean ensureTaskServiceConnected() {
        if (taskService != null) {
            Log.d(TAG, "✓ TaskService已连接");
            return true;
        }
        
        Log.w(TAG, "⚠ TaskService未连接，尝试重新绑定...");
        
        // thử bind
        bindTaskService();
        
        // chờkết nối（tối đa3giây）
        int attempts = 0;
        while (taskService == null && attempts < 30) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
            attempts++;
        }
        
        if (taskService != null) {
            Log.d(TAG, "✅ TaskService重连成功");
            return true;
        } else {
            Log.e(TAG, "❌ TaskService重连失败（超时3秒）");
            return false;
        }
    }
    
    /**
 * liên tụcđánh thứcmàn hình sauvụ - quaytrong thời gianngăn chặnmàn hình sautắt màn hình
 */
    private final Runnable wakeupRearScreenRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording && taskService != null) {
                try {
                    // vềmàn hình sau(displayId=1)gửiWAKEUPđánh thứcsố
                    taskService.executeShellCommand("input -d 1 keyevent KEYCODE_WAKEUP");
                    // khôngxuấtlogbằngítquétmàn hình
                } catch (Exception e) {
                    Log.w(TAG, "背屏唤醒失败: " + e.getMessage());
                }
            }
            
            // liên tục gửi, 100msmột lần
            if (isRecording) {
                wakeupHandler.postDelayed(this, WAKEUP_INTERVAL_MS);
            }
        }
    };
    
    /**
 * khởi độngmàn hình sauliên tụcđánh thức
 */
    private void startRearScreenWakeup() {
        if (wakeupHandler != null) {
            // ngaymột lầnđánh thức, sau đóbắt đầuliên tục gửi
            wakeupHandler.post(wakeupRearScreenRunnable);
            Log.d(TAG, "⏰ 背屏持续唤醒已启动 (100ms间隔)");
        }
    }
    
    /**
 * dừngmàn hình sauliên tụcđánh thức
 */
    private void stopRearScreenWakeup() {
        if (wakeupHandler != null) {
            wakeupHandler.removeCallbacks(wakeupRearScreenRunnable);
            Log.d(TAG, "⏸️ 背屏持续唤醒已停止");
        }
    }
    
    /**
 * bắt đầuquay
 */
    private void startRecording() {
        new Thread(() -> {
            // đảm bảo TaskService đãkết nối
            if (!ensureTaskServiceConnected()) {
                Log.e(TAG, "TaskService未连接");
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, getString(R.string.toast_service_not_ready_retry), Toast.LENGTH_SHORT).show();
                });
                return;
            }
            
            // khởi độngquaytrướctrướcgửimột lầnkeycode wakeupđếnmàn hình sau
            try {
                taskService.executeShellCommand("input -d 1 keyevent KEYCODE_WAKEUP");
                Thread.sleep(200); // chờwakeup
            } catch (Exception e) {
                Log.w(TAG, "启动前背屏keycode wakeup失败: " + e.getMessage());
            }
            
            try {
                // thànhfiletên
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new java.util.Date());
                currentVideoPath = "/storage/emulated/0/Movies/MRSS_" + timestamp + ".mp4";
                
                // tạolưumụcghi
                taskService.executeShellCommand("mkdir -p /storage/emulated/0/Movies");
                Log.d(TAG, "✓ 目录已创建");
                
                // lấymàn hình sauthậtthựcdisplay ID（chụp màn hìnhlogic）
                String getDisplayIdCmd = "dumpsys SurfaceFlinger --display-id | grep -oE 'Display [0-9]+' | awk 'NR==2{print $2}'";
                String displayId = taskService.executeShellCommandWithResult(getDisplayIdCmd);
                
                if (displayId == null || displayId.trim().isEmpty()) {
                    displayId = "1"; // mặc địnhsử dụng1
                    Log.w(TAG, "⚠ 未能获取display ID，使用默认值: 1");
                } else {
                    displayId = displayId.trim();
                    Log.d(TAG, "✓ 背屏display ID: " + displayId);
                }
                
                // trướcđothửscreenrecordlệnhcócó thểngười dùng
                String testCmd = "which screenrecord";
                String testResult = taskService.executeShellCommandWithResult(testCmd);
                Log.d(TAG, "screenrecord路径: " + testResult);
                
                if (testResult == null || testResult.trim().isEmpty()) {
                    Log.e(TAG, "❌ screenrecord命令不存在");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(this, getString(R.string.toast_screenrecord_not_supported), Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                
                // sử dụnghoàn toànđường dẫnkhởi độngquay màn hình
                String screenrecordPath = testResult.trim();
                String pidFile = "/data/local/tmp/mrss_record.pid";
                String logFile = "/data/local/tmp/mrss_record.log";
                
                // backgroundkhởi độngquay màn hìnhvàlưuxuấtđếnlog
                String recordCmd = String.format(
                    "%s --display-id %s --bit-rate 20000000 %s > %s 2>&1 & echo $! > %s",
                    screenrecordPath, displayId, currentVideoPath, logFile, pidFile
                );
                
                Log.d(TAG, "→ 执行录屏命令: " + recordCmd);
                
                // qua TaskService（cóShizukuquyền）
                boolean cmdSuccess = taskService.executeShellCommand(recordCmd);
                Log.d(TAG, "命令执行结果: " + cmdSuccess);
                
                if (!cmdSuccess) {
                    Log.e(TAG, "❌ 启动录屏命令失败");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(this, getString(R.string.toast_start_record_failed), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // chờprocesskhởi độngvàPIDfilethành
                Thread.sleep(800);
                
                // PID
                String pidStr = taskService.executeShellCommandWithResult("cat " + pidFile);
                Log.d(TAG, "PID文件内容: " + pidStr);
                
                if (pidStr != null && !pidStr.trim().isEmpty()) {
                    try {
                        recordPid = Integer.parseInt(pidStr.trim());
                        Log.d(TAG, "✓ 录屏进程PID: " + recordPid);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "⚠ 解析PID失败: " + pidStr);
                    }
                } else {
                    Log.e(TAG, "❌ 无法读取PID文件");
                }
                
                // khởi độnglogtralỗi
                String logContent = taskService.executeShellCommandWithResult("cat " + logFile);
                if (logContent != null && !logContent.trim().isEmpty()) {
                    Log.d(TAG, "录屏进程日志: " + logContent);
                }
                
                // trải nghiệmprocesscóthậtở（nhiềucách）
                Log.d(TAG, "→ 验证录屏进程...");
                
                // phương thức1: ps aux
                String checkCmd1 = "ps -A | grep screenrecord";
                String checkResult1 = taskService.executeShellCommandWithResult(checkCmd1);
                Log.d(TAG, "ps -A结果: " + checkResult1);
                
                // phương thức2: ps -p
                String checkCmd2 = "ps -p " + recordPid;
                String checkResult2 = taskService.executeShellCommandWithResult(checkCmd2);
                Log.d(TAG, "ps -p结果: " + checkResult2);
                
                // phương thức3: kiểm trafilecóbắt đầuthành
                Thread.sleep(500);
                String checkFile = "ls -l " + currentVideoPath;
                String fileCheck = taskService.executeShellCommandWithResult(checkFile);
                Log.d(TAG, "文件检查: " + fileCheck);
                
                // nếuprocessở hoặc fileđãbắt đầuthành, nghĩthành công
                boolean processRunning = (checkResult1 != null && checkResult1.contains("screenrecord")) ||
                                       (checkResult2 != null && checkResult2.contains(String.valueOf(recordPid)));
                boolean fileExists = (fileCheck != null && !fileCheck.contains("No such file"));
                
                if (processRunning || fileExists) {
                    Log.d(TAG, "✓ 录屏已启动 (进程运行=" + processRunning + ", 文件存在=" + fileExists + ")");
                    isRecording = true;
                    
                    // quaythành côngkhởi độngsau, bắt đầuliên tụcđánh thứcmàn hình sau
                    startRearScreenWakeup();
                } else {
                    Log.e(TAG, "❌ 录屏进程未启动");
                    
                    // kiểm tralỗinguyên nhân
                    String errorCheck = "screenrecord --display-id 1 --help 2>&1 | head -n 5";
                    String errorMsg = taskService.executeShellCommandWithResult(errorCheck);
                    Log.e(TAG, "错误信息: " + errorMsg);
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(this, getString(R.string.toast_record_process_failed), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // cập nhậtthông báovàToast
                new Handler(Looper.getMainLooper()).post(() -> {
                    Notification notification = buildNotification();
                    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (nm != null) {
                        nm.notify(NOTIFICATION_ID, notification);
                    }
                    
                    Toast.makeText(this, getString(R.string.toast_start_recording_rear), Toast.LENGTH_SHORT).show();
                });
                
                Log.d(TAG, "✅ 录屏已开始: " + currentVideoPath);
                
            } catch (Exception e) {
                Log.e(TAG, "录屏失败", e);
                e.printStackTrace();
                isRecording = false;
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, getString(R.string.toast_record_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    /**
 * dừng quay（nútngười dùng, dùng chocập nhậttrạng thái）
 */
    private void stopRecordingInternal(final View recordButton, final android.widget.TextView closeButton) {
        if (!isRecording) {
            return;
        }
        
        new Thread(() -> {
            // đảm bảo TaskService kết nối（chủ độngkết nối lại）
            if (!ensureTaskServiceConnected()) {
                Log.e(TAG, "❌ 停止录制失败：TaskService未连接");
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(this, getString(R.string.toast_service_not_ready_stop), Toast.LENGTH_SHORT).show();
                });
                return;
            }
            
            try {
                if (recordPid > 0) {
                    Log.d(TAG, "→ 停止录屏进程 (PID=" + recordPid + ")");
                    
                    // gửiSIGINTsốdừng quay（ưudừng）
                    String killCmd = "kill -2 " + recordPid;
                    boolean killed = taskService.executeShellCommand(killCmd);
                    
                    if (killed) {
                        Log.d(TAG, "✓ SIGINT信号已发送");
                    } else {
                        Log.w(TAG, "⚠ SIGINT失败，尝试SIGTERM");
                        taskService.executeShellCommand("kill " + recordPid);
                    }
                    
                    Thread.sleep(1000); // chờprocessưuthoátvàlưufile
                    
                    isRecording = false;
                    recordPid = -1;
                    
                    // dừngmàn hình sauliên tụcđánh thức
                    stopRearScreenWakeup();
                    
                    // trải nghiệmfilecótồnở
                    String checkFile = "ls -lh " + currentVideoPath;
                    String fileInfo = taskService.executeShellCommandWithResult(checkFile);
                    Log.d(TAG, "文件信息: " + fileInfo);
                    
                    // làm mớimedia library
                    String refreshCmd = "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://" + currentVideoPath;
                    taskService.executeShellCommand(refreshCmd);
                    Log.d(TAG, "✓ 媒体库已刷新");
                    
                    // cập nhậtthông báovàToast
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Notification notification = buildNotification();
                        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        if (nm != null) {
                            nm.notify(NOTIFICATION_ID, notification);
                        }
                        
                        if (fileInfo != null && !fileInfo.contains("No such file")) {
                            Toast.makeText(this, getString(R.string.toast_record_saved_movies), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, getString(R.string.toast_record_may_failed), Toast.LENGTH_LONG).show();
                        }
                        
                        // hiển thịnút đóng
                        if (closeButton != null) {
                            closeButton.setVisibility(View.VISIBLE);
                        }
                    });
                    
                    Log.d(TAG, "✅ 录屏已停止并保存: " + currentVideoPath);
                }
            } catch (Exception e) {
                Log.e(TAG, "停止录屏失败", e);
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
 * dừng quay（tương thíchphương thức）
 */
    private void stopRecording() {
        stopRecordingInternal(null, null);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // dừngmàn hình sauliên tụcđánh thức
        stopRearScreenWakeup();
        
        // dừng quay
        if (isRecording) {
            stopRecording();
        }
        
        // gỡ bỏoverlay window
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
                Log.d(TAG, "✓ 悬浮窗已移除");
            } catch (Exception e) {
                Log.e(TAG, "移除悬浮窗失败", e);
            }
        }
        
        // unbind TaskService
        if (taskService != null) {
            try {
                Shizuku.unbindUserService(serviceArgs, taskServiceConnection, true);
            } catch (Exception e) {
                Log.e(TAG, "解绑TaskService失败", e);
            }
            taskService = null;
        }
        
        instance = null;
        Log.d(TAG, "⚠ Service已销毁");
    }
}

