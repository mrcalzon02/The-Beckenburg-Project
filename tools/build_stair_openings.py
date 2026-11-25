"""Physical stair corridors through active structural walls and floor/caps.

No terrain/prop/sealed-context removal; no collision-only holes or tolerances.
"""
from pathlib import Path
import argparse,csv,json,math,struct,sys
import bpy,bmesh
from mathutils import Matrix,Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from append_static_glb import append
from export_roster_collision import face_keys
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'stair-opening-pass'

def corridor(name,p,q,width,bottom=.005):
    run=math.hypot(q[0]-p[0],q[2]-p[2]);nx=-(q[2]-p[2])/run*(width/2+.02);nz=(q[0]-p[0])/run*(width/2+.02)
    coords=[]
    for height in [bottom,2.2]:
        for a,side in [(p,-1),(p,1),(q,1),(q,-1)]:coords.append((a[0]+side*nx,-a[2]-side*nz,a[1]+height))
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(coords,[],[(0,1,2,3),(7,6,5,4),(0,4,5,1),(1,5,6,2),(2,6,7,3),(3,7,4,0)]);mesh.update();bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(mesh);bm.free();obj=bpy.data.objects.new(name,mesh);bpy.context.scene.collection.objects.link(obj);return obj

def bounds(obj):
    ps=[obj.matrix_world@Vector(v) for v in obj.bound_box]
    return [(min(p[j] for p in ps),max(p[j] for p in ps)) for j in range(3)]

def volume(obj):
    bm=bmesh.new();bm.from_mesh(obj.data);v=abs(bm.calc_volume(signed=True))*abs(obj.matrix_world.determinant());bm.free();return v

