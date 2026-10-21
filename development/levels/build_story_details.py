"""Original CC0 environmental storytelling kit. Native metre pivots and packed diffuse maps.
Wall modules face glTF +Z; floor/table modules start at Y=0. No imported components.
"""
import bpy,sys,math,json,random
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'development/assets'),str(ROOT/'development/levels')]
import rebuild_support as s
import build_lived_assets as b

def finish(name,p,view=(2,-3,2)):
    b.export('level_trace_'+name,p,'site-storytelling',view)

def records(journal=False):
    m=b.init();p=[]
    for side in [-1,1]:
        p.append(s.box('Open ledger leather cover',(side*.115,0,.012),(.225,.30,.024),m['leather']))
        p.append(s.box('Yellowed ledger leaves',(side*.115,0,.030),(.215,.287,.014),m['paper']))
        for row in range(9):
            p.append(s.box('Hand ruled ledger line',(side*.115,-.116+row*.027,.038),(.18,.0015,.001),m['blue']))
        for row in range(5):
            p.append(s.box('Faded uneven handwriting',(side*.105,-.106+row*.047,.039),(.08+(row%3)*.025,.003,.001),m['iron']))
    if not journal:
        for i in range(4):p.append(s.box('Loose shipping papers',(.31,-.01+i*.005,.003+i*.002),(.17,.25,.002),m['paper']))
        p.append(s.rod('Hexagonal pencil',(-.24,-.11,.012),(-.24,.095,.012),.004,m['pine'],6))
        p.append(s.rod('Graphite pencil point',(-.24,.095,.012),(-.24,.112,.012),.004,m['black'],6,0))
        p.append(s.rod('Capped fountain pen',(.27,-.10,.016),(.27,.065,.016),.007,m['black'],8))
        p.append(s.rod('Pen pocket clip',(.266,-.093,.024),(.266,-.05,.024),.0015,m['brass'],4))
    finish('journal' if journal else 'records',p,view=(.8,-1,1.5))

def wall_items():
    m=b.init();p=[s.box('Calendar backing',(0,-.012,.23),(.34,.024,.46),m['pine']),s.box('Curled calendar page',(0,-.028,.22),(.31,.008,.42),m['paper'])]
    for row in range(5):
        for col in range(7):p.append(s.box('Calendar day cell',(-.125+col*.041,-.034,.10+row*.047),(.029,.002,.030),m['blue' if col<5 else 'red']))
    p.append(s.rod('Calendar nail',(0,0,.44),(0,-.045,.44),.009,m['iron'],6));finish('calendar',p)
    m=b.init();p=[s.box('Peg rail',(0,0,1.48),(.86,.05,.11),m['oak'])]
    for x in [-.30,0,.30]:p+=s.tube('Bent coat hook',[(x,-.02,1.48),(x,-.12,1.42),(x,-.14,1.48)],.012,m['iron'],6)
    p.append(s.mesh('Forgotten work coat',[(-.32,-.11,1.39),(-.09,-.11,1.39),(.04,-.12,.98),(-.06,-.13,.93),(-.08,-.12,1.14),(-.06,-.13,.55),(-.37,-.14,.55),(-.35,-.13,1.14),(-.43,-.13,.94),(-.52,-.13,1.0)],[(0,1,2,3,4,5,6,7,8,9)],m['green']))
    p.append(s.box('Coat pocket',(-.24,-.15,.83),(.12,.015,.13),m['cloth']));finish('coat',p)
    m=b.init();p=[s.box('Small personal photograph frame',(0,0,.14),(.25,.04,.28),m['oak']),s.box('Faded photograph paper',(0,-.025,.14),(.21,.008,.23),m['paper'])]
    for x,h in [(-.05,.10),(.045,.125)]:
        p.append(s.box('Faded family silhouette',(x,-.031,.12),(.045,.004,h),m['iron']))
        p.append(s.rod('Faded face',(x,-.033,.12+h*.65),(x,-.037,.12+h*.65),.021,m['leather'],8))
    finish('photograph',p)

def belongings():
    m=b.init();p=[]
    for i in range(6):
        o=s.box('Discarded folded work clothes',((i%2)*.18-.09,(i%3)*.09-.09,.025+i*.024),(.40-i*.025,.29,.045),m['blue' if i%2 else 'cloth']);o.rotation_euler[2]=i*.61;p.append(o)
    p.append(s.rod('One stocking',(-.24,0,.02),(-.30,.19,.02),.035,m['green'],6));finish('clothes',p)

