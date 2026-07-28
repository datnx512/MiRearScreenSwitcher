/*
 * Author: AntiOblivionis
 * QQ: 319641317
 * Github: https://github.com/GoldenglowSusie/
 * Bilibili: Rhodes Island T0 Thuật sư điều khiển cơ giới Chengshan
 * 
 * Co-developed with AI assistants:
 * - Cursor
 * - Claude-4.5-Sonnet
 * - GPT-5
 * - Gemini-2.5-Pro
 */

package com.tgwgroup.MiRearScreenSwitcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * lắng nghenhỏmàn hình sautrạng tháibroadcast
 * tất nhiênmàn hình saubật sáng/tắtthời gian, tự độngkhôi phụcgiữ sángActivity, ngăn chặnbịhệ thốngLauncher
 */
public class RearScreenBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "RearScreenReceiver";
    
    // lưucuối cùngứng dụngthông tin
    private static String lastMovedPackage = null;
    private static int lastTaskId = -1;
    private static boolean rearScreenActive = false;
    
    /**
 * lưucuối cùngứng dụngthông tin
 * TaskService gọi
 */
    public static void saveLastTask(String packageName, int taskId) {
        lastMovedPackage = packageName;
        lastTaskId = taskId;
        rearScreenActive = true;
    }
    
    /**
 * xóalưuthông tin task
 */
    public static void clearLastTask() {
        lastMovedPackage = null;
        lastTaskId = -1;
        rearScreenActive = false;
    }
    
    /**
 * kiểm tracócósốngmàn hình sauvụ
 */
    public static boolean hasActiveTask() {
        return rearScreenActive && lastMovedPackage != null;
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        long timestamp = System.currentTimeMillis();
        if (hasActiveTask()) {
        }
        if ("miui.intent.action.SUB_SCREEN_ON".equals(action)) {
            // màn hình saubật sángthời gianxử lý
            handleScreenOn(context);
        } else if ("miui.intent.action.SUB_SCREEN_OFF".equals(action)) {
            // màn hình sautắtthời gianxử lý
            handleScreenOff(context);
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            // hệ thốngmàn hìnhđóng（có thểlàtắt màn hình）
            handleSystemScreenOff(context);
        } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
            // hệ thốngmàn hìnhmở
            handleSystemScreenOn(context);
        }
    }
    
    /**
 * xử lýmàn hình saubật sángsự kiện
 * thửkhôi phụctrước đógiữ sángActivityvàứng dụng
 */
    private void handleScreenOn(Context context) {
        if (hasActiveTask()) {
            // gỡ bỏActivity - hoàn toànService
            // Activitycửa sổ trong suốtsẽcan thiệp khóa màn hìnhthời giansự kiện cảm ứng, gâytrượtkhựng
            // không cầngửikhôi phụcbroadcast, Servicesẽliên tụctắt Launcher
        } else {
        }
    }
    
    /**
 * xử lýmàn hình sautắtsự kiện
 */
    private void handleScreenOff(Context context) {
        // màn hình sautắtthời gian, giữthông tin task, bằngdướilầnbật sángthời giankhôi phục
        if (hasActiveTask()) {
        } else {
        }
    }
    
    /**
 * xử lýhệ thốngmàn hìnhđóngsự kiện（tắt màn hìnhchờ）
 */
    private void handleSystemScreenOff(Context context) {
        if (hasActiveTask()) {
            // đảm bảoServiceở
            if (!RearScreenKeeperService.isRunning()) {
                Intent serviceIntent = new Intent(context, RearScreenKeeperService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } else {
            }
        }
    }
    
    /**
 * xử lýhệ thốngmàn hìnhmởsự kiện
 */
    private void handleSystemScreenOn(Context context) {
        if (hasActiveTask()) {
            // đảm bảoServiceở
            if (!RearScreenKeeperService.isRunning()) {
                Intent serviceIntent = new Intent(context, RearScreenKeeperService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
}

