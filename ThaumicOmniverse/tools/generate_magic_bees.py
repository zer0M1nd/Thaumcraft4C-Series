"""Export the pre-Futures Magic Bees fixtures to an explicit comparison directory."""
import argparse
import io
import json
from pathlib import Path
from zipfile import ZipFile
from PIL import Image

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('output', type=Path, help='directory for the historical fixture')
BASE = parser.parse_args().output.resolve()
ROOT = BASE / 'src/main/resources'
DATA = ROOT / 'data/thaumicomniverse'
ASSETS = ROOT / 'assets/thaumicomniverse'
LEGACY = Path(__file__).resolve().parents[4] / 'References/Collaboration/jars'
NS = 'thaumicomniverse:'
CONDS = [{'type': 'forge:mod_loaded', 'modid': mod} for mod in ('forestry', 'mobees')]

def write(path, data):
    if 'loot_tables' in path.parts: data['omniverse_requires'] = ['forestry', 'mobees']
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

write(DATA / 'tags/worldgen/biome/magical.json', {'replace': False, 'values': [
    {'id': '#forge:is_magical', 'required': False},
    {'id': '#forge:is_magic', 'required': False},
    {'id': 'thaumcraft:magical_forest', 'required': False},
    {'id': 'thaumcraft:eerie', 'required': False},
    {'id': 'thaumcraft:tainted_land', 'required': False},
    {'id': 'thaumcraft:outer_lands', 'required': False}]})

forestry = ZipFile(LEGACY / 'forestry_1.7.10-4.2.16.64.jar')
magic = ZipFile(LEGACY / 'magicbees-1.7.10-2.4.4.jar')
names = {}
aspects = []

