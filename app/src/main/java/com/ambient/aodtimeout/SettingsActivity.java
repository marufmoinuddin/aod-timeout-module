package com.ambient.aodtimeout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Settings activity for configuring timeout values.
 */
public class SettingsActivity extends Activity {
    
    private static final String TAG = "AOD_TIMEOUT";
    private static final String PREFS_NAME = "aod_timeout_prefs";
    
    private EditText etAodTimeout;
    private EditText etPausedTimeout;
    private EditText etDockedTimeout;
    private CheckBox cbFallback;
    private Button btnApply;
    
    private SharedPreferences prefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Initialize views
        etAodTimeout = findViewById(R.id.et_aod_timeout);
        etPausedTimeout = findViewById(R.id.et_paused_timeout);
        etDockedTimeout = findViewById(R.id.et_docked_timeout);
        cbFallback = findViewById(R.id.cb_fallback);
        btnApply = findViewById(R.id.btn_apply);
        
        // Load saved values
        if (etAodTimeout != null) {
            etAodTimeout.setText(String.valueOf(prefs.getInt("aod_timeout", 30)));
        }
        if (etPausedTimeout != null) {
            etPausedTimeout.setText(String.valueOf(prefs.getInt("paused_timeout", 150)));
        }
        if (etDockedTimeout != null) {
            etDockedTimeout.setText(String.valueOf(prefs.getInt("docked_timeout", 300)));
        }
        if (cbFallback != null) {
            cbFallback.setChecked(prefs.getBoolean("use_fallback", true));
        }
        
        // Apply button
        if (btnApply != null) {
            btnApply.setOnClickListener(v -> applySettings());
        }
    }
    
    private void applySettings() {
        try {
            int aodTimeout = 30;
            int pausedTimeout = 150;
            int dockedTimeout = 300;
            boolean useFallback = cbFallback.isChecked();
            
            if (etAodTimeout != null) {
                aodTimeout = Integer.parseInt(etAodTimeout.getText().toString());
            }
            if (etPausedTimeout != null) {
                pausedTimeout = Integer.parseInt(etPausedTimeout.getText().toString());
            }
            if (etDockedTimeout != null) {
                dockedTimeout = Integer.parseInt(etDockedTimeout.getText().toString());
            }
            
            // Save to SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("aod_timeout", aodTimeout);
            editor.putInt("paused_timeout", pausedTimeout);
            editor.putInt("docked_timeout", dockedTimeout);
            editor.putBoolean("use_fallback", useFallback);
            editor.apply();
            
            // Also set the properties via root
            setProperty("persist.sys.doze_aod_timeout", String.valueOf(aodTimeout));
            
            Toast.makeText(this, R.string.apply_success, Toast.LENGTH_SHORT).show();
            
            // Return to main
            Intent result = new Intent();
            setResult(RESULT_OK, result);
            finish();
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.apply_failed, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setProperty(String key, String value) {
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", "setprop " + key + " " + value})
                .waitFor();
        } catch (Exception e) {
            // Fallback to reflection if root fails
            tryReflectionSetProperty(key, value);
        }
    }
    
    private void tryReflectionSetProperty(String key, String value) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method setMethod = systemProperties.getMethod("set", String.class, String.class);
            setMethod.invoke(null, key, value);
        } catch (Exception e) {
            // Silently fail
        }
    }
}