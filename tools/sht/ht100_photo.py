"""Clean HT-100 surface printing, redrawn from the undamaged reference photograph.

All molded parts, glass, controls, and apertures are separate three-dimensional
surfaces in Ht100Visual. This transparent sheet contains only printing and sheen.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parents[2] / 'src/sht/resources/assets/homenet_sht101c/ht100'
OUT.mkdir(parents=True, exist_ok=True)
S = 4
normal = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
bold = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'

def font(size, heavy=False):
    return ImageFont.truetype(bold if heavy else normal, round(size*S))

def pts(seq):
    return [(round(x*S),round(y*S)) for x,y in seq]

print_sheet = Image.new('RGBA', (226*S,300*S))
d = ImageDraw.Draw(print_sheet)
# Home-automation roof emblem and factory model marking. These follow the
# clean silver-blue reference, rather than the yellowed and painted shell.
d.line(pts([(61,38),(72,26),(83,38)]),fill=(65,93,103,230),width=round(2.2*S),joint='curve')
d.line(pts([(65,36),(65,44),(79,44),(79,36)]),fill=(65,93,103,230),width=round(1.3*S),joint='curve')
d.text((91*S,24*S),'HT-100',font=font(15,True),fill=(55,83,92,245))
d.text((91*S,43*S),'DOOR CAMERA',font=font(7.4,True),fill=(63,85,91,225))
# Very faint raised lettering on the button-side base.
d.text((145*S,280*S),'SAMSUNG',font=font(7,True),fill=(93,107,105,128))
d.text((144*S,279*S),'SAMSUNG',font=font(7,True),fill=(226,232,227,100))
print_sheet.save(OUT/'print.png')

# Emission only while ringing. Keep the lens/window mesh visible beneath it.
glow = Image.new('RGBA',(83*S,44*S))
p = glow.load()
for y in range(44*S):
    for x in range(83*S):
        nx=abs((x+.5)/(83*S)-.5)*2
        ny=abs((y+.5)/(44*S)-.5)*2
        amount=max(0.0,1-.18*nx*nx-.23*ny*ny)
        p[x,y]=(29,105,167,round(118*amount))
glow.save(OUT/'glow.png')
print('HT-100 new clean-photo inscriptions and call-state lamp generated')
