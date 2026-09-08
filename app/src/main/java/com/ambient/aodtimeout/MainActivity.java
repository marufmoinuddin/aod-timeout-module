package com.ambient.aodtimeout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main dashboard activity showing module status and settings.
 */
public class MainActivity extends Activity {
    
    private static final String TAG = "AOD_TIMEOUT";
    
    private AodTimeoutPreferences prefs;
    private DeviceStateReceiver deviceStateReceiver;
    private AodTimeoutHook hook;
    
    // UI Elements
    private TextView tvModuleStatus;
    private TextView tvHookStatus;
    private TextView tvCurrentState;
    private TextView tvTimeoutValue;
    private TextView tvRootStatus;
    private ProgressBar progressBar;
    private View viewError;
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
        
        // Initialize device state monitor
        deviceStateReceiver = new DeviceStateReceiver(this);
        deviceStateReceiver.setListener(new DeviceStateReceiver.StateChangeListener() {
            @Override
            public void onStateChanged(DeviceStateReceiver.DeviceState newState) {
                runOnUiThread(() -> updateDeviceStateDisplay(newState));
            }
            
            @Override
            public void onChargingStatusChanged(boolean isCharging) {
                // Could update UI here if needed
            }
            
            @Override
            public void onDockStatusChanged(boolean isDocked) {
                // Could update UI here if needed
            }
        });
        
        // Find views
        initViews();
        
        // Update status
        updateStatus();
        
        // Set up listeners
        setupListeners();
    }
    
    private void initViews() {
        tvModuleStatus = findViewById(R.id.tv_title);
        tvHookStatus = findViewById(R.id.tv_hook_status);
        tvCurrentState = findViewById(R.id.tv_current_state);
        tvTimeoutValue = findViewById(R.id.tv_timeout_value);
        tvRootStatus = findViewById(R.id.tv_root_status);
        progressBar = findViewById(R.id.view_loading);
        viewError = findViewById(R.id.view_error);
        btnSettings = findViewById(R.id.btn_settings);
        btnTest = findViewById(R.id.btn_test);
    }
    
    private void setupListeners() {
        // Settings button
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Log.i(TAG, "Opening settings");
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            });
        }
        
        // Test button
        if (btnTest != null) {
            btnTest.setOnClickListener(v -> testHook());
        }
    }
    
    private void updateStatus() {
        Log.i(TAG, "Updating status display");
        
        // Module status
        if (tvModuleStatus != null) {
            tvModuleStatus.setText(AodTimeoutModule.getModuleInfo());
        }
        
        // Hook status (check if we can access properties)
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
            String state = deviceStateReceiver != null ? 
                deviceStateReceiver.getCurrentState().toString() : "NORMAL";
            tvCurrentState.setText(state);
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
        // Check if we can read/write properties
        try {
            String sdkVersion = hook.tryGetProperty("ro.build.version.sdk");
            return sdkVersion != null && !sdkVersion.isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "Error checking hook status: " + e.getMessage());
            return false;
        }
    }
    
    private void updateDeviceStateDisplay(DeviceStateReceiver.DeviceState state) {
        if (tvCurrentState != null) {
            tvCurrentState.setText(state.toString());
        }
    }
    
    private void testHook() {
        Log.i(TAG, "Testing hook...");
        
        // Show progress
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        // Test both root and reflection
        boolean rootWorks = hook.testRootAccess();
        boolean reflectionWorks = hook.testReflectionAccess();
        
        String message = "Hook Test Results:\n\n";
        message += "Root Access: " + (rootWorks ? "✓ Available" : "✗ Not available") + "\n";
        message += "Reflection: " + (reflectionWorks ? "✓ Available" : "✗ Not available") + "\n";
        message += "\nCurrent timeout: " + hook.getAodTimeout() + "s";
        
        // Hide progress
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - updating status");
        updateStatus();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        
        // Unregister receiver
        if (deviceStateReceiver != null) {
            deviceStateReceiver.unregister();
        }
    }
}
