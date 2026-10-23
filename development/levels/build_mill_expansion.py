"""Blackpine second working-place pass. Original static CC0 models, metre pivots.
Run Blender --background --python this_file -- dust_extractor [timber_rack safety_station].
Each output has a packed GLB, editable source, studio preview and hashed manifest.
"""
import sys, math, json
from pathlib import Path
import bpy
ROOT = Path(__file__).resolve().parents[2]
sys.path[:0] = [str(ROOT/'development/levels'), str(ROOT/'development/assets')]
import build_lived_assets as b
import rebuild_support as s


def materials():
    m = b.init()
    m['green'] = s.material('Old machine enamel', (.16,.22,.18), 'paint')
    return m


def text(parts, body, x, y, z, size, material):
    curve = bpy.data.curves.new('Original mill lettering', 'FONT')
    curve.body=body; curve.size=size; curve.align_x='CENTER'; curve.resolution_u=1
    curve.extrude=.0006
    obj=bpy.data.objects.new(body,curve); bpy.context.collection.objects.link(obj)
    obj.location=(x,y,z); obj.rotation_euler=(math.pi/2,0,0); curve.materials.append(material)
    bpy.ops.object.select_all(action='DESELECT'); obj.select_set(True)
    bpy.context.view_layer.objects.active=obj; bpy.ops.object.convert(target='MESH')
    s.uv_project(bpy.context.object); parts.append(bpy.context.object)


