"""Verify restored v0.9 shapes and their baked art, retaining other campaigns."""
from pathlib import Path
import json,subprocess,sys,hashlib,struct
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'assets/terrain/frontier/worlds.json').read_text())
assert data['campaign']=='frontier' and data['first_stage']==31 and len(data['worlds'])==30
design=json.loads((root/'tools/data/frontier-design.json').read_text())
archive=json.loads((root/'tools/data/frontier-region-plans.json').read_text())
turns=[0,0,0,1,0,0,2,2,1,2,1,2,3,2,1,2,1,0,0,3,3,3,1,3,1,0,2,0,3,0]
manifest=json.loads((root/'assets/terrain/frontier/manifest.json').read_text())['overlays']
rows=[json.loads(line.strip().rstrip(',')) for line in (root/'src/castle/Frontier.java').read_text().split('MARSHES = {')[1].splitlines() if line.strip().startswith('"')]
count=0
for i,(entry,world) in enumerate(zip(design,data['worlds'])):
 assert entry['stage']==i+31 and entry['terrain_layout_version']=='v0.13-curved'
 expected=[]
 for kind,vertices in archive['plans'][archive['stage_plans'][i]]:
  points=[]
  for x,y in vertices:
   u,v=x/960,y/640
   for _ in range(turns[i]):u,v=1-v,u
   points.append([round(u*960),round(v*640)])
  expected.append(dict(type=kind,points=points))
 assert [dict(type=r['type'],points=r['control_points']) for r in entry['regions']]==expected
 for region,original_region in zip(entry['regions'],expected):
  points=region['points'];controls=original_region['points']
  assert len(points)>=64
  for axis in (0,1):
   assert abs(min(p[axis] for p in points)-min(p[axis] for p in controls))<=1
   assert abs(max(p[axis] for p in points)-max(p[axis] for p in controls))<=1
  assert max(((points[j][0]-points[j-1][0])**2+(points[j][1]-points[j-1][1])**2)**.5 for j in range(len(points)))<35
 assert world['overlay']==[f'terrain/frontier/terrain{i+31}.png',480,320,960,640]
 path=root/'assets'/str(world['overlay'][0]);png=path.read_bytes()
 assert png[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',png[16:24])==(960,640) and png[25]==6
 assert manifest[path.name]==hashlib.sha256(rows[i].encode()).hexdigest()
 count+=len(expected)
assert count==59
preserved=sorted((root/'assets/terrain').glob('*/*.json'))
preserved=[p for p in preserved if p!=root/'assets/terrain/frontier/worlds.json']+[root/'assets/map/campaigns.json']
original=[p.read_bytes() for p in preserved]
terrain_before=(root/'assets/terrain/frontier/worlds.json').read_bytes()
source=(root/'src/castle/Frontier.java').read_bytes()
subprocess.run([sys.executable,'tools/generate_frontier.py'],cwd=root,check=True)
updated=json.loads((root/'assets/terrain/frontier/worlds.json').read_text())
assert [p.read_bytes() for p in preserved]==original
assert (root/'assets/terrain/frontier/worlds.json').read_bytes()==terrain_before
assert (root/'src/castle/Frontier.java').read_bytes()==source
print('Restored Frontier checks passed: curved outlines at original footprints, 59 regions, PNG geometry fingerprints; other campaigns preserved.')
