"""
Generates the 1024x500 Play Store feature graphic.

The mark geometry is imported from generate_icons rather than redrawn, so the banner and the
launcher icon are the same mark by construction and cannot drift when one is edited.

Type is the real bundled Instrument Serif and the real Roboto that FontFamily.SansSerif resolves
to on device (pulled from /system/fonts), not lookalike substitutes. Colours are the literal
values from ui/theme/Color.kt.

Rendered at 3x and downsampled, which is what keeps the pieslice seam and the serif brackets
clean at final size.
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generate_icons import INK, PAPER, URGENT, MARK_FRACTION  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")

# ui/theme/Color.kt — PaperInkMuted. The tagline grey is a warm neutral, not a system grey.
INK_MUTED = (0x6E, 0x65, 0x59, 255)

SERIF = os.path.join(ROOT, "app", "src", "main", "res", "font", "instrument_serif_regular.ttf")
# LapseSans = FontFamily.SansSerif, which is Roboto on Android.
SANS = os.path.join(HERE, "fonts", "Roboto-Regular.ttf")

W, H = 1024, 500
SS = 3               # supersample factor
MARGIN = 80          # hard keep-out from every edge
BOTTOM_THIRD = H * 2 // 3   # nothing important below this line
# Geometry placed exactly on a keep-out line still bleeds a pixel past it once the 3x render is
# downsampled, which measures as a violation. Inset every edge by the width of that bleed.
SAFE = 2

# The reference stack occupies ~68% of its height. Here the top margin and the bottom-third
# keep-out leave only 253px of 500, so the wordmark is sized to the height that remains rather
# than to a target width — otherwise it overflows the safe band (the guard in render() catches
# this). The tagline is what sets the composition's width instead, reaching ~58% of the canvas.
#
# Instrument Serif is condensed — "Lapse" is 2.08x as wide as it is tall — so a wordmark as wide
# as the reference's would need 221px of the 253px band and leave nothing for the mark or the
# tagline. The wordmark therefore wins on height rather than width (2.5x the tagline's), and the
# tagline is what carries the composition out to ~58% of the canvas.
TAGLINE_W = 0.50
MARK_D = 0.0625
GAP_MARK = 18   # mark to wordmark
GAP_TAG = 14    # wordmark to tagline

WORDMARK = "Lapse"
TAGLINE = "Know before it expires."


PROBE = 200


def fit_width(path, text, target_px):
    """Point size at which `text` renders `target_px` wide, measured on real ink extents."""
    font = ImageFont.truetype(path, PROBE)
    b = font.getbbox(text)
    return ImageFont.truetype(path, max(1, round(PROBE * target_px / (b[2] - b[0]))))


def fit_height(path, text, target_px):
    """Point size at which `text`'s ink box is `target_px` tall, ascender to descender."""
    font = ImageFont.truetype(path, PROBE)
    b = font.getbbox(text)
    return ImageFont.truetype(path, max(1, round(PROBE * target_px / (b[3] - b[1]))))


def draw_mark_on_paper(draw, cx, cy, d):
    """
    The launcher mark, colour-adapted to a paper field.

    The icon draws a paper disc on an ink tile with the 12->3 quarter in urgent. On a paper
    background that disc would vanish, so the disc takes the ink role and the quarter keeps
    its own colour. Same geometry, same quarter, same direction — only the ground is inverted,
    exactly as the app's own dark mode re-tones rather than inverts.
    """
    box = [cx - d / 2, cy - d / 2, cx + d / 2, cy + d / 2]
    # Pillow angles: 0 deg is 3 o'clock, increasing clockwise. Identical spans to draw_mark().
    draw.pieslice(box, start=-90, end=0, fill=URGENT)   # 12 -> 3, the quarter running out
    draw.pieslice(box, start=0, end=270, fill=INK)      # the rest


def draw_mark_as_badge(draw, cx, cy, d):
    """The launcher icon verbatim: ink disc, paper face, urgent quarter. Reads as a ring."""
    box = [cx - d / 2, cy - d / 2, cx + d / 2, cy + d / 2]
    draw.ellipse(box, fill=INK)
    inner = d * MARK_FRACTION
    ibox = [cx - inner / 2, cy - inner / 2, cx + inner / 2, cy + inner / 2]
    draw.pieslice(ibox, start=-90, end=0, fill=URGENT)
    draw.pieslice(ibox, start=0, end=270, fill=PAPER)


def render(mark_fn, verbose=False):
    s = SS
    img = Image.new("RGB", (W * s, H * s), PAPER[:3])
    draw = ImageDraw.Draw(img)

    left = (MARGIN + SAFE) * s
    d = round(W * MARK_D) * s
    gap_mark, gap_tag = round(GAP_MARK * s), round(GAP_TAG * s)

    band_top, band_bottom = (MARGIN + SAFE) * s, (BOTTOM_THIRD - SAFE) * s
    band = band_bottom - band_top

    # The tagline sets the composition's width; the wordmark takes whatever height is left.
    sans = fit_width(SANS, TAGLINE, round(W * TAGLINE_W) * s)
    tb = sans.getbbox(TAGLINE)
    tag_h = tb[3] - tb[1]

    word_h = band - d - gap_mark - gap_tag - tag_h
    if word_h <= 0:
        raise SystemExit(f"no room for the wordmark in the {band/s:.0f}px safe band")
    serif = fit_height(SERIF, WORDMARK, word_h)
    wb = serif.getbbox(WORDMARK)
    word_h = wb[3] - wb[1]   # re-measure: rounding to an integer point size moves it slightly

    total = d + gap_mark + word_h + gap_tag + tag_h
    y = band_top + (band - total) / 2

    mark_fn(draw, left + d / 2, y + d / 2, d)
    y += d + gap_mark
    draw.text((left - wb[0], y - wb[1]), WORDMARK, font=serif, fill=INK)
    y += word_h + gap_tag
    draw.text((left - tb[0], y - tb[1]), TAGLINE, font=sans, fill=INK_MUTED)
    bottom = y + tag_h

    if verbose:
        top = (band_top + (band - total) / 2) / s
        right = max(left + d, left + (wb[2] - wb[0]), left + (tb[2] - tb[0])) / s
        print(f"  serif {serif.size//s}pt, sans {sans.size//s}pt, mark {d//s}px")
        print(f"  block top {top:.0f}px, bottom {bottom/s:.0f}px (keep-out at {BOTTOM_THIRD})")
        print(f"  wordmark {(wb[2]-wb[0])/s:.0f}px wide, tagline {(tb[2]-tb[0])/s:.0f}px")
        print(f"  content right edge {right:.0f}px = {right/W*100:.0f}% of width")

    return img.resize((W, H), Image.LANCZOS)


VARIANTS = {"solid": draw_mark_on_paper, "badge": draw_mark_as_badge}

if __name__ == "__main__":
    which = sys.argv[1] if len(sys.argv) > 1 else "solid"
    out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, "feature-graphic.png")
    print(f"{which}:")
    # RGB, never RGBA: Play rejects a feature graphic with an alpha channel.
    render(VARIANTS[which], verbose=True).save(out)
    print(f"  -> {out}")
