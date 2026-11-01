"""Authored S02 space/section studies; no runtime geometry or acceptance grants.

Coordinates below are the revised rough authority, not copied legacy room labels.
Generated JSON/SVG/OBJ are derived review artifacts. Later construction must
consume these decisions and prove physical doors, stairs, terrain and services.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import math
import struct

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'development/levels/rebuild'
spec = importlib.util.spec_from_file_location('layout_review', ROOT / 'development/levels/layouts/build_review.py')
review = importlib.util.module_from_spec(spec)
spec.loader.exec_module(review)


def school():
    zones, links = [], []

    def room(key, name, bounds, y, height, owner, kind='inside'):
        zones.append(dict(id=key, name=name, bounds=bounds, y=y, height=height,
                          owner=owner, kind=kind, purpose=name))

    # A three-metre public spine, with bounded service rooms beside it. The
    # former eight-metre corridor is not filled with toilets and called a hall.
    room('foyer', 'Pupil entrance and visitor vestibule', [20,30,25,36], 0,3.6,0,'circulation')
    room('interview', 'Visitor reception and interview', [25,30,30,33], 0,3.6,0)
    room('visitor-wc', 'Visitor WC from public vestibule', [25,33,27.5,36], 0,3.6,0)
    room('office', 'Administration work room', [27.5,33,30,36], 0,3.6,0)
    room('corridor', 'Ground public teaching corridor', [20,12,23,30], 0,3.6,1,'circulation')
    room('staff', 'Staff rest and work room', [23,15,27.2,20], 0,3.6,1)
    room('cleaner', 'Cleaner sink and supplies', [23,12,25,15], 0,3.6,1)
    room('staff-wc', 'Staff sanitary room', [25,12,27.2,15], 0,3.6,1)
    room('service-stair', 'Plant service stair and rear delivery exit', [27.2,12,30,20], 0,3.6,1,'circulation')
    room('main-stair', 'Main stair bay with lower approach', [23,20,30,30], 0,3.6,1,'circulation')
    room('class-a', 'Class A: 24 desks and teaching area', [10,24,20,34], 0,3.6,2)
    room('class-a-store', 'Class A coats and teaching store', [10,34,20,36], 0,3.6,2)
    room('science', 'Shared practical science teaching', [10,12,20,21], 0,3.6,3)
    room('science-prep', 'Science preparation and secure supplies', [10,21,20,24], 0,3.6,3)
    room('dining', 'Dining: two classes in one sitting', [30,24,38,36], 0,3.6,4)
    room('kitchen', 'Preparation cooking and controlled serving', [38,28,42,36], 0,3.6,4)
    room('receiving', 'Food receiving wash-up and refuse exit', [42,28,46,36], 0,3.6,4)
    room('gym-link', 'Public dining-to-gym connector', [38,24,42,28], 0,3.6,4,'circulation')
    room('health-link', 'Health and changing corridor', [30,12,32,24], 0,3.6,5,'circulation')
    room('nurse', 'Private nurse consultation and rest', [32,18,38,24], 0,3.6,5)
    room('change-a', 'Changing group A lockers and bench', [32,12,36,15], 0,3.6,5)
    room('shower-a', 'Group A screened washing', [36,12,38,15], 0,3.6,5)
    room('change-b', 'Changing group B lockers and bench', [32,15,36,18], 0,3.6,5)
    room('shower-b', 'Group B screened washing', [36,15,38,18], 0,3.6,5)
    room('gym', 'Multipurpose PE hall: no upper floor', [38,8,50,24], 0,7.2,6)
    room('upper-corridor', 'Upper public teaching corridor', [20,12,23,36], 3.6,3.6,7,'circulation')
    room('wash-a', 'Student washroom A with screened entry', [23,12,30,17], 3.6,3.6,7)
    room('wash-b', 'Student washroom B with screened entry', [23,17,30,22], 3.6,3.6,7)
    room('upper-stair', 'Guarded stair void and top landing', [23,22,30,30], 3.6,3.6,7,'circulation')
    room('upper-lobby', 'Daylit library and assembly lobby', [23,30,30,36], 3.6,3.6,7,'circulation')
    room('library', 'Library reading lending and stacks', [10,24,20,36], 3.6,3.6,8)
    room('class-b', 'Class B: 24 desks and teaching area', [10,12,20,22], 3.6,3.6,9)
    room('class-b-store', 'Class B coats and teaching store', [10,22,20,24], 3.6,3.6,9)
    room('assembly', 'Assembly: 48 pupils and staff', [30,24,42,36], 3.6,4.5,10)
    room('lower-stair', 'Cellar service stair', [27.2,12,30,20], -3,3,11,'circulation')
    room('plant', 'Boiler hot-water and electrical maintenance', [23,12,27.2,20], -3,3,11)
    room('plant-store', 'Maintenance store and clear ritual reservation', [23,20,30,30], -3,3,11)
    room('grounds', 'Outdoor grounds: footprint is envelope only', [5,2,55,36], 0,12,12,'outside')
    room('entry', 'School forecourt and shared arrival support', [1,36,55,58.4], 0,12,13,'outside')
    room('stage', 'Canonical van arrival reservation', [18,45,38,58], 0,3,14,'staging')
    byid = {z['id']: z for z in zones}

    def door(a,b,x,z,width=1.2,access='public'):
        ra,rb=byid[a],byid[b]
        assert ra['y']==rb['y']
        y=ra['y']; pts=[review.center(ra),[x,y,z],review.center(rb)]
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='level',width=width,points=pts,
                          threshold=[x,y,z],access=access,note='Exact proposed threshold; centre-to-door polylines are diagrams, not navigation proof.'))

    for row in [
        ('foyer','interview',25,31.5),('foyer','visitor-wc',25,34.5),
        ('interview','office',28.75,33),('foyer','corridor',21.5,30),
        ('corridor','staff',23,17),('corridor','cleaner',23,13.5),('staff','staff-wc',26,15),
        ('main-stair','service-stair',28.5,20),('corridor','main-stair',23,21),
        ('corridor','class-a',20,27),('class-a','class-a-store',15,34),
        ('corridor','science',20,17),('science','science-prep',17,21),
        ('main-stair','dining',30,26),('dining','kitchen',38,31),('kitchen','receiving',42,31),
        ('dining','gym-link',38,26),('gym-link','gym',40,24),
        ('main-stair','health-link',30,21),('health-link','nurse',32,21),
        ('health-link','change-a',32,13.5),('change-a','shower-a',36,13.5),
        ('health-link','change-b',32,16.5),('change-b','shower-b',36,16.5),
        ('upper-corridor','wash-a',23,13.2),('upper-corridor','wash-b',23,18.2),
        ('upper-corridor','upper-stair',23,29),('upper-corridor','upper-lobby',23,33),
        ('upper-corridor','library',20,30),('upper-corridor','class-b',20,17),
        ('class-b','class-b-store',15,22),('upper-lobby','assembly',30,33),
        ('lower-stair','plant',27.2,18.8),('plant','plant-store',26,20),
        ('foyer','entry',22.5,36),('service-stair','grounds',28.5,12),
        ('science','grounds',10,16),('class-a','grounds',10,28),
        ('gym','grounds',44,8),('receiving','grounds',46,32),
        ('grounds','entry',52,36),('stage','entry',28,45)]:
        door(*row,access='service' if any(k in row[:2] for k in ['plant','plant-store','service-stair','receiving','kitchen','science-prep','cleaner']) else 'public')
    for a,b,points,width in [
        ('main-stair','upper-stair',[[25,0,22],[25,3.6,27.76]],1.5),
        ('lower-stair','service-stair',[[28.5,-3,13.5],[28.5,0,18.3]],1.5)]:
        rise=abs(points[-1][1]-points[0][1]);run=math.dist(points[0][::2],points[-1][::2])
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='stairs',width=width,points=points,rise_m=rise,
                          run_m=run,headroom_m=2.2,landing_depth_m=1.5,risers=20 if rise>3 else 18,
                          construction='Straight flight; reserve full-run deck void, structural depth, guards and unobstructed end landings. Actual capsule/ceiling proof required.',
                          note='Measured section reservation, not a generic centre-to-centre diagonal ramp.'))
    return dict(version=2,id='SCHOOL',title='Ravenshade School — revised function-first rough',
        status='S02_DESIGN_STUDY_NOT_RUNTIME_ACCEPTED',site_bounds=[1,2,55,59],zones=zones,links=links,
        capacity='48 pupils in two general classes; one shared 24-place practical science room; six staff positions',
        rituals=[dict(rite='Banishment',zone='assembly',point=[36,3.6,30]),
                 dict(rite='Cleansing',zone='plant-store',point=[26.5,-3,25]),
                 dict(rite='Absolution',zone='grounds',point=[49,0,31])],
        staging=dict(zone='stage',point=[28,0,51],yaw=-math.pi/2,
                     cargo_route=[[24.372,0,51],[22.5,0,40],[22.5,0,36]],
                     note='Canonical VanStaging source; no substitute vehicle mesh. Arrival remains to be physically authored.'),
        terrain_cutouts=[dict(bounds=[23,12,30,30],world_surface_y=-.65,
                             method='Persisted editor AdaptiveTerrainTopology punched leaves; not authored yet')],
        furniture_reservations=[dict(space=key,model='level_student_desk',columns=[11.5,12.75,14,16,17.25,18.5],
                                     rows=[z0+2,z0+3.8,z0+5.6,z0+7.4],chair_offset_z=.72,
                                     model_scale=1,required_quantity=24,
                                     note='Measured source desk 1.1 x 0.65 m; native chair bounds and pivots retained. 0.9 m central aisle; final collider/door approaches untested.')
                                for key,z0 in [('class-a',24),('class-b',12)]],
        sanitary_reservations=[dict(space=k,entry_screen_depth_m=1.5,stall_count=3,
                                   stall_reservation_m=[1.5,2],shared_wash_zone_m=[3,2],
                                   note='Place full-scale source WCs/basins; no miniature accessibility stall. Detail placement and privacy rays remain in blockout.')
                              for k in ['wash-a','wash-b']],
        decisions=['Replace broad locker-corridor washroom partitions with separate bounded sanitary rooms off a 3 m public corridor.',
                   'Keep the gym a separate 7.2 m clear volume; upper assembly has its own 4.5 m height and roof.',
                   'Food receiving and refuse leave by the east service yard, never through teaching rooms.',
                   'Plant access has a rear service door and separate stair; it does not require entering nurse or pupil washrooms.',
                   'Classroom stores, staff facilities and private visitor interview have explicit floor area instead of generic props in a hall.'],
        remaining=['Engineer secondary upper-floor emergency exit and route fairness; no code-compliance claim.',
                   'Resolve individual sanitary/changing/PE-equipment fixtures and privacy screens in actual geometry.',
                   'Author structural beams/roof, complete source furniture clearances, electrical/acoustic ownership and hiding budget.',
                   'Punch editor world terrain and prove full-body traversal, doors, networking, gameplay, human and GPU gates.'])


def validate(data):
    zones={z['id']:z for z in data['zones']}
    assert len(zones)==len(data['zones'])
    assert {z['owner'] for z in zones.values()}==set(range(15)), 'All stable gameplay owners must be mapped'
    for a in zones.values():
        x0,z0,x1,z1=a['bounds']; assert x1>x0 and z1>z0 and a['height']>0
        for b in zones.values():
            if a['id']>=b['id'] or a['y']!=b['y'] or a['kind'] in ('outside','staging') or b['kind'] in ('outside','staging'):continue
            bx,bz,bxx,bzz=b['bounds']
            assert min(x1,bxx)<=max(x0,bx)+1e-8 or min(z1,bzz)<=max(z0,bz)+1e-8, (a['id'],b['id'],'overlap')
    graph={k:set() for k in zones}
    for link in data['links']:
        a,b=zones[link['a']],zones[link['b']]
        for room,p in [(a,link['points'][0]),(b,link['points'][-1])]:
            x0,z0,x1,z1=room['bounds'];assert x0<=p[0]<=x1 and z0<=p[2]<=z1 and abs(room['y']-p[1])<1e-7,link
        if link['kind']=='stairs':
            assert link['run_m']>=link['rise_m']*1.6-1e-7
            start,end=link['points']; assert start[0]==end[0] and end[2]>start[2]
            assert start[2]-a['bounds'][1]>=link['landing_depth_m']-1e-7, 'Bottom landing '+link['id']
            assert b['bounds'][3]-end[2]>=link['landing_depth_m']-1e-7, 'Top landing '+link['id']
            for room in [a,b]:
                assert room['bounds'][0]+link['width']/2<=start[0]<=room['bounds'][2]-link['width']/2
        else:
            x,y,z=link['threshold']
            for room in [a,b]:
                x0,z0,x1,z1=room['bounds'];assert x0<=x<=x1 and z0<=z<=z1,(link,'threshold outside')
        graph[a['id']].add(b['id']);graph[b['id']].add(a['id'])
    reached=set();pending=['stage']
    while pending:
        key=pending.pop()
        if key in reached:continue
        reached.add(key);pending.extend(graph[key]-reached)
    assert reached==set(zones), set(zones)-reached
    for rite in data['rituals']:
        room=zones[rite['zone']];x,y,z=rite['point'];x0,z0,x1,z1=room['bounds']
        assert x0+2.2<=x<=x1-2.2 and z0+2.2<=z<=z1-2.2, 'Ritual clear area '+rite['zone']
        if room['kind']=='outside':
            for other in zones.values():
                if other['kind'] in ('outside','staging') or other['y']!=y:continue
                bx,bz,bxx,bzz=other['bounds']
                assert min(x+2.2,bxx)<=max(x-2.2,bx) or min(z+2.2,bzz)<=max(z-2.2,bz), 'Ritual intersects '+other['id']
    return len(zones),len(data['links'])


def sections(data):
    out=['<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="840" viewBox="0 0 1200 840">',
         '<rect width="1200" height="840" fill="#f5f4ef"/>',
         '<g font-family="Arial" fill="#1c2a37"><text x="30" y="35" font-size="24">Ravenshade School: proposed stair sections</text>',
         '<text x="30" y="60" font-size="15">S02 dimensions only. Full-run architectural voids plus editor world-ground opening remain to be built and tested.</text>']
    for index,link in enumerate(x for x in data['links'] if x['kind']=='stairs'):
        run,rise=link['run_m'],link['rise_m'];base=360+index*340;left=110;scale=45
        x0=left+1.5*scale;x1=x0+run*scale;y0=base;y1=base-rise*scale
        out.extend([f'<text x="30" y="{base-205}" font-size="19">{link["a"]} to {link["b"]}</text>',
                    f'<path d="M{left},{y0} H{x0} M{x1},{y1} h{1.5*scale}" stroke="#536477" stroke-width="8" fill="none"/>',
                    f'<path d="M{x0},{y0} L{x1},{y1}" stroke="#a86516" stroke-width="5"/>',
                    f'<path d="M{x0},{y0-2.2*scale} L{x1},{y1-2.2*scale}" stroke="#307e99" stroke-dasharray="5 4" fill="none"/>',
                    f'<text x="680" y="{base-160}" font-size="17">Rise {rise:g} m / run {run:g} m / clear width {link["width"]:g} m</text>',
                    f'<text x="680" y="{base-130}" font-size="17">{link["risers"]} risers, {rise/link["risers"]:.3f} m each (proposed)</text>',
                    f'<text x="680" y="{base-100}" font-size="17">1.5 m landing reservations at both ends</text>',
                    f'<text x="680" y="{base-70}" font-size="17">Cyan: 2.2 m headroom envelope above flight</text>',
                    f'<text x="680" y="{base-40}" font-size="17">Floor/roof depth and underside rays require actual geometry</text>'])
    return '\n'.join(out+['</g></svg>'])


def asset_bounds():
    records=[]
    for name in ['level_student_desk','chair_mp_5','toilet_mp_1','level_bath_basin']:
        path=ROOT/'assets/runtime/models'/(name+'.glb');raw=path.read_bytes()
        size,kind=struct.unpack_from('<II',raw,12);assert kind==0x4e4f534a
        document=json.loads(raw[20:20+size])
        assert all(not any(k in node for k in ['matrix','scale','translation','rotation']) for node in document['nodes']), 'Review transformed source '+name
        indices={p['attributes']['POSITION'] for mesh in document['meshes'] for p in mesh['primitives']}
        accessors=[document['accessors'][i] for i in indices]
        lo=[min(a['min'][i] for a in accessors) for i in range(3)]
        hi=[max(a['max'][i] for a in accessors) for i in range(3)]
        records.append(dict(asset=name,path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(raw).hexdigest(),
                            local_min=lo,local_max=hi,size_m=[b-a for a,b in zip(lo,hi)],
                            note='Static GLB POSITION bounds with identity nodes; native pivot preserved. Actual placed support/collider fit remains unverified.'))
    return dict(status='SOURCE_BOUNDS_FOR_ROUGH_PLANNING_ONLY',assets=records)


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    for folder,data in [('school',school())]:
        spaces,links=validate(data)
        outputs={'rough-layout.json':json.dumps(data,indent=2,ensure_ascii=False)+'\n',
                 'rough-plan.svg':review.render_svg(data),'rough-sections.svg':sections(data),
                 'rough-asset-bounds.json':json.dumps(asset_bounds(),indent=2)+'\n',
                 'rough-volumes.obj':review.render_obj(data)}
        for name,text in outputs.items():
            path=OUT/folder/name;raw=text.encode('utf-8')
            if args.check:assert path.is_file() and path.read_bytes()==raw, 'Stale '+str(path)
            else:path.write_bytes(raw)
        print(f'PASS {folder} revised S02 study: {spaces} architectural spaces / {links} reserved connections; geometry and acceptance remain open')


if __name__=='__main__':main()
