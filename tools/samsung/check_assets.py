#!/usr/bin/env python3
import json,math
from pathlib import Path
r=Path(__file__).resolve().parents[2]/'src/samsung/resources/assets/qwertys_homenet_samsung'
g=json.loads((r/'geometry.json').read_text());s=g['scale'];parts=g['parts'];keys=g['keys']
assert math.isclose(g['blue_width']*s,.5,abs_tol=1e-12)
assert 1000*s<1 and 740*s<1
for p in parts:
 assert all(math.isfinite(p[k]) for k in ['x','y','w','h','z','d','r','bevel'])
 assert p['w']>0 and p['h']>0 and p['d']>0
 assert 0<=p['r']<=min(p['w'],p['h'])/2+.001,(p['name'],p['r'])
 if p['group']=='body':assert 0<=p['x'] and p['x']+p['w']<=1000 and p['y']>=0 and p['y']+p['h']<=740,p['name']
assert len([k for k in keys if k['name'].startswith('digit_')])==12
for k in keys:
 assert any(p['name']==k['name'] and math.isclose(p['x']+p['w']/2,k['x']) and math.isclose(p['y']+p['h']/2,k['y']) for p in parts)
for n in ['front','gui_docked','gui_empty','handset','handset_labels','white']:assert (r/f'textures/{n}.png').is_file()
assert any(p['group']=='upright' and p['y']<0 for p in parts)
assert any(p['group']=='folded' for p in parts)
print(f'{len(parts)} rounded parts / {len(keys)} keys: exact .5 blue width, outer {1000*s:.6f} × {740*s:.6f}; finite geometry, matched hit regions, antenna states verified')
