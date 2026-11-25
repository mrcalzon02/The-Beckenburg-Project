"""Index committed development captures without copying or altering originals.
Run before each map/model checkpoint; --check is the CI stale-index gate.
"""
from pathlib import Path
import argparse, hashlib, html, json, struct, subprocess
from urllib.parse import quote
ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');parser.add_argument('--tracked-only',action='store_true',help='Index the staged checkpoint without unrelated untracked captures');args=parser.parse_args()
paths=subprocess.check_output(['git','ls-files','--cached',* ([] if args.tracked_only else ['--others','--exclude-standard']),'-z','development'],cwd=ROOT).decode('utf-8').split('\0')
unstaged=set()
if args.tracked_only:
    changed=subprocess.check_output(['git','diff','--name-only','-z','--','development'],cwd=ROOT)
    unstaged=set(changed.decode('utf-8').split('\0'))
items=[]
for path in sorted(set(paths)):
    if not path.lower().endswith('.png'):continue
    if not any(t in path for t in ['/previews/','/captures/','/screenshots/','/blockouts/','/rebuild/structures/']):continue
    p=ROOT/path
    if path in unstaged:
        # Staged publication must hash the image actually included in the
        # commit, even if a package build rewrites the working copy.
        data=subprocess.check_output(['git','cat-file','blob',':'+path],cwd=ROOT)
    else:
        if not p.exists():continue
        data=p.read_bytes()
    if data[:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('Invalid PNG: '+path)
    w,h=struct.unpack('>II',data[16:24])
    kind='Development review'
    if '/monastery-perimeter-pass/' in path:
        if '/structure/' in path:
            kind='Native Monastery fitted estate boundary and exterior terrain'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Monastery perimeter and named routes in the '+renderer+' game renderer'
    elif '/monastery-loose-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Monastery of Ashen Bells native belongings and playable routes in the '+renderer+' game renderer'
    elif '/monastery-landscape-pass/' in path:
        if '/structure/' in path:
            kind='Monastery of Ashen Bells terrain and garden furnishing inspection'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Monastery of Ashen Bells fitted landscape and route review in the '+renderer+' game renderer'
    elif '/monastery-north-drainage-pass/' in path:
        if '/structure/' in path:
            kind='Monastery of Ashen Bells northern hillside interceptors and roof spillways'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Monastery of Ashen Bells packaged navigation and drainage review in the '+renderer+' game renderer'
    elif '/monastery-court-drainage-pass/' in path:
        if '/structure/' in path:
            kind='Monastery of Ashen Bells herb-court stormwater fittings and gallery sections'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Monastery of Ashen Bells courtyard drainage and walking review in the '+renderer+' game renderer'
    elif '/monastery-ground-drainage-pass/' in path:
        if '/structure/' in path:
            kind='Monastery of Ashen Bells surveyed stormwater terrain and built spillways'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Monastery of Ashen Bells named exterior drainage and retained routes in the '+renderer+' game renderer'
    elif '/monastery-furnishing-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Monastery of Ashen Bells furnishing and refuge review in the '+renderer+' game renderer'
    elif '/monastery-named-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Monastery of Ashen Bells named packaged review in the '+renderer+' game renderer'
    elif '/monastery-vertical-power-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Monastery of Ashen Bells bell gallery and catacomb lighting in the '+renderer+' game renderer'
    elif '/monastery-west-power-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Monastery of Ashen Bells gate and western circuit lighting in the '+renderer+' game renderer'
    elif '/monastery-north-power-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Monastery of Ashen Bells archive and vestry lighting in the '+renderer+' game renderer'
    elif '/monastery-east-power-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Monastery of Ashen Bells east-range switched lighting in the '+renderer+' game renderer'
    elif '/monastery-power-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Monastery of Ashen Bells service power and switched fixtures in the '+renderer+' game renderer'
    elif '/monastery-eave-pass/' in path:
        if '/export-inspection/' in path:
            kind='Monastery of Ashen Bells exported gate and herb court eave inspection'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Monastery of Ashen Bells fitted eave gutter in the '+renderer+' game renderer'
    elif '/monastery-flashing-pass/' in path:
        if '/export-inspection/' in path:
            kind='Monastery of Ashen Bells exported tower and cloister roof flashing inspection'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Monastery of Ashen Bells fitted roof abutment in the '+renderer+' game renderer'
    elif '/monastery-valley-pass/' in path:
        if '/export-inspection/' in path:
            kind='Monastery of Ashen Bells exported roof valley inspection'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Monastery of Ashen Bells roof outfall in the '+renderer+' game renderer'
    elif '/monastery-handrail-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        subject='retained walk-in refuge' if 'cupboard-' in path else 'fitted stair handrail'
        kind='Monastery of Ashen Bells '+subject+' in the '+renderer+' game renderer'
    elif '/monastery-refuge-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Monastery of Ashen Bells walk-in spectral refuge in the '+renderer+' game renderer'
    elif '/monastery-door-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Monastery of Ashen Bells operating masonry threshold in the '+renderer+' game renderer'
    elif '/dam-named-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Blackwater Dam named packaged site in the '+renderer+' game renderer'
    elif '/dam-ventilation-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Blackwater Dam fitted generator exhaust and weather louver in the '+renderer+' game renderer'
    elif '/dam-carrier-pass/' in path:
        if '/structure/' in path:
            kind='Blackwater Dam '+('isolated carrier and sluice cutaway' if 'cutaway' in path
                                     else 'assembled covered carrier terminal structural source')
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Blackwater Dam covered carrier and retained water in the '+renderer+' game renderer'
    elif '/dam-reservoir-pass/' in path:
        if '/structure/' in path:
            kind='Blackwater Dam '+('prior unsupported west wing source' if 'west-wing-before' in path
                                     else 'built headpond and foundation structural source')
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Blackwater Dam '+('matched dry terrain control' if 'dry-control' in path
                                     else 'headpond or retained tailrace')+' in the '+renderer+' game renderer'
    elif '/dam-power-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Blackwater Dam '+('fitted equipment with investigator flashlight' if '-inspection.png' in path
                                 else 'paired auxiliary power state')+' in the '+renderer+' game renderer'
    elif '/dam-refuge-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Blackwater Dam '+('zero-cover refuge control' if '-disabled.png' in path
                                 else 'full walk-in refuge')+' in the '+renderer+' game renderer'
    elif '/dam-door-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        position='open' if '-open.png' in path else 'closed'
        kind='Blackwater Dam fitted '+position+' door in the '+renderer+' game renderer'
    elif '/dam-valley-pass/' in path:
        if '/structure/' in path:
            kind='Blackwater Dam joined valley and founded perimeter architectural review'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Blackwater Dam valley and retained water in the '+renderer+' game renderer'
    elif '/dam-tailrace-pass/' in path:
        if '/structure/' in path:
            kind='Blackwater Dam cast downstream channel structural review'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            state='dry renderer control' if 'dry-control' in path else 'contained downstream water'
            kind='Blackwater Dam '+state+' in the '+renderer+' game renderer'
    elif '/dam-catwalk-ground-pass/' in path:
        phase='baseline' if '-before.png' in path else 'revised'
        kind='Blackwater Dam '+phase+' east bank native architectural review'
    elif '/northwatch-named-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        view='forecourt and radio entrance' if 'forecourt' in path else 'arrival van'
        kind='Northwatch named review '+view+' in the '+renderer+' game renderer'
    elif '/northwatch-furnishing-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind=('Northwatch readable station record' if 'northwatch-record-' in path
              else 'Northwatch fitted furnishings in the '+renderer+' game renderer')
    elif '/northwatch-water-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Northwatch independent wash-water service in the '+renderer+' game renderer'
    elif '/northwatch-complete-power-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Northwatch heated tunnel and outdoor lighting in the '+renderer+' game renderer'
    elif '/northwatch-surface-power-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Northwatch occupied surface electrical service in the '+renderer+' game renderer'
    elif '/northwatch-distribution-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Northwatch west room circuit in the '+renderer+' game renderer'
    elif '/northwatch-power-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Northwatch shed electrical supply and blackout in the '+renderer+' game renderer'
    elif '/northwatch-refuge-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Northwatch walk-in refuge in the '+renderer+' game renderer'
    elif '/northwatch-door-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Northwatch fitted operating door in the '+renderer+' game renderer'
    elif '/northwatch-boundary-pass/' in path:
        if '/structure/' in path:
            kind='Northwatch founded windbreak, road gate and joined valley construction'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Northwatch closed physical perimeter in the '+renderer+' game renderer'
    elif '/cemetery-named-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        if 'cemetery-record-' in p.name:
            kind='Named Cemetery and Vale Mortuary readable record'
        else:
            kind='Named Cemetery and Vale Mortuary case in the '+renderer+' game renderer'
    elif '/cemetery-furnishings-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        if '/structure/' in path:
            kind='Vale Mortuary rough furnishing and standing aisle study'
        elif p.name.startswith('cemetery-record-'):
            kind='Cemetery and Vale Mortuary readable case record'
        elif p.name.startswith('cemetery-desk-'):
            kind='Cemetery and Vale Mortuary supported record in the '+renderer+' game renderer'
        else:
            kind='Cemetery services retained after furnishing in the '+renderer+' game renderer'
    elif '/cemetery-distribution-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        if '/structure/' in path:
            kind='Cemetery surveyed pavilion wall riser and existing doorway'
        elif p.name.startswith('caretaker-power-'):
            kind='Cemetery caretaker plant in the '+renderer+' game renderer'
        else:
            kind='Cemetery and Vale Mortuary switched loads in the '+renderer+' game renderer'
    elif '/cemetery-power-pass/' in path:
        if '/structure/' in path:
            kind='Cemetery caretaker yard slab, ramp and service shelter'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Cemetery caretaker generator, breaker and actual lamp in the '+renderer+' game renderer'
    elif '/cemetery-refuge-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Cemetery and Vale Mortuary full walk-in safe zones in the '+renderer+' game renderer'
    elif '/cemetery-door-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Cemetery and Vale Mortuary operating doors in the '+renderer+' game renderer'
    elif '/cemetery-boundary-pass/' in path:
        if '/structure/' in path:
            kind='Cemetery and Vale Mortuary fitted enclosure and joined ground'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Cemetery and Vale Mortuary standing boundary review in the '+renderer+' game renderer'
    elif '/wharf-case-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        if p.name.startswith('wharf-record-'):
            kind='Wharf in-game readable case record'
        elif p.name.startswith('wharf-bench-'):
            kind='Wharf supported case record in the '+renderer+' game renderer'
        else:
            kind='Named Wharf service and refuge check in the '+renderer+' game renderer'
    elif '/wharf-distribution-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Wharf fitted room circuit in the '+renderer+' game renderer'
    elif '/wharf-power-pass/' in path:
        renderer='structural inspection' if '/structure/' in path else 'OpenGL' if '/opengl/' in path else 'software'
        kind='Wharf fitted electrical supply and lamp, '+renderer
    elif '/wharf-cover-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Wharf full walk-in safe zone in the '+renderer+' game renderer'
    elif '/wharf-door-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Wharf fitted door leaf in the '+renderer+' game renderer'
    elif '/wharf-landward-pass/' in path:
        if '/structure/' in path:
            kind='Wharf surveyed road and shore enclosure'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Wharf landward enclosure and retained routes in the '+renderer+' game renderer'
    elif '/wharf-tidal-pass/' in path:
        if '/structure/' in path:
            kind='Wharf native estuary and surveyed mean tide'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Wharf tide, dry undercroft and canonical arrival in the '+renderer+' game renderer'
    elif '/wharf-foundation-pass/' in path:
        kind='Wharf undercroft and pile grid cutaway' if 'undercroft' in p.name else 'Wharf native deck and pier foundation review'
    elif '/wall-finish-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Beckenburg wall finish at player height in the '+renderer+' game renderer'
    elif '/mine-case-pass/' in path:
        if '/structure/' in path:
            kind='Dead Man’s Claim supported case furnishings and hoist cutaway'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Dead Man’s Claim named case in the '+renderer+' game renderer'
    elif '/mine-sump-pass/' in path:
        if '/structure/' in path:
            kind='Dead Man’s Claim formed lower sump structure'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Dead Man’s Claim contained Mine sump and lower chamber in the '+renderer+' game renderer'
    elif '/mine-drainage-pass/' in path:
        if '/structure/' in path:
            kind='Dead Man’s Claim roof runoff and terrain drainage construction'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Dead Man’s Claim surface drainage and retained routes in the '+renderer+' game renderer'
    elif '/mine-lower-slabs-pass/' in path:
        if '/structure/' in path:
            kind='Dead Man’s Claim lower and ore-west slabs structural cutaway'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Dead Man’s Claim rebuilt lower chambers in the '+renderer+' game renderer'
    elif '/mine-distribution-pass/' in path:
        if '/structure/' in path:
            kind='Dead Man’s Claim haul shaft electrical raceway structural cutaway'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Dead Man’s Claim working electrical distribution in the '+renderer+' game renderer'
    elif '/mine-service-yard-pass/' in path:
        if '/structure/' in path:
            kind='Dead Man’s Claim service shed structural review'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Dead Man’s Claim service yard in the '+renderer+' game renderer'
    elif '/mine-handrail-pass/' in path:
        if '/structure/' in path:
            kind='Dead Man’s Claim stair handrail structural cutaway'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Dead Man’s Claim stair handrails in the '+renderer+' game renderer'
    elif '/mine-cover-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Dead Man’s Claim walk-in safe zone in the '+renderer+' game renderer'
    elif '/mine-stair-threshold-pass/' in path:
        if '/structure/' in path:
            kind='Dead Man’s Claim headhouse structural cutaway'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Dead Man’s Claim fitted stairs and doorways in the '+renderer+' game renderer'
    elif '/lookout-case-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        subject='readable record' if '-story-' in p.name else 'verified case view'
        kind='Widow’s Peak Lookout '+subject+' in the '+renderer+' game renderer'
    elif '/lookout-power-pass/' in path:
        if '/structure/' in path:
            kind='Widow’s Peak Lookout roadhead shelter structural review'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            subject='electrical service' if 'lookout-power-' in p.name else 'retained arrival and cover'
            kind='Widow’s Peak Lookout '+subject+' in the '+renderer+' game renderer'
    elif '/lookout-cover-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        subject='full walk-in safe zone' if '-cupboard-' in path else 'retained arrival and doors'
        kind='Widow’s Peak Lookout '+subject+' in the '+renderer+' game renderer'
    elif '/lookout-native-arrival-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Widowâ€™s Peak Lookout staged doorway and arrival in the '+renderer+' game renderer'
    elif '/asylum-enclosure-pass/' in path:
        if '/structure/' in path:
            kind='Native St. Dymphna estate enclosure and carriage road'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='St. Dymphna enclosure in the '+renderer+' game renderer'
    elif '/asylum-flight-rails-pass/' in path:
        if '/structure/' in path:
            kind='Asylum main stair handrail structural cutaway'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Asylum finished main stair flight in the '+renderer+' game renderer'
    elif '/asylum-upper-spine-pass/' in path:
        if '/structure/' in path:
            kind='Asylum upper masonry and stairwell guard structural cutaway'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Asylum upper spine and guarded staircase in the '+renderer+' game renderer'
    elif '/asylum-spine-masonry-pass/' in path:
        if '/structure/' in path:
            kind='Asylum isolated native ground spine construction review'
        else:
            renderer='OpenGL' if '/opengl/' in path else 'software'
            kind='Asylum rebuilt ground spine in the '+renderer+' game renderer'
    elif '/asylum-interior-door-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Asylum fitted interior and chapel doorways in the '+renderer+' game renderer'
    elif '/asylum-power-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Asylum plant power and walk-in cupboard protection in the '+renderer+' game renderer'
    elif '/asylum-patient-court-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Asylum fitted patient court in the '+renderer+' game renderer'
    elif '/asylum-admission-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Asylum staged admission thresholds in the '+renderer+' game renderer'
    elif '/prison-vehicle-sally-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Complete Prison vehicle facade and gate in the '+renderer+' game renderer'
    elif '/prison-hiding-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Prison physical cupboard review in the '+renderer+' game renderer'
    elif '/prison-plant-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Prison packaged gate or plant in the '+renderer+' game renderer'
    elif '/prison-sally-gate-pass/' in path:
        renderer='OpenGL' if '/opengl/' in path else 'software'
        kind='Prison sally gate staged views in the '+renderer+' game renderer'
    elif '/prison-public-entry-pass/' in path:
        kind='Prison staged public entrances in the software game renderer'
    elif '/school-electrical-pass/' in path:
        renderer='OpenGL' if 'opengl' in p.name else 'software'
        kind='Ravenshade packaged standing view in the '+renderer+' renderer'
    elif '/college-electrical-batch/' in path:
        kind='College circuit and fixture plan' if 'layout' in p.name else 'College OpenGL lighting and blackout' if 'opengl' in p.name else 'College software standing electrical review'
    elif '/college-services-batch/' in path:
        if 'cutaway' in p.name:
            kind='College native plant cutaway with upper structure and selected walls hidden'
        elif 'native' in p.name:
            kind='College native roof and installed duct inspection'
        elif 'sealed-' in p.name:
            kind='College sealed room inspection in the '+('OpenGL' if 'opengl' in p.name else 'software')+' game renderer'
        else:
            kind='College standing service review in the '+('OpenGL' if 'opengl' in p.name else 'software')+' game renderer'
    elif '/college-containment-batch/' in path:
        if 'cutaway' in p.name:
            kind='College native stair tower cutaway with overhead and near wall surfaces hidden'
        elif 'native' in p.name:
            kind='College native grounds and gate inspection'
        elif 'opengl' in p.name:
            kind='College OpenGL standing containment review'
        else:
            kind='College software standing containment review'
    elif '/college-doors-batch/' in path:
        if 'plan' in p.name:
            kind='College surveyed door and circulation plan'
        elif 'opengl' in p.name:
            kind='College fitted doors in the OpenGL game renderer'
        else:
            kind='College fitted doors at standing height'
    elif '/house-parcel-boundary-batch/' in path:
        kind='House surveyed parcel and doorway plan' if 'parcel-plan' in p.name else 'House timber boundary at standing height'
    elif '/seacliff-receiving-batch/' in path:
        kind='Battery protected staircase cutaway' if 'section' in p.name else 'Battery game renderer' if 'runtime' in p.name else 'Battery native construction review'
    elif '/flush-porch-and-threshold-batch/' in path:
        kind='House porch at standing height' if p.name.startswith('house-porch-') else 'House game renderer' if p.name.startswith('house-') else 'Saved doorway and stair landing'
    elif '/native-foyer-door-batch/' in path:
        kind='Staged School runtime door state' if p.name.startswith('school-') else 'Native fitted crossdock and doorway review'
    elif '/measured-arrival-and-approach-batch/' in path:
        kind='Native farm and station threshold review or School staged arrival geometry'
    elif '/interior-stair-landing-batch/' in path:
        kind='Native Manor and School before stair landing repairs' if 'before' in p.name else 'Native Manor and School service doors, guarded aisles and stair approaches'
    elif '/lookout-landing-batch/' in path:
        kind='Native Lookout before the receiving landing and foundation rebuild' if 'before' in p.name else 'Native Lookout grounds, landings, foundations and tower inspection'
    elif '/dam-east-hall-batch/' in path:
        kind='Native Dam east hall before the bearing rebuild' if 'before' in p.name else 'Native Dam east hall girder, aisle and landing inspection'
    elif '/workyard-station-batch/' in path:
        kind='Native cannery or railway before the workyard and landing rebuild' if 'before' in p.name else 'Native cannery and railway grounds, doors and lower landing'
    elif '/estate-parish-batch/' in path:
        kind='Native estate, farm and parish before entrance reconstruction' if 'before' in p.name else 'Native estate, farm and parish courts and doorway inspection'
    elif '/canyon-marsh-batch/' in path:
        if 'before' in p.name:kind='Native canyon and marsh maps before structural reconstruction'
        elif 'section' in p.name:kind='Native canyon terrace and marsh foundation inspection'
        else:kind='Native Red Mesa and Mourning Marsh construction review'
    elif '/coast-entrances-batch/' in path:
        if 'before' in p.name:kind='Native coast map before reconstruction'
        elif 'section' in p.name:kind='Native fixed gangway bearing section with water hidden'
        else:kind='Native Lightstation and Saint Elara construction review'
    elif '/monastery-cloister-batch/' in path:
        if 'before' in p.name:kind='Native Monastery before the cloister and bell tower rebuild'
        elif p.stem=='tower-section':kind='Native bell stair section with inspection layers hidden'
        else:kind='Native Monastery courts, doorway and stair inspection'
    elif '/mine-yard-batch/' in path:
        if 'before' in p.name:kind='Native Mine before reconstruction with physical study surfaces'
        elif p.stem=='shaft-section':kind='Native Mine structural section with review layers hidden'
        else:kind='Native Mine work yard, headhouse and enclosed stair review'
    elif '/cemetery-grounds-batch/' in path:
        if 'cutaway' in p.name:kind='Native crypt section with inspection layers hidden'
        elif p.stem=='before':kind='Native before reconstruction with physical study strips'
        else:kind='Native Cemetery and Vale Mortuary terrain, entrances and structure'
    elif '/college-ground-batch/' in path:
        kind='Native campus before ground reconstruction' if 'before' in p.name else 'Native College terrain and building review with the shared van at arrival'
    elif '/dam-spillway-gallery-batch/' in path:
        kind='Native structural cutaway; terrain and overhead layers hidden' if 'cutaway' in p.name else 'Native before construction; terrain and overhead layers hidden' if 'before' in p.name else 'Native exported Dam gallery and crest inspection'
    elif '/service-ground-bay-batch/' in path or '/service-ground-bay-pass/' in path:
        kind='Native earlier pilot/before architecture; source retained, not final acceptance' if 'before' in p.name or 'pilot' in p.name else 'Native architectural structural cutaway; hidden layers recorded, not runtime or whole-map acceptance' if 'cutaway' in p.name or 'section' in p.name else 'Native exported architecture and ordinary standing-height door/stair inspection; no runtime or whole-map acceptance'
    elif '/household-sacred-bay-batch/' in path:
        kind='Rejected native generated architecture before replacement; roofs hidden for inspection, original source retained' if 'rejected-before' in p.name else 'Native architectural bay cutaway; terrain, overhead layers and selected faces hidden only for load-path inspection; unfinished map/runtime gates' if 'cutaway' in p.name else 'Native ordinary standing-height reconstructed stair/landing/door inspection; no objects hidden, not runtime or whole-map acceptance'
    elif '/architectural-restart-batch/' in path:
        kind='Rejected native generated architecture before replacement; source history retained' if 'rejected-before' in p.name else 'Native reconstructed tower/frame or Dam workshop; ordinary standing-height source views, section hides terrain/overhead layers only; unresolved ground/emergency/runtime gates, no playable acceptance'
    elif '/dam-contained-stair-batch/' in path:
        kind='Native contained Dam stair construction; roof cutaway, walls/floors and upper well partitions hidden in load-path sections only; emergency/spillway and runtime gates open'
    elif '/terrain-ownership-construction-batch/' in path:
        kind='Native Dam bearing section; deck above -0.8m, ceiling caps and terrain hidden only for load-path inspection; older amber flights unfinished, no runtime acceptance' if 'bearing-section' in p.name else 'Native actual saved Motel Academy Dam terrain and full floors/arrival structures; roofs hidden, landscape and older stair/runtime gates open'
    elif '/service-portal-construction-batch/' in path:
        kind='Native Cinder gallery section; geometry above cut plane and terrain hidden only for inspection, other amber stairs unfinished, no runtime acceptance' if 'gallery-section' in p.name else 'Native reconstructed service doorways and Cinder galleries/U stair; roofs hidden, Motel original terrain and ordinary-route blockers remain open, no runtime acceptance'
    elif '/arrival-grounds-construction-batch/' in path:
        kind='Native actual saved Research Railway Cinder arrival/workground earthworks with founded trestle approaches; pitched roofs hidden, unfinished Cinder underground connections and doorway/landscape/runtime gates open'
    elif '/constant-flight-construction-batch/' in path:
        kind='Native constant-width stair cells, supported landings, rectangular wells and saved terrain punches; pitched roofs hidden, Research tunnel roof retained for support context, other amber Cinder flights unfinished, ordinary-ground and runtime gates open'
    elif '/stair-cell-construction-batch/' in path:
        kind='Native enclosed Research tunnel elbow; ceiling/upper layers hidden, old flared flights unfinished, no runtime acceptance' if 'wld-08' in p.name else 'Native service stair cutaway; east wall and upper layers hidden only for inspection, no runtime acceptance' if 'lower-cell-section' in p.name else 'Native constant stair flights, complete landings and rectangular floor openings; roofs hidden, no runtime acceptance'
    elif '/institution-earthworks-batch/' in path:kind='Native actual saved institution arrival and grounds sculpt with supported forecourt aprons; roofs hidden, rough materials, landscape/guard/runtime acceptance open'
    elif '/institution-courts-batch/' in path:kind='Native constructed institution courts and normal exterior doorways; roofs hidden, unfinished grade/edge protection and runtime gates open'
    elif '/institution-doorway-batch/' in path:kind='Native institutional entrance walls, indexed frames and soil-bearing landings; study solids removed, roofs hidden, outdoor movement defects and runtime gates open'
    elif '/doorway-construction-batch/' in path:kind='Native architectural doorway construction; indexed full-depth jambs and heads, roofs hidden for inspection, runtime acceptance open'
    elif '/wharf-stair-cells-batch/' in path:kind='Native reconstructed rectangular Wharf stair cells; constant flights and supported landings, roof context hidden, runtime acceptance open'
    elif '/site-earthworks-batch/' in path:kind='Native actual saved editor geographic terrain sculpt; roof cutaway, rough materials, landscape/runtime acceptance open'
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
    elif '/stair-interface-construction-batch/' in path:kind='Native physical stair/landing construction iteration; pitched roofs hidden; Dam interface is a labeled underground section; not runtime/arrival acceptance'
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
