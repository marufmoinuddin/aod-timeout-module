#!/usr/bin/env python3
"""Complete build pipeline for AOD Timeout Module."""
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
print("1. Cleaning...")
subprocess.run(["rm", "-rf", str(BUILD), str(DIST)], check=False)
for d in ["build/res", "build/java", "build/classes", "build/dex", "build/apk", "dist"]:
    (BASE / d).mkdir(parents=True, exist_ok=True)

# Resources
print("\n2. Compiling resources...")
run([f"{SDK}/build-tools/36.1.0/aapt2", "compile", "-o", str(BUILD/"res"), "--dir", str(BASE/"app/src/main/res")])

# Link
print("\n3. Linking manifest...")
flat_files = list((BUILD/"res").glob("*.flat"))
cmd = [f"{SDK}/build-tools/36.1.0/aapt2", "link", "-o", str(BUILD/"output.apk"),
       "--manifest", str(BASE/"app/src/main/AndroidManifest.xml"),
       "--java", str(BUILD/"java"),
       "-I", str(SDK/"platforms/android-34/android.jar")]
cmd.extend(str(f) for f in flat_files)
run(cmd)

# Compile Java
print("\n4. Compiling Java...")
sources = list((BASE/"app/src/main/java/de/robv/android/xposed").glob("*.java"))
sources.append(BUILD/"java/com/ambient/aodtimeout/R.java")
sources.extend(list((BASE/"app/src/main/java/com/ambient/aodtimeout").glob("*.java")))
run(["javac", "-source", "17", "-target", "17",
     "-cp", str(SDK/"platforms/android-34/android.jar"),
     "-d", str(BUILD/"classes")] + [str(s) for s in sources])

# DEX
print("\n5. Creating DEX...")
run(["jar", "cf", str(BUILD/"classes.jar"), "-C", str(BUILD/"classes"), "."])
run([f"{SDK}/build-tools/36.1.0/d8", "--lib", str(SDK/"platforms/android-34/android.jar"),
     "--output", str(BUILD/"dex"), str(BUILD/"classes.jar")])

# Build APK
print("\n6. Building APK...")
apk_build = BUILD / "apk"
run(["jar", "xf", str(BUILD/"output.apk")], cwd=apk_build)
final_apk = DIST / "aod_timeout_module_v1.1_debug.apk"
cmd = ["zip", "-j", str(final_apk)]
cmd.extend(str(f) for f in apk_build.rglob("*") if f.is_file())
cmd.extend([str(BUILD/"dex/classes.dex"), str(BASE/"module.prop"),
            str(BASE/"app/src/main/assets/xposed_init")])
run(cmd)

# Sign
print("\n7. Signing...")
run([f"{SDK}/build-tools/34.0.0/apksigner", "sign",
     "--min-sdk-version", "28",
     "--ks", str(BASE/"debug.keystore"),
     "--ks-key-alias", "androiddebugkey",
     "--ks-pass", "pass:android",
     "--key-pass", "pass:android",
     "--v1-signing-enabled", "--v2-signing-enabled", "false",
     "--out", str(final_apk), str(final_apk)])

# Package module
print("\n8. Packaging module...")
module_zip = DIST / "aod_timeout_module_v1.1_production.zip"
subprocess.run(["rm", "-f", str(module_zip)], check=False)
module_dir = BASE / "build/module_tmp"
module_dir.mkdir(exist_ok=True)
(module_dir / "module.prop").write_text(Path(BASE / "module.prop").read_text())
(module_dir / "xposed_init").write_text(Path(BASE / "app/src/main/assets/xposed_init").read_text())
module_lib = module_dir / "lib" / "x86_64"
module_lib.mkdir(parents=True)
(module_lib / "arm64-v8a.so").write_bytes(Path(BASE / "build/dex/classes.dex").read_bytes())
subprocess.run(["zip", "-r", str(module_zip), "."], cwd=module_dir, check=False)
subprocess.run(["rm", "-rf", str(module_dir)], check=False)

print(f"\n✓ Done! Outputs:")
print(f"  • {final_apk} ({final_apk.stat().st_size // 1024}KB)")
print(f"  • {module_zip} ({module_zip.stat().st_size // 1024}KB)")
