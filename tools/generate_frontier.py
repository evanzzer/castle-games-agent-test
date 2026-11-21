"""Author the second 30-stage campaign from explicit layouts and tactical briefs.

Uses original sprites and paired terrain overlays/regions, with new building arrangements, altar
routes, scenery placement and campaign markers. Fixed seeds make builds repeatable.
"""
from pathlib import Path
import json,math,random
ROOT=Path(__file__).resolve().parents[1]
# Ground positions on a 960 x 640 battlefield. R/B are starting castle pairs.
layouts={
'fork':[(90,220,'R'),(90,420,'R'),(870,220,'B'),(870,420,'B'),(270,140,'h'),(270,500,'h'),(430,180,'c'),(430,460,'c'),(590,140,'h'),(590,500,'h'),(480,320,'t'),(250,320,'h'),(710,320,'h')],
'ring':[(110,200,'R'),(110,440,'R'),(850,200,'B'),(850,440,'B'),(230,90,'h'),(480,90,'c'),(730,90,'h'),(230,550,'h'),(480,550,'c'),(730,550,'h'),(350,260,'t'),(610,380,'t'),(480,320,'H'),(350,380,'c'),(610,260,'c')],
'diagonal':[(110,100,'R'),(110,260,'R'),(850,540,'B'),(850,380,'B'),(280,100,'h'),(110,440,'c'),(480,110,'t'),(300,300,'h'),(480,320,'c'),(660,340,'h'),(850,200,'c'),(680,540,'h'),(480,530,'t'),(240,540,'h')],
'crown':[(110,320,'R'),(230,140,'R'),(850,320,'B'),(730,500,'B'),(230,500,'h'),(730,140,'h'),(480,120,'c'),(480,520,'c'),(350,220,'t'),(610,420,'t'),(480,320,'H'),(350,420,'h'),(610,220,'h')],
'islands':[(110,130,'R'),(110,510,'R'),(850,130,'B'),(850,510,'B'),(280,130,'c'),(280,510,'c'),(680,130,'c'),(680,510,'c'),(480,100,'h'),(480,540,'h'),(360,260,'t'),(600,380,'t'),(480,320,'H')],
'citadel':[(90,220,'R'),(90,420,'R'),(870,220,'B'),(870,420,'B'),(270,110,'c'),(270,530,'c'),(690,110,'c'),(690,530,'c'),(350,220,'t'),(350,420,'t'),(610,220,'t'),(610,420,'t'),(480,180,'h'),(480,460,'h'),(480,320,'H')]
}
# (name, authored building layout, orientation, three nations, altar)
specs=[
 ('Twin Crossings', 'fork', 0, False, 0),
 ('Bitter Ring', 'ring', 0, False, 0),
 ('Broken Diagonal', 'diagonal', 0, False, 0),
 ('Shield Road', 'crown', 1, False, 2),
 ('Four Wells', 'islands', 0, False, 0),
 ('Third Banner', 'citadel', 0, True, 0),
 ('Angel Passage', 'fork', 2, True, 1),
 ('Split Crown', 'crown', 2, True, 0),
 ('Venom Circuit', 'ring', 1, False, 2),
 ('Siege Orchard', 'citadel', 2, True, 0),
 ('Crosswind', 'diagonal', 1, True, 1),
 ('Crystal Race', 'islands', 2, True, 0),
 ('Two Shields', 'crown', 3, True, 3),
 ('Long Detour', 'ring', 2, True, 0),
 ('Black Narrows', 'fork', 1, False, 2),
 ('Fallen Halo', 'diagonal', 2, True, 1),
 ('Watchfire', 'citadel', 1, True, 2),
 ('Bitter Harvest', 'islands', 0, True, 0),
 ('Crown of Thorns', 'crown', 0, True, 1),
 ('Turning Wheel', 'ring', 3, True, 3),
 ('Three Fronts', 'fork', 3, True, 0),
 ('White Standard', 'diagonal', 3, True, 3),
 ('Silent Wells', 'islands', 1, True, 2),
 ('Iron Orchard', 'citadel', 3, True, 0),
 ('Poisoned Crown', 'crown', 1, True, 3),
 ('Encirclement', 'ring', 0, True, 1),
 ('Last Crossing', 'fork', 2, True, 2),
 ('Storm Divide', 'diagonal', 0, True, 3),
 ('Fourfold Siege', 'islands', 3, True, 3),
 ('Frontier Throne', 'citadel', 0, True, 3),
]

