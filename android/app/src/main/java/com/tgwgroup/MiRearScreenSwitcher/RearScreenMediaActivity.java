package com.tgwgroup.MiRearScreenSwitcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.List;

public class RearScreenMediaActivity extends Activity {
    private static final String TAG = "RearScreenMedia";
    
    private MediaSessionManager mediaSessionManager;
    private MediaController activeController;
    private Handler handler;
    
    private TextView titleText;
    private TextView artistText;
    private TextView appNameText;
    private ImageView playPauseBtn;
    private SeekBar progressBar;
    private LinearLayout rootView;
    
    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 1000);
        }
    };
    
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener = controllers -> {
        if (controllers != null && !controllers.isEmpty()) {
            setActiveController(controllers.get(0));
        } else {
            clearController();
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        
        handler = new Handler(Looper.getMainLooper());
        
        buildUI();
        
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        if (mediaSessionManager != null) {
            ComponentName notificationListener = new ComponentName(this, NotificationService.class);
            try {
                List<MediaController> controllers = mediaSessionManager.getActiveSessions(notificationListener);
                if (!controllers.isEmpty()) {
                    setActiveController(controllers.get(0));
                }
                mediaSessionManager.addOnActiveSessionsChangedListener(sessionsListener, notificationListener);
            } catch (SecurityException e) {
                appNameText.setText("Cần quyền Notification Listener");
            }
        }
        
        handler.post(updateProgressRunnable);
    }
    
    private void buildUI() {
        rootView = new LinearLayout(this);
        rootView.setOrientation(LinearLayout.VERTICAL);
        rootView.setGravity(Gravity.CENTER_HORIZONTAL);
        rootView.setPadding(48, 80, 48, 48);
        
        int[] colors = {0xFFFF9D88, 0xFFFFB5C5, 0xFFE0B5DC, 0xFFA8C5E5};
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        rootView.setBackground(gradient);
        
        // App name (top)
        appNameText = new TextView(this);
        appNameText.setTextColor(Color.argb(150, 255, 255, 255));
        appNameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        appNameText.setGravity(Gravity.CENTER);
        rootView.addView(appNameText);
        
        // Spacer
        rootView.addView(new View(this), new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 40));
        
        // Title (large)
        titleText = new TextView(this);
        titleText.setTextColor(Color.WHITE);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        titleText.setGravity(Gravity.CENTER);
        titleText.setMaxLines(2);
        rootView.addView(titleText);
        
        // Artist
        artistText = new TextView(this);
        artistText.setTextColor(Color.argb(200, 255, 255, 255));
        artistText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        artistText.setGravity(Gravity.CENTER);
        artistText.setMaxLines(1);
        rootView.addView(artistText);
        
        // Spacer
        rootView.addView(new View(this), new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 40));
        
        // Progress bar
        progressBar = new SeekBar(this);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        LinearLayout progressParams = new LinearLayout(this);
        progressParams.setOrientation(LinearLayout.HORIZONTAL);
        progressParams.addView(progressBar, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        rootView.addView(progressParams);
        
        // Spacer
        rootView.addView(new View(this), new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 24));
        
        // Controls row
        LinearLayout controlsRow = new LinearLayout(this);
        controlsRow.setOrientation(LinearLayout.HORIZONTAL);
        controlsRow.setGravity(Gravity.CENTER);
        
        // Previous button
        TextView prevBtn = new TextView(this);
        prevBtn.setText("⏮");
        prevBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        prevBtn.setTextColor(Color.WHITE);
        prevBtn.setPadding(32, 0, 32, 0);
        prevBtn.setOnClickListener(v -> {
            if (activeController != null) {
                activeController.getTransportControls().skipToPrevious();
            }
        });
        controlsRow.addView(prevBtn);
        
        // Play/Pause button
        playPauseBtn = new ImageView(this);
        // Use text since we can't easily set drawable without resources
        TextView playPauseText = new TextView(this);
        playPauseText.setText("▶");
        playPauseText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
        playPauseText.setTextColor(Color.WHITE);
        playPauseText.setPadding(48, 0, 48, 0);
        playPauseText.setOnClickListener(v -> {
            if (activeController != null) {
                PlaybackState state = activeController.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    activeController.getTransportControls().pause();
                } else {
                    activeController.getTransportControls().play();
                }
            }
        });
        controlsRow.addView(playPauseText);
        
        // Next button
        TextView nextBtn = new TextView(this);
        nextBtn.setText("⏭");
        nextBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        nextBtn.setTextColor(Color.WHITE);
        nextBtn.setPadding(32, 0, 32, 0);
        nextBtn.setOnClickListener(v -> {
            if (activeController != null) {
                activeController.getTransportControls().skipToNext();
            }
        });
        controlsRow.addView(nextBtn);
        
        rootView.addView(controlsRow);
        
        // Tap anywhere to close
        rootView.setOnLongClickListener(v -> { finish(); return true; });
        
        setContentView(rootView);
    }
    
    private void setActiveController(MediaController controller) {
        if (activeController != null) {
            activeController.unregisterCallback(mediaCallback);
        }
        activeController = controller;
        if (activeController != null) {
            activeController.registerCallback(mediaCallback);
            updateMetadata();
            updatePlaybackState();
        }
    }
    
    private void clearController() {
        if (activeController != null) {
            activeController.unregisterCallback(mediaCallback);
        }
        activeController = null;
        titleText.setText("Không có nhạc");
        artistText.setText("");
        appNameText.setText("");
    }
    
    private void updateMetadata() {
        if (activeController == null) return;
        MediaMetadata metadata = activeController.getMetadata();
        if (metadata != null) {
            String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            titleText.setText(title != null ? title : "Không có tiêu đề");
            artistText.setText(artist != null ? artist : "");
        }
        
        String packageName = activeController.getPackageName();
        if (packageName != null) {
            appNameText.setText(packageName);
        }
    }
    
    private void updatePlaybackState() {
        if (activeController == null) return;
        PlaybackState state = activeController.getPlaybackState();
        if (state != null) {
            // Update play/pause icon would go here
            // For now just update progress
        }
    }
    
    private void updateProgress() {
        if (activeController == null) return;
        PlaybackState state = activeController.getPlaybackState();
        MediaMetadata metadata = activeController.getMetadata();
        if (state == null || metadata == null) return;
        
        long duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        long position = state.getPosition();
        
        if (duration > 0) {
            int progress = (int) ((position * 100) / duration);
            progressBar.setProgress(Math.min(progress, 100));
        }
    }
    
    private final MediaController.Callback mediaCallback = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            updateMetadata();
        }
        
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            updatePlaybackState();
        }
    };
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(updateProgressRunnable);
        }
        if (mediaSessionManager != null) {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsListener);
        }
        if (activeController != null) {
            activeController.unregisterCallback(mediaCallback);
        }
    }
}
