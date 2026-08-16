#!/usr/bin/env python3
"""Build production monster and combat-effect drawables from ImageGen atlases."""

from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "art-review" / "combat-redesign" / "transparent-source"
REPLACEMENT_SOURCE = ROOT / "tools" / "art-source" / "combat"
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable"

MONSTER_CELL = 384
MONSTER_COLS = 3
MONSTER_ROWS = 2
MONSTER_PADDING = 18
MONSTER_BOSS_PADDING = 18
SOURCE_INSET = 4
EFFECT_CELL = 512
EFFECT_PADDING = 10

MONSTER_GROUPS = (
    "monster-atlas-00-meadow-forest-cave.png",
    "monster-atlas-01-mine-swamp-volcano.png",
    "monster-atlas-02-snow-dragon-abyss.png",
    "monster-atlas-03-hall-sky-capital.png",
    "monster-atlas-04-temple-desert-isle.png",
    "monster-atlas-05-wood-sunken-ash.png",
    "monster-atlas-06-star-time-blood.png",
    "monster-atlas-07-frost-forge-final.png",
)

# These ImageGen review atlases have no cell borders. Their characters are not
# perfectly centred on a mathematical 6 x 3 grid, so a grid crop cuts limbs and
# weapons. Extract their 18 real connected subjects from the entire atlas first.
COMPONENT_GROUPS = frozenset({0, 1, 3, 4, 5})

# The review originals for these two bosses were close-up compositions rather
# than complete silhouettes. Their full-body reconstructions live in the repo so
# rebuilding the combat atlases remains deterministic on another machine.
MONSTER_REPLACEMENTS = {
    (8, 5): REPLACEMENT_SOURCE / "monster_abyss_king.png",
    (15, 5): REPLACEMENT_SOURCE / "monster_twisted_root.png",
}

EFFECT_NAMES = (
    "normal",
    "critical",
    "flash",
    "moonfall",
    "collapse",
    "flurry",
    "twinmoon",
    "drain",
    "judgment",
    "dragonbreath",
    "reap",
    "crush",
    "pierce",
    "spiritburst",
    "allthings",
    "voidcall",
)


def grid_crop(image: Image.Image, col: int, row: int, cols: int, rows: int) -> Image.Image:
    """Crop a logical cell without losing remainder pixels at the far edge."""
    left = round(col * image.width / cols)
    right = round((col + 1) * image.width / cols)
    top = round(row * image.height / rows)
    bottom = round((row + 1) * image.height / rows)
    return image.crop((left, top, right, bottom))


def alpha_bbox(image: Image.Image, threshold: int = 8) -> tuple[int, int, int, int]:
    alpha = image.getchannel("A").point(lambda value: 255 if value > threshold else 0)
    return alpha.getbbox() or (0, 0, image.width, image.height)


def largest_component(image: Image.Image, threshold: int = 18) -> Image.Image:
    """Keep the dominant keyed subject and discard spill from neighboring grid cells."""
    rgba = np.asarray(image).copy()
    mask = rgba[:, :, 3] > threshold
    height, width = mask.shape
    seen = np.zeros_like(mask, dtype=bool)
    best: list[tuple[int, int]] = []
    for start_y, start_x in zip(*np.nonzero(mask & ~seen)):
        if seen[start_y, start_x]:
            continue
        stack = [(int(start_y), int(start_x))]
        seen[start_y, start_x] = True
        component: list[tuple[int, int]] = []
        while stack:
            y, x = stack.pop()
            component.append((y, x))
            for next_y, next_x in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
                if 0 <= next_y < height and 0 <= next_x < width:
                    if mask[next_y, next_x] and not seen[next_y, next_x]:
                        seen[next_y, next_x] = True
                        stack.append((next_y, next_x))
        if len(component) > len(best):
            best = component
    if not best:
        return image
    keep = np.zeros_like(mask)
    for y, x in best:
        keep[y, x] = True
    rgba[:, :, 3] = np.where(keep, rgba[:, :, 3], 0)
    return Image.fromarray(rgba, "RGBA")


