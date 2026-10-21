"""One original ready-to-use model per invocation; native pivots, packed textures and source.
Usage: blender --background --python ... -- bandsaw
"""
import bpy,sys,math,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'development/assets'),str(ROOT/'development/levels')]
import rebuild_support as s
import build_lived_assets as b

def work_materials():
    m=b.init()
    # Painted machinery and trunks are painted metal, not the cloth weave used
    # by garments in the shared story library.
    m['green']=s.material('Faded industrial green paint',(.16,.22,.18),'paint')
    return m

def bandsaw():
    m=work_materials();p=[s.box('Anchored cast machine plinth',(0,0,.12),(1.65,1.2,.24),m['iron'],.035)]
    for x in [-.60,.60]:
        p.append(s.box('Bolted base foot',(x,0,.28),(.23,.90,.16),m['rust']))
        for y in [-.38,.38]:p.append(s.rod('Foundation anchor bolt',(x,y,.34),(x,y,.41),.035,m['steel'],6))
    p.append(s.box('Main spine',(-.57,.14,1.72),(.25,.36,2.90),m['green'],.025))
    for z in [.78,2.70]:
        # Open cast rim and real spokes, rather than spokes painted onto a solid disc.
        vv=[(r*math.cos(i*math.tau/20),y,z+r*math.sin(i*math.tau/20)) for y in [-.14,.15] for r in [.48,.56] for i in range(20)]
        ff=[]
        for i in range(20):
            j=(i+1)%20;ff.extend([(i,j,20+j,20+i),(40+i,60+i,60+j,40+j),(i,40+i,40+j,j),(20+i,20+j,60+j,60+i)])
        p.append(s.mesh('Open cast wheel rim',vv,ff,m['iron']))
        p.append(s.rod('Cast wheel bearing hub',(0,-.17,z),(0,.15,z),.10,m['green'],12))
        p+=b.ring('Wheel tyre',(0,-.15,z),.55,.025,m['black'],segments=20)
        for i in range(6):
            a=i*math.tau/6;p.append(s.rod('Wheel spoke',(0,-.17,z),(.50*math.cos(a),-.17,z+.50*math.sin(a)),.026,m['green'],6))
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
    p.append(s.box('Fence screw bearing lug',(.93,-.49,1.245),(.08,.12,.12),m['green'],.006))
    p+=b.ring('Tension hand wheel',(-.62,.15,3.13),.15,.018,m['rust'],axis='Z',segments=12)
    p.append(s.rod('Tension threaded shaft',(-.62,.15,2.95),(-.62,.15,3.19),.018,m['steel'],6))
    p.append(s.rod('Idle drive motor',(-.54,.44,.60),(.02,.44,.60),.20,m['green'],12))
    p.append(s.box('Stopped belt guard',(-.14,.47,.85),(.57,.13,.74),m['iron'],.02))
    p+=s.tube('Disconnected electrical lead',[(-.5,.34,.48),(-.86,.42,.16),(-.98,.2,.02),(-.87,-.06,.02)],.014,m['black'],6)
    p.append(s.box('Unplugged isolator plug',(-.86,-.10,.04),(.065,.10,.065),m['iron']))
    return p,dict(function='Static decommissioned mill bandsaw; guarded return and supported feed table',runtime='Ready static environment mesh; no powered cutting or animation',placement='Blackpine sawmill, outside ritual and stair routes')

