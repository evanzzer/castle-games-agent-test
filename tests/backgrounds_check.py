"""Keep the recovered Original assignments and authored campaign background sequences intact."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[1]
orders = json.loads((root / 'tools/data/background-orders.json').read_text())['campaigns']
original = [1,1,1,1,2,1,1,1,1,2,2,2,3,2,3,3,4,4,3,3,4,4,4,2,1,2,2,2,1,1]
assert orders['original'] == original
for campaign, order in orders.items():
    assert len(order) == 30 and set(order) == {1, 2, 3, 4}
    worlds = json.loads((root / 'assets/terrain' / campaign / 'worlds.json').read_text())['worlds']
    assert [w['background'] for w in worlds] == ['map/backgrounds/map%d.png' % n for n in order]
    for world in worlds:
        assert (root / 'assets' / world['background']).is_file()
assert orders['frontier'] != orders['wild'] != orders['original']
view = (root / 'src/castle/presentation/GameView.java').read_text()
assert 'painter.image(canvas,"map/backgrounds/map1.png"' not in view
print('Background checks passed: recovered Original mapping and distinct Frontier/Wild orders; all four images used.')
