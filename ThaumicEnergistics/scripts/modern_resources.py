"""Install the approved 0920_02 art after legacy resource generation.

The handoff remains the editable source of truth. PNGs must match its explicit
manifest; merely using a modern/ path does not authorize bundling another image.
"""
from pathlib import Path
import hashlib
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
HANDOFF = ROOT / 'art/0920_02-handoff'
RES = ROOT / 'src/main/resources'
MOD = 'thaumicenergistics'


def apply():
    manifest = json.loads((HANDOFF / 'manifest.json').read_text(encoding='utf8'))
    approved = {entry['file'].removeprefix('runtime/'): entry['sha256']
                for entry in manifest['textures']}
    runtime = HANDOFF / 'runtime'
    actual = {p.relative_to(runtime).as_posix() for p in (runtime / 'assets').rglob('*.png')}
    if actual != approved.keys():
        raise ValueError('Handoff PNG inventory differs from its approved manifest')
    for path, digest in approved.items():
        if hashlib.sha256((runtime / path).read_bytes()).hexdigest() != digest:
            raise ValueError(f'Handoff texture changed without updating its provenance: {path}')
    for source in sorted((runtime / 'assets').rglob('*')):
        if source.is_file():
            target = RES / source.relative_to(runtime)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
            if '/models/' in target.as_posix() and target.suffix == '.json':
                model = json.loads(target.read_text(encoding='utf8'))
                # Blockbench supplied face textures but omitted Minecraft's
                # separate breaking/landing particle material.
                if (model.get('elements') or model.get('parent') == 'ae2:block/drive/drive_cell') and model.get('textures') and 'particle' not in model['textures']:
                    model['textures']['particle'] = '#' + next(iter(model['textures']))
                    target.write_text(json.dumps(model, indent=2) + '\n', encoding='utf8')

    import resource_review
    resource_review.apply(approved)
    meta = RES / 'META-INF'
    meta.mkdir(exist_ok=True)
    (meta / 'thaumicenergistics-modern-assets.json').write_text(json.dumps({
        'handoff': '0920_02-resource-handoff.zip',
        'sha256': '3b0de5073da57c517a655714862d45fb99f51dd12fa9390609500f35ec05282f',
        'provenance': 'META-INF/thaumicenergistics-art-provenance.md',
        'textures': approved,
    }, indent=2) + '\n', encoding='utf8')
    shutil.copyfile(HANDOFF / 'PROVENANCE.md', meta / 'thaumicenergistics-art-provenance.md')
    with (meta / 'thaumicenergistics-art-provenance.md').open('a', encoding='utf8') as output:
        output.write('\n\n' + (ROOT / 'art/resource-review-0.2.6/PROVENANCE.md').read_text(encoding='utf8'))
    shutil.copyfile(ROOT / 'art/resource-review-0.2.6/LICENSE-AE2-LGPL-3.0.txt',
                    meta / 'LICENSE-AE2-LGPL-3.0.txt')

    # Retire only known replaced mappings, and only if no surviving asset/Java
    # references that path. Compatibility block forms may still use old art.
    retired = [f'items/essentia.cell.{tier}.png' for tier in ['1k', '4k', '16k', '64k', 'creative']]
    retired += [f'items/storage.component.{tier}.png' for tier in ['1k', '4k', '16k', '64k']]
    retired += ['items/essentia.cell.casing.png', 'models/arcane.assembler.png',
                'models/gearbox.iron.png', 'models/gearbox.thaumium.png',
                'blocks/parts/vis.interface.png', 'blocks/parts/arcane.crafting.side.png',
                'blocks/parts/arcane.crafting.overlay1.png', 'blocks/arcane.assembler.fallback.png',
                'blocks/gear.box.fallback.png', 'blocks/golem.gear.box.fallback.png',
                'gui/knowledge.inscriber.png', 'gui/arcane.assembler.png']
    references = '\n'.join(p.read_text(encoding='utf8') for parent, pattern in
                           [(RES / 'assets', '*.json'), (ROOT / 'src/main/java', '*.java')]
                           for p in parent.rglob(pattern) if 'atlases' not in p.parts)
    legacy_path = RES / f'data/{MOD}/thaumcraft/original_resources.json'
    legacy = json.loads(legacy_path.read_text(encoding='utf8'))
    for path in retired:
        if path not in references and f'{MOD}:{path.removesuffix(".png")}' not in references:
            legacy['mapping'].pop(f'assets/{MOD}/textures/{path}', None)
    legacy_path.write_text(json.dumps(legacy, indent=2, ensure_ascii=False) + '\n', encoding='utf8')
    # Minecraft only auto-stitches the singular block/ and item/ directories.
    # Both legacy plurals and modern/ therefore need explicit atlas entries.
    atlas_path = RES / 'assets/minecraft/atlases/blocks.json'
    atlas = json.loads(atlas_path.read_text(encoding='utf8'))
    retired_ids = {f'{MOD}:{path.removesuffix(".png")}' for path in retired
                   if f'assets/{MOD}/textures/{path}' not in legacy['mapping']}
    sources = [entry for entry in atlas['sources'] if entry.get('resource') not in retired_ids]
    known = {entry.get('resource') for entry in sources}
    for path in approved:
        local = path.removeprefix(f'assets/{MOD}/textures/').removesuffix('.png')
        if local.startswith('modern/gui/'):
            continue  # Directly bound GUI textures, not atlas sprites.
        sprite = f'{MOD}:{local}'
        if sprite not in known:
            sources.append({'type': 'single', 'resource': sprite})
            known.add(sprite)
    atlas['sources'] = sources
    atlas_path.write_text(json.dumps(atlas, indent=2) + '\n', encoding='utf8')
    print(f'Installed handoff: {len(approved)} approved PNGs; {len(legacy["mapping"])} legacy mappings retained')


if __name__ == '__main__':
    apply()
