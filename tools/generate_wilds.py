"""Author stages 61–90: new terrain silhouettes using the existing game's art.

TerrainArt.java bakes these regions into separate transparent PNG overlays using
small material patches from the original marsh/poison art. No original map shape
is copied into this campaign. Existing stages 1–60 are preserved.
"""
from pathlib import Path
import json,math,random
ROOT=Path(__file__).resolve().parents[1]
layouts={
'delta':[(90,100,'R'),(90,300,'R'),(90,500,'R'),(870,140,'B'),(870,340,'B'),(870,540,'B'),(260,150,'c'),(260,490,'c'),(700,150,'c'),(700,490,'c'),(440,100,'h'),(520,540,'h'),(340,300,'h'),(620,340,'h'),(480,210,'t'),(480,430,'t'),(480,320,'H')],
'crescent':[(120,100,'R'),(110,240,'R'),(140,400,'R'),(840,540,'B'),(850,400,'B'),(820,240,'B'),(320,100,'c'),(640,540,'c'),(160,540,'c'),(800,100,'c'),(360,400,'t'),(600,240,'t'),(360,220,'h'),(600,420,'h'),(480,90,'h'),(480,550,'h'),(480,320,'H')],
'spokes':[(110,100,'R'),(110,320,'R'),(110,540,'R'),(850,100,'B'),(850,320,'B'),(850,540,'B'),(300,90,'c'),(660,90,'c'),(300,550,'c'),(660,550,'c'),(300,230,'h'),(300,410,'h'),(480,140,'h'),(660,230,'h'),(660,410,'h'),(480,500,'h'),(480,320,'H'),(390,320,'t'),(570,320,'t')],
'cascade':[(90,130,'R'),(250,90,'R'),(90,450,'R'),(870,510,'B'),(710,550,'B'),(870,190,'B'),(280,250,'c'),(680,390,'c'),(440,100,'c'),(520,540,'c'),(350,400,'t'),(610,240,'t'),(200,540,'h'),(760,100,'h'),(480,320,'H'),(350,220,'h'),(610,420,'h')],
'hourglass':[(130,100,'R'),(330,90,'R'),(150,270,'R'),(830,540,'B'),(630,550,'B'),(810,370,'B'),(170,500,'c'),(790,140,'c'),(340,350,'c'),(620,290,'c'),(400,220,'t'),(560,420,'t'),(700,90,'h'),(260,550,'h'),(480,320,'H'),(250,200,'h'),(710,440,'h')],
'ridges':[(100,140,'R'),(100,320,'R'),(100,500,'R'),(860,140,'B'),(860,320,'B'),(860,500,'B'),(300,110,'c'),(300,530,'c'),(660,110,'c'),(660,530,'c'),(350,250,'t'),(610,390,'t'),(480,90,'h'),(480,550,'h'),(480,320,'H'),(350,430,'h'),(610,210,'h')]
}
names=['Fen Gate','Venom Teeth','Crescent Marsh','Four Bitter Pools','Braided Crossing','Serpent Hollow','Split Current','Hollow Crown','Toxic Switchback','Broken Ring',
 'Sundered Fen','Threefold Venom','Moonwater','Blackwater Wells','Crooked Banks','The Coil','Mired Spears','Poisoned Halo','Winding Siege','Reed Crown',
 'Drowned Gate','Fang Basin','Last Crescent','Fourfold Mire','Severed Current','Serpent Throne','Sinking Spears','Hollow Dominion','Final Switchback','Crown of the Wilds']
briefs=['Two slow basins leave a central crossing. Control the staging castles.',
 'Poison guards the centre while swamp delays the flanks.',
 'Curved swamp banks leave a narrow opening. Time your reinforcements.',
 'Separate poison pools punish careless straight-line deployments.',
 'The winding slow and poison channels create distinct crossing routes.',
 'Poison curls around the middle. Approach through the open side.',
 'Scattered hazards split the front. Reinforce the nearest fighting castle.',
 'A slow crescent and poison pools reward careful redirection.',
 'The winding poison channel makes short reinforcement routes valuable.',
 'Broken swamp arcs shelter poison. Contest the safe gaps.']
def transform(x,y,turn):
 u,v=x/960,y/640
 for _ in range(turn):u,v=1-v,u
 return [round(u*960,2),round(v*640,2)]
def oval(cx,cy,rx,ry,rng,phase=0):
 phase+=rng.random()*2
 return [(cx+math.cos(a)*rx*(1+.08*math.sin(3*a+phase)+.035*math.sin(7*a)),cy+math.sin(a)*ry*(1+.06*math.cos(5*a+phase))) for a in [i*2*math.pi/48 for i in range(48)]]
def crescent(cx,cy,rx,ry,width,rot=0):
 outer=[];inner=[]
 for i in range(37):
  a=math.radians(-130+i*260/36+rot);r=1+.035*math.sin(i*.7)
  outer.append((cx+math.cos(a)*rx*r,cy+math.sin(a)*ry*r))
  inner.append((cx+math.cos(a)*(rx-width),cy+math.sin(a)*(ry-width)))
 return outer+inner[::-1]
def channel(points,width):
 left=[];right=[]
 # Interpolated bends keep the region organic without self-intersecting.
 smooth=[]
 for a,b in zip(points,points[1:]):
  for k in range(7):smooth.append((a[0]+(b[0]-a[0])*k/7,a[1]+(b[1]-a[1])*k/7))
 smooth.append(points[-1])
 for i,(x,y) in enumerate(smooth):
  a=smooth[max(0,i-1)];b=smooth[min(len(smooth)-1,i+1)]
  dx,dy=b[0]-a[0],b[1]-a[1];length=math.hypot(dx,dy) or 1
  w=width*(1+.07*math.sin(i*.8))
  left.append((x-dy/length*w,y+dx/length*w));right.append((x+dy/length*w,y-dx/length*w))
 return left+right[::-1]
