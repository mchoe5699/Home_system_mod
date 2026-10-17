from pathlib import Path
import json,math
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[2]/'src/samsung/resources';A=ROOT/'assets/qwertys_homenet_samsung'
for n in ['textures','models/item','blockstates','lang']:(A/n).mkdir(parents=True,exist_ok=True)
font='/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
def f(n,bold=False):return ImageFont.truetype(font.replace('Regular','Bold') if bold else font,n,index=1)
parts=[];keys=[]
# All front dimensions are photo coordinates. z is distance from the wall in photo units.
def part(name,x,y,w,h,z,d,r,color,group='body',bevel=2):
 parts.append(dict(name=name,x=x,y=y,w=w,h=h,z=z,d=d,r=r,color=color,group=group,bevel=bevel))
def key(name,label,x,y,w,h,z=53,color='#f2eedc',style='pill'):
 part(name,x-w/2,y-h/2,w,h,z,4,h/2-1 if style=='pill' else 1,color,'body',1.3);keys.append(dict(name=name,label=label,x=x,y=y,w=w,h=h))
part('outer_mount',0,0,1000,740,0,25,12,'#e9e5d3',bevel=5)
part('mount_inset',23,22,954,699,25,5,10,'#aaa99f')
part('left_trim',35,34,40,672,28,40,18,'#f5f2e1',bevel=7)
part('right_trim',924,34,41,672,28,40,18,'#f5f2e1',bevel=7)
part('control_housing',247,35,187,668,28,26,7,'#f3efdd',bevel=4)
part('function_strip_housing',435,35,70,668,28,28,4,'#f3efdd',bevel=2)
part('monitor_housing',508,35,414,668,28,26,7,'#f5f1df',bevel=4)
# Sculpted recessed charger: backing, deep cups, grille and metal contacts.
part('cradle_back',76,37,170,665,27,18,8,'#ebe7d4')
part('cradle_shadow',91,85,141,571,43,1,45,'#c5c5b6')
part('ear_cup',91,85,141,150,44,1,39,'#d9d9c8')
part('microphone_cup',91,515,141,141,44,1,34,'#d1d1bd')
for i in range(11):part('cradle_grille_'+str(i),108,319+i*9,110,3.6,44,1,1,'#787c70',bevel=.2)
part('hook_contact',151,235,25,49,44,10,3,'#e7e7db')
for i in range(4):
 for j in range(3):part('hook_hole',156+j*6,243+i*7,2,2,54,1,1,'#939487',bevel=.1)
for x in (148,199):part('charging_contact',x,622,9,9,45,5,4,'#a79c6a',bevel=1)
# Cordless back faces forward when docked. Continuous rounded shell, no ribbed layered boxes.
part('handset_back',86,96,148,571,44,66,44,'#f5f2e1','docked',11)
part('handset_bottom_seam',98,585,123,1,110,1,.4,'#d0d0c5','docked',.1)
part('handset_status',158,190,4,4,110,1,2,'#867a6a','docked',.1)
part('handset_mic',158,627,5,5,110,1,2,'#777b70','docked',.1)
# smoked panel and indicator lenses
part('indicator_panel',256,139,172,43,54,1,2,'#303c39',bevel=.3)
for name,x,c in [('fire',285,'#744e4c'),('gas',341,'#8b8650'),('security',397,'#4e6b53')]:
 part(name+'_rim',x-18,143,36,36,55,1,18,'#6c736b',bevel=.4);part(name+'_lens',x-15,146,30,30,56,1,15,c,bevel=.3)
# Each row shares z and d so adjacent buttons have identical protrusion.
# All key positions from the clear front image.
for name,label,x in [('test','시험',285),('stop','경보정지',339),('restore','복귀',393)]:key(name,label,x,313,40,13)
for name,label,x in [('quick_fire','소방',285),('quick_police','경찰',339),('quick_medical','병원',393)]:key(name,label,x,348,40,13)
for row,vals in enumerate(['123','456','789','*0#']):
 for col,v in enumerate(vals):key('digit_'+v,v,286+55*col,435+48*row,36,37,z=54,style='pill')