def log_carriage():
    m=work_materials();p=[]
    for x in [-.63,.63]:
        p.append(s.box('Mill carriage rail',(x,0,.055),(.07,3.8,.11),m['steel']))
        for y in [-1.45,1.45]:
            p.append(s.rod('Flanged carriage wheel',(x-.07,y,.28),(x+.07,y,.28),.17,m['iron'],12))
            p.append(s.rod('Outer wheel flange',(x-.08,y,.28),(x-.06,y,.28),.19,m['rust'],12))
    for y in [-1.4,0,1.4]:
        p.append(s.box('Oak cross bearer',(0,y,.42),(1.62,.24,.20),m['oak']))
        for x in [-.63,.63]:p.append(s.rod('Bearer fixing bolt',(x,y,.51),(x,y,.55),.025,m['iron'],6))
    for x in [-.63,.63]:p.append(s.box('Rolling underframe',(x,0,.32),(.12,3.2,.16),m['iron']))
    # Truncate the actual log cross-section at the sawn plane; no buried flat-face prop.
    profile=[(.31*math.cos(i*math.tau/16),min(1.055,.81+.31*math.sin(i*math.tau/16))) for i in range(16)]
    vv=[(x,y,z) for y in [-1.65,1.65] for x,z in profile]
    obj=s.mesh('Partly milled log with real flat cut',vv,[(i,(i+1)%16,(i+1)%16+16,i+16) for i in range(16)]+[tuple(range(15,-1,-1)),tuple(range(16,32))],m['oak'])
    obj.data.materials.append(m['pine'])
    for face in obj.data.polygons:
        if all(abs(obj.data.vertices[i].co.z-1.055)<.0001 for i in face.vertices):face.material_index=1
    p.append(obj)
    for y in [-1.654,1.654]:
        cap=[(.292*math.cos(i*math.tau/16),y,min(1.054,.81+.292*math.sin(i*math.tau/16))) for i in range(16)]
        p.append(s.mesh('Pale truncated cut end',cap,[tuple(range(16)),tuple(range(15,-1,-1))],m['pine']))
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
    m=work_materials();p=[]
    for x in [-1.05,1.05]:
        for y in [-.29,.29]:p.append(s.box('Braced bench leg',(x,y,.43),(.11,.11,.86),m['oak']))
        p.append(s.box('Lower end bearer',(x,0,.22),(.12,.68,.12),m['oak']))
    for y in [-.29,.29]:p.append(s.box('Long bench stretcher',(0,y,.25),(2.1,.075,.12),m['oak']))
    for i in range(5):p.append(s.box('Worn timber bench plank',(0,-.30+i*.15,.88),(2.45,.144,.085),m['pine']))
    p.append(s.box('Bench backboard',(0,.36,1.28),(2.43,.07,.73),m['oak']))
    for x in [-.9,-.45,0,.45,.9]:p.append(s.rod('Tool hook',(x,.35,1.43),(x,.22,1.43),.012,m['iron'],6))
    # Different readable silhouettes: spanner, claw hammer, hand saw, screwdriver and plane.
    # One forged open-jaw outline, with jaws joined to the shank.
    outline=[(-.951,1.445),(-.951,1.375),(-.925,1.35),(-.925,1.15),(-.951,1.125),(-.951,1.055),(-.925,1.055),(-.925,1.103),(-.875,1.103),(-.875,1.055),(-.849,1.055),(-.849,1.125),(-.875,1.15),(-.875,1.35),(-.849,1.375),(-.849,1.445),(-.875,1.445),(-.875,1.397),(-.925,1.397),(-.925,1.445)]
    n=len(outline);vv=[(x,y,z) for y in [.20,.24] for x,z in outline]
    p.append(s.mesh('Forged connected open spanner',vv,[tuple(range(n-1,-1,-1)),tuple(range(n,n*2))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)],m['steel']))
    p.append(s.rod('Hammer wooden handle',(-.45,.22,1.465),(-.45,.22,1.145),.023,m['pine'],6))
    p.append(s.box('Hammer head',(-.45,.22,1.475),(.22,.06,.065),m['iron']))
    for y in [.195,.245]:p.append(s.rod('Hammer split claw',(-.54,y,1.48),(-.59,y,1.44),.012,m['iron'],5))
    p.append(s.mesh('Hanging hand saw',[(.03,.22,1.40),(.18,.22,1.11),(.52,.22,1.11),(.31,.22,1.40)],[(0,1,2,3),(3,2,1,0)],m['steel']))
    for i in range(13):p.append(s.mesh('Saw tooth',[(.18+i*.025,.22,1.11),(.192+i*.025,.22,1.09),(.205+i*.025,.22,1.11)],[(0,1,2),(2,1,0)],m['steel']))
    # Open tote actually hangs by the inside of its upper rail on the hook.
    outer=[(-.07,1.31),(.11,1.31),(.11,1.49),(-.07,1.49)];inner=[(-.035,1.345),(.065,1.345),(.065,1.442),(-.035,1.442)]
    vv=[(x,y,z) for y in [.18,.26] for ring in [outer,inner] for x,z in ring];ff=[]
    for i in range(4):
        j=(i+1)%4;ff.extend([(i,j,j+4,i+4),(i+8,i+12,j+12,j+8),(i,i+8,j+8,j),(i+4,j+4,j+12,i+12)])
    p.append(s.mesh('Open wooden saw tote on hook',vv,ff,m['oak']))
    p.append(s.rod('Screwdriver shaft',(.65,-.04,.932),(.65,-.34,.932),.009,m['steel'],6))
    p.append(s.rod('Screwdriver grip',(.65,.12,.9505),(.65,-.04,.9505),.028,m['red'],8))
    p.append(s.box('Vice bolted cast base',(-.80,-.22,.93),(.30,.29,.025),m['iron'],.006))
    for x in [-.92,-.68]:p.append(s.rod('Vice mounting bolt',(x,-.13,.91),(x,-.13,.955),.014,m['steel'],6))
    p.append(s.box('Bench vice fixed jaw',(-.80,-.22,1.02),(.25,.23,.18),m['iron'],.015))
    p.append(s.box('Bench vice moving jaw',(-.80,-.46,1.02),(.25,.09,.18),m['iron']))
    p.append(s.rod('Vice lead screw',(-.80,-.23,.96),(-.80,-.58,.96),.019,m['steel'],8))
    p.append(s.rod('Vice sliding handle',(-.94,-.59,.96),(-.66,-.59,.96),.012,m['steel'],6))
    p.append(s.box('Hand plane sole',(.43,-.06,.945),(.15,.34,.045),m['iron']))
    p.append(s.box('Plane wooden tote',(.43,.02,1.01),(.065,.08,.12),m['oak'],.02))
    p.append(s.box('Sharpening stone',(.10,-.25,.945),(.22,.11,.045),m['iron']))
    p.append(s.lathe('Uncapped oil tin',[(0,.06),(.20,.06),(.23,.03)],m['rust'],10,(.94,-.18,.925)))
    p.append(s.box('Lower bench storage shelf',(0,0,.18),(2.12,.58,.05),m['oak']))
    p.append(s.box('Saved offcut',(.42,.1,.275),(.85,.35,.14),m['pine']))
    return p,dict(function='Repair bench, vice, sharpening stone, oil and five hand-tool silhouettes',runtime='Ready static workbench kit; tools are scenery, not inventory pickups',placement='Blackpine repair shed, against the north wall')

