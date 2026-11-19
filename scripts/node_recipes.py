from alchemy_recipes import PRIMALS


def generate(shaped, infusion):
    dust = 'thaumcraft:salis_mundus'
    dust_block = 'thaumicbases:salis_mundus_block'
    charm = 'thaumcraft:primal_charm'
    mixed = 'thaumicbases:crystal_mixed'
    tainted = 'thaumicbases:crystal_tainted'
    infusion('nodeManipulatorRec', 'TB.NodeMan', 'thaumicbases:node_manipulator', 'thaumcraft:node_converter', 6,
             {'machina': 32, 'auram': 32, 'praecantatio': 16, 'potentia': 64, 'vacuos': 16},
             ['thaumcraft:advanced_node_stabilizer', 'thaumcraft:item_mirror', 'thaumcraft:essentia_mirror', 'thaumcraft:flux_scrubber', dust_block] * 2)
    # The cheaper config alternative is applied by the addon recipe configuration layer, not the base.
    infusion('brightFociRec', 'TB.NodeFoci.Bright', 'thaumicbases:node_focus_brightness', 'thaumcraft:primordial_pearl', 3,
             {'auram': 256, 'lux': 64, 'potentia': 256}, ['thaumcraft:arcane_lamp', dust, 'thaumcraft:cinderpearl', dust] * 2)
    for name, research, mode, instability, aspects, components in [
        ('destructionFociRec', 'Destr', 'destruction', 5, {'auram': 64, 'vacuos': 16, 'mortuus': 32},
         ['thaumcraft:amber', tainted, 'thaumcraft:essentia_crystallizer', 'thaumcraft:focus_excavation'] * 2),
        ('efficiencyFociRec', 'Efficiency', 'efficiency', 1, {'auram': 64, 'vacuos': 16, 'mortuus': 32}, ['thaumcraft:balanced_shard', dust_block] * 4),
        ('hungerFociRec', 'Hunger', 'hunger', 7, {'auram': 64, 'vacuos': 64, 'fames': 256, 'lucrum': 64, 'potentia': 128}, [dust, 'thaumcraft:thaumometer', dust, 'thaumcraft:node_converter'] * 2),
        ('unstableFociRec', 'Unstable', 'instability', 3, {'auram': 64, 'mortuus': 32, 'perditio': 64}, ['ender_pearl', 'thaumicbases:crystal_entropy'] * 4),
        ('pureFociRec', 'Purity', 'purity', 0, {'auram': 64, 'victus': 64, 'ordo': 64},
         ['thaumcraft:silverwood_sapling', 'thaumcraft:ethereal_bloom', 'thaumcraft:silverwood_log', 'thaumcraft:shimmerleaf', 'thaumcraft:silverwood_leaves', 'thaumcraft:ethereal_bloom', 'thaumcraft:silverwood_log', 'thaumcraft:shimmerleaf']),
        ('sinisterFociRec', 'Sinister', 'sinister', 4, {'auram': 64, 'tenebrae': 32, 'exanimis': 64}, ['thaumcraft:wisp_essence', tainted, 'thaumcraft:focus_pech', mixed] * 2),
        ('speedFociRec', 'Speed', 'speed', 1, {'auram': 64, 'motus': 32, 'praecantatio': 16}, [mixed, dust_block] * 4),
        ('stabilityFociRec', 'Stability', 'stability', 1, {'auram': 64, 'ordo': 32, 'permutatio': 16}, [dust_block, 'thaumcraft:mirrored_glass', dust_block, mixed] * 2),
        ('taintFociRec', 'Taint', 'taint', 5, {'auram': 64, 'vitium': 64, 'perditio': 16}, ['thaumcraft:tainted_goo', tainted, 'thaumcraft:tainted_tendril', 'thaumicbases:knose_fragment_tainted'] * 2)]:
        infusion(name, 'TB.NodeFoci.' + research, 'thaumicbases:node_focus_' + mode, charm, instability, aspects, components)
    shaped('nodeLinkRec', 'TB.NodeLinker', 'thaumicbases:node_linker', ['TPT', 'ANA', 'BOB'],
           {'T': 'thaumcraft:vis_relay', 'P': charm, 'A': 'thaumcraft:arcane_stone', 'B': 'thaumcraft:arcane_stone_bricks', 'O': 'obsidian', 'N': 'thaumcraft:alumentum'}, dict.fromkeys(PRIMALS, 5))
    infusion('entityDeconstructorRec', 'TB.EntityDec', 'thaumicbases:entity_deconstructor', 'thaumcraft:thaumometer', 4,
             {'mortuus': 16, 'spiritus': 4, 'cognitio': 16}, ['light_weighted_pressure_plate'] * 2 + ['heavy_weighted_pressure_plate'] * 2 + ['thaumcraft:knowledge_fragment'] * 2 + [charm] * 2)
