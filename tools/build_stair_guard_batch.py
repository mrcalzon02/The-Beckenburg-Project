from pathlib import Path
import sys,json,math,struct,io,copy
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from review_roster_macroterrain import records,sha
from append_static_glb import append,read
from export_roster_collision import face_keys,native_faces
from build_roster_foundations import prism
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'stair-guard-pass';MODEL=ROOT/'development/levels/primitive-props-v1/models/level_blockout_guardrail.glb'
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
def build(s):
 site=s['id'];folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];native=ROOT/s['native'];assert sha(source)==s['glb_sha256'] and sha(native)==s['native_sha256'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));before=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(MODEL));rail=next(o for o in set(bpy.context.scene.objects)-before if o.type=='MESH');template=rail.data.copy();mat=template.materials[0]
 for o in set(bpy.context.scene.objects)-before:bpy.data.objects.remove(o,do_unlink=True)
 built=[];flights=[]
 for link in data['links']:
  if link['id'] not in meta['design']['build']:continue
  for index,(a,b) in enumerate(zip(link['points'],link['points'][1:])):
   if abs(a[1]-b[1])<.01:continue
   length=math.hypot(b[0]-a[0],b[2]-a[2]);ux=(b[0]-a[0])/length;uz=(b[2]-a[2])/length;slope=(b[1]-a[1])/length;half=link['width']/2;setback=1.6;usable=length-2*setback;bay_count=max(1,math.ceil(usable/2));assert usable>=2
   for side in [-1,1]:
    nx=-uz*side;nz=ux*side
    for j in range(bay_count):
     start=setback+j*(usable-2)/max(1,bay_count-1);mesh=template.copy();o=bpy.data.objects.new('Supported guard '+site+' '+link['id']+' '+str(index)+' '+str(side)+' '+str(j),mesh);bpy.context.collection.objects.link(o)
     for v in mesh.vertices:
      run=start+v.co.x+1;off=half+.08+v.co.y*side;x=a[0]+ux*run+nx*off;z=a[2]+uz*run+nz*off;y=a[1]+slope*run-.05+v.co.z;v.co=(x,-z,y)
     o['role']='fitted library stair guard';o['source_asset']='level_blockout_guardrail';o['link']=link['id'];built.append(o)
    # Solid side stringer seats the rail bases, overlaps tread edges by 20mm,
    # and remains outside the authored capsule sweep width.
    aa=[a[0]+ux*setback,a[1]+slope*setback,a[2]+uz*setback];bb=[b[0]-ux*setback,b[1]-slope*setback,b[2]-uz*setback]
    p0=(aa[0]+nx*(half-.02),aa[2]+nz*(half-.02));p1=(bb[0]+nx*(half-.02),bb[2]+nz*(half-.02));p2=(bb[0]+nx*(half+.18),bb[2]+nz*(half+.18));p3=(aa[0]+nx*(half+.18),aa[2]+nz*(half+.18));poly=[p0,p1,p2,p3]
    # prism helper only permits level tops; author the matching sloping solid.
    vs=[(x,-z,y) for heights in [[aa[1]-.4,bb[1]-.4,bb[1]-.4,aa[1]-.4],[aa[1]+.02,bb[1]+.02,bb[1]+.02,aa[1]+.02]] for (x,z),y in zip(poly,heights)];m=bpy.data.meshes.new('Seated stringer');m.from_pydata(vs,[],[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]);m.materials.append(mat);o=bpy.data.objects.new('Side stringer '+site+' '+link['id']+' '+str(side),m);bpy.context.collection.objects.link(o);o['role']='structural side stringer';o['link']=link['id'];built.append(o)
   flights.append(dict(link=link['id'],segment=index,width=link['width'],bay_count_per_side=bay_count,source_asset=MODEL.relative_to(ROOT).as_posix(),source_sha256=sha(MODEL),method='Source rail profile/UV/material copied; shear follows authored flight grade, uprights remain vertical; tread-edge stringers seat post bases. Original library master unchanged.'))
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.00001);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,o.name;bm.to_mesh(o.data);bm.free()
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'guarded-stairs.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 addition=ROOT/'build'/('guard-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source,addition,folder/'guarded-stairs.glb');parts=[(o.name,records(o)) for o in built];raw=native.read_bytes();magic,n,t=struct.unpack_from('>III',raw);payload=struct.pack('>III',magic,n+len(parts),t+sum(len(rs) for _,rs in parts))+raw[12:]
 for name,rs in parts:
  encoded=name.encode();payload+=struct.pack('>H',len(encoded))+encoded+struct.pack('>BI',1,len(rs))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in rs)
 target=folder/'guarded-stairs.hcs2';target.write_bytes(payload);assert face_keys(native_faces(target))==face_keys(native_faces(native)+[r for _,rs in parts for r in rs]);bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'guarded-stairs.glb'));objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,rs in parts:assert face_keys(records(objects[name]))==face_keys(rs),(site,name)
 meta.update(status='FITTED_LIBRARY_GUARDS_AND_STRINGERS_NATIVE_CANDIDATE_RUNTIME_OPEN',source_glb=s['glb'],source_glb_sha256=sha(source),source_physics=s['native'],source_physics_sha256=sha(native),before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],guard_flights=flights,new_guard_parts=len(parts),glb_sha256=sha(folder/'guarded-stairs.glb'),physics_sha256=sha(target),remaining='Terrain interfaces, landing perimeter guards/returns, frame/anchor detailing and runtime integration remain open. No terrain-hole change or map unlock.');write(folder/'construction.json',meta)
 print('GUARDED_FLIGHTS',site,len(flights),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=s['layout'],glb=(folder/'guarded-stairs.glb').relative_to(ROOT).as_posix(),native=target.relative_to(ROOT).as_posix(),blend=(folder/'guarded-stairs.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 roster=json.loads((BASE/'orthogonal-stair-pass/roster.json').read_text());active=[build(s) if s['id'] in ['WLD-06','WLD-08','WLD-12'] else s for s in roster['active_sources']];write(OUT/'roster.json',dict(active_sources=active,sites=[dict(id=s['id'],decision='GUARD_STRINGER_CANDIDATE' if s['id'] in ['WLD-06','WLD-08','WLD-12'] else 'RETAIN_PRIOR_SOURCE') for s in roster['sites']]))
if __name__=='__main__':main()

