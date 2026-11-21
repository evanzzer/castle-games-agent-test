from pathlib import Path
import json,hashlib,struct,subprocess,sys
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'assets/terrain/zenith/worlds.json').read_text())
assert data['campaign']=='zenith' and data['first_stage']==121 and len(data['worlds'])==30
manifest=json.loads((root/'assets/terrain/zenith/manifest.json').read_text())['overlays']
hashes=set();plan=[1,1,1,2,2,3,3,4,4,4]
for index,world in enumerate(data['worlds']):
 assert world['overlay'][0]=='terrain/zenith/terrain%d.png'%(121+index)
 assert world['background']=='map/backgrounds/map%d.png'%plan[index%10] and (root/'assets'/world['background']).is_file()
 png=(root/'assets'/world['overlay'][0]).read_bytes()
 assert png[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',png[16:24])==(960,640) and png[25]==6
 hashes.add(hashlib.sha256(png).hexdigest())
assert len(hashes)==30 and len(manifest)==30
cit=json.loads((root/'assets/terrain/citadel/worlds.json').read_text())['worlds']
assert [w['background'] for w in cit]==[w['background'] for w in data['worlds']],'3-2-2-3 background mix'
files=sorted((root/'assets/terrain/zenith').glob('*.json'))+[root/'assets/map/campaigns.json',root/'assets/manifest.json',root/'src/castle/domain/stages/data/Zenith.java']
before=[p.read_bytes() for p in files]
subprocess.run([sys.executable,'tools/generate_zenith.py'],cwd=root,check=True)
assert before==[p.read_bytes() for p in files],'generator is deterministic'
print('Zenith data checks passed: 30 unique RGBA overlays; 3-2-2-3 background mix; deterministic generation.')
