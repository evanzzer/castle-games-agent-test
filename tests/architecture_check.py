"""Enforce domain independence, the audio boundary and short explanatory comments."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
source = root / 'src/castle'
core = ['Engine', 'Campaign', 'StageDefinition', 'CampaignInfo', 'Missions',
        'Altars', 'Frontier', 'Wilds', 'UpgradeRules', 'GameAudio']
for name in core:
    text = (source / (name + '.java')).read_text()
    assert not re.search(r'\b(?:android|org\.json)\.', text), name + ' depends on a platform API'
    assert not re.search(r'\b(?:Main|AndroidGameAudio|AssetMetadata|TerrainArt)\b', text), name + ' depends outward'

adapter = (source / 'AndroidGameAudio.java').read_text()
assert 'implements GameAudio' in adapter
assert not re.search(r'\b(?:Main|Board)\b', adapter), 'Audio adapter depends on presentation'
activity = (source / 'Main.java').read_text()
assert 'private GameAudio audio;' in activity
assert not re.search(r'\b(?:SoundPool|MediaPlayer)\b', activity), 'Playback details leaked into activity'

for path in source.glob('*.java'):
    text = path.read_text()
    for block in re.findall(r'/\*.*?\*/', text, re.S):
        assert len(block.splitlines()) <= 2, str(path) + ' has a long block comment'
    consecutive = 0
    for number, line in enumerate(text.splitlines(), 1):
        consecutive = consecutive + 1 if line.lstrip().startswith('//') else 0
        assert consecutive <= 2, str(path) + ':' + str(number) + ' has a long comment group'

print('Architecture checks passed: platform-free domain, independent audio adapter and one/two-line comments.')
