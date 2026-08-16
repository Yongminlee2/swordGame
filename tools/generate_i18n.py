"""Generate the 12 non-Korean runtime catalogs used by Sword Forge.

The game keeps domain labels in Korean so old saves and pure-JVM rules remain stable.
This script extracts the literal pieces that can reach the UI, translates missing
entries, and writes UTF-8 JSON catalogs under app/src/main/assets/i18n.

Existing entries are retained. Running the script again therefore translates only
new strings and does not unexpectedly rewrite reviewed wording.
"""

from __future__ import annotations

import json
import http.cookiejar
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOTS = (
    ROOT / "app/src/main/java",
    ROOT / "core/src/main/kotlin",
)
OUTPUT = ROOT / "app/src/main/assets/i18n"
HANGUL = re.compile(r"[\uac00-\ud7a3]")
PLACEHOLDER = re.compile(
    r"\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*|"
    r"%(?:\d+\$)?[,#+\- 0<(]*\d*(?:\.\d+)?[a-zA-Z%]"
)
SEPARATOR = "<<<SFGSEP_7F3A>>>"

TARGETS = {
    "en": "en",
    "ja": "ja",
    "zh": "zh-CN",
    "zh_Hant": "zh-TW",
    "es": "es",
    "fr": "fr",
    "de": "de",
    "pt": "pt",
    "ru": "ru",
    "th": "th",
    "vi": "vi",
    "id": "id",
}

# Short game terms are ambiguous without context. Longer sentences are left to the
# translator, while these terms define the vocabulary seen throughout the game.
ENGLISH_OVERRIDES = {
    "개": "",
    "검": "Sword",
    "룬": "Rune",
    "만": "×10K",
    "분": " min",
    "알": "Egg",
    "억": "×100M",
    "조": "T",
    "종": " types",
    "초": " sec",
    "층": "Floor",
    "펫": "Pet",
    "회": " times",
    "강": "",
    "강화": "Enhance",
    "계열": "Family",
    "곡도": "Curved Sword",
    "골드": "Gold",
    "기록": "Records",
    "다음": "Next",
    "단계": "Level",
    "단련": "Training",
    "대검": "Greatsword",
    "도감": "Collection",
    "뒤로": "Back",
    "마검": "Demon Sword",
    "방지권": "Protection Ticket",
    "보관함": "Storage",
    "사냥": "Hunt",
    "사냥터": "Hunting Grounds",
    "상점": "Shop",
    "설정": "Settings",
    "성검": "Holy Sword",
    "성공": "Success",
    "세검": "Rapier",
    "수령": "Claim",
    "용검": "Dragon Sword",
    "유지": "Unchanged",
    "일반": "Normal",
    "재료": "Materials",
    "전설": "Legend",
    "조각": "Shards",
    "조합": "Craft",
    "직검": "Straight Sword",
    "쌍검": "Twin Blades",
    "낫검": "Scythe Sword",
    "도끼검": "Axe Sword",
    "창검": "Spear Sword",
    "정령검": "Spirit Sword",
    "합검": "Fusion Sword",
    "허검": "Void Sword",
    "전설검": "Legend Sword",
    "진동": "Haptics",
    "취소": "Cancel",
    "통계": "Stats",
    "파괴": "Destroyed",
    "확인": "OK",
    "하락": "Downgrade",
    "강화석": "Upgrade Stone",
    "고유검": "Unique Swords",
    "기기 언어 따르기": "Use device language",
    "기록 메뉴": "Records Menu",
    "대장 기술": "Smithing Skills",
    "방지권 자동 사용": "Auto-use Protection Ticket",
    "별 강화": "Star Upgrade",
    "언어": "Language",
    "언어 변경": "Change language",
    "언어 선택": "Choose language",
    "전설 파괴 방지권": "Legend Protection Ticket",
    "축복서": "Blessing Scroll",
    "행운부적": "Lucky Charm",
}