for name,label,x in [('up','▲',286),('down','▼',341),('intercom','호출',396)]:key(name,label,x,635,42,13)
strip=[('emergency','비상',139,48),('security','방범',248,42),('away','외출',292,42),('memory','메모리',379,45),('hold','보류',425,44),('redial','재다이얼',470,44),('guard','경비실',515,44),('door','문/열림',560,44),('speaker','스피커',605,44)]
for name,label,y,h in strip:key(name,label,470,y+h/2,68,h,z=56,color='#c78242' if name=='emergency' else '#f3efdd',style='flat')
for name,y in [('charge',210),('power',232),('armed',245),('away',290),('guard',513),('door',558),('speaker',603)]:part(name+'_lamp',440,y,12,4,57,1,1,'#7e8678',bevel=.2)
# CRT has a real shallow recess and inward sloping surround.
part('monitor_upper_bezel',540,87,300,25,54,3,5,'#18211f',bevel=2)
part('monitor_lower_bezel',540,330,300,24,54,3,5,'#18211f',bevel=2)
part('monitor_left_bezel',540,112,25,218,54,3,1,'#18211f',bevel=1)
part('monitor_right_bezel',815,112,25,218,54,3,1,'#18211f',bevel=1)
part('crt_recess',565,112,250,218,37,1,2,'#090e0d',bevel=.5)
part('crt_back',578,124,224,193,42,1,2,'#8b918a',bevel=.5)
for name,y,label in [('contrast',215,'명암조절'),('brightness',250,'밝기조절')]:
 part(name+'_track',875,y,42,13,54,1,2,'#a7aa9b');key(name,label,894,y+6,26,10,z=55,style='flat')
# Body hinge and telescopic aerials, separately selectable fold states.
part('antenna_hinge',435,32,70,106,54,22,2,'#f2eedb',bevel=4)
part('antenna_pivot',462,4,15,32,43,18,5,'#858d89','body',3)
part('antenna_folded',470,-1,465,8,44,8,4,'#9ba29b','folded',2)
part('antenna_folded_tip',930,-3,11,12,43,10,4,'#aeb5aa','folded',2)
part('antenna_upright',465,-520,6,525,44,6,3,'#8d9790','upright',1)
part('antenna_upright_tip',463,-526,10,10,43,8,4,'#9da59d','upright',1)
# Cordless aerial stays vertical both docked and handheld, per the requested installation.
part('radio_antenna_socket',114,84,13,24,55,12,5,'#f2eedb','docked',2)
part('radio_antenna',118,-110,5,198,59,5,2,'#52584c','radio_antenna',1)
part('radio_antenna_tip',117,-116,7,8,58,7,3,'#707768','radio_antenna',1)
# Artwork has transparent backgrounds, leaving the geometry to provide real highlights/shadows.
im=Image.new('RGBA',(1000,740));d=ImageDraw.Draw(im)
def text(x,y,t,size=11,fill='#4f564b',anchor='mm',bold=False):d.text((x,y),t,font=f(size,bold),fill=fill,anchor=anchor)
text(318,101,'SAMSUNG',17,bold=True);text(334,222,'SHT-101C',12,bold=True);text(334,236,'CORDLESS SECURITY PHONE',6)
text(625,393,'HV-121',9,bold=True);text(625,405,'VIDEO MONITOR',7)
for k in keys:
 if k['name'].startswith('digit_'):text(k['x'],k['y'],k['label'],25,'#697362')
 elif k['name'] in ['test','stop','restore','up','down','intercom']:text(k['x'],k['y']-19,k['label'],10)
 elif k['name'] not in ['contrast','brightness','quick_fire','quick_police','quick_medical']:text(k['x'],k['y'],k['label'],10)
for y,t in [(210,'충전'),(232,'전원')]:text(484,y+2,t,9)
text(894,205,'명암조절',8);text(894,241,'밝기조절',8)
for x,t,c in [(285,'火','#a16f67'),(341,'가스','#b5ab5a'),(397,'방범','#6b9d76')]:text(x,161,t,13,c)
text(341,674,'SAMSUNG HOME TECH',6);text(470,660,'• •',10)
text(286,403,'화재',10);text(341,403,'가스',10)
d.polygon([(278,336),(275,330),(281,322),(283,328),(287,319),(291,329),(294,332),(290,337)],fill='#bf9867')
d.rounded_rectangle((329,325,349,334),3,fill='#678c9a');d.line((334,325,337,320,343,320,346,325),fill='#678c9a',width=2)
d.ellipse((332,332,336,336),fill='#678c9a');d.ellipse((343,332,347,336),fill='#678c9a')
d.rectangle((390,320,396,336),fill='#759b7e');d.rectangle((385,325,401,331),fill='#759b7e')
d.line((550,390,562,381,574,390),fill='#69776a',width=3);d.line((553,389,553,400,570,400,570,389),fill='#69776a',width=2)
im.save(A/'textures/front.png')
# Handheld face is on the opposite side of its docked back.
hi=Image.new('RGBA',(240,680));hd=ImageDraw.Draw(hi)
hd.rounded_rectangle((5,5,235,675),radius=45,fill='#f4f1df',outline='#c7c9bc',width=2)
hd.ellipse((60,31,180,151),fill='#d0d3c4',outline='#a9af9c',width=2)
for a in range(8):
 ang=a*math.pi/4;x=120+22*math.cos(ang);y=90+22*math.sin(ang);hd.ellipse((x-2,y-2,x+2,y+2),fill='#737b6a')
