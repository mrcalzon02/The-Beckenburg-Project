"""Original CC0 enclosure modules and inherited belongings; native metres, no imported art."""
import bpy,sys,math,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/assets'),str(ROOT/'development/levels')]
import rebuild_support as s
import build_lived_assets as b

def fence(kind):
 m=b.init();p=[]
 if kind=='garden':
  for x in [-.96,.96]:p.append(s.box('Fence mortised post',(x,0,.98),(.08,.09,1.96),m['oak'],.008))
  for z in [.38,1.48]:p.append(s.box('Back cross rail',(0,.025,z),(1.92,.055,.09),m['oak'],.006))
  for i in range(16):
   x=-.90+i*.12;h=1.79+.025*math.sin(i*2)
   v=[(x-.055,-.035,0),(x+.055,-.035,0),(x+.055,-.035,h-.08),(x,-.035,h),(x-.055,-.035,h-.08)]
   vv=v+[(a,.005,c) for a,y,c in v];p.append(s.mesh('Individual weatherboard paling',vv,[(0,1,2,3,4),(9,8,7,6,5)]+[(j,(j+1)%5,(j+1)%5+5,j+5) for j in range(5)],m['pine']))
 elif kind=='field':
  for x in [-.96,.96]:p.append(s.box('Field fence post',(x,0,.70),(.09,.11,1.40),m['oak'],.012))
  for z in [.40,.91,1.27]:p.append(s.box('Rough sawn fence rail',(0,-.035,z),(1.94,.075,.105),m['pine'],.009))
  for z in [.20,.65,1.12]:p.append(s.rod('Taut fence wire',(-1,.035,z),(1,.035,z),.003,m['rust'],5))
 else:
  p=[s.box('Capped masonry base',(0,0,.25),(2,.26,.50),m['rust'],.012),s.box('Stone coping',(0,0,.52),(2,.30,.07),m['steel'],.012)]
  for x in [-.94,.94]:p.append(s.box('Square iron pier',(x,0,1.16),(.07,.07,1.28),m['iron'],.005))
  for z in [.72,1.60]:p.append(s.box('Welded cross rail',(0,0,z),(2,.035,.045),m['iron'],.003))
  for i in range(13):
   x=-.84+i*.14;p.append(s.rod('Vertical wrought iron paling',(x,0,.55),(x,0,1.78),.009,m['iron'],6));p.append(s.lathe('Forged pointed finial',[(1.75,.018),(1.81,.025),(1.87,.001)],m['iron'],6,(x,0,0)))
 b.export('level_enclosure_'+kind,p,'enclosures')

def tree(conifer):
 m=b.init();p=[]
 p+=s.tube('Tapering irregular trunk',[(0,0,0),(.05,.02,2.5),(-.08,.02,4.6),(0,0,6.4)],.15,m['oak'],8)
 if conifer:
  for h,r in [(2.3,1.65),(3.4,1.4),(4.5,1.12),(5.6,.80)]:
   p.append(s.lathe('Layered low-poly conifer crown',[(h,r*.7),(h+.18,r),(h+1.9,.015)],m['green'],9))
 else:
  for i in range(6):
   a=i*math.tau/6;x=math.cos(a)*1.1;y=math.sin(a)*1.1;z=3.7+(i%3)*.55
   p+=s.tube('Growing attached branch',[(0,0,z-.5),(x*.6,y*.6,z+.2),(x,y,z+.55)],.075,m['oak'],7)
   bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1,radius=1,location=(x,y,z+1.0));o=bpy.context.object;o.name='Faceted irregular leaf canopy';o.scale=(1.45,1.32,1.48);o.data.materials.append(m['green']);p.append(o)
 b.export('level_tree_conifer' if conifer else 'level_tree_broadleaf',p,'enclosures',view=(7,-10,6))

