from pathlib import Path
import json,hashlib,struct,subprocess,sys
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'assets/terrain/wild/manifest.json').read_text())
assert data['campaign']=='wild' and data['first_stage']==61 and len(data['worlds'])==30
preserved=[root/'assets/terrain/original/manifest.json',root/'assets/terrain/frontier/manifest.json',root/'assets/map/campaigns.json']
original=[p.read_bytes() for p in preserved]
terrain_before=(root/'assets/terrain/wild/manifest.json').read_bytes()
metadata=json.loads((root/'tools/data/wilds-design.json').read_text())
assert [m['stage'] for m in metadata]==list(range(61,91))
assert len({json.dumps(m['regions'],sort_keys=True) for m in metadata})==30
hashes=set()
manifest=json.loads((root/'assets/terrain/wild/manifest.json').read_text())['overlays']
regions=[json.loads(line.strip().rstrip(',')) for line in (root/'src/castle/Wilds.java').read_text().split('MARSHES = {')[1].splitlines() if line.strip().startswith('"')]
for world,item in zip(data['worlds'],metadata):
 assert world['new_terrain'] and world['name']==item['name']
 assert item['original_shape_source'] is None
 assert all(len(r['points'])>=45 for r in item['regions'])
 path=root/'assets'/str(world['overlay'][0])
 assert manifest[path.name]==hashlib.sha256(regions[item['stage']-61].encode()).hexdigest()
 contents=path.read_bytes();assert contents[:8]==b'\x89PNG\r\n\x1a\n'
 assert struct.unpack('>II',contents[16:24])==(960,640)
 assert contents[25]==6 # RGBA, retaining transparent background
 hashes.add(hashlib.sha256(contents).hexdigest())
assert len(hashes)==30
subprocess.run([sys.executable,'tools/generate_wilds.py'],cwd=root,check=True)
assert [p.read_bytes() for p in preserved]==original
assert (root/'assets/terrain/wild/manifest.json').read_bytes()==terrain_before
print('Wilds data checks passed: 30 unique RGBA terrain overlays; original artwork materials; first sixty worlds preserved.')
