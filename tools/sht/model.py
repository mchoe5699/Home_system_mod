"""New photo-coordinate model. Clear 1042x702 front reference; no prior SHT assets."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json,math
root=Path(__file__).resolve().parents[2]/'src/sht/resources'
a=root/'assets/homenet_sht101c';(a/'textures').mkdir(parents=True,exist_ok=True)
parts=[];keys=[]
IV='#f1efdf';EDGE='#e9e6d4';DARK='#172223'
def body(name,x,y,w,h,z,depth,r=3,color=IV,group='base',curve=0):
 parts.append(dict(name=name,x=x,y=y,w=w,h=h,z=z,d=depth,r=r,color=color,group=group,curve=curve))
def button(name,label,x,y,w,h,front=73,color=IV,group='base',textsize=8,labelY=None):
 body(name,x-w/2,y-h/2,w,h,front-3,3,min(w,h)/2-1,color,group,1)
 keys.append(dict(name=name,label=label,x=x,y=y,w=w,h=h,group=group,size=textsize,labelY=labelY))
# Rear boxes, mounting plate and independent shell covers. Side photo exposes ventilated lower cases.
body('rear_controller',155,100,355,535,0,28,8,EDGE)
body('rear_monitor',525,100,365,535,0,28,8,EDGE)
body('mount_plate',76,48,895,652,28,7,9,EDGE)
body('mount_lip',86,60,875,630,35,5,8,IV)
body('left_rounded_end',110,89,44,583,40,38,17,IV,curve=5)
body('right_rounded_end',891,89,43,583,40,38,17,IV,curve=5)
body('cradle_panel',155,90,143,580,40,27,5,IV)
body('dial_panel',300,112,155,534,40,28,3,IV)
body('dial_top_cap',300,89,155,22,40,29,3,IV)
body('dial_bottom_cap',155,647,300,24,40,28,3,IV)
body('monitor_shell',520,110,310,536,40,28,4,IV)
body('monitor_top_cap',520,88,370,20,40,29,3,IV)
body('monitor_right_strip',832,110,57,536,40,29,3,IV)
body('monitor_bottom_cap',520,647,370,24,40,28,3,IV)
body('side_strip',457,110,61,537,40,29,1,IV)
# Deep cradle with retaining hook, contacts and visible horizontal speaker openings.
body('cradle_well',170,136,116,480,65,1,35,'#d5d2bf')
body('cradle_upper_cup',172,139,111,129,66,1,30,'#dedbc8')
body('cradle_lower_cup',176,505,106,106,66,1,26,'#dcd8c2')
body('hook',207,271,32,44,67,9,4,IV)
for row in range(4):
 for col in range(3):body('hook_perforation',214+col*6,278+row*7,2,3,76,1,1,'#8c917e')
for i in range(10):body('speaker_slot',176,351+i*7,105,3,67,1,1,'#727965')
for x in (222,260):body('charge_contact',x-5,596,10,10,68,3,5,'#b7a77c')
body('base_led_panel',304,174,145,38,68,1,1,'#303b36')
for x,c in [(328,'#75575a'),(376,'#8a8654'),(420,'#547353')]:
 body('alarm_lens_rim',x-12,180,24,24,69,1,12,'#8b9485')
 body('alarm_lens',x-10,182,20,20,70,1,10,c)
# Buttons: each row constructed with one front datum, not individually stepped.
for n,t,x in [('test','시험',337),('stop','경보정지',378),('restore','복귀',418)]:button(n,t,x,323,31,10,labelY=309)
for n,t,x in [('fire_call','',337),('police_call','',378),('medical_call','',418)]:button(n,t,x,355,31,10)
for row,vs in enumerate(['123','456','789','*0#']):
 for col,k in enumerate(vs):button('n'+k,k,339+40*col,425+40*row,29,30,front=74,textsize=20)
for n,t,x in [('up','▲',340),('down','▼',380),('intercom','호출',421)]:button(n,t,x,597,31,10,labelY=580)
for n,t,y,h in [('emergency','비상',191,39),('security','방범',285,37),('away','외출',323,37),('memory','메모리',397,37),('hold','보류',436,37),('redial','재다이얼',475,37),('guard','경비실',514,37),('door','문/열림',552,37),('speaker','스피커',591,37)]:
 body(n,458,y-h/2,58,h,69,2,1,'#d98748' if n=='emergency' else IV,curve=.4)
 keys.append(dict(name=n,label=t,x=487,y=y,w=58,h=h,group='base',size=9,labelY=None))
# Central gaps are genuinely flush blanks, not extra unlabeled buttons.
for n,y in [('charge',231),('power',251),('security_lamp',270),('away_lamp',307),('guard_lamp',498),('door_lamp',535),('speaker_lamp',575)]:body(n,460,y-2,12,4,71,1,1,'#788474')
# CRT surround: rounded outer black face and a continuous sloping cavity.
body('crt_outer',548,129,248,216,68,3,8,'#101b1d')
body('crt_glass',573,157,201,166,62,1,3,'#8d9691')
for n,y in [('contrast',233),('brightness',265)]:
 body(n+'_recess',838,y-5,48,14,68,1,2,'#b3b6a4')
 button(n,'명암조절' if n=='contrast' else '밝기조절',862,y,29,9,front=73,labelY=y-15)
# Hidden bottom edges have drilled ventilation, visible in the new low-angle photographs.
for x in range(539,814,13):body('bottom_vent',x,649,5,5,69,1,2,'#717667')
for x in range(173,450,17):body('rear_lower_slot',x,661,5,18,8,1,1,'#797d6e')
# Smooth, slightly waisted back of the cordless handset. Single sculpted surface, seam at lower cap.
body('handset_back',145,126,151,514,68,60,37,IV,'docked',13)
body('handset_cap_seam',153,570,134,1.1,140,1,.5,'#bcbfae','docked')
body('handset_status_hole',206,183,3,3,142,1,1.5,'#566055','docked')
body('radio_socket',176,110,12,24,74,14,5,IV,'docked')
# The radio aerial is vertical in both poses; the base telescopic aerial lies horizontally.
for n,x,y,w,h,z,d,c in [('radio_lower',180,8,5,105,79,5,'#393f39'),('radio_upper',181,-105,3,116,80,3,'#525d51'),('radio_tip',180,-111,5,7,79,5,'#687264')]:body(n,x,y,w,h,z,d,min(w,h)/2,c,'docked')
body('hinge_cover',456,82,61,88,68,22,5,IV,curve=4)
body('hinge_joint',478,45,16,37,54,18,5,'#8b9690')
for n,x,y,w,h,c in [('folded_rod',488,49,430,7,'#a9b1a8'),('folded_inner',706,50,208,4,'#c3c9bf'),('folded_tip',914,47,10,11,'#9aa598')]:body(n,x,y,w,h,57,7,min(w,h)/2,c,'folded')
body('upright_rod',484,-395,5,440,57,5,2,'#a0aaa1','extended')
# Opposite side of the removed handset: deep earpiece bowl, glossy oval digits, dark function keys.
body('radio_face',0,0,240,950,0,62,47,IV,'radio',14)
body('ear_rim',31,28,178,167,62,9,62,IV,'radio',5)
body('ear_bowl',58,51,125,121,69,1,58,'#dedfd0','radio')
body('ear_disc',78,78,84,81,70,1,40,'#e9eadb','radio')
for i in range(9):
 ang=2*math.pi*i/8;x=120 if i==8 else 120+25*math.sin(ang);y=120 if i==8 else 120+25*math.cos(ang)
 body('ear_hole',x-3,y-3,6,6,71,1,3,'#526052','radio')
body('ear_slot',86,225,65,6,62,1,1,'#9caa98','radio')
body('radio_black_panel',12,270,216,86,62,2,8,DARK,'radio',1)
button('talk','통화',92,322,83,31,front=68,color='#dce0db',group='radio',textsize=13)
for row,vs in enumerate(['123','456','789','*0#']):
 for col,k in enumerate(vs):button('r'+k,k,57+65*col,393+57*row,46,34,front=69,color='#25332f',group='radio',textsize=24)
for row,ns in enumerate([[('channel','채널전환'),('redial','재발신'),('memory','메모리')],[('intercom','호출'),('door','문/열림'),('guard','경비실')]]):
 for col,(n,t) in enumerate(ns):button(n,t,56+65*col,616+48*row,47,15,front=68,color='#283630',group='radio',labelY=602+48*row)
body('radio_mic_recess',91,840,57,19,62,1,9,'#c7cdbb','radio')
body('radio_mic',96,843,47,13,63,1,6,IV,'radio')
for x in (73,166):body('radio_charger',x,921,21,21,62,1,10,'#888d80','radio')
body('radio_switch',-2,746,4,25,25,6,1,'#4c584b','radio')
body('radio_aerial',181,-295,5,305,20,5,2,'#3e4d3d','radio')
# Artwork is only ink. Depth, shade and highlights come from the new solid mesh.
fonts='/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
def font(sz):return ImageFont.truetype(fonts,sz,index=1)
def ink(remote):
 im=Image.new('RGBA',(1042,950 if remote else 702));d=ImageDraw.Draw(im)
 def text(x,y,t,size=9,color='#4d574b'):d.text((x,y),t,font=font(size),fill=color,anchor='mm')
 for k in keys:
  if (k['group']=='radio')!=remote:continue
  text(k['x'],k['labelY'] if k['labelY'] is not None else k['y'],k['label'],k['size'],'#718a6b' if remote and k['name'].startswith('r') and len(k['name'])==2 else '#536050')
 if remote:
  text(120,288,'CORDLESS TELEPHONE SHT-101C',8,'#b5beb7');text(184,312,'방전',9,'#bac3bb')
  d.ellipse((58,743,184,778),fill='#4d5d55');text(121,760,'SAMSUNG',20,'#eff0e3')
 else:
  text(353,143,'SAMSUNG',15);text(366,250,'SHT-101C',9);text(373,259,'CORDLESS SECURITY PHONE',5)
  text(610,380,'HV-121',9);text(610,390,'VIDEO MONITOR',6)
  for x in (310,565):d.line((x,250 if x==310 else 380,x+13,241 if x==310 else 371,x+26,250 if x==310 else 380),fill='#687561',width=2)
  d.polygon([(327,183),(324,188),(323,194),(328,199),(334,195),(334,189),(331,192),(330,184),(327,189)],fill='#b98272')
  for gx in [370,376,382]:d.arc((gx-2,185,gx+2,194),0,300,fill='#c0b460',width=1)
  d.line((368,195,384,195),fill='#c0b460',width=1)
  d.ellipse((418,184,422,188),fill='#739773');d.line((420,188,420,198),fill='#739773',width=2);d.line((415,191,425,191),fill='#739773',width=2);d.line((420,194,416,199),fill='#739773',width=1);d.line((420,194,424,199),fill='#739773',width=1)
  for x,t,c in [(337,'♨','#c09150'),(378,'','#668e9f'),(418,'✚','#77a18a')]:text(x,339,t,15,c)
  d.rounded_rectangle((369,336,388,344),2,fill='#668e9f');d.line((373,336,376,332,382,332,386,336),fill='#668e9f',width=2);d.ellipse((372,342,376,346),fill='#668e9f');d.ellipse((382,342,386,346),fill='#668e9f')
  text(339,398,'화재',8);text(380,398,'가스',8)
  text(494,231,'충전',8);text(494,251,'전원',8);text(486,625,'• •',9)
 return im
for remote in (False,True):ink(remote).save(a/('textures/radio_ink.png' if remote else 'textures/base_ink.png'))
# UI drawings use the identical geometry and key coordinates, never game screenshots.
for remote,docked in [(False,False),(False,True),(True,False)]:
 im=Image.new('RGBA',(1042,950 if remote else 702));d=ImageDraw.Draw(im)
 for p in sorted(parts,key=lambda p:p['z']+p['d']):
  if remote and p['group']!='radio' or not remote and p['group'] not in ['base','docked','folded'] or not remote and p['group']=='docked' and not docked:continue
  x,y,w,h=p['x'],p['y'],p['w'],p['h'];rad=min(p['r'],w/2,h/2)
  d.rounded_rectangle((x+1,y+2,x+w+1,y+h+2),rad,fill='#5d65553a');d.rounded_rectangle((x,y,x+w,y+h),rad,fill=p['color'])
  if p['name'] in ['n'+c for c in '123456789*0#']+['r'+c for c in '123456789*0#']:
   d.ellipse((x,y,x+w,y+h),fill=p['color']);d.arc((x+3,y+2,x+w-3,y+h-4),200,300,fill='#ffffffa0',width=1)
  if p['d']>2:d.rounded_rectangle((x+1,y+1,x+w-1,y+h-1),max(0,rad-1),outline='#ffffff80',width=1)
 
 if not remote:
  d.rounded_rectangle((573,157,774,323),3,fill='#8d9691')
  d.line((574,158,773,158),fill='#a7afa3',width=2)
 im.alpha_composite(ink(remote));im.save(a/('textures/radio_gui.png' if remote else 'textures/base_'+('docked' if docked else 'empty')+'.png'))
Image.new('RGBA',(2,2),'white').save(a/'textures/solid.png')
(a/'model.json').write_text(json.dumps(dict(parts=parts,keys=keys,scale=.5/824,blue=[110,89,934,672],outer=[76,48,971,700]),ensure_ascii=False))
for folder in ['models/item','blockstates','lang']:(a/folder).mkdir(parents=True,exist_ok=True)
for n in ['sht_101c','wireless_handset']:(a/f'models/item/{n}.json').write_text('{"parent":"builtin/entity"}')
(a/'blockstates/sht_101c.json').write_text('{"variants":{"":{"model":"minecraft:block/air"}}}')
for lang in ['ko_kr','en_us']:(a/f'lang/{lang}.json').write_text(json.dumps({'block.homenet_sht101c.sht_101c':'삼성 SHT-101C 홈오토메이션','item.homenet_sht101c.wireless_handset':'SHT-101C 무선수화기'},ensure_ascii=False))
(root/'pack.mcmeta').write_text('{"pack":{"pack_format":15,"description":"SHT-101C new photo model"}}')
print(len(parts),'new solids,',len(keys),'buttons; blue width',824*.5/824,'outer',895*.5/824,652*.5/824)
