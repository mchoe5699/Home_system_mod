"""Manual pp.6-10: SHT-102C separate cordless accessory and charging stand."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json,math
root=Path(__file__).resolve().parents[2]/'src/sht/resources/assets/homenet_sht101c';a=root/'portable102';(a/'textures').mkdir(parents=True,exist_ok=True)
parts=[];keys=[]
IV='#f1efdf'
def part(n,x,y,w,h,z=0,d=1,r=1,col=IV,curve=0):parts.append(dict(name=n,x=x,y=y,w=w,h=h,z=z,d=d,r=r,color=col,group='radio',curve=curve))
def key(n,label,x,y,w=32,h=20,r=8):
 part(n,x-w/2,y-h/2,w,h,68,3,r,'#e8e8d7',.8);keys.append(dict(name=n,label=label,x=x,y=y,w=w,h=h,group='radio'))
part('portable_case',30,300,165,600,0,68,59,IV,3)
part('battery_cover_join',36,870,153,1.1,20,.6,.5,'#b6baa9')
part('aerial',178,12,8,308,17,8,4,'#d4d8c7');part('aerial_tip',177,6,10,23,17,9,5,'#b1b9aa')
part('ear_recess',61,332,102,70,68,.7,32,'#caccba')
for row in range(4):
 for col in range(5):
  x=87+col*12;y=351+row*11
  if (x-111)**2/32**2+(y-367)**2/24**2<1.1:part('ear_hole',x,y,3.8,3.8,69,1,1.9,'#6d7767')
part('control_strip',40,418,145,48,68,1,7,'#56615b')
key('talk','통화',76,442,58,24,10);key('intercom','내선',148,442,52,24,10)
for row,vals in enumerate(['123','456','789','*0#']):
 for col,n in enumerate(vals):key('r'+n,n,62+49*col,505+50*row,36,28,14)
for n,l,x,y in [('flash','재발신/포즈',62,723),('memory','메모리',111,723),('redial','재다이얼',160,723),('door','현관',62,780),('guard','경비실',111,780),('onhook','온후크',160,780)]:key(n,l,x,y,36,18,7)
for i in range(3):part('microphone',104+i*5,866,2,2,70,1,1,'#6d7767')
for x in [58,158]:part('charge_contact',x,895,7,5,25,8,2,'#b69c63')
model=dict(parts=parts,keys=keys,footprint=[.4,.4],handset_scale=.0009,atlas=[240,950],dock_lean_degrees=-10)
(a/'model.json').write_text(json.dumps(model,ensure_ascii=False))
fontpath='/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
def f(n):return ImageFont.truetype(fontpath,n,index=1)
ink=Image.new('RGBA',(240,950));di=ImageDraw.Draw(ink)
for k in keys:
 size=16 if k['name'].startswith('r') and len(k['name'])==2 else 8
 y=k['y'] if k['name'] in ['talk','intercom'] or k['name'].startswith('r') and len(k['name'])==2 else k['y']-18
 di.text((k['x'],y),k['label'],font=f(size),fill='#53604e',anchor='mm')
di.text((112,831),'SAMSUNG',font=f(12),fill='#586351',anchor='mm');di.text((112,849),'SHT-102C',font=f(7),fill='#66715f',anchor='mm')
ink.save(a/'textures/radio_ink.png')
im=Image.new('RGBA',(240,950));d=ImageDraw.Draw(im)
for p in parts:
 x,y,w,h=p['x'],p['y'],p['w'],p['h'];r=p['r']
 d.rounded_rectangle((x+1,y+2,x+w+1,y+h+2),r,fill='#aab19c')
 d.rounded_rectangle((x,y,x+w,y+h),r,fill=p['color'])
 if any(k['name']==p['name'] for k in keys):d.rounded_rectangle((x,y,x+w,y+h),r,outline='#b0b9a4',width=1);d.arc((x+1,y+1,x+w-1,y+h-1),200,300,fill='#fffdef',width=1)
im.alpha_composite(ink);im.save(a/'textures/radio_gui.png')
for n in ['sht_102c_portable','sht_102c_charger']:(root/f'models/item/{n}.json').write_text('{"parent":"builtin/entity"}')
(root/'blockstates/sht_102c_charger.json').write_text('{"variants":{"":{"model":"minecraft:block/air"}}}')
for lang in ['ko_kr','en_us']:
 path=root/f'lang/{lang}.json';data=json.loads(path.read_text());data.update({'item.homenet_sht101c.sht_102c_portable':'삼성 SHT-102C 휴대장치','block.homenet_sht101c.sht_102c_charger':'삼성 SHT-102C 휴대장치 충전기'});path.write_text(json.dumps(data,ensure_ascii=False))
assert model['footprint']==[.4,.4];assert len(keys)==20
print('SHT-102C portable: 20 key regions, separate handset; dock footprint .4 x .4 blocks')
