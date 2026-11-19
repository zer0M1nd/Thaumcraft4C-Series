from alchemy_recipes import PRIMALS, ingredient


def generate(shaped, infusion, emit):
    dust = 'thaumcraft:salis_mundus'
    tobacco = 'thaumicbases:tobacco_pile'
    def shapeless(name, research, output, inputs, vis):
        emit(name, {'type': 'thaumcraft:arcane', 'research': 'TB.' + research, 'mode': 'shapeless',
                    'ingredients': [ingredient(item) for item in inputs], 'vis': vis, 'result': {'item': 'thaumicbases:' + output}})
    for name, seed in [('saturatingTobaccoRec', 'pumpkin_seeds'), ('saturatingTobaccoRecM', 'melon_seeds')]:
        shapeless(name, 'Tobacco.Saturating', 'tobacco_hunger', [tobacco, seed], {'terra': 10, 'ignis': 10, 'aqua': 10})
    shapeless('pestleRecipe', 'Tobacco', 'mortar', ['bowl', 'stick', 'oak_slab', 'oak_slab', 'oak_slab'], dict.fromkeys(PRIMALS, 4))
    shapeless('genTobaccoRec', 'Tobacco', 'tobacco_pile', ['#forge:tools/mortar', 'thaumicbases:tobacco_leaves'], {'ordo': 1, 'perditio': 1})
    shaped('pipeRecipe', 'TB.Tobacco', 'thaumicbases:greatwood_pipe', ['#@ ', ' ##'],
           {'#': 'thaumcraft:greatwood_planks', '@': dust}, dict.fromkeys(PRIMALS, 4))
    infusion('silverwoodPipeRec', 'TB.SilverwoodPipe', 'thaumicbases:silverwood_pipe', 'thaumicbases:greatwood_pipe', 4,
             {'auram': 8, 'ordo': 32, 'sano': 16}, [dust] * 3 + ['thaumcraft:silverwood_planks'] * 4 + ['thaumcraft:shimmerleaf'])
    infusion('eldritchTobacoRec', 'TB.Tobacco.Eldritch', 'thaumicbases:tobacco_eldritch', tobacco, 1,
             {'alienis': 4, 'mortuus': 1}, ['ender_eye', 'arrow'])
    infusion('wispyTobaccoRec', 'TB.Tobacco.Wispy', 'thaumicbases:tobacco_wispy', tobacco, 1,
             {'auram': 4, 'praecantatio': 6}, [dust, 'thaumcraft:essence_crystal'])
    shapeless('sanityTobaccoRec', 'Tobacco.Sanity', 'tobacco_sanity', [tobacco], {'ordo': 10, 'aer': 10, 'perditio': 10})
    shapeless('rosehipRec', 'Briar', 'rosehip_syrup', ['thaumcraft:empty_phial', 'thaumicbases:briar_seedbag', 'sugar'], {'ignis': 3, 'aqua': 5})
    infusion('voidSeedRec', 'TB.VoidSeed', 'thaumicbases:void_seed', 'thaumcraft:void_seed', 3,
             {'alienis': 16, 'tenebrae': 16, 'messis': 16}, ['wheat_seeds', 'pumpkin_seeds', 'melon_seeds', 'poisonous_potato', 'carrot', 'nether_wart'])
    infusion('goldenOakRec', 'TB.GoldenOak', 'thaumicbases:golden_oak_sapling', 'oak_sapling', 2,
             {'herba': 16, 'lucrum': 64}, ['apple', 'apple', 'golden_apple', 'golden_apple'])
    infusion('peacefulSaplingRec', 'TB.PeacefulSapling', 'thaumicbases:peaceful_sapling', 'birch_sapling', 2,
             {'arbor': 16, 'victus': 12, 'sano': 6}, ['slime_ball', dust, 'slime_ball', 'thaumcraft:shimmerleaf', dust, 'thaumcraft:fertility_lamp', dust, 'thaumcraft:shimmerleaf'])
    infusion('netherSaplingRec', 'TB.NetherSapling', 'thaumicbases:nether_sapling', 'thaumcraft:greatwood_sapling', 5,
             {'arbor': 32, 'ignis': 32, 'fames': 16}, ['thaumcraft:cinderpearl', 'thaumcraft:fire_shard', 'thaumcraft:cinderpearl', 'thaumcraft:ethereal_bloom',
             'thaumcraft:cinderpearl', 'thaumcraft:cinderpearl', 'thaumcraft:fire_shard', 'lava_bucket', 'thaumcraft:fire_shard', 'thaumcraft:ethereal_bloom'])
    infusion('enderSaplingRec', 'TB.EnderSapling', 'thaumicbases:ender_sapling', 'thaumcraft:silverwood_sapling', 7,
             {'arbor': 64, 'alienis': 24, 'vacuos': 16, 'tenebrae': 8}, ['thaumcraft:balanced_shard', 'ender_pearl', 'thaumcraft:balanced_shard', 'thaumcraft:nitor',
             'ender_pearl', 'ender_pearl', 'thaumcraft:ethereal_bloom', 'thaumcraft:balanced_shard', 'ender_pearl', 'thaumcraft:balanced_shard', 'thaumcraft:nitor'])
    for name, output, pattern, vis in [
        ('bloodyChestRec', 'bloody_chestplate', ['# #', '###', '###'], {'aer': 5}),
        ('bloodyLegsRec', 'bloody_leggings', ['###', '# #', '# #'], {'aqua': 5}),
        ('bloodyBootsRec', 'bloody_boots', ['# #', '# #'], {'terra': 3})]:
        shaped(name, 'TB.BloodyRobes', 'thaumicbases:' + output, pattern, {'#': 'thaumicbases:bloody_cloth'}, vis)
    for name, research, kind, metal, center, vis in [
        ('ironSpikeRec', 'Iron', 'iron', 'iron', 'heavy_weighted_pressure_plate', {'ignis': 1, 'perditio': 1}),
        ('thaumiumSpikeRec', 'Thaumic', 'thaumic', 'thaumium', 'thaumicbases:spike_iron', dict.fromkeys(PRIMALS, 4)),
        ('voidSpikeRec', 'Void', 'void', 'void', 'thaumicbases:spike_thaumic', dict.fromkeys(PRIMALS, 4))]:
        shaped(name, 'TB.Spike.' + research, 'thaumicbases:spike_' + kind, [' * ', '*#*', '#_#'],
               {'*': '#forge:nuggets/' + metal, '#': '#forge:ingots/' + metal, '_': center}, vis)
