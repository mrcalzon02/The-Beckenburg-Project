"""Author non-destructive edge-blocking and sky-inclusion plans for all existing packets.
JSON/SVG/Markdown only: no runtime, navigation, geometry or original layout mutation.
North is source -Z (0 degrees); east +X (90), south +Z (180), west -X (270).
"""
from pathlib import Path
import json,math,hashlib,html
ROOT=Path(__file__).resolve().parents[2];LEVELS=ROOT/'development/levels';OUT=LEVELS/'edge-sky-plans'
# Authored N/E/S/W treatments and background identities. These are art-direction
# reservations, not measured terrain, completed collision or acquired sky textures.
P={
'house':('Residential lane and overgrown rear plot',('hedge','wall','gate','hedge'),('Rear garden trees and distant roof ridge','Neighbor chimney and brick boundary','Quiet lane receding between gardens','Uneven hedgerow and house gable'),('trees','roofs','roofs','trees')),
'farmhouse':('Farmyard, hedged fields and drainage banks',('logfall','bank','gate','hedge'),('Broken woodland behind the barn','Undulating pasture and far hedgerows','Farm lane and low field horizon','Orchard silhouettes over a stone bank'),('trees','fields','fields','trees')),
'school':('School enclosure and neighboring district',('fence','wall','gate','hedge'),('Playing-field tree belt','Adjacent brick institutional roofs','Approach lane and distant residences','Hedges and recreation-ground trees'),('trees','roofs','roofs','trees')),
'mansion':('Manor terraces and wooded estate limits',('bank','wall','gate','hedge'),('Estate ridge above the upper garden','Terraced stone boundary and far woodland','Formal drive disappearing into trees','Overgrown garden and orchard canopy'),('hills','trees','trees','trees')),
'prison':('Secure institution with outer earthworks',('secure','secure','gate','secure'),('Service ridge beyond secure perimeter','Distant industrial roofline','Vehicle approach and gatehouse corridor','Wooded outer bank beyond secure wall'),('hills','roofs','roofs','trees')),
'asylum':('Stepped hospital grounds and service enclosure',('bank','wall','gate','hedge'),('Wooded rise behind hospital wings','Service enclosure with distant institutional roofs','Hospital approach lined with trees','Descending garden and boundary tree belt'),('hills','roofs','trees','trees')),
'academy':('Cloister ridge, gardens and district skyline',('wall','bank','gate','hedge'),('Low ridge behind academy roofs','Sloped garden and far woodland','Institutional drive and district roof glimpses','Tree-framed cloister garden'),('hills','trees','roofs','trees')),
'college':('Campus perimeter with service access',('fence','bank','gate','hedge'),('Campus playing-field trees','Service road and district buildings','Entrance forecourt with distant campus blocks','Green slope and adjacent mature trees'),('trees','roofs','roofs','trees')),
'church':('Parish close, churchyard wall and village lane',('wall','hedge','gate','wall'),('Churchyard trees and low village ridge','Neighbor gardens over parish hedge','Village lane, roofline and porch arrival','Stone boundary with tree silhouettes'),('trees','trees','roofs','trees')),
'castle':('Castle grounds and outer landscape frame',('cliff','wall','gate','bank'),('Rocky wooded rise beyond the keep','Outer stonework against distant district roofs','Arrival road framed by low hills','Estate slope and woodland canopy'),('hills','roofs','hills','trees')),
'wld-01':('Logging clearing and irregular forest edge',('logfall','bank','gate','logfall'),('Dense conifers behind the mill','Rising forest bank and uneven trunks','Logging road recess beyond the van','Tangled logfall and layered conifers'),('trees','hills','trees','trees')),
'wld-02':('Camp woodland edge, open lake and creek corridor',('shore','bank','gate','logfall'),('Low open lake horizon and distant wooded shore','Creek-cut bank and woodland shoulder','Camp road curving back through conifers','Fallen timber and rising wooded ground'),('water','trees','trees','hills')),
'wld-03':('Canyon rim, recesses and distant mesa shelves',('cliff','cliff','gate','bank'),('Broken mesa skyline above canyon opening','Layered canyon rim and narrow sky slots','Access wash bending between red-rock shoulders','Eroded rock buttresses and far mesa'),('mesa','mesa','mesa','mesa')),
'wld-04':('High lookout landscape with long elevated views',('cliff','bank','gate','logfall'),('Multiple receding mountain ridges','Wind-shaped forest slope below the tower','Access track swallowed by woodland','Open valley framed by close conifers'),('mountains','hills','trees','mountains')),
'wld-05':('Mine surface ridge and sealed workings',('cliff','bank','gate','logfall'),('Rock spoil and wooded mine ridge','Steep uneven ground above adits','Old service track and far forest','Collapsed timber backed by natural rock'),('hills','hills','trees','trees')),
'wld-06':('Tidal wharf, shore and protected dock edges',('shore','shore','gate','bank'),('Open tidal channel and low opposite shore','Water vista beyond guarded pier edges','Shore road and weathered harbor buildings','Steep tidal bank with sparse trees'),('water','water','roofs','hills')),
'wld-07':('Cemetery perimeter and quiet woodland backdrop',('wall','hedge','gate','wall'),('Cemetery trees and distant parish roof','Mortuary expansion direction; removable boundary section','Cemetery lane and entrance pillars','Weathered stone wall and woodland'),('trees','trees','roofs','trees')),
'wld-08':('Research compound in wind-shaped snow terrain',('snowbank','secure','gate','snowbank'),('Snow ridge and restrained distant mountains','Fenced service edge against low white terrain','Flagged access track and far snow horizon','Wind-scoured bank and dark rock outcrops'),('snow','snow','snow','mountains')),
'wld-09':('Dam abutments, reservoir and tailrace',('shore','cliff','gate','bank'),('Reservoir surface and distant valley sides','Guarded tailrace and cut rock abutment','Service approach against high valley shoulder','Vegetated retaining bank and far ridge'),('water','hills','hills','hills')),
'wld-10':('Monastery wall, terraces and high garden views',('wall','cliff','gate','bank'),('Broken monastery roofline and far hills','Valley view beyond guarded terrace','Walled approach with a recessed service gate','Garden bank and layered cypress silhouettes'),('hills','mountains','trees','trees')),
'wld-11':('Marsh boardwalk enclave with genuine wetland depth',('shore','shore','gate','shore'),('Low reed islands and broad open wetland sky','Flooded timber and distant reed horizon','Raised causeway back to firm ground','Sparse swamp trees above open water'),('water','water','trees','trees')),
'wld-12':('Rail cutting, ravine and sealed tunnel ends',('cliff','bank','gate','cliff'),('Tunnel ridge and irregular rock skyline','Steep rail cutting and far valley','Service access and tracks lost around a bend','Guarded ravine framed by broken rock'),('hills','mountains','trees','mountains')),
'wld-13':('Shore cannery yard, tidal dock and forest slope',('shore','fence','gate','bank'),('Open working harbor and opposite shoreline','Service fence with distant industrial roofs','Cannery access lane and tree-framed road','Rising forest slope behind worker housing'),('water','roofs','trees','hills')),
'wld-14':('Lightstation headland with an exposed sea horizon',('cliff','cliff','gate','bank'),('Uninterrupted sea horizon beyond the lantern','Open sea with a very low distant headland','Keeper-house road recessed in coastal vegetation','Rock headland and low windswept land'),('water','water','hills','hills')),
'wld-15':('Moored vessel within a quayside investigation envelope',('shore','shore','gate','secure'),('Open harbor water and distant breakwater','Low open water vista beside guarded vessel rails','Quay access and distant warehouse silhouettes','Quay fence and industrial harbor backdrop'),('water','water','roofs','roofs')),
'wld-16':('Motor court roadside edge and service-bank backdrop',('bank','fence','gate','hedge'),('Low pine ridge behind guest rooms','Screened service edge and distant trees','Open roadside view between fictional sign and reception','Uneven hedge and scattered roadside trees'),('hills','trees','fields','trees')),
'wld-17':('Coastal battery berms, exposed sea and service road',('cliff','cliff','gate','bank'),('Open coastal horizon beyond the observation post','Low sea view between concrete and rock frames','Old military access road against wooded ridge','Earth-covered battery crest and far headland'),('water','water','hills','hills')),
'wld-18':('Decommissioned compound with service and escape access',('secure','bank','gate','secure'),('Low scrub ridge beyond compound fencing','Uneven maintenance bank and far woodland','Service road disappearing below a low ridge','Fenced escape-bank edge and sparse distant trees'),('hills','trees','hills','trees')),
'wld-07-mortuary':('Expanded cemetery and screened mortuary service boundary',('wall','hedge','gate','wall'),('Original cemetery tree frame and parish roof','Screened mortuary service edge with quiet distant trees','Lane access kept clear of reception and service circulation','Original cemetery wall and woodland frame'),('trees','trees','roofs','trees'))
}
MECHANISMS={
'hedge':dict(height=2.3,depth=2.6,visible='Dense uneven hedge with visible woody core; occasional stone base',collision='Full visible woody/core volume, no crouch-sized holes; verify both approach directions.'),
'wall':dict(height=2.0,depth=.6,visible='Stepped stone/brick boundary with piers and local bank support',collision='Closed wall solid following visible faces; low decorative sections require a backed bank or railing.'),
'gate':dict(height=2.2,depth=1.0,visible='Road-edge hedge/fence returns, posts and one closed out-of-area road portal',collision='Gate/side-return solids beyond staging. Arrival and cargo routes remain clear; no arbitrary invisible line.'),
'fence':dict(height=2.2,depth=.25,visible='Supported mesh/timber fence with grounded posts and braced corners',collision='Match visible fence envelope; avoid triangle/crouch gaps and terminate against real ground.'),
'secure':dict(height=2.8,depth=.4,visible='Institutional/service fence or wall, post bracing and closed returns',collision='Physical visible enclosure; no operational security system implied. Human and ghost boundary rules need explicit runtime review.'),
'bank':dict(height=2.5,depth=5.0,visible='Irregular retaining bank with stone/rock face and shrubs above',collision='Ground-backed steep rise/retaining face, not a hovering wall; test slope climbing and seams.'),
'logfall':dict(height=2.0,depth=4.5,visible='Interlocked fallen logs, root plates and brush backed by a rising bank',collision='A single low log is traversable decoration. Use continuous visible tangled volume and bank; test crawl/climb gaps.'),
'cliff':dict(height=2.0,depth=5.0,visible='Rock buttresses, steep edge and rails at accessible overlooks',collision='Cliff scenery alone is not a safe stop. Keep paths set back and use visible guards/rock barriers before the drop; verify falling/recovery.'),
'shore':dict(height=1.8,depth=4.0,visible='Natural bank or guarded quay/deck edge, with water continuing beyond',collision='Water color/alpha is never collision. Use grounded banks, guarded deck ends and tested water-entry/recovery policy; preserve approved dock/bridge access.'),
'snowbank':dict(height=2.5,depth=6.0,visible='Wind-shaped snow over grounded rock/retaining forms',collision='Snow veneer is not a solid barrier. Match rock/packed-bank body and verify visibility in snow/fog.')
}
SKY={
'water':dict(crest=[-.5,1.5],middle='Low distant shore/breakwater only in authored sectors; retain broad open water horizon',distance=[500,4000]),
'fields':dict(crest=[1,4],middle='Low hedgerows and gently undulating field strips',distance=[150,1500]),
'roofs':dict(crest=[3,9],middle='Limited district roof/chimney silhouettes behind near enclosure',distance=[100,800]),
'trees':dict(crest=[4,12],middle='Staggered tree clusters with trunks at near edge and opaque distant crowns',distance=[60,450]),
'hills':dict(crest=[4,14],middle='Layered rolling ridges and separated tree bands',distance=[200,2000]),
'mountains':dict(crest=[8,22],middle='Receding valley/mountain silhouettes beyond foreground land',distance=[700,5000]),
'mesa':dict(crest=[8,25],middle='Broken flat-topped rock shelves and narrow canyon sky gaps',distance=[150,2500]),
'snow':dict(crest=[1,7],middle='Snow ridge, dark outcrop accents and restrained distant peaks',distance=[200,4000])
}
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(s.encode('utf-8'))
def save(p,d):write(p,json.dumps(d,indent=2,ensure_ascii=False)+'\n')
def center(z):
 if 'label_point' in z:return z['label_point']
 a,b,c,d=z['bounds'];return [(a+c)/2,z['y'],(b+d)/2]
