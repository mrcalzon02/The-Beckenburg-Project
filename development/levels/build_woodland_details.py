"""Original reusable camp and woodland meshes. Metres, native floor pivots, CC0.
Run with Blender; all diffuse images are authored here, packed in each GLB/source.
"""
import bpy,sys,math,json,random
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'development/assets'),str(ROOT/'development/levels')]
import rebuild_support as s
import build_lived_assets as b

def materials():
    m=b.init()
    for key,color,kind in [('stone',(.29,.31,.27),'paint'),('mortar',(.14,.16,.14),'paint'),
        ('soil',(.19,.135,.085),'paint'),('needles',(.22,.19,.115),'wood'),
        ('leaf',(.16,.23,.10),'paint'),('dry',(.31,.29,.14),'cloth'),
        ('petal',(.63,.60,.41),'ceramic'),('purple',(.34,.29,.43),'cloth'),('floor',(.19,.21,.15),'paint')]:
        m[key]=s.material('Woodland '+key,color,kind)
    for key,base in [('needles',(.24,.21,.145)),('floor',(.19,.21,.15)),('soil',(.245,.205,.15))]:
        image=next(n.image for n in m[key].node_tree.nodes if n.type=='TEX_IMAGE')
        rng=random.Random('floor-'+key);pixels=[]
        for y in range(64):
            for x in range(64):
                factor=1+.12*math.sin(x*.27+math.sin(y*.19)*2)+.10*math.sin(y*.38+x*.11)+rng.uniform(-.09,.09)
                fleck=.045 if (x*7+y*11)%53<2 else 0
                pixels.extend([min(1,c*factor+fleck) for c in base]+[1])
        image.pixels.foreach_set(pixels);image.pack()
    return m

def finish(name,p,view=(4,-5,3)):
    b.export(name,p,'woodland-details',view=view)

def well():
    m=materials();p=[]
    # Individual staggered stones form a real ring; a sealed iron grate prevents a fake pit.
    for row in range(4):
        for i in range(12):
            a=(i+row%2*.5)*math.tau/12;d=math.tau/12*.47
            verts=[(r*math.cos(t),r*math.sin(t),z) for z in [row*.19,(row+1)*.19-.012] for r,t in [(.52,a-d),(.78,a-d),(.78,a+d),(.52,a+d)]]
            p.append(s.mesh('Staggered well stone',verts,[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)],m['stone']))
    for y in [-.40,-.20,0,.20,.40]:
        half=math.sqrt(.50**2-y*y)
        p.append(s.rod('Sealed safety grille',(-half,y,.62),(half,y,.62),.018,m['iron'],5))
    for x in [-.91,.91]:p.append(s.box('Roof bearing post',(x,0,1.3),(.13,.16,2.6),m['oak']))
    p.append(s.rod('Winch axle',(-1.05,0,1.25),(1.05,0,1.25),.07,m['oak'],10))
    p+=s.tube('Crank',[(1.05,0,1.25),(1.05,0,1.53),(1.23,0,1.53)],.035,m['iron'],7)
    for i in range(8):
        x=-.14+i*.04
        p+=s.tube('Wound rope',[(x,.09*math.cos(j*math.tau/10),1.25+.09*math.sin(j*math.tau/10)) for j in range(11)],.012,m['cloth'],4)
    p.append(s.rod('Hanging rope',(0,0,.65),(0,0,1.23),.016,m['cloth'],5))
    for side in [-1,1]:
        p.append(s.mesh('Pitched well roof',[(0,-.94,2.8),(side*1.2,-.94,2.35),(side*1.2,.94,2.35),(0,.94,2.8)],[(0,1,2,3),(3,2,1,0)],m['pine']))
        for y in [-.88,-.45,0,.45,.88]:p.append(s.rod('Roof batten',(0,y,2.81),(side*1.2,y,2.36),.025,m['oak'],4))
    p.append(s.lathe('Empty water pail',[(0,.13),(.04,.17),(.30,.20),(.31,.18),(.05,.15)],m['iron'],12,(.97,-.45,0)))
    finish('level_camp_well',p)

def patio():
    m=materials();p=[];rng=random.Random(49)
    for ix in range(8):
        for iy in range(6):
            x=-1.75+ix*.5;y=-1.25+iy*.5
            p.append(s.box('Flush uneven flagstone',(x+rng.uniform(-.012,.012),y+rng.uniform(-.012,.012),-.055),(.478,.478,.11),m['stone'],.013))
    # Native upper face is exactly zero: this surface does not create a false raised deck.
    finish('level_camp_patio',p,view=(4,-4,5))

