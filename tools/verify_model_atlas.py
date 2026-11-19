"""Check TC baked-model texture files and block-atlas coverage without starting a client."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACKS = [ROOT / 'src/main/resources', ROOT / 'src/generated/resources']

def read(path):
    return json.loads(path.read_text(encoding='utf-8'))

def exists(relative):
    return any((pack / relative).is_file() for pack in PACKS)

atlas = read(PACKS[1] / 'assets/minecraft/atlases/blocks.json')
singles = {entry.get('sprite', entry['resource']) for entry in atlas['sources']
           if entry['type'] == 'minecraft:single'}
errors = []
models = 0
sprites = set()
for pack in PACKS:
    for model in (pack / 'assets/thaumcraft/models').rglob('*.json'):
        models += 1
        for sprite in read(model).get('textures', {}).values():
            if not isinstance(sprite, str) or not sprite.startswith('thaumcraft:'):
                continue
            sprites.add(sprite)
            namespace, path = sprite.split(':', 1)
            if not exists(f'assets/{namespace}/textures/{path}.png'):
                errors.append(f'{model.relative_to(ROOT)}: texture file missing: {sprite}')
            if not path.startswith(('block/', 'item/')) and sprite not in singles:
                errors.append(f'{model.relative_to(ROOT)}: sprite absent from block atlas: {sprite}')
for sprite in singles:
    namespace, path = sprite.split(':', 1)
    if not exists(f'assets/{namespace}/textures/{path}.png'):
        errors.append(f'atlas single references missing file: {sprite}')
barrier = read(PACKS[1] / 'assets/thaumcraft/blockstates/warding_barrier.json')
barrier_model = barrier['variants']['']['model']
namespace, path = barrier_model.split(':', 1)
if not exists(f'assets/{namespace}/models/{path}.json'):
    errors.append('warding barrier model missing')
if errors:
    raise SystemExit('\n'.join(errors))
print(f'PASS: {models} models, {len(sprites)} distinct TC texture references, {len(singles)} explicit atlas sprites; barrier model resolves.')
