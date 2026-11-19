"""Author broad site landforms, independently of old room pads and path strips.

Writes explicit source grids and cut programs for the real saved editor terrain
builder. Existing scene/terrain masters are preserved until fit review promotes
these candidates. No inferred geographic survey or playable acceptance.
"""
from pathlib import Path
import argparse,hashlib,json,math

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'development/levels/rebuild/terrain-overall-pass'

def inside(x,z,poly):
    hit=False
    for (ax,az),(bx,bz) in zip(poly,poly[1:]+poly[:1]):
        if (az>z)!=(bz>z) and x<(bx-ax)*(z-az)/(bz-az)+ax:hit=not hit
    return hit

def smooth(t):
    t=max(0,min(1,t));return t*t*(3-2*t)

def height(spec,x,z):
    h=spec['base_y']
    for layer in spec['layers']:
        kind=layer['type']
        if kind=='plane':h+=layer['dx']*(x-layer['origin'][0])+layer['dz']*(z-layer['origin'][1])
        elif kind=='hill':
            cx,cz=layer['center'];rx,rz=layer['radii']
            h+=layer['height']*math.exp(-2*((x-cx)**2/rx**2+(z-cz)**2/rz**2))
        elif kind=='terrace':
            coordinate=x if layer['axis']=='x' else z
            h+=layer['height']*(1-smooth((coordinate-layer['from'])/(layer['to']-layer['from'])))
        elif kind=='channel':
            best=float('inf')
            for (ax,az),(bx,bz) in zip(layer['points'],layer['points'][1:]):
                dx=bx-ax;dz=bz-az;t=max(0,min(1,((x-ax)*dx+(z-az)*dz)/(dx*dx+dz*dz)))
                best=min(best,math.hypot(x-ax-t*dx,z-az-t*dz))
            h-=layer['depth']*(1-smooth(best/layer['half_width']))
        else:raise ValueError(kind)
    assert math.isfinite(h) and -32<h<32
    return h

