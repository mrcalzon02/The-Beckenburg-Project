"""Original CC0 low-poly shrub with a real crouched central refuge; metres, floor pivot."""
import bpy,sys,math,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'development/assets'),str(ROOT/'development/levels')]
import rebuild_support as s
import build_lived_assets as b
m=b.init();parts=[]
for i in range(8):
    angle=i*math.tau/8
    x,y=math.cos(angle)*.75,math.sin(angle)*.75
    parts+=s.tube('Woody shrub stem',[(x,y,0),(x*.94,y*.94,.48),(x*1.1,y*1.1,.88)],.025,m['oak'],5)
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1,radius=1,location=(x,y,.80+(i%2)*.03))
    leaf=bpy.context.object;leaf.name='Dense low-poly broadleaf cluster';leaf.scale=(.45,.45,.62)
    leaf.rotation_euler.z=angle;leaf.data.materials.append(m['green']);parts.append(leaf)
    # Small leaf fans give an organic silhouette instead of a single solid ball.
    for j in range(3):
        a=angle+j*.7
        p=(x+math.cos(a)*.28,y+math.sin(a)*.28,1.03+j*.10)
        parts.append(s.mesh('Original leaf fan',[p,(p[0]+.16,p[1]+.04,p[2]+.08),(p[0]+.23,p[1]+.08,p[2]),(p[0]+.08,p[1]+.12,p[2]-.025)],[(0,1,2,3)],m['green']))
b.export('level_shrub_refuge',parts,'enclosures',view=(3,-4,2.3))
(ROOT/'development/levels/outdoor-refuge-assets.json').write_text(json.dumps({'license':'CC0-1.0','central_clear_radius_m':.36,'concealment_requires':'crouched, stationary, available authored socket; nearby ghost search reveals','assets':b.LOG},indent=2)+'\n')