def circular_headrig():
    m=materials();p=[]
    # Blade plane X=-.95 follows the vertical sawn log face. The shaft is below
    # the stock; neither the disc nor the bearings crosses the moving cradle.
    for y in [-.56,.56]:
        p.append(s.box('Headrig foundation runner',(-1.65,y,.07),(1.68,.18,.14),m['oak']))
        for x in [-1.30,-2.12]:
            p.append(s.box('Arbor bearing pedestal',(x,y,.51),(.20,.18,.88),m['iron']))
            p.append(s.rod('Headrig foundation fixing',(x,y,.12),(x,y,.19),.026,m['steel'],6))
    for x in [-1.30,-2.12]:
        p.append(s.box('Arbor bearing crosshead',(x,0,.94),(.28,1.28,.16),m['iron']))
        p.append(s.rod('Split arbor bearing',(x-.10,0,1.04),(x+.10,0,1.04),.13,m['green'],12))
    p.append(s.rod('Head saw arbor',(-2.40,0,1.04),(-.91,0,1.04),.065,m['steel'],12))
    outline=[]
    for i in range(64):
        for offset,radius in [(0,.96),(.24,1.025),(.48,.99),(.82,.94)]:
            a=(i+offset)*math.tau/64;outline.append((radius*math.cos(a),1.04+radius*math.sin(a)))
    n=len(outline);vv=[(x,y,z) for x in [-.962,-.938] for y,z in outline]
    ff=[tuple(range(n-1,-1,-1)),tuple(range(n,n*2))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
    p.append(s.mesh('Two metre circular head saw with 64 real teeth',vv,ff,m['steel']))
    for x in [-1.01,-.925]:
        p.append(s.rod('Saw arbor clamping flange',(x-.018,0,1.04),(x+.018,0,1.04),.18,m['iron'],16))
    p.append(s.rod('Arbor retaining nut',(-.93,0,1.04),(-.89,0,1.04),.10,m['brass'],6))
    p.append(s.rod('Stopped headrig pulley',(-2.36,0,1.04),(-2.24,0,1.04),.39,m['iron'],20))
    p.append(s.box('Belt guard floor shoe',(-2.31,.68,.06),(.26,1.52,.12),m['iron']))
    # A framed mesh guard reads as a separate drive system, not a cutting table.
    for z in [.18,1.48]:p.append(s.box('Drive guard horizontal frame',(-2.31,.55,z),(.08,1.65,.07),m['green']))
    for y in [-.24,1.34]:p.append(s.box('Drive guard upright',(-2.31,y,.83),(.08,.07,1.37),m['green']))
    for i in range(12):p.append(s.rod('Drive guard mesh bar',(-2.31,-.19+i*.13,.20),(-2.31,-.19+i*.13,1.46),.009,m['iron'],4))
    p.append(s.box('Idle motor foundation',(-1.65,1.02,.14),(1.13,.69,.28),m['iron']))
    p.append(s.rod('Isolated headrig motor',(-2.17,1.02,.46),(-1.28,1.02,.46),.27,m['green'],16))
    for x in [-.63,.63]:
        p.append(s.box('Continuous outfeed rail',(x,0,.055),(.07,3.0,.11),m['steel']))
    for y in [-1.35,0,1.35]:p.append(s.box('Outfeed sleeper',(0,y,.035),(1.50,.17,.07),m['oak']))
    p.append(s.box('Outfeed buffer',(0,-1.44,.14),(1.5,.12,.17),m['oak']))
    for x in [-1.20,-.70]:p.append(s.box('Sawdust trough beside real blade slot',(x,0,.025),(.40,2.18,.05),m['rust']))
    p.append(s.box('Headrig lockout plate',(-1.66,-.665,.76),(.65,.025,.21),m['enamel']))
    text(p,'DRIVE ISOLATED',-1.66,-.68,.73,.065,m['red'])
    return p,dict(function='Circular sawmill headrig, two metre toothed blade, below-stock arbor, separate bearings, guarded idle drive and continuous carriage rails',placement='Blackpine logway terminal, with blade plane aligned to carriage vertical sawn face',runtime='Static decommissioned sawmill; no cutting, powered belt or moving carriage',reference='User-supplied historical circular-sawmill photograph, 22 October 2026; original geometry, not a literal reconstruction')


def dust_extractor():
    m=materials(); p=[]
    # Stand carries the fan, filter and collection sack. No floating ductwork.
    for x in [-.67,.67]:
        p.append(s.box('Skid foot',(x,0,.045),(.13,1.10,.09),m['iron']))
        for y in [-.40,.40]:
            p.append(s.box('Bolted stand upright',(x,y,.69),(.065,.065,1.30),m['green']))
            p.append(s.rod('Skid anchor bolt',(x,y,.08),(x,y,.12),.022,m['steel'],6))
        p.append(s.rod('Stand diagonal',(x,-.40,.12),(x,.40,1.28),.024,m['iron'],6))
    # Split deck leaves a real aperture for the hopper outlet and sack neck.
    for x,w in [(-.31,.86),(.58,.32)]:
        p.append(s.box('Fan bearing deck side',(x,0,1.31),(w,1.0,.075),m['green']))
    for y in [-.33,.33]:
        p.append(s.box('Hopper throat deck rim',(.27,y,1.31),(.30,.34,.075),m['green']))
    p.append(s.rod('Stopped fan housing',(-.45,-.24,1.59),(-.45,.24,1.59),.25,m['green'],16))
    p.append(s.rod('Idle motor',(-.45,.24,1.59),(-.45,.49,1.59),.14,m['iron'],12))
    for x in [-.54,-.45,-.36]:
        p.append(s.box('Motor cooling rib',(x,.37,1.59),(.018,.25,.31),m['steel']))
    # Hollow duct ends and a visible cap indicate disconnection, not active extraction.
    p+=s.tube('Short disconnected suction elbow',[(-.45,-.24,1.59),(-.45,-.43,1.59),(-.45,-.53,1.36)],.095,m['steel'],12)
    p.append(s.rod('Bolted inlet blank',(-.45,-.53,1.36),(-.45,-.55,1.33),.115,m['rust'],12))
    p+=s.tube('Fan to separator neck',[(-.21,0,1.59),(.03,0,1.59),(.20,0,1.78)],.095,m['steel'],12)
    p.append(s.lathe('Separator hopper',[(1.31,.14),(1.55,.34),(1.87,.34),(1.91,.30)],m['green'],16,(.27,0,0)))
    p.append(s.lathe('Pleated upper filter',[(1.91,.27),(2.42,.27),(2.47,.24)],m['cloth'],16,(.27,0,0)))
    for i in range(16):
        a=i*math.tau/16
        p.append(s.rod('Filter vertical seam',(.27+.272*math.cos(a),.272*math.sin(a),1.94),(.27+.272*math.cos(a),.272*math.sin(a),2.42),.008,m['oak'],5))
    p.append(s.rod('Filter lid',(.27,0,2.47),(.27,0,2.50),.28,m['iron'],16))
    # Sack base is on its shelf, with a continuous neck up to the hopper outlet.
    p.append(s.box('Collection sack support shelf',(.27,0,.17),(.76,.76,.06),m['iron']))
    p.append(s.lathe('Heavy dust sack',[(.20,.24),(.29,.32),(.85,.32),(1.20,.13),(1.32,.13)],m['cloth'],16,(.27,0,0)))
    p+=b.ring('Sack clamping band',(.27,0,1.30),.145,.015,m['rust'],axis='Z',segments=16)
    p.append(s.box('Lockout tag plate',(-.45,-.262,1.57),(.31,.018,.19),m['enamel']))
    text(p,'ISOLATED',-.45,-.274,1.55,.052,m['red'])
    return p,dict(function='Stopped dust extractor: braced stand, fan, capped inlet, filter and supported sack',placement='Blackpine sawmill west wall, away from feed carriage and loft stair',runtime='Static isolated machinery; no suction, dust particles or powered simulation')


def timber_rack():
    m=materials(); p=[]
    for x in [-1.12,1.12]:
        p.append(s.box('Rack bearing foot',(x,0,.05),(.20,.92,.10),m['oak']))
        for y in [-.35,.35]:
            p.append(s.box('Rack upright',(x,y,.92),(.09,.09,1.74),m['oak']))
        for z in [.32,.91,1.5]:
            p.append(s.box('Load bearing crossarm',(x,0,z),(.12,.87,.10),m['pine']))
            for y in [-.35,.35]:
                p.append(s.rod('Rack fixing bolt',(x-.07,y,z),(x+.07,y,z),.021,m['rust'],6))
    p.append(s.rod('Rear diagonal brace',(-1.12,.36,.15),(1.12,.36,1.72),.028,m['iron'],6))
    for shelf,z in enumerate([.39,.98,1.57]):
        for row in range(3):
            length=2.55-.13*((shelf+row)%3)
            p.append(s.box('Sorted sawn timber',(0,-.25+row*.245,z),(length,.21,.06),m['pine']))
        if shelf<2:
            for x in [-.92,.92]:
                p.append(s.box('Air drying sticker',(x,-.005,z+.047),(.055,.72,.034),m['oak']))
            for row in range(3):
                p.append(s.box('Second separated timber course',(.04,-.25+row*.245,z+.094),(2.40,.21,.06),m['pine']))
        for x in [-.93,.93]:
            p.append(s.box('Timber end stop',(x,-.44,z+.015),(.11,.055,.15),m['iron']))
    p.append(s.box('Stock label bearing rail',(0,-.4125,1.48),(2.33,.035,.18),m['oak']))
    for x in [-1.12,1.12]:
        p.append(s.rod('Label rail fixing bolt',(x,-.425,1.48),(x,-.447,1.48),.017,m['rust'],6))
    p.append(s.box('Old stock label',(0,-.440,1.48),(.72,.02,.16),m['paper']))
    text(p,'DRY STOCK',0,-.452,1.45,.083,m['black'])
    return p,dict(function='Braced dry-timber rack with supported crossarms, air-drying stickers and stock label',placement='Blackpine timber tool store south wall; both east/west doors remain open',runtime='Static sorted timber; no movable boards or rack climbing')


def safety_station():
    m=materials(); p=[]
    # Freestanding panel avoids inventing a wall-mounted pivot on an existing mesh.
    for x in [-.63,.63]:
        p.append(s.box('Station floor foot',(x,0,.045),(.11,.64,.09),m['iron']))
        p.append(s.box('Station bearing post',(x,.17,.92),(.075,.075,1.75),m['oak']))
    p.append(s.box('Station back panel',(0,.17,1.17),(1.42,.065,1.30),m['pine']))
    p.append(s.box('First aid cabinet',(0,.015,1.28),(.69,.25,.58),m['enamel'],.012))
    p.append(s.box('Cabinet door seam',(0,-.114,1.28),(.64,.009,.53),m['green']))
    p.append(s.box('Cross upright',(0,-.121,1.29),(.075,.008,.28),m['ivory']))
    p.append(s.box('Cross arms',(0,-.122,1.29),(.27,.008,.075),m['ivory']))
    for z in [1.08,1.48]:p.append(s.rod('Cabinet hinge',(-.33,-.12,z-.035),(-.33,-.12,z+.035),.015,m['brass'],8))
    p.append(s.rod('Cabinet pull',(.25,-.14,1.22),(.25,-.14,1.35),.012,m['steel'],8))
    p.append(s.box('Station shelf',(0,-.025,.63),(1.32,.49,.065),m['oak']))
    for x in [-.55,.55]:p.append(s.rod('Shelf support strut',(x,.15,.40),(x,-.22,.59),.021,m['iron'],6))
    for i in range(4):p.append(s.box('Folded old blanket',(-.30,0,.684+i*.043),(.51,.36,.043),m['cloth'],.006))
    p.append(s.box('Bandage supply tin',(.34,.01,.76),(.30,.25,.20),m['enamel'],.012))
    p.append(s.box('Tin lid',(.34,.01,.867),(.32,.27,.025),m['steel']))
    p.append(s.box('Safety heading',(0,.125,1.72),(1.15,.02,.17),m['enamel']))
    text(p,'FIRST AID',0,.111,1.68,.11,m['red'])
    p.append(s.box('Expired inspection card',(.49,.129,1.12),(.19,.018,.28),m['paper']))
    text(p,'2009',.49,.118,1.12,.047,m['black'])
    return p,dict(function='Freestanding first aid cabinet, supported shelf, old blanket and closed bandage tin',placement='Blackpine repair shed east wall; workshop circulation stays clear',runtime='Static historical dressing; cabinet and supplies do not heal or award inventory')


if __name__=='__main__':
    names=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else ['dust_extractor','timber_rack','safety_station']
    for name in names:
        parts,notes=globals()[name]()
        b.export('level_work_'+name,parts,'working-place-details',view=(3.5,-5,3.1))
        entry=b.LOG[-1];entry.update(notes)
        (ROOT/f'development/levels/production/{name}.json').write_text(json.dumps(entry,indent=2)+'\n')
        bpy.ops.wm.open_mainfile(filepath=str(ROOT/entry['editable']))
        obj=bpy.data.objects[entry['id']];obj.data.calc_loop_triangles()
        assert len(obj.data.loop_triangles)==entry['triangles'] and obj.data.uv_layers
        assert all(im.packed_file for im in bpy.data.images if im.source=='FILE')
        print('MILL_EXPANSION_COMPLETE',name,entry['triangles'],flush=True)
