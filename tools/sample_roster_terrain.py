"""Sample actual candidate terrain for the saved editor source; Blender background."""
from pathlib import Path
import argparse,hashlib,json,math,sys,subprocess
import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT=Path(__file__).resolve().parents[1]

def sample(site,source_ref=None,terrain_site=None):
    source_site=terrain_site or site
    source=ROOT/f'development/levels/rebuild/structures/{source_site}/rough-shell.glb'
    layout=next(p for p in [ROOT/f'development/levels/rebuild/{site}/rough-layout.json',ROOT/f'development/levels/layouts/{site}/layout.json',ROOT/f'development/levels/layout-expansions/{site}/layout.json'] if p.exists())
    cut_layout=layout;cut_bytes=layout.read_bytes();cut_data=json.loads(cut_bytes)
    source_bytes=source.read_bytes();layout_bytes=layout.read_bytes()
    commit=subprocess.check_output(['git','rev-parse',source_ref or 'HEAD'],cwd=ROOT,text=True).strip()
    import_source=source
    if source_ref:
        candidates=[ROOT/f'development/levels/rebuild/{source_site}/rough-layout.json',ROOT/f'development/levels/layouts/{source_site}/layout.json',ROOT/f'development/levels/layout-expansions/{source_site}/layout.json']
        layout=next((p for p in candidates if subprocess.run(['git','cat-file','-e',commit+':'+p.relative_to(ROOT).as_posix()],cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0),None)
        assert layout is not None,('No authoritative source at seed ref',site,commit)
        source_bytes=subprocess.check_output(['git','show',commit+':'+source.relative_to(ROOT).as_posix()],cwd=ROOT)
        layout_bytes=subprocess.check_output(['git','show',commit+':'+layout.relative_to(ROOT).as_posix()],cwd=ROOT)
        import_source=ROOT/f'build/terrain-seed-{site}.glb';import_source.write_bytes(source_bytes)
    data=json.loads(layout_bytes);a,b,c,d=cut_data['site_bounds'];sa,sb,sc,sd=data['site_bounds']
    assert a>=sa and b>=sb and c<=sc and d<=sd,'Current play footprint must lie inside the explicit source terrain coordinates'
    if not terrain_site:assert cut_data['site_bounds']==data['site_bounds'],'Same-site seed bounds changed'
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(import_source))
    ground=[o for o in bpy.context.scene.objects if o.type=='MESH' and o.name.startswith('Authored ') and 'terrain' in o.name]
    assert len(ground)==1,(site,[o.name for o in ground])
    obj=ground[0];points=[obj.matrix_world@v.co for v in obj.data.vertices]
    tree=BVHTree.FromPolygons(points,[list(p.vertices) for p in obj.data.polygons])
    def hit(x,z):return tree.ray_cast(Vector((x,-z,100)),Vector((0,0,-1)))[0]
    output=ROOT/f'build/terrain-samples/{site}';output.mkdir(parents=True,exist_ok=True)
    values=[];fallback=0
    for iz in range(65):
        for ix in range(65):
            x=a+ix*2;z=b+iz*2;p=hit(x,z)
            if p is None:p=tree.find_nearest(Vector((x,-z,0)))[0];fallback+=1
            values.append(p.z)
    (output/'heights.txt').write_bytes(('\n'.join(str(y) for y in values)+'\n').encode())
    cuts=[]
    # Preserve the actual source coastline/shaft footprint at quarter-metre
    # native grid resolution, with contiguous row rectangles, not render masks.
    step=.25
    for row in range(round((d-b)/step)):
        start=None;z=b+(row+.5)*step
        for col in range(round((c-a)/step)+1):
            outside=col<round((c-a)/step) and hit(a+(col+.5)*step,z) is None
            if outside and start is None:start=col
            if not outside and start is not None:
                cuts.append((a+start*step,b+row*step,a+col*step,b+(row+1)*step,'source-footprint'));start=None
    # Immutable seed geometry supplies heights/coastline only. Current authored
    # basement/entry coordinates must drive new persistent punch requests.
    for zone in cut_data['zones']:
        if zone['kind'] in ['outside','staging']:continue
        if zone['y']<=0 and zone['y']+zone['height']>=0:
            if zone.get('shape')=='circle':
                x0,z0,x1,z1=zone['bounds'];cx=(x0+x1)/2;cz=(z0+z1)/2;radius=zone['radius']-.125
                for row in range(math.floor((cz-radius-b)/step),math.ceil((cz+radius-b)/step)):
                    low=b+row*step;far=max(abs(low-cz),abs(low+step-cz))
                    if far>=radius:continue
                    half=math.sqrt(radius*radius-far*far)
                    left=a+math.ceil((cx-half-a)/step)*step;right=a+math.floor((cx+half-a)/step)*step
                    if right>left:cuts.append((left,low,right,low+step,'foundation-'+zone['id']))
            else:cuts.append((*zone['bounds'],'foundation-'+zone['id']))
    for link in cut_data['links']:
        if link.get('player_access')=='SEALED_NO_ROUTE':continue
        for p,q in zip(link['points'],link['points'][1:]):
            if min(p[1],q[1])>=0 or max(p[1],q[1])<0:continue
            run=math.hypot(q[0]-p[0],q[2]-p[2])
            if run<.01:continue
            width=link['width']/2+.25;nx=-(q[2]-p[2])/run*width;nz=(q[0]-p[0])/run*width
            corners=[(v[0]+s*nx,v[2]+s*nz) for v in [p,q] for s in [-1,1]]
            x0=math.floor(min(v[0] for v in corners)/step)*step;z0=math.floor(min(v[1] for v in corners)/step)*step
            x1=math.ceil(max(v[0] for v in corners)/step)*step;z1=math.ceil(max(v[1] for v in corners)/step)*step
            cuts.append((x0,z0,x1,z1,'stair-'+link['id']))
    (output/'cuts.tsv').write_bytes(('\n'.join('\t'.join(str(v) for v in row) for row in cuts)+'\n').encode())
    grades=[];links={link['id']:link for link in cut_data['links']}
    for grade in cut_data.get('terrain_grading',[]):
        assert '\t' not in grade['name'] and '\n' not in grade['name']
        link=links[grade['link']];p,q=link['points'][grade['segment']:grade['segment']+2]
        grades.append('\t'.join(map(str,[grade['name'],grade['half_width_metres'],grade['shoulder_metres'],p[0],p[2],p[1],q[0],q[2],q[1]])))
    (output/'grades.tsv').write_bytes(('\n'.join(grades)+('\n' if grades else '')).encode())
    (output/'settings.tsv').write_bytes(('\t'.join(str(v) for v in [a,b,c,d])+'\n').encode())
    # Exclude derived terrain bindings from the cut-definition digest: binding
    # the resulting world back to this JSON must not make its own cut data stale.
    cut_definition={key:cut_data[key] for key in ['site_bounds','zones','links']}
    # Explicit donor is recorded in the actual path/source-layout fields; no
    # fallback to an unrelated map or synthetic flat source is permitted.
    (output/'provenance.json').write_bytes((json.dumps(dict(source_commit=commit,source_glb=source.relative_to(ROOT).as_posix(),source_glb_sha256=hashlib.sha256(source_bytes).hexdigest(),source_layout=layout.relative_to(ROOT).as_posix(),source_layout_sha256=hashlib.sha256(layout_bytes).hexdigest(),cut_layout=cut_layout.relative_to(ROOT).as_posix(),cut_definition_sha256=hashlib.sha256(json.dumps(cut_definition,sort_keys=True,separators=(',',':')).encode()).hexdigest(),sampler_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),grid_metres=2,cut_grid_metres=step,edge_and_void_resampling='Quarter-metre cells classify immutable source footprint; current authored foundations/entrances supply persistent cut requests; no final terrain/edge acceptance',nearest_fallback_samples=fallback,cuts=len(cuts)),indent=2)+'\n').encode())
    print('SAMPLED_EDITOR_SOURCE',site,len(values),len(cuts),flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--sites',nargs='+',required=True);parser.add_argument('--source-ref');parser.add_argument('--terrain-source',nargs='*',default=[])
    args=parser.parse_args(sys.argv[sys.argv.index('--')+1:])
    mappings=dict(row.lower().split('=',1) for row in args.terrain_source)
    assert set(mappings)<={site.lower() for site in args.sites},'Unexpected terrain source mapping'
    for site in args.sites:sample(site.lower(),args.source_ref,mappings.get(site.lower()))