def sprite(archive, path, color):
    image = Image.open(io.BytesIO(archive.read(path))).convert('RGBA')
    rgb = (color >> 16 & 255, color >> 8 & 255, color & 255)
    image.putdata([tuple(pixel[i] * rgb[i] // 255 for i in range(3)) + (pixel[3],)
                   for pixel in image.getdata()])
    return image

def product(name, zh, en, layers, essence):
    names['item.thaumicomniverse.' + name] = (zh, en)
    canvas = None
    for archive, path, color in layers:
        layer = sprite(archive, path, color)
        if canvas is None:
            canvas = layer
        else:
            canvas.alpha_composite(layer)
    target = ASSETS / 'textures/item' / (name + '.png')
    target.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(target)
    write(ASSETS / 'models/item' / (name + '.json'), {
        'parent': 'minecraft:item/generated', 'textures': {'layer0': NS + 'item/' + name}})
    aspects.append({'items': [NS + name], 'aspects': [
        {'aspect': key, 'amount': amount} for key, amount in essence.items()]})

def forestry_layers(stem, colors):
    return [(forestry, f'assets/forestry/textures/items/{stem}.{i}.png', color)
            for i, color in enumerate(colors)]

def centrifuge(name, time, outputs):
    write(DATA / 'recipes/collab/bees' / (name + '.json'), {
        'type': 'forestry:centrifuge', 'input': {'item': NS + name}, 'time': time,
        'products': [{'item': item, 'chance': chance} for item, chance in outputs],
        'conditions': CONDS})

rows = [
    ('air', '风', 'aer', (16768788, 7368729), 10591760, 'feather'),
    ('fire', '火', 'ignis', (14817282, 4132370), 9769775, 'blaze_powder'),
    ('water', '水', 'aqua', (46847, 997181), 1070241, 'ink_sac'),
    ('earth', '地', 'terra', (2675496, 0x333004), 40960, 'clay_ball'),
    ('order', '秩序', 'ordo', (0xDDDDFF, 10329525), 0xDDDDFF, 'redstone'),
    ('chaos', '混沌', 'perditio', (0x555577, 2960726), 0x555577, 'gunpowder')]
for element, zh, aspect, colors, propolis_color, byproduct in rows:
    product('bee_comb_' + element, zh + '要素蜂巢', element.title() + ' Infused Comb',
            forestry_layers('beeCombs', colors), {'bestia': 1, aspect: 2})
    product('bee_propolis_' + element, zh + '要素蜂胶', element.title() + ' Propolis',
            forestry_layers('propolis', [propolis_color]), {aspect: 2})
    product('bee_dust_' + element, zh + '晶尘', element.title() + ' Crystal Dust',
            [(magic, f'assets/magicbees/textures/items/TC{element}Dust.png', 0xFFFFFF)], {aspect: 1})
    centrifuge('bee_comb_' + element, 20, [(NS + 'bee_wax_magic', 1.0),
                ('minecraft:' + byproduct, .6), (NS + 'bee_propolis_' + element, .8)])
    centrifuge('bee_propolis_' + element, 8, [('forestry:propolis_normal', 1.0),
                (NS + 'bee_dust_' + element, .9)])
    # Four legacy crystal dust units craft one corresponding infused shard.
    shard = 'entropy' if element == 'chaos' else element
    write(DATA / 'recipes/collab/bees' / ('shard_' + element + '.json'), {
        'type': 'minecraft:crafting_shapeless',
        'ingredients': [{'item': NS + 'bee_dust_' + element}] * 4,
        'result': {'item': 'thaumcraft:' + shard + '_shard'}, 'conditions': CONDS})

for kind, zh, colors, aspect in [
        ('intellect', '智慧', [37609, 6393855], 'cognitio'),
        ('temporal', '时光', [3117953, 7814193], NS + 'tempus'),
        ('soul', '灵魂', [0x7F7171, 8875347], 'spiritus')]:
    product('bee_comb_' + kind, zh + '蜂巢', kind.title() + ' Comb',
            forestry_layers('beeCombs', colors), {'bestia': 1, aspect: 2})
for kind, zh, index, color, essence in [
        ('magic', '魔法', 0, 13779679, {'praecantatio': 1}),
        ('soul', '灵魂', 1, 9862243, {'spiritus': 1})]:
    product('bee_wax_' + kind, zh + '蜂蜡', kind.title() + ' Wax',
            [(magic, f'assets/magicbees/textures/items/wax.{index}.png', color)], essence)
for kind, zh, colors in [('unusual', '奇异', [14172539, 10498137]),
                          ('phased', '相位', [4814004, 4549541])]:
    product('bee_pollen_' + kind, zh + '花粉', kind.title() + ' Pollen',
            forestry_layers('pollen', colors), {'herba': 1, 'praecantatio': 1})
product('bee_propolis_unstable', '不稳定蜂胶', 'Unstable Propolis',
        forestry_layers('propolis', [15709330]), {'permutatio': 2})
product('bee_drop_intellect', '智慧蜜滴', 'Drop of Intellect',
        forestry_layers('honeyDrop', [2462029, 1630322]), {'cognitio': 1})
product('bee_drop_enchanted', '魔力蜜滴', 'Enchanted Drop',
        forestry_layers('honeyDrop', [7216237, 0xFF8FFF]), {'praecantatio': 1})
product('bee_lore_fragment', '学识碎片', 'Lore Fragment',
        [(magic, 'assets/magicbees/textures/items/fragment.png', 0xFFFFFF)], {'cognitio': 1})
centrifuge('bee_comb_intellect', 20, [(NS + 'bee_wax_magic', .9),
    ('forestry:honeydew', .4), (NS + 'bee_drop_intellect', .1)])
centrifuge('bee_comb_temporal', 20, [(NS + 'bee_wax_magic', 1.0),
    (NS + 'bee_pollen_phased', .055), ('forestry:honeydew', .6)])
centrifuge('bee_comb_soul', 20, [(NS + 'bee_wax_soul', .95), ('forestry:honeydew', .26)])
legacy_combs = [
    ('mundane', '平凡', [16750681, 16762254], [('forestry:beeswax', .9), ('forestry:honey_drop', .6), (NS + 'bee_wax_magic', .1)]),
    ('molten', '熔融', [0xCC3333, 1971726], [('forestry:refractory_wax', .86), ('forestry:honey_drop', .087)]),
    ('occult', '玄秘', [9990911, 0x2D2D2D], [(NS + 'bee_wax_magic', 1.0), ('forestry:honey_drop', .6)]),
    ('otherworldly', '异界', [4120792, 3815456], [('forestry:beeswax', .5), (NS + 'bee_wax_magic', .22), ('forestry:honey_drop', 1.0)]),
    ('transmuted', '嬗变', [15024733, 3289745], [('forestry:beeswax', .8), (NS + 'bee_wax_magic', .8), (NS + 'bee_propolis_unstable', .15)]),
    ('furtive', '隐秘', [12037303, 0x636363], [('forestry:beeswax', .9), ('forestry:propolis_normal', .2), ('forestry:honeydew', .35)]),
    ('airy', '轻盈', [0xFFFF7E, 6316808], [(NS + 'bee_wax_magic', 1.0), ('minecraft:feather', .6)]),
    ('firey', '炽热', [16727041, 5967120], [(NS + 'bee_wax_magic', 1.0), ('minecraft:blaze_powder', .6)]),
    ('watery', '水润', [37119, 1060715], [(NS + 'bee_wax_magic', 1.0), ('minecraft:ink_sac', .6)]),
    ('earthy', '大地', [40960, 274436], [(NS + 'bee_wax_magic', 1.0), ('minecraft:clay_ball', .6)])]
for name, zh, colors, outputs in legacy_combs:
    product('bee_comb_legacy_' + name, zh + '蜂巢', name.title() + ' Comb',
            forestry_layers('beeCombs', colors), {'bestia': 1, 'praecantatio': 1})
    centrifuge('bee_comb_legacy_' + name, 20, outputs)
write(DATA / 'item_aspects/collab_magic_bee_products.json', {'format': 1, 'entries': aspects})

key = NS + 'MAGIC_BEES'
write(DATA / 'research_catalog/magic_bees.json', {'format': 1, 'categories': [{
    'key': 'THAUMICOMNIVERSE', 'icon': NS + 'textures/gui/research_category.png',
    'background': 'thaumcraft:textures/gui/gui_researchback.png', 'research': [{
        'key': key, 'category': 'THAUMICOMNIVERSE', 'displayColumn': 0, 'displayRow': 26,
        'complexity': 1, 'isSecondary': True, 'parents': ['BASICTHAUMATURGY'], 'pageCount': 1,
        'aspects': {'bestia': 2, 'praecantatio': 2}, 'omniverse_requires': ['forestry', 'mobees'],
        'icon_item': {'item': NS + 'bee_comb_air'}}]}]})
write(DATA / 'research_pages/magic_bees.json', {'format': 1, 'entries': [{
    'key': key, 'omniverse_requires': ['forestry', 'mobees'],
    'pages': [{'type': 'TEXT', 'text': 'omniverse.page.magic_bee_products'}]}]})
names['tc.research_name.' + key] = ('神秘养蜂', 'Thaumic Apiculture')
names['tc.research_text.' + key] = ('花粉中的魔法', 'Magic in pollen')
names['omniverse.page.magic_bee_products'] = (
    '要素蜂巢可以在林业离心机中分离。每次处理都会产生魔法蜂蜡，并有机会得到副产物和对应蜂胶。再次离心蜂胶可以得到普通蜂胶，并有九成概率得到晶尘。四份同种晶尘可以合成一枚对应蕴魔石。智慧、时光和灵魂蜂巢另有各自的产物；加工概率可在配方查看器中查阅。',
    'Process infused combs in a Forestry centrifuge for magic wax, a chance of a byproduct and matching propolis. Centrifuging that propolis gives normal propolis and a 90% chance of crystal dust. Craft four matching dusts into one infused shard. Intellect, temporal and soul combs have their own products. Consult the recipe viewer for processing probabilities.')

tool_rows = [
    ('thaumium_scoop', 'thaumiumScoop', '神秘捕蜂网', 'Thaumium Scoop', 'MB_SCOOP', ['THAUMIUM'],
     ['sWs', 'sTs', ' T '], {'s': {'item': 'minecraft:stick'}, 'W': {'tag': 'minecraft:wool'},
      'T': {'item': 'thaumcraft:thaumium_ingot'}}, {'ordo': 2}, {'instrumentum': 1, 'praecantatio': 1, 'aer': 1},
     '神秘捕蜂网拥有 30 点耐久，可用神秘锭修复，并支持修复附魔。',
     'The thaumium scoop has 30 durability, accepts thaumium ingots for repairs and supports Repair enchantments.'),
    ('thaumium_grafter', 'thaumiumGrafter', '神秘嫁接刀', 'Thaumium Grafter', 'MB_GRAFTER', [NS + 'MB_SCOOP'],
     ['  T', ' s ', 's  '], {'s': {'item': 'minecraft:stick'}, 'T': {'item': 'thaumcraft:thaumium_ingot'}},
     {'ordo': 5}, {'instrumentum': 1, 'arbor': 1, 'lucrum': 1},
     '神秘嫁接刀拥有 15 次使用次数。通过林业嫁接接口提高树苗掉落率，可用神秘锭修复。修复附魔额外消耗与附魔等级平方成正比的饥饿消耗度。',
     'This grafter has 15 uses and increases sapling drops through Forestry. Repair it with thaumium ingots. The Repair enchantment additionally causes exhaustion proportional to the square of its level.'),
    ('bee_frame_magic', 'frameMagic', '魔法框架', 'Magic Frame', 'MB_FRAME_MAGIC', [key],
     ['sss', 'sCs', 'sss'], {'s': {'item': 'minecraft:stick'}, 'C': {'item': 'thaumcraft:enchanted_fabric'}},
     {'ordo': 5, 'aer': 2, 'terra': 2}, {'instrumentum': 1, 'tutamen': 1},
     '魔法框架拥有 240 点耐久，使生产速度翻倍，并将遗传退化倍率降低至原来的六成。',
     'The magic frame has 240 durability, doubles production speed and multiplies genetic decay by 0.6.')]
catalog_path = DATA / 'research_catalog/magic_bees.json'
pages_path = DATA / 'research_pages/magic_bees.json'
catalog = json.loads(catalog_path.read_text(encoding='utf-8'))
pages = json.loads(pages_path.read_text(encoding='utf-8'))
displays = []
for index, (item, texture, zh, en, short, parents, pattern, ingredients, vis, points, desc_zh, desc_en) in enumerate(tool_rows):
    research = NS + short
    product(item, zh, en, [(magic, f'assets/magicbees/textures/items/{texture}.png', 0xFFFFFF)], points)
    write(DATA / 'recipes/collab/bees' / (item + '.json'), {
        'type': 'thaumcraft:arcane', 'research': research, 'pattern': pattern, 'key': ingredients,
        'vis': vis, 'result': {'item': NS + item}, 'conditions': CONDS})
    catalog['categories'][0]['research'].append({
        'key': research, 'category': 'THAUMICOMNIVERSE', 'displayColumn': 4 + index * 3,
        'displayRow': 26, 'complexity': 2 if index == 1 else 1, 'parents': parents,
        'pageCount': 2, 'aspects': points, 'icon_item': {'item': NS + item},
        'omniverse_requires': ['forestry', 'mobees']})
    pages['entries'].append({'key': research, 'omniverse_requires': ['forestry', 'mobees'],
        'pages': [{'type': 'TEXT', 'text': 'omniverse.page.' + item},
                  {'type': 'ARCANE_CRAFTING', 'recipe': {'kind': 'reference',
                   'class': 'thaumcraft.api.crafting.ShapedArcaneRecipe', 'keys': [research]}}]})
    def stack(identifier):
        return {'kind': 'stack', 'stack': {'item': identifier, 'count': 1}}
    # Research viewer uses representative white wool for the modern wool tag.
    grid = [None if ch == ' ' else stack(ingredients[ch].get('item', 'minecraft:white_wool'))
            for line in pattern for ch in line]
    displays.append({'key': research, 'class': 'thaumcraft.api.crafting.ShapedArcaneRecipe',
        'omniverse_requires': ['forestry', 'mobees'], 'data': {'kind': 'arcane_shaped',
        'research': research, 'width': 3, 'height': 3, 'shaped': True, 'mirrored': True,
        'output': stack(NS + item), 'ingredients': {'kind': 'sequence', 'elements': grid},
        'aspects': [{'aspect': a, 'amount': n} for a, n in vis.items()]}})
    names['tc.research_name.' + research] = (zh, en)
    names['tc.research_text.' + research] = ('神秘养蜂器具', 'Tools for thaumic apiculture')
    names['omniverse.page.' + item] = (desc_zh, desc_en)
names['omniverse.bees.frame_modifiers'] = (
    '产量 ×%s；寿命 ×%s；突变 ×%s；退化 ×%s',
    'Production ×%s; lifespan ×%s; mutation ×%s; decay ×%s')

def add_research(short, item, title, parents, point_aspects, recipe_pages, description, column, row):
    research = NS + short
    catalog['categories'][0]['research'].append({
        'key': research, 'category': 'THAUMICOMNIVERSE', 'displayColumn': column, 'displayRow': row,
        'complexity': 2, 'parents': parents, 'pageCount': 1 + len(recipe_pages), 'aspects': point_aspects,
        'icon_item': {'item': NS + item}, 'omniverse_requires': ['forestry', 'mobees']})
    pages['entries'].append({'key': research, 'omniverse_requires': ['forestry', 'mobees'], 'pages': [
        {'type': 'TEXT', 'text': 'omniverse.page.' + item}] + recipe_pages})
    names['tc.research_name.' + research] = title
    names['tc.research_text.' + research] = ('蜂巢中的炼金术', 'Alchemy in the apiary')
    names['omniverse.page.' + item] = description

def reference(page_type, recipe_class, recipe_key):
    return {'type': page_type, 'recipe': {'kind': 'reference', 'class': recipe_class, 'keys': [recipe_key]}}

essences = [
    ('life', 'essenceLife', '虚假生命精华', 'Essence of False Life', 'minecraft:poppy',
     {'permutatio': 4, 'herba': 4}, ['CRUCIBLE'], 'gentle', 'Gentle', '温和'),
    ('armor', 'essenceDurability', '永恒耐久精华', 'Essence of Everlasting Durability', 'minecraft:leather',
     {'tutamen': 4, 'praecantatio': 2}, [NS + 'MB_ESSENCE_LIFE'], 'resilient', 'Resilient', '坚韧'),
    ('unstable', 'essenceMutable', '变幻恒常精华', 'Essence of Fickle Permanence', NS + 'bee_propolis_unstable',
     {'perditio': 2, 'permutatio': 1}, [NS + 'MB_ESSENCE_LIFE'], 'metabolic', 'Metabolic', '代谢'),
    ('death', 'essenceDeath', '浅墓精华', 'Essence of the Shallow Grave', 'minecraft:rotten_flesh',
     {'mortuus': 4, 'perditio': 4}, [NS + 'MB_ESSENCE_UNSTABLE'], 'necrotic', 'Necrotic', '坏死'),
    ('time', 'essenceTime', '失落时光精华', 'Essence of Lost Time', 'minecraft:clock',
     {'ordo': 10, 'vacuos': 10, 'vinculum': 4}, [NS + 'MB_ESSENCE_UNSTABLE'], 'temporal', 'Temporal', '时光')]
for index, (kind, texture, zh, en, catalyst, required, parents, frame, frame_en, frame_zh) in enumerate(essences):
    item = 'bee_essence_' + kind
    research = NS + 'MB_ESSENCE_' + kind.upper()
    product(item, zh, en, [(magic, f'assets/magicbees/textures/items/{texture}.png', 0xFFFFFF)],
            {'praecantatio': 2, next(iter(required)): 2})
    product('bee_frame_' + frame, frame_zh + '框架', frame_en + ' Frame',
            [(magic, f'assets/magicbees/textures/items/frame{frame_en}.png', 0xFFFFFF)],
            {'arbor': 2, 'praecantatio': 2})
    write(DATA / 'recipes/collab/bees' / (item + '.json'), {
        'type': 'thaumcraft:crucible', 'research': research, 'catalyst': {'item': catalyst},
        'aspects': required, 'result': {'item': NS + item}, 'conditions': CONDS})
    recipe_class = 'thaumcraft.api.crafting.CrucibleRecipe'
    displays.append({'key': research, 'class': recipe_class, 'omniverse_requires': ['forestry', 'mobees'],
        'data': {'kind': 'crucible', 'research': research, 'catalyst': stack(catalyst),
                 'output': stack(NS + item), 'aspects': [{'aspect': a, 'amount': n} for a, n in required.items()]}})
    write(DATA / 'recipes/collab/bees' / ('frame_' + frame + '.json'), {
        'type': 'minecraft:crafting_shapeless', 'ingredients': [
            {'item': NS + item}, {'item': NS + 'bee_frame_magic'}],
        'result': {'item': NS + 'bee_frame_' + frame}, 'conditions': CONDS})
    recipe_pages = [reference('CRUCIBLE_CRAFTING', recipe_class, research)]
    if kind == 'unstable':
        alternate = {'type': 'thaumcraft:crucible', 'research': research,
            'catalyst': {'item': 'minecraft:netherrack'}, 'aspects': {'perditio': 8, 'permutatio': 5},
            'result': {'item': NS + item}, 'conditions': CONDS}
        write(DATA / 'recipes/collab/bees/bee_essence_unstable_netherrack.json', alternate)
        displays.append({'key': research + '_NETHERRACK', 'class': recipe_class,
            'omniverse_requires': ['forestry', 'mobees'], 'data': {'kind': 'crucible', 'research': research,
            'catalyst': stack('minecraft:netherrack'), 'output': stack(NS + item),
            'aspects': [{'aspect': a, 'amount': n} for a, n in alternate['aspects'].items()]}})
        recipe_pages.append(reference('CRUCIBLE_CRAFTING', recipe_class, research + '_NETHERRACK'))
    add_research('MB_ESSENCE_' + kind.upper(), item, (zh, en), parents,
        {'praecantatio': 1, next(iter(required)): 1}, recipe_pages,
        (zh + '与魔法框架进行无序合成，可以制作' + frame_zh + '框架。蜂框提示中列出了各项倍率。',
         'Craft this essence with a magic frame to make a ' + frame_en.lower() +
         ' frame. Its tooltip lists the production, lifespan, mutation and genetic decay multipliers.'), index * 3, 30)

product('bee_dimensional_singularity', '维度奇点', 'Dimensional Singularity',
    [(magic, 'assets/magicbees/textures/items/dimensionalSingularity.png', 0xFFFFFF)], {'alienis': 2, 'permutatio': 2})
singularity_pages = []
for suffix, center, parts, required, instability in [
    ('gold', 'minecraft:gold_block', ['minecraft:ender_pearl'] * 2, {'alienis': 10, 'permutatio': 20}, 5),
    ('propolis', NS + 'bee_propolis_unstable', ['minecraft:ender_pearl'] * 2 + ['minecraft:diamond'],
     {'alienis': 10, 'permutatio': 20, 'vacuos': 15}, 6)]:
    research = NS + 'MB_SINGULARITY'
    recipe_key = research + '_' + suffix.upper()
    write(DATA / 'recipes/collab/bees' / ('singularity_' + suffix + '.json'), {
        'type': 'thaumcraft:infusion', 'research': research, 'instability': instability,
        'center': {'ingredient': {'item': center}}, 'components': [{'ingredient': {'item': part}} for part in parts],
        'aspects': required, 'result': {'item': NS + 'bee_dimensional_singularity', 'count': 3}, 'conditions': CONDS})
    output = stack(NS + 'bee_dimensional_singularity'); output['stack']['count'] = 3
    recipe_class = 'thaumcraft.api.crafting.InfusionRecipe'
    displays.append({'key': recipe_key, 'class': recipe_class, 'omniverse_requires': ['forestry', 'mobees'],
        'data': {'kind': 'infusion', 'research': research, 'instability': instability,
        'input': stack(center), 'output': output, 'ingredients': {'kind': 'sequence', 'elements': [stack(x) for x in parts]},
        'aspects': [{'aspect': a, 'amount': n} for a, n in required.items()]}})
    singularity_pages.append(reference('INFUSION_CRAFTING', recipe_class, recipe_key))
add_research('MB_SINGULARITY', 'bee_dimensional_singularity', ('维度奇点', 'Dimensional Singularity'),
    ['INFUSION', NS + 'MB_ESSENCE_UNSTABLE'], {'alienis': 2, 'permutatio': 2}, singularity_pages,
    ('通过注魔将末影珍珠的维度能量凝结为三个奇点。两种配方均可使用。',
     'Infuse the dimensional energy of ender pearls into three singularities using either recipe.'), 4, 34)
product('bee_essence_oblivion', '蔑视遗忘精华', 'Essence of Scornful Oblivion',
    [(magic, 'assets/magicbees/textures/items/essenceOblivion.png', 0xFFFFFF)], {'alienis': 2, 'perditio': 2})
product('bee_frame_oblivion', '遗忘框架', 'Oblivion Frame',
    [(magic, 'assets/magicbees/textures/items/frameOblivion.png', 0xFFFFFF)], {'arbor': 2, 'perditio': 2})
research = NS + 'MB_ESSENCE_OBLIVION'
parts = [NS + 'bee_dimensional_singularity', 'minecraft:dragon_egg']
vis = {'perditio': 25, 'aer': 40, 'ordo': 15}
write(DATA / 'recipes/collab/bees/bee_essence_oblivion.json', {
    'type': 'thaumcraft:arcane', 'mode': 'shapeless', 'research': research,
    'ingredients': [{'item': part} for part in parts], 'vis': vis,
    'result': {'item': NS + 'bee_essence_oblivion'}, 'conditions': CONDS})
recipe_class = 'thaumcraft.api.crafting.ShapelessArcaneRecipe'
displays.append({'key': research, 'class': recipe_class, 'omniverse_requires': ['forestry', 'mobees'],
    'data': {'kind': 'arcane_shapeless', 'research': research, 'shaped': False,
    'output': stack(NS + 'bee_essence_oblivion'), 'ingredients': {'kind': 'sequence', 'elements': [stack(x) for x in parts]},
    'aspects': [{'aspect': a, 'amount': n} for a, n in vis.items()]}})
add_research('MB_ESSENCE_OBLIVION', 'bee_essence_oblivion', ('蔑视遗忘精华', 'Essence of Scornful Oblivion'),
    [NS + 'MB_SINGULARITY'], {'alienis': 2, 'perditio': 2}, [reference('ARCANE_CRAFTING', recipe_class, research)],
    ('此精华会消耗龙蛋。将其与久经考验的框架无序合成得到遗忘框架：蜂群寿命降至万分之一，停止生产，但仍可繁殖。',
     'This essence consumes the dragon egg. Craft it with a proven frame for an oblivion frame: lifespan becomes one ten-thousandth of normal and production stops, while breeding remains possible.'), 8, 34)
write(DATA / 'recipes/collab/bees/frame_oblivion.json', {'type': 'minecraft:crafting_shapeless',
    'ingredients': [{'item': NS + 'bee_essence_oblivion'}, {'item': 'forestry:frame_proven'}],
    'result': {'item': NS + 'bee_frame_oblivion'}, 'conditions': CONDS})
write(DATA / 'recipes/collab/bees/experience_bottle.json', {'type': 'minecraft:crafting_shaped',
    'pattern': ['DDD', 'DBD', 'DDD'], 'key': {'D': {'item': NS + 'bee_drop_intellect'},
    'B': {'item': 'minecraft:glass_bottle'}}, 'result': {'item': 'minecraft:experience_bottle'}, 'conditions': CONDS})
write(DATA / 'recipes/collab/bees/frame_temporal_pollen.json', {'type': 'minecraft:crafting_shaped',
    'pattern': ['sPs', 'PfP', 'sPs'], 'key': {'s': {'tag': 'minecraft:sand'},
    'P': {'item': NS + 'bee_pollen_phased'}, 'f': {'item': NS + 'bee_frame_magic'}},
    'result': {'item': NS + 'bee_frame_temporal'}, 'conditions': CONDS})

for kind, meta, zh, en in [('deep', 3, '深邃蜂巢', 'Deep Hive'), ('infernal', 4, '炼狱蜂巢', 'Infernal Hive')]:
    name = 'bee_hive_' + kind
    names['block.thaumicomniverse.' + name] = (zh, en)
    for face in ('top', 'side'):
        target = ASSETS / 'textures/block' / (name + '_' + face + '.png')
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(magic.read(f'assets/magicbees/textures/blocks/beehive.{meta}.{face}.png'))
    write(ASSETS / 'models/block' / (name + '.json'), {
        'parent': 'minecraft:block/cube_column', 'textures': {
            'end': NS + 'block/' + name + '_top', 'side': NS + 'block/' + name + '_side'}})
    write(ASSETS / 'models/item' / (name + '.json'), {'parent': NS + 'block/' + name})
    write(ASSETS / 'blockstates' / (name + '.json'), {'variants': {'': {'model': NS + 'block/' + name}}})
    aspects.append({'items': [NS + name], 'aspects': [
        {'aspect': 'bestia', 'amount': 4}, {'aspect': 'terra' if kind == 'deep' else 'ignis', 'amount': 4}]})
add_research('MB_WILD_HIVES', 'bee_hive_deep', ('野生神秘蜂巢', 'Wild Magic Hives'), [], {'bestia': 2, 'terra': 2}, [], (
    '调谐蜂可从深邃蜂巢取得：山地、丘陵及魔法群系的 Y=10–24，包裹于至少五面石头中。炼狱蜂巢生成于下界岩中（Y=0–174），也罕见于魔法或炎热群系的 Y=5–17。它们附近分别出现红石、石英或荧石。用林业捕蜂网采集；精准采集捕蜂网可搬运蜂巢。深邃蜂巢也可能产出耐雨调谐蜂或英勇蜂；炼狱蜂巢偶尔产出坚定蜂。',
    'Attuned bees inhabit Deep Hives at Y=10–24 in hills, mountains and magical biomes, surrounded by at least five stone blocks. Infernal Hives occur in netherrack at Y=0–174, and rarely at Y=5–17 in hot or magical overworld biomes. Nearby deposits are redstone, quartz or glowstone respectively. Harvest with a Forestry scoop; a Silk Touch scoop moves the hive. Deep hives may yield rain-resistant Attuned or Valiant bees; Infernal hives occasionally yield Steadfast bees.'), 1, 26)

for name, zh, en in [('magic_apiary', '魔法蜂箱', 'Magic Apiary'), ('vis_aura_provider', '魔力灵气供给器', 'Vis Aura Provider')]:
    names['block.thaumicomniverse.' + name] = (zh, en)
    write(ASSETS / 'blockstates' / (name + '.json'), {'variants': {'': {'model': NS + 'block/' + name}}})
    write(ASSETS / 'models/item' / (name + '.json'), {'parent': NS + 'block/' + name})
    write(DATA / 'loot_tables/blocks' / (name + '.json'), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': NS + name}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    aspects.append({'items': [NS + name], 'aspects': [
        {'aspect': 'praecantatio', 'amount': 8}, {'aspect': 'arbor', 'amount': 8}]})
