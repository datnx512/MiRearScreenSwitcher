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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Color;
import android.graphics.BlurMaskFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import androidx.core.graphics.PathParser;

/**
 * tia séttrạng tháichất lỏngsạcđồ
 * từbộ phậnvềtrênsạcchất lỏng, trọng lựccảmứng
 */
public class LightningShapeView extends View implements SensorEventListener {
    private Paint liquidPaint;      // chất lỏngbút vẽ
    private Paint liquidShinePaint; // chất lỏngbút vẽ
    private Paint bubblePaint;      // bong bóngbút vẽ
    private Paint outlinePaint;     // khungbút vẽ
    private Paint glassHighlightPaint;  // caobút vẽ
    private Paint glassReflectionPaint; // bút vẽ
    private Paint innerGlowPaint;   // trongbộ phậnphátbút vẽ
    private Paint glassDepthPaint;  // sâubút vẽ
    private Path lightningPath;     // tia séttrạng tháiđường dẫn
    private Path highlightPath;     // caođường dẫn（tráitrên）
    private Path wavePath;          // mặt chất lỏngsóngđường dẫn
    private float fillLevel = 0f;   // sạcví dụ 0.0 - 1.0
    private float waveOffset = 0f;  // sónghoạt ảnhchuyển
    private float tiltX = 0f;       // X（trọng lựccảmứng）
    private float tiltY = 0f;       // Y（trọng lựccảmứng）
    private float[] bubblePositions = new float[6]; // bong bóngYcấu hình（trọng lựcảnh hưởng）
    private SensorManager sensorManager;
    private Sensor accelerometer;
    
    // V3.5: toàn màn hìnhchất lỏngchế độ（khôngvẽtia sétkhung）
    private boolean fullScreenMode = false;
    
