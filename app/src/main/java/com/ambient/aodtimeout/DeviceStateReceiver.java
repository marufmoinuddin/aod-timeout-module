package com.ambient.aodtimeout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Monitors device state changes (charging, dock, screen on/off).
 */
public class DeviceStateReceiver extends BroadcastReceiver {
    
    private static final String TAG = "AOD_TIMEOUT";
    
    // Current device state
    public enum DeviceState {
        NORMAL,
        CHARGING,
        DOCKED,
        SCREEN_ON
    }
    
    private DeviceState currentState = DeviceState.NORMAL;
    private StateChangeListener listener;
    private final Context context;
    private final Handler handler;
    
    public interface StateChangeListener {
        void onStateChanged(DeviceState newState);
        void onChargingStatusChanged(boolean isCharging);
        void onDockStatusChanged(boolean isDocked);
    }
    
    public DeviceStateReceiver(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        registerReceiver();
    }
    
    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_DOCK_EVENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        
        try {
            context.registerReceiver(this, filter);
            Log.i(TAG, "DeviceStateReceiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to register receiver: " + e.getMessage());
        }
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (action == null) return;
        
        switch (action) {
            case Intent.ACTION_BATTERY_CHANGED:
                handleBatteryChanged(intent);
                break;
            case Intent.ACTION_DOCK_EVENT:
                handleDockChanged(intent);
                break;
            case Intent.ACTION_SCREEN_ON:
                handleScreenOn();
                break;
            case Intent.ACTION_SCREEN_OFF:
                handleScreenOff();
                break;
        }
    }
    
    private void handleBatteryChanged(Intent intent) {
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean isCharging = plugged == BatteryManager.BATTERY_PLUGGED_AC 
                          || plugged == BatteryManager.BATTERY_PLUGGED_USB
                          || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        
        Log.d(TAG, "Battery changed: charging=" + isCharging);
        
        if (listener != null) {
            listener.onChargingStatusChanged(isCharging);
        }
        
        // Update state based on charging
        if (isCharging) {
            updateState(DeviceState.CHARGING);
        }
    }
    
    private void handleDockChanged(Intent intent) {
        int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
        boolean isDocked = dockState >= 0;
        
        Log.d(TAG, "Dock changed: docked=" + isDocked + " state=" + dockState);
        
        if (listener != null) {
            listener.onDockStatusChanged(isDocked);
        }
        
        // Update state based on dock
        if (isDocked) {
            updateState(DeviceState.DOCKED);
        } else if (currentState != DeviceState.SCREEN_ON) {
            updateState(DeviceState.NORMAL);
        }
    }
    
    private void handleScreenOn() {
        Log.d(TAG, "Screen turned on");
        updateState(DeviceState.SCREEN_ON);
        
        if (listener != null) {
            listener.onStateChanged(DeviceState.SCREEN_ON);
        }
    }
    
    private void handleScreenOff() {
        Log.d(TAG, "Screen turned off");
        // Don't change state here - let battery/dock events handle it
    }
    
    private void updateState(DeviceState newState) {
        if (currentState != newState) {
            Log.i(TAG, "State changed: " + currentState + " -> " + newState);
            currentState = newState;
            
            if (listener != null) {
                listener.onStateChanged(newState);
            }
        }
    }
    
    public DeviceState getCurrentState() {
        return currentState;
    }
    
    public void setListener(StateChangeListener listener) {
        this.listener = listener;
    }
    
    public void unregister() {
        try {
            context.unregisterReceiver(this);
            Log.i(TAG, "DeviceStateReceiver unregistered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregister receiver: " + e.getMessage());
        }
    }
}
