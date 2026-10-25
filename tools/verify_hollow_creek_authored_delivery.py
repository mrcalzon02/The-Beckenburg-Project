"""Verify immutable committed source/GLBs plus the actually deployed review."""
from pathlib import Path
import json,hashlib,subprocess,time,sys
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError
from urllib.parse import quote
from concurrent.futures import ThreadPoolExecutor
ROOT=Path(__file__).resolve().parents[1]
commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
berm='--berm' in sys.argv;siteart='--siteart' in sys.argv;fitted='--fitted' in sys.argv;tailored='--tailored' in sys.argv
r=json.loads((ROOT/('development/levels/hollow-creek/fitted-floors.json' if fitted else 'development/levels/hollow-creek/tailored-fabric.json' if tailored else 'development/levels/hollow-creek/authored-surfaces.json')).read_text(encoding='utf-8'))
paths=[p for a in r['assets'] for p in ['assets/runtime/models/'+a['id']+'.glb',a['editable']]]
paths+=['development/levels/hollow-creek/hollow-creek-authored.blend','development/levels/hollow-creek/AUTHORED_REVIEW.html','development/levels/hollow-creek/authored-surfaces.json','development/levels/author_hollow_creek_surfaces.py','development/levels/review_hollow_creek_authored.py','development/levels/verification/hollow-creek-authored-surfaces.json','development/levels/verification/hollow-creek-authored-pixels.tsv']
if tailored:paths=paths[:6]+['development/levels/hollow-creek/hollow-creek-tailored.blend','development/levels/hollow-creek/TAILORED_REVIEW.html','development/levels/hollow-creek/tailored-fabric.json','development/levels/tailor_hollow_creek_fabric.py','development/levels/review_hollow_creek_fabric.py','development/levels/verification/hollow-creek-tailored-shape.json','development/levels/verification/hollow-creek-tailored-fit.json','development/levels/verification/hollow-creek-tailored-pixels.tsv']
if fitted:paths=paths[:16]+['development/levels/hollow-creek/hollow-creek-fitted.blend','development/levels/hollow-creek/FITTED_FLOOR_REVIEW.html','development/levels/hollow-creek/fitted-floors.json','development/levels/fit_hollow_creek_floors.py','development/levels/validate_hollow_creek_floors.py','development/levels/render_hollow_creek_floors.py','development/levels/verification/hollow-creek-fitted-floors.json','development/levels/verification/hollow-creek-fitted-fit.json','development/levels/verification/hollow-creek-fitted-captures.json','development/levels/verification/hollow-creek-fitted-pixels.tsv','development/levels/verification/hollow-creek-fitted-geometry-cost.json']
if siteart:
    folder='development/levels/hollow-creek/'
    packet=json.loads((ROOT/(folder+'siteart-portable-assets.json')).read_text(encoding='utf-8'))
    paths=[p for a in packet['assets'] for p in ['assets/runtime/models/'+a['id']+'.glb',a['editable']]]
    paths+=['assets/runtime/models/level_camp_memorial_uv_tracks.glb','development/levels/blender/level_camp_memorial_uv_tracks.blend']
    paths+=[folder+p for p in ['hollow-creek-siteart.blend','hollow-creek-siteart-oriented.blend','hollow-creek-siteart-fitted.blend','SITE_ART_REVIEW.html','site-art.json','siteart-headings.json','siteart-fitted-floors.json','siteart-portable-assets.json','organic-memorial-model.json']]
    paths+=['development/levels/textures/hollow-creek/'+p for p in ['grass-dirty-v2.png','trail-dirt-v2.png','shore-grit-v2.png','GENERATION_PROMPTS.json']]
    paths+=['development/audio/hollow-creek/'+p for p in ['goose-trumpet-source-hq.mp3','goose-colossal-omen-v1.wav','PROVENANCE.json','build_goose_omen.py']]
    paths+=['assets/runtime/sfx/camp_hollow_creek_goose_omen.wav']
    paths+=['development/levels/verification/hollow-creek-siteart'+p for p in ['.json','-floors.json','-fit.json','-captures.json','-runtime.json','-pixels.tsv']]
