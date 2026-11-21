"""Flux Calc logo concepts, drawn as adaptive-icon layers (108-unit viewport, 72-unit visible, 66 safe).

python logo.py preview <out.png>          -> contact sheet of all concepts
python logo.py export <concept> <projdir> -> mipmap PNG layers + play-store/icon-512.png
"""
import math
import os
import sys

from PIL import Image, ImageChops, ImageDraw, ImageFilter

CYAN = (0x22, 0xD3, 0xEE)
VIOLET = (0x7C, 0x5C, 0xFF)
PINK = (0xFF, 0x4F, 0xA3)


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def brand(t):
    t = max(0.0, min(1.0, t))
    return lerp(CYAN, VIOLET, t * 2) if t < 0.5 else lerp(VIOLET, PINK, (t - 0.5) * 2)


class Canvas:
    """Draw in 108-unit coordinates onto an S x S RGBA image."""

    def __init__(self, S):
        self.S = S
        self.k = S / 108
        self.img = Image.new("RGBA", (S, S), (0, 0, 0, 0))

    def u(self, v):
        return v * self.k

    def pts(self, ps):
        return [(self.u(x), self.u(y)) for x, y in ps]

    def mask(self, fn):
        m = Image.new("L", (self.S, self.S), 0)
        fn(ImageDraw.Draw(m), self)
        return m

    def fill(self, mask, color_img, alpha=1.0):
        if alpha < 1:
            mask = mask.point(lambda p: round(p * alpha))
        layer = color_img.copy() if isinstance(color_img, Image.Image) else Image.new("RGBA", (self.S, self.S), color_img)
        layer = layer.convert("RGBA")
        a = ImageChops.multiply(layer.getchannel("A"), mask)
        layer.putalpha(a)
        self.img = Image.alpha_composite(self.img, layer)

    def glow(self, mask, color, radius_u, alpha):
        g = mask.filter(ImageFilter.GaussianBlur(self.u(radius_u)))
        self.fill(g, color + (255,), alpha)

    def gradient(self, fn, angle=45):
        """fn(t)->RGB along direction `angle` (deg, 0 = left->right, 90 = top->bottom)."""
        n = 128
        small = Image.new("RGBA", (n, n))
        px = small.load()
        ca, sa = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        lo = min(0, ca) + min(0, sa)
        hi = max(0, ca) + max(0, sa)
        for y in range(n):
            for x in range(n):
                t = ((x / (n - 1)) * ca + (y / (n - 1)) * sa - lo) / (hi - lo)
                px[x, y] = fn(t) + (255,)
        return small.resize((self.S, self.S), Image.BICUBIC)


def stroke(d, c, ps, w):
    """Polyline with round caps/joins, width w (units)."""
    P = c.pts(ps)
    W = c.u(w)
    d.line(P, fill=255, width=round(W), joint="curve")
    for x, y in P:
        d.ellipse((x - W / 2, y - W / 2, x + W / 2, y + W / 2), fill=255)


def dark_background(S):
    c = Canvas(S)
    c.fill(Image.new("L", (S, S), 255), c.gradient(lambda t: lerp((0x12, 0x16, 0x36), (0x24, 0x0F, 0x3A), t), 60))
    blob = lambda cx, cy, r: c.mask(lambda d, c: d.ellipse(c.pts([(cx - r, cy - r), (cx + r, cy + r)]), fill=255))
    c.glow(blob(24, 22, 26), (0x0E, 0x9A, 0xB8), 18, 0.55)
    c.glow(blob(88, 90, 30), (0xB0, 0x2A, 0x9A), 18, 0.55)
    return c.img


def glyph_ops(d, c, cx, cy, s, w, op):
    if op == "+":
        stroke(d, c, [(cx - s, cy), (cx + s, cy)], w)
        stroke(d, c, [(cx, cy - s), (cx, cy + s)], w)
    elif op == "-":
        stroke(d, c, [(cx - s, cy), (cx + s, cy)], w)
    elif op == "x":
        r = s * 0.78
        stroke(d, c, [(cx - r, cy - r), (cx + r, cy + r)], w)
        stroke(d, c, [(cx - r, cy + r), (cx + r, cy - r)], w)
    elif op == "=":
        stroke(d, c, [(cx - s, cy - s * 0.45), (cx + s, cy - s * 0.45)], w)
        stroke(d, c, [(cx - s, cy + s * 0.45), (cx + s, cy + s * 0.45)], w)


