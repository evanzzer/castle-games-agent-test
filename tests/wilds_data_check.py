from pathlib import Path
import json,hashlib,struct,subprocess,sys
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'assets/world.json').read_text())
assert len(data['worlds'])==90 and len(data['wildsMarkers'])==30
first60=data['worlds'][:60]
metadata=json.loads((root/'assets/wilds-design.json').read_text())
assert [m['stage'] for m in metadata]==list(range(61,91))
assert len({json.dumps(m['regions'],sort_keys=True) for m in metadata})==30
hashes=set()
manifest=json.loads((root/'assets/Image/wilds/terrain-manifest.json').read_text())
regions=[json.loads(line.strip().rstrip(',')) for line in (root/'src/castle/Wilds.java').read_text().split('MARSHES = {')[1].splitlines() if line.strip().startswith('"')]
for world,item in zip(data['worlds'][60:],metadata):
 assert world['new_terrain'] and world['name']==item['name']
 assert item['original_shape_source'] is None
 assert all(len(r['points'])>=45 for r in item['regions'])
 path=root/'assets/Image'/str(world['overlay'][0]+'.png')
 assert manifest[path.name]==hashlib.sha256(regions[item['stage']-61].encode()).hexdigest()
 contents=path.read_bytes();assert contents[:8]==b'\x89PNG\r\n\x1a\n'
 assert struct.unpack('>II',contents[16:24])==(960,640)
 assert contents[25]==6 # RGBA, retaining transparent background
 hashes.add(hashlib.sha256(contents).hexdigest())
assert len(hashes)==30
subprocess.run([sys.executable,'generate_wilds.py'],cwd=root,check=True)
assert json.loads((root/'assets/world.json').read_text())['worlds'][:60]==first60
print('Wilds data checks passed: 30 unique RGBA terrain overlays; original artwork materials; first sixty worlds preserved.')
