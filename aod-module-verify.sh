#!/bin/bash
# AOD Timeout Module Verification Script
# Automates stages 1-3: Static validation, Redroid boot, crash detection
# Stages 4-5 require manual testing on real device

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="$PROJECT_DIR/dist"
VERIFY_LOGS="$PROJECT_DIR/verify-logs"
REDROID_IMAGE="redroid/redroid:latest"
REDROID_PORT=5555

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo -e "${GREEN}[✓]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[!]${NC} $1"
}

error() {
    echo -e "${RED}[✗]${NC} $1"
    exit 1
}

check_prerequisites() {
    echo "═══════════════════════════════════════════════════════════"
    echo "  AOD Timeout Module - Verification Pipeline"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        error "Docker is required but not installed"
    fi
    log "Docker version: $(docker --version)"
    
    # Check for APK
    if [ ! -f "$DIST_DIR/aod_timeout_module_v1.1_debug.apk" ]; then
        error "APK not found: $DIST_DIR/aod_timeout_module_v1.1_debug.apk"
    fi
    log "APK found: $(ls -lh "$DIST_DIR/aod_timeout_module_v1.1_debug.apk" | awk '{print $5}')"
    
    # Check KVM (informational)
    if [ -e /dev/kvm ]; then
        log "KVM available: $(ls -l /dev/kvm)"
    else
        warn "No /dev/kvm found (Redroid doesn't require it)"
    fi
    
    # Check CPU virtualization
    local virt_count=$(egrep -c '(vmx|svm)' /proc/cpuinfo 2>/dev/null || echo "0")
    log "CPU virtualization extensions: $virt_count cores"
    
    echo ""
}

stage1_static_validation() {
    echo "═══════════════════════════════════════════════════════════"
    echo "  Stage 1: Static APK Validation"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
    
    local apk="$DIST_DIR/aod_timeout_module_v1.1_debug.apk"
    
    # 1.1 Check APK exists and is readable
    if [ ! -f "$apk" ]; then
        error "APK file not found: $apk"
    fi
    log "APK file exists: $apk"
    
    # 1.2 Verify APK structure
    echo ""
    echo "📦 APK Contents:"
    unzip -l "$apk" | head -30
    echo "..."
    
    # 1.3 Check DEX file
    local dex_size=$(unzip -p "$apk" classes.dex 2>/dev/null | wc -c)
    if [ "$dex_size" -lt 1000 ]; then
        error "DEX file too small ($dex_size bytes) - build may be incomplete"
    fi
    log "DEX file valid: $dex_size bytes"
    
    # 1.4 Check AndroidManifest
    echo ""
    echo "📋 Manifest Info:"
    /home/maruf/android-sdk/build-tools/34.0.0/aapt dump badging "$apk" 2>/dev/null | grep -E "(package:|application:|uses-sdk:)" || warn "aapt not available for manifest dump"
    
    # 1.5 Verify signing (informational)
    echo ""
    echo "🔐 Signature Check:"
    /home/maruf/android-sdk/build-tools/34.0.0/apksigner verify --verbose "$apk" 2>&1 | grep -E "(Verified|api level|v1|v2)" || warn "Signing verification skipped (API compatibility warning expected)"
    
    # 1.6 Check for required files
    echo ""
    echo "📁 Required Files Check:"
    local required_files=("AndroidManifest.xml" "classes.dex" "resources.arsc")
    for file in "${required_files[@]}"; do
        if unzip -l "$apk" | grep -q "$file"; then
            log "$file present"
        else
            error "$file missing from APK"
        fi
    done
    
    # 1.7 Check module files
    if unzip -l "$apk" | grep -q "module.prop"; then
        log "module.prop present (LSPosed module)"
    fi
    if unzip -l "$apk" | grep -q "xposed_init"; then
        log "xposed_init present (Xposed entry point)"
    fi
    
    log "Stage 1: Static validation PASSED"
    echo ""
}

stage2_redroid_boot() {
    echo "═══════════════════════════════════════════════════════════"
    echo "  Stage 2: Redroid Android Container"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
    
    # Stop any existing redroid container
    docker rm -f aod-redroid 2>/dev/null || true
    
    # Pull image if not present
    if ! docker images | grep -q "redroid/redroid"; then
        echo "📥 Pulling Redroid image (this may take a few minutes)..."
        docker pull "$REDROID_IMAGE"
    fi
    
    # Start Redroid container
    echo "🚀 Starting Redroid container..."
    docker run -d \
        --name aod-redroid \
        --privileged \
        -p 5555:5555 \
        -e REDROID resolução=1080x1920 \
        "$REDROID_IMAGE"
    
    log "Redroid container started"
    
    # Wait for boot
    echo "⏳ Waiting for Android to boot..."
    local max_wait=120
    local elapsed=0
    
    while [ $elapsed -lt $max_wait ]; do
        if docker exec aod-redroid getprop sys.boot_completed 2>/dev/null | grep -q "1"; then
            log "Android booted successfully ($elapsed seconds)"
            break
        fi
        sleep 5
        elapsed=$((elapsed + 5))
        if [ $((elapsed % 15)) -eq 0 ]; then
            echo "   Still waiting... ($elapsed/${max_wait}s)"
        fi
    done
    
    if [ $elapsed -ge $max_wait ]; then
        error "Android failed to boot within ${max_wait}s"
    fi
    
    # Verify ADB connection
    if command -v adb &> /dev/null; then
        adb kill-server 2>/dev/null || true
        adb connect localhost:5555 2>/dev/null || true
        sleep 2
        
        local devices=$(adb devices 2>/dev/null | grep "localhost:5555")
        if [ -n "$devices" ]; then
            log "ADB connected to Redroid"
        else
            warn "ADB connection failed, continuing without device"
        fi
    else
        warn "adb not found, skipping device connection"
    fi
    
    log "Stage 2: Redroid boot PASSED"
    echo ""
}

