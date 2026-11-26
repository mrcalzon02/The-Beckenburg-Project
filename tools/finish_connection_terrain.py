"""Compose actual saved battery terrain and close its approach as one net candidate."""
from pathlib import Path
import json,sys
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import compose_stair_terrain as terrain
import build_stair_bays as builder
from build_stair_connections import PLANS
from compose_room_terrain import split_native
from review_roster_macroterrain import sha
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'stair-connection-pass'
def main():
 terrain.OUT=OUT/'terrain';terrain.main(['WLD-17'])
 folder=OUT/'wld-17';soil=OUT/'terrain/assemblies/wld-17';comp=json.loads((soil/'construction.json').read_text());builder.OUT=OUT;builder.build('WLD-17',PLANS['WLD-17'],[(soil,'benched-terrain')],'connected-stairs')
 p=folder/'construction.json';landing=json.loads(p.read_text());r=dict(landing);prior=json.loads((ROOT/comp['source_glb']).with_name('construction.json').read_text());old={n:(count,raw) for n,count,raw in split_native(ROOT/comp['source_physics'])};new={n:(count,raw) for n,count,raw in split_native(folder/'connected-stairs.hcs2')};removed=set(comp['removed_native_parts'])|set(landing['removed_native_parts']);added=set(new)-(set(old)-removed)
 for name in set(old)-removed:assert old[name]==new[name],name
 r.update(status='SAVED_EDITOR_TERRAIN_AND_PHYSICAL_APPROACH_CANDIDATE_NOT_ACCEPTED',source_glb=comp['source_glb'],source_glb_sha256=comp['source_glb_sha256'],source_physics=comp['source_physics'],source_physics_sha256=comp['source_physics_sha256'],before_layout=comp['before_layout'],before_layout_sha256=comp['before_layout_sha256'],before_segment_map=comp['before_segment_map'],before_stair_components=comp['before_stair_components'],removed_source_meshes=comp['removed_source_meshes']+landing['removed_source_meshes'],removed_native_parts=sorted(removed),new_parts=len(added),new_triangles=sum(new[n][0] for n in added),terrain_mesh=comp['terrain_mesh'],native_terrain_parts=[n for n,count,raw in split_native(ROOT/comp['world']/'terrain-physics.hcs2')],world=comp['world'],world_files_sha256=comp['world_files_sha256'],terrain_pipeline=dict(terrain_composite=comp,landing_assembly=landing),net_composer_sha256=sha(Path(__file__)))
 assert set(r['removed_native_parts'])==set(comp['removed_native_parts'])|set(landing['removed_native_parts'])
 p.write_bytes((json.dumps(r,indent=2)+'\n').encode())
 print('NET_BATTERY_TERRAIN_APPROACH',len(added),r['new_triangles'],flush=True)
if __name__=='__main__':main()
