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

import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.Keep;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * ởShizukuprocesstrongdịch vụ, cóshellquyền
 */
public class TaskService extends ITaskService.Stub {
    private static final String TAG = "TaskService";

    @Keep
    public TaskService() {

    }

    @Override
    public void destroy() {

        System.exit(0);
    }

    @Override
    public String getCurrentForegroundApp() throws RemoteException {
        try {

            // am stack list, ởShizukuprocesstrongcóshellquyền
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", "am stack list");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            
            boolean inDisplayZero = false;
            String line;
            while ((line = reader.readLine()) != null) {
                // RootTask：kiểm tradisplayId
                if (line.startsWith("RootTask")) {
                    inDisplayZero = line.contains("displayId=0");
                    continue;
                }
                
                // taskId（vào）
                if (inDisplayZero && line.contains("taskId=") && line.contains("/")) {
                    // parse: taskId=1471: com.example.display_switcher/com.example.display_switcher.MainActivity
                    int tidStart = line.indexOf("taskId=") + 7;
                    int tidEnd = line.indexOf(':', tidStart);
                    String taskId = line.substring(tidStart, tidEnd).trim();
                    
                    int pkgStart = tidEnd + 2;
                    int pkgEnd = line.indexOf('/', pkgStart);
                    String packageName = line.substring(pkgStart, pkgEnd).trim();
                    
                    // bỏ quaLaunchervàứng dụngtự
                    if (packageName.contains("launcher") || 
                        packageName.contains("miui.home") ||
                        packageName.equals("com.tgwgroup.MiRearScreenSwitcher")) {
                        continue;
                    }
                    
                    reader.close();
                    process.destroy();
                    
                    String result = packageName + ":" + taskId;

                    return result;
                }
            }
            
            int exitCode = process.waitFor();
            reader.close();

            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting current app", e);
            return null;
        }
    }

    @Override
    public int getTaskIdByPackage(String packageName) throws RemoteException {
        try {

            // am stack list, ởShizukuprocesstrongcóshellquyền
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", "am stack list");
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("taskId=") && line.contains(packageName)) {
                    // parse: taskId=1434: com.android.camera/...
                    int start = line.indexOf("taskId=") + 7;
                    int end = line.indexOf(':', start);
                    String taskId = line.substring(start, end).trim();
                    int tid = Integer.parseInt(taskId);
                    
                    reader.close();
                    process.destroy();

                    return tid;
                }
            }
            
            reader.close();
            process.waitFor();