hd.rounded_rectangle((43,190,197,251),12,fill='#253b34')
hd.rounded_rectangle((75,210,165,227),8,fill='#e2e2d3');hd.text((120,218),'통화',font=f(11),fill='#535d49',anchor='mm')
for r,vals in enumerate(['123','456','789','*0#']):
 for c,v in enumerate(vals):
  x=65+c*55;y=290+r*48;hd.ellipse((x-15,y-15,x+15,y+15),fill='#294637',outline='#85937e',width=2);hd.text((x,y),v,font=f(20),fill='#c5d2af',anchor='mm')
for r,vals in enumerate([['채널','메모리','재발신'],['호출','문/열림','경비실']]):
 for c,v in enumerate(vals):
  x=65+c*55;y=490+r*32;hd.rounded_rectangle((x-23,y-7,x+23,y+7),5,fill='#d7d9c9');hd.text((x,y),v,font=f(8),fill='#4c5e46',anchor='mm')
hd.text((120,568),'SAMSUNG',font=f(18,True),fill='#405240',anchor='mm');hd.ellipse((100,620,140,637),outline='#b1b8a4',width=2)
hi.save(A/'textures/handset.png')
# White texel provides vertex-coloured solid material; CRT remains a genuine recessed mesh.
Image.new('RGBA',(2,2),'white').save(A/'textures/white.png')
(A/'geometry.json').write_text(json.dumps(dict(parts=parts,keys=keys,blue_width=930,scale=.5/930,canvas=[1000,740]),ensure_ascii=False))
for name in ['sht_101c','sht_101c_cordless']:(A/f'models/item/{name}.json').write_text(json.dumps({'parent':'builtin/entity','gui_light':'front','display':{'gui':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},'fixed':{'scale':[.8,.8,.8]},'thirdperson_righthand':{'scale':[.7,.7,.7]},'firstperson_righthand':{'scale':[.75,.75,.75]}}}))
(A/'blockstates/sht_101c.json').write_text(json.dumps({'variants':{'':{'model':'minecraft:block/air'}}}))
for lang in ['en_us','ko_kr']:(A/f'lang/{lang}.json').write_text(json.dumps({'block.qwertys_homenet_samsung.sht_101c':'삼성 SHT-101C 홈오토메이션','item.qwertys_homenet_samsung.sht_101c_cordless':'SHT-101C 무선수화기'},ensure_ascii=False))
(ROOT/'pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':15,'description':'Samsung SHT-101C'}}))
print(len(parts),'sculpted parts,',len(keys),'photo-positioned keys; blue width exactly .5 block')

# Static product-control artwork, generated from the same part layout (never a gameplay screenshot).
for docked in [False,True]:
 gui=Image.new('RGBA',(1000,740))
 gd=ImageDraw.Draw(gui)
 for p in sorted(parts,key=lambda p:p['z']+p['d']):
  if p['group'] not in ['body','docked'] or p['group']=='docked' and not docked:continue
  x,y,w,h=p['x'],p['y'],p['w'],p['h'];r=max(0,min(p['r'],w/2,h/2))
  gd.rounded_rectangle((x+1,y+2,x+w+1,y+h+2),radius=r,fill='#999e9140')
  gd.rounded_rectangle((x,y,x+w,y+h),radius=r,fill=p['color'])
  if p['d']>3:gd.rounded_rectangle((x+1,y+1,x+w-1,y+h-1),radius=max(0,r-1),outline='#ffffff80',width=1)
 gui.alpha_composite(im);gui.save(A/('textures/gui_docked.png' if docked else 'textures/gui_empty.png'))

labels=Image.new('RGBA',(240,680));ld=ImageDraw.Draw(labels)
for r,vals in enumerate(['123','456','789','*0#']):
 for c,v in enumerate(vals):ld.text((65+c*55,290+r*48),v,font=f(20),fill='#b6c8a0',anchor='mm')
ld.text((120,218),'통화',font=f(11),fill='#53614b',anchor='mm')
for r,vals in enumerate([['채널','메모리','재발신'],['호출','문/열림','경비실']]):
 for c,v in enumerate(vals):ld.text((65+c*55,490+r*32),v,font=f(8),fill='#4c5e46',anchor='mm')
ld.text((120,568),'SAMSUNG',font=f(18,True),fill='#405240',anchor='mm')
labels.save(A/'textures/handset_labels.png')