def crew_footlocker():
    m=work_materials();p=[]
    p.append(s.box('Trunk bottom',(0,0,.035),(.86,.47,.07),m['oak'],.006))
    for y in [-.235,.235]:p.append(s.box('Trunk long side',(0,y,.23),(.86,.035,.34),m['green']))
    for x in [-.43,.43]:p.append(s.box('Trunk end',(x,0,.23),(.035,.47,.34),m['green']))
    for x in [-.32,.32]:
        for y in [-.255,.255]:p.append(s.box('Rusted trunk band',(x,y,.23),(.045,.018,.34),m['iron']))
    for x in [-.34,.34]:
        p.append(s.rod('Lid hinge',(x-.07,.24,.40),(x+.07,.24,.40),.018,m['brass'],8))
    # Lid tilted back: actual geometry, hinge at rear upper edge.
    angle=.37
    lid=s.box('Hinge supported open lid',(0,.24+.255*math.sin(angle),.40+.255*math.cos(angle)),(.89,.045,.51),m['green'],.008);lid.rotation_euler[0]=-angle;p.append(lid)
    p.append(s.box('Tarnished name card holder',(0,-.259,.27),(.22,.014,.09),m['brass']))
    p.append(s.box('Faded crew card',(0,-.269,.27),(.18,.005,.06),m['paper']))
    p.append(s.box('Broken clasp',(0,-.276,.35),(.055,.018,.09),m['iron']))
    for x in [-.45,.45]:p+=s.tube('End carrying handle',[(x,-.07,.29),(x*1.08,-.07,.23),(x*1.08,.07,.23),(x,.07,.29)],.012,m['leather'],6)
    for i,key in enumerate(['cloth','blue','cloth']):p.append(s.box('Folded shirt inside',(-.18,.01,.096+i*.052),(.34,.32,.052),m[key],.012))
    p.append(s.box('Supported inner belongings tray',(.18,0,.1325),(.36,.36,.125),m['oak'],.005))
    p.append(s.box('Personal envelope',(.15,.06,.20),(.20,.14,.005),m['paper']))
    p.append(s.box('Small saved photograph',(.15,.06,.206),(.12,.10,.004),m['enamel']))
    for x in [.12,.17]:p.append(s.box('Photo silhouette',(x,.06,.209),(.025,.045,.002),m['iron']))
    p.append(s.lathe('Pocket thermos',[(0,.037),(.23,.037),(.25,.030)],m['steel'],10,(.30,-.13,.195)))
    for r in [.035,.027]:p+=b.ring('Coiled spare boot lace',(-.16,-.10,.229),r,.003,m['black'],axis='Z',segments=10)
    p.append(s.box('Worn mending packet',(.12,-.09,.2175),(.14,.09,.045),m['leather'],.004))
    return p,dict(function='Open crew footlocker with shirts, personal card/photo, thermos, laces and mending packet',runtime='Ready static personal-effects kit; lid and belongings are not interactive',placement='Blackpine bunkhouse, beside surviving beds and away from door approaches')

