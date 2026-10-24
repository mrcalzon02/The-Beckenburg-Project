"""Render editable planning JSON as a portable atlas, measured SVGs and OBJ volumes."""
from pathlib import Path
import html
import json
import math
import textwrap

ROOT = Path(__file__).resolve().parent
COLORS = {'inside': '#d0d8e2', 'outside': '#c5d6be', 'circulation': '#b2cbd4', 'hazard': '#e6c2a2', 'staging': '#afcecf'}

def esc(value):
    return html.escape(str(value), quote=True)

def center(zone):
    if 'label_point' in zone:
        return zone['label_point']
    a,b,c,d = zone['bounds']
    return [(a+c)/2, zone['y'], (b+d)/2]

def render_svg(data):
    zones = data['zones']
    ys = sorted(set(z['y'] for z in zones))
    bx = data['site_bounds']
    scale = min(11, 1000/(bx[2]-bx[0]), 610/(bx[3]-bx[1]))
    panel_h = (bx[3]-bx[1])*scale+80
    width = 1280
    key_rows = math.ceil(len(zones)/3)
    height = 120 + len(ys)*panel_h + 60 + key_rows*30
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height:.0f}" viewBox="0 0 {width} {height:.0f}">',
           '<style>text{font-family:Arial,sans-serif;fill:#1c2a37}.small{font-size:12px}.label{font-size:13px;font-weight:bold}.title{font-size:26px;font-weight:bold}</style>',
           f'<rect width="100%" height="100%" fill="#f5f4ef"/><text x="32" y="38" class="title">{esc(data["title"])}</text>',
           f'<text x="32" y="65" font-size="15">{esc(data["id"])} · ROUGH LAYOUT v1 · metres · planning only</text>',
           '<text x="32" y="90" class="small">Cyan: route reservation · amber dashed: elevation change · B/C/A: ritual clear areas · numbered spaces: key below</text>']
    for fi, y in enumerate(ys):
        top = 120+fi*panel_h
        def point(x,z):
            return (70+(x-bx[0])*scale,top+35+(z-bx[1])*scale)
        out.append(f'<text x="32" y="{top+18:.1f}" font-size="18">Standing elevation Y {y:+g} m</text>')
        for x in range(math.ceil(bx[0]/10)*10, math.floor(bx[2]/10)*10+1, 10):
            px,pz = point(x,bx[1])
            out.append(f'<path d="M{px:.1f},{pz:.1f} v{(bx[3]-bx[1])*scale:.1f}" stroke="#dfdfd8"/><text x="{px:.1f}" y="{pz-6:.1f}" class="small">{x}</text>')
        for z in range(math.ceil(bx[1]/10)*10, math.floor(bx[3]/10)*10+1, 10):
            px,pz = point(bx[0],z)
            out.append(f'<path d="M{px:.1f},{pz:.1f} h{(bx[2]-bx[0])*scale:.1f}" stroke="#dfdfd8"/><text x="{px-30:.1f}" y="{pz+4:.1f}" class="small">{z}</text>')
        # Exterior support may intentionally underlie the original building.
        # Draw it first so ground-floor room footprints remain readable.
        for zone in sorted(zones,key=lambda z: {'outside':0,'staging':1,'inside':2,'hazard':2,'circulation':3}[z['kind']]):
            if zone['y'] != y: continue
            a,b,c,d = zone['bounds']; px,pz = point(a,b)
            if zone.get('shape'):
                cx,cz=point((a+c)/2,(b+d)/2);rr=zone['radius']*scale
                inner=zone.get('inner_radius',0)*scale
                out.append(f'<circle cx="{cx:.1f}" cy="{cz:.1f}" r="{rr:.1f}" fill="{COLORS[zone["kind"]]}" stroke="#536477" stroke-width="1.4"/>')
                if inner:out.append(f'<circle cx="{cx:.1f}" cy="{cz:.1f}" r="{inner:.1f}" fill="#f5f4ef" stroke="#812c6b" stroke-width="1.4"><title>Empty shaft — no route or ritual</title></circle>')
            else:
                out.append(f'<rect x="{px:.1f}" y="{pz:.1f}" width="{(c-a)*scale:.1f}" height="{(d-b)*scale:.1f}" fill="{COLORS[zone["kind"]]}" stroke="#536477" stroke-width="1.4"><title>{esc(zone["name"])}; {c-a:g} × {d-b:g} m; Y {y:g}</title></rect>')
        for link in data['links']:
            pts = link['points']
            if not any(p[1] == y for p in pts): continue
            if link['kind'] == 'level' and pts[0][1] != y: continue
            xy = [point(p[0],p[2]) for p in pts]
            path = 'M'+' L'.join(f'{x:.1f},{z:.1f}' for x,z in xy)
            color = '#307e99' if link['kind']=='level' else '#a86516'
            dash = '' if link['kind']=='level' else 'stroke-dasharray="6 4"'
            out.append(f'<path d="{path}" fill="none" stroke="{color}" stroke-width="2.2" {dash}><title>{esc(link["id"])}: {esc(link["note"])}</title></path>')
            if link['kind'] != 'level':
                px,pz = xy[0] if pts[0][1]==y else xy[-1]
                out.append(f'<text x="{px+4:.1f}" y="{pz-5:.1f}" class="small">↕ {esc(link["id"])}</text>')
        for i,zone in enumerate(zones,1):
            if zone['y'] != y: continue
            cx,_,cz = center(zone); px,pz = point(cx,cz)
            out.append(f'<circle cx="{px:.1f}" cy="{pz:.1f}" r="10" fill="#f8f7f1" stroke="#536477"/><text x="{px:.1f}" y="{pz+4:.1f}" text-anchor="middle" class="small">{i}</text>')
        for r in data['rituals']:
            if r['point'][1] != y: continue
            px,pz = point(r['point'][0],r['point'][2]); side=4.4*scale
            out.append(f'<rect x="{px-side/2:.1f}" y="{pz-side/2:.1f}" width="{side:.1f}" height="{side:.1f}" fill="none" stroke="#812c6b" stroke-width="2"/><text x="{px+side/2+3:.1f}" y="{pz:.1f}" class="label">{r["rite"][0]}</text>')
        if data['staging']['point'][1] == y:
            s=data['staging']; px,pz=point(s['point'][0],s['point'][2])
            out.append(f'<text x="{px:.1f}" y="{pz-15:.1f}" class="label">VAN / EXTRACT</text>')
        out.extend([f'<path d="M1150,{top+95:.1f} v-35 l-5,9 m5,-9 l5,9" fill="none" stroke="#283949" stroke-width="2"/><text x="1144" y="{top+52:.1f}" class="label">N</text>',
                    f'<path d="M1080,{top+145:.1f} h{10*scale:.1f}" stroke="#283949" stroke-width="3"/><text x="1080" y="{top+163:.1f}" class="small">10 metres</text>'])
    key_top=120+len(ys)*panel_h+12
    for i,z in enumerate(zones):
        col=i%3; row=i//3; a,b,c,d=z['bounds']
        out.append(f'<text x="{32+col*414}" y="{key_top+row*30:.1f}" class="small">{i+1}. {esc(z["name"])} ({c-a:g}×{d-b:g}; Y{z["y"]:+g})</text>')
    out.append('</svg>')
    return '\n'.join(out)

