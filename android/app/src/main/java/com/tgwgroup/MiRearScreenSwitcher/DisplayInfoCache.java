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

import android.graphics.Rect;
import android.util.Log;

/**
 * cache thông tin màn hình
 * khi ứng dụng khởi động lấy một lần, sau đó trực tiếp sử dụng dữ liệu cache
 */
public class DisplayInfoCache {
    private static final String TAG = "DisplayInfoCache";
    
    // singleton
    private static volatile DisplayInfoCache instance;
    
    // cache thông tin màn hình sau
    private RearDisplayHelper.RearDisplayInfo cachedInfo;
    private boolean initialized = false;
    
    private DisplayInfoCache() {}
    
    public static DisplayInfoCache getInstance() {
        if (instance == null) {
            synchronized (DisplayInfoCache.class) {
                if (instance == null) {
                    instance = new DisplayInfoCache();
                }
            }
        }
        return instance;
    }
    
    /**
 * khởi tạo cache (khi ứng dụng khởi động gọi một lần)
 */
    public synchronized void initialize(ITaskService taskService) {
        if (initialized) {
            Log.d(TAG, "ℹ️ 已初始化，跳过");
            return;
        }
        
        try {
            Log.d(TAG, "🔄 开始获取背屏信息...");
            cachedInfo = RearDisplayHelper.getRearDisplayInfo(taskService);
            initialized = true;
            
            Log.d(TAG, String.format("✅ 背屏信息已缓存: %dx%d, DPI=%d, Cutout=%s",
                cachedInfo.width, cachedInfo.height, cachedInfo.densityDpi,
                cachedInfo.hasCutout() ? cachedInfo.cutout.toString() : "无"));
                
        } catch (Exception e) {
            Log.e(TAG, "❌ 初始化失败", e);
            // đặt giá trị mặc định
            cachedInfo = new RearDisplayHelper.RearDisplayInfo();
            cachedInfo.width = 904;
            cachedInfo.height = 572;
            cachedInfo.densityDpi = 450;
            cachedInfo.cutout = new Rect(0, 0, 0, 0);
            initialized = true;
            Log.w(TAG, "⚠️ 使用默认背屏信息");
        }
    }
    
    /**
 * lấycache thông tin màn hình sau
 */
    public RearDisplayHelper.RearDisplayInfo getCachedInfo() {
        if (!initialized) {
            Log.w(TAG, "⚠️ 缓存未初始化，返回默认值");
            RearDisplayHelper.RearDisplayInfo defaultInfo = new RearDisplayHelper.RearDisplayInfo();
            defaultInfo.width = 904;
            defaultInfo.height = 572;
            defaultInfo.densityDpi = 450;
            defaultInfo.cutout = new Rect(0, 0, 0, 0);
            return defaultInfo;
        }
        return cachedInfo;
    }
    
    /**
 * làm lạilấy（dùng cholàm mớicache）
 */
    public synchronized void refresh(ITaskService taskService) {
        initialized = false;
        initialize(taskService);
    }
    
    /**
 * kiểm tracóđãkhởi tạo
 */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
 * xóacache（dùng chođothửhoặcreset）
 */
    public synchronized void clear() {
        cachedInfo = null;
        initialized = false;
        Log.d(TAG, "🗑️ 缓存已清除");
    }
}

