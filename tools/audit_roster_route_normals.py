"""Independently inspect walking-side normals in actual HCS2 path solids."""
from pathlib import Path
import argparse,hashlib,json,re,struct,subprocess

ROOT=Path(__file__).resolve().parents[1]
def inspect(raw):
    magic,parts,total=struct.unpack_from('>III',raw);assert magic==0x48435332
    pos=12;flat=[];count=0
    for _ in range(parts):
        size=struct.unpack_from('>H',raw,pos)[0];pos+=2;name=raw[pos:pos+size].decode();pos+=size
        closed,n=struct.unpack_from('>BI',raw,pos);pos+=5;faces=[]
        for _ in range(n):
            *v,flags=struct.unpack_from('>9fB',raw,pos);pos+=37;count+=1
            faces.append((v,flags))
        if not re.match(r'^\d+:(L\d+|route-\d+) support \d+',name):continue
        lo=min(v[k] for v,_ in faces for k in [1,4,7]);hi=max(v[k] for v,_ in faces for k in [1,4,7])
        if abs(hi-lo-.12)>.0001:continue # sloping strips are audited separately by native walking grade
        top=[]
        for v,flags in faces:
            if max(abs(v[k]-hi) for k in [1,4,7])>.00005:continue
            ny=(v[5]-v[2])*(v[6]-v[0])-(v[3]-v[0])*(v[8]-v[2])
            top.append(ny>0 and bool(flags&1))
        assert top,(name,'No top face in an actual flat path solid')
        flat.append(dict(component=name,closed=bool(closed),walking_y=hi,outward_walking_top=all(top),top_triangles=len(top)))
    assert pos==len(raw) and count==total
    return flat
def main():
    p=argparse.ArgumentParser();p.add_argument('--ref');p.add_argument('--output',type=Path,required=True);p.add_argument('--require-outward',action='store_true');args=p.parse_args()
    sites=json.loads((ROOT/'development/levels/rebuild/furnishings/room-programs.json').read_text())['sites'];rows=[]
    ref=subprocess.check_output(['git','rev-parse',args.ref],cwd=ROOT,text=True).strip() if args.ref else None
    for site in sites:
        relative=f'development/levels/rebuild/structures/{site.lower()}/structure-physics.hcs2'
        raw=subprocess.check_output(['git','show',ref+':'+relative],cwd=ROOT) if ref else (ROOT/relative).read_bytes()
        flat=inspect(raw);rows.append(dict(site=site,physics_sha256=hashlib.sha256(raw).hexdigest(),flat_path_solids=len(flat),inward_or_unwalkable_tops=sum(not r['outward_walking_top'] for r in flat),components=flat))
    result=dict(reference=ref or 'CURRENT_CANDIDATE',parser_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),sites=rows,limitation='Only flat12cm authored route solids; complete body/support sweeps, other solids, terrain/guards and runtime integration remain separate.')
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_bytes((json.dumps(result,indent=2)+'\n').encode())
    print({k:sum(r[k] for r in rows) for k in ['flat_path_solids','inward_or_unwalkable_tops']})
    if args.require_outward:assert all(r['inward_or_unwalkable_tops']==0 for r in rows),'An actual flat path top still points inward or lacks walking support'
if __name__=='__main__':main()
