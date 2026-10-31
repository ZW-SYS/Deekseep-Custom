"""Generates the Deekseep launcher icon (adaptive + legacy mipmaps). GPL-3.0 project asset."""
from PIL import Image, ImageDraw, ImageFilter
import math, os

S = 1024  # master canvas (represents 108dp adaptive canvas)
C1 = (77, 107, 254)   # #4D6BFE
C2 = (124, 77, 255)   # #7C4DFF

def gradient(size):
    img = Image.new("RGB", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * (size - 1))
            px[x, y] = tuple(int(C1[i] + (C2[i] - C1[i]) * t) for i in range(3))
    return img

def sparkle(draw, cx, cy, r, fill):
    pts = []
    for k in range(8):
        ang = math.pi / 4 * k - math.pi / 2
        rr = r if k % 2 == 0 else r * 0.28
        pts.append((cx + rr * math.cos(ang), cy + rr * math.sin(ang)))
    draw.polygon(pts, fill=fill)

def foreground(size=S):
    """White chat bubble with a 'D' monogram cut out and a sparkle, inside the 66dp safe zone."""
    ss = 4
    W = size * ss
    fg = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(fg)
    u = W / 108.0  # 1dp
    # bubble body (safe zone is the central 66dp circle -> keep within ~ 30..78dp)
    x0, y0, x1, y1 = 31 * u, 33 * u, 77 * u, 70 * u
    d.rounded_rectangle([x0, y0, x1, y1], radius=14 * u, fill=255)
    # bubble tail
    d.polygon([(40 * u, 68 * u), (37.5 * u, 78 * u), (50 * u, 69 * u)], fill=255)
    # 'D' monogram cut-out (stroke): outer D minus inner D
    def dshape(drw, x0_, y0_, x1_, y1_, fill):
        h = y1_ - y0_
        drw.rounded_rectangle([x0_, y0_, x1_, y1_], radius=h / 2, fill=fill)
        drw.rounded_rectangle([x0_, y0_, x0_ + h / 2 + 1, y1_], radius=2.2 * u, fill=fill)
    cut = Image.new("L", (W, W), 0)
    cd = ImageDraw.Draw(cut)
    dx0, dy0, dx1, dy1 = 42 * u, 40.5 * u, 66 * u, 62.5 * u
    stroke = 5.4 * u
    dshape(cd, dx0, dy0, dx1, dy1, 255)
    dshape(cd, dx0 + stroke, dy0 + stroke, dx1 - stroke, dy1 - stroke, 0)
    fg = Image.composite(Image.new("L", (W, W), 0), fg, cut)
    # sparkle top-right
    sp = Image.new("L", (W, W), 0)
    sparkle(ImageDraw.Draw(sp), 73.5 * u, 34.5 * u, 7.5 * u, 255)
    # ring around sparkle so it reads separately from the bubble
    ring = Image.new("L", (W, W), 0)
    sparkle(ImageDraw.Draw(ring), 73.5 * u, 34.5 * u, 11 * u, 255)
    fg = Image.composite(Image.new("L", (W, W), 0), fg, ring)
    fg = Image.composite(Image.new("L", (W, W), 255), fg, sp)
    fg = fg.resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    out.putalpha(fg)
    white = Image.new("RGBA", (size, size), (255, 255, 255, 255))
    white.putalpha(fg)
    # soft shadow
    shadow = Image.new("RGBA", (size, size), (20, 20, 60, 0))
    shadow.putalpha(fg.filter(ImageFilter.GaussianBlur(size / 90)).point(lambda v: int(v * 0.35)))
    base = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    base.alpha_composite(shadow, (0, int(size / 140)))
    base.alpha_composite(white)
    return base

def legacy(size, round_shape):
    bg = gradient(S).convert("RGBA")
    bg.alpha_composite(foreground(S))
    # crop the central 72dp of 108dp like a launcher would, then mask
    m = int(S * 18 / 108)
    icon = bg.crop((m, m, S - m, S - m)).resize((size * 4, size * 4), Image.LANCZOS)
    mask = Image.new("L", icon.size, 0)
    md = ImageDraw.Draw(mask)
    if round_shape:
        md.ellipse([0, 0, icon.size[0] - 1, icon.size[1] - 1], fill=255)
    else:
        md.rounded_rectangle([0, 0, icon.size[0] - 1, icon.size[1] - 1],
                             radius=icon.size[0] * 0.22, fill=255)
    icon.putalpha(mask)
    return icon.resize((size, size), Image.LANCZOS)

res = "../../module-universal/res"
dens = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
fg_master = foreground(S)
bg_master = gradient(S)
for name, f in dens.items():
    d = os.path.join(res, "mipmap-" + name)
    os.makedirs(d, exist_ok=True)
    legacy(int(48 * f), False).save(os.path.join(d, "ic_launcher.png"), optimize=True)
    legacy(int(48 * f), True).save(os.path.join(d, "ic_launcher_round.png"), optimize=True)
    a = int(108 * f)
    fg_master.resize((a, a), Image.LANCZOS).save(os.path.join(d, "ic_launcher_foreground.png"), optimize=True)
    bg_master.resize((a, a), Image.LANCZOS).save(os.path.join(d, "ic_launcher_background.png"), optimize=True)
d = os.path.join(res, "mipmap-anydpi-v26"); os.makedirs(d, exist_ok=True)
xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
"""
for n in ("ic_launcher.xml", "ic_launcher_round.xml"):
    open(os.path.join(d, n), "w").write(xml)
# preview / store icon
legacy(512, False).save("deekseep_icon_512.png", optimize=True)
prev = Image.new("RGBA", (1100, 300), (240, 242, 248, 255))
x = 30
for sz in (48, 72, 96, 144, 192):
    prev.alpha_composite(legacy(sz, False), (x, 60)); x += sz + 30
prev.alpha_composite(legacy(192, True), (x, 60))
prev.convert("RGB").save("preview.png")
print("ok")