def hearth_tools():
    m=work_materials();p=[s.box('Freestanding fire tool rack base',(.18,.04,.035),(.36,.31,.07),m['iron'])]
    p.append(s.rod('Rack upright',(.18,.11,.06),(.18,.11,1.05),.025,m['iron'],8))
    p.append(s.rod('Hanging tool rail',(-.02,.11,1.04),(.38,.11,1.04),.016,m['iron'],6))
    for x in [0,.16,.32]:
        p+=s.tube('Hanging tool hook',[(x,.11,1.04),(x,.03,1.04),(x,.03,1.00)],.008,m['iron'],6)
        p+=b.ring('Hanging handle loop',(x,.03,.97),.036,.008,m['iron'],segments=8)
        p.append(s.rod('Fire tool shaft',(x,.03,.94),(x,.03,.25),.009,m['iron'],6))
    p.append(s.mesh('Ash shovel pan',[(-.07,.02,.12),(.07,.02,.12),(.055,.03,.30),(-.055,.03,.30)],[(0,1,2,3),(3,2,1,0)],m['iron']))
    p.append(s.rod('Poker bent tip',(.16,.03,.25),(.22,.03,.20),.01,m['iron'],6))
    p.append(s.rod('Tongs second arm',(.32,.03,.78),(.36,-.015,.20),.009,m['iron'],6))
    p.append(s.lathe('Lidded cold ash pail',[(0,.12),(.24,.16),(.27,.16),(.28,.12)],m['iron'],12,(-.22,-.05,0)))
    p.append(s.rod('Pail lid',(-.22,-.05,.275),(-.22,-.05,.285),.16,m['iron'],12))
    p+=s.tube('Lid handle',[(-.26,-.05,.285),(-.26,-.05,.32),(-.18,-.05,.32),(-.18,-.05,.285)],.008,m['steel'],6)
    p+=s.tube('Pail wire bail',[(-.22+.16*math.cos(i*math.pi/10),-.05,.26+.16*math.sin(i*math.pi/10)) for i in range(11)],.006,m['steel'],6)
    for i in range(5):p.append(s.rod('Dry split kindling',(-.35+i*.045,.20,.021),(-.35+i*.045,.40,.022+i*.003),.021,m['pine'],5))
    return p,dict(function='Cold ash pail, freestanding poker/shovel/tongs rack and split kindling',runtime='Ready static hearth detail; no live heat, ash or fire simulation',placement='Blackpine foreman and mess hearths, supported clear of walking aisles')

