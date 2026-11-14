"""Finish physical convergences, level wall entries and full-footprint flats.
Uses current source candidates, preserving all unrelated native/GLB records.
"""
from pathlib import Path
import json,sys
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import build_stair_bays as builder
from review_roster_macroterrain import sha
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'stair-connection-pass'
PLANS={
 'WLD-04':dict(points={
  'L04':[[28,3,36],[29.5,6,25.5],[30,6,22]],
  'L11':[[43,3,51],[31.57,6,25.5],[30,6,22]],
  'L06':[[12,6,20],[12,6,28],[20,6,28],[28,3,36]]},build=['L04','L11','L06'],shared={},reason='Finish both hillside stair heads before convergence at a common level landing; enter the hut through an actual floor-level opening instead of climbing through its wall.'),
 'WLD-05':dict(points={
  'L09':[[30,0,52],[28.3,0,52],[28.3,0,49],[28,-6,31]],
  'L17':[[11,-6,52],[26.5,0,52],[30,0,52]]},build=['L09','L17'],shared={},hub_landings=[dict(link='L09',elevation=0,polygon=[[27.3,50],[31.5,50],[31.5,53],[27.3,53]])],reason='Separate the two sloping columns before a common level adit hub; finish both rises before the return routes meet.'),
 'WLD-15':dict(points={'route-17':[[26,0,46],[23,0,46],[23,3,36],[26,3,36]]},build=[],retain_flights=['route-17'],shared={},reason='Add closed full-width low/high approach and endpoint support while retaining the existing passing physical flight.'),
 'WLD-17':dict(points={'route-02':[[32,0,72],[32,0,68],[32,2,58]],'route-03':[[32,2,58],[40,2,56],[40,4,48]]},build=[],retain_flights=['route-02','route-03'],shared={},hub_landings=[dict(link='route-02',elevation=0,polygon=[[31,71.5],[33,71.5],[33,75.5],[31,75.5]])],reason='Support the complete turning approach footprint, not only its centreline; retain the existing passing flight.'),
 'WLD-18':dict(points={'route-13':[[36,0,48],[52,0,48],[52,0,47.2],[52,-4,40.5],[52,-4,34],[48,-4,34]]},build=[],retain_flights=['route-13'],shared={},reason='Close the lower exhibit entry/endpoint support around the preserved floor-height wall aperture and existing stair.')}
def main(sites=None):
 builder.OUT=OUT
 for site,plan in PLANS.items():
  if sites is not None and site not in sites:continue
  candidates=[(BASE/'stair-terrain-pass/assemblies'/site.lower(),'benched-terrain'),(BASE/'stair-bay-pass'/site.lower(),'bayed-stairs'),(BASE/'stair-opening-pass'/site.lower(),'opened-stairs'),(BASE/'stair-landing-pass'/site.lower(),'supported-stairs'),(BASE/'floor-closure-pass'/site.lower(),'closed-floors')]
  builder.build(site,plan,candidates,'connected-stairs')
  p=OUT/site.lower()/'construction.json';d=json.loads(p.read_text());d['status']='PHYSICAL_STAIR_CONVERGENCE_AND_APPROACH_CANDIDATE_NOT_ACCEPTED';d['plan_author_sha256']=sha(Path(__file__));p.write_bytes((json.dumps(d,indent=2)+'\n').encode())
if __name__=='__main__':main(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else None)
