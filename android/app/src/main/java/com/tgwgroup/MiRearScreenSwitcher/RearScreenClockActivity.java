/*
 * RearScreenClockActivity
 * Displays a large digital clock on the rear screen as an overlay activity.
 * Shows current time (HH:MM), date, and battery percentage with a brand
 * gradient background (coral -> pink -> purple -> blue).
 *
 * Launchable via MethodChannel case "showRearClock" from Flutter.
 */

package com.tgwgroup.MiRearScreenSwitcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RearScreenClockActivity extends Activity {
    private static final String TAG = "RearScreenClock";

    private Handler handler;
    private Runnable clockRunnable;
    private TextView timeText;
    private TextView dateText;
    private TextView batteryText;
    private View rootView;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra("level", 0);
            int scale = intent.getIntExtra("scale", 100);
            int percent = (scale > 0) ? (level * 100) / scale : 0;
            if (batteryText != null) {
                batteryText.setText(percent + "%");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen overlay + keep screen on
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        try {
            getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } catch (Exception e) {
            // Some devices/versions reject setting overlay type directly; ignore.
        }

        // Root: FrameLayout so the close button can float in the corner.
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.TRANSPARENT);

        // Brand gradient background (coral -> pink -> purple -> blue)
        int[] colors = {0xFFFF9D88, 0xFFFFB5C5, 0xFFE0B5DC, 0xFFA8C5E5};
        GradientDrawable gradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            colors
        );
        root.setBackground(gradient);

        // Centered content column
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        content.setLayoutParams(contentParams);

        // Time text (large)
        timeText = new TextView(this);
        timeText.setTextColor(Color.WHITE);
        timeText.setTypeface(Typeface.DEFAULT_BOLD);
        timeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 72);
        timeText.setGravity(Gravity.CENTER);
        timeText.setShadowLayer(8, 0, 2, 0x40000000);

        // Date text
        dateText = new TextView(this);
        dateText.setTextColor(Color.argb(200, 255, 255, 255));
        dateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        dateText.setGravity(Gravity.CENTER);
        dateText.setPadding(0, 8, 0, 0);

        // Battery text
        batteryText = new TextView(this);
        batteryText.setTextColor(Color.argb(150, 255, 255, 255));
        batteryText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        batteryText.setGravity(Gravity.CENTER);
        batteryText.setPadding(0, 16, 0, 0);

        content.addView(timeText);
        content.addView(dateText);
        content.addView(batteryText);

        root.addView(content);

        // Close button (small X in top-right corner)
        TextView closeBtn = new TextView(this);
        closeBtn.setText("✕");
        closeBtn.setTextColor(Color.argb(180, 255, 255, 255));
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        closeBtn.setPadding(48, 32, 48, 32);
        closeBtn.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.END
        );
        int margin = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        closeParams.setMargins(0, margin, margin, 0);
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> finish());
        root.addView(closeBtn);

        setContentView(root);
        rootView = root;

        // Start clock update (every second)
        handler = new Handler(Looper.getMainLooper());
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                updateClock();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(clockRunnable);

        // Register battery receiver (sticky broadcast gives immediate value)
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        // Tap anywhere (outside the close button) also closes
        content.setOnClickListener(v -> finish());
    }

    private void updateClock() {
        Date now = new Date();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));

        if (timeText != null) {
            timeText.setText(timeFormat.format(now));
        }
        if (dateText != null) {
            dateText.setText(dateFormat.format(now));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && clockRunnable != null) {
            handler.removeCallbacks(clockRunnable);
        }
        try {
            unregisterReceiver(batteryReceiver);
        } catch (Exception e) {
            // Ignore if not registered
        }
    }
}
