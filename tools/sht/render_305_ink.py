"""Render the surface markings of the SHT-305V at twice the model resolution."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

SCALE = 2
W, H = 700, 600
FONT = "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"
FONT_BOLD = "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc"
image = Image.new("RGBA", (W * SCALE, H * SCALE))
draw = ImageDraw.Draw(image)


def font(size, bold=False):
    return ImageFont.truetype(FONT_BOLD if bold else FONT, size * SCALE)


def text(x, y, value, size=10, color="#384047", bold=False, anchor="lt"):
    draw.text((x * SCALE, y * SCALE), value, font=font(size, bold), fill=color, anchor=anchor)


def line(points, color, width=1):
    draw.line([(x * SCALE, y * SCALE) for x, y in points], fill=color, width=width * SCALE, joint="curve")


def ellipse(box, color, outline=None, width=1):
    draw.ellipse(tuple(v * SCALE for v in box), fill=color, outline=outline, width=width * SCALE)


# CRT perimeter catches a faint sliver of room light; the glass stays translucent.
line([(258, 66), (258, 214)], "#9998a36a")
line([(260, 64), (511, 64)], "#8d8b9760")

text(557, 42, "전원  사용중", 8, "#a7ada9")
for i, (label, color) in enumerate(
    [("화재", "#d29c79"), ("가스", "#c5ba76"), ("방범", "#87aca4"), ("비상", "#c3978a")]
):
    y = 90 + i * 38
    ellipse((573, y + 11, 579, y + 17), color)
    if i == 0:  # flame
        line([(588, y + 21), (592, y + 9), (596, y + 17), (600, y + 6), (605, y + 21)], color, 2)
    elif i == 1:  # gas / fan
        ellipse((590, y + 9, 601, y + 20), None, color, 2)
        ellipse((594, y + 13, 597, y + 16), color)
    elif i == 2:  # shield
        line([(590, y + 8), (597, y + 6), (604, y + 8), (603, y + 17), (597, y + 22), (591, y + 17), (590, y + 8)], color, 2)
    else:
        line([(589, y + 14), (605, y + 14)], color, 2)
        line([(597, y + 6), (597, y + 22)], color, 2)
    text(620, y + 7, label, 11, "#b1b6b2", anchor="mt")

text(239, 284, "SAMSUNG", 14, "#27313e", bold=True)
for i, label in enumerate(("경비실", "외출", "현관")):
    text(523, 297 + i * 54, label, 10, "#3d4747", anchor="mt")

for i, label in enumerate(("비상", "시험", "가스·화재", "방범설정", "복귀")):
    y = 314 + i * 37
    text(648, y - 18, label, 9, "#454d4b", anchor="mt")
    # Off-state lamp is a small smoked lens beside each slim switch.
    ellipse((606, y + 5, 610, y + 9), "#62695f")

text(527, 478, "문열림", 10, "#394342", anchor="mt")
text(645, 478, "경보정지", 10, "#394342", anchor="mt")

# House-shaped HTA mark is printed low on the face, as in the SHT-305V photo.
line([(215, 563), (226, 553), (237, 563)], "#303f51", 2)
line([(219, 561), (219, 569), (233, 569), (233, 561)], "#303f51", 2)
text(239, 553, "SHT-305V", 9, "#29384b", bold=True)
text(239, 566, "VIDEO DOOR PHONE", 7, "#3a4a58")
text(624, 555, "SAMSUNG", 10, "#5a594d", bold=True, anchor="mt")

target = Path(__file__).resolve().parents[2] / "src/sht/resources/assets/homenet_sht101c/sht305v/ink.png"
target.parent.mkdir(parents=True, exist_ok=True)
image.save(target)
