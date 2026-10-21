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

def repair_bench():
    m=b.init();p=[]
    for x in [-1.05,1.05]:
        for y in [-.29,.29]:p.append(s.box('Braced bench leg',(x,y,.43),(.11,.11,.86),m['oak']))
        p.append(s.box('Lower end bearer',(x,0,.22),(.12,.68,.12),m['oak']))
    for y in [-.29,.29]:p.append(s.box('Long bench stretcher',(0,y,.25),(2.1,.075,.12),m['oak']))
    for i in range(5):p.append(s.box('Worn timber bench plank',(0,-.30+i*.15,.88),(2.45,.144,.085),m['pine']))
    p.append(s.box('Bench backboard',(0,.36,1.28),(2.43,.07,.73),m['oak']))
    for x in [-.9,-.45,0,.45,.9]:p.append(s.rod('Tool hook',(x,.30,1.43),(x,.22,1.43),.012,m['iron'],6))
    # Different readable silhouettes: spanner, claw hammer, hand saw, screwdriver and plane.
    p.append(s.rod('Hanging spanner shank',(-.90,.22,1.40),(-.90,.22,1.10),.022,m['steel'],6))
    for z in [1.40,1.10]:
        for x in [-.94,-.86]:p.append(s.box('Open spanner jaw',(x,.22,z),(.024,.045,.08),m['steel']))
    p.append(s.rod('Hammer wooden handle',(-.45,.22,1.35),(-.45,.22,1.03),.023,m['pine'],6))
    p.append(s.box('Hammer head',(-.45,.22,1.36),(.22,.06,.065),m['iron']))
    p.append(s.mesh('Hanging hand saw',[(.03,.22,1.40),(.18,.22,1.11),(.52,.22,1.11),(.31,.22,1.40)],[(0,1,2,3),(3,2,1,0)],m['steel']))
    for i in range(13):p.append(s.mesh('Saw tooth',[(.18+i*.025,.22,1.11),(.192+i*.025,.22,1.09),(.205+i*.025,.22,1.11)],[(0,1,2),(2,1,0)],m['steel']))
    p.append(s.box('Saw wooden handle',(.08,.22,1.38),(.13,.075,.17),m['oak'],.025))
    p.append(s.rod('Screwdriver shaft',(.9,.22,1.4),(.9,.22,1.09),.009,m['steel'],6))
    p.append(s.rod('Screwdriver grip',(.9,.22,1.23),(.9,.22,1.08),.028,m['red'],8))
    p.append(s.box('Bench vice fixed jaw',(-.80,-.22,1.02),(.25,.23,.18),m['iron'],.015))
    p.append(s.box('Bench vice moving jaw',(-.80,-.46,1.02),(.25,.09,.18),m['iron']))
    p.append(s.rod('Vice lead screw',(-.80,-.23,.96),(-.80,-.58,.96),.019,m['steel'],8))
    p.append(s.rod('Vice sliding handle',(-.94,-.59,.96),(-.66,-.59,.96),.012,m['steel'],6))
    p.append(s.box('Hand plane sole',(.43,-.06,.945),(.15,.34,.045),m['iron']))
    p.append(s.box('Plane wooden tote',(.43,.02,1.01),(.065,.08,.12),m['oak'],.02))
    p.append(s.box('Sharpening stone',(.10,-.25,.96),(.22,.11,.045),m['iron']))
    p.append(s.lathe('Uncapped oil tin',[(0,.06),(.20,.06),(.23,.03)],m['rust'],10,(.94,-.18,.925)))
    p.append(s.box('Saved offcut',(.42,.1,.18),(.85,.35,.14),m['pine']))
    return p,dict(function='Repair bench, vice, sharpening stone, oil and five hand-tool silhouettes',runtime='Ready static workbench kit; tools are scenery, not inventory pickups',placement='Blackpine repair shed, against the north wall')

def crew_footlocker():
    m=b.init();p=[]
    p.append(s.box('Trunk bottom',(0,0,.055),(.86,.47,.07),m['oak']))
    for y in [-.235,.235]:p.append(s.box('Trunk long side',(0,y,.23),(.86,.035,.34),m['green']))
    for x in [-.43,.43]:p.append(s.box('Trunk end',(x,0,.23),(.035,.47,.34),m['green']))
    for x in [-.32,.32]:
        for y in [-.255,.255]:p.append(s.box('Rusted trunk band',(x,y,.23),(.045,.018,.34),m['iron']))
    for x in [-.34,.34]:
        p.append(s.rod('Lid hinge',(x-.07,.24,.40),(x+.07,.24,.40),.018,m['brass'],8))
    # Lid tilted back: actual geometry, hinge at rear upper edge.
    lid=s.box('Open slatted lid',(0,.39,.60),(.89,.045,.51),m['green']);lid.rotation_euler[0]=-.37;p.append(lid)
    p.append(s.box('Tarnished name card holder',(0,-.259,.27),(.22,.014,.09),m['brass']))
    p.append(s.box('Faded crew card',(0,-.269,.27),(.18,.005,.06),m['paper']))
    p.append(s.box('Broken clasp',(0,-.276,.35),(.055,.018,.09),m['iron']))
    for x in [-.45,.45]:p+=s.tube('End carrying handle',[(x,-.07,.29),(x*1.08,-.07,.23),(x*1.08,.07,.23),(x,.07,.29)],.012,m['leather'],6)
    for i,key in enumerate(['cloth','blue','cloth']):p.append(s.box('Folded shirt inside',(-.18,.01,.13+i*.055),(.34,.32,.052),m[key],.012))
    p.append(s.box('Personal envelope',(.17,.0,.27),(.22,.16,.005),m['paper']))
    p.append(s.box('Small saved photograph',(.17,.0,.277),(.12,.10,.004),m['enamel']))
    for x in [.14,.19]:p.append(s.box('Photo silhouette',(x,0,.280),(.025,.045,.002),m['iron']))
    p.append(s.lathe('Pocket thermos',[(0,.037),(.23,.037),(.25,.030)],m['steel'],10,(.30,.05,.10)))
    for i in range(4):p.append(s.rod('Spare boot lace',(-.02+i*.018,-.15,.30),(.03+i*.018,-.11,.30),.0025,m['black'],4))
    p.append(s.box('Worn mending packet',(.12,.12,.23),(.14,.09,.045),m['leather']))
    return p,dict(function='Open crew footlocker with shirts, personal card/photo, thermos, laces and mending packet',runtime='Ready static personal-effects kit; lid and belongings are not interactive',placement='Blackpine bunkhouse, beside surviving beds and away from door approaches')

