"""Dot-matrix drawing primitives for the Matrix icon pack.

Everything is authored on a GRID x GRID logical grid. A glyph is simply a
set of integer (x, y) dot coordinates. Shapes are rasterised by testing dot
centres, which keeps every icon perfectly aligned to the matrix and makes the
output deterministic (same source -> byte-identical resources).
"""

from __future__ import annotations

import math

GRID = 13
LAST = GRID - 1
MID = LAST / 2.0  # 6.0

Point = tuple[int, int]
Glyph = set[Point]

ALL_DOTS: list[Point] = [(x, y) for y in range(GRID) for x in range(GRID)]


def _clamp(v: float, lo: int = 0, hi: int = LAST) -> int:
    return max(lo, min(hi, int(round(v))))


def dots(*points: Point) -> Glyph:
    return {(_clamp(x), _clamp(y)) for x, y in points}


def rect(x0: float, y0: float, x1: float, y1: float) -> Glyph:
    x0, x1 = sorted((_clamp(x0), _clamp(x1)))
    y0, y1 = sorted((_clamp(y0), _clamp(y1)))
    return {(x, y) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1)}


def outline(x0: float, y0: float, x1: float, y1: float, t: int = 1) -> Glyph:
    x0, x1 = sorted((_clamp(x0), _clamp(x1)))
    y0, y1 = sorted((_clamp(y0), _clamp(y1)))
    out = rect(x0, y0, x1, y1)
    if x1 - x0 > 2 * t and y1 - y0 > 2 * t:
        out -= rect(x0 + t, y0 + t, x1 - t, y1 - t)
    return out


def disc(cx: float, cy: float, r: float) -> Glyph:
    r2 = r * r + 1e-9
    return {(x, y) for x, y in ALL_DOTS if (x - cx) ** 2 + (y - cy) ** 2 <= r2}


def ring(cx: float, cy: float, r: float, thickness: float = 0.85) -> Glyph:
    lo = (r - thickness) ** 2
    hi = (r + thickness) ** 2
    return {
        (x, y)
        for x, y in ALL_DOTS
        if lo <= (x - cx) ** 2 + (y - cy) ** 2 <= hi
    }


def line(x0: float, y0: float, x1: float, y1: float) -> Glyph:
    """Bresenham line snapped to dot centres."""
    x0i, y0i, x1i, y1i = _clamp(x0), _clamp(y0), _clamp(x1), _clamp(y1)
    out: Glyph = set()
    dx = abs(x1i - x0i)
    dy = -abs(y1i - y0i)
    sx = 1 if x0i < x1i else -1
    sy = 1 if y0i < y1i else -1
    err = dx + dy
    x, y = x0i, y0i
    while True:
        out.add((x, y))
        if x == x1i and y == y1i:
            return out
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x += sx
        if e2 <= dx:
            err += dx
            y += sy


def polyline(*points: Point, close: bool = False) -> Glyph:
    out: Glyph = set()
    pts = list(points)
    if close:
        pts = pts + [pts[0]]
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        out |= line(x0, y0, x1, y1)
    return out


def thick_line(x0: float, y0: float, x1: float, y1: float, r: float) -> Glyph:
    """A brush stroke: discs swept along a segment."""
    steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 3) + 1
    out: Glyph = set()
    for i in range(steps + 1):
        t = i / steps
        out |= disc(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, r)
    return out


def poly(points: list[tuple[float, float]], test: str = "center") -> Glyph:
    """Even-odd polygon fill sampled at dot centres."""
    out: Glyph = set()
    n = len(points)
    for x, y in ALL_DOTS:
        px, py = x, y
        if test == "corner":
            px, py = x + 0.35, y + 0.35
        inside = False
        j = n - 1
        for i in range(n):
            xi, yi = points[i]
            xj, yj = points[j]
            if (yi > py) != (yj > py):
                xint = xi + (py - yi) * (xj - xi) / (yj - yi)
                if px < xint:
                    inside = not inside
            j = i
        if inside:
            out.add((x, y))
    return out


def tri(p1: tuple[float, float], p2: tuple[float, float], p3: tuple[float, float]) -> Glyph:
    return poly([p1, p2, p3])


def arc(cx: float, cy: float, r: float, a0: float, a1: float, thickness: float = 0.85) -> Glyph:
    out: Glyph = set()
    band = ring(cx, cy, r, thickness)
    for x, y in band:
        ang = math.degrees(math.atan2(y - cy, x - cx)) % 360.0
        a, b = a0 % 360.0, a1 % 360.0
        inside = (a <= ang <= b) if a <= b else (ang >= a or ang <= b)
        if inside:
            out.add((x, y))
    return out


def mirror_x(g: Glyph) -> Glyph:
    return {(LAST - x, y) for x, y in g}


def sym(g: Glyph) -> Glyph:
    """Mirror across the vertical axis, keeping the original."""
    return g | mirror_x(g)


def flip_y(g: Glyph) -> Glyph:
    return {(x, LAST - y) for x, y in g}


def shift(g: Glyph, dx: int, dy: int) -> Glyph:
    return {(x + dx, y + dy) for x, y in g if 0 <= x + dx <= LAST and 0 <= y + dy <= LAST}


def scale(g: Glyph, factor: float) -> Glyph:
    """Nearest-neighbour scale about the grid centre (integer snapping)."""
    out: Glyph = set()
    for x, y in g:
        nx = MID + (x - MID) * factor
        ny = MID + (y - MID) * factor
        out.add((_clamp(nx), _clamp(ny)))
    return out


def bbox(g: Glyph) -> tuple[int, int, int, int]:
    xs = [p[0] for p in g]
    ys = [p[1] for p in g]
    return min(xs), min(ys), max(xs), max(ys)


def fit(g: Glyph, max_span: int = 11) -> Glyph:
    """Centre a glyph on the matrix, shrinking if it overflows the safe area."""
    if not g:
        return g
    out = g
    x0, y0, x1, y1 = bbox(out)
    w, h = x1 - x0, y1 - y0
    if max(w, h) > max_span:
        out = scale(out, max_span / max(w, h))
        x0, y0, x1, y1 = bbox(out)
        w, h = x1 - x0, y1 - y0
    dx = int(round(MID - (x0 + x1) / 2.0))
    dy = int(round(MID - (y0 + y1) / 2.0))
    return shift(out, dx, dy)


def to_ascii(g: Glyph) -> str:
    return "\n".join(
        "".join("#" if (x, y) in g else "." for x in range(GRID)) for y in range(GRID)
    )