def transform(x,y,turn):
 u,v=x/960,y/640
 for _ in range(turn):u,v=1-v,u
 return round(u*960),round(v*640)
def curve_region(vertices,seed):
 """Round broad patches and bend narrow channels; art and effects share samples."""
 rng=random.Random(seed)
 cx=sum(x for x,y in vertices)/len(vertices);cy=sum(y for x,y in vertices)/len(vertices)
 xx=sum((x-cx)**2 for x,y in vertices);yy=sum((y-cy)**2 for x,y in vertices)
 xy=sum((x-cx)*(y-cy) for x,y in vertices)
 angle=.5*math.atan2(2*xy,xx-yy);ux,uy=math.cos(angle),math.sin(angle);vx,vy=-uy,ux
 us=[(x-cx)*ux+(y-cy)*uy for x,y in vertices];vs=[(x-cx)*vx+(y-cy)*vy for x,y in vertices]
 length=(max(us)-min(us))/2;width=(max(vs)-min(vs))/2
 if length/width>2.1:
  phase=rng.uniform(-.7,.7);points=[]
  for i in range(96):
   a=i*math.tau/96;u=length*math.cos(a)
   bend=.55*width*math.sin(1.6*math.pi*u/length+phase)*(1-(u/length)**2)
   v=width*math.sin(a)*(1+.045*math.sin(3*a+phase))+bend
   points.append((cx+u*ux+v*vx,cy+u*uy+v*vy))
 else:
  controls=[]
  for i,(x,y) in enumerate(vertices):
   nx,ny=vertices[(i+1)%len(vertices)];dx,dy=nx-x,ny-y;d=math.hypot(dx,dy)
   offset=rng.uniform(-1,1)*min(12,d*.065)
   controls.extend([(x,y),((x+nx)/2-dy/d*offset,(y+ny)/2+dx/d*offset)])
  points=[]
  for i in range(len(controls)):
   a,b,c,d=[controls[j%len(controls)] for j in (i-1,i,i+1,i+2)]
   for sample in range(12):
    t=sample/12;t2=t*t;t3=t2*t
    points.append(tuple(.5*(2*b[k]+(-a[k]+c[k])*t+(2*a[k]-5*b[k]+4*c[k]-d[k])*t2+(-a[k]+3*b[k]-3*c[k]+d[k])*t3) for k in (0,1)))
 # Keep the same general footprint and routes while removing angular outlines.
 minx=min(x for x,y in points);maxx=max(x for x,y in points);miny=min(y for x,y in points);maxy=max(y for x,y in points)
 left=min(x for x,y in vertices);right=max(x for x,y in vertices);top=min(y for x,y in vertices);bottom=max(y for x,y in vertices)
 return [(left+(x-minx)/(maxx-minx)*(right-left),top+(y-miny)/(maxy-miny)*(bottom-top)) for x,y in points]
def java_array(name,values):
 return [' static final String[] '+name+' = {']+['  '+json.dumps(v)+',' for v in values]+[' };']