for face in (0, 1, 2, 4):
    target = ASSETS / 'textures/block' / f'magic_apiary_{face}.png'
    target.write_bytes(magic.read(f'assets/magicbees/textures/blocks/thaumicapiary.{face}.png'))
write(ASSETS / 'models/block/magic_apiary.json', {'parent': 'minecraft:block/cube', 'textures': {
    side: NS + 'block/magic_apiary_' + str(face) for side, face in
    [('down', 0), ('up', 1), ('north', 2), ('south', 2), ('east', 4), ('west', 4), ('particle', 2)]}})
# Preserve the original mesh and UVs; the modern material points into the block atlas.
mesh = magic.read('assets/magicbees/model/visAuraConverter.obj').decode('utf-8')
mesh = 'mtllib vis_aura_provider.mtl\nusemtl provider\n' + mesh
(ASSETS / 'models/block/vis_aura_provider.obj').write_text(mesh, encoding='utf-8')
(ASSETS / 'models/block/vis_aura_provider.mtl').write_text(
    'newmtl provider\nKd 1 1 1\nmap_Kd thaumicomniverse:block/vis_aura_provider\n', encoding='utf-8')
(ASSETS / 'textures/block/vis_aura_provider.png').write_bytes(magic.read('assets/magicbees/model/visAuraProvider.png'))
(ASSETS / 'textures/gui').mkdir(parents=True, exist_ok=True)
(ASSETS / 'textures/gui/magic_apiary.png').write_bytes(magic.read('assets/magicbees/textures/gui/apiary.png'))
names['omniverse.bees.apiary_boosts'] = ('突变 ×%s；衰老 ×%s；产量 ×%s', 'Mutation ×%s; aging ×%s; production ×%s')
# Split only faces; retain every vertex/UV/normal so original global indices remain valid.
for part in ('centre', 'frame'):
    group = None
    lines = []
    for line in mesh.splitlines():
        if line.startswith('o '): group = line[2:].strip()
        if not line.startswith(('o ', 'f ')) or group == part:
            lines.append(line)
    (ASSETS / 'models/block' / ('vis_aura_' + part + '.obj')).write_text('\n'.join(lines) + '\n', encoding='utf-8')
    write(ASSETS / 'models/block' / ('vis_aura_' + part + '.json'), {'loader': 'forge:obj',
        'model': NS + 'models/block/vis_aura_' + part + '.obj', 'flip_v': True,
        'textures': {'particle': NS + 'block/vis_aura_provider'}})
