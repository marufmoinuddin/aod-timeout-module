# AOD Timeout Module v1.1 - Build Report

**Status:** ✅ COMPLETE AND READY FOR INSTALLATION
**Build Date:** 2026-09-08
**Version:** 1.1.0

---

## 📦 Build Artifacts

| Artifact | Size | Purpose |
|----------|------|---------|
| `dist/aod_timeout_module_v1.1_debug.apk` | 60 KB | Debug APK with UI |
| `dist/aod_timeout_module_v1.1_production.zip` | 24 KB | LSPosed module |

---

## 🔧 Architecture

### Core Components (13 Java classes)

| Class | Purpose |
|-------|---------|
| `AodTimeoutModule` | LSPosed entry point, hooks DozeMachine |
| `AodTimeoutHook` | Property management (root + reflection) |
| `AodTimeoutPreferences` | Settings persistence |
| `DeviceStateReceiver` | Monitors charging/dock/screen state |
| `MainActivity` | Dashboard UI showing status |
| `SettingsActivity` | Configuration UI |

### Xposed API Stubs (5 classes)
- `IXposedHookLoadPackage`
- `LoadPackageParam`
- `XC_MethodHook`
- `XposedBridge`
- `XposedHelpers`

---

## 🔨 Build Process

1. **Resource Compilation**: `aapt2 compile` → `.flat` files
2. **Resource Linking**: `aapt2 link` → R.java + linked.apk
3. **Java Compilation**: `javac 17` → 13 `.class` files
4. **DEX Conversion**: `d8` → `classes.dex` (15KB)
5. **APK Packaging**: Combined resources + DEX
6. **Signing**: `apksigner` with debug keystore
7. **Alignment**: `zipalign 4`

---

## 📱 Installation

### For Testing (with UI)
```bash
adb install -r dist/aod_timeout_module_v1.1_debug.apk
```

### For LSPosed Module
```bash
adb push dist/aod_timeout_module_v1.1_production.zip /sdcard/Download/
# Then enable in LSPosed Manager and reboot
```

---

## 🔗 Repository

**GitHub:** https://github.com/marufmoinuddin/aod-timeout-module

---

## ✅ Verification

- [x] All 13 classes compile without errors
- [x] APK is signed and zipaligned
- [x] R.java resource IDs match layout files
- [x] Module includes all required files
- [x] Built with Android SDK 34, min API 28

---

## 🎯 Features

- Hooks `DozeMachine.requestState()` in SystemUI
- Supports 3 timeout states:
  - `DOZE_AOD`: Default timeout (30s)
  - `DOZE_AOD_PAUSED`: Paused timeout (150s)
  - `DOZE_AOD_DOCKED`: Docked timeout (300s)
- Property setting via:
  - Root shell (`su -c setprop`)
  - Reflection (`SystemProperties.set()`)
- Settings UI for configuration
- Status dashboard showing hook/root state

---

**Ready for testing on Pixel 10!** 🚀
