from pathlib import Path
import json,hashlib,struct,subprocess,sys
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'assets/terrain/citadel/worlds.json').read_text())
assert data['campaign']=='citadel' and data['first_stage']==91 and len(data['worlds'])==30
manifest=json.loads((root/'assets/terrain/citadel/manifest.json').read_text())['overlays']
hashes=set()
for index,world in enumerate(data['worlds']):
 assert world['overlay'][0]=='terrain/citadel/terrain%d.png'%(91+index)
 png=(root/'assets'/world['overlay'][0]).read_bytes()
 assert png[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',png[16:24])==(960,640) and png[25]==6
 hashes.add(hashlib.sha256(png).hexdigest())
assert len(hashes)==30 and len(manifest)==30
assert not (root/'assets/terrain/citadel/markers.json').exists(),'markers belong in map/campaigns.json'
files=sorted((root/'assets/terrain/citadel').glob('*.json'))+[root/'assets/map/campaigns.json',root/'src/castle/domain/stages/data/Citadel.java']
before=[p.read_bytes() for p in files]
subprocess.run([sys.executable,'tools/generate_citadel.py'],cwd=root,check=True)
assert before==[p.read_bytes() for p in files],'generator is deterministic'
print('Citadel data checks passed: 30 unique RGBA terrain overlays; deterministic generation.')
