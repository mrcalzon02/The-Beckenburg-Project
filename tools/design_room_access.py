"""Native room-access alternatives for currently incomplete source links.
Keep stage/van anchors, actual doorway coordinates, widths, room bounds and
previously passing routes. Proposals are not adopted before independent checks.
"""
from pathlib import Path
import csv,hashlib,json,math,os,subprocess
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild';OUT=BASE/'room-access-pass';BUILD=ROOT/'build/room-access-probe'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(d,indent=2)+'\n').encode())
def main():
 manifest={r['id']:r for r in json.loads((BASE/'flat-terrain-pass/roster.json').read_text(encoding='utf-8'))['active_sources']};work=json.loads((BASE/'flat-terrain-pass/verification/remaining-routes.json').read_text(encoding='utf-8'))['links'];inputs=[];sockets=[];names=set();v=OUT/'verification';v.mkdir(parents=True,exist_ok=True);BUILD.mkdir(parents=True,exist_ok=True);source_bindings={}
 def serial(ps):return ';'.join(','.join(map(str,p)) for p in ps)
 for item in work:
  site=item['site'];source=manifest[site];data=json.loads((ROOT/source['layout']).read_text(encoding='utf-8'));meta=json.loads((ROOT/source['construction']).read_text(encoding='utf-8'));link=next(l for l in data['links'] if l['id']==item['link']);zones={z['id']:z for z in data['zones']};native=ROOT/source['native'];assert sha(native)==source['native_sha256'];names.update(site+'\t'+n for n in meta['stair_components']);source_bindings[site]=dict(source,contract_sha256=sha(ROOT/source['construction']),layout_sha256=sha(ROOT/source['layout']))
  anchors=[link['points'][0]]
  if link.get('threshold') is not None and link['threshold'] not in [link['points'][0],link['points'][-1]]:anchors.append(link['threshold'])
  anchors.append(link['points'][-1]);sealed=[[*z['bounds'],z['y'],z['y']+z['height']] for z in data['zones'] if z.get('player_access')=='SEALED_CONTEXT'];all_candidates=[]
  for side,old in [(link['a'],anchors[0]),(link['b'],anchors[-1])]:
   zone=zones[side];a,b,c,d=zone['bounds'];points=[]
   if zone['kind']!='staging':
    for other in data['links']:
     if other.get('player_access')=='SEALED_NO_ROUTE':continue
     for key,point in [(other['a'],other['points'][0]),(other['b'],other['points'][-1])]:
      if key==side and abs(point[1]-old[1])<.01:points.append(point)
    for ix in range(math.ceil((a+.35)*2),math.floor((c-.35)*2)+1):
     for iz in range(math.ceil((b+.35)*2),math.floor((d-.35)*2)+1):
      p=[ix/2,old[1],iz/2]
      if math.hypot(p[0]-old[0],p[2]-old[2])>3:continue
      if zone['kind']=='outside' and any(z['kind']=='inside' and abs(z['y']-old[1])<.01 and z['bounds'][0]-.1<p[0]<z['bounds'][2]+.1 and z['bounds'][1]-.1<p[2]<z['bounds'][3]+.1 for z in data['zones']):continue
      points.append(p)
   points=[list(p) for p in sorted(set(tuple(p) for p in points),key=lambda p:(math.hypot(p[0]-old[0],p[2]-old[2]),p)) if a+.25<=p[0]<=c-.25 and b+.25<=p[2]<=d-.25];all_candidates.append(points)
  a,b,c,d=data['site_bounds'];box=[max(a,min(p[0] for p in link['points'])-6),max(b,min(p[2] for p in link['points'])-6),min(c,max(p[0] for p in link['points'])+6),min(d,max(p[2] for p in link['points'])+6)];inputs.append('\t'.join(map(str,[site,native,source['native_sha256'],link['id'],link['width'],serial(anchors),';'.join(','.join(map(str,r)) for r in sealed),','.join(map(str,box))])));sockets.append('\t'.join([site,link['id'],serial(all_candidates[0]),serial(all_candidates[1])]))
 for name,rows in [('input.tsv',inputs),('socket-candidates.tsv',sockets),('stair-names.tsv',sorted(names))]:(v/name).write_bytes(('\n'.join(rows)+'\n').encode())
 write(v/'source-bindings.json',source_bindings);java=Path('C:/Program Files/Java/jdk-17/bin');jar=ROOT/'build/beckenburg-game.jar';sources=[ROOT/'src/main/java/org/beckenburg/AuthoredSitePhysics.java',ROOT/'tools/java/FlatRouteDesigner.java',ROOT/'tools/java/RoomAccessRouteDesigner.java'];subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(BUILD),*[str(p) for p in sources]],check=True)
 with (BUILD/'design.log').open('wb') as log:subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-Xmx2g','-cp',str(BUILD)+os.pathsep+str(jar),'RoomAccessRouteDesigner',str(v/'input.tsv'),str(v/'stair-names.tsv'),str(v/'socket-candidates.tsv'),str(v/'results.tsv')],stdout=log,stderr=subprocess.STDOUT,check=True)
 rows=list(csv.DictReader((v/'results.tsv').open(),delimiter='\t'));proposals=[]
 for row in rows:
  if row['status']!='REROUTED_ROOM_ACCESS':continue
  source=manifest[row['site']];data=json.loads((ROOT/source['layout']).read_text(encoding='utf-8'));link=next(l for l in data['links'] if l['id']==row['link']);points=[[float(v) for v in p.split(',')] for p in row['points'].split(';')];assert 'threshold' not in link or link['threshold'] in points;proposals.append(dict(site=row['site'],link=row['link'],rooms=[link['a'],link['b']],before=link['points'],after=points,width=link['width'],threshold=link.get('threshold'),source=source))
 write(v/'summary.json',dict(status='ACTUAL_NATIVE_ACCESS_SOCKET_PROPOSALS_NOT_YET_ADOPTED',tested_links=len(rows),proposals=proposals,decisions=rows,source_hashes={p.relative_to(ROOT).as_posix():sha(p) for p in sources+[Path(__file__)]},scope='Original geometry/physics/doorways/widths unchanged; local access sockets may move within their own room. Staging, source functions and passing routes retained. Full independent native/source graph checks required before adoption.'));print('ROOM_ACCESS_PROPOSALS',len(proposals),'of',len(rows),flush=True)
if __name__=='__main__':main()
