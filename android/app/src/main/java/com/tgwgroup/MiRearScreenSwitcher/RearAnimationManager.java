/*
 * Author: AntiOblivionis
 * QQ: 319641317
 * Github: https://github.com/GoldenglowSusie/
 * Bilibili: Rhodes Island T0 Thuật sư điều khiển cơ giới Chengshan
 *
 * Chief Tester: �? *
 * Co-developed with AI assistants:
 * - Cursor
 * - Claude-4.5-Sonnet
 * - GPT-5
 * - Gemini-2.5-Pro
 */

package com.tgwgroup.MiRearScreenSwitcher;

import android.util.Log;

/**
 * màn hình sauanimation manager
 * thống nhấtquản lýhoạt ảnh sạcvàthông báo hoạt ảnh, thựchiệnhoạt ảnhngắt
 */
public class RearAnimationManager {
    private static final String TAG = "RearAnimationManager";
    
    // hoạt ảnhloại
    public enum AnimationType {
        NONE,           // không cóhoạt ảnh
        CHARGING,       // hoạt ảnh sạc
        NOTIFICATION    // thông báo hoạt ảnh
    }
    
    // hiện tạiđangpháthoạt ảnhloại
    private static volatile AnimationType currentAnimation = AnimationType.NONE;
    
    // hiện tạihoạt ảnhcónênkhôi phụcLauncher chính thức（bịmớihoạt ảnhngắtthìkhôngkhôi phục）
    private static volatile boolean shouldRestoreOnDestroy = true;
    
    // V3.5: hoạt ảnh sạc bị ngắtcólàchế độ giữ sáng
    private static volatile boolean interruptedChargingWasAlwaysOn = false;
    
    /**
 * bắt đầupháthoạt ảnh
 * @param type hoạt ảnhloại
 * @return hoạt ảnh cũ bị ngắtloại（NONEhiển thịkhôngcóhoạt ảnh cũ）
 */
    public static synchronized AnimationType startAnimation(AnimationType type) {
        if (type == AnimationType.NONE) {
            Log.w(TAG, "⚠️ 尝试启动NONE类型的动画，忽略");
            return AnimationType.NONE;
        }
        
        AnimationType oldAnimation = currentAnimation;
        
        if (oldAnimation != AnimationType.NONE) {
            Log.d(TAG, String.format("🔄 新动画[%s]打断旧动画[%s]", type, oldAnimation));
            // đánh dấuhoạt ảnh cũkhông cầnkhôi phụcLauncher chính thức
            shouldRestoreOnDestroy = false;
        } else {
            Log.d(TAG, String.format("▶️ 开始播放动画[%s]", type));
        }
        
        // cài đặtmớihoạt ảnhlàhiện tạihoạt ảnh
        currentAnimation = type;
        shouldRestoreOnDestroy = true;  // mớihoạt ảnhmặc địnhcầnkhôi phục
        
        return oldAnimation;  // trả vềhoạt ảnh cũ bị ngắt
    }
    
    /**
 * V3.5: đánh dấuhoạt ảnh sạc bị ngắtlàchế độ giữ sáng
 */
    public static synchronized void markInterruptedChargingAsAlwaysOn(boolean alwaysOn) {
        interruptedChargingWasAlwaysOn = alwaysOn;
        Log.d(TAG, "🔖 被打断的充电动画常亮标记: " + alwaysOn);
    }
    
    /**
 * V3.5: kiểm tra hoạt ảnh sạc bị ngắt có cần khôi phục
 */
    public static synchronized boolean shouldResumeChargingAnimation() {
        return interruptedChargingWasAlwaysOn;
    }
    
    /**
 * V3.5: xóahoạt ảnh sạc giữ sángđánh dấu
 */
    public static synchronized void clearChargingAlwaysOnFlag() {
        interruptedChargingWasAlwaysOn = false;
    }
    
    /**
 * kết thúchoạt ảnh
 * @param type hoạt ảnhloại
 * @return có cần khôi phụcLauncher chính thức
 */
    public static synchronized boolean endAnimation(AnimationType type) {
        if (currentAnimation != type) {
            Log.w(TAG, String.format("⚠️ 尝试结束动画[%s]，但当前动画是[%s]", type, currentAnimation));
            return false;  // khônglàhiện tạihoạt ảnh, không cầnkhôi phục
        }
        
        boolean shouldRestore = shouldRestoreOnDestroy;
        
        if (shouldRestore) {
            Log.d(TAG, String.format("⏹️ 动画[%s]正常结束，需要恢复官方Launcher", type));
        } else {
            Log.d(TAG, String.format("⏹️ 动画[%s]被打断结束，不需要恢复官方Launcher", type));
        }
        
        currentAnimation = AnimationType.NONE;
        shouldRestoreOnDestroy = true;
        
        return shouldRestore;
    }
    
    /**
 * kiểm tracócóhoạt ảnhđangphát
 */
    public static synchronized boolean isAnimationPlaying() {
        return currentAnimation != AnimationType.NONE;
    }
    
    /**
 * lấy hiện tạihoạt ảnhloại
 */
    public static synchronized AnimationType getCurrentAnimation() {
        return currentAnimation;
    }
    
    /**
 * ngắtchắc chắnloạihoạt ảnh
 */
    private static void interruptAnimation(AnimationType type) {
        android.content.Intent intent;
        String action;
        
        switch (type) {
            case CHARGING:
                action = "com.tgwgroup.MiRearScreenSwitcher.INTERRUPT_CHARGING_ANIMATION";
                break;
            case NOTIFICATION:
                action = "com.tgwgroup.MiRearScreenSwitcher.INTERRUPT_NOTIFICATION_ANIMATION";
                break;
            default:
                return;
        }
        
        try {
            // quastatictrêndướivăngửibroadcast（cầntừServicelấy）
            // nàythời gianngười dùnglogđánh dấu, thực tếgửigọicáchxử lý
            Log.d(TAG, String.format("🔔 准备发送打断广播: %s", action));
        } catch (Exception e) {
            Log.e(TAG, "Failed to interrupt animation", e);
        }
    }
    
    /**
 * gửi broadcast ngắt（Servicegọi）
 */
    public static void sendInterruptBroadcast(android.content.Context context, AnimationType type) {
        String action;
        
        switch (type) {
            case CHARGING:
                action = "com.tgwgroup.MiRearScreenSwitcher.INTERRUPT_CHARGING_ANIMATION";
                break;
            case NOTIFICATION:
                action = "com.tgwgroup.MiRearScreenSwitcher.INTERRUPT_NOTIFICATION_ANIMATION";
                break;
            default:
                return;
        }
        
        try {
            android.content.Intent intent = new android.content.Intent(action);
            intent.setPackage(context.getPackageName());
            context.sendBroadcast(intent);
            Log.d(TAG, String.format("✓ 已发送打断广播: %s", action));
        } catch (Exception e) {
            Log.e(TAG, "Failed to send interrupt broadcast", e);
        }
    }
}