def letters():
 m=b.init();p=[]
 for i in range(9):
  o=s.box('Uneven folded envelope',((i%3)*.005,math.sin(i)*.012,.002+i*.003),(.26,.18,.003),m['paper']);o.rotation_euler.z=.035*math.sin(i);p.append(o)
 # Fold and address are actual restrained geometry, rather than illegible neon scribbles.
 p+=s.tube('Envelope flap seam',[(-.13,.08,.029),(0,-.01,.029),(.13,.08,.029)],.001,m['oak'],4)
 for j,w in enumerate([.09,.13,.07]):p.append(s.box('Faded address line',(.025,-.035+j*.016,.030),(w,.002,.001),m['oak']))
 for x in [-.018,.018]:p+=s.tube('Parcel binding twine',[(x,-.091,.001),(x,-.091,.034),(x,.092,.034),(x,.092,.001)],.0018,m['cloth'],5)
 b.export('level_story_letters',p,'household')

def sewing():
 m=b.init();p=[s.box('Old rounded sewing tin',(0,0,.042),(.25,.17,.084),m['blue'],.024),s.box('Loose embossed tin lid',(.19,.025,.008),(.25,.17,.016),m['blue'],.018)]
 for x in [-.07,.015]:
  p.append(s.lathe('Thread bobbin',[(.085,.022),(.09,.029),(.10,.021),(.15,.021),(.155,.029),(.16,.029)],m['pine'],12,(x,0,0)))
  p.append(s.lathe('Wound thread',[(.102,.024),(.15,.024)],m['cloth'],12,(x,0,0)))
 for x in [.04,.09]:p+=b.ring('Scissors finger ring',(x,-.045,.093),.019,.004,m['steel'],axis='Z',segments=10)
 p.append(s.rod('Scissors first blade',(.05,-.03,.094),(.105,.07,.094),.004,m['steel'],5));p.append(s.rod('Scissors second blade',(.09,-.03,.095),(.045,.07,.095),.004,m['steel'],5))
 for i in range(4):p.append(s.rod('Needle and mending pin',(-.08+i*.015,-.025,.167),(-.075+i*.015,.03,.167),.001,m['steel'],5))
 b.export('level_story_sewing_tin',p,'household')

def boots():
 m=b.init();p=[]
 for x in [-.11,.11]:
  # Multiple elliptical cross sections give an actual toe, instep and open boot cuff.
  sections=[(.025,.09,.16,-.025),(.065,.087,.16,-.025),(.12,.08,.14,-.02),(.18,.072,.09,.025),(.30,.064,.074,.05),(.32,.066,.075,.05),(.32,.053,.061,.05),(.20,.047,.053,.05)]
  v=[(x+rx*math.cos(j*math.tau/16),cy+ry*math.sin(j*math.tau/16),z) for z,rx,ry,cy in sections for j in range(16)]
  f=[(i*16+j,i*16+(j+1)%16,(i+1)*16+(j+1)%16,(i+1)*16+j) for i in range(len(sections)-1) for j in range(16)]
  p.append(s.mesh('Shaped leather boot upper',v,f,m['leather']))
  p.append(s.lathe('Rubber sole',[(.001,.09),(.025,.095),(.04,.09)],m['black'],16,(x,-.02,0)))
  for j in range(5):p.append(s.rod('Crossed boot lace',(x-.034,-.08,.14+j*.024),(x+.034,-.085,.158+j*.024),.002,m['cloth'],5))
 b.export('level_story_work_boots',p,'household')

