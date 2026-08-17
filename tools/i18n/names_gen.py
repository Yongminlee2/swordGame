# -*- coding: utf-8 -*-
"""검 이름을 계열 이름 + 수식어로 다시 만든다.

한국어는 수식어가 앞에 오지만 스페인어·프랑스어·포르투갈어·인도네시아어·
베트남어·태국어는 뒤에 온다. 지금 값들은 한국어 어순을 그대로 옮겨 「Grieta
Gran espada」처럼 낱말을 늘어놓은 꼴이었고, 포르투갈어·러시아어·인도네시아어
일부는 「Gyun-yeol」「Мучхэ」「Nokseun」처럼 한국어 소리가 그대로 남아 있었다.

계열 이름은 한 곳에서만 정하고 수식어는 붙일 꼴로 미리 써 둔다 - 프랑스어의
de/d' 나 성 일치를 매번 따지지 않아도 되게.
"""
import io
import json
import os
import sys

I18N = os.path.join(sys.argv[1], "app/src/main/assets/i18n")

FAMILIES = ["직검", "곡도", "장검", "대검", "세검", "마검", "성검", "용검", "단검"]

BASE = {
 "es": {"직검": "Espada recta", "곡도": "Espada curva", "장검": "Espada larga",
        "대검": "Gran espada", "세검": "Estoque", "마검": "Espada demoníaca",
        "성검": "Espada sagrada", "용검": "Espada del dragón", "단검": "Daga"},
 "fr": {"직검": "Épée droite", "곡도": "Épée courbe", "장검": "Épée longue",
        "대검": "Grande épée", "세검": "Rapière", "마검": "Épée démoniaque",
        "성검": "Épée sacrée", "용검": "Épée du dragon", "단검": "Dague"},
 "pt": {"직검": "Espada reta", "곡도": "Espada curva", "장검": "Espada longa",
        "대검": "Espada grande", "세검": "Florete", "마검": "Espada demoníaca",
        "성검": "Espada sagrada", "용검": "Espada do dragão", "단검": "Adaga"},
 "ru": {"직검": "Прямой меч", "곡도": "Изогнутый меч", "장검": "Длинный меч",
        "대검": "Большой меч", "세검": "Рапира", "마검": "Демонический меч",
        "성검": "Святой меч", "용검": "Меч дракона", "단검": "Кинжал"},
 "id": {"직검": "Pedang Lurus", "곡도": "Pedang Melengkung", "장검": "Pedang Panjang",
        "대검": "Pedang Hebat", "세검": "Rapier", "마검": "Pedang Setan",
        "성검": "Pedang Suci", "용검": "Pedang Naga", "단검": "Belati"},
 "vi": {"직검": "Kiếm thẳng", "곡도": "Đao cong", "장검": "Trường kiếm",
        "대검": "Đại kiếm", "세검": "Rapier", "마검": "Quỷ kiếm",
        "성검": "Thánh kiếm", "용검": "Kiếm rồng", "단검": "Đoản kiếm"},
 "th": {"직검": "ดาบตรง", "곡도": "ดาบโค้ง", "장검": "ดาบยาว",
        "대검": "ดาบใหญ่", "세검": "เรเปียร์", "마검": "ดาบปีศาจ",
        "성검": "ดาบศักดิ์สิทธิ์", "용검": "ดาบมังกร", "단검": "ดาบสั้น"},
}

