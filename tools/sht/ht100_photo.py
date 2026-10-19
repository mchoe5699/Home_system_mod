"""Generate clean-reference HT-100 printing; geometry lives in Ht100Visual."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

assets = Path(__file__).resolve().parents[2] / 'src/sht/resources/assets/homenet_sht101c'
out = assets / 'ht100'
out.mkdir(parents=True, exist_ok=True)
scale = 4
font_file = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
bold_file = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'

def font(size, bold=False):
    return ImageFont.truetype(bold_file if bold else font_file, round(size*scale))

def points(coords):
    return [(round(x*scale), round(y*scale)) for x,y in coords]

# The near-front silver reference establishes the blue-gray factory printing.
label = Image.new('RGBA', (110*scale, 32*scale))
d = ImageDraw.Draw(label)
d.line(points([(2,22),(13,8),(26,22)]), fill=(64,86,101,243), width=round(2.2*scale), joint='curve')
d.line(points([(7,19),(7,27),(19,27),(19,19)]), fill=(64,86,101,243), width=round(1.5*scale), joint='curve')
d.polygon(points([(13,16),(18,21),(13,24)]), fill=(64,86,101,210))
d.text((39*scale,2*scale), 'HT-100', font=font(12.3,True), fill=(63,80,90,250))
d.text((39*scale,18*scale), 'DOOR CAMERA', font=font(6.7,True), fill=(63,80,90,238))
label.save(out/'label.png')

brand = Image.new('RGBA', (70*scale,18*scale))
b=ImageDraw.Draw(brand)
b.text((4*scale,2*scale), 'SAMSUNG', font=font(8,True), fill=(235,239,237,145))
b.text((4*scale,3*scale), 'SAMSUNG', font=font(8,True), fill=(87,101,104,145))
brand.save(out/'brand.png')

# The factory lamp is deep navy and unlit; only a call adds restrained blue emission.
glow = Image.new('RGBA',(76*scale,34*scale))
pixels=glow.load()
for y in range(glow.height):
    for x in range(glow.width):
        nx=abs((x+.5)/glow.width-.5)*2
        ny=abs((y+.5)/glow.height-.5)*2
        pixels[x,y]=(17,70,119,round(max(0,105*(1-.32*nx*nx-.27*ny*ny))))
glow.save(out/'glow.png')

# Item icon uses the same cool silver as the removable rear surround.
icon = Image.new('RGBA',(64,64))
i = ImageDraw.Draw(icon)
i.rounded_rectangle((5,4,59,60),radius=7,fill='#b8c1c2',outline='#718186',width=2)
i.rounded_rectangle((12,11,52,53),radius=5,fill=(0,0,0,0),outline='#809092',width=2)
(assets/'textures/item').mkdir(parents=True,exist_ok=True)
icon.save(assets/'textures/item/ht_100_frame.png')
print('Clean cool-silver HT-100 inscriptions, blue call emission and surround icon generated')
