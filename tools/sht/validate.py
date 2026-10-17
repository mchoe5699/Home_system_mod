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
# A solid panel in front of the glass must never intersect the active CRT rectangle.
glass=next(s for s in parts if s['name']=='crt_glass')
assert glass['w']/glass['h']==4/3
for s in parts:
 if s['group']!='base' or s['name'] in ['body_shell','crt_outer','crt_glass','cradle_void']:continue
 overlap=s['x']<glass['x']+glass['w'] and s['x']+s['w']>glass['x'] and s['y']<glass['y']+glass['h'] and s['y']+s['h']>glass['y']
 assert not (overlap and s['z']+s['d']+s['curve']>glass['z']+glass['d']),('CRT covered',s['name'])
assert not any(s['name'] in ['rear_controller','rear_monitor','rear_lower_slot'] for s in parts)
assert all(s['group']=='underside' and s['y']==672 for s in parts if s['name']=='bottom_vent')
print(f"{len(parts)} finite solids / {len(keys)} key regions: exact .5 blue width, uniform key rows, open 4:3 CRT, underside vents, no rear boxes")

import hashlib
handset=[v for v in parts if v['group'] in ['radio','docked']]
assert hashlib.sha256(json.dumps(handset,sort_keys=True).encode()).hexdigest()=='720a4bd3f62e2d0d5f710195e0c278e59a0196f93bfc5cfce12763effe7ea365'
assert hashlib.sha256((p.parent/"textures"/'radio_ink.png').read_bytes()).hexdigest()=='ead751e40e85c27464d70b6726fa152b19a27d4f1430d2a63d3a75101d8acc44'
assert hashlib.sha256((p.parent/"textures"/'radio_gui.png').read_bytes()).hexdigest()=='6eb8ee8a60f48e179fe360735e58a125e0d8f4ce4bf54f7061ce2cda174cc964'
print("Handset shape, antenna and GUI textures match the approved 1.0.11 version")

frame=next(s for s in parts if s['name']=='mount_plate')
assert math.isclose(frame['d']*m['scale'],.0005,abs_tol=1e-12)
mount=m['mounting']
ratio=mount['depth']/(mount['front']-mount['back'])
def mounted(z):return mount['frame']+(z-mount['back'])*ratio
assert frame['z']==0 and math.isclose(mounted(m['shell']['back']),frame['d'])
assert 0<mount['depth']<mount['front']-mount['back']
assert mounted(mount['front'])<mount['front']-40
assert math.isclose(mounted(mount['front'])-frame['d'],24)
assert sum(s['name']=='body_shell' for s in parts)==1
assert not any(s['name'].startswith('face_') or s['name'] in ['mount_lip','dial_panel','dial_bottom_cap','monitor_top_cap','monitor_bottom_cap','side_strip','left_rounded_end','right_rounded_end'] for s in parts)
assert m['shell']['bounds']==[110,89,934,672]
assert [548,129,248,216] in m['shell']['holes'] and [170,136,116,480] in m['shell']['holes']
print('Outer trim exactly 0.5 mm; body is one shell reaching the sheet, with no separate bars/panel boxes')

print(f"Body panel depth {mount['depth']*m['scale']*1000:.3f} mm; front moved {(mount['front']-mounted(mount['front']))*m['scale']*1000:.3f} mm toward wall; handset shape remains rigid")

assert m["shell"]["sideRoll"]==[44,43,10]
print("Photo side-roll bands: left 44/824, right 43/824; monotone elliptical falloff, no raised side rail")

seams=m['shell']['seams']
main=next(j for j in seams if j['name']=='controller_monitor')
assert main['y0']==89 and main['y1']==672
assert {(j['width'],j['depth'],j['color']) for j in seams}=={(2.2,1.2,0x929584)}
assert any(j['name']=='brand_keypad' and j['y0']==230 and j['x0']==298 and j['x1']==456 for j in seams)
print('Photo joint positions preserved; uniform original width, depth and shading restored')
