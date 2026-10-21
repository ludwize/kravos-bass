#!/usr/bin/env bash
# Installs the APK on the emulator, opens every screen via the "open" intent extra, records screenshots + crashes.
set -u
OUT=smoke
mkdir -p "$OUT"
APK=$(ls out/*.apk | head -1)
PKG=net.kravos.bass
ACT=$PKG/.ui.MainActivity

adb install -r "$APK" > "$OUT/install.txt" 2>&1
adb logcat -c
adb shell am start -W -n $ACT > "$OUT/start.txt" 2>&1
sleep 8

SIZE=$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1)
W=${SIZE%x*}; H=${SIZE#*x}
echo "size $W x $H" > "$OUT/steps.txt"
shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1" >> "$OUT/steps.txt"; }
open() { adb shell am start -n $ACT --es open "$1" >/dev/null 2>&1; echo "open $1" >> "$OUT/steps.txt"; sleep 3; }
scroll() { adb shell input swipe $((W / 2)) $((H * 3 / 4)) $((W / 2)) $((H / 4)) 400; sleep 2; }
# tap at fraction of the screen (per-mille)
tapf() { adb shell input tap $((W * $1 / 1000)) $((H * $2 / 1000)); sleep 2; }

shot 01-lessons
scroll; shot 02-lessons-scroll

open lesson:neck1
shot 10-lesson1
scroll; shot 11-lesson1-scroll
scroll; shot 12-lesson1-scroll2

open lesson:ch1
shot 15-lesson-chords
scroll; shot 16-lesson-chords-scroll
open lesson:imp3
scroll; shot 17-lesson-penta

open tab:1
shot 20-practice

open quiz:NOTE_NAME
shot 21-note-name
tapf 160 640
shot 22-note-name-answered

open quiz:INTERVAL_TAP
shot 23-interval-tap
# tap middle of the neck rows (neck sits in the upper half)
tapf 500 400
shot 24-interval-answered

open quiz:CHORD_TAP
shot 25-chordtap
open quiz:SAFE_NOTE
shot 26-safe-note
tapf 250 560
shot 27-safe-answered
open quiz:KEY_FUNCTION
shot 28-key-function

open tab:2
shot 30-chords
scroll; shot 31-chords-scroll

open tab:3
shot 40-key
scroll; shot 41-key-scroll
scroll; shot 42-key-scroll2

open jam:play
sleep 3
shot 50-jam-playing
sleep 3
shot 51-jam-playing2
scroll; shot 52-jam-scroll
scroll; shot 53-jam-scroll2
open tab:0

PID=$(adb shell pidof $PKG | tr -d '\r')
echo "pid=$PID" > "$OUT/status.txt"
adb logcat -d > "$OUT/logcat.txt"
grep -n -A 40 "FATAL EXCEPTION" "$OUT/logcat.txt" > "$OUT/crash.txt" || true
grep -n -E "net.kravos|AudioTrack" "$OUT/logcat.txt" | tail -300 > "$OUT/app-log.txt" || true
if [ -s "$OUT/crash.txt" ]; then echo "CRASH" >> "$OUT/status.txt"; fi
rm -f "$OUT/logcat.txt"
cat "$OUT/status.txt" "$OUT/steps.txt"
exit 0
