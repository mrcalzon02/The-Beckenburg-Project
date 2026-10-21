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

def log_carriage():
    m=b.init();p=[]
    for x in [-.63,.63]:
        p.append(s.box('Mill carriage rail',(x,0,.055),(.07,3.8,.11),m['steel']))
        for y in [-1.45,1.45]:
            p.append(s.rod('Flanged carriage wheel',(x-.07,y,.21),(x+.07,y,.21),.17,m['iron'],12))
            p.append(s.rod('Outer wheel flange',(x-.08,y,.21),(x-.06,y,.21),.19,m['rust'],12))
    for y in [-1.4,0,1.4]:
        p.append(s.box('Oak cross bearer',(0,y,.42),(1.62,.24,.20),m['oak']))
        for x in [-.63,.63]:p.append(s.rod('Bearer fixing bolt',(x,y,.51),(x,y,.55),.025,m['iron'],6))
    for x in [-.63,.63]:p.append(s.box('Rolling underframe',(x,0,.32),(.12,3.2,.16),m['iron']))
    p.append(s.rod('Partly milled log',(0,-1.65,.81),(0,1.65,.81),.31,m['oak'],12))
    p.append(s.box('First flat sawn face',(0,0,1.055),(.38,3.27,.035),m['pine']))
    for y in [-1.654,1.654]:
        p.append(s.rod('Pale cut end',(0,y,.81),(0,y+.003,.81),.285,m['pine'],12))
        for r in [.08,.16,.24]:
            # Growth rings lie in the X/Z end plane.
            p+=b.ring('Visible growth ring',(0,y-.003,.81),r,.0025,m['oak'],segments=12)
    for y in [-1.15,1.15]:
        p.append(s.box('Log dog guide',(.54,y,.63),(.16,.19,.56),m['iron']))
        p+=s.tube('Clamping dog',[(.54,y,.89),(.31,y,1.0),(.17,y,.96)],.027,m['rust'],6)
        p.append(s.rod('Clamp screw',(.55,y,.47),(.77,y,.47),.019,m['brass'],6))
        p.append(s.rod('Screw handle',(.78,y-.10,.47),(.78,y+.10,.47),.012,m['iron'],6))
    for y in [-1.75,1.75]:p.append(s.box('Rail end stop',(0,y,.13),(1.48,.11,.15),m['oak']))
    return p,dict(function='Stopped rail log carriage with growth rings, sawn face and clamping dogs',runtime='Ready static environment mesh; wheels and log are not simulated',placement='Blackpine mill feed route, aligned toward bandsaw and clear of ritual')

name=sys.argv[sys.argv.index('--')+1] if '--' in sys.argv else 'bandsaw'
parts,notes=globals()[name]()
b.export('level_work_'+name,parts,'working-place-details',view=(4,-6,3.5))
entry=b.LOG[0];entry.update(notes)
folder=ROOT/'development/levels/production';folder.mkdir(exist_ok=True)
(folder/(name+'.json')).write_text(json.dumps(entry,indent=2)+'\n')
print('PRODUCTION_ASSET_COMPLETE',name,entry['triangles'],flush=True)
