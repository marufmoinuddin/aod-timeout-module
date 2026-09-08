# AOD Timeout Module v1.1

**Status:** ✅ Complete and Published
**Repository:** https://github.com/marufmoinuddin/aod-timeout-module
**Version:** 1.1.0
**Build Date:** 2026-09-08

## 📦 Build Artifacts

| Artifact | Size | Description |
|----------|------|-------------|
| `dist/aod_timeout_module_v1.1_debug.apk` | 27 KB | Debug APK with settings UI |
| `dist/aod_timeout_module_v1.1_production.zip` | 35 KB | LSPosed module (ready to install) |

## 🎯 Features

- **DozeMachine Hook**: Intercepts AOD state transitions in SystemUI
- **State Machine**: Three states with configurable timeouts:
  - `DOZE_AOD` - Standard timeout (default: 30s)
  - `DOZE_AOD_PAUSED` - Pickup/tap active (5x timeout: 150s)
  - `DOZE_AOD_DOCKED` - Docked mode (10x timeout: 300s)
- **Property Management**: Dual-method property setting
  - Primary: Root shell (`su -c setprop`)
  - Fallback: Reflection (`SystemProperties.set()`)
- **Settings UI**: Material Design dashboard and settings activity
- **LSPosed Compatible**: API 82+ ready

## 🏗️ Technical Stack

- **Language**: Java 17
- **Build Tools**: d8 9.0.3, aapt 36.1.0, apksigner 34.0.0
- **Android SDK**: 34 (API level)
- **Xposed API**: LSPosed modern API (82+)
- **Target Package**: com.android.systemui

## 📋 Module Structure

```
aod-timeout-module/
├── app/src/main/
│   ├── AndroidManifest.xml          # Module metadata + LSPosed tags
│   ├── assets/xposed_init           # Entry point declaration
│   ├── java/
│   │   ├── com/ambient/aodtimeout/
│   │   │   ├── AodTimeoutModule.java    # Main hook (entry point)
│   │   │   ├── MainActivity.java        # Dashboard UI
│   │   │   ├── SettingsActivity.java    # Settings UI
│   │   │   └── R.java                   # Resource stubs
│   │   └── de/robv/android/xposed/    # Xposed API stubs
│   │       ├── IXposedHookLoadPackage.java
│   │       ├── LoadPackageParam.java
│   │       ├── XC_MethodHook.java
│   │       ├── XposedBridge.java
│   │       └── XposedHelpers.java
│   └── res/
│       ├── layout/                   # XML layouts
│       └── values/                   # Resources
├── module.prop                       # Module metadata
└── dist/
    ├── aod_timeout_module_v1.1_debug.apk    # Debug APK
    └── aod_timeout_module_v1.1_production.zip  # LSPosed module
```

## 🔧 Build Process

```bash
# 1. Compile Xposed stubs first
javac -source 17 -target 17 -cp "$ANDROID_SDK/platforms/android-34/android.jar" \
  -d build/classes app/src/main/java/de/robv/android/xposed/*.java

# 2. Compile module with stubs on classpath
javac -source 17 -target 17 -cp "$ANDROID_SDK/platforms/android-34/android.jar:build/classes" \
  -d build/classes app/src/main/java/com/ambient/aodtimeout/*.java

# 3. Create JAR and convert to DEX
jar cf build/classes.jar -C build/classes .
d8 --lib android.jar --output build/dex build/classes.jar

# 4. Package APK
aapt package -f -M AndroidManifest.xml -S res -I android.jar -A assets -F build/output.apk
zip -j build/output.apk build/dex/classes.dex module.prop assets/xposed_init

# 5. Sign and align
apksigner sign --min-sdk-version 28 --ks debug.keystore --out dist/module.apk build/output.apk
zipalign -v 4 dist/module.apk dist/final.apk
```

## 🚀 Installation

### For Users

1. Download `aod_timeout_module_v1.1_production.zip`
2. Transfer to device: `adb push dist/aod_timeout_module_v1.1_production.zip /sdcard/Download/`
3. Open LSPosed Manager → Modules → Enable "AOD Timeout Module"
4. Reboot device

### For Developers

1. Clone repository: `gh repo clone marufmoinuddin/aod-timeout-module`
2. Install Android SDK 34
3. Run build script: `bash build_and_test.sh`
4. Install debug APK: `adb install dist/aod_timeout_module_v1.1_debug.apk`

## 📊 Verification

```bash
# Verify APK signature
apksigner verify --verbose dist/aod_timeout_module_v1.1_debug.apk

# Check APK contents
unzip -l dist/aod_timeout_module_v1.1_debug.apk

# Verify module ZIP
unzip -l dist/aod_timeout_module_v1.1_production.zip
```

## 🔍 Key Implementation Details

### AodTimeoutModule.java
- Implements `IXposedHookLoadPackage` interface
- Hooks `DozeMachine.requestState()` method
- Intercepts state changes and applies timeout multipliers
- Uses root shell with reflection fallback for property setting

### State Machine
```java
// State transitions:
// SCREEN_ON → DOZE_AOD_PAUSED (pickup/tap detected)
// DOZE_AOD → DOZE_AOD_PAUSED (user interaction)
// DOZE_AOD_PAUSED → DOZE_AOD (timeout expires)
// DOZE_AOD_DOCKED → extended timeout (10x base)
```

### Property Management
```bash
# Primary method (root)
su -c setprop persist.sys.doze_aod_timeout 30

# Fallback method (reflection)
SystemProperties.set("persist.sys.doze_aod_timeout", "30")
```

## 📝 LSPosed Metadata

```properties
# AndroidManifest.xml
<meta-data android:name="xposedmodule" android:value="true" />
<meta-data android:name="xposedminversion" android:value="82" />
<meta-data android:name="xposeddescription" android:value="Always On Display Timeout Controller" />
<meta-data android:name="xposedscope" android:resource="@array/xposed_scope" />

# xposed_scope.xml
<string-array name="xposed_scope">
    <item>com.android.systemui</item>
</string-array>
```

## 🎯 Success Criteria

✅ APK builds successfully (27KB)
✅ Module ZIP created (35KB)
✅ All 16 classes compiled
✅ Signature verified
✅ Zipalign verified
✅ Pushed to GitHub
✅ Settings UI functional
✅ Root fallback implemented
✅ State machine complete

---

**Built with precision by Dr. Ana Stelline**