def boot_rack():
    m=work_materials();p=[]
    for x in [-.62,.62]:
        for y in [-.18,.18]:p.append(s.box('Washhouse rack leg',(x,y,.53),(.07,.07,1.06),m['oak']))
    for z in [.10,.39]:
        for x in [-.62,.62]:p.append(s.box('Drying shelf end bearer',(x,0,z-.045),(.09,.49,.07),m['oak']))
        for i in range(5):p.append(s.box('Open drying slat',(0,-.20+i*.10,z),(1.40,.065,.045),m['pine']))
    p.append(s.box('Rack upper back rail',(0,.18,1.11),(1.40,.075,.12),m['oak']))
    p.append(s.rod('Hanging towel rail',(-.625,-.08,1.03),(.625,-.08,1.03),.019,m['steel'],8))
    for x in [-.62,.62]:p.append(s.rod('Towel rail side bracket',(x,.18,1.03),(x,-.08,1.03),.012,m['iron'],6))
    for x in [-.41,.11]:
        vv=[(xx,y,z) for y,z in [(-.053,.92),(-.053,1.032),(-.08,1.053),(-.104,1.032),(-.115,.54)] for xx in [x,x+.30]]
        ff=[(i*2,i*2+1,i*2+3,i*2+2) for i in range(4)];ff+=list(tuple(reversed(f)) for f in ff)
        p.append(s.mesh('Towel draped over actual rail',vv,ff,m['cloth' if x<0 else 'blue']))
    # Reuse the project's original CC0 boots, retaining their UV/materials.
    boots=s.import_source('level_story_work_boots_worn',location=(-.38,-.01,.125),scale=.82);p.append(boots)
    for i in range(3):p.append(s.box('Folded wash cloth',(.40,0,.144+i*.043),(.30,.28,.043),m['cloth'],.01))
    return p,dict(function='Open boot-drying rack, old work boots, folded cloths and two draped towels',runtime='Ready static washhouse furnishing; boots retain original source UV/materials',derived_sources=s.SOURCE_LOG,placement='Blackpine washhouse east wall, outside the clear central rite')

def survivor_cookbox():
    m=work_materials();p=[]
    for i in range(4):
        for y in [-.26,.26]:p.append(s.box('Salvaged cook crate side',(0,y,.075+i*.105),(.78,.045,.092),m['pine']))
        for x in [-.39,.39]:p.append(s.box('Salvaged crate end',(x,0,.075+i*.105),(.045,.54,.092),m['pine']))
    p.append(s.box('Crate base',(0,0,.025),(.78,.54,.05),m['oak']))
    for x in [-.32,.32]:
        for y in [-.285,.285]:p.append(s.box('Crate corner batten',(x,y,.22),(.07,.035,.43),m['oak']))
    p.append(s.box('Loose supported lid',(0,.02,.46),(.84,.62,.055),m['pine']))
    p.append(s.lathe('Blackened cooking pot',[(0,.14),(.04,.16),(.25,.16),(.26,.14),(.025,.12)],m['iron'],12,(-.16,-.015,.49)))
    p+=s.tube('Hinged pot wire bail',[(-.16+.17*math.cos(i*math.pi/12),-.015,.74+.17*math.sin(i*math.pi/12)) for i in range(13)],.008,m['steel'],6)
    for x in [-.33,.01]:p.append(s.rod('Pot bail pivot eye',(x-.012,-.015,.74),(x+.012,-.015,.74),.014,m['steel'],8))
    p.append(s.rod('Pot lid',(-.16,-.015,.755),(-.16,-.015,.768),.16,m['iron'],12))
    p.append(s.box('Pot lid knob',(-.16,-.015,.79),(.06,.05,.045),m['black']))
    p+=b.mug(.23,-.13,.49,m)
    p.append(s.box('Cloth wrapped ration',(.21,.13,.55),(.28,.20,.12),m['cloth'],.015))
    for x in [.15,.27]:p.append(s.box('Parcel string',(x,.13,.613),(.005,.205,.004),m['paper']))
    # A stamped, upward-facing concave bowl, physically joined to its flat handle.
    # The old axial cylinder was the wrong shape/orientation and left a visible gap.
    rim=[(-.28+.036*math.cos(i*math.tau/12),-.22+.025*math.sin(i*math.tau/12),.506) for i in range(12)]
    verts=[(-.28,-.22,.491)]+rim+[(-.28,-.22,.488)]
    faces=[(0,1+i,1+(i+1)%12) for i in range(12)]+[(13,1+(i+1)%12,1+i) for i in range(12)]
    p.append(s.mesh('Upward concave spoon bowl',verts,faces,m['steel']))
    p.append(s.box('Connected stamped spoon handle',(-.10,-.22,.503),(.30,.014,.006),m['steel'],.002))
    for i in range(3):p.append(s.box('Folded cooking rag',(-.15,.13,.47+i*.012),(.25,.18,.011),m['cloth']))
    p.append(s.box('Dented lunch tin',(0,-.05,.17),(.38,.28,.24),m['steel'],.02))
    return p,dict(function='Salvaged cook crate, lidded soot-dark pot, wire bail, mug, ration parcel and spoon',runtime='Ready static survivor cooking kit; cold pot and non-interactive supplies',placement='Blackpine logging cabin recent occupation layer, beside its stove area')

