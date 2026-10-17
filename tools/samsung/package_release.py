#!/usr/bin/env python3
"""Add Samsung classes/resources without replacing any published KOCOM/Hyundai class."""
from pathlib import Path
import zipfile,hashlib,re,sys
root=Path(__file__).resolve().parents[2]
base=root/'.samsung-baseline/homenet-1.0.8-baseline.jar'
addon=root/'build/samsung/libs/homenet-samsung-extension-1.0.9.jar'
out=root/'build/samsung/release/qwertys_homenet-1.0.9.jar'
assert hashlib.sha256(base.read_bytes()).hexdigest()=='f523a392449c967bdca61081f9425ed0cb706f7d2d03096b4964d732f8695593'
out.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(base) as old,zipfile.ZipFile(addon) as new,zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as dest:
 for n in old.namelist():
  if n=='META-INF/mods.toml':
   text=old.read(n).decode().replace('version="1.0.8"','version="1.0.9"')
   ext=new.read(n).decode();ext=ext[ext.index('[[mods]]'):]
   dest.writestr(n,text+'\n'+ext)
  elif n=='META-INF/MANIFEST.MF':dest.writestr(n,old.read(n).replace(b'Implementation-Version: 1.0.8',b'Implementation-Version: 1.0.9'))
  else:dest.writestr(n,old.read(n))
 for n in new.namelist():
  if n.startswith(('com/qwerty/homenet/samsung/','assets/qwertys_homenet_samsung/','data/qwertys_homenet_samsung/')):
   assert n not in old.namelist(),f'Baseline override: {n}'
   assert not any(t in n for t in ['ShtTests','ShtClientCheck','/structures/empty']),n
   dest.writestr(n,new.read(n))
with zipfile.ZipFile(base) as old,zipfile.ZipFile(out) as final:
 classes=[n for n in old.namelist() if n.endswith('.class')]
 assert all(final.read(n)==old.read(n) for n in classes)
 assert b'qwertys_homenet_samsung' in final.read('META-INF/mods.toml')
 print(f'Preserved all {len(classes)} baseline classes byte-for-byte')
 print(out)
 print(hashlib.sha256(out.read_bytes()).hexdigest())
