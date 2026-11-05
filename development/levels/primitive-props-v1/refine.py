#!/usr/bin/env python3
"""Independent, deterministic source-only refinement models for map reviews.

Uses the established GLB/UV exporter, never the live model registry or scenes.
All geometry is constructed in meters, Y up, with the support plane at Y=0.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import math
from pathlib import Path
import build
import fit
import kits

ROOT = Path(__file__).resolve().parent
OUT = ROOT / 'refinement'


def box(x, y, z, w, h, d, mat='wood', yaw=0, roll=0):
    q = {'type': 'box', 'p': [x,y,z], 'size': [w,h,d], 'material': mat}
    if yaw: q['yaw'] = yaw
    if roll: q['roll'] = roll
    return q


def cyl(x, y, z, r, h, mat='metal', axis='y', seg=10):
    return {'type':'cylinder', 'p':[x,y,z], 'radius':r, 'height':h,
            'material':mat, 'axis':axis, 'segments':seg}


def taper(x, y, z, rb, rt, h, mat='stone', seg=10):
    return {'type':'frustum', 'p':[x,y,z], 'bottom_radius':rb,
            'top_radius':rt, 'height':h, 'material':mat, 'segments':seg}


def model(short, label, maps, mount, parts):
    return {'id':'level_blockout_refined_'+short, 'label':label,
            'maps':maps, 'mount':mount, 'runtime_approved':False,
            'review_only':True, 'parts':parts}


def sources():
    entries=[]
    # Open four-point masonry framing; no full-rectangle crypt backplate.
    p=[box(-.70, .12, 0, .33,.24,.74,'stone'),
       box(.70, .12, 0, .33,.24,.74,'stone')]
    for x in (-.69,.69):
        p += [box(x,1.10,0,.24,1.84,.53,'stone'),
              box(x,2.12,0,.37,.23,.70,'stone'),
              box(x,2.43,0,.28,.44,.55,'stone',roll=(-.30 if x<0 else .30))]
    p += [box(0,2.67,0,.75,.24,.58,'stone'),
          box(0,2.85,0,1.77,.14,.70,'ivory')]
    entries.append(model('castle_open_crypt_niche','Open masonry crypt niche',['CASTLE'],'opening',p))

    # Load-bearing bier, not a second (uncounted) sarcophagus.
    p=[box(0,.13,0,2.16,.26,1.02,'stone'), box(0,.43,0,2.06,.13,.92,'ivory')]
    for x in (-.82,.82):
        for z in (-.35,.35):
            p += [box(x,.28,z,.24,.27,.24,'stone')]
    for x in (-.9,.9): p += [box(x,.50,0,.10,.10,1.02,'gilt')]
    entries.append(model('castle_sarcophagus_bier','Counted tomb support bier',['CASTLE'],'floor',p))

    # Separate iron arms and candle bodies, no runtime light emitted.
    p=[taper(0,.09,0,.30,.21,.18,'bronze'),cyl(0,.78,0,.055,1.35,'bronze'),
       box(0,1.46,0,1.05,.07,.09,'bronze')]
    for x in (-.47,-.24,0,.24,.47):
        p += [cyl(x,1.54,0,.07,.08,'bronze'),cyl(x,1.72,0,.035,.29,'wax'),
              taper(x,1.89,0,.035,0,.055,'accent',6)]
    entries.append(model('castle_candelabrum','Five-candle iron candelabrum',['CASTLE','CHURCH','WLD-10'],'floor',p))

    # Iron body and actual freestanding chimney, not a photo plane.
    p=[box(0,.63,0,.89,.84,.69,'dark'),box(0,.16,.03,.96,.15,.72,'metal'),
       box(0,.96,-.365,.70,.45,.035,'metal')]
    for x in (-.34,.34):
        for z in (-.25,.25):p.append(box(x,.095,z,.095,.19,.095,'metal'))
    p += [box(0,.67,-.36,.51,.44,.042,'metal'),
          box(0,.67,-.392,.39,.32,.018,'dark'),
          cyl(.19,.67,-.41,.035,.055,'gilt','z'),
          cyl(.24,1.39,.18,.105,.75,'metal'),
          box(0,.19,-.43,.53,.03,.18,'metal')]
    entries.append(model('house_iron_stove','Cast-iron house heating stove',['HOUSE','FARMHOUSE','WLD-14'],'floor',p))

    p=[]
    for x in (-.72,.72):
        p += [box(x,.05,0,.16,.10,.70,'wood'),box(x,.96,0,.11,1.84,.12,'wood')]
    p += [box(0,1.75,0,1.57,.10,.11,'wood'),box(0,.48,.02,1.49,.07,.12,'wood')]
    for x in (-.56,-.28,0,.28,.56):
        p += [cyl(x,1.64,-.085,.026,.19,'metal'),
              box(x,1.54,-.17,.08,.035,.20,'metal')]
    entries.append(model('school_coat_rail','Free-standing five-hook coat rack',['SCHOOL','ACADEMY'],'floor',p))

    # Four wheels have contact at Y=0, leaving floor clearance.
    p=[]
    for x in (-.51,.51):
        for z in (-.31,.31):
            p += [cyl(x,.085,z,.085,.08,'dark','x'),
                  box(x,.54,z,.035,.83,.035,'metal')]
    for h in (.36,.86):
        p += [box(0,h,0,1.15,.045,.76,'silver')]
        for z in (-.36,.36):p += [box(0,h+.055,z,1.15,.08,.025,'metal')]
    p += [box(.59,.94,0,.035,.035,.59,'metal')]
    entries.append(model('mortuary_instrument_cart','Castored mortuary instrument cart',['ASYLUM','WLD-07-MORTUARY'],'floor',p))

    p=[box(0,.10,0,.47,.20,.49,'stone'),
       cyl(0,1.75,0,.085,3.25,'metal'),
       box(-.46,2.74,0,1.05,.14,.09,'redcloth'),
       cyl(-.06,2.74,-.08,.12,.12,'dark','z'),
       box(.44,1.36,0,.08,.80,.12,'metal'),
       box(.42,.91,0,.31,.26,.13,'metal'),
       cyl(.17,2.34,.08,.12,.18,'glass','z')]
    entries.append(model('rail_mechanical_semaphore','Mechanical rail semaphore with counterweight',['WLD-12'],'floor',p))

    # Separate bands/braces leave the lens visible between ribs.
    p=[box(0,.10,0,1.22,.20,1.22,'stone'),
       cyl(0,.35,0,.23,.32,'bronze'),
       cyl(0,.68,0,.42,.06,'bronze',seg=12)]
    for y in (.80,1.02,1.24,1.46):
        for a in range(12):
            t=2*math.pi*a/12
            p.append(box(.43*math.cos(t),y,.43*math.sin(t),.07,.045,.18,
                         'glass',yaw=t))
    for a in range(8):
        t=2*math.pi*a/8
        p.append(cyl(.43*math.cos(t),1.18,.43*math.sin(t),.026,1.02,'bronze'))
    p += [cyl(0,1.64,0,.49,.07,'bronze',seg=12),
          taper(0,1.82,0,.34,.08,.30,'bronze',12)]
    entries.append(model('lighthouse_fresnel_cage','Open-braced lighthouse optic assembly',['WLD-14'],'floor',p))

    # Wheel-floor alignment at 0; the tub has hollow center.
    p=[]
    for x in (-.72,.72):
        for z in (-.42,.42):p.append(cyl(x,.17,z,.17,.09,'metal','z'))
    p += [box(0,.38,0,1.62,.13,.88,'wood'),
          box(0,.84,-.45,1.68,.74,.08,'metal'),
          box(0,.84,.45,1.68,.74,.08,'metal'),
          box(-.81,.84,0,.08,.74,.90,'metal'),
          box(.81,.84,0,.08,.74,.90,'metal'),
          box(0,.50,0,1.54,.05,.83,'dark'),
          box(0,.26,0,1.42,.12,.13,'metal')]
    entries.append(model('mine_open_ore_cart','Four-wheel open-top ore trolley',['WLD-05'],'floor',p))

    p=[box(0,.64,0,2.18,.10,.93,'wood')]
    for x in (-.93,.93):
        for z in (-.38,.38):p.append(box(x,.30,z,.095,.60,.095,'metal'))
    p += [cyl(0,.98,0,.34,.04,'metal','z',16),
          cyl(0,.98,-.055,.09,.10,'dark','z'),
          box(0,1.36,0,.90,.08,.13,'metal'),
          box(-.55,.79,0,.12,.20,.09,'industrial'),
          box(.55,.79,0,.12,.20,.09,'industrial')]
    for n in range(12):
        t=2*math.pi*n/12
        p.append(box(.35*math.sin(t),.98+.35*math.cos(t),0,.09,.065,.035,
                     'silver',roll=-t))
    entries.append(model('sawmill_guarded_blade_table','Guarded low-poly mill saw station',['WLD-01'],'floor',p))

    p=[]
    for x in (-1.03,1.03):
        p += [box(x,1.51,0,.09,3.02,.10,'metal'),
              box(x,.11,0,.28,.22,.32,'stone')]
    p += [box(0,2.92,0,2.12,.10,.13,'industrial'),
          cyl(0,2.79,0,.05,2.03,'metal','x')]
    for x in (-.79,-.47,-.15,.17,.49,.81):
        p += [cyl(x,2.62,0,.026,.28,'metal'),
              box(x,2.47,-.05,.07,.04,.12,'metal'),
              box(x,2.40,-.105,.035,.12,.04,'metal')]
    entries.append(model('cannery_suspended_hooks','Freestanding fish processing hook rail',['WLD-13'],'floor',p))

    p=[box(0,.48,0,2.35,.95,.45,'stone'),
       box(0,1.70,0,2.33,1.53,.13,'metal')]
    for x in [-1.00,-.75,-.50,-.25,0,.25,.50,.75,1.00]:
        p.append(cyl(x,1.73,-.09,.023,1.33,'metal'))
    p += [box(0,2.50,0,2.36,.10,.24,'metal'),
          box(0,.98,-.30,2.35,.06,.54,'wood')]
    entries.append(model('prison_visitation_grille','Visitation grille and pass-through counter',['PRISON'],'floor',p))

    p=[box(0,.76,0,1.29,.09,.66,'wood'),
       box(0,.39,0,1.23,.08,.58,'wood'),
       cyl(0,.86,0,.28,.13,'ceramic'),
       cyl(0,.97,.21,.026,.16,'metal'),
       box(0,1.04,.14,.04,.04,.20,'metal')]
    for x in (-.53,.53):
        for z in (-.24,.24):p.append(box(x,.36,z,.08,.72,.08,'wood'))
    entries.append(model('camp_timber_washstand','Standalone campground washstand',['WLD-02'],'floor',p))

    p=[cyl(0,.055,0,.32,.11,'metal'),
       cyl(0,.30,0,.16,.42,'industrial'),
       cyl(0,.56,0,.12,.12,'metal','x')]
    for x in (-.25,.25):
        p += [cyl(x,.55,0,.11,.10,'metal')]
    for x,z in [(-.23,-.20),(-.23,.20),(.23,-.20),(.23,.20)]:
        p.append(cyl(x,.12,z,.025,.035,'silver'))
    entries.append(model('wharf_mooring_bollard','Deck-bolted dual-horn mooring bollard',['WLD-06','WLD-15'],'floor',p))

    p=[]
    for x in (-.64,.64):
        for z in (-.44,.44):p.append(box(x,.61,z,.075,1.22,.075,'industrial'))
    p += [box(0,1.25,0,1.41,.10,1.01,'metal'),
          box(0,.07,0,1.41,.14,1.01,'metal')]
    for x in (-.51,-.34,-.17,0,.17,.34,.51):
        p += [box(x,.70,-.47,.055,1.08,.035,'metal'),
              box(x,.70,.47,.055,1.08,.035,'metal')]
    entries.append(model('battery_vent_cage','Open-slat concrete-bunker vent cage',['WLD-17'],'structure',p))

    return entries


def produce():
    entries=sources()
    if len(entries)!=15 or len({m['id'] for m in entries})!=len(entries):
        raise ValueError('Expected 15 unique source refinement models')
    outputs={}
    records=[]
    for m in entries:
        bounds, low, high = fit.footprint(m,0,0,0)
        if abs(low)>.025 or high>4.1 or bounds[2]-bounds[0]>5.8 or bounds[3]-bounds[1]>5.8:
            raise ValueError('Unfit grounded source geometry '+m['id']+' '+str((low,high,bounds)))
        blob, triangles=build.glb(m)
        if triangles>3500:raise ValueError('Excess triangle count '+m['id'])
        filename=m['id']+'.glb'
        outputs[OUT/filename]=blob
        records.append({'id':m['id'], 'label':m['label'], 'maps':m['maps'],
                        'mount':m['mount'], 'source_parts':len(m['parts']),
                        'source_bounds_m': [round(x,4) for x in (*bounds,low,high)],
                        'triangles':triangles,'bytes':len(blob),
                        'sha256':hashlib.sha256(blob).hexdigest(),
                        'runtime_approved':False})
    pages=[]
    for k in range(0,len(entries),6):
        blob, info=kits.make_page({'id':'REFINED','title':'Source refinement review'},
                                  entries[k:k+6],k//6)
        outputs[OUT/info['file']]=blob
        pages.append(info)
    manifest={'schema':'beckenburg.primitive-refinements/1',
              'status':'source_only_review_not_production_approval',
              'runtime_approved':False,'model_count':len(entries),
              'map_associations_only':True,'review_pages':pages,'models':records}
    outputs[OUT/'index.json']=(json.dumps(manifest,indent=2)+'\n').encode()
    return outputs


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--check',action='store_true')
    opts=parser.parse_args()
    outputs=produce()
    if not opts.check:
        OUT.mkdir(exist_ok=True)
        for old in OUT.glob('*.glb'):
            if old not in outputs:old.unlink()
    for path,blob in outputs.items():
        if opts.check:
            if not path.exists() or path.read_bytes()!=blob:
                raise SystemExit('Missing/stale refinement: '+str(path))
        else:path.write_bytes(blob)
    print('PASS',15,'grounded independently selectable source refinements,',
          3,'contact rooms; mode',('check' if opts.check else 'generate'))


if __name__=='__main__':main()