write(ASSETS / 'blockstates/vis_aura_provider.json', {'variants': {'': {'model': NS + 'block/vis_aura_frame'}}})
write(ASSETS / 'models/block/vis_aura_provider.json', {'loader': 'forge:obj',
    'model': NS + 'models/block/vis_aura_provider.obj', 'flip_v': True,
    'textures': {'particle': NS + 'block/vis_aura_provider'}})
write(DATA / 'recipes/collab/bees/magic_apiary.json', {'type': 'minecraft:crafting_shapeless', 'conditions': CONDS,
    'ingredients': [{'item': NS + part} for part in ['bee_pollen_unusual'] * 2 + ['bee_drop_enchanted'] * 2]
        + [{'item': 'forestry:apiary'}], 'result': {'item': NS + 'magic_apiary'}})
research = NS + 'MB_VIS_AURA_PROVIDER'
pattern = ['ngn', 'gvg', 'npn']
key = {'n': {'item': 'minecraft:gold_nugget'}, 'g': {'item': 'thaumcraft:greatwood_planks'},
       'v': {'item': 'thaumcraft:vis_relay'}, 'p': {'item': NS + 'bee_pollen_unusual'}}
vis = {'ordo': 60, 'aer': 60, 'perditio': 60, 'aqua': 60}
write(DATA / 'recipes/collab/bees/vis_aura_provider.json', {'type': 'thaumcraft:arcane', 'research': research,
    'pattern': pattern, 'key': key, 'vis': vis, 'result': {'item': NS + 'vis_aura_provider'}, 'conditions': CONDS})
