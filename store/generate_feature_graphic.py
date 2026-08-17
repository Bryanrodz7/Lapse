"""
Generates the 1024x500 Play Store feature graphic.

The mark geometry is imported from generate_icons rather than redrawn, so the banner and the
launcher icon are the same mark by construction and cannot drift when one is edited.

Type is the real bundled Instrument Serif and the real Roboto that FontFamily.SansSerif resolves
to on device (see fonts/README.md), not lookalike substitutes. Colours are the literal values
from ui/theme/Color.kt.

Rendered at 3x and downsampled, which is what keeps the pieslice seam and the serif brackets
clean at final size.

Two presets, because the constraints genuinely conflict — see PRESETS:

    python generate_feature_graphic.py both
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generate_icons import INK, PAPER, URGENT  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")

# ui/theme/Color.kt — PaperInkMuted. The tagline grey is a warm neutral, not a system grey.
INK_MUTED = (0x6E, 0x65, 0x59, 255)

SERIF = os.path.join(ROOT, "app", "src", "main", "res",
                     "font", "instrument_serif_regular.ttf")
# LapseSans = FontFamily.SansSerif, which is Roboto on Android.
SANS = os.path.join(HERE, "fonts", "Roboto-Regular.ttf")

W, H = 1024, 500
SS = 3               # supersample factor
MARGIN = 80          # hard keep-out from every edge
BOTTOM_THIRD = H * 2 // 3
# Geometry placed exactly on a keep-out line still bleeds a pixel past it once the 3x render is
# downsampled, which measures as a violation. Inset every edge by the width of that bleed.
SAFE = 2

WORDMARK = "Lapse"
TAGLINE = "Know before it expires."

# Instrument Serif is condensed — "Lapse" is 2.08x as wide as it is tall — so the wordmark's
# width is governed entirely by how much *height* the layout can spare it. That is the whole
# difference between these two presets.
#
#   poster — the Play asset. Uses the full height between the margins: 336px of band, wordmark
#            ~341px wide, at the cost of putting the tagline inside the bottom third.
#   safe    — the alternate. Nothing below the bottom-third line, but only 253px of band, which
#            holds the wordmark to ~216px — too small to land at real display size.
#
# The poster mark is 88px against safe's 64px. It is not just scaled with the canvas: a mark that
# stays small while the wordmark grows reads as a tittle over the "L" rather than as a logo. The
# 24px it takes back costs the wordmark ~56px of width, and buys one thing beyond legibility —
# it pulls the wordmark's right edge clear of a centred video play button.
PRESETS = {
    "poster": dict(bottom=H - MARGIN,   mark_d=0.0859, gap_mark=21, gap_tag=14, tagline_w=0.50),
    "safe":   dict(bottom=BOTTOM_THIRD, mark_d=0.0625, gap_mark=18, gap_tag=14, tagline_w=0.50),
}

PROBE = 200


def fit_width(path, text, target_px):
    """Point size at which `text` renders `target_px` wide, measured on real ink extents."""
    b = ImageFont.truetype(path, PROBE).getbbox(text)
    return ImageFont.truetype(path, max(1, round(PROBE * target_px / (b[2] - b[0]))))


def fit_height(path, text, target_px):
    """Point size at which `text`'s ink box is `target_px` tall, ascender to descender."""
    b = ImageFont.truetype(path, PROBE).getbbox(text)
    return ImageFont.truetype(path, max(1, round(PROBE * target_px / (b[3] - b[1]))))