    // V3.5: phụcngười dùngđốitránhGC（tínhcó thểtối ưu）
    private Path fullScreenLiquidPath = new Path();
    private Path fullScreenWavePath = new Path();  // phụcngười dùngsóngđường dẫn
    private Paint fullScreenShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint fullScreenBottomShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);  // phụcngười dùngbộ phậnbút vẽ
    private Paint fullScreenWavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);  // phụcngười dùngsóngbút vẽ
    private Paint fullScreenEdgeShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);  // phụcngười dùngbút vẽ
    private int lastShadowHeight = -1;  // cachetrênlầnđộ cao, tránh lặp lạitạoshader
    private int lastBottomShadowHeight = -1;  // cachebộ phậnđộ cao
    private int lastEdgeShineWidth = -1;  // cacheđộ rộng
    private Paint bubbleHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);  // phụcngười dùngbong bóngcaobút vẽ
    
    // V3.5: sóngtínhtối ưu（tính, tránhkhung hìnhsintính）
    private float[] wavePoints = new float[200];  // tínhsóng
    private int lastWaveWidth = -1;  // cachesóngđộ rộng
    private float lastWaveOffset = -1f;  // cachesóngchuyển
    
    // V3.14: khôi phụcsóngtính, giữchảy
    private static final float WAVE_UPDATE_THRESHOLD = 0.01f;  // sóngcập nhậtgiá trị（）
    private float lastProcessedWaveOffset = -1f;  // trênlầnxử lýsóngchuyển
    
    public LightningShapeView(Context context) {
        super(context);
        init();
    }
    
    public LightningShapeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        // khởi tạotrọng lựccảm biến
        try {
            sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }
        } catch (Exception e) {
            Log.w("LightningShapeView", "重力传感器初始化失败", e);
        }
        
        // bậtphầnthêmđồloại
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // chất lỏngbút vẽ（dải màu）
        liquidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        liquidPaint.setStyle(Paint.Style.FILL);
        liquidPaint.setDither(true); // động, hơndải màu
        
        // chất lỏngbút vẽ（chất lỏngmặt）
        liquidShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        liquidShinePaint.setStyle(Paint.Style.FILL);
        
        // bong bóngbút vẽ（chất lỏngtrongbong bóng）
        bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubblePaint.setStyle(Paint.Style.FILL);
        bubblePaint.setColor(0x80FFFFFF);  // thêmtrong suốt, cho bong bónghơnsánghiển
        bubblePaint.setMaskFilter(new BlurMaskFilter(2f, BlurMaskFilter.Blur.NORMAL)); // ít, cho bong bónghơntrong
        
        // V3.5: bong bóngcaobút vẽ（trướckhởi tạo, tránhkhung hìnhtạo）
        bubbleHighlightPaint.setColor(0xB0FFFFFF);
        
        // chínhkhungbút vẽ（trong suốt）
        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(6f);
        outlinePaint.setColor(0x80FFFFFF);
        
        // caobút vẽ（tráitrênsángsáng）
        glassHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassHighlightPaint.setStyle(Paint.Style.STROKE);
        glassHighlightPaint.setStrokeWidth(4f);
        glassHighlightPaint.setColor(0xF0FFFFFF); // giữ sáng
        glassHighlightPaint.setMaskFilter(new BlurMaskFilter(2f, BlurMaskFilter.Blur.OUTER));
        
        // bút vẽ（ngoàibộ phận）
        glassReflectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassReflectionPaint.setStyle(Paint.Style.STROKE);
        glassReflectionPaint.setStrokeWidth(12f);
        glassReflectionPaint.setColor(0x50FFFFFF);
        glassReflectionPaint.setMaskFilter(new BlurMaskFilter(6f, BlurMaskFilter.Blur.OUTER));
        
        // sâubút vẽ（trongbộ phận, lậpcơ thểcảm）
        glassDepthPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassDepthPaint.setStyle(Paint.Style.STROKE);
        glassDepthPaint.setStrokeWidth(8f);
        glassDepthPaint.setColor(0x40000000);
        glassDepthPaint.setMaskFilter(new BlurMaskFilter(4f, BlurMaskFilter.Blur.INNER));
        
        // trongbộ phậnphátbút vẽ（chất lỏngtuần）
        innerGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerGlowPaint.setStyle(Paint.Style.STROKE);
        innerGlowPaint.setStrokeWidth(2f);
        innerGlowPaint.setColor(0x60FFFFFF);
        
        // tạođường dẫn
        lightningPath = new Path();
        highlightPath = new Path();
        wavePath = new Path();
        
        // khởi tạobong bóngcấu hình（đơngiải pháp）
        for (int i = 0; i < bubblePositions.length; i++) {
            bubblePositions[i] = (float) Math.random();
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // sử dụngAndroidPathParserparseSVGđường dẫn
        // nguyênbắt đầuSVG path data（từlightening.xml）
        String pathData = "M511.616,85.333 c-27.947,0 -54.059,14.08 -69.717,37.547 l-256.811,385.707 " +
                         "a86.187,86.187 0,0,0 22.613,118.571 l6.101,3.84 " +
                         "c12.501,7.04 26.624,10.795 41.003,10.795 h172.544 " +
                         "v211.499 c0,47.147 37.675,85.376 84.139,85.376 " +
                         "c27.861,0 53.888,-13.952 69.547,-37.291 l257.707,-383.829 " +
                         "a86.187,86.187 0,0,0 -22.187,-118.613 l-6.144,-3.883 " +
                         "a83.2,83.2 0,0,0 -41.216,-10.965 h-173.44 " +
                         "v-213.333 C595.755,123.52 558.08,85.333 511.616,85.333 z";
        
        try {
            // sử dụngAndroidXPathParserparseSVGđường dẫn
            lightningPath = PathParser.createPathFromPathData(pathData);
            
            // đường dẫnbằngứngđồlớnnhỏ（nguyênbắt đầuviewBoxlà1024x1024）
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.setScale(w / 1024f, h / 1024f);
            lightningPath.transform(matrix);
            
        } catch (Exception e) {
            Log.e("LightningShapeView", "解析SVG路径失败，使用简化闪电形状", e);
            
            // quaylùi：sử dụnghóatia séttrạng thái
            lightningPath.reset();
            float centerX = w / 2f;
            
            lightningPath.moveTo(centerX, h * 0.08f);
            lightningPath.lineTo(centerX - w * 0.18f, h * 0.5f);
            lightningPath.lineTo(centerX + w * 0.05f, h * 0.52f);
            lightningPath.lineTo(centerX - w * 0.08f, h * 0.92f);
            lightningPath.lineTo(centerX + w * 0.12f, h * 0.58f);
            lightningPath.lineTo(centerX + w * 0.18f, h * 0.56f);
            lightningPath.close();
        }
        
        // sử dụnghệ thốngđiện（#34C759）, dải màu, sử dụngmàu thuần
        liquidPaint.setShader(null);  // gỡ bỏdải màu
        liquidPaint.setColor(0xFF34C759);  // hệ thốngđiện
        
        // tạotráitrêncaođường dẫn（）
        highlightPath.reset();
        highlightPath.moveTo(w * 0.2f, h * 0.1f);
        highlightPath.lineTo(w * 0.35f, h * 0.15f);
        highlightPath.lineTo(w * 0.3f, h * 0.35f);
        highlightPath.lineTo(w * 0.15f, h * 0.3f);
        highlightPath.close();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        long drawStartTime = System.nanoTime();  // tínhcó thểbắt đầu
        
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        
        // V3.5: toàn màn hìnhchất lỏngchế độ - trực tiếpvẽchất lỏng, khônglàtia séttrạng thái
        if (fullScreenMode) {
            drawFullScreenLiquid(canvas, width, height);
            
            // V3.5: tínhcó thể（sửabug + thêmkhung hìnhkhoảng）
            long drawEndTime = System.nanoTime();
            long drawTimeNanos = drawEndTime - drawStartTime;
            totalDrawTime += drawTimeNanos;
            
            // tínhkhung hìnhkhoảng
            if (lastFrameTimeNanos > 0) {
                long frameInterval = drawStartTime - lastFrameTimeNanos;
                totalFrameInterval += frameInterval;
            }
            lastFrameTimeNanos = drawStartTime;
            frameCount++;
            
            // 60khung hìnhxuấtmột lầnthốngtính
            if (frameCount % 60 == 0) {
                float avgDrawTimeMs = (totalDrawTime / (float)frameCount) / 1_000_000f;  // giây→giây
                float currentDrawMs = drawTimeNanos / 1_000_000f;
                float avgFrameIntervalMs = (totalFrameInterval / (float)(frameCount - 1)) / 1_000_000f;  // khung hìnhkhoảng
                
                long currentTime = System.currentTimeMillis();
                long timeSinceLastLog = currentTime - lastFrameTime;
                float actualFps = (timeSinceLastLog > 0) ? (60000f / timeSinceLastLog) : 0;
                
                // tínhlýtối ưulớnkhung hìnhkhoảng（vẽthời gian）
                float drawTimeMs = currentDrawMs;
                float maxTheoreticalFps = (drawTimeMs > 0) ? (1000f / drawTimeMs) : 999;
                float vsyncFps = (avgFrameIntervalMs > 0) ? (1000f / avgFrameIntervalMs) : 0;
                
                Log.d("LightningPerf", String.format("📊 性能: FPS=%.1f, VSync=%.1fHz (间隔%.2fms), 平均绘制=%.2fms", 
                    actualFps, vsyncFps, avgFrameIntervalMs, avgDrawTimeMs));
                
                lastFrameTime = currentTime;
                totalDrawTime = 0;
                totalFrameInterval = 0;
                frameCount = 0;
                lastFrameTimeNanos = 0;
            }
            
            return;
        }
        
        // nguyêncótia sétcontainerchế độ
        // ứng dụngtrọng lực（kết quả, thậtthựcchất lỏng）
        canvas.save();
        canvas.translate(tiltX * 5, tiltY * 3);
        
        // 0：vẽsâu（trongbộ phậncảm）
        canvas.save();
        canvas.translate(2, 2);
        canvas.drawPath(lightningPath, glassDepthPaint);
        canvas.restore();
        
        // 1：vẽngoàibộ phậnvà（tối ưungoài）
        canvas.save();
        canvas.translate(4, 4);
        canvas.drawPath(lightningPath, glassReflectionPaint);
        canvas.restore();
        
        // 2：lưucanvasvàlàtia séttrạng thái
        canvas.save();
        canvas.clipPath(lightningPath);
        
        // vẽchất lỏngsạc（từbộ phậnvềtrên）
        if (fillLevel > 0) {
            float fillHeight = height * fillLevel;
            
            // 2.1 vẽchínhchất lỏng（dải màu）
            canvas.drawRect(0, height - fillHeight, width, height, liquidPaint);
            
            // 2.2 vẽchất lỏngbộ phậnsâu（phụcngười dùngPaint, độ caothay đổithời giannặngshader）
            if (lastBottomShadowHeight != height) {
                fullScreenBottomShadowPaint.setShader(new LinearGradient(
                    0, height - 30, 0, height,
                    new int[]{0x00000000, 0x40000000, 0x50000000},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
                ));
                lastBottomShadowHeight = height;
            }
            canvas.drawRect(0, height - 30, width, height, fullScreenBottomShadowPaint);
            
            // 2.3 vẽmặt chất lỏngsóng（tối ưu：íttính）
            if (fillHeight > 20) {
                float waveY = height - fillHeight;
                fullScreenWavePath.reset();
                
                // V3.17: nhỏtia sétchế độtrọng lực, kết quả
                float leftTilt = tiltX * 6;  // tráilượng（nhỏ）
                float rightTilt = -tiltX * 6; // phảilượng（nhỏ）
                
                fullScreenWavePath.moveTo(0, waveY + leftTilt);
                
                // V3.6: thống nhấtsóngtính（tránh lặp lạitính）
                updateWavePoints(width, waveOffset);
                
                // sử dụngtínhsóng
                int pointCount = Math.min(width / 8, wavePoints.length);  // ítvẽsố
                for (int i = 0; i < pointCount; i++) {
                    float x = (float) i / (pointCount - 1) * width;
                    float wave = wavePoints[i];
                    float tilt = leftTilt + (rightTilt - leftTilt) * (x / (float)width);
                    fullScreenWavePath.lineTo(x, waveY + wave + tilt);
                }
                fullScreenWavePath.lineTo(width, height);
                fullScreenWavePath.lineTo(0, height);
                fullScreenWavePath.close();
                
                // vẽsóngchất lỏng（phụcngười dùngPaint, chỉcài đặtalpha）
                fullScreenWavePaint.set(liquidPaint);
                fullScreenWavePaint.setAlpha(220);
                canvas.drawPath(fullScreenWavePath, fullScreenWavePaint);
            }
            
            // 2.4 mặt chất lỏngđã gỡ（người dùngcầnchất lỏngbộ phận）
            // khônglạivẽcao, giữthuầnchất lỏngmàu sắc
            
            // V3.15: sửabong bónglấp lánh, khung hìnhđềuvẽ
            if (fillHeight > 10) {  // thấpphần, cho bong bóng ở độ cao chất lỏng thấp hơn khi cũng hiển thị
                drawBubbles(canvas, width, height, fillHeight);
            }
            
            // 2.6 vẽchất lỏngtráisángsáng（phụcngười dùngPaint, độ rộngthay đổithời giannặngshader）
            if (lastEdgeShineWidth != width) {
                fullScreenEdgeShinePaint.setStyle(Paint.Style.FILL);
                fullScreenEdgeShinePaint.setShader(new LinearGradient(
                    width * 0.08f, 0, width * 0.22f, 0,
                    new int[]{0x00FFFFFF, 0x30FFFFFF, 0x20FFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.3f, 0.7f, 1f},
                    Shader.TileMode.CLAMP
                ));
                lastEdgeShineWidth = width;
            }
            canvas.drawRect(width * 0.08f, height - fillHeight, 
                           width * 0.22f, height, fullScreenEdgeShinePaint);
            
            // 2.8 chất lỏngtrongbộ phậntuyếnkết quảđã gỡ
            // giữthuầnchất lỏngmàu sắc, khôngthêm
            
            // 2.9 chất lỏngvớigiaoxử lýđã gỡ
            // giữthuầnchất lỏngmàu sắc
        }
        
        // khôi phụccanvas（）
        canvas.restore();
        
        // 3：vẽchínhkhung
       canvas.drawPath(lightningPath, outlinePaint);
        
        // 4：tráitrêncao（）
        //canvas.save();
        //canvas.clipPath(lightningPath);
        //canvas.translate(-width * 0.05f, -height * 0.05f);
        //canvas.drawPath(lightningPath, glassHighlightPaint);
        //canvas.restore();
        
        // 5：phảidướivà（3Dkết quả）
        //canvas.save();
        //canvas.translate(width * 0.02f, height * 0.02f);
        //Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //shadowPaint.setStyle(Paint.Style.STROKE);
        //shadowPaint.setStrokeWidth(3f);
        //shadowPaint.setColor(0x30000000); // 19% trong suốtđen
        //canvas.drawPath(lightningPath, shadowPaint);
        //canvas.restore();
        
        // 6：trongbộ phậncao（tráitrên）
        canvas.save();
        canvas.clipPath(lightningPath);
        // vẽtráitrênnhỏmặtcao（phụcngười dùngPaint, tránhkhung hìnhtạo）
        if (lastEdgeShineWidth != width) {
            fullScreenEdgeShinePaint.setStyle(Paint.Style.FILL);
            fullScreenEdgeShinePaint.setShader(new android.graphics.RadialGradient(
                width * 0.25f, height * 0.2f, width * 0.3f,
                new int[]{0x50FFFFFF, 0x20FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
            ));
            lastEdgeShineWidth = width;
        }
        canvas.drawPath(highlightPath, fullScreenEdgeShinePaint);
        canvas.restore();
        
        // khôi phụctrọng lựcđổi
        canvas.restore();
    }
    
    /**
 * V3.6: thống nhấtsóngtính（tránh lặp lạitính）
 */
    private void updateWavePoints(int width, float waveOffset) {
        if (lastWaveWidth != width || Math.abs(lastProcessedWaveOffset - waveOffset) > WAVE_UPDATE_THRESHOLD) {
            // V3.14: khôi phụcsóngsố, giữchảy
            int pointCount = Math.min(width / 6, wavePoints.length);
            for (int i = 0; i < pointCount; i++) {
                float x = (float) i / (pointCount - 1) * width;
                wavePoints[i] = (float) Math.sin((x / (float)width * 4 * Math.PI) + waveOffset) * 8f;
            }
            lastWaveWidth = width;
            lastProcessedWaveOffset = waveOffset;
        }
    }
    
    /**
 * V3.7: vẽchất lỏngtrongbong bóng（khôi phụctrọng lựckết quả, nhưnggiữtínhcó thểtối ưu）
 */
    private void drawBubbles(Canvas canvas, int width, int height, float fillHeight) {
        float baseY = height - fillHeight;
        
        // V3.17: nhỏbong bóngtrọng lựcphản hồi, kết quả
        float gravityOffsetX = -tiltX * 5; // tayvềtrái, bong bóngvềphải（nhỏ）
        float gravityOffsetY = tiltY * 2;   // trướcsauảnh hưởng（nhỏ）
        
        // V3.15: thêmbong bóngsốlượng, sửalấp lánh
        // vẽbong bóng（đơngiải pháp）
        // bong bóng1（lớn）
        float bubble1X = width * 0.2f + gravityOffsetX;
        float bubble1Y = baseY + fillHeight * bubblePositions[0] + gravityOffsetY;
        canvas.drawCircle(bubble1X, bubble1Y, 6f, bubblePaint);
        
        // bong bóng2（trong）
        float bubble2X = width * 0.4f + gravityOffsetX * 0.8f;
        float bubble2Y = baseY + fillHeight * bubblePositions[1] + gravityOffsetY;
        canvas.drawCircle(bubble2X, bubble2Y, 4f, bubblePaint);
        
        // bong bóng3（nhỏ）
        float bubble3X = width * 0.6f + gravityOffsetX * 0.6f;
        float bubble3Y = baseY + fillHeight * bubblePositions[2] + gravityOffsetY;
        canvas.drawCircle(bubble3X, bubble3Y, 3f, bubblePaint);
        
        // bong bóng4（nhỏ）
        float bubble4X = width * 0.8f + gravityOffsetX * 0.9f;
        float bubble4Y = baseY + fillHeight * bubblePositions[3] + gravityOffsetY;
        canvas.drawCircle(bubble4X, bubble4Y, 3.5f, bubblePaint);
        
        // bong bóng5（trong）
        float bubble5X = width * 0.3f + gravityOffsetX * 0.7f;
        float bubble5Y = baseY + fillHeight * bubblePositions[4] + gravityOffsetY;
        canvas.drawCircle(bubble5X, bubble5Y, 4.5f, bubblePaint);
        
        // bong bóng6（nhỏ）
        float bubble6X = width * 0.7f + gravityOffsetX * 0.5f;
        float bubble6Y = baseY + fillHeight * bubblePositions[5] + gravityOffsetY;
        canvas.drawCircle(bubble6X, bubble6Y, 2.5f, bubblePaint);
        
        // đơnbong bóngtrênlênlogic
        for (int i = 0; i < bubblePositions.length; i++) {
            bubblePositions[i] -= 0.002f; // chắc chắntrênlên
            if (bubblePositions[i] < 0) {
                bubblePositions[i] = 1.0f; // từbộ phậnlàm lạibắt đầu
            }
        }
    }
    
    private long waveAnimationStartTime = 0;
    private android.view.Choreographer.FrameCallback frameCallback;
    
    // V3.5: tínhcó thể
    private long lastFrameTime = 0;
    private long frameCount = 0;
    private long totalDrawTime = 0;
    private long lastFrameTimeNanos = 0;  // trênmộtkhung hìnhgiâythời gian
    private long totalFrameInterval = 0;  // khung hìnhkhoảngvà
    
    /**
 * khởi độngsónghoạt ảnh（tối ưulà120fps）
 */
    private void startWaveAnimation() {
        // tránh lặp lạikhởi động
        if (frameCallback != null) {
            return;
        }
        
        // ghibắt đầuthời gian（sử dụngthực tếkhung hìnhthời gian）
        waveAnimationStartTime = 0;
        
        // tạoFrameCallback
        frameCallback = new android.view.Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (fillLevel > 0) {
                    // khởi tạobắt đầuthời gian
                    if (waveAnimationStartTime == 0) {
                        waveAnimationStartTime = frameTimeNanos;
                    }
                    
                    // V3.14: khôi phụcsóng, giữchảy
                    long elapsedNanos = frameTimeNanos - waveAnimationStartTime;
                    waveOffset = (float)((elapsedNanos / 1_000_000_000.0) * Math.PI * 1.5); // 0.67giâymộtcáituầnkỳ
                    
                    // yêu cầunặng（sử dụngpostInvalidateOnAnimationđảm bảovớivsyncđồng bộ）
                    postInvalidateOnAnimation();
                    
                    // V3.6: sửa - chỉ ởcầnthời giantiếp tụcdướimộtkhung hình, tránhkhông cógiới hạn
                    if (fillLevel > 0) {
                        android.view.Choreographer.getInstance().postFrameCallback(this);
                    }
                }
            }
        };
        
        // bắt đầukhung hìnhquaygọi
        android.view.Choreographer.getInstance().postFrameCallback(frameCallback);
        Log.d("LightningShapeView", "✓ 波浪动画已启动（Choreographer.FrameCallback，跟随屏幕刷新率）");
    }
    
    /**
 * cài đặtsạcví dụ
 * @param level 0.0 - 1.0
 */
    public void setFillLevel(float level) {
        this.fillLevel = Math.max(0f, Math.min(1f, level));
        
        // nếubắt đầusạc, khởi độngsónghoạt ảnh（khởi độngmột lần）
        if (level > 0.01f && frameCallback == null) {
            startWaveAnimation();
        }
        
        // nếusạclà0, dừnghoạt ảnh
        if (level <= 0 && frameCallback != null) {
            android.view.Choreographer.getInstance().removeFrameCallback(frameCallback);
            frameCallback = null;
        }
        
        // nặng
        invalidate();
        Log.d("LightningShapeView", "🔋 填充比例已更新: " + (level * 100) + "%");
    }
    
    /**
 * V3.5: cài đặttoàn màn hìnhchất lỏngchế độ
 */
    public void setFullScreenMode(boolean enabled) {
        this.fullScreenMode = enabled;
        
        // toàn màn hìnhchế độdướikhởi động ngaysónghoạt ảnh
        if (enabled && fillLevel > 0) {
            startWaveAnimation();
        }
        
        invalidate();
    }
    
    /**
 * V3.7: vẽtoàn màn hìnhchất lỏng（khôi phụcsóngkết quả, nhưnggiữtínhcó thểtối ưu）
 */
    private void drawFullScreenLiquid(Canvas canvas, int width, int height) {
        if (fillLevel <= 0) return;
        
        float fillHeight = height * fillLevel;
        
        // V3.17: nhỏtrọng lực, kết quả
        float leftTilt = tiltX * 8;  // tráilượng（nhỏ）
        float rightTilt = -tiltX * 8; // phảilượng（nhỏ）
        
        // 1. phụcngười dùngPathđối, tránhkhung hìnhtạomớiđối
        fullScreenLiquidPath.reset();
        
        // mặt chất lỏngsóng + trọng lực
        float waveY = height - fillHeight;
        fullScreenLiquidPath.moveTo(0, waveY + leftTilt);
        
        // V3.7: thống nhấtsóngtính（tránh lặp lạitính）
        updateWavePoints(width, waveOffset);
        
        // V3.14: khôi phụcsóngsố, giữchảy
        int pointCount = Math.min(width / 6, wavePoints.length);  // khôi phụcsóng
        for (int i = 0; i < pointCount; i++) {
            float x = (float) i / (pointCount - 1) * width;
            float wave = wavePoints[i];
            float tilt = leftTilt + (rightTilt - leftTilt) * (x / (float)width);
            fullScreenLiquidPath.lineTo(x, waveY + wave + tilt);
        }
        
        // kết nốiđếnphảidưới, lạiđếntráidưới, thànhđường dẫn
        fullScreenLiquidPath.lineTo(width, height);
        fullScreenLiquidPath.lineTo(0, height);
        fullScreenLiquidPath.close();
        
        // 2. vẽcơ thểchất lỏng
        canvas.drawPath(fullScreenLiquidPath, liquidPaint);
        
        // 3. vẽbộ phận（ởđộ caothay đổithời gianlàm lạitạoshader）
        if (lastShadowHeight != height) {
            fullScreenShadowPaint.setShader(new LinearGradient(
                0, height - 40, 0, height,
                new int[]{0x00000000, 0x20000000, 0x40000000},
                new float[]{0f, 0.7f, 1f},
                Shader.TileMode.CLAMP
            ));
            lastShadowHeight = height;
        }
        canvas.drawPath(fullScreenLiquidPath, fullScreenShadowPaint);
        
        // V3.15: sửabong bónglấp lánh, khung hìnhđềuvẽ
        if (fillHeight > 10) {  // thấpphần, cho bong bóng ởđộ cao chất lỏngđềucó thểhiển thị
            drawBubbles(canvas, width, height, fillHeight);
        }
    }
    
    /**
 * lấy hiện tạisạcví dụ
 */
    public float getFillLevel() {
        return fillLevel;
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // V3.5: đăng kýtrọng lựccảm biến（sử dụngUItrễ, thấpquaygọi）
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            Log.d("LightningShapeView", "✅ 重力传感器已注册（UI延迟）");
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // hủy đăng kýtrọng lựccảm biến
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            Log.d("LightningShapeView", "❌ 重力传感器已注销");
        }
        
        // V3.5: dừngChoreographerquaygọi
        if (frameCallback != null) {
            android.view.Choreographer.getInstance().removeFrameCallback(frameCallback);
            frameCallback = null;
        }
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // lấytrọng lựcthêm（XvàY）
            float x = event.values[0]; // tráiphải（-10 đến 10）
            float y = event.values[1]; // trướcsau（-10 đến 10）
            
            // V3.17: nhỏtrọng lựccảmứng, hơnkết quả
            float smoothFactor = 0.05f; // nhỏ
            tiltX = tiltX * (1 - smoothFactor) + x * smoothFactor;
            tiltY = tiltY * (1 - smoothFactor) + y * smoothFactor;
            
            // giới hạn（nhỏ）
            tiltX = Math.max(-2f, Math.min(2f, tiltX));
            tiltY = Math.max(-2f, Math.min(2f, tiltY));
            
            // V3.5: khôngởnàyinvalidate(), Choreographerthống nhấtđộnglàm mới, tránhquá trìnhvẽ
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // không cầnxử lýthay đổi
    }
}