recipe_class = 'thaumcraft.api.crafting.ShapedArcaneRecipe'
displays.append({'key': research, 'class': recipe_class, 'omniverse_requires': ['forestry', 'mobees'], 'data': {
    'kind': 'arcane_shaped', 'research': research, 'width': 3, 'height': 3, 'shaped': True, 'mirrored': True,
    'output': stack(NS + 'vis_aura_provider'), 'ingredients': {'kind': 'sequence',
    'elements': [stack(key[ch]['item']) for line in pattern for ch in line]},
    'aspects': [{'aspect': a, 'amount': n} for a, n in vis.items()]}})
add_research('MB_MAGIC_APIARY', 'magic_apiary', ('魔法蜂箱', 'Magic Apiary'), [NS + 'MB_WILD_HIVES'], {'bestia': 2}, [], (
    '将林业蜂箱与两份异常花粉、两份魔力蜜滴无序合成。奥术蜂的特殊产物有 9% 概率产生魔力蜜滴。蜂箱支持三个框架槽；基础产量为 0.9 倍，遗传退化为 0.8 倍。供给器可增强六格范围内的魔法蜂箱。',
    'Craft a Forestry apiary with two unusual pollen and two enchanted drops. Arcane bees produce enchanted drops as a 9% specialty. The apiary accepts three frames, has base production of 0.9x and genetic decay of 0.8x. A provider can boost magic apiaries within six blocks.'), 1, 29)
