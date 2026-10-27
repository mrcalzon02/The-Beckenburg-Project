"""Original roof-mounted suspension. Ceiling origin is Z0; fixture mount Z-1.4."""
import bpy,sys,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import build_lived_assets as b
import rebuild_support as s
m=b.init();parts=[]
for x in [-.42,.42]:
    parts.append(s.box('Roof attachment plate',(x,0,-.014),(.14,.16,.028),m['iron']))
    for y in [-.045,.045]:parts.append(s.rod('Attachment screw',(x,y,-.024),(x,y,-.038),.012,m['steel'],6))
    parts.append(s.rod('Threaded suspension drop',(x,0,-.025),(x,0,-1.355),.012,m['steel'],8))
    parts.append(s.rod('Suspension locking nut',(x,0,-1.335),(x,0,-1.36),.025,m['brass'],6))
parts.append(s.box('Fixture bearing crossbar',(0,0,-1.37),(1.08,.16,.06),m['iron']))
b.export('level_mill_suspension',parts,'mill-lighting',view=(3,-4,2))
entry=b.LOG[-1];entry.update(mount='Ceiling plane Y0; lower mounting face Y-1.40; never floor normalize',runtime='Static physical supports for existing room-circuit utility fixture')
(ROOT/'development/levels/mill-lighting-assets.json').write_text(json.dumps(dict(assets=[entry]),indent=2)+'\n')
