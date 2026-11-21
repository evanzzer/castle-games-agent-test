"""Author stages 121-150: thirty symmetric three-faction battlefields with new river/diamond terrain.

Red (player) holds the left half, blue its mirror image, and yellow starts with its own keeps on the
centre column, so three armies are active from the first second. Every stage is exactly symmetric under
D2, ROT or MIRROR, has its own terrain PNG and building count, and takes its background from a 3-2-2-3
mix of the four map biomes. Difficulty is a ledger chosen per stage from measured win rates.
"""
from pathlib import Path
import hashlib, json, math, random
import generate_citadel as C
ROOT = C.ROOT; W, H = C.W, C.H
NAMES = ['Three Banners', 'Triple Meridian', 'Amber Divide', 'Frozen Triad', 'Sand Crossing', 'Dueling Rivers',
 'Crimson Axis', 'Glass Delta', 'Quiet Harbor', 'Ninefold Gate', 'Salt Marches', 'Winter Court',
 'Twin Fords', 'Ashen Sovereigns', 'Shattered Ridge', 'The Long Table', 'Poison Crown', 'Rivermouth',
 'Kingmaker', 'Pale Reach', 'Seventh Seal', 'Zenith Gambit', 'Cold Alliance', 'Sunken Throne',
 'Tripartite Siege', 'Iron Meridian', 'Last Banner', 'Hollow Zenith', 'Crown of Three', 'Zenith Sovereign']
# Yellow column entries (id, kind, size, distance from the centre line) in claim order.
YCOL = [('Y0', 'house', 'b', 230), ('Y1', 'house', 'm', 172), ('Y2', 'mine', '', 118)]

def bg_plan(i): return 'map/backgrounds/map%d.png' % ([1, 1, 1, 2, 2, 3, 3, 4, 4, 4][i % 10])

def channel(points, width):
    smooth = []
    for a, b in zip(points, points[1:]):
        for k in range(8): smooth.append((a[0] + (b[0] - a[0]) * k / 8, a[1] + (b[1] - a[1]) * k / 8))
    smooth.append(points[-1]); left, right = [], []
    for i, (x, y) in enumerate(smooth):
        a = smooth[max(0, i - 1)]; b = smooth[min(len(smooth) - 1, i + 1)]
        dx, dy = b[0] - a[0], b[1] - a[1]; ln = math.hypot(dx, dy) or 1; w = width * (1 + .1 * math.sin(i * .8))
        left.append((x - dy / ln * w, y + dx / ln * w)); right.append((x + dy / ln * w, y - dx / ln * w))
    return left + right[::-1]
def diamond(cx, cy, rx, ry, n=56):
    pts = []
    for j in range(n):
        a = 2 * math.pi * j / n; r = 1 / (abs(math.cos(a)) ** 1.6 + abs(math.sin(a)) ** 1.6) ** (1 / 1.6)
        pts.append((cx + math.cos(a) * rx * r * (1 + .03 * math.cos(4 * a)), cy + math.sin(a) * ry * r * (1 + .03 * math.cos(4 * a))))
    return pts

FAILS = {}
def layout(i):
    for attempt in range(600):
        lay = try_layout(i, attempt)
        if lay: return lay
    raise RuntimeError('no valid layout for stage %d: %s' % (121 + i, FAILS))