def bedroll():
    m=b.init();p=[s.box('Recent sleeping mat',(0,0,.025),(.66,1.86,.05),m['black'],.015)]
    # The quilt stops below the head space. The pillow has its own visible volume
    # resting on the mat rather than being swallowed inside a full-length baffle.
    verts=[];rows=[-.80,-.64,-.48,-.32,-.16,0,.16,.32,.43]
    for i,y in enumerate(rows):
        width=.24+.065*math.sin((i+1)/10*math.pi);offset=.008*math.sin(i*1.7)
        for j,t in enumerate([-1,-.72,0,.72,1]):
            h=[.07,.145,.174,.15,.07][j]+.005*math.sin(i*2+j)
            verts.append((offset+t*width,y,h))
    faces=[(i*5+j,i*5+j+1,(i+1)*5+j+1,(i+1)*5+j) for i in range(8) for j in range(4)]
    p.append(s.mesh('Sleeping bag quilted shell',verts,faces,m['blue']))
    boundary=[verts[i*5] for i in range(9)]+[verts[40+j] for j in range(1,5)]+[verts[i*5+4] for i in range(7,-1,-1)]+[verts[j] for j in range(3,0,-1)]
    n=len(boundary);under=boundary+[(x,y,.05) for x,y,z in boundary]
    p.append(s.mesh('Sleeping bag supported underside',under,[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]+[tuple(range(n,2*n))],m['blue']))
    for i in range(1,8):
        y=rows[i];p+=s.tube('Sewn transverse baffle',[(verts[i*5+j][0],y,verts[i*5+j][2]+.001) for j in range(5)],.0016,m['black'],4)
    p+=s.tube('Side zipper',[verts[i*5+4] for i in range(9)],.006,m['iron'],6)
    t=(.36-.32)/(.43-.32);edge=tuple(verts[39][j]*(1-t)+verts[44][j]*t for j in range(3))
    p.append(s.rod('Zipper pull connecting link',edge,(edge[0],.36,edge[2]+.02),.004,m['steel'],6))
    p+=b.ring('Zipper pull tab',(edge[0]+.022,.36,edge[2]+.02),.022,.004,m['steel'],segments=8)
    p.append(s.box('Separate exposed pillow',(-.015,.685,.124),(.49,.29,.14),m['cloth'],.034))
    for x in [-.22,.22]:p.append(s.box('Mat carrying loop',(x,-.90,.047),(.09,.055,.025),m['leather'],.006))
    finish('bedroll',p,view=(2,-3,3))

def food_trash():
    m=b.init();p=[]
    for i in range(5):
        x=-.22+i*.11;y=.09*math.sin(i*3)
        p.append(s.lathe('Opened food tin',[(0,.046),(.10,.046),(.101,.038),(.01,.038)],m['rust' if i%2 else 'steel'],10,(x,y,0)))
        p.append(s.rod('Partly folded tin lid',(x,y,.104),(x+.04,y,.14),.043,m['steel'],10))
    for i in range(3):p.append(s.mesh('Crumpled wrapper',[(-.26+i*.16,-.24,.008),(-.13+i*.16,-.22,.018),(-.12+i*.16,-.10,.004),(-.24+i*.16,-.09,.022)],[(0,1,2,3)],m['paper']))
    p.append(s.lathe('Empty green bottle',[(0,.04),(.18,.04),(.22,.022),(.28,.018)],m['green'],8,(.26,.19,0)));finish('food_trash',p)