# Restored v0.9 authored region plans; rendered as textured PNGs by TerrainArt.
region_plans=json.loads((ROOT/'tools/data/frontier-region-plans.json').read_text())
plans=region_plans['plans']
terrain_plans=region_plans['stage_plans']
terrain_briefs=['Swamp divides the approaches. Use the outer routes to reinforce.', 'Central poison punishes shortcuts; crystal mines fund your spells.', 'Slow and poison bands reward support castles on both sides.', 'Parallel poison and slow lanes leave a clear middle route.', 'Poison pockets guard opposite approaches. Contest the clear crossings.', 'A diagonal swamp exposes armies. Use nearby castles to shorten the march.', 'Mixed hazards split the centre. Coordinate arrivals along safe flanks.']
world_data=json.loads((ROOT/'assets/terrain/frontier/manifest.json').read_text())
world_data['worlds']=[]
names=[];tips=[];buildings=[];marshes=[];altars=[];details=[]
for index,(name,layout,turn,three,altar) in enumerate(specs):
 rng=random.Random(31000+index)
 nodes=[];records=[]
 raw=list(layouts[layout])
 # The third force begins at two central outer castles, not at an opponent's home.
 if three:
  choices=[i for i,node in enumerate(raw) if node[2] in ('h','H')]
  choices.sort(key=lambda i:abs(raw[i][0]-480)+abs(raw[i][1]-320)*.25)
  for chosen in choices[:2]:
   x,y,_=raw[chosen];raw[chosen]=(x,y,'Y')
 for i,(x,y,kind) in enumerate(raw):
  if kind not in 'RBY':x+=rng.randint(-16,16);y+=rng.randint(-12,12)
  x,y=transform(x,y,turn)
  side={'R':'r','B':'b','Y':'y'}.get(kind,'g')
  if kind=='c':sprite='mine';pivot=9
  elif kind=='t':
   size='s' if index<8 else ('b' if index>=23 and i%2 else 'm')
   sprite='tower_'+size;pivot={'s':37,'m':42,'b':46}[size]
  else:
   size='b' if kind=='H' or (index>=18 and kind in 'RBY') else 'm'
   sprite='house_'+size;pivot=10 if size=='b' else (21 if side=='g' else 14)
  records.append(f'{side}_{sprite},{x},{y},{x},{y-pivot}')
  nodes.append((x,y,kind))
 zones=[];zone_records=[]
 for zone_index,(zone_type,vertices) in enumerate(plans[terrain_plans[index]]):
  controls=[transform(x,y,turn) for x,y in vertices]
  points=[transform(x,y,turn) for x,y in curve_region(vertices,13000+index*7+zone_index)]
  zones.append(dict(type=zone_type,points=points,control_points=controls))
  zone_records.append(str(zone_type)+','+','.join(f'{x:g},{y:g}' for x,y in points))
 overlay=[f'terrain/frontier/terrain{index+31}.png',480,320,960,640]
 brief=terrain_briefs[terrain_plans[index]]
 pickups=[]
 if altar:
  for x,y,kind in ([(420,320,100)] if altar==1 else [(540,320,101)] if altar==2 else [(420,320,100),(540,320,101)]):
   x,y=transform(x,y,turn);pickups.append(f'{x},{y},{kind}')
 objects=[]
 # A sparse border and clusters leave building/troop routes legible.
 for decor in range(65):
  x=rng.randint(22,938);y=rng.randint(22,618)
  if min(math.hypot(x-a,y-b) for a,b,_ in nodes)<58:continue
  if any(350<x<610 and 245<y<395 for _ in [0]):continue
  objects.append(['g_'+str(rng.choice([1,2,3,4,5])),x,y,32,42])
 world_data['worlds'].append(dict(width=960,height=640,overlay=overlay,objects=objects,custom=True,name=name,brief=brief,terrain_layout_version="v0.13-curved"))
 names.append(name);tips.append(brief);buildings.append(';'.join(records));marshes.append(';'.join(zone_records));altars.append(';'.join(pickups))
 details.append(dict(stage=index+31,name=name,layout=layout,brief=brief,building_count=len(nodes),teams=3 if three else 2,regions=zones,altars=pickups,terrain_layout_version="v0.13-curved"))
(ROOT/'assets/terrain/frontier/manifest.json').write_text(json.dumps(world_data,indent=2,ensure_ascii=False)+'\n')
lines=['package castle;','final class Frontier {']
for key,values in [('NAMES',names),('BRIEFS',tips),('BUILDINGS',buildings),('MARSHES',marshes),('ALTARS',altars)]:lines+=java_array(key,values)
lines+=['}'];(ROOT/'src/castle/Frontier.java').write_text('\n'.join(lines)+'\n')
(ROOT/'tools/data/frontier-design.json').write_text(json.dumps(details,indent=2,ensure_ascii=False)+'\n')
print('Authored 30 Frontier levels; other terrain campaigns and map navigation untouched.')