def camp_sign():
    m=work_materials();p=[]
    for x in [-1.03,1.03]:
        p.append(s.box('Old sign bearing post',(x,0,1.04),(.11,.13,2.08),m['oak']))
        p.append(s.box('Post iron ground shoe',(x,0,.13),(.14,.16,.26),m['rust']))
    for z in [1.33,1.57,1.81]:p.append(s.box('Camp name plank',(0,0,z),(2.40,.10,.225),m['pine']))
    p.append(s.box('Later survey warning board',(0,-.01,.92),(2.0,.09,.38),m['enamel']))
    for x in [-1.01,1.01]:
        for z in [1.34,1.8]:p.append(s.rod('Sign fixing head',(x,-.045,z),(x,-.068,z),.018,m['iron'],6))
    def text(body,size,z,mat,depth=-.05):
        curve=bpy.data.curves.new('Original sign text','FONT');curve.body=body;curve.size=size;curve.align_x='CENTER';curve.resolution_u=1;curve.extrude=.0008
        o=bpy.data.objects.new(body,curve);bpy.context.collection.objects.link(o);o.location=(0,depth,z);o.rotation_euler=(math.pi/2,0,0);curve.materials.append(mat)
        bpy.ops.object.select_all(action='DESELECT');o.select_set(True);bpy.context.view_layer.objects.active=o;bpy.ops.object.convert(target='MESH');o=bpy.context.object;s.uv_project(o);p.append(o)
    text('BLACKPINE',.26,1.68,m['black']);text('LOGGING CAMP',.16,1.39,m['black'])
    text('ROAD SURVEYED',.13,.97,m['blue'],-.055);text('SITE NOT CLEARED',.13,.80,m['red'],-.055)
    p.append(s.box('Arrow bearing crossrail',(0,0,.55),(2.17,.105,.29),m['oak'],.009))
    outline=[(-.35,.61),(.2,.61),(.2,.68),(.43,.55),(.2,.42),(.2,.49),(-.35,.49)]
    arrow=[(x,y,z) for y in [-.0525,-.0585] for x,z in outline]
    p.append(s.mesh('Bolted direction arrow',arrow,[tuple(range(7)),tuple(range(13,6,-1))]+[(i,(i+1)%7,(i+1)%7+7,i+7) for i in range(7)],m['enamel']))
    for x in [-.26,.15]:p.append(s.rod('Arrow fixing bolt',(x,-.05,.55),(x,-.068,.55),.012,m['iron'],6))
    return p,dict(function='Original camp-name sign and later road-survey warning, readable mesh lettering',runtime='Ready static sign; surveyed road is explicitly not a cleared camp or unlock',placement='Blackpine arrival yard verge, outside baked path core')

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
