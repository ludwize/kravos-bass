#!/usr/bin/env bash
# Installs the APK on the emulator, walks every screen, records screenshots + crashes.
set -u
OUT=smoke
mkdir -p "$OUT"
APK=$(ls out/*.apk | head -1)
PKG=net.kravos.bass

adb install -r "$APK" > "$OUT/install.txt" 2>&1
adb logcat -c
adb shell am start -W -n $PKG/.ui.MainActivity > "$OUT/start.txt" 2>&1
sleep 8

shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1" >> "$OUT/steps.txt"; }

find_xy() {
  adb shell rm -f /sdcard/ui.xml >/dev/null 2>&1
  adb shell uiautomator dump /sdcard/ui.xml >> "$OUT/dump.txt" 2>&1
  adb pull /sdcard/ui.xml "$OUT/ui.xml" >> "$OUT/dump.txt" 2>&1
  cp "$OUT/ui.xml" "$OUT/last-ui.xml" 2>/dev/null
  python3 - "$1" "$OUT/ui.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
want, path = sys.argv[1], sys.argv[2]
try:
    root = ET.parse(path).getroot()
except Exception:
    sys.exit(0)
for n in root.iter('node'):
    t = n.get('text') or ''
    d = n.get('content-desc') or ''
    if t == want or d == want or (want.endswith('*') and t.startswith(want[:-1])):
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
}

tap_text() {
  XY=$(find_xy "$1")
  if [ -n "$XY" ]; then
    adb shell input tap $XY
    echo "tapped $1" >> "$OUT/steps.txt"
  else
    echo "not found: $1" >> "$OUT/steps.txt"
  fi
  sleep 2
}

SIZE=$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1)
W=${SIZE%x*}; H=${SIZE#*x}
scroll() { adb shell input swipe $((W / 2)) $((H * 3 / 4)) $((W / 2)) $((H / 4)) 400; sleep 2; }

shot 01-lessons
scroll; shot 02-lessons-scroll

# open first lesson
tap_text "Prirodne note na E i A žici"
shot 10-lesson1
scroll; shot 11-lesson1-scroll
scroll; shot 12-lesson1-scroll2
tap_text "Proveri se (8 pitanja)"
sleep 2
shot 13-quiz
# answer a choice question by tapping the first answer button area (third quarter of the screen)
tap_text "C"
shot 14-quiz-answered
adb shell input keyevent KEYCODE_BACK; sleep 2
adb shell input keyevent KEYCODE_BACK; sleep 2

# chords lesson with boards
scroll; scroll
tap_text "Trozvuk: koren, terca, kvinta"
shot 15-lesson-chords
scroll; shot 16-lesson-chords-scroll
adb shell input keyevent KEYCODE_BACK; sleep 2

tap_text "Vežbe"
shot 20-practice
tap_text "Intervali na vratu"
sleep 2
shot 21-interval-tap
# tap somewhere on the neck (upper third of the screen area where the board is)
XY=$(find_xy "Tapni na vrat.")
if [ -n "$XY" ]; then
  Y=${XY#* }
  adb shell input tap $((W / 2)) $((Y - 150))
  sleep 2
fi
shot 22-interval-answered
adb shell input keyevent KEYCODE_BACK; sleep 2
tap_text "Tonovi akorda na vratu"
shot 23-chordtap
adb shell input keyevent KEYCODE_BACK; sleep 2
tap_text "Sigurna nota"
shot 24-safe-note
adb shell input keyevent KEYCODE_BACK; sleep 2

tap_text "Akordi"
shot 30-chords
tap_text "7"
shot 31-chords-a7
scroll; shot 32-chords-scroll

tap_text "Tonalitet"
shot 40-key
tap_text "Am"
shot 41-key-am
scroll; shot 42-key-scroll
scroll; shot 43-key-scroll2

tap_text "Džem"
shot 50-jam
tap_text "Sviraj"
sleep 4
shot 51-jam-playing
sleep 3
shot 52-jam-playing2
scroll; shot 53-jam-scroll
scroll
tap_text "Stop"
shot 54-jam-stopped

tap_text "Podešavanja"
shot 60-settings
adb shell input keyevent KEYCODE_BACK; sleep 2

PID=$(adb shell pidof $PKG | tr -d '\r')
echo "pid=$PID" > "$OUT/status.txt"
adb logcat -d > "$OUT/logcat.txt"
grep -n -A 40 "FATAL EXCEPTION" "$OUT/logcat.txt" > "$OUT/crash.txt" || true
grep -n -E "AndroidRuntime|net.kravos|AudioTrack" "$OUT/logcat.txt" | tail -300 > "$OUT/app-log.txt" || true
if [ -s "$OUT/crash.txt" ]; then echo "CRASH" >> "$OUT/status.txt"; fi
rm -f "$OUT/logcat.txt"
cat "$OUT/status.txt" "$OUT/steps.txt"
exit 0
