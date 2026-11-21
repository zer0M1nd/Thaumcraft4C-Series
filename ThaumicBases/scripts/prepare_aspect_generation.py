"""Create calculation input for the frozen base engine; never alter its bundled graph."""
import json
import zipfile
from pathlib import Path
from book_recipes import TAG_ITEMS

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'src/main/resources'
with zipfile.ZipFile(ROOT/'.deps/thaumcraft-0.4.11.jar') as archive:
    graph = json.loads(archive.read('data/thaumcraft/recipe_aspect_inputs/legacy.json'))
    catalog = json.loads(archive.read('data/thaumcraft/aspect_registry.json'))
    aliases = {}
    for name in archive.namelist():
        if '/legacy_item_mappings/' in name and name.endswith('.json'):
            for row in json.loads(archive.read(name))['items']:
                aliases[row['item']] = {'item': row['legacy_item'], 'metadata': row['metadata']}

def identity(name):
    if name.startswith('thaumicbases:'):
        return {'item': name, 'metadata': 0}
    if name not in aliases:
        raise ValueError('Unmapped ingredient identity: ' + name)
    return aliases[name]

def ingredient(row):
    if isinstance(row, list):
        choices = [item for child in row for item in ingredient(child)['alternatives']]
        return {'choose_nonempty': True, 'alternatives': choices}
    if 'item' in row:
        return {'choose_nonempty': False, 'alternatives': [identity(row['item'])]}
    if 'tag' in row:
        return {'choose_nonempty': True, 'alternatives': [identity(item) for item in TAG_ITEMS[row['tag']]]}
    raise ValueError('Unsupported calculation ingredient: ' + str(row))

explicit = json.loads((OUT/'data/thaumicbases/item_aspects/explicit.json').read_text())['entries']
targets = set()
bright_fallback = None
for row in explicit:
    for name in row['items']:
        targets.add(name)
        graph['registrations'].append({'item': name, 'metadata': 32767, 'aspects': row['aspects']})
recipes = {p.relative_to(OUT/'data/thaumicbases').as_posix(): json.loads(p.read_text()) for p in (OUT/'data/thaumicbases/recipes').rglob('*.json')}
recipes.update({key: value for key, value in json.loads((OUT/'tbmodern/configured-data.json').read_text()).items() if key.startswith('recipes/')})
for path, row in sorted(recipes.items()):
    if row['type'] == 'forge:conditional':
        row = row['recipes'][-1]['recipe']
    output = row.get('result', {})
    if not isinstance(output, dict) or 'item' not in output or 'enchantment' in row:
        continue
    kind = row['type']
    if kind in ['minecraft:smelting', 'minecraft:blasting']:
        continue  # The original aspect engine has no smelting calculation branch.
    if kind == 'thaumcraft:crucible':
        inputs = [ingredient(row['catalyst'])]
        semantic = 'CRUCIBLE'
    elif kind == 'thaumcraft:infusion':
        inputs = [ingredient(row['center']['ingredient'])] + [ingredient(value['ingredient']) for value in row['components']]
        semantic = 'INFUSION'
    elif kind in ['thaumcraft:arcane', 'minecraft:crafting_shaped', 'minecraft:crafting_shapeless']:
        inputs = ([ingredient(row['key'][char]) for line in row['pattern'] for char in line if char != ' '] if 'pattern' in row else [ingredient(value) for value in row['ingredients']])
        semantic = 'ARCANE' if kind == 'thaumcraft:arcane' else 'CRAFTING'
    else:
        raise ValueError('Unmapped calculation recipe ' + path)
    graph['recipes'].append({'kind': semantic, 'output': identity(output['item']), 'count': output.get('count', 1),
                             'ingredients': inputs, 'cost': [{'aspect': key, 'amount': value} for key, value in row.get('aspects', row.get('vis', {})).items()]})
    if path == 'recipes/equipment/brightfocirec.json':
        bright_fallback = json.loads(json.dumps(graph['recipes'][-1]))
        bright_fallback['ingredients'][0] = ingredient({'item': 'thaumcraft:primal_charm'})
        bright_fallback['cost'] = [{'aspect': key, 'amount': value} for key, value in {'auram':64,'lux':16,'potentia':32}.items()]
    if output['item'].startswith('thaumicbases:'):
        targets.add(output['item'])
for name in sorted(targets):
    definition = {'item': name, 'damageable': False, 'subtypes': False}
    if name == 'thaumicbases:mortar':
        definition['container'] = name
    if name == 'thaumicbases:pyrofluid_bucket':
        definition['container'] = 'minecraft:bucket'
    graph['items'].append(definition)
if bright_fallback is None: raise ValueError('Missing configurable brightness recipe')
payload = {'catalog': catalog, 'graph': graph, 'bright_fallback': bright_fallback, 'targets': sorted(targets), 'explicit': [item for row in explicit for item in row['items']]}
(ROOT/'.tooling/aspect-generation-input.json').write_text(json.dumps(payload,ensure_ascii=False),encoding='utf-8')
print('Prepared aspect calculation targets:',len(targets))
