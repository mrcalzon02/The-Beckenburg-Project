/* Optional raster previews. Use a Node environment with the sharp package. */
const fs = require('fs');
const path = require('path');
const sharp = require('sharp');
const root = __dirname;
const esc = s => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
async function main() {
  const entries=JSON.parse(fs.readFileSync(path.join(root,'catalog.json'),'utf8')).maps;
  const layers=[];
  for(let i=0;i<entries.length;i++){
    const entry=entries[i], folder=path.join(root,entry.folder);
    const data=JSON.parse(fs.readFileSync(path.join(folder,'layout.json'),'utf8'));
    const png=await sharp(path.join(folder,'plan.svg')).png().toBuffer();
    fs.writeFileSync(path.join(folder,'plan.png'),png);
    const b=data.site_bounds;
    const scale=Math.min(11,1000/(b[2]-b[0]),610/(b[3]-b[1]));
    const ph=(b[3]-b[1])*scale+80;
    const ys=[...new Set(data.zones.map(z=>z.y))].sort((a,b)=>a-b);
    // Prefer the main investigation level over an otherwise empty arrival pad.
    const score=y=>data.zones.filter(z=>z.y===y&&z.kind!=='staging').length;
    const floor=ys.reduce((best,y,i)=>score(y)>score(ys[best])?i:best,ys.indexOf(0));
    const floorLabel=`Y ${ys[floor]>=0?'+':''}${ys[floor]} m plan`;
    const thumb=await sharp(png).extract({left:0,top:Math.round(120+floor*ph),width:1280,height:Math.floor(ph)})
      .resize(460,274,{fit:'contain',background:'#f5f4ef'}).png().toBuffer();
    const left=(i%4)*480,top=Math.floor(i/4)*360+100;
    const lines=entry.title.length>33?[entry.title.slice(0,entry.title.lastIndexOf(' ',33)),entry.title.slice(entry.title.lastIndexOf(' ',33)+1)]:[entry.title];
    const label=Buffer.from(`<svg width="480" height="72"><rect width="480" height="72" fill="#21313d"/>${lines.map((line,j)=>`<text x="12" y="${23+j*21}" fill="#eef2f3" font-family="Arial" font-size="18">${esc(line)}</text>`).join('')}<text x="12" y="65" fill="#b9d2db" font-family="Arial" font-size="12">${esc(entry.id)} · ${esc(entry.region)} · ${floorLabel}</text></svg>`);
    layers.push({input:label,left,top},{input:thumb,left:left+10,top:top+76});
  }
  layers.push({input:Buffer.from('<svg width="1920" height="100"><text x="24" y="42" fill="#eef2f3" font-family="Arial" font-size="32">THE BECKENBURG PROJECT — ALL 23 ROUGH MAP LAYOUTS</text><text x="24" y="74" fill="#b9d2db" font-family="Arial" font-size="20">Nine initial families · thirteen remote proposals · Castle headquarters · space reservations, not playable levels</text></svg>'),left:0,top:0});
  await sharp({create:{width:1920,height:2260,channels:4,background:'#121c24'}}).composite(layers).png().toFile(path.join(root,'ATLAS.png'));
  console.log('Rendered 23 full plan PNGs and representative-floor contact sheet.');
}
main().catch(e=>{console.error(e);process.exit(1)});