# ---------------------------------------------------------------- concepts

def concept_wave(S):
    """A glowing gradient sine wave: trigonometry + flux."""
    bg = dark_background(S)
    c = Canvas(S)
    wave = [(x, 56 - 12 * math.sin(2 * math.pi * (x - 29) / 50)) for x in [29 + i * 0.5 for i in range(101)]]
    m = c.mask(lambda d, c: stroke(d, c, wave, 9))
    c.glow(m, (0x9A, 0x6B, 0xFF), 5, 0.9)
    c.fill(m, c.gradient(brand, 0))
    # glossy highlight along the top of the stroke
    hl = c.mask(lambda d, c: stroke(d, c, [(x, y - 2.2) for x, y in wave], 2.2))
    c.fill(ImageChops.multiply(hl, m), (255, 255, 255, 255), 0.35)
    # a plotted point sitting on the crest of the curve
    px, py, r = 41.5, 44, 5.4
    ring = c.mask(lambda d, c: d.ellipse(c.pts([(px - r, py - r), (px + r, py + r)]), fill=255))
    c.glow(ring, (255, 255, 255), 4, 0.8)
    c.fill(ring, (255, 255, 255, 255))
    core = c.mask(lambda d, c: d.ellipse(c.pts([(px - 2.3, py - 2.3), (px + 2.3, py + 2.3)]), fill=255))
    c.fill(core, (0x5E, 0x8C, 0xFF, 255))
    return bg, c.img


def concept_keys(S):
    """2x2 glass keys mirroring the app's keypad, with the gradient '=' key."""
    bg = dark_background(S)
    c = Canvas(S)
    size, gap = 21, 4.5
    x0 = 54 - size - gap / 2
    cells = [(0, 0, "+"), (1, 0, "-"), (0, 1, "x"), (1, 1, "=")]
    for col, row, op in cells:
        x, y = x0 + col * (size + gap), x0 + row * (size + gap)
        box = c.pts([(x, y), (x + size, y + size)])
        tile = c.mask(lambda d, c: d.rounded_rectangle(box, c.u(7), fill=255))
        if op == "=":
            c.glow(tile, (0xE0, 0x4F, 0xC8), 6, 0.8)
            c.fill(tile, c.gradient(lambda t: brand(0.45 + t * 0.55), 45))
        else:
            c.fill(tile, (255, 255, 255, 255), 0.13)
            inner = c.mask(lambda d, c: d.rounded_rectangle(box, c.u(7), outline=255, width=max(1, round(c.u(0.8)))))
            c.fill(inner, (255, 255, 255, 255), 0.30)
        g = c.mask(lambda d, c: glyph_ops(d, c, x + size / 2, y + size / 2, 5.2, 3.2, op))
        c.fill(g, (255, 255, 255, 255) if op == "=" else (0xC8, 0xF4, 0xFF, 255))
    return bg, c.img


def concept_orb(S):
    """A glass orb half-filled with flowing gradient liquid, white '=' floating on top."""
    bg = dark_background(S)
    c = Canvas(S)
    R, cx, cy = 27, 54, 54
    orb = c.mask(lambda d, c: d.ellipse(c.pts([(cx - R, cy - R), (cx + R, cy + R)]), fill=255))
    c.glow(orb, (0x7C, 0x5C, 0xFF), 7, 0.55)
    c.fill(orb, (255, 255, 255, 255), 0.10)
    surface = [(x, 58 + 3.2 * math.sin(2 * math.pi * (x - 20) / 30)) for x in [20 + i * 0.5 for i in range(141)]]
    liquid = c.mask(lambda d, c: d.polygon(c.pts(surface + [(90, 92), (18, 92)]), fill=255))
    liquid = ImageChops.multiply(liquid, orb)
    c.fill(liquid, c.gradient(brand, 60))
    crest = c.mask(lambda d, c: stroke(d, c, surface, 1.4))
    c.fill(ImageChops.multiply(crest, orb), (255, 255, 255, 255), 0.55)
    for bx, by, br in [(44, 72, 1.6), (61, 77, 1.1), (66, 67, 2.0), (50, 81, 0.9)]:
        b = c.mask(lambda d, c: d.ellipse(c.pts([(bx - br, by - br), (bx + br, by + br)]), fill=255))
        c.fill(b, (255, 255, 255, 255), 0.55)
    ring = c.mask(lambda d, c: d.ellipse(c.pts([(cx - R, cy - R), (cx + R, cy + R)]), outline=255, width=max(1, round(c.u(1.2)))))
    c.fill(ring, (255, 255, 255, 255), 0.35)
    gloss = c.mask(lambda d, c: d.arc(c.pts([(cx - R + 5, cy - R + 5), (cx + R - 5, cy + R - 5)]), 200, 255, fill=255, width=round(c.u(2.6))))
    c.fill(gloss, (255, 255, 255, 255), 0.6)
    eq = c.mask(lambda d, c: glyph_ops(d, c, 54, 44, 9, 4.6, "="))
    c.glow(eq, (255, 255, 255), 2.5, 0.5)
    c.fill(eq, (255, 255, 255, 255))
    return bg, c.img