            return -1;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting taskId", e);
            return -1;
        }
    }

    @Override
    public boolean moveTaskToDisplay(int taskId, int displayId) throws RemoteException {
        try {
            long startTime = System.currentTimeMillis();

            // trướclấytên package
            String packageName = getPackageNameFromTaskId(taskId);

            // service calllệnh, ởShizukuprocesstrongcóshellquyền
            // chú ý：Androidmỗi màn hình của hệ thống đều có thanh trạng thái độc lập（SystemUI�?
            // tất nhiênứng dụngchuyểnđếnmàn hình sauthời gian, nósẽhiển thịmàn hình sauthanh trạng thái, nàylàhệ thốngmặc định�?
            // cần giữ thanh trạng thái màn hình chính nhìn thấy cần sửa cấp hệ thống, không cóphápquaứng dụngthực�?
            String cmd = "service call activity_task 50 i32 " + taskId + " i32 " + displayId;

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            boolean success = (exitCode == 0);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // nếuthành côngchuyển đến màn hình sau（displayId=1）, lưuthông tin task
            if (success && displayId == 1) {
                try {
                    if (packageName != null) {
                        // lưu vàobroadcast receiver, bằnghệ thốngsự kiệnsaukhôi phục
                        RearScreenBroadcastReceiver.saveLastTask(packageName, taskId);

                    } else {

                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to save task info", e);
                }
            }

            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION in moveTaskToDisplay", e);
            return false;
        }
    }
    
    /**
 * theotaskIdlấytên package（phương thức）
 */
    private String getPackageNameFromTaskId(int taskId) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", "am stack list");
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("taskId=" + taskId) && line.contains("/")) {
                    // parse: taskId=1471: com.example.app/...
                    int pkgStart = line.indexOf(':') + 2;
                    int pkgEnd = line.indexOf('/', pkgStart);
                    if (pkgEnd > pkgStart) {
                        String packageName = line.substring(pkgStart, pkgEnd).trim();
                        reader.close();
                        process.destroy();
                        return packageName;
                    }
                }
            }
            
            reader.close();
            process.waitFor();
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting package name from taskId", e);
            return null;
        }
    }

    @Override
    public boolean launchWakeActivity(int displayId) throws RemoteException {
        try {
            long startTime = System.currentTimeMillis();

            // sử dụngam startlệnhởchắc chắndisplaytrênkhởi độngRearScreenWakeupActivity
            // --displaytham sốchắc chắnmụcđánh dấudisplay
            // chú ý：RearScreenWakeupActivitysử dụngFLAG_TURN_SCREEN_ONbật sáng màn hình
            String cmd = "am start --display " + displayId + 
                        " -n com.tgwgroup.MiRearScreenSwitcher/.RearScreenWakeupActivity";

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // xuất
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");

            }
            reader.close();
            
            int exitCode = process.waitFor();
            boolean success = (exitCode == 0);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION in launchWakeActivity", e);
            return false;
        }
    }
    
    @Override
    public boolean disableSubScreenLauncher() throws RemoteException {
        try {

            // dừngprocess（processcó thểsẽtự độngkhởi động lại, cầnliên tụckill）
            String killCmd = "am force-stop com.xiaomi.subscreencenter";

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", killCmd);
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {

            } else {

            }

            return (exitCode == 0);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION in disableSubScreenLauncher", e);
            return false;
        }
    }
    
    /**
 * V12kill processpháp：kiểm traLauncherprocesscóở
 */
    @Override
    public boolean isLauncherProcessRunning() throws RemoteException {
        try {
            // kiểm traprocesscóở
            String cmd = "ps -A | grep com.xiaomi.subscreencenter";
            
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            
            String line = reader.readLine();
            reader.close();
            process.waitFor();
            
            // nếu cóxuất�?�?processở�?�?trả vềtrue（cầnkill�?
            // nếukhông cóxuất�?�?processkhôngở �?trả vềfalse（không cầnxử lý）
            boolean isRunning = (line != null && !line.isEmpty());
            
            if (isRunning) {

            }
            
            return isRunning;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION in isLauncherProcessRunning", e);
            return false;
        }
    }
    
    /**
 * V12kill processpháp：thửkillLauncherprocess
 * trả vềtrue = thành côngkill（giải thíchprocessở）
 * trả vềfalse = thất bại（giải thíchprocesskhôngở）
 */
    @Override
    public boolean killLauncherProcess() throws RemoteException {
        try {
            // dừngprocess
            String cmd = "am force-stop com.xiaomi.subscreencenter";
            
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            
            // force-stop làtrả về0, nêncầnkiểm traprocesscóthậtbịkill
            // đơn, nếulệnhthành côngliềntrả vềtrue
            return (exitCode == 0);
            
        } catch (Exception e) {
            // exceptioncũngtrả vềfalse（im lặng）
            return false;
        }
    }
    
    @Override
    public boolean enableSubScreenLauncher() throws RemoteException {
        try {

            // khởi độngSubScreenLauncher（processsẽtự độngkhởi động）
            String startCmd = "am start --display 1 -n com.xiaomi.subscreencenter/.SubScreenLauncher";

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", startCmd);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            
            String line;
            while ((line = reader.readLine()) != null) {

            }
            reader.close();
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {

            } else {

            }

            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION in enableSubScreenLauncher", e);
            return false;
        }
    }
    
    // xóachưasử dụngwakeUpDisplayphương thức
    
    @Override
    public boolean forceStatusBarToMainDisplay() throws RemoteException {
        try {

            // mớichiến lược：trực tiếpmở rộngthanh trạng thái màn hình chính, màkhônglàchuyểnhoặckhởi động lạiSystemUI
            // nàysẽmàn hình chínhhiển thịSystemUI, từmàgiữtiêu điểmởmàn hình chính
            
            // phương thức1: mở rộngthanh trạng thái màn hình chính（khônghoàn toànmở rộng, chỉlàsống）
            String expandCmd = "cmd statusbar expand-settings";

            ProcessBuilder pb1 = new ProcessBuilder("sh", "-c", expandCmd);
            Process process1 = pb1.start();
            int exitCode1 = process1.waitFor();
            
            if (exitCode1 == 0) {

                Thread.sleep(30);  // ngắntrễ
                
                // ngaythu gọn
                String collapseCmd = "cmd statusbar collapse";
                ProcessBuilder pb2 = new ProcessBuilder("sh", "-c", collapseCmd);
                Process process2 = pb2.start();
                int exitCode2 = process2.waitFor();
                
                if (exitCode2 == 0) {

                } else {

                }
            } else {

            }
            
            // phương thức2: màn hình chínhSystemUInhìn thấy（quawmlệnh�?
            // cài đặtmàn hình chínhdisplaylàim lặng�?
            String wmCmd = "wm set-display-type 0 home";

            ProcessBuilder pb3 = new ProcessBuilder("sh", "-c", wmCmd);
            Process process3 = pb3.start();
            
            BufferedReader reader3 = new BufferedReader(
                new InputStreamReader(process3.getInputStream()), 8192
            );
            String line;
            while ((line = reader3.readLine()) != null) {

            }
            reader3.close();
            
            int exitCode3 = process3.waitFor();
            if (exitCode3 == 0) {

            } else {

            }
            
            // phương thức3: kiểm trahiện tạithanh trạng tháicấu hình

            ProcessBuilder pb4 = new ProcessBuilder("sh", "-c", "dumpsys window displays | grep -A20 'Display: 0'");
            Process process4 = pb4.start();
            
            BufferedReader reader4 = new BufferedReader(
                new InputStreamReader(process4.getInputStream()), 8192
            );
            while ((line = reader4.readLine()) != null) {
                if (line.contains("StatusBar") || line.contains("systemui")) {

                }
            }
            reader4.close();
            process4.waitFor();

            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION in forceStatusBarToMainDisplay", e);
            return false;
        }
    }
    
    /**
 * thu gọnthanh trạng thái/Control Center
 * @return cóthành công
 */
    @Override
    public boolean collapseStatusBar() throws RemoteException {
        try {

            // sử dụng cmd statusbar collapse lệnh
            String cmd = "cmd statusbar collapse";

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {

            } else {

            }

            return (exitCode == 0);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION in collapseStatusBar", e);
            return false;
        }
    }
    
    /**
 * lấy hiện tạimàn hình sauDPI
 * @return DPIgiá trị
 */
    @Override
    public int getCurrentRearDpi() throws RemoteException {
        try {

            // sử dụng wm density lệnhlấydisplay 1DPI
            String cmd = "wm density -d 1";

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            
            String line;
            int dpi = 0;
            while ((line = reader.readLine()) != null) {

                // parsexuất: "Physical density: 450" hoặc "Override density: 300"
                if (line.contains("density:")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        try {
                            String dpiStr = parts[1].trim();
                            // nếulà "Override density: 300", ưutrướcsử dụng
                            if (line.contains("Override density")) {
                                dpi = Integer.parseInt(dpiStr);

                                break; // đếnoverrideliềnkhôngtiếp tục
                            } else if (dpi == 0) {
                                // nếuvẫnkhôngđếnoverride, trướcghiphysical
                                dpi = Integer.parseInt(dpiStr);

                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Failed to parse DPI value from density output", e);
                        }
                    }
                }
            }
            reader.close();
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0 && dpi > 0) {

            } else {

            }

            return dpi;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION in getCurrentRearDpi", e);
            return 0;
        }
    }
    
    /**
 * cài đặtmàn hình sauDPI
 * @param dpi DPIgiá trị
 * @return cóthành công
 */
    @Override
    public boolean setRearDpi(int dpi) throws RemoteException {
        try {

            // sử dụng wm density lệnhcài đặtdisplay 1DPI
            String cmd = "wm density " + dpi + " -d 1";

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {

            } else {

            }

            return (exitCode == 0);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION in setRearDpi", e);
            return false;
        }
    }
    
    /**
 * khôi phụcmàn hình sauDPIđếngiá trị mặc định
 * @return cóthành công
 */
    @Override
    public boolean resetRearDpi() throws RemoteException {
        try {

            // sử dụng wm density reset lệnhkhôi phụcdisplay 1DPI
            String cmd = "wm density reset -d 1";

            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {

            } else {

            }

            return (exitCode == 0);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION in resetRearDpi", e);
            return false;
        }
    }
    
    /**
 * chụp màn hình saumàn hình
 * @return cóthành công
 */
    @Override
    public boolean takeRearScreenshot() throws RemoteException {
        try {
            // cắtmàn hìnhtrướcthửmàn hình saugửikeycode wakeup
            try {
                executeShellCommand("input -d 1 keyevent KEYCODE_WAKEUP");
                Thread.sleep(200); // chờwakeup
            } catch (Exception e) {
                Log.w(TAG, "背屏keycode wakeup失败: " + e.getMessage());
            }

            // tạolưumụcghi
            String mkdirCmd = "mkdir -p /storage/emulated/0/Pictures/RearDisplay";

            ProcessBuilder pb1 = new ProcessBuilder("sh", "-c", mkdirCmd);
            Process process1 = pb1.start();
            process1.waitFor();
            
            // lấymàn hình saudisplay ID
            String getDisplayIdCmd = "dumpsys SurfaceFlinger --display-id | grep -oE 'Display [0-9]+' | awk 'NR==2{print $2}'";

            ProcessBuilder pb2 = new ProcessBuilder("sh", "-c", getDisplayIdCmd);
            Process process2 = pb2.start();
            
            BufferedReader reader2 = new BufferedReader(
                new InputStreamReader(process2.getInputStream()), 8192
            );
            
            String displayId = reader2.readLine();
            reader2.close();
            process2.waitFor();
            
            if (displayId == null || displayId.isEmpty()) {
                displayId = "1"; // mặc địnhsử dụng1

            } else {

            }
            
            // thànhfiletên（thời gian）
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new java.util.Date());
            String filename = "/storage/emulated/0/Pictures/RearDisplay/RD_" + timestamp + ".png";
            
            // chụp màn hìnhlệnh
            String screenshotCmd = "screencap -p -d " + displayId + " " + filename;

            ProcessBuilder pb3 = new ProcessBuilder("sh", "-c", screenshotCmd);
            Process process3 = pb3.start();
            
            int exitCode = process3.waitFor();
            
            // làm mớimedia library, chochụp màn hìnhxuất hiệnởalbumtrong
            String refreshCmd = "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://" + filename;
            ProcessBuilder pb4 = new ProcessBuilder("sh", "-c", refreshCmd);
            pb4.start();
            
            // không cóthành côngthất bạiđềutrả vềtrue, choToasthiển thịthành công
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "❌ EXCEPTION in takeRearScreenshot", e);
            // tức làsử dụngexceptioncũngtrả vềtrue, choToasthiển thịthành công
            return true;
        }
    }
    
    @Override
    public boolean isTaskOnDisplay(int taskId, int displayId) throws RemoteException {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", "am stack list");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            
            boolean inTargetDisplay = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("RootTask")) {
                    inTargetDisplay = line.contains("displayId=" + displayId);
                    continue;
                }
                
                if (inTargetDisplay && line.contains("taskId=" + taskId)) {
                    reader.close();
                    process.destroy();
                    return true;
                }
            }
            
            reader.close();
            process.waitFor();
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking task on display", e);
            return false;
        }
    }
    
    @Override
    public String getForegroundAppOnDisplay(int displayId) throws RemoteException {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", "am stack list");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            
            boolean inTargetDisplay = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("RootTask")) {
                    inTargetDisplay = line.contains("displayId=" + displayId);
                    continue;
                }
                
                if (inTargetDisplay && line.contains("taskId=") && line.contains("/")) {
                    int tidStart = line.indexOf("taskId=") + 7;
                    int tidEnd = line.indexOf(':', tidStart);
                    String taskId = line.substring(tidStart, tidEnd).trim();
                    
                    int pkgStart = tidEnd + 2;
                    int pkgEnd = line.indexOf('/', pkgStart);
                    String packageName = line.substring(pkgStart, pkgEnd).trim();
                    
                    reader.close();
                    process.destroy();
                    
                    return packageName + ":" + taskId;
                }
            }
            
            reader.close();
            process.waitFor();
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting foreground app on display", e);
            return null;
        }
    }
    
    /**
 * V2.1: cài đặtmàn hìnhxoaycáchvề
 * @param displayId màn hìnhID (0=màn hình chính, 1=màn hình sau)
 * @param rotation xoay (0=0°, 1=90°, 2=180°, 3=270°)
 * @return cóthành công
 */
    @Override
    public boolean setDisplayRotation(int displayId, int rotation) throws RemoteException {
        try {
            // lấy hiện tạiứng dụng foreground màn hình sau（nếu có）
            String currentApp = null;
            int currentTaskId = -1;
            if (displayId == 1) {
                currentApp = getForegroundAppOnDisplay(1);
                if (currentApp != null && currentApp.contains(":")) {
                    String[] parts = currentApp.split(":");
                    try {
                        currentTaskId = Integer.parseInt(parts[1]);
                    } catch (Exception ignored) {
                        Log.e(TAG, "Failed to parse taskId from currentApp: " + currentApp, ignored);
                    }
                }
            }
            
            // sử dụng wm user-rotation lệnhcài đặtxoay
            String cmd = "wm user-rotation -d " + displayId + " lock " + rotation;
            
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()), 8192
            );
            
            String line;
            while ((line = reader.readLine()) != null) {}
            while ((line = errorReader.readLine()) != null) {}
            
            reader.close();
            errorReader.close();
            
            int exitCode = process.waitFor();
            
            // nếulàmàn hình sauvàcóứng dụngở, chờ500mssaukiểm travàphụcsống
            if (displayId == 1 && exitCode == 0 && currentTaskId > 0) {
                Thread.sleep(500);
                
                // kiểm traứng dụngcóvẫnởmàn hình sau
                boolean stillOnRear = isTaskOnDisplay(currentTaskId, 1);
                
                if (!stillOnRear) {
                    // ứng dụngbịđóng, làm lạicast
                    moveTaskToDisplay(currentTaskId, 1);
                }
            }
            
            return (exitCode == 0);
            
        } catch (Exception e) {
            Log.e(TAG, "设置旋转异常", e);
            return false;
        }
    }
    
    /**
 * V2.1: lấymàn hìnhhiện tạixoaycáchvề
 * @param displayId màn hìnhID (0=màn hình chính, 1=màn hình sau)
 * @return xoay (0-3), -1hiển thịthất bại
 */
    @Override
    public int getDisplayRotation(int displayId) throws RemoteException {
        try {
            // sử dụng wm user-rotation lệnhtrực tiếp, xuấtthức: "lock 2" hoặc "free"
            String cmd = "wm user-rotation -d " + displayId;
            
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            
            String line = reader.readLine();
            reader.close();
            process.waitFor();
            
            if (line != null && !line.isEmpty()) {
                // parse "lock 2" hoặc "free" thức
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    try {
                        return Integer.parseInt(parts[1]);
                    } catch (Exception ignored) {
                        Log.e(TAG, "Failed to parse rotation from wm user-rotation output: " + line, ignored);
                    }
                }
            }
            
            return 0;
            
        } catch (Exception e) {
            Log.e(TAG, "获取旋转异常", e);
            return 0;
        }
    }
    
    @Override
    public boolean executeShellCommand(String cmd) throws RemoteException {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()), 8192
            );
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            reader.close();
            errorReader.close();
            
            int exitCode = process.waitFor();
            
            // ghixuất
            if (output.length() > 0) {
                Log.d(TAG, "Command stdout: " + output.toString().trim());
            }
            if (errorOutput.length() > 0) {
                Log.w(TAG, "Command stderr: " + errorOutput.toString().trim());
            }
            
            return (exitCode == 0);
            
        } catch (Exception e) {
            Log.e(TAG, "执行命令失败: " + cmd, e);
            return false;
        }
    }
    
    @Override
    public String executeShellCommandWithResult(String cmd) throws RemoteException {
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()), 8192
            );
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            reader.close();
            process.waitFor();
            
            return output.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "执行命令失败: " + cmd, e);
            return "";
        }
    }
    
}
