from zipfile import ZipFile
from pathlib import Path
from io import StringIO
import csv
import hashlib
import math
import sys

def rows(z,name):
    return list(csv.DictReader(StringIO(z.read(name).decode())))



import math,csv,io


def check_clock(z):
    def rows(n):return list(csv.DictReader(io.StringIO(z.read(n).decode())))
    def records(arm,extra):return rows(f"game-clock/{arm}/{extra}.csv")
    def timing(cases,field):
        v=sorted(int(r[field])/1e6 for r in cases)
        return [round(v[math.ceil(len(v)*q)-1],2) for q in [.5,.95,.99]],sum(t>100 for t in v)
    for step in ("","-step"):
        which="scope-study"+step
        source=records(which,"receiver-factors")
        sources=records(which,"all-sources")
        changing={int(r["source"]) for r in sources if int(r["frame"])>=8 and r["changed"]=="true"}
        rr=[r for r in source if r["kind"]=="world" and r["has_prior"]=="true"]
        cupboards=[r for r in rr if r["cupboard"]=="true"]
        assert rr and cupboards and all(r["exact_source_match"]==r["exact_payload_match"]=="true" for r in rr)
        ids=lambda s:set(map(int,s.split("|"))) if s else set()
        assert all(not ids(r["source_ids"])&changing for r in cupboards)
        print("Revised revisits and cupboards",which,len(rr),len(cupboards),"changing source IDs",sorted(changing))
        fixed="pose-world-play-clock"+step
        a=records("base-play-clock"+step,"frames");b=records(fixed,"frames")
        assert [[v[k] for k in ("argb_sha256","depth_bits_sha256")] for v in a]==[[v[k] for k in ("argb_sha256","depth_bits_sha256")] for v in b]
        assert max(int(v["restored_vertices"]) for v in records(fixed,"scope"))==0
        for i in (1,2):
            a=records(f"base-clocked{step}{i}","frames");b=records(f"scope-opt-clocked{step}{i}","frames")
            assert len(a)==len(b)==34 and all(r["argb_sha256"]==s["argb_sha256"] and r["depth_bits_sha256"]==s["depth_bits_sha256"] for r,s in zip(a,b))
            c=records(f"scope-opt-clocked{step}{i}","scope")
            print("Candidate",step,i,"restored vertices",max(int(v["restored_vertices"]) for v in c))
        for arm in ("base","scope-opt"):
            cases=[r for i in (1,2) for r in records(f"{arm}-clocked{step}{i}","frames") if r["phase"]=="toggle"]
            print("Settled logical half-second cadence",arm,step,*(timing(cases,f) for f in ("end_to_end_ns","render_thread_cpu_ns")))
        for arm in ("base","scope-opt"):
            cases=records(arm+"-swingplay"+step,"frames");traces=records(arm+"-swingplay"+step,"scope")
            assert len(cases)==len(traces)==152
            print("Real swing",arm,step,"restored",max(int(r["restored_vertices"]) for r in traces))
            for phase in ("toggle","swing","settled"):
                group=[r for r in cases if r["phase"]==phase]
                print(phase,"count",len(group),*(timing(group,f) for f in ("end_to_end_ns","render_thread_cpu_ns")))
        a=records("base-swingplay"+step,"frames");b=records("scope-opt-swingplay"+step,"frames")
        print("Swing differential ARGB/depth counts",step,{phase:tuple(sum(r[k]!=s[k] for r,s in zip(a,b) if r["phase"]==phase) for k in ("argb_sha256","depth_bits_sha256")) for phase in ("toggle","swing","settled")})

