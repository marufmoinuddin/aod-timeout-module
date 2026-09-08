#!/usr/bin/env python3
"""Build AOD Timeout Module APK with proper resource bundling."""

import subprocess
from pathlib import Path

BASE = Path("/home/maruf/workspace/aod-timeout-module")
SDK = Path("/home/maruf/android-sdk")
BUILD = BASE / "build"
DIST = BASE / "dist"

def run(cmd, cwd=None, check=True):
    print(f">>> {' '.join(str(c) for c in cmd[:4])}...")
    result = subprocess.run(cmd, cwd=cwd or BASE, check=check, capture_output=True, text=True)
    if result.stdout:
        print(f"  {result.stdout.strip()}")
    if result.stderr and "warning" not in result.stderr.lower():
        print(f"  ERR: {result.stderr.strip()[:200]}")
    return result

# Setup - use parents=True for nested dirs
print("=== Setting up build directories ===")
for d in ["build/res", "build/java", "build/classes", "build/dex", "build/apk", "dist"]:
    (BASE / d).mkdir(parents=True, exist_ok=True)

# Compile resources
print("\n=== Compiling resources ===")
run([f"{SDK}/build-tools/36.1.0/aapt2", "compile", "-o", str(BUILD/"res"), "--dir", str(BASE/"app/src/main/res")])

# Get flat files
flat_files = list((BUILD/"res").glob("*.flat"))
print(f"Found {len(flat_files)} compiled resources")

# Link manifest and resources
print("\n=== Linking resources ===")
cmd = [f"{SDK}/build-tools/36.1.0/aapt2", "link", "-o", str(BUILD/"output.apk"),
     "--manifest", str(BASE/"app/src/main/AndroidManifest.xml"),
     "--java", str(BUILD/"java"),
     "-I", str(SDK/"platforms/android-34/android.jar")]
cmd.extend(str(f) for f in flat_files)
run(cmd)

# Compile Java sources
print("\n=== Compiling Java ===")
sources = []
sources.extend(list((BASE/"app/src/main/java/de/robv/android/xposed").glob("*.java")))
sources.append(BUILD/"java/com/ambient/aodtimeout/R.java")
sources.extend(list((BASE/"app/src/main/java/com/ambient/aodtimeout").glob("*.java")))

cmd = ["javac", "-source", "17", "-target", "17",
       "-cp", str(SDK/"platforms/android-34/android.jar"),
       "-d", str(BUILD/"classes")] + [str(s) for s in sources]
run(cmd)

# Create DEX
print("\n=== Creating DEX ===")
run(["jar", "cf", str(BUILD/"classes.jar"), "-C", str(BUILD/"classes"), "."])
run([f"{SDK}/build-tools/36.1.0/d8", "--lib", str(SDK/"platforms/android-34/android.jar"),
     "--output", str(BUILD/"dex"), str(BUILD/"classes.jar")])

# Extract linked APK and add components
print("\n=== Building final APK ===")
apk_build = BUILD / "apk"
run(["jar", "xf", str(BUILD/"output.apk")], cwd=apk_build)

# Build final APK
final_apk = DIST / "aod_timeout_module_v1.1_debug.apk"
cmd = ["zip", "-j", str(final_apk)]
cmd.extend(str(f) for f in apk_build.rglob("*") if f.is_file())
cmd.extend([str(BUILD/"dex/classes.dex"), str(BASE/"module.prop"), 
            str(BASE/"app/src/main/assets/xposed_init")])
run(cmd)

# Sign
print("\n=== Signing APK ===")
run([f"{SDK}/build-tools/34.0.0/apksigner", "sign",
     "--min-sdk-version", "28",
     "--ks", str(BASE/"debug.keystore"),
     "--ks-key-alias", "androiddebugkey",
     "--ks-pass", "pass:android",
     "--key-pass", "pass:android",
     "--v1-signing-enabled", "--v2-signing-enabled", "false",
     "--out", str(final_apk), str(final_apk)])

# Zipalign
print("\n=== Zipaligning ===")
aligned = DIST / "aod_timeout_module_v1.1_debug_aligned.apk"
run([f"{SDK}/build-tools/34.0.0/zipalign", "-v", "4", str(final_apk), str(aligned)])
aligned.rename(final_apk)

size_kb = final_apk.stat().st_size // 1024
print(f"\n✓ Built: {final_apk.name} ({size_kb}KB)")
