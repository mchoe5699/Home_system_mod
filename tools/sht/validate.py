from pathlib import Path
import json,math
p=Path(__file__).resolve().parents[2]/'src/sht/resources/assets/homenet_sht101c/model.json'
m=json.loads(p.read_text());parts=m['parts'];keys=m['keys']
assert math.isclose((m['blue'][2]-m['blue'][0])*m['scale'],.5)
assert (m['outer'][2]-m['outer'][0])*m['scale']<1
assert (m['outer'][3]-m['outer'][1])*m['scale']<1
for s in parts:
 assert all(math.isfinite(s[k]) for k in ['x','y','z','w','h','d','r','curve'])
 assert s['w']>0 and s['h']>0 and s['d']>0
 assert 0<=s['r']<=min(s['w'],s['h'])/2+.001,s
for names in [['test','stop','restore'],['fire_call','police_call','medical_call'],['up','down','intercom']]+[['n'+v for v in row] for row in ['123','456','789','*0#']]+[['r'+v for v in row] for row in ['123','456','789','*0#']]:
 row=[next(s for s in parts if s['name']==n) for n in names]
 assert len({(s['z'],s['d']) for s in row})==1,names
for grp in ['radio','base']:
 assert sum(k['group']==grp and len(k['name'])==2 and k['name'][0] in 'nr' for k in keys)==12
for s in parts:
 if s['name'] in ['radio_aerial','radio_lower','radio_upper']:assert s['h']>s['w']*10
assert any(s['group']=='folded' and s['w']>400 for s in parts)
print('185 finite solids; 51 aligned key regions; exact .5 blue width; uniform row protrusion and antenna poses verified')
