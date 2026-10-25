"""Turn dependency snapshots into directly openable, isolated model scenes."""
import bpy,json,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def materialize(assets):
 for a in assets:
  path=ROOT/a['editable'];staging=ROOT/'build/earthen-source-staging';staging.mkdir(parents=True,exist_ok=True);snapshot=staging/(a['id']+'.blend');shutil.copyfile(path,snapshot)
  bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0
  with bpy.data.libraries.load(str(snapshot),link=False) as (src,dst):dst.objects=[a['id']]
  o=dst.objects[0];assert o is not None;bpy.context.scene.collection.objects.link(o);bpy.context.view_layer.objects.active=o;o.select_set(True);bpy.context.view_layer.update()
  bpy.ops.wm.save_as_mainfile(filepath=str(path),compress=True)
if __name__=='__main__':materialize(json.loads((ROOT/'development/levels/hollow-creek/grounded-berm.json').read_text(encoding='utf-8'))['assets'])
