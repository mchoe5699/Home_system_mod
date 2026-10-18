"""SHT-102C photo variant. Right-side coordinates are inherited and asserted unchanged."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json,copy,math
root=Path(__file__).resolve().parents[2]/'src/sht/resources/assets/homenet_sht101c'
a=root/'sht102';(a/'textures').mkdir(parents=True,exist_ok=True)
m=copy.deepcopy(json.loads((root/'model.json').read_text()));old=copy.deepcopy(m)
parts=m['parts'];keys=m['keys'];IV='#f1efdf'
# No cordless handset, handset antenna, charging contacts or cable is modeled.
remove={'hook','hook_perforation','charge_contact','charge','power','radio_socket','hinge_joint'}
parts[:]=[p for p in parts if p['group'] not in ['radio','docked','folded','extended'] and p['name'] not in remove]
keys[:]=[k for k in keys if k['group']=='base']
def add(name,x,y,w,h,z,d,r=2,color=IV,group='base',curve=0):
 parts.append(dict(name=name,x=x,y=y,w=w,h=h,z=z,d=d,r=r,color=color,group=group,curve=curve))
# Widen the lamp/test/dial column into the narrower wired-handset bay.
for p in parts:
 n=p['name']
 if n=='base_led_panel':p.update(x=285,w=171)
 if n in ['alarm_lens','alarm_lens_rim']:
  center=p['x']+p['w']/2;delta={328:-8,376:-6,420:0}[int(center)];p['x']+=delta
 if n=='cradle_void':p.update(x=160,w=114)
 if n=='speaker_slot':p.update(x=168,w=96,z=53.1)
 if n in ['test','stop','restore','fire_call','police_call','medical_call']:
  p['x']-=9;p['y']+=26
 if len(n)==2 and n[0]=='n':
  center=p['x']+p['w']/2;p['x']={339:328,379:369,419:410}[int(center)]-15.5;p['w']=31;p['h']=31;p['r']=14.5;p['y']+=9.5
 if n in ['up','down','intercom']:p['x']-=9;p['y']+=6
 if n=='memory':p['y']-=37
 if n=='door':p['name']='front'
 if n=='speaker':p['name']='line'
for k in keys:
 n=k['name']
 if n in ['test','stop','restore','fire_call','police_call','medical_call']:
  k['x']-=9;k['y']+=26
  if k['labelY'] is not None:k['labelY']+=26
 if len(n)==2 and n[0]=='n':k.update(x={339:328,379:369,419:410}[int(k['x'])],y=k['y']+10,w=31,h=31)
 if n in ['up','down','intercom']:
  k['x']-=9;k['y']+=6
  if k['labelY'] is not None:k['labelY']+=6
 if n=='intercom':k['label']='내선'
 if n=='memory':k['y']-=37
 if n=='door':k.update(name='front',label='현관')
 if n=='speaker':k.update(name='line',label='국선')
add('wired_hook',198,190,25,55,53,15,2)
add('power_round',321,305,10,10,68,1,5,'#526052')
add('wired_handle',154,132,120,508,68,39,9,IV,'docked',2)
add('wired_handle_join',155,627,118,1,107,0.3,.3,'#b9bcaa','docked')
add('black_aerial_hinge',478,45,16,37,54,18,5,'#252b28')
add('black_aerial',483,-205,7,250,57,7,3,'#202522','extended')
add('black_aerial_tip',482,-215,9,20,57,7,4,'#171c19','extended')
add('black_aerial_folded',488,49,250,7,57,7,3,'#202522','folded')
add('black_aerial_folded_tip',730,48,18,9,57,7,4,'#171c19','folded')
m['shell']['holes'][0]=[160,136,114,480]
for j in m['shell']['seams']:
 if j['name']=='cradle_keypad':j['x0']=j['x1']=285
 if j['name']=='brand_keypad':j['x0']=285
m['variant']='SHT-102C';m['wired']=True
(a/'model.json').write_text(json.dumps(m,ensure_ascii=False))
# Ink is authored separately; no reference photograph is pasted into the model.
fontpath='/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
def font(sz,bold=False):return ImageFont.truetype(fontpath.replace('Regular','Bold') if bold else fontpath,sz,index=1)
ink=Image.new('RGBA',(1042,720));d=ImageDraw.Draw(ink)
def text(x,y,t,sz=9,color='#4d574b'):d.text((x,y),t,font=font(sz,t=='SAMSUNG'),fill=color,anchor='mm')
for k in keys:text(k['x'],k['labelY'] if k['labelY'] is not None else k['y'],k['label'],k['size'])
text(350,143,'SAMSUNG',15);text(366,250,'SHT-102C',9);text(369,259,'CORDLESS SECURITY PHONE',5)
text(610,380,'HV-121',9);text(610,390,'VIDEO MONITOR',6);text(326,294,'전원',8)
for x,y in [(298,250),(565,380)]:
 d.line((x,y,x+13,y-9,x+26,y),fill='#687561',width=2);d.line((x+4,y-1,x+4,y+8,x+21,y+8,x+21,y-1),fill='#687561',width=2)
# Indicator icons match the photo: fire, gas, security, followed by the three direct-call icons.
d.polygon([(319,183),(315,191),(319,199),(326,195),(325,187),(321,191),(320,184)],fill='#b98272')
for x in [364,370,376]:d.arc((x-2,185,x+2,194),0,300,fill='#c0b460')
d.ellipse((418,184,422,188),fill='#739773');d.line((420,188,420,198),fill='#739773',width=2);d.line((415,191,425,191),fill='#739773',width=2)
for x,t,c in [(328,'♨','#c09150'),(369,'☎','#668e9f'),(410,'✚','#77a18a')]:text(x,365,t,15,c)
text(328,408,'화재',8);text(369,408,'가스',8);text(486,625,'• •',9)
text(369,637,'경보음 정지: 경보정지 → 복귀',5)
( a/'textures/base_ink.png').parent.mkdir(exist_ok=True);ink.save(a/'textures/base_ink.png')
for docked in [False,True]:
 im=Image.new('RGBA',(1042,720));g=ImageDraw.Draw(im)
 for p in parts:
  if p['group'] in ['folded','radio'] or p['group']=='docked' and not docked:continue
  if p['group']=='underside':continue
  x,y,w,h=p['x'],p['y'],p['w'],p['h'];r=p['r'];n=p['name']
  if n=='body_shell':
   # Copy only the unchanged shell shading. Left openings/ink are rebuilt below.
   g.rounded_rectangle((110,89,934,672),12,fill=IV)
   for xx in range(44):
    for left in [True,False]:
     xside=110+xx if left else 934-xx;u=1-xx/44;shade=int(241-17*u*u);g.line((xside,89,xside,672),fill=(shade,shade-2,shade-15,255))
   continue
  if n=='cradle_void':g.rounded_rectangle((160,136,274,616),5,fill='#dedfce',outline='#bbbda8',width=2);continue
  if n=='crt_outer':
   g.rounded_rectangle((548,129,796,345),8,fill='#172223')
   for t in range(18):
    q=t/17;v=int(48-18*q);g.rounded_rectangle((548+18*q,129+25*q,796-18*q,345-17*q),max(2,8-6*q),fill=(v,v+7,v+7,255))
   continue
  g.rounded_rectangle((x,y,x+w,y+h),r,fill=p['color'])
  if n in ['security','away','memory','hold','redial','guard','front','line']:
   for row in range(int(h)):
    dome=max(0,1-(2*(row+.5)/h-1)**2);shade=round(236+7*dome);g.line((x,y+row,x+w,y+row),fill=(shade,shade-2,shade-15,255))
   g.line((x,y+h,x+w,y+h),fill='#b5b9a5',width=1)
  if n in ['test','stop','restore','fire_call','police_call','medical_call','up','down','intercom']:
   g.rounded_rectangle((x,y,x+w,y+h),r,fill='#eeeddf',outline='#b9bdab',width=1)
   g.line((x+r,y+1,x+w-r,y+1),fill='#fffdf0',width=1)
  if n=='wired_handle':
   g.rounded_rectangle((x+2,y+3,x+w+3,y+h+3),r,fill='#acb09c')
   g.rounded_rectangle((x,y,x+w,y+h),r,fill='#e8e7d6',outline='#b4b8a3',width=1)
   for xx in range(1,8):
    q=xx/8;shade=round(220+21*q);g.line((x+xx,y+r,x+xx,y+h-r),fill=(shade,shade-2,shade-15,255));g.line((x+w-xx,y+r,x+w-xx,y+h-r),fill=(shade,shade-2,shade-15,255))
   g.line((x+r,y+2,x+w-r,y+2),fill='#faf8ea',width=2)
  if len(n)==2 and n[0]=='n':g.ellipse((x,y,x+w,y+h),fill='#f0eedf',outline='#b8bca8',width=1)
 im.alpha_composite(ink)
 g=ImageDraw.Draw(im)
 for j in m['shell']['seams']:g.line((j['x0'],j['y0'],j['x1'],j['y1']),fill='#929584',width=2)
 im.save(a/f'textures/base_{"docked" if docked else "empty"}.png')
# Hard guards for the user's immovable right-hand components and central key sizes.
for name in ['mount_plate','body_shell','crt_outer','crt_glass','contrast','brightness','contrast_recess','brightness_recess','hinge_cover']:
 before=next(p for p in old['parts'] if p['name']==name);after=next(p for p in parts if p['name']==name);assert before==after,name
for before in old['parts']:
 if before['name'] in ['emergency','security','away','memory','hold','redial','guard','door','speaker'] and before['group']=='base':
  name={'door':'front','speaker':'line'}.get(before['name'],before['name']);after=next(p for p in parts if p['name']==name and p['group']=='base')
  assert all(before[k]==after[k] for k in ['x','w','h','z','d','r','curve']),name
assert next(k for k in keys if k['name']=='line')['y']==next(k for k in old['keys'] if k['name']=='speaker' and k['group']=='base')['y']
assert not any(p['group']=='base' and p['x']>=458 and p['x']<518 and p['y']>610 and p['name'] not in ['body_shell'] for p in parts)
print('SHT-102C:',len(parts),'solids; wired handset without cable; fixed right-side parts and key dimensions preserved')
