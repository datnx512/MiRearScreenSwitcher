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

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

/**
 * lâuhóamàn hình saugiữ sángActivity
 * sửachiến lược：
 * 1. khônglạitự độngđóng, giữtồnsốngbằngFLAG_KEEP_SCREEN_ON
 * 2. sử dụngTYPE_APPLICATION_OVERLAYgiữnhìn thấy, cùngthời gianFLAG_NOT_TOUCHABLEkhông ảnh hưởnggiaolẫn
 * 3. toàn màn hình + trong suốt hoàn toàn（alpha=0）, người dùnghoàn toànkhôngđếnnhưnghệ thốngnghĩnhìn thấy
 * 
 * keyphímpháthiện（3lần）：
 * V1: FLAG_NOT_FOCUSABLE → ngayonPause/onStop（18ms）
 * V2: gỡ bỏFLAG_NOT_FOCUSABLE + màn hìnhngoài(-1000,-1000) → sau đóonStop（109ms）
 * V3: cửa sổphảiởmàn hìnhtrong(0,0) + alpha=0trong suốt → tối ưukết thúcgiải pháp✅
 */
public class RearScreenWakeActivity extends Activity {
    private static final String TAG = "RearScreenWakeActivity";
    private static RearScreenWakeActivity instance = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // lưu instancengười dùng
        instance = this;
        
        // lấy hiện tạidisplay
        int displayId = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            displayId = getDisplay().getDisplayId();
        }
        // V6cập nhật：phân vùngphânmàn hình chínhvàmàn hình saukhácchiến lược
        boolean isMainDisplay = (displayId == 0);
        
        if (isMainDisplay) {
        } else {
        }
        
        // --- thông dụngcài đặt（màn hình chínhvàmàn hình sauđềucần） ---
        
        // cài đặtđen thuần trong suốttrongchứa（OLEDtối ưu）
        View rootView = new View(this);
        rootView.setBackgroundColor(0x00000000); // trong suốt hoàn toàn
        setContentView(rootView);
        
        // keyphímsửa：sử dụngTYPE_APPLICATION_OVERLAYgiữ cửa sổ luôn nhìn thấy
        // loại nàycửa sổ loạikhôngsẽbịhệ thống tự ẩn
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        }
        
        // màn hình chínhvàmàn hình sausử dụng kháccấu hình cửa sổ
        WindowManager.LayoutParams params = getWindow().getAttributes();
        
        if (isMainDisplay) {
            // màn hình chính：nhỏcửa sổ + không cótiêu điểm, hoàn toànkhông ảnh hưởngngười dùngthao tác
            params.width = 1;   // 1pixelđộ rộng
            params.height = 1;  // 1pixelđộ cao
            params.x = 0;
            params.y = 0;
            params.alpha = 0.0f;  // trong suốt hoàn toàn
        } else {
            // màn hình sau：toàn màn hình + có thểlấytiêu điểm, giữgiữ sáng
            params.screenBrightness = 0.01f;  // 1% độ sáng
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.x = 0;
            params.y = 0;
            params.alpha = 0.0f;  // trong suốt hoàn toàn
        }
        getWindow().setAttributes(params);
        
        // Flagscấu hình：màn hình chínhvàmàn hình saukhác
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        
        if (isMainDisplay) {
            // màn hình chính：thêmFLAG_NOT_FOCUSABLE, tránhảnh hưởngngười dùngthao tác
            flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            // màn hình sau：thêmFLAG_KEEP_SCREEN_ON, giữmàn hìnhgiữ sáng
            // gỡ bỏFLAG_SHOW_WHEN_LOCKEDbằngtránh khi khóa màn hìnhcan thiệpcảm ứngtay
            flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        }
        
        getWindow().addFlags(flags);
        
        // gỡ bỏ setShowWhenLocked, tránh khi khóa màn hìnhcan thiệpcảm ứng
        // bây giờkhi khóa màn hìnhActivitysẽbịẩn, nhưngServicetiếp tụcgiữLaunchertắt
        // **khônglạitự độngđóng** - giữActivitytồnsốngbằnggiữ sáng
        // gỡ bỏnguyên postDelayed(finish()) code
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // lấy hiện tạidisplay
        int displayId = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            displayId = getDisplay().getDisplayId();
        }
        boolean isMainDisplay = (displayId == 0);
        // đảm bảoflagsliên tục
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
        }
        
        // làm lạiứng dụngflags（theodisplaykhác）
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        
        if (isMainDisplay) {
            flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        }
        
        getWindow().addFlags(flags);
        
        // làm lạicài đặtcửa sổtham số
        WindowManager.LayoutParams params = getWindow().getAttributes();
        if (isMainDisplay) {
            params.width = 1;
            params.height = 1;
            params.x = 0;
            params.y = 0;
            params.alpha = 0.0f;
        } else {
            params.screenBrightness = 0.01f;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.x = 0;
            params.y = 0;
            params.alpha = 0.0f;
        }
        getWindow().setAttributes(params);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // ngaysẽtiêu điểmchuyểnquay（thửchoứng dụnglàm lạinhận đượcđượctiêu điểm）
            // sử dụngmoveTaskToBackmàkhônglàfinish, giữActivitytồnsống
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    moveTaskToBack(true);
                }
            }, 100); // 100mstrễ
        } else {
            // thửlấy hiện tạiforegroundthông tin task
            logCurrentTaskStack();
        }
    }
    
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }
    
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
    
    /**
 * thửghihiện tạivụthông tin（gọidùng thử）
 */
    private void logCurrentTaskStack() {
        try {
            // đơnghi, khôngsử dụngcầnquyềnAPI
        } catch (Exception e) {
            Log.e(TAG, "Error logging task stack", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }
    
    /**
 * nângchophương thức staticdùng chongoàibộ phậnđóngActivity
 * có thểqua RearScreenWakeActivity.closeIfExists() dừnggiữ sáng
 */
    public static void closeIfExists() {
        if (instance != null) {
            instance.finish();
        }
    }
    
    /**
 * kiểm traActivitycótồnsống
 */
    public static boolean isAlive() {
        return instance != null;
    }
    
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0); // tắtchuyểnhoạt ảnh
    }
}

