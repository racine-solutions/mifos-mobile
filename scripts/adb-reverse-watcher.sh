#!/usr/bin/env bash
#
# Keeps `adb reverse tcp:8443 tcp:8443` applied for the local Fineract dev
# instance, no matter how many times the Android emulator restarts.
#
# adb reverse rules are tied to the current adb server session and are
# dropped on every emulator/adb restart, so the app's `https://localhost:8443`
# base URL stops reaching the host-machine Fineract instance until the rule
# is re-applied. This script polls for a connected device and re-applies the
# rule whenever one (re)connects.
#
# Run directly, or install as a systemd --user service (see
# scripts/adb-reverse-watcher.service).

set -uo pipefail

ADB="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}/platform-tools/adb"
PORT="${MIFOS_DEV_FINERACT_PORT:-8443}"

log() {
    printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

if [[ ! -x "$ADB" ]]; then
    log "adb not found at $ADB (set ANDROID_HOME/ANDROID_SDK_ROOT if it's elsewhere)"
    exit 1
fi

log "Watching for emulator/device connections to auto-apply adb reverse tcp:$PORT (adb: $ADB)"

last_state=""
while true; do
    state=$("$ADB" get-state 2>/dev/null)

    if [[ "$state" == "device" && "$last_state" != "device" ]]; then
        log "Device connected — applying adb reverse tcp:$PORT tcp:$PORT"
        if "$ADB" reverse "tcp:$PORT" "tcp:$PORT"; then
            log "adb reverse applied"
        else
            log "adb reverse FAILED"
        fi
    elif [[ "$state" != "device" && "$last_state" == "device" ]]; then
        log "Device disconnected"
    fi

    last_state="$state"
    sleep 2
done
