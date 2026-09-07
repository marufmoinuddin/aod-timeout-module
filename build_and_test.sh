# Build script for AOD Timeout Module v1.1
# Uses standalone Android build tools (no Gradle dependency)

set -e

ANDROID_SDK="/home/maruf/android-sdk"
PLATFORM="$ANDROID_SDK/platforms/android-34"
BUILD_TOOLS="$ANDROID_SDK/build-tools/34.0.0"
PROJECT_DIR="/home/maruf/workspace/aod-timeout-module"
DIST_DIR="$PROJECT_DIR/dist"

# Clean previous builds
rm -rf "$PROJECT_DIR/build"
mkdir -p "$PROJECT_DIR/build/classes"
mkdir -p "$DIST_DIR"

echo "=== Compiling Java source ==="
javac -source 17 -target 17 \
    -cp "$PLATFORM/android.jar" \
    -d "$PROJECT_DIR/build/classes" \
    "$PROJECT_DIR/app/src/main/java/com/ambient/aodtimeout"/*.java

echo "=== Compiling stubs ==="
javac -source 17 -target 17 \
    -cp "$PLATFORM/android.jar" \
    -d "$PROJECT_DIR/build/classes" \
    "$PROJECT_DIR/app/src/main/java/de/robv/android/xposed/Stubs.java" 2>/dev/null || true

echo "=== Compiling resources with aapt2 ==="
$BUILD_TOOLS/aapt2 compile \
    --dir "$PROJECT_DIR/app/src/main/res" \
    -o "$PROJECT_DIR/build/resources.aapt"

echo "=== Linking resources ==="
$BUILD_TOOLS/aapt2 link \
    -o "$PROJECT_DIR/build/resources.ap_" \
    --manifest "$PROJECT_DIR/app/src/main/AndroidManifest.xml" \
    -I "$PLATFORM/android.jar" \
    "$PROJECT_DIR/build/resources.aapt" \
    --target-sdk 34

echo "=== Converting classes to DEX ==="
$BUILD_TOOLS/d8 \
    --lib "$PLATFORM/android.jar" \
    --output "$PROJECT_DIR/build/dex" \
    "$PROJECT_DIR/build/classes"

echo "=== Creating APK ==="
$BUILD_TOOLS/aapt2 link \
    -o "$PROJECT_DIR/build/unaligned.apk" \
    --manifest "$PROJECT_DIR/app/src/main/AndroidManifest.xml" \
    -I "$PLATFORM/android.jar" \
    -F "$PROJECT_DIR/build/resources.ap_"\
    --target-sdk 34 \
    --custom-package com.ambient.aodtimeout

# Add DEX to APK
zip -j "$PROJECT_DIR/build/unaligned.apk" "$PROJECT_DIR/build/dex/classes.dex"

# Add assets
if [ -f "$PROJECT_DIR/app/src/main/assets/xposed_init" ]; then
    zip -j "$PROJECT_DIR/build/unaligned.apk" "$PROJECT_DIR/app/src/main/assets/xposed_init"
fi

echo "=== Signing APK ==="
# Self-sign with debug key
keytool -genkeypair -alias debug -keyalg RSA -keysize 2048 -validity 10000 \
    -keystore "$PROJECT_DIR/debug.keystore" \
    -storepass android -keypass android \
    -dname "CN=Debug, OU=Debug, O=Debug, L=Debug, ST=Debug, C=US" 2>/dev/null || true

$BUILD_TOOLS/apksigner sign \
    --ks "$PROJECT_DIR/debug.keystore" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$DIST_DIR/aod_timeout_module_v1.1_debug.apk" \
    "$PROJECT_DIR/build/unaligned.apk"

echo "=== Aligning APK ==="
$BUILD_TOOLS/zipalign -v 4 "$DIST_DIR/aod_timeout_module_v1.1_debug.apk" "$DIST_DIR/aod_timeout_module_v1.1_debug_aligned.apk"
mv "$DIST_DIR/aod_timeout_module_v1.1_debug_aligned.apk" "$DIST_DIR/aod_timeout_module_v1.1_debug.apk"

# Verify
$BUILD_TOOLS/apksigner verify --verbose "$DIST_DIR/aod_timeout_module_v1.1_debug.apk"

echo ""
echo "=== Build Complete ==="
echo "Debug APK: $DIST_DIR/aod_timeout_module_v1.1_debug.apk"
ls -lh "$DIST_DIR/aod_timeout_module_v1.1_debug.apk"