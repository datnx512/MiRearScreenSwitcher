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
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;

/**
 * dùng chobật sángmàn hình sautrong suốtActivity
 * tham MiRearScreenNotification thựchiện
 * V2.1: độngtrạng tháixoay
 */
public class RearScreenWakeupActivity extends Activity {
    private static final String TAG = "RearScreenWakeup";
    
    // biến statictồnmàn hình sauxoaycáchvề
    private static int sRearDisplayRotation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    
    /**
 * V2.1: cài đặtmàn hình sauxoaycáchvề（từngoàibộ phậngọi）
 * @param rotation xoaycáchvề (0=0°, 1=90°, 2=180°, 3=270°)
 */
    public static void setRearDisplayRotation(int rotation) {
        // sẽrotationgiá trịchuyểnđổilàActivityInfothườnglượng
        switch (rotation) {
            case 0:
                sRearDisplayRotation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case 1:
                sRearDisplayRotation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case 2:
                sRearDisplayRotation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                break;
            case 3:
                sRearDisplayRotation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
            default:
                sRearDisplayRotation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                break;
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ứng dụngxoaycài đặt
        if (sRearDisplayRotation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            setRequestedOrientation(sRearDisplayRotation);
        }
        
        // lấy hiện tạidisplay
        int displayId = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            displayId = getDisplay().getDisplayId();
        }
        // nếuởmàn hình chính, đềukhông
        if (displayId == 0) {
            return;
        }
        
        // --- bằngdướicodechỉ ởmàn hình sau (displayId == 1) ---
        
        // keyphím：ởmàn hình sauthời gianbật sáng màn hìnhvàgiữgiữ sáng
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        );
        
        // phân phốimớiAPI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        // trễđóng（thời gianbật sáng màn hình）
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            finish();
        }, 1000); // sau 1 giâyđóng
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // lần nữađảm bảobật sáng
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        );
    }
    
    @Override
    public void finish() {
        super.finish();
        // tắtchuyểnhoạt ảnh
        overridePendingTransition(0, 0);
    }
}

