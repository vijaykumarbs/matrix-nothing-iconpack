#!/usr/bin/env python3
"""Render a contact sheet of every glyph so the artwork can be reviewed."""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))

from dotlib import GRID, fit  # noqa: E402
from glyphs import G  # noqa: E402

CELL = 128
LABEL_H = 18
COLS = 9
BG = (10, 11, 12)
TILE = (17, 19, 20)
DOT = (59, 245, 160)
TEXT = (150, 156, 160)

FONT_PATHS = [
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/System/Library/Fonts/Helvetica.ttc",
    "/System/Library/Fonts/SFNSMono.ttf",
]


def load_font(size: int):
    for p in FONT_PATHS:
        if Path(p).exists():
            try:
                return ImageFont.truetype(p, size)
            except OSError:
                continue
    return ImageFont.load_default()


def render_glyph(dots, size: int = CELL):
    """Draw dots using the same geometry the vector resources use."""
    scale = 4
    img = Image.new("RGBA", (size * scale, size * scale), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    step = 3.8 * (size * scale / 108.0)
    cx = cy = (GRID - 1) / 2.0
    r = 1.3 * (size * scale / 108.0)
    ox = oy = size * scale / 2.0
    for (x, y) in sorted(dots):
        px = ox + (x - cx) * step
        py = oy + (y - cy) * step
        d.ellipse((px - r, py - r, px + r, py + r), fill=DOT + (255,))
    return img.resize((size, size), Image.LANCZOS)


def main() -> None:
    names = sorted(G)
    rows = (len(names) + COLS - 1) // COLS
    sheet = Image.new("RGB", (COLS * CELL, rows * (CELL + LABEL_H)), BG)
    d = ImageDraw.Draw(sheet)
    font = load_font(11)
    for i, name in enumerate(names):
        gx, gy = i % COLS, i // COLS
        x0, y0 = gx * CELL, gy * (CELL + LABEL_H)
        d.rounded_rectangle((x0 + 6, y0 + 4, x0 + CELL - 6, y0 + CELL - 4), radius=26, fill=TILE)
        sheet.paste(render_glyph(fit(G[name])), (x0, y0), render_glyph(fit(G[name])))
        d.text((x0 + CELL // 2, y0 + CELL + 2), name, font=font, fill=TEXT, anchor="ma")
    out = Path(__file__).parent / "preview.png"
    sheet.save(out)
    print(f"{len(names)} glyphs -> {out} ({sheet.width}x{sheet.height})")


if __name__ == "__main__":
    main()
