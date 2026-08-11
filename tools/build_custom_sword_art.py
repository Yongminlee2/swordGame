"""Build per-level production sword sheets from the approved v2 evolution grids.

Unlike the first pass, every enhancement level owns a genuinely different source
sprite.  The generator only removes the chroma background, normalises scale, and
reduces the art to a deliberately chunky pixel grid; it never reuses a milestone
sprite for neighbouring levels.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "art-review" / "sword-evolution-v2" / "raw-generated"
OUTPUT_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

CELL = 128
FAMILY_COLUMNS = 21
LEGEND_COLUMNS = 30
LOGICAL_MAX = 58
SCALE = 2


@dataclass(frozen=True)
class FamilySpec:
    family_id: str
    filename: str


FAMILIES = (
    FamilySpec("straight", "straight-grid.png"),
    FamilySpec("curved", "curved-grid.png"),
    FamilySpec("great", "great-grid.png"),
    FamilySpec("rapier", "rapier-grid.png"),
    FamilySpec("demon", "demon-grid.png"),
    FamilySpec("holy", "holy-grid.png"),
    FamilySpec("dragon", "dragon-grid.png"),
)


def remove_green_screen(source: Image.Image) -> Image.Image:
    """Turn the generated flat green backdrop into a clean transparent matte."""
    rgb = np.asarray(source.convert("RGB"), dtype=np.float32)
    red, green, blue = np.moveaxis(rgb, -1, 0)
    excess = green - np.maximum(red, blue)

    # The generator's key is slightly graded (roughly #08f808).  Cyan crystals
    # remain opaque because their blue channel tracks green instead of leaving a
    # large green-only excess.
    key_strength = np.clip((excess - 28.0) / 82.0, 0.0, 1.0)
    bright_green = np.clip((green - 105.0) / 80.0, 0.0, 1.0)
    alpha = ((1.0 - key_strength * bright_green) * 255.0).astype(np.uint8)
    alpha[alpha < 20] = 0
    alpha[alpha > 232] = 255

    # Remove green spill from anti-aliased edge pixels before the sprite is
    # reduced to its logical pixel resolution.
    opaque = alpha > 0
    green_dominant = opaque & (green > red + 10.0) & (green > blue + 10.0)
    # The prompt intentionally contains no green sword parts.  Any surviving
    # green-only pixel is key spill or a model-invented gem, so redirect it to
    # the UI's cyan accent instead of leaving a neon-green halo.
    rgb[..., 2] = np.where(green_dominant, np.maximum(blue, green * 0.84), blue)
    neutral_green = np.maximum(red, rgb[..., 2]) + 3.0
    rgb[..., 1] = np.where(opaque, np.minimum(green, neutral_green), green)
    rgba = np.dstack((rgb.astype(np.uint8), alpha))
    return Image.fromarray(rgba, mode="RGBA")


def split_grid(path: Path, columns: int, rows: int) -> list[Image.Image]:
    source = remove_green_screen(Image.open(path))
    sprites: list[Image.Image] = []
    for index in range(columns * rows):
        row, column = divmod(index, columns)
        left = round(column * source.width / columns)
        right = round((column + 1) * source.width / columns)
        top = round(row * source.height / rows)
        bottom = round((row + 1) * source.height / rows)
        cell = keep_largest_component(source.crop((left, top, right, bottom)))
        bbox = cell.getchannel("A").getbbox()
        if bbox is None:
            raise ValueError(f"empty sprite cell {index}: {path}")
        pad = 3
        x0 = max(0, bbox[0] - pad)
        y0 = max(0, bbox[1] - pad)
        x1 = min(cell.width, bbox[2] + pad)
        y1 = min(cell.height, bbox[3] + pad)
        sprites.append(cell.crop((x0, y0, x1, y1)))
    return sprites


def keep_largest_component(source: Image.Image) -> Image.Image:
    """Drop keying dust and fragments from neighbouring grid cells."""
    alpha = np.asarray(source.getchannel("A"))
    mask = alpha > 72
    height, width = mask.shape
    visited = np.zeros(mask.size, dtype=np.bool_)
    flat_mask = mask.reshape(-1)
    largest: list[int] = []

    for raw_start in np.flatnonzero(flat_mask):
        start = int(raw_start)
        if visited[start]:
            continue
        visited[start] = True
        stack = [start]
        component: list[int] = []
        while stack:
            point = stack.pop()
            component.append(point)
            y, x = divmod(point, width)
            for ny in range(max(0, y - 1), min(height, y + 2)):
                row = ny * width
                for nx in range(max(0, x - 1), min(width, x + 2)):
                    neighbour = row + nx
                    if not visited[neighbour] and flat_mask[neighbour]:
                        visited[neighbour] = True
                        stack.append(neighbour)
        if len(component) > len(largest):
            largest = component

    if not largest:
        return source

    keep = np.zeros(mask.size, dtype=np.uint8)
    keep[largest] = 255
    # Generated highlights can leave one-pixel gaps.  A tiny dilation keeps
    # those details attached while still rejecting distant background dust.
    keep_image = Image.fromarray(keep.reshape(mask.shape), mode="L").filter(
        ImageFilter.MaxFilter(5),
    )
    cleaned = source.copy()
    cleaned.putalpha(ImageChops.multiply(source.getchannel("A"), keep_image))
    return cleaned


def resize_rgba(source: Image.Image, size: tuple[int, int]) -> Image.Image:
    """Resize premultiplied RGBA without creating a dark or green fringe."""
    rgba = np.asarray(source, dtype=np.float32)
    alpha = rgba[..., 3:4] / 255.0
    premultiplied = np.concatenate((rgba[..., :3] * alpha, rgba[..., 3:4]), axis=-1)
    channels = [
        np.asarray(
            Image.fromarray(premultiplied[..., channel].astype(np.uint8), mode="L").resize(
                size,
                Image.Resampling.LANCZOS,
            ),
            dtype=np.float32,
        )
        for channel in range(4)
    ]
    resized = np.stack(channels, axis=-1)
    out_alpha = resized[..., 3:4]
    safe_alpha = np.maximum(out_alpha / 255.0, 1.0 / 255.0)
    colour = np.clip(resized[..., :3] / safe_alpha, 0.0, 255.0)
    result = np.concatenate((colour, out_alpha), axis=-1).astype(np.uint8)
    return Image.fromarray(result, mode="RGBA")


def render_cell(source: Image.Image) -> Image.Image:
    ratio = LOGICAL_MAX / max(source.width, source.height)
    logical_size = (
        max(1, round(source.width * ratio)),
        max(1, round(source.height * ratio)),
    )
    small = resize_rgba(source, logical_size)

    # A limited palette and binary logical alpha make the sprites read as
    # intentional pixel art rather than downscaled paintings.
    alpha = small.getchannel("A").point(lambda value: 255 if value >= 92 else 0)
    quantized = small.convert("RGB").quantize(
        colors=48,
        method=Image.Quantize.MEDIANCUT,
        dither=Image.Dither.NONE,
    ).convert("RGBA")
    quantized.putalpha(alpha)
    sprite = quantized.resize(
        (logical_size[0] * SCALE, logical_size[1] * SCALE),
        Image.Resampling.NEAREST,
    )

    canvas = Image.new("RGBA", (CELL, CELL), (0, 0, 0, 0))
    offset = ((CELL - sprite.width) // 2, (CELL - sprite.height) // 2)
    canvas.alpha_composite(sprite, offset)
    return canvas


def build_family_sheet() -> Path:
    sheet = Image.new(
        "RGBA",
        (FAMILY_COLUMNS * CELL, len(FAMILIES) * CELL),
        (0, 0, 0, 0),
    )
    for row, spec in enumerate(FAMILIES):
        sprites = split_grid(SOURCE_DIR / spec.filename, columns=7, rows=3)
        if len(sprites) != FAMILY_COLUMNS:
            raise ValueError(f"{spec.family_id}: expected 21 sprites, got {len(sprites)}")
        for level, source in enumerate(sprites):
            sheet.alpha_composite(render_cell(source), (level * CELL, row * CELL))

    output = OUTPUT_DIR / "sword_custom_family_sheet.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, optimize=True)
    return output


def build_legend_sheet() -> Path:
    generated = split_grid(SOURCE_DIR / "legend-grid-32.png", columns=8, rows=4)
    # The image model returned 32 strong candidates.  The first 29 preserve the
    # intended +21/+30/+40 milestones; the most ornate final candidate is +50.
    selected = generated[:29] + [generated[31]]
    if len(selected) != LEGEND_COLUMNS:
        raise ValueError(f"legend: expected 30 sprites, got {len(selected)}")

    sheet = Image.new(
        "RGBA",
        (LEGEND_COLUMNS * CELL, CELL),
        (0, 0, 0, 0),
    )
    for column, source in enumerate(selected):
        sheet.alpha_composite(render_cell(source), (column * CELL, 0))

    output = OUTPUT_DIR / "sword_custom_legend_sheet.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, optimize=True)
    return output


def validate_unique_cells(path: Path, columns: int, rows: int) -> None:
    sheet = Image.open(path).convert("RGBA")
    hashes: set[bytes] = set()
    for row in range(rows):
        for column in range(columns):
            cell = sheet.crop(
                (
                    column * CELL,
                    row * CELL,
                    (column + 1) * CELL,
                    (row + 1) * CELL,
                ),
            )
            bbox = cell.getchannel("A").getbbox()
            if bbox is None:
                raise ValueError(f"empty production cell: {path} ({column}, {row})")
            if bbox[0] < 4 or bbox[1] < 4 or bbox[2] > CELL - 4 or bbox[3] > CELL - 4:
                raise ValueError(f"sprite is too close to edge: {path} ({column}, {row}) {bbox}")
            digest = cell.tobytes()
            if digest in hashes:
                raise ValueError(f"duplicate production cell: {path} ({column}, {row})")
            hashes.add(digest)


def main() -> None:
    family = build_family_sheet()
    legend = build_legend_sheet()
    validate_unique_cells(family, FAMILY_COLUMNS, len(FAMILIES))
    validate_unique_cells(legend, LEGEND_COLUMNS, 1)
    print(f"Wrote {family}")
    print(f"Wrote {legend}")
    print("Validated 177 non-empty, edge-safe, byte-unique per-level sprites")


if __name__ == "__main__":
    main()
