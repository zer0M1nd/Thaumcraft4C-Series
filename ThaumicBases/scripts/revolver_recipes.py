"""Level-specific modern infusion recipes, preserving the legacy upgrade costs."""
import copy
import json


def generate(write, definitions):
    dust = 'thaumcraft:salis_mundus'
    eye = 'thaumcraft:eldritch_eye'
    pearl = 'thaumcraft:primordial_pearl'
    rows = [
        ('accuracy', 'Accuracy', 1, {'ordo': 8, 'sensus': 8}, ['glass_pane', 'carrot', 'glass_pane', 'glass_pane', dust, 'glass_pane']),
        ('bane_of_arthropods', 'Atropods', 1, {'mortuus': 8, 'bestia': 8, 'pannus': 8}, ['spider_eye', 'fermented_spider_eye', 'spider_eye', dust]),
        ('bane_of_eldritch', 'EldritchBane', 2, {'mortuus': 8, 'alienis': 8}, ['ender_eye', dust, 'ender_pearl']),
        ('dueling', 'Dueling', 2, {'mortuus': 8, 'humanus': 8}, [dust, 'red_bed', dust, 'ghast_tear', dust]),
        ('efficiency', 'Efficiency', 0, {'ordo': 8, 'permutatio': 8}, [dust, 'blaze_powder', dust, 'enchanted_book', dust]),
        ('eldritch', 'Eldritch', 5, {'alienis': 8, 'vacuos': 8}, [eye, dust, eye, dust]),
        ('heavy', 'Heavy', 0, {'telum': 8, 'metallum': 8}, [dust, 'iron_sword', dust]),
        ('knowledge', 'Knowledge', 0, {'cognitio': 8, 'auram': 8}, ['thaumcraft:scribing_tools', dust, 'book', dust, 'paper']),
        ('piercing', 'Piercing', 12, {'telum': 32, 'aer': 64}, ['thaumcraft:primal_arrow_air', 'thaumcraft:primal_arrow_fire', 'thaumcraft:primal_arrow_water', 'diamond_sword', 'thaumcraft:primal_arrow_earth', 'thaumcraft:primal_arrow_order', 'thaumcraft:primal_arrow_entropy']),
        ('power', 'Power', 2, {'telum': 16}, ['gunpowder', dust, 'fire_charge', dust, 'firework_rocket']),
        ('primal', 'Primal', 12, dict.fromkeys(['telum', 'aer', 'ignis', 'aqua', 'terra', 'ordo', 'perditio', 'praecantatio'], 64), [dust, pearl, 'diamond', pearl, dust]),
        ('silver', 'Silver', 0, {'mortuus': 8, 'bestia': 8, 'exanimis': 8}, [dust, 'thaumcraft:quicksilver', 'thaumcraft:primal_charm', 'thaumcraft:quicksilver', dust]),
        ('speed', 'Speed', 2, {'motus': 8, 'aer': 8}, [dust, 'feather', dust]),
        ('tainted', 'Tainted', 4, {'vitium': 8, 'telum': 8}, ['thaumcraft:tainted_goo', dust, 'thaumcraft:tainted_tendril', dust]),
        ('void', 'Void', 4, {'vacuos': 8, 'instrumentum': 8}, ['thaumcraft:void_ingot'] * 5),
    ]
    by_key = {row['key']: row for row in definitions}
    for name, research, instability, aspects, components in rows:
        key = 'thaumicbases:' + name
        definition = by_key[key]
        for level in range(definition['maximum']):
            recipe = {
                'type': 'thaumcraft:infusion', 'research': 'TB.Revolver.' + research,
                'instability': instability + level * definition['instability'],
                'center': {'ingredient': {'type': 'thaumicbases:revolver_upgrade', 'upgrade': key, 'level': level}, 'damage': -1,
                           'display': [{'item': 'thaumicbases:revolver', 'nbt': json.dumps({'tbUpgrade:' + key: level})}]},
                'components': [{'ingredient': {'item': item if ':' in item else 'minecraft:' + item}, 'damage': -1} for item in components],
                'aspects': {aspect: cost * 2 ** level for aspect, cost in aspects.items()},
                'result': {'patch': json.dumps({'tbUpgrade:' + key: level + 1})},
            }
            if name == 'silver':
                # Use silver when present; otherwise retain the original primal-charm fallback.
                silver = copy.deepcopy(recipe)
                silver['components'][2]['ingredient'] = {'tag': 'forge:ingots/silver'}
                empty = {'type': 'forge:tag_empty', 'tag': 'forge:ingots/silver'}
                recipe = {'type': 'forge:conditional', 'recipes': [
                    {'conditions': [{'type': 'forge:not', 'value': empty}], 'recipe': silver},
                    {'conditions': [empty], 'recipe': recipe},
                ]}
            write(f'data/thaumicbases/recipes/revolver/{name}_{level + 1}.json', recipe)
