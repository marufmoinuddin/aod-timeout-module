package com.ambient.aodtimeout;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages persistent preferences for AOD timeout settings.
 */
public class AodTimeoutPreferences {
    
    public static final String PREFS_NAME = "aod_timeout_prefs";
    
    // Preference keys
    public static final String KEY_AOD_TIMEOUT = "aod_timeout";
    public static final String KEY_PAUSED_TIMEOUT = "paused_timeout";
    public static final String KEY_DOCKED_TIMEOUT = "docked_timeout";
    public static final String KEY_ENABLE_FALLBACK = "enable_fallback";
    public static final String KEY_USE_REFLECTION = "use_reflection";
    
    // Default values
    public static final int DEFAULT_AOD_TIMEOUT = 30;
    public static final int DEFAULT_PAUSED_TIMEOUT = 150;
    public static final int DEFAULT_DOCKED_TIMEOUT = 300;
    public static final boolean DEFAULT_ENABLE_FALLBACK = true;
    public static final boolean DEFAULT_USE_REFLECTION = true;
    
    private final SharedPreferences prefs;
    
    public AodTimeoutPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    // AOD Timeout (base timeout)
    public int getAodTimeout() {
        return prefs.getInt(KEY_AOD_TIMEOUT, DEFAULT_AOD_TIMEOUT);
    }
    
    public void setAodTimeout(int seconds) {
        prefs.edit().putInt(KEY_AOD_TIMEOUT, seconds).apply();
    }
    
    // Paused Timeout
    public int getPausedTimeout() {
        return prefs.getInt(KEY_PAUSED_TIMEOUT, DEFAULT_PAUSED_TIMEOUT);
    }
    
    public void setPausedTimeout(int seconds) {
        prefs.edit().putInt(KEY_PAUSED_TIMEOUT, seconds).apply();
    }
    
    // Docked Timeout
    public int getDockedTimeout() {
        return prefs.getInt(KEY_DOCKED_TIMEOUT, DEFAULT_DOCKED_TIMEOUT);
    }
    
    public void setDockedTimeout(int seconds) {
        prefs.edit().putInt(KEY_DOCKED_TIMEOUT, seconds).apply();
    }
    
    // Fallback settings
    public boolean isFallbackEnabled() {
        return prefs.getBoolean(KEY_ENABLE_FALLBACK, DEFAULT_ENABLE_FALLBACK);
    }
    
    public void setFallbackEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLE_FALLBACK, enabled).apply();
    }
    
    public boolean isReflectionEnabled() {
        return prefs.getBoolean(KEY_USE_REFLECTION, DEFAULT_USE_REFLECTION);
    }
    
    public void setReflectionEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_USE_REFLECTION, enabled).apply();
    }
    
    // Apply all settings
    public boolean applySettings(int aodTimeout, int pausedTimeout, int dockedTimeout, 
                                  boolean enableFallback, boolean useReflection) {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_AOD_TIMEOUT, aodTimeout);
            editor.putInt(KEY_PAUSED_TIMEOUT, pausedTimeout);
            editor.putInt(KEY_DOCKED_TIMEOUT, dockedTimeout);
            editor.putBoolean(KEY_ENABLE_FALLBACK, enableFallback);
            editor.putBoolean(KEY_USE_REFLECTION, useReflection);
            return editor.commit();
        } catch (Exception e) {
            android.util.Log.e("AOD_TIMEOUT", "Failed to apply settings: " + e.getMessage());
            return false;
        }
    }
    
    // Reset to defaults
    public void resetToDefaults() {
        prefs.edit()
            .putInt(KEY_AOD_TIMEOUT, DEFAULT_AOD_TIMEOUT)
            .putInt(KEY_PAUSED_TIMEOUT, DEFAULT_PAUSED_TIMEOUT)
            .putInt(KEY_DOCKED_TIMEOUT, DEFAULT_DOCKED_TIMEOUT)
            .putBoolean(KEY_ENABLE_FALLBACK, DEFAULT_ENABLE_FALLBACK)
            .putBoolean(KEY_USE_REFLECTION, DEFAULT_USE_REFLECTION)
            .apply();
    }
}
