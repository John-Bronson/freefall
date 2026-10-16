from PIL import Image, ImageDraw, ImageFont
import math, random

S = 36          # native sprite size (same as Subspace)
UP = 4          # integer upscale factor ("4x window")
T = S * UP      # 144 px tile

TRANS  = (0, 0, 0, 0)
OUT    = (26, 28, 40, 255)
HULL   = (150, 156, 172, 255)
HI     = (208, 212, 224, 255)
SH     = (90, 96, 114, 255)
COCK   = (64, 146, 230, 255)
COCKHI = (176, 218, 255, 255)
RED    = (204, 54, 62, 255)
ENG    = (255, 138, 36, 255)
ENGC   = (255, 234, 132, 255)
BG     = (10, 11, 18, 255)


def make_ship():
    img = Image.new("RGBA", (S, S), TRANS)
    d = ImageDraw.Draw(img)
    # fuselage (pointing up)
    d.polygon([(17, 1), (18, 1), (21, 13), (22, 27), (13, 27), (14, 13)], fill=HULL)
    # wings
    d.polygon([(14, 15), (2, 30), (3, 33), (14, 29)], fill=HULL)
    d.polygon([(21, 15), (33, 30), (32, 33), (21, 29)], fill=HULL)
    # engine pods
    d.rectangle([14, 27, 16, 32], fill=HULL)
    d.rectangle([19, 27, 21, 32], fill=HULL)

    px = img.load()
    hull = {(x, y) for x in range(S) for y in range(S) if px[x, y][3] > 0}
    nb = lambda x, y: [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
    outline = {p for p in hull if any(n not in hull for n in nb(*p))}
    for p in outline:
        px[p] = OUT
    for (x, y) in hull - outline:
        if (x - 1, y - 1) in outline or (x - 1, y) in outline or (x, y - 1) in outline:
            px[x, y] = HI
        elif (x + 1, y + 1) in outline or (x + 1, y) in outline or (x, y + 1) in outline:
            px[x, y] = SH
    # centre ridge highlight
    for y in range(18, 26):
        px[17, y] = HI
    # cockpit
    d.ellipse([15, 7, 20, 16], fill=COCK)
    for p in [(16, 9), (16, 10), (17, 8)]:
        px[p] = COCKHI
    # wing stripes
    d.line([(5, 30), (11, 23)], fill=RED)
    d.line([(30, 30), (24, 23)], fill=RED)
    # engine glow
    d.rectangle([14, 33, 16, 34], fill=ENG)
    d.rectangle([19, 33, 21, 34], fill=ENG)
    px[15, 33] = ENGC
    px[20, 33] = ENGC
    return img


def scale2x(img):
    w, h = img.size
    src = img.load()
    out = Image.new("RGBA", (w * 2, h * 2), TRANS)
    dst = out.load()

    def g(x, y):
        return src[x, y] if 0 <= x < w and 0 <= y < h else TRANS

    for y in range(h):
        for x in range(w):
            B, D, E, F, H = g(x, y - 1), g(x - 1, y), g(x, y), g(x + 1, y), g(x, y + 1)
            if B != H and D != F:
                e0 = D if D == B else E
                e1 = F if B == F else E
                e2 = D if D == H else E
                e3 = F if H == F else E
            else:
                e0 = e1 = e2 = e3 = E
            dst[2 * x, 2 * y] = e0
            dst[2 * x + 1, 2 * y] = e1
            dst[2 * x, 2 * y + 1] = e2
            dst[2 * x + 1, 2 * y + 1] = e3
    return out


# ---- the rendering methods -------------------------------------------------

def m_smooth(ship, ang):
    """Upscale first, then rotate at screen resolution with filtering (the phony look)."""
    big = ship.resize((T, T), Image.NEAREST)
    return big.rotate(-ang, resample=Image.BICUBIC)


def m_native(ship, ang):
    """Rotate at native res with nearest sampling, then integer-upscale (low-res FBO)."""
    return ship.rotate(-ang, resample=Image.NEAREST).resize((T, T), Image.NEAREST)


def snap9(ang):
    return math.floor(ang / 9 + 0.5) * 9


def m_quant(ship, ang):
    return m_native(ship, snap9(ang))


_big = {}
def m_rotsprite(ship, ang):
    """Scale2x x3 (8x), rotate nearest, downsample nearest = pre-generated frame."""
    if "b" not in _big:
        _big["b"] = scale2x(scale2x(scale2x(ship)))
    r = _big["b"].rotate(-ang, resample=Image.NEAREST)
    return r.resize((S, S), Image.NEAREST).resize((T, T), Image.NEAREST)


def m_rotsprite_q(ship, ang):
    return m_rotsprite(ship, snap9(ang))


# ---- helpers ---------------------------------------------------------------

def font(size):
    for p in ["/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
              "/usr/share/fonts/dejavu/DejaVuSans.ttf"]:
        try:
            return ImageFont.truetype(p, size)
        except OSError:
            pass
    try:
        return ImageFont.load_default(size=size)
    except TypeError:
        return ImageFont.load_default()


random.seed(7)
STARS = [(random.randrange(0, S), random.randrange(0, S), random.choice([60, 90, 130]))
         for _ in range(14)]


def tile(method, ship, ang):
    """A 144x144 'screen' tile: starfield on the 4x grid + ship rendered by method."""
    t = Image.new("RGBA", (T, T), BG)
    d = ImageDraw.Draw(t)
    for sx, sy, v in STARS:
        d.rectangle([sx * UP, sy * UP, sx * UP + UP - 1, sy * UP + UP - 1], fill=(v, v, v + 20, 255))
    t.alpha_composite(method(ship, ang))
    return t


if __name__ == "__main__":
    ship = make_ship()
    ship.save("ship_36.png")
    ship.resize((S * 8, S * 8), Image.NEAREST).save("ship_preview.png")

    f_big, f_small = font(15), font(12)

    # ---------- static comparison ----------
    angles = [0, 9, 18, 27, 45, 63]
    rows = [
        ("Smooth: upscale 4x, then rotate with filtering", m_smooth),
        ("Native res, nearest rotation, integer upscale (low-res FBO)", m_native),
        ("RotSprite-style pre-generated frame", m_rotsprite),
    ]
    GAP, LEFT, TOP = 10, 20, 40
    W = LEFT + len(angles) * (T + GAP) + LEFT
    H = TOP + len(rows) * (T + GAP + 26) + 10
    sheet = Image.new("RGB", (W, H), (34, 36, 48))
    d = ImageDraw.Draw(sheet)
    for ci, a in enumerate(angles):
        x = LEFT + ci * (T + GAP)
        d.text((x + T // 2, 14), f"{a}°", fill=(220, 220, 230), font=f_big, anchor="mm")
    for ri, (label, m) in enumerate(rows):
        y = TOP + ri * (T + GAP + 26)
        d.text((LEFT, y + T + 6), label, fill=(190, 192, 205), font=f_small)
        for ci, a in enumerate(angles):
            x = LEFT + ci * (T + GAP)
            sheet.paste(tile(m, ship, a).convert("RGB"), (x, y))
    sheet.save("rotation_comparison.png")

    # ---------- animated gif ----------
    panels = [
        ("Smooth\ncontinuous angle", m_smooth),
        ("Native + nearest\ncontinuous angle", m_native),
        ("Native + nearest\n9° steps", m_quant),
        ("RotSprite frames\n9° steps", m_rotsprite_q),
    ]
    STEP = 3
    GGAP = 28
    frames = []
    GW = LEFT + len(panels) * (T + GGAP) - GGAP + LEFT
    GH = 12 + T + 54
    for ang in range(0, 360, STEP):
        fr = Image.new("RGB", (GW, GH), (34, 36, 48))
        d = ImageDraw.Draw(fr)
        for pi, (label, m) in enumerate(panels):
            x = LEFT + pi * (T + GGAP)
            fr.paste(tile(m, ship, ang).convert("RGB"), (x, 12))
            d.multiline_text((x + T // 2, 12 + T + 8), label, fill=(200, 202, 215),
                             font=f_small, anchor="ma", align="center", spacing=3)
        frames.append(fr)
    pal = frames[0].quantize(colors=128, method=Image.Quantize.MEDIANCUT)
    qframes = [f.quantize(colors=128, palette=pal, dither=Image.Dither.NONE) for f in frames]
    qframes[0].save("rotation_demo.gif", save_all=True, append_images=qframes[1:],
                    duration=45, loop=0, disposal=1, optimize=False)
    print("done", sheet.size, len(frames))
