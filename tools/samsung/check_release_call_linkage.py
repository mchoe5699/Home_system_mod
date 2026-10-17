#!/usr/bin/env python3
"""Check remapped interface references in the actual release, independently of dev tests."""
import struct,sys,zipfile

def read_class(data):
 p=8
 def u2():
  nonlocal p
  n=struct.unpack_from('>H',data,p)[0];p+=2;return n
 def skip_attrs():
  nonlocal p
  for _ in range(u2()):
   p+=2;n=struct.unpack_from('>I',data,p)[0];p+=4+n
 count=u2();cp=[None]*count;i=1
 while i<count:
  tag=data[p];p+=1
  if tag==1:
   n=u2();cp[i]=(tag,data[p:p+n].decode('utf8',errors='replace'));p+=n
  elif tag in (7,8,16,19,20):cp[i]=(tag,u2())
  elif tag in (9,10,11,12,17,18):cp[i]=(tag,u2(),u2())
  elif tag in (3,4):p+=4
  elif tag in (5,6):p+=8;i+=1
  elif tag==15:p+=3
  else:raise AssertionError(('constant tag',tag))
  i+=1
 p+=6
 for _ in range(u2()):p+=2
 for _ in range(u2()):p+=6;skip_attrs()
 methods=set()
 for _ in range(u2()):
  p+=2;name=u2();desc=u2();methods.add((cp[name][1],cp[desc][1]));skip_attrs()
 refs=[]
 for entry in cp:
  if entry and entry[0]==11:
   owner=cp[cp[entry[1]][1]][1];nt=cp[entry[2]];refs.append((owner,cp[nt[1]][1],cp[nt[2]][1]))
 return methods,refs

with zipfile.ZipFile(sys.argv[1]) as jar:
 owner='com/qwerty/homenet/intercom/IntercomCaller'
 declared,_=read_class(jar.read(owner+'.class'));count=0
 guard='com/qwerty/homenet/guard/GuardExchange'
 guard_methods,_=read_class(jar.read(guard+'.class'))
 contracts={owner:declared,guard:declared|guard_methods}
 for name in jar.namelist():
  if not name.endswith('.class'):continue
  _,refs=read_class(jar.read(name))
  for interface,method,desc in refs:
   if interface in contracts:
    assert (method,desc) in contracts[interface],f'{name}: unresolved {interface}.{method}{desc}'
    count+=1
 assert count>0,'No production interface references checked'
 print(f'Release call linkage: {count} interface references resolved')