# Original simple 5x7 construction alphabet; no font files or third-party artwork.
GLYPHS={
'A':['01110','10001','10001','11111','10001','10001','10001'],'B':['11110','10001','10001','11110','10001','10001','11110'],'C':['01111','10000','10000','10000','10000','10000','01111'],'D':['11110','10001','10001','10001','10001','10001','11110'],'E':['11111','10000','10000','11110','10000','10000','11111'],'F':['11111','10000','10000','11110','10000','10000','10000'],'G':['01111','10000','10000','10111','10001','10001','01110'],'H':['10001','10001','10001','11111','10001','10001','10001'],'I':['11111','00100','00100','00100','00100','00100','11111'],'J':['00111','00010','00010','00010','10010','10010','01100'],'K':['10001','10010','10100','11000','10100','10010','10001'],'L':['10000','10000','10000','10000','10000','10000','11111'],'M':['10001','11011','10101','10101','10001','10001','10001'],'N':['10001','11001','10101','10011','10001','10001','10001'],'O':['01110','10001','10001','10001','10001','10001','01110'],'P':['11110','10001','10001','11110','10000','10000','10000'],'Q':['01110','10001','10001','10001','10101','10010','01101'],'R':['11110','10001','10001','11110','10100','10010','10001'],'S':['01111','10000','10000','01110','00001','00001','11110'],'T':['11111','00100','00100','00100','00100','00100','00100'],'U':['10001','10001','10001','10001','10001','10001','01110'],'V':['10001','10001','10001','10001','10001','01010','00100'],'W':['10001','10001','10001','10101','10101','11011','10001'],'X':['10001','10001','01010','00100','01010','10001','10001'],'Y':['10001','10001','01010','00100','00100','00100','00100'],'Z':['11111','00001','00010','00100','01000','10000','11111'],
'0':['01110','10001','10011','10101','11001','10001','01110'],'1':['00100','01100','00100','00100','00100','00100','01110'],'2':['01110','10001','00001','00010','00100','01000','11111'],'3':['11110','00001','00001','01110','00001','00001','11110'],'4':['00010','00110','01010','10010','11111','00010','00010'],'5':['11111','10000','10000','11110','00001','00001','11110'],'6':['01110','10000','10000','11110','10001','10001','01110'],'7':['11111','00001','00010','00100','01000','01000','01000'],'8':['01110','10001','10001','01110','10001','10001','01110'],'9':['01110','10001','10001','01111','00001','00001','01110']}
def sign(name,lines):
 m=b.init();p=[s.box('Shaped timber name board',(0,-.02,.18),(1.68,.04,.36),m['oak'],.025),s.box('Recessed painted plaque',(0,-.043,.18),(1.58,.008,.29),m['blue'],.012)]
 for x in [-.78,.78]:
  for z in [.055,.305]:p.append(s.rod('Recessed fixing screw',(x,-.047,z),(x,-.053,z),.008,m['steel'],8))
 # Fit the complete glyph block inside the inset plaque, including its lowest pixels.
 # The previous fixed baseline placed the third line below the painted board.
 verts=[];faces=[];gap=.024;text_top=.305;text_bottom=.055
 unit=min(.011,1.44/max(1,max(len(t)*6-1 for t in lines)),(text_top-text_bottom-gap*(len(lines)-1))/(7*len(lines)))
 for line,text in enumerate(lines):
  left=-(len(text)*6-1)*unit/2;top=text_top-line*(7*unit+gap)
  for char_index,char in enumerate(text):
   for row,bits in enumerate(GLYPHS.get(char,[])):
    for col,bit in enumerate(bits):
     if bit=='1':
      x=left+(char_index*6+col)*unit;z=top-row*unit;v=len(verts);verts.extend([(x,-.0472,z),(x+unit*.91,-.0472,z),(x+unit*.91,-.0472,z-unit*.91),(x,-.0472,z-unit*.91)]);faces.append((v,v+1,v+2,v+3))
 assert all(-.73<=x<=.73 and text_bottom-.001<=z<=text_top+.001 for x,y,z in verts),'Sign text outside its plaque'
 p.append(s.mesh('Original painted lettering',verts,faces,m['ivory']))
 b.export('level_sign_'+name,p,'enclosures')
 b.LOG[-1]['lettering_bounds']=[[min(v[i] for v in verts) for i in range(3)],[max(v[i] for v in verts) for i in range(3)]]

if __name__=='__main__':
 if '--signs-only' not in sys.argv:
  for k in ['garden','field','urban']:fence(k)
  tree(True);tree(False);letters();sewing();boots()
 for name,lines in [('house',['THE BRIAR HOUSE','17 BRIAR LANE','EST 1886']),('farmhouse',['HOLLOWMERE FARM','4 MILL ROAD','EST 1798']),('school',['RAVENSHADE SCHOOL','8 SCHOOL ROAD','EST 1894']),('mens',['MENS','WASHROOM']),('womens',['WOMENS','WASHROOM'])]:sign(name,lines)
 path=ROOT/'development/levels/lived-assets.json';doc=json.loads(path.read_text());ids={r['id'] for r in b.LOG};doc['assets']=[r for r in doc['assets'] if r['id'] not in ids]+b.LOG;path.write_text(json.dumps(doc,indent=2)+'\n')
