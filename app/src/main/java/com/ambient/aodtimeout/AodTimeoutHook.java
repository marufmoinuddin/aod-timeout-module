package com.ambient.aodtimeout;

import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Handles the actual AOD timeout property management.
 */
public class AodTimeoutHook {
    
    private static final String TAG = "AOD_TIMEOUT_HOOK";
    
    // System properties
    private static final String PROP_DOZE_ENABLED = "persist.sys.doze_enabled";
    private static final String PROP_DOZE_ALWAYS_ON = "persist.sys.doze_always_on";
    private static final String PROP_AOD_TIMEOUT = "persist.sys.doze_aod_timeout";
    
    // DozeMachine state names
    public static final String STATE_DOZE_AOD = "DOZE_AOD";
    public static final String STATE_DOZE_AOD_PAUSED = "DOZE_AOD_PAUSED";
    public static final String STATE_DOZE_AOD_DOCKED = "DOZE_AOD_DOCKED";
    public static final String STATE_DOZE = "DOZE";
    
    private boolean useRoot = true;
    private boolean useReflection = true;
    
    public AodTimeoutHook() {
        Log.i(TAG, "AodTimeoutHook initialized");
    }
    
    /**
     * Sets the AOD timeout property.
     * @param timeoutSeconds Timeout in seconds
     * @return true if successful
     */
    public boolean setAodTimeout(int timeoutSeconds) {
        Log.i(TAG, "Setting AOD timeout: " + timeoutSeconds + "s");
        
        // Try root first
        if (useRoot && tryRootSetProperty(PROP_AOD_TIMEOUT, String.valueOf(timeoutSeconds))) {
            return true;
        }
        
        // Fallback to reflection
        if (useReflection) {
            return tryReflectionSetProperty(PROP_AOD_TIMEOUT, String.valueOf(timeoutSeconds));
        }
        
        Log.e(TAG, "Failed to set property via any method");
        return false;
    }
    
    /**
     * Sets the doze_enabled property.
     * @param enabled true to enable, false to disable
     * @return true if successful
     */
    public boolean setDozeEnabled(boolean enabled) {
        String value = enabled ? "1" : "0";
        Log.i(TAG, "Setting doze_enabled: " + value);
        
        if (useRoot && tryRootSetProperty(PROP_DOZE_ENABLED, value)) {
            return true;
        }
        
        if (useReflection) {
            return tryReflectionSetProperty(PROP_DOZE_ENABLED, value);
        }
        
        return false;
    }
    
    /**
     * Sets the doze_always_on property.
     * @param enabled true to enable, false to disable
     * @return true if successful
     */
    public boolean setAlwaysOnEnabled(boolean enabled) {
        String value = enabled ? "1" : "0";
        Log.i(TAG, "Setting doze_always_on: " + value);
        
        if (useRoot && tryRootSetProperty(PROP_DOZE_ALWAYS_ON, value)) {
            return true;
        }
        
        if (useReflection) {
            return tryReflectionSetProperty(PROP_DOZE_ALWAYS_ON, value);
        }
        
        return false;
    }
    
    /**
     * Gets the current AOD timeout value.
     * @return timeout in seconds, or -1 if unable to read
     */
    public int getAodTimeout() {
        String value = tryGetProperty(PROP_AOD_TIMEOUT);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid timeout value: " + value);
            }
        }
        return -1;
    }
    
    /**
     * Gets the current doze_enabled value.
     * @return true if enabled, false otherwise
     */
    public boolean isDozeEnabled() {
        String value = tryGetProperty(PROP_DOZE_ENABLED);
        return "1".equals(value) || "true".equals(value);
    }
    
    /**
     * Gets the current doze_always_on value.
     * @return true if enabled, false otherwise
     */
    public boolean isAlwaysOnEnabled() {
        String value = tryGetProperty(PROP_DOZE_ALWAYS_ON);
        return "1".equals(value) || "true".equals(value);
    }
    
    // ===== Private Methods =====
    
    private boolean tryRootSetProperty(String key, String value) {
        try {
            Log.d(TAG, "Attempting root setprop: " + key + "=" + value);
            Process process = Runtime.getRuntime().exec(new String[]{
                "su", "-c", "setprop " + key + " " + value
            });
            
            int exitCode = process.waitFor();
            boolean success = exitCode == 0;
            
            if (success) {
                Log.i(TAG, "Root setprop successful");
            } else {
                Log.w(TAG, "Root setprop failed with exit code: " + exitCode);
            }
            
            return success;
            
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Root property set failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean tryReflectionSetProperty(String key, String value) {
        try {
            Log.d(TAG, "Attempting reflection setprop: " + key + "=" + value);
            
            Class<?> systemProperties = XposedHelpers.findClass(
                "android.os.SystemProperties", null);
            
            Method setMethod = XposedHelpers.findMethodExact(
                systemProperties, "set", String.class, String.class);
            
            setMethod.invoke(null, key, value);
            
            Log.i(TAG, "Reflection setprop successful");
            return true;
            
        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            Log.e(TAG, "Reflection property set failed: " + e.getMessage());
            return false;
        }
    }
    
    public String tryGetProperty(String key) {
        try {
            Class<?> systemProperties = XposedHelpers.findClass(
                "android.os.SystemProperties", null);
            
            Method getMethod = XposedHelpers.findMethodExact(
                systemProperties, "get", String.class);
            
            return (String) getMethod.invoke(null, key);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to get property: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Tests if root access is available.
     * @return true if root is available
     */
    public boolean testRootAccess() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.getOutputStream().close();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Tests if reflection method works.
     * @return true if reflection works
     */
    public boolean testReflectionAccess() {
        try {
            // Try to read a known property
            String value = tryGetProperty("ro.build.version.sdk");
            return value != null && !value.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    // Getters and Setters
    public void setUseRoot(boolean useRoot) {
        this.useRoot = useRoot;
    }
    
    public void setUseReflection(boolean useReflection) {
        this.useReflection = useReflection;
    }
}