def connected_subjects(image: Image.Image, expected: int = 18) -> list[Image.Image]:
    """Extract complete subjects without assuming hard atlas cell boundaries.

    ImageGen laid out six characters on each of three rows, but several arms,
    wings and weapons cross the equal-grid midpoint. We label the entire atlas,
    use its 18 large connected components as subject anchors, and attach nearby
    detached particles or props to the closest anchor before making each cutout.
    """
    rgba = np.asarray(image).copy()
    mask = rgba[:, :, 3] > 8
    height, width = mask.shape
    labels = np.zeros((height, width), dtype=np.int16)
    components: list[dict[str, int]] = []
    label = 0

    for start_y, start_x in zip(*np.nonzero(mask)):
        if labels[start_y, start_x] != 0:
            continue
        label += 1
        stack = [(int(start_y), int(start_x))]
        labels[start_y, start_x] = label
        size = 0
        min_x = max_x = int(start_x)
        min_y = max_y = int(start_y)
        sum_x = 0
        sum_y = 0
        while stack:
            y, x = stack.pop()
            size += 1
            sum_x += x
            sum_y += y
            min_x = min(min_x, x)
            max_x = max(max_x, x)
            min_y = min(min_y, y)
            max_y = max(max_y, y)
            for next_y, next_x in (
                (y - 1, x),
                (y + 1, x),
                (y, x - 1),
                (y, x + 1),
            ):
                if 0 <= next_y < height and 0 <= next_x < width:
                    if mask[next_y, next_x] and labels[next_y, next_x] == 0:
                        labels[next_y, next_x] = label
                        stack.append((next_y, next_x))
        components.append(
            {
                "label": label,
                "size": size,
                "min_x": min_x,
                "min_y": min_y,
                "max_x": max_x,
                "max_y": max_y,
                "center_x": sum_x // size,
                "center_y": sum_y // size,
            }
        )

    anchors = sorted(components, key=lambda component: component["size"], reverse=True)[:expected]
    if len(anchors) != expected or anchors[-1]["size"] < 5_000:
        raise ValueError(
            f"expected {expected} complete monster components, got "
            f"{len(anchors)} with smallest={anchors[-1]['size'] if anchors else 0}",
        )

    # The montage contract is three visual rows of six. Sorting the actual
    # component centres, rather than cutting at row/column midpoints, preserves
    # every pixel that crossed a nominal grid boundary.
    by_y = sorted(anchors, key=lambda component: component["center_y"])
    ordered: list[dict[str, int]] = []
    for row in range(3):
        ordered.extend(
            sorted(by_y[row * 6 : (row + 1) * 6], key=lambda component: component["center_x"]),
        )

    slot_by_label = np.zeros(label + 1, dtype=np.int16)
    for slot, anchor in enumerate(ordered, start=1):
        slot_by_label[anchor["label"]] = slot

    # Preserve detached effects, floating shards and weapon pieces when they are
    # close to a subject. Tiny distant keying specks stay discarded.
    anchor_labels = {anchor["label"] for anchor in anchors}
    for component in components:
        if component["label"] in anchor_labels or component["size"] < 8:
            continue

        def gap(anchor: dict[str, int]) -> int:
            dx = max(
                anchor["min_x"] - component["max_x"],
                component["min_x"] - anchor["max_x"],
                0,
            )
            dy = max(
                anchor["min_y"] - component["max_y"],
                component["min_y"] - anchor["max_y"],
                0,
            )
            return dx * dx + dy * dy

        nearest = min(ordered, key=gap)
        if gap(nearest) <= 96 * 96:
            slot_by_label[component["label"]] = ordered.index(nearest) + 1

    sprites: list[Image.Image] = []
    for slot in range(1, expected + 1):
        selected = slot_by_label[labels] == slot
        ys, xs = np.nonzero(selected)
        if len(xs) == 0:
            raise ValueError(f"empty extracted monster slot: {slot - 1}")
        left = int(xs.min())
        top = int(ys.min())
        right = int(xs.max()) + 1
        bottom = int(ys.max()) + 1
        crop = rgba[top:bottom, left:right].copy()
        crop[:, :, 3] = np.where(selected[top:bottom, left:right], crop[:, :, 3], 0)
        sprites.append(Image.fromarray(crop, "RGBA"))
    return sprites


def clear_source_frame(image: Image.Image, inset: int = SOURCE_INSET) -> Image.Image:
    """Remove the generated white grid stroke before subject extraction."""
    rgba = np.asarray(image).copy()
    rgba[:inset, :, 3] = 0
    rgba[-inset:, :, 3] = 0
    rgba[:, :inset, 3] = 0
    rgba[:, -inset:, 3] = 0
    return Image.fromarray(rgba, "RGBA")