def regions(kind,rng):
 o=lambda *args:oval(*args,rng)
 if kind==0:return [(51,o(405,175,165,90)),(51,o(555,465,165,90))]
 if kind==1:return [(50,o(480,320,100,175)),(51,o(290,320,70,125)),(51,o(670,320,70,125))]
 if kind==2:return [(51,crescent(480,320,255,205,65)),(50,o(485,320,60,65))]
 if kind==3:return [(50,o(x,y,94,74)) for x,y in [(300,180),(650,180),(300,460),(650,460)]]
 if kind==4:return [(51,channel([(250,90),(330,185),(290,300),(390,410),(440,550)],44)),(50,channel([(700,90),(630,200),(690,320),(590,440),(540,550)],35))]
 if kind==5:return [(50,crescent(480,320,250,205,72,180)),(51,o(480,320,75,65))]
 if kind==6:return [(51,o(305,205,125,90)),(51,o(655,435,125,90)),(50,o(480,320,80,90))]
 if kind==7:return [(51,crescent(480,320,235,180,60,180)),(50,o(365,170,85,65)),(50,o(595,470,85,65))]
 if kind==8:return [(50,channel([(350,85),(490,200),(400,320),(540,440),(450,555)],32)),(51,o(250,480,95,65)),(51,o(710,160,95,65))]
 return [(51,crescent(480,320,250,200,62,75)),(50,o(390,330,60,72)),(50,o(620,450,75,60))]
data=json.loads((ROOT/'assets/terrain/wild/manifest.json').read_text())
data['worlds']=[]
rows=[];terrain=[];altars=[];titles=[];tips=[];catalog=[]
keys=list(layouts)
for i,name in enumerate(names):
 rng=random.Random(61000+i);turn=(i//6)%4;family=keys[i%6];raw=list(layouts[family]);three=i>=3
 if three:
  slots=[j for j,n in enumerate(raw) if n[2]=='h']
  slots.sort(key=lambda j:abs(raw[j][0]-480)+abs(raw[j][1]-320)*.2)
  for j in slots[:2]:x,y,_=raw[j];raw[j]=(x,y,'Y')
 records=[];nodes=[]
 for j,(x,y,kind) in enumerate(raw):
  if kind not in 'RBY':x+=rng.randint(-8,8);y+=rng.randint(-6,6)
  x,y=transform(x,y,turn);side={'R':'r','B':'b','Y':'y'}.get(kind,'g')
  if kind=='c':sprite='mine';pivot=9
  elif kind=='t':size='m' if i<12 else 'b';sprite='tower_'+size;pivot=42 if size=='m' else 46
  else:size='b' if kind=='H' or (kind in 'RBY' and i>=15) else 'm';sprite='house_'+size;pivot=10 if size=='b' else (21 if side=='g' else 14)
  records.append(f'{side}_{sprite},{x:g},{y:g},{x:g},{y-pivot:g}');nodes.append([x,y,kind])
 zones=[];zone_rows=[]
 for effect,points in regions(i%10,rng):
  points=[transform(x+5*math.sin(y*.025+i*.61),y+4*math.cos(x*.018+i*.79),turn) for x,y in points]
  assert all(15<x<945 and 15<y<625 for x,y in points)
  zones.append(dict(type=effect,points=points));zone_rows.append(str(effect)+','+','.join(f'{x:g},{y:g}' for x,y in points))
 pickups=[]
 for x,y,kind in [(410,320,100),(550,320,101)]:
  x,y=transform(x,y,turn);pickups.append(f'{x:g},{y:g},{kind}')
 objects=[]
 for _ in range(55):
  x,y=rng.randint(25,935),rng.randint(25,615)
  if min(math.hypot(x-a,y-b) for a,b,_ in nodes)<65:continue
  objects.append(['g_'+str(rng.randint(1,5)),x,y,32,42])
 brief=briefs[i%10];stage=61+i
 data['worlds'].append(dict(width=960,height=640,overlay=[f'terrain/wild/terrain{stage}.png',480,320,960,640],objects=objects,custom=True,name=name,brief=brief,new_terrain=True))
 rows.append(';'.join(records));terrain.append(';'.join(zone_rows));altars.append(';'.join(pickups));titles.append(name);tips.append(brief)
 catalog.append(dict(stage=stage,name=name,brief=brief,layout=family,regions=zones,altars=pickups,buildings=nodes,art_sources=['terrain5.png material','terrain7.png material'],original_shape_source=None))
(ROOT/'assets/terrain/wild/manifest.json').write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')
lines=['package castle;','final class Wilds {']
for key,values in [('NAMES',titles),('BRIEFS',tips),('BUILDINGS',rows),('MARSHES',terrain),('ALTARS',altars)]:
 lines+=[' static final String[] '+key+' = {']+['  '+json.dumps(v)+',' for v in values]+[' };']
lines+=['}'];(ROOT/'src/castle/Wilds.java').write_text('\n'.join(lines)+'\n')
(ROOT/'tools/data/wilds-design.json').write_text(json.dumps(catalog,indent=2,ensure_ascii=False)+'\n')
print('Authored stages 61–90 with',sum(len(x['regions']) for x in catalog),'new region shapes; other terrain campaigns and map navigation untouched.')