def canopy():
    m=materials();p=[]
    for x in [-1.75,1.75]:
        p.append(s.box('Front load bearing post',(x,-1.1,1.27),(.14,.14,2.54),m['oak']))
        p.append(s.rod('Diagonal knee brace',(x,-1.1,2.05),(x,-.65,2.57),.055,m['oak'],4))
    for y,z in [(1.22,2.96),(-1.22,2.53)]:p.append(s.box('Continuous supported beam',(0,y,z),(4.12,.12,.16),m['oak']))
    for x in [-1.9,-.95,0,.95,1.9]:p.append(s.rod('Roof rafter',(x,1.25,2.99),(x,-1.28,2.56),.055,m['pine'],4))
    for i in range(14):
        x=-2.0+i*.30
        p.append(s.mesh('Overlapping canopy board',[(x,1.30,3.08),(x+.29,1.30,3.08),(x+.29,-1.34,2.63),(x,-1.34,2.63)],[(0,3,2,1),(1,2,3,0)],m['pine']))
    finish('level_camp_canopy',p)

def fireplace():
    m=materials();p=[s.box('Supported hearth slab',(0,-.15,.07),(1.9,1.15,.14),m['stone'])]
    for side in [-1,1]:
        for row in range(7):p.append(s.box('Masonry jamb',(side*.68,0,.23+row*.19),(.36,.64,.18),m['stone'],.016))
    p.append(s.box('Soot black firebox back',(0,.27,.66),(1.05,.09,1.05),m['black']))
    p.append(s.box('Stone lintel',(0,0,1.62),(1.78,.72,.23),m['stone'],.02))
    p.append(s.box('Mantel timber',(0,-.04,1.80),(1.96,.79,.13),m['oak'],.015))
    for x in [-.42,-.21,0,.21,.42]:p.append(s.rod('Cold grate bars',(x,-.43,.20),(x,.14,.27),.018,m['iron'],5))
    for y in [-.18,.02]:p.append(s.rod('Cold split firewood',(-.43,y,.33),(.43,y,.35),.085,m['oak'],7))
    p+=b.mug(.61,-.20,1.865,m)
    finish('level_camp_fireplace',p)

def chimney():
    m=materials();p=[]
    for row in range(25):
        # Open flue above the roof, closed masonry shaft lower down.
        z=row*.19+.095
        if row<20:p.append(s.box('Bonded chimney course',(0,0,z),(.94,.76,.181),m['stone']))
        else:
            for x in [-.36,.36]:p.append(s.box('Open flue side',(x,0,z),(.22,.76,.181),m['stone']))
            for y in [-.27,.27]:p.append(s.box('Open flue return',(0,y,z),(.50,.22,.181),m['stone']))
        for side in [-1,1]:p.append(s.box('Staggered mortar joint',(side*.473,(-.13 if row%2 else .16),z),(.008,.018,.18),m['mortar']))
    for x in [-.36,.36]:p.append(s.box('Cap stone side',(x,0,4.78),(.28,.91,.12),m['stone']))
    for y in [-.33,.33]:p.append(s.box('Cap stone end',(0,y,4.78),(.48,.25,.12),m['stone']))
    p.append(s.mesh('Pitched roof flashing collar',[(-.55,-.47,2.91),(.55,-.47,3.24),(.55,.47,3.24),(-.55,.47,2.91)],[(0,1,2,3),(3,2,1,0)],m['iron']))
    finish('level_camp_chimney',p,view=(5,-7,5))