def perimeter(bounds,zones,links,key):
 # Derive a site-specific envelope from actual reserved spaces/corridors rather
 # than wrapping every map in the same rectangular site-bounds outline.
 points=[]
 for zone in zones:
  a,b,c,d=zone['bounds'];points += [(a,b),(c,b),(c,d),(a,d)]
 for link in links:points += [(p[0],p[2]) for p in link['points']]
 expanded=[]
 seed=int(hashlib.sha256(key.encode()).hexdigest()[:6],16)
 for x,z in points:
  for i in range(12):
   angle=i*math.pi/6;radius=7.5+1.5*math.sin(angle*3+seed%17)
   expanded.append((x+radius*math.cos(angle),z+radius*math.sin(angle)))
 def cross(o,a,b):return (a[0]-o[0])*(b[1]-o[1])-(a[1]-o[1])*(b[0]-o[0])
 pp=sorted(set(expanded));lower=[];upper=[]
 for q in pp:
  while len(lower)>=2 and cross(lower[-2],lower[-1],q)<=0:lower.pop()
  lower.append(q)
 for q in reversed(pp):
  while len(upper)>=2 and cross(upper[-2],upper[-1],q)<=0:upper.pop()
  upper.append(q)
 hull=lower[:-1]+upper[:-1];out=[]
 for i,(a,b) in enumerate(zip(hull,hull[1:]+hull[:1])):
  out.append([round(a[0],4),round(a[1],4)]);dx=b[0]-a[0];dz=b[1]-a[1];length=math.hypot(dx,dz)
  if length>15:
   # Outward landform bulge preserves all protected reservations; never pinch a route.
   bulge=1.5+.7*math.sin(i+seed%11)
   out.append([round((a[0]+b[0])/2+dz/length*bulge,4),round((a[1]+b[1])/2-dx/length*bulge,4)])
 return out