add_research('MB_VIS_AURA_PROVIDER', 'vis_aura_provider', ('魔力灵气供给器', 'Vis Aura Provider'), ['VISPOWER', NS + 'MB_MAGIC_APIARY'],
    {'praecantatio': 1, 'potentia': 1, 'auram': 1}, [reference('ARCANE_CRAFTING', recipe_class, research)], (
    '从魔力网络汲取水、混沌和风能量，分别将突变率提高为两倍、衰老速度提高为两倍、产量提高至 1.8 倍。每份充能消耗 8 个网络单位（0.08 vis），三个通道各最多储存 10 份。每份增益分别持续 850、400、300 tick；用尽后自动续充，断供时已有增益会持续至到期。',
    'Draws water, entropy and air from the vis network to double mutation chance, double aging speed, and raise production to 1.8x. Each charge costs eight network units (0.08 vis); each channel stores up to ten charges. Effects last 850, 400 and 300 ticks respectively, renewing when charges are available. Existing effects last until expiry after supply stops.'), 1, 32)

write(DATA / 'recipes/collab/bees/knowledge_fragment.json', {'type': 'minecraft:crafting_shapeless',
    'conditions': CONDS, 'ingredients': [{'item': NS + 'bee_lore_fragment'}] * 4,
    'result': {'item': 'thaumcraft:knowledge_fragment'}})