def draw_mark(draw, cx, cy, d):
    """
    The launcher mark, colour-adapted to a paper field.

    The icon draws a paper disc on an ink tile with the 12->3 quarter in urgent. On a paper
    background that disc would vanish, so the disc takes the ink role and the quarter keeps its
    own colour. Same geometry, same quarter, same direction — only the ground is inverted,
    exactly as the app's own dark mode re-tones rather than inverts.
    """
    box = [cx - d / 2, cy - d / 2, cx + d / 2, cy + d / 2]
    # Pillow angles: 0 deg is 3 o'clock, increasing clockwise. Identical spans to the icon's.
    # 12 -> 3, the quarter running out
    draw.pieslice(box, start=-90, end=0, fill=URGENT)
    draw.pieslice(box, start=0, end=270, fill=INK)      # the rest


def render(preset, verbose=False, boxes=None):
    """
    Draws one preset. `boxes`, if given, is filled with each element's final rectangle in
    output pixels — the layout maths is the only honest source for those, since the downsample
    rings dark pixels into the tagline and defeats measuring elements back out by colour.
    """
    p = PRESETS[preset]
    s = SS
    img = Image.new("RGB", (W * s, H * s), PAPER[:3])
    draw = ImageDraw.Draw(img)

    left = (MARGIN + SAFE) * s
    d = round(W * p["mark_d"]) * s
    gap_mark, gap_tag = round(p["gap_mark"] * s), round(p["gap_tag"] * s)

    band_top, band_bottom = (MARGIN + SAFE) * s, (p["bottom"] - SAFE) * s
    band = band_bottom - band_top

    # The tagline sets the composition's width; the wordmark takes whatever height is left.
    sans = fit_width(SANS, TAGLINE, round(W * p["tagline_w"]) * s)
    tb = sans.getbbox(TAGLINE)
    tag_h = tb[3] - tb[1]

    word_h = band - d - gap_mark - gap_tag - tag_h
    if word_h <= 0:
        raise SystemExit(
            f"no room for the wordmark in the {band/s:.0f}px band")
    serif = fit_height(SERIF, WORDMARK, word_h)
    wb = serif.getbbox(WORDMARK)
    # re-measure: rounding to an integer point size moves it slightly
    word_h = wb[3] - wb[1]

    total = d + gap_mark + word_h + gap_tag + tag_h
    y = band_top + (band - total) / 2
    top = y

    draw_mark(draw, left + d / 2, y + d / 2, d)
    mark_box = (left, y, left + d, y + d)
    y += d + gap_mark
    draw.text((left - wb[0], y - wb[1]), WORDMARK, font=serif, fill=INK)
    word_box = (left, y, left + (wb[2] - wb[0]), y + word_h)
    y += word_h + gap_tag
    draw.text((left - tb[0], y - tb[1]), TAGLINE, font=sans, fill=INK_MUTED)
    tag_box = (left, y, left + (tb[2] - tb[0]), y + tag_h)

    if boxes is not None:
        boxes.update({k: tuple(round(v / s) for v in box) for k, box in
                      (("mark", mark_box), ("wordmark", word_box), ("tagline", tag_box))})

    if verbose:
        right = max(left + d, left +
                    (wb[2] - wb[0]), left + (tb[2] - tb[0])) / s
        print(
            f"  serif {serif.size//s}pt, sans {sans.size//s}pt, mark {d//s}px")
        print(
            f"  block y {top/s:.0f}-{(y + tag_h)/s:.0f}   wordmark {(wb[2]-wb[0])/s:.0f}px wide")
        print(
            f"  content right edge {right:.0f}px = {right/W*100:.0f}% of width")

    return img.resize((W, H), Image.LANCZOS)


if __name__ == "__main__":
    which = sys.argv[1] if len(sys.argv) > 1 else "both"
    # feature-graphic.png is the file that gets uploaded, so it is the poster cut.
    targets = {
        "poster": "feature-graphic.png",
        "safe": "feature-graphic-safe.png",
    }
    for name in (targets if which == "both" else [which]):
        print(f"{name}:")
        # RGB, never RGBA: Play rejects a feature graphic with an alpha channel.
        out = os.path.join(HERE, targets[name])
        render(name, verbose=True).save(out)
        print(f"  -> {targets[name]}")
