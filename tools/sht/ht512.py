"""HT-512 photo 1 rectification: 464 x 610, warm champagne gold, no call behavior."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import numpy as np,json,zipfile
R=Path(__file__).resolve().parents[2];A=R/'src/sht/resources/assets/homenet_sht101c';T=A/'ht512';T.mkdir(parents=True,exist_ok=True)
W,H,S=464,610,3
rng=np.random.default_rng(512)
y,x=np.mgrid[:H*S,:W*S];n=rng.normal(0,2.3,(H*S,W*S));base=np.zeros((H*S,W*S,3))
for i,c in enumerate((184,179,148)):base[:,:,i]=c+n+(x/(W*S)-.5)*3
im=Image.fromarray(np.uint8(np.clip(base,0,255)),'RGB').convert('RGBA');d=ImageDraw.Draw(im)
ink=Image.new('RGBA',im.size);k=ImageDraw.Draw(ink)
fontfile='/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
def font(s,b=False):return ImageFont.truetype(fontfile.replace('Regular','Bold') if b else fontfile,round(s*S),index=1)
def rect(box,c,draw=d):draw.rectangle(tuple(round(v*S) for v in box),fill=c)
def line(pts,c,w=1,draw=d):draw.line([(round(x*S),round(y*S)) for x,y in pts],fill=c,width=max(1,round(w*S)))
def text(x,y,t,s=7,c='#343a31',b=False,draw=d):draw.text((round(x*S),round(y*S)),t,font=font(s,b),fill=c)
def rr(box,c,r=2,draw=d):draw.rounded_rectangle(tuple(round(v*S) for v in box),round(r*S),fill=c)
# Mitered trim is sculpted separately in the world; the atlas is also the orthographic model view.
for pts,c in [([(0,0),(W,0),(W-17,17),(17,17)],'#c4c2a7'), ([(0,0),(17,17),(17,H-17),(0,H)],'#bebca2'), ([(W,0),(W,H),(W-17,H-17),(W-17,17)],'#aaa78e'), ([(0,H),(17,H-17),(W-17,H-17),(W,H)],'#aaa78c')]:d.polygon([(a*S,b*S) for a,b in pts],fill=c)
for xx in [17,W-17]:line([(xx,17),(xx,H-17)],'#5e6051',.8)
for yy in [17,H-17]:line([(17,yy),(W-17,yy)],'#5e6051',.8)
# Printed roof emblem and identification.
line([(42,55),(51,46),(61,55)],'#2f382d',2);line([(47,52),(47,58),(55,58),(55,52)],'#2f382d',1.5)
text(65,43,'HT-512',7.2);text(65,52,'무인경비관리비디오폰',4.3)
text(142,105,'통화방법',11)
text(207,107,'■ 동(라인) 세대의 수신 번호를 누르고',6.4)
text(217,120,'호출  버튼을 누르십시오',6.4)
text(207,142,'■ 경비실과 통화하려면  경비실  버튼을 누르고',6.2)
text(217,155,'호출  버튼을 누르십시오',6.4)
text(207,177,'■ 버튼을 잘못 누른 경우에는  #  취소 버튼을',6.2)
text(217,190,'누르고 처음부터 다시 하십시오.',6.4)
for box in [(216,121,231,130), (207+font(6.2).getlength('■ 경비실과 통화하려면  ')/S-1,143,207+font(6.2).getlength('■ 경비실과 통화하려면  경비실')/S+1,152), (207+font(6.2).getlength('■ 버튼을 잘못 누른 경우에는  ')/S-1,178,207+font(6.2).getlength('■ 버튼을 잘못 누른 경우에는  #')/S+1,187)]:
 d.rectangle(tuple(round(v*S) for v in box),outline='#646751',width=S//2+1)
# Smoked glass, purple camera window and red four-digit LED (static photo appearance).
rect((78,210,402,306),'#101512')
for yy in range(213,304):
 t=(yy-213)/91;rect((81,yy,399,yy+1),tuple(round(a+(b-a)*t) for a,b in zip((72,79,71),(46,53,48)))+(255,))
line([(82,213),(398,213)],'#959c8c',.6);line([(82,214),(115,300)],'#667065',.35)
rr((146,227,190,250),'#292234',1);rect((150,230,187,247),'#282137')
d.ellipse((157*S,255*S,180*S,278*S),fill='#30392f',outline='#777c67',width=S)
d.ellipse((162*S,260*S,175*S,273*S),fill='#172626',outline='#4d594c',width=S)
d.ellipse((165*S,263*S,173*S,271*S),fill='#263638');d.ellipse((166*S,264*S,169*S,267*S),fill='#738075')
text(287,229,'통화중',6.3,'#e0e2d2',True);text(336,229,'문열림',6.3,'#e0e2d2',True)
rect((298,244,303,249),'#462025');rect((347,244,352,249),'#422728')
rect((278,260,357,287),'#451d2b')
# Seven segments with a dark housing and individual bevel-like core strokes.
for xx in [281,300,319,338]:
 for pts in [[(xx+4,263),(xx+15,263)],[(xx+3,274),(xx+14,274)],[(xx+2,284),(xx+13,284)],[(xx+3,264),(xx+2,272)],[(xx+16,264),(xx+15,272)],[(xx+2,276),(xx+1,282)],[(xx+15,276),(xx+14,282)]]:
  line(pts,'#8e2344',3.5);line(pts,'#d82e58',1.4)
text(361,274,'호',7,'#d8dbcf')
# Exactly 9 x 11 round perforations, cut into the 3D front panel too.
for row in range(11):
 for col in range(9):
  xx=112.1+col*10.2;yy=335.1+row*10.2;d.ellipse(((xx-2.6)*S,(yy-2.6)*S,(xx+2.6)*S,(yy+2.6)*S),fill='#101611')
keys=[]
for row,chars in enumerate(['123','456','789','*0#']):
 for col,ch in enumerate(chars):keys.append(dict(label=ch,x=282+31*col,y=326+31*row,w=28,h=28))
keys += [dict(label='경비실',x=283,y=455,w=41,h=28),dict(label='호출',x=329,y=455,w=41,h=28)]
for q in keys:
 xx,yy,ww,hh=(q[v] for v in ['x','y','w','h']);rr((xx-.75,yy-.75,xx+ww+.75,yy+hh+.75),'#111b1b',3);rr((xx,yy,xx+ww,yy+hh),'#c7c8ba',3);rr((xx+2,yy+2,xx+ww-2,yy+hh-2),'#576b82',2)
 sz=21 if len(q['label'])==1 else 9.5;f=font(sz);bb=d.textbbox((0,0),q['label'],font=f);tx=xx+(ww-(bb[2]-bb[0])/S)/2;ty=yy+(hh-(bb[3]-bb[1])/S)/2-bb[1]/S
 for dd in [d,k]:text(tx,ty,q['label'],sz,'#f0f1e7',draw=dd)
text(379,426,'취소',5.8)
# Card insertion pocket and arrow.
rect((112,462,194,538),'#2f352a');rect((113.5,463.5,192.5,536.5),'#b9b497')
rect((124,495,182,500),'#34392c');line([(124,500),(182,500)],'#d8d2b2',1)
line([(135,529),(153,516),(171,529),(161,529)],'#272f27',1.8);line([(145,529),(135,529)],'#272f27',1.8)
text(140,529,'CARD',7,'#242d24',True)
text(211,518,'■ 통화중 램프가 켜져 있으면 경비실은 통화중 입니다.',6.2)
text(211,531,'■ 문 열림 램프가 켜지면 들어가십시오.',6.2)
text(345,561,'SAMSUNG',18.5,'#202921',True)
im.save(T/'front.png');ink.save(T/'key_ink.png')
# Match the actual KLP-203 baked outer bounds, not its approximate click box.
with zipfile.ZipFile(R/'.device-baseline/base.jar') as z:
 e=json.loads(z.read('assets/qwertys_homenet/models/block/klp_203.json'))['elements']
 h=(max(q['to'][1] for q in e)-min(q['from'][1] for q in e))/16
spec=dict(width=W,height=H,blockHeight=h,blockWidth=h*W/H,depthPixels=20,frame=17,faceZ=15,speaker=dict(x=107,y=330,cols=9,rows=11,pitch=10.2,radius=2.6),keys=keys,mode='BUS',modelOnly=True)
(T/'model.json').write_text(json.dumps(spec,ensure_ascii=False,indent=2))
for sub,obj in [('blockstates/ht_512.json',{'variants':{'':{'model':'minecraft:block/air'}}}),('models/item/ht_512.json',{'parent':'builtin/entity'}),('loot_tables/blocks/ht_512.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'homenet_sht101c:ht_512'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})]:
 # Loot tables live in data, not assets.
 p=(R/'src/sht/resources/data/homenet_sht101c'/sub if sub.startswith('loot_tables/') else A/sub);p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj))
for lang,name in [('ko_kr','삼성 HT-512 디지털 로비폰 (BUS)'),('en_us','Samsung HT-512 Digital Lobby Phone (BUS)')]:
 p=A/'lang'/f'{lang}.json';o=json.loads(p.read_text());o['block.homenet_sht101c.ht_512']=name;p.write_text(json.dumps(o,ensure_ascii=False))
print('HT-512',spec['blockWidth'],'x',h,'blocks,',len(keys),'keys, 99 actual perforations')
