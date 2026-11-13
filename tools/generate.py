#!/usr/bin/env python3
"""Generate every Android resource for the Matrix icon pack.

Runs from the repo root:  python3 tools/generate.py

Outputs
-------
app/src/main/res/drawable/fg_*.xml            dot foreground (white, tintable)
app/src/main/res/drawable-anydpi-v26/ic_*.xml adaptive icon (background+foreground+mono)
app/src/main/res/xml/appfilter.xml            launcher component -> icon map
app/src/main/res/xml/drawable.xml             icon picker index
app/src/<flavor>/res/...                      accent tint + tile for each style
app/src/main/java/.../IconCatalog.java        zero-lookup icon index for the UI
docs/media/*.png                              marketing previews
"""

from __future__ import annotations

import json
import shutil
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).parent))

from catalog import APPS  # noqa: E402
from dotlib import GRID, fit  # noqa: E402
from glyphs import G  # noqa: E402
from letters import letter  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "app"
MAIN_RES = APP / "src" / "main" / "res"
JAVA_DIR = APP / "src" / "main" / "java" / "dev" / "vijaykumarbs" / "matrix"
DOCS_MEDIA = ROOT / "docs" / "media"
TOOLS = ROOT / "tools"

VIEWPORT = 108
GRID_STEP = 3.8
DOT_STROKE = 2.6  # diameter; matches the preview renderer

STYLES = [
    dict(id="phosphorGlass", name="Phosphor Glass", accent="#3BF5A0", glass=True, slug="phosphor-glass"),
    dict(id="phosphorFlat", name="Phosphor Flat", accent="#3BF5A0", glass=False, slug="phosphor-flat"),
    dict(id="emberGlass", name="Ember Glass", accent="#FF5540", glass=True, slug="ember-glass"),
    dict(id="emberFlat", name="Ember Flat", accent="#FF5540", glass=False, slug="ember-flat"),
    dict(id="iceGlass", name="Ice Glass", accent="#E9F0F6", glass=True, slug="ice-glass"),
    dict(id="iceFlat", name="Ice Flat", accent="#E9F0F6", glass=False, slug="ice-flat"),
    dict(id="amberGlass", name="Amber Glass", accent="#FFB454", glass=True, slug="amber-glass"),
    dict(id="amberFlat", name="Amber Flat", accent="#FFB454", glass=False, slug="amber-flat"),
]

HEADER = '<?xml version="1.0" encoding="utf-8"?>\n'


# --------------------------------------------------------------------------
# geometry
# --------------------------------------------------------------------------
def dot_path(dots) -> str:
    cx = cy = (GRID - 1) / 2.0
    parts = []
    for x, y in sorted(dots):
        px = VIEWPORT / 2 + (x - cx) * GRID_STEP
        py = VIEWPORT / 2 + (y - cy) * GRID_STEP
        parts.append(f"M{px:.2f},{py:.2f}h.01")
    return "".join(parts)


def foreground_vector(dots, color: str = "#FFFFFFFF") -> str:
    return (
        HEADER
        + '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        + '    android:width="108dp"\n    android:height="108dp"\n'
        + '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        + '    <path\n        android:pathData="%s"\n' % dot_path(dots)
        + '        android:strokeColor="%s"\n' % color
        + '        android:strokeWidth="%.1f"\n' % DOT_STROKE
        + '        android:strokeLineCap="round" />\n'
        + "</vector>\n"
    )


def adaptive_icon(name: str) -> str:
    return (
        HEADER
        + '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        + '    <background android:drawable="@drawable/bg_tile" />\n'
        + '    <foreground android:drawable="@drawable/tint_%s" />\n' % name
        + '    <monochrome android:drawable="@drawable/fg_%s" />\n' % name
        + "</adaptive-icon>\n"
    )


def app_launcher_icon() -> str:
    return (
        HEADER
        + '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        + '    <background android:drawable="@drawable/bg_tile" />\n'
        + '    <foreground android:drawable="@drawable/ic_launcher_fg" />\n'
        + '    <monochrome android:drawable="@drawable/ic_launcher_fg" />\n'
        + "</adaptive-icon>\n"
    )


