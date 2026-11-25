"""Verify Northwatch's exact native perimeter and existing walkable approaches."""
import argparse
import json
import os
from pathlib import Path
import subprocess

from verify_measured_approaches import JOURNEYS, ROOT, join, table
from verify_interior_stair_landings import svg_line, svg_rect, svg_text

PACKET = ROOT / "development/levels/rebuild/northwatch-boundary-pass/wld-08"
OWNERS = dict(lab=0,dorm=1,mess=2,radio=3,records=4,generator=5,store=6,tunnel=7,
              **{"tunnel-n":8,"shelter":9,"array":10,"court":11,"entry":12,"stage":13,"tunnel-elbow":7})
CONTROLS = (((32,0,89),(32,0,92)),((53,0,-3),(53,0,-6)),((-5,0,53),(-8,0,53)))


def draw_plan(meta,layout):
    def xy(x,z):
        return 128+7*x,110+7*z

    body=[svg_text(30,42,"Northwatch Research Station","title"),
          svg_text(30,69,"Built perimeter and retained ground-level approaches ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â· north up","small")]
    for zone in layout["zones"]:
        if zone["y"]!=0:
            continue
        a,b,c,d=zone["bounds"]
        x,z=xy(a,b)
        style="building" if zone["kind"]=="inside" else "grounds"
        body.append(svg_rect(x,z,7*(c-a),7*(d-b),style))
        label=svg_text(x+5,z+15,zone["id"].replace("-"," ").capitalize(),"small")
        body.append(label)
    for link in layout["links"]:
        if any(point[1]<-.05 for point in link["points"]):
            continue
        body.append(svg_line([xy(x,z) for x,_,z in link["points"]],"guide"))
    for arch in layout["authored_doorways"]:
        along,fixed=arch["along"],arch["boundary"]
        half=arch["clear_width"]/2
        a,b=((along-half,fixed),(along+half,fixed)) if arch["axis"]=="z" else ((fixed,along-half),(fixed,along+half))
        body.append(svg_line([xy(*a),xy(*b)],"opening"))
    body.append(svg_line([xy(*point) for point in meta["alignment"]],"windbreak"))
    body.append(svg_line([xy(*point) for point in meta["gate"]["endpoints"]],"gate"))
    x,z=xy(32,76)
    body.append(svg_rect(x-7.3,z-19,14.6,36,"van"))
    body.extend([svg_line([(695,177),(727,177)],"windbreak"),svg_text(735,182,"Founded windbreak","small"),
                 svg_line([(695,211),(727,211)],"gate"),svg_text(735,216,"Closed vehicle gate","small"),
                 svg_line([(695,245),(727,245)],"opening"),svg_text(735,250,"Existing doorway","small"),
                 svg_line([(695,279),(727,279)],"guide"),svg_text(735,284,"Nonphysical route guide","small"),
                 svg_rect(704,306,13,16,"van"),svg_text(735,321,"Canonical van","small")])
    svg='''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 932 768" role="img">
<title>Northwatch perimeter and existing ground-level approaches</title>
<style>
text{fill:#243541;font:16px "Segoe UI",sans-serif}.title{font-size:27px;font-weight:700}.small{font-size:13px}
.building{fill:#cfdbe3;stroke:#638092;stroke-width:1.2}.grounds{fill:#e6ebe8;stroke:#c2cbc7;stroke-width:.7}
.windbreak,.gate,.guide,.opening{fill:none}.windbreak{stroke:#284453;stroke-width:3.6}
.gate{stroke:#934b34;stroke-width:4.5}.guide{stroke:#548075;stroke-width:1.6;stroke-dasharray:5 4}
.opening{stroke:#145285;stroke-width:3.5}.van{fill:#717d84;stroke:#263d4b;stroke-width:.6}
</style><rect width="932" height="768" fill="#f8faf9"/>'''
    (PACKET/"perimeter-plan.svg").write_text(svg+''.join(body)+"</svg>\n",encoding="utf-8",newline="\n")


