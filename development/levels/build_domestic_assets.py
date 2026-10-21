"""Original CC0 bathroom/laundry fittings, with native metre origins and real shaped parts.
Blender Z-up. Local front is -Y (glTF +Z); wall-mounted kits have their back at Y=0.
"""
import bpy,sys,math,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'development/assets'));sys.path.insert(0,str(ROOT/'development/levels'))
import rebuild_support as s
import build_lived_assets as b
TAU=math.tau

def elliptical_ring(name,rx,ry,z,wire,material,count=24):
 p=[]
 for i in range(count):
  a=i*TAU/count;c=(i+1)*TAU/count;p.append(s.rod(name,(rx*math.cos(a),ry*math.sin(a),z),(rx*math.cos(c),ry*math.sin(c),z),wire,material,5))
 return p

def washer():
 m=b.init();p=[s.box('Enamel formed chassis',(0,0,.43),(.62,.61,.84),m['enamel'],.035),s.box('Pressed top cover',(0,-.005,.855),(.63,.63,.035),m['ivory'],.012),s.box('Recessed toe plate',(0,-.306,.075),(.54,.009,.085),m['black'],.008)]
 # A separately formed door and drum, with integral hinge/handle and control strip.
 p+=b.ring('Door outer steel rim',(0,-.322,.405),.222,.022,m['steel'],segments=24)
 p+=b.ring('Deep black door gasket',(0,-.329,.405),.193,.018,m['black'],segments=24)
 p.append(s.rod('Recessed drum window',(0,-.321,.405),(0,-.326,.405),.174,m['black'],24))
 for dx,z in [(-.075,.46),(.05,.36),(.09,.46)]:p.append(s.rod('Dim steel drum reflection',(dx,-.328,z),(dx+.03,-.329,z+.035),.012,m['steel'],5))
 p += [s.box('Door hinge',(-.222,-.331,.41),(.038,.027,.10),m['steel'],.008),s.box('Attached door pull',(.201,-.353,.43),(.035,.035,.105),m['ivory'],.012),s.box('Control fascia',(0,-.314,.755),(.55,.016,.095),m['ivory'],.01),s.box('Detergent drawer',(-.173,-.326,.754),(.16,.012,.065),m['enamel'],.006),s.rod('Selector spindle',(.12,-.324,.756),(.12,-.348,.756),.034,m['black'],12)]
 for x in [-.02,.03,.21]:p.append(s.rod('Sealed programme key',(x,-.327,.755),(x,-.337,.755),.009,m['iron'],8))
 p+=s.tube('Supply hose',[(.19,.30,.60),(.19,.42,.60),(.19,.42,.82)],.013,m['black'],8)
 for x in [-.23,.23]:
  for y in [-.23,.23]:p.append(s.rod('Adjustable rubber foot',(x,y,0),(x,y,.04),.03,m['black'],8))
 b.export('level_laundry_washer',p,'household',view=(1,-1.6,1))

def hamper():
 m=b.init();p=[];n=24
 for j in range(n):
  a=j*TAU/n;points=[]
  for k in range(6):
   h=.04+k*.105;rx=.205+.025*math.sin(k*math.pi/5);ry=.165+.02*math.sin(k*math.pi/5);points.append((rx*math.cos(a),ry*math.sin(a),h))
  p+=s.tube('Woven upright',points,.010,m['pine'],5)
 for k in range(10):p+=elliptical_ring('Basket weave',.205+.025*math.sin(k*math.pi/9),.165+.02*math.sin(k*math.pi/9),.04+k*.059,.009,m['oak'])
 p+=elliptical_ring('Bound basket rim',.208,.168,.58,.017,m['pine'])
 p.append(s.lathe('Wicker base',[(.005,.15),(.03,.18)],m['oak'],16))
 # Uneven fabric edges and a draped sleeve, rather than an opaque cubic lid.
 v=[(-.16,-.10,.47),(.03,-.13,.49),(.15,-.05,.46),(.12,.10,.50),(-.12,.12,.48),(-.17,-.15,.58),(-.12,-.18,.38)]
 p.append(s.mesh('Rumpled linen and hanging sleeve',v,[(0,1,2,3,4),(0,5,1),(0,6,5)],m['cloth']))
 b.export('level_laundry_hamper',p,'household')

