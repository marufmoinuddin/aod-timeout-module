#!/usr/bin/env python3
"""Build AOD Timeout Module with proper resource paths."""
import subprocess
from pathlib import Path

BASE = Path("/home/maruf/workspace/aod-timeout-module")
SDK = Path("/home/maruf/android-sdk")
BUILD = BASE / "build"
DIST = BASE / "dist"

def run(cmd, cwd=None):
    print(f"  $ {' '.join(str(c) for c in cmd[:4])}...")
    result = subprocess.run(cmd, cwd=cwd or BASE, capture_output=True, text=True)
    if result.returncode != 0 and result.stderr:
        print(f"  WARN: {result.stderr[:100]}")
    return result.returncode == 0

print("=== Building AOD Timeout Module v1.1 ===\n")

# Clean
for d in ["build/res", "build/java", "build/classes", "build/dex", "build/apk", "dist"]:
    (BASE / d).mkdir(parents=True, exist_ok=True)
subprocess.run(["rm", "-rf", str(BASE/"build/classes")], check=False)
subprocess.run(["rm", "-rf", str(BASE/"build/java")], check=False)
subprocess.run(["rm", "-f", str(DIST/"*.apk"), str(DIST/"*.zip")], check=False)

# Resources
print("\n1. Compiling resources...")
run([f"{SDK}/build-tools/36.1.0/aapt2", "compile", "-o", str(BUILD/"res"), "--dir", str(BASE/"app/src/main/res")])

# Link
print("\n2. Linking manifest...")
flat_files = list((BUILD/"res").glob("*.flat"))
cmd = [f"{SDK}/build-tools/36.1.0/aapt2", "link", "-o", str(BUILD/"output.apk"),
       "--manifest", str(BASE/"app/src/main/AndroidManifest.xml"),
       "--java", str(BUILD/"java"),
       "-I", str(SDK/"platforms/android-34/android.jar")]
cmd.extend(str(f) for f in flat_files)
run(cmd)

# Compile Java
print("\n3. Compiling Java...")
sources = list((BASE/"app/src/main/java/de/robv/android/xposed").glob("*.java"))
sources.append(BUILD/"java/com/ambient/aodtimeout/R.java")
sources.extend(list((BASE/"app/src/main/java/com/ambient/aodtimeout").glob("*.java")))
run(["javac", "-source", "17", "-target", "17",
     "-cp", str(SDK/"platforms/android-34/android.jar"),
     "-d", str(BUILD/"classes")] + [str(s) for s in sources])

# DEX
print("\n4. Creating DEX...")
run(["jar", "cf", str(BUILD/"classes.jar"), "-C", str(BUILD/"classes"), "."])
run([f"{SDK}/build-tools/36.1.0/d8", "--lib", str(SDK/"platforms/android-34/android.jar"),
     "--output", str(BUILD/"dex"), str(BUILD/"classes.jar")])

# Build APK - keep resource directory structure
print("\n5. Building APK...")
apk_build = BUILD / "apk"
run(["jar", "xf", str(BUILD/"output.apk")], cwd=apk_build)

final_apk = DIST / "aod_timeout_module_v1.1_debug.apk"
final_apk.unlink(missing_ok=True)

# Copy DEX and other files into the APK structure
subprocess.run(["cp", str(BUILD/"dex/classes.dex"), str(apk_build/"classes.dex")], check=True)
subprocess.run(["cp", str(BASE/"module.prop"), str(apk_build/"module.prop")], check=True)
subprocess.run(["cp", str(BASE/"app/src/main/assets/xposed_init"), str(apk_build/"xposed_init")], check=True)

# Create APK preserving structure (use absolute path for output)
run(["zip", "-r", "-0", str(final_apk), "."], cwd=apk_build)

# Sign
print("\n6. Signing...")
run([f"{SDK}/build-tools/34.0.0/apksigner", "sign",
     "--min-sdk-version", "28",
     "--ks", str(BASE/"debug.keystore"),
     "--ks-key-alias", "androiddebugkey",
     "--ks-pass", "pass:android",
     "--key-pass", "pass:android",
     "--v1-signing-enabled", "--v2-signing-enabled", "false",
     "--out", str(final_apk), str(final_apk)])

# Package module
print("\n7. Packaging module...")
module_zip = DIST / "aod_timeout_module_v1.1_production.zip"
module_dir = BASE / "build/module_tmp"
module_dir.mkdir(exist_ok=True)
module_lib = module_dir / "lib" / "x86_64"
module_lib.mkdir(parents=True)
subprocess.run(["cp", str(BASE/"module.prop"), str(module_dir/"module.prop")], check=True)
subprocess.run(["cp", str(BASE/"app/src/main/assets/xposed_init"), str(module_dir/"xposed_init")], check=True)
subprocess.run(["cp", str(BUILD/"dex/classes.dex"), str(module_lib/"arm64-v8a.so")], check=True)
subprocess.run(["zip", "-r", str(module_zip), "."], cwd=module_dir, check=False)
subprocess.run(["rm", "-rf", str(module_dir)], check=False)

print(f"\n✓ Done! Outputs:")
print(f"  • {final_apk} ({final_apk.stat().st_size // 1024}KB)")
print(f"  • {module_zip} ({module_zip.stat().st_size // 1024}KB)")
