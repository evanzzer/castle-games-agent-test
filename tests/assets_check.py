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
worlds = load(manifest['worlds'])
assert len(worlds['worlds']) == 90
for key in ['markers', 'frontierMarkers', 'wildsMarkers']: assert len(worlds[key]) == 30
for index, world in enumerate(worlds['worlds']):
    if world.get('overlay'):
        path = world['overlay'][0]
        image_size(path)
        group = 'original' if index < 30 else 'frontier' if index < 60 else 'wild'
        assert path.startswith('terrain/' + group + '/')
for path in assets.rglob('*'):
    if path.is_file() and not path.name.startswith('.'):
        assert path.suffix in ['.json','.png','.wav'], path
        assert all(part == part.lower() for part in path.relative_to(assets).parts), path
        if path.suffix == '.json': json.loads(path.read_text())
for name in ['move1','move2','attack1','attack2','arrow','fireball','explosion','twister','hypnoses','support','won','lost']:
    assert (assets / 'audio/effects' / (name+'.wav')).is_file()
print('Asset checks passed: %d JSON atlases, %d frames, font glyphs, 90 worlds and lowercase category folders.' % (len(manifest['atlases']),frames))
