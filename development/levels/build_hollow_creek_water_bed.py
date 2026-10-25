"""Author an initial optical basin from the actual bank control lines.
Explicit new authored bathymetry, not a survey or player-support replacement.
"""
from pathlib import Path
import math,json,struct,hashlib,shutil
ROOT=Path(__file__).resolve().parents[2];src=ROOT/'development/levels/hollow-creek/amenities.json';j=json.loads(src.read_text());banks=[b for b in j['blockers'] if b['id'].startswith(('Creek bank','Lake shore'))]
def distance(x,z,b):
 a,c=b['a'],b['b'];dx,dz=c[0]-a[0],c[1]-a[1];t=max(0,min(1,((x-a[0])*dx+(z-a[1])*dz)/(dx*dx+dz*dz)));return math.hypot(x-a[0]-t*dx,z-a[1]-t*dz)
x0,z0,step,w,h=-105.,-120.,2.5,101,61
values=[-1.95-min(4.5,.05+.22*min(distance(x0+i*step,z0+k*step,b) for b in banks)) for k in range(h) for i in range(w)]
p=ROOT/'assets/runtime/sites/hollow-creek-water-bed.bin';p.write_bytes(struct.pack('>Idddii',0x57424431,x0,z0,step,w,h)+struct.pack('>'+str(len(values))+'d',*values))
texture=ROOT/'development/levels/textures/hollow-creek/shore-grit-v2.png';shutil.copyfile(texture,ROOT/'assets/runtime/sites/hollow-creek-water-bed.png')
r=dict(kind='new authored optical basin; no player collision or terrain edits',source_bank_controls=src.relative_to(ROOT).as_posix(),bank_control_sha256=hashlib.sha256(src.read_bytes()).hexdigest(),excluded='All dock movement blockers; no artificial shoal around the boat',segments=len(banks),grid=dict(x=x0,z=z0,step=step,width=w,height=h,triangles=2*(w-1)*(h-1)),depth_rule='min(4.5m, 0.05m + 0.22 * distance to actual bank line)',bed_sha256=hashlib.sha256(p.read_bytes()).hexdigest(),texture=texture.relative_to(ROOT).as_posix(),texture_sha256=hashlib.sha256(texture.read_bytes()).hexdigest(),limitations=['Initial basin is authored, not measured. Fine shoals and creek flow tuning remain visual review items.','No swimming or underwater camera handling in this milestone.'])
(ROOT/'development/levels/hollow-creek/water-bed.json').write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8');print('Authored optical bed',len(values),'vertices',r['grid']['triangles'],'triangles')
