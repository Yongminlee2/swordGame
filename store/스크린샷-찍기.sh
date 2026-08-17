#!/bin/bash
# 스토어용 스크린샷을 13개 언어 × 6화면으로 찍는다.
# 세이브·언어를 파일로 바꿔 넣으므로 디버그 빌드가 깔려 있어야 한다.
export PATH="$PATH:/c/Users/사용자/AppData/Local/Android/Sdk/platform-tools"
SP="/c/Users/사용자/AppData/Local/Temp/claude/C--workAndroid-app/1e4212e7-1819-4c63-ab06-58423f42256d/scratchpad"
OUT="/c/workAndroid/SwordForge/store/screenshots"
D="-s RF9Y101ZZPB"

LANGS="ko en ja zh-Hans zh-Hant es fr de pt ru th vi id"
SCREENS="none hunt gaunt craft codex pets"

SB64=$(base64 -w0 < "$SP/s2_save.json")

for TAG in $LANGS; do
  DIR="$OUT/$TAG"
  mkdir -p "$DIR"
  CB64=$(printf '{"languageTag":"%s","autoPrevent":false,"soundOn":true,"musicOn":true,"hapticsOn":true}' "$TAG" | base64 -w0)
  N=0
  for S in $SCREENS; do
    N=$((N+1))
    adb $D shell am force-stop com.geomgang.game >/dev/null
    adb $D shell "run-as com.geomgang.game sh -c 'rm -f files/*.bak files/*.rejected; echo $CB64 | base64 -d > files/settings.json; echo $SB64 | base64 -d > files/save_endless.json'"
    adb $D shell monkey -p com.geomgang.game -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
    sleep 7
    adb $D shell input tap 782 1321      # 자리비움 알림 닫기
    sleep 2
    case "$S" in
      craft)  adb $D shell input tap 289 2050 ;;
      codex)  adb $D shell input tap 957 2050 ;;
      hunt)   adb $D shell input tap 790 2050 ;;
      gaunt)  adb $D shell input tap 790 2050; sleep 3; adb $D shell input tap 540 590 ;;
      pets)   adb $D shell input tap 910 80; sleep 2.5; adb $D shell input tap 540 532 ;;
    esac
    sleep 3
    adb $D exec-out screencap -p > "$DIR/$(printf '%02d' $N)-$S.png"
  done
  echo "$TAG 완료"
done
echo "전부 완료"
