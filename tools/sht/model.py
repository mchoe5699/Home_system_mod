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
def button(name,label,x,y,w,h,front=70,color=IV,group='base',textsize=8,labelY=None):
 depth=3 if group=='radio' else (1.5 if h<15 else 2.3)
 body(name,x-w/2,y-h/2,w,h,front-depth,depth,min(w,h)/2-1,color,group,1 if group=='radio' else .6)
 keys.append(dict(name=name,label=label,x=x,y=y,w=w,h=h,group=group,size=textsize,labelY=labelY))
# Rear boxes, mounting plate and independent shell covers. Side photo exposes ventilated lower cases.
body('mount_plate',76,48,895,652,0,35,9,EDGE)
body('mount_lip',86,60,875,630,35,5,8,IV)
body('left_rounded_end',110,89,44,583,40,38,17,IV,curve=5)
body('right_rounded_end',891,89,43,583,40,38,17,IV,curve=5)
# Four coplanar faces leave a true opening; no white sheet covers the bowl.
for n,x,y,w,h in [('cradle_top',155,90,143,46),('cradle_bottom',155,616,143,54),('cradle_left',155,136,15,480),('cradle_right',286,136,12,480)]:
 body('face_'+n,x,y,w,h,40,27,0,IV)
body('cradle_void',170,136,116,480,40,20,0,IV)
body('dial_panel',300,89,155,557,40,28,3,IV)
body('dial_bottom_cap',155,647,300,24,40,28,3,IV)
# Leave the entire CRT aperture open at every depth in front of the glass.
for n,x,y,w,h in [('screen_top',520,110,310,19),('screen_bottom',520,345,310,301),('screen_left',520,129,28,216),('screen_right',796,129,34,216)]:
 body('face_'+n,x,y,w,h,40,28,0,IV)
body('monitor_top_cap',520,88,370,22,40,28,3,IV)
# A pair of actual recessed tracks interrupt the otherwise flush right strip.
for n,x,y,w,h in [('slider_top',830,110,59,118),('slider_middle',830,242,59,18),('slider_bottom',830,274,59,372),('slider_left',830,228,8,46),('slider_right',886,228,3,46)]:
 body('face_'+n,x,y,w,h,40,28,0,IV)
body('monitor_bottom_cap',520,646,370,25,40,28,3,IV)
body('side_strip',457,110,61,537,40,28,1,IV)
# Deep cradle with retaining hook, contacts and visible horizontal speaker openings.
body('hook',207,271,32,44,56,12,4,IV)
for row in range(4):
 for col in range(3):body('hook_perforation',214+col*6,278+row*7,2,3,68,1,1,'#8c917e')
for i in range(10):body('speaker_slot',176,351+i*7,105,3,60.1,0.3,1,'#727965')
for x in (222,260):body('charge_contact',x-5,596,10,10,49,3,5,'#b7a77c')
body('base_led_panel',304,174,145,38,68,1,1,'#303b36')
for x,c in [(328,'#75575a'),(376,'#8a8654'),(420,'#547353')]:
 body('alarm_lens_rim',x-12,180,24,24,69,1,12,'#8b9485')
 body('alarm_lens',x-10,182,20,20,70,1,10,c)
# Buttons: each row constructed with one front datum, not individually stepped.
for n,t,x in [('test','시험',337),('stop','경보정지',378),('restore','복귀',418)]:button(n,t,x,323,31,10,labelY=309)
for n,t,x in [('fire_call','',337),('police_call','',378),('medical_call','',418)]:button(n,t,x,355,31,10)
for row,vs in enumerate(['123','456','789','*0#']):
 for col,k in enumerate(vs):button('n'+k,k,339+40*col,425+40*row,29,30,front=71,textsize=20)
for n,t,x in [('up','▲',340),('down','▼',380),('intercom','호출',421)]:button(n,t,x,597,31,10,labelY=580)
for n,t,y,h in [('emergency','비상',191,39),('security','방범',285,37),('away','외출',323,37),('memory','메모리',397,37),('hold','보류',436,37),('redial','재다이얼',475,37),('guard','경비실',514,37),('door','문/열림',552,37),('speaker','스피커',591,37)]:
 body(n,458,y-h/2,58,h,68.1,.9,0.3,'#d98748' if n=='emergency' else IV,curve=.1)
 keys.append(dict(name=n,label=t,x=487,y=y,w=58,h=h,group='base',size=9,labelY=None))
