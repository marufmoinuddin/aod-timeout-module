package com.ambient.aodtimeout;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Settings activity for configuring AOD timeout values.
 */
public class SettingsActivity extends Activity {
    
    private static final String TAG = "AOD_TIMEOUT";
    
    private AodTimeoutPreferences prefs;
    
    // UI Elements
    private EditText etAodTimeout;
    private EditText etPausedTimeout;
    private EditText etDockedTimeout;
    private CheckBox cbFallback;
    private CheckBox cbReflection;
    private Button btnApply;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        Log.i(TAG, "SettingsActivity.onCreate()");
        
        // Initialize preferences
        prefs = new AodTimeoutPreferences(this);
        
        // Initialize UI
        initViews();
        
        // Load current values
        loadSettings();
        
        // Set up listeners
        setupListeners();
    }
    
    private void initViews() {
        etAodTimeout = findViewById(R.id.et_aod_timeout);
        etPausedTimeout = findViewById(R.id.et_paused_timeout);
        etDockedTimeout = findViewById(R.id.et_docked_timeout);
        cbFallback = findViewById(R.id.cb_fallback);
        cbReflection = findViewById(R.id.cb_reflection);
        btnApply = findViewById(R.id.btn_apply);
    }
    
    private void loadSettings() {
        Log.i(TAG, "Loading settings");
        
        // Load timeout values
        if (etAodTimeout != null) {
            etAodTimeout.setText(String.valueOf(prefs.getAodTimeout()));
        }
        
        if (etPausedTimeout != null) {
            etPausedTimeout.setText(String.valueOf(prefs.getPausedTimeout()));
        }
        
        if (etDockedTimeout != null) {
            etDockedTimeout.setText(String.valueOf(prefs.getDockedTimeout()));
        }
        
        // Load boolean settings
        if (cbFallback != null) {
            cbFallback.setChecked(prefs.isFallbackEnabled());
        }
        
        if (cbReflection != null) {
            cbReflection.setChecked(prefs.isReflectionEnabled());
        }
    }
    
    private void setupListeners() {
        if (btnApply != null) {
            btnApply.setOnClickListener(v -> applySettings());
        }
    }
    
    private void applySettings() {
        Log.i(TAG, "Applying settings");
        
        try {
            // Parse timeout values
            int aodTimeout = parseTimeoutValue(etAodTimeout, AodTimeoutPreferences.DEFAULT_AOD_TIMEOUT);
            int pausedTimeout = parseTimeoutValue(etPausedTimeout, AodTimeoutPreferences.DEFAULT_PAUSED_TIMEOUT);
            int dockedTimeout = parseTimeoutValue(etDockedTimeout, AodTimeoutPreferences.DEFAULT_DOCKED_TIMEOUT);
            
            // Get boolean settings
            boolean enableFallback = cbFallback != null && cbFallback.isChecked();
            boolean useReflection = cbReflection != null && cbReflection.isChecked();
            
            // Apply via preferences
            boolean success = prefs.applySettings(aodTimeout, pausedTimeout, dockedTimeout, 
                                                   enableFallback, useReflection);
            
            if (success) {
                // Also try to apply via hook
                AodTimeoutHook hook = new AodTimeoutHook();
                hook.setUseRoot(enableFallback);
                hook.setUseReflection(useReflection);
                hook.setAodTimeout(aodTimeout);
                
                String message = getString(R.string.apply_success);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                
                Log.i(TAG, "Settings applied successfully");
                
                // Go back
                finish();
            } else {
                String message = getString(R.string.apply_failed);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to apply settings");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying settings: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private int parseTimeoutValue(EditText editText, int defaultValue) {
        if (editText == null || editText.getText() == null) {
            return defaultValue;
        }
        
        try {
            int value = Integer.parseInt(editText.getText().toString().trim());
            // Validate range (1-3600 seconds)
            return Math.max(1, Math.min(3600, value));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid timeout value, using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
    }
}
