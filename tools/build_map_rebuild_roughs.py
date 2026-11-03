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


def prison():
    zones, links = [], []

    def room(key,name,bounds,y,height,owner,kind='inside'):
        zones.append(dict(id=key,name=name,bounds=bounds,y=y,height=height,owner=owner,kind=kind,purpose=name))

    # The two original parallel-wing masses are replaced by actual cells,
    # their corridor and dayhall, with the gallery on the CELL DOOR side.
    cells=[]
    for wing,cell_x,gallery_x,day_x,stair_x,owners in [
        ('a',[10,14],[14,16],[16,22],[16,20],(0,7)),
        ('b',[54,58],[52,54],[46,52],[48,52],(4,10))]:
        for floor,y,owner in [('lower',0,owners[0]),('upper',3.4,owners[1])]:
            for index in range(8):
                z=24+index*3.5;key=f'{wing}-{floor}-cell-{index+1:02}'
                room(key,f'{wing.upper()} {floor} single cell {index+1}',[cell_x[0],z,cell_x[1],z+3.5],y,3,owner)
                cells.append(dict(space=key,wing=wing,standing_y=y,capacity=1,door=[14 if wing=='a' else 54,y,z+2.6],
                    bed_reservation=[cell_x[0]+.18,z+.65,cell_x[0]+1.13,z+2.75] if wing=='a' else [cell_x[1]-1.13,z+.65,cell_x[1]-.18,z+2.75],
                    wc_origin=[13.3 if wing=='a' else 54.7,y+.235,z+.70],wc_yaw=0,
                    basin_origin=[12.05 if wing=='a' else 55.95,y,z+.33],
                    note='Full-scale WC native pivot corrected by +0.235 m; fixed steel bed remains to be authored at 2.1 x 0.95 m. Clear circulation/door/fixture support requires actual blockout.'))
            room(f'{wing}-{floor}-gallery',f'{wing.upper()} {floor} cell-door gallery',
                 [gallery_x[0],24,gallery_x[1],52 if floor=='lower' else 58],y,3,owner,'circulation')
        owner=owners[0]
        room(f'{wing}-rear-link',f'{wing.upper()} rear secure cross-link',
             [10 if wing=='a' else 52,22,16 if wing=='a' else 58,24],0,3,owner,'circulation')
        room(f'{wing}-front-link',f'{wing.upper()} front secure cross-link',
             [10 if wing=='a' else 52,52,16 if wing=='a' else 58,58],0,3,owner,'circulation')
        room(f'{wing}-dayhall',f'{wing.upper()} supervised dayhall',[day_x[0],38,day_x[1],46],0,6.8,owner)
        room(f'{wing}-rear-stair',f'{wing.upper()} alternate rear stair bay',[stair_x[0],28,stair_x[1],38],0,6.8,owner,'circulation')
        room(f'{wing}-rear-bypass',f'{wing.upper()} rear dayhall bypass',[20 if wing=='a' else 46,28,22 if wing=='a' else 48,38],0,6.8,owner,'circulation')
        room(f'{wing}-upper-rear-stair',f'{wing.upper()} upper alternate landing',[stair_x[0],28,stair_x[1],38],3.4,3,owners[1],'circulation')
        room(f'{wing}-change',f'{wing.upper()} prisoner wash changing and benches',
             [16 if wing=='a' else 49,22,19 if wing=='a' else 52,28],0,3,owner)
        room(f'{wing}-showers',f'{wing.upper()} three shower bays and drainage',
             [19 if wing=='a' else 46,22,22 if wing=='a' else 49,28],0,3,owner)
        room(f'{wing}-stair',f'{wing.upper()} independent stair bay',[stair_x[0],46,stair_x[1],58],0,6.8,owner,'circulation')
        room(f'{wing}-upper-stair',f'{wing.upper()} upper stair void and landing',[stair_x[0],46,stair_x[1],58],3.4,3,owners[1],'circulation')
        room(f'{wing}-front-aisle',f'{wing.upper()} front bypass aisle',[20 if wing=='a' else 46,46,22 if wing=='a' else 48,58],0,3,owner,'circulation')
    # Public reception and the visitor side of the booths stay outside custody.
    room('reception','Public reception and identity check',[10,62,18,68],0,3,1)
    room('intake','Search and intake custody corridor',[10,58,18,62],0,3,1,'circulation')
    room('holding','Supervised temporary holding',[18,58,22,62],0,3,1)
    room('search','Private search and belongings',[22,58,26,64],0,3,1)
    room('records','Prisoner records and staff work',[18,62,22,68],0,3,9)
    room('public-wc','Public sanitary room',[22,64,26,68],0,3,9)
    room('sally','Vehicle sally port and double-gate control',[26,58,42,68],0,4,2,'circulation')
    room('guard','Guard control and dispatch',[42,58,50,64],0,3,2)
    room('staff','Staff rest and lockers',[42,64,46.8,68],0,3,2)
    room('staff-wc','Staff sanitary room',[46.8,64,50,68],0,3,2)
    room('guard-link','Controlled B-block entrance',[50,58,58,68],0,3,2,'circulation')
    room('visitors','Public visiting booths',[0,58,6,68],0,3,6)
    room('visitor-custody','Custody visiting booths',[6,58,10,68],0,3,6)
    room('cross','Rear secure distribution passage',[10,16,58,22],0,3,2,'circulation')
    room('dining','Thirty-four-place mess',[10,4,22,16],0,3,3)
    room('kitchen','Kitchen cooking and serving',[22,4,28,12],0,3,3)
    room('food-store','Secure dry and chilled food store',[22,12,25,16],0,3,3)
    room('wash-up','Wash-up refuse and service handling',[25,12,28,16],0,3,3)
    room('medical-link','Medical reception and private access',[28,4,32,16],0,3,5,'circulation')
    room('exam','Medical examination and handwashing',[32,4,39,10],0,3,5)
    room('recovery','Two-bed recovery observation',[39,4,46,10],0,3,5)
    room('medical-store','Clean medical supplies',[32,10,39,16],0,3,5)
    room('sluice','Soiled utility wash and linen',[39,10,46,16],0,3,5)
    room('chapel','Worship and reconciliation',[58,34,70,52],0,4,6)
    room('isolation-link','Observed isolation corridor',[58,16,61,34],0,3,8,'circulation')
    room('isolation-cell-1','Isolation single cell one with sanitary bay',[61,18,66,22.5],0,3,8)
    room('isolation-cell-2','Isolation single cell two with sanitary bay',[61,22.5,66,27],0,3,8)
    room('observation','Isolation observation and controlled supplies',[61,16,70,18],0,3,8)
    room('isolation-service','Isolation clinical and linen service',[66,18,70,27],0,3,8)
    room('isolation-wash','Isolation washing and work room',[61,27,70,34],0,3,8)
    room('plant-stair','Separate plant stair enclosure',[28,22,32,34],0,3,11,'circulation')
    room('lower-stair','Plant stair and maintenance access',[28,22,32,34],-3,3,11,'circulation')
    room('plant','Heating hot water and electrical plant',[32,22,46,34],-3,3,11)
    room('laundry','Laundry showers supply and maintenance store',[28,34,46,42],-3,3,11)
    room('yard','Bounded exercise yard around plant headhouse',[22,22,46,58],0,12,12,'outside')
    room('entry','Front public approach and perimeter roadhead',[0,68,76,94],0,12,13,'outside')
    room('north-service','North food and maintenance delivery apron',[0,0,76,4],0,12,13,'outside')
    room('east-service','East exterior service walk',[70,4,76,68],0,12,13,'outside')
    room('west-service','West exterior service walk',[0,4,6,58],0,12,13,'outside')
    room('stage','Canonical van staging and extraction reservation',[24,78,44,92],0,3,14,'staging')
    byid={z['id']:z for z in zones}

    def door(a,b,x,z,width=1.2,access='custody'):
        ra,rb=byid[a],byid[b];assert ra['y']==rb['y'];y=ra['y']
        links.append(dict(id=f'L{len(links)+1:03}',a=a,b=b,kind='level',width=width,
            points=[review.center(ra),[x,y,z],review.center(rb)],threshold=[x,y,z],access=access,
            note='Proposed physical threshold; route diagrams do not prove traversability or lock behavior.'))
    for cell in cells:
        wing=cell['wing'];floor='lower' if cell['standing_y']==0 else 'upper'
        door(cell['space'],f'{wing}-{floor}-gallery',cell['door'][0],cell['door'][2],1.05,'individual-cell')
    for wing in ['a','b']:
        gx=15 if wing=='a' else 53;edge=16 if wing=='a' else 52
        door(f'{wing}-lower-gallery',f'{wing}-rear-link',gx,24)
        door(f'{wing}-lower-gallery',f'{wing}-front-link',gx,52)
        door(f'{wing}-lower-gallery',f'{wing}-dayhall',edge,42,1.8)
        door(f'{wing}-lower-gallery',f'{wing}-change',edge,26)
        door(f'{wing}-change',f'{wing}-showers',19 if wing=='a' else 49,25)
        door(f'{wing}-change',f'{wing}-rear-stair',17.5 if wing=='a' else 50.5,28)
        door(f'{wing}-rear-stair',f'{wing}-dayhall',18 if wing=='a' else 50,38,1.5)
        door(f'{wing}-rear-bypass',f'{wing}-dayhall',21 if wing=='a' else 47,38,1.5)
        door(f'{wing}-upper-rear-stair',f'{wing}-upper-gallery',edge,36.5,1.5)
        door(f'{wing}-rear-link','cross',gx,22,1.5)
        door(f'{wing}-dayhall',f'{wing}-stair',18 if wing=='a' else 50,46,1.5)
        door(f'{wing}-dayhall',f'{wing}-front-aisle',21 if wing=='a' else 47,46,1.5)
        door(f'{wing}-stair',f'{wing}-front-link',edge,55,1.5)
        door(f'{wing}-upper-stair',f'{wing}-upper-gallery',edge,56,1.5)
        x=18 if wing=='a' else 50
        links.append(dict(id=f'L{len(links)+1:03}',a=f'{wing}-stair',b=f'{wing}-upper-stair',kind='stairs',
            width=1.5,points=[[x,0,48],[x,3.4,53.44]],rise_m=3.4,run_m=5.44,headroom_m=2.2,
            landing_depth_m=1.5,risers=20,construction='Independent straight flight, full-run deck void, top bridge to cell-door gallery and both end landings.',note='Actual stair geometry, guards and headroom remain to be built.'))
        links.append(dict(id=f'L{len(links)+1:03}',a=f'{wing}-rear-stair',b=f'{wing}-upper-rear-stair',kind='stairs',
            width=1.5,points=[[x,0,30],[x,3.4,35.44]],rise_m=3.4,run_m=5.44,headroom_m=2.2,
            landing_depth_m=1.5,risers=20,construction='Second separated gallery flight and landing; same required deck opening and guard contract.',note='Provides alternate vertical circulation instead of a single long gallery trap.'))
    for row in [
        ('entry','sally',34,68),('entry','reception',14,68),('reception','intake',14,62),
        ('reception','records',18,65),('entry','public-wc',24,68),('intake','holding',18,60),
        ('holding','search',22,60),('search','sally',26,61),('intake','a-front-link',14,58),
        ('sally','guard',42,61),('guard','staff',44,64),('guard','staff-wc',48.4,64),('guard','guard-link',50,61),
        ('guard-link','b-front-link',55,58),('guard','b-front-aisle',47,58),
        ('sally','yard',34,58),('a-dayhall','yard',22,42),('b-dayhall','yard',46,42),
        ('yard','cross',40,22),('yard','plant-stair',28,28),('plant-stair','cross',30,22),
        ('entry','visitors',3,68),('intake','visitor-custody',10,60),
        ('chapel','isolation-link',59.5,34),
        ('isolation-link','cross',58,19),('isolation-link','observation',61,17),
        ('isolation-link','isolation-cell-1',61,20.25),('isolation-link','isolation-cell-2',61,24.75),
        ('observation','isolation-service',68,18),('isolation-link','isolation-wash',61,30.5),
        ('cross','dining',16,16),('dining','kitchen',22,8),('kitchen','food-store',23.5,12),
        ('kitchen','wash-up',26.5,12),('kitchen','north-service',25,4),
        ('cross','medical-link',30,16),('medical-link','exam',32,7),('exam','recovery',39,7),
        ('medical-link','medical-store',32,13),('medical-store','sluice',39,13),
        ('lower-stair','plant',32,31),('plant','laundry',38,34),
        ('entry','east-service',73,68),('east-service','north-service',73,4),
        ('north-service','west-service',3,4),('west-service','visitors',3,58),('stage','entry',34,78)]:
        access='service' if any(k in row[:2] for k in ['north-service','east-service','west-service','plant','laundry','lower-stair','kitchen','food-store','wash-up']) else 'custody'
        if set(row[:2]) in [{'entry','visitors'},{'entry','reception'},{'entry','public-wc'}]:access='public'
        door(*row,access=access)
    links.append(dict(id=f'L{len(links)+1:03}',a='lower-stair',b='plant-stair',kind='stairs',width=1.5,
        points=[[30,-3,24],[30,0,28.8]],rise_m=3,run_m=4.8,headroom_m=2.2,landing_depth_m=1.5,
        risers=18,construction='Independent service stair with saved editor world hole and matching ground deck void.',note='Do not allow the yard terrain or a backdrop sheet to cap the plant.'))
    return dict(version=2,id='PRISON',title='Beckenburg Penitentiary — revised cellhouse rough',
        status='S02_DESIGN_STUDY_NOT_RUNTIME_ACCEPTED',site_bounds=[-4,-4,78,94],zones=zones,links=links,
        capacity='32 single cells across two wings and two floors, plus two isolation places; 34 prisoners maximum in the proposed normal-use program.',
        rituals=[dict(rite='Banishment',zone='isolation-wash',point=[65.5,0,30.5]),
                 dict(rite='Cleansing',zone='sluice',point=[42.5,0,13]),
                 dict(rite='Absolution',zone='chapel',point=[64,0,43])],
        staging=dict(zone='stage',point=[34,0,85],yaw=-math.pi/2,cargo_route=[[30.372,0,85],[34,0,73],[34,0,68]],note='Canonical VanStaging transform; actual shared van must appear in later arrival review.'),
        terrain_cutouts=[dict(bounds=[28,22,46,42],world_surface_y=-.65,
            method='Persisted editor topology under the full plant/laundry footprint, not authored yet')],
        cell_fit_reservations=cells,
        fit_assets=['level_trace_camp_bed','toilet_mp_1','level_bath_basin','level_workbench'],
        asset_decision='Camp-bed bounds provide a full-scale compact-bed fit reference only; author a fixed steel cell-bed derivative instead of placing a folding camp bed as final prison furniture.',
        security_interfaces=[dict(spaces=['visitors','visitor-custody'],plane_x=6,open_door=False,
                                 note='Glazed/meshed supervised booth divider; no visitor-to-custody through-route.'),
                             dict(spaces=['sally','yard'],two_sided_manual_release=True,
                                  note='Ordinary custody control and event behavior cannot softlock hunters on power failure; physical gate/release acceptance required.')],
        decisions=['Build 32 actual cell rooms with independent doors, native-sized sanitary fittings and two storeys of continuous cell-door gallery.',
                   'Preserve the exercise yard as an orientation landmark; shape its terrain around the service headhouse instead of stamping a rectangular pad.',
                   'Public visitors approach reception and exterior booth entrance without passing through cell blocks or medical care.',
                   'Medical examination, recovery, clean stores and soiled utility have separate bounded functions; medical access comes from the rear distribution passage.',
                   'Kitchen deliveries and refuse use the north service apron; prisoner movement does not cross cooking/store rooms.'],
        remaining=['Resolve individual prisoner/staff/visitor fixture positions and privacy screens with actual full-scale support and approach rays.',
                   'Construct the six shared shower bays, floor drains and two isolation sanitary bays in actual geometry.',
                   'Author booth glazing, security release mechanisms, roof structure, cell windows, services and exact guard/deck voids.',
                   'Complete native terrain holes, actual van/capsule stair routes, hunt/ritual tests, networking and runtime/native/GPU/human acceptance.'])


