# -*- coding: utf-8 -*-
"""숫자 자리가 낱말에 달라붙은 곳을 찾는다.

독일어에서 「{}Die Entschädigung」, 프랑스어에서 「{}Seul %」였다. 숫자 뒤에
바로 문장이 붙어 「5Die …」로 그려진다. 낱말을 띄어 쓰는 언어만 본다.
"""
import io
import json
import os
import re
import sys

I18N = os.path.join(sys.argv[1], "app/src/main/assets/i18n")
CODES = ["en", "de", "es", "fr", "pt", "ru", "vi", "id"]
STUCK = re.compile(r"(?:\{\}[^\W\d_]{2,}|[^\W\d_]{2,}\{\})")


def main():
    for code in CODES:
        table = json.load(io.open(os.path.join(I18N, code + ".json"), encoding="utf-8"))
        hits = [(k, v) for k, v in sorted(table.items()) if STUCK.search(v)]
        print("== %-4s %d자리" % (code, len(hits)))
        for key, value in hits:
            print("   %-26s %s" % (key.strip()[:26], value[:76]))


main()
