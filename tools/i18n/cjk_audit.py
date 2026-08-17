# -*- coding: utf-8 -*-
"""한자·가나 사이에 끼어든 빈칸을 찾는다.

일본어와 중국어는 낱말을 띄어 쓰지 않는다. 「伝説 破壊 保護券」처럼 빈칸이
보이면 조각을 그냥 늘어놓은 자리다. 숫자·라틴 문자 옆의 빈칸은 정상이라 뺀다.
"""
import io
import json
import os
import re
import sys

I18N = os.path.join(sys.argv[1], "app/src/main/assets/i18n")
CJK = r"[぀-ヿ㐀-鿿豈-﫿]"
GLUE = re.compile(CJK + r" +" + CJK)


def main():
    for code in ["ja", "zh", "zh_Hant"]:
        table = json.load(io.open(os.path.join(I18N, code + ".json"), encoding="utf-8"))
        hits = [(k, v) for k, v in table.items() if GLUE.search(v)]
        print("== %s  %d자리" % (code, len(hits)))
        for key, value in sorted(hits):
            print("   %-28s %s" % (key.strip()[:28], value))


main()
