"""Convert a Techne .tcn model (axis-aligned boxes, ModelBase UV layout) into a Minecraft JSON block model.

Techne block models use ModelBase space: y points down, the block spans y 8..24 and x/z -8..8, and the renderer
flips x. Rotations: multiples of 90 on Y are baked into the box; X/Z rotations snap to the nearest 22.5 degrees.

usage: python tcn2json.py model.tcn texture_id out.json [shape names...]
With shape names, only those shapes are exported (to split a model into parts).
"""
import json
import re
import sys
import zipfile


def num(s):
    return [float(v) for v in s.split(",")]


def convert(tcn_path, texture, out_path, only=None):
    xml = zipfile.ZipFile(tcn_path).read("model.xml").decode("utf-8")
    tw, th = num(re.search(r"<TextureSize>(.*?)</TextureSize>", xml).group(1))
    su, sv = 16.0 / tw, 16.0 / th
    elements = []
    for name, body in re.findall(r'<Shape[^>]*name="([^"]*)"[^>]*>(.*?)</Shape>', xml, re.S):
        if only and name not in only:
            continue
        def g(tag):
            m = re.search("<%s>(.*?)</%s>" % (tag, tag), body)
            return m.group(1) if m else None

        px, py, pz = num(g("Position"))
        ox, oy, oz = num(g("Offset"))
        w, h, d = num(g("Size"))
        rx, ry, rz = num(g("Rotation"))
        u, v = num(g("TextureOffset"))

        # Box corners in ModelBase space, relative to the rotation point.
        x0, y0, z0 = ox, oy, oz
        x1, y1, z1 = ox + w, oy + h, oz + d
        # Bake Y rotations of +-90/180 by swapping axes about the rotation point.
        # A Techne y rotation of +90 turns the box a quarter the other way in our (x-flipped) frame.
        q = int(round(-ry / 90.0)) % 4
        for _ in range(q):
            x0, z0, x1, z1 = -z1, x0, -z0, x1
        # ModelBase -> block space (pixels): x' = 8 - x, y' = 24 - y, z' = 8 + z
        fx = sorted([8 - (px + x0), 8 - (px + x1)])
        fy = sorted([24 - (py + y0), 24 - (py + y1)])
        fz = sorted([8 + (pz + z0), 8 + (pz + z1)])

        def uv(a, b, c, e):
            return [round(a * su, 4), round(b * sv, 4), round(c * su, 4), round(e * sv, 4)]

        W, H, D = (d, h, w) if q % 2 else (w, h, d)
        # ModelBox UV layout. x is mirrored, so faces whose horizontal axis is x are flipped back with swapped u.
        faces = {
            "east": {"uv": uv(u, v + D, u + D, v + D + H)},
            "west": {"uv": uv(u + D + W, v + D, u + D + W + D, v + D + H)},
            "up": {"uv": uv(u + D + W, v, u + D, v + D)},
            "down": {"uv": uv(u + D + W + W, v, u + D + W, v + D)},
            "north": {"uv": uv(u + D + W, v + D, u + D, v + D + H)},
            "south": {"uv": uv(u + D + W + D + W, v + D, u + D + W + D, v + D + H)},
        }
        for f in faces.values():
            f["texture"] = "#tex"
        el = {"name": name, "from": [fx[0], fy[0], fz[0]], "to": [fx[1], fy[1], fz[1]], "faces": faces}
        for axis, angle in (("x", rx), ("z", rz)):
            if abs(angle) > 1:
                snapped = max(-45.0, min(45.0, round(angle / 22.5) * 22.5))
                if snapped:
                    # ModelBase flips y and x, so an x rotation keeps its sign and a z rotation flips.
                    el["rotation"] = {"angle": snapped if axis == "x" else -snapped, "axis": axis,
                                      "origin": [8 - px, 24 - py, 8 + pz]}
        elements.append(el)

    model = {"textures": {"tex": texture, "particle": texture}, "elements": elements}
    with open(out_path, "w") as fh:
        json.dump(model, fh, indent=1)
    print("%s: %d elements" % (out_path, len(elements)))


if __name__ == "__main__":
    convert(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4:] or None)
