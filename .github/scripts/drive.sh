#!/usr/bin/env bash
# Installs Range on the emulator, walks through it and screenshots each stop.
# Taps resolve against the live UI hierarchy by label rather than guessing at
# screen percentages, so the walk survives layout changes.
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
  # A tiny grab means the compositor handed us a mid-animation buffer.
  if [ "$(stat -c%s "$SHOTS/$1.png" 2>/dev/null || echo 0)" -lt 40000 ]; then
    sleep 2
    adb exec-out screencap -p > "$SHOTS/$1.png" 2>/dev/null
  fi
  log "shot $1: $(stat -c%s "$SHOTS/$1.png" 2>/dev/null || echo 0) bytes"
}

dump_ui() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb shell cat /sdcard/ui.xml 2>/dev/null
}

tap_text() {
  local needle="$1"
  local coords
  coords=$(dump_ui | NEEDLE="$needle" python3 -c '
import os, re, sys
xml = sys.stdin.read()
needle = os.environ["NEEDLE"].lower()
for tag in re.findall(r"<node[^>]*>", xml):
    t = re.search(r"text=\"([^\"]*)\"", tag)
    d = re.search(r"content-desc=\"([^\"]*)\"", tag)
    hay = ((t.group(1) if t else "") + " " + (d.group(1) if d else "")).lower()
    if needle and needle in hay:
        b = re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", tag)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
')
  if [ -z "$coords" ]; then
    sleep 2
    coords=$(dump_ui | NEEDLE="$needle" python3 -c '
import os, re, sys
xml = sys.stdin.read()
needle = os.environ["NEEDLE"].lower()
for tag in re.findall(r"<node[^>]*>", xml):
    t = re.search(r"text=\"([^\"]*)\"", tag)
    d = re.search(r"content-desc=\"([^\"]*)\"", tag)
    hay = ((t.group(1) if t else "") + " " + (d.group(1) if d else "")).lower()
    if needle and needle in hay:
        b = re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", tag)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
')
  fi
  if [ -z "$coords" ]; then
    log "  tap '$needle': not found"
    return 1
  fi
  log "  tap '$needle' -> $coords"
  adb shell input tap $coords
  sleep "${2:-2}"
  return 0
}

on_screen() { dump_ui | grep -qiF "$1"; }

# Relaunching is the only reliable way back to a known screen — counting BACK
# presses walks out of the app the moment a screen is one level shallower than
# the script assumed, and every shot after that is of the launcher.
home_screen() {
  # force-stop first: a plain `am start` resumes the task wherever it was
  # left, so "go home" would silently land on whatever screen was already
  # open. Settings survive in DataStore, so a restart costs nothing.
  adb shell am force-stop "$PKG" >/dev/null 2>&1
  sleep 1
  adb shell am start -n "$PKG/.MainActivity" >/dev/null 2>&1
  wait_for_app || log "  app window never came back"
  sleep 3
  if on_screen "How much can you spend"; then
    log "  back at home"
  else
    log "  WARNING: not at home after relaunch"
  fi
}

in_app() { adb shell dumpsys window 2>/dev/null | grep -q "$PKG"; }

wait_for_app() {
  for _ in $(seq 1 12); do
    if in_app && dump_ui | grep -q "$PKG"; then return 0; fi
    sleep 2
  done
  return 1
}

swipe_up() { adb shell input swipe $((W/2)) $((H*78/100)) $((W/2)) $((H*22/100)) "${1:-280}"; sleep 1; }

adb wait-for-device
adb shell settings put global hide_error_dialogs 1

log "== device =="
SIZE=$(adb shell wm size | tr -d '\r')
W=$(echo "$SIZE" | sed 's/.*: //' | cut -dx -f1)
H=$(echo "$SIZE" | sed 's/.*: //' | cut -dx -f2)
log "$SIZE (w=$W h=$H)"

log "== install =="
adb install -r -t app/build/outputs/apk/debug/app-debug.apk 2>&1 | tail -2 | tee -a "$REPORT"

adb logcat -c
log "== launch =="
adb shell am start -W -n "$PKG/.MainActivity" 2>&1 | tail -4 | tee -a "$REPORT"
sleep 7
shot 01-onboarding 2

log "== onboarding =="
tap_text "Next" 2; shot 02-onboarding-2 2
tap_text "Next" 2; shot 03-onboarding-3 2
tap_text "Find my range" 3
shot 04-home 3
if on_screen "How much can you spend"; then log "  home: yes"; else log "  home: NO"; tap_text "Skip" 3; fi

log "== home =="
swipe_up; shot 05-home-when 2
swipe_up; shot 06-home-travel 2
swipe_up; shot 07-home-comfort 2

log "== explore =="
tap_text "places in reach" 5
shot 08-explore 4
if on_screen "YOUR RANGE FROM"; then log "  explore: yes"; else log "  explore: NO"; fi
swipe_up; shot 09-explore-list 2
swipe_up; shot 10-explore-list2 2

log "== detail =="
adb shell input tap $((W/2)) $((H*50/100)); sleep 3
shot 11-detail 3
if on_screen "TRIP TOTAL"; then log "  detail: yes"; else log "  detail: NO"; fi
swipe_up; shot 12-detail-breakdown 2
swipe_up; shot 13-detail-nights 2
swipe_up; shot 14-detail-transport 2

home_screen
shot 15-home-again 2

# Both themes, explicitly. The app defaults to following the system, so which
# theme a screenshot shows depends on the emulator rather than on the app —
# which is exactly how a broken light theme stayed invisible for days. Each
# pass now sets the theme by name first, so the evidence means something.
theme_pass() {
  local mode="$1" prefix="$2"
  log "== $mode theme pass =="
  home_screen
  tap_text "Settings" 3 || { log "  could not reach settings"; return 1; }
  if ! on_screen "STARTING CITY"; then
    log "  settings did not open"
    return 1
  fi
  tap_text "$mode" 3 || { log "  could not set $mode"; return 1; }
  home_screen
  shot "${prefix}-home" 3
  tap_text "places in reach" 5
  shot "${prefix}-explore" 4
  adb shell input tap $((W/2)) $((H*50/100))
  sleep 3
  shot "${prefix}-detail" 3
  home_screen
}

theme_pass "Dark" "21-dark"
theme_pass "Light" "24-light"

log "== crash check =="
adb logcat -d -b crash | grep -i "$PKG" -A 10 | tail -30 | tee -a "$REPORT"
log "== app errors =="
adb logcat -d "*:E" | grep -iE "$PKG" | tail -20 | tee -a "$REPORT"
log "== still running? =="
adb shell pidof "$PKG" | tee -a "$REPORT"
exit 0