def remove_green_spill(image: Image.Image) -> Image.Image:
    """Remove generated chroma reflections while preserving cyan, yellow, and white VFX."""
    rgba = np.asarray(image).copy()
    red = rgba[:, :, 0].astype(np.int16)
    green = rgba[:, :, 1].astype(np.int16)
    blue = rgba[:, :, 2].astype(np.int16)
    spill = (green - red > 38) & (green - blue > 38)
    rgba[spill, 3] = 0
    return Image.fromarray(rgba, "RGBA")


def fitted(image: Image.Image, side: int, padding: int) -> Image.Image:
    """Trim keyed space and fit the source into a square without distortion."""
    crop = image.crop(alpha_bbox(image))
    limit = side - padding * 2
    scale = min(limit / crop.width, limit / crop.height)
    size = (max(1, round(crop.width * scale)), max(1, round(crop.height * scale)))
    resized = crop.resize(size, Image.Resampling.LANCZOS)
    result = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    result.alpha_composite(resized, ((side - size[0]) // 2, (side - size[1]) // 2))
    return result


def build_monsters() -> None:
    zone = 0
    for group_index, filename in enumerate(MONSTER_GROUPS):
        atlas = Image.open(SOURCE / filename).convert("RGBA")
        subjects = connected_subjects(atlas) if group_index in COMPONENT_GROUPS else None
        for source_row in range(3):
            sheet = Image.new(
                "RGBA",
                (MONSTER_CELL * MONSTER_COLS, MONSTER_CELL * MONSTER_ROWS),
                (0, 0, 0, 0),
            )
            for monster in range(6):
                replacement = MONSTER_REPLACEMENTS.get((zone, monster))
                if replacement is not None:
                    source = Image.open(replacement).convert("RGBA")
                elif subjects is not None:
                    source = subjects[source_row * 6 + monster]
                else:
                    source = clear_source_frame(grid_crop(atlas, monster, source_row, 6, 3))
                    source = largest_component(source)
                padding = MONSTER_BOSS_PADDING if monster == 5 else MONSTER_PADDING
                cell = fitted(source, MONSTER_CELL, padding)
                x = (monster % MONSTER_COLS) * MONSTER_CELL
                y = (monster // MONSTER_COLS) * MONSTER_CELL
                sheet.alpha_composite(cell, (x, y))
            target = DRAWABLE / f"monster_zone_{zone:02d}.webp"
            sheet.save(target, "WEBP", quality=94, method=3, exact=True)
            for monster in range(6):
                x = (monster % MONSTER_COLS) * MONSTER_CELL
                y = (monster // MONSTER_COLS) * MONSTER_CELL
                cell = sheet.crop((x, y, x + MONSTER_CELL, y + MONSTER_CELL))
                bbox = cell.getchannel("A").getbbox()
                safe = min(MONSTER_PADDING, MONSTER_BOSS_PADDING) - 1
                if bbox is None or min(
                    bbox[0], bbox[1], MONSTER_CELL - bbox[2], MONSTER_CELL - bbox[3],
                ) < safe:
                    raise ValueError(
                        f"unsafe monster edge: zone={zone} cell={monster} bbox={bbox}",
                    )
            print(f"wrote {target.relative_to(ROOT)}")
            zone += 1


def build_effects() -> None:
    atlas = remove_green_spill(Image.open(SOURCE / "combat-effects-atlas.png").convert("RGBA"))
    for index, name in enumerate(EFFECT_NAMES):
        source = grid_crop(atlas, index % 4, index // 4, 4, 4)
        effect = fitted(source, EFFECT_CELL, EFFECT_PADDING)
        target = DRAWABLE / f"combat_fx_{name}.webp"
        effect.save(target, "WEBP", quality=95, method=3, exact=True)
        print(f"wrote {target.relative_to(ROOT)}")

    banner = Image.open(SOURCE / "combat-skill-banner.png").convert("RGBA")
    banner = banner.crop(alpha_bbox(banner))
    target_width = 960
    target_height = round(banner.height * target_width / banner.width)
    banner = banner.resize((target_width, target_height), Image.Resampling.LANCZOS)
    banner_target = DRAWABLE / "combat_skill_banner.webp"
    banner.save(banner_target, "WEBP", quality=95, method=3, exact=True)
    print(f"wrote {banner_target.relative_to(ROOT)}")


if __name__ == "__main__":
    DRAWABLE.mkdir(parents=True, exist_ok=True)
    build_monsters()
    build_effects()
