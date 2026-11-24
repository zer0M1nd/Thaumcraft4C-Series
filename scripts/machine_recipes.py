"""Base machinery and decoration recipes; reference variable names remain stable handbook keys."""
from alchemy_recipes import PRIMALS, ELEMENTS, ingredient


def generate(write):
    index = {}
    def emit(name, recipe):
        path = 'machines/' + name.lower()
        write('data/thaumicbases/recipes/' + path + '.json', recipe)
        index[name] = 'thaumicbases:' + path
    def shaped(name, research, output, pattern, keys, vis, count=1):
        emit(name, {'type': 'thaumcraft:arcane', 'research': 'TB.' + research, 'mode': 'shaped', 'pattern': pattern,
                    'key': {key: ingredient(value) for key, value in keys.items()}, 'vis': vis,
                    'result': {'item': 'thaumicbases:' + output, 'count': count}})
    def infusion(name, research, output, center, instability, aspects, components):
        pedestal = lambda value: {'ingredient': ingredient(value), 'damage': -1}
        emit(name, {'type': 'thaumcraft:infusion', 'research': 'TB.' + research, 'center': pedestal(center),
                    'instability': instability, 'aspects': aspects, 'components': [pedestal(item) for item in components],
                    'result': {'item': 'thaumicbases:' + output}})
    shaped('advFurnaceRecipe', 'AdvAlc', 'advanced_alchemical_furnace', ['# #', '#@#', '$$$'],
           {'#': 'thaumcraft:thaumium_ingot', '@': 'thaumcraft:alchemy_furnace', '$': 'thaumcraft:bellows'}, {'aqua': 20, 'ignis': 20, 'aer': 10})
    infusion('cryingObsidianRec', 'CryingObs', 'crying_obelisk', 'obsidian', 2, {'iter': 16, 'victus': 16, 'spiritus': 16},
             ['diamond', 'red_bed', 'thaumicbases:old_lapis', 'thaumicbases:old_lapis'])
    infusion('overchanterRec', 'Overchanter', 'overchanter', 'enchanting_table', 4,
             {**dict.fromkeys(PRIMALS, 32), 'praecantatio': 32, 'machina': 32, 'cognitio': 16},
             ['thaumicbases:crystal_' + element for element in ELEMENTS + ['mixed', 'mixed']]
             + ['thaumicbases:salis_mundus_block'] * 2 + ['thaumcraft:thaumium_block'] * 2)
    for name, output, center in [('relocatorRec', 'relocator', 'thaumcraft:nitor'), ('irelocatorRec', 'attracting_relocator', 'thaumcraft:alumentum')]:
        shaped(name, 'Relocator', output, ['GAG', 'ENE', 'GWG'],
               {'G': 'thaumcraft:greatwood_planks', 'A': 'thaumcraft:air_shard', 'E': 'thaumcraft:earth_shard', 'W': 'thaumcraft:water_shard', 'N': center},
               {'aer': 5, 'terra': 15, 'aqua': 5})
    shaped('thaumicAnvilRec', 'ThaumicAnvil', 'thaumic_anvil', ['###', ' $ ', '$$$'],
           {'#': '#forge:storage_blocks/thaumium', '$': '#forge:ingots/thaumium'}, dict.fromkeys(PRIMALS, 25))
    infusion('voidAnvilRec', 'VoidAnvil', 'void_anvil', 'thaumicbases:thaumic_anvil', 3,
             {'fabrico': 32, 'instrumentum': 24, 'telum': 24, 'ordo': 48}, ['#forge:storage_blocks/void'] * 5)
    shaped('campfireRec', 'Campfire', 'campfire', ['SSS', 'COC', 'GGG'],
           {'S': '#forge:rods/wooden', 'C': '#forge:cobblestone', 'O': ['coal', 'charcoal'], 'G': 'gravel'}, {'ignis': 5, 'perditio': 3})
    shaped('brazierRec', 'Brazier', 'brazier', ['SCS', ' V ', 'SSS'],
           {'S': 'thaumcraft:arcane_stone_slab', 'C': ['coal', 'charcoal'], 'V': 'thaumcraft:arcane_stone'}, {'ignis': 5, 'perditio': 3})
    for i, (source, output) in enumerate([('cobblestone', 'old_cobble'), ('gravel', 'old_gravel'), ('mossy_cobblestone', 'old_mossy_cobble'),
                                         ('iron_block', 'old_iron'), ('diamond_block', 'old_diamond'), ('lapis_block', 'old_lapis'),
                                         ('gold_block', 'old_gold'), ('bricks', 'old_brick')]):
        shaped('oldRec_' + str(i), 'DecoBlocks', output, ['###', '#@#', '###'],
               {'#': source, '@': 'thaumcraft:entropy_crystal_cluster'}, {'perditio': 5}, 8)
    emit('voidBlockRec', {'type': 'minecraft:crafting_shaped', 'pattern': ['###', '###', '###'],
                         'key': {'#': ingredient('#forge:ingots/void')}, 'result': {'item': 'thaumicbases:void_block'}})
    emit('voidIngotRec', {'type': 'minecraft:crafting_shapeless', 'ingredients': [ingredient('#forge:storage_blocks/void')],
                         'result': {'item': 'thaumcraft:void_ingot', 'count': 9}})
    for name, output, keys in [
        ('eldritchArk', 'eldritch_ark', {'@': '#forge:nuggets/gold', '#': 'obsidian', '$': 'thaumcraft:order_crystal_cluster'}),
        ('ironGreatwood', 'iron_greatwood', {'@': '#forge:nuggets/iron', '#': 'thaumcraft:greatwood_planks', '$': 'thaumcraft:order_crystal_cluster'})]:
        emit(name, {'type': 'minecraft:crafting_shaped', 'pattern': ['@#@', '#$#', '@#@'],
                    'key': {key: ingredient(value) for key, value in keys.items()}, 'result': {'item': 'thaumicbases:' + output, 'count': 4}})
    write('tbmodern/machine_recipe_index.json', index)