def mansion():
    zones,links=[],[]
    def room(key,name,bounds,y,owner,kind='inside',height=3.6):
        zones.append(dict(id=key,name=name,bounds=bounds,y=y,height=height,owner=owner,kind=kind,purpose=name))
    for key,name,bounds,owner,kind in [
        ('foyer','Entrance vestibule and guest orientation',[20,44,24,50],0,'circulation'),
        ('visitor-wc','Visitor WC outside private residence',[24,44,28,47],0,'inside'),
        ('coats','Guest coats and event cloakroom',[24,47,28,50],0,'inside'),
        ('hall','Formal reception and stair approach',[20,36,28,44],0,'circulation'),
        ('grand-stair','Grand stair with clear lower landing',[20,24,28,36],0,'circulation'),
        ('library','Library shelves and reading tables',[8,24,20,36],1,'inside'),
        ('drawing','Drawing room seated reception',[8,36,20,50],5,'inside'),
        ('dining','Twelve-place formal dining',[28,32,40,42],2,'inside'),
        ('morning','Morning room and household breakfast',[28,42,40,50],6,'inside'),
        ('ballroom','Clear-span ballroom without upper deck',[40,30,54,50],4,'inside'),
        ('service-pass','Dining serving passage',[28,28,40,32],3,'circulation'),
        ('kitchen','Cooking and preparation with separate wash-up',[28,20,36,28],3,'inside'),
        ('scullery','Dirty return and utensil washing',[36,20,40,28],3,'inside'),
        ('pantry','Dry provisions and preparation pantry',[28,14,34,20],3,'inside'),
        ('cold-store','Cold storage and dairy work',[34,14,40,20],3,'inside'),
        ('service','Rear household service distribution',[40,14,43,30],9,'circulation'),
        ('receiving','Delivery inspection and unpacking',[43,20,49,26],3,'inside'),
        ('refuse','Enclosed waste awaiting service collection',[49,20,53,26],3,'inside'),
        ('service-stair','Independent household service stair',[40,2,46,14],9,'circulation')]:
        room(key,name,bounds,0,owner,kind,7.2 if key=='ballroom' else 3.6)
    for key,name,bounds,owner,kind in [
        ('gallery','Upper formal gallery and stair landing',[20,24,28,50],7,'circulation'),
        ('suite-entry','Private suite entrance and sitting lobby',[8,36,20,40],8,'circulation'),
        ('suite-bed','Principal bedroom',[8,40,16,50],8,'inside'),
        ('suite-dress','Principal dressing room',[16,40,20,46],8,'inside'),
        ('suite-bath','Principal private bath and WC',[16,46,20,50],8,'inside'),
        ('guest-a','Guest bedroom A',[8,24,16,30],7,'inside'),
        ('guest-a-entry','Guest A private entry',[16,27,20,30],7,'circulation'),
        ('guest-a-bath','Guest A private washing and WC',[16,24,20,27],7,'inside'),
        ('guest-b','Guest bedroom B',[8,30,16,36],7,'inside'),
        ('guest-b-entry','Guest B private entry',[16,33,20,36],7,'circulation'),
        ('guest-b-bath','Guest B private washing and WC',[16,30,20,33],7,'inside'),
        ('upper-service-link','Controlled formal-to-service threshold',[28,28,40,32],9,'circulation'),
        ('staff-corridor','Staff sleeping access',[28,20,40,24],9,'circulation'),
        ('staff-a','Two staff beds and personal storage A',[28,14,34,20],9,'inside'),
        ('staff-b','Two staff beds and personal storage B',[34,14,40,20],9,'inside'),
        ('staff-bath','Staff washing and separate WC bays',[28,24,34,28],9,'inside'),
        ('linen','Clean linen and household supplies',[34,24,40,28],9,'inside'),
        ('upper-service','Upper service passage',[40,14,43,30],9,'circulation'),
        ('upper-service-stair','Service stair upper landing',[40,2,46,14],9,'circulation')]:
        room(key,name,bounds,3.6,owner,kind)
    room('attic-stair','Attic stair and guarded landing',[40,2,46,14],7.2,10,'circulation',3)
    room('attic-passage','Attic distribution under roof',[40,14,43,28],7.2,10,'circulation',3)
    room('archive','Estate archive: dry papers and shelving',[28,14,40,21],7.2,10,height=3)
    room('attic-store','Disused furniture and clear ritual bay',[28,21,40,28],7.2,10,height=3)
    room('cellar-stair','Cellar service stair',[40,2,46,14],-3,11,'circulation',3)
    room('cellar-pass','Cellar service distribution',[40,14,43,28],-3,11,'circulation',3)
    room('plant','Heat hot water and electrical maintenance',[32,14,40,20],-3,11,height=3)
    room('cellar-provisions','Cool provisions separate from plant',[28,14,32,20],-3,11,height=3)
    room('wine','Wine racks and retrieval aisle',[28,20,40,28],-3,11,height=3)
    room('garden','Estate garden envelope: terrain not accepted',[3,0,57,50],0,12,'outside',12)
    room('entry','Entrance terrace and carriage approach',[3,50,57,65],0,13,'outside',12)
    room('stage','Canonical van unloading reservation',[20,65,36,78],0,14,'staging',3)
    byid={z['id']:z for z in zones}
    def door(a,b,x,z,width=1.2,access='household'):
        y=byid[a]['y'];assert byid[b]['y']==y
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='level',width=width,
            points=[review.center(byid[a]),[x,y,z],review.center(byid[b])],threshold=[x,y,z],access=access,
            note='Proposed physical threshold; diagram lines do not establish navigation.'))
    for a,b,x,z in [
        ('stage','entry',28,65),('entry','foyer',22,50),('foyer','visitor-wc',24,45.5),('foyer','coats',24,48.5),
        ('foyer','hall',22,44),('hall','grand-stair',24,36),('hall','drawing',20,40),('grand-stair','library',20,30),
        ('library','drawing',14,36),('hall','dining',28,39),('hall','morning',28,43),('dining','morning',34,42),
        ('dining','ballroom',40,37),('morning','ballroom',40,46),('dining','service-pass',34,32),
        ('service-pass','kitchen',32,28),('service-pass','scullery',38,28),('kitchen','scullery',36,24),
        ('kitchen','pantry',31,20),('scullery','cold-store',38,20),('scullery','service',40,24),
        ('service-pass','service',40,29),('service','service-stair',41.5,14),('service','receiving',43,23),
        ('receiving','refuse',49,23),('receiving','garden',46,20),('refuse','garden',53,23),
        ('service-stair','garden',43,2),('library','garden',8,30),('ballroom','garden',54,40),('garden','entry',55,50),
        ('gallery','suite-entry',20,38),('suite-entry','suite-bed',12,40),('suite-entry','suite-dress',18,40),
        ('suite-dress','suite-bath',18,46),('gallery','guest-a-entry',20,28.5),('guest-a-entry','guest-a',16,28.5),
        ('guest-a-entry','guest-a-bath',18,27),('gallery','guest-b-entry',20,34.5),('guest-b-entry','guest-b',16,34.5),
        ('guest-b-entry','guest-b-bath',18,33),('gallery','upper-service-link',28,30),
        ('upper-service','linen',40,26),('linen','staff-corridor',37,24),('staff-corridor','staff-bath',31,24),
        ('staff-corridor','staff-a',31,20),('staff-corridor','staff-b',37,20),('staff-corridor','upper-service',40,22),
        ('upper-service-link','upper-service',40,29),('upper-service','upper-service-stair',41.5,14),
        ('attic-stair','attic-passage',41.5,14),('attic-passage','archive',40,18),('attic-passage','attic-store',40,25),
        ('cellar-stair','cellar-pass',41.5,14),('cellar-pass','plant',40,17),('cellar-pass','wine',40,24),
        ('wine','cellar-provisions',30,20)]:door(a,b,x,z)
    for a,b,x,z,y0,y1,width in [
        ('grand-stair','gallery',24,26,0,3.6,1.8),
        ('cellar-stair','service-stair',43,4,-3,0,1.3),
        ('service-stair','upper-service-stair',43,4,0,3.6,1.3),
        ('upper-service-stair','attic-stair',43,4,3.6,7.2,1.3)]:
        rise=y1-y0;run=rise*1.6
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='stairs',width=width,points=[[x,y0,z],[x,y1,z+run]],
            rise_m=rise,run_m=run,headroom_m=2.2,landing_depth_m=1.5,risers=20 if rise>3 else 18,
            construction='Separate flights stacked with full-run deck voids, real structure depth and continuous guards; landings remain clear.',
            note='Measured reservation only; physical capsule/headroom and underside tests remain required.'))
    return dict(version=2,id='MANSION',title='Blackthorn Manor — revised household-first rough',
        status='S02_DESIGN_STUDY_NOT_RUNTIME_ACCEPTED',site_bounds=[3,0,57,79],zones=zones,links=links,
        capacity='Principal pair, two guest bedrooms for up to four guests, four staff beds; twelve-place formal dining.',
        fit_assets=['bed_4','bed_5','toilet_mp_1','level_bath_basin','level_bathtub'],
        prop_fit_reservations=[
            dict(space=space,asset=asset,origin=origin) for space,asset,origin in [
                ('suite-bed','bed_4',[12,3.6,45]),('guest-a','bed_4',[12,3.6,26.5]),('guest-b','bed_4',[12,3.6,32.5]),
                ('staff-a','bed_5',[31,3.6,15.7]),('staff-a','bed_5',[31,3.6,18.3]),
                ('staff-b','bed_5',[37,3.6,15.7]),('staff-b','bed_5',[37,3.6,18.3]),
                ('suite-bath','level_bathtub',[18,3.585,49]),('suite-bath','toilet_mp_1',[19.3,3.835,47.1]),
                ('suite-bath','level_bath_basin',[16.6,3.6,47.1]),
                ('guest-a-bath','toilet_mp_1',[19.2,3.835,25]),('guest-a-bath','level_bath_basin',[16.6,3.6,25]),
                ('guest-b-bath','toilet_mp_1',[19.2,3.835,31]),('guest-b-bath','level_bath_basin',[16.6,3.6,31])]],
        rituals=[dict(rite='Banishment',zone='ballroom',point=[47,0,40]),
                 dict(rite='Cleansing',zone='wine',point=[34,-3,24]),
                 dict(rite='Absolution',zone='garden',point=[14,0,14])],
        staging=dict(zone='stage',point=[28,0,71],yaw=-math.pi/2,cargo_route=[[24.372,0,71],[22,0,57],[22,0,50]],
            note='Use actual canonical VanStaging placement; apron and cart route remain to be physically proved.'),
        terrain_cutouts=[dict(bounds=bounds,world_surface_y=-.65,
            method='Persisted editor AdaptiveTerrainTopology punched leaves beneath cellar/stair union; not yet authored')
            for bounds in [[40,2,46,14],[28,14,43,28]]],
        decisions=['Formal arrival reaches reception, library, drawing, dining and ballroom without crossing kitchens or private bedrooms.',
            'Every guest bedroom has a bounded entry and private washing/WC; principal bedroom, dressing and bath are separately bounded.',
            'Delivery, unpacking, refuse, cold/dry storage, cooking and dirty wash-up occupy a service wing with its own circulation.',
            'Four staff beds and staff sanitation are reached from the service stair; cellar and attic do not require traversing a bedroom.',
            'Ballroom is a tall independent wing: no automatic upper floor stamps; roof structure must span it credibly.'],
        remaining=['Resolve actual furniture/fixture support, full-scale room fit, door swings and screened sanitary approaches.',
            'Author stacked service stair return/landing guard interfaces, grand stair floor openings and roof/headroom geometry.',
            'Separate electrical/hot-water plant from food and paper stores with actual enclosure and maintenance access.',
            'Punch saved editor world terrain beneath the complete cellar; test render/body/optical/object passage with filled-ground negatives.',
            'Build terrain and garden around play instead of rectangular pads; prove native/runtime parity, van routes, hunts, rituals, networking and hardware performance.'])