def damage():
    m=b.init();p=[]
    rng=random.Random(140)
    for i in range(17):
        x=rng.uniform(-.48,.48);y=rng.uniform(-.37,.37)
        p.append(s.mesh('Abraded timber scar',[(x,y,.004),(x+.014,y+.08,.004),(x+.009,y+.24,.004),(x-.008,y+.15,.004)],[(0,1,2,3)],m['pine']))
    for x in [-.19,.24]:p.append(s.mesh('Surface split dark recess',[(x,-.37,.006),(x+.013,.04,.006),(x+.004,.38,.006),(x-.008,.02,.006)],[(0,1,2,3)],m['black']))
    finish('floor_wear',p,view=(1,-1,2))
    m=b.init();p=[s.mesh('Missing plaster exposing backing',[(-.43,0,.08),(-.50,0,.57),(-.21,0,.89),(.17,0,.78),(.44,0,.47),(.30,0,.05)],[(0,1,2,3,4,5)],m['oak'])]
    for i in range(5):p.append(s.box('Exposed timber lath',(0,-.008,.13+i*.13),(.73,.014,.045),m['pine']))
    for i in range(6):p.append(s.box('Plaster flake',(-.43+i*.16,-.017,.07+(i%3)*.28),(.09,.018,.065),m['enamel']))
    finish('wall_damage',p)
    m=b.init();p=[]
    for i in range(9):
        x=-.34+i*.085;h=.30+(i%4)*.12
        p.append(s.mesh('Rain and damp streak',[(x,0,.02),(x+.035,0,.02),(x+.025,0,h),(x+.008,0,h+.12)],[(0,1,2,3)],m['green' if i%3==0 else 'rust']))
    finish('damp_wall',p)
    m=b.init();p=[s.lathe('Leak catching bucket',[(0,.15),(.02,.17),(.29,.21),(.31,.19),(.025,.14)],m['iron'],12)]
    p+=b.ring('Bucket wire handle',(0,0,.31),.21,.008,m['steel'])
    for i in range(9):
        a=i*math.tau/9;p.append(s.mesh('Irregular dried water stain',[(.17*math.cos(a),.17*math.sin(a),.003),(.48*math.cos(a),.38*math.sin(a),.003),(.48*math.cos(a+.5),.38*math.sin(a+.5),.003)],[(0,1,2)],m['rust']))
    finish('leak_bucket',p)
    m=b.init();p=[]
    for i in range(4):
        o=s.box('Weathered window boarding',(0,-.02,.13+i*.31),(1.31,.05,.20),m['pine']);o.rotation_euler[1]=(-.035 if i%2 else .045);p.append(o)
        for x in [-.53,.53]:p.append(s.rod('Board fixing nail',(x,-.05,.13+i*.31),(x,-.057,.13+i*.31),.009,m['iron'],6))
    finish('window_boards',p)

def stove():
    m=b.init();p=[s.box('Noncombustible stove hearth',(0,0,.035),(.96,1.03,.07),m['steel']),s.box('Cast iron stove',(0,0,.50),(.59,.56,.63),m['iron'],.03)]
    for x in [-.23,.23]:
        for y in [-.21,.21]:p.append(s.box('Stove foot',(x,y,.14),(.055,.055,.18),m['iron']))
    p.append(s.box('Latched fire door',(0,-.29,.49),(.44,.025,.37),m['rust'],.01))
    p.append(s.rod('Fire door handle',(.15,-.33,.44),(.15,-.33,.58),.015,m['brass'],6))
    p.append(s.rod('Continuous vertical stove flue',(0,.13,.82),(0,.13,3.65),.078,m['iron'],10))
    p.append(s.box('Roof flashing plate',(0,.13,3.03),(.43,.43,.025),m['steel']))
    p.append(s.box('Weather cap',(0,.13,3.70),(.32,.32,.04),m['iron']))
    p+=b.mug(.12,-.08,.82,m);finish('woodstove',p,view=(3,-4,2.7))