def main():
    p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+',required=True);args=p.parse_args(sys.argv[sys.argv.index('--')+1:]);fail=list(csv.DictReader((BASE/'stair-landing-pass/verification/failure-contacts.tsv').open(),delimiter='\t'))
    for site in args.sites:
        oldfolder=BASE/'stair-landing-pass'/site.lower();r=json.loads((oldfolder/'construction.json').read_text());source=oldfolder/'supported-stairs.glb';native=oldfolder/'supported-stairs.hcs2';layout=ROOT/r['layout'];data=json.loads(layout.read_text());links={l['id']:l for l in data['links']};targets=sorted({(row['link'],int(row['segment'])) for row in fail if row['site']==site and any(word in row['component'] for word in ['Wall ','Floor ','Ceiling cap '])})
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));original=[o for o in bpy.context.scene.objects if o.type=='MESH'];cutters=[];corridors=[]
        # Include intermediate flat landings: their body volume is part of the
        # same continuous stair route and can intersect a different floor/cap.
        for identity in sorted({identity for identity,index in targets}):
            link=links[identity]
            for actual,(a,b) in enumerate(zip(link['points'],link['points'][1:])):
                run=math.hypot(b[0]-a[0],b[2]-a[2])
                if run<.01:continue
                label='Stair opening corridor '+identity+' segment'+str(actual)
                for wall in [False,True]:
                    cutter=corridor(label+(' wall' if wall else ' slab'),a,b,link['width'],-.25 if wall else .005);cutter['wall_cutter']=wall;cutters.append(cutter)
                dx=(b[0]-a[0])/run;dz=(b[2]-a[2])/run
                for suffix,end,sign in [('start',a,-1),('end',b,1)]:
                    extended=[end[0]+dx*.35*sign,end[1],end[2]+dz*.35*sign]
                    for wall in [False,True]:
                        cutter=corridor(label+' '+suffix+(' wall' if wall else ' slab'),end,extended,link['width'],-.25 if wall else .005);cutter['wall_cutter']=wall;cutters.append(cutter)
                corridors.append(dict(link=identity,source_segment=r['source_segment_map'][identity].get(str(actual)),actual_segment=actual,points=[a,b],width=link['width']+.04,bottom_offset=.005,wall_bottom_offset=-.25,headroom=2.2,terminal_extension=.35,cutter_name=label))
        bpy.context.view_layer.update();built=[];removed=[];changes=[]
        for obj in original:
            if obj.get('role') not in ['rough shell wall','floor','ceiling-cap']:continue
            # Context seals/props and terrain are never candidates for this cut.
            bb=bounds(obj);near=[c for c in cutters if c['wall_cutter']==(obj.get('role')=='rough shell wall') and all(a[0]<b[1] and a[1]>b[0] for a,b in zip(bb,bounds(c)))]
            if not near:continue
            copy=obj.copy();copy.data=obj.data.copy();bpy.context.scene.collection.objects.link(copy);before=volume(copy);bm=bmesh.new();bm.from_mesh(copy.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-8);bm.to_mesh(copy.data);bm.free()
            for cutter in near:
                bpy.context.view_layer.objects.active=copy;modifier=copy.modifiers.new('Actual authored stair opening','BOOLEAN');modifier.operation='DIFFERENCE';modifier.solver='EXACT';modifier.object=cutter;bpy.ops.object.modifier_apply(modifier=modifier.name)
            after=volume(copy)
            if before-after<1e-7:bpy.data.objects.remove(copy,do_unlink=True);continue
            oldname=obj.name;removed.append(oldname);bpy.data.objects.remove(obj,do_unlink=True);copy.name='Opened stair shell '+oldname;copy['source_mesh']=oldname;copy['opening_purpose']='Measured active stair body corridor; terrain and fixed context unchanged'
            if not copy.data.polygons:
                changes.append(dict(source_mesh=oldname,new_mesh=None,removed_volume_m3=before,reason='Entire small slab cell inside actual shaft'));bpy.data.objects.remove(copy,do_unlink=True);continue
            bpy.ops.object.select_all(action='DESELECT');copy.select_set(True);bpy.context.view_layer.objects.active=copy;bpy.ops.object.transform_apply(location=False,rotation=True,scale=True);bm=bmesh.new();bm.from_mesh(copy.data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bmesh.ops.triangulate(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges),(site,oldname,'opening is not closed');bm.to_mesh(copy.data);bm.free();built.append(copy);changes.append(dict(source_mesh=oldname,new_mesh=copy.name,removed_volume_m3=before-after,source_role=copy.get('role'),source_origin=list(copy.location),cutters=[c.name for c in near]))
        for cutter in cutters:bpy.data.objects.remove(cutter,do_unlink=True)
        folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'opened-stairs.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
        for obj in built:obj.select_set(True)
        assert built,(site,'No actual structural aperture built')
        addition=ROOT/'build'/('stair-opening-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);retained=append(source,addition,folder/'opened-stairs.glb',remove_mesh_names=removed)
        oldparts=split_native(native);byname={n.split(':',1)[-1]:n for n,count,raw in oldparts};removed_native=[byname[n] for n in removed];kept=[part for part in oldparts if part[0] not in removed_native];parts=[(o.name,records(o)) for o in built];payload=struct.pack('>III',0x48435332,len(kept)+len(parts),sum(n for name,n,raw in kept)+sum(len(ts) for name,ts in parts))+b''.join(raw for name,n,raw in kept)
        for name,ts in parts:
            encoded=name.encode();payload+=struct.pack('>H',len(encoded))+encoded+struct.pack('>BI',1,len(ts))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in ts)
        (folder/'opened-stairs.hcs2').write_bytes(payload);bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'opened-stairs.glb'));expected={n:face_keys(ts) for n,ts in parts}
        for obj in bpy.context.scene.objects:
            if obj.name in expected:assert face_keys(records(obj))==expected.pop(obj.name),(site,obj.name,'export/native mismatch')
        assert not expected
        report=dict(id=site,status='PHYSICAL_STAIR_SHELL_APERTURE_CANDIDATE_NOT_RUNTIME_ACCEPTANCE',source_glb=source.relative_to(ROOT).as_posix(),source_glb_sha256=sha(source),source_physics=native.relative_to(ROOT).as_posix(),source_physics_sha256=sha(native),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),source_layout=r['source_layout'],source_layout_sha256=r['source_layout_sha256'],source_segment_map=r['source_segment_map'],stair_components=r['stair_components'],removed_source_meshes=removed,removed_native_parts=removed_native,new_parts=len(parts),new_triangles=sum(len(ts) for n,ts in parts),changes=changes,corridors=corridors,retained_glb_resources=retained,glb_sha256=sha(folder/'opened-stairs.glb'),physics_sha256=sha(folder/'opened-stairs.hcs2'),generator_sha256=sha(Path(__file__)),remaining='Independent source/solid/native and all25flat/stair comparisons; terrain/crossed flights, framing/guards, finishes and runtime integration remain open.')
        (folder/'construction.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print('PHYSICAL_STAIR_APERTURES',site,len(removed),len(parts),flush=True)
if __name__=='__main__':main()
