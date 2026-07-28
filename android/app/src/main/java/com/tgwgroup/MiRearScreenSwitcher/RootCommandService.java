package com.tgwgroup.MiRearScreenSwitcher;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Executes shell commands with root privilege via su.
 * Used as fallback when Shizuku is not available.
 */
public class RootCommandService {
    private static final String TAG = "RootCommandService";
    private static Boolean hasRoot = null;

    /**
     * Check if device has root access by trying to run 'su -c id'.
     * Caches the result.
     */
    public static boolean hasRootAccess() {
        if (hasRoot != null) return hasRoot;
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            int exitCode = process.waitFor();
            reader.close();
            hasRoot = (exitCode == 0 && line != null && line.contains("uid=0"));
            Log.i(TAG, "Root check: " + (hasRoot ? "GRANTED" : "DENIED") + " (exit=" + exitCode + ")");
        } catch (Exception e) {
            Log.e(TAG, "Root check failed", e);
            hasRoot = false;
        }
        return hasRoot;
    }

    /**
     * Execute a shell command with root privilege.
     * @param command The shell command to execute
     * @return The stdout output, or null on failure
     */
    public static String execute(String command) {
        if (!hasRootAccess()) {
            Log.e(TAG, "No root access, cannot execute: " + command);
            return null;
        }
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 8192);
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder error = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                error.append(line).append("\n");
            }
            int exitCode = process.waitFor();
            reader.close();
            errorReader.close();
            
            if (exitCode != 0) {
                Log.e(TAG, "Command failed (exit=" + exitCode + "): " + command + "\nError: " + error);
                return null;
            }
            return output.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "Execute failed: " + command, e);
            return null;
        }
    }

    /**
     * Execute a shell command and return success/failure.
     */
    public static boolean executeBoolean(String command) {
        return execute(command) != null;
    }

    // === Mirrored TaskService methods ===

    public static String getCurrentForegroundApp() {
        String output = execute("am stack list");
        if (output == null) return null;
        // Parse the same way as TaskService
        boolean inDisplayZero = false;
        for (String line : output.split("\n")) {
            if (line.startsWith("RootTask")) {
                inDisplayZero = line.contains("displayId=0");
                continue;
            }
            if (inDisplayZero && line.contains("taskId=") && line.contains("/")) {
                int tidStart = line.indexOf("taskId=") + 7;
                int tidEnd = line.indexOf(':', tidStart);
                String taskId = line.substring(tidStart, tidEnd).trim();
                int pkgStart = tidEnd + 2;
                int pkgEnd = line.indexOf('/', pkgStart);
                String packageName = line.substring(pkgStart, pkgEnd).trim();
                if (packageName.contains("launcher") || 
                    packageName.contains("miui.home") ||
                    packageName.equals("com.tgwgroup.MiRearScreenSwitcher")) {
                    continue;
                }
                return packageName + ":" + taskId;
            }
        }
        return null;
    }

    public static boolean switchToRearDisplay(int taskId) {
        return executeBoolean("service call activity_task 50 s16 " + taskId + " i32 1");
    }

    public static boolean killPackage(String packageName) {
        return executeBoolean("am force-stop " + packageName);
    }

    public static int getCurrentRearDpi() {
        String output = execute("settings get system display_density_forced 1");
        if (output == null || output.isEmpty()) {
            output = execute("wm density -d 1");
            if (output != null && output.contains("Physical density: ")) {
                try {
                    return Integer.parseInt(output.replace("Physical density: ", "").trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            return 0;
        }
        try {
            return Integer.parseInt(output.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean setRearDpi(int dpi) {
        return executeBoolean("wm density -d 1 " + dpi);
    }

    public static boolean resetRearDpi() {
        return executeBoolean("wm density -d 1 reset");
    }

    public static boolean setDisplayRotation(int displayId, int rotation) {
        return executeBoolean("service call display 1 i32 " + displayId + " i32 " + rotation);
    }

    public static boolean sendWakeup() {
        return executeBoolean("input keyevent KEYCODE_WAKEUP");
    }

    public static boolean takeScreenshot(String path) {
        return executeBoolean("screencap -d 1 " + path);
    }

    public static boolean startScreenRecord(String path) {
        return executeBoolean("screenrecord --output-format mp4 " + path + " &");
    }
}