def farmhouse():
    zones,links=[],[]
    def room(key,name,bounds,y,owner,kind='inside',height=3):
        zones.append(dict(id=key,name=name,bounds=bounds,y=y,height=height,owner=owner,kind=kind,purpose=name))
    for key,name,bounds,owner,kind in [
        ('mudroom','Boot removal coats and muddy entrance',[20,22,26,28],0,'circulation'),
        ('parlour','Family sitting and visitor reception',[12,20,20,28],1,'inside'),
        ('dining','Six-place family dining',[12,16,20,20],3,'inside'),
        ('kitchen','Cooking and sink cleanup',[12,13,20,16],2,'inside'),
        ('pantry','Dry household food storage',[12,10,15,13],2,'inside'),
        ('prep','Food preparation and garden receiving',[15,10,20,13],2,'inside'),
        ('stairs','Domestic stair bay with side circulation',[20,12,26,22],4,'circulation'),
        ('cleaner','Household cleaning and utility cupboard',[20,10,26,12],4,'inside'),
        ('barn-aisle','Through barn aisle and loading approach',[35,9,39,24],10,'circulation'),
        ('stall-a','Livestock stall A and access gate',[31,9,35,15],10,'inside'),
        ('stall-b','Livestock stall B and access gate',[31,15,35,21],10,'inside'),
        ('feed','Feed store separate from equipment',[31,21,35,24],10,'inside'),
        ('equipment','Agricultural equipment bay',[39,9,43,16],10,'inside'),
        ('loft-stair','Barn loft stair and lower landings',[39,16,43,24],10,'circulation'),
        ('repair','Tool repair bench and clear work aisle',[31,26,36,33],11,'inside'),
        ('parts','Repair parts and dry tools',[36,26,39,30],11,'inside'),
        ('work-store','Screened work consumables and lockable storage',[36,30,39,33],11,'inside'),
        ('laundry','Separate wash laundry and drying support',[27,26,31,33],11,'inside')]:room(key,name,bounds,0,owner,kind)
    for key,name,bounds,owner,kind in [
        ('upper-hall','Upper bedroom circulation',[20,10,23,28],5,'circulation'),
        ('upper-stair','Main stair upper landing and void',[23,12,26,22],5,'circulation'),
        ('wc','Private family WC',[23,22,26,25],5,'inside'),
        ('wash','Family washing room',[23,25,26,28],5,'inside'),
        ('parents','Parents bedroom and personal storage',[12,20,20,28],6,'inside'),
        ('children','Two child beds and play/storage',[12,14,20,20],7,'inside'),
        ('linen','Household linen and seasonal storage',[12,10,20,14],5,'inside'),
        ('hayloft','Supported hay deck with clear retrieval aisle',[31,16,39,24],8,'inside'),
        ('loft-landing','Guarded loft stair arrival',[39,16,43,24],8,'circulation')]:room(key,name,bounds,3,owner,kind)
    for key,name,bounds,kind in [
        ('cellar-stair','Cellar access below domestic stair bay',[20,12,26,22],'circulation'),
        ('cellar-landing','Cellar receiving landing',[20,22,26,28],'circulation'),
        ('root-store','Root crop storage and retrieval aisle',[12,14,20,28],'inside'),
        ('preserves','Preserved food shelves',[12,10,20,14],'inside')]:room(key,name,bounds,-3,9,kind)
    room('grounds','Orchard and workyard envelope only',[3,0,48,34],0,12,'outside',12)
    room('entry','Lane arrival and equipment approach',[3,34,50,50],0,13,'outside',12)
    room('stage','Canonical van staging clear of barn deliveries',[18,38,34,50],0,14,'staging')
    byid={z['id']:z for z in zones}
    def door(a,b,x,z,width=1.2,access='household'):
        y=byid[a]['y'];assert byid[b]['y']==y
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='level',width=width,points=[review.center(byid[a]),[x,y,z],review.center(byid[b])],threshold=[x,y,z],access=access,
            note='Proposed opening; diagram connectivity is not actual capsule traversal.'))
    for a,b,x,z in [
        ('stage','entry',26,38),('entry','grounds',24,34),('grounds','mudroom',23,28),
        ('mudroom','parlour',20,25),('mudroom','stairs',23,22),('parlour','dining',16,20),
        ('dining','kitchen',16,16),('dining','stairs',20,18),('kitchen','prep',17,13),
        ('prep','pantry',15,11.5),('prep','grounds',17,10),('stairs','cleaner',23,12),
        ('upper-hall','upper-stair',23,21),('upper-hall','wc',23,23.5),('upper-hall','wash',23,26.5),
        ('upper-hall','parents',20,24),('upper-hall','children',20,17),('upper-hall','linen',20,11.5),
        ('cellar-stair','cellar-landing',23,22),('cellar-landing','root-store',20,25),('root-store','preserves',16,14),
        ('grounds','barn-aisle',37,9),('grounds','barn-aisle',37,24),('barn-aisle','stall-a',35,12),
        ('barn-aisle','stall-b',35,18),('barn-aisle','feed',35,22.5),('barn-aisle','equipment',39,12),
        ('barn-aisle','loft-stair',39,17.5),('loft-landing','hayloft',39,23),
        ('grounds','repair',33,26),('repair','parts',36,28),('repair','work-store',36,31.5),
        ('grounds','laundry',29,33)]:door(a,b,x,z,2.4 if 'barn-aisle' in (a,b) and 'grounds' in (a,b) else 1.2)
    for a,b,x,z,y0,y1,width in [
        ('stairs','upper-stair',24.5,13.6,0,3,1.2),
        ('cellar-stair','stairs',21.5,13.6,-3,0,1.2),
        ('loft-stair','loft-landing',41,17.5,0,3,1.2)]:
        run=(y1-y0)*1.6
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='stairs',width=width,points=[[x,y0,z],[x,y1,z+run]],rise_m=y1-y0,run_m=run,headroom_m=2.2,landing_depth_m=1.5,risers=18,
            construction='Full-run deck void and structural depth; reserve continuous guards, side bypass and clear end landings.',
            note='Domestic flights use separate X lanes; no assumed ladder-only hayloft access. Physical proof remains open.'))
    return dict(version=2,id='FARMHOUSE',title='Hollowmere Farm — revised domestic and working-yard rough',
        status='S02_DESIGN_STUDY_NOT_RUNTIME_ACCEPTED',site_bounds=[3,0,50,51],zones=zones,links=links,
        capacity='Family of four, six dining places, two livestock stalls and bounded equipment/feed work areas.',
        rituals=[dict(rite='Banishment',zone='hayloft',point=[35,3,20]),dict(rite='Cleansing',zone='root-store',point=[16,-3,21]),dict(rite='Absolution',zone='grounds',point=[7,0,5])],
        staging=dict(zone='stage',point=[26,0,44],yaw=-math.pi/2,cargo_route=[[22.372,0,44],[23,0,34],[23,0,28]],note='Canonical van source; terrain support and six-player unloading remain unproved.'),
        terrain_cutouts=[dict(bounds=b,world_surface_y=-.65,method='Actual saved editor punched leaves, not yet authored') for b in [[12,10,20,28],[20,12,26,28]]],
        fit_assets=['bed_4','bed_5','level_stall','level_hay_bale','toilet_mp_1','level_bath_basin'],
        prop_fit_reservations=[dict(space=space,asset=asset,origin=origin) for space,asset,origin in [
            ('parents','bed_4',[16,3,24]),('children','bed_5',[16,3,15.8]),('children','bed_5',[16,3,18.3]),
            ('wc','toilet_mp_1',[24.5,3.235,23.1]),('wash','level_bath_basin',[25.4,3,26]),
            ('stall-a','level_stall',[33,0,12]),('stall-b','level_stall',[33,0,18])]],
        decisions=['Retain the original house/barn/tool-shed grouping and source floor elevations; extend the domestic north/south walls to Z10/28 to reserve storage and proper landings instead of preserving a deficient room trace.',
            'Provide private bounded WC and washing rooms off upper circulation, not a basin standing in the landing.',
            'Food preparation and pantry are distinct from muddy entrance and household cleaning storage.',
            'Barn gates open onto a four-metre through aisle; stall/feed/equipment functions remain bounded, with a real stair to the hay deck.',
            'Laundry has its own workyard entrance and drainage requirement outside the tool/consumables enclosure.'],
        remaining=['Fit and support native full-scale furniture, stall gates, feed bins, workbench, bathroom and washing fixtures; reserve real door swings.',
            'Author foundation/roof/loft support, daylight/ventilation, dry hay handling and separated utility/electrical services.',
            'Punch the saved world terrain beneath the cellar union and build actual deck/stair openings; test body/ray/object passage and filled-ground negatives.',
            'Shape farm terrain around lane, orchard, drained workyard and outbuildings; prove canonical van support, six-player/hunt routes, rituals, networking, native parity and hardware performance.'])


