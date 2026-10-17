"""Resolve custom interface method references in the remapped shipping archive."""
import struct,zipfile,sys

def parse(blob):
 off=8
 def u2():
  nonlocal off
  n=struct.unpack_from('>H',blob,off)[0];off+=2;return n
 def attrs():
  nonlocal off
  for _ in range(u2()):off+=2;sz=struct.unpack_from('>I',blob,off)[0];off+=4+sz
 pool=[None]*u2();i=1
 while i<len(pool):
  tag=blob[off];off+=1
  if tag==1:
   n=u2();pool[i]=(tag,blob[off:off+n].decode('utf8',errors='replace'));off+=n
  elif tag in [7,8,16,19,20]:pool[i]=(tag,u2())
  elif tag in [9,10,11,12,17,18]:pool[i]=(tag,u2(),u2())
  elif tag in [3,4]:off+=4
  elif tag in [5,6]:off+=8;i+=1
  elif tag==15:off+=3
  else:raise AssertionError(tag)
  i+=1
 def cls(n):return pool[pool[n][1]][1] if n else ''
 u2();u2();superclass=cls(u2());parents=[superclass]+[cls(u2()) for _ in range(u2())]
 for _ in range(u2()):off+=6;attrs()
 methods=set()
 for _ in range(u2()):u2();n=u2();d=u2();methods.add((pool[n][1],pool[d][1]));attrs()
 refs=[]
 for t in pool:
  if t and t[0]==11:
   nt=pool[t[2]];refs.append((cls(t[1]),pool[nt[1]][1],pool[nt[2]][1]))
 return parents,methods,refs
with zipfile.ZipFile(sys.argv[1]) as jar:
 classes={n[:-6]:parse(jar.read(n)) for n in jar.namelist() if n.endswith('.class')}
def exists(owner,name,desc,seen=None):
 seen=set() if seen is None else seen
 if owner in seen or owner not in classes:return False
 seen.add(owner);parents,methods,_=classes[owner]
 return (name,desc) in methods or any(exists(p,name,desc,seen) for p in parents)
count=0
for owner,(_,_,refs) in classes.items():
 if not owner.startswith('com/qwerty/homenet/sht/'):continue
 for target,name,desc in refs:
  if target.startswith('com/qwerty/homenet/'):
   assert exists(target,name,desc),(owner,target,name,desc)
   count+=1
print('Resolved',count,'shipping interface references')