CATALOG_OVERRIDES = {
    "en": {
        "정수": "Essence", "정수력": "Essence Power", "정수력 ": "Essence Power ",
        "강화석": "Upgrade Stone",
        "쌍검": "Twin Blades", "낫검": "Scythe Sword", "도끼검": "Axe Sword",
        "창검": "Spear Sword", "정령검": "Spirit Sword", "합검": "Fusion Sword",
        "허검": "Void Sword", "전설검": "Legend Sword", "회 겪기 ": " encounters ",
    },
    "ja": {
        "정수": "エッセンス", "정수력": "エッセンス力", "정수력 ": "エッセンス力 ",
        "강화석": "強化石",
        "쌍검": "双剣", "낫검": "鎌剣", "도끼검": "斧剣", "창검": "槍剣",
        "정령검": "精霊剣", "합검": "融合剣", "허검": "虚無剣", "전설검": "伝説剣",
    },
    "zh": {
        "정수": "精华", "정수력": "精华力量", "정수력 ": "精华力量 ",
        "강화석": "强化石",
        "쌍검": "双剑", "낫검": "镰剑", "도끼검": "斧剑", "창검": "枪剑",
        "정령검": "精灵剑", "합검": "融合剑", "허검": "虚空剑", "전설검": "传奇剑",
    },
    "zh_Hant": {
        "정수": "精華", "정수력": "精華力量", "정수력 ": "精華力量 ",
        "강화석": "強化石",
        "쌍검": "雙劍", "낫검": "鐮劍", "도끼검": "斧劍", "창검": "槍劍",
        "정령검": "精靈劍", "합검": "融合劍", "허검": "虛空劍", "전설검": "傳說劍",
    },
    "es": {
        "정수": "Esencia", "정수력": "Poder de esencia", "정수력 ": "Poder de esencia ",
        "강화석": "Piedra de mejora",
        "쌍검": "Espadas dobles", "낫검": "Espada guadaña", "도끼검": "Espada hacha",
        "창검": "Espada lanza", "정령검": "Espada espiritual", "합검": "Espada fusionada",
        "허검": "Espada del Vacío", "전설검": "Espada legendaria",
    },
    "fr": {
        "정수": "Essence", "정수력": "Pouvoir d’essence", "정수력 ": "Pouvoir d’essence ",
        "강화석": "Pierre d’amélioration",
        "쌍검": "Lames jumelles", "낫검": "Épée-faux", "도끼검": "Épée-hache",
        "창검": "Épée-lance", "정령검": "Épée spirituelle", "합검": "Épée fusionnée",
        "허검": "Épée du Néant", "전설검": "Épée légendaire",
    },
    "de": {
        "정수": "Essenz", "정수력": "Essenzkraft", "정수력 ": "Essenzkraft ",
        "강화석": "Verstärkungsstein",
        "쌍검": "Zwillingsklingen", "낫검": "Sensenschwert", "도끼검": "Axtschwert",
        "창검": "Speerschwert", "정령검": "Geisterschwert", "합검": "Fusionsschwert",
        "허검": "Schwert der Leere", "전설검": "Legendenschwert",
    },
    "pt": {
        "정수": "Essência", "정수력": "Poder de essência", "정수력 ": "Poder de essência ",
        "강화석": "Pedra de aprimoramento",
        "쌍검": "Lâminas Gêmeas", "낫검": "Espada-Foice", "도끼검": "Espada-Machado",
        "창검": "Espada-Lança", "정령검": "Espada Espiritual", "합검": "Espada Fundida",
        "허검": "Espada do Vazio", "전설검": "Espada Lendária",
    },
    "ru": {
        "정수": "Эссенция", "정수력": "Сила эссенции", "정수력 ": "Сила эссенции ",
        "강화석": "Камень улучшения",
        "쌍검": "Парные клинки", "낫검": "Меч-коса", "도끼검": "Меч-топор",
        "창검": "Меч-копьё", "정령검": "Духовный меч", "합검": "Меч слияния",
        "허검": "Меч Пустоты", "전설검": "Легендарный меч",
    },
    "th": {
        "정수": "แก่นแท้", "정수력": "พลังแก่นแท้", "정수력 ": "พลังแก่นแท้ ",
        "강화석": "หินเสริมพลัง",
        "쌍검": "ดาบคู่", "낫검": "ดาบเคียว", "도끼검": "ดาบขวาน", "창검": "ดาบหอก",
        "정령검": "ดาบวิญญาณ", "합검": "ดาบผสาน", "허검": "ดาบแห่งความว่างเปล่า",
        "전설검": "ดาบในตำนาน",
    },
    "vi": {
        "정수": "Tinh chất", "정수력": "Sức mạnh tinh chất", "정수력 ": "Sức mạnh tinh chất ",
        "강화석": "Đá cường hóa",
        "쌍검": "Song kiếm", "낫검": "Kiếm lưỡi hái", "도끼검": "Kiếm rìu",
        "창검": "Kiếm giáo", "정령검": "Kiếm linh hồn", "합검": "Kiếm hợp nhất",
        "허검": "Kiếm Hư Không", "전설검": "Kiếm huyền thoại",
    },
    "id": {
        "정수": "Esensi", "정수력": "Kekuatan Esensi", "정수력 ": "Kekuatan Esensi ",
        "강화석": "Batu Peningkatan",
        "쌍검": "Pedang Kembar", "낫검": "Pedang Sabit", "도끼검": "Pedang Kapak",
        "창검": "Pedang Tombak", "정령검": "Pedang Roh", "합검": "Pedang Fusi",
        "허검": "Pedang Kehampaan", "전설검": "Pedang Legendaris",
    },
}

