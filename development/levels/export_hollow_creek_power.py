"""Actual shed panel, supported generator control and string-globe coordinates."""
import bpy,json,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';r=json.loads((OUT/'green-grass.json').read_text(encoding='utf-8'));assert hashlib.sha256((ROOT/r['scene']).read_bytes()).hexdigest()==r['scene_sha256'];bpy.ops.wm.open_mainfile(filepath=str(ROOT/r['scene']))
def points(o,prefix):
 ids={g.index for g in o.vertex_groups if g.name.startswith(prefix)};v=[o.matrix_world@v.co for v in o.data.vertices if any(a.group in ids and a.weight>.5 for a in v.groups)];assert v,(o.name,prefix);return v
def bounds(v):return [[min(p[i] for p in v),max(p[i] for p in v)] for i in range(3)]
shed=bpy.data.objects['Dock boat-service shed'];panel=bounds(points(shed,'External reachable distribution board'));handle=bounds(points(shed,'Main breaker handle'));panel_face=[handle[0][1]+.005,sum(panel[2])/2,-sum(panel[1])/2]
generator=bpy.data.objects['Dock reused generator'];generator.data.calc_loop_triangles();tree=BVHTree.FromPolygons([generator.matrix_world@v.co for v in generator.data.vertices],[tuple(t.vertices) for t in generator.data.loop_triangles],all_triangles=True);hit,normal,_,_=tree.ray_cast(Vector((26.65,6,-.35)),Vector((0,1,0)),3);assert hit is not None and normal.y<-.9,'No continuous generator front housing to support control';starter_surface=[hit.x,hit.z,-hit.y+.002]
utilities=bpy.data.objects['level_camp_dock_utilities'];lights=[]
for g in utilities.vertex_groups:
 if not g.name.startswith('Non-emissive review lamp globe'):continue
 p=bounds(points(utilities,g.name));lights.append([sum(p[0])/2,p[2][0]-.02,-sum(p[1])/2])
assert len(lights)==10,'Actual HC-DOCK-01 loads changed'
def nums(v):return ','.join(repr(float(n)) for n in v)
java='''package org.beckenburg;
import java.util.*;
import org.beckenburg.LevelDefinition.*;
/** Generated from actual native supports; HC-DOCK-01, not a replacement grid. */
public final class HollowCreekPower {
 public static final String SOURCE_SHA="SHA";
 public static final Vec PANEL=new Vec(PANEL);
 public static final Vec STARTER_SUPPORT=new Vec(STARTER);
 public static final double[][] LAMPS={LAMPS};
 public static void install(List<Socket> sockets){
  if(!HollowCreekScene.sourceSha().equals(SOURCE_SHA))throw new IllegalStateException("Regenerate actual native power supports");
  sockets.add(new Socket("hc-main-breaker","breaker",9,PANEL.x(),PANEL.y(),PANEL.z(),Math.PI/2,"HC-DOCK-01"));
  var b=VanInteriorLayout.bounds("button_12");
  sockets.add(new Socket("hc-generator","generator",9,STARTER_SUPPORT.x()-.15*(b.x0()+b.x1()),STARTER_SUPPORT.y()-.15*(b.y0()+b.y1()),STARTER_SUPPORT.z()-.30*b.z0(),0,"diesel-source-starter"));
  int i=0;for(var p:LAMPS)sockets.add(new Socket("hc-dock-light-"+i++,"light",9,p[0],p[1],p[2],0,"native-string"));
 }
 private HollowCreekPower(){}
}
'''.replace('"SHA"','"'+r['scene_sha256']+'"').replace('new Vec(PANEL)','new Vec('+nums(panel_face)+')').replace('new Vec(STARTER)','new Vec('+nums(starter_surface)+')').replace('{LAMPS}','{'+','.join('{'+nums(p)+'}' for p in lights)+'}')
(ROOT/'src/main/java/org/beckenburg/HollowCreekPower.java').write_text(java,encoding='utf-8')
report=dict(scene=r['scene'],source_sha256=r['scene_sha256'],source='Dock reused generator',circuit='HC-DOCK-01',load_count=10,lamps=lights,panel_face=panel_face,actual_panel_bounds_native=panel,starter_support=starter_surface,starter_support_normal=list(normal),source_running_and_breaker_are_separate=True,host_gameplay_validation_pending=True)
(OUT/'runtime-power.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('EXPORTED actual board/control bearing +10 string emitters',flush=True)
