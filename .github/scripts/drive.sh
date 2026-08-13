#!/usr/bin/env bash
# Installs Range on the running emulator, walks through the app and screenshots
# each stop. Never fails the job — the report and the images are the output.
set +e

SHOTS=/tmp/shots
mkdir -p "$SHOTS"
REPORT="$SHOTS/report.txt"
PKG=com.vythera.range
: > "$REPORT"

log() { echo "$@" | tee -a "$REPORT"; }

shot() {
  sleep "${2:-2}"
  adb exec-out screencap -p > "$SHOTS/$1.png" 2>/dev/null
  local size
  size=$(stat -c%s "$SHOTS/$1.png" 2>/dev/null || echo 0)
  log "shot $1: ${size} bytes"
}

tap() { adb shell input tap "$1" "$2"; sleep "${3:-1}"; }
swipe() { adb shell input swipe "$1" "$2" "$3" "$4" "${5:-300}"; sleep 1; }

adb wait-for-device
adb shell settings put global window_animation_scale 1.0
adb shell settings put global transition_animation_scale 1.0
adb shell settings put global animator_duration_scale 1.0

log "== device =="
adb shell getprop ro.build.version.sdk | tee -a "$REPORT"
SIZE=$(adb shell wm size | tr -d '\r')
log "$SIZE"
W=$(echo "$SIZE" | sed 's/.*: //' | cut -dx -f1)
H=$(echo "$SIZE" | sed 's/.*: //' | cut -dx -f2)
log "w=$W h=$H"

log "== install =="
adb install -r -t app/build/outputs/apk/debug/app-debug.apk 2>&1 | tail -3 | tee -a "$REPORT"

adb logcat -c
log "== launch =="
adb shell am start -W -n "$PKG/.MainActivity" 2>&1 | tee -a "$REPORT"
sleep 6

shot 01-onboarding 2

# Onboarding: Next, Next, then pick an origin and start.
tap $((W/2)) $((H*88/100)) 1
shot 02-onboarding-2 2
tap $((W/2)) $((H*88/100)) 1
shot 03-onboarding-3 2
tap $((W/2)) $((H*88/100)) 2
shot 04-home 3

# Scroll the composer.
swipe $((W/2)) $((H*70/100)) $((W/2)) $((H*30/100)) 400
shot 05-home-mid 2
swipe $((W/2)) $((H*70/100)) $((W/2)) $((H*30/100)) 400
shot 06-home-lower 2
swipe $((W/2)) $((H*70/100)) $((W/2)) $((H*30/100)) 400
shot 07-home-bottom 2

# The CTA sits at the very bottom of the scroll.
swipe $((W/2)) $((H*70/100)) $((W/2)) $((H*20/100)) 400
sleep 1
shot 08-home-cta 2
tap $((W/2)) $((H*80/100)) 3
shot 09-explore 4

swipe $((W/2)) $((H*70/100)) $((W/2)) $((H*30/100)) 400
shot 10-explore-list 2
swipe $((W/2)) $((H*70/100)) $((W/2)) $((H*30/100)) 400
shot 11-explore-list2 2

# Open the first card.
tap $((W/2)) $((H*45/100)) 3
shot 12-detail 3
swipe $((W/2)) $((H*70/100)) $((W/2)) $((H*25/100)) 400
shot 13-detail-costs 2
swipe $((W/2)) $((H*70/100)) $((W/2)) $((H*25/100)) 400
shot 14-detail-transport 2
swipe $((W/2)) $((H*70/100)) $((W/2)) $((H*25/100)) 400
shot 15-detail-more 2

adb shell input keyevent KEYCODE_BACK
sleep 2
adb shell input keyevent KEYCODE_BACK
sleep 2
shot 16-back-home 2

log "== crash check =="
adb logcat -d -b crash | tail -60 | tee -a "$REPORT"
log "== app errors =="
adb logcat -d "*:E" | grep -iE "$PKG|AndroidRuntime|Compose" | tail -60 | tee -a "$REPORT"
log "== still running? =="
adb shell pidof "$PKG" | tee -a "$REPORT"

exit 0
