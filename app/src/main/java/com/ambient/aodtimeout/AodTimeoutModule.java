package com.ambient.aodtimeout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.LoadPackageParam;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * LSPosed module entry point for AOD Timeout control.
 * Hooks DozeMachine in com.android.systemui to intercept AOD state transitions.
 */
public class AodTimeoutModule implements IXposedHookLoadPackage {
    
    private static final String TAG = "AOD_TIMEOUT";
    private static final String PROP_DOZE_ENABLED = "persist.sys.doze_enabled";
    private static final String PROP_DOZE_ALWAYS_ON = "persist.sys.doze_always_on";
    private static final String PROP_AOD_TIMEOUT = "persist.sys.doze_aod_timeout";
    
    // Default timeout values
    private static final int DEFAULT_AOD_TIMEOUT = 30;
    private static final int DEFAULT_PAUSED_TIMEOUT = 150;
    private static final int DEFAULT_DOCKED_TIMEOUT = 300;
    
    // State multipliers
    private static final float PAUSED_MULTIPLIER = 5.0f;
    private static final float DOCKED_MULTIPLIER = 10.0f;
    
    private SharedPreferences prefs;
    private boolean useFallback = true;
    
    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        Log.i(TAG, "Module loaded for: " + lpparam.packageName);
        
        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }
        
        Log.i(TAG, "Hooking DozeMachine in SystemUI...");
        
        // Hook DozeMachine.requestState() to intercept AOD state changes
        XposedHelpers.findAndHookMethod(
            "com.android.systemui.doze.DozeMachine",
            lpparam.classLoader,
            "requestState",
            XposedHelpers.findClass("com.android.systemui.doze.DozeMachine$State", lpparam.classLoader),
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    handleStateChange(param);
                }
            }
        );
        
        Log.i(TAG, "DozeMachine.requestState() hooked successfully");
    }
    
    private void handleStateChange(XC_MethodHook.MethodHookParam param) {
        try {
            Object state = param.args[0];
            String stateName = state.toString();
            
            Log.i(TAG, "State change detected: " + stateName);
            
            // Get current timeout from preferences
            int baseTimeout = DEFAULT_AOD_TIMEOUT;
            if (prefs != null) {
                baseTimeout = prefs.getInt("aod_timeout", DEFAULT_AOD_TIMEOUT);
            }
            
            // Calculate timeout based on state
            int finalTimeout = baseTimeout;
            
            if ("DOZE_AOD_PAUSED".equals(stateName)) {
                finalTimeout = (int) (baseTimeout * PAUSED_MULTIPLIER);
                Log.i(TAG, "DOZE_AOD_PAUSED: timeout = " + finalTimeout + "s");
            } else if ("DOZE_AOD_DOCKED".equals(stateName)) {
                finalTimeout = (int) (baseTimeout * DOCKED_MULTIPLIER);
                Log.i(TAG, "DOZE_AOD_DOCKED: timeout = " + finalTimeout + "s");
            } else if ("DOZE_AOD".equals(stateName)) {
                Log.i(TAG, "DOZE_AOD: timeout = " + finalTimeout + "s");
            }
            
            // Set the property
            setProperty(PROP_AOD_TIMEOUT, String.valueOf(finalTimeout));
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling state change: " + e.getMessage());
        }
    }
    
    private void setProperty(String key, String value) {
        // Try root shell first
        if (tryRootSetProperty(key, value)) {
            Log.i(TAG, "Property set via root: " + key + "=" + value);
            return;
        }
        
        // Fallback to reflection
        if (useFallback) {
            tryReflectionSetProperty(key, value);
        }
    }
    
    private boolean tryRootSetProperty(String key, String value) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", 
                "setprop " + key + " " + value});
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Root property set failed: " + e.getMessage());
            return false;
        }
    }
    
    private void tryReflectionSetProperty(String key, String value) {
        try {
            Class<?> systemProperties = XposedHelpers.findClass(
                "android.os.SystemProperties", null);
            Method setMethod = XposedHelpers.findMethodExact(
                systemProperties, "set", String.class, String.class);
            setMethod.invoke(null, key, value);
            Log.i(TAG, "Property set via reflection: " + key + "=" + value);
        } catch (Exception e) {
            Log.e(TAG, "Reflection property set failed: " + e.getMessage());
        }
    }
}