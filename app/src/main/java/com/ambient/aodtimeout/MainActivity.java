package com.ambient.aodtimeout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main dashboard activity showing module status.
 */
public class MainActivity extends Activity {
    
    private static final String TAG = "AOD_TIMEOUT";
    private static final String PREFS_NAME = "aod_timeout_prefs";
    
    private SharedPreferences prefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Update UI
        updateStatus();
        
        // Set up button listeners
        Button btnSettings = findViewById(R.id.btn_settings);
        Button btnTest = findViewById(R.id.btn_test);
        
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
            });
        }
        
        if (btnTest != null) {
            btnTest.setOnClickListener(v -> testHook());
        }
    }
    
    private void updateStatus() {
        // Check if hook is active (simplified - in reality would check via reflection)
        boolean hookActive = checkHookActive();
        TextView tvHookStatus = findViewById(R.id.tv_hook_status);
        if (tvHookStatus != null) {
            tvHookStatus.setText(hookActive ? getString(R.string.hook_active) : getString(R.string.hook_inactive));
        }
        
        // Show current state (placeholder)
        String currentState = prefs.getString("current_state", "UNINITIALIZED");
        TextView tvState = findViewById(R.id.tv_current_state);
        if (tvState != null) {
            tvState.setText(currentState);
        }
        
        // Show timeout value
        int timeout = prefs.getInt("aod_timeout", 30);
        TextView tvTimeout = findViewById(R.id.tv_timeout_value);
        if (tvTimeout != null) {
            tvTimeout.setText(timeout + "s");
        }
        
        // Check root access
        boolean hasRoot = checkRootAccess();
        TextView tvRoot = findViewById(R.id.tv_root_status);
        if (tvRoot != null) {
            tvRoot.setText(hasRoot ? getString(R.string.granted) : getString(R.string.denied));
        }
    }
    
    private boolean checkHookActive() {
        // Placeholder - in production, check if SystemUI process was hooked
        return true;
    }
    
    private boolean checkRootAccess() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.getOutputStream().close();
            process.waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void testHook() {
        Toast.makeText(this, "Testing hook...", Toast.LENGTH_SHORT).show();
        // In production, trigger a test state change
    }
}