if berm:
    folder='development/levels/hollow-creek/'
    packet=json.loads((ROOT/(folder+'grounded-berm.json')).read_text(encoding='utf-8'))
    paths=[p for a in packet['assets'] for p in [a['model'],a['editable']]]
    paths+=[folder+p for p in ['hollow-creek-grounded-berm.blend','grounded-berm.json','EARTHEN_BANK_REVIEW.html']]
    paths+=['development/levels/'+p for p in ['build_hollow_creek_earthen_banks.py','validate_hollow_creek_earthen_banks.py','render_hollow_creek_berm.py','build_hollow_creek_berm_review.py','record_hollow_creek_berm_checkpoint.py','finalize_earthen_bank_sources.py','DEVELOPMENT_HANDOFF_2026-10-25_EARTHEN_BANK.md']]
    paths+=['development/levels/textures/hollow-creek/'+p for p in ['bedrock-v1.png','bedrock-v1-prompt.json']]
    paths+=['development/levels/verification/hollow-creek-berm'+p for p in ['.json','-fit.json','-runtime.json','-captures.json','-pixels.tsv']]
    paths+=['src/main/java/org/beckenburg/GlbProps.java','src/main/java/org/beckenburg/LevelAssets.java','src/test/java/org/beckenburg/HollowCreekEarthenBankTest.java','regression-tests.txt']
    paths+=['development/quality/'+p for p in ['defects.json','DEFECT_LEDGER.md','evidence/DEV-080-berm-slope-uv-stretch.txt','evidence/DEV-081-berm-normal-fan-drift.txt','evidence/DEV-084-actual-crest-bearing-before-after.json']]

def download(url):
    for attempt in range(4):
        try:
            with urlopen(Request(url,headers={'User-Agent':'Beckenburg-source-verifier/1.0'}),timeout=45) as response:return response.read()
        except (HTTPError,URLError,TimeoutError) as e:
            if attempt==3 or getattr(e,'code',None) not in (None,429,500,502,503,504):raise
            if getattr(e,'code',None)==429:raise # preserve server backoff; retry publication later
            time.sleep([1,3,7][attempt])
def check(path):
    expected=subprocess.check_output(['git','show',commit+':'+path],cwd=ROOT)
    data=download('https://raw.githubusercontent.com/mrcalzon02/The-Beckenburg-Project/'+commit+'/'+quote(path,safe='/'))
    assert data==expected,path
    return dict(path=path,bytes=len(data),sha256=hashlib.sha256(data).hexdigest(),actual_download_matches=True)
with ThreadPoolExecutor(max_workers=4) as pool:checks=list(pool.map(check,paths))
review='development/levels/hollow-creek/'+('EARTHEN_BANK_REVIEW.html' if berm else 'SITE_ART_REVIEW.html' if siteart else 'FITTED_FLOOR_REVIEW.html' if fitted else 'TAILORED_REVIEW.html' if tailored else 'AUTHORED_REVIEW.html')
live=download('https://mrcalzon02.github.io/The-Beckenburg-Project/'+review)
assert live==subprocess.check_output(['git','show',commit+':'+review],cwd=ROOT),'Deployed review differs'
report=dict(status='pass',published_commit=commit,files=len(checks),checks=checks,deployed_review_matches=True,runtime_gameplay_acceptance=False)
(ROOT/('development/levels/verification/hollow-creek-berm-delivery.json' if berm else 'development/levels/verification/hollow-creek-siteart-delivery.json' if siteart else 'development/levels/verification/hollow-creek-fitted-delivery.json' if fitted else 'development/levels/verification/hollow-creek-tailored-delivery.json' if tailored else 'development/levels/verification/hollow-creek-authored-delivery.json')).write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('PASS',len(checks),'actual immutable downloads and deployed authored review;',commit)
