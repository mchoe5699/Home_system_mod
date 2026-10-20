"""Markings placed to match the approved two-view canvas reference."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
root=Path(__file__).resolve().parents[2]
im=Image.new('RGBA',(778,1116),(0,0,0,0));d=ImageDraw.Draw(im)
font='/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc';bold='/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc'
def text(x,y,s,size=8,c=(48,49,52,235),strong=False):d.text(((x-208)*2,(y-28)*2),s,font=ImageFont.truetype(bold if strong else font,size*2),fill=c)
text(225,263,'EZon',15,strong=True);text(277,271,'SHT-305V',7)
text(225,558,'SAMSUNG',11,strong=True)
text(527,45,'전원',7,(174,180,180,220));text(565,45,'사용중',7,(174,180,180,220))
for y,s,c in [(117,'가스',(221,195,97,240)),(163,'방범',(106,177,176,240)),(209,'비상',(204,115,87,240))]:
 d.rectangle(((513-208)*2,(y-28)*2,(528-208)*2,(y+14-28)*2),outline=c,width=2);text(547,y-1,s,10,(187,192,189,238))
for y,s in zip((314,368,422),('경비실','외출','현관')):text(447,y-18,s,7)
text(438,473,'문열림',8)
for y,s in zip((296,332,368,404,440),('비상','시험','가스','방범설정','복귀')):text(550,y-18,s,7)
text(541,473,'경보정지',7)
out=root/'src/sht/resources/assets/homenet_sht101c/sht305v_canvas/ink.png';im.save(out)
