# -*- coding: utf-8 -*-
"""플레이 스토어에 올릴 아이콘과 그래픽 이미지를 앱 리소스에서 만든다.

아이콘은 512×512, 그래픽 이미지는 1024×500 이다. 그래픽 이미지에는 글자를
넣지 않는다 - 스토어 언어가 열넷이라 한 장에 한 나라 말만 넣으면 나머지
열셋에서 어색해진다. 그림만으로 두면 어느 언어에서든 그대로 쓸 수 있다.
"""
import io
import os
import sys

from PIL import Image, ImageFilter

ROOT = sys.argv[1]
RES = os.path.join(ROOT, "app/src/main/res/drawable-nodpi")
OUT = os.path.join(ROOT, "store")
os.makedirs(OUT, exist_ok=True)

art = Image.open(os.path.join(RES, "ic_launcher_art.png")).convert("RGB")

# --- 스토어 아이콘 512×512 -------------------------------------------------
# 플레이는 자기 마스크를 씌우므로 모서리를 둥글리지 않고 꽉 채워 낸다.
icon = art.resize((512, 512), Image.LANCZOS)
icon.save(os.path.join(OUT, "play-icon-512.png"), optimize=True)

# --- 그래픽 이미지 1024×500 ------------------------------------------------
# 원화를 꽉 채워 자른다. 왼쪽을 비워 두면 죽은 자리가 생겨 미완성으로 보였다.
W, H = 1024, 500
scale = W / art.width * 1.35          # 망치와 검이 화면을 가로지르게 키운다
big = art.resize((int(art.width * scale), int(art.height * scale)), Image.LANCZOS)
left = int(big.width * 0.5 - W / 2)
top = int(big.height * 0.46 - H / 2)  # 불꽃이 터지는 지점을 가운데에 둔다
banner = big.crop((left, top, left + W, top + H))

# 위아래 가장자리를 살짝 어둡게 해 글자를 얹어도 읽히게 한다.
vig = Image.new("L", (W, H), 255)
vp = vig.load()
for y in range(H):
    edge = min(y, H - 1 - y) / (H * 0.22)
    v = 255 if edge >= 1 else int(150 + 105 * edge)
    for x in range(W):
        vp[x, y] = v
banner = Image.composite(banner, Image.new("RGB", (W, H), (2, 1, 7)), vig)

banner.save(os.path.join(OUT, "play-feature-1024x500.png"), optimize=True)

for name in ("play-icon-512.png", "play-feature-1024x500.png"):
    path = os.path.join(OUT, name)
    print("%-28s %s  %.0f KB"
          % (name, Image.open(path).size, os.path.getsize(path) / 1024))
