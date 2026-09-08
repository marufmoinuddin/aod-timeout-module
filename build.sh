#!/bin/bash
set -e

# AOD Timeout Module Build Script
# Uses aapt2 pipeline for proper resource compilation

ANDROID_SDK=/home/maruf/android-sdk
AAPT2=$ANDROID_SDK/build-tools/36.1.0/aapt2
D8=$ANDROID_SDK/build-tools/36.1.0/d8
APKSIGNER=$ANDROID_SDK/build-tools/34.0.0/apksigner
JAVA=/usr/bin/javac

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
DIST_DIR="$PROJECT_DIR/dist"

# Clean build
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"/{res,java,classes,dex,final}

echo "=== Compiling Resources ==="
$AAPT2 compile -o "$BUILD_DIR/res" --dir app/src/main/res

echo "=== Linking Resources ==="
$AAPT2 link -o "$BUILD_DIR/linked.apk" \
    -I $ANDROID_SDK/platforms/android-34/android.jar \
    --manifest app/src/main/AndroidManifest.xml \
    --java "$BUILD_DIR/java" \
    "$BUILD_DIR/res"/*.flat

echo "=== Compiling Java Sources ==="
rm -rf "$BUILD_DIR/classes"
mkdir -p "$BUILD_DIR/classes"
$JAVA -source 17 -target 17 \
    -cp "$ANDROID_SDK/platforms/android-34/android.jar" \
    -d "$BUILD_DIR/classes" \
    app/src/main/java/de/robv/android/xposed/*.java \
    "$BUILD_DIR/java"/com/ambient/aodtimeout/R.java \
    app/src/main/java/com/ambient/aodtimeout/AodTimeoutModule.java \
    app/src/main/java/com/ambient/aodtimeout/MainActivity.java \
    app/src/main/java/com/ambient/aodtimeout/SettingsActivity.java

echo "=== Creating DEX ==="
jar cf "$BUILD_DIR/classes.jar" -C "$BUILD_DIR/classes" .
$D8 --lib $ANDROID_SDK/platforms/android-34/android.jar \
    --output "$BUILD_DIR/dex" \
    "$BUILD_DIR/classes.jar"

echo "=== Extracting Linked APK ==="
cd "$BUILD_DIR"
jar xf linked.apk
rm linked.apk

echo "=== Creating Final APK ==="
cd "$BUILD_DIR/final"
zip -r "$DIST_DIR/aod_timeout_module_v1.1_debug.apk" \
    AndroidManifest.xml \
    res/ \
    resources.arsc \
    ../dex/classes.dex \
    ../../module.prop \
    ../../app/src/main/assets/xposed_init

echo "=== Signing APK ==="
$APKSIGNER sign --min-sdk-version 28 \
    --ks "$PROJECT_DIR/debug.keystore" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$DIST_DIR/aod_timeout_module_v1.1_debug_signed.apk" \
    "$DIST_DIR/aod_timeout_module_v1.1_debug.apk"

mv "$DIST_DIR/aod_timeout_module_v1.1_debug_signed.apk" "$DIST_DIR/aod_timeout_module_v1.1_debug.apk"

echo "=== Creating Production Module ==="
mkdir -p "$DIST_DIR"
cd "$BUILD_DIR/classes"
zip -r "$DIST_DIR/aod_timeout_module_v1.1_production.zip" .
zip "$DIST_DIR/aod_timeout_module_v1.1_production.zip" \
    ../../module.prop \
    ../../app/src/main/assets/xposed_init

echo ""
echo "✅ Build Complete!"
echo ""
echo "Debug APK: $(du -h "$DIST_DIR/aod_timeout_module_v1.1_debug.apk" | cut -f1)"
echo "Production ZIP: $(du -h "$DIST_DIR/aod_timeout_module_v1.1_production.zip" | cut -f1)"
echo ""
echo "Install:"
echo "  adb install -r $DIST_DIR/aod_timeout_module_v1.1_debug.apk"
echo "  # or for LSPosed:"
echo "  adb push $DIST_DIR/aod_timeout_module_v1.1_production.zip /sdcard/Download/"
