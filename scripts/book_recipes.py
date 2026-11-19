"""Build handbook illustrations from the actual modern recipes, avoiding a second recipe specification."""
import json

CLASS = 'thaumicbases.modern.RecipeDisplay'
TAG_ITEMS = {
    'minecraft:stone_bricks': ['minecraft:'+kind for kind in ['stone_bricks','mossy_stone_bricks','cracked_stone_bricks','chiseled_stone_bricks']],
    'forge:rods/wooden': ['minecraft:stick'], 'forge:tools/mortar': ['thaumicbases:mortar'],
    'forge:glass': ['minecraft:glass'], 'forge:cobblestone': ['minecraft:cobblestone'],
    'forge:ingots/silver': ['thaumcraft:primal_charm'],
    'minecraft:wool': ['minecraft:' + color + '_wool' for color in ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']],
}
for element in ['air','fire','water','earth','order','entropy']:
    TAG_ITEMS['forge:ores/infused_'+element]=['thaumcraft:'+element+'_infused_stone']
for metal in ['iron', 'gold', 'thaumium', 'void', 'thauminite']:
    namespace = 'minecraft' if metal in ['iron', 'gold'] else 'thaumicbases' if metal == 'thauminite' else 'thaumcraft'
    for form in ['ingot', 'nugget']:
        TAG_ITEMS[f'forge:{form}s/{metal}'] = [f'{namespace}:{metal}_{form}']


def stack(value):
    return {'kind': 'stack', 'stack': value if isinstance(value, dict) else {'item': value}}


def sequence(values):
    return {'kind': 'sequence', 'elements': values}


def ingredient(value):
    if isinstance(value, list):
        return sequence([ingredient(item) for item in value])
    if 'item' in value:
        return stack(value['item'])
    if 'tag' in value:
        tag = value['tag']
        if tag not in TAG_ITEMS:
            raise ValueError('Missing handbook tag illustration: ' + tag)
        return {'kind': 'ore', 'names': [tag], 'elements': [stack(item) for item in TAG_ITEMS[tag]]}
    if value.get('type') == 'thaumicbases:revolver_upgrade':
        return stack({'item': 'thaumicbases:revolver', 'nbt': json.dumps({'tbUpgrade:' + value['upgrade']: value['level']})})
    raise ValueError('Unsupported handbook ingredient: ' + repr(value))


def definition(recipe):
    kind = recipe['type']
    if kind == 'forge:conditional':
        return definition(recipe['recipes'][-1]['recipe'])
    research = recipe.get('research', '')
    result = recipe.get('result')
    data = {'research': research}
    if kind == 'thaumcraft:crucible':
        data.update(kind='crucible', catalyst=ingredient(recipe['catalyst']), output=stack(result), aspects=recipe['aspects'])
        return 'CRUCIBLE_CRAFTING', data
    if kind == 'thaumcraft:infusion':
        data.update(aspects=recipe['aspects'], instability=recipe['instability'],
                    ingredients=sequence([ingredient(row['ingredient']) for row in recipe['components']]))
        if 'enchantment' in recipe:
            name = recipe['enchantment'].split(':')[1]
            data.update(kind='infusion_enchantment', enchantment={'id': -1, 'name': 'enchantment.thaumicbases.' + name, 'max_level': 5 if name in ['elder_knowledge', 'eldritch_bane'] else 3}, recipeXP=2 if name in ['elder_knowledge', 'eldritch_bane'] else 3)
            return 'INFUSION_ENCHANTMENT', data
        if 'patch' in result:
            result = {'item': 'thaumicbases:revolver', 'nbt': result['patch']}
        data.update(kind='infusion', input=ingredient(recipe['center']['ingredient']), output=stack(result))
        return 'INFUSION_CRAFTING', data
    if kind not in ['thaumcraft:arcane', 'minecraft:crafting_shaped', 'minecraft:crafting_shapeless']:
        return None
    shaped = 'pattern' in recipe
    arcane = kind == 'thaumcraft:arcane'
    if shaped:
        rows = recipe['pattern']
        inputs = [None if char == ' ' else ingredient(recipe['key'][char]) for row in rows for char in row]
        data.update(width=len(rows[0]), height=len(rows), mirrored=True)
    else:
        inputs = [ingredient(row) for row in recipe['ingredients']]
    data.update(kind=('arcane_' if arcane else 'crafting_') + ('shaped' if shaped else 'shapeless'),
                output=stack(result), ingredients=sequence(inputs), aspects=recipe.get('vis', {}))
    return 'ARCANE_CRAFTING' if arcane else 'NORMAL_CRAFTING', data


def generate(write, out):
    entries = []
    types = {}
    for file in sorted((out/'data/thaumicbases/recipes').rglob('*.json')):
        recipe = json.loads(file.read_text(encoding='utf-8'))
        # Only recipes referenced by the handbook need display records; basic generated recipes are included for shared pages.
        converted = definition(recipe)
        if converted is None:
            continue
        key = 'thaumicbases:' + file.relative_to(out/'data/thaumicbases/recipes').with_suffix('').as_posix()
        types[key], data = converted
        entries.append({'key': key, 'class': CLASS, 'data': data})
    write('data/thaumicbases/research_page_recipes/modern.json', {'format': 1, 'entries': entries})
    return types
