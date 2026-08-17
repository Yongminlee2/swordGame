# -*- coding: utf-8 -*-
"""단계 이름 + 계열 이름으로 만들어지는 검 이름.

고정 이름이 없는 계열과 +20 위 단계의 검은 이름을 그때그때 「단계 계열」로
붙여 만든다. 그 문장이 사전에 없어 조각 이어 붙이기로 흘렀고, 중국어에서
「深渊 龙剑」처럼 낱말 사이에 빈칸이 남았으며 로망스어에서는 어순이 뒤집혔다.
나올 수 있는 짝을 미리 다 넣어 준다.
"""
import io
import json
import os
import re
import sys

I18N = os.path.join(sys.argv[1], "app/src/main/assets/i18n")
CORE = os.path.join(sys.argv[1], "core/src/main/kotlin/com/geomgang/core")

# 계열 이름은 사전에서 읽는다. 단계 낱말만 여기 적는다.
STAGE = {
 "en": {
  "낡은": "Worn", "손질한": "Tended", "날 세운": "Forged", "단단한": "Sturdy",
  "날카로운": "Keen", "정교한": "Fine", "청옥": "Sapphire", "은빛": "Silver",
  "왕가의": "Royal", "룬": "Rune", "마력": "Arcane", "균열의": "Fissure",
  "심연의": "Abyssal", "벽력": "Thunder", "흑요": "Obsidian", "성운": "Nebula",
  "새벽의": "Dawn", "천공": "Sky", "황금": "Golden", "태양": "Solar",
  "초월": "Transcendent", "심연": "Deep", "성좌": "Constellation",
  "무명": "Nameless",
 },
 "de": {
  "낡은": "Abgenutztes", "손질한": "Gepflegtes", "날 세운": "Geschmiedetes",
  "단단한": "Festes", "날카로운": "Scharfes", "정교한": "Feines",
  "청옥": "Saphir-", "은빛": "Silbernes", "왕가의": "Königliches",
  "룬": "Runen-", "마력": "Arkanes", "균열의": "Riss-", "심연의": "Abgrund-",
  "벽력": "Donner-", "흑요": "Obsidian-", "성운": "Nebel-",
  "새벽의": "Morgen-", "천공": "Himmels-", "황금": "Goldenes",
  "태양": "Sonnen-", "초월": "Transzendentes", "심연": "Tiefen-",
  "성좌": "Sternbild-", "무명": "Namenloses",
 },
 "es": {
  "낡은": "gastada", "손질한": "cuidada", "날 세운": "forjada", "단단한": "firme",
  "날카로운": "afilada", "정교한": "fina", "청옥": "de zafiro",
  "은빛": "plateada", "왕가의": "real", "룬": "rúnica", "마력": "arcana",
  "균열의": "de la grieta", "심연의": "del abismo", "벽력": "del trueno",
  "흑요": "de obsidiana", "성운": "de la nebulosa", "새벽의": "del alba",
  "천공": "del firmamento", "황금": "dorada", "태양": "solar",
  "초월": "trascendente", "심연": "de las profundidades", "성좌": "de las constelaciones",
  "무명": "sin nombre",
 },
 "fr": {
  "낡은": "usée", "손질한": "entretenue", "날 세운": "forgée", "단단한": "solide",
  "날카로운": "acérée", "정교한": "fine", "청옥": "de saphir",
  "은빛": "argentée", "왕가의": "royale", "룬": "runique", "마력": "arcanique",
  "균열의": "de la faille", "심연의": "de l’abîme", "벽력": "du tonnerre",
  "흑요": "d’obsidienne", "성운": "de la nébuleuse", "새벽의": "de l’aube",
  "천공": "du firmament", "황금": "dorée", "태양": "solaire",
  "초월": "transcendante", "심연": "des profondeurs", "성좌": "des constellations",
  "무명": "sans nom",
 },
 "pt": {
  "낡은": "gasta", "손질한": "cuidada", "날 세운": "forjada", "단단한": "firme",
  "날카로운": "afiada", "정교한": "fina", "청옥": "de safira",
  "은빛": "prateada", "왕가의": "real", "룬": "rúnica", "마력": "arcana",
  "균열의": "da fenda", "심연의": "do abismo", "벽력": "do trovão",
  "흑요": "de obsidiana", "성운": "da nebulosa", "새벽의": "da aurora",
  "천공": "do firmamento", "황금": "dourada", "태양": "solar",
  "초월": "transcendente", "심연": "das profundezas", "성좌": "das constelações",
  "무명": "sem nome",
 },
 "ru": {
  "낡은": "изношенный", "손질한": "ухоженный", "날 세운": "выкованный",
  "단단한": "крепкий", "날카로운": "острый", "정교한": "тонкий",
  "청옥": "сапфира", "은빛": "серебристый", "왕가의": "королевский",
  "룬": "рун", "마력": "чар", "균열의": "разлома", "심연의": "бездны",
  "벽력": "грома", "흑요": "обсидиана", "성운": "туманности",
  "새벽의": "рассвета", "천공": "небесной тверди", "황금": "золота",
  "태양": "солнца", "초월": "запредельный", "심연": "глубин",
  "성좌": "созвездий", "무명": "безымянный",
 },
 "vi": {
  "낡은": "cũ", "손질한": "đã chỉnh", "날 세운": "đã rèn", "단단한": "cứng",
  "날카로운": "sắc", "정교한": "tinh xảo", "청옥": "thanh ngọc",
  "은빛": "ánh bạc", "왕가의": "vương gia", "룬": "rune", "마력": "ma lực",
  "균열의": "vết nứt", "심연의": "thâm uyên", "벽력": "phích lịch",
  "흑요": "hắc diệu", "성운": "tinh vân", "새벽의": "bình minh",
  "천공": "thiên không", "황금": "hoàng kim", "태양": "thái dương",
  "초월": "siêu việt", "심연": "vực sâu", "성좌": "tinh tòa",
  "무명": "vô danh",
 },
 "id": {
  "낡은": "Tua", "손질한": "Terawat", "날 세운": "Tempaan", "단단한": "Kokoh",
  "날카로운": "Tajam", "정교한": "Halus", "청옥": "Safir", "은빛": "Keperakan",
  "왕가의": "Wangsa Raja", "룬": "Rune", "마력": "Arkana", "균열의": "Celah",
  "심연의": "Jurang Dalam", "벽력": "Guruh", "흑요": "Obsidian",
  "성운": "Nebula", "새벽의": "Fajar", "천공": "Cakrawala", "황금": "Emas",
  "태양": "Surya", "초월": "Adiwarna", "심연": "Palung",
  "성좌": "Rasi Bintang", "무명": "Tanpa Nama",
 },
 "th": {
  "낡은": "เก่า", "손질한": "ผ่านการดูแล", "날 세운": "ตีขึ้นรูป",
  "단단한": "แข็งแกร่ง", "날카로운": "คม", "정교한": "ประณีต",
  "청옥": "ไพลิน", "은빛": "สีเงิน", "왕가의": "ราชวงศ์", "룬": "รูน",
  "마력": "เวทมนตร์", "균열의": "รอยแยก", "심연의": "เหวลึก",
  "벽력": "อสนีบาต", "흑요": "ออบซิเดียน", "성운": "เนบิวลา",
  "새벽의": "รุ่งอรุณ", "천공": "ฟากฟ้า", "황금": "ทอง", "태양": "ตะวัน",
  "초월": "เหนือโลก", "심연": "ห้วงลึก", "성좌": "กลุ่มดาว",
  "무명": "ไร้นาม",
 },
 "ja": {
  "낡은": "古びた", "손질한": "手入れした", "날 세운": "研ぎ上げた",
  "단단한": "頑丈な", "날카로운": "鋭い", "정교한": "精巧な", "청옥": "青玉の",
  "은빛": "銀色の", "왕가의": "王家の", "룬": "ルーンの", "마력": "魔力の",
  "균열의": "亀裂の", "심연의": "深淵の", "벽력": "雷鳴の", "흑요": "黒曜の",
  "성운": "星雲の", "새벽의": "暁の", "천공": "天空の", "황금": "黄金の",
  "태양": "太陽の", "초월": "超越の", "심연": "深奥の", "성좌": "星座の",
  "무명": "無銘の",
 },
 "zh": {
  "낡은": "陈旧", "손질한": "修整", "날 세운": "锻打", "단단한": "坚实",
  "날카로운": "锋利", "정교한": "精巧", "청옥": "青玉", "은빛": "银色",
  "왕가의": "王家", "룬": "符文", "마력": "魔力", "균열의": "裂隙",
  "심연의": "深渊", "벽력": "雷霆", "흑요": "黑曜", "성운": "星云",
  "새벽의": "黎明", "천공": "天空", "황금": "黄金", "태양": "太阳",
  "초월": "超越", "심연": "深邃", "성좌": "星座", "무명": "无名",
 },
 "zh_Hant": {
  "낡은": "陳舊", "손질한": "修整", "날 세운": "鍛打", "단단한": "堅實",
  "날카로운": "鋒利", "정교한": "精巧", "청옥": "青玉", "은빛": "銀色",
  "왕가의": "王家", "룬": "符文", "마력": "魔力", "균열의": "裂隙",
  "심연의": "深淵", "벽력": "雷霆", "흑요": "黑曜", "성운": "星雲",
  "새벽의": "黎明", "천공": "天空", "황금": "黃金", "태양": "太陽",
  "초월": "超越", "심연": "深邃", "성좌": "星座", "무명": "無名",
 },
}

