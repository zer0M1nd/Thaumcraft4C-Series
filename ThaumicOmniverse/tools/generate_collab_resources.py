"""Author the new collaboration data and pixel art. Existing imported aspect lists are not rewritten."""
import json
from pathlib import Path
from PIL import Image, ImageDraw
from generate_item_textures import generate as recolor_items

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
NS = 'thaumicomniverse'
DATA = ROOT / 'data' / NS
ASSETS = ROOT / 'assets' / NS

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def condition(mod):
    return [{'type': 'forge:mod_loaded', 'modid': mod}]

def recipe(name, value, mod):
    write(DATA / 'recipes/collab' / (name + '.json'), dict(value, conditions=condition(mod)))

def sprite(name, kind, color):
    image = Image.new('RGBA', (16, 16))
    d = ImageDraw.Draw(image)
    outline = '#252032'
    light = tuple(min(255, c + 62) for c in color)
    dark = tuple(max(0, c - 50) for c in color)
    if kind == 'gem':
        d.polygon([(7, 1), (12, 4), (14, 9), (9, 14), (4, 13), (1, 8), (3, 3)], fill=outline)
        d.polygon([(7, 2), (11, 4), (13, 9), (9, 13), (4, 12), (2, 8), (4, 4)], fill=dark)
        d.polygon([(7, 3), (11, 5), (10, 10), (6, 12), (4, 8)], fill=color)
        d.line([(4, 7), (7, 3), (10, 5)], fill=light)
        d.point((6, 5), fill='white')
    else:
        d.ellipse((1, 1, 14, 14), fill=outline, outline='#c6a864', width=1)
        d.ellipse((4, 4, 11, 11), fill=dark, outline=light)
        d.line([(7, 2), (7, 13)], fill=color)
        d.line([(2, 7), (13, 7)], fill=color)
        d.ellipse((6, 6, 9, 9), fill='#fff4be')
    path = ASSETS / 'textures' / (name + '.png')
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)

sprite('gui/research_category', 'sigil', (137, 93, 201))

for fluid, ids in {
    'liquid_death': ['thaumcraft:liquid_death'],
    'flux_goo': ['thaumcraft:flux_goo'],
    'purifying_water': ['thaumcraft:purifying_water', 'thaumcraft:flowing_purifying_water'],
}.items():
    write(DATA / 'tags/fluids' / (fluid + '.json'), {'replace': False, 'values': ids})

translations = {
    'tc.research_category.THAUMICOMNIVERSE': ('神秘万象', 'Thaumic Omniverse'),
    'material.thaumicomniverse.thaumium': ('神秘金属', 'Thaumium'),
    'modifier.thaumicomniverse.thaumic': ('神秘', 'Thaumic'),
    'modifier.thaumicomniverse.thaumic.flavor': ('为更多强化留出空间', 'Room for more improvements'),
    'modifier.thaumicomniverse.thaumic.description': ('神秘金属部件提供额外升级槽。两部件工具每个部件提供一槽；其他工具每两个部件提供一槽，向上取整。', 'Thaumium grants upgrade slots: one per part on two-part tools, otherwise one per two parts, rounded up.'),
    'item.apotheosis.gem.thaumicomniverse:arcane': ('奥术宝石', 'Arcane Gem'),
    'item.apotheosis.gem.thaumicomniverse:warped': ('扭曲宝石', 'Warped Gem'),
    'bonus.thaumicomniverse:warped': ('%s，%s；%s', '%s, %s; %s'),
}
recolor_items('tools')
for material, chinese, color in [('thaumium', '神秘', (111, 84, 156)), ('void', '虚空', (53, 36, 84))]:
    for tool, cname, mod in [('knife', '小刀', 'farmersdelight'), ('paxel', '镐尖斧', 'mekanismtools')]:
        name = material + '_' + tool
        translations['item.' + NS + '.' + name] = (chinese + cname, material.title() + ' ' + tool.title())
        write(ASSETS / 'models/item' / (name + '.json'), {'parent': 'minecraft:item/handheld', 'textures': {'layer0': NS + ':item/' + name}})
        if tool == 'knife':
            pattern, key = [' I', 'S '], {'I': {'item': 'thaumcraft:' + material + '_ingot'}, 'S': {'tag': 'forge:rods/wooden'}}
        else:
            pattern, key = ['APS', ' R ', ' R '], {
                'A': {'item': 'thaumcraft:' + material + '_axe'},
                'P': {'item': 'thaumcraft:' + material + '_pickaxe'},
                'S': {'item': 'thaumcraft:' + material + '_shovel'}, 'R': {'tag': 'forge:rods/wooden'}}
        recipe(name, {'type': 'minecraft:crafting_shaped', 'pattern': pattern, 'key': key, 'result': {'item': NS + ':' + name}}, mod)
