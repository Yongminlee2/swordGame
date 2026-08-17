# -*- coding: utf-8 -*-
"""값이 다른 값들을 이어 붙인 꼴인지 본다.

「사냥 전리품과 보스 검 보관」이 독일어에서 「Beute der Jagd und Boss Schwert
Lager」였다. Schwert(검)와 Lager(보관)가 낱말째로 붙었다. 이런 자리는 값이
다른 키의 값 두 개 이상으로 정확히 쪼개진다 - 그걸 세어 찾는다.
"""
import io
import json
import os
import sys

I18N = os.path.join(sys.argv[1], "app/src/main/assets/i18n")
CODES = ["de", "es", "fr", "pt", "ru", "vi", "id", "th"]


def segment(value, index, depth=0):
    """값을 다른 값들로 남김없이 쪼갠다. 쪼갠 조각 목록, 못 쪼개면 None."""
    if not value:
        return []
    if depth > 5:
        return None
    for length in range(len(value), 2, -1):
        head = value[:length]
        if head not in index:
            continue
        rest = value[length:].lstrip(" ·")
        tail = segment(rest, index, depth + 1)
        if tail is not None:
            return [head] + tail
    return None


def main():
    for code in CODES:
        table = json.load(io.open(os.path.join(I18N, code + ".json"), encoding="utf-8"))
        # 자기 값은 후보에서 뺀다. 짧은 값은 아무 데나 걸려 잡음만 만든다.
        hits = []
        for key, value in table.items():
            if len(value) < 9 or "{" in value:
                continue
            others = {v for k, v in table.items() if k != key and len(v) >= 4}
            parts = segment(value, others)
            if parts and len(parts) >= 2:
                hits.append((len(parts), key, value, parts))
        hits.sort(reverse=True)
        print("== %s  %d자리" % (code, len(hits)))
        for count, key, value, parts in hits:
            print("  %d %s\n    %s\n    %s" % (count, key, value, " | ".join(parts)))


main()
