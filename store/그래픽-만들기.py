# -*- coding: utf-8 -*-
"""플레이 스토어에 올릴 아이콘과 그래픽 이미지를 앱 리소스에서 만든다.

아이콘은 512×512, 그래픽 이미지는 1024×500 이다.

그래픽 이미지에는 **글자를 넣지 않는다** - 스토어 언어가 열넷이라 한 나라 말만
넣으면 나머지 열셋에서 어색해진다. 대신 게임이 무엇인지를 그림 하나로 말한다:
**검 한 자루가 점점 세지는 것.** 실제 게임 스프라이트를 왼쪽 낡은 검부터
오른쪽 전설검까지 올라가는 계단으로 늘어놓았다.

처음에는 런처 아이콘 원화를 확대해 잘라 썼는데, 아이콘을 크게 키운 것일 뿐이라
게임을 하나도 설명하지 못했다.
"""
import math
import os
import sys

from PIL import Image, ImageFilter

ROOT = sys.argv[1]
RES = os.path.join(ROOT, "app/src/main/res/drawable-nodpi")
OUT = os.path.join(ROOT, "store")
os.makedirs(OUT, exist_ok=True)

CELL = 328                     # CustomSwordArt.CELL 과 같아야 한다

art = Image.open(os.path.join(RES, "ic_launcher_art.png")).convert("RGB")

# --- 스토어 아이콘 512×512 -------------------------------------------------
# 플레이는 자기 마스크를 씌우므로 모서리를 둥글리지 않고 꽉 채워 낸다.
art.resize((512, 512), Image.LANCZOS).save(
    os.path.join(OUT, "play-icon-512.png"), optimize=True)


def sword(sheet, cell, columns):
    """검 아틀라스에서 한 칸을 떼어 빈 여백을 잘라 낸다."""
    im = Image.open(os.path.join(RES, sheet)).convert("RGBA")
    x, y = (cell % columns) * CELL, (cell // columns) * CELL
    tile = im.crop((x, y, x + CELL, y + CELL))
    return tile.crop(tile.getbbox())


def glow(size, color, strength):
    """검 뒤에 깔 둥근 빛."""
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = layer.load()
    half = size / 2
    for gy in range(size):
        for gx in range(size):
            d = math.hypot(gx - half, gy - half) / half
            if d < 1:
                px[gx, gy] = color + (int(strength * (1 - d) ** 2),)
    return layer


# --- 그래픽 이미지 1024×500 ------------------------------------------------
W, H = 1024, 500

# 배경은 대장간 그림이다. 흐리고 어둡게 깔아야 검이 앞으로 나온다.
bg = Image.open(os.path.join(RES, "forge_background_season2.png")).convert("RGB")
scale = max(W / bg.width, H / bg.height)
bg = bg.resize((int(bg.width * scale), int(bg.height * scale)), Image.LANCZOS)
left = (bg.width - W) // 2
top = int(bg.height * 0.30)
banner = bg.crop((left, top, left + W, top + H)).filter(ImageFilter.GaussianBlur(7))
banner = Image.blend(banner, Image.new("RGB", (W, H), (3, 6, 12)), 0.5).convert("RGBA")

# 아래에서 올라오는 화로 불빛
banner.alpha_composite(
    glow(600, (255, 140, 40), 95).resize((1180, 470), Image.LANCZOS), (-78, 200))

# 낡은 직검에서 전설검까지 - 오른쪽으로 갈수록 크고 높고 밝아진다.
STEPS = [
    ("sword_custom_straight_sheet.png", 0, 7, 158, 0.60),
    ("sword_custom_straight_sheet.png", 10, 7, 198, 0.72),
    ("sword_custom_straight_sheet.png", 20, 7, 240, 0.84),
    ("sword_custom_dragon_sheet.png", 20, 7, 292, 0.94),
    ("sword_custom_legend_sheet.png", 29, 10, 344, 1.0),
]

for i, (sheet, cell, columns, height, bright) in enumerate(STEPS):
    piece = sword(sheet, cell, columns)
    piece = piece.resize(
        (max(1, round(piece.width * height / piece.height)), height), Image.LANCZOS)
    if bright < 1:
        # 앞 단계는 그늘에 둔다. 검은 판을 덮으면 투명한 여백까지 네모로
        # 드러나므로, 색만 어둡게 하고 투명도는 그대로 둔다.
        r, g, b, a = piece.split()
        piece = Image.merge("RGBA", [c.point(lambda v: int(v * bright))
                                     for c in (r, g, b)] + [a])
    cx, cy = 108 + i * 188, 330 - i * 28
    if i == len(STEPS) - 1:            # 마지막 검 뒤에만 빛을 깐다
        banner.alpha_composite(glow(560, (255, 190, 90), 130), (cx - 280, cy - 280))
    banner.alpha_composite(piece, (cx - piece.width // 2, cy - piece.height // 2))

# 위아래 가장자리를 눌러 스토어 카드에서 잘려도 어색하지 않게 한다.
vig = Image.new("L", (W, H), 255)
vp = vig.load()
for y in range(H):
    edge = min(y, H - 1 - y) / (H * 0.15)
    v = 255 if edge >= 1 else int(115 + 140 * edge)
    for x in range(W):
        vp[x, y] = v
banner = Image.composite(banner.convert("RGB"),
                         Image.new("RGB", (W, H), (2, 4, 9)), vig)

banner.save(os.path.join(OUT, "play-feature-1024x500.png"), optimize=True)

for name in ("play-icon-512.png", "play-feature-1024x500.png"):
    path = os.path.join(OUT, name)
    print("%-28s %s  %.0f KB"
          % (name, Image.open(path).size, os.path.getsize(path) / 1024))