add_research('MB_LORE_FRAGMENT', 'bee_lore_fragment', ('学识碎片', 'Lore Fragments'), [NS + 'MB_WILD_HIVES'], {'cognitio': 1}, [], (
    '好学蜂和博学蜂（Mo’Bees 学术分支的第二、第三阶段）分别有 2% 和 5% 的特殊产物概率产生学识碎片。将四片无序合成为神秘时代的知识碎片。特殊产物仍需要满足蜂种的气候和花卉条件。',
    'Studious and Erudite bees, the second and third stages of the Mo’Bees scholarly branch, produce Lore Fragments as 2% and 5% specialties. Craft four fragments shapelessly into a Thaumcraft Knowledge Fragment. Specialty production still requires the species climate and flower conditions.'), 1, 35)

guide_zh = [
    '从野生深邃蜂巢取得调谐蜂，或使用 Mo’Bees 的异常蜂。它们与普通蜂交配可通向勤奋蜂；与勤奋蜂交配得到诡异蜂。勤奋×诡异得到迷人蜂，诡异×迷人得到附魔蜂，迷人×附魔得到超自然蜂。奥术×超自然得到空灵蜂；空灵×超自然在蜂箱下方分别放置树叶、水、黏土、熔岩，可得到风、水、土、火系前置蜂。实际名称与基因、详细概率可在林业分析仪及配方查看器查看。',
    '四种元素前置蜂分别自交，蜂箱下方放对应蕴魔晶簇，8% 产生风、火、水、土神秘蜂。空灵×奥术在秩序晶簇上产生秩序蜂，空灵×超自然在混沌晶簇上产生混沌蜂，均为8%。资源条件检查蜂箱堆叠下方的第一个非蜂箱方块。',
    '空灵×炼狱产生魔力蜂（9%，上弦月至下弦月，跨满月）。调谐×魔力产生再生蜂（8%）。魔力×再生产生强化蜂（6%，满月额外×1.322）。再生×强化产生枢纽蜂（10%，魔法群系、蜂箱下方灵气节点）。嬗变×强化产生腐化蜂（11%，新月）；嬗变×再生产生净化蜂（8%，魔法群系、新月）。大坏狼×魔力产生饥饿蜂（20%）；空灵×恶魂产生精灵蜂（9%，残月至蛾眉月）；铁×腐化产生虚空蜂（5%，魔法群系、夜间）。月相按旧版正午分日。',
    '魔力蜂恢复节点要素，再生蜂修复节点品质，强化蜂永久增加节点容量。腐化、净化和饥饿蜂会改变节点类型；它们只修改真实节点，不会将临时增幅转成永久容量。节点类花卉接受普通或充能节点，但不会凭空生成节点。神秘花卉接受水银花和火焰花；蜂群可按原生林业规则授粉。',
    '潜行×学生产生脑髓蜂（9%）；普通×潜行在森林、平原、山地分别产生鸡、牛、猪蜂（12%）。脑髓蜂提供僵尸之脑；肉类蜂提供对应肉粒。脑髓蜂会在附近有未受完整养蜂服保护的玩家时生成脑髓僵尸。动物蜂的领地内会出现对应动物，并受本地数量上限限制。精灵蜂同样可生成精灵，附近空间及难度仍须允许生成。']
guide_en = [
    'Begin with Attuned bees from Deep Hives or native Mo’Bees Unusual bees. Cross these roots with Common for Cultivated, then cross a root with Cultivated for Eldritch. Cultivated x Eldritch yields Charmed; Eldritch x Charmed yields Enchanted; Charmed x Enchanted yields Supernatural. Arcane x Supernatural yields Ethereal. Supernatural x Ethereal yields Windy, Watery, Earthen or Firey with leaves, water, clay or lava beneath the housing. Use Forestry analysis and the recipe viewer for complete genomes and probabilities.',
    'Self-cross each elemental predecessor with its matching infused crystal beneath the housing for an 8% chance of the corresponding thaumic bee. Ethereal x Arcane yields Order on an order crystal; Ethereal x Supernatural yields Chaos on an entropy crystal, both at 8%. Resource conditions examine the first non-housing block underneath a housing stack.',
    'Ethereal x Infernal: Vis,9%,first quarter through last quarter across full moon. Attuned x Vis: Rejuvenating,8%. Vis x Rejuvenating: Empowering,6%,full moon bonus x1.322. Rejuvenating x Empowering: Nexus,10%,magical biome and aura node below. Transmuting x Empowering: Taint,11%,new moon. Transmuting x Rejuvenating: Pure,8%,magical biome and new moon. BigBad x Vis: Hungry,20%. Ethereal x Ghastly: Wispy,9%,waning through waxing crescent across new moon. Iron x Taint: Void,5%,magical biome at night. Moon days change at noon as in the original.',
    'Vis restores node aspects, Rejuvenating repairs node quality, and Empowering permanently expands capacity. Taint, Pure and Hungry change node type. Effects operate on permanent node state and never promote temporary amplifiers into permanent capacity. Node flowers accept normal and energized nodes without creating new nodes. Thaumic flowers accept shimmerleaf and cinderpearl; native Forestry pollination remains available.',
    'Skulking x Pupil yields Brainy at9%. Common x Skulking yields Chicken, Beef or Pork at12% in forest, plains or mountain biomes respectively. Brainy produces zombie brains; meat bees provide matching meat nuggets. Brainy bees summon brainy zombies only near players without a complete protective apiarist suit. Animal bees summon their animals within local population caps. Wispy bees summon wisps when space and difficulty allow.']
