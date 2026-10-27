"""Original periodic 64px rock diffuse textures; standard-library PNG output, no external art."""
from pathlib import Path
import math,struct,zlib,json,hashlib
ROOT=Path(__file__).resolve().parents[2]
def png(path,rows):
 def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
 path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',64,64,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(b'\0'+r for r in rows),9))+chunk(b'IEND',b''))
files=[]
for worn in [False,True]:
 rows=[]
 for y in range(64):
  row=bytearray()
  for x in range(64):
   u=x/64;v=y/64;h=((x*73856093)^(y*19349663))&0xffffffff;h^=h>>13
   grain=h%19-9;strata=7*math.sin(u*math.tau*3+math.sin(v*math.tau*2)*1.8)+4*math.sin(v*math.tau*7+u*math.tau)
   c=103+grain+strata
   if worn:c=c*.90-8*max(0,math.sin(u*math.tau*4+math.sin(v*math.tau)*.6))
   row.extend(max(0,min(255,round(c+d))) for d in [0,3,2])
  rows.append(bytes(row))
 name='beckenburg_rock'+('_worn' if worn else '')+'.png';path=ROOT/'assets/runtime/level-textures'/name;png(path,rows);files.append({'file':name,'variant':'worn' if worn else 'clean','sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
(ROOT/'development/levels/rock-textures.json').write_text(json.dumps({'license':'CC0-1.0','provenance':'Original deterministic periodic strata and grain; no source images','size':[64,64],'files':files},indent=2)+'\n')
print('ORIGINAL_CC0_ROCK',len(files))
