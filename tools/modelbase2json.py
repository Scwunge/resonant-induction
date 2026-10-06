"""Convert boxes from a Java ModelBase class (the original's hand-written models) into a Minecraft JSON block model.

Reads `X = new ModelRenderer(this, u, v)`, `X.addBox(...)`, `X.setRotationPoint(...)` and `setRotation(X, rx, ry, rz)` (radians),
writes them as a Techne model and converts that with tcn2json, so both share the same coordinate and UV handling.

usage: python modelbase2json.py Model.java texture_id out.json part [part...]
"""
import math
import os
import re
import sys
import tempfile
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tcn2json  # noqa: E402

NUM = r"(-?[\d.]+)F?"


def parse(java):
    src = open(java, encoding="utf-8").read()
    tw = int(re.search(r"textureWidth\s*=\s*(\d+)", src).group(1))
    th = int(re.search(r"textureHeight\s*=\s*(\d+)", src).group(1))
    parts = {}
    for name, u, v in re.findall(r"(\w+)\s*=\s*new ModelRenderer\(this,\s*(\d+),\s*(\d+)\)", src):
        parts[name] = {"uv": (float(u), float(v)), "rot": (0.0, 0.0, 0.0)}
    for name, *vals in re.findall(r"(\w+)\.addBox\(" + r",\s*".join([NUM] * 6), src):
        parts[name]["box"] = [float(x) for x in vals]
    for name, *vals in re.findall(r"(\w+)\.setRotationPoint\(" + r",\s*".join([NUM] * 3) + r"\)", src):
        parts.setdefault(name, {})["point"] = [float(x) for x in vals]
    for name, *vals in re.findall(r"setRotation\((\w+),\s*" + r",\s*".join([NUM] * 3) + r"\)", src):
        parts[name]["rot"] = tuple(math.degrees(float(x)) for x in vals)
    return tw, th, parts


def convert(java, texture, out, names):
    tw, th, parts = parse(java)
    shapes = []
    for n in names:
        p = parts[n]
        ox, oy, oz, w, h, d = p["box"]
        px, py, pz = p["point"]
        rx, ry, rz = p["rot"]
        shapes.append(
            '<Shape type="d9e621f7-957f-4b77-b1ae-20dcd0da7751" name="%s"><Position>%s,%s,%s</Position><Offset>%s,%s,%s</Offset>'
            '<Size>%d,%d,%d</Size><Rotation>%s,%s,%s</Rotation><TextureOffset>%d,%d</TextureOffset></Shape>'
            % (n, px, py, pz, ox, oy, oz, w, h, d, rx, ry, rz, p["uv"][0], p["uv"][1]))
    xml = "<Techne><Models><Model><TextureSize>%d,%d</TextureSize><Geometry>%s</Geometry></Model></Models></Techne>" % (tw, th, "".join(shapes))
    fd, tcn = tempfile.mkstemp(suffix=".tcn")
    os.close(fd)
    with zipfile.ZipFile(tcn, "w") as z:
        z.writestr("model.xml", xml)
    try:
        tcn2json.convert(tcn, texture, out)
    finally:
        os.remove(tcn)


if __name__ == "__main__":
    convert(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4:])
