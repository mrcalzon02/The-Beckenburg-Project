"""Test real connections between incident route endpoints inside changed rooms.
Direct joins prove connectivity; missing joins are unresolved, not proof that an
indirect path cannot exist. Invalid original sockets remain explicit.
"""
from pathlib import Path
import collections,csv,hashlib,json,os,subprocess
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'development/levels/rebuild/access-route-pass';V=OUT/'verification'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 manifest=json.loads((OUT/'roster.json').read_text());rows=[];names=[];bindings={};identities={}
 for source in manifest['active_sources']:
  c=ROOT/source['construction'];meta=json.loads(c.read_text())
  if 'access_route_changes' not in meta:continue
  site=source['id'];native=ROOT/source['native'];assert sha(native)==source['native_sha256'];data=json.loads((ROOT/source['layout']).read_text());zones={z['id']:z for z in data['zones']};changed={p['link'] for p in meta['access_route_changes']};rooms={l[k] for l in data['links'] if l['id'] in changed for k in ['a','b']};groups=collections.defaultdict(dict)
  for link in data['links']:
   if link.get('player_access')=='SEALED_NO_ROUTE':continue
   for side,point in [(link['a'],link['points'][0]),(link['b'],link['points'][-1])]:
    if side not in rooms:continue
    assert abs(point[1]-zones[side]['y'])<.021,(site,side,point)
    key=(side,point[1]);groups[key].setdefault(tuple(point),[]).append(link['id'])
  for (room,y),points in groups.items():
   key=site+':'+room+':'+str(y);identities[key]=[dict(point=list(p),links=ls) for p,ls in points.items()]
   rows.append('\t'.join(map(str,[site,native,sha(native),room,y,';'.join(','.join(map(str,p)) for p in points),','.join(map(str,zones[room]['bounds']))])))
  names.extend(site+'\t'+n for n in meta['stair_components']);bindings[site]=dict(source,layout_sha256=sha(ROOT/source['layout']))
 for file,lines in [('room-socket-input.tsv',rows),('room-socket-stairs.tsv',sorted(set(names)))]:(V/file).write_bytes(('\n'.join(lines)+'\n').encode())
 java=Path('C:/Program Files/Java/jdk-17/bin');build=ROOT/'build/room-socket-probe';build.mkdir(exist_ok=True);jar=ROOT/'build/beckenburg-game.jar';sources=[ROOT/'src/main/java/org/beckenburg/AuthoredSitePhysics.java',ROOT/'tools/java/RoomSocketConnectivityProbe.java'];subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(build),*[str(p) for p in sources]],check=True)
 subprocess.run([str(java/'java.exe'),'-Xmx2g','-cp',str(build)+os.pathsep+str(jar),'RoomSocketConnectivityProbe',str(V/'room-socket-input.tsv'),str(V/'room-socket-stairs.tsv'),str(V/'room-socket-results.tsv')],check=True)
 results=list(csv.DictReader((V/'room-socket-results.tsv').open(),delimiter='\t'));report=dict(status='ACTUAL_NATIVE_ROOM_SOCKET_CONNECTIVITY_NOT_GLOBAL_MAP_ACCEPTANCE',groups=results,identities=identities,source_bindings=bindings,source_hashes={p.relative_to(ROOT).as_posix():sha(p) for p in sources+[Path(__file__)]},invalid_socket_groups=[r for r in results if r['valid_nodes']!=r['nodes']],unresolved_multi_component_groups=[r for r in results if int(r['components'])>1],scope='All unique incident route endpoints in rooms touched by adopted routes; strict standing and bidirectional5cm direct production sweeps. Convex room bounds must contain endpoint joins. Multiple components mean no direct connection was proven, not no possible indirect route. Invalid original sockets remain open. No room-name-only connectivity inference.')
 (V/'room-socket-connectivity.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print('ROOM_SOCKET_GROUPS',len(results),'invalid',len(report['invalid_socket_groups']),'unresolved',len(report['unresolved_multi_component_groups']))
if __name__=='__main__':main()