def tint_wrapper(name: str) -> str:
    return (
        HEADER
        + '<layer-list xmlns:android="http://schemas.android.com/apk/res/android">\n'
        + '    <item android:drawable="@drawable/fg_%s" android:tint="@color/accent" />\n' % name
        + "</layer-list>\n"
    )


def tile_vector(accent: str, glass: bool) -> str:
    """Adaptive-icon background. Glass = smoked, softly lit panel."""
    aapt = 'xmlns:aapt="http://schemas.android.com/aapt"'
    if not glass:
        return (
            HEADER
            + '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            + f'    {aapt}\n    android:width="108dp"\n    android:height="108dp"\n'
            + '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
            + '    <path android:pathData="M0,0h108v108h-108z">\n'
            + '        <aapt:attr name="android:fillColor">\n'
            + '            <gradient android:type="linear" android:startX="0" android:startY="0"\n'
            + '                android:endX="0" android:endY="108">\n'
            + '                <item android:offset="0" android:color="#FF101214" />\n'
            + '                <item android:offset="1" android:color="#FF07080A" />\n'
            + "            </gradient>\n        </aapt:attr>\n    </path>\n</vector>\n"
        )
    return (
        HEADER
        + '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        + f'    {aapt}\n    android:width="108dp"\n    android:height="108dp"\n'
        + '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        # smoked base
        + '    <path android:pathData="M0,0h108v108h-108z">\n'
        + '        <aapt:attr name="android:fillColor">\n'
        + '            <gradient android:type="linear" android:startX="0" android:startY="0"\n'
        + '                android:endX="108" android:endY="108">\n'
        + '                <item android:offset="0" android:color="#FF191D20" />\n'
        + '                <item android:offset="0.55" android:color="#FF0C0E10" />\n'
        + '                <item android:offset="1" android:color="#FF07080A" />\n'
        + "            </gradient>\n        </aapt:attr>\n    </path>\n"
        # specular sheen from the top-left
        + '    <path android:pathData="M0,0h108v108h-108z">\n'
        + '        <aapt:attr name="android:fillColor">\n'
        + '            <gradient android:type="radial" android:centerX="26" android:centerY="18"\n'
        + '                android:gradientRadius="92">\n'
        + '                <item android:offset="0" android:color="#24FFFFFF" />\n'
        + '                <item android:offset="0.45" android:color="#0AFFFFFF" />\n'
        + '                <item android:offset="1" android:color="#00FFFFFF" />\n'
        + "            </gradient>\n        </aapt:attr>\n    </path>\n"
        # accent bounce from the bottom-right
        + '    <path android:pathData="M0,0h108v108h-108z">\n'
        + '        <aapt:attr name="android:fillColor">\n'
        + '            <gradient android:type="radial" android:centerX="92" android:centerY="96"\n'
        + '                android:gradientRadius="86">\n'
        + f'                <item android:offset="0" android:color="#22{accent[1:]}" />\n'
        + '                <item android:offset="0.55" android:color="#08' + accent[1:] + '" />\n'
        + '                <item android:offset="1" android:color="#00' + accent[1:] + '" />\n'
        + "            </gradient>\n        </aapt:attr>\n    </path>\n"
        # edge vignette for glass thickness
        + '    <path android:pathData="M0,0h108v108h-108z">\n'
        + '        <aapt:attr name="android:fillColor">\n'
        + '            <gradient android:type="radial" android:centerX="54" android:centerY="48"\n'
        + '                android:gradientRadius="78">\n'
        + '                <item android:offset="0.55" android:color="#00000000" />\n'
        + '                <item android:offset="1" android:color="#59000000" />\n'
        + "            </gradient>\n        </aapt:attr>\n    </path>\n"
        + "</vector>\n"
    )


def launcher_foreground(accent: str) -> str:
    return foreground_vector(fit(G["matrix"]), accent)


def colors_xml(accent: str) -> str:
    return (
        HEADER
        + "<resources>\n"
        + f'    <color name="accent">{accent}</color>\n'
        + f'    <color name="accent_soft">#33{accent[1:]}</color>\n'
        + '    <color name="tile">#FF0B0C0D</color>\n'
        + "</resources>\n"
    )