def shower(rail=False):
 m=b.init();p=[]
 if not rail:
  # Hollow low tray and tiled back/right walls; the front remains physically enterable.
  p=[s.box('Shower tray floor',(0,0,.045),(.91,.86,.075),m['ivory'],.018)]
  for x in [-.455,.455]:p.append(s.box('Tray raised side',(x,0,.09),(.035,.89,.13),m['ivory'],.012))
  for y in [-.43,.43]:p.append(s.box('Tray raised end',(0,y,.09),(.94,.035,.13),m['ivory'],.012))
  p += [s.box('Tiled back panel',(0,.442,1.15),(.96,.035,2.10),m['enamel'],.007),s.box('Tiled return panel',(.464,0,1.15),(.035,.89,2.10),m['enamel'],.007)]
  for x in [-.24,0,.24]:p.append(s.box('Back tile grout',(x,.422,1.15),(.005,.002,2.08),m['steel']))
  for z in [.35,.60,.85,1.1,1.35,1.6,1.85]:p.append(s.box('Tile course grout',(0,.422,z),(.94,.002,.005),m['steel']))
  width=.88;back=.40;pipe_x=.13;low=.65;rod_y=-.38
 else:width=1.55;back=.32;pipe_x=.61;low=.60;rod_y=-.34
 p+=s.tube('Continuous attached shower riser',[(pipe_x,back,low),(pipe_x,back,1.98),(pipe_x,back-.08,2.06),(pipe_x,back-.24,2.06)],.014,m['brass'],10)
 p.append(s.rod('Shower spray rose',(pipe_x,back-.25,2.07),(pipe_x,back-.26,2.02),.07,m['steel'],16))
 for x in [pipe_x-.085,pipe_x+.085]:p.append(s.rod('Mixer valve and handle',(x,back+.01,.99),(x,back-.035,.99),.026,m['steel'],8))
 p.append(s.rod('Curtain rod',(-width/2,rod_y,2.10),(width/2,rod_y,2.10),.015,m['steel'],10))
 for x in [-width/2,width/2]:p.append(s.rod('Rod wall stay',(x,rod_y,2.10),(x,.52 if rail else .442,2.10),.012,m['steel'],8))
 for z in [1.05,1.85]:p.append(s.rod('Riser wall attachment',(pipe_x,back,z),(pipe_x,.52 if rail else .442,z),.009,m['steel'],8))
 # Curtain drawn back to one side, leaving a real opening rather than an invisible solid cube.
 verts=[]
 for z in [.26,2.06]:
  for i in range(13):verts.append((-width/2+i*.026,rod_y+.035*math.sin(i*math.pi),z+.012*math.cos(i)))
 p.append(s.mesh('Pleated drawn shower curtain',verts,[(i,i+1,i+14,i+13) for i in range(12)],m['cloth']))
 for i in range(7):
  xx=-width/2+i*.048
  for j in range(8):
   a=j*TAU/8;c=(j+1)*TAU/8
   p.append(s.rod('Curtain ring around rod',(xx,rod_y+.023*math.cos(a),2.09+.023*math.sin(a)),(xx,rod_y+.023*math.cos(c),2.09+.023*math.sin(c)),.003,m['steel'],5))
 b.export('level_bath_shower_rail' if rail else 'level_bath_shower',p,'household')

def wall_set():
 m=b.init();p=[s.box('Timber mirror surround',(0,-.018,1.65),(.55,.04,.68),m['oak'],.025),s.box('Muted silver mirror glass',(0,-.042,1.65),(.48,.006,.60),m['steel'],.02),s.box('Fixed toiletry shelf',(0,-.09,1.25),(.58,.19,.028),m['pine'],.008)]
 for x in [-.77,-.33]:p.append(s.rod('Towel rail bracket',(x,0,.89),(x,-.09,.89),.012,m['brass'],8))
 p.append(s.rod('Towel bar',(-.78,-.09,.89),(-.32,-.09,.89),.012,m['brass'],8))
 p.append(s.mesh('Draped hand towel',[(x-.55,y,z) for x,y,z in [(-.14,-.10,.88),(.14,-.10,.88),(.13,-.12,.52),(-.14,-.105,.55),(-.14,-.07,.86),(.14,-.07,.86),(.14,-.06,.58),(-.14,-.055,.61)]],[(0,1,2,3),(4,5,1,0),(4,7,6,5)],m['cloth']))
 p += [s.lathe('Toothbrush tumbler',[(1.27,.035),(1.375,.035),(1.38,.030),(1.28,.027)],m['ivory'],12,(-.17,-.10,0)),s.box('Soap dish',(.09,-.10,1.28),(.13,.08,.02),m['ivory'],.012),s.box('Used soap cake',(.09,-.10,1.298),(.09,.05,.025),m['enamel'],.012)]
 for x in [-.18,-.16]:p.append(s.rod('Toothbrush handle',(x,-.10,1.325),(x+.01,-.10,1.485),.005,m['red'] if x<-.17 else m['blue'],6))
 b.export('level_bathroom_wall_set',p,'household')

def laundry_shelf():
 m=b.init();p=[s.box('Utility shelf',(0,-.12,1.25),(.75,.25,.035),m['pine'],.008)]
 for x in [-.29,.29]:p+=s.tube('Shelf wall bracket',[(x,0,1.06),(x,-.21,1.23),(x,0,1.23)],.012,m['iron'],6)
 for x,z in [(-.22,1.27),(-.02,1.27)]:
  p.append(s.lathe('Detergent bottle',[(z,.055),(z+.18,.055),(z+.21,.025),(z+.24,.025)],m['blue'] if x<-.1 else m['ivory'],12,(x,-.10,0)))
  p.append(s.lathe('Attached cap',[(z+.24,.028),(z+.26,.028)],m['red'],10,(x,-.10,0)))
 for i in range(3):p.append(s.box('Folded linen towel',(.23,-.10,1.29+i*.045),(.22,.17,.044),m['cloth'],.018))
 b.export('level_laundry_shelf',p,'household')