def point_in(p,poly):
 x,z=p;hit=False
 for (a,b),(c,d) in zip(poly,poly[1:]+poly[:1]):
  if (b>z)!=(d>z) and x<(c-a)*(z-b)/(d-b)+a:hit=not hit
 return hit
def make(path):
 d=read(path);key=path.parent.name;identity,types,backdrops,skies=P[key];a,b,c,e=d['site_bounds'];stage=d['staging']
 # Existing native Hollow Creek elevations supersede its Y0 schematic for viewpoints.
 refined=read(LEVELS/'hollow-creek/terrain.json') if key=='wld-02' else None
 zones=refined['zones'] if refined else d['zones'];staging=refined['staging'] if refined else stage
 poly=perimeter(d['site_bounds'],zones,refined['routes'] if refined else d['links'],key)
 edges=[]
 for i,(p,q) in enumerate(zip(poly,poly[1:]+poly[:1])):
  dx=(p[0]+q[0])/2-(a+c)/2;dz=(p[1]+q[1])/2-(b+e)/2;angle=math.degrees(math.atan2(dx,-dz))%360;side=int((angle+45)//90)%4;kind=types[side];m=MECHANISMS[kind]
  edges.append(dict(id=f'edge-{i+1:02}',points=[p,q],outward_bearing_deg=round(angle,2),treatment=kind,physical_recipe=m,base_y='Sample/refit to final authoritative terrain; no floating default Y0.',status='planning reservation; no new collision or mesh',models=[m['visible'],'Ground contact/footing and closed returns','Weathered variants retaining collision silhouette']))
 # The incoming road is south of all currently yaw-zero staging points. Reserve a
 # portal in the southernmost segment, but do not silently turn it into walkable area.
 sx,sy,sz=staging['point'];crossings=[]
 for p,q in zip(poly,poly[1:]+poly[:1]):
  if min(p[0],q[0])<=sx<=max(p[0],q[0]) and abs(q[0]-p[0])>1e-6:crossings.append(p[1]+(q[1]-p[1])*(sx-p[0])/(q[0]-p[0]))
 south=max(crossings);portal=dict(id='arrival-road-reserve',center=[sx,sy,south],clear_width_m=8,road_stub_length_m=18,closed_outer_gate='Beyond canonical van staging; visually returned into both boundary sides',status='Proposed access opening, not new navigation. Clip real boundary mesh only after final terrain/collision review.',protected_cargo_route=staging['cargo_route'])
 sectors=[]
 for j in range(8):
  ci=(j//2)%4;ni=(ci+1)%4;typ=skies[ci];base=SKY[typ]
  sectors.append(dict(id=f'frame-{j*45:03}',bearing_range_deg=[j*45,(j+1)*45],identity=backdrops[ci] if j%2==0 else backdrops[ci]+' transitioning to '+backdrops[ni],profile=typ if j%2==0 else typ+'/'+skies[ni],horizon_crest_elevation_deg=base['crest'],open_sky_above_deg=base['crest'][1],mid_distance_m=base['distance'],mid_layer=base['middle'],parallax='Near/middle layers are world-space scenery; sky/weather is direction-fixed background with no gameplay collision.',night_readability='Readable silhouettes without emissive solid-black tree cards or repeated skyline tiles; review fog and flashlight contrast.'))
 entries=[z for z in zones if z['kind']=='outside'];high=max(zones,key=lambda z:z['y']);low=min(zones,key=lambda z:z['y']);entry=next((z for z in zones if z['id']=='entry'),entries[0] if entries else zones[0])
 anchors=[dict(id='van-arrival',position=[sx,sy+1.62,sz],source='Canonical staged van eye/roadhead; preserve cargo route',heading_deg=0),dict(id='primary-approach',position=[center(entry)[0],entry['y']+1.62,center(entry)[2]],source_zone=entry['id'],source='Authored approach-space reservation; actual viewpoint must be stood in',heading_deg=0),dict(id='highest-accessible-layer',position=[center(high)[0],high['y']+1.62,center(high)[2]],source_zone=high['id'],source='Highest authored standing zone; actual window/balcony aperture and guards still require review',heading_deg=0)]
 for z in entries:
  p=center(z);anchors.append(dict(id='outdoor-'+z['id'],position=[p[0],p[1]+1.62,p[2]],source_zone=z['id'],source='Outdoor-space viewpoint reservation',heading_deg=0))
 if low['y']<0:
  p=center(low);anchors.append(dict(id='below-grade-seal',position=[p[0],p[1]+1.62,p[2]],source_zone=low['id'],source='Negative test: no sky through sealed floor/wall/roof; allow only actual authored entrance/shaft apertures.',heading_deg=0))
 protected=[dict(id=z['id'],bounds=z['bounds'],y=z['y'],kind=z['kind']) for z in zones]
 special=[]
 if key in ('wld-14','wld-17'):special.append('Preserve open sea vistas from upper tower/director; do not replace coastal horizon with the shared all-map hill waveform.')
 if key=='wld-15':special.append('Vessel rail/hull/gangway is the inner physical boundary. Harbor is visual background; quay roadhead remains accessible by both gangways. No moving sky/horizon tied to ship motion.')
 if key=='wld-18':special.append('Empty silo shaft is an internal guarded hazard, not an outer boundary or a sky-textured floor; inspect ring views upward through only the real shaft opening.')
 if key=='wld-07':special.append('East boundary is modular: reserve relocation for the separate Vale Mortuary expansion rather than building a second cemetery.')
 if key=='wld-07-mortuary':special.append('Use the annex footprint in this alternate proposal; retain original cemetery plan. Screen the service bay without blocking reception/service approaches.')
 if key=='wld-02':special.append('Existing site_perimeter describes the land edge only: extend the containment review around the real dock and creek crossing. Original native terrain and its perimeter are reference inputs, unchanged.')
 if key=='castle':special.append('Retain existing authored castle/hub geometry, gates and training spaces. This is an outer-landscape planning overlay, not replacement castle walls.')
 return dict(version=1,id=d['id'],title=d['title'],source_layout=str(path.relative_to(ROOT)),source_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),refined_terrain_source='development/levels/hollow-creek/terrain.json' if refined else None,status='planning-only / no runtime geometry or navigation mutation',identity=identity,coordinates='metres X/Z, Y up; azimuth 0 north=-Z, 90 east=+X, 180 south=+Z, 270 west=-X',protected_source_bounds=d['site_bounds'],protected_zones=protected,protected_links=refined['routes'] if refined else d['links'],protected_rituals=refined['rituals'] if refined else d['rituals'],protected_staging=staging,edge_envelope=poly,edges=edges,arrival_portal=portal,
 layers=dict(near='0–12 m beyond accepted playable edge: grounded physical enclosure with irregular rhythm, visible support and closed returns.',middle='World-space terrain/vegetation/buildings beyond boundary: no accessible paths or hidden evidence; parallax from player translation.',far='Direction-fixed distant horizon/sky/weather; no collision. Per-map sector masks replace generic silhouette only in a separately reviewed renderer change.'),
 asset_worklist=[dict(role='physical edge kit',model=MECHANISMS[t]['visible'],status='Audit existing source assets for scale, UVs, contact and collision before creating variants.') for t in sorted(set(types))]+[dict(role='middle/far scenery',model=text,status='Scenery-only frame reservation; world-space middle models need distance/height fit and upper-view review.') for text in backdrops],sky_frames=sectors,review_views=anchors,review_states=dict(eye_offsets_m=[.95,1.45,1.62],horizontal_fov_deg=[60,90,110],vertical_scan_deg=[-45,60],lighting=['daylight authoring view','night normal exposure','adjoining circuits toggled','portable flashlight grazing edge'],weather=['clear visibility','fog','rain','snow where appropriate'],camera='Standing, crouched and evidence camera views; rotate through 360 degrees at each anchor and translate to test parallax/seams.'),
 inclusion_rules=['Sky exists behind real world geometry and must not write foreground depth. Closed opaque rooms occlude sky; actual windows and open courtyards reveal it.','No backdrop may appear through a wall/floor/roof join, under an upper deck or through a light-leak seam. Apply the shared defect checklist.','Keep the water/sky horizon level and continuous; layered water transparency/depth remains a separate acceptance gate.','Do not use fog/darkness/painted horizon as a physical barrier or conceal unsupported terrain edges.','Near occluders must frame vistas without cutting routes, door swings, van/ramp, ritual squares or sparse hiding pockets.','No player-facing map detail should depend on distant scenery collision. Keep ghost/human containment rules explicit and host-authoritative.','Highest windows, towers and platforms need a larger visible terrain apron and farther horizon than ground-only cameras.','No stock sky texture has been acquired or licensed by this plan. Use existing procedural sky/weather; any new bitmap needs separate source/license intake.'],special_cases=special,
 next_gates=['Fit proposed perimeter to actual native terrain and existing enclosure; extend off-map terrain before placing any new barrier.','Author supported barrier spans, road portal and closed returns as selected additive geometry; inspect all overlap/clearance.','Run swept human/ghost/crouch and under/over/around checks, water/fall recovery and van extraction.','Render ordinary-viewpoint frame captures and verify no sky leaks, near-card artifacts or horizon discontinuity; gallery publish.','Integrate per-map scenery/sky profile only after engine branch coordination and CPU/GPU parity/performance review.'],acceptance='No boundary continuity, collision, actual screenshot framing, runtime sky mask or gameplay acceptance is claimed.')

def svg(plan):
 poly=plan['edge_envelope'];xs=[p[0] for p in poly];zs=[p[1] for p in poly];a=min(xs);b=min(zs);c=max(xs);d=max(zs);s=min(8,740/(c-a),650/(d-b));ox=45;oy=120
 def pt(p):return (ox+(p[0]-a)*s,oy+(p[1]-b)*s)
 def coords(seq):return ' '.join(f'{pt(p)[0]:.1f},{pt(p)[1]:.1f}' for p in seq)
 out=['<svg xmlns="http://www.w3.org/2000/svg" width="1600" height="1040" viewBox="0 0 1600 1040">','<style>text{font-family:Arial,sans-serif;fill:#e7ecee}.small{font-size:12px}.medium{font-size:15px}</style><rect width="1600" height="1040" fill="#20292f"/>',f'<text x="32" y="38" font-size="25">{html.escape(plan["title"])} — edge / sky frame plan</text>','<text x="32" y="67" class="medium">PLANNING DIAGRAM · metres · no new geometry/collision · dashed ring is a proposed envelope, not accepted navigation</text>','<text x="45" y="104" class="medium">NORTH / −Z ↑   •   route and room reservations retained</text>',f'<polygon points="{coords(poly)}" fill="#2e3a3d" stroke="#f2be67" stroke-width="3" stroke-dasharray="7,5"/>']
 for z in sorted(plan['protected_zones'],key=lambda z:z['kind'] not in ('outside','staging')):
  a0,b0,c0,d0=z['bounds'];x,y=pt([a0,b0]);fill='#53604a' if z['kind']=='outside' else '#6a9193' if z['kind']=='staging' else '#52616a'
  out.append(f'<rect x="{x:.1f}" y="{y:.1f}" width="{(c0-a0)*s:.1f}" height="{(d0-b0)*s:.1f}" fill="{fill}" stroke="#9badb5" stroke-width=".8" opacity=".75"><title>{html.escape(z["id"])}, Y{z["y"]:g}</title></rect>')
 for l in plan['protected_links']:out.append(f'<polyline points="{coords([[p[0],p[2]] for p in l["points"]])}" fill="none" stroke="#79c9d1" stroke-width="1.2"/>')
 for i,e in enumerate(plan['edges']):
  p,q=e['points'];x,y=pt([(p[0]+q[0])/2,(p[1]+q[1])/2]);out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="10" fill="#695132"/><text x="{x:.1f}" y="{y+4:.1f}" text-anchor="middle" class="small">{i+1}</text>')
 gate=plan['arrival_portal'];x,y=pt([gate['center'][0],gate['center'][2]]);out.append(f'<rect x="{x-4*s:.1f}" y="{y-4:.1f}" width="{8*s:.1f}" height="8" fill="#a8d8b8"/><text x="{x:.1f}" y="{y+24:.1f}" class="small" text-anchor="middle">Arrival road reserve (8 m)</text>')
 for i,view in enumerate(plan['review_views']):
  x,y=pt([view['position'][0],view['position'][2]]);out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="4" fill="#ffd6f0"><title>{html.escape(view["id"])}, eye Y {view["position"][1]:g}</title></circle>')
 out.append('<text x="845" y="105" font-size="20">360° inclusion frames / art-direction bands</text>')
 out.append('<text x="845" y="130" class="small">N 0° → E 90° → S 180° → W 270° • bands are schematic, not screenshots</text>')
 for i,fr in enumerate(plan['sky_frames']):
  yy=155+i*91;crest=fr['horizon_crest_elevation_deg'][1];ycrest=yy+58-crest
  if fr['profile'] in ('water','water/water'):
   silhouette=f'M845,{ycrest+3} H1555 V{yy+62} H845 Z'
  elif fr['profile'].startswith('water/'):
   silhouette=f'M845,{ycrest+3} H1150 L1290,{ycrest+2} L1430,{ycrest+5} L1555,{ycrest} V{yy+62} H845 Z'
  else:
   silhouette=f'M845,{ycrest+4} L985,{ycrest} L1135,{ycrest+7} L1290,{ycrest+2} L1430,{ycrest+5} L1555,{ycrest} V{yy+62} H845 Z'
  out.append(f'<rect x="845" y="{yy}" width="710" height="62" fill="#526b80"/><path d="{silhouette}" fill="#263c40"/>')
  out.append(f'<text x="853" y="{yy+18}" class="small">{fr["bearing_range_deg"][0]}–{fr["bearing_range_deg"][1]}° • {html.escape(fr["profile"])} • crest {crest:g}° provisional</text>')
  caption=html.escape(fr['identity']);out.append(f'<text x="845" y="{yy+80}" class="small">{caption[:110]}</text>')
 out += ['<text x="35" y="920" class="medium">Amber: proposed physical edge · cyan: protected routes · pink dots: view anchors · green: road portal</text>','<text x="35" y="948" class="medium">Near: grounded enclosure • Middle: world-space terrain / silhouettes • Far: procedural sky + weather, visual only</text>','<text x="35" y="978" class="medium">Review: standing / crouched / camera; upper layers; van extraction; clear + fog; no sky through sealed joins.</text>','<text x="35" y="1008" class="small">Do not build from this drawing alone. See JSON for exact spans, recipes, elevation fitting, all source reservations and acceptance gates.</text></svg>']
 return '\n'.join(out)

def brief(p):
 lines=['# '+p['title']+' — edge blocking and sky inclusion','',p['status'],'',p['identity'],'','[Diagram](plan.svg) · [Exact planning contract](plan.json)','','## Layer roles','']+[f'- **{k}**: {v}' for k,v in p['layers'].items()]
 lines+=['','## Physical edge reservations','','| Span | Treatment | Height / depth reservation | Fit / containment |','|---|---|---|---|']
 for e in p['edges']:
  m=e['physical_recipe'];lines.append(f'| {e["id"]} | {e["treatment"]} | {m["height"]} / {m["depth"]} m | {m["collision"]} |')
 lines+=['','All base heights must be fitted to authoritative terrain, not Y0. The outer ring encloses source reservations but grants no extra walkable area. Fit it to actual terrain/architecture before construction.','', 'Arrival: 8 m road portal reservation beyond staged van; retain its exact cargo route and fit a visible closed outer gate/road bend.','', '## Sky inclusion sectors','','| Azimuth | Identity | Provisional crest / open sky above | Mid-distance |','|---|---|---|---|']
 for fr in p['sky_frames']:lines.append(f'| {fr["bearing_range_deg"]}° | {fr["identity"]} | {fr["horizon_crest_elevation_deg"]}° / {fr["open_sky_above_deg"]}° | {fr["mid_distance_m"]} m |')
 lines+=['','Sector heights/distances are original art-direction targets, not measured horizon coverage. Real camera captures must fit them, particularly from high towers and windows.','', '## Viewpoints','','| Anchor | Eye position X/Y/Z | Source |','|---|---|---|']
 for v in p['review_views']:lines.append(f'| {v["id"]} | {v["position"]} | {v["source"]} |')
 lines+=['','## Site-specific constraints','']+['- '+v for v in p['special_cases']]+['','## Inclusion and separation rules','']+['- '+v for v in p['inclusion_rules']]+['','## Next gates','']+['- '+v for v in p['next_gates']]+['',p['acceptance'],'']
 return '\n'.join(lines)

def main():
 paths=sorted((LEVELS/'layouts').glob('*/layout.json'))+sorted((LEVELS/'layout-expansions').glob('*/layout.json'));rows=[]
 for path in paths:
  plan=make(path);key=path.parent.name;save(OUT/key/'plan.json',plan);write(OUT/key/'plan.svg',svg(plan));write(OUT/key/'README.md',brief(plan))
  rows.append(dict(id=plan['id'],title=plan['title'],folder=key,spans=len(plan['edges']),frames=len(plan['sky_frames']),viewpoints=len(plan['review_views']),expansion=key.endswith('mortuary')))
 assert len(rows)==len(P)==29
 save(OUT/'catalog.json',dict(status='planning only',maps=rows,base_maps=28,additive_expansions=1))
 cards=''.join('<article><h2>'+html.escape(r['title'])+'</h2><p>'+str(r['spans'])+' edge spans · '+str(r['frames'])+' sky sectors · '+str(r['viewpoints'])+' view anchors</p><a href="'+r['folder']+'/plan.svg"><img loading="lazy" src="'+r['folder']+'/plan.svg" alt="Boundary and sky inclusion planning diagram"></a><p><a href="'+r['folder']+'/README.md">Plan notes</a> · <a href="'+r['folder']+'/plan.json">Exact contract</a></p></article>' for r in rows)
 write(OUT/'REVIEW.html','<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>All-map edge and sky plans</title><style>body{background:#20292f;color:#eee;font:16px Arial;margin:25px}a{color:#b4dced}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(480px,1fr));gap:20px}article{padding:20px;background:#303b42}img{width:100%}</style><h1>Level edges and sky inclusion frames</h1><p>28 base maps + one additive cemetery/mortuary expansion. Planning diagrams, not runtime captures or collision approval. Existing maps are preserved.</p><main>'+cards+'</main></html>')
 write(OUT/'README.md','# All-map edge blocking and sky inclusion plans\n\n23 October 2026. [Visual atlas](REVIEW.html) · [Catalog](catalog.json).\n\n28 existing/new base packets and the separate cemetery/mortuary annex now have site-specific edge recipes, proposed irregular containment envelopes, exact protected source reservations, 8 azimuth sectors and actual-coordinate viewpoint reservations. No map geometry, runtime registry, engine or original layout is changed.\n\nSky inclusion means the sky/horizon revealed between real terrain, vegetation, buildings and openings. It is a view-composition contract, not a texture pasted over the map. Near physical containment, middle world-space scenery and far sky/weather have distinct jobs. Frame diagrams are deliberately schematic; they are not fabricated game screenshots.\n\nExisting renderer source: SceneRenderer.paintSky paints weather and a shared hill waveform behind depth-tested world geometry. WeatherSky supplies procedural day/night/cloud weather pixels. This plan retains that system as the integration baseline; it does not implement new per-site runtime horizons. The shared generic hill silhouette needs a coordinated, separately tested per-map treatment for open coasts and other distinct settings.\n\nDo not use low decorative logs, water alpha, fog, a sky picture or invisible walls to claim containment. All proposed barriers need grounded support, closed returns and human/ghost/crouch checks; upper windows/towers require broader scenery coverage. Preserve the van and its cargo approach.\n\n'+ '\n'.join('- ['+r['title']+']('+r['folder']+'/README.md)' for r in rows)+'\n')
 print('AUTHORED',len(rows),'edge/sky packets;',sum(r['spans'] for r in rows),'spans;',sum(r['frames'] for r in rows),'sectors.')
if __name__=='__main__':main()
