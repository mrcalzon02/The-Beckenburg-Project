"""Reproduce the quiet colossal-goose treatment from the retained CC0 preview.
No network/login is required for regeneration. This does not wire a game event.
"""
from pathlib import Path
import subprocess,json,hashlib,wave,array,math,shutil
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[2]
FILTER='asetrate=44100*0.42,aresample=44100,atempo=0.55,vibrato=f=0.85:d=0.7,tremolo=f=0.68:d=0.2,lowpass=f=3200,equalizer=f=180:t=q:w=0.9:g=3,aecho=0.65:0.55:240|480|900:0.12|0.08|0.04,volume=0.24,afade=t=in:d=0.025,afade=t=out:st=2.7:d=0.55'
source=HERE/'goose-trumpet-source-hq.mp3';target=HERE/'goose-colossal-omen-v1.wav'
subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(source),'-af',FILTER,'-ac','1','-ar','44100','-c:a','pcm_s16le',str(target)],check=True)
runtime=ROOT/'assets/runtime/sfx/camp_hollow_creek_goose_omen.wav';runtime.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(target,runtime)
with wave.open(str(target)) as w:
 assert w.getnchannels()==1 and w.getsampwidth()==2 and w.getframerate()==44100
 samples=array.array('h',w.readframes(w.getnframes()));duration=len(samples)/44100
 peak=max(abs(v) for v in samples)/32768;rms=math.sqrt(sum(v*v for v in samples)/len(samples))/32768
 assert 2<duration<5 and 0<peak<.5 and rms>0,'Non-silent quiet audio, no clipping'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
r=dict(date='2026-10-24',title='Goose Trumpet (loud honk) - Best Capture',author='el_boss',source_page='https://freesound.org/people/el_boss/sounds/740197/',license='CC0-1.0',license_url='https://creativecommons.org/publicdomain/zero/1.0/',public_preview_url='https://cdn.freesound.org/previews/740/740197_9129912-hq.mp3',source_kind='Public HQ preview; original WAV download requires login and was not used',source_sha256=sha(source),filter=FILTER,output_sha256=sha(target),runtime=runtime.relative_to(ROOT).as_posix(),duration_seconds=duration,peak_dbfs=20*math.log10(peak),rms_dbfs=20*math.log10(rms),runtime_event_status='NOT CONNECTED: requires actual active UV beam, range, occlusion, cooldown/reset, host/LAN and mix verification; cosmetic location lore, not ghost-ID evidence')
(HERE/'PROVENANCE.json').write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8');print(json.dumps(r,indent=2))
