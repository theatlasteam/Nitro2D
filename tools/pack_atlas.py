#!/usr/bin/env python3
"""Pack art/ sprites into assets/atlas.png + assets/atlas.tbl, bake
h-flipped variants (id_flip, avoids per-frame setTransform cost on
device), and composite background layers. Anchors: bottom-center for
actors, center for pickups/particles.
"""
from PIL import Image, ImageOps
import os

ROOT = os.path.dirname(os.path.abspath(__file__))
ART = os.path.join(ROOT, "..", "art")
ASSETS = os.path.join(ROOT, "..", "assets")
os.makedirs(ASSETS, exist_ok=True)

# (file, id, anchor, bake_flip)
SPRITES = [
    ("hero0.png", "hero0", (14, 30), True),
    ("hero1.png", "hero1", (14, 30), True),
    ("hero2.png", "hero2", (14, 30), True),
    ("hero3.png", "hero3", (14, 30), True),
    ("coin0.png", "coin0", (8, 8), False),
    ("coin1.png", "coin1", (8, 8), False),
    ("coin2.png", "coin2", (8, 8), False),
    ("coin3.png", "coin3", (8, 8), False),
    ("dot.png", "dot", (4, 4), False),
    ("spark.png", "spark", (3, 3), False),
    ("crate.png", "crate0", (12, 24), False),
]

PAD = 1


def pot(v):
    p = 1
    while p < v:
        p *= 2
    return p


items = []
for fn, sid, anchor, flip in SPRITES:
    im = Image.open(os.path.join(ART, fn)).convert("RGBA")
    items.append([sid, im, anchor])
    if flip:
        f = ImageOps.mirror(im)
        w, h = im.size
        items.append([sid + "_flip", f, (w - anchor[0], anchor[1])])

items.sort(key=lambda e: -e[1].size[1])

WIDTH = 256
x = y = row_h = 0
placements = []
for sid, im, anchor in items:
    w, h = im.size
    if x + w + PAD > WIDTH:
        x = 0
        y += row_h + PAD
        row_h = 0
    placements.append((sid, im, anchor, x + PAD, y + PAD))
    x += w + PAD * 2
    row_h = max(row_h, h)
HEIGHT = pot(y + row_h + PAD * 2)
assert HEIGHT <= 512, "atlas too tall: %d" % HEIGHT

sheet = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
lines = ["N2ATLAS 1", "sheet %d %d" % (WIDTH, HEIGHT)]
for sid, im, anchor, px, py in placements:
    sheet.alpha_composite(im, (px, py))
    w, h = im.size
    lines.append("s %s %d %d %d %d %d %d" % (sid, px, py, w, h, anchor[0], anchor[1]))
sheet.save(os.path.join(ASSETS, "atlas.png"), optimize=True)
with open(os.path.join(ASSETS, "atlas.tbl"), "w") as f:
    f.write("\n".join(lines) + "\n")
print("atlas %dx%d, %d sprites" % (WIDTH, HEIGHT, len(placements)))

# backgrounds
sky = Image.open(os.path.join(ART, "bg_far_sky.png")).convert("RGBA")
far = Image.open(os.path.join(ART, "bg_far_hills.png")).convert("RGBA")
bg = sky.copy()
bg.alpha_composite(far.resize((240, 320)))
bg.convert("RGB").save(os.path.join(ASSETS, "bg_far.png"), optimize=True)
near = Image.open(os.path.join(ART, "bg_near_hills.png")).convert("RGBA")
near.save(os.path.join(ASSETS, "bg_near.png"), optimize=True)

# tiles image for TiledLayer (single 24x24 tile, index 1)
Image.open(os.path.join(ART, "tile.png")).convert("RGBA").save(
    os.path.join(ASSETS, "tiles.png"), optimize=True)

# icon
icon = Image.new("RGBA", (32, 32), (15, 23, 42, 255))
hero = Image.open(os.path.join(ART, "hero1.png")).convert("RGBA")
icon.alpha_composite(hero, (2, 1))
icon.save(os.path.join(ASSETS, "icon.png"))
print("bg/tiles/icon done")