def asylum():
    zones,links,fit,sleeping=[],[],[],[]
    def room(key,name,bounds,y,owner,kind='inside',height=3.4):
        zones.append(dict(id=key,name=name,bounds=bounds,y=y,height=height,owner=owner,kind=kind,purpose=name))
    for prefix,y,owner in [('ground',0,4),('upper',3.4,7)]:
        for key,x,z in [('north-a',10,12),('north-b',16,12),('south-a',10,22),('south-b',16,22)]:
            sid=f'{prefix}-{key}';room(sid,f'{prefix.title()} two-place patient room {key}',[x,z,x+6,z+7],y,owner);sleeping.append(sid)
            for offset in [2.1,5]:fit.append(dict(space=sid,asset='bed_5',origin=[x+3,y,z+offset]))
        for key,name,bounds,kind in [
            ('ward-link','Daylight ward distribution',[10,19,26,22],'circulation'),
            ('observation','Staff observation and notes',[22,12,26,16],'inside'),
            ('linen','Ward clean linen and supplies',[22,16,26,19],'inside'),
            ('wc','Screened patient WC bays',[22,22,26,25.5],'inside'),
            ('wash','Private patient washing and assistance',[22,25.5,26,29],'inside')]:room(prefix+'-'+key,prefix.title()+' '+name,bounds,y,owner,kind)
    for key,name,bounds,owner,kind in [
        ('spine','Main patient and staff distribution',[26,12,29,50],0,'circulation'),
        ('main-stair','Main stair with clear bypass',[29,20,35,30],0,'circulation'),
        ('staff-office','Staff coordination and private work',[29,12,35,20],0,'inside'),
        ('service-link','Staff-only service distribution',[35,16,54,20],0,'circulation'),
        ('admission','Public reception and waiting',[26,50,35,58],1,'inside'),
        ('interview','Private admissions interview',[35,50,41,54],1,'inside'),
        ('visitor-wc','Visitor WC outside care wards',[35,54,38,58],1,'inside'),
        ('records','Confidential records office',[38,54,44,58],8,'inside'),
        ('day','Supervised sitting crafts and ordinary activity',[10,40,26,50],2,'inside'),
        ('clinical-link','Clinical approach outside work rooms',[29,30,35,40],5,'circulation'),
        ('treatment','Treatment examination and handwashing',[35,30,45,40],5,'inside'),
        ('clinical-clean','Clean treatment stores',[35,26,40,30],5,'inside'),
        ('clinical-soiled','Enclosed soiled treatment utility',[40,26,45,30],5,'inside'),
        ('clinical-service','Rear clinical utility service approach',[35,24,54,26],0,'circulation'),
        ('staff-rest','Staff rest and changing',[35,20,40,24],0,'inside'),
        ('staff-wc','Staff sanitary room',[40,20,45,24],0,'inside'),
        ('hydro-change','Private hydrotherapy changing',[45,30,49,35],6,'inside'),
        ('hydro-bath','Assisted bathing treatment room',[49,30,54,40],6,'inside'),
        ('hydro-dry','Private drying and recovery',[45,35,49,40],6,'inside'),
        ('dining','Sixteen-place ward dining',[29,40,41,50],3,'inside'),
        ('kitchen','Food preparation cooking and serving',[41,44,48,48],3,'inside'),
        ('washup','Meal return and dish washing',[48,44,54,48],3,'inside'),
        ('meal-return','Dining dish return outside cooking',[41,48,54,50],3,'circulation'),
        ('food-store','Dry and cool food storage reservations',[41,40,48,44],3,'inside'),
        ('receiving','Food and household receiving',[48,40,54,44],3,'inside'),
        ('service','Service passage separated from care rooms',[54,24,57,50],0,'circulation'),
        ('refuse','Enclosed refuse awaiting collection',[57,40,61,44],3,'inside'),
        ('service-stair','Independent basement service stair',[54,12,60,24],0,'circulation'),
        ('quiet-lobby','Calm-room care and staff approach',[45,12,54,16],9,'circulation'),
        ('quiet','Short-stay quiet care room',[45,4,50,12],9,'inside'),
        ('quiet-observation','Quiet care observation and support',[50,4,54,12],9,'inside'),
        ('quiet-wc','Private quiet-care WC and washing',[45,0,50,4],9,'inside'),
        ('chapel','Quiet remembrance chapel',[10,2,22,10],10,'inside'),
        ('lift','Patient lift lower landing and shaft reservation',[22,31,26,36],0,'circulation')]:room(key,name,bounds,0,owner,kind)
    room('upper-spine','Upper ward circulation',[26,12,29,40],3.4,7,'circulation')
    room('upper-stair','Upper main stair and guarded landing',[29,20,35,30],3.4,7,'circulation')
    room('upper-lift','Patient lift upper landing reservation',[22,31,26,36],3.4,7,'circulation')
    for key,name,bounds,kind in [
        ('cellar-stair','Basement service stair',[54,12,60,24],'circulation'),
        ('cellar-service','Basement service distribution',[54,24,57,50],'circulation'),
        ('cellar-cross','Plant and laundry approach',[35,36,54,40],'circulation'),
        ('plant','Boiler hot water and electrical operating clearances',[35,24,45,36],'inside'),
        ('maintenance','Maintenance workshop and stores',[45,24,54,36],'inside'),
        ('dirty-laundry','Soiled linen receiving and washing',[35,40,39,50],'inside'),
        ('clean-laundry','Clean laundry drying and finishing',[39,40,43,50],'inside'),
        ('clean-store','Finished clean linen storage',[43,40,54,50],'inside')]:room(key,name,bounds,-3,11,kind,3)
    room('court','Daylight court: no upper deck',[10,29,26,40],0,12,'outside',12)
    room('grounds','Stepped pavilion grounds envelope only',[4,0,64,58],0,12,'outside',12)
    room('entry','Public drive and patient arrival',[4,58,64,70],0,13,'outside',12)
    room('stage','Canonical investigation van apron',[24,70,40,83],0,14,'staging',3)
    byid={z['id']:z for z in zones}
    def door(a,b,x,z,width=1.4,access='care'):
        y=byid[a]['y'];assert byid[b]['y']==y
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='level',width=width,points=[review.center(byid[a]),[x,y,z],review.center(byid[b])],threshold=[x,y,z],access=access,
            note='Authored proposed doorway; centre lines are diagrams, not host navigation proof.'))
    for prefix in ['ground','upper']:
        hall='spine' if prefix=='ground' else 'upper-spine'
        for key,x,z in [('north-a',13,19),('north-b',19,19),('south-a',13,22),('south-b',19,22)]:door(prefix+'-'+key,prefix+'-ward-link',x,z)
        for key,x,z in [('observation',26,14),('linen',26,17.5),('wc',26,24),('wash',26,27)]:door(prefix+'-'+key,hall,x,z)
        door(prefix+'-ward-link',hall,26,20.5)
    for a,b,x,z in [
        ('stage','entry',32,70),('entry','admission',30,58),('admission','visitor-wc',35,56),
        ('admission','interview',35,52),('interview','records',39.5,54),('admission','spine',27.5,50),
        ('spine','day',26,45),('day','court',18,40),('ground-south-a','court',13,29),
        ('spine','staff-office',29,16),('staff-office','service-link',35,18),('service-link','staff-rest',37.5,20),
        ('staff-rest','staff-wc',40,22),('spine','main-stair',29,28),('spine','clinical-link',29,35),
        ('clinical-link','treatment',35,35),('service','clinical-service',54,25),('clinical-service','clinical-clean',37.5,26),('clinical-service','clinical-soiled',42.5,26),('treatment','clinical-clean',37.5,30),('treatment','clinical-soiled',42.5,30),
        ('treatment','hydro-change',45,32.5),('hydro-change','hydro-bath',49,32.5),
        ('hydro-bath','hydro-dry',49,37.5),('hydro-dry','treatment',45,37.5),
        ('spine','dining',29,45),('dining','kitchen',41,46),('dining','meal-return',41,49),('meal-return','washup',51,48),
        ('kitchen','food-store',44,44),('receiving','food-store',48,42),('washup','receiving',51,44),('receiving','service',54,42),
        ('service','refuse',57,42),('service','service-stair',55.5,24),('service-stair','service-link',54,18),
        ('service-link','quiet-lobby',49,16),('quiet-lobby','quiet',47.5,12),('quiet-lobby','quiet-observation',52,12),
        ('quiet','quiet-wc',47.5,4),('quiet-observation','quiet',50,8),
        ('spine','lift',26,33.5),('upper-spine','upper-lift',26,33.5),('upper-spine','upper-stair',29,28),
        ('cellar-stair','cellar-service',55.5,24),('cellar-service','cellar-cross',54,38),
        ('cellar-cross','plant',40,36),('cellar-cross','maintenance',49,36),
        ('cellar-cross','dirty-laundry',37,40),('cellar-cross','clean-laundry',41,40),
        ('clean-laundry','clean-store',43,45),('cellar-cross','clean-store',48,40),
        ('grounds','chapel',16,10),('grounds','court',10,35),('grounds','entry',60,58),
        ('grounds','service-stair',57,12),('grounds','refuse',61,42),('grounds','service',57,48)]:door(a,b,x,z)
    for a,b,x,z,y0,y1,width in [('main-stair','upper-stair',32,22,0,3.4,1.6),('cellar-stair','service-stair',57,14,-3,0,1.5)]:
        rise=y1-y0;run=rise*1.6
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='stairs',width=width,points=[[x,y0,z],[x,y1,z+run]],rise_m=rise,run_m=run,headroom_m=2.2,landing_depth_m=1.5,risers=20 if rise>3 else 18,
            construction='Full deck openings, actual floor/roof depth, clear bypass and continuous guards required.',note='Physical movement, care access and lift operation remain unproved.'))
    return dict(version=2,id='ASYLUM',title='St. Dymphna Hospital — revised care-pavilion rough',
        status='S02_DESIGN_STUDY_NOT_RUNTIME_ACCEPTED',site_bounds=[4,0,64,84],zones=zones,links=links,
        capacity='Sixteen ward places: four two-place rooms per floor. Quiet care is short-stay support, not an extra permanent bed or a supernatural premise.',
        fit_assets=['bed_5','toilet_mp_1','level_bath_basin','level_bathtub'],prop_fit_reservations=fit,expected_single_beds=16,sleeping_spaces=sleeping,
        asset_decision='Native single beds establish full-scale spatial reservations only. Author and inspect appropriate clinical bed derivatives rather than presenting domestic masters as final hospital equipment.',
        rituals=[dict(rite='Banishment',zone='chapel',point=[16,0,6]),dict(rite='Cleansing',zone='plant',point=[40,-3,30]),dict(rite='Absolution',zone='day',point=[18,0,45])],
        staging=dict(zone='stage',point=[32,0,76],yaw=-math.pi/2,cargo_route=[[28.372,0,76],[30,0,64],[30,0,58]],note='Use actual canonical van; arrival surface/support and six-player unloading remain unproved.'),
        terrain_cutouts=[dict(bounds=b,world_surface_y=-.65,method='Persisted editor AdaptiveTerrainTopology punched leaves under actual cellar union; not yet authored') for b in [[35,24,57,50],[54,12,60,24]]],
        flow_reservations=[dict(id=key,path=path,avoid=avoid) for key,path,avoid in [
            ('public-visitor',['stage','entry','admission','visitor-wc'],sleeping+['treatment','quiet','hydro-bath']),
            ('food-delivery',['service','receiving','food-store','kitchen','dining'],['washup','treatment','clinical-soiled']),
            ('dirty-dishes',['dining','meal-return','washup'],['kitchen','food-store']),
            ('clean-clinical',['service','clinical-service','clinical-clean','treatment'],['clinical-soiled','staff-rest','staff-wc']),
            ('soiled-clinical',['treatment','clinical-soiled','clinical-service','service'],['clinical-clean','staff-rest','staff-wc']),
            ('dirty-linen',['service','service-stair','cellar-stair','cellar-service','cellar-cross','dirty-laundry'],['clean-laundry','clean-store','food-store','kitchen'])]],
        service_interfaces=[dict(spaces=['dirty-laundry','clean-laundry'],plane_x=39,through_door=False,operation='Sealed dirty-to-clean laundry transfer equipment; actual support/drains and contamination separation remain to be authored.')],
        lift_reservation=dict(spaces=['lift','upper-lift'],rise_m=3.4,operation='Patient transport lift required; no implemented elevator or navigation edge is claimed by this rough.',failure_route='Main physical stair remains available; validate mission return and host authority independently.'),
        decisions=['Replace generic cross-wing rooms with stepped west care pavilion, daylight court and east treatment/service wing.',
            'Eight patient places on each floor have their own ward circulation, observation, clean linen and separate screened WC/washing.',
            'Admissions interview and visitor sanitation remain outside private sleeping and treatment functions.',
            'Hydrotherapy has bounded changing, bathing and drying; treatment clean and soiled utility remain separate.',
            'Food receiving, cooking and dirty dish return are separate; laundry and plant use an independent service stair.',
            'Illness, admission and quiet-care rooms are normal-use functions, not explanations for haunting or automatic narrative punishment.'],
        remaining=['Fit clinical fixtures, bed derivatives, bedside clearances, screened assistance and services at actual source scale.',
            'Resolve clean/soiled laundry transfer interfaces, observation glazing and controlled care thresholds without ghost-event or power-loss softlocks.',
            'Author the actual patient lift, full-run stair/deck openings, roof/structure and courtyard access without stamping an upper floor over daylight.',
            'Punch saved editor world terrain under the complete cellar union; prove body/ray/object/render passage and filled-ground negatives.',
            'Complete terrain, native/runtime parity, van routes, electrical operation, hunts, rituals, networking, weathering/story and physical hardware acceptance.'])


