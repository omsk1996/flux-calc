"""Generate Google Play store graphics for Flux Calc.

Usage: python make_assets.py <project_dir>
Writes into <project_dir>/play-store/.
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

proj = sys.argv[1]
src = os.path.join(proj, "screenshots")
out = os.path.join(proj, "play-store")
os.makedirs(out, exist_ok=True)

CYAN = (0x22, 0xD3, 0xEE)
VIOLET = (0x7C, 0x5C, 0xFF)
PINK = (0xFF, 0x4F, 0xA3)
FONTS = r"C:\Windows\Fonts"


def font(name, size):
    for n in name:
        p = os.path.join(FONTS, n)
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default(size)


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def brand(t):
    t = max(0.0, min(1.0, t))
    return lerp(CYAN, VIOLET, t * 2) if t < 0.5 else lerp(VIOLET, PINK, (t - 0.5) * 2)


def diag_gradient(w, h, fn):
    """Diagonal gradient (top-left -> bottom-right) built from a small strip, then resized."""
    small = Image.new("RGB", (64, 64))
    px = small.load()
    for y in range(64):
        for x in range(64):
            px[x, y] = fn((x + y) / 126)
    return small.resize((w, h), Image.BICUBIC)


def glow(img, cx, cy, r, color, alpha):
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=color + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(r * 0.6))
    return Image.alpha_composite(img.convert("RGBA"), layer)


def dark_bg(w, h):
    bg = diag_gradient(w, h, lambda t: lerp((0x10, 0x14, 0x2E), (0x1E, 0x10, 0x2C), t)).convert("RGBA")
    bg = glow(bg, int(w * 0.15), int(h * 0.1), int(max(w, h) * 0.35), (0x0E, 0x6A, 0x80), 120)
    bg = glow(bg, int(w * 0.9), int(h * 0.85), int(max(w, h) * 0.35), (0x8A, 0x2B, 0x8F), 110)
    return bg


def draw_glyph(d, ox, oy, s, width):
    """The launcher glyph: '+' above '=' in 108-unit viewport coords."""
    segs = [((54, 30), (54, 54)), ((42, 42), (66, 42)), ((42, 66), (66, 66)), ((42, 77), (66, 77))]
    r = width * s / 2
    for (x1, y1), (x2, y2) in segs:
        a = (ox + x1 * s, oy + y1 * s)
        b = (ox + x2 * s, oy + y2 * s)
        d.line([a, b], fill="white", width=round(width * s))
        for cx, cy in (a, b):
            d.ellipse((cx - r, cy - r, cx + r, cy + r), fill="white")


def make_icon(size):
    """Square icon showing the adaptive icon's visible 72-unit centre, supersampled 4x."""
    ss = size * 4
    s = ss / 72
    img = diag_gradient(ss, ss, lambda t: brand((t * 72 + 36) / 108))
    draw_glyph(ImageDraw.Draw(img), -18 * s, -18 * s, s, 6)
    return img.resize((size, size), Image.LANCZOS)


def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, img.width - 1, img.height - 1), radius, fill=255)
    img = img.convert("RGBA")
    img.putalpha(mask)
    return img


def shadow(canvas, box, radius, blur=40, alpha=150):
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    x0, y0, x1, y1 = box
    ImageDraw.Draw(layer).rounded_rectangle((x0, y0 + 18, x1, y1 + 18), radius, fill=(0, 0, 0, alpha))
    return Image.alpha_composite(canvas, layer.filter(ImageFilter.GaussianBlur(blur)))


def centered_text(d, cx, y, text, f, fill):
    w = d.textlength(text, font=f)
    d.text((cx - w / 2, y), text, font=f, fill=fill)


# 1. App icon 512x512 now comes from logo.py (export); reuse it here for the feature graphic.
make_icon = lambda size: Image.open(os.path.join(out, "icon-512.png")).convert("RGB").resize((size, size), Image.LANCZOS)

