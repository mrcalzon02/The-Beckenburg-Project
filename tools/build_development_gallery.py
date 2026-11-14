"""Index committed development captures without copying or altering originals.
Run before each map/model checkpoint; --check is the CI stale-index gate.
"""
from pathlib import Path
import argparse, hashlib, html, json, struct, subprocess
from urllib.parse import quote
ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');parser.add_argument('--tracked-only',action='store_true',help='Index the staged checkpoint without unrelated untracked captures');args=parser.parse_args()
paths=subprocess.check_output(['git','ls-files','--cached',* ([] if args.tracked_only else ['--others','--exclude-standard']),'-z','development'],cwd=ROOT).decode('utf-8').split('\0')
items=[]
for path in sorted(set(paths)):
    if not path.lower().endswith('.png'):continue
    if not any(t in path for t in ['/previews/','/captures/','/screenshots/','/blockouts/','/rebuild/structures/']):continue
    p=ROOT/path
    if not p.exists():continue
    data=p.read_bytes()
    if data[:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('Invalid PNG: '+path)
    w,h=struct.unpack('>II',data[16:24])
    kind='Development review'
    if '/site-earthworks-batch/' in path:kind='Native actual saved editor geographic terrain sculpt; roof cutaway, rough materials, landscape/runtime acceptance open'
    elif '/courts-and-docks-batch/' in path:kind='Native area-built courtyards and piled dock construction; roofs hidden for review, terrain/finish/runtime gates open'
    elif '/study-solid-removal-batch/' in path:kind='Native correction after removing mistaken physical study lines; incomplete terrain/thresholds exposed, roofs hidden, NOT accepted construction'
    elif '/circulation-construction-batch/' in path:kind='Native exported piled perimeter walk, aligned service aisle or housed mezzanine stair; upper roof context hidden, no runtime acceptance'
    elif '/entrance-construction-batch/' in path:kind='Native exported entrance/courtyard construction cutaway; pitched roofs hidden for inspection, unresolved other routes, no runtime acceptance'
    elif '/retaining-shoulder-batch/' in path:kind='Rejected native Cinder retaining-stub study; prior source retained' if '/rejected-' in path else 'Native retaining-shoulder construction; isolated views hide terrain/context, no runtime acceptance'
    elif '/stair-end-batch/' in path:kind='Native widened stair and quay/undercroft construction; cutaway context hidden, incomplete guard returns, not runtime acceptance'
    elif '/stair-guard-batch/' in path:kind='Native fitted library stair guards/stringers; cutaway context hidden, incomplete ends, not runtime acceptance'
    elif '/orthogonal-stair-batch/' in path:kind='Native physical stair construction cutaway; context hidden, no runtime acceptance' if 'cutaway' in path else 'Native physical stair construction context; no runtime acceptance'
    elif '/atmosphere-all-levels/' in path:kind='Runtime shared fog and skybox comparison' if 'sheet' in p.name else 'Runtime OpenGL atmosphere capture' if '/opengl/' in path else 'Runtime software atmosphere capture'
    elif '/roof-construction-batch/' in path:kind='Rejected roof wing-selection study; superseded source, not runtime' if '/rejected-' in path else 'Native exported enclosed roof construction; rough source, no gameplay integration acceptance'
    elif '/access-route-batch/' in path:kind='Rejected partial native route-review selector study; not final review' if '/rejected-' in path else 'Native exported floor cutaway with coloured route authoring guides; upper/lower layers hidden, no runtime/arrival acceptance'
    elif '/flat-terrain-batch/' in path:kind='Rejected native circulation/terrain construction study' if '/rejected-' in path else 'Native frozen three-site terrain pilot; not runtime' if '/pilot-archive/' in path else 'Actual saved editor terrain inspection; not runtime' if '-saved-terrain' in p.name else 'Native paired terrain/whole-site/contact review; rough source, no canonical van/runtime acceptance'
    elif '/arrival-bearing-batch/' in path:kind='Rejected native arrival bearing; terrain intrusion unresolved' if '/rejected-' in path else 'Native paired arrival end-bearing construction; no canonical van/runtime acceptance'
    elif '/flat-circulation-batch/' in path:kind='Native rejected flat-circulation study; not final source' if 'rejected-' in path else 'Native isolated actual exported wall profile; all other context hidden' if '-profile-' in path else 'Native paired intact flat-threshold inspection, neutral150Wfill; not runtime' if '-threshold-' in path else 'Native flat circulation architectural context/contact sheet; no arrival/complete-join/runtime acceptance'
    elif '/stair-connection-batch/' in path:kind='Native rejected connection pilot; not final candidate' if 'rejected-' in path else 'Native isolated stair hub; all other context hidden' if 'isolated-' in path else 'Actual saved editor terrain diagnostic; not runtime' if 'saved-terrain' in path else 'Native paired intact connection/landing inspection, neutral150Wfill; not runtime' if '-before.' in path or '-after.' in path else 'Native connection construction context/contact sheet; not arrival/runtime acceptance'
    elif '/stair-terrain-batch/' in path:kind='Native rejected terrain pilot, not final source' if '/rejected-' in path else 'Native isolated soil-bearing construction; other site context hidden' if 'soil-bearing' in path else 'Actual saved editor terrain topology diagnostic; not runtime' if 'saved-terrain' in path else 'Native paired intact terrain/threshold inspection, neutral150Wfill; not runtime' if '-stair-' in path or '-threshold-' in path else 'Native terrain construction overview/contact sheet; no arrival/runtime acceptance'
    elif '/stair-bay-batch/' in path:kind='Native superseded bay pilot; not final source' if 'superseded-' in path else 'Native stair bay cutaway; walls/terrain/higher floors hidden, not runtime' if 'cutaway' in path else 'Native authored stair bay construction inspection; not runtime'
    elif '/stair-opening-batch/' in path:kind='Native rejected pilot aperture review; not final source' if 'pilot-before-' in path else 'Native physical stair aperture inspection; construction cameras, not runtime'
    elif '/stair-landing-fit-batch/' in path:kind='Native stair closeup; overhead caps/distant context hidden, some joins occluded; not runtime' if 'closeup' in p.name else 'Native isolated stair additions; context hidden, not runtime' if 'new-landings' in p.name else 'Native stair/landing construction overview; runtime gates open'
    elif '/floor-closure-batch/' in path:kind='Native paired structural plate inspection;150W neutral fill, not runtime' if '-room-' in p.name or 'paired-closure' in p.name else 'Native isolated floor/cap additions; zero-part outcomes may be empty' if 'repaired-plates' in p.name else 'Native floor/cap construction overview or comparison; runtime gates open'
    elif '/room-terrain-batch/' in path:kind='Native first-person paired occupied-terrain review; neutral150W fill, not runtime' if '-room-' in p.name or 'paired-room' in p.name else 'Native saved editor occupied-room excavation review; construction candidate'
    elif '/deck-fit-batch/' in path:kind='Native physical exterior deck construction review; runtime acceptance pending'
    elif '/terrain-fit-batch/' in path:kind='Rejected native support placement study' if p.name.startswith('rejected-') else 'Native terrain and bearing construction review; runtime acceptance pending'
    elif '/macroterrain-batch/' in path:kind='Native saved editor terrain topology; candidate, not runtime' if p.name.endswith('-saved-terrain.png') else 'Native macroterrain-only inspection; candidate, not runtime' if '-terrain-plan' in p.name else 'Native macroterrain and preserved structure composite; fit review open'
    elif p.name.endswith('-saved-terrain.png'):kind='Native saved editor terrain inspection; punched physical geometry'
    elif '/basement-batch/' in path:kind='Native first-person export inspection; neutral review illumination, not runtime'
    elif '/landing-batch/' in path:kind='Native stair landing comparison; rejected and candidate geometry' if '-actual-export-contact' in p.name else 'Rejected native stair landing geometry' if 'rejected-' in p.name else 'Native stair landing construction candidate; runtime integration pending'
    elif '/stair-movement-batch/' in path:kind='Rejected native curved-stair geometry' if 'rejected-' in p.name else 'Native curved-stair construction candidate; runtime acceptance pending'
    elif '/furnishing-batch/' in path:kind='Rejected native rough-prop placement study' if '/rejected-' in path else 'Native staged rough-model inspection' if p.name=='staged-source-inspection.png' else 'Native map rough-prop placement; refinement pending' if '-export-overview' in p.name or '-room-' in p.name or 'rough-allocation-sheet-' in p.name else 'Native model-library sample inspection'
    elif '/synthetic-controls/' in path:kind='Synthetic GPU depth control'
    elif '/structure-batch/' in path:kind='Native structural blocking review; runtime integration pending'
    elif '/rebuild/structures/' in path:kind='Native planning-scene preview' if p.name=='preview.png' else 'Reimported native structure export review'
    elif '/synthetic-controls/' in path:kind='Synthetic GPU depth control'
    elif '/runtime-gpu/' in path:kind='Runtime GPU capture'
    elif '/hunter-reactions-' in path and '/raster/' in path:kind='Synthetic floor control'

    elif '/hunter-gait-recovery-software/' in path:kind='Runtime software hunter gait diagnostic'
    elif '/house-fresh-default/' in path:kind='Runtime fresh default House editor and CLI verification'
    elif '/hiding-energy/' in path:kind='Runtime hiding-location energy material'
    elif '/house-final-lighting/' in path:kind='Runtime matched House door and circuit lighting review'
    elif '/house-functional-fill/' in path:kind='Rejected runtime inspection' if p.stem.startswith('rejected-') else 'Native geometry overview' if p.stem.startswith('native-') else 'Runtime daily household and garden functions'
    elif '/house-template-compatibility/' in path:kind='Rejected runtime layout inspection' if p.stem.startswith('rejected-') else 'Runtime native editor custom House compatibility'
    elif '/house-native-migration/' in path:kind='Runtime saved native House migration'
    elif '/house-native-recovery/' in path:kind='Runtime legacy House editor recovery control'
    elif '/house-encounter/' in path:kind='Runtime authored House encounter study'
    elif '/house-room-review/' in path:kind='Runtime four-direction contact sheet' if p.stem=='four-way' else 'Runtime whole-House visual review'
    elif '/house-heating-water/' in path:kind='Rejected runtime inspection' if p.stem.startswith('rejected-') else 'Native geometry overview' if p.stem.startswith('native-') else 'Runtime household heating and water construction'
    elif '/house-story/' in path:kind='Rejected runtime inspection' if p.stem.startswith('rejected-') else 'Native geometry overview' if p.stem.startswith('native-') else 'Runtime household story and source wiring'
    elif '/house-fitout/' in path:kind='Rejected runtime inspection' if p.stem.startswith('rejected-') else 'Native geometry overview' if p.stem.startswith('native-') else 'Runtime household service fit-out'
    elif '/house-services/' in path:kind='Runtime electrical control and lighting verification'
    elif '/house-functions/' in path:kind='Native geometry overview' if p.stem.startswith('native-') else 'Runtime household construction checkpoint'
    elif '/house-sanctuary/' in path:kind='Runtime rebuilt-House sanctuary'
    elif '/house-envelope/' in path:kind='Rejected runtime inspection' if p.stem.startswith('rejected-') else 'Native texture diagnostic' if 'van-texture' in p.stem else 'Native geometry overview' if p.stem.startswith('native-') else 'Runtime dwelling construction'
    elif '/house-landscape/' in path:kind='Rejected runtime inspection' if p.stem.startswith('rejected-') else 'Native geometry overview' if p.stem.startswith('native-') else 'Runtime terrain construction'
    elif '/house-fixture-fit/' in path:kind='Runtime construction fixture fit'
    elif '/castle-loose-placements/' in path:kind='Runtime capture'
    elif '/castle-loose-ui/' in path:kind='Game UI verification'
    elif '/castle-window-frame/' in path:kind='Runtime lighting diagnostic'
    elif '/castle-story-ui/' in path:kind='Game UI verification'
    elif '/castle-training-ui/' in path:kind='Game UI verification'
    elif '/ci-ui/' in path:kind='Game UI verification'
    elif '/performance/hardware/' in path:kind='Runtime GPU diagnostic'
    elif '/fixture-join-diagnostic/' in path:kind='Runtime lighting diagnostic'
    elif '/rebuild/previews/rejected-' in path:kind='Rejected native study'
    elif '/previews/rejected-' in path:kind='Rejected model study'
    elif '/rebuild/previews/' in path and 'rough-plan' in p.stem:kind='Planning diagram'
    elif '/rebuild/previews/' in path and p.stem.endswith('terrain-inspection'):kind='Native terrain inspection'
    elif '/rebuild/previews/' in path and p.stem.endswith('cutaway'):kind='Native inspection cutaway'
    elif '/rebuild/captures/' in path and p.stem.startswith('rejected-'):kind='Rejected runtime study'
    elif any('/castle-closeout/castle/'+r+'/' in path for r in ['pantry','linen','eave-store']):kind='Sealed context inspection'
    elif '/castle-closeout/castle/' in path:kind='Runtime room review baseline'
    elif '/rebuild/captures/house-graybox/' in path:kind='Runtime graybox capture'
    elif '/rebuild/captures/baseline/' in path:kind='Runtime baseline capture'
    elif '/rebuild/captures/' in path:kind='Runtime capture'
    elif '/castle-survey/' in path:kind='Runtime GPU capture'
    elif '/lighting-camera-audit/' in path or '/van-castle-repair/' in path or '/metal-refinement/' in path or '/van-unification/' in path or '/safe-area-barrier/' in path:kind='Runtime capture'
    elif '/water-editor/' in path:kind='Editor UI verification'
    elif '/equipment-menu/' in path:kind='Model inspection'
    elif '/menu-workflow/' in path:kind='Game UI verification'
    elif 'player-webm-playback-proof' in path:kind='Browser playback verification'
    elif 'physics-query-diagnostic' in path:kind='Runtime geometry diagnostic'
    elif '/edge-sky-plans/' in path:kind='Planning diagram'
    elif '/blockouts/' in path:kind='Rough blockout'
    elif '-blender-' in path or 'native-fitout' in p.stem or 'native-circuit' in p.stem:kind='Native scene overview'
    elif 'studio' in p.stem:kind='Model inspection'
    elif any(t in path for t in ['/runtime/','-runtime/','/blackpine/','/mill-lighting/','/mill-expansion/','/van/','/optical-evidence/']):kind='Runtime capture'
    elif '/castle-training/previews/generated-bench-materials/' in path:kind='Model inspection'
    elif '/castle-services/previews/plant-assembly/' in path:kind='Model inspection'
    elif '/castle-museum/previews/tapestry-rebuild/' in path or '/castle-museum/previews/reused-surfaces/' in path or '/castle-museum/previews/heraldic-rebuild/' in path or '/castle-museum/previews/keeper-finish/' in path:kind='Model inspection'
    elif '/castle-museum/previews/' in path:kind='Unaccepted model study'
    elif p.stem.startswith('level_') or '/model-production/' in path or '/equipment-catalog/' in path:kind='Model inspection'
    title=p.stem.replace('_',' ').replace('-',' ').strip().capitalize()
    items.append(dict(path=path,url=quote(path,safe='/'),title=title,kind=kind,width=w,height=h,bytes=len(data),sha256=hashlib.sha256(data).hexdigest()))
    clips=[p.with_suffix(ext) for ext in ['.webm','.mp4'] if p.with_suffix(ext).exists()]
    if clips:
        variants=[dict(url=quote(clip.relative_to(ROOT).as_posix(),safe='/'),bytes=clip.stat().st_size,sha256=hashlib.sha256(clip.read_bytes()).hexdigest()) for clip in clips]
        items[-1]['animation']=variants[0]
        if len(variants)>1:items[-1]['animation']['fallbacks']=variants[1:]
manifest=dict(version=1,title='Beckenburg development gallery',count=len(items),images=items)
encoded=json.dumps(manifest,indent=2,ensure_ascii=False)+'\n'
template=(ROOT/'development/gallery/template.html').read_text(encoding='utf-8')
page=template.replace('GALLERY_DATA',json.dumps(manifest,ensure_ascii=False).replace('<','\\u003c'))
outputs={ROOT/'development/gallery/index.json':encoded,ROOT/'beckenburg-development-gallery.html':page}
for p,text in outputs.items():
    if args.check:
        if not p.exists() or p.read_text(encoding='utf-8')!=text:raise SystemExit('Stale gallery: run python tools/build_development_gallery.py')
    else:p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(text.encode('utf-8'))
print('PASS development gallery:',len(items),'original PNGs, valid dimensions and SHA-256; index '+('current' if args.check else 'generated'))