for tag in ['forge:tools/knives', 'farmersdelight:tools/knives']:
    namespace, path = tag.split(':')
    write(ROOT / 'data' / namespace / 'tags/items' / (path + '.json'), {'replace': False, 'values': [
        {'id': NS + ':' + material + '_knife', 'required': False} for material in ['thaumium', 'void']]})
write(ROOT / 'data/mekanismtools/tags/items/paxels.json', {'replace': False, 'values': [
    {'id': NS + ':' + material + '_paxel', 'required': False} for material in ['thaumium', 'void']]})

# Old TConstruct stats: harvest 3, durability 400, speed 7, damage 2, handle 1.3.
write(DATA / 'tinkering/materials/definition/thaumium.json', {'craftable': True, 'hidden': False, 'sortOrder': 20, 'tier': 3})
write(DATA / 'tinkering/materials/stats/thaumium.json', {'stats': {
    'tconstruct:head': {'durability': 400, 'melee_attack': 2.0, 'mining_speed': 7.0, 'mining_tier': 'minecraft:diamond'},
    'tconstruct:handle': {'durability': .3, 'melee_damage': 0.0, 'melee_speed': 0.0, 'mining_speed': 0.0},
    'tconstruct:binding': {},
}})
write(DATA / 'tinkering/materials/traits/thaumium.json', {'default': [{'name': NS + ':thaumic', 'level': 1}]})
recipe('thaumium_material', {'type': 'tconstruct:material', 'ingredient': {'item': 'thaumcraft:thaumium_ingot'},
    'material': NS + ':thaumium', 'needed': 1, 'value': 1}, 'tconstruct')
write(ASSETS / 'tinkering/materials/thaumium.json', {'color': 'FF6F549C', 'fallbacks': ['metal'], 'generator': {
    'supported_stats': ['tconstruct:head', 'tconstruct:handle', 'tconstruct:binding', 'tconstruct:repair_kit'],
    'transformer': {'type': 'tconstruct:recolor_sprite', 'color_mapping': {'type': 'tconstruct:grey_to_color',
        'palette': [{'grey': g, 'color': c} for g,c in [(0,'FF171022'),(63,'FF332044'),(102,'FF4B3265'),(140,'FF6F549C'),(178,'FF9877BE'),(216,'FFC2A5E0'),(255,'FFFFF0FF')]]}}}})

rarities = ['common','uncommon','rare','epic','mythic','ancient']
def values(maximum):
    return dict(zip(rarities, [round(maximum * f, 5) for f in [.15,.3,.45,.65,.8,1]]))
def attr(name, maximum, operation='ADDITION'):
    return {'attribute': name, 'operation': operation, 'values': values(maximum)}
def bonus(key, types, modifiers):
    return {'type': 'apotheosis:multi_attribute', 'desc': 'bonus.thaumicomniverse:warped',
        'gem_class': {'key': key, 'types': types}, 'modifiers': modifiers + [
            {'attribute': 'thaumcraft:warp', 'operation': 'ADDITION', 'values': dict(zip(rarities,[1,2,3,4,5,7]))}]}