# Central gaps are genuinely flush blanks, not extra unlabeled buttons.
for n,y in [('charge',231),('power',251),('security_lamp',270),('away_lamp',307),('guard_lamp',498),('door_lamp',535),('speaker_lamp',575)]:body(n,460,y-2,12,4,71,1,1,'#788474')
# CRT surround: rounded outer black face and a continuous sloping cavity.
body('crt_outer',548,129,248,216,68,3,8,'#101b1d')
body('crt_glass',578,174,192,144,53,1,3,'#8d9691')
for n,y in [('contrast',233),('brightness',265)]:
 body(n+'_recess',838,y-5,48,14,59,1,2,'#b3b6a4')
 button(n,'명암조절' if n=='contrast' else '밝기조절',862,y,21,8,front=68,labelY=y-15)
# Hidden bottom edges have drilled ventilation, visible in the new low-angle photographs.
for x in range(539,814,13):body('bottom_vent',x,671,5,5,54,1,2,'#525a4c','underside')
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
def font(sz,bold=False):return ImageFont.truetype(fonts.replace('Regular','Bold') if bold else fonts,sz,index=1)
def ink(remote):
 im=Image.new('RGBA',(1042,950 if remote else 702));d=ImageDraw.Draw(im)
 def text(x,y,t,size=9,color='#4d574b'):d.text((x,y),t,font=font(size,t=='SAMSUNG' and not remote),fill=color,anchor='mm')
 for k in keys:
  if (k['group']=='radio')!=remote:continue
  text(k['x'],k['labelY'] if k['labelY'] is not None else k['y'],k['label'],k['size'],'#718a6b' if remote and k['name'].startswith('r') and len(k['name'])==2 else '#536050')
 if remote:
  text(120,288,'CORDLESS TELEPHONE SHT-101C',8,'#b5beb7');text(184,312,'방전',9,'#bac3bb')
  d.ellipse((58,743,184,778),fill='#4d5d55');text(121,760,'SAMSUNG',20,'#eff0e3')
 else:
  text(353,143,'SAMSUNG',15);text(366,250,'SHT-101C',9);text(373,259,'CORDLESS SECURITY PHONE',5)
  text(610,380,'HV-121',9);text(610,390,'VIDEO MONITOR',6)
  for x,y in [(310,250),(565,380)]:
   d.line((x,y,x+13,y-9,x+26,y),fill='#687561',width=2)
   d.line((x+4,y-1,x+4,y+8,x+21,y+8,x+21,y-1),fill='#687561',width=2)
   d.line((x+10,y+8,x+10,y+1,x+16,y+1,x+16,y+8),fill='#687561',width=2)
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
  if not remote and p['name']=='cradle_void':
   mask=Image.new('L',im.size);md=ImageDraw.Draw(mask);md.rounded_rectangle((170,136,286,616),30,fill=255)
   layer=Image.new('RGBA',im.size);ld=ImageDraw.Draw(layer)
   for yy in range(136,617):
    bowl=math.exp(-((yy-204)/65)**4)+math.exp(-((yy-555)/55)**4)
    shade=int(216-15*min(1,bowl));ld.line((170,yy,286,yy),fill=(shade+4,shade+3,shade-13,255))
   im.paste(layer,(0,0),mask);d=ImageDraw.Draw(im)
   d.rounded_rectangle((170,136,286,616),30,outline='#c1c2af',width=2)
   continue
  if remote:
   d.rounded_rectangle((x+1,y+2,x+w+1,y+h+2),rad,fill='#5d65553a');d.rounded_rectangle((x,y,x+w,y+h),rad,fill=p['color'])
  else:
   d.rounded_rectangle((x,y,x+w,y+h),rad,fill=p['color'])
   is_key=any(p['name']==k['name'] and k['group']=='base' for k in keys)
   if is_key:
    if p['name'].startswith('n') and len(p['name'])==2:
     d.ellipse((x,y,x+w,y+h),fill='#f0eedf',outline='#b8bca8',width=1)
     d.arc((x+1,y+1,x+w-1,y+h-1),190,310,fill='#ffffffc0',width=1)
    elif h<20:
     d.rounded_rectangle((x,y,x+w,y+h),rad,fill='#eeeddf',outline='#b9bdab',width=1)
     d.line((x+rad,y+1,x+w-rad,y+1),fill='#ffffffb0',width=1)
    else:
     d.line((x,y+h,x+w,y+h),fill='#b5b9a5',width=1)
   if p['name'] in ['left_rounded_end','right_rounded_end']:
    mask=Image.new('L',im.size);ImageDraw.Draw(mask).rounded_rectangle((x,y,x+w,y+h),rad,fill=255)
    shine=Image.new('RGBA',im.size);sd=ImageDraw.Draw(shine)
    for xx in range(int(w)+1):
     v=math.sin(math.pi*xx/w);shade=int(225+16*v)
     sd.line((x+xx,y,x+xx,y+h),fill=(shade,shade-2,shade-15,255))
    im.paste(shine,(0,0),mask);d=ImageDraw.Draw(im)
  if remote and p['name'] in ['r'+c for c in '123456789*0#']:
   d.ellipse((x,y,x+w,y+h),fill=p['color']);d.arc((x+3,y+2,x+w-3,y+h-4),200,300,fill='#ffffffa0',width=1)
  if remote and p['d']>2:d.rounded_rectangle((x+1,y+1,x+w-1,y+h-1),max(0,rad-1),outline='#ffffff80',width=1)
 
 if not remote:
  # Hairline joints describe connected shell covers instead of highlighted stacked bars.
  for xx,y0,y1 in [(155,90,670),(298,90,647),(455,89,646),(518,110,646),(830,110,646),(890,90,670)]:
   d.line((xx,y0,xx,y1),fill='#c2c5b2',width=1)
  for x0,x1,yy in [(155,455,647),(520,890,646),(520,890,110)]:d.line((x0,yy,x1,yy),fill='#c2c5b2',width=1)
  # Broad bezel, sloping throat and a separate 4:3 glass face.
  d.rounded_rectangle((548,129,796,345),8,fill='#172223')
  for t in range(18):
   q=t/17;v=int(48-18*q)
   d.rounded_rectangle((548+18*q,129+25*q,796-18*q,345-17*q),max(2,8-6*q),fill=(v,v+7,v+7,255))
  d.rounded_rectangle((578,174,770,318),3,fill='#8d9691')
  d.line((579,175,769,175),fill='#a7afa3',width=2)
 im.alpha_composite(ink(remote));im.save(a/('textures/radio_gui.png' if remote else 'textures/base_'+('docked' if docked else 'empty')+'.png'))
Image.new('RGBA',(2,2),'white').save(a/'textures/solid.png')
(a/'model.json').write_text(json.dumps(dict(parts=parts,keys=keys,scale=.5/824,blue=[110,89,934,672],outer=[76,48,971,700]),ensure_ascii=False))
for folder in ['models/item','blockstates','lang']:(a/folder).mkdir(parents=True,exist_ok=True)
for n in ['sht_101c','wireless_handset']:(a/f'models/item/{n}.json').write_text('{"parent":"builtin/entity"}')
(a/'blockstates/sht_101c.json').write_text('{"variants":{"":{"model":"minecraft:block/air"}}}')
for lang in ['ko_kr','en_us']:(a/f'lang/{lang}.json').write_text(json.dumps({'block.homenet_sht101c.sht_101c':'삼성 SHT-101C 홈오토메이션','item.homenet_sht101c.wireless_handset':'SHT-101C 무선수화기'},ensure_ascii=False))
(root/'pack.mcmeta').write_text('{"pack":{"pack_format":15,"description":"SHT-101C new photo model"}}')
print(len(parts),'new solids,',len(keys),'buttons; blue width',824*.5/824,'outer',895*.5/824,652*.5/824)