def academy():
    zones,links,fit,sleeping=[],[],[],[]
    def room(key,name,bounds,y,owner,kind='inside',height=3.6):
        zones.append(dict(id=key,name=name,bounds=bounds,y=y,height=height,owner=owner,kind=kind,purpose=name))
    for key,name,bounds,owner,kind in [
        ('west','West sheltered cloister',[19,9,22,53],0,'circulation'),
        ('east','East teaching cloister and return',[42,19,45,55],0,'circulation'),
        ('north','North dining cloister',[22,19,42,22],0,'circulation'),
        ('south','South entrance cloister',[22,42,42,45],0,'circulation'),
        ('reception','Visitor reception and waiting',[22,45,32,53],1,'inside'),
        ('visitor-wc','Visitor WC outside residence',[32,45,36,49],1,'inside'),
        ('reception-store','Coats and visitor supplies',[32,49,36,53],1,'inside'),
        ('reading','Library quiet reading',[7,32,19,42],6,'inside'),
        ('library-issue','Library issue return and staff desk',[7,28,19,32],6,'inside'),
        ('stacks','Open library stacks',[7,22,19,28],6,'inside'),
        ('office','Headmaster private meeting room',[11,9,19,19],5,'inside'),
        ('office-entry','Covered administration approach',[7,19,19,22],5,'circulation'),
        ('staff-ground-wc','Staff private sanitary support',[7,9,11,13],5,'inside'),
        ('office-records','Confidential staff records',[7,13,11,19],5,'inside'),
        ('laundry','Household laundry and wash work',[22,4,28,9],0,'inside'),
        ('house-store','Household stores under north residence',[28,4,34,9],0,'inside'),
        ('staff-change','Kitchen and household staff changing',[34,4,40,9],5,'inside'),
        ('bulk-food','Bulk food supplies and delivery buffer',[40,4,46,9],4,'inside'),
        ('dining','Twenty-place pupil and staff dining',[22,9,34,19],4,'inside'),
        ('kitchen','Kitchen preparation and cooking',[34,9,40,15],4,'inside'),
        ('serving','Dining serving passage',[34,15,40,19],4,'circulation'),
        ('pantry','Food provisions',[40,9,45,12],4,'inside'),
        ('washup','Dirty dishes and cleanup',[40,12,45,19],4,'inside'),
        ('receiving','Service receiving and unpacking',[45,9,50,15],4,'inside'),
        ('refuse','Enclosed refuse awaiting removal',[45,15,50,19],4,'inside'),
        ('lecture','Sixteen-place teaching room',[45,32,57,45],2,'inside'),
        ('lab','Practical teaching benches and handwashing',[45,19,53,29],3,'inside'),
        ('lab-prep','Secure practical supplies and preparation',[53,19,57,25],3,'inside'),
        ('lab-wash','Practical cleanup and sink',[53,25,57,29],3,'inside'),
        ('lab-entry','Lab coats and approach',[45,29,57,32],3,'circulation'),
        ('main-stair','Main stair and archive approach',[7,42,19,53],0,'circulation'),
        ('east-stair','Independent east return stair',[45,45,57,55],0,'circulation')]:room(key,name,bounds,0,owner,kind)
    for key,name,bounds,owner,kind in [
        ('upper-west','Upper west residential passage',[19,9,22,53],7,'circulation'),
        ('upper-north','Upper dormitory distribution',[22,16,45,19],7,'circulation'),
        ('upper-east','Upper teaching-library return',[42,19,45,55],7,'circulation'),
        ('upper-main-stair','Main stair guarded upper landing',[7,42,19,53],7,'circulation'),
        ('upper-east-stair','East stair guarded upper landing',[45,45,57,55],7,'circulation'),
        ('student-change','Residential washing approach',[7,14,19,19],7,'circulation'),
        ('student-wc','Student screened WC bays',[7,9,13,14],7,'inside'),
        ('student-shower','Private student shower bays',[13,9,19,14],7,'inside'),
        ('linen','Residential linen and cleaning supplies',[7,19,19,22],7,'inside'),
        ('staff-a','Two resident staff beds A',[7,22,13,30],5,'inside'),
        ('staff-b','Two resident staff beds B',[13,22,19,30],5,'inside'),
        ('staff-hall','Staff sleeping approach',[7,30,19,33],5,'circulation'),
        ('staff-wash','Resident staff WC and washing',[7,33,13,38],5,'inside'),
        ('staff-work','Resident staff work room',[13,33,19,38],5,'inside'),
        ('staff-common','Staff common room',[7,38,19,42],5,'inside'),
        ('restricted','Supervised restricted library stacks',[45,19,57,29],10,'inside'),
        ('study','Quiet advanced study and research',[45,32,57,45],10,'inside')]:room(key,name,bounds,3.6,owner,kind)
    for i in range(4):
        x=22+i*6;sid=f'dorm-{i+1}';room(sid,f'Four-place pupil sleeping room {i+1}',[x,4,x+6,16],3.6,8 if i<2 else 9);sleeping.append(sid)
        for z in [6,8.8,11.6,14.4]:fit.append(dict(space=sid,asset='bed_5',origin=[x+3,3.6,z]))
    for key,x in [('staff-a',7),('staff-b',13)]:
        sleeping.append(key)
        for z in [24,27]:fit.append(dict(space=key,asset='bed_5',origin=[x+3,3.6,z]))
    for key,name,bounds,kind in [('archive','Dry institutional archive',[7,22,13,38],'inside'),('plant','Heat water and electrical maintenance',[13,22,19,38],'inside'),('cellar-hall','Archive and plant service approach',[7,38,19,42],'circulation'),('cellar-stair','Cellar stair below entrance range',[7,42,19,53],'circulation')]:room(key,name,bounds,-3,11,kind,3)
    room('quad','Open quadrangle with real daylight',[22,22,42,42],0,12,'outside',12)
    room('grounds','Academy service grounds envelope',[3,0,62,55],0,12,'outside',12)
    room('entry','Gate court and public approach',[3,55,62,68],0,13,'outside',12)
    room('stage','Canonical van gate apron',[24,68,40,81],0,14,'staging',3)
    byid={z['id']:z for z in zones}
    def door(a,b,x,z,width=1.3):
        y=byid[a]['y'];assert byid[b]['y']==y
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='level',width=width,points=[review.center(byid[a]),[x,y,z],review.center(byid[b])],threshold=[x,y,z],access='household' if 'staff' in a+b else 'academic',note='Exact proposed opening; diagram paths are not runtime movement.'))
    for a,b,x,z in [
        ('stage','entry',32,68),('entry','grounds',60,55),('grounds','reception',27,53),
        ('reception','visitor-wc',32,47),('reception','reception-store',32,51),('reception','south',27,45),
        ('south','west',22,43.5),('south','east',42,43.5),('north','west',22,20.5),('north','east',42,20.5),
        ('south','quad',32,42),('north','quad',32,22),('west','reading',19,37),('west','library-issue',19,30),
        ('library-issue','stacks',13,28),('library-issue','reading',13,32),('west','office-entry',19,20.5),('office-entry','office',15,19),('office','staff-ground-wc',11,11),('office','office-records',11,16),('grounds','laundry',25,4),('grounds','house-store',31,4),('kitchen','staff-change',37,9),('grounds','staff-change',37,4),('pantry','bulk-food',42.5,9),
        ('north','dining',28,19),('dining','serving',34,17),('serving','kitchen',37,15),('serving','washup',40,17),
        ('kitchen','pantry',40,10.5),('pantry','receiving',45,10.5),('washup','refuse',45,17),
        ('receiving','grounds',50,12),('refuse','grounds',50,17),('east','lecture',45,38),
        ('east','lab-entry',45,30.5),('lab-entry','lab',49,29),('lab','lab-prep',53,22),('lab','lab-wash',53,27),
        ('west','main-stair',19,43.5),('east','east-stair',45,54),('east-stair','grounds',57,50),
        ('upper-west','upper-main-stair',19,51),('upper-west','upper-north',22,17.5),('upper-north','upper-east',43.5,19),
        ('upper-east','upper-east-stair',45,54),('upper-west','student-change',19,16.5),
        ('student-change','student-wc',10,14),('student-change','student-shower',16,14),('upper-west','linen',19,20.5),
        ('upper-west','staff-hall',19,31.5),('staff-hall','staff-a',10,30),('staff-hall','staff-b',16,30),
        ('staff-hall','staff-wash',10,33),('staff-hall','staff-work',16,33),('upper-west','staff-common',19,40),
        ('upper-east','restricted',45,24),('upper-east','study',45,38),
        ('cellar-stair','cellar-hall',16,42),('cellar-hall','archive',10,38),('cellar-hall','plant',16,38)]:door(a,b,x,z)
    for i in range(4):door('upper-north',f'dorm-{i+1}',23+i*6,16)
    for a,b,x,z,y0,y1,width in [('main-stair','upper-main-stair',16,44,0,3.6,1.5),('east-stair','upper-east-stair',54,47,0,3.6,1.5),('cellar-stair','main-stair',10,44,-3,0,1.3)]:
        rise=y1-y0;run=rise*1.6
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='stairs',width=width,points=[[x,y0,z],[x,y1,z+run]],rise_m=rise,run_m=run,headroom_m=2.2,landing_depth_m=1.5,risers=20 if rise>3 else 18,
            construction='Real deck voids, guards, floor/roof depth and open side bypasses remain required.',note='Measured section reservation; actual six-player and ghost traversal unproved.'))
    return dict(version=2,id='ACADEMY',title='St. Alban Academy — revised residential cloister rough',status='S02_DESIGN_STUDY_NOT_RUNTIME_ACCEPTED',site_bounds=[3,0,62,82],zones=zones,links=links,
        capacity='Sixteen resident pupils in four actual four-place rooms, four resident staff beds, twenty dining places and sixteen teaching places.',
        fit_assets=['bed_5','level_student_desk','chair_mp_5','toilet_mp_1','level_bath_basin'],prop_fit_reservations=fit,expected_single_beds=20,sleeping_spaces=sleeping,
        rituals=[dict(rite='Banishment',zone='study',point=[51,3.6,38]),dict(rite='Cleansing',zone='archive',point=[10,-3,30]),dict(rite='Absolution',zone='quad',point=[32,0,32])],
        staging=dict(zone='stage',point=[32,0,74],yaw=-math.pi/2,cargo_route=[[28.372,0,74],[27,0,60],[27,0,53]],note='Actual canonical van placement; cargo lane and ground support require runtime proof.'),
        terrain_cutouts=[dict(bounds=[7,22,19,53],world_surface_y=-.65,method='Persisted editor topology punched leaves under archive, plant and stair; not yet authored')],
        flow_reservations=[dict(id=key,path=path,avoid=avoid) for key,path,avoid in [
            ('visitor',['stage','entry','grounds','reception','visitor-wc'],sleeping+['lab','archive']),
            ('food-supply',['grounds','receiving','pantry','kitchen','serving','dining'],['washup','refuse']),
            ('dish-return',['dining','serving','washup','refuse'],['pantry','kitchen']),
            ('residential-return',['upper-north','upper-east','upper-east-stair','east-stair','grounds'],['restricted','study','lab','lecture'] )]],
        decisions=['Four unequal ranges surround an actual open quadrangle; cloister and outside envelopes are not filled by a blanket upper slab.',
            'Teaching and practical prep/cleanup remain in the east range; quiet library issue/reading/stacks remain west.',
            'North sleeping rooms have real boundaries, individual storage obligations and side access beside full-scale beds; student and staff sanitary support are distinct.',
            'Two independent upper return stairs serve the residential circulation without passing through occupied teaching or sleeping rooms.',
            'Food, washing-up, provisions and refuse are separately bounded behind dining; archive and heat/utility plant have separate enclosures.'],
        remaining=['Author actual individual storage, sanitary bays, task furniture, supported props and normal-use service infrastructure.',
            'Resolve actual confidentiality controls and an accessible vertical route without creating a power-loss softlock; covered office approach and private staff sanitary enclosure are reserved.',
            'Build full stair/roof/structure openings and load paths; upper restricted stacks and residential ranges require real support and daylight design.',
            'Punch saved editor ground beneath the complete cellar/stair footprint and prove render/body/optical/object holes with filled negatives.',
            'Shape quad/service terrain, place canonical van and prove six-player/hunt/ritual routes, native parity, networking, performance and final environmental storytelling.'])


