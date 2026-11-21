"""Validate the runtime JSON contract and every relocated asset reference."""
from pathlib import Path
import json, struct, hashlib
root = Path(__file__).resolve().parents[1]
assets = root / 'assets'
def load(path):
    path = assets / path
    assert path.is_file(), path
    return json.loads(path.read_text())
def image_size(path):
    data = (assets / path).read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', path
    return struct.unpack('>II', data[16:24])
def frame(entry, width, height):
    x, y, w, h = entry['frame']
    assert all(isinstance(n, int) for n in [x, y, w, h])
    assert min(x,y) >= 0 and min(w,h) > 0 and x+w <= width and y+h <= height
manifest = load('manifest.json')
assert manifest['schema_version'] == 1
frames = 0
for path in manifest['atlases']:
    atlas = load(path)
    assert atlas['schema_version'] == 1
    width, height = image_size(atlas['image'])
    for entry in atlas['frames'].values(): frame(entry, width, height); frames += 1
font = load(manifest['number_font'])
assert font['schema_version'] == 1 and font['line_height'] == 27
for entry in font['glyphs'].values(): frame(entry, *image_size(font['image']))
assert all(str(code) in font['glyphs'] for code in range(48,58))
navigation = load(manifest['campaigns'])['campaigns']
assert set(navigation) == {'original', 'frontier', 'wild'}
assert not (assets / 'map/worlds.json').exists()
for index, group in enumerate(['original', 'frontier', 'wild']):
    assert len(navigation[group]['markers']) == 30
    entry = manifest['terrain'][group]
    terrain = load(entry['worlds'])
    fingerprints = load(entry['manifest'])
    assert set(fingerprints) == {'schema_version', 'overlays'}
    assert terrain['campaign'] == group and terrain['first_stage'] == index * 30 + 1
    assert len(terrain['worlds']) == 30
    java_name = {'original':'Missions', 'frontier':'Frontier', 'wild':'Wilds'}[group]
    source = (root / 'src/castle' / (java_name + '.java')).read_text().split('MARSHES = {')[1]
    rows = [json.loads(line.strip().rstrip(',')) for line in source.splitlines() if line.strip().startswith('"')]
    for local, world in enumerate(terrain['worlds']):
        assert world['width'] > 0 and world['height'] > 0
        if world.get('overlay'):
            path = world['overlay'][0]
            image_size(path)
            assert fingerprints['overlays'][Path(path).name] == hashlib.sha256(rows[local].encode()).hexdigest()
            assert path.startswith('terrain/' + group + '/')
for path in assets.rglob('*'):
    if path.is_file() and not path.name.startswith('.'):
        assert path.suffix in ['.json','.png','.wav'], path
        assert all(part == part.lower() for part in path.relative_to(assets).parts), path
        if path.suffix == '.json': json.loads(path.read_text())
for name in ['move1','move2','attack1','attack2','arrow','fireball','explosion','twister','hypnoses','support','won','lost']:
    assert (assets / 'audio/effects' / (name+'.wav')).is_file()
print('Asset checks passed: %d JSON atlases, %d frames, font glyphs, 90 worlds and lowercase category folders.' % (len(manifest['atlases']),frames))
