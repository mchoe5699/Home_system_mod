"""Print markings only; physical depth comes from the mesh, not this texture."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
root=Path(__file__).resolve().parents[2]
im=Image.new('RGBA',(768,1110),(0,0,0,0)); d=ImageDraw.Draw(im)
font='/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
bold='/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc'
def label(x,y,s,size=10,color=(47,50,56,230),heavy=False):
 d.text(((x-223)*2,(y-26)*2),s,font=ImageFont.truetype(bold if heavy else font,size*2),fill=color)
label(245,288,'SAMSUNG',11,heavy=True)
label(245,548,'SHT-305V  VIDEO DOOR PHONE',8)
for y,name in zip((345,399,453),('경비실','외출','현관')):label(469,y-17,name,8)
label(466,504,'문열림',8)
for y,name in zip((343,379,415,451,487),('비상','시험','가스','방범설정','복귀')):label(567,y-17,name,7)
label(545,502,'경보정지',8)
for y,name in zip((94,131,168,205),('화재','가스','방범','비상')):label(554,y,name,9,(179,186,176,220))
label(534,47,'전원',7,(180,187,185,190));label(572,47,'사용중',7,(180,187,185,190))
path=root/'src/sht/resources/assets/homenet_sht101c/sht305v/ink.png';im.save(path)
