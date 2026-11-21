"""Retain original runtime translation keys without bundling legacy text."""
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def complete_aliases(data, archive):
    source = data['mapping']['assets/gadomancy/lang/en_us.json']
    keys = {
        line.split('=', 1)[0].strip()
        for line in archive.read(source).decode('utf-8-sig').splitlines()
        if '=' in line and not line.lstrip().startswith('#')
    }
    for aliases in data['languages'].values():
        for key in sorted(keys):
            aliases.setdefault(key, key)


if __name__ == '__main__':
    path = ROOT / 'src/main/resources/data/gadomancy/thaumcraft/original_resources.json'
    data = json.loads(path.read_text(encoding='utf-8'))
    with zipfile.ZipFile(ROOT.parent / 'references/phase2/Gadomancy/gadomancy-1.7.10-1.0.7.3.jar') as archive:
        complete_aliases(data, archive)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
