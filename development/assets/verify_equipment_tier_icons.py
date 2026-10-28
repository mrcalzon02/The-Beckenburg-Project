from PIL import Image, ImageDraw
from pathlib import Path
import json
root=Path('assets/runtime/ui/item-tier-icons'); rows=json.loads((root/'manifest.json').read_text()); out=Path('development/levels/previews/equipment-menu');out.mkdir(parents=True,exist_ok=True)
qa=[]
for start in range(0,len(rows),18):
 page=rows[start:start+18];sheet=Image.new('RGB',(960,6*185),(25,32,42));draw=ImageDraw.Draw(sheet)
 for i,row in enumerate(page):
  image=Image.open(root/row['file']);bbox=image.getchannel('A').getbbox();fill=max(bbox[2]-bbox[0],bbox[3]-bbox[1])/512
  assert image.size==(512,512) and .78<fill<.96,(row['file'],fill)
  qa.append(dict(file=row['file'],bounds=bbox,fill=round(fill,3)))
  image.thumbnail((160,155));x=(i%6)*160;y=(i//6)*185;sheet.paste(image,(x,y),image);draw.text((x+4,y+156),row['id'].lower()[:22],fill=(235,225,204));draw.text((x+4,y+170),'Tier '+str(row['tier']),fill=(175,213,188))
 sheet.crop((0,0,960,3*185)).save(out/f'tier-icons-{start//18+1:02}.png')
(out/'icon-framing.json').write_text(json.dumps(qa,indent=2)+'\n');print('PASS',len(qa),'singular 512px Blender renders, object extent78-96%')
