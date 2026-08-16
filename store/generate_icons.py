"""
Generates the legacy PNG mipmaps and the Play listing icon.

minSdk is 24, so API 24-25 devices ignore the adaptive XML and need real rasters. These are
drawn from the same geometry as ic_launcher_foreground.xml so the two never drift: a disc
whose final quarter is the urgent accent, on the app's warm ink.

Rendered at 4x and downsampled, which is what keeps the sector seam and the circle edge clean
at 48dp.
"""
from PIL import Image, ImageDraw

INK = (0x1E, 0x1A, 0x15, 255)      # PaperInk
PAPER = (0xFA, 0xF7, 0xF2, 255)    # PaperBackground
URGENT = (0xB8, 0x55, 0x29, 255)   # LightStatusPalette.urgent

SS = 4  # supersample factor

# Mark diameter as a fraction of the canvas.
#
# The adaptive foreground draws a 60dp mark inside a 108dp viewport, but the launcher mask only
# reveals the middle ~72dp — so the mark fills roughly 83% of what you actually see. Legacy
# tiles have no mask, so matching that ratio literally would run the disc into the corners.
# 0.72 keeps the two looking like the same icon while leaving the tile some air. At 0.60 the
# mark read as a dot lost in padding at 48dp.
MARK_FRACTION = 0.72


def draw_mark(draw, size):
    d = size * MARK_FRACTION
    box = [(size - d) / 2, (size - d) / 2, (size + d) / 2, (size + d) / 2]
    # Pillow angles: 0 deg is 3 o'clock, increasing clockwise.
    draw.pieslice(box, start=-90, end=0, fill=URGENT)   # 12 -> 3, the quarter running out
    draw.pieslice(box, start=0, end=270, fill=PAPER)    # the rest


def square_icon(size, corner_fraction=0.22):
    """Legacy square icon with rounded corners, matching what launchers expect pre-API 26."""
    s = size * SS
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([0, 0, s - 1, s - 1], radius=int(s * corner_fraction), fill=INK)
    draw_mark(draw, s)
    return img.resize((size, size), Image.LANCZOS)


def round_icon(size):
    s = size * SS
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.ellipse([0, 0, s - 1, s - 1], fill=INK)
    draw_mark(draw, s)
    return img.resize((size, size), Image.LANCZOS)


def play_icon(size=512):
    """Play requires a full-bleed 512x512; the store applies its own shaping."""
    s = size * SS
    img = Image.new("RGBA", (s, s), INK)
    draw_mark(ImageDraw.Draw(img), s)
    return img.resize((size, size), Image.LANCZOS)


DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

if __name__ == "__main__":
    import os
    res = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
    for name, px in DENSITIES.items():
        out = os.path.join(res, f"mipmap-{name}")
        os.makedirs(out, exist_ok=True)
        square_icon(px).save(os.path.join(out, "ic_launcher.png"))
        round_icon(px).save(os.path.join(out, "ic_launcher_round.png"))
        print(f"  mipmap-{name}: {px}x{px}")
    store = os.path.dirname(__file__)
    play_icon().save(os.path.join(store, "play-icon-512.png"))
    print("  store/play-icon-512.png: 512x512")
