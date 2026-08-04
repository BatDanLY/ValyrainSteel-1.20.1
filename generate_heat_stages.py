"""
Generates the 11 heat-stage textures (dark_sister_stage0.png ... stage10.png) for Dark Sister,
starting from your real dark_sister.png texture.

Only the pixels belonging to the "blade" element's UV islands are recolored/heated - the hilt,
crossguard and pommel artwork is copied through untouched. Run this once locally:

    pip install pillow
    python generate_heat_stages.py /path/to/your/dark_sister.png /path/to/output/folder

Then drop the 11 output PNGs into assets/valyrainsteel/textures/item/ alongside your existing
texture, and put the model jsons (dark_sister.json, dark_sister_base.json, dark_sister_stage1..10.json)
into assets/valyrainsteel/models/item/.
"""

import os
import sys
from PIL import Image

# UV islands used by the "blade" element's faces, in 16-unit model-UV space, converted to pixel
# space for a 64x64 texture (4 px per unit). Update PX_PER_UNIT if your texture_size differs.
PX_PER_UNIT = 4

# (x0, y0, x1, y1) rectangles, taken directly from the blade element's face UVs in the model file
BLADE_UV_RECTS_16 = [
    (0.00, 0.00, 2.25, 13.50),   # north face (front of blade)
    (2.25, 0.00, 4.50, 13.50),   # south face (back of blade)
    (7.75, 0.00, 8.00, 12.50),   # east edge
    (8.00, 0.00, 8.25, 12.50),   # west edge
    (8.25, 8.00, 9.75, 8.25),    # up (tip cross-section sliver)
    (9.75, 8.00, 11.25, 8.25),   # down (base cross-section sliver)
]

STEEL = (210, 213, 218)
GLOW_MID = (255, 140, 40)
WHITE_HOT = (255, 250, 220)

# If your model's UV v=0 end is the HILT end rather than the TIP, flip this to False so the
# flame accents land at the correct end of the blade.
UV_ZERO_IS_TIP = True


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def blade_color_for_stage(stage):
    t = stage / 10.0
    if t <= 0.5:
        return lerp(STEEL, GLOW_MID, t / 0.5)
    return lerp(GLOW_MID, WHITE_HOT, (t - 0.5) / 0.5)


def to_pixel_rects():
    return [tuple(v * PX_PER_UNIT for v in rect) for rect in BLADE_UV_RECTS_16]


def generate(src_path, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    base = Image.open(src_path).convert("RGBA")
    w, h = base.size
    rects = to_pixel_rects()

    for stage in range(0, 11):
        img = base.copy()
        px = img.load()
        color = blade_color_for_stage(stage)
        t = stage / 10.0

        for (x0, y0, x1, y1) in rects:
            xi0, yi0 = max(0, int(x0)), max(0, int(y0))
            xi1, yi1 = min(w, int(round(x1))), min(h, int(round(y1)))
            for x in range(xi0, xi1):
                for y in range(yi0, yi1):
                    r, g, b, a = px[x, y]
                    if a == 0:
                        continue  # leave transparent/background pixels alone
                    # Blend original shading with the stage color, keep some of the original
                    # luminance so linework/shading on your art still reads through
                    lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                    nr = int(color[0] * (0.5 + 0.5 * lum))
                    ng = int(color[1] * (0.5 + 0.5 * lum))
                    nb = int(color[2] * (0.5 + 0.5 * lum))
                    px[x, y] = (min(255, nr), min(255, ng), min(255, nb), a)

            # Flame-lick accents near the tip for hot stages
            if stage >= 7:
                tip_y = yi0 if UV_ZERO_IS_TIP else yi1 - 1
                step = 1 if UV_ZERO_IS_TIP else -1
                flame_color = (255, 210, 80, 230) if stage < 9 else (255, 245, 200, 240)
                for x in range(xi0, xi1, 2):
                    yy = tip_y + step * ((x - xi0) % 3)
                    if 0 <= yy < h and px[x, yy][3] > 0:
                        px[x, yy] = flame_color

        out_path = f"{out_dir}/dark_sister_stage{stage}.png"
        img.save(out_path)
        print(f"wrote {out_path}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("usage: python generate_heat_stages.py <path/to/dark_sister.png> <output_dir>")
        sys.exit(1)
    generate(sys.argv[1], sys.argv[2])
