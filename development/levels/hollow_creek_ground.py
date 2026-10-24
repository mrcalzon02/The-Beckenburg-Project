"""Metre-based Hollow Creek site authority. No random seeds or imported terrain."""
import math,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
LAYOUT=ROOT/'development/levels/layouts/wld-02/layout.json'
BOUNDS=(-4,-16,64,90);STEP=.625;WATER=-1.95
DECKS=[dict(id='dock-foundation',bounds=[25.5,-10,30.5,4],top=-1.3,depth=.18),dict(id='creek-crossing-foundation',bounds=[43,11.5,53,14.5],top=-1.0,depth=.18)]
PAVILION=(24,34,40,50)
def distance(x,z,a,b):
 dx=b[0]-a[0];dz=b[2]-a[2];n=dx*dx+dz*dz;t=max(0,min(1,((x-a[0])*dx+(z-a[2])*dz)/n)) if n else 0
 return math.hypot(x-a[0]-t*dx,z-a[2]-t*dz)
def shoreline(x):return 4+.9*math.sin((x-28)*.18)+.35*math.sin((x-28)*.47)
def creek_center(z):return 48+.65*math.sin((z-13)*.13)
def smooth(t):
 t=max(0,min(1,t));return t*t*(3-2*t)
def rect_distance(x,z,rect):
 a,b,c,d=rect;return math.hypot(max(a-x,0,x-c),max(b-z,0,z-d))
# Only actual foundations are leveled. Open outdoor zones keep their topography.
LEVELS={'stage':1.5,'entry':1.0,'ranger':.5,'bath':.3,'pavilion':0,'camp-a':-.28,'camp-b':.10,'junction':-.65,'camper':-.95,'shore':-1.3,'dock':-1.3,'creek':-1.0,'store':.45}
PADS=[([24,72,40,84],1.5),([8,48,22,60],.5),([42,48,56,60],.3),([24,34,40,50],0),([2,48,8,58],.45),([15.8,31.45,17.1,33.15],-.35),([7.06,30.06,11.94,34.94],-.40),([9,40.5,11,42.5],-.05)]
def pad_height(x,z):
 for rect,y in PADS:
  if rect_distance(x,z,rect)<1e-7:return y
 return None
def routes(layout):
 out=json.loads(json.dumps(layout['links']))
 for l in out:
  if l['id']=='L13':l['points']=[[32,0,12],[41,0,13],[48,0,13]]
  if l['id']=='L09':l['points']=[[32,0,27],[23,0,29],[14,0,37]]
  if l['id']=='L06':l['points']=[[32,0,42],[40,0,44],[44,0,48],[49,0,54]]
  if l['id']=='L14':
   l['points']=[[48,0,13],[55,0,13],[55,0,26],[50,0,37]]
   l['refinement']='Exit the crossing on the east bank before returning to east camp.'
  pts=l['points'];lengths=[math.hypot(b[0]-a[0],b[2]-a[2]) for a,b in zip(pts,pts[1:])];total=sum(lengths);travel=0;new=[]
  for index,(a,b) in enumerate(zip(pts,pts[1:])):
   dx,dz=b[0]-a[0],b[2]-a[2];cuts={0.,1.}
   for rect,level in PADS:
    for axis,low,high in [(0,rect[0],rect[2]),(2,rect[1],rect[3])]:
     delta=b[axis]-a[axis]
     if abs(delta)>1e-9:
      for edge in [low,high]:
       t=(edge-a[axis])/delta
       if 0<t<1 and rect_distance(a[0]+dx*t,a[2]+dz*t,rect)<1e-6:cuts.add(t)
   for t in sorted(cuts):
    x,z=a[0]+dx*t,a[2]+dz*t;f=(travel+lengths[index]*t)/total;y=LEVELS[l['a']]*(1-f)+LEVELS[l['b']]*f
    fixed=pad_height(x,z)
    if fixed is not None:y=fixed
    if l['id'] in ['L13','L14'] and 43<=x<=53 and 11.5<=z<=14.5:y=-1.0
    if l['id']=='L14' and abs(z-13)<1e-6 and x<=55:y=-1.0
    if not new or math.hypot(x-new[-1][0],z-new[-1][2])>1e-7:new.append([round(x,6),round(y,6),round(z,6)])
   travel+=lengths[index]
  l['points']=new
 return out
def closest(x,z,links):
 candidates=[]
 for link in links:
  for a,b in zip(link['points'],link['points'][1:]):
   dx,dz=b[0]-a[0],b[2]-a[2];n=dx*dx+dz*dz;t=max(0,min(1,((x-a[0])*dx+(z-a[2])*dz)/n)) if n else 0
   dist=math.hypot(x-a[0]-t*dx,z-a[2]-t*dz);gap=dist-link['width']/2
   candidates.append((gap,a[1]*(1-t)+b[1]*t,dist))
 nearest=min(candidates,key=lambda q:q[0]);weighted=[]
 for gap,y,dist in candidates:
  if gap>nearest[0]+2 or gap>3:continue
  weight=(1-smooth((gap-nearest[0])/2))/(dist*dist+1)
  weighted.append((weight,y))
 profile=sum(w*y for w,y in weighted)/sum(w for w,y in weighted) if weighted else nearest[1]
 return nearest[0],profile,nearest[2]
