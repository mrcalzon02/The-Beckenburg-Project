"""Compose saved native terrain with preserved furnished exports for fit review.

Blender background. Current master maps and props remain untouched. These are
real terrain/structure collision candidates, not unlocked runtime locations.
"""
from pathlib import Path
import argparse,collections,hashlib,io,json,math,struct,sys
import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from export_roster_collision import native_faces,face_keys
BASE=ROOT/'development/levels/rebuild/terrain-overall-pass'
CAP=ROOT/'development/levels/rebuild/captures/macroterrain-batch'

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def records(obj):
    obj.data.calc_loop_triangles();result=[]
    for tri in obj.data.loop_triangles:
        vs=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices];normal=(vs[1]-vs[0]).cross(vs[2]-vs[0])
        flags=6
        if normal.z>0 and math.hypot(normal.x,normal.y)/normal.z<=.20+1e-5:flags|=1
        xyz=[v for p in vs for v in (p.x,p.z,-p.y)]
        xyz=struct.unpack('>9f',struct.pack('>9f',*xyz));result.append((xyz,flags))
    return result

def main():
    p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+',required=True);args=p.parse_args(sys.argv[sys.argv.index('--')+1:])
    roster={r['id']:r for r in json.loads((BASE/'terrain-envelopes.json').read_text())['sites']}
    for key in args.sites:
        key=key.upper();site=key.lower();world=BASE/'worlds'/site/'terrain-world';out=BASE/'assemblies'/site;out.mkdir(parents=True,exist_ok=True)
        source=ROOT/f'development/levels/rebuild/furnishings/{site}/furnished.glb';old_physics=source.with_name('furnished-physics.hcs2');layout=ROOT/roster[key]['layout'];data=json.loads(layout.read_text())
        preserved={str(p.relative_to(ROOT)):sha(p) for p in [source,old_physics,layout]}
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source))
        assert not any(o.type=='MESH' and 'terrain' in o.name.lower() for o in bpy.context.scene.objects),'Do not silently double existing terrain'
        points=[];faces=[]
        for chunk in sorted((world/'scene-package/meshes').glob('site*.obj')):
            offset=len(points)
            for row in chunk.read_text().splitlines():
                vs=row.split()
                if not vs:continue
                if vs[0]=='v':x,y,z=map(float,vs[1:4]);points.append((x,-z,y))
                elif vs[0]=='f':faces.append(tuple(offset+int(v.split('/')[0])-1 for v in vs[1:]))
        mesh=bpy.data.meshes.new('Persistent terrain triangles');mesh.from_pydata(points,[],faces);mesh.update()
        obj=bpy.data.objects.new('Saved editor macroterrain '+key,mesh);bpy.context.scene.collection.objects.link(obj);obj['role']='saved editor terrain / macroform candidate'
        for name,color in [('Weathered soil / terrain massing',(.28,.34,.21,1)),('Exposed rock shoulder / terrain massing',(.32,.29,.24,1)),('Low basin sediment / terrain massing',(.21,.23,.18,1))]:
            mat=bpy.data.materials.new(name);mat.diffuse_color=color;mat.use_nodes=True;mat.node_tree.nodes['Principled BSDF'].inputs['Base Color'].default_value=color;mat.node_tree.nodes['Principled BSDF'].inputs['Roughness'].default_value=.95;mesh.materials.append(mat)
        for poly in mesh.polygons:
            average=sum(mesh.vertices[i].co.z for i in poly.vertices)/len(poly.vertices)
            poly.material_index=1 if poly.normal.z<.93 else 2 if average<-.9 else 0
        uv=mesh.uv_layers.new(name='World metre UV')
        for poly in mesh.polygons:
            for li in poly.loop_indices:
                v=mesh.vertices[mesh.loops[li].vertex_index].co;uv.data[li].uv=(v.x/8,v.y/8)
        terrain_expected=native_faces(world/'terrain-physics.hcs2');assert face_keys(records(obj))==face_keys(terrain_expected),'Saved OBJ/render/native terrain mismatch'
        tree=BVHTree.FromPolygons(points,faces)
        fit=[]
        for link in data['links']:
            if link.get('player_access')=='SEALED_NO_ROUTE':continue
            for index,(p0,q) in enumerate(zip(link['points'],link['points'][1:])):
                n=max(1,math.ceil(math.dist(p0,q)/.5));ys=[];missing=0;above=0;below=0
                for j in range(n+1):
                    t=j/n;x=p0[0]+(q[0]-p0[0])*t;y=p0[1]+(q[1]-p0[1])*t;z=p0[2]+(q[2]-p0[2])*t
                    hit=tree.ray_cast(Vector((x,-z,50)),Vector((0,0,-1)))[0]
                    if hit is None:missing+=1;continue
                    delta=hit.z-y;ys.append(delta);above+=delta>.1;below+=delta<-.6
                if above or below or missing:fit.append(dict(link=link['id'],segment=index,samples=n+1,terrain_above_route_samples=above,terrain_below_route_samples=below,no_terrain_samples=missing,delta_range=[min(ys),max(ys)] if ys else None,interpretation='Terrain relationship review only; floors, tunnels and elevated bridges intentionally differ. Not a body movement test.'))
        bpy.ops.wm.save_as_mainfile(filepath=str(out/'terrain-assembly.blend'),compress=True)
        bpy.ops.export_scene.gltf(filepath=str(out/'terrain-assembly.glb'),export_format='GLB',export_extras=True)
        # Exact HCS2 union: preserve all existing part bytes/flags, append actual
        # terrain part bytes. Rendering is independently checked after reimport.
        old=old_physics.read_bytes();ground=(world/'terrain-physics.hcs2').read_bytes()
        om,op,ot=struct.unpack_from('>III',old);gm,gp,gt=struct.unpack_from('>III',ground);assert om==gm==0x48435332
        physics=struct.pack('>III',om,op+gp,ot+gt)+old[12:]+ground[12:];(out/'terrain-assembly.hcs2').write_bytes(physics)
        assert len(native_faces(out/'terrain-assembly.hcs2'))==ot+gt
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(out/'terrain-assembly.glb'))
        terrain=next(o for o in bpy.context.scene.objects if o.type=='MESH' and o.name.startswith('Saved editor macroterrain'))
        assert face_keys(records(terrain))==face_keys(terrain_expected),'Exported GLB no longer matches editor physics'
        scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.light='STUDIO';scene.display.shading.show_shadows=True;scene.display.shading.show_cavity=True
        scene.render.resolution_x=1200;scene.render.resolution_y=900;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.view_settings.view_transform='Standard'
        a,b,c,d=data['site_bounds'];span=max(c-a,d-b);center=Vector(((a+c)/2,-(b+d)/2,1));scene.world=bpy.data.worlds.new('Native terrain review')
        bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO';cam.data.ortho_scale=span*1.65;capture=[]
        for name,offset in [('whole-site',Vector((span*.8,-span*.95,span*1.05))),('terrain-plan',Vector((0,0,span*1.5)))]:
            for o in scene.objects:
                if o.type=='MESH':o.hide_render=name=='terrain-plan' and o!=terrain
            cam.location=center+offset;cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler()
            path=CAP/(site+'-'+name+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);capture.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=sha(path),type='native export composite' if name=='whole-site' else 'saved native terrain-only overhead',limitation='Terrain fit candidate; no canonical van/runtime/finished art acceptance'))
        for name,digest in preserved.items():assert sha(ROOT/name)==digest,'Master map changed'
        report=dict(id=key,status='NATIVE_MACROTERRAIN_ASSEMBLY_REVIEW_OPEN',preserved_sources=preserved,saved_terrain_triangles=gt,existing_triangles=ot,composite_triangles=ot+gt,exact_render_native_terrain_parity=True,original_collision_parts_byte_preserved=True,route_terrain_relationship_findings=fit,captures=capture,generator_sha256=sha(Path(__file__)),native_glb_sha256=sha(out/'terrain-assembly.glb'),native_physics_sha256=sha(out/'terrain-assembly.hcs2'),limitations='No master replacement or map unlock. Real foundations/retaining edges/guards, terrain-to-blockout reshuffles, actual van arrival and all runtime gates remain open.')
        (out/'review.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print('MACROTERRAIN_REVIEW',key,gt,'terrain faces',len(fit),'route relationship findings',flush=True)

if __name__=='__main__':main()
