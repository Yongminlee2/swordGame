# -*- coding: utf-8 -*-
"""옮기지 않고 한국어 소리만 적어 둔 자리를 찾는다.

포르투갈어 「Gyun-yeol」, 러시아어 「Бёнся」처럼 뜻 대신 소리가 남은 자리가
있었다. 라틴 문자 언어에서는 한국어 로마자에만 나오는 글자 짜임을, 다른
문자를 쓰는 언어에서는 라틴 문자 낱말 자체를 눈여겨본다.
"""
import io
import json
import os
import re
import sys

I18N = os.path.join(sys.argv[1], "app/src/main/assets/i18n")
LATIN = ["en", "de", "es", "fr", "pt", "id", "vi"]
OTHER = ["ru", "th", "ja", "zh", "zh_Hant"]

# 한국어 로마자에 흔하고 서양말에는 드문 짜임
KOREAN_SOUND = re.compile(
    r"\b\w*(yeo|yeong|eun\b|eul\b|ung\b|hwa|kkw|ssa|jj|gwa|gyeo|hyeo|myeo|"
    r"nyeo|pyeo|ryeo|syeo|tyeo|byeo|jeo[bgmn]|seong|cheon|gyun|nok|piy)\w*\b",
    re.IGNORECASE)
# 다른 문자를 쓰는 언어에 남아도 되는 라틴 낱말
ALLOWED = {"sfsv", "swordforge", "backup", "hp", "atk", "s", "m", "b", "k",
           "rapier", "rune", "boss", "px", "dp", "sp", "ok"}
LATIN_WORD = re.compile(r"[A-Za-z][A-Za-z'-]{2,}")


def main():
    for code in LATIN + OTHER:
        table = json.load(io.open(os.path.join(I18N, code + ".json"), encoding="utf-8"))
        hits = []
        for key, value in sorted(table.items()):
            if code in LATIN:
                found = KOREAN_SOUND.findall(value)
                if found:
                    hits.append((key, value))
            else:
                words = [w for w in LATIN_WORD.findall(value)
                         if w.lower() not in ALLOWED]
                if words:
                    hits.append((key, value))
        print("== %-9s %d자리" % (code, len(hits)))
        for key, value in hits[:60]:
            print("   %-26s %s" % (key.strip()[:26], value[:70]))


main()