def render_obj(data):
    out=['# ROUGH SPACE VOLUMES ONLY; NOT A WALKABLE LEVEL. Units: metres; Y up.']
    vi=1
    for z in data['zones']:
        a,b,c,d=z['bounds']; y=z['y']; h=.12 if z['kind'] in ('outside','staging') else z['height']
        out.append('o '+z['id'])
        for yy in (y,y+h):
            for x,zz in ((a,b),(c,b),(c,d),(a,d)):
                out.append(f'v {x:g} {yy:g} {zz:g}')
        for face in ((0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)):
            out.append('f '+' '.join(str(vi+i) for i in face))
        vi+=8
    return '\n'.join(out)+'\n'

def brief(data):
    out=[f'# {data["title"]}', '',f'**{data["id"]} · {data["region"]} · {data["scale"]} · rough layout v1**', '', data['intent'], '',
         '[Measured floor plans](plan.svg) · [PNG preview](plan.png) · [Editable coordinates](layout.json) · [Volume study](blockout.obj)', '',
         'Planning only. Bounds and links are reservations; walkability, shell construction and gameplay remain unverified.', '',
         '## Spaces', '', '| ID | Space | X/Z bounds (m) | Standing Y / height | Function |', '|---|---|---|---|---|']
    for z in data['zones']:
        out.append(f'| {z["id"]} | {z["name"]} | {z["bounds"]} | {z["y"]:g} / {z["height"]:g} m | {z["purpose"]} |')
    out+=['','## Routes and retreat','',data['circulation'],'']
    for loop in data['loops']: out.append('- Loop: '+' → '.join(loop))
    out+=['', 'Every listed loop is an authored graph cycle, not a verified physical pursuit route. Cyan lines require later wall openings, surface support and clearance review.', '',
          '| Link | Zones | Clear width | Route / construction note |','|---|---|---|---|']
    for l in data['links']: out.append(f'| {l["id"]} | {l["a"]} ↔ {l["b"]} | {l["width"]:g} m | {l["note"]} |')
    s=data['staging']
    out+=['','## Arrival and extraction','',f'Shared van placement: X={s["point"][0]:g}, Z={s["point"][2]:g}, yaw={s["yaw"]:g} radians. Ground Y={s["point"][1]:g}. Cargo ramp approach: {s["cargo_route"]}. {s["logistics"]}', '',
          '## Rite reservations','', '| Rite | Zone | Centre X/Y/Z (m) | Installation |','|---|---|---|---|']
    for r in data['rituals']: out.append(f'| {r["rite"]} | {r["zone"]} | {r["point"]} | {r["installation"]} |')
    out+=['','Each reservation is a 4.4 m clear square. All three investigation outcomes remain independent of the narrative. HQ reservations are separated training bays, not live haunting anchors.', '',
          '## Systems and environmental identity','',data['systems'],'','## Campaign use','',data['campaign'],'','## Refinement risks / next pass','']
    out += ['- '+r for r in data['refine']]
    out += ['', '## Source basis','']+['- '+s for s in data['sources']]
    return '\n'.join(out)+'\n'