add_research('MB_BREEDING', 'bee_comb_air', ('神秘蜜蜂谱系', 'Thaumic Bee Breeding'), [NS + 'MB_WILD_HIVES'], {'bestia': 2, 'cognitio': 2}, [],
    (guide_zh[0], guide_en[0]), 1, 38)
for index in range(1, len(guide_zh)):
    key = 'omniverse.page.bee_breeding.' + str(index)
    names[key] = (guide_zh[index], guide_en[index])
    pages['entries'][-1]['pages'].append({'type': 'TEXT', 'text': key})
catalog['categories'][0]['research'][-1]['pageCount'] = len(guide_zh)

write(catalog_path, catalog)
write(pages_path, pages)
write(DATA / 'research_page_recipes/magic_bees.json', {'format': 1, 'entries': displays})
write(DATA / 'item_aspects/collab_magic_bee_products.json', {'format': 1, 'entries': aspects})
write(ROOT / 'data/forestry/tags/items/scoops.json', {
    'replace': False, 'values': [{'id': NS + 'thaumium_scoop', 'required': False}]})
legacy_languages = []
for language in ('zh_CN', 'en_US'):
    contents = magic.read(f'assets/magicbees/lang/{language}.lang').decode('utf-8-sig')
    legacy_languages.append(dict(line.split('=', 1) for line in contents.splitlines()
                                 if '=' in line and not line.lstrip().startswith('#')))
species = Path(__file__).resolve().parents[2] / 'ThaumicFutures/src/main/resources/data/thaumicfutures/forestry/legacy_species.json'
for definition in json.loads(species.read_text(encoding='utf-8')):
    old_key = 'magicbees.species' + definition['legacy_name']
    english = legacy_languages[1].get(old_key, definition['name'].title())
    names['allele.forestry.bee_species.thaumicomniverse.bee_legacy_' + definition['name']] = (
        legacy_languages[0].get(old_key, english), english)
names['omniverse.bees.leaves_below'] = ('蜂箱下方需要树叶', 'Requires leaves beneath the housing')
names['omniverse.bees.feature_required'] = ('需要启用神秘万象的林业蜜蜂联动', 'Requires Thaumic Omniverse Forestry bee integration')
write(DATA / 'tags/worldgen/biome/plains.json', {'replace': False, 'values': [
    {'id': '#forge:is_plains', 'required': False}, 'minecraft:plains', 'minecraft:sunflower_plains']})
for effect, zh, en in [('batty', '蝙蝠', 'Batty'), ('ghastly', '恶魂', 'Ghastly'),
                      ('wolf', '犬类', 'Canine'), ('transmuting', '嬗变', 'Transmuting'),
                      ('brainy', '脑髓僵尸', 'Brainy'), ('chicken', '鸡群', 'Chicken'),
                      ('cow', '牛群', 'Bovine'), ('pig', '猪群', 'Porcine')]:
    names['allele.forestry.bee_effect.thaumicomniverse.effect_legacy_' + effect] = (zh, en)
for name in ('air', 'fire', 'water', 'earth', 'order', 'chaos', 'vis', 'rejuvenating',
             'empowering', 'nexus', 'taint', 'pure', 'hungry', 'wispy', 'void'):
    legacy_name = 'TC' + ('Flux' if name == 'taint' else name.title())
    old_key = 'magicbees.species' + legacy_name
    english = legacy_languages[1].get(old_key, name.title())
    names['allele.forestry.bee_species.thaumicomniverse.bee_thaumic_' + name] = (
        legacy_languages[0].get(old_key, english), english)
for name, zh, en in [
        ('flowers_thaumic', '神秘花卉', 'Thaumic Flowers'),
        ('flowers_aura_nodes', '灵气节点', 'Aura Nodes')]:
    names['allele.forestry.flower_type.thaumicomniverse.' + name] = (zh, en)
for name, zh, en in [
        ('node_recharge', '节点充能', 'Node Recharging'), ('node_empower', '节点扩容', 'Node Empowering'),
        ('node_repair', '节点修复', 'Node Repair'), ('node_taint', '节点腐化', 'Node Tainting'),
        ('node_pure', '节点净化', 'Node Purifying'), ('node_hungry', '节点饥饿化', 'Node Ravening'),
        ('speed', '迅捷', 'Speed'), ('haste', '急迫', 'Haste'), ('cleanse', '净化', 'Cleansing'),
        ('wispy', '精灵', 'Wispy')]:
    names['allele.forestry.bee_effect.thaumicomniverse.effect_' + name] = (zh, en)
names['omniverse.bees.moon_required'] = ('需要月相：%s 至 %s', 'Requires moon phases %s through %s')
names['omniverse.bees.moon_bonus'] = ('月相 %s 至 %s：突变概率 ×%s', 'Moon phases %s through %s: mutation chance ×%s')
for phase, (zh, en) in enumerate([
        ('满月', 'Full Moon'), ('亏凸月', 'Waning Gibbous'), ('下弦月', 'Last Quarter'),
        ('残月', 'Waning Crescent'), ('新月', 'New Moon'), ('蛾眉月', 'Waxing Crescent'),
        ('上弦月', 'First Quarter'), ('盈凸月', 'Waxing Gibbous')]):
    names['omniverse.bees.moon.' + str(phase)] = (zh, en)
for index, language in enumerate(('zh_cn', 'en_us')):
    path = ASSETS / 'lang' / (language + '.json')
    data = json.loads(path.read_text(encoding='utf-8')) if path.is_file() else {}
    data.update({key: pair[index] for key, pair in names.items()})
    write(path, data)
print(f'Generated {len(aspects)} Magic Bees products and their processing chain')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](BASE)