def college():
    zones,links,fit,sleeping=[],[],[],[]
    def room(key,name,bounds,y,owner,kind='inside',height=3.6):
        zones.append(dict(id=key,name=name,bounds=bounds,y=y,height=height,owner=owner,kind=kind,purpose=name))
    for key,name,bounds,owner,kind in [
        ('teaching-hall','Teaching block distribution',[20,10,24,54],0,'circulation'),
        ('teaching-lobby','Public teaching reception',[20,54,28,62],0,'circulation'),
        ('lecture','Thirty-two-place clear-span lecture room',[6,34,20,54],5,'inside'),
        ('seminar','Sixteen-place discussion seminar',[6,24,20,34],2,'inside'),
        ('lab','Sixteen-place practical teaching benches',[6,10,16,22],3,'inside'),
        ('lab-prep','Secure practical supplies',[16,10,20,16],3,'inside'),
        ('lab-wash','Practical cleanup and handwashing',[16,16,20,22],3,'inside'),
        ('lab-entry','Practical coats and approach',[6,22,20,24],3,'circulation'),
        ('teaching-stair','Teaching and tunnel access stair bay',[24,34,30,46],0,'circulation'),
        ('cleaner','Public teaching cleaner supplies',[24,46,28,50],0,'inside'),
        ('public-wc','Public teaching WC bays',[24,50,28,54],0,'inside'),
        ('admin','Campus administration work room',[28,46,36,54],1,'inside'),
        ('meeting','Private staff and visitor meeting',[28,54,36,58],1,'inside'),
        ('records','Confidential administration records',[28,58,36,62],1,'inside'),
        ('library-stair','Library public stair bay',[24,4,30,16],6,'circulation'),
        ('issue','Library issue return and enquiry',[24,16,30,20],6,'inside'),
        ('library-hall','Library distribution',[30,10,34,20],6,'circulation'),
        ('reading','Quiet daylit library reading',[34,10,50,20],6,'inside'),
        ('open-stacks','Public reference shelves',[30,4,38,10],6,'inside'),
        ('library-office','Library staff work and returns',[38,4,44,10],6,'inside'),
        ('reference','Small-group reference consultation',[44,4,50,10],6,'inside'),
        ('res-hall','Residence and union distribution',[50,20,54,64],0,'circulation'),
        ('res-lift','Residence accessible lift reservation',[42,20,50,28],0,'circulation'),
        ('laundry','Residence laundry and drying support',[42,28,50,36],4,'inside'),
        ('union-common','Union informal common room',[42,36,50,44],4,'inside'),
        ('cafe','Twenty-place union cafe seating',[42,44,50,60],4,'inside'),
        ('service-base','Union staff service and utility',[54,16,62,24],4,'inside'),
        ('staff-cleaner','Union cleaner and work supplies',[54,24,58,28],4,'inside'),
        ('staff-wc','Union staff WC and washing',[58,24,62,28],4,'inside'),
        ('staff-rest','Union staff rest and changing',[54,28,62,32],4,'inside'),
        ('north-cross','North stair approach',[54,32,62,36],0,'circulation'),
        ('food-store','Cafe provisions',[54,36,58,44],4,'inside'),
        ('receiving','Cafe delivery receiving',[58,36,62,44],4,'inside'),
        ('kitchen','Cafe cooking and serving',[54,44,62,52],4,'inside'),
        ('washup','Cafe dish return and cleanup',[54,52,62,60],4,'inside'),
        ('south-cross','South stair approach',[54,60,62,64],0,'circulation'),
        ('north-stair','Independent north residence stair',[62,24,68,36],0,'circulation'),
        ('south-stair','Independent south residence stair',[62,52,68,64],0,'circulation'),
        ('res-cellar-stair','Residence tunnel exit stair',[42,60,50,72],0,'circulation'),
        ('rear-hall','Tunnel exit to rear service yard',[50,64,54,72],0,'circulation')]:room(key,name,bounds,0,owner,kind,7.2 if key=='lecture' else 3.6)
    for key,name,bounds,owner,kind in [
        ('upper-teaching','Upper teaching circulation',[20,10,24,54],10,'circulation'),
        ('upper-teaching-stair','Teaching stair guarded arrival',[24,34,30,46],10,'circulation'),
        ('tutorial','Small-group tutorial and staff study',[24,46,36,54],10,'inside'),
        ('project','Upper supervised project room',[6,10,20,24],10,'inside'),
        ('upper-seminar','Upper discussion and quiet study',[6,24,20,34],10,'inside'),
        ('upper-library-stair','Upper library guarded arrival',[24,4,30,16],8,'circulation'),
        ('upper-library-landing','Upper library stair approach',[24,16,30,20],8,'circulation'),
        ('upper-stacks','Upper library book stacks',[30,4,42,16],8,'inside'),
        ('upper-reading','Upper quiet reading',[42,4,50,16],8,'inside'),
        ('bridge-lobby','Public library bridge approach',[30,16,50,20],7,'circulation'),
        ('bridge','Supported upper campus bridge',[50,16,54,20],7,'circulation'),
        ('bridge-return','Upper bridge return to residence',[50,20,54,28],7,'circulation'),
        ('upper-res-hall','Private residence corridor',[50,28,54,64],9,'circulation'),
        ('upper-res-lift','Residence lift upper landing',[42,20,50,28],9,'circulation'),
        ('bath-entry','Residence private washing approach',[54,20,62,24],9,'circulation'),
        ('res-wc','Residence screened WC bays',[54,16,58,20],9,'inside'),
        ('res-shower','Residence private shower bays',[58,16,62,20],9,'inside'),
        ('upper-north-cross','North residence stair approach',[54,32,62,36],9,'circulation'),
        ('upper-south-cross','South residence stair approach',[54,60,62,64],9,'circulation'),
        ('upper-north-stair','North residence guarded arrival',[62,24,68,36],9,'circulation'),
        ('upper-south-stair','South residence guarded arrival',[62,52,68,64],9,'circulation')]:room(key,name,bounds,3.6,owner,kind)
    for side,x,starts in [('west',42,[28,36,44,52]),('east',54,[24,36,44,52])]:
        for i,z in enumerate(starts):
            key=f'res-{side}-{i+1}';room(key,f'Private twin residence {side} {i+1}',[x,z,x+8,z+8],3.6,9);sleeping.append(key)
            for offset in [2.5,5.5]:fit.append(dict(space=key,asset='bed_5',origin=[x+4,3.6,z+offset]))
    for key,name,bounds,kind in [
        ('west-tunnel','West building service distribution',[20,10,24,54],'circulation'),
        ('plant','Teaching heating water and electrical plant',[12,10,20,24],'inside'),
        ('maintenance','Campus maintenance and parts',[12,24,20,34],'inside'),
        ('west-cellar-stair','Teaching tunnel stair',[24,34,30,46],'circulation'),
        ('cross-tunnel','Supported tunnel below campus green',[30,38,46,42],'circulation'),
        ('east-tunnel','Residence tunnel return',[42,42,46,60],'circulation'),
        ('east-cellar-stair','Residence tunnel exit stair',[42,60,50,72],'circulation')]:room(key,name,bounds,-3,11,kind,3)
    room('green','Shared campus green envelope',[3,0,72,72],0,12,'outside',12)
    room('entry','Public campus approach',[3,72,72,84],0,13,'outside',12)
    room('stage','Canonical campus roadhead van apron',[24,84,40,97],0,14,'staging',3)
    byid={z['id']:z for z in zones}
    def door(a,b,x,z,width=1.4):
        y=byid[a]['y'];assert byid[b]['y']==y
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='level',width=width,points=[review.center(byid[a]),[x,y,z],review.center(byid[b])],threshold=[x,y,z],access='residential' if 'res-' in a+b else 'campus',note='Exact proposed threshold; diagram lines do not establish capsule or privacy acceptance.'))
    for a,b,x,z in [
        ('stage','entry',32,84),('entry','green',32,72),('green','teaching-lobby',24,62),('teaching-lobby','teaching-hall',22,54),
        ('teaching-hall','lecture',20,44),('teaching-hall','seminar',20,28),('teaching-hall','lab-entry',20,23),('lab-entry','lab',12,22),
        ('lab','lab-prep',16,13),('lab','lab-wash',16,19),('teaching-hall','teaching-stair',24,44),
        ('teaching-hall','cleaner',24,48),('teaching-hall','public-wc',24,52),('teaching-lobby','meeting',28,56),('meeting','admin',32,54),('meeting','records',32,58),
        ('green','issue',27,20),('issue','library-hall',30,18),('issue','library-stair',27,16),('library-hall','reading',34,15),
        ('library-hall','open-stacks',32,10),('reading','library-office',41,10),('reading','reference',47,10),
        ('green','res-hall',52,64),('res-hall','res-lift',50,24),('res-hall','laundry',50,32),('res-hall','union-common',50,40),('res-hall','cafe',50,48),
        ('res-hall','service-base',54,22),('res-hall','staff-rest',54,30),('staff-rest','staff-cleaner',56,28),('staff-rest','staff-wc',60,28),
        ('res-hall','north-cross',54,34),('north-cross','north-stair',62,34.5),('res-hall','south-cross',54,62),('south-cross','south-stair',62,62),
        ('green','receiving',62,40),('receiving','food-store',58,40),('food-store','kitchen',56,44),('kitchen','res-hall',54,48),('washup','res-hall',54,56),
        ('res-hall','rear-hall',52,64),('rear-hall','res-cellar-stair',50,69),('rear-hall','green',52,72),('north-stair','green',68,30),('south-stair','green',68,58),
        ('upper-teaching','upper-teaching-stair',24,44),('upper-teaching','tutorial',24,50),('upper-teaching','project',20,16),('upper-teaching','upper-seminar',20,28),
        ('upper-teaching','upper-library-stair',24,14),('upper-library-stair','upper-library-landing',27,16),
        ('upper-library-landing','bridge-lobby',30,18),('bridge-lobby','upper-stacks',36,16),('bridge-lobby','upper-reading',46,16),('bridge-lobby','bridge',50,18),
        ('bridge','bridge-return',52,20),('bridge-return','upper-res-hall',52,28),('bridge-return','upper-res-lift',50,24),('bridge-return','bath-entry',54,22),
        ('bath-entry','res-wc',56,20),('bath-entry','res-shower',60,20),('upper-res-hall','upper-north-cross',54,34),('upper-north-cross','upper-north-stair',62,34.5),
        ('upper-res-hall','upper-south-cross',54,62),('upper-south-cross','upper-south-stair',62,62),
        ('west-tunnel','plant',20,16),('west-tunnel','maintenance',20,28),('west-tunnel','west-cellar-stair',24,40),
        ('west-cellar-stair','cross-tunnel',30,40),('cross-tunnel','east-tunnel',44,42),('east-tunnel','east-cellar-stair',44,60)]:door(a,b,x,z)
    for side,x,starts in [('west',50,[28,36,44,52]),('east',54,[24,36,44,52])]:
        for i,z in enumerate(starts):door('bridge-return' if side=='east' and i==0 else 'upper-res-hall',f'res-{side}-{i+1}',x,z+3)
    for a,b,x,z,y0,y1,width in [
        ('teaching-stair','upper-teaching-stair',27,36,0,3.6,1.5),('library-stair','upper-library-stair',27,6,0,3.6,1.5),
        ('north-stair','upper-north-stair',65,26,0,3.6,1.5),('south-stair','upper-south-stair',65,54,0,3.6,1.5),
        ('west-cellar-stair','teaching-stair',27,36,-3,0,1.4),('east-cellar-stair','res-cellar-stair',46,62,-3,0,1.4)]:
        rise=y1-y0;run=rise*1.6
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='stairs',width=width,points=[[x,y0,z],[x,y1,z+run]],rise_m=rise,run_m=run,headroom_m=2.2,landing_depth_m=1.5,risers=20 if rise>3 else 18,
            construction='Full-run voids and structural undersides; clear side bypass and both end landings remain to be physically built.',note='Separate real entrance/return flights; no floor-hopping graph substitute.'))
    # Keep six metres of real ground between the library and residence.
    # Apply the same authored offset to rooms, thresholds, flights and props;
    # only the connecting bridge/tunnel lengths change.
    shifted=set(['res-hall','res-lift','laundry','union-common','cafe','service-base','staff-cleaner','staff-wc','staff-rest','north-cross','food-store','receiving','kitchen','washup','south-cross','north-stair','south-stair','res-cellar-stair','rear-hall','upper-res-hall','upper-res-lift','bath-entry','res-wc','res-shower','upper-north-cross','upper-south-cross','upper-north-stair','upper-south-stair','east-cellar-stair','entry','stage']+sleeping)
    for key in shifted:
        box=byid[key]['bounds'];box[1]+=6;box[3]+=6
    byid['bridge-return']['bounds'][3]+=6
    byid['east-tunnel']['bounds'][3]+=6
    byid['green']['bounds'][3]+=6
    for item in fit:item['origin'][2]+=6
    for link in links:
        if link['kind']=='stairs':
            if link['a'] in shifted:
                assert link['b'] in shifted
                for point in link['points']:point[2]+=6
        else:
            if link['a'] in shifted or link['b'] in shifted:link['threshold'][2]+=6
            link['points']=[review.center(byid[link['a']]),link['threshold'].copy(),review.center(byid[link['b']])]
    # The library is also four metres clear of the teaching block: separate
    # buildings need a real gap, not a party wall with three different names.
    library_keys={'library-stair','issue','library-hall','reading','open-stacks','library-office','reference','upper-library-stair','upper-library-landing','upper-stacks','upper-reading','bridge-lobby'}
    for key in library_keys:
        box=byid[key]['bounds'];box[0]+=4;box[2]+=4
    links[:]=[l for l in links if {l['a'],l['b']}!={'upper-teaching','upper-library-stair'}]
    byid['bridge']['bounds']=[50,20,54,24]
    byid['bridge-return']['bounds']=[50,24,54,26]
    room('teaching-library-link','Four-metre teaching to library upper link',[24,12,28,16],3.6,7,'circulation')
    room('res-bridge-approach','Residence bridge landing outside bedrooms',[50,26,54,34],3.6,7,'circulation')
    byid={z['id']:z for z in zones}
    for link in links:
        if link['kind']=='stairs':
            if link['a'] in library_keys:
                assert link['b'] in library_keys
                for point in link['points']:point[0]+=4
        else:
            if link['a'] in library_keys or link['b'] in library_keys:link['threshold'][0]+=4
            if link['a']=='bridge-return' and link['b']!='bridge':link['a']='res-bridge-approach'
            if link['b']=='bridge-return' and link['a']!='bridge':link['b']='res-bridge-approach'
            if {link['a'],link['b']}=={'bridge-lobby','bridge'}:link['threshold']=[52,3.6,20]
            if {link['a'],link['b']}=={'bridge','bridge-return'}:link['threshold']=[52,3.6,24]
            link['points']=[review.center(byid[link['a']]),link['threshold'].copy(),review.center(byid[link['b']])]
    door('upper-teaching','teaching-library-link',24,14)
    door('teaching-library-link','upper-library-stair',28,14)
    door('bridge-return','res-bridge-approach',52,26)
    ground={z['id'] for z in zones if z['y']==0 and z['kind'] not in ('outside','staging')}
    buildings=[dict(id='teaching',spaces=sorted(ground-library_keys-shifted)),dict(id='library',spaces=sorted(ground&library_keys)),dict(id='residence-union',spaces=sorted(ground&shifted))]
    for i,link in enumerate(links,1):link['id']=f'L{i:02}'
    return dict(version=2,id='COLLEGE',title='Beckenburg College — revised three-building campus rough',status='S02_DESIGN_STUDY_NOT_RUNTIME_ACCEPTED',site_bounds=[3,0,72,104],zones=zones,links=links,
        building_groups=buildings,
        capacity='Eight private twin residence rooms: sixteen places. Thirty-two lecture, sixteen seminar/practical and twenty cafe places are separate bounded activity capacities.',
        fit_assets=['bed_5','level_student_desk','chair_mp_5','toilet_mp_1','level_bath_basin'],prop_fit_reservations=fit,expected_single_beds=16,sleeping_spaces=sleeping,
        rituals=[dict(rite='Banishment',zone='lecture',point=[13,0,44]),dict(rite='Cleansing',zone='maintenance',point=[16,-3,29]),dict(rite='Absolution',zone='green',point=[35,0,30])],
        staging=dict(zone='stage',point=[32,0,96],yaw=-math.pi/2,cargo_route=[[28.372,0,96],[24,0,78],[24,0,62]],note='Canonical van and actual future surface support required; arrival diagrams are not traversability proof.'),
        terrain_cutouts=[dict(bounds=b,world_surface_y=-.65,method='Persisted editor terrain punching under exact tunnel/plant/stair union; not yet authored. Real ceilings and supported ground cover must close buried segments without restoring interior collision.') for b in [[20,10,24,54],[12,10,20,34],[24,34,30,46],[30,38,46,42],[42,42,46,66],[42,66,50,78]]],
        bridge_reservation=dict(spaces=['teaching-library-link','bridge','bridge-return','res-bridge-approach'],standing_y=3.6,width_m=4,design='Supported upper links cross four metres between teaching and library and the stepped two-to-six-metre library/residence separation; columns/abutments and clear ground underpass need real structure and collision proof.'),
        lift_reservation=dict(spaces=['res-lift','upper-res-lift'],rise_m=3.6,operation='Operational accessible lift remains to be authored; two ordinary residential stair return paths remain independently reserved.'),
        flow_reservations=[dict(id=key,path=path,avoid=avoid) for key,path,avoid in [
            ('public-wc',['green','teaching-lobby','teaching-hall','public-wc'],sleeping+['lab','admin']),
            ('food-delivery',['green','receiving','food-store','kitchen','res-hall','cafe'],['washup','staff-wc']),
            ('dirty-dishes',['cafe','res-hall','washup'],['kitchen','food-store']),
            ('north-residential-return',['upper-res-hall','upper-north-cross','upper-north-stair','north-stair','green'],sleeping+['kitchen']),
            ('south-residential-return',['upper-res-hall','upper-south-cross','upper-south-stair','south-stair','green'],sleeping+['kitchen']),
            ('tunnel-return',['west-tunnel','west-cellar-stair','cross-tunnel','east-tunnel','east-cellar-stair','res-cellar-stair','rear-hall','green'],['plant','maintenance'])]],
        decisions=['Three genuinely separate teaching/library/residence footprints face a shared green; tall lecture has no upper floor stamped through it.',
            'Public classrooms, practical prep/cleanup, administration and library issue/reading/stacks have separate normal-use rooms.',
            'Eight twin residence rooms have actual bounds, independent hall doors, shared private sanitation, laundry and two upper return stairs.',
            'Library bridge joins circulation outside sleeping rooms; underground service return avoids walking through operating plant or stores.',
            'Cafe cooking, receiving, provisions and dirty return are bounded; shared public hall is an explicit serving/return interface still needing physical fit and privacy review.'],
        remaining=['Place full-scale teaching/cafe/library/bedroom fixtures and individual storage, support and services without shrinking source assets.',
            'Build real bridge load paths, abutments, glass/guards, roof and upper privacy controls; retain unobstructed ground routes.',
            'Author actual lift, sanitary bays, power/water/drainage, tunnel roof/ground cover and separate service machinery access.',
            'Punch saved editor ground beneath the entire buried union, build real stairs/deck holes and test filled-ground render/body/optical/object negative controls.',
            'Complete terrain around three buildings and canonical van; prove six-player/hunt/ritual routes, native/runtime/network parity, performance and final weathered storytelling detail.'])


