from pathlib import Path
import zipfile,hashlib,json
r=Path(__file__).resolve().parents[2]
base=r/'.device-baseline/base.jar';extra=r/'build/sht101c/libs/sht101c.jar';target=r/'build/sht101c/release/qwertys_homenet.jar'
assert hashlib.sha256(base.read_bytes()).hexdigest()=='f523a392449c967bdca61081f9425ed0cb706f7d2d03096b4964d732f8695593'
target.parent.mkdir(parents=True,exist_ok=True)
caption_overrides={f'assets/qwertys_homenet/textures/block/kvm931t/{name}.png' for name in ['face','gui','gui_open']}
with zipfile.ZipFile(base) as b,zipfile.ZipFile(extra) as a,zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as out:
 for path in b.namelist():
  raw=a.read(path) if path in caption_overrides else b.read(path)
  if path=='META-INF/mods.toml':raw=raw.replace(b'version="1.0.8"',b'version="1.0.63"')+b'\n[[mods]]'+a.read(path).split(b'[[mods]]',1)[1]
  if path=='META-INF/MANIFEST.MF':raw=raw.replace(b'Implementation-Version: 1.0.8',b'Implementation-Version: 1.0.63')
  out.writestr(path,raw)
 out.writestr("META-INF/coremods.json",a.read("META-INF/coremods.json"))
 for path in a.namelist():
  if path.startswith(('com/qwerty/homenet/sht/','assets/homenet_sht101c/','data/homenet_sht101c/')):
   assert path not in b.namelist()
   assert 'OperationTests' not in path and 'ClientRun' not in path and 'MeshProbe' not in path and 'PerformanceProbe' not in path and '/structures/empty' not in path
   out.writestr(path,a.read(path))
with zipfile.ZipFile(base) as b,zipfile.ZipFile(target) as out:
 assert 'com/qwerty/homenet/sht/MovementSupport.class' in out.namelist()
 for script in json.loads(out.read('META-INF/coremods.json')).values():assert script in out.namelist()
 old=[n for n in b.namelist() if n.endswith('.class')]
 assert all(b.read(n)==out.read(n) for n in old)
 assert not any('qwertys_homenet_samsung' in n or 'homenet/samsung/' in n for n in out.namelist())
 print(len(old),'baseline classes preserved; previous SHT namespace absent')
print(target);print(hashlib.sha256(target.read_bytes()).hexdigest())
