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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * màn hình sauhiển thịthông tinloại
 * qua dumpsys display lấymàn hình sauđộ phân giải、DPI、Cutoutthông tin
 */
public class RearDisplayHelper {
    private static final String TAG = "RearDisplayHelper";
    
    /**
 * thông tin màn hình saudữ liệuloại
 */
    public static class RearDisplayInfo {
        public int width;           // màn hìnhđộ rộng（pixel）
        public int height;          // màn hìnhđộ cao（pixel）
        public int densityDpi;      // DPI
        public Rect cutout;         // Cutoutphân vùng（insetsthức） 
        public RearDisplayInfo() {
            // giá trị mặc định（nhỏ14 Ultramàn hình sau） width = 1200;
            height = 2200;
            densityDpi = 440;
            cutout = new Rect(0, 0, 0, 0);
        }
        
        @Override
        public String toString() {
            return String.format("RearDisplayInfo{width=%d, height=%d, dpi=%d, cutout=%s}",
                width, height, densityDpi, cutout.toString());
        }
        
        /**
 * kiểm tracócócutout
 */
        public boolean hasCutout() {
            return cutout.left > 0 || cutout.top > 0 || cutout.right > 0 || cutout.bottom > 0;
        }
    }
    
    /**
 * lấythông tin màn hình sau（qua TaskService）
 */
    public static RearDisplayInfo getRearDisplayInfo(ITaskService taskService) {
        RearDisplayInfo info = new RearDisplayInfo();
        
        if (taskService == null) {
            Log.w(TAG, "⚠️ TaskService为null，使用默认背屏信息");
            return info;
        }
        
        try {
            // dumpsys display lệnh
            String result = taskService.executeShellCommandWithResult("dumpsys display");
            if (result == null || result.isEmpty()) {
                Log.w(TAG, "⚠️ dumpsys display返回为空，使用默认背屏信息");
                return info;
            }
            
            // 🔍 log：xuấthoàn toàndumpsys displaykết quả（trước2000chữ）
            String preview = result.length() > 2000 ? result.substring(0, 2000) : result;
            Log.d(TAG, "📋 dumpsys display 完整输出（前2000字符）：\n" + preview);
            Log.d(TAG, "📏 dumpsys display 总长度: " + result.length() + " 字符");
            
            // parsethông tin màn hình sau（Display 1）
            parseRearDisplayInfo(result, info);
            
            Log.d(TAG, "✓ 背屏信息: " + info.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 获取背屏信息失败，使用默认值", e);
        }
        
        return info;
    }
    
    /**
 * parse dumpsys display xuất
 */
    private static void parseRearDisplayInfo(String dumpsys, RearDisplayInfo info) {
        try {
            // phương thức1: từ mViewports trongparse（tối ưuchắc chắn）
            Pattern viewportPattern = Pattern.compile(
                "displayId=1[^}]*deviceWidth=(\\d+),\\s*deviceHeight=(\\d+)"
            );
            Matcher viewportMatcher = viewportPattern.matcher(dumpsys);
            if (viewportMatcher.find()) {
                info.width = Integer.parseInt(viewportMatcher.group(1));
                info.height = Integer.parseInt(viewportMatcher.group(2));
                Log.d(TAG, String.format("✓ 从mViewports解析分辨率: %dx%d", info.width, info.height));
            }
            
            // phương thức2: traDisplay 1DisplayDeviceInfophân vùng（góicutout）
            // tìm kiếm uniqueId="local:4630946949513469332" (Display 1mộtđánh dấu)
            // hoặctìm kiếmgói "904 x 572" DisplayDeviceInfo
            int display1DeviceStart = -1;
            
            // trướcthửđếngói displayId=1 DisplayViewport lấy uniqueId
            Pattern uniqueIdPattern = Pattern.compile("displayId=1[^}]*uniqueId='([^']+)'");
            Matcher uniqueIdMatcher = uniqueIdPattern.matcher(dumpsys);
            String display1UniqueId = null;
            if (uniqueIdMatcher.find()) {
                display1UniqueId = uniqueIdMatcher.group(1);
                Log.d(TAG, "🔍 Display 1 uniqueId: " + display1UniqueId);
            }
            
            // người dùnguniqueIdhoặcđộ phân giảichắc chắnDisplay 1DisplayDeviceInfo
            int searchPos = 0;
            while (true) {
                int idx = dumpsys.indexOf("DisplayDeviceInfo", searchPos);
                if (idx == -1) break;
                
                // kiểm tratiếpdưới2000chữtrongcócóphân phốiphần
                int checkEnd = Math.min(idx + 2000, dumpsys.length());
                String snippet = dumpsys.substring(idx, checkEnd);
                
                boolean isDisplay1 = false;
                if (display1UniqueId != null && snippet.contains(display1UniqueId)) {
                    isDisplay1 = true;
                } else if (snippet.contains(info.width + " x " + info.height)) {
                    // người dùngđãparseđộ phân giảiphân phối（904 x 572）
                    isDisplay1 = true;
                }
                
                if (isDisplay1) {
                    display1DeviceStart = idx;
                    break;
                }
                searchPos = idx + 17; // "DisplayDeviceInfo".length()
            }
            
            String display1Block = "";
            if (display1DeviceStart != -1) {
                // đếndướimộtcái "DisplayDeviceInfo" làmlàkết thúc
                int nextBlockIdx = dumpsys.indexOf("DisplayDeviceInfo", display1DeviceStart + 17);
                
                display1Block = nextBlockIdx > 0 
                    ? dumpsys.substring(display1DeviceStart, nextBlockIdx)
                    : dumpsys.substring(display1DeviceStart, Math.min(display1DeviceStart + 3000, dumpsys.length()));
                
                Log.d(TAG, "🔍 Display 1 DisplayDeviceInfo区块长度: " + display1Block.length() + " 字符");
                
                // xuấttrước600chữdùng chodebug
                String preview = display1Block.length() > 600 
                    ? display1Block.substring(0, 600) 
                    : display1Block;
                Log.d(TAG, "📋 Display 1 DisplayDeviceInfo区块（前600字符）：\n" + preview);
            } else {
                Log.w(TAG, "⚠️ 未找到Display 1的DisplayDeviceInfo区块");
                display1Block = ""; // khôngquaylùiđếntoànvăn, tránhsaiphân phốimàn hình chínhdữ liệu
            }
            
            // parseDPI（từDisplayDeviceInfophân vùng）
            // thức: density 450
            if (!display1Block.isEmpty()) {
                Pattern dpiPattern = Pattern.compile("density\\s+(\\d+)");
                Matcher dpiMatcher = dpiPattern.matcher(display1Block);
                if (dpiMatcher.find()) {
                    info.densityDpi = Integer.parseInt(dpiMatcher.group(1));
                    Log.d(TAG, "✓ 解析DPI: " + info.densityDpi);
                }
            }
            
            // parseCutout（MIUIthức）
            // thức: DisplayCutout{insets=Rect(296, 0 - 0, 0)
            // chú ý：MIUIngười dùng "top - right" màkhônglà "top, right"
            info.cutout = parseCutoutFromDumpsys(display1Block);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 解析背屏信息异常", e);
        }
    }
    
    /**
 * parseCutoutthông tin（MIUIthức）
 */
    private static Rect parseCutoutFromDumpsys(String display1Block) {
        Rect cutout = new Rect(0, 0, 0, 0);
        
        try {
            // 🔍 trasởcógói "Cutout" hoặc "cutout" 
            String[] lines = display1Block.split("\n");
            StringBuilder cutoutLines = new StringBuilder("📋 所有Cutout相关行：\n");
            boolean foundCutout = false;
            for (String line : lines) {
                if (line.toLowerCase().contains("cutout")) {
                    cutoutLines.append("  ").append(line.trim()).append("\n");
                    foundCutout = true;
                }
            }
            if (foundCutout) {
                Log.d(TAG, cutoutLines.toString());
            } else {
                Log.d(TAG, "ℹ️ Display 1区块中未找到任何包含'Cutout'的行");
            }
            
            // MIUIthức: Rect(296, 0 - 0, 0)
            // đánh dấuthức: Rect(left, top, right, bottom)
            // MIUIthức: Rect(left, top - right, bottom)
            
            // trướcthửMIUIthức（cóngắntuyến）
            Pattern miuiPattern = Pattern.compile("DisplayCutout\\{insets=Rect\\((\\d+),\\s*(\\d+)\\s*-\\s*(\\d+),\\s*(\\d+)\\)");
            Matcher miuiMatcher = miuiPattern.matcher(display1Block);
            
            if (miuiMatcher.find()) {
                cutout.left = Integer.parseInt(miuiMatcher.group(1));
                cutout.top = Integer.parseInt(miuiMatcher.group(2));
                cutout.right = Integer.parseInt(miuiMatcher.group(3));
                cutout.bottom = Integer.parseInt(miuiMatcher.group(4));
                Log.d(TAG, String.format("✓ 解析Cutout(MIUI格式): left=%d, top=%d, right=%d, bottom=%d",
                    cutout.left, cutout.top, cutout.right, cutout.bottom));
                return cutout;
            }
            
            // lạithửđánh dấuthức（không cóngắntuyến）
            Pattern standardPattern = Pattern.compile("DisplayCutout\\{insets=Rect\\((\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\)");
            Matcher standardMatcher = standardPattern.matcher(display1Block);
            
            if (standardMatcher.find()) {
                cutout.left = Integer.parseInt(standardMatcher.group(1));
                cutout.top = Integer.parseInt(standardMatcher.group(2));
                cutout.right = Integer.parseInt(standardMatcher.group(3));
                cutout.bottom = Integer.parseInt(standardMatcher.group(4));
                Log.d(TAG, String.format("✓ 解析Cutout(标准格式): left=%d, top=%d, right=%d, bottom=%d",
                    cutout.left, cutout.top, cutout.right, cutout.bottom));
                return cutout;
            }
            
            // thửhơnrộngchế độ（góiRectCutout）
            Pattern loosePattern = Pattern.compile("cutout.*?Rect\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
            Matcher looseMatcher = loosePattern.matcher(display1Block);
            if (looseMatcher.find()) {
                String rectContent = looseMatcher.group(1);
                Log.d(TAG, "🔍 找到Cutout但格式未识别，Rect内容: " + rectContent);
            }
            
            Log.d(TAG, "ℹ️ 未找到可识别的Cutout信息，使用默认值(0,0,0,0)");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 解析Cutout异常", e);
        }
        
        return cutout;
    }
}