def church():
    zones,links,fit=[],[],[]
    def room(key,name,bounds,y,owner,kind='inside',height=3):
        zones.append(dict(id=key,name=name,bounds=bounds,y=y,height=height,owner=owner,kind=kind,purpose=name))
    for key,name,bounds,owner,kind,height in [
        ('nave','Forty-place nave with centre and side aisles',[18,12,26,26],0,'inside',6),
        ('rear-nave','Under-choir circulation and stair approach',[18,26,26,34],0,'circulation',3),
        ('vestibule','Weather vestibule and arrival',[18,34,26,39],1,'circulation',3),
        ('parish-lobby','Parish work approach',[13,30,18,34],2,'circulation',3),
        ('office','Private parish work and meetings',[10,26,15,30],2,'inside',3),
        ('records','Confidential parish records',[15,26,18,30],2,'inside',3),
        ('parish-wc','Discreet parish sanitary support',[10,30,13,34],2,'inside',3),
        ('conversation-entry','Quiet conversation waiting and access',[14,18,18,26],3,'circulation',3),
        ('confessional','Private confessional conversation enclosure',[10,18,14,22],3,'inside',3),
        ('consultation','Private parish consultation',[10,22,14,26],3,'inside',3),
        ('chancel','Chancel and supported altar arrangement',[18,6,26,12],4,'inside',5),
        ('chapel','Side chapel and quiet remembrance',[26,14,34,26],5,'inside',4),
        ('vestry','Vestment preparation and service work',[26,6,31,14],6,'inside',3),
        ('vestry-wash','Vestry washing and supported sink',[31,6,34,10],6,'inside',3),
        ('vestments','Dry vestment storage',[31,10,34,14],6,'inside',3),
        ('crypt-entry','Separate crypt stair and clear side bypass',[26,26,34,34],11,'circulation',3)]:room(key,name,bounds,0,owner,kind,height)
    for key,name,bounds,owner,kind in [
        ('choir-stair','Choir landing outside flight guards',[18,26,21,34],7,'circulation'),
        ('choir','Choir seating and music stands',[21,26,26,34],7,'inside'),
        ('tower-stair','Tower stair and protected side bypass',[10,26,18,34],8,'circulation'),
        ('music-entry','Music and organ service approach',[26,26,29,34],9,'circulation'),
        ('music-archive','Music scores and parish performance archive',[29,26,34,30],9,'inside'),
        ('organ-service','Organ maintenance and working clearance',[29,30,34,34],9,'inside')]:room(key,name,bounds,3,owner,kind)
    room('belfry','Bell frame with guarded inspection access',[10,26,18,34],6,10)
    for key,name,bounds,kind in [
        ('crypt-stair','Crypt stair lower approach',[26,26,34,34],'circulation'),
        ('crypt-aisle','Crypt distribution and memorial approach',[22,12,26,34],'circulation'),
        ('crypt-memorial','Crypt memorial chamber and clear rite bay',[10,26,22,34],'inside'),
        ('vault-west-a','Sealed western burial vault A',[10,12,16,26],'inside'),
        ('vault-west-b','Sealed western burial vault B',[16,12,22,26],'inside'),
        ('vault-east','Sealed eastern burial vault',[26,12,34,26],'inside'),
        ('vault-north','Sealed under-chancel burial vault',[10,6,34,12],'inside')]:room(key,name,bounds,-3,11,kind)
    room('grounds','Churchyard and cemetery envelope',[4,0,40,39],0,12,'outside',12)
    room('entry','Churchyard gate and entrance approach',[1,39,44,58.4],0,13,'outside',12)
    room('stage','Canonical parish van apron',[14,45,30,58],0,14,'staging')
    byid={z['id']:z for z in zones}
    def door(a,b,x,z,width=1.2,opening='doorway'):
        y=byid[a]['y'];assert byid[b]['y']==y
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='level',width=width,points=[review.center(byid[a]),[x,y,z],review.center(byid[b])],threshold=[x,y,z],opening=opening,access='parish',note='Exact proposed threshold; roof, wall, leaf and circulation must share this authority.'))
    for a,b,x,z in [
        ('stage','entry',22,45),('entry','vestibule',22,39),('vestibule','rear-nave',22,34),
        ('rear-nave','parish-lobby',18,32),('parish-lobby','office',14,30),('parish-lobby','records',16.5,30),('parish-lobby','parish-wc',13,32),
        ('nave','conversation-entry',18,22),('conversation-entry','confessional',14,20),('conversation-entry','consultation',14,24),
        ('nave','chapel',26,20),('chancel','vestry',26,9),('vestry','chapel',28.5,14),('vestry','vestry-wash',31,8),('vestry','vestments',31,12),
        ('chapel','crypt-entry',28.5,26),('chapel','grounds',34,20),('vestry','grounds',28.5,6),
        ('choir-stair','choir',21,33),('choir-stair','tower-stair',18,33.25),('choir','music-entry',26,32.7),
        ('music-entry','music-archive',29,28),('music-entry','organ-service',29,32),
        ('crypt-stair','crypt-aisle',26,30),('crypt-aisle','crypt-memorial',22,30),
        ('crypt-memorial','vault-west-a',13,26),('crypt-aisle','vault-west-b',22,20),('crypt-aisle','vault-east',26,20),('crypt-aisle','vault-north',24,12),
        ('grounds','entry',6,39)]:door(a,b,x,z)
    door('nave','rear-nave',22,26,7.6,'open structural archway')
    door('nave','chancel',22,12,2,'open chancel archway')
    for a,b,x,z,y0,y1,width in [('rear-nave','choir-stair',19.5,27.6,0,3,1.3),('tower-stair','belfry',16.5,27.6,3,6,1.3),('crypt-stair','crypt-entry',31.5,27.6,-3,0,1.3)]:
        run=(y1-y0)*1.6
        links.append(dict(id=f'L{len(links)+1:02}',a=a,b=b,kind='stairs',width=width,points=[[x,y0,z],[x,y1,z+run]],rise_m=y1-y0,run_m=run,headroom_m=2.2,landing_depth_m=1.5,risers=18,
            construction='Real full-run voids, floor/roof depth, guards and unobstructed side bypass/end landings required.',note='Tower threshold is beyond the choir flight side guard, not inside its run. Crypt flight is outside nave seating.'))
    for row in range(10):
        for x in [19.3,20.3,23.7,24.7]:fit.append(dict(space='nave',asset='chair_mp_5',origin=[x,0,13.5+row*1.2],yaw=math.pi))
    fit.extend([dict(space='parish-wc',asset='toilet_mp_1',origin=[11.5,.235,31.2]),dict(space='parish-wc',asset='level_bath_basin',origin=[10.6,0,33.1]),dict(space='vestry-wash',asset='level_bath_basin',origin=[33.4,0,8])])
    return dict(version=2,id='CHURCH',title='St. Brigid Parish — revised worship and crypt rough',status='S02_DESIGN_STUDY_NOT_RUNTIME_ACCEPTED',site_bounds=[1,0,44,59],zones=zones,links=links,
        capacity='Forty seated nave places, a small choir and bounded parish work; normal-use seats do not create forty search rooms.',
        seating_aisles=[dict(name='centre',bounds=[21,12,23,26]),dict(name='west side',bounds=[18.08,12,18.95,26]),dict(name='east side',bounds=[25.05,12,25.92,26])],
        fit_assets=['chair_mp_5','toilet_mp_1','level_bath_basin'],prop_fit_reservations=fit,expected_single_beds=0,sleeping_spaces=[],
        rituals=[dict(rite='Banishment',zone='chapel',point=[30,0,20]),dict(rite='Cleansing',zone='crypt-memorial',point=[16,-3,30]),dict(rite='Absolution',zone='grounds',point=[7,0,7])],
        staging=dict(zone='stage',point=[22,0,51],yaw=-math.pi/2,cargo_route=[[18.372,0,51],[22,0,42],[22,0,39]],note='Use actual canonical van, ground support and parish gate clearance; no substitute vehicle geometry.'),
        terrain_cutouts=[dict(bounds=[10,6,34,34],world_surface_y=-.65,method='Persisted editor AdaptiveTerrainTopology punching below the entire crypt including sealed vaults and stair. Architectural ceilings/foundation cover and floor voids remain separate construction requirements.')],
        structural_interfaces=[dict(id='choir-front',standing_y=3,bounds=[18,26,26,34],requirement='Only rear choir has a deck. Nave/chancel remain tall, open volumes; build supported beam/pier load paths, guard and full flight opening.'),
            dict(id='bell-frame',standing_y=6,bounds=[10,26,18,34],requirement='Tower masonry piers pass to ground foundations; bell load attaches to real braced frame, not floating roof props.')],
        flow_reservations=[dict(id=key,path=path,avoid=avoid) for key,path,avoid in [
            ('public-arrival',['stage','entry','vestibule','rear-nave','nave','chancel'],['office','records','crypt-entry']),
            ('chapel-return',['nave','chapel','grounds','entry'],['vestry-wash','vestments']),
            ('crypt-access',['chapel','crypt-entry','crypt-stair','crypt-aisle','crypt-memorial'],['nave','vault-west-a','vault-west-b','vault-east','vault-north'])]],
        decisions=['Retain the distinctive parish footprint, nave/chancel, side chapel, choir at+3, belfry at+6 and crypt at-3, while replacing unsupported use of leftover room collections.',
            'Move crypt flight to an east ground bay beneath music service; keep forty actual-sized nave seat reservations and clear centre/side aisles.',
            'Parish meetings, confidential records and discreet sanitary room are bounded; vestry preparation, washing and dry vestments are separate.',
            'Keep choir/tower thresholds beyond guard ends. Full physical upper-wall holes, supports and optical/player passage remain required.',
            'Keep the memorial crypt playable while four larger burial vaults remain permanently sealed context, without tasks or ghost anchors inside.'],
        remaining=['Author/inspect actual altar, choir, organ-maintenance, bell frame, masonry/roof beams, seating, support and source materials.',
            'Construct permanent vault barriers, actual runtime sockets and shared native ownership inside the accessible envelope.',
            'Punch saved world terrain below the complete crypt, with actual roof/ground cover and architectural stair openings; prove body/optical/object/render holes using filled-ground negatives.',
            'Verify grounded parish arrival, stairs, closed/open doors, ghost/hunter routes, rituals, electrical operation, native/runtime/network parity and required performance; no round-time testing.'])


SEARCH_LIMIT=json.loads((OUT/'SEARCH_ENVELOPE_POLICY.json').read_text(encoding='utf-8'))['maximum_accessible_spaces']


