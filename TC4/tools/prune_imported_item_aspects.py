"""Remove 0917_3 item overrides that already have original aspect evidence.

Reads the immutable supplied pack, TC4's recorded 1.7.10 observations and all
local addon source resource packs (including optional compatibility packs).
No runtime calculation, client launch or server modification is performed.
Use --write to regenerate the cleaned table and per-item audit; default checks.
"""
from pathlib import Path
import argparse
import json
import zipfile

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / 'src/main/resources/data/tc4ai_aspects/item_aspects/ai_generated.json'
AUDIT = ROOT / 'docs/validation/2026-10-08-original-item-aspects-audit.json'


def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def collect():
    resources = [ROOT / 'src/main/resources', ROOT / 'src/generated/resources']
    resources += sorted((ROOT.parent / 'Addons').glob('*/src/main/resources'))
    resources += sorted((ROOT.parent / 'Addons').glob('*/src/generated/resources'))
    files = sorted({p for root in resources for p in root.rglob('*.json')
                    if p.parent.name in ('item_aspects', 'item_bonus_aspects',
                                         'recipe_aspect_inputs', 'legacy_item_mappings')})
    reasons, mappings, groups, registrations = {}, {}, {}, {}

    def mark(item, reason):
        reasons.setdefault(item, set()).add(reason)

    for p in files:
        if p == TARGET:
            continue
        data = read(p)
        source = p.relative_to(ROOT.parent).as_posix()
        kind = p.parent.name
        if kind in ('item_aspects', 'item_bonus_aspects'):
            for row in data['entries']:
                for item in row['items']:
                    mark(item, source)
        elif kind == 'legacy_item_mappings':
            for row in data['items']:
                mappings[row['item']] = (row['legacy_item'], row['metadata'])
            for row in data.get('spawn_eggs', []):
                mappings[row['item']] = ('minecraft:spawn_egg', row['metadata'])
            for row in data['groups']:
                groups[(row['item'], row['metadata'])] = row['group'][0]
        else:
            for row in data['registrations']:
                registrations.setdefault((row['item'], row['metadata']), set()).add(source)

    # Original observed results include both explicit and recipe-derived values.
    observations = {(row['item'], row['metadata']): row for row in
                    read(ROOT / 'docs/legacy-baseline/2026-09-08/resolved-vanilla-aspects.json')}
    for item, key in mappings.items():
        scan = (key[0], groups.get(key, key[1]))
        for candidate in (key, scan, (key[0], 32767)):
            for source in registrations.get(candidate, ()):
                mark(item, source + '#' + str(candidate))
        row = observations.get(scan, observations.get(key))
        if row and (row['aspects'] or row['canonical_aspects']):
            mark(item, 'legacy-observation:' + str(scan))
    # Modern IDs may be used directly in addon recipe registrations.
    for (item, metadata), sources in registrations.items():
        if metadata in (0, 32767):
            for source in sources:
                mark(item, source)

    with zipfile.ZipFile(ROOT / 'manual_test/0917_3/tc4_ai_aspects-1.0.0.jar') as pack:
        original = json.loads(pack.read('data/tc4ai_aspects/item_aspects/ai_generated.json'))
    kept, removed = [], []
    for row in original['entries']:
        surviving = []
        for item in row['items']:
            if item in reasons:
                removed.append({'item': item, 'removed_aspects': row['aspects'],
                                'original_sources': sorted(reasons[item])})
            else:
                surviving.append(item)
        if surviving:
            kept.append({**row, 'items': surviving})
    cleaned = {**original, 'entries': kept}
    audit = {'original_count': len(original['entries']), 'removed_count': len(removed),
             'remaining_count': len(kept), 'removed': removed}
    return cleaned, audit


if __name__ == '__main__':
    if (ROOT / 'src/main/resources/data/thaumcraft/item_aspects/explicit.json').exists():
        raise SystemExit('Retired after explicit aspect migration; edit item_aspects/explicit.json instead.')
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    cleaned, audit = collect()
    if args.write:
        for path, data in ((TARGET, cleaned), (AUDIT, audit)):
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    else:
        assert read(TARGET) == cleaned, 'Imported table has unpruned or changed definitions'
        assert read(AUDIT) == audit, 'Original definition audit is stale'
    print(json.dumps({k: v for k, v in audit.items() if k != 'removed'}, ensure_ascii=False))
