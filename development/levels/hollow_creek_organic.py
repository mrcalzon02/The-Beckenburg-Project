"""Desquareified landscape overlay: physical foundations only, organic clearings.
Original terrain/layout checkpoints remain unchanged. These routes/contours are
derived construction authority pending independent triangle and runtime review.
"""
import math,json
from pathlib import Path
import hollow_creek_ground as old
ROOT=Path(__file__).resolve().parents[2]
WEST=[[5,12],[5,20],[3.5,29],[4,36],[2,44],[-.5,50],[-.5,59],[6,65],[17,67],[24,72],[25,81],[28,85]]
EAST=[[36,85],[39,81],[40,72],[44,66],[58,63],[59,55],[59,48],[55.5,44],[56.5,35],[57.5,28],[59,24],[58.8,17],[56.5,9],[54.8,6]]
LAND=WEST+EAST+[[45,2],[34,1],[23,3],[14,8]]
ELLIPSES=[dict(id='van-roadhead',center=[32,78],radii=[4.6,5.7],y=1.5),dict(id='ranger-clearing',center=[15,54],radii=[4.8,4.1],y=.5),dict(id='bath-clearing',center=[49,54],radii=[4.3,3.9],y=.3),dict(id='store-clearing',center=[5,53],radii=[2,2.5],y=.45),dict(id='west-table',center=[16.45,32.3],radii=[1.0,1.1],y=-.35),dict(id='west-fire-ring',center=[10,41.5],radii=[1.35,1.35],y=-.05)]
def inside(x,z,polygon=LAND):
 hit=False
 for (a,b),(c,d) in zip(polygon,polygon[1:]+polygon[:1]):
  if (b>z)!=(d>z) and x<(c-a)*(z-b)/(d-b)+a:hit=not hit
 return hit
def segment_distance(x,z,a,b):
 dx=b[0]-a[0];dz=b[1]-a[1];t=max(0,min(1,((x-a[0])*dx+(z-a[1])*dz)/(dx*dx+dz*dz or 1)))
 return math.hypot(x-a[0]-t*dx,z-a[1]-t*dz)
def keep(x,z):
 # Land follows the playable lobes and narrow arrival neck. Water beds continue
 # separately into the lake rather than being mistaken for a rectangular yard.
 if z<old.shoreline(x):return -2<x<64 and -15<z<8
 return inside(x,z) or min(segment_distance(x,z,a,b) for a,b in zip(LAND,LAND[1:]+LAND[:1]))<3.0
def height(x,z,links):
 base=.04*(z-42)+.12+.30*math.sin(x*.11)*math.cos(z*.08)+.65*math.exp(-((x-60)/10)**2-((z-65)/20)**2)
 for pad in ELLIPSES:
  q=math.hypot((x-pad['center'][0])/pad['radii'][0],(z-pad['center'][1])/pad['radii'][1]);weight=1-old.smooth((q-.9)/.75);base=base*(1-weight)+pad['y']*weight
 gap,profile,dist=old.closest(x,z,links);shoulder=1-old.smooth((gap-.85)/1.2);base=max(base,base+(profile+.12-base)*shoulder)
 weight=1-old.smooth((gap-1.0)/1.0);depth=.12
 # Fade worn soil into actual built slab, not the whole rectangular zone.
 physical=old.rect_distance(x,z,[25,35,39,49]);vanq=math.hypot((x-32)/4.6,(z-78)/5.7);depth*=old.smooth(min(physical,max(0,(vanq-.9)*4))/2)
 result=base*(1-weight)+(profile+depth)*weight-depth*(1-old.smooth((dist-.45)/1.25))
 for pad in ELLIPSES:
  q=math.hypot((x-pad['center'][0])/pad['radii'][0],(z-pad['center'][1])/pad['radii'][1])
  if q<.88 and pad['id'] in ('van-roadhead','west-table','west-fire-ring'):result=pad['y']
 if 25<=x<=39 and 35<=z<=49:result=0
 if z<old.shoreline(x):result=min(result,-1.3-1.8*old.smooth((old.shoreline(x)-z)/4))
 if -2<=z<=28:
  channel=1-old.smooth((abs(x-old.creek_center(z))-1.25)/1.75);result=min(result,(-2.55-.018*(28-z))*channel*old.smooth((28-z)/2))
 if 25.5<=x<=30.5 and 4<=z<=7:
  w=1-old.smooth((z-4)/3);result=result*(1-w)-1.3*w
 if 11.5<=z<=14.5:
  distance=43-x if x<43 else x-53 if x>53 else 0
  if 0<distance<3:w=1-old.smooth(distance/3);result=result*(1-w)-1*w
 for deck in old.DECKS:
  if old.rect_distance(x,z,deck['bounds'])>1e-7:continue
  a,b,c,e=deck['bounds'];result=min(result,deck['top']-deck['depth'])
  if deck['id']=='creek-crossing-foundation':
   edge=min(x-a,c-x)
   if edge<=.5:result=deck['top']-deck['depth']*old.smooth(edge/.5)
  elif e-z<=.5:result=deck['top']-deck['depth']*old.smooth((e-z)/.5)
 return result
def make():
 d=json.loads((ROOT/'development/levels/hollow-creek/terrain.json').read_text(encoding='utf-8'));d['status']='desquareified native construction contract; runtime pending';d['organic_clearings']=ELLIPSES;d['boundary_paths']=[dict(id='west-woodland',points=WEST),dict(id='east-service',points=EAST)];d['land_outline']=LAND;d['render_floor_cutout']=[25,35,39,49];d['original_level_pads_reference_only']=d.pop('level_pads');d['level_pads']=[([25,35,39,49],0)];d['limitations']+=['Only the physical 14m pavilion slab remains rectangular. Unbuilt building-zone rectangles are no longer baked ground pads.','Organic terrain footprint and construction boundaries supersede the loose rectangular draft only in this derived scene; full-width grade validation and runtime integration remain required.'];return d