def prepare():
    meta=json.loads((PACKET/"construction.json").read_bytes())
    layout=json.loads((ROOT/meta["layout"]).read_bytes())
    links=layout["links"]
    spawns={}
    for link in links:
        spawns.setdefault(link["a"],link["points"][0])
        spawns.setdefault(link["b"],link["points"][-1])
    spawns["tunnel-elbow"]=[22,-2.5,24]
    rows=[("SPACE",zone["id"],OWNERS[zone["id"]],*zone["bounds"],zone["y"],zone["height"],
           str(zone["kind"] in ("outside","staging")).lower(),zone["name"],*spawns[zone["id"]])
          for zone in layout["zones"]]
    rows.append(("SPACE","station-exterior",12,-54,-41,114,135,-1.0,9.0,"true",
                 "Station exterior",*spawns["entry"]))
    rows.extend(("TREAD",name) for name in meta["stair_components"])
    rows.extend(("RITUAL",rite["rite"].lower(),OWNERS[rite["zone"]],*rite["point"]) for rite in layout["rituals"])
    rows.extend(("RIM_CONTROL",*coords) for pair in CONTROLS for coords in pair)
    cargo=layout["staging"]["cargo_route"]
    rows.extend(("ARRIVAL",*point) for point in cargo)
    routes={link["id"]:link["points"] for link in links}
    chains=[(f"J{i:02}",join(routes,chain.split())) for i,(_,chain) in enumerate(JOURNEYS["WLD-08"],1)]
    rows.extend(("POINT",name,*point) for name,coords in [*routes.items(),*chains] for point in coords)
    table(PACKET/"runtime-input.tsv",rows)
    draw_plan(meta,layout)
    return meta


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--gpu",action="store_true")
    stage=parser.add_mutually_exclusive_group()
    stage.add_argument("--doors",action="store_true")
    stage.add_argument("--refuges",action="store_true")
    stage.add_argument("--power",action="store_true")
    stage.add_argument("--distribution",action="store_true")
    stage.add_argument("--surface-power",action="store_true")
    stage.add_argument("--complete-power",action="store_true")
    stage.add_argument("--water",action="store_true")
    stage.add_argument("--furnishings",action="store_true")
    args=parser.parse_args()
    fitted=args.doors or args.refuges or args.power or args.distribution or args.surface_power or args.complete_power or args.water or args.furnishings
    stage_name="furnishings" if args.furnishings else "water" if args.water else "complete-power" if args.complete_power else "surface-power" if args.surface_power else "distribution" if args.distribution else "power" if args.power else "refuges" if args.refuges else "doors"
    review=PACKET
    if fitted:
        name={"furnishings":"northwatch-furnishing-pass","water":"northwatch-water-pass","complete-power":"northwatch-complete-power-pass","surface-power":"northwatch-surface-power-pass","distribution":"northwatch-distribution-pass","power":"northwatch-power-pass","refuges":"northwatch-refuge-pass","doors":"northwatch-door-pass"}[stage_name]
        review=PACKET.parents[1]/name/"wld-08"
    meta=json.loads((PACKET/"construction.json").read_bytes()) if fitted else prepare()
    if args.refuges or args.power or args.distribution or args.surface_power or args.complete_power or args.water or args.furnishings:
        stem="station-with-furnishings" if args.furnishings else "station-with-water" if args.water else "station-with-complete-power" if args.complete_power else "station-with-surface-power" if args.surface_power else "station-with-distribution" if args.distribution else "station-with-power" if args.power else "station-with-refuges"
        source=json.loads((review/"construction.json").read_bytes())["exports"]
        meta["glb_sha256"]=source[(review/(stem+".glb")).relative_to(ROOT).as_posix()]
        meta["physics_sha256"]=source[(review/(stem+".hcs2")).relative_to(ROOT).as_posix()]
    build=ROOT/"build/northwatch-boundary-review"
    build.mkdir(parents=True,exist_ok=True)
    java=Path("C:/Program Files/Java/jdk-17/bin")
    jar=ROOT/"build/beckenburg-game.jar"
    gpu=ROOT/"gpu-client/target/BeckenburgGPU.jar"
    tools=ROOT/"tools/java"
    names=("NorthwatchBoundaryReview.java","NorthwatchDoorRuntime.java","NativeSchoolInteriorReview.java","NativeSchoolDoorReview.java","CemeteryRefugeReview.java","NorthwatchPowerReview.java","WharfPowerChecks.java","NorthwatchDistributionReview.java","NorthwatchWaterReview.java","NorthwatchFurnishingReview.java")
    subprocess.run([str(java/"javac.exe"),"-encoding","UTF-8","-cp",str(jar),"-d",str(build),
                    *(str(tools/name) for name in names)],cwd=ROOT,check=True)
    # Compile the shared fixture binding from the checkout over the baseline
    # game package so the staged native review uses the same production code.
    if args.water or args.furnishings:
        game=ROOT/"src/main/java/org/beckenburg"
        subprocess.run([str(java/"javac.exe"),"--release","17","-encoding","UTF-8","-cp",str(jar),"-d",str(build),
                        str(game/"HauntedFixtures.java"),str(game/"InteractionFocus.java"),str(game/"SiteStories.java")],cwd=ROOT,check=True)
    cp=os.pathsep.join(map(str,(build,jar,gpu) if args.gpu else (build,jar)))
    subprocess.run([str(java/"java.exe"),"-Djava.awt.headless=true","-Xmx3g","-cp",cp,
                    "org.beckenburg.NorthwatchBoundaryReview",str(review),meta["glb_sha256"],meta["physics_sha256"],
                    str(ROOT/meta["source_physics"]),meta["source_physics_sha256"],
                    *([str(PACKET),"--"+stage_name] if fitted else []),*(["--gpu"] if args.gpu else [])],cwd=ROOT,check=True)


if __name__ == "__main__":
    main()