def main():
    cards=[]; rows=[]
    for path in sorted(ROOT.glob('*/layout.json')):
        d=json.loads(path.read_text(encoding='utf-8')); folder=path.parent
        (folder/'plan.svg').write_text(render_svg(d),encoding='utf-8')
        (folder/'blockout.obj').write_text(render_obj(d),encoding='utf-8')
        (folder/'README.md').write_text(brief(d),encoding='utf-8')
        rel=folder.name
        cards.append(f'<article data-region="{esc(d["region"])}"><h2>{esc(d["title"])}</h2><p>{esc(d["id"])} · {esc(d["region"])} · {len(d["zones"])} spaces</p><p>{esc(d["intent"])}</p><a href="{rel}/plan.svg" target="_blank"><img loading="lazy" src="{rel}/plan.svg" alt="Measured layout for {esc(d["title"])}"/></a><nav><a href="{rel}/plan.svg">Full plan</a><a href="{rel}/README.md">Brief</a><a href="{rel}/layout.json">Coordinates</a><a href="{rel}/blockout.obj">Volume study</a></nav></article>')
        rows.append({'id':d['id'],'title':d['title'],'folder':rel,'region':d['region'],'zone_count':len(d['zones']),'status':'rough-layout-only'})
    (ROOT/'catalog.json').write_text(json.dumps({'version':1,'status':'planning-only','maps':rows},indent=2)+'\n',encoding='utf-8')
    map_list=['# Complete proposed map list', '', 'Planning baseline, 21 October 2026. Every row has a rough measured layout and an individual layout commit. Investigation roster: nine original families + thirteen remote proposals; Castle is a separate headquarters.', '',
      '[Visual atlas](REVIEW.html) · [Contact sheet](ATLAS.png) · [Workflow / refinement gates](WORKFLOW.md)', '',
      '| Location | Region | Layout | Runtime position at intake |','|---|---|---|---|']
    for row in rows:
        status='Existing selectable site; retain current geometry' if row['folder'] in ('house','farmhouse','school') else 'Existing hub; expansion is proposed' if row['folder']=='castle' else 'Locked authored definition; not production accepted' if row['folder']=='church' else 'Locked shared prototype; distinct replacement layout proposed' if row['folder'] in ('mansion','prison','asylum','academy','college') else 'Proposed remote site; no runtime implementation'
        map_list.append(f'| {row["title"]} | {row["region"]} | [{row["zone_count"]} spaces]({row["folder"]}/README.md) | {status} |')
    map_list+=['', 'All layouts remain rough planning. A space count is a reservation count, not implemented room or finished-content credit. Mission selection, campaign state and shipping-stage status remain governed by existing runtime code and the stage ledger.', '',
      'Next: review this complete roster, then refine the House construction reference. Refine remote exemplars in order Blackpine → Hollow Creek → Red Mesa. Each map brief records its own unresolved geometry, reference and fairness questions.']
    (ROOT/'MAP_LIST.md').write_text('\n'.join(map_list)+'\n',encoding='utf-8')
    (ROOT/'REVIEW.html').write_text('''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Beckenburg — rough map atlas</title><style>
*{box-sizing:border-box}body{margin:0;background:#121c24;color:#e6e9e9;font:16px/1.55 system-ui,sans-serif}header{max-width:1450px;margin:auto;padding:40px 28px 22px}h1{font-size:38px;line-height:1.15}header p{max-width:960px;color:#bac7cc}input{width:100%;max-width:620px;padding:14px;font:inherit;border:1px solid #5b737f;border-radius:6px;background:#243541;color:white}main{max-width:1450px;margin:auto;padding:20px 28px;display:grid;grid-template-columns:repeat(auto-fit,minmax(430px,1fr));gap:24px}article{background:#21313d;border:1px solid #3e5664;border-radius:10px;overflow:hidden;padding:24px}article h2{margin:0;font-size:25px}article p{color:#c9d2d6}article img{width:100%;max-height:480px;object-fit:contain;background:#f5f4ef;border-radius:4px}nav{display:flex;gap:18px;flex-wrap:wrap;padding-top:12px}a{color:#a8dfe8}a:focus-visible,input:focus-visible{outline:3px solid #ffd17d;outline-offset:4px}footer{padding:30px;text-align:center;color:#aebcc4}@media(max-width:520px){main{grid-template-columns:1fr;padding:12px}header{padding:24px}h1{font-size:30px}article{padding:16px}}
</style><header><p>THE BECKENBURG PROJECT / 21 OCTOBER 2026</p><h1>Rough map layout atlas</h1><p>''' + str(len(cards)) + ''' measured planning packets. Nine initial investigation families, thirteen remote proposals, one headquarters. Layouts and volume studies remain unverified for runtime traversal. Select a full plan to inspect every floor at readable scale.</p><label for="search">Find a location or region</label><br><input id="search" type="search" placeholder="House, Northern Woodlands, mine…"><p id="count" role="status"></p></header><main>'''+'\n'.join(cards)+'''</main><footer>Editable JSON → measured SVG + brief + OBJ volume study. No playable-site unlocks are implied.</footer><script>const input=document.querySelector('#search'),cards=[...document.querySelectorAll('article')];function filter(){let n=0;cards.forEach(c=>{c.hidden=!c.textContent.toLowerCase().includes(input.value.toLowerCase());if(!c.hidden)n++});document.querySelector('#count').textContent=n+' of '+cards.length+' layouts'}input.addEventListener('input',filter);filter()</script></html>''',encoding='utf-8')
    print(f'Rebuilt {len(cards)} planning packets and atlas.')

if __name__=='__main__': main()