# --------------------------------------------------------------------------
# writers
# --------------------------------------------------------------------------
def verify_catalog() -> None:
    names = [a[0] for a in APPS]
    assert len(names) == len(set(names)), "duplicate drawable names in catalog"
    for name, label, glyph, _cat, comps in APPS:
        assert glyph in G, f"{name}: unknown glyph {glyph}"
        assert comps, f"{name}: no components"
        for comp in comps:
            assert "/" in comp, comp
            pkg, cls = comp.split("/", 1)
            assert "." in pkg and "." in (cls or "."), comp
    assert len(APPS) == len({a[0] for a in APPS})


def normalize_component(comp: str) -> str:
    pkg, cls = comp.split("/", 1)
    if cls.startswith("."):
        cls = pkg + cls
    elif cls == "":
        cls = pkg
    return f"{pkg}/{cls}"


def write_main_resources() -> None:
    drawables = MAIN_RES / "drawable"
    anydpi = MAIN_RES / "drawable-anydpi-v26"
    xml = MAIN_RES / "xml"
    for d in (drawables, anydpi, xml):
        d.mkdir(parents=True, exist_ok=True)
    # Only remove files this script owns; hand-written UI drawables stay put.
    for p in drawables.glob("fg_*.xml"):
        p.unlink()
    for p in anydpi.glob("ic_*.xml"):
        p.unlink()
    for p in xml.glob("*.xml"):
        p.unlink()

    for app in APPS:
        name, _label, glyph = app[0], app[1], app[2]
        dots = fit(G[glyph])
        (drawables / f"fg_{name}.xml").write_text(foreground_vector(dots))
        (anydpi / f"ic_{name}.xml").write_text(adaptive_icon(name))
    (anydpi / "ic_launcher.xml").write_text(app_launcher_icon())

    items = "\n".join(
        f'    <item component="ComponentInfo{{{normalize_component(c)}}}" drawable="ic_{a[0]}" />'
        for a in APPS
        for c in a[4]
    )
    (xml / "appfilter.xml").write_text(
        HEADER + "<resources>\n" + items + "\n</resources>\n"
    )

    by_cat: dict[str, list[str]] = {}
    for name, _label, _glyph, cat, _comps in APPS:
        by_cat.setdefault(cat, []).append(name)
    chunks = []
    for cat, names in by_cat.items():
        chunks.append(f'    <category title="{cat}" />')
        chunks.extend(f'    <item drawable="ic_{n}" />' for n in names)
    (xml / "drawable.xml").write_text(
        HEADER + "<resources>\n" + "\n".join(chunks) + "\n</resources>\n"
    )

    labels = ",\n        ".join(json.dumps(a[1]) for a in APPS)
    cats = ",\n        ".join(json.dumps(a[3]) for a in APPS)
    names = ",\n        ".join(json.dumps(a[0]) for a in APPS)
    res = ",\n        ".join(f"R.drawable.ic_{a[0]}" for a in APPS)
    (JAVA_DIR / "IconCatalog.java").write_text(
        "package dev.vijaykumarbs.matrix;\n\n"
        "/**\n * Generated by tools/generate.py. Do not edit.\n */\n"
        "public final class IconCatalog {\n"
        "    public static final String[] NAMES = {\n        " + names + "\n    };\n"
        "    public static final String[] LABELS = {\n        " + labels + "\n    };\n"
        "    public static final String[] CATEGORIES = {\n        " + cats + "\n    };\n"
        "    public static final int[] RES = {\n        " + res + "\n    };\n"
        "    public static final int COUNT = NAMES.length;\n\n"
        "    private IconCatalog() {\n    }\n}\n"
    )

    # Build-time style table so the UI never parses XML and never hits the network.
    style_rows = []
    for style in STYLES:
        rgb = style["accent"]
        style_rows.append(
            "            new Style(%s, %s, %s, 0xFF%s, %s, %s)"
            % (
                json.dumps(style["id"]),
                json.dumps(style["name"]),
                json.dumps(style["slug"]),
                rgb[1:],
                "true" if style["glass"] else "false",
                json.dumps(f"matrix-{style['slug']}.apk"),
            )
        )
    (JAVA_DIR / "Styles.java").write_text(
        "package dev.vijaykumarbs.matrix;\n\n"
        "/**\n * Generated by tools/generate.py. Do not edit.\n */\n"
        "public final class Styles {\n"
        "    public static final class Style {\n"
        "        public final String id;\n        public final String name;\n"
        "        public final String slug;\n        public final int accent;\n"
        "        public final boolean glass;\n        public final String apk;\n\n"
        "        Style(String id, String name, String slug, int accent, boolean glass, String apk) {\n"
        "            this.id = id;\n            this.name = name;\n            this.slug = slug;\n"
        "            this.accent = accent;\n            this.glass = glass;\n            this.apk = apk;\n"
        "        }\n    }\n\n"
        "    public static final Style[] ALL = {\n" + ",\n".join(style_rows) + "\n    };\n\n"
        "    public static Style current() {\n"
        "        for (Style s : ALL) {\n"
        "            if (s.id.equals(BuildConfig.STYLE_ID)) return s;\n        }\n"
        "        return ALL[0];\n    }\n\n"
        "    private Styles() {\n    }\n}\n"
    )

    packages = sorted({
        c.split("/", 1)[0] for a in APPS for c in a[4]
    })
    (JAVA_DIR / "ThemedPackages.java").write_text(
        "package dev.vijaykumarbs.matrix;\n\n"
        "/**\n * Generated by tools/generate.py. Do not edit.\n */\n"
        "public final class ThemedPackages {\n"
        "    public static final String[] ALL = {\n        "
        + ",\n        ".join(json.dumps(p) for p in packages)
        + "\n    };\n\n"
        "    public static boolean contains(String pkg) {\n"
        "        for (String p : ALL) {\n            if (p.equals(pkg)) return true;\n        }\n"
        "        return false;\n    }\n\n"
        "    private ThemedPackages() {\n    }\n}\n"
    )