def services():
    m=b.init();p=[s.box('Fused branch enclosure',(0,-.045,1.65),(.31,.09,.40),m['iron']),s.box('Fuse service cover',(0,-.097,1.65),(.27,.015,.35),m['enamel'])]
    for z in [1.55,1.70]:p.append(s.rod('Ceramic fuse cap',(-.06,-.11,z),(-.06,-.135,z),.035,m['ivory'],10))
    p+=s.tube('Wall mounted conduit',[(0,0,2.64),(0,0,1.86),(0,0,1.44),(.38,0,1.44),(.38,0,.36)],.012,m['steel'],6)
    p.append(s.box('Wall socket plate',(.38,-.025,.35),(.10,.05,.15),m['ivory']))
    for x in [.36,.40]:p.append(s.box('Socket recess',(x,-.053,.36),(.009,.003,.025),m['black']))
    finish('electric_branch',p)
    m=b.init();p=[s.box('Diesel generator skid',(0,0,.07),(1.55,.88,.14),m['iron']),s.box('Retired engine block',(-.23,0,.47),(.78,.61,.60),m['green'],.03),s.rod('Generator alternator',(.15,0,.46),(.62,0,.46),.28,m['iron'],12),s.box('Fuel day tank',(-.28,0,.87),(.66,.54,.19),m['rust'],.025)]
    for i in range(7):p.append(s.box('Engine cooling fin',(-.60+i*.09,-.32,.48),(.018,.05,.44),m['iron']))
    p+=s.tube('Exhaust pipe',[(-.44,.16,.64),(-.44,.16,1.12),(-.1,.16,1.12)],.035,m['rust'],8)
    p.append(s.box('Locked maintenance power inlet',(.58,-.33,.75),(.27,.09,.20),m['enamel']))
    finish('generator',p)
    m=b.init();p=[s.lathe('Oil water heater jacket',[(0,.27),(.12,.32),(1.27,.32),(1.36,.23)],m['enamel'],12)]
    p.append(s.box('Oil burner service housing',(0,-.35,.30),(.33,.25,.25),m['rust']))
    p+=s.tube('Flue with visible wall brackets',[(0,0,1.36),(0,0,3.55)],.08,m['iron'],10)
    p.append(s.box('Flue roof flashing',(0,0,3.02),(.40,.40,.03),m['steel']))
    p.append(s.box('Oil can',( .47,0,.18),(.23,.25,.36),m['rust']))
    p+=s.tube('Oil supply line',[(.47,0,.20),(.45,-.25,.20),(0,-.48,.20)],.012,m['brass'],6)
    finish('oil_waterheater',p,view=(3,-4,2.7))
    m=b.init();p=[]
    for x in [-.65,.65]:p.append(s.box('Trough wall bracket',(x,.03,.55),(.06,.52,1.10),m['iron']))
    p.append(s.box('Trough bottom',(0,0,.79),(1.65,.52,.045),m['enamel']))
    for y in [-.28,.28]:p.append(s.box('Trough rim',(0,y,.90),(1.7,.035,.23),m['enamel']))
    for x in [-.83,.83]:p.append(s.box('Trough end',(x,0,.90),(.035,.56,.23),m['enamel']))
    for x in [-.44,.44]:p+=s.tube('Cold water tap',[(x,.26,.99),(x,.26,1.14),(x,.05,1.14)],.016,m['brass'],6)
    p+=s.tube('Drain to floor',[(0,0,.77),(0,0,.48),(.24,0,.48),(.24,0,0)],.022,m['iron'],8)
    finish('wash_trough',p)

def bed():
    m=b.init();p=[]
    for x in [-.43,.43]:
        for y in [-.99,.99]:p.append(s.box('Camp bed post',(x,y,.41),(.065,.065,.82),m['oak']))
        p.append(s.box('Camp bed side rail',(x,0,.34),(.055,2.02,.13),m['oak']))
    for y in [-.99,.99]:p.append(s.box('Camp bed end rail',(0,y,.65),(.90,.055,.15),m['oak']))
    p.append(s.box('Old striped ticking mattress',(0,0,.45),(.84,1.91,.19),m['cloth'],.035))
    p.append(s.box('Folded wool blanket',(0,-.47,.57),(.83,.72,.07),m['green'],.01))
    p.append(s.box('Flattened pillow',(.02,.72,.58),(.54,.32,.12),m['paper'],.025));finish('camp_bed',p)

manifest_path=ROOT/'development/levels/story-detail-assets.json'
target=sys.argv[sys.argv.index('--')+1] if '--' in sys.argv else None
if target:
    globals()[target]();manifest=json.loads(manifest_path.read_text())
    replacements={a['id']:a for a in b.LOG};manifest['assets']=[replacements.get(a['id'],a) for a in manifest['assets']]
else:
    records();records(True);wall_items();belongings();bedroll();food_trash();damage();stove();services();bed()
    manifest={'license':'CC0-1.0','contract':'metres; preserve native origin, UV and material; wall modules face +Z','assets':b.LOG}
manifest_path.write_text(json.dumps(manifest,indent=2)+'\n')
print('STORY_DETAILS_COMPLETE',len(b.LOG),flush=True)
