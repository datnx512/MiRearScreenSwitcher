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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * V2.6: URInhậnActivity
 * trong suốt hoàn toàn, chỉchuyểnphátURIđếnUriCommandService, sau đóngayfinish
 * khôngsẽhiển thịUI, tránhbỏ quađếnMRSStrang
 */
public class UriReceiverActivity extends Activity {
    private static final String TAG = "UriReceiverActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // khôngcài đặtbố cục, giữtrong suốt
        
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null && "mrss".equals(uri.getScheme())) {
                Log.d(TAG, "🔗 URI接收: " + uri.toString());
                
                // chuyểnphátđếnUriCommandServicexử lý
                Intent serviceIntent = new Intent(this, UriCommandService.class);
                serviceIntent.setData(uri);
                startService(serviceIntent);
                
                Log.d(TAG, "✓ 已转发到UriCommandService");
            }
        }
        
        // ngayfinish, không hiển thịUI
        finish();
    }
    
    @Override
    public void finish() {
        super.finish();
        // tắtchuyểnhoạt ảnh, trong suốt hoàn toàn
        overridePendingTransition(0, 0);
    }
}

