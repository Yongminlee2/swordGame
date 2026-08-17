# -*- coding: utf-8 -*-
"""조합식에 박혀 있던 옛 계열 이름을 지금 이름으로 맞춘다.

계열 이름을 다시 세우고 나서도 조합식 문구에는 옛 이름이 굳어 있었다 -
스페인어는 재료 줄에 「Espada demoníaca」, 결과 줄에 「Espada Demonio」로
두 이름이 한 화면에 같이 떴다. 중국어는 세검을 칼자루 「劍桿」로 쓰고 있었고,
프랑스어는 대검을 장검과 같은 「Épée longue」로 쓰고 있었다.

조합식은 계열 이름에서 짜 맞추고, 도움말 본문은 옛 이름을 새 이름으로 바꾼다.
"""
import io
import json
import os
import sys

I18N = os.path.join(sys.argv[1], "app/src/main/assets/i18n")

FAM = ["직검", "곡도", "장검", "대검", "세검", "마검", "성검", "용검"]

# 조합식을 만들 때 쓰는 이음말
WORD = {
 "en": {"req": "Required", "two": "two swords", "mat": "%s material: %s·%s +"},
 "de": {"req": "Braucht", "two": "zwei Schwerter", "mat": "%s-Material: %s·%s +"},
 "es": {"req": "Requiere", "two": "dos espadas", "mat": "Materiales de %s: %s·%s +"},
 "fr": {"req": "Requis", "two": "deux épées", "mat": "Matériaux de %s : %s·%s +"},
 "pt": {"req": "Requer", "two": "duas espadas", "mat": "Materiais de %s: %s·%s +"},
 "ru": {"req": "Нужно", "two": "два меча", "mat": "Материалы для %s: %s·%s +"},
 "vi": {"req": "Cần", "two": "hai thanh kiếm", "mat": "Nguyên liệu %s: %s·%s +"},
 "id": {"req": "Butuh", "two": "dua pedang", "mat": "Bahan %s: %s·%s +"},
 "th": {"req": "ต้องการ", "two": "สองเล่ม", "mat": "วัสดุของ %s: %s·%s +"},
 "ja": {"req": "必要", "two": "二本", "mat": "%sの材料: %s·%s +"},
 "zh": {"req": "需要", "two": "两把", "mat": "%s材料: %s·%s +"},
 "zh_Hant": {"req": "需要", "two": "兩把", "mat": "%s材料：%s·%s +"},
}

# 도움말 본문에 남은 옛 이름. 긴 것부터 바꾼다.
OLD = {
 "es": [("Espada Demoníaca", "Espada demoníaca"), ("Espada Demonio", "Espada demoníaca"),
        ("Espada Sagrada", "Espada sagrada"), ("Espada Dragón", "Espada del dragón"),
        ("estoque+", "Estoque+")],
 "fr": [("Épée du Dragon", "Épée du dragon"), ("Épée Sacrée", "Épée sacrée"),
        ("Épée démon+", "Épée démoniaque+"), ("Épée démon ", "Épée démoniaque "),
        ("Épée Courbée", "Épée courbe"), ("Épée courbée", "Épée courbe")],
 "pt": [("Espada Demoníaca", "Espada demoníaca"), ("Espada Sagrada", "Espada sagrada"),
        ("Espada do Dragão", "Espada do dragão"), ("Espada larga", "Espada grande")],
 "ru": [("Святой Меч", "Святой меч"), ("Меч Дракона", "Меч дракона")],
 "vi": [("Thánh Kiếm", "Thánh kiếm"), ("Thanh kiếm rồng", "Kiếm rồng"),
        ("Thanh kiếm cong", "Đao cong")],
 "id": [("rapier+", "Rapier+")],
 "zh": [("剑杆", "细剑")],
 "zh_Hant": [("劍桿", "細劍")],
}


def build(base, word):
    """계열 이름과 이음말로 조합식 문구를 만든다."""
    b = base
    return {
     "\n필요 · ": "\n" + word["req"] + " · ",
     "  =  용검 +": "  =  " + b["용검"] + " +",
     "  =  용검 +{}": "  =  " + b["용검"] + " +{}",
     " + 성검 +": " + " + b["성검"] + " +",
     "대검+20 + 세검+20 → 성검+1\n":
        "%s+20 + %s+20 → %s+1\n" % (b["대검"], b["세검"], b["성검"]),
     "직검+20 + 곡도+20 → 마검+1\n":
        "%s+20 + %s+20 → %s+1\n" % (b["직검"], b["곡도"], b["마검"]),
     "마검 +": b["마검"] + " +",
     "마검 +20 + 성검 +20": "%s +20 + %s +20" % (b["마검"], b["성검"]),
     "마검 +{} + 성검 +{} → ": "%s +{} + %s +{} → " % (b["마검"], b["성검"]),
     "마검+20 + 성검+20 → 용검+":
        "%s+20 + %s+20 → %s+" % (b["마검"], b["성검"], b["용검"]),
     "마검+{} + 성검+{} → 용검+{}":
        "%s+{} + %s+{} → %s+{}" % (b["마검"], b["성검"], b["용검"]),
     "용검 +": b["용검"] + " +",
     "용검 재료: 마검·성검 +":
        word["mat"] % (b["용검"], b["마검"], b["성검"]),
     "용검 재료: 마검·성검 +{}":
        (word["mat"] % (b["용검"], b["마검"], b["성검"])) + "{}",
    }


def main():
    for code, word in WORD.items():
        path = os.path.join(I18N, code + ".json")
        table = json.load(io.open(path, encoding="utf-8"))
        base = {f: table[f] for f in FAM if f in table}
        # 장검·단검은 홀로 쓰이는 키가 없어 조합식에도 안 나온다.
        for missing in set(FAM) - set(base):
            base[missing] = missing
        for key, value in build(base, word).items():
            if key in table:
                table[key] = value
        for old, new in OLD.get(code, []):
            for key, value in list(table.items()):
                if old in value:
                    table[key] = value.replace(old, new)
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(dict(sorted(table.items())), f, ensure_ascii=False, indent=2)
            f.write("\n")
        print("%-9s %s" % (code, table["마검+{} + 성검+{} → 용검+{}"]))


main()
