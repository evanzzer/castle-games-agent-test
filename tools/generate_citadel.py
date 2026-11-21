"""Author stages 91-120: thirty distinct, individually symmetric battlefields.

Each stage picks a symmetry group (D2 = mirror on both axes, MIRROR = x-mirror, ROT = 180-degree turn),
derives every building, terrain region, altar and scenery object from one half so the board is exactly
symmetric, and gets its own terrain PNG. Difficulty is a ledger of deliberate departures from symmetry: who owns which site (CANDIDATES),
chosen per stage from tools/data/citadel-ladder.json after measuring each candidate on that stage.
"""
from pathlib import Path
import hashlib, json, math, random
ROOT = Path(__file__).resolve().parents[1]
W, H = 960, 640
GROUPS = ['D2', 'ROT', 'MIRROR']
NAMES = ['First Reflection', 'Twin Keeps', 'Quiet Meridian', 'Glass Border', 'Turning Tide', 'Level Ground',
 'Mirrored Mines', 'Pivot of Dusk', 'Narrow Margin', 'Yellow Wedge', 'Cold Symmetry', 'Borrowed Time',
 'Towers of Dusk', 'Shared Crown', 'Hairline Fracture', 'Counterweight', 'Split Allegiance', 'Last Neutral',
 'The Lone Rival', 'Vacant Seats', 'Stolen North', 'Stolen South', 'Closing Pincer', 'Heavy Hand',
 'Annexed Gate', 'Broken Mirror', 'Unequal Crown', 'Citadel Siege', 'Mirror Eclipse', 'Citadel Sovereign']

# Template left-half sites in claim order: (id, kind, size, x, y). South entries mirror north under D2.
TEMPLATE = [
 ('H0', 'house', 'b', 90, 320), ('H1n', 'house', 'm', 110, 120), ('H1s', 'house', 'm', 110, 520),
 ('MNn', 'mine', '', 265, 150), ('MNs', 'mine', '', 265, 490), ('S0', 'house', 's', 235, 320),
 ('Qn', 'house', 'm', 300, 235), ('Qs', 'house', 'm', 300, 405), ('M0', 'mine', '', 395, 320),
 ('TMn', 'tower', 'm', 190, 225), ('TMs', 'tower', 'm', 190, 415),
 ('TWn', 'tower', 's', 390, 140), ('TWs', 'tower', 's', 390, 500),
 ('Fn', 'house', 's', 200, 90), ('Fs', 'house', 's', 200, 550), ('Gn', 'mine', '', 335, 70), ('Gs', 'mine', '', 335, 570)]
# Optional mirrored groups: each stage keeps a different subset, so building counts differ per stage.
OPTIONAL = [['MNn', 'MNs'], ['S0'], ['Qn', 'Qs'], ['M0'], ['TMn', 'TMs'], ['TWn', 'TWs'], ['Fn', 'Fs'], ['Gn', 'Gs']]
AXIS_SITES = ('H0', 'S0', 'M0')   # live on y=320 under D2
PIVOT = {('house', 'b', 'c'): 10, ('house', 'm', 'c'): 14, ('house', 's', 'c'): 19,
         ('house', 'b', 'g'): 10, ('house', 'm', 'g'): 21, ('house', 's', 'g'): 19,
         ('tower', 'b', 'g'): 46, ('tower', 'm', 'g'): 42, ('tower', 's', 'g'): 37}
mx = lambda p: (W - p[0], p[1]); my = lambda p: (p[0], H - p[1]); rot = lambda p: (W - p[0], H - p[1])
ident = lambda p: p
def transforms(group):
    return [ident, mx, my, rot] if group == 'D2' else [ident, rot if group == 'ROT' else mx]

def wobble(a): return 1 + .045 * math.cos(4 * a) + .03 * math.cos(6 * a)
def ellipse(cx, cy, rx, ry, n=56, tilt=0):
    out = []
    for j in range(n):
        a = 2 * math.pi * j / n; r = wobble(a); ex, ey = math.cos(a) * rx * r, math.sin(a) * ry * r
        out.append((cx + ex * math.cos(tilt) - ey * math.sin(tilt), cy + ex * math.sin(tilt) + ey * math.cos(tilt)))
    return out