# 2. Feature graphic 1024x500.
W, H = 1024, 500
fg = dark_bg(W, H)
icon = rounded(make_icon(220), 52)
fg = shadow(fg, (90, 140, 310, 360), 52, blur=30, alpha=170)
fg.alpha_composite(icon, (90, 140))
d = ImageDraw.Draw(fg)
title = font(["seguisb.ttf", "segoeuib.ttf", "arialbd.ttf"], 96)
sub = font(["segoeuil.ttf", "segoeui.ttf", "arial.ttf"], 40)
d.text((370, 158), "Flux Calc", font=title, fill="white")
d.text((374, 286), "Scientific calculator,", font=sub, fill=(0xC9, 0xD6, 0xF2))
d.text((374, 334), "fluid by design.", font=sub, fill=(0xC9, 0xD6, 0xF2))
bar = diag_gradient(260, 8, brand)
fg.alpha_composite(rounded(bar, 4), (374, 270))
fg.convert("RGB").save(os.path.join(out, "feature-graphic-1024x500.png"))

# 3. Framed phone screenshots (Play rejects anything longer than 2:1; the raw shots are 20:9).
shots = [
    ("result.png", "Scientific power,", "beautifully simple"),
    ("second-functions.png", "Every function", "one tap away"),
    ("history.png", "Your history,", "always at hand"),
]
cap_bold = font(["seguisb.ttf", "segoeuib.ttf", "arialbd.ttf"], 72)
cap_light = font(["segoeuil.ttf", "segoeui.ttf", "arial.ttf"], 72)
for i, (name, l1, l2) in enumerate(shots, 1):
    W, H = 1080, 1920
    c = dark_bg(W, H)
    shot = Image.open(os.path.join(src, name)).convert("RGB")
    sh = 1500
    sw = round(shot.width * sh / shot.height)
    shot = rounded(shot.resize((sw, sh), Image.LANCZOS), 48)
    x, y = (W - sw) // 2, H - sh - 90
    c = shadow(c, (x, y, x + sw, y + sh), 48)
    c.alpha_composite(shot, (x, y))
    d = ImageDraw.Draw(c)
    centered_text(d, W / 2, 80, l1, cap_bold, "white")
    centered_text(d, W / 2, 170, l2, cap_light, (0xC9, 0xD6, 0xF2))
    c.convert("RGB").save(os.path.join(out, f"phone-{i}-{os.path.splitext(name)[0]}.png"))

# Landscape shot -> 1920x1080.
W, H = 1920, 1080
c = dark_bg(W, H)
shot = Image.open(os.path.join(src, "landscape.png")).convert("RGB")
sw = 1640
sh = round(shot.height * sw / shot.width)
shot = rounded(shot.resize((sw, sh), Image.LANCZOS), 40)
x, y = (W - sw) // 2, H - sh - 60
c = shadow(c, (x, y, x + sw, y + sh), 40)
c.alpha_composite(shot, (x, y))
d = ImageDraw.Draw(c)
centered_text(d, W / 2, 70, "Turn sideways for the full scientific keypad", cap_bold, "white")
c.convert("RGB").save(os.path.join(out, "phone-4-landscape.png"))

# Contact sheet for a quick visual check (not for upload).
files = sorted(f for f in os.listdir(out) if f.endswith(".png") and not f.startswith("_"))
thumbs = []
for f in files:
    im = Image.open(os.path.join(out, f)).convert("RGB")
    im.thumbnail((360, 360))
    thumbs.append(im)
sheet = Image.new("RGB", (sum(t.width for t in thumbs) + 20 * (len(thumbs) + 1), 400), (40, 40, 40))
xx = 20
for t in thumbs:
    sheet.paste(t, (xx, 20))
    xx += t.width + 20
sheet.save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "contact-sheet.png"))
for f in files:
    im = Image.open(os.path.join(out, f))
    print(f, im.size, im.mode)
