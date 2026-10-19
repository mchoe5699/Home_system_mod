"""Generate clean HT-100 inscriptions and registration assets (228 x 300)."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/sht/resources/assets/homenet_sht101c'
DATA = ROOT / 'src/sht/resources/data/homenet_sht101c'
OUT = ASSETS / 'ht100'
OUT.mkdir(parents=True, exist_ok=True)
S = 4
font_path = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
bold_path = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'

def font(size, bold=False):
    return ImageFont.truetype(bold_path if bold else font_path, round(size * S))

im = Image.new('RGBA', (228 * S, 300 * S))
d = ImageDraw.Draw(im)
def line(points, color, width=1):
    d.line([(round(x*S),round(y*S)) for x,y in points],fill=color,width=max(1,round(width*S)))
def text(x,y,value,size,color='#3c555e',bold=False):
    d.text((round(x*S),round(y*S)),value,font=font(size,bold),fill=color)

# Delicate perimeter seams and the seam which separates camera and speaking sections.
line([(25,14),(42,9),(192,9),(205,17)],'#8f9c99',.65)
line([(20,194),(29,198),(202,198)],'#87938f',.7)
line([(17,199),(17,280),(22,289)],'#d5dad5',.7)
line([(209,198),(209,281),(203,290)],'#83918f',.7)
# Samsung home-automation emblem in the position shown in the clean reference.
line([(59,39),(71,27),(83,39)],'#54707a',2.5)
line([(65,36),(65,44),(77,44),(77,36)],'#54707a',1.7)
text(89,26,'HT-100',15.0,'#3d5963',True)
text(89,43,'DOOR CAMERA',7.7,'#415b63',True)
text(139,280,'SAMSUNG',7.2,'#7b8784',True)
# Tiny clean surface joints behind the two microphone apertures.
line([(204,29),(204,65)],'#9aa6a2',.45)
# The lower plug has a narrow central keeper line.
line([(110,282),(111,279),(129,279),(130,282)],'#6d7876',.55)
im.save(OUT/'detail.png')

# Blue translucent lamp glow is a separate emissive layer, driven by call state.
lamp = Image.new('RGBA',(86*S,39*S))
ld = ImageDraw.Draw(lamp)
for y in range(39*S):
    v = y/(39*S)
    ld.line([(2*S,y),(84*S,y)],fill=(23,82+round(18*(1-v)),130+round(20*(1-v)),195),width=1)
lamp.save(OUT/'light.png')

for sub, value in [
    ('blockstates/ht_100.json',{'variants':{'':{'model':'minecraft:block/air'}}}),
    ('models/item/ht_100.json',{'parent':'builtin/entity'}),
    ('models/item/ht_100_frame.json',{'parent':'minecraft:item/generated','textures':{'layer0':'homenet_sht101c:item/ht_100_frame'}}),
]:
    target = ASSETS / sub
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(json.dumps(value))
frame_icon=Image.new('RGBA',(64,64));fd=ImageDraw.Draw(frame_icon)
fd.rounded_rectangle((6,4,58,60),radius=6,fill='#b7bdb5',outline='#687876',width=2)
fd.rounded_rectangle((13,11,51,53),radius=4,fill=(0,0,0,0),outline='#6a7470',width=2)
fd.rectangle((15,13,49,51),fill=(0,0,0,0))
(ASSETS/'textures/item').mkdir(parents=True,exist_ok=True)
frame_icon.save(ASSETS/'textures/item/ht_100_frame.png')
loot=DATA/'loot_tables/blocks/ht_100.json';loot.parent.mkdir(parents=True,exist_ok=True)
loot.write_text(json.dumps({'type':'minecraft:block','pools':[
    {'rolls':1,'entries':[{'type':'minecraft:item','name':'homenet_sht101c:ht_100'}]},
    {'rolls':1,'entries':[{'type':'minecraft:item','name':'homenet_sht101c:ht_100_frame'}],
     'conditions':[{'condition':'minecraft:block_state_property','block':'homenet_sht101c:ht_100','properties':{'framed':'true'}}]}
]}))
for lang,names in [('ko_kr',('삼성 HT-100 도어카메라','HT-100 탈착식 외곽틀')),('en_us',('Samsung HT-100 Door Camera','HT-100 Removable Surround'))]:
    path=ASSETS/'lang'/f'{lang}.json';data=json.loads(path.read_text())
    data['block.homenet_sht101c.ht_100']=names[0]
    data['item.homenet_sht101c.ht_100_frame']=names[1]
    path.write_text(json.dumps(data,ensure_ascii=False))
print('HT-100 clean body: 0.300 x 0.228 blocks; separate surround and emissive lamp')
