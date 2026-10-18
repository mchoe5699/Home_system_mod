"""Remove the second printed label from KVM-931T only; preserve the original 현관 glyphs."""
from pathlib import Path
from PIL import Image,ImageDraw
import zipfile
root=Path(__file__).resolve().parents[2]
with zipfile.ZipFile(root/'.device-baseline/base.jar') as z:
 for name in ['face','gui','gui_open']:
  path=f'assets/qwertys_homenet/textures/block/kvm931t/{name}.png'
  im=Image.open(z.open(path)).convert('RGBA');before=im.copy()
  glyph=im.crop((429,172,445,182))
  ImageDraw.Draw(im).rectangle((429,171,444,191),fill=(16,20,19,255))
  im.paste(glyph,(429,176))
  # Only the caption changes. Button geometry, keys and every other model are untouched.
  from PIL import ImageChops
  box=ImageChops.difference(before,im).convert('RGB').getbbox()
  assert box and box[0]>=429 and box[1]>=171 and box[2]<=445 and box[3]<=192,box
  target=root/'src/sht/resources'/path;target.parent.mkdir(parents=True,exist_ok=True);im.save(target)
print('KVM-931T: original 현관 glyphs, removed 로비; three printed atlases only')
