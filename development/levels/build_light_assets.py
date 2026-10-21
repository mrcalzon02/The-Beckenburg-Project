"""Original CC0 ceiling lamps with connected supports and explicit emitter coordinates."""
import bpy,sys,math,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/assets'),str(ROOT/'development/levels')]
import rebuild_support as s
import build_lived_assets as b

def bare():
 m=b.init();glass=s.material('Original lamp glass',(.77,.72,.54),'ceramic');p=[s.lathe('Ceiling fixing rose',[(-.03,.07),(0,.09)],m['iron'],16),s.rod('Hanging lamp cable',(0,0,-.02),(0,0,-.39),.007,m['black'],8),s.lathe('Pendant bulb socket',[(-.47,.035),(-.39,.029)],m['brass'],12),s.lathe('Real bulb envelope',[(-.62,.012),(-.61,.045),(-.57,.067),(-.52,.055),(-.48,.026),(-.47,.026)],glass,16)]
 b.export('level_light_bare_bulb',p,'household',view=(1,-2,.3))

def chandelier():
 m=b.init();flame=s.material('Original candle flame',(.90,.62,.22),'paint');p=[s.lathe('Chandelier ceiling plate',[(-.035,.12),(0,.14)],m['brass'],16),s.rod('Suspension stem',(0,0,-.03),(0,0,-.79),.018,m['brass'],10),s.lathe('Turned central hub',[(-.88,.035),(-.84,.08),(-.78,.10),(-.73,.055)],m['brass'],16)]
 for i in range(6):
  a=i*math.tau/6;x=.40*math.cos(a);y=.40*math.sin(a)
  p+=s.tube('Attached sweeping chandelier arm',[(0,0,-.81),(x*.5,y*.5,-.89),(x,y,-.86),(x,y,-.74)],.015,m['brass'],8)
  p.append(s.lathe('Wax-catching candle saucer',[(-.76,.026),(-.74,.085),(-.72,.080)],m['brass'],16,(x,y,0)))
  p.append(s.lathe('Uneven wax candle',[(-.73,.031),(-.55,.030),(-.53,.021)],m['ivory'],12,(x,y,0)))
  p.append(s.rod('Connected candle wick',(x,y,-.545),(x,y,-.52),.002,m['black'],5));p.append(s.lathe('Small candle flame',[(-.53,.005),(-.515,.013),(-.485,.001)],flame,8,(x,y,0)))
 b.export('level_light_candle_chandelier',p,'household',view=(2,-3,.25))

bare();chandelier();path=ROOT/'development/levels/lived-assets.json';d=json.loads(path.read_text());ids={r['id'] for r in b.LOG};d['assets']=[r for r in d['assets'] if r['id'] not in ids]+b.LOG;path.write_text(json.dumps(d,indent=2)+'\n')
