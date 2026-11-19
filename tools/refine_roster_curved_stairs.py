"""Measured mitered tread solids on existing authored curved stair paths.

Background Blender candidate refinement; retain original objects/pivots and source
layouts. Native/render/collision use the same authored polygon, never a hidden ramp.
"""
from pathlib import Path
import argparse,hashlib,json,math,subprocess,sys
import bpy,bmesh
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'development/levels/blockouts'))
from structural_pass import inspect_export
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--site',required=True);args=parser.parse_args(sys.argv[sys.argv.index('--')+1:])
    folder=ROOT/f'development/levels/rebuild/structures/{args.site.lower()}'
    report=json.loads((folder/'implementation.json').read_text(encoding='utf-8'));layout=ROOT/report['authoritative_input'];data=json.loads(layout.read_text(encoding='utf-8'))
    links=[link for link in data['links'] if link['kind']=='stairs' and len(link['points'])>10 and link.get('player_access')!='SEALED_NO_ROUTE']
    assert links,'No authored curved stair path'
    previous={name:sha(folder/name) for name in ['rough-shell.blend','rough-shell.glb']}
    assert previous['rough-shell.glb']==report['structural_review']['glb_sha256']
    assert previous['rough-shell.blend']==report['blend_sha256']
    source_ref=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    bpy.ops.wm.open_mainfile(filepath=str(folder/'rough-shell.blend'))
    objects=list(bpy.context.scene.objects);revised=[]
    for link in links:
        points=[link['points'][0]]
        for p,q in zip(link['points'],link['points'][1:]):
            if abs(q[1]-p[1])<.01:break
            points.append(q)
        assert len(points)>10,'A curved flight needs its explicit rising polyline'
        dirs=[]
        for p,q in zip(points,points[1:]):
            dx,dz=q[0]-p[0],q[2]-p[2];run=math.hypot(dx,dz)
            assert run>.01 and 0<q[1]-p[1]<=.18+1e-6,'Only ascending one-tread curve segments in this revision'
            dirs.append((dx/run,dz/run))
        offsets=[];half=link['width']/2
        cyclic=math.hypot(points[-1][0]-points[0][0],points[-1][2]-points[0][2])<.001
        for i in range(len(points)):
            before=dirs[max(0,i-1)];after=dirs[min(i,len(dirs)-1)]
            if cyclic and i in [0,len(points)-1]:before,after=dirs[-1],dirs[0]
            den=1+before[0]*after[0]+before[1]*after[1];assert den>1,'Reject sharp switchback; author a real landing'
            offsets.append((-(before[1]+after[1])/den*half,(before[0]+after[0])/den*half))
        for i,(p,q) in enumerate(zip(points,points[1:])):
            obj=next(o for o in objects if o.name==f'{link["id"]} flight{i} step1')
            assert obj.get('role')=='structural-stair-step' and obj['link']==link['id']
            corners=[(p[0]+offsets[i][0],p[2]+offsets[i][1]),(q[0]+offsets[i+1][0],q[2]+offsets[i+1][1]),(q[0]-offsets[i+1][0],q[2]-offsets[i+1][1]),(p[0]-offsets[i][0],p[2]-offsets[i][1])]
            inv=obj.matrix_world.inverted();vertices=[inv@Vector((x,-z,y)) for y in [p[1]-.16,q[1]] for x,z in corners]
            faces=[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]
            mesh=bpy.data.meshes.new(obj.name+' mitered authored solid');mesh.from_pydata(vertices,[],faces);mesh.update()
            for mat in obj.data.materials:mesh.materials.append(mat)
            # Simple plane-projected UVs retain physical scale on this rough stair
            # material. No source furnishing UV or model master is changed.
            uv=mesh.uv_layers.new(name='Rough construction UV')
            for face in mesh.polygons:
                for loop in face.loop_indices:
                    point=obj.matrix_world@mesh.vertices[mesh.loops[loop].vertex_index].co
                    uv.data[loop].uv=(point.x,point.y if abs(face.normal.z)>.5 else point.z)
            bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(mesh);bm.free();mesh.update()
            obj.data=mesh;obj['curved_tread']='Shared miter boundaries; no rectangular inner overlap or outer gap'
            revised.append(obj.name)
    bpy.ops.wm.save_as_mainfile(filepath=str(folder/'rough-shell.blend'),compress=True)
    bpy.ops.object.select_all(action='DESELECT')
    for obj in bpy.context.scene.objects:obj.select_set(obj.type=='MESH' and obj.get('role') not in ['ritual clear-area reservation','placeholder envelope; not replacement van'])
    bpy.ops.export_scene.gltf(filepath=str(folder/'rough-shell.glb'),export_format='GLB',export_extras=True,use_selection=True)
    sealed=[link for link in data['links'] if link.get('player_access')=='SEALED_NO_ROUTE']
    report['structural_review']=inspect_export(folder,data,sealed)
    report['blend_sha256']=sha(folder/'rough-shell.blend')
    report['curved_stair_revision']={'source_ref':source_ref,'previous':previous,'previous_input_origin':'Local structural generator output on the current authoritative layout; source_ref identifies repository context, not asserted byte identity of the generated input','builder_sha256':sha(Path(__file__)),'treads':len(revised),'components':revised,'method':'Mitered continuous ribbon with matched cyclic end boundaries; authored elevations/clear width and original object transforms preserved','limitation':'Actual complete native sweeps, guards, end landing and full source integration still gate acceptance.'}
    (folder/'implementation.json').write_bytes((json.dumps(report,indent=2)+'\n').encode())
    print('REVISED_CURVED_STAIRS',args.site,len(revised),'closed authored tread solids',flush=True)

if __name__=='__main__':main()