UNIVERSAL_OVERRIDES = {
    "개": "",
    "만": "×10K",
    "억": "×100M",
    "조": "T",
}

# These nouns must keep their game meaning even inside a longer sentence. They are
# replaced with opaque markers before translation and restored afterwards. Verbs
# such as "강화한다" are deliberately not protected so each language can conjugate
# them naturally.
PROTECTED_ENGLISH_TERMS = {
    "전설 파괴 방지권": "Legend Protection Ticket",
    "방지권 자동 사용": "Auto-use Protection Ticket",
    "대장 기술": "Smithing Skills",
    "별 강화": "Star Upgrade",
    "기기 언어 따르기": "Use device language",
    "강화석": "Upgrade Stone",
    "행운부적": "Lucky Charm",
    "축복서": "Blessing Scroll",
    "방지권": "Protection Ticket",
    "정수력": "Essence Power",
    "정수": "Essence",
    "사냥터": "Hunting Grounds",
    "보관함": "Storage",
    "고유검": "Unique Sword",
    "도감": "Collection",
    "조각": "Shards",
    "골드": "Gold",
    "용검": "Dragon Sword",
    "성검": "Holy Sword",
    "마검": "Demon Sword",
    "대검": "Greatsword",
    "세검": "Rapier",
    "곡도": "Curved Sword",
    "직검": "Straight Sword",
    "펫": "Pet",
}

TERM_MARKERS = {
    source: f"SFGTERM{index:03d}"
    for index, source in enumerate(
        sorted(PROTECTED_ENGLISH_TERMS, key=len, reverse=True)
    )
}


def decode_regular_string(value: str) -> str:
    replacements = {
        r"\n": "\n",
        r"\r": "\r",
        r"\t": "\t",
        r'\"': '"',
        r"\'": "'",
        r"\$": "$",
        r"\\": "\\",
    }
    return re.sub(
        r"\\[nrt\"'\$\\]",
        lambda match: replacements.get(match.group(0), match.group(0)),
        value,
    )