def apply_search_envelope(data):
    """Owner's 3 November scope: coherent building, bounded accessible search.
    Count every traversable authored space, including circulation and exterior
    sectors. Context stays modeled but all incident player links are sealed.
    """
    all_ids={z['id'] for z in data['zones']}
    selected={
        'PRISON': 'stage entry yard reception intake records sally guard guard-link a-front-link a-lower-gallery a-stair a-upper-stair a-upper-gallery b-front-link b-lower-gallery b-stair b-upper-stair b-upper-gallery a-lower-cell-01 a-upper-cell-01 b-lower-cell-01 b-upper-cell-01 cross a-rear-link b-rear-link dining medical-link medical-store sluice isolation-link isolation-wash chapel plant-stair lower-stair plant isolation-cell-1',
        'ASYLUM': 'stage entry grounds court spine main-stair upper-stair upper-spine ground-ward-link ground-north-a ground-south-a upper-ward-link upper-north-a upper-south-a admission interview records day clinical-link treatment hydro-change hydro-bath hydro-dry dining service-stair service-link staff-office quiet-lobby quiet chapel cellar-stair cellar-service cellar-cross plant clinical-service service clinical-soiled ground-wc upper-wc ground-observation',
        'ACADEMY': 'stage entry grounds quad west east north south reception lecture lab-entry lab reading library-issue office-entry office dining serving kitchen main-stair east-stair upper-west upper-north upper-east upper-main-stair upper-east-stair dorm-1 dorm-3 study archive cellar-hall cellar-stair plant student-change student-wc student-shower visitor-wc lab-prep',
        'COLLEGE': 'stage entry green teaching-lobby teaching-hall lecture seminar lab-entry lab meeting admin teaching-stair upper-teaching-stair upper-teaching library-stair issue upper-library-stair upper-library-landing bridge-lobby bridge bridge-return res-bridge-approach teaching-library-link upper-res-hall res-west-1 res-east-1 upper-north-cross upper-north-stair north-stair north-cross res-hall cafe west-cellar-stair west-tunnel maintenance cross-tunnel east-tunnel east-cellar-stair res-cellar-stair rear-hall',
    }
    if data['id']=='CHURCH':
        opened=all_ids-set('vault-west-a vault-west-b vault-east vault-north'.split())
    elif data['id']=='MANSION':
        closed=set('coats visitor-wc pantry cold-store refuse suite-bath guest-a-bath guest-b guest-b-entry guest-b-bath staff-bath'.split())
        opened=all_ids-closed
    elif data['id'] in selected:opened=set(selected[data['id']].split())
    else:opened=all_ids
    assert opened<=all_ids,(data['id'],'Unknown accessible room',opened-all_ids)
    for zone in data['zones']:
        zone['player_access']='OPEN_SEARCH' if zone['id'] in opened else 'SEALED_CONTEXT'
        zone['gameplay_eligible']=zone['id'] in opened
    barriers=[]
    for link in data['links']:
        traversable=link['a'] in opened and link['b'] in opened
        link['player_access']='OPEN_ROUTE' if traversable else 'SEALED_NO_ROUTE'
        if not traversable:
            boundary=(link['a'] in opened)!=(link['b'] in opened)
            link['closure']='structural_deck_and_partition' if link['kind']=='stairs' else 'fixed_closed_leaf_or_solid_infill'
            if boundary:barriers.append(dict(link=link['id'],a=link['a'],b=link['b'],kind=link['closure'],
                requirement='Permanent body/object/ghost/interaction boundary; rendering and optical response must match solid leaf/infill or intended glazing. No player key, power state, event or damaged leaf may reopen it. Author and verify native/runtime collision; annotation alone is not a seal.'))
    for flow in data.get('flow_reservations',[]):flow['scope']='HISTORICAL_NORMAL_USE_NOT_PLAYER_SEARCH'
    data['search_envelope']=dict(status='DESIGN_ONLY_SEALS_NOT_RUNTIME_IMPLEMENTED',maximum=SEARCH_LIMIT,
        accessible_spaces=len(opened),sealed_context_spaces=len(all_ids-opened),
        count_rule='Every OPEN_SEARCH space counts, including corridors, stairs, service rooms, exterior sectors and staging. Stable fifteen gameplay owners are not the room budget.',
        barriers=barriers,
        gameplay_rule='Evidence, ghost spawn/anchor, hiding, switches/tasks, ritual sites, loose objective props and required travel belong only to the connected open envelope. Context windows may show rooms, but no interaction or required search crosses the seal.',
        context_rule='Retain coherent historical rooms/assets behind permanent boundaries. Restrict whole wings to their hallways where useful; do not count shuttable ordinary doors as permanent seals.',
        next_gate='Build actual permanent seals and audit runtime player/ghost/object/optical boundaries and all objective placements; construct the maps inside the forty-space cap. Round-duration testing is outside this rebuild task.')
    return data


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
    assert len({l['id'] for l in data['links']})==len(data['links']),'Duplicate link identity'
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
        if link['kind']!='stairs' and all(r['kind'] not in ('outside','staging') for r in [a,b]):
            x,y,z=link['threshold'];aa=a['bounds'];bb=b['bounds'];half=link['width']/2+.08
            on_x=any(abs(x-u)<1e-7 for u in [aa[0],aa[2]]) and any(abs(x-u)<1e-7 for u in [bb[0],bb[2]])
            on_z=any(abs(z-u)<1e-7 for u in [aa[1],aa[3]]) and any(abs(z-u)<1e-7 for u in [bb[1],bb[3]])
            assert (on_x and max(aa[1],bb[1])+half<=z<=min(aa[3],bb[3])-half) or (on_z and max(aa[0],bb[0])+half<=x<=min(aa[2],bb[2])-half),(link['id'],a['id'],b['id'],'door width crosses corner or not on shared boundary')
        graph[a['id']].add(b['id']);graph[b['id']].add(a['id'])
    reached=set();pending=['stage']
    while pending:
        key=pending.pop()
        if key in reached:continue
        reached.add(key);pending.extend(graph[key]-reached)
    assert reached==set(zones), set(zones)-reached
    if data['id']=='PRISON':
        cells=data['cell_fit_reservations'];assert len(cells)==32
        for wing in ['a','b']:
            for floor in [0,3.4]:assert sum(c['wing']==wing and c['standing_y']==floor for c in cells)==8
        measured={a['asset']:a for a in asset_bounds(data['fit_assets'])['assets']}
        for cell in cells:
            room=zones[cell['space']];x0,z0,x1,z1=room['bounds'];bx,bz,bxx,bzz=cell['bed_reservation']
            assert x0+.08<=bx<bxx<=x1-.08 and z0+.08<=bz<bzz<=z1-.08
            for asset,field in [('toilet_mp_1','wc_origin'),('level_bath_basin','basin_origin')]:
                x,y,z=cell[field];lo,hi=measured[asset]['local_min'],measured[asset]['local_max']
                assert x+lo[0]>=x0+.08-1e-6 and x+hi[0]<=x1-.08+1e-6
                assert z+lo[2]>=z0+.08-1e-6 and z+hi[2]<=z1-.08+1e-6
                assert y+lo[1]>=room['y']-1e-6 and y+hi[1]<room['y']+room['height']
                assert x+hi[0]<=bx or x+lo[0]>=bxx or z+hi[2]<=bz or z+lo[2]>=bzz, 'Cell bed/fixture overlap '+cell['space']
        assert not any({l['a'],l['b']}=={'visitors','visitor-custody'} for l in data['links']), 'Visitor booth divider is not a custody doorway'
    if data.get('prop_fit_reservations'):
        measured={a['asset']:a for a in asset_bounds(data['fit_assets'])['assets']}
        placed=[]
        for item in data['prop_fit_reservations']:
            room=zones[item['space']];a=measured[item['asset']];origin=item['origin']
            yaw=item.get('yaw',0);c,s=math.cos(yaw),math.sin(yaw)
            corners=[(origin[0]+x*c+z*s,origin[2]-x*s+z*c) for x in [a['local_min'][0],a['local_max'][0]] for z in [a['local_min'][2],a['local_max'][2]]]
            lo=[min(p[0] for p in corners),origin[1]+a['local_min'][1],min(p[1] for p in corners)]
            hi=[max(p[0] for p in corners),origin[1]+a['local_max'][1],max(p[1] for p in corners)]
            x0,z0,x1,z1=room['bounds']
            assert x0+.08<=lo[0]<hi[0]<=x1-.08 and z0+.08<=lo[2]<hi[2]<=z1-.08,item
            assert room['y']-1e-6<=lo[1]<hi[1]<=room['y']+room['height'],item
            for previous,pl,ph in placed:
                if previous['space']!=item['space']:continue
                assert any(hi[k]<=pl[k] or lo[k]>=ph[k] for k in range(3)),(previous,item,'native asset overlap')
            placed.append((item,lo,hi))
        if data['id']=='CHURCH':
            seats=[(item,lo,hi) for item,lo,hi in placed if item['space']=='nave' and item['asset']=='chair_mp_5']
            assert len(seats)==40,'Forty actual-size seat reservations'
            for aisle in data['seating_aisles']:
                x0,z0,x1,z1=aisle['bounds']
                for item,lo,hi in seats:
                    assert hi[0]<=x0 or lo[0]>=x1 or hi[2]<=z0 or lo[2]>=z1,('Seat blocks reserved nave aisle',aisle['name'],item)
        assert sum(i['asset']=='bed_5' for i in data['prop_fit_reservations'])==data.get('expected_single_beds',4 if data['id']=='MANSION' else 2)
        # Bedrooms must have a circulation doorway; a private bathroom cannot
        # become the only route to a guest sleeping space.
        for key in data.get('sleeping_spaces',(['suite-bed','guest-a','guest-b','staff-a','staff-b'] if data['id']=='MANSION' else ['parents','children'])):
            adjacent=[zones[b] for b in graph[key]]
            assert any(z['kind']=='circulation' for z in adjacent),key
    if data['id']=='COLLEGE':
        groups=data['building_groups'];assert len(groups)==3
        assigned=[key for g in groups for key in g['spaces']]
        assert len(assigned)==len(set(assigned))
        for i,a in enumerate(groups):
            for b in groups[i+1:]:
                for ak in a['spaces']:
                    for bk in b['spaces']:
                        aa=zones[ak]['bounds'];bb=zones[bk]['bounds']
                        assert math.hypot(max(aa[0]-bb[2],bb[0]-aa[2],0),max(aa[1]-bb[3],bb[1]-aa[3],0))>=2-1e-7,('Campus building footprints must be genuinely separate',ak,bk)
    for flow in data.get('flow_reservations',[]):
        assert not set(flow['path']) & set(flow['avoid']),flow
        for a,b in zip(flow['path'],flow['path'][1:]):assert b in graph[a],(flow['id'],a,b,'missing doorway or stair')
    if data['id']=='ASYLUM':
        for floor in [0,3.4]:
            beds=[p for p in data['prop_fit_reservations'] if p['asset']=='bed_5' and zones[p['space']]['y']==floor]
            assert len(beds)==8 and len({p['space'] for p in beds})==4,'Four two-place rooms per floor'
        assert not any({l['a'],l['b']}=={'dirty-laundry','clean-laundry'} for l in data['links']),'Clean transfer is not a through-door'
    for rite in data['rituals']:
        room=zones[rite['zone']];x,y,z=rite['point'];x0,z0,x1,z1=room['bounds']
        assert x0+2.2<=x<=x1-2.2 and z0+2.2<=z<=z1-2.2, 'Ritual clear area '+rite['zone']
        if room['kind']=='outside':
            for other in zones.values():
                if other['kind'] in ('outside','staging') or other['y']!=y:continue
                bx,bz,bxx,bzz=other['bounds']
                assert min(x+2.2,bxx)<=max(x-2.2,bx) or min(z+2.2,bzz)<=max(z-2.2,bz), 'Ritual intersects '+other['id']
    if 'search_envelope' in data:
        opened={z['id'] for z in zones.values() if z['player_access']=='OPEN_SEARCH'}
        assert len(opened)<=SEARCH_LIMIT,(data['id'],'Search exceeds owner limit',len(opened))
        assert {zones[k]['owner'] for k in opened}==set(range(15)),'Open search must retain deliberate stable owner mapping'
        assert all(r['zone'] in opened for r in data['rituals']),'Ritual behind permanent seal'
        reached=set();pending=['stage']
        while pending:
            key=pending.pop()
            if key in reached:continue
            reached.add(key);pending.extend((graph[key]&opened)-reached)
        assert reached==opened,(data['id'],'Disconnected open search',opened-reached)
        for link in data['links']:
            assert (link['player_access']=='OPEN_ROUTE')==(link['a'] in opened and link['b'] in opened)
        assert all(z['gameplay_eligible']==(z['id'] in opened) for z in zones.values()),'Gameplay eligible behind seal'
        boundary={l['id'] for l in data['links'] if (l['a'] in opened)!=(l['b'] in opened)}
        assert boundary=={b['link'] for b in data['search_envelope']['barriers']},'Missing permanent search boundary'
        assert data['search_envelope']['sealed_context_spaces']==len(zones)-len(opened)
        assert data['search_envelope']['accessible_spaces']==len(opened)
    return len(zones),len(data['links'])


def render_search_svg(data):
    view=dict(data,id=data['id']+f" / {data['search_envelope']['accessible_spaces']} accessible, {data['search_envelope']['sealed_context_spaces']} sealed")
    view['links']=[l for l in data['links'] if l['player_access']=='OPEN_ROUTE']
    view['zones']=[dict(z,name=z['name']+(' [SEALED CONTEXT]' if z['player_access']=='SEALED_CONTEXT' else ' [OPEN]')) for z in data['zones']]
    return review.render_svg(view)


def sections(data):
    height=160+340*sum(link['kind']=='stairs' for link in data['links'])
    title=review.esc(data['title'].split(' —')[0])
    out=[f'<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="{height}" viewBox="0 0 1200 {height}">',
         f'<rect width="1200" height="{height}" fill="#f5f4ef"/>',
         f'<g font-family="Arial" fill="#1c2a37"><text x="30" y="35" font-size="24">{title}: proposed stair sections</text>',
         '<text x="30" y="60" font-size="15">S02 dimensions only. Full-run architectural voids plus editor world-ground opening remain to be built and tested.</text>']
    for index,link in enumerate(x for x in data['links'] if x['kind']=='stairs'):
        run,rise=link['run_m'],link['rise_m'];base=360+index*340;left=110;scale=45
        x0=left+1.5*scale;x1=x0+run*scale;y0=base;y1=base-rise*scale
        out.extend([f'<text x="30" y="{base-205}" font-size="19">{link["a"]} to {link["b"]} - {link.get("player_access","UNREVIEWED")}</text>',
                    f'<path d="M{left},{y0} H{x0} M{x1},{y1} h{1.5*scale}" stroke="#536477" stroke-width="8" fill="none"/>',
                    f'<path d="M{x0},{y0} L{x1},{y1}" stroke="#a86516" stroke-width="5"/>',
                    f'<path d="M{x0},{y0-2.2*scale} L{x1},{y1-2.2*scale}" stroke="#307e99" stroke-dasharray="5 4" fill="none"/>',
                    f'<text x="680" y="{base-160}" font-size="17">Rise {rise:g} m / run {run:g} m / clear width {link["width"]:g} m</text>',
                    f'<text x="680" y="{base-130}" font-size="17">{link["risers"]} risers, {rise/link["risers"]:.3f} m each (proposed)</text>',
                    f'<text x="680" y="{base-100}" font-size="17">1.5 m landing reservations at both ends</text>',
                    f'<text x="680" y="{base-70}" font-size="17">Cyan: 2.2 m headroom envelope above flight</text>',
                    f'<text x="680" y="{base-40}" font-size="17">Floor/roof depth and underside rays require actual geometry</text>'])
    return '\n'.join(out+['</g></svg>'])


def asset_bounds(names=None):
    records=[]
    for name in names or ['level_student_desk','chair_mp_5','toilet_mp_1','level_bath_basin']:
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
    envelopes={}
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    for folder,data in [('school',school()),('prison',prison()),('mansion',mansion()),('farmhouse',farmhouse()),('asylum',asylum()),('academy',academy()),('college',college()),('church',church())]:
        data=apply_search_envelope(data)
        spaces,links=validate(data)
        envelopes[data['id']]=dict(folder=folder,accessible=data['search_envelope']['accessible_spaces'],sealed=data['search_envelope']['sealed_context_spaces'])
        outputs={'rough-layout.json':json.dumps(data,indent=2,ensure_ascii=False)+'\n',
                 'rough-plan.svg':render_search_svg(data),'rough-sections.svg':sections(data),
                 'rough-asset-bounds.json':json.dumps(asset_bounds(data.get('fit_assets')),indent=2)+'\n',
                 'rough-volumes.obj':review.render_obj(data),
                 'search-layout.json':json.dumps(dict(id=data['id'],title=data['title'],status='SEARCH_GRAPH_DESIGN_NOT_RUNTIME_CONSTRUCTION',construction_source='rough-layout.json',artifact_role='Open search graph only; full structure, furnishings and sealed context remain in rough-layout.json.',site_bounds=data['site_bounds'],zones=[z for z in data['zones'] if z['player_access']=='OPEN_SEARCH'],links=[l for l in data['links'] if l['player_access']=='OPEN_ROUTE'],rituals=data['rituals'],staging=data['staging'],search_envelope=data['search_envelope']),indent=2,ensure_ascii=False)+'\n',
                 'search-envelope.json':json.dumps(dict(site=data['id'],**data['search_envelope'],spaces=[dict(id=z['id'],name=z['name'],owner=z['owner'],access=z['player_access']) for z in data['zones']]),indent=2,ensure_ascii=False)+'\n'}
        for name,text in outputs.items():
            path=OUT/folder/name;raw=text.encode('utf-8')
            if args.check:assert path.is_file() and path.read_bytes()==raw, 'Stale '+str(path)
            else:path.write_bytes(raw)
        print(f"PASS {folder} revised S02 study: {spaces} architectural spaces / {links} reserved connections; {data['search_envelope']['accessible_spaces']} accessible / {data['search_envelope']['sealed_context_spaces']} sealed; geometry and acceptance remain open")
    catalog=json.loads((ROOT/'development/levels/edge-sky-plans/catalog.json').read_text(encoding='utf-8'))
    sites=[]
    for site in catalog['maps']:
        authored=envelopes.get(site['id'])
        sites.append(dict(id=site['id'],folder=site['folder'],maximum=SEARCH_LIMIT,
            status='DESIGN_ENVELOPE_AUTHORED_NOT_RUNTIME_VERIFIED' if authored else 'ROOM_LEVEL_ENVELOPE_AUDIT_REQUIRED',
            accessible=authored['accessible'] if authored else None,sealed=authored['sealed'] if authored else None,
            evidence=f"development/levels/rebuild/{site['folder']}/search-envelope.json" if authored else None))
    assert len(sites)==29
    raw=(json.dumps(dict(policy='development/levels/rebuild/SEARCH_ENVELOPE_POLICY.json',sites=sites),indent=2)+'\n').encode('utf-8')
    path=OUT/'search-envelope-register.json'
    if args.check:assert path.read_bytes()==raw,'Stale search-envelope roster'
    else:path.write_bytes(raw)


if __name__=='__main__':main()
