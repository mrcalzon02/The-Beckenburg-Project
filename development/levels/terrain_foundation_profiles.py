"""Site-specific original landforms for the 16 unrefined rough layouts.
Profiles are fictional metre-based authoring choices, not geographic surveys.
"""
import math

def clamp(t):return max(0,min(1,t))
def smooth(t):t=clamp(t);return t*t*(3-2*t)
def bump(x,z,cx,cz,rx,rz):return math.exp(-((x-cx)/rx)**2-((z-cz)/rz)**2)
PROFILES={
'academy':dict(label='Cloister ridge and drained quadrangle',color=(.24,.28,.18),deck=[],below=['archive'],note='Ground ranges and quadrangle sit on a benched ridge; archive remains a separate below-grade layer.'),
'asylum':dict(label='Stepped hospital garden slopes',color=(.21,.27,.18),deck=[],below=['plant'],note='Day court falls toward the west drainage margin; stepped building foundations retain their source levels.'),
'college':dict(label='Campus green and service-tunnel cover',color=(.20,.28,.17),deck=['bridge'],below=['tunnel','tunnel-west'],note='Three foundation clusters share rolling green; tunnels are not collapsed onto the ground surface.'),
'mansion':dict(label='Manor terrace and descending service garden',color=(.23,.28,.16),deck=[],below=['cellar'],note='Reception terrace, lower garden swale and service-side bank follow the asymmetric plan.'),
'prison':dict(label='Excavated yard and perimeter earthwork',color=(.29,.29,.25),deck=[],below=['plant'],note='Existing ground pads remain engineered; outer earthwork rises around the bounded exercise complex.'),
'wld-03':dict(label='Red Mesa canyon and 4/8m cliff terraces',color=(.49,.26,.14),deck=[],below=['lower-cave'],note='Canyon floor, four-metre ledge and eight-metre upper approach are distinct; stacked caves require excavation/headroom review.'),
'wld-04':dict(label='Lookout hillside and contour return',color=(.24,.30,.17),deck=['landing','cab','deck'],below=[],note='Six-metre hill supports the tower foot; upper tower floors do not raise the ground to fifteen metres.'),
'wld-05':dict(label='Mine ridge with layered adit/drift levels',color=(.31,.29,.23),deck=[],below=['haul','ore-west','ore-east','cross','lift','lower','vent'],note='Surface mine ridge retains rock cover over -6/-10m workings; excavation/stair geometry remains separately visible in study views.'),
'wld-06':dict(label='Tidal shore and piled wharf foundations',color=(.28,.29,.23),deck=['pier-w','pier-e','crossdock','boathouse','net','fish','undercroft','west-stair','east-stair'],below=['undercroft'],water=-3.1,north_water=36,note='Water stays below the dry -2.5m undercroft; piers/buildings over water receive pile foundation reservations.'),
'wld-07':dict(label='Rolling burial ground and ossuary cover',color=(.24,.29,.19),deck=[],below=['ossuary','chamber'],note='Burial terraces retain rolling ground between worn paths; underground routes stay on a separate layer.'),
'wld-08':dict(label='Snow shelf, wind drifts and tunnel cover',color=(.69,.74,.75),deck=[],below=['tunnel','tunnel-n'],note='Insulated prefab foundations and flagged court sit between wind-shaped drifts; snow must not fill tunnel approaches.'),
'wld-09':dict(label='Dam abutment, six-metre cut and tailrace',color=(.32,.34,.31),deck=['crest','east-stair'],below=['gallery','valve','return'],water=-8.2,water_rect=[65,4,80,22],note='Crest datum, -6m powerhouse excavation and -10m lower gallery remain separate; retaining faces and guarded descents are still required.'),
'wld-10':dict(label='Monastery high garden and service banks',color=(.30,.32,.22),deck=['bell'],below=['catacombs','chamber'],note='The cloister garden remains open; bell floor and catacombs do not replace terrain with a single elevation.'),
'wld-11':dict(label='Marsh basin and raised boardwalk foundations',color=(.22,.27,.17),deck=['main','service','south-deck','north-deck','house-a','house-b','chapel','warehouse','shack','ferry','pump'],below=[],water=-.1,north_water=62,note='Do not fill the wetland to deck height: +1.2m routes remain on supported foundations above the flood plane.'),
'wld-12':dict(label='Rail cutting, trestle ravine and tunnel cover',color=(.31,.30,.24),deck=['trestle','lower','crossing'],below=['tunnel','refuge'],note='Cutting banks, trestle ravine and lower walkway have independent supports; tunnel roof/rock is not an open field.'),
'wld-13':dict(label='Cannery tidal bank and forest-side slope',color=(.24,.29,.22),deck=['dock'],below=[],water=-1.0,north_water=14,note='Dry industrial foundations meet a piled shipping edge; woodland ground rises west of the workers housing.')
}
def base(key,x,z):
 if key=='academy':return .026*(60-z)+.75*bump(x,z,4,8,16,22)-.65*bump(x,z,35,33,16,18)
 if key=='asylum':return .024*(x-30)+.70*bump(x,z,52,10,18,18)-.60*bump(x,z,2,53,14,18)
 if key=='college':return .022*(76-z)+.60*math.sin(x*.08)*math.cos(z*.055)+.8*bump(x,z,70,12,16,20)
 if key=='mansion':return .03*(62-z)+.95*bump(x,z,49,8,13,22)-.8*bump(x,z,13,20,15,14)
 if key=='prison':return .3+.025*(76-z)+2.2*(1-bump(x,z,37,43,31,39))-.45*bump(x,z,34,40,16,19)
 if key=='wld-03':return 8*smooth((65-z)/43)+5.5*(1-bump(x,z,39,45,33,44))+.45*math.sin(x*.15+z*.12)
 if key=='wld-04':return 6*smooth((70-z)/43)+1.8*bump(x,z,47,9,17,18)-.6*bump(x,z,8,56,16,24)
 if key=='wld-05':return .095*(76-z)+1.5*bump(x,z,33,19,23,24)+.3*math.sin(x*.2)*math.cos(z*.09)
 if key=='wld-06':return .018*(z-54)+.55*math.sin(x*.10)*math.cos(z*.06)
 if key=='wld-07':return .020*(70-z)+.6*math.sin(x*.11)*math.cos(z*.09)-.3*bump(x,z,14,29,12,12)
 if key=='wld-08':return .8+.8*math.sin(x*.10+z*.055)**2+.5*bump(x,z,64,9,14,16)-.65*bump(x,z,33,39,15,20)
 if key=='wld-09':return -6*smooth((54-z)/10)+1.8*bump(x,z,10,12,16,26)+1.2*bump(x,z,76,63,10,18)
 if key=='wld-10':return .036*(74-z)+.9*bump(x,z,-6,14,18,22)-.55*bump(x,z,30,35,16,22)
 if key=='wld-11':return -.9+1.0*smooth((z-59)/13)+.16*math.sin(x*.17)*math.cos(z*.08)
 if key=='wld-12':return 3.2*(1-bump(x,z,34,56,12,42))-5*bump(x,z,34,32,11,8)+.8*bump(x,z,32,5,12,18)
 if key=='wld-13':return .018*(z-68)+1.8*bump(x,z,-14,30,14,28)+.25*math.sin(x*.13+z*.08)
 raise KeyError(key)
