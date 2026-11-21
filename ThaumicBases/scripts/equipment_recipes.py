"""Equipment, focus and enchantment recipe definitions for the modern recipe serializers."""
from alchemy_recipes import PRIMALS, ingredient


def generate(write):
    index = {}
    def emit(name, recipe):
        path = 'equipment/' + name.lower()
        write('data/thaumicbases/recipes/' + path + '.json', recipe)
        index[name] = 'thaumicbases:' + path
    def shaped(name, research, output, pattern, keys, vis, count=1):
        emit(name, {'type': 'thaumcraft:arcane', 'research': research, 'mode': 'shaped', 'pattern': pattern,
                    'key': {symbol: ingredient(value) for symbol, value in keys.items()}, 'vis': vis,
                    'result': {'item': output, 'count': count}})
    def pedestal(item):
        return {'ingredient': ingredient(item), 'damage': -1}
    def infusion(name, research, output, center, instability, aspects, components):
        emit(name, {'type': 'thaumcraft:infusion', 'research': research, 'center': pedestal(center),
                    'instability': instability, 'aspects': aspects, 'components': [pedestal(item) for item in components],
                    'result': {'item': output}})
    dust = 'thaumcraft:salis_mundus'
    for i, (tool, pattern) in enumerate([
        ('axe', ['##', '#|', ' |']), ('hoe', ['##', ' |', ' |']), ('shovel', ['#', '|', '|']),
        ('sword', ['#', '#', '|']), ('shears', [' #', '# ']), ('pickaxe', ['###', ' | ', ' | ']),
        ('helmet', ['###', '# #']), ('chestplate', ['# #', '###', '###']),
        ('leggings', ['###', '# #', '# #']), ('boots', ['# #', '# #'])]):
        keys = {'#': 'thaumicbases:thauminite_ingot'}
        if any('|' in row for row in pattern):
            keys['|'] = '#forge:rods/wooden'
        shaped(f'toolsRec_{i}', 'TB.Thauminite', 'thaumicbases:thauminite_' + tool, pattern, keys, dict.fromkeys(PRIMALS, 15 if i < 6 else 25))
    shaped('thauminiteCaps', 'CAP_thauminite', 'thaumicbases:thauminite_wand_cap', ['###', '#@#', '@@@'],
           {'#': 'thaumicbases:thauminite_nugget', '@': dust}, dict.fromkeys(PRIMALS, 25))
    shaped('thaumicWandCore', 'ROD_tbthaumium', 'thaumicbases:thaumium_wand_core', [' @#', '@#@', '#@ '],
           {'#': 'thaumcraft:thaumium_ingot', '@': dust}, dict.fromkeys(PRIMALS, 50))
    emit('salisMundusBlockRec', {'type': 'thaumcraft:arcane', 'research': 'TB.SMB', 'mode': 'shapeless',
                              'ingredients': [ingredient(dust)] * 9, 'vis': dict.fromkeys(PRIMALS, 1),
                              'result': {'item': 'thaumicbases:salis_mundus_block'}})
    infusion('voidRodRecipe', 'ROD_tbvoid', 'thaumicbases:void_wand_core', 'thaumicbases:thaumium_wand_core', 6,
             {'alienis': 64, 'vacuos': 64, 'praecantatio': 64, 'auram': 24, 'potentia': 24},
             ['thaumcraft:void_ingot'] * 4 + ['thaumicbases:salis_mundus_block'] * 2 + ['thaumicbases:crystal_tainted'] * 2)
    infusion('drainFociRec', 'TB.Foci.Drain', 'thaumicbases:focus_drain', 'thaumcraft:focus_excavation', 0,
             {'aqua': 3, 'vacuos': 6}, ['bucket', 'bucket', 'thaumcraft:void_jar', 'thaumcraft:void_jar'])
    infusion('expFociRec', 'TB.Foci.Experience', 'thaumicbases:focus_experience', 'thaumcraft:focus_excavation', 5,
             {'lucrum': 32, 'vacuos': 32, 'cognitio': 16, 'fames': 32, 'perfodio': 8},
             ['gold_ingot', 'gold_ingot', 'emerald', 'emerald', 'iron_axe', 'iron_pickaxe', 'iron_shovel', 'shears'])
    infusion('fluxFociRec', 'TB.Foci.Flux', 'thaumicbases:focus_flux', 'diamond', 3,
             {'aqua': 16, 'vacuos': 16, 'ordo': 16}, ['thaumcraft:balanced_shard'] * 2 + ['thaumcraft:flux_scrubber', 'thaumcraft:void_jar'])
    infusion('activationFociRec', 'TB.Foci.Activation', 'thaumicbases:focus_activation', 'thaumcraft:mirrored_glass', 0,
             {'iter': 8, 'sensus': 8, 'motus': 8}, ['thaumcraft:knowledge_fragment'] * 2 + [dust] * 2)
    infusion('revolverRec', 'TB.Revolver', 'thaumicbases:revolver', 'iron_block', 5,
             {'telum': 256, 'metallum': 32, 'arbor': 16, 'instrumentum': 12, 'potentia': 32, 'machina': 12, 'vacuos': 16, 'ignis': 32},
             ['fire_charge', 'gunpowder', 'thaumcraft:primal_charm', 'thaumcraft:greatwood_log', 'gunpowder', 'thaumcraft:focus_fire',
              'fire_charge', 'gunpowder', 'thaumcraft:bone_bow', 'thaumcraft:greatwood_log', 'gunpowder', 'redstone_torch'])
    for name, research, enchantment, instability, aspects, components in [
        ('elderKnowledgeEnch', 'ElderKnowledge', 'elder_knowledge', 2, {'praecantatio': 8, 'cognitio': 8, 'telum': 8}, ['paper'] * 3 + ['thaumcraft:zombie_brain', dust]),
        ('elderBaneEnch', 'Eldritchbane', 'eldritch_bane', 2, {'mortuus': 8, 'alienis': 8, 'telum': 8}, ['ender_pearl', 'ender_eye', dust, dust]),
        ('magicTouchEnch', 'MagicTouch', 'magic_touch', 2, {'praecantatio': 8, 'auram': 8, 'telum': 8}, [dust] * 3 + ['paper']),
        ('vaporisingEnch', 'Vaporising', 'vaporising', 2, {'praecantatio': 8, 'vitreus': 8, 'vinculum': 8}, [dust, dust, 'diamond']),
        ('taintedEnch', 'Tainted', 'tainted', 5, {'vitium': 16}, ['thaumcraft:tainted_goo', 'thaumcraft:tainted_tendril', dust])]:
        emit(name, {'type': 'thaumcraft:infusion', 'research': 'TB.' + research, 'enchantment': 'thaumicbases:' + enchantment,
                    'instability': instability, 'aspects': aspects, 'components': [pedestal(item) for item in components]})
    for name, output, pattern, keys in [
        ('voidShearsRec', 'void_shears', [' #', '# '], {'#': 'thaumcraft:void_ingot'}),
        ('voidFlint', 'void_flint_and_steel', ['# ', ' C'], {'#': 'thaumcraft:void_ingot', 'C': 'flint'})]:
        emit(name, {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': {key: ingredient(value) for key, value in keys.items()}, 'result': {'item': 'thaumicbases:' + output}})
    from plant_equipment_recipes import generate as plants
    plants(shaped, infusion, emit)
    from node_recipes import generate as nodes
    nodes(shaped, infusion)
    from bracelet_recipes import generate as bracelets
    bracelets(shaped, infusion)
    write('tbmodern/equipment_recipe_index.json', index)