def write_flavor_resources() -> None:
    for style in STYLES:
        res = APP / "src" / style["id"] / "res"
        (res / "values").mkdir(parents=True, exist_ok=True)
        (res / "drawable").mkdir(parents=True, exist_ok=True)
        for p in (res / "drawable").glob("tint_*.xml"):
            p.unlink()
        for name in ("bg_tile.xml", "ic_launcher_fg.xml"):
            p = res / "drawable" / name
            if p.exists():
                p.unlink()
        (res / "values" / "colors.xml").write_text(colors_xml(style["accent"]))
        (res / "drawable" / "bg_tile.xml").write_text(
            tile_vector(style["accent"], style["glass"])
        )
        (res / "drawable" / "ic_launcher_fg.xml").write_text(
            launcher_foreground(style["accent"])
        )
        for app in APPS:
            (res / "drawable" / f"tint_{app[0]}.xml").write_text(tint_wrapper(app[0]))
        # flavour name shown in the launcher's icon-pack list and app drawer
        escaped = style["name"].replace("&", "&amp;")
        (res / "values" / "strings.xml").write_text(
            HEADER
            + "<resources>\n"
            + f'    <string name="app_name">Matrix {escaped}</string>\n'
            + f'    <string name="pack_name">Matrix · {escaped}</string>\n'
            + "</resources>\n"
        )