def report(path):
    with ZipFile(path) as z:
        checks=[line.split("  ",1) for line in z.read("SHA256SUMS").decode().splitlines()]
        assert set(n for _,n in checks)==set(z.namelist())-{"SHA256SUMS"}
        for digest,name in checks: assert hashlib.sha256(z.read(name)).hexdigest()==digest,name
        ref=rows(z,"trials/base1/frames.csv")
        for name in z.namelist():
            if not name.endswith("/frames.csv") or not name.startswith("trials/"): continue
            r=rows(z,name)
            if len(r)!=58: continue
            diff=lambda k:[i for i,(a,b) in enumerate(zip(ref,r)) if a[k]!=b[k]]
            print(name,"different color",diff("argb_sha256"),"depth",diff("depth_bits_sha256"))
        for name in z.namelist():
            if not name.startswith("mixed/") or not name.endswith("/frames.csv"): continue
            case=name.split("/")[1]
            if "base" in case:continue
            kind="trim-mixed-base-reverseorder" if "reverseorder" in case else "mixed-base-rev" if "-rev" in case else "mixed-base"
            if f"mixed/{kind}/frames.csv" not in z.namelist(): continue
            a=rows(z,f"mixed/{kind}/frames.csv"); b=rows(z,name)
            assert len(b) <= len(a), (case,len(b),len(a))
            print(name,len(b),"different color", [i for i,(x,y) in enumerate(zip(a,b)) if x["argb_sha256"]!=y["argb_sha256"]],
                  "depth",[i for i,(x,y) in enumerate(zip(a,b)) if x["depth_bits_sha256"]!=y["depth_bits_sha256"]])
        check_clock(z)
        for suffix in ("","-rev","-reverseorder"):
            baseline=("trim-" if "order" in suffix else "")+"mixed-base"+suffix
            ref=rows(z,"mixed/"+baseline+"/frames.csv")
            for arm in ("scope-opt","scope-lamp"):
                out=rows(z,"mixed/"+arm+"-trim-mixed"+suffix+"/frames.csv")
                assert len(out)==160
                assert all(all(a[k]==b[k] for k in ("frame","argb_sha256","depth_bits_sha256","uploaded_batches","vertices_uploaded_bytes")) for a,b in zip(ref,out)), (arm,suffix)
        ref=rows(z,"game-clock/base-posegrid/frames.csv")
        cand=rows(z,"game-clock/scope-lamp-posegrid/frames.csv")
        assert len(ref)==len(cand)==144
        assert all(all(a[k]==b[k] for k in ("phase","argb_sha256","depth_bits_sha256","uploaded_batches","vertices_uploaded_bytes")) for a,b in zip(ref,cand))
        print("All 17 leaf poses with intervening light ticks",len(cand),"ARGB/raw-depth matches")
        for step in ("","-step"):
            for arm in ("base","scope-lamp"):
                cases=[rows(z,"game-clock/"+arm+"-swingplay"+step+s+"/frames.csv") for s in ("","-repeat")]
                scopes=[rows(z,"game-clock/"+arm+"-swingplay"+step+s+"/scope.csv") for s in ("","-repeat")]
                assert all(len(case)==152 for case in cases)
                for c in scopes:
                    assert max(int(r["restored_vertices"]) for r in c)>0 if arm=="scope-lamp" else all(int(r["restored_vertices"])==0 for r in c)
                for phase in ("toggle","swing","settled"):
                    spans=[r for case in cases for r in case if r["phase"]==phase]
                    values=sorted(int(r["end_to_end_ns"])/1e6 for r in spans)
                    print("Actual swing",arm,step,phase,len(spans),*[round(values[math.ceil(len(values)*q)-1],2) for q in (.5,.95,.99)],sum(t>100 for t in values))
                    print("Within-arm differential frames", [sum(a[k]!=b[k] for a,b in zip(*cases) if a["phase"]==phase) for k in ("argb_sha256","depth_bits_sha256")])
        import gzip
        pixel="pixels/frame-61.argb.gz"
        a=gzip.decompress(z.read("witness/mixed-pixel-rev/"+pixel))
        b=gzip.decompress(z.read("witness/pose-cert-mixed-pixel-rev/"+pixel))
        assert sum(a[i:i+4]!=b[i:i+4] for i in range(0,len(a),4))==365
        assert a[::4]==b[::4] and max(abs(x-y) for x,y in zip(a,b))==5
        for group in (("base1","base2"),("replay1","replay2"),("pose-cert1","pose-cert2"),("base3","base4"),("pose-world1","pose-world2"),("baseWorld1","baseWorld2")):
            if not all(f"trials/{n}/frames.csv" in z.namelist() for n in group):continue
            cases=[r for n in group for r in rows(z,f"trials/{n}/frames.csv") if r["phase"]=="toggle"]
            assert len(cases)==48
            for field in ("end_to_end_ns","render_thread_cpu_ns"):
                vals=sorted(int(r[field])/1e6 for r in cases)
                print(group,field,*[round(vals[math.ceil(len(vals)*p)-1],2) for p in (.5,.95,.99)],
                      ">100 ms",sum(v>100 for v in vals),"/",len(vals))

if __name__=="__main__":
    report(Path(sys.argv[1]) if len(sys.argv)>1 else Path(__file__).with_name("door-cpu-experiments.zip"))
