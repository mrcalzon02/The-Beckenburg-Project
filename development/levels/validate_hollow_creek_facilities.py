"""Independently reopen native sources, test supports and measured placement.
Route checks are against planning reservations, not runtime navigation acceptance.
"""
import bpy,json,hashlib,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek'
d=json.loads((OUT/'facilities.json').read_text(encoding='utf-8'))
layout=json.loads((ROOT/'development/levels/layouts/wld-02/layout.json').read_text(encoding='utf-8'))
def part(obj,prefix):
    groups={g.index for g in obj.vertex_groups if g.name.startswith(prefix)}
    points=[v.co for v in obj.data.vertices if any(g.group in groups and g.weight>.5 for g in v.groups)];assert points,prefix
    return [[min(v[i] for v in points) for i in range(3)],[max(v[i] for v in points) for i in range(3)]]
def touch(obj,a,b):
    a,b=part(obj,a),part(obj,b)
    assert all(max(0,a[0][i]-b[1][i],b[0][i]-a[1][i])<.001 for i in range(3)),(a,b)
asset_checks=[]
for asset in d['assets']:
    path=ROOT/asset['editable'];bpy.ops.wm.open_mainfile(filepath=str(path));obj=bpy.data.objects[asset['id']]
    obj.data.calc_loop_triangles();assert len(obj.data.loop_triangles)==asset['triangles'];assert obj.data.uv_layers
    assert abs(min(v.co.z for v in obj.data.vertices))<1e-5
    assert hashlib.sha256((ROOT/f"assets/runtime/models/{asset['id']}.glb").read_bytes()).hexdigest()==asset['sha256']
    assert all(im.packed_file for im in bpy.data.images if im.source=='FILE')
    if asset['id'].endswith('picnic_table'):
        for a,b in [('Tabletop slat','Tabletop bearing rail'),('Bench slat','Bench bearing crossmember'),('Bench bearing crossmember','Splayed support leg'),('Splayed support leg','Ground-bearing foot shoe'),('Frame triangulation','Central longitudinal stretcher')]:touch(obj,a,b)
        assert abs(part(obj,'Tabletop slat')[1][2]-.76)<1e-5
        assert abs(part(obj,'Bench slat')[1][2]-.45)<1e-5
        # End approach: actual vertices, including brackets/bolts, not overall bounds.
        for v in obj.data.vertices:
            assert not (-1.22<v.co.x<-.61 and abs(v.co.y)<.381 and .23<v.co.z<.686),'blocked end knee/toe space'
    else:
        for a,b in [('Grate perimeter','Grate support lug'),('Cooking grate bar','Grate perimeter'),('Rolled upper ring edge','Open steel fire ring')]:touch(obj,a,b)
    contrasts=[]
    for mat in obj.data.materials:
        if not any(mat.name.startswith('Rebuild Original '+key) for key in ['iron','steel','rust']):continue
        im=next(n.image for n in mat.node_tree.nodes if n.type=='TEX_IMAGE');w,h=im.size;pixels=list(im.pixels)
        rows=[sum(sum(pixels[(y*w+x)*4:(y*w+x)*4+3])/3 for x in range(w))/w for y in range(h)]
        value=(max(rows)-min(rows))/(sum(rows)/len(rows));assert value<.025
        contrasts.append(dict(material=mat.name,row_contrast=value))
    asset_checks.append(dict(id=asset['id'],triangles=asset['triangles'],floor_pivot=True,support_contacts=True,metal_pixel_checks=contrasts))

def point_distance(p,a,b):
    dx=b[0]-a[0];dz=b[1]-a[1];n=dx*dx+dz*dz;t=max(0,min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dz)/n)) if n else 0
    return math.hypot(p[0]-a[0]-dx*t,p[1]-a[1]-dz*t)
def segment_rect(a,b,rect):
    x0,z0,x1,z1=rect;lo=0;hi=1
    for start,delta,mn,mx in [(a[0],b[0]-a[0],x0,x1),(a[1],b[1]-a[1],z0,z1)]:
        if abs(delta)<1e-10:
            if not mn<=start<=mx:break
        else:
            t0,t1=sorted([(mn-start)/delta,(mx-start)/delta]);lo=max(lo,t0);hi=min(hi,t1)
            if lo>hi:break
    else:return 0
    corners=[(x0,z0),(x1,z0),(x1,z1),(x0,z1)]
    distance=min(point_distance(p,a,b) for p in corners)
    for p in [a,b]:distance=min(distance,math.hypot(max(x0-p[0],0,p[0]-x1),max(z0-p[1],0,p[1]-z1)))
    return distance

bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-facilities.blend'))
zones={z['id']:z for z in layout['zones']};assets={a['id']:a for a in d['assets']};rects=[];placements=[]
for p in d['placements']:
    obj=bpy.data.objects[p['native_object']];assert all(abs(a-b)<1e-5 for a,b in zip(obj.location,(p['x'],-p['z'],p['y'])))
    lo,hi=assets[p['model']]['bounds'];rect=[p['x']+lo[0],p['z']+lo[2],p['x']+hi[0],p['z']+hi[2]]
    x0,z0,x1,z1=zones[p['zone']]['bounds'];assert x0+.08<rect[0]<rect[2]<x1-.08 and z0+.08<rect[1]<rect[3]<z1-.08
    for other in rects:assert min(rect[2],other[2])<=max(rect[0],other[0]) or min(rect[3],other[3])<=max(rect[1],other[1]),'model overlap'
    rects.append(rect);margin=999
    for link in layout['links']:
        for a,b in zip(link['points'],link['points'][1:]):
            clearance=segment_rect((a[0],a[2]),(b[0],b[2]),rect)-link['width']/2-.22
            margin=min(margin,clearance);assert clearance>.03,(p['id'],link['id'],clearance)
    # Do not occupy the clear ritual reservation or tent pad.
    for reservation in d['reservations']:
        rx,rz=reservation['x'],reservation['z'];hx,hz=reservation['width']/2,reservation['depth']/2
        assert min(rect[2],rx+hx)<=max(rect[0],rx-hx) or min(rect[3],rz+hz)<=max(rect[1],rz-hz)
    placements.append(dict(id=p['id'],within_zone=True,disjoint_from_facilities_and_reservations=True,min_planning_route_extra_clearance=round(margin,4)))
for name,digest in d['source_hashes'].items():assert hashlib.sha256((ROOT/name).read_bytes()).hexdigest()==digest,name
report=dict(status='pass-native-only',assets=asset_checks,placements=placements,original_layout_and_17_blockout_glbs_unchanged=True,native_scene_sha256=hashlib.sha256((OUT/'hollow-creek-facilities.blend').read_bytes()).hexdigest(),limitations=d['limitations'])
(ROOT/'development/levels/verification/hollow-creek-facilities.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('PASS 2 native sources, physical supports, knee-space candidate, metal pixels, 4 facility positions, reserved routes and unchanged rough roster',flush=True)
