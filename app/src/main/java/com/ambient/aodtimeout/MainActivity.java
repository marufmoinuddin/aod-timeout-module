package com.ambient.aodtimeout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

/**
 * Main dashboard activity showing module status and settings.
 */
public class MainActivity extends Activity {
    
    private static final String TAG = "AOD_TIMEOUT";
    
    private AodTimeoutPreferences prefs;
    private AodTimeoutHook hook;
    
    // UI Elements
    private TextView tvHookStatus;
    private TextView tvCurrentState;
    private TextView tvTimeoutValue;
    private TextView tvRootStatus;
    private Button btnSettings;
    private Button btnTest;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.i(TAG, "MainActivity.onCreate()");
        
        // Initialize components
        prefs = new AodTimeoutPreferences(this);
        hook = new AodTimeoutHook();
        
        // Find views
        tvHookStatus = findViewById(R.id.tv_hook_status);
        tvCurrentState = findViewById(R.id.tv_current_state);
        tvTimeoutValue = findViewById(R.id.tv_timeout_value);
        tvRootStatus = findViewById(R.id.tv_root_status);
        btnSettings = findViewById(R.id.btn_settings);
        btnTest = findViewById(R.id.btn_test);
        
        // Set up listeners
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            });
        }
        
        if (btnTest != null) {
            btnTest.setOnClickListener(v -> testHook());
        }
        
        // Update status
        updateStatus();
    }
    
    private void updateStatus() {
        Log.i(TAG, "Updating status display");
        
        // Hook status
        if (tvHookStatus != null) {
            boolean hookActive = checkHookStatus();
            String hookText = hookActive ? getString(R.string.hook_active) : getString(R.string.hook_inactive);
            tvHookStatus.setText(hookText);
            tvHookStatus.setTextColor(hookActive ? 
                getResources().getColor(R.color.status_active) : 
                getResources().getColor(R.color.status_inactive));
        }
        
        // Current state
        if (tvCurrentState != null) {
            tvCurrentState.setText("NORMAL");
        }
        
        // Timeout value
        if (tvTimeoutValue != null) {
            int timeout = prefs.getAodTimeout();
            tvTimeoutValue.setText(timeout + "s");
        }
        
        // Root status
        if (tvRootStatus != null) {
            boolean hasRoot = hook.testRootAccess();
            String rootText = hasRoot ? getString(R.string.granted) : getString(R.string.denied);
            tvRootStatus.setText(rootText);
            tvRootStatus.setTextColor(hasRoot ? 
                getResources().getColor(R.color.status_active) : 
                getResources().getColor(R.color.status_inactive));
        }
    }
    
    private boolean checkHookStatus() {
        try {
            String sdkVersion = hook.tryGetProperty("ro.build.version.sdk");
            return sdkVersion != null && !sdkVersion.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    private void testHook() {
        boolean rootWorks = hook.testRootAccess();
        boolean reflectionWorks = hook.testReflectionAccess();
        
        Log.i(TAG, "Hook test: root=" + rootWorks + ", reflection=" + reflectionWorks);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