def work_objects():
    m=materials();p=[]
    for y in [-.52,.52]:p.append(s.box('Raised timber skid',(0,y,.09),(3,.13,.18),m['oak']))
    for row,n in enumerate([5,4,3]):
        for j in range(n):
            y=(j-(n-1)/2)*.28
            p.append(s.rod('Barked saw log',(-1.35,y,.32+row*.25),(1.35,y,.32+row*.25),.14,m['oak'],9))
            for x in [-1.355,1.355]:p.append(s.rod('Exposed end grain',(x-.003,y,.32+row*.25),(x+.003,y,.32+row*.25),.12,m['pine'],9))
    finish('level_camp_logpile',p)
    m=materials();p=[s.box('Sawhorse top',(0,0,.84),(1.65,.15,.16),m['pine'])]
    for x in [-.58,.58]:
        for side in [-1,1]:p.append(s.rod('Splayed trestle leg',(x,side*.36,0),(x,side*.055,.82),.065,m['oak'],4))
        p.append(s.box('Trestle brace',(x,0,.31),(.08,.60,.08),m['oak']))
    p.append(s.box('Hand saw blade',(.22,0,.943),(.73,.14,.016),m['steel']))
    p.append(s.box('Saw wood grip',(-.22,0,.95),(.22,.18,.04),m['oak']))
    finish('level_camp_sawhorse',p)
    m=materials();p=[s.lathe('Stave water butt',[(0,.29),(.1,.34),(.65,.38),(1.12,.33),(1.15,.32)],m['oak'],16)]
    for z in [.15,.58,1.04]:p+=b.ring('Barrel hoop',(0,0,z),.35,.022,m['iron'],axis='Z',segments=16)
    p.append(s.rod('Covered barrel lid',(0,0,1.14),(0,0,1.18),.33,m['pine'],16))
    p+=s.tube('Roof rainwater downpipe',[(.20,.20,2.9),(.20,.20,1.3),(0,.13,1.2)],.04,m['rust'],8)
    p+=s.tube('Brass tap',[(0,-.33,.24),(0,-.47,.24),(0,-.47,.18)],.02,m['brass'],7)
    finish('level_camp_rainbarrel',p)
    m=materials();p=[]
    for y in [-.19,0,.19]:p.append(s.box('Separate seat plank',(0,y,.45),(1.75,.17,.07),m['pine']))
    for x in [-.64,.64]:
        for y in [-.19,.19]:p.append(s.rod('Bench leg',(x,y,0),(x,y,.42),.045,m['oak'],4))
        p.append(s.rod('Backrest upright',(x,.22,.3),(x,.30,.87),.04,m['oak'],4))
    for z in [.65,.82]:p.append(s.box('Backrest rail',(0,.28,z),(1.75,.055,.12),m['pine']))
    p.append(s.box('Underseat stretcher',(0,0,.18),(1.34,.07,.08),m['oak']))
    finish('level_camp_bench',p)

def plants():
    m=materials();p=[];rng=random.Random(12)
    for i in range(28):
        x,y=rng.uniform(-.42,.42),rng.uniform(-.42,.42);h=rng.uniform(.12,.36);a=rng.random()*math.tau;w=.022
        p.append(s.mesh('Bent grass blade',[(x-math.cos(a)*w,y-math.sin(a)*w,0),(x+math.cos(a)*w,y+math.sin(a)*w,0),(x+math.sin(a)*.08,y+math.cos(a)*.08,h)],[(0,1,2),(2,1,0)],m['leaf' if i%3 else 'dry']))
    finish('level_woodland_grass',p,view=(1.5,-2,1.3))
    m=materials();p=[]
    for i in range(7):
        a=i*math.tau/7
        for j in range(1,6):
            t=j/6;d=.65*t;z=.48*math.sin(t*math.pi*.85);x,y=math.cos(a)*d,math.sin(a)*d
            for side in [-1,1]:
                l=.21*(1-t)+.03;px,py=x-side*math.sin(a)*l,y+side*math.cos(a)*l
                p.append(s.mesh('Fern pinna',[(x-.03,y-.03,z),(px,py,z-.08),(x+.04,y+.04,z)],[(0,1,2),(2,1,0)],m['leaf']))
    finish('level_woodland_fern',p,view=(1.6,-2,1.2))
    m=materials();p=[];rng=random.Random(18)
    for i in range(9):
        x,y=rng.uniform(-.33,.33),rng.uniform(-.33,.33);z=rng.uniform(.18,.38)
        p.append(s.rod('Small wildflower stem',(x,y,0),(x,y,z),.006,m['leaf'],4))
        for j in range(5):
            a=j*math.tau/5;r=.05
            p.append(s.mesh('Wildflower petal',[(x,y,z),(x+math.cos(a)*r,y+math.sin(a)*r,z+.012),(x+math.cos(a+.65)*r,y+math.sin(a+.65)*r,z)],[(0,1,2),(2,1,0)],m['petal' if i%2 else 'purple']))
    finish('level_woodland_flowers',p,view=(1.5,-2,1.2))
    m=materials();p=[]
    for i in range(5):
        a=i*math.tau/5;x,y=math.cos(a)*.28,math.sin(a)*.28
        p.append(s.rod('Low scrub twig',(0,0,0),(x,y,.49),.015,m['oak'],5))
        bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1,radius=1,location=(x,y,.42))
        o=bpy.context.object;o.name='Low sparse scrub leaves';o.scale=(.34,.28,.28);o.data.materials.append(m['leaf']);p.append(o)
    finish('level_woodland_scrub',p,view=(2,-2,1.5))