def height(x,z,layout,links):
 # The road is upland; terrain drops through camp toward the lake. Low rolling
 # ground survives between trails instead of flattening entire outdoor zones.
 base=.04*(z-42)+.12+.30*math.sin(x*.11)*math.cos(z*.08)+.65*math.exp(-((x-60)/10)**2-((z-65)/20)**2)
 for rect,level in PADS:
  proximity=rect_distance(x,z,rect);feather=.30 if min(rect[2]-rect[0],rect[3]-rect[1])<3 else 1.25;weight=1-smooth(proximity/feather)
  base=base*(1-weight)+level*weight
 gap,profile,dist=closest(x,z,links)
 shoulder=1-smooth((gap-.85)/1.2)
 base=max(base,base+(profile+.12-base)*shoulder)
 # The .5m triangulation needs a graded shoulder outside the tread, so a body
 # near its edge cannot land on a sharp foundation-to-path interpolation face.
 weight=1-smooth((gap-1.0)/1.0)
  # Worn soil fades into foundation/crossing landings, avoiding a raised lip
 # where a broad trail shoulder meets a rigid standing surface.
 pad_distance=min([rect_distance(x,z,rect) for rect,y in PADS]+[rect_distance(x,z,q['bounds']) for q in DECKS])
 depth=.12*smooth(pad_distance/2)
 result=base*(1-weight)+(profile+depth)*weight
 result-=depth*(1-smooth((dist-.45)/1.25))
 fixed=pad_height(x,z)
 if fixed is not None:result=fixed
 # Water cuts override path/pad grading; a bridge must carry the path.
 if z<shoreline(x):result=min(result,-1.3-1.8*smooth((shoreline(x)-z)/4))
 if -2<=z<=28:
  channel=1-smooth((abs(x-creek_center(z))-1.25)/1.75)
  result=min(result,(-2.55-.018*(28-z))*channel*smooth((28-z)/2))
 # Engineered crossing landings taper the worn tread into the deck surface.
 if 25.5<=x<=30.5 and 4<=z<=7:
  w=1-smooth((z-4)/3);result=result*(1-w)-1.3*w
 if 11.5<=z<=14.5:
  dist=43-x if x<43 else x-53 if x>53 else 0
  if 0<dist<3:
   w=1-smooth(dist/3);result=result*(1-w)-1.0*w
 # Grade only the land abutments. Do not fill the creek/lake bed beneath a deck.
 for deck in DECKS:
  if rect_distance(x,z,deck['bounds'])>1e-7:continue
  a,b,c,e=deck['bounds']
  if deck['id']=='creek-crossing-foundation':
   edge=min(x-a,c-x)
   if edge<=.5:result=deck['top']-deck['depth']*smooth(edge/.5)
  elif e-z<=.5:result=deck['top']-deck['depth']*smooth((e-z)/.5)
 return round(result,6)
def make():
 layout=json.loads(LAYOUT.read_text(encoding='utf-8'));links=routes(layout)
 x0,z0,x1,z1=BOUNDS
 xs=sorted(set([x0+i*STEP for i in range(int((x1-x0)/STEP)+1)]+[x1,24,40]+[v for q in DECKS for v in [q['bounds'][0],q['bounds'][2]]]))
 zs=sorted(set([z0+j*STEP for j in range(int((z1-z0)/STEP)+1)]+[z1,34,50]+[v for q in DECKS for v in [q['bounds'][1],q['bounds'][3]]]))
 nx,nz=len(xs),len(zs);heights=[height(x,z,layout,links) for z in zs for x in xs]
 return dict(status='site-foundation-review',origin='Original world X/Z and canonical van yaw; refined standing elevations in this contract',bounds=list(BOUNDS),step=STEP,nx=nx,nz=nz,xs=xs,zs=zs,heights=heights,water_y=WATER,routes=links,decks=DECKS,render_floor_cutout=list(PAVILION),staging=dict(layout['staging'],point=[32,1.5,78],cargo_route=[[32,1.5,74.372],[32,1.5,72],[32,1,66]]),zones=[dict(q,y=LEVELS[q['id']]) for q in layout['zones']],level_pads=PADS,facility_elevations={'west-table':-.35,'west-fire-ring':-.05,'pavilion-table-west':0,'pavilion-table-east':0},trail_incision_m=.12,site_perimeter=[[-1,84],[18,87],[31,88],[44,84],[59,78],[62,62],[60,47],[62,34],[59,24],[61,10],[56,3],[44,1],[34,2],[24,3],[16,8],[5,13],[1,25],[-1,42],[0,59],[-1,72]],boundary_palette=['hedges at managed edges','stone retaining banks at grade changes','fences around service areas','fallen-log barriers along woodland'],rituals=[dict(q,point=[q['point'][0],LEVELS[q['zone']],q['point'][2]]) for q in layout['rituals']],limitations=['Native surface/crossing foundation only; not yet wired into runtime collision, water restrictions or map ownership.','Crossing decks are infrastructure blockouts; guards, footing detail and final dock/bridge models remain pending.','Refined site elevations supersede rough Y0 reservations only in this derived contract; runtime integration remains pending.'])
if __name__=='__main__':
 target=ROOT/'development/levels/hollow-creek/terrain.json';target.write_text(json.dumps(make(),separators=(',',':'))+'\n',encoding='utf-8');print('Wrote deterministic site terrain contract')
