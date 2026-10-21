"""One original ready-to-use model per invocation; native pivots, packed textures and source.
Usage: blender --background --python ... -- bandsaw
"""
import bpy,sys,math,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'development/assets'),str(ROOT/'development/levels')]
import rebuild_support as s
import build_lived_assets as b

def bandsaw():
    m=b.init();p=[s.box('Anchored cast machine plinth',(0,0,.12),(1.65,1.2,.24),m['iron'],.035)]
    for x in [-.60,.60]:
        p.append(s.box('Bolted base foot',(x,0,.28),(.23,.90,.16),m['rust']))
        for y in [-.38,.38]:p.append(s.rod('Foundation anchor bolt',(x,y,.34),(x,y,.41),.035,m['steel'],6))
    p.append(s.box('Main spine',(-.57,.14,1.72),(.25,.36,2.90),m['green'],.025))
    for z in [.78,2.70]:
        p.append(s.rod('Cast band wheel',(0,-.14,z),(0,.15,z),.56,m['iron'],20))
        p+=b.ring('Wheel tyre',(0,-.15,z),.55,.025,m['black'],segments=20)
        for i in range(6):
            a=i*math.tau/6;p.append(s.rod('Wheel spoke',(0,-.17,z),(.46*math.cos(a),-.17,z+.46*math.sin(a)),.026,m['green'],6))
        p.append(s.rod('Wheel axle cap',(0,-.17,z),(0,-.23,z),.085,m['brass'],10))
    for x in [-.56,.56]:p.append(s.box('Continuous saw band',(x,-.03,1.74),(.012,.13,1.94),m['steel']))
    # Guards cover the returning blade and all but the controlled cutting gap.
    p.append(s.box('Returning blade guard',(-.60,-.03,1.73),(.16,.23,2.03),m['green'],.015))
    for z,h in [(.77,.75),(2.64,.74)]:p.append(s.box('Cutting blade guard',(.56,-.03,z),(.17,.24,h),m['green'],.012))
    for i in range(22):
        z=1.21+i*.037;p.append(s.mesh('Exposed band tooth',[(.566,-.10,z),(.589,-.10,z+.014),(.566,-.10,z+.028)],[(0,1,2),(2,1,0)],m['steel']))
    p.append(s.box('Heavy slotted feed table',(.58,-.08,1.18),(1.73,1.30,.075),m['steel']))
    p.append(s.box('Blade throat slot',(.56,-.04,1.221),(.029,.33,.003),m['black']))
    p.append(s.box('Adjustable timber fence',(.93,.10,1.30),(.07,.95,.18),m['green']))
    p.append(s.rod('Fence adjustment screw',(.78,-.49,1.26),(1.13,-.49,1.26),.017,m['brass'],7))
    p+=b.ring('Tension hand wheel',(-.62,.15,3.13),.15,.018,m['rust'],axis='Z',segments=12)
    p.append(s.rod('Tension threaded shaft',(-.62,.15,2.95),(-.62,.15,3.19),.018,m['steel'],6))
    p.append(s.rod('Idle drive motor',(-.54,.44,.60),(.02,.44,.60),.20,m['green'],12))
    p.append(s.box('Stopped belt guard',(-.14,.47,.85),(.57,.13,.74),m['iron'],.02))
    p+=s.tube('Disconnected electrical lead',[(-.5,.34,.48),(-.86,.42,.16),(-.98,.2,.02),(-.87,-.06,.02)],.014,m['black'],6)
    p.append(s.box('Unplugged isolator plug',(-.86,-.10,.04),(.065,.10,.065),m['iron']))
    return p,dict(function='Static decommissioned mill bandsaw; guarded return and supported feed table',runtime='Ready static environment mesh; no powered cutting or animation',placement='Blackpine sawmill, outside ritual and stair routes')

name=sys.argv[sys.argv.index('--')+1] if '--' in sys.argv else 'bandsaw'
parts,notes=globals()[name]()
b.export('level_work_'+name,parts,'working-place-details',view=(4,-6,3.5))
entry=b.LOG[0];entry.update(notes)
folder=ROOT/'development/levels/production';folder.mkdir(exist_ok=True)
(folder/(name+'.json')).write_text(json.dumps(entry,indent=2)+'\n')
print('PRODUCTION_ASSET_COMPLETE',name,entry['triangles'],flush=True)
