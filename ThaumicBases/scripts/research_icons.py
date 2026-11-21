import re
from research_source import arguments

ELEMENTS = ['air', 'fire', 'water', 'earth', 'order', 'entropy', 'mixed', 'tainted']
ALIASES = {
    'TBItems.pyroBucket': 'pyrofluid_bucket', 'TBItems.plaxSeed': 'plax_seeds',
    'TBItems.metalleatSeeds': 'metalleat_seeds', 'TBItems.lucriteSeeds': 'lucrite_seeds',
    'TBItems.glieoniaSeed': 'glieonia_seed', 'TBItems.lazulliaSeeds': 'lazullia_seeds',
    'TBItems.redlonSeeds': 'redlon_seeds', 'TBItems.voidSeed': 'void_seed',
    'TBItems.bloodyChest': 'bloody_chestplate', 'TBItems.greatwoodPipe': 'greatwood_pipe',
    'TBItems.silverwoodPipe': 'silverwood_pipe', 'TBBlocks.advAlchFurnace': 'advanced_alchemical_furnace',
    'TBBlocks.cryingObsidian': 'crying_obelisk', 'TBBlocks.dustBlock': 'salis_mundus_block',
    'TBBlocks.braizer': 'brazier',
}
VARIANTS = {
    'TBItems.castingBracelet': ['bracelet_' + k for k in 'iron gold greatwood thaumium silverwood reed bone obsidian blaze ice quartz void primal'.split()],
    'TBItems.resource': ['thauminite_nugget', 'thauminite_ingot', 'thauminite_wand_cap', 'thaumium_wand_core', 'void_wand_core', 'aurelia_petal', 'briar_seedbag', 'tobacco_leaves', 'bloody_cloth'],
    'TBItems.tobacco': ['tobacco_' + kind for kind in ['pile', 'eldritch', 'fighting', 'hunger', 'knowledge', 'mining', 'sanity', 'tainted', 'wispy']],
    'TBItems.knoseFragment': ['knose_fragment_' + kind for kind in ELEMENTS],
    'TBItems.nodeFoci': ['node_focus_' + kind for kind in ['brightness', 'destruction', 'efficiency', 'hunger', 'instability', 'purity', 'sinister', 'speed', 'stability', 'taint']],
    'TBBlocks.crystalBlock': ['crystal_' + kind for kind in ELEMENTS],
    'TBBlocks.sapling': [kind + '_sapling' for kind in ['golden_oak', 'peaceful', 'nether', 'ender']],
    'TBBlocks.spike': ['spike_iron', 'spike_iron', 'spike_thaumic', 'spike_thaumic', 'spike_void', 'spike_void'],
}


def item(expression):
    values, _ = arguments(expression, expression.index('('))
    owner = values[0]
    meta = 0 if len(values) < 3 or 'WILDCARD' in values[2] else int(values[2])
    if owner in VARIANTS:
        path = VARIANTS[owner][meta]
    elif owner in ALIASES:
        path = ALIASES[owner]
    elif owner.startswith('TBItems.foci'):
        path = 'focus_' + owner.removeprefix('TBItems.foci').lower()
    elif owner.startswith(('TBItems.', 'TBBlocks.')):
        path = re.sub(r'(?<!^)(?=[A-Z])', '_', owner.split('.')[1]).lower()
    elif owner == 'ConfigItems.itemResource':
        return {'item': 'thaumcraft:' + {3: 'quicksilver', 6: 'amber', 14: 'salis_mundus'}[meta]}
    elif owner == 'ConfigItems.itemShard':
        return {'item': 'thaumcraft:' + ELEMENTS[meta] + '_shard'}
    elif owner == 'ConfigBlocks.blockCrystal':
        return {'item': 'thaumcraft:' + ELEMENTS[meta] + '_crystal_cluster'}
    elif owner.startswith(('Items.', 'Blocks.')):
        path = owner.split('.')[1]
        return {'item': 'minecraft:' + {'reeds': 'sugar_cane'}.get(path, path)}
    else:
        raise ValueError('Unmapped research icon: ' + expression)
    return {'item': 'thaumicbases:' + path}
