#!/bin/bash
# Package LSPosed module as ZIP
set -e
BASE="/home/maruf/workspace/aod-timeout-module"
DIST="$BASE/dist"
mkdir -p "$DIST"

# Clean
rm -f "$DIST/aod_timeout_module_v1.1_production.zip"

# Create module directory structure
MODULE_DIR=$(mktemp -d)
trap "rm -rf $MODULE_DIR" EXIT
mkdir -p "$MODULE_DIR/lib/x86_64"
cp "$BASE/module.prop" "$MODULE_DIR/"
cp "$BASE/app/src/main/assets/xposed_init" "$MODULE_DIR/"
cp "$BASE/build/classes.dex" "$MODULE_DIR/lib/x86_64/arm64-v8a.so" 2>/dev/null || \
  zip -j "$MODULE_DIR/lib/arm64-v8a.so" "$BASE/build/dex/classes.dex"

# Create ZIP
cd "$MODULE_DIR"
zip -r "$DIST/aod_timeout_module_v1.1_production.zip" . > /dev/null
cd "$BASE"

echo "✓ Production ZIP created: $(ls -lh $DIST/aod_timeout_module_v1.1_production.zip)"
