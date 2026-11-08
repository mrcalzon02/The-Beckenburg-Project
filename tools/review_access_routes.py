"""Blender export-backed route source review; overlays are authoring guides.
Run headlessly. Physical geometry/UV/pivots are copied byte for byte, not rebuilt.
"""
from pathlib import Path
import datetime,hashlib,json,math,sys
import bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'development/levels/rebuild/access-route-pass'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def point(p):return Vector((p[0],-p[2],p[1]))
def material(name,colour):
 m=bpy.data.materials.new(name);m.diffuse_color=(*colour,1);return m
def route(points,mat,name):
 curve=bpy.data.curves.new(name,'CURVE');curve.dimensions='3D';curve.bevel_depth=.055;curve.bevel_resolution=0
 line=curve.splines.new('POLY');line.points.add(len(points)-1)
 for dst,p in zip(line.points,points):dst.co=(*point([p[0],p[1]+.08,p[2]]),1)
 obj=bpy.data.objects.new(name,curve);bpy.context.collection.objects.link(obj);obj.data.materials.append(mat)
 for p in [points[0],points[-1]]:
  bpy.ops.mesh.primitive_uv_sphere_add(segments=8,ring_count=4,radius=.15,location=point([p[0],p[1]+.1,p[2]]));bpy.context.object.data.materials.append(mat)
 return obj
def main():
 prior=json.loads((OUT/'verification/primary-floor-captures.json').read_text());already={(r['site'],r['floor_y']) for r in prior['captures']};captures=[];run=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');cap=ROOT/'development/levels/rebuild/captures/access-route-batch'/run;cap.mkdir(parents=True)
 roster=json.loads((OUT/'roster.json').read_text())
 for source in roster['active_sources']:
  contract=ROOT/source['construction'];meta=json.loads(contract.read_text())
  if 'access_route_changes' not in meta:continue
  site=source['id'];glb=ROOT/source['glb'];assert sha(glb)==source['glb_sha256']==meta['source_glb_sha256'];assert sha(ROOT/source['native'])==meta['source_physics_sha256']
  changes=meta['access_route_changes'];levels=sorted({p['after'][0][1] for p in changes});data=json.loads((ROOT/source['layout']).read_text())
  for y in levels:
   if (site,y) in already:continue
   selected=[p for p in changes if p['after'][0][1]==y];focus=max(selected,key=lambda p:sum(math.dist(a,b) for a,b in zip(p['before'],p['before'][1:])))
   for phase,colour in [('before',(1,.3,.04)),('after',(.03,.85,1))]:
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(glb));scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.light='STUDIO';scene.display.shading.show_shadows=True;scene.display.shading.show_cavity=True;scene.render.resolution_x=1100;scene.render.resolution_y=850;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard';scene.world=bpy.data.worlds.new('Neutral source review');scene.world.color=(.04,.04,.04)
    hidden=[]
    for obj in list(scene.objects):
     if obj.type!='MESH':continue
     heights=[(obj.matrix_world@Vector(v)).z for v in obj.bound_box]
     if min(heights)>y+.2 or max(heights)<y-.3:obj.hide_render=True;hidden.append(obj.name)
    mat=material('Authoring guide only: '+phase,colour)
    for p in selected:route(p[phase],mat,'GUIDE '+p['link'])
    bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO';cam.data.clip_start=.01;cam.data.clip_end=1000
    for view in ['floor-plan','route-detail']:
     if view=='floor-plan':a,b,c,d=data['site_bounds']
     else:
      ps=focus['before']+focus['after'];a=min(p[0] for p in ps)-2;b=min(p[2] for p in ps)-2;c=max(p[0] for p in ps)+2;d=max(p[2] for p in ps)+2
     centre=point([(a+c)/2,y,(b+d)/2]);cam.location=centre+Vector((0,0,120));cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=max(c-a,(d-b)*1100/850)*1.08
     path=cap/(site.lower()+'-y'+str(y).replace('.','p').replace('-','minus')+'-'+view+'-'+phase+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);captures.append(dict(site=site,phase=phase,view=view,path=path.relative_to(ROOT).as_posix(),sha256=sha(path),source_glb=source['glb'],source_glb_sha256=sha(glb),layout=meta['before_layout'] if phase=='before' else source['layout'],layout_sha256=sha(ROOT/(meta['before_layout'] if phase=='before' else source['layout'])),floor_y=y,shown_links=[p['link'] for p in selected],focus_link=focus['link'],hidden_objects=hidden,type='Native exported geometry top-down floor cutaway with coloured route authoring guides; upper/lower layers hidden; not runtime/arrival/landscape acceptance'))
   print('ACCESS_ROUTE_CAPTURE',site,len(selected),'links on floor',y,flush=True)
 report=dict(status='NATIVE_EXPORTED_GEOMETRY_AND_AUTHORED_ROUTE_GUIDES',captures=prior['captures']+captures,prior_checker_source=prior['checker_source'],prior_checker_sha256=prior['checker_sha256'],checker_source=Path(__file__).relative_to(ROOT).as_posix(),checker_sha256=sha(Path(__file__)))
 (OUT/'verification/captures.json').write_bytes((json.dumps(report,indent=2)+'\n').encode())
if __name__=='__main__':main()