# --------------------------------------------------------------------------
# previews
# --------------------------------------------------------------------------
def _paint_tile(img: Image.Image, accent: str, glass: bool) -> None:
    """Cheap approximation of the vector tile, used only for marketing art."""
    w, h = img.size
    d = ImageDraw.Draw(img)
    rgb = tuple(int(accent[i:i + 2], 16) for i in (1, 3, 5))
    if glass:
        for y in range(h):
            t = y / max(1, h - 1)
            base = (int(25 - 18 * t), int(29 - 21 * t), int(32 - 22 * t))
            d.line([(0, y), (w, y)], fill=base + (255,))
        glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        gd = ImageDraw.Draw(glow)
        for r in range(120, 0, -1):
            a = int(38 * (1 - r / 120) ** 2)
            gd.ellipse((w * 0.24 - r, h * 0.18 - r, w * 0.24 + r, h * 0.18 + r),
                       fill=(255, 255, 255, a))
        img.alpha_composite(glow)
        acc = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        ad = ImageDraw.Draw(acc)
        for r in range(140, 0, -1):
            a = int(30 * (1 - r / 140) ** 2)
            ad.ellipse((w * 0.86 - r, h * 0.9 - r, w * 0.86 + r, h * 0.9 + r),
                       fill=rgb + (a,))
        img.alpha_composite(acc)
    else:
        for y in range(h):
            t = y / max(1, h - 1)
            d.line([(0, y), (w, y)], fill=(int(16 - 9 * t), int(18 - 10 * t), int(20 - 10 * t), 255))


def render_icon(name: str, size: int, accent: str, glass: bool, framed: bool = True) -> Image.Image:
    dots = fit(G[next(a[2] for a in APPS if a[0] == name)])
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    if framed:
        _paint_tile(img, accent, glass)
    scale = 4
    dot_img = Image.new("RGBA", (size * scale, size * scale), (0, 0, 0, 0))
    d = ImageDraw.Draw(dot_img)
    step = GRID_STEP * (size * scale / VIEWPORT)
    r = (DOT_STROKE / 2) * (size * scale / VIEWPORT)
    rgb = tuple(int(accent[i:i + 2], 16) for i in (1, 3, 5))
    c = (GRID - 1) / 2.0
    o = size * scale / 2.0
    for (x, y) in sorted(dots):
        px, py = o + (x - c) * step, o + (y - c) * step
        d.ellipse((px - r, py - r, px + r, py + r), fill=rgb + (255,))
    img.alpha_composite(dot_img.resize((size, size), Image.LANCZOS))
    if framed:
        mask = Image.new("L", (size * 4, size * 4), 0)
        ImageDraw.Draw(mask).rounded_rectangle(
            (0, 0, size * 4 - 1, size * 4 - 1), radius=int(size * 4 * 0.225), fill=255
        )
        mask = mask.resize((size, size), Image.LANCZOS)
        img.putalpha(Image.composite(img.getchannel("A"), Image.new("L", (size, size), 0), mask))
    return img


def write_previews() -> None:
    DOCS_MEDIA.mkdir(parents=True, exist_ok=True)
    # one big gallery sheet in the signature style
    cell, cols = 176, 8
    rows = (len(APPS) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (0, 0, 0, 0))
    for i, app in enumerate(APPS):
        sheet.alpha_composite(
            render_icon(app[0], cell, "#3BF5A0", True),
            ((i % cols) * cell, (i // cols) * cell),
        )
    sheet.save(DOCS_MEDIA / "gallery-phosphor.png", optimize=True)
    sheet.save(TOOLS / "gallery.png", optimize=True)

    # style swatches for the landing page
    for style in STYLES:
        sw = Image.new("RGBA", (360, 360), (0, 0, 0, 0))
        for i, name in enumerate(["gmail", "whatsapp", "spotify", "gcamera"]):
            sw.alpha_composite(
                render_icon(name, 180, style["accent"], style["glass"]),
                ((i % 2) * 180, (i // 2) * 180),
            )
        sw.save(DOCS_MEDIA / f"style-{style['slug']}.png", optimize=True)

    meta = {
        "count": len(APPS),
        "styles": [
            {"id": s["id"], "name": s["name"], "slug": s["slug"],
             "accent": s["accent"], "glass": s["glass"],
             "apk": f"matrix-{s['slug']}.apk"}
            for s in STYLES
        ],
        "apps": [{"name": a[0], "label": a[1], "category": a[3]} for a in APPS],
    }
    (ROOT / "docs" / "data.json").write_text(json.dumps(meta, indent=2))


def main() -> None:
    verify_catalog()
    JAVA_DIR.mkdir(parents=True, exist_ok=True)
    write_main_resources()
    write_flavor_resources()
    write_previews()
    print(f"generated {len(APPS)} icons x {len(STYLES)} styles")


if __name__ == "__main__":
    main()
