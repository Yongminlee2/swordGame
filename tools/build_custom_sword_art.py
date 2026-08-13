"""Build faithful per-level sword sheets from the approved evolution grids.

Every enhancement level owns a genuinely different source sprite.  Production
processing is deliberately limited to green-screen removal, transparent cropping,
and a high-quality fit inside the atlas cell.  It does not quantize the palette,
snap alpha, or enlarge a tiny logical image with nearest-neighbour filtering.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "art-review" / "sword-evolution-v2" / "raw-generated"
OUTPUT_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

CELL = 328
FAMILY_COLUMNS = 21
LEGEND_COLUMNS = 30
FAMILY_ATLAS_COLUMNS = 7
LEGEND_ATLAS_COLUMNS = 10
ART_MAX = 300
SAFE_CELL_MARGIN = 12
COMPONENT_ALPHA = 72
MIN_SPRITE_PIXELS = 1_000
SOURCE_PAD = 5


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

    # Remove green spill from anti-aliased edge pixels.  The source prompt has
    # no green sword parts, so this only cleans the chroma matte; opaque source
    # colours and all original shading remain otherwise untouched.
    opaque = alpha > 0
    green_dominant = opaque & (green > red + 10.0) & (green > blue + 10.0)
    # Do not recolour the edge to the UI accent.  That previous workaround left
    # a cyan contour around the blade.  Neutralising only the excess green keeps
    # the source's red/blue values and therefore its original black outline.
    neutral_green = np.maximum(red, blue)
    rgb[..., 1] = np.where(green_dominant, neutral_green, green)
    rgba = np.dstack((rgb.astype(np.uint8), alpha))
    return Image.fromarray(rgba, mode="RGBA")


def connected_components(source: Image.Image) -> list[tuple[list[int], tuple[int, int, int, int]]]:
    """Return substantial connected alpha components and their source bounds."""
    alpha = np.asarray(source.getchannel("A"))
    mask = alpha > COMPONENT_ALPHA
    height, width = mask.shape
    visited = np.zeros(mask.size, dtype=np.bool_)
    flat_mask = mask.reshape(-1)
    components: list[tuple[list[int], tuple[int, int, int, int]]] = []

    for raw_start in np.flatnonzero(flat_mask):
        start = int(raw_start)
        if visited[start]:
            continue
        visited[start] = True
        stack = [start]
        component: list[int] = []
        min_x, min_y, max_x, max_y = width, height, 0, 0
        while stack:
            point = stack.pop()
            component.append(point)
            y, x = divmod(point, width)
            min_x, min_y = min(min_x, x), min(min_y, y)
            max_x, max_y = max(max_x, x + 1), max(max_y, y + 1)
            for ny in range(max(0, y - 1), min(height, y + 2)):
                row = ny * width
                for nx in range(max(0, x - 1), min(width, x + 2)):
                    neighbour = row + nx
                    if not visited[neighbour] and flat_mask[neighbour]:
                        visited[neighbour] = True
                        stack.append(neighbour)
        if len(component) >= MIN_SPRITE_PIXELS:
            components.append((component, (min_x, min_y, max_x, max_y)))
    return components


def split_grid(path: Path, columns: int, rows: int) -> list[Image.Image]:
    """Extract whole silhouettes, even when artwork crosses an ideal grid line.

    The generated contact sheets are visually arranged as a grid, but several
    sword tips and pommels extend into the mathematical neighbour cell. Cropping
    fixed rectangles first therefore amputates otherwise valid source pixels.
    Each sword is already an isolated alpha component after key removal, so find
    those components on the full sheet and order them by their nearest grid slot.
    """
    source = remove_green_screen(Image.open(path))
    expected = columns * rows
    components = connected_components(source)
    if len(components) != expected:
        raise ValueError(
            f"expected {expected} sword silhouettes, found {len(components)}: {path}",
        )

    slots: dict[int, tuple[list[int], tuple[int, int, int, int]]] = {}
    for component, bbox in components:
        center_x = (bbox[0] + bbox[2]) / 2.0
        center_y = (bbox[1] + bbox[3]) / 2.0
        column = min(columns - 1, max(0, int(center_x * columns / source.width)))
        row = min(rows - 1, max(0, int(center_y * rows / source.height)))
        slot = row * columns + column
        if slot in slots:
            raise ValueError(f"two silhouettes mapped to grid slot {slot}: {path}")
        slots[slot] = (component, bbox)

    missing = sorted(set(range(expected)) - slots.keys())
    if missing:
        raise ValueError(f"missing grid slots {missing}: {path}")

    source_alpha = source.getchannel("A")
    sprites: list[Image.Image] = []
    for index in range(expected):
        component, bbox = slots[index]
        keep = np.zeros(source.width * source.height, dtype=np.uint8)
        keep[component] = 255
        keep_image = Image.fromarray(
            keep.reshape((source.height, source.width)),
            mode="L",
        ).filter(ImageFilter.MaxFilter(5))
        isolated = source.copy()
        isolated.putalpha(ImageChops.multiply(source_alpha, keep_image))
        x0 = max(0, bbox[0] - SOURCE_PAD)
        y0 = max(0, bbox[1] - SOURCE_PAD)
        x1 = min(source.width, bbox[2] + SOURCE_PAD)
        y1 = min(source.height, bbox[3] + SOURCE_PAD)
        sprites.append(isolated.crop((x0, y0, x1, y1)))
    return sprites


def keep_largest_component(source: Image.Image) -> Image.Image:
    """Drop keying dust and fragments from neighbouring grid cells."""
    alpha = np.asarray(source.getchannel("A"))
    mask = alpha > COMPONENT_ALPHA
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
    ratio = min(1.0, ART_MAX / max(source.width, source.height))
    fitted_size = (
        max(1, round(source.width * ratio)),
        max(1, round(source.height * ratio)),
    )
    sprite = source if fitted_size == source.size else resize_rgba(source, fitted_size)

    canvas = Image.new("RGBA", (CELL, CELL), (0, 0, 0, 0))
    offset = ((CELL - sprite.width) // 2, (CELL - sprite.height) // 2)
    canvas.alpha_composite(sprite, offset)
    return canvas


def build_family_sheets() -> list[Path]:
    outputs: list[Path] = []
    for spec in FAMILIES:
        sheet = Image.new(
            "RGBA",
            (FAMILY_ATLAS_COLUMNS * CELL, 3 * CELL),
            (0, 0, 0, 0),
        )
        sprites = split_grid(SOURCE_DIR / spec.filename, columns=7, rows=3)
        if len(sprites) != FAMILY_COLUMNS:
            raise ValueError(f"{spec.family_id}: expected 21 sprites, got {len(sprites)}")
        for level, source in enumerate(sprites):
            atlas_row, atlas_column = divmod(level, FAMILY_ATLAS_COLUMNS)
            sheet.alpha_composite(
                render_cell(source),
                (atlas_column * CELL, atlas_row * CELL),
            )
        output = OUTPUT_DIR / f"sword_custom_{spec.family_id}_sheet.png"
        output.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(output, optimize=True)
        outputs.append(output)
    return outputs


def build_legend_sheet() -> Path:
    generated = split_grid(SOURCE_DIR / "legend-grid-32.png", columns=8, rows=4)
    # The image model returned 32 strong candidates.  The first 29 preserve the
    # intended +21/+30/+40 milestones; the most ornate final candidate is +50.
    selected = generated[:29] + [generated[31]]
    if len(selected) != LEGEND_COLUMNS:
        raise ValueError(f"legend: expected 30 sprites, got {len(selected)}")

    sheet = Image.new(
        "RGBA",
        (
            LEGEND_ATLAS_COLUMNS * CELL,
            ((LEGEND_COLUMNS + LEGEND_ATLAS_COLUMNS - 1) // LEGEND_ATLAS_COLUMNS) * CELL,
        ),
        (0, 0, 0, 0),
    )
    for column, source in enumerate(selected):
        atlas_row, atlas_column = divmod(column, LEGEND_ATLAS_COLUMNS)
        sheet.alpha_composite(
            render_cell(source),
            (atlas_column * CELL, atlas_row * CELL),
        )

    output = OUTPUT_DIR / "sword_custom_legend_sheet.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, optimize=True)
    return output


def validate_unique_cells(
    path: Path,
    columns: int,
    count: int,
    hashes: set[bytes],
) -> None:
    sheet = Image.open(path).convert("RGBA")
    for index in range(count):
        row, column = divmod(index, columns)
        cell = sheet.crop(
            (
                column * CELL,
                row * CELL,
                (column + 1) * CELL,
                (row + 1) * CELL,
            )
        )
        bbox = cell.getchannel("A").getbbox()
        if bbox is None:
            raise ValueError(f"empty production cell: {path} ({column}, {row})")
        if (
            bbox[0] < SAFE_CELL_MARGIN
            or bbox[1] < SAFE_CELL_MARGIN
            or bbox[2] > CELL - SAFE_CELL_MARGIN
            or bbox[3] > CELL - SAFE_CELL_MARGIN
        ):
            raise ValueError(f"sprite is too close to edge: {path} ({column}, {row}) {bbox}")
        digest = cell.tobytes()
        if digest in hashes:
            raise ValueError(f"duplicate production cell: {path} ({column}, {row})")
        hashes.add(digest)


def main() -> None:
    families = build_family_sheets()
    legend = build_legend_sheet()
    hashes: set[bytes] = set()
    for family in families:
        validate_unique_cells(family, FAMILY_ATLAS_COLUMNS, FAMILY_COLUMNS, hashes)
    validate_unique_cells(legend, LEGEND_ATLAS_COLUMNS, LEGEND_COLUMNS, hashes)
    for family in families:
        print(f"Wrote {family}")
    print(f"Wrote {legend}")
    print("Validated 177 native-detail, non-empty, edge-safe, byte-unique sprites")


if __name__ == "__main__":
    main()
