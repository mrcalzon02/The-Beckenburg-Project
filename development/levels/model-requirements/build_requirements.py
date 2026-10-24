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
def regenerate_legacy_worklists():
    raise RuntimeError("Legacy full regeneration is disabled: preserve curated worklists; edit a selected packet explicitly.")

def main():
    from refresh_catalog import main as refresh
    refresh()

if __name__=='__main__':main()
