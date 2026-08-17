# -*- coding: utf-8 -*-
"""서로 다른 검이 같은 이름으로 나오는 자리를 찾는다.

프랑스어에서 대검을 「Épée longue」로 옮긴 이름이 몇 개 있는데, 장검이 이미
「Épée longue」다. 「가시 대검」과 「가시 장검」이 화면에서 구별되지 않는다.
"""
import io
import json
import os
import sys
from collections import defaultdict

I18N = os.path.join(sys.argv[1], "app/src/main/assets/i18n")
FAMILIES = ("직검", "곡도", "장검", "대검", "세검", "마검", "성검", "용검", "단검", "검")


def main():
    korean = [k for k in json.load(
        io.open(os.path.join(I18N, "en.json"), encoding="utf-8"))
        if k.endswith(FAMILIES) and 2 <= len(k) <= 10 and "{" not in k]
    for path in sorted(os.listdir(I18N)):
        code = path[:-5]
        table = json.load(io.open(os.path.join(I18N, path), encoding="utf-8"))
        seen = defaultdict(list)
        for name in korean:
            value = table.get(name)
            if value:
                seen[value.lower()].append(name)
        clash = {v: ks for v, ks in seen.items() if len(ks) > 1}
        print("== %-9s %d자리" % (code, len(clash)))
        for value, names in sorted(clash.items()):
            print("   %-34s %s" % (value, " / ".join(names)))


main()