def hearth_tools():
    m=b.init();p=[s.box('Freestanding fire tool rack base',(.18,.04,.035),(.36,.31,.07),m['iron'])]
    p.append(s.rod('Rack upright',(.18,.11,.06),(.18,.11,1.05),.025,m['iron'],8))
    p.append(s.rod('Hanging tool rail',(-.02,.11,1.04),(.38,.11,1.04),.016,m['iron'],6))
    for x in [0,.16,.32]:
        p+=b.ring('Hanging handle loop',(x,.03,.97),.036,.008,m['iron'],segments=8)
        p.append(s.rod('Fire tool shaft',(x,.03,.94),(x,.03,.25),.009,m['iron'],6))
    p.append(s.mesh('Ash shovel pan',[(-.07,.02,.12),(.07,.02,.12),(.055,.03,.30),(-.055,.03,.30)],[(0,1,2,3),(3,2,1,0)],m['iron']))
    p.append(s.rod('Poker bent tip',(.16,.03,.25),(.22,.03,.20),.01,m['iron'],6))
    p.append(s.rod('Tongs second arm',(.32,.03,.78),(.36,-.015,.20),.009,m['iron'],6))
    p.append(s.lathe('Lidded cold ash pail',[(0,.12),(.24,.16),(.27,.16),(.28,.12)],m['iron'],12,(-.22,-.05,0)))
    p.append(s.rod('Pail lid',(-.22,-.05,.275),(-.22,-.05,.285),.16,m['iron'],12))
    p+=s.tube('Lid handle',[(-.26,-.05,.285),(-.26,-.05,.32),(-.18,-.05,.32),(-.18,-.05,.285)],.008,m['steel'],6)
    p+=b.ring('Pail wire handle',(-.22,-.05,.26),.16,.006,m['steel'],segments=10)
    for i in range(5):p.append(s.rod('Dry split kindling',(-.35+i*.045,.20,.04),(-.35+i*.045,.40,.05+i*.006),.021,m['pine'],5))
    return p,dict(function='Cold ash pail, freestanding poker/shovel/tongs rack and split kindling',runtime='Ready static hearth detail; no live heat, ash or fire simulation',placement='Blackpine foreman and mess hearths, supported clear of walking aisles')

def boot_rack():
    m=b.init();p=[]
    for x in [-.62,.62]:
        for y in [-.18,.18]:p.append(s.box('Washhouse rack leg',(x,y,.53),(.07,.07,1.06),m['oak']))
    for z in [.10,.39]:
        for i in range(5):p.append(s.box('Open drying slat',(0,-.20+i*.10,z),(1.40,.065,.045),m['pine']))
    p.append(s.box('Rack upper back rail',(0,.18,1.11),(1.40,.075,.12),m['oak']))
    p.append(s.rod('Hanging towel rail',(-.60,-.08,1.03),(.60,-.08,1.03),.019,m['steel'],8))
    for x in [-.41,.11]:
        p.append(s.mesh('Draped old wash towel',[(x,-.12,1.02),(x+.30,-.12,1.02),(x+.30,-.14,.56),(x+.23,-.15,.53),(x+.08,-.14,.55),(x,-.14,.54)],[(0,1,2,3,4,5),(5,4,3,2,1,0)],m['cloth' if x<0 else 'blue']))
    # Reuse the project's original CC0 boots, retaining their UV/materials.
    boots=s.import_source('level_story_work_boots_worn',location=(-.38,-.01,.125),scale=.82);p.append(boots)
    for i in range(3):p.append(s.box('Folded wash cloth',(.40,0,.15+i*.045),(.30,.28,.043),m['cloth'],.01))
    return p,dict(function='Open boot-drying rack, old work boots, folded cloths and two draped towels',runtime='Ready static washhouse furnishing; boots retain original source UV/materials',derived_sources=s.SOURCE_LOG,placement='Blackpine washhouse east wall, outside the clear central rite')

name=sys.argv[sys.argv.index('--')+1] if '--' in sys.argv else 'bandsaw'
parts,notes=globals()[name]()
b.export('level_work_'+name,parts,'working-place-details',view=(4,-6,3.5))
entry=b.LOG[0];entry.update(notes)
folder=ROOT/'development/levels/production';folder.mkdir(exist_ok=True)
(folder/(name+'.json')).write_text(json.dumps(entry,indent=2)+'\n')
bpy.ops.wm.open_mainfile(filepath=str(ROOT/entry['editable']))
obj=bpy.data.objects[entry['id']];obj.data.calc_loop_triangles()
assert len(obj.data.loop_triangles)==entry['triangles'] and obj.data.uv_layers
assert all(image.packed_file for image in bpy.data.images if image.source=='FILE')
print('PRODUCTION_ASSET_COMPLETE',name,entry['triangles'],flush=True)
