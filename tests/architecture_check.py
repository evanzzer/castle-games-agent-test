"""Enforce domain independence, application and adapter boundaries and short explanatory comments."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
source = root / 'src/castle'
files = {path.stem: path for path in source.rglob('*.java')}
def java(name):
    return files[name].read_text()
core = ['Engine', 'Campaign', 'StageDefinition', 'CampaignInfo', 'Missions',
        'Altars', 'Frontier', 'Wilds', 'UpgradeRules', 'GameAudio', 'AiCommander',
        'PlayerProfile', 'ProfileRepository', 'MatchFactory', 'MatchAudioEvents',
        'WorldLayout', 'MapMarker']
assert {p.name for p in source.glob('*.java')} == {'Main.java'}, 'Only the entry point belongs outside responsibility folders'
expected = {
    'Main': 'Main.java',
    'Engine': 'domain/Engine.java', 'AiCommander': 'domain/AiCommander.java',
    'PlayerProfile': 'domain/PlayerProfile.java', 'Campaign': 'domain/stages/Campaign.java',
    'MatchFactory': 'application/MatchFactory.java', 'MatchAudioEvents': 'application/MatchAudioEvents.java',
    'GameAudio': 'application/GameAudio.java', 'ProfileRepository': 'application/ProfileRepository.java',
    'GameView': 'presentation/GameView.java', 'GameResources': 'presentation/GameResources.java',
    'CanvasPainter': 'presentation/CanvasPainter.java', 'ArmyRenderer': 'presentation/ArmyRenderer.java',
    'AndroidGameAudio': 'infrastructure/android/AndroidGameAudio.java',
    'AndroidProfileRepository': 'infrastructure/android/AndroidProfileRepository.java',
    'AndroidGameResources': 'infrastructure/android/AndroidGameResources.java',
}
for name, relative in expected.items():
    assert files[name] == source / relative, name + ' belongs in its responsibility folder'
for name in core:
    text = java(name)
    assert not re.search(r'\b(?:android|org\.json)\.', text), name + ' depends on a platform API'
    assert not re.search(r'\b(?:Main|GameView|AndroidGameAudio|AndroidProfileRepository|AndroidGameResources|AssetMetadata|TerrainArt|CanvasPainter|ArmyRenderer)\b', text), name + ' depends outward'

for name, port in [('AndroidGameAudio', 'GameAudio'),
                   ('AndroidProfileRepository', 'ProfileRepository'),
                   ('AndroidGameResources', 'GameResources')]:
    adapter = java(name)
    assert 'implements ' + port in adapter
    assert not re.search(r'\b(?:Main|GameView)\b', adapter), name + ' depends on presentation'

activity = java('Main')
assert len(activity.splitlines()) <= 100, 'Activity must remain a small composition root'
assert not re.search(r'\b(?:SoundPool|MediaPlayer|SharedPreferences|JSONObject)\b', activity)
view = java('GameView')
assert not re.search(r'\b(?:SharedPreferences|JSONObject|JSONArray|BitmapFactory|SoundPool|MediaPlayer|getAssets)\b', view)
assert 'audioCues.clear' not in view, 'Event delivery belongs to the application service'
for name in ['ArmyRenderer', 'CanvasPainter']:
    renderer = java(name)
    assert not re.search(r'\b(?:GameView|GameAudio|ProfileRepository|SharedPreferences|MatchFactory)\b', renderer)
    assert 'audioCues' not in renderer, 'Drawing cannot consume match events'

for path in source.rglob('*.java'):
    text = path.read_text()
    for block in re.findall(r'/\*.*?\*/', text, re.S):
        assert len(block.splitlines()) <= 2, str(path) + ' has a long block comment'
    consecutive = 0
    for number, line in enumerate(text.splitlines(), 1):
        consecutive = consecutive + 1 if line.lstrip().startswith('//') else 0
        assert consecutive <= 2, str(path) + ':' + str(number) + ' has a long comment group'

print('Architecture checks passed: platform-free domain/application, independent adapters, thin activity and one/two-line comments.')