# 단계 낱말이 계열 이름 앞에 붙는 언어와 뒤에 붙는 언어.
PREFIX = {"en", "de", "ja", "zh", "zh_Hant"}
GLUE = {"ja": "", "zh": "", "zh_Hant": "", "th": ""}
# 러시아어는 형용사만 앞에 오고, 속격 낱말은 뒤에 온다.
RU_ADJECTIVE = {
 "낡은", "손질한", "날 세운", "단단한", "날카로운", "정교한", "은빛", "왕가의",
 "초월", "무명",
}


def read_arrays():
    src = io.open(os.path.join(CORE, "SwordNames.kt"), encoding="utf-8").read()
    hidden = re.search(r"HIDDEN_STAGES = arrayOf\((.*?)\)", src, re.S).group(1)
    endless = re.search(r"ENDLESS_STAGES = arrayOf\((.*?)\)", src, re.S).group(1)
    visible = re.search(r"VISIBLE_BY_FAMILY = mapOf\((.*?)\n    \)", src, re.S).group(1)
    named = set(re.findall(r"WeaponFamily\.([A-Z]+) to", visible))
    model = io.open(os.path.join(CORE, "Model.kt"), encoding="utf-8").read()
    block = re.search(r"enum class WeaponFamily.*?\n    ;", model, re.S).group(0)
    families = re.findall(r'([A-Z]+)\("[a-z]+", "([^"]+)"\)', block)
    return (re.findall(r'"([^"]+)"', hidden),
            re.findall(r'"([^"]+)"', endless),
            named, families)


def main():
    hidden, endless, named, families = read_arrays()
    pairs = []
    for enum, label in families:
        if enum not in named:
            pairs += [(s, label) for s in hidden]
        pairs += [(s, label) for s in endless]
    pairs = sorted(set(pairs))
    for code, words in STAGE.items():
        path = os.path.join(I18N, code + ".json")
        table = json.load(io.open(path, encoding="utf-8"))
        glue = GLUE.get(code, " ")
        for stage, family in pairs:
            word = words[stage]
            base = table.get(family, family)
            key = "%s %s" % (stage, family)
            prefix = code in PREFIX or (code == "ru" and stage in RU_ADJECTIVE)
            if prefix:
                # 독일어 합성어는 이음줄로 끝나면 붙여 쓴다 - Abgrund-Drachenschwert.
                joint = "" if word.endswith("-") else (glue if code in PREFIX else " ")
                table[key] = word + joint + base
            else:
                table[key] = base + glue + word
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(dict(sorted(table.items())), f, ensure_ascii=False, indent=2)
            f.write("\n")
        print("%-9s %d개  보기: %s / %s"
              % (code, len(pairs), table["심연 용검"], table["무명 허검"]))


main()
