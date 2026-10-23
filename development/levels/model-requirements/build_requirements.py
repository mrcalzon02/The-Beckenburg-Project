"""Room-specific model worklists, backed by real layout rooms and local inventory.
Existing maps receive planning documentation only; no geometry is generated here.
"""
from pathlib import Path
import json,hashlib
ROOT=Path(__file__).resolve().parent;LEVELS=ROOT.parent;PROJECT=LEVELS.parents[1]
EXISTING={'HOUSE','FARMHOUSE','SCHOOL','CHURCH','WLD-01','CASTLE'}
SPECIAL={
'HOUSE':('Domestic timber/plaster, damp cellar','Family-photo frame variations; child toy cluster; fitted cellar service manifold; house address and mail set'),
'FARMHOUSE':('Farm timber, straw, soil and weathered iron','Barn stall and loft hay modules; farm gate and trough; root-cellar shelving and produce bins; family workwear and feed sacks'),
'SCHOOL':('Painted institutional timber, tile and old steel','Gym wall apparatus and scoreboard; assembly-stage curtain and lectern; school-specific noticeboard; classroom science demonstration equipment'),
'MANSION':('Carved timber, plaster, glass and cellar masonry','Grand stair balustrade and newel set; ballroom chandelier and curtain kit; greenhouse glazing and planting bench; wine racks and servants-call board'),
'PRISON':('Concrete, barred steel and institutional enamel','Cell door/frame and observation hatch; two-storey gallery railing and stair set; sally-port gate assembly; guard control desk and key board; isolation-room fitting set'),
'ASYLUM':('Hospital tile, enamel, timber and damp masonry','Hydrotherapy tub and service pipes; treatment theatre table and lamp; patient ward bedside kit; records trolley and chart board; remembrance chapel screen'),
'ACADEMY':('Cloister masonry, dark timber, brass and slate','Cloister arcade bays and buttresses; headmaster ceremonial desk; historical study/laboratory bench; upper dormitory partition and bed kit; archive folio cabinets'),
'COLLEGE':('Teaching concrete, glass, laminate and service steel','Tiered lecture seating and lectern; campus bridge glazing and supports; laboratory island and extraction hood; residence mailboxes and kitchenette; cafe counter and serving queue'),
'CHURCH':('Parish stone, timber, brass and worn textile','Organ console and pipe ranks; bell/beam/rope assembly; confessional partition and screens; chancel altar and lectern; crypt memorial slabs and niches'),
'CASTLE':('Castle stone, timber, iron and service additions','Separate training-bay installation sets; relic containment plinths and cages; quartermaster issue counter; map-office pinboard and dossiers; infirmary bed/screen kit; restoration scaffolding'),
'WLD-01':('Wet woodland timber, machine iron and dust','Circular headrig and aligned log carriage; stopped dust extractor; braced dry-stock rack; supported first-aid station; sawdust and timber-offcut scatter'),
'WLD-02':('Woodland cabins, canvas, enamel and shoreline timber','Abandoned camper shell and interior fittings; pitched and collapsed tent variants; campfire rings and grate; ranger trail sign family; bathhouse privacy and wash fixtures'),
'WLD-03':('Fictional dry canyon geology and modern expedition supplies','Modular cave bends and chamber transitions; canyon ledge/switchback supports; expedition shelter frames; survey markers and sample cases; rockfall barriers and acoustic alcoves'),
'WLD-04':('Exposed forest timber, stone and galvanized supports','Lookout tower structural bays and bracing; switchback stair and landing modules; glazed lookout cab; spotting table and optical instrument; wind-damaged ranger-station shell'),
'WLD-05':('Mine rock, braced timber, iron and mineral dust','Mine drift junction and support sets; ore cart and rail switches; assay bench and scales; collapsed lift barrier; ventilation fan/duct and escape-ladder reservations'),
'WLD-06':('Salt-worn timber, rope, nets and corroded fittings','Dock pile/bearer/deck modules; boathouse slip cradle; suspended net drying frames; fish sorting bench and crates; dry undercroft tide-line dressing'),
'WLD-07':('Weathered memorial stone, wrought iron and vegetation','Memorial marker families and readable plaques; ossuary niche and passage kit; ruined chapel arch fragments; cemetery gate and boundary railing; caretaker grave-maintenance tools'),
'WLD-08':('Insulated prefabs, frost, snow and utility steel','Sealed prefab wall/airlock modules; exterior instrument array; radio operations console; generator and fuel service skid; insulated tunnel joints and snow-drift barriers'),
'WLD-09':('Wet mass concrete, painted machinery and steel','Turbine/generator static assembly; control desk and mimic panel; spillway gate/actuator; crest guardrails and maintenance stairs; valve manifold and service-gallery pipe supports'),
'WLD-10':('Monastic masonry, timber, worn plaster and brass','Cloister arcade and garden well; bell frame/rope/toll mechanism; refectory serving hatch; monastic archive and writing desk; catacomb niche and stair kit'),
'WLD-11':('Water-worn stilt timber, reeds and rusted hardware','Stilt pile/bracing and boardwalk modules; flooded warehouse boundary and debris; ferry-stage mooring hardware; marsh chapel fittings; fishing-shack drying and storage kit'),
'WLD-12':('Rail steel, ballast, timber, glass and tunnel masonry','Distinct passenger/guard carriage shells and bogies; derailed coupler and wheelset; signal hut lever frame; trestle beams/guards; tunnel mouth and track-switch kit'),
'WLD-13':('Salt-corroded plant steel, wet tile and timber','Fish-processing conveyor and guards; can-seaming/packing static machine; cold-store insulated doors and racks; cannery boiler and process-pipe manifolds; dock loading and payroll clock station')}
# Reuse candidates are actual local files, not claims that a category is complete.
KITS={
'seating':(['dinner_table_1','chair_mp_5','level_camp_bench'],'chairs, tables and bench variants'),
'sleeping':(['level_trace_camp_bed','level_work_crew_footlocker','level_trace_bedroll'],'bed frames, bedding, personal storage and hanging clothing'),
'office':(['dinner_table_1','level_work_repair_bench','book_mp_2'],'desk/counter, task chair, filing/storage and record supports'),
'sanitary':(['level_bath_basin','level_bathtub','toilet_mp_1','level_trace_wash_trough'],'toilet/privacy units, basin/taps, drain and washing accessories'),
'kitchen':(['level_kitchen_counter','plate_mp_1','level_work_survivor_cookbox'],'food preparation, cold/hot appliance shells, serving and crockery'),
'library':(['shelf_mp_6','book_mp_2','level_trace_records'],'shelves, books/folios, reading table and supported records'),
'service':(['level_boiler_coal','level_boiler_oil','level_work_repair_bench'],'plant machinery, cable/pipe supports, tools and spares'),
'teaching':(['level_blackboard','level_student_desk','level_lockers'],'teaching surface, student seating, storage and demonstration props'),
'medical':(['level_work_safety_station','level_bath_basin'],'care bed/trolley, privacy screen, cabinet and medical containers'),
'worship':(['level_camp_bench','level_light_candle_chandelier_worn'],'seating, lectern/altar, safe candle fixtures and memorial objects'),
'circulation':(['light_switch_1','level_utility_light','level_domestic_light'],'mounted signs, switches, handrails, door furniture and light fittings'),
'storage':(['wooden_crate_1','shelf_mp_6','level_work_timber_rack'],'shelves, crates/bins, labels, pallets and restrained stored contents'),
'outdoor':(['level_camp_bench','level_woodland_boulder','level_woodland_stump','level_woodland_fern'],'terrain transitions, boundaries, route signs, local planting and supported weather debris')}
RULES=[('kitchen',['kitchen','scullery','cafe','mess','dining','refectory','common room']),('sleeping',['bedroom','dorm','bunk','lodging','accommodation','patient ward']),('sanitary',['bath','wash','hydro','mudroom']),('office',['office','reception','intake','admission','guard','radio','control','briefing','records']),('library',['library','archive','study','record','research']),('service',['boiler','plant','workshop','repair','generator','turbine','maintenance','tool','assay','mill']),('medical',['nurse','infirmary','medical','treatment','hydro','hospital']),('teaching',['classroom','lecture','seminar','assembly','laboratory','lab','training']),('worship',['chapel','church','chancel','nave','vestry','choir','crypt','memorial']),('storage',['store','storage','vault','shed','attic','cellar','pantry'])]
rows=[]
for path in sorted((LEVELS/'layouts').glob('*/layout.json')):
    d=json.loads(path.read_text());finish,unique=SPECIAL[d['id']];zones=[]
    for z in d['zones']:
        name=z['name'].lower();kits=['outdoor'] if z['kind'] in ['outside','staging'] else ['circulation']
        for kit,words in RULES:
            if any(w in name for w in words):kits.append(kit)
        if any(k in kits for k in ['kitchen','office','library','teaching','worship']):kits.append('seating')
        if len(kits)==1 and z['kind'] not in ['outside','staging']:kits.append('storage')
        kitrows=[]
        for k in dict.fromkeys(kits):
            candidates=[n for n in KITS[k][0] if (PROJECT/f'assets/runtime/models/{n}.glb').exists()]
            kitrows.append(dict(category=k,needed=KITS[k][1],existing_local_candidates=candidates,status='reuse candidate; dimensions, pivot, UVs and scene fit still require inspection'))
        zones.append(dict(id=z['id'],room=z['name'],kits=kitrows,clutter=['supported papers/labels appropriate to this room','small personal or work-use objects on real surfaces','waste/wear patches only where the room history supports them'],details=['threshold/trim joins and supporting brackets','local damp, dirt, fading or corrosion matching exposure','readable labels without assigning ghost identity']))
    record=dict(id=d['id'],title=d['title'],geometry_scope='existing map: documentation only' if d['id'] in EXISTING else 'new rough blockout: model planning',source_layout_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),finish_palette=finish,standard_architecture=['distinct wall/floor/ceiling kit','door leaves, frames, hinges and handles','window frames/glazing/sills','proper stairs, landings, deck openings and guards','mounted room lights, switches, breaker and supported service runs','separate van dock contract; reuse canonical van'],map_specific_models=[dict(model=x.strip(),status='needed or existing candidate: audit before authoring duplicates') for x in unique.split(';')],zones=zones,variants=['clean source plus separate aged skins where needed','damage/wear variations retaining believable supports','do not stretch a source prop into a different object'],implementation_order=['audit existing runtime and owner library','architecture and route supports','major furniture and fixtures','map-specific landmark machinery/props','supported clutter','wear, signage and story detail','collision, lighting and in-room visual review'],limitations=['Quantities and asset budgets await room dressing plans.','Existing candidates are verified filenames, not certified fit or finished availability.','This is a model worklist, not implemented gameplay.'])
    (ROOT/(path.parent.name+'.json')).write_text(json.dumps(record,indent=2)+'\n')
    lines=['# '+d['title']+' — model requirements','',record['geometry_scope']+'. No map rebuild is requested by this checklist.','','Finish direction: '+finish+'.','','## Standard architecture and fixtures','']+['- '+x for x in record['standard_architecture']]+['','## Map-specific models / one-off candidates','']+['- '+x['model']+' — inspect existing assets first.' for x in record['map_specific_models']]+['','## Room-by-room furniture, clutter and details','']
    for z in zones:
        lines+=['### '+z['room']+' (`'+z['id']+'`)','']
        for k in z['kits']:
            reuse=', '.join('`'+x+'`' for x in k['existing_local_candidates']) or 'no verified local candidate from the starter inventory'
            lines+=['- '+k['needed']+'. Reuse candidates: '+reuse+'.']
        lines+=['- Clutter: '+ '; '.join(z['clutter'])+'.','- Detail: '+ '; '.join(z['details'])+'.','']
    lines+=['## Refinement order','']+['- '+x for x in record['implementation_order']]+['','No quantities are invented. Audit source dimensions, pivots, UVs, licensing, supports and clearance before reuse. Clean/worn variants are separate assets. Investigator equipment comes from the existing loadout system.']
    (ROOT/(path.parent.name+'.md')).write_text('\n'.join(lines)+'\n');rows.append(dict(id=d['id'],title=d['title'],folder=path.parent.name,rooms=len(zones),map_specific_candidates=len(record['map_specific_models'])))
(ROOT/'catalog.json').write_text(json.dumps(dict(maps=rows,existing_map_geometry_untouched=True),indent=2)+'\n')
(ROOT/'README.md').write_text('# Model requirements for all planned maps\n\nPlanning checklists for all 23 maps: standard furniture/fixtures, reusable local candidates, map-specific models, room clutter and finish/detail work. Existing maps receive documentation only. Geometry is not rebuilt.\n\n'+ '\n'.join('- ['+r['title']+']('+r['folder']+'.md) · [structured JSON]('+r['folder']+'.json)' for r in rows)+'\n\nGenerated from measured layouts and actual local model filenames. Quantities, new-asset budgets and scene acceptance remain for the implementation pass.\n')
assert len(rows)==23
print('MODEL_REQUIREMENTS_COMPLETE',len(rows),'maps;',sum(r['rooms'] for r in rows),'room/zone checklists;',sum(r['map_specific_candidates'] for r in rows),'map-specific candidates',flush=True)