def forest_floor():
    m=materials();p=[s.lathe('Old cut stump',[(0,.40),(.08,.32),(.48,.29),(.52,.32)],m['oak'],9)]
    p.append(s.rod('Freshly weathered cut end',(0,0,.517),(0,0,.525),.28,m['pine'],12))
    for i in range(6):
        a=i*math.tau/6;p.append(s.rod('Root flare',(math.cos(a)*.52,math.sin(a)*.52,.02),(math.cos(a)*.23,math.sin(a)*.23,.25),.06,m['oak'],5))
    finish('level_woodland_stump',p)
    m=materials();p=[]
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1,radius=1,location=(0,0,.27))
    o=bpy.context.object;o.name='Irregular mossy field boulder';o.scale=(.9,.62,.50);o.data.materials.append(m['stone']);p.append(o)
    p.append(s.mesh('Moss cap',[(-.4,-.1,.64),(.2,-.3,.68),(.5,.1,.60),(-.1,.28,.70)],[(0,1,2,3),(3,2,1,0)],m['leaf']))
    finish('level_woodland_boulder',p)
    m=materials();p=[s.rod('Fallen decaying trunk',(-2.3,0,.20),(2.3,0,.24),.22,m['oak'],9)]
    for x,side in [(-1.2,1),(.3,-1),(1.2,1)]:p.append(s.rod('Broken branch stub',(x,0,.27),(x+.28,side*.58,.37),.065,m['oak'],6))
    finish('level_woodland_deadfall',p,view=(4,-6,3))
    # Normalized ground stamps: native X/Z axes, irregular edges, no opaque billboard leaves.
    for name,key in [('track','soil'),('litter','needles'),('floor','floor')]:
        m=materials();p=[];verts=[(0,0,0)]
        for i in range(12):
            a=i*math.tau/12
            if name in ('track','floor'):x,y=math.cos(a)*.5,math.sin(a)*.5
            else:x,y=math.cos(a)*(.44+(i%3)*.025),math.sin(a)*(.44+(i%2)*.04)
            verts.append((x,y,0))
        p.append(s.mesh('Irregular '+name+' diffuse ground',verts,[(0,1+i,1+(i+1)%12) for i in range(12)],m[key]))
        rng=random.Random(name)
        for i in range(0 if name=='floor' else 18):
            x,y=rng.uniform(-.33,.33),rng.uniform(-.33,.33)
            p.append(s.mesh('Needle or gravel fleck',[(x,y,.001),(x+.025,y+.004,.001),(x+.006,y+.018,.001)],[(0,1,2)],m['dry' if name=='litter' else 'stone']))
        finish('level_woodland_'+name,p,view=(1,-1,2))
    m=materials();p=[];verts=[]
    for iy in range(4):
        for ix in range(5):
            h=0 if iy in (0,3) or ix in (0,4) else 1.8+math.sin(ix*3+iy)*.7
            verts.append(((ix-2)*3,(iy-1.5)*3,h-.12))
    p.append(s.mesh('Low wooded bank',verts,[(j*5+i,j*5+i+1,(j+1)*5+i+1,(j+1)*5+i) for j in range(3) for i in range(4)],m['leaf']))
    finish('level_woodland_bank',p,view=(10,-12,8))

if '--' in sys.argv and sys.argv[sys.argv.index('--')+1:] == ['chimney']:
    chimney();sys.exit(0)
well();patio();canopy();fireplace();chimney();work_objects();plants();forest_floor()
(ROOT/'development/levels/woodland-detail-assets.json').write_text(json.dumps({'license':'CC0-1.0','coordinate_contract':'metres; Blender Z up, glTF Y up; preserve native origin','assets':b.LOG},indent=2)+'\n')
print('WOODLAND_DETAILS_COMPLETE',len(b.LOG),flush=True)