def kotlin_strings(source: str):
    """Yield string literal bodies while skipping comments and character literals."""
    i = 0
    size = len(source)
    while i < size:
        if source.startswith("//", i):
            newline = source.find("\n", i + 2)
            i = size if newline < 0 else newline + 1
            continue
        if source.startswith("/*", i):
            depth = 1
            i += 2
            while i < size and depth:
                if source.startswith("/*", i):
                    depth += 1
                    i += 2
                elif source.startswith("*/", i):
                    depth -= 1
                    i += 2
                else:
                    i += 1
            continue
        if source[i] == "'":
            i += 1
            while i < size:
                if source[i] == "\\":
                    i += 2
                elif source[i] == "'":
                    i += 1
                    break
                else:
                    i += 1
            continue
        if source.startswith('"""', i):
            end = source.find('"""', i + 3)
            if end < 0:
                return
            yield source[i + 3 : end]
            i = end + 3
            continue
        if source[i] == '"':
            i += 1
            body = []
            while i < size:
                char = source[i]
                if char == "\\" and i + 1 < size:
                    body.extend((char, source[i + 1]))
                    i += 2
                elif char == '"':
                    i += 1
                    break
                else:
                    body.append(char)
                    i += 1
            yield decode_regular_string("".join(body))
            continue
        i += 1


def source_fragments() -> list[str]:
    fragments: set[str] = set()
    for source_root in SOURCE_ROOTS:
        for path in source_root.rglob("*.kt"):
            # Generated/native language names should not become UI source phrases.
            if path.name in {"GameLanguage.kt", "GameTranslator.kt"}:
                continue
            for literal in kotlin_strings(path.read_text(encoding="utf-8")):
                if not HANGUL.search(literal):
                    continue
                for part in PLACEHOLDER.split(literal):
                    if HANGUL.search(part):
                        fragments.add(part)

    # Settings strings are introduced with the localization UI itself. Keeping them
    # explicit makes the generator independent from whether that screen is staged yet.
    fragments.update(
        {
            "언어",
            "기기 언어 따르기",
            "언어 변경",
            "언어 선택",
            "기기 언어와 같은 언어를 자동으로 사용한다.",
        }
    )
    return sorted(fragments, key=lambda value: (-len(value), value))


def preserve_edge_whitespace(source: str, translated: str) -> str:
    prefix = source[: len(source) - len(source.lstrip())]
    suffix = source[len(source.rstrip()) :]
    return prefix + translated.strip() + suffix


def protect_terms(source: str) -> str:
    result = source
    for term in sorted(PROTECTED_ENGLISH_TERMS, key=len, reverse=True):
        result = result.replace(term, TERM_MARKERS[term])
    return result


def restore_terms(translated: str, glossary: dict[str, str]) -> str:
    result = translated
    for term, marker in TERM_MARKERS.items():
        result = result.replace(marker, glossary[term])
    return result


def chunks(values: list[str], max_chars: int = 400):
    current: list[str] = []
    length = 0
    for value in values:
        next_length = length + len(value) + len(SEPARATOR) + 2
        if current and next_length > max_chars:
            yield current
            current = []
            length = 0
        current.append(value)
        length += len(value) + len(SEPARATOR) + 2
    if current:
        yield current


def split_translation(values: list[str], translated: str) -> list[str]:
    parts = re.split(rf"\s*{re.escape(SEPARATOR)}\s*", translated)
    if len(parts) != len(values):
        raise RuntimeError(
            f"separator mismatch: expected {len(values)}, received {len(parts)}"
        )
    return [
        preserve_edge_whitespace(original, result)
        for original, result in zip(values, parts)
    ]


_bing_opener = urllib.request.build_opener(
    urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
)
_bing_credentials: tuple[str, str, str] | None = None
_bing_user_agent = (
    "Mozilla/5.0 (Windows NT 10.0; Microsoft Windows 10.0.19045; ko-KR) "
    "PowerShell/7.6.4"
)


def bing_credentials(refresh: bool = False) -> tuple[str, str, str]:
    global _bing_credentials
    if _bing_credentials is not None and not refresh:
        return _bing_credentials
    request = urllib.request.Request(
        "https://www.bing.com/translator",
        headers={"User-Agent": _bing_user_agent},
    )
    with _bing_opener.open(request, timeout=45) as response:
        page = response.read().decode("utf-8")
    ig_match = re.search(r'IG:"([^"]+)"', page)
    token_match = re.search(
        r'params_AbusePreventionHelper\s*=\s*\[(\d+),"([^"]+)"', page
    )
    if not ig_match or not token_match:
        raise RuntimeError("could not read Bing translation credentials")
    _bing_credentials = (ig_match.group(1), token_match.group(1), token_match.group(2))
    return _bing_credentials


