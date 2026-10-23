"""Original metre-scale NPC proxy. NOT approved character master art or animation."""
import bpy,sys,json,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'development/assets'),str(ROOT/'development/levels')]
import rebuild_support as s
import build_lived_assets as b
m=b.init();m['skin']=s.material('Rook weathered skin',(.51,.37,.27),'leather');m['silver']=s.material('Rook silver hair',(.55,.54,.46),'cloth')
p=[]
for x in [-.11,.11]:
    p.append(s.box('Worn boot',(x,-.055,.055),(.16,.28,.11),m['black'],.015))
    p.append(s.rod('Trouser leg',(x,0,.13),(x,0,.72),.075,m['iron'],7))
p.append(s.lathe('Long charcoal wool coat',[(.38,.24),(.76,.22),(1.18,.20),(1.27,.15)],m['iron'],8))
p.append(s.box('Moss waistcoat',(0,-.19,1.04),(.22,.035,.34),m['green']))
for z in [.94,1.03,1.12]:p.append(s.rod('Waistcoat button',(.02,-.212,z),(.02,-.218,z),.009,m['brass'],6))
for x,side in [(-.23,-1),(.23,1)]:
    p+=s.tube('Coat sleeve',[(x,0,1.22),(x+side*.06,-.03,1.01),(x+side*.08,-.13,.94)],.065,m['iron'],7)
    p.append(s.rod('Ink stained hand',(x+side*.08,-.13,.94),(x+side*.08,-.18,.90),.042,m['skin'],7))
p.append(s.rod('Neck',(0,0,1.25),(0,0,1.40),.055,m['skin'],8))
p.append(s.lathe('Weathered human head',[(1.35,.065),(1.40,.105),(1.54,.115),(1.63,.09),(1.66,.045)],m['skin'],9))
p.append(s.lathe('Silver hair crown',[(1.59,.115),(1.65,.10),(1.69,.03)],m['silver'],9))
p.append(s.mesh('White beard', [(-.10,-.085,1.48),(.10,-.085,1.48),(.07,-.115,1.32),(0,-.15,1.25),(-.07,-.115,1.32)],[(0,1,2,3,4)],m['silver']))
p.append(s.rod('Human nose',(0,-.10,1.50),(0,-.15,1.48),.021,m['skin'],6))
for x in [-.052,.052]:
    p+=b.ring('Repaired wire spectacle lens',(x,-.108,1.535),.041,.003,m['brass'],segments=12)
    p.append(s.rod('Eye',(x,-.11,1.535),(x,-.115,1.535),.009,m['black'],6))
p.append(s.rod('Spectacle bridge',(-.012,-.112,1.535),(.012,-.112,1.535),.003,m['brass'],4))
p+=s.tube('Worn scarf',[(-.1,-.04,1.31),(0,-.13,1.29),(.13,-.09,1.30),(.11,-.21,.97)],.036,m['cloth'],6)
p+=s.tube('Ash walking cane',[(.32,-.19,.025),(.32,-.19,.88),(.28,-.19,.94)],.015,m['oak'],7)
b.export('level_npc_matthis_rook_proxy',p,'npc-prototype',view=(2,-4,1.9))
entry=b.LOG[0];entry['art_status']='Original low-poly proxy, NOT approved master art or finished character animation'
(ROOT/'development/levels/lorekeeper-proxy.json').write_text(json.dumps(entry,indent=2)+'\n')
print('LOREKEEPER_PROXY_COMPLETE',flush=True)
