"""Read-only dependency discovery in the user's pack; copy only to this isolated project's evidence."""
import hashlib
import io
import json
import shutil
import tomllib
from pathlib import Path
from zipfile import ZipFile

base = Path(__file__).resolve().parents[1]
dest = base / 'evidence/collab-jars'
pack = Path(r'E:\MC\All the mods 9 TC\.minecraft\versions\All the Mods 9\mods')

def metadata(source):
    ids, dependencies = set(), set()
    with ZipFile(source) as z:
        if 'META-INF/mods.toml' in z.namelist():
            doc = tomllib.loads(z.read('META-INF/mods.toml').decode('utf-8-sig'))
            ids.update(row['modId'] for row in doc.get('mods', []))
            groups = doc.get('dependencies', {})
            groups = groups.values() if isinstance(groups, dict) else [groups]
            dependencies.update(row['modId'] for rows in groups for row in rows
                                if row.get('mandatory') and row.get('side') != 'CLIENT')
        if 'META-INF/jarjar/metadata.json' in z.namelist():
            for row in json.loads(z.read('META-INF/jarjar/metadata.json')).get('jars', []):
                nested_ids, nested_deps = metadata(io.BytesIO(z.read(row['path'])))
                ids.update(nested_ids)
                dependencies.update(nested_deps)
    return ids, dependencies

manifest = dest / 'manifest.json'
rows = json.loads(manifest.read_text(encoding='utf-8'))
available = {}
for jar in sorted(pack.glob('*.jar')):
    for name in metadata(jar)[0]: available[name] = jar
provided = {'minecraft', 'forge', 'thaumcraft', 'thaumicomniverse', 'curios', 'terrablender'}
required = {'jei', 'allthemodium', 'allthetweaks', 'kubejs', 'mysticalagradditions'}
for jar in dest.glob('*.jar'):
    ids, deps = metadata(jar)
    provided.update(ids)
    required.update(deps)
added = []
while required - provided:
    name = sorted(required - provided)[0]
    if name not in available: raise ValueError('Missing required dependency: ' + name)
    source = available[name]
    target = dest / (name + '-local.jar')
    shutil.copy2(source, target)
    ids, deps = metadata(target)
    provided.update(ids)
    required.update(deps)
    added.append(name)
    rows = [row for row in rows if row['module'] != name]
    rows.append({'module': name, 'source': str(source), 'file': target.name,
                 'sha256': hashlib.sha256(target.read_bytes()).hexdigest()})
manifest.write_text(json.dumps(rows, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
(dest / 'dependency-closure.json').write_text(json.dumps({'provided': sorted(provided), 'required': sorted(required),
    'added': added}, indent=2) + '\n', encoding='utf-8')
print('Copied dependency closure: ' + ', '.join(added))
