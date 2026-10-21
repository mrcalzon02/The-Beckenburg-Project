"""Preserve original bindings; apply authored artwork to the measured curved page UVs.
Run EvidenceBookArt first, then Blender --background --python this_file.
"""
import sys,json,hashlib
from pathlib import Path
import bpy
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(Path(__file__).resolve().parent))
import rebuild_medical_occult as M
import rebuild_support as S

S.reset();objects=[];records=[]
folder=S.DEV/'textures/evidence-books'
for tier in range(1,4):
    for style in ['blank','child_drawings','frantic_scrawls','ritual_circles']:
        kit=M.books(tier,'open');path=folder/f'tier-{tier}-{style}.png'
        mat=bpy.data.materials.new(f'Evidence book tier {tier} / {style} / original painted pages');mat.use_nodes=True
        image=bpy.data.images.load(str(path));image.pack()
        shader=mat.node_tree.nodes.get('Principled BSDF');shader.inputs['Roughness'].default_value=.93
        node=mat.node_tree.nodes.new('ShaderNodeTexImage');node.image=image;node.interpolation='Closest';mat.node_tree.links.new(node.outputs['Color'],shader.inputs['Base Color'])
        half=.175 if tier<3 else .198;depth=.112 if tier<3 else .127
        for obj in kit.parts:
            # Tier III's source has raised folded page regions above the added leaves.
            # Paint all page-facing source polygons too, using the same spread projection.
            page_slots={i for i,m in enumerate(obj.data.materials) if m and 'blank antique book paper' in m.name}
            obj.data.materials.append(mat);target=len(obj.data.materials)-1
            uv=obj.data.uv_layers.active
            for face in obj.data.polygons:
                if face.material_index not in page_slots:continue
                face.material_index=target
                for index in face.loop_indices:
                    v=obj.data.vertices[obj.data.loops[index].vertex_index].co
                    uv.data[index].uv=(.005+.99*(v.x+half)/(half*2),.005+.99*(v.y+depth)/(depth*2))
        state='open' if style=='blank' else 'open_'+style
        features=['Original binding, cover and source UVs preserved','Painted parchment follows curved leaves; no floating page overlay',
                  'Blank until a real authorized writing event; all three styles available at every tier; style is not a species verdict']
        record,obj=S.finish('GHOST_WRITING_BOOK',tier,kit.parts,state,features=features)
        record['page_art']=path.relative_to(ROOT).as_posix();record['page_art_sha256']=hashlib.sha256(path.read_bytes()).hexdigest()
        records.append(record);objects.append(obj)
for index,style in enumerate(['blank','child_drawings','frantic_scrawls','ritual_circles']):
    S.render_family('GHOST_WRITING_BOOK',[objects[tier*4+index] for tier in range(3)],path=S.DEV/'previews'/f'ghost_writing_book_{"open" if style=="blank" else style}.png',view=(.10,-1.2,2.7))
S.save_family('GHOST_WRITING_BOOK',objects)
(S.DEV/'evidence-book-pages.json').write_text(json.dumps(dict(assets=records,sources=list(S.SOURCE_LOG.values())),indent=2)+'\n')
assert len(records)==12 and all(r['texture_images'] for r in records)
print('EVIDENCE_BOOK_PAGES_COMPLETE',len(records),flush=True)
