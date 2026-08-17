# -*- coding: utf-8 -*-
"""계열 조합 칸의 제목과 부제.

「마검 조합」과 「+20 두 자루 → 마검 +1」이 사전에 없어 조각 이어 붙이기로
흘렀다. 베트남어에서 「Quỷ kiếm Rèn」「+20 hai thanh kiếm」처럼 어순이
뒤집혔다. 결과 계열이 셋뿐이니 자리마다 키를 넣어 준다.
"""
import io
import json
import os
import sys

I18N = os.path.join(sys.argv[1], "app/src/main/assets/i18n")
RESULTS = ["마검", "성검", "용검"]

# %s 는 계열 이름.
TITLE = {
 "en": "Forge %s", "de": "%s schmieden", "es": "Forjar %s",
 "fr": "Forger %s", "pt": "Forjar %s", "ru": "Ковка: %s",
 "vi": "Rèn %s", "id": "Tempa %s", "th": "หลอม%s",
 "ja": "%sの合成", "zh": "合成%s", "zh_Hant": "合成%s",
}
# %s 는 계열 이름. {} 두 자리는 재료 단계와 결과 단계.
LINE = {
 "en": "Two at +{} → %s +{}",
 "de": "Zwei auf +{} → %s +{}",
 "es": "Dos a +{} → %s +{}",
 "fr": "Deux à +{} → %s +{}",
 "pt": "Duas em +{} → %s +{}",
 "ru": "Два меча +{} → %s +{}",
 "vi": "Hai thanh kiếm +{} → %s +{}",
 "id": "Dua pedang di +{} → %s +{}",
 "th": "ดาบ +{} สองเล่ม → %s +{}",
 "ja": "+{} 二本 → %s +{}",
 "zh": "+{} 两把 → %s +{}",
 "zh_Hant": "+{} 兩把 → %s +{}",
}


def main():
    for code in TITLE:
        path = os.path.join(I18N, code + ".json")
        table = json.load(io.open(path, encoding="utf-8"))
        for family in RESULTS:
            name = table.get(family, family)
            table["%s 조합" % family] = TITLE[code] % name
            table["+{} 두 자루 → %s +{}" % family] = LINE[code] % name
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(dict(sorted(table.items())), f, ensure_ascii=False, indent=2)
            f.write("\n")
        print("%-9s %s / %s" % (code, table["마검 조합"],
                                table["+{} 두 자루 → 마검 +{}"]))


main()