def request_bing_translation(values: list[str], source: str, target: str) -> list[str]:
    target = {"zh-CN": "zh-Hans", "zh-TW": "zh-Hant"}.get(target, target)
    joined = ("\n" + SEPARATOR + "\n").join(value.strip() for value in values)
    last_error: Exception | None = None
    for attempt in range(4):
        try:
            ig, key, token = bing_credentials(refresh=attempt > 0)
            payload = urllib.parse.urlencode(
                {
                    "fromLang": source,
                    "to": target,
                    "text": joined,
                    "token": token,
                    "key": key,
                }
            ).encode("utf-8")
            request = urllib.request.Request(
                f"https://www.bing.com/ttranslatev3?isVertical=1&IG={ig}&IID=translator.5028.1",
                data=payload,
                headers={"User-Agent": _bing_user_agent},
            )
            with _bing_opener.open(request, timeout=45) as response:
                data = json.loads(response.read().decode("utf-8"))
            if not isinstance(data, list):
                raise RuntimeError(f"unexpected Bing response: {data}")
            translated = data[0]["translations"][0]["text"]
            results = split_translation(values, translated)
            time.sleep(0.2)
            return results
        except (OSError, KeyError, IndexError, ValueError, RuntimeError) as error:
            last_error = error
            time.sleep(min(2**attempt, 8))
    raise RuntimeError(f"Bing translation request failed: {last_error}")


def request_translation(values: list[str], source: str, target: str) -> list[str]:
    stripped = [value.strip() for value in values]
    joined = ("\n" + SEPARATOR + "\n").join(stripped)
    payload = urllib.parse.urlencode(
        {"client": "gtx", "sl": source, "tl": target, "dt": "t", "q": joined}
    ).encode("utf-8")
    request = urllib.request.Request(
        "https://translate.googleapis.com/translate_a/single",
        data=payload,
        headers={"User-Agent": "SwordForge localization generator"},
    )
    last_error: Exception | None = None
    for attempt in range(6):
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                data = json.loads(response.read().decode("utf-8"))
            translated = "".join(segment[0] for segment in data[0])
            results = split_translation(values, translated)
            time.sleep(0.2)
            return results
        except urllib.error.HTTPError as error:
            if error.code == 429:
                return request_bing_translation(values, source, target)
            last_error = error
            time.sleep(min(2**attempt, 12))
        except (OSError, ValueError, RuntimeError, urllib.error.URLError) as error:
            last_error = error
            time.sleep(min(2**attempt, 12))
    print(f"Google unavailable ({last_error}); using Bing", flush=True)
    return request_bing_translation(values, source, target)


