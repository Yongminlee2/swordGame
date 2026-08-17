# -*- coding: utf-8 -*-
"""문장 가운데에 큰 글자가 섞인 자리를 찾는다.

기계 번역이 낱말마다 첫 글자를 크게 써서 「Scherben Weiter Abbauen」처럼
됐다. 독일어는 명사를 크게 쓰니 빼고, 이름이 들어갈 자리도 빼야 하니
문장(끝에 마침표가 있거나 낱말이 넷 이상)만 본다.
"""
import io
import json
import os
import re
import sys

I18N = os.path.join(sys.argv[1], "app/src/main/assets/i18n")
CODES = ["en", "es", "fr", "pt", "ru", "vi", "id"]
WORD = re.compile(r"[^\W\d_][\w’'-]*", re.UNICODE)


def main():
    for code in CODES:
        table = json.load(io.open(os.path.join(I18N, code + ".json"), encoding="utf-8"))
        hits = []
        for key, value in sorted(table.items()):
            words = WORD.findall(value)
            if len(words) < 4:
                continue
            odd = [w for w in words[1:] if w[:1].isupper() and not w.isupper()]
            if len(odd) >= 2:
                hits.append((key, value, odd))
        print("== %-4s %d자리" % (code, len(hits)))
        for key, value, odd in hits[:24]:
            print("   %-24s %s" % (key.strip()[:24], value[:88]))


main()
