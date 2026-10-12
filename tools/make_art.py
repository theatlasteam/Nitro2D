#!/usr/bin/env python3
"""Generate HQ demo art for Nitro2D (Pillow). Vector-crisp shapes with
subtle grain so gradients survive the 16-bit phone screen. Writes art/.
Run pack_atlas.py after.
"""
from PIL import Image, ImageDraw
import os, random

ART = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "art")
os.makedirs(ART, exist_ok=True)
random.seed(7)


def grain(im, amt=7):
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            n = random.randint(-amt, amt)
            px[x, y] = (max(0, min(255, r + n)), max(0, min(255, g + n)),
                        max(0, min(255, b + n)), a)
    return im


def hero(frame):
    im = Image.new("RGBA", (28, 30), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # legs (run cycle offsets)
    legs = [(8, 0), (11, 3), (8, 5), (5, 3)][frame]
    d.rounded_rectangle([7 + legs[1] - 4, 20, 7 + legs[1] + 1, 29], radius=2, fill=(30, 41, 59, 255))
    d.rounded_rectangle([19 - legs[1] - 1, 20, 19 - legs[1] + 4, 29], radius=2, fill=(30, 41, 59, 255))
    # body
    d.rounded_rectangle([5, 6, 23, 23], radius=5, fill=(56, 189, 248, 255))
    d.rounded_rectangle([5, 6, 23, 12], radius=5, fill=(125, 211, 252, 255))
    d.rectangle([5, 9, 23, 12], fill=(125, 211, 252, 255))
    # visor
    d.rounded_rectangle([8, 11, 20, 17], radius=3, fill=(15, 23, 42, 255))
    d.rounded_rectangle([10, 12, 15, 14], radius=1, fill=(165, 243, 252, 255))
    # antenna
    d.line([14, 6, 14, 1], fill=(30, 41, 59, 255), width=2)
    d.ellipse([12, 0, 16, 3], fill=(250, 204, 21, 255))
    return grain(im)


def coin(frame):
    w = [16, 11, 6, 11][frame]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    x0 = (16 - w) // 2
    d.ellipse([x0, 1, x0 + w - 1, 14], fill=(250, 204, 21, 255))
    d.ellipse([x0, 1, x0 + w - 1, 14], outline=(161, 98, 7, 255))
    if w > 8:
        d.ellipse([x0 + 3, 4, x0 + w - 4, 11], outline=(253, 224, 71, 255))
    return grain(im, 5)


def tile():
    im = Image.new("RGBA", (24, 24), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 23, 23], fill=(100, 116, 139, 255))
    d.rectangle([0, 0, 23, 5], fill=(148, 163, 184, 255))
    d.rectangle([0, 11, 23, 12], fill=(71, 85, 105, 255))
    d.rectangle([0, 22, 23, 23], fill=(51, 65, 85, 255))
    d.rectangle([11, 0, 12, 11], fill=(71, 85, 105, 255))
    d.rectangle([5, 12, 6, 23], fill=(71, 85, 105, 255))
    d.rectangle([17, 12, 18, 23], fill=(71, 85, 105, 255))
    return grain(im)


def crate():
    im = Image.new("RGBA", (24, 24), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 23, 23], fill=(180, 120, 60, 255))
    d.rectangle([0, 0, 23, 23], outline=(110, 70, 35, 255))
    d.rectangle([3, 3, 20, 20], outline=(110, 70, 35, 255))
    d.line([0, 0, 23, 23], fill=(110, 70, 35, 255))
    d.line([23, 0, 0, 23], fill=(110, 70, 35, 255))
    d.rectangle([0, 0, 23, 4], fill=(205, 145, 80, 255))
    return grain(im)


def dot():
    im = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse([0, 0, 7, 7], fill=(255, 255, 255, 220))
    d.ellipse([2, 2, 5, 5], fill=(255, 255, 255, 255))
    return im


def spark():
    im = Image.new("RGBA", (6, 6), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.line([3, 0, 3, 5], fill=(253, 224, 71, 255))
    d.line([0, 3, 5, 3], fill=(253, 224, 71, 255))
    d.point([3, 3], fill=(255, 255, 255, 255))
    return im


def sky(w, h):
    top = (13, 25, 70)
    bot = (190, 120, 200)
    im = Image.new("RGBA", (w, h))
    px = im.load()
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)) + (255,)
        for x in range(w):
            px[x, y] = c
    d = ImageDraw.Draw(im)
    d.ellipse([w - 90, 30, w - 10, 110], fill=(255, 236, 180, 255))
    d.ellipse([w - 80, 40, w - 20, 100], fill=(255, 244, 200, 255))
    return grain(im)


def hills(w, h, base, amp, color, seed):
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rnd = random.Random(seed)
    pts = []
    x = 0
    while x <= w:
        pts.append((x, base + rnd.randint(-amp, amp)))
        x += 40
    pts = [(0, h)] + pts + [(w, h)]
    d.polygon(pts, fill=color + (255,))
    return grain(im, 4)


for f in range(4):
    hero(f).save(os.path.join(ART, "hero%d.png" % f))
    coin(f).save(os.path.join(ART, "coin%d.png" % f))
tile().save(os.path.join(ART, "tile.png"))
crate().save(os.path.join(ART, "crate.png"))
dot().save(os.path.join(ART, "dot.png"))
spark().save(os.path.join(ART, "spark.png"))
sky(240, 320).save(os.path.join(ART, "bg_far_sky.png"))
hills(240, 320, 210, 26, (46, 60, 110), 3).save(os.path.join(ART, "bg_far_hills.png"))
hills(480, 320, 250, 30, (22, 32, 68), 9).save(os.path.join(ART, "bg_near_hills.png"))
print("art done:", sorted(os.listdir(ART)))