def try_layout(i, attempt):
    rng = random.Random(121000 + i * 101 + attempt * 17)
    group = C.GROUPS[(i + i // 4 + 1) % 3]
    sx, sy = rng.uniform(.9, 1.08), rng.uniform(.88, 1.06)
    deform = lambda x, y: (480 - (480 - x) * sx, 320 + (y - 320) * sy)
    jit = lambda s: rng.uniform(-s, s)
    keep = {'H0', 'H1n', 'H1s'}
    for grp in rng.sample(C.OPTIONAL, max(2, min(8, 2 + (i * 5) // 29 + rng.choice([0, 1, 2, 3])))): keep.update(grp)
    template = [t for t in C.TEMPLATE if t[0] in keep]; sites = {}
    if group == 'D2':
        for sid, kind, size, x, y in template:
            if sid.endswith('s') and sid != 'S0' and sid[:-1] + 'n' in [t[0] for t in C.TEMPLATE]: continue
            nx, ny = deform(x, y)
            if sid in C.AXIS_SITES: sites[sid + 'L'] = (kind, size, nx + jit(14), 320)
            else:
                nx += jit(22); ny = min(ny + jit(28), 290); sites[sid + 'L'] = (kind, size, nx, ny); sites[sid[:-1] + 's' + 'L'] = (kind, size, nx, H - ny)
    else:
        for sid, kind, size, x, y in template:
            nx, ny = deform(x, y); sites[sid + 'L'] = (kind, size, min(nx + jit(26), 440), min(max(ny + jit(38), 60), 580))
    claim = [s[0] + 'L' for s in template if s[0] + 'L' in sites]
    if len(claim) < 7: return None
    right = C.rot if group == 'ROT' else C.mx
    for sid in list(sites):
        kind, size, x, y = sites[sid]; rx, ry = right((x, y)); sites[sid[:-1] + 'R'] = (kind, size, rx, ry)
    # yellow's column keeps and the optional centre castle
    ycount = rng.choice([2, 3, 3]); yclaim = []
    for k, (sid, kind, size, d) in enumerate(YCOL[:ycount]):
        d += jit(8)
        if group == 'D2': a, b = (480, 320 - d), (480, 320 + d)
        elif group == 'MIRROR': a, b = (480, 320 - d + jit(10)), (480, 320 + d + jit(10))
        else:
            dx = rng.uniform(20, 55) * (1 if k % 2 == 0 else -1); a = (480 + dx, 320 - d); b = C.rot(a)
        sites[sid + 'n'] = (kind, size) + a; sites[sid + 's'] = (kind, size) + b; yclaim += [sid + 'n', sid + 's']
    if rng.random() < .85: sites['C0'] = ('house', 'b', 480, 320)
    pts = [(x, y) for _, _, x, y in sites.values()]
    if not (all(40 < x < 920 and 40 < y < 600 for x, y in pts)
            and all(math.hypot(a[0] - b[0], a[1] - b[1]) > 56 for ii, a in enumerate(pts) for b in pts[ii + 1:])):
        FAILS['sites'] = FAILS.get('sites', 0) + 1; return None
    regions = []
    def clear(poly):
        return (all(C.poly_distance(p, poly) > 22 for p in pts) and all(
            C.poly_distance(v, other) > 10 and C.poly_distance(ov, poly) > 10 for _, other in regions for v in poly[::6] for ov in other[::6]))
    for _ in range(rng.choice([2, 2, 3])):
        for _try in range(250):
            eff = rng.choice([50, 51]); roll = rng.random()
            if roll < .45:
                x0, y0 = rng.uniform(120, 260), rng.uniform(70, 570); way = [(x0, y0)]
                for _k in range(rng.choice([2, 3, 4])): way.append((min(440, way[-1][0] + rng.uniform(40, 110)), min(600, max(40, way[-1][1] + rng.uniform(-150, 150)))))
                poly = channel(way, rng.uniform(16, 26))
            elif roll < .75: poly = diamond(rng.uniform(170, 410), rng.uniform(120, 520), rng.uniform(34, 62), rng.uniform(46, 90))
            else: poly = C.crescent(rng.uniform(170, 400), rng.uniform(120, 520), rng.uniform(50, 85), rng.uniform(65, 110), rng.uniform(24, 34), rng.uniform(0, 360), rng.uniform(130, 190))
            if not all(10 < x < 470 and 10 < y < 630 for x, y in poly): continue
            polys = [[tr(p) for p in poly] for tr in C.transforms(group)]
            if clear(polys[0]) and all(C.poly_distance(p, gp) > 22 for gp in polys for p in pts):
                for gp in polys: regions.append((eff, gp))
                break
    if rng.random() < .5:
        ell = C.ellipse(480, 320, rng.uniform(30, 44), rng.uniform(34, 50))
        if all(C.poly_distance(p, ell) > 22 for p in pts): regions.append((rng.choice([50, 51]), ell))
    if len(regions) < 2: FAILS['regions'] = FAILS.get('regions', 0) + 1; return None
    altars = []
    for k in range(2):
        for _try in range(100):
            p = (rng.uniform(300, 420), rng.uniform(150, 490)); imgs = [tr(p) for tr in C.transforms(group)]
            if all(C.poly_distance(q, poly) > 12 for q in imgs for _, poly in regions) and all(math.hypot(q[0] - a[0], q[1] - a[1]) > 40 for q in imgs for a in pts):
                altars += [(*q, 100 if k == 0 else 101) for q in imgs]; break
    if len(altars) < 2 * len(C.transforms(group)): FAILS['altars'] = FAILS.get('altars', 0) + 1; return None
    return dict(stage=121 + i, group=group, sites=sites, claim=claim, yclaim=yclaim, regions=regions, altars=altars, scenery=C.scenery(rng, group, pts))

def _candidates():
    rng = random.Random(11); rows = set()
    while len(rows) < 48:
        rows.add((rng.randint(0, 6), rng.randint(0, 9), rng.choice([2, 3]), rng.choice(['g', 'g', 'y'])))
    return [dict(kR=r[0], kB=r[1], nY=r[2], c0=r[3]) for r in sorted(rows)]
CANDIDATES = _candidates()
def ladder(index): return CANDIDATES[index]

def yellow_houses(index):
    """Yellow's own house count (3-5), chosen independently of red/blue's equal count."""
    return 3 + (index * 5 + 1) % 3

def owners(lay, row):
    own, kinds, cap = C.owners(lay, row, first=121)
    ykinds = {sid: v[0] for sid, v in lay['sites'].items()}
    want = yellow_houses(lay['stage'] - 121) - (1 if row['c0'] == 'y' and 'C0' in lay['sites'] else 0)
    C.assign(lay['yclaim'], min(want, 4), 2 if row['nY'] == 3 else 0, 'y', own, ykinds)
    if 'C0' in lay['sites']: own['C0'] = row['c0']
    return own

def buildings(lay, row):
    own = owners(lay, row); out = []
    for sid, (kind, size, x, y) in lay['sites'].items():
        out.append((own[sid], C.record(sid, lay, own[sid], size)))
    out.sort(key=lambda t: 'rbyg'.index(t[0]))
    return ';'.join(r for _, r in out), own

def brief(lay, row, own):
    kinds = {sid: v[0] for sid, v in lay['sites'].items()}
    n = lambda c, house: sum(1 for sid, v in own.items() if v == c and (kinds[sid] == 'house') == house)
    text = 'Red/blue %d houses. Posts: you %d, blue %d. Yellow %d houses.' % (n('r', True), n('r', False), n('b', False), n('y', True))
    assert len(text) <= 73, text; return text

def markers():
    """Every campaign shares the Original map's stage positions, so the preview path looks the same."""
    return json.loads((ROOT / 'assets/map/campaigns.json').read_text())['campaigns']['original']['markers']

LAYOUTS = None
def layouts():
    global LAYOUTS
    if LAYOUTS is None: LAYOUTS = [layout(i) for i in range(30)]
    return LAYOUTS
def levels():
    p = ROOT / 'tools/data/zenith-ladder.json'
    return json.loads(p.read_text()) if p.exists() else [i * 47 // 29 for i in range(30)]

def main():
    lays, lv = layouts(), levels(); d = ROOT / 'assets/terrain/zenith'; d.mkdir(parents=True, exist_ok=True)
    worlds, names, briefs, blds, marsh, alt, catalog, manifest = [], [], [], [], [], [], [], {}
    for i, lay in enumerate(lays):
        row = ladder(lv[i]); recs, own = buildings(lay, row); b = brief(lay, row, own); stage = 121 + i
        rs = C.region_string(lay); png = f'terrain{stage}.png'
        C.bake_png(d / png, lay['regions']); manifest[png] = hashlib.sha256(rs.encode()).hexdigest()
        worlds.append(dict(width=W, height=H, overlay=[f'terrain/zenith/{png}', 480, 320, W, H], objects=lay['scenery'], custom=True, name=NAMES[i], brief=b, new_terrain=True, background=bg_plan(i)))
        names.append(NAMES[i]); briefs.append(b); blds.append(recs); marsh.append(rs)
        alt.append(';'.join(f'{a[0]:.2f},{a[1]:.2f},{a[2]}'.replace('.00,', ',') for a in lay['altars']))
        catalog.append(dict(stage=stage, name=NAMES[i], brief=b, symmetry=lay['group'], background=bg_plan(i), level=lv[i], program=row, owners=own,
                            regions=[dict(type=e, points=[[round(x, 2), round(y, 2)] for x, y in p]) for e, p in lay['regions']]))
    assert len(set(blds)) == 30 and len(set(marsh)) == 30 and len(set(manifest.values())) == 30, 'every stage must be unique'
    (d / 'worlds.json').write_text(json.dumps(dict(schema_version=1, campaign='zenith', first_stage=121, worlds=worlds), indent=2, ensure_ascii=False) + '\n')
    (d / 'manifest.json').write_text(json.dumps(dict(schema_version=1, overlays=manifest), indent=2) + '\n')
    nav = ROOT / 'assets/map/campaigns.json'; data = json.loads(nav.read_text()); data['campaigns']['zenith'] = dict(markers=markers()); nav.write_text(json.dumps(data, indent=2) + '\n')
    root_manifest = ROOT / 'assets/manifest.json'; rm = json.loads(root_manifest.read_text())
    rm['terrain']['zenith'] = {'manifest': 'terrain/zenith/manifest.json', 'worlds': 'terrain/zenith/worlds.json'}; root_manifest.write_text(json.dumps(rm, indent=2) + '\n')
    (ROOT / 'src/castle/domain/stages/data/Zenith.java').write_text(C.java_source([('NAMES', names), ('BRIEFS', briefs), ('BUILDINGS', blds), ('MARSHES', marsh), ('ALTARS', alt)]).replace('class Citadel', 'class Zenith'))
    (ROOT / 'tools/data/zenith-design.json').write_text(json.dumps(catalog, indent=2, ensure_ascii=False) + '\n')
    print('Authored stages 121-150:', sum(len(l['regions']) for l in lays), 'symmetric regions across 30 unique overlays.')
if __name__ == '__main__': main()
