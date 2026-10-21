"""Apply the shared authoring metadata to existing generated scenes; no geometry edits."""
import bpy,json,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'development/levels'))
import scene_metadata
for name in ['house','farmhouse','school','arrival-road','enclosure-suburban','enclosure-woodland','enclosure-urban','enclosure-mountain','castle-surroundings']:
 path=ROOT/f'development/levels/blender/{name}-complete.blend';definition=json.loads((ROOT/f'build/level-verification/scenes/{name}/definition.json').read_text())
 bpy.ops.wm.open_mainfile(filepath=str(path));bpy.context.preferences.filepaths.save_version=0;scene_metadata.apply(bpy.context.scene,definition);bpy.ops.wm.save_as_mainfile(filepath=str(path),compress=True);print('SCENE_METADATA',name,flush=True)
