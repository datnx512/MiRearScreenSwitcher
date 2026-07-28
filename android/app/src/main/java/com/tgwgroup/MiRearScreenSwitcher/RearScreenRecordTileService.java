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

import android.content.Intent;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

/**
 * Quick Settings Tile - màn hình sauquay màn hình
 * nhấpsau hiển thị/ẩnquay màn hìnhoverlay window
 */
public class RearScreenRecordTileService extends TileService {
    private static final String TAG = "RearScreenRecordTile";
    
    @Override
    public void onStartListening() {
        super.onStartListening();
        
        Tile tile = getQsTile();
        if (tile != null) {
            // kiểm traoverlay windowcóđanghiển thị
            boolean isRecording = ScreenRecordService.isRunning();
            tile.setState(isRecording ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }
    
    @Override
    public void onClick() {
        super.onClick();
        
        unlockAndRun(() -> {
            // kiểm traoverlay windowquyền
            if (!Settings.canDrawOverlays(this)) {
                Log.w(TAG, "无悬浮窗权限");
                
                Toast.makeText(this, getString(R.string.toast_grant_overlay_permission), Toast.LENGTH_LONG).show();
                
                // chuyển đếnquyềncài đặttrang
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                
                return;
            }
            
            // kiểm tracóđã ở
            if (ScreenRecordService.isRunning()) {
                // đãcóoverlay window, thu gọnoverlay window（dừngdịch vụ）
                stopService(new Intent(this, ScreenRecordService.class));
                Log.d(TAG, "✓ 录屏悬浮窗已关闭");
                
                // cập nhậtTiletrạng thái
                Tile tile = getQsTile();
                if (tile != null) {
                    tile.setState(Tile.STATE_INACTIVE);
                    tile.updateTile();
                }
            } else {
                // khởi độngquay màn hìnhdịch vụ（hiển thịoverlay window）
                Intent intent = new Intent(this, ScreenRecordService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
                
                Log.d(TAG, "✓ 录屏悬浮窗已启动");
                
                // cập nhậtTiletrạng thái
                Tile tile = getQsTile();
                if (tile != null) {
                    tile.setState(Tile.STATE_ACTIVE);
                    tile.updateTile();
                }
            }
        });
    }
}