arcane = {'type': 'apotheosis:attribute', 'gem_class': {'key': 'armor', 'types': ['helmet','chestplate','leggings','boots']},
    **attr('thaumcraft:vis_cost', -.1)}
warped = [
    bonus('melee_weapon', ['sword','heavy_weapon'], [attr('attributeslib:crit_chance', .35),attr('attributeslib:crit_damage', .45)]),
    bonus('ranged_weapon', ['bow','crossbow','trident'], [attr('attributeslib:arrow_damage', .35, 'MULTIPLY_BASE'),attr('attributeslib:arrow_velocity', .35, 'MULTIPLY_BASE')]),
    bonus('upper_armor', ['helmet','chestplate'], [attr('minecraft:generic.max_health', 6),attr('minecraft:generic.armor', 3)]),
    bonus('lower_armor', ['leggings','boots'], [attr('minecraft:generic.movement_speed', .35, 'MULTIPLY_BASE'),attr('attributeslib:dodge_chance', .07)]),
    bonus('shield', ['shield'], [attr('minecraft:generic.armor', .3, 'MULTIPLY_BASE'),attr('minecraft:generic.armor_toughness', .2, 'MULTIPLY_BASE')]),
    bonus('tools', ['pickaxe','shovel'], [attr('attributeslib:mining_speed', .3, 'MULTIPLY_BASE'),attr('minecraft:generic.luck', 2)]),
]
for name, bonuses, color in [('arcane',[arcane],(76,171,198)),('warped',warped,(150,65,166))]:
    write(DATA / 'gems' / (name + '.json'), {'variant': NS + '_' + name, 'weight': 10, 'quality': 0, 'dimensions': [], 'unique': True, 'bonuses': bonuses})
    sprite('item/gems/' + name, 'gem', color)
    write(ROOT / 'assets/apotheosis/models/item/gems' / (name + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': NS + ':item/gems/' + name}})

recipe('warp_ward', {'type': 'botania:brew', 'brew': NS + ':warp_ward', 'ingredients': [
    {'item': name} for name in ['minecraft:nether_wart','thaumcraft:salis_mundus','thaumcraft:bath_salts','thaumcraft:amber']]}, 'botania')
translations['thaumicomniverse.brew.warp_ward'] = ('理智', 'Sanity')

# One material manifest drives MA crops and Productive Bees. Infused stone means the ore block,
# not a silently substituted shard. Pearl essence yields fragments, requiring 81 essence per pearl.
renewables = json.loads((ROOT / 'thaumicomniverse/renewables.json').read_text(encoding='utf-8'))
for row in renewables:
    name = row['name']
    existing = [{'type': 'forge:item_exists', 'item': row['item']}]
    essence = 'mysticalagriculture:omniverse_' + name + '_essence'
    output = NS + ':primordial_fragment' if row.get('fragment') else row['item']
    crop_recipe = {'type': 'minecraft:crafting_shaped', 'pattern': ['EEE','EEE' if row.get('fragment') else 'E E','EEE'],
        'key': {'E': {'item': essence}}, 'result': {'item': output, 'count': row['yield']},
        'conditions': condition('mysticalagriculture') + condition(row['mod']) + existing}
    write(DATA / 'recipes/collab/crops' / (name + '.json'), crop_recipe)
    translations['crop.' + NS + '.omniverse_' + name] = (row['zh'], row['en'])
    for suffix, zh, en in [('seeds','种子','Seeds'),('essence','精华','Essence'),('crop','作物','Crop')]:
        translations['item.mysticalagriculture.omniverse_' + name + '_' + suffix] = (row['zh'] + zh, row['en'] + ' ' + en)
    bee = NS + ':' + name
    color = f"#{row['color']:06x}"
    write(DATA / 'productivebees/thaumaturgy' / (name + '.json'), {
        'primaryColor': color, 'secondaryColor': '#372646', 'tertiaryColor': '#d8c8ef', 'particleColor': color,
        'flowerItem': row['item'], 'size': .6, 'renderer': 'default_crystal' if 'infused' in name else 'default',
        'selfbreed': not row.get('fragment', False), 'conditions': condition(row['mod']) + existing})
    bee_conditions = condition('productivebees') + condition(row['mod']) + existing
    source = 'productivebees:diamond' if row.get('bee_tier', row['tier']) == 5 else 'productivebees:iron'
    write(DATA / 'recipes/collab/bees' / (name + '_conversion.json'), {'type': 'productivebees:bee_conversion',
        'source': source, 'result': bee, 'item': {'item': row['item']}, 'conditions': bee_conditions})
    comb = {'type': 'forge:nbt', 'item': 'productivebees:configurable_honeycomb', 'nbt': {'EntityTag': {'type': bee}}}
    write(DATA / 'recipes/collab/bees' / (name + '_produce.json'), {'type': 'productivebees:advanced_beehive',
        'ingredient': bee, 'results': [{'item': comb}], 'conditions': bee_conditions})
    # ATM9 derives GTCEu IDs by stripping exactly 'productivebees:centrifuge'.
    # Keep our own subdirectory to avoid collisions with native bee recipes.
    write(ROOT / 'data/productivebees/recipes/centrifuge' / NS / (name + '.json'), {'type': 'productivebees:centrifuge',
        'ingredient': comb, 'outputs': [{'item': {'item': output}, 'chance': 5 if row.get('fragment') else 15 if row.get('bee_tier', row['tier']) == 5 else 35},
            {'item': {'tag': 'forge:wax'}}, {'fluid': {'fluid': 'productivebees:honey'}, 'amount': 50}], 'conditions': bee_conditions})
    # Productive Bees drops the namespace in ProductiveBee.getBeeName().
    translations['entity.productivebees.' + name + '_bee'] = (row['zh'] + '蜂', row['en'] + ' Bee')
    translations['entity.productivebees.' + NS + '.' + name] = (row['zh'] + '蜂', row['en'] + ' Bee')
    translations['entity.' + NS + '.' + name] = (row['zh'] + '蜂', row['en'] + ' Bee')
write(DATA / 'recipes/collab/primordial_pearl_from_fragments.json', {'type': 'minecraft:crafting_shaped',
    'pattern': ['FFF','FFF','FFF'], 'key': {'F': {'item': NS + ':primordial_fragment'}},
    'result': {'item': 'thaumcraft:primordial_pearl'}, 'conditions': [{'type': 'forge:or', 'values': [condition('mysticalagriculture')[0],condition('productivebees')[0]]}]})
sprite('item/primordial_fragment','gem',(214,147,217))
write(ASSETS / 'models/item/primordial_fragment.json', {'parent':'minecraft:item/generated','textures':{'layer0':NS + ':item/primordial_fragment'}})
translations['item.' + NS + '.primordial_fragment'] = ('元始珍珠碎屑','Primordial Pearl Fragment')

translations['item.' + NS + '.blood_wand_cap'] = ('血魔杖端','Blood Wand Cap')
translations['item.' + NS + '.veined_scribing_tools'] = ('血管书写工具','Veined Scribing Tools')
translations['tc.wandcap.' + NS + ':blood'] = ('血魔','Blood')
translations['config.jade.plugin_' + NS + '.revealed_aspects'] = ('揭示要素','Revealed Aspects')

for index, lang in enumerate(['zh_cn','en_us']):
    target = ASSETS / 'lang' / (lang + '.json')
    existing = json.loads(target.read_text(encoding='utf-8')) if target.exists() else {}
    existing.update({key:value[index] for key,value in translations.items()})
    write(target, existing)
print('Wrote collaboration recipes, materials, gems, tags, language entries and authored sprites')