def caddy():
 m=b.init();p=[s.lathe('Paper stand base',[(0,.11),(.035,.13),(.06,.09)],m['iron'],16),s.rod('Paper stand',(-.04,0,.03),(-.04,0,.64),.012,m['steel'],10),s.rod('Attached paper axle',(-.04,0,.62),(.09,0,.62),.009,m['steel'],8),s.rod('Toilet paper roll',(-.025,0,.62),(.07,0,.62),.055,m['paper'],16)]
 p.append(s.lathe('Toilet brush holder',[(0,.065),(.17,.065),(.19,.05)],m['ivory'],12,(.10,.10,0)))
 p.append(s.rod('Brush handle',(.10,.10,.13),(.10,.10,.40),.008,m['pine'],8))
 b.export('level_bathroom_caddy',p,'household')

def clothesline():
 m=b.init();p=[]
 for x in [-1.8,1.8]:
  p.append(s.rod('Grounded washing pole',(x,0,0),(x,0,1.96),.038,m['steel'],10));p.append(s.rod('Washing pole crossbar',(x,-.40,1.84),(x,.40,1.84),.018,m['steel'],8))
 for y in [-.29,.29]:
  points=[(-1.8+i*.15,y,1.84-.14*math.sin(i*math.pi/24)) for i in range(25)];p+=s.tube('Sagging clothes cord',points,.004,m['cloth'],5)
 for index,x in enumerate([-.95,-.15,.75]):
  y=-.29 if index!=1 else .29;top=1.84-.14*math.sin((x+1.8)/3.6*math.pi)
  verts=[(x-.22,y,top),(x+.22,y,top),(x+.20,y+.04,top-.65),(x-.21,y-.025,top-.69)]
  p.append(s.mesh('Hanging household linen',verts,[(0,1,2,3)],m['cloth'] if index==1 else m['ivory']))
  for xx in [x-.17,x+.17]:p.append(s.box('Attached wooden clothes peg',(xx,y,top),(.024,.028,.07),m['pine'],.004))
 b.export('level_yard_clothesline',p,'household',view=(3,-4,2.7))

def urinal():
 m=b.init();p=[]
 # Elliptic porcelain bowl projects from its mounting plane; open centre remains visible.
 rings=[(.10,.07,.42),(.18,.12,.49),(.18,.14,.76),(.15,.08,1.0),(.12,.055,.98),(.13,.09,.73),(.10,.065,.52)]
 verts=[(rx*math.cos(i*math.pi/24),-.03-ry*math.sin(i*math.pi/24),z) for rx,ry,z in rings for i in range(25)]
 faces=[(j*25+i,j*25+i+1,(j+1)*25+i+1,(j+1)*25+i) for j in range(len(rings)-1) for i in range(24)]
 p.append(s.mesh('Concave wall urinal',verts,faces,m['ivory']));p.append(s.box('Porcelain wall back',(0,.0,.73),(.34,.045,.54),m['ivory'],.025))
 p+=s.tube('Chrome flush pipe',[(0,-.03,.97),(0,-.03,1.22),(0,0,1.22)],.014,m['steel'],8)
 p.append(s.rod('Flush valve',(-.04,-.035,1.13),(.04,-.035,1.13),.02,m['steel'],8))
 b.export('level_school_urinal',p,'household')

def smoke_alarm():
 m=b.init();p=[s.rod('Wall detector mounting disc',(0,0,.09),(0,-.035,.09),.078,m['ivory'],24),s.rod('Bevelled detector body',(0,-.034,.09),(0,-.058,.09),.068,m['enamel'],24)]
 for i in range(12):
  a=i*TAU/12;x=.047*math.cos(a);z=.09+.047*math.sin(a);p.append(s.box('Smoke chamber vent',(x,-.060,z),(.006,.003,.014),m['black'],.002))
 p.append(s.rod('Test and hush key',(0,-.059,.09),(0,-.064,.09),.017,m['ivory'],12));p.append(s.box('Indicator lens',(.032,-.063,.10),(.007,.003,.007),m['red'],.002))
 b.export('level_event_smoke_alarm',p,'events')

if __name__=='__main__':
 selected=set(sys.argv[sys.argv.index('--')+1:]) if '--' in sys.argv else set()
 tasks={'washer':washer,'hamper':hamper,'shower':shower,'bath_rail':lambda:shower(True),'wall_set':wall_set,'shelf':laundry_shelf,'caddy':caddy,'clothesline':clothesline,'urinal':urinal,'smoke_alarm':smoke_alarm}
 for name,fn in tasks.items():
  if not selected or name in selected:fn()
 path=ROOT/'development/levels/lived-assets.json';doc=json.loads(path.read_text());ids={r['id'] for r in b.LOG};doc['assets']=[r for r in doc['assets'] if r['id'] not in ids]+b.LOG;path.write_text(json.dumps(doc,indent=2)+'\n')
