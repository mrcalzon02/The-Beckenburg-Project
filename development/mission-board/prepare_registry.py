from pathlib import Path
import json, shutil, hashlib
SOURCE=Path('../The-Beckenburg-Project').resolve();TARGET=Path('.').resolve()
rows=[]; entries=[]
for path in sorted((SOURCE/'development/levels/layouts').glob('*/layout.json')):
    d=json.loads(path.read_text(encoding='utf-8'));folder=path.parent.name
    choices=[SOURCE/f'development/levels/blockouts/{folder}/preview.png', SOURCE/f'development/levels/previews/{folder}-blender-exterior.png']
    if folder=='wld-01':choices=[SOURCE/'development/levels/previews/blackpine-mill-expanded-blender-exterior.png']+choices
    if folder=='castle':choices=[SOURCE/'development/levels/previews/castle-surroundings-blender-exterior.png']+choices
    choices.append(path.parent/'plan.png');image=next(p for p in choices if p.exists())
    dest=TARGET/f'assets/runtime/mission-board/{folder}.png';dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(image,dest)
    caption='STATIC BLOCKOUT VIEW' if 'blockouts' in image.parts else 'STATIC SITE VIEW' if 'previews' in image.parts else 'STATIC LAYOUT PLACEHOLDER'
    data=[d['id'],d['title'],d['region'],d['scale'],d['intent'],folder,caption]
    entries.append('        new Entry('+','.join(json.dumps(s,ensure_ascii=True) for s in data)+')')
    rows.append(dict(id=d['id'],title=d['title'],folder=folder,region=d['region'],image=str(dest.relative_to(TARGET)).replace('\\','/'),image_sha256=hashlib.sha256(dest.read_bytes()).hexdigest(),source_image=str(image.relative_to(SOURCE)).replace('\\','/'),caption=caption))
java='''package org.beckenburg;
import java.util.*;
/** Lore names and stable image slots; shorthand codes stay internal protocol IDs. */
public final class MissionSites {
    private MissionSites(){}
    public record Entry(String id,String title,String region,String scale,String description,String imageKey,String imageCaption){
        public SiteCatalog.Location location(){try{return SiteCatalog.Location.valueOf(id);}catch(IllegalArgumentException planned){return null;}}
        public boolean headquarters(){return id.equals("CASTLE");}
        public boolean playable(){var site=location();return site!=null&&site.playable();}
        public String status(){return headquarters()?"HEADQUARTERS / NO INVESTIGATION":playable()?"AVAILABLE":"IN DEVELOPMENT / NOT DISPATCHABLE";}
        @Override public String toString(){return title;}
    }
    private static final List<Entry> ALL=List.of(
'''+',\n'.join(entries)+''');
    public static List<Entry> all(){return ALL;}
    public static Entry byId(String id){return ALL.stream().filter(e->e.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown site "+id));}
    public static String title(String id){return byId(id).title();}
}
'''
(TARGET/'src/main/java/org/beckenburg/MissionSites.java').write_text(java,encoding='utf-8')
(TARGET/'development/mission-board/sites.json').write_text(json.dumps(dict(sites=rows),indent=2)+'\n',encoding='utf-8')
print('23 lore names and unchanged static source images prepared')