def load_catalog(code: str) -> dict[str, str]:
    path = OUTPUT / f"{code}.json"
    if not path.exists():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def save_catalog(code: str, values: dict[str, str], ordered_keys: list[str]) -> None:
    ordered = {key: values[key] for key in ordered_keys}
    path = OUTPUT / f"{code}.json"
    path.write_text(
        json.dumps(ordered, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def apply_catalog_overrides(code: str, catalog: dict[str, str]) -> None:
    for key, value in UNIVERSAL_OVERRIDES.items():
        if key in catalog:
            catalog[key] = value
    for key, value in CATALOG_OVERRIDES.get(code, {}).items():
        if key in catalog:
            catalog[key] = value


def generate_language(
    code: str,
    source_language: str,
    target_language: str,
    source_values: dict[str, str],
    keys: list[str],
    glossary: dict[str, str],
    refresh: bool = False,
    refresh_terms: bool = False,
) -> dict[str, str]:
    catalog = {} if refresh else load_catalog(code)
    if refresh_terms:
        catalog = {
            key: value
            for key, value in catalog.items()
            if not any(term in key for term in PROTECTED_ENGLISH_TERMS)
        }
    missing = [key for key in keys if key not in catalog]
    if missing:
        print(f"{code}: translating {len(missing)} new entries", flush=True)
    for batch in chunks(missing):
        inputs = [source_values[key] for key in batch]
        inputs = [protect_terms(value) for value in inputs]
        outputs = request_translation(inputs, source_language, target_language)
        outputs = [restore_terms(value, glossary) for value in outputs]
        catalog.update(dict(zip(batch, outputs)))
        save_catalog(code, catalog, [key for key in keys if key in catalog])
    # Remove strings that are no longer present, so coverage checks stay meaningful.
    catalog = {key: catalog[key] for key in keys}
    save_catalog(code, catalog, keys)
    return catalog


def localized_glossary(code: str, target: str) -> dict[str, str]:
    """Return stable game terms, using reviewed catalog entries when available."""
    existing = load_catalog(code)
    glossary: dict[str, str] = {}
    missing: list[str] = []
    for source, english in PROTECTED_ENGLISH_TERMS.items():
        reviewed = existing.get(source)
        override = CATALOG_OVERRIDES.get(code, {}).get(source)
        if override:
            glossary[source] = override.strip()
        elif reviewed:
            glossary[source] = reviewed.strip()
        elif code == "en":
            glossary[source] = english
        else:
            missing.append(source)

    for batch in chunks(missing):
        english_values = [PROTECTED_ENGLISH_TERMS[source] for source in batch]
        translated = request_translation(english_values, "en", target)
        glossary.update(
            (source, value.strip()) for source, value in zip(batch, translated)
        )
    return glossary


def repair_hangul_leaks(
    code: str,
    target: str,
    catalog: dict[str, str],
    english_fallback: dict[str, str] | None = None,
) -> None:
    """Re-translate partial results once, then fall back to readable English."""
    leaked_keys = [key for key, value in catalog.items() if HANGUL.search(value)]
    if not leaked_keys:
        return
    print(f"{code}: repairing {len(leaked_keys)} partial translations", flush=True)
    for batch in chunks(leaked_keys):
        outputs = request_translation(batch, "ko", target)
        for key, output in zip(batch, outputs):
            repaired = preserve_edge_whitespace(key, output)
            if HANGUL.search(repaired) and english_fallback is not None:
                repaired = english_fallback[key]
            catalog[key] = repaired
    remaining = [key for key, value in catalog.items() if HANGUL.search(value)]
    if remaining:
        raise RuntimeError(f"Hangul remains in {code}: {remaining[:3]}")


def main() -> int:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    refresh = "--refresh" in sys.argv[1:]
    refresh_terms = "--refresh-terms" in sys.argv[1:]
    only_arg = next((arg for arg in sys.argv[1:] if arg.startswith("--only=")), None)
    only = set(only_arg.split("=", 1)[1].split(",")) if only_arg else set(TARGETS)
    unknown = only - set(TARGETS)
    if unknown:
        raise SystemExit(f"unknown language codes: {sorted(unknown)}")
    keys = source_fragments()
    print(f"source fragments: {len(keys)}", flush=True)

    identity = {key: key for key in keys}
    if "en" in only:
        english = generate_language(
            "en",
            "ko",
            "en",
            identity,
            keys,
            glossary=localized_glossary("en", "en"),
            refresh=refresh,
            refresh_terms=refresh_terms,
        )
        for key, value in ENGLISH_OVERRIDES.items():
            if key in english:
                english[key] = preserve_edge_whitespace(key, value)
        apply_catalog_overrides("en", english)
        repair_hangul_leaks("en", "en", english)
        save_catalog("en", english, keys)
    else:
        english = load_catalog("en")
        if english.keys() != set(keys):
            raise SystemExit("English catalog must be complete before translating other languages")

    for code, target in TARGETS.items():
        if code == "en" or code not in only:
            continue
        catalog = generate_language(
            code,
            "ko",
            target,
            identity,
            keys,
            glossary=localized_glossary(code, target),
            refresh=refresh,
            refresh_terms=refresh_terms,
        )
        apply_catalog_overrides(code, catalog)
        repair_hangul_leaks(code, target, catalog, english)
        save_catalog(code, catalog, keys)

    print(f"wrote {len(TARGETS)} catalogs to {OUTPUT}", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