def cuts(data,spec,bounds):
    """Quarter-metre persistent punches: footprint first, then actual foundations.

    No masks in renders. Existing layouts supply construction apertures, never
    land heights. Raised decks receive no broad earth pad.
    """
    a,b,c,d=bounds;step=.25;rows=[]
    def emit(ix,iz,size):
        states=[not inside(a+(x+.5)*step,b+(z+.5)*step,spec['boundary']) for z in range(iz,iz+size) for x in range(ix,ix+size)]
        if all(states):rows.append((a+ix*step,b+iz*step,a+(ix+size)*step,b+(iz+size)*step,'authored-landform-edge'))
        elif any(states):
            assert size>1
            for dz in [0,size//2]:
                for dx in [0,size//2]:emit(ix+dx,iz+dz,size//2)
    # Align outside cuts to source-cell quadtrees. Thin row strips would force
    # every fully absent 2m cell into 64 tiny leaves and exhaust the editor's
    # valid 20,000-cut budget without adding any geometric fidelity.
    for iz in range(0,round((d-b)/step),8):
        for ix in range(0,round((c-a)/step),8):emit(ix,iz,8)
    for zone in data['zones']:
        if zone['kind'] in ['outside','staging']:continue
        if zone['y']<=0 and zone['y']+zone['height']>=0:
            assert not zone.get('shape'),'Author circular foundation explicitly'
            # This new macroterrain source explicitly authors an aligned
            # excavation envelope. Preserve the building's measured footprint;
            # any excess requires a real foundation rim in the assembly pass.
            x0,z0,x1,z1=zone['bounds']
            rows.append((math.floor(x0/step)*step,math.floor(z0/step)*step,math.ceil(x1/step)*step,math.ceil(z1/step)*step,'foundation-'+zone['id']))
    for link in data['links']:
        if link.get('player_access')=='SEALED_NO_ROUTE':continue
        for p,q in zip(link['points'],link['points'][1:]):
            if min(p[1],q[1])>=0 or max(p[1],q[1])<0:continue
            run=math.hypot(q[0]-p[0],q[2]-p[2])
            if run<.01:continue
            width=link['width']/2+.25;nx=-(q[2]-p[2])/run*width;nz=(q[0]-p[0])/run*width
            corners=[(v[0]+sign*nx,v[2]+sign*nz) for v in [p,q] for sign in [-1,1]]
            rows.append((math.floor(min(p[0] for p in corners)/step)*step,math.floor(min(p[1] for p in corners)/step)*step,math.ceil(max(p[0] for p in corners)/step)*step,math.ceil(max(p[1] for p in corners)/step)*step,'stair-'+link['id']))
    return rows

def write_json(path,data):path.write_bytes((json.dumps(data,indent=2)+'\n').encode())

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--sites',nargs='+');args=parser.parse_args()
    catalogue=json.loads((DEST/'macroterrain-specs.json').read_text());audit=json.loads((DEST/'terrain-envelopes.json').read_text());roster={s['id']:s for s in audit['sites']}
    selected=args.sites or list(catalogue['sites'])
    for key in selected:
        spec=catalogue['sites'][key];row=roster[key];layout=ROOT/row['layout'];data=json.loads(layout.read_text());assert row['status']=='CONTINUOUS_TERRAIN_SOURCE_ABSENT'
        # Explicit footprint envelope determines source tile, not reservation rectangles.
        xs=[p[0] for p in spec['boundary']];zs=[p[1] for p in spec['boundary']]
        a=math.floor(min(xs)/2)*2;b=math.floor(min(zs)/2)*2;c=math.ceil(max(xs)/2)*2;d=math.ceil(max(zs)/2)*2
        assert c-a<=128 and d-b<=128,(key,'requires multi-tile authoring')
        output=DEST/'samples'/key.lower();output.mkdir(parents=True,exist_ok=True)
        values=[height(spec,a+x*2,b+z*2) for z in range(65) for x in range(65)]
        (output/'heights.txt').write_bytes(('\n'.join(f'{v:.9f}' for v in values)+'\n').encode())
        (output/'settings.tsv').write_bytes(('\t'.join(map(str,[a,b,c,d]))+'\n').encode())
        requests=cuts(data,spec,[a,b,c,d]);(output/'cuts.tsv').write_bytes(('\n'.join('\t'.join(map(str,r)) for r in requests)+'\n').encode())
        (output/'grades.tsv').write_bytes(b'')
        failures=[]
        # Deliberate full floor-envelope checking, not just convenient room centres.
        for zone in data['zones']:
            if zone['kind']=='outside':continue
            x0,z0,x1,z1=zone['bounds']
            for x,z in [(x0,z0),(x1,z0),(x1,z1),(x0,z1),((x0+x1)/2,(z0+z1)/2)]:
                if not inside(x,z,spec['boundary']):failures.append(dict(zone=zone['id'],point=[x,z],issue='Construction footprint crosses terrain perimeter'))
        rims=[]
        for request in requests:
            if not request[4].startswith('foundation-'):continue
            zone=next(z for z in data['zones'] if z['id']==request[4][11:])
            if list(request[:4])!=zone['bounds']:rims.append(dict(zone=zone['id'],building_bounds=zone['bounds'],authored_excavation_bounds=list(request[:4]),status='EXPLICIT_FOUNDATION_RIM_REQUIRED_NOT_YET_BUILT'))
        checks=dict(id=key,status='MACROTERRAIN_CANDIDATE_FIT_REVIEW_OPEN',foundation_datums=sorted({z['y'] for z in data['zones']}),perimeter_fit_failures=failures,excavation_rim_requirements=rims,drainage=spec['drainage'],construction_response=spec['construction_response'],source_layout=row['layout'],source_layout_sha256=hashlib.sha256(layout.read_bytes()).hexdigest(),spec_sha256=hashlib.sha256(json.dumps(spec,sort_keys=True).encode()).hexdigest(),author_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),height_range=[min(values),max(values)],pad_snapping=False,path_snapping=False,remaining='Inspect native full-site/sections; reshape blockouts, construct real retaining/foundation transitions and canonical arrival. Source grids are newly authored designs, not measured surveys or terrain acceptance.')
        write_json(output/'provenance.json',checks)
        print('AUTHORED_MACROTERRAIN',key,'cuts',len(requests),'perimeter findings',len(failures),flush=True)
    assert len(catalogue['sites'])==18

if __name__=='__main__':main()