def crescent(cx, cy, rx, ry, width, start, sweep, n=30):
    o = [(cx + math.cos(math.radians(start + sweep * j / n)) * rx, cy + math.sin(math.radians(start + sweep * j / n)) * ry) for j in range(n + 1)]
    i_ = [(cx + math.cos(math.radians(start + sweep * j / n)) * (rx - width), cy + math.sin(math.radians(start + sweep * j / n)) * (ry - width)) for j in range(n + 1)]
    return o + i_[::-1]

def poly_distance(pt, poly):
    inside = False; n = len(poly); best = 1e9
    for k in range(n):
        x1, y1 = poly[k]; x2, y2 = poly[(k + 1) % n]
        if (y1 > pt[1]) != (y2 > pt[1]) and pt[0] < (x2 - x1) * (pt[1] - y1) / (y2 - y1) + x1: inside = not inside
        best = min(best, math.hypot(pt[0] - x1, pt[1] - y1))
    return 0 if inside else best

FAILS = {}
def layout(i):
    for attempt in range(400):
        lay = try_layout(i, attempt)
        if lay: return lay
    raise RuntimeError('no valid layout for stage %d: %s' % (91 + i, FAILS))

def try_layout(i, attempt):
    rng = random.Random(91000 + i * 97 + attempt * 13)
    group = GROUPS[(i + i // 3) % 3]
    sx, sy = rng.uniform(.9, 1.08), rng.uniform(.88, 1.06)
    deform = lambda x, y: (480 - (480 - x) * sx, 320 + (y - 320) * sy)
    jit = lambda s: rng.uniform(-s, s)
    sites, claim = {}, []
    keep = {'H0', 'H1n', 'H1s'}
    for grp in rng.sample(OPTIONAL, max(2, min(8, 2 + (i * 5) // 29 + rng.choice([0, 1, 2, 3])))): keep.update(grp)
    template = [t for t in TEMPLATE if t[0] in keep]
    if group == 'D2':
        for sid, kind, size, x, y in template:
            if sid.endswith('s') and sid not in ('S0',) and sid[:-1] + 'n' in [t[0] for t in TEMPLATE]: continue
            nx, ny = deform(x, y)
            if sid in AXIS_SITES: sites[sid + 'L'] = (kind, size, nx + jit(14), 320)
            else:
                nx += jit(22); ny = min(ny + jit(28), 290)
                sites[sid + 'L'] = (kind, size, nx, ny); sites[sid[:-1] + 's' + 'L'] = (kind, size, nx, H - ny)
    else:
        for sid, kind, size, x, y in template:
            nx, ny = deform(x, y); sites[sid + 'L'] = (kind, size, min(nx + jit(26), 440), min(max(ny + jit(38), 60), 580))
    claim = [s[0] + 'L' for s in template if s[0] + 'L' in sites]
    if len(claim) < 7: return None
    right = rot if group == 'ROT' else mx
    for sid in list(sites):
        kind, size, x, y = sites[sid]; rx, ry = right((x, y)); sites[sid[:-1] + 'R'] = (kind, size, rx, ry)
    if rng.random() < .85: sites['C0'] = ('house', 'b', 480, 320)
    keep_t, keep_y = rng.random() < .7, rng.random() < .8
    d1, d2 = rng.uniform(92, 130), rng.uniform(205, 240)
    if group == 'D2': a = [(480, 320 - d1), (480, 320 + d1)]; b = [(480, 320 - d2), (480, 320 + d2)]
    elif group == 'MIRROR': a = [(480, 320 - d1 + jit(12)), (480, 320 + d1 + jit(12))]; b = [(480, 320 - d2 + jit(8)), (480, 320 + d2 + jit(8))]
    else:
        dx = rng.uniform(25, 60); p = (480 + dx, 320 - d1); q = (480 - dx * .8, 320 - d2); a = [p, rot(p)]; b = [q, rot(q)]
    if keep_t: sites['T1n'] = ('tower', 'b') + a[0]; sites['T1s'] = ('tower', 'b') + a[1]
    if keep_y: sites['Y1n'] = ('house', 'm') + b[0]; sites['Y1s'] = ('house', 'm') + b[1]
    pts = [(x, y) for _, _, x, y in sites.values()]
    if not (all(40 < x < 920 and 40 < y < 600 for x, y in pts)
            and all(math.hypot(a[0] - b[0], a[1] - b[1]) > 56 for ii, a in enumerate(pts) for b in pts[ii + 1:])):
        FAILS['sites'] = FAILS.get('sites', 0) + 1; return None
    regions = []
    def clear(poly):
        return (all(poly_distance(p, poly) > 22 for p in pts) and all(
            poly_distance(v, other) > 10 and poly_distance(ov, poly) > 10 for _, other in regions for v in poly[::6] for ov in other[::6]))
    for _ in range(rng.choice([2, 2, 3])):
        for _try in range(200):
            eff = rng.choice([50, 51]); cx, cy = rng.uniform(160, 400), rng.uniform(110, 530)
            poly = crescent(cx, cy, rng.uniform(55, 95), rng.uniform(70, 120), rng.uniform(26, 38), rng.uniform(0, 360), rng.uniform(130, 200)) \
                if rng.random() < .4 else ellipse(cx, cy, rng.uniform(28, 62), rng.uniform(50, 105), tilt=rng.uniform(-.7, .7))
            if not all(10 < x < 470 for x, y in poly): continue
            group_polys = [[tr(p) for p in poly] for tr in transforms(group)]
            staged = list(regions)
            if all(clear(gp) for gp in group_polys[:1]) and all(poly_distance(p, gp) > 22 for gp in group_polys for p in pts):
                for gp in group_polys: regions.append((eff, gp))
                break
    if rng.random() < .55:
        ell = ellipse(480, 320 if group != 'MIRROR' else rng.uniform(300, 340), rng.uniform(34, 52), rng.uniform(40, 62))
        if all(poly_distance(p, ell) > 22 for p in pts): regions.append((rng.choice([50, 51]), ell))
    if len(regions) < 2: FAILS['regions'] = FAILS.get('regions', 0) + 1; return None
    altars = []
    for k in range(2):
        for _try in range(100):
            p = (rng.uniform(300, 420), rng.uniform(150, 490)); imgs = [tr(p) for tr in transforms(group)]
            if all(poly_distance(q, poly) > 12 for q in imgs for _, poly in regions) and all(math.hypot(q[0] - a[0], q[1] - a[1]) > 40 for q in imgs for a in pts):
                altars += [(*q, 100 if k == 0 else 101) for q in imgs]; break
    if len(altars) < 2 * len(transforms(group)): FAILS['altars'] = FAILS.get('altars', 0) + 1; return None
    return dict(stage=91 + i, group=group, sites=sites, claim=claim, regions=regions, altars=altars, scenery=scenery(rng, group, pts))

def scenery(rng, group, pts):
    quad = []
    while len(quad) < 13:
        x, y = rng.randint(25, 470), rng.randint(25, 315)
        if any(math.hypot(x - a, y - b) < 70 for a, b in pts) or any(math.hypot(x - a, y - b) < 45 for a, b, _ in quad): continue
        quad.append((x, y, rng.randint(1, 5)))
    return [[f'g_{t}', round(tr((x, y))[0]), round(tr((x, y))[1]), 32, 42] for x, y, t in quad for tr in transforms(group)]

def _candidates():
    """48 sampled ownership ledgers concentrated around parity; each stage picks the one that measures right."""
    rng = random.Random(7); rows = set()
    while len(rows) < 48:
        y1 = rng.choice(['y', 'y', 'g', 'b']); t1 = rng.choice(['g', 'y', 'b']) if y1 == 'y' else rng.choice(['g', 'g', 'b'])
        rows.add((rng.choice([4, 5, 5, 6, 6]), rng.randint(3, 10), y1, t1, rng.choice(['g', 'g', 'b']), rng.choice([0, 0, 1, 2])))
    return [dict(nR=r[0], nB=r[1], y1=r[2], t1=r[3], c0=r[4], promote=r[5]) for r in sorted(rows)]
CANDIDATES = _candidates()
def ladder(index): return CANDIDATES[index]

def owners(lay, row):
    own = {}
    for k, sid in enumerate(lay['claim']):
        own[sid] = 'r' if k < row['nR'] else 'g'; own[sid[:-1] + 'R'] = 'b' if k < row['nB'] else 'g'
    for sid in ('Y1n', 'Y1s'):
        if sid in lay['sites']: own[sid] = row['y1'] if row['y1'] != 'b' or sid == 'Y1n' or row['promote'] >= 1 else 'g'
    for sid in ('T1n', 'T1s'):
        if sid in lay['sites']: own[sid] = row['t1']
    if 'C0' in lay['sites']: own['C0'] = row['c0']
    return own

def record(sid, lay, side, size):
    kind, _, x, y = lay['sites'][sid]; x, y = round(x, 2), round(y, 2)
    if kind == 'mine': name, pivot = f'{side}_mine', 9
    elif kind == 'tower': name, pivot = f'{side}_tower_{size}', PIVOT[('tower', size, 'g')]
    else: name, pivot = f'{side}_house_{size}', PIVOT[('house', size, 'g' if side == 'g' else 'c')]
    return f'{name},{x:g},{y:g},{x:g},{y - pivot:g}'

def buildings(lay, row):
    own = owners(lay, row); out = []; bump = {'s': 'm', 'm': 'b', 'b': 'b'}
    promoted = [s[:-1] + 'R' for s in lay['claim'] if lay['sites'][s][0] == 'house'][:row['promote'] * 2]
    for sid, (kind, size, x, y) in lay['sites'].items():
        side = own[sid]
        if side == 'b' and kind == 'house' and sid in promoted: size = bump[size]
        out.append((side, record(sid, lay, side, size)))
    out.sort(key=lambda t: 'rbyg'.index(t[0]))
    return ';'.join(r for _, r in out), own

def brief(row, own):
    r = sum(v == 'r' for v in own.values()); b = sum(v == 'b' for v in own.values()); y = sum(v == 'y' for v in own.values())
    parts = ['Mirror: %d keeps each.' % r if r == b else 'You %d, foe %d keeps.' % (r, b)]
    if y: parts.append('Yellow holds %d.' % y)
    if row['c0'] == 'b': parts.append('Foe holds centre.')
    if row['promote']: parts.append('Foe fortified.')
    text = ' '.join(parts); assert len(text) <= 73, text; return text

def markers():
    return [[round(55 + t * 730, 1), round(150 + 95 * math.sin(t * math.pi * 3.2 + .6) * (1 - .25 * t), 1)] for t in (i / 29 for i in range(30))]

def region_string(lay):
    return ';'.join(str(e) + ',' + ','.join(f'{x:g},{y:g}' for x, y in [(round(x, 2), round(y, 2)) for x, y in p]) for e, p in lay['regions'])

def bake_png(path, regions):
    from PIL import Image, ImageDraw, ImageFilter, ImageChops
    root = ROOT / 'assets/terrain/original'
    def tile(name, x, y, w, h, base):
        src = Image.open(root / name).convert('RGBA').crop((x, y, x + w, y + h))
        bg = Image.new('RGBA', (w, h), base); bg.alpha_composite(src); return bg
    slow = tile('terrain5.png', 210, 30, 64, 64, (0x4c, 0x38, 0x28, 255)); poison = tile('terrain7.png', 310, 170, 48, 48, (0x31, 0x40, 0x1e, 255))
    out = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    for index, (effect, pts) in enumerate(regions):
        mat = slow if effect == 51 else poison; scale = 1.1 + index * .025
        mw, mh = int(mat.width * scale), int(mat.height * scale); m = mat.resize((mw, mh), Image.BILINEAR)
        big = Image.new('RGBA', (mw * 2, mh * 2)); big.paste(m, (0, 0)); big.paste(m.transpose(Image.FLIP_LEFT_RIGHT), (mw, 0))
        big.paste(m.transpose(Image.FLIP_TOP_BOTTOM), (0, mh)); big.paste(m.transpose(Image.ROTATE_180), (mw, mh))
        layer = Image.new('RGBA', (W, H)); ox, oy = (index * 29) % (mw * 2), (index * 47) % (mh * 2)
        for ty in range(-oy, H, mh * 2):
            for tx in range(-ox, W, mw * 2): layer.paste(big, (tx, ty))
        shape = Image.new('L', (W, H), 0); ImageDraw.Draw(shape).polygon(pts, fill=255)
        shadow = Image.new('RGBA', (W, H), (0x40, 0x2c, 0x1a, 0x88)); shadow.putalpha(ImageChops.multiply(shape.filter(ImageFilter.GaussianBlur(5.5)), Image.new('L', (W, H), 0x88)))
        out.alpha_composite(shadow); layer.putalpha(shape.filter(ImageFilter.GaussianBlur(3))); out.alpha_composite(layer)
    out.save(path, optimize=True)

LAYOUTS = None
def layouts():
    global LAYOUTS
    if LAYOUTS is None: LAYOUTS = [layout(i) for i in range(30)]
    return LAYOUTS
def levels():
    p = ROOT / 'tools/data/citadel-ladder.json'
    return json.loads(p.read_text()) if p.exists() else [i * 47 // 29 for i in range(30)]

def java_source(rows):
    def arr(n, v): return ' static final String[] %s = {\n' % n + ''.join('  ' + json.dumps(x, ensure_ascii=False) + ',\n' for x in v) + ' };\n'
    return 'package castle;\nfinal class Citadel {\n' + ''.join(arr(n, v) for n, v in rows) + '}\n'

def main():
    lays, lv = layouts(), levels(); d = ROOT / 'assets/terrain/citadel'; d.mkdir(parents=True, exist_ok=True)
    worlds, names, briefs, blds, marsh, alt, catalog, manifest = [], [], [], [], [], [], [], {}
    for i, lay in enumerate(lays):
        row = ladder(lv[i]); recs, own = buildings(lay, row); b = brief(row, own); stage = 91 + i
        rs = region_string(lay); png = f'terrain{stage}.png'
        bake_png(d / png, lay['regions']); manifest[png] = hashlib.sha256(rs.encode()).hexdigest()
        worlds.append(dict(width=W, height=H, overlay=[f'terrain/citadel/{png}', 480, 320, W, H], objects=lay['scenery'], custom=True, name=NAMES[i], brief=b, new_terrain=True))
        names.append(NAMES[i]); briefs.append(b); blds.append(recs); marsh.append(rs)
        alt.append(';'.join(f'{a[0]:.2f},{a[1]:.2f},{a[2]}'.replace('.00,', ',') for a in lay['altars']))
        catalog.append(dict(stage=stage, name=NAMES[i], brief=b, symmetry=lay['group'], level=lv[i], program=row, owners=own,
                            regions=[dict(type=e, points=[[round(x, 2), round(y, 2)] for x, y in p]) for e, p in lay['regions']]))
    assert len(set(blds)) == 30 and len(set(marsh)) == 30 and len(set(manifest.values())) == 30, 'every stage must be unique'
    (d / 'worlds.json').write_text(json.dumps(dict(schema_version=1, campaign='citadel', first_stage=91, worlds=worlds), indent=2, ensure_ascii=False) + '\n')
    (d / 'manifest.json').write_text(json.dumps(dict(schema_version=1, overlays=manifest), indent=2) + '\n')
    nav = ROOT / 'assets/map/campaigns.json'; data = json.loads(nav.read_text())
    data['campaigns']['citadel'] = dict(markers=markers()); nav.write_text(json.dumps(data, indent=2) + '\n')
    (ROOT / 'src/castle/domain/stages/data/Citadel.java').write_text(java_source([('NAMES', names), ('BRIEFS', briefs), ('BUILDINGS', blds), ('MARSHES', marsh), ('ALTARS', alt)]))
    (ROOT / 'tools/data/citadel-design.json').write_text(json.dumps(catalog, indent=2, ensure_ascii=False) + '\n')
    print('Authored stages 91-120:', sum(len(l['regions']) for l in lays), 'symmetric regions across 30 unique overlays.')
if __name__ == '__main__': main()
