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

import android.content.Context;
import android.os.Build;
import android.util.Log;
import io.flutter.app.FlutterApplication;
import rikka.sui.Sui;

/**
 * tựchắc chắnApplication - khởi tạoShizuku
 */
public class MyApplication extends FlutterApplication {
    
    private static final String TAG = "MyApplication";
    private static boolean isSui = false;
    
    static {
        // keyphím！ởstatictrongkhởi tạoSui
        try {
            isSui = Sui.init("com.tgwgroup.MiRearScreenSwitcher");
        } catch (Throwable e) {
            Log.e(TAG, "Sui init failed", e);
        }
    }
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        
        // HiddenAPI（Android 9+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                Class<?> hiddenApiBypass = Class.forName("org.lsposed.hiddenapibypass.HiddenApiBypass");
                hiddenApiBypass.getMethod("addHiddenApiExemptions", String.class)
                    .invoke(null, "L");
            } catch (Exception e) {
                Log.e(TAG, "Failed to apply HiddenApiBypass exemption", e);
            }
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
    }
    
    public static boolean isSui() {
        return isSui;
    }
}

