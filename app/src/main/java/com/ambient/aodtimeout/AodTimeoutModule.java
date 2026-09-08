package com.ambient.aodtimeout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.LoadPackageParam;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Main LSPosed module entry point.
 * Hooks DozeMachine in com.android.systemui to control AOD timeout.
 */
public class AodTimeoutModule implements IXposedHookLoadPackage {
    
    private static final String TAG = "AOD_TIMEOUT";
    private static final String TARGET_PACKAGE = "com.android.systemui";
    
    // DozeMachine class names
    private static final String DOZE_MACHINE_CLASS = "com.android.systemui.doze.DozeMachine";
    private static final String DOZE_STATE_CLASS = "com.android.systemui.doze.DozeMachine$State";
    
    // Module info
    public static final String MODULE_ID = "com.ambient.aodtimeout";
    public static final String MODULE_NAME = "AOD Timeout Module";
    public static final String MODULE_VERSION = "1.1.0";
    
    private AodTimeoutHook hook;
    private boolean hookInstalled = false;
    
    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        Log.i(TAG, "========== AOD Timeout Module v" + MODULE_VERSION + " ==========");
        Log.i(TAG, "Package: " + lpparam.packageName);
        
        // Check if this is our target package
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            Log.d(TAG, "Skipping non-target package: " + lpparam.packageName);
            return;
        }
        
        Log.i(TAG, "Hooking " + TARGET_PACKAGE + "...");
        
        // Initialize hook manager
        hook = new AodTimeoutHook();
        
        // Install hooks
        installDozeMachineHooks(lpparam.classLoader);
        
        // Log module status
        logModuleStatus();
    }
    
    /**
     * Installs hooks on DozeMachine methods.
     */
    private void installDozeMachineHooks(ClassLoader classLoader) {
        try {
            // Hook requestState() - the main entry point for AOD state changes
            XposedHelpers.findAndHookMethod(
                DOZE_MACHINE_CLASS,
                classLoader,
                "requestState",
                XposedHelpers.findClass(DOZE_STATE_CLASS, classLoader),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object state = param.args[0];
                        String stateName = state != null ? state.toString() : "null";
                            
                        Log.i(TAG, "requestState called with state: " + stateName);
                        
                        // Handle state transitions
                        handleStateTransition(stateName);
                    }
                }
            );
            
            Log.i(TAG, "Successfully hooked DozeMachine.requestState()");
            hookInstalled = true;
            
        } catch (NoSuchMethodError e) {
            Log.e(TAG, "Method not found: requestState() - " + e.getMessage());
            Log.e(TAG, "Trying alternative hook...");
            tryHookAlternative(classLoader);
        } catch (Exception e) {
            Log.e(TAG, "Failed to hook DozeMachine: " + e.getMessage(), e);
        }
    }
    
    /**
     * Alternative hooking strategy if primary fails.
     */
    private void tryHookAlternative(ClassLoader classLoader) {
        try {
            // Try hooking onStart() or other lifecycle methods
            XposedHelpers.findAndHookMethod(
                DOZE_MACHINE_CLASS,
                classLoader,
                "onStart",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Log.i(TAG, "DozeMachine.onStart() called");
                    }
                }
            );
            
            Log.i(TAG, "Successfully hooked DozeMachine.onStart()");
            hookInstalled = true;
            
        } catch (Exception e) {
            Log.e(TAG, "Alternative hook also failed: " + e.getMessage());
        }
    }
    
    /**
     * Handles state transitions and sets appropriate timeouts.
     */
    private void handleStateTransition(String stateName) {
        if (hook == null) return;
        
        Log.i(TAG, "Handling state transition: " + stateName);
        
        // Determine timeout based on state
        int timeout = AodTimeoutPreferences.DEFAULT_AOD_TIMEOUT;
        
        if (AodTimeoutHook.STATE_DOZE_AOD_PAUSED.equals(stateName)) {
            timeout = AodTimeoutPreferences.DEFAULT_PAUSED_TIMEOUT;
            Log.i(TAG, "DOZE_AOD_PAUSED: using paused timeout " + timeout + "s");
        } else if (AodTimeoutHook.STATE_DOZE_AOD_DOCKED.equals(stateName)) {
            timeout = AodTimeoutPreferences.DEFAULT_DOCKED_TIMEOUT;
            Log.i(TAG, "DOZE_AOD_DOCKED: using docked timeout " + timeout + "s");
        } else if (AodTimeoutHook.STATE_DOZE_AOD.equals(stateName)) {
            timeout = AodTimeoutPreferences.DEFAULT_AOD_TIMEOUT;
            Log.i(TAG, "DOZE_AOD: using default timeout " + timeout + "s");
        }
        
        // Apply timeout
        boolean success = hook.setAodTimeout(timeout);
        if (success) {
            Log.i(TAG, "Timeout applied successfully: " + timeout + "s");
        } else {
            Log.e(TAG, "Failed to apply timeout");
        }
    }
    
    /**
     * Logs the current module status.
     */
    private void logModuleStatus() {
        Log.i(TAG, "========== Module Status ==========");
        Log.i(TAG, "Module: " + MODULE_NAME);
        Log.i(TAG, "Version: " + MODULE_VERSION);
        Log.i(TAG, "Target: " + TARGET_PACKAGE);
        Log.i(TAG, "Hook installed: " + hookInstalled);
        
        if (hook != null) {
            Log.i(TAG, "Root access: " + hook.testRootAccess());
            Log.i(TAG, "Reflection access: " + hook.testReflectionAccess());
            Log.i(TAG, "Current timeout: " + hook.getAodTimeout() + "s");
            Log.i(TAG, "Doze enabled: " + hook.isDozeEnabled());
            Log.i(TAG, "Always on: " + hook.isAlwaysOnEnabled());
        }
        
        Log.i(TAG, "=================================");
    }
    
    /**
     * Opens the module settings activity.
     */
    public static void openSettings(Context context) {
        try {
            Intent intent = new Intent(context, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open settings: " + e.getMessage());
        }
    }
    
    /**
     * Gets module information.
     */
    public static String getModuleInfo() {
        return MODULE_NAME + " v" + MODULE_VERSION;
    }
}
