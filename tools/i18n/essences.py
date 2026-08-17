# -*- coding: utf-8 -*-
"""구역 정수 표시를 언어마다 제 어순으로 만든다.

「심연 정수 5 (0)」이 조각을 이어 붙는 길로 흘러 스페인어에서 「Abismo
Esencia 5 (0)」이 됐다. 명사 뒤에 수식어가 오는 언어에서는 순서가 뒤집혀야
한다. 구역이 스물넷이니 자리마다 키를 하나씩 넣어 준다 - 그러면 이어 붙는
길을 타지 않고 바로 맞는 문구가 나온다.

정수라는 낱말 자체도 언어마다 두 가지로 갈려 있었다 - 러시아어는 Сущность 와
эссенция, 베트남어는 Tinh Thần 과 tinh chất 을 섞어 썼다. 한 가지로 맞춘다.
"""
import io
import json
import os
import re
import sys

I18N = os.path.join(sys.argv[1], "app/src/main/assets/i18n")
ZONES_KT = os.path.join(sys.argv[1], "core/src/main/kotlin/com/geomgang/core/Zones.kt")

# 구역 정수 한 줄. %s 는 구역 이름.
SHAPE = {
 "en": "%s Essence {} ({})",
 "de": "%s-Essenz {} ({})",
 "es": "Esencia — %s {} ({})",
 "fr": "Essence — %s {} ({})",
 "pt": "Essência — %s {} ({})",
 "ru": "Эссенция — %s {} ({})",
 "vi": "Tinh chất %s {} ({})",
 "id": "Esensi %s {} ({})",
 "th": "แก่นแท้%s {} ({})",
 "ja": "%sのエッセンス {} ({})",
 "zh": "%s精华 {} ({})",
 "zh_Hant": "%s精華 {} ({})",
}

# 낱말을 한 가지로 맞춘다.
TERM = {
 "ru": {" 정수 ": " эссенция ", "정수와 제단": "Эссенция и жертвенник"},
 "vi": {" 정수 ": " tinh chất ", "정수와 제단": "Tinh chất và bàn thờ"},
 "fr": {"정수와 제단": "Essence et autel"},
 "th": {"정수와 제단": "แก่นแท้และแท่นบูชา"},
}


def zones():
    src = io.open(ZONES_KT, encoding="utf-8").read()
    return [m[1] for m in re.findall(r'^\s{4}([A-Z_]+)\(\s*\n?\s*"[a-z_]+",\s*"([^"]+)"',
                                     src, re.M)]


def main():
    names = zones()
    if len(names) != 24:
        raise SystemExit("구역이 %d곳이다" % len(names))
    for code, shape in SHAPE.items():
        path = os.path.join(I18N, code + ".json")
        table = json.load(io.open(path, encoding="utf-8"))
        added = 0
        for zone in names:
            key = "%s 정수 {} ({})" % zone
            label = table.get(zone, zone)
            table[key] = shape % label
            added += 1
        for key, value in TERM.get(code, {}).items():
            if key in table:
                table[key] = value
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(dict(sorted(table.items())), f, ensure_ascii=False, indent=2)
            f.write("\n")
        print("%-9s %d개  보기: %s" % (code, added, table["심연 정수 {} ({})"]))


main()