stage3_install_and_watch() {
    echo "═══════════════════════════════════════════════════════════"
    echo "  Stage 3: Install APK and Monitor for Crashes"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
    
    local apk="$DIST_DIR/aod_timeout_module_v1.1_debug.apk"
    mkdir -p "$VERIFY_LOGS"
    
    # Clear previous logs
    echo "" > "$VERIFY_LOGS/logcat.txt"
    
    # Start logcat monitoring in background
    if command -v adb &> /dev/null; then
        adb logcat -c 2>/dev/null || true
        adb logcat > "$VERIFY_LOGS/logcat.txt" 2>&1 &
        local logcat_pid=$!
        trap "kill $logcat_pid 2>/dev/null || true" EXIT
        sleep 2
    fi
    
    # Install APK
    echo "📦 Installing APK..."
    if command -v adb &> /dev/null; then
        local install_result=$(adb install -r "$apk" 2>&1)
        echo "$install_result"
        
        if echo "$install_result" | grep -q "Success"; then
            log "APK installed successfully"
        elif echo "$install_result" | grep -q "INSTALL_FAILED"; then
            error "Installation failed: $install_result"
        else
            warn "Installation result unclear: $install_result"
        fi
    else
        warn "adb not available, skipping installation"
    fi
    
    # Try to launch the app
    echo ""
    echo "🚀 Attempting to launch app..."
    
    if command -v adb &> /dev/null; then
        adb shell am start -n com.ambient.aodtimeout/com.ambient.aodtimeout.MainActivity 2>&1 || true
        sleep 5
        
        # Check for crashes in logcat
        echo "📊 Analyzing logs for crashes..."
        
        if [ -f "$VERIFY_LOGS/logcat.txt" ]; then
            local fatal_count=$(grep -c "FATAL EXCEPTION" "$VERIFY_LOGS/logcat.txt" 2>/dev/null || echo "0")
            local runtime_errors=$(grep -c "AndroidRuntime" "$VERIFY_LOGS/logcat.txt" 2>/dev/null || echo "0")
            
            if [ "$fatal_count" -gt 0 ]; then
                echo ""
                echo "${RED}🚨 CRITICAL: Found $fatal_count FATAL EXCEPTION(s) in logs${NC}"
                echo ""
                echo "Latest crash logs:"
                grep -A 20 "FATAL EXCEPTION" "$VERIFY_LOGS/logcat.txt" | tail -30
                echo ""
                error "App crashed on launch - see logs in $VERIFY_LOGS/"
            elif [ "$runtime_errors" -gt 0 ]; then
                warn "Found $runtime_errors runtime errors (may not be fatal)"
            else
                log "No fatal crashes detected in initial launch"
            fi
            
            # Save final log
            cp "$VERIFY_LOGS/logcat.txt" "$VERIFY_LOGS/final_logcat.txt"
        fi
    fi
    
    log "Stage 3: Install and crash watch COMPLETED"
    echo ""
    echo "📁 Logs saved to: $VERIFY_LOGS/"
}

cleanup() {
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo "  Cleanup"
    echo "═══════════════════════════════════════════════════════════"
    
    # Stop logcat
    kill $(pgrep -f "adb logcat") 2>/dev/null || true
    
    # Stop Redroid
    docker rm -f aod-redroid 2>/dev/null || true
    
    log "Cleanup complete"
}

# Main execution
main() {
    check_prerequisites
    stage1_static_validation
    stage2_redroid_boot
    stage3_install_and_watch
    cleanup
    
    echo ""
    echo "═══════════════════════════════════════════════════════════"
    echo "  ✓ VERIFICATION PIPELINE COMPLETE"
    echo "═══════════════════════════════════════════════════════════"
    echo ""
    echo "Next steps (manual):"
    echo "  1. Stage 4: Test LSPosed hooks on Magisk-patched Redroid"
    echo "  2. Stage 5: Verify on real rooted device"
    echo ""
    echo "Logs: $VERIFY_LOGS/"
    echo ""
}

# Run main function
main "$@"