# 수식어를 계열 이름 뒤에 붙일 꼴로 쓴다.
MOD = {
 "es": {
  "가시": "de espinas", "격노": "de furia", "골룡": "de hueso de dragón",
  "광휘": "de esplendor", "귀왕": "del rey espectro", "귀화": "de fuego fantasma",
  "균열": "de la grieta", "기사단": "de la orden", "나락": "del averno",
  "날개": "alada", "낡은": "viejo", "녹슨": "oxidada", "독안": "del ojo venenoso",
  "룬": "rúnica", "마력": "arcana", "멸혼": "devoradora de almas",
  "명예의": "del honor", "무쇠": "de hierro fundido", "백금": "de platino",
  "백은": "de plata blanca", "날 세운": "forjada", "벽력": "del trueno",
  "사막": "del desierto", "사슬": "de cadenas", "새벽": "del alba",
  "서광": "de la primera luz", "성운": "de la nebulosa", "성휘": "de luz estelar",
  "손질한": "afilada", "송곳니": "de colmillo", "수정": "de cristal",
  "수호": "de la guardia", "심연": "del abismo", "심판": "del juicio",
  "쌍날": "de doble filo", "쌍익": "de alas gemelas", "악몽": "de pesadilla",
  "여명의": "del amanecer", "왕가": "de la casa real", "왕관": "de la corona",
  "왕실": "de la corte real", "용골": "de hueso de dragón",
  "용광": "de metal fundido", "용암": "de lava", "은": "de plata",
  "은월": "de luna plateada", "자염": "de llama violeta", "자월": "de luna violeta",
  "적안": "del ojo rojo", "정화": "de la purificación", "종말": "del apocalipsis",
  "주술": "del hechizo", "중철": "de hierro pesado", "찬란": "del fulgor",
  "천공": "del firmamento", "천륜": "de la rueda celeste", "천문": "astral",
  "천벌": "del castigo divino", "천사": "del ángel", "천상": "del cielo",
  "청광": "de luz azul", "청린": "de escama azul", "청명": "del cielo claro",
  "청옥": "de zafiro", "청풍": "del viento azul", "축복": "de la bendición",
  "태양": "solar", "톱날": "de dientes de sierra", "파동": "de ondas",
  "파멸": "de la ruina", "파쇄": "trituradora", "폭염": "de llamas ardientes",
  "핏빛": "sangrienta", "혈석": "de piedra de sangre", "혈아": "de colmillo sangriento",
  "혈월": "de luna de sangre", "혈혼": "de alma de sangre",
  "홍련": "del loto carmesí", "홍염": "de llama roja", "화심": "de corazón de fuego",
  "황금": "dorada", "흑": "negra", "흑염": "de llama negra",
  "흑요": "de obsidiana", "흑철": "de hierro negro",
 },
 "fr": {
  "가시": "d’épines", "격노": "de fureur", "골룡": "d’os de dragon",
  "광휘": "de splendeur", "귀왕": "du roi spectre", "귀화": "de feu fantôme",
  "균열": "de la faille", "기사단": "de l’ordre", "나락": "des enfers",
  "날개": "ailée", "낡은": "usée", "녹슨": "rouillée", "독안": "à l’œil venimeux",
  "룬": "runique", "마력": "arcanique", "멸혼": "dévoreuse d’âmes",
  "명예의": "de l’honneur", "무쇠": "de fonte", "백금": "de platine",
  "백은": "d’argent blanc", "날 세운": "forgée", "벽력": "du tonnerre",
  "사막": "du désert", "사슬": "de chaînes", "새벽": "de l’aube",
  "서광": "de la première lueur", "성운": "de la nébuleuse", "성휘": "de lumière stellaire",
  "손질한": "affûtée", "송곳니": "à crocs", "수정": "de cristal",
  "수호": "de la garde", "심연": "de l’abîme", "심판": "du jugement",
  "쌍날": "à double tranchant", "쌍익": "aux ailes jumelles", "악몽": "de cauchemar",
  "여명의": "du point du jour", "왕가": "de la maison royale", "왕관": "de la couronne",
  "왕실": "de la cour royale", "용골": "d’os de dragon",
  "용광": "de métal en fusion", "용암": "de lave", "은": "d’argent",
  "은월": "de lune d’argent", "자염": "de flamme violette", "자월": "de lune violette",
  "적안": "à l’œil rouge", "정화": "de la purification", "종말": "de l’apocalypse",
  "주술": "du sortilège", "중철": "de fer lourd", "찬란": "de l’éclat",
  "천공": "du firmament", "천륜": "de la roue céleste", "천문": "astrale",
  "천벌": "du châtiment divin", "천사": "de l’ange", "천상": "des cieux",
  "청광": "de lumière bleue", "청린": "à écailles bleues", "청명": "du ciel clair",
  "청옥": "de saphir", "청풍": "du vent bleu", "축복": "de la bénédiction",
  "태양": "solaire", "톱날": "dentelée", "파동": "des ondes",
  "파멸": "de la ruine", "파쇄": "broyeuse", "폭염": "de flammes ardentes",
  "핏빛": "sanglante", "혈석": "de pierre de sang", "혈아": "au croc sanglant",
  "혈월": "de lune de sang", "혈혼": "d’âme de sang",
  "홍련": "du lotus pourpre", "홍염": "de flamme rouge", "화심": "au cœur de feu",
  "황금": "dorée", "흑": "noire", "흑염": "de flamme noire",
  "흑요": "d’obsidienne", "흑철": "de fer noir",
 },
 "pt": {
  "가시": "de espinhos", "격노": "da fúria", "골룡": "de osso de dragão",
  "광휘": "do esplendor", "귀왕": "do rei espectro", "귀화": "de fogo fantasma",
  "균열": "da fenda", "기사단": "da ordem", "나락": "do abismo profundo",
  "날개": "alada", "낡은": "velho", "녹슨": "enferrujada", "독안": "do olho venenoso",
  "룬": "rúnica", "마력": "arcana", "멸혼": "devoradora de almas",
  "명예의": "da honra", "무쇠": "de ferro fundido", "백금": "de platina",
  "백은": "de prata branca", "날 세운": "forjada", "벽력": "do trovão",
  "사막": "do deserto", "사슬": "de correntes", "새벽": "da aurora",
  "서광": "da primeira luz", "성운": "da nebulosa", "성휘": "de luz estelar",
  "손질한": "afiada", "송곳니": "de presa", "수정": "de cristal",
  "수호": "da guarda", "심연": "do abismo", "심판": "do juízo",
  "쌍날": "de dois fios", "쌍익": "de asas gêmeas", "악몽": "do pesadelo",
  "여명의": "do amanhecer", "왕가": "da casa real", "왕관": "da coroa",
  "왕실": "da corte real", "용골": "de osso de dragão",
  "용광": "de metal fundido", "용암": "de lava", "은": "de prata",
  "은월": "de lua prateada", "자염": "de chama violeta", "자월": "de lua violeta",
  "적안": "do olho vermelho", "정화": "da purificação", "종말": "do apocalipse",
  "주술": "do feitiço", "중철": "de ferro pesado", "찬란": "do fulgor",
  "천공": "do firmamento", "천륜": "da roda celeste", "천문": "astral",
  "천벌": "do castigo divino", "천사": "do anjo", "천상": "dos céus",
  "청광": "de luz azul", "청린": "de escama azul", "청명": "do céu claro",
  "청옥": "de safira", "청풍": "do vento azul", "축복": "da bênção",
  "태양": "solar", "톱날": "de dentes de serra", "파동": "das ondas",
  "파멸": "da ruína", "파쇄": "trituradora", "폭염": "de chamas ardentes",
  "핏빛": "sangrenta", "혈석": "de pedra de sangue", "혈아": "de presa sangrenta",
  "혈월": "de lua de sangue", "혈혼": "de alma de sangue",
  "홍련": "do lótus carmesim", "홍염": "de chama vermelha", "화심": "de coração de fogo",
  "황금": "dourada", "흑": "negra", "흑염": "de chama negra",
  "흑요": "de obsidiana", "흑철": "de ferro negro",
 },
 "ru": {
  "가시": "шипов", "격노": "ярости", "골룡": "костяного дракона",
  "광휘": "сияния", "귀왕": "короля призраков", "귀화": "призрачного огня",
  "균열": "разлома", "기사단": "рыцарского ордена", "나락": "преисподней",
  "날개": "крыла", "낡은": "старая", "녹슨": "ржавый", "독안": "ядовитого глаза",
  "룬": "рун", "마력": "чар", "멸혼": "пожирающий души",
  "명예의": "чести", "무쇠": "чугуна", "백금": "платины",
  "백은": "белого серебра", "날 세운": "выкованный", "벽력": "грома",
  "사막": "пустыни", "사슬": "цепей", "새벽": "рассвета",
  "서광": "первого света", "성운": "туманности", "성휘": "звёздного света",
  "손질한": "заточенный", "송곳니": "клыка", "수정": "кристалла",
  "수호": "стражи", "심연": "бездны", "심판": "правосудия",
  "쌍날": "двойного лезвия", "쌍익": "двух крыльев", "악몽": "кошмара",
  "여명의": "утренней зари", "왕가": "королевского дома", "왕관": "короны",
  "왕실": "королевского двора", "용골": "драконьей кости",
  "용광": "расплавленного металла", "용암": "лавы", "은": "серебра",
  "은월": "серебряной луны", "자염": "фиолетового пламени", "자월": "фиолетовой луны",
  "적안": "красного глаза", "정화": "очищения", "종말": "конца времён",
  "주술": "колдовства", "중철": "тяжёлого железа", "찬란": "блеска",
  "천공": "небесной тверди", "천륜": "небесного круга", "천문": "звёздного неба",
  "천벌": "небесной кары", "천사": "ангела", "천상": "небес",
  "청광": "синего света", "청린": "синей чешуи", "청명": "ясного неба",
  "청옥": "сапфира", "청풍": "синего ветра", "축복": "благословения",
  "태양": "солнца", "톱날": "пильного лезвия", "파동": "волн",
  "파멸": "погибели", "파쇄": "дробящий", "폭염": "бушующего пламени",
  "핏빛": "цвета крови", "혈석": "кровавого камня", "혈아": "кровавого клыка",
  "혈월": "кровавой луны", "혈혼": "кровавой души",
  "홍련": "багряного лотоса", "홍염": "алого пламени", "화심": "пламенного сердца",
  "황금": "золота", "흑": "тьмы", "흑염": "чёрного пламени",
  "흑요": "обсидиана", "흑철": "чёрного железа",
 },
 "id": {
  "가시": "Duri", "격노": "Amarah", "골룡": "Tulang Naga",
  "광휘": "Cahaya Agung", "귀왕": "Raja Arwah", "귀화": "Api Arwah",
  "균열": "Celah", "기사단": "Ordo Ksatria", "나락": "Jurang",
  "날개": "Bersayap", "낡은": "Tua", "녹슨": "Berkarat", "독안": "Mata Berbisa",
  "룬": "Rune", "마력": "Arkana", "멸혼": "Pemakan Jiwa",
  "명예의": "Kehormatan", "무쇠": "Besi Tuang", "백금": "Platina",
  "백은": "Perak Putih", "날 세운": "Tempaan", "벽력": "Guruh",
  "사막": "Padang Pasir", "사슬": "Rantai", "새벽": "Fajar",
  "서광": "Cahaya Pertama", "성운": "Nebula", "성휘": "Cahaya Bintang",
  "손질한": "Terasah", "송곳니": "Taring", "수정": "Kristal",
  "수호": "Penjaga", "심연": "Jurang Dalam", "심판": "Penghakiman",
  "쌍날": "Bilah Ganda", "쌍익": "Sayap Kembar", "악몽": "Mimpi Buruk",
  "여명의": "Subuh", "왕가": "Wangsa Raja", "왕관": "Mahkota",
  "왕실": "Istana Raja", "용골": "Tulang Naga",
  "용광": "Logam Cair", "용암": "Lava", "은": "Perak",
  "은월": "Bulan Perak", "자염": "Nyala Ungu", "자월": "Bulan Ungu",
  "적안": "Mata Merah", "정화": "Penyucian", "종말": "Kiamat",
  "주술": "Ilmu Sihir", "중철": "Besi Berat", "찬란": "Kilau",
  "천공": "Cakrawala", "천륜": "Roda Langit", "천문": "Perbintangan",
  "천벌": "Hukuman Langit", "천사": "Malaikat", "천상": "Surgawi",
  "청광": "Cahaya Biru", "청린": "Sisik Biru", "청명": "Langit Bening",
  "청옥": "Safir", "청풍": "Angin Biru", "축복": "Berkah",
  "태양": "Surya", "톱날": "Mata Gergaji", "파동": "Gelombang",
  "파멸": "Kehancuran", "파쇄": "Peremuk", "폭염": "Nyala Berkobar",
  "핏빛": "Warna Darah", "혈석": "Batu Darah", "혈아": "Taring Darah",
  "혈월": "Bulan Darah", "혈혼": "Jiwa Darah",
  "홍련": "Teratai Merah", "홍염": "Nyala Merah", "화심": "Inti Api",
  "황금": "Emas", "흑": "Hitam", "흑염": "Nyala Hitam",
  "흑요": "Obsidian", "흑철": "Besi Hitam",
 },
 "vi": {
  "가시": "gai", "격노": "phẫn nộ", "골룡": "cốt long",
  "광휘": "quang huy", "귀왕": "quỷ vương", "귀화": "quỷ hỏa",
  "균열": "vết nứt", "기사단": "kỵ sĩ đoàn", "나락": "nại lạc",
  "날개": "cánh", "낡은": "cũ", "녹슨": "gỉ", "독안": "độc nhãn",
  "룬": "rune", "마력": "ma lực", "멸혼": "diệt hồn",
  "명예의": "danh dự", "무쇠": "gang", "백금": "bạch kim",
  "백은": "bạch ngân", "날 세운": "đã rèn", "벽력": "phích lịch",
  "사막": "sa mạc", "사슬": "dây xích", "새벽": "bình minh",
  "서광": "thự quang", "성운": "tinh vân", "성휘": "tinh huy",
  "손질한": "đã mài", "송곳니": "nanh", "수정": "thủy tinh",
  "수호": "thủ hộ", "심연": "thâm uyên", "심판": "phán xét",
  "쌍날": "song nhẫn", "쌍익": "song dực", "악몽": "ác mộng",
  "여명의": "lê minh", "왕가": "vương gia", "왕관": "vương miện",
  "왕실": "vương thất", "용골": "long cốt",
  "용광": "kim loại nung", "용암": "dung nham", "은": "bạc",
  "은월": "ngân nguyệt", "자염": "tử diễm", "자월": "tử nguyệt",
  "적안": "xích nhãn", "정화": "tịnh hóa", "종말": "chung mạt",
  "주술": "chú thuật", "중철": "thiết nặng", "찬란": "xán lạn",
  "천공": "thiên không", "천륜": "thiên luân", "천문": "thiên văn",
  "천벌": "thiên phạt", "천사": "thiên sứ", "천상": "thiên thượng",
  "청광": "thanh quang", "청린": "thanh lân", "청명": "thanh minh",
  "청옥": "thanh ngọc", "청풍": "thanh phong", "축복": "chúc phúc",
  "태양": "thái dương", "톱날": "lưỡi cưa", "파동": "ba động",
  "파멸": "phá diệt", "파쇄": "phá toái", "폭염": "bộc diễm",
  "핏빛": "sắc huyết", "혈석": "huyết thạch", "혈아": "huyết nha",
  "혈월": "huyết nguyệt", "혈혼": "huyết hồn",
  "홍련": "hồng liên", "홍염": "hồng diễm", "화심": "hỏa tâm",
  "황금": "hoàng kim", "흑": "hắc", "흑염": "hắc diễm",
  "흑요": "hắc diệu", "흑철": "hắc thiết",
 },
 "th": {
  "가시": "หนาม", "격노": "พิโรธ", "골룡": "กระดูกมังกร",
  "광휘": "รัศมี", "귀왕": "ราชาผี", "귀화": "ไฟผี",
  "균열": "รอยแยก", "기사단": "คณะอัศวิน", "나락": "อเวจี",
  "날개": "ปีก", "낡은": "เก่า", "녹슨": "ขึ้นสนิม", "독안": "ตาพิษ",
  "룬": "รูน", "마력": "เวทมนตร์", "멸혼": "กลืนวิญญาณ",
  "명예의": "เกียรติยศ", "무쇠": "เหล็กหล่อ", "백금": "แพลตินัม",
  "백은": "เงินขาว", "날 세운": "ตีขึ้นรูป", "벽력": "อสนีบาต",
  "사막": "ทะเลทราย", "사슬": "โซ่", "새벽": "รุ่งอรุณ",
  "서광": "แสงแรก", "성운": "เนบิวลา", "성휘": "แสงดาว",
  "손질한": "ลับคม", "송곳니": "เขี้ยว", "수정": "ผลึกแก้ว",
  "수호": "พิทักษ์", "심연": "เหวลึก", "심판": "พิพากษา",
  "쌍날": "สองคม", "쌍익": "สองปีก", "악몽": "ฝันร้าย",
  "여명의": "อรุณรุ่ง", "왕가": "ราชวงศ์", "왕관": "มงกุฎ",
  "왕실": "ราชสำนัก", "용골": "กระดูกมังกร",
  "용광": "โลหะหลอม", "용암": "ลาวา", "은": "เงิน",
  "은월": "จันทร์เงิน", "자염": "เพลิงม่วง", "자월": "จันทร์ม่วง",
  "적안": "ตาแดง", "정화": "ชำระล้าง", "종말": "วันสิ้นโลก",
  "주술": "ไถ้มนตร์", "중철": "เหล็กหนัก", "찬란": "เจิดจ้า",
  "천공": "ฟากฟ้า", "천륜": "วงล้อสวรรค์", "천문": "ดารา",
  "천벌": "ทัณฑ์สวรรค์", "천사": "เทวทูต", "천상": "สวรรค์",
  "청광": "แสงคราม", "청린": "เกล็ดคราม", "청명": "ฟ้าใส",
  "청옥": "ไพลิน", "청풍": "ลมคราม", "축복": "พร",
  "태양": "ตะวัน", "톱날": "ฟันเลื่อย", "파동": "คลื่น",
  "파멸": "ล่มสลาย", "파쇄": "บดขยี้", "폭염": "เพลิงโหม",
  "핏빛": "สีเลือด", "혈석": "ศิลาเลือด", "혈아": "เขี้ยวเลือด",
  "혈월": "จันทร์เลือด", "혈혼": "วิญญาณเลือด",
  "홍련": "บัวชาด", "홍염": "เพลิงแดง", "화심": "ใจไฟ",
  "황금": "ทอง", "흑": "ดำ", "흑염": "เพลิงดำ",
  "흑요": "ออบซิเดียน", "흑철": "เหล็กดำ",
 },
}


def main():
    korean = json.load(io.open(os.path.join(I18N, "en.json"), encoding="utf-8"))
    pairs = []
    for key in korean:
        if "{" in key or len(key) > 10:
            continue
        for family in FAMILIES:
            if key.endswith(family) and key != family:
                modifier = key[: -len(family)].strip()
                if modifier:
                    pairs.append((key, modifier, family))
                break
    for code in BASE:
        path = os.path.join(I18N, code + ".json")
        table = json.load(io.open(path, encoding="utf-8"))
        glue = "" if code == "th" else " "
        count = 0
        for key, modifier, family in pairs:
            phrase = MOD[code].get(modifier)
            if phrase is None:
                # 단계 이름으로 만들어지는 이름은 stages.py 가 따로 넣는다.
                continue
            table[key] = BASE[code][family] + glue + phrase
            count += 1
        # 계열 이름 자체도 한 곳에서 정한 것으로 맞춘다.
        for family, value in BASE[code].items():
            if family in table:
                table[family] = value
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(dict(sorted(table.items())), f, ensure_ascii=False, indent=2)
            f.write("\n")
        print("%-4s 이름 %d개" % (code, count))


main()