CONCEPTS = {"wave": concept_wave, "keys": concept_keys, "orb": concept_orb}


def flatten(bg, fg):
    return Image.alpha_composite(bg, fg)


def visible(img):
    """Crop the 72-unit visible area (what launchers and Play show)."""
    S = img.width
    a, b = round(S * 18 / 108), round(S * 90 / 108)
    return img.crop((a, a, b, b))


def masked(img, shape):
    m = Image.new("L", img.size, 0)
    d = ImageDraw.Draw(m)
    if shape == "circle":
        d.ellipse((0, 0, img.width - 1, img.height - 1), fill=255)
    else:
        d.rounded_rectangle((0, 0, img.width - 1, img.height - 1), round(img.width * 0.22), fill=255)
    out = img.copy()
    out.putalpha(m)
    return out


if __name__ == "__main__":
    mode = sys.argv[1]
    if mode == "preview":
        S, T = 1296, 300
        sheet = Image.new("RGB", (40 + len(CONCEPTS) * (T * 2 + 60), T + 140), (238, 238, 242))
        x = 40
        for name, fn in CONCEPTS.items():
            icon = visible(flatten(*fn(S))).resize((T, T), Image.LANCZOS)
            sheet.paste(masked(icon, "circle"), (x, 40), masked(icon, "circle"))
            small = icon.resize((96, 96), Image.LANCZOS)
            sm = masked(small, "squircle")
            sheet.paste(sm, (x + T + 30, 40), sm)
            ImageDraw.Draw(sheet).text((x, T + 60), name, fill=(30, 30, 40), font_size=36)
            x += T * 2 + 60
        sheet.save(sys.argv[2])
    elif mode == "export":
        name, proj = sys.argv[2], sys.argv[3]
        fn = CONCEPTS[name]
        res = os.path.join(proj, "app", "src", "main", "res")
        bg, fg = fn(432 * 4)
        for dpi, px in {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}.items():
            d = os.path.join(res, f"mipmap-{dpi}")
            os.makedirs(d, exist_ok=True)
            bg.convert("RGB").resize((px, px), Image.LANCZOS).save(os.path.join(d, "ic_launcher_background.png"), optimize=True)
            fg.resize((px, px), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.png"), optimize=True)
        bg, fg = fn(768 * 4)
        play = visible(flatten(bg, fg)).convert("RGB").resize((512, 512), Image.LANCZOS)
        os.makedirs(os.path.join(proj, "play-store"), exist_ok=True)
        play.save(os.path.join(proj, "play-store", "icon-512.png"))
        if name == "wave":
            # Single-colour vector for Android 13+ themed icons (the system tints it).
            xs = [29 + i * 2 for i in range(26)]
            path = "M" + " L".join(f"{x:g},{56 - 12 * math.sin(2 * math.pi * (x - 29) / 50):.2f}" for x in xs)
            px, py, r = 41.5, 44, 5.4
            dot = f"M{px - r:g},{py:g} a{r:g},{r:g} 0 1,0 {2 * r:g},0 a{r:g},{r:g} 0 1,0 {-2 * r:g},0 Z"
            xml = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Themed (monochrome) launcher icon: the Flux Calc wave with its plotted point. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:pathData="{path}"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="9"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="{dot}"
        android:fillColor="#FFFFFFFF" />
</vector>
"""
            with open(os.path.join(res, "drawable", "ic_launcher_monochrome.xml"), "w", encoding="utf-8") as f:
                f.write(xml)
        print("exported", name)
