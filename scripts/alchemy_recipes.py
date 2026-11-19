"""Modern crucible inputs; quantities/research/aspects follow TBThaumonomicon's non-GregTech branch."""

PRIMALS = ['aer', 'ignis', 'aqua', 'terra', 'ordo', 'perditio']
ELEMENTS = ['air', 'fire', 'water', 'earth', 'order', 'entropy']


def ingredient(value):
    if isinstance(value, list):
        return [ingredient(item) for item in value]
    if value.startswith('#'):
        return {'tag': value[1:]}
    return {'item': value if ':' in value else 'minecraft:' + value}


def generate(write):
    rows = []
    def add(name, research, output, catalyst, aspects, count=1):
        rows.append((name, research, output, catalyst, aspects, count))

    sandstone = ['sandstone', 'chiseled_sandstone', 'cut_sandstone']
    for count, research in [(3, 'AdvancedEntropy'), (4, 'MasterEntropy')]:
        costs = {'perditio': 1, **({'fabrico': 1} if count == 4 else {})}
        for name, output, source in [('wool', 'string', '#minecraft:wool'), ('sandstone', 'sand', sandstone), ('blaze', 'blaze_powder', 'blaze_rod')]:
            add(f'{name}{count}Rec', research, output, source, costs, count)
    add('glassSand', 'StrongEntropy', 'sand', '#forge:glass', {'perditio': 1})
    add('gravelSand', 'StrongEntropy', 'sand', 'gravel', {'perditio': 1})
    add('barsIron', 'StrongEntropy', 'iron_nugget', 'iron_bars', {'perditio': 1}, 3)
    for name, item, costs in [('arrows', 'arrow', {'telum': 1}), ('snowball', 'snowball', {'gelum': 1}), ('redstone', 'redstone', {'machina': 1, 'potentia': 1})]:
        add(name, 'SimpleDublication', item, item, costs, 2)
    add('amber', 'Amber', 'thaumcraft:amber', 'spruce_sapling', {'vinculum': 2})
    add('cinnabar', 'Quicksilver', 'thaumcraft:quicksilver', 'thaumcraft:silverwood_log', {'ordo': 1, 'venenum': 1}, 3)
    add('salisMundis', 'SM', 'thaumcraft:salis_mundus', 'thaumcraft:salis_mundus', {**dict.fromkeys(PRIMALS, 2), 'praecantatio': 3}, 2)
    add('chiseledBricks', 'AlchemyRestoration', 'chiseled_stone_bricks', '#minecraft:stone_bricks', {'ordo': 1})
    add('gravel2Cobble', 'AlchemyRestoration', 'cobblestone', 'gravel', {'ordo': 1})
    add('icePacking', 'AlchemyRestoration', 'packed_ice', 'ice', {'terra': 1, 'gelum': 3})
    add('blazepowderRest', 'Backprocessing', 'blaze_rod', 'blaze_powder', {'ordo': 6, 'ignis': 3, 'praecantatio': 2, 'fabrico': 8, 'permutatio': 6, 'potentia': 3})
    add('boneRest', 'Backprocessing', 'bone', 'bone_meal', {'ordo': 2, 'sensus': 6, 'fabrico': 2, 'permutatio': 1, 'mortuus': 4})
    add('sugarRest', 'Backprocessing', 'sugar_cane', 'sugar', {'ordo': 1, 'victus': 2})
    add('cactiRest', 'Backprocessing', 'cactus', 'green_dye', {'ordo': 1, 'victus': 2, 'terra': 1})
    add('pyrofluidRec', 'Pyrofluid', 'thaumicbases:pyrofluid_bucket', 'lava_bucket', {'ignis': 24, 'praecantatio': 8})
    add('thauminiteRec', 'Thauminite', 'thaumicbases:thauminite_nugget', 'thaumcraft:thaumium_ingot', {'metallum': 4, 'vitreus': 8, 'praecantatio': 2}, 8)
    add('tobaccoSeedRec', 'Tobacco', 'thaumicbases:tobacco_seeds', 'wheat_seeds', {'cognitio': 4, 'aqua': 4, 'humanus': 4, 'messis': 4})
    for name, research, kind, costs in [
        ('angryTobaccoRec', 'Angry', 'fighting', {'telum': 4, 'fames': 4}),
        ('miningTobaccoRec', 'Mining', 'mining', {'perfodio': 4, 'motus': 4}),
        ('wisdomTobaccoRec', 'Wisdom', 'knowledge', {'cognitio': 4, 'aer': 4}),
        ('taintTobaccoRec', 'Tainted', 'tainted', {'vitium': 4, 'cognitio': 4})]:
        add(name, 'Tobacco.' + research, 'thaumicbases:tobacco_' + kind, 'thaumicbases:tobacco_pile', costs)
    add('cleanEFabricRec', 'Spike.Iron', 'thaumcraft:enchanted_fabric', 'thaumicbases:bloody_cloth', {'ordo': 1})
    for name, research, output, catalyst, costs in [
        ('plaxRec', 'Plax', 'plax_seeds', 'wheat_seeds', {'pannus': 4}),
        ('metalleatRec', 'Metalleat', 'metalleat_seeds', 'wheat_seeds', {'ordo': 4, 'metallum': 12, 'victus': 4}),
        ('lucriteRec', 'Lucrite', 'lucrite_seeds', 'golden_carrot', {'fames': 4, 'lucrum': 12, 'victus': 4}),
        ('aureliaRec', 'Aurelia', 'aurelia', 'pink_tulip', {'herba': 8, 'alienis': 2, 'auram': 2}),
        ('knoseRec', 'Knose', 'knose_seed', 'rose_bush', {'cognitio': 16, 'victus': 8, 'potentia': 2}),
        ('sweedRec', 'BasicPlants', 'sweed_seeds', ['grass', 'fern', 'dead_bush'], {'motus': 2, 'victus': 1}),
        ('lazulliaRec', 'Lazullia', 'lazullia_seeds', 'wheat_seeds', {'sensus': 12, 'lucrum': 3}),
        ('rainbowCactiRec', 'RainbowCacti', 'rainbow_cactus', 'cactus', {'permutatio': 24, 'sensus': 6}),
        ('redlonRec', 'Redlon', 'redlon_seeds', 'melon_seeds', {'machina': 16, 'potentia': 16}),
        ('aspectShroomRec', 'Ashroom', 'ashroom', 'thaumcraft:vishroom', {'perditio': 4, 'messis': 2}),
        ('flaxiumRec', 'Flaxium', 'flaxium', 'allium', {'perditio': 1, 'praecantatio': 2}),
        ('glieoniaRec', 'Glieonia', 'glieonia_seed', 'azure_bluet', {'lux': 8, 'praecantatio': 6, 'victus': 4}),
        ('briarRec', 'Briar', 'briar', 'rose_bush', {'sano': 4, 'victus': 4, 'herba': 4})]:
        add(name, research, 'thaumicbases:' + output, catalyst, costs)
    add('knowledgeFragmentRec', 'Knose', 'thaumcraft:knowledge_fragment', ['thaumicbases:knose_fragment_' + name for name in ELEMENTS + ['mixed', 'tainted']], {'ordo': 1, 'permutatio': 1})
    for i, element in enumerate(ELEMENTS + ['mixed']):
        costs = dict.fromkeys(PRIMALS, 1 if i < 6 else 2)
        costs['fabrico'] = 2
        if i < 6:
            costs[PRIMALS[i]] += 2
        source = 'thaumcraft:' + element + '_crystal_cluster'
        add('cBlocks_' + element, 'CrystalBlocks', 'thaumicbases:crystal_' + element, source, costs)
        add('cTaintedBlocks_' + element, 'CrystalBlocks', 'thaumicbases:crystal_tainted', source, {**costs, 'vitium': 2})
    for element, aspect in zip(ELEMENTS, PRIMALS):
        add('shards_' + element, 'OreDestruction', 'thaumcraft:' + element + '_shard', '#forge:ores/infused_' + element,
            {'perditio': 2 + (3 if aspect == 'perditio' else 0), 'praecantatio': 1, **({aspect: 3} if aspect != 'perditio' else {})}, 8)
    for order, (name, research, output, catalyst, aspects, count) in enumerate(rows):
        write(f'data/thaumicbases/recipes/alchemy/{name.lower()}.json', {
            'type': 'thaumcraft:crucible', 'research': 'TB.' + research,
            'legacy_order': 10000 + order,
            'result': {'item': output if ':' in output else 'minecraft:' + output, 'count': count},
            'catalyst': ingredient(catalyst), 'aspects': aspects,
        })
    # Stable old variable -> modern recipe references for the subsequent handbook generator.
    write('tbmodern/alchemy_recipe_index.json', {name: 'thaumicbases:alchemy/' + name.lower() for name, *_ in rows})
