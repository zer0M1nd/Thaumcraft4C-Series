"""Machine ports: original available shells/templates, authored missing aspect faces and recipes."""
import io
import json
from pathlib import Path
from zipfile import ZipFile
from PIL import Image

base = Path(__file__).resolve().parents[1]
root = base / 'src/main/resources'
assets = root / 'assets/thaumicomniverse'
data = root / 'data/thaumicomniverse'
ns = 'thaumicomniverse:'
legacy = ZipFile(base.parents[1] / 'Collaboration/jars/omoshiroikamo-26.09.20.0.jar')
core = ZipFile(base.parents[1] / 'Collaboration/jars/Thaumcraft-1.7.10-4.2.3.5.jar')
condition = [{'type': 'forge:mod_loaded', 'modid': 'modular_machinery_reborn'}]
faces = ['down', 'up', 'north', 'south', 'west', 'east']
names, aspects, block_ids = {}, [], []

def write(path, content):
    if 'loot_tables' in path.parts: content['omniverse_requires'] = ['modular_machinery_reborn']
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(content, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def sprite(archive, path):
    return Image.open(io.BytesIO(archive.read(path))).convert('RGBA')

def save_texture(name, image):
    path = assets / 'textures/block' / (name + '.png')
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)

for resource, zh, en, glyph in [('essentia', '源质', 'Essentia', 'praecantatio'), ('vis', '魔力', 'Vis', 'aer')]:
    for direction, cn, english in [('input', '输入', 'Input'), ('output', '输出', 'Output')]:
        for tier in range(1, 17):
            name = f'machine_{resource}_{direction}_{tier}'
            block_ids.append(ns + name)
            names['block.thaumicomniverse.' + name] = (f'{zh}{cn}端口 · {tier}', f'{en} {english} Port {tier}')
            shell = sprite(legacy, f'assets/omoshiroikamo/textures/blocks/modular/tier_{min(tier - 1, 5)}_base.png')
            enabled = shell.copy()
            overlay = sprite(legacy, f'assets/omoshiroikamo/textures/blocks/modularmachineryOverlay/overlay_energy{direction}_{tier}.png')
            if overlay.size != enabled.size: overlay = overlay.resize(enabled.size, Image.Resampling.NEAREST)
            enabled.alpha_composite(overlay)
            # The legacy release lacks the referenced essentia/vis overlays. Keep its tier/direction template,
            # replacing only the centre with the corresponding authentic TC aspect glyph.
            size = max(4, enabled.width // 2)
            icon = sprite(core, f'assets/thaumcraft/textures/aspects/{glyph}.png').resize((size, size), Image.Resampling.NEAREST)
            enabled.alpha_composite(icon, ((enabled.width - size) // 2, (enabled.height - size) // 2))
            save_texture(name + '_off', shell)
            save_texture(name + '_on', enabled)
            parts = []
            for face in faces:
                for active in (False, True):
                    variant = name + '_' + face + ('_on' if active else '_off')
                    texture = ns + 'block/' + name + ('_on' if active else '_off')
                    write(assets / 'models/block' / (variant + '.json'), {'textures': {'face': texture, 'particle': texture},
                        'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {
                            face: {'texture': '#face', 'cullface': face}}}]})
                    parts.append({'when': {face: str(active).lower()}, 'apply': {'model': ns + 'block/' + variant}})
            write(assets / 'blockstates' / (name + '.json'), {'multipart': parts})
            write(assets / 'models/item' / (name + '.json'), {'parent': 'minecraft:block/cube_all',
                'textures': {'all': ns + 'block/' + name + '_on'}})
            if tier == 1:
                write(data / 'recipes/collab/machinery' / (name + '.json'), {'type': 'minecraft:crafting_shaped',
                    'conditions': condition, 'pattern': ['TCT', 'GHG', 'TCT'], 'key': {
                        'T': {'item': 'thaumcraft:thaumium_ingot'}, 'C': {'item': 'modular_machinery_reborn:blockcasing_plain'},
                        'G': {'tag': 'forge:glass'}, 'H': {'item': 'thaumcraft:essentia_jar' if resource == 'essentia' else 'thaumcraft:vis_relay'}},
                    'result': {'item': ns + name}})
            else:
                previous = ns + f'machine_{resource}_{direction}_{tier - 1}'
                write(data / 'recipes/collab/machinery' / (name + '.json'), {'type': 'minecraft:crafting_shapeless',
                    'conditions': condition, 'ingredients': [{'item': previous}] * (2 if tier <= 11 else 1)
                        + [{'item': 'thaumcraft:thaumium_ingot'}], 'result': {'item': ns + name}})
            # Dropped ports are empty; this recipe only reconfigures the empty block item.
            if direction == 'output':
                for src, dst in [('input', 'output'), ('output', 'input')]:
                    write(data / 'recipes/collab/machinery' / f'{resource}_{src}_to_{dst}_{tier}.json', {
                        'type': 'minecraft:crafting_shapeless', 'conditions': condition,
                        'ingredients': [{'item': ns + f'machine_{resource}_{src}_{tier}'}, {'item': 'minecraft:redstone_torch'}],
                        'result': {'item': ns + f'machine_{resource}_{dst}_{tier}'}})
            aspects.append({'items': [ns + name], 'aspects': [{'aspect': 'metallum', 'amount': 8}, {'aspect': 'praecantatio', 'amount': 4 + min(tier, 11)}]})

name = 'machine_vis_bridge'
block_ids.append(ns + name)
names['block.thaumicomniverse.' + name] = ('魔力网络桥', 'Vis Network Bridge')
# Bridge keeps the tier-six shell; each selected absorption face lights up using the authored vis face.
bridge_on = Image.open(assets / 'textures/block/machine_vis_output_6_on.png').convert('RGBA')
bridge_off = Image.open(assets / 'textures/block/machine_vis_output_6_off.png').convert('RGBA')
save_texture(name + '_on', bridge_on)
save_texture(name + '_off', bridge_off)
parts = []
for face in faces:
    for active in (False, True):
        model = name + '_' + face + ('_on' if active else '_off')
        texture = ns + 'block/' + name + ('_on' if active else '_off')
        write(assets / 'models/block' / (model + '.json'), {'textures': {'face': texture, 'particle': texture},
            'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {face: {'texture': '#face', 'cullface': face}}}]})
        parts.append({'when': {face: str(active).lower()}, 'apply': {'model': ns + 'block/' + model}})
write(assets / 'blockstates' / (name + '.json'), {'multipart': parts})
write(assets / 'models/item' / (name + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': ns + 'block/' + name + '_on'}})
write(data / 'recipes/collab/machinery/vis_bridge.json', {'type': 'minecraft:crafting_shaped', 'conditions': condition,
    'pattern': ['TRT', 'ROR', 'TRT'], 'key': {'T': {'item': 'thaumcraft:thaumium_ingot'},
    'R': {'item': 'thaumcraft:vis_relay'}, 'O': {'item': ns + 'machine_vis_output_1'}}, 'result': {'item': ns + name}})
aspects.append({'items': [ns + name], 'aspects': [{'aspect': 'praecantatio', 'amount': 16}, {'aspect': 'auram', 'amount': 8}]})
for identifier in block_ids:
    path = identifier.split(':')[1]
    write(data / 'loot_tables/blocks' / (path + '.json'), {'type': 'minecraft:block', 'pools': [{'rolls': 1,
        'entries': [{'type': 'minecraft:item', 'name': identifier}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
pickaxe = root / 'data/minecraft/tags/blocks/mineable/pickaxe.json'
values = json.loads(pickaxe.read_text(encoding='utf-8')).get('values', []) if pickaxe.exists() else []
existing = {value if isinstance(value, str) else value['id'] for value in values}
values.extend({'id': identifier, 'required': False} for identifier in block_ids if identifier not in existing)
write(pickaxe, {'replace': False, 'values': values})
write(data / 'item_aspects/collab_machine_ports.json', {'format': 1, 'entries': aspects})

names.update({
    'omniverse.machine.side': ('%s：%s', '%s: %s'), 'omniverse.machine.on': ('开启', 'On'),
    'omniverse.machine.off': ('关闭', 'Off'), 'omniverse.machine.empty': ('空', 'Empty'),
    'omniverse.machine.essentia_units': ('每种要素独立容量 · 源质', 'Per-aspect capacity · Essentia'),
    'omniverse.machine.vis_units': ('每种要素独立容量 · 100 单位 = 1 vis', 'Per-aspect capacity · 100 units = 1 vis'),
    'omniverse.machine.requirement': ('%s %s %s · %s', '%s %s %s · %s'),
    'omniverse.machine.centivis': ('百分之一 vis', 'centivis'), 'omniverse.machine.essentia': ('源质', 'essentia'),
    'omniverse.machine.input': ('输入', 'Input'), 'omniverse.machine.output': ('输出', 'Output'),
    'omniverse.machine.each_tick': ('每 tick', 'Every tick'), 'omniverse.machine.chance': ('应用概率：%s%%', 'Application chance: %s%%'),
    'omniverse.machine.unaffected': ('数量不随并行数变化', 'Amount unaffected by parallelism')})
for face, zh in zip(faces, ['下', '上', '北', '南', '西', '东']): names['omniverse.machine.face.' + face] = (zh, face.title())
research = ns + 'MACHINE_ASPECT_PORTS'
requires = ['modular_machinery_reborn']
guide = [
    ('源质与 vis 端口必须写入模块化机器的结构定义，且位于成功匹配的结构中。输入端口供应配方，输出端口接收产物。源质按整数单位计量；vis 按百分之一点计量，100 单位等于 1 vis。两种资源不能互相转换。',
     'Add aspect ports to the machine pattern definition. Only ports inside the matched structure participate. Input ports supply recipes; output ports receive products. Essentia uses whole units. Vis uses hundredths of a point: 100 units equal 1 vis. The two resources are distinct.'),
    ('每种要素独立存储。一级端口容量为 64 源质或 100 vis 单位，此后每级翻倍，十一级达到 65536 源质或 102400 vis 单位。十二至十六级保持此上限。右键打开存量与六面设置；潜行右键切换点击面。拆除会丢失内部资源，请先排空。',
     'Capacity is independent for each aspect. Tier 1 holds 64 essentia or 100 vis units. Each tier doubles capacity through tier 11: 65536 essentia or 102400 vis units. Tiers 12 through 16 retain that limit. Right-click for contents and side controls; sneak-right-click toggles a face. Breaking a port loses its stored resources: empty it first.'),
    ('源质输入端口吸力为 128，每 10 tick 从每个开启面抽取最多 1 源质。vis 输入端口每 10 tick 从网络为每种元始要素抽取最多 10 单位。网络桥从所选相邻 vis 输出端口每 10 tick 吸取每种要素最多 1000 单位，自身每种容量 10000，向 8 格内的 vis 网络供能。桥的吸收面最初关闭。',
     'Essentia inputs have suction 128 and pull up to one unit per enabled face every 10 ticks. Vis inputs draw up to 10 units of each primal from the network every 10 ticks. A bridge draws up to 1000 units per primal every 10 ticks from its selected adjacent vis output. It holds 10000 per primal and supplies the vis network within 8 blocks. Its absorption face starts disabled.'),
    ('本扩展为端口提供新的合成配方：一级使用神秘锭、普通机壳、玻璃与要素罐或 vis 中继器；升级使用前一级端口和神秘锭，红石火把可切换输入输出类型。配方查看器显示要素需求。整合包可通过 thaumicomniverse:essentia 和 thaumicomniverse:vis 需求定义机器配方，详见随附接口文档。',
     'This port supplies new crafting recipes using thaumium, plain casings, glass and an essentia jar or vis relay. Upgrade with earlier ports and thaumium; a redstone torch switches input/output variants. The recipe viewer lists aspect requirements. Pack authors can use thaumicomniverse:essentia and thaumicomniverse:vis requirement types; see the included interface documentation.')]
names['tc.research_name.' + research] = ('神秘机器接口', 'Thaumic Machine Interfaces')
names['tc.research_text.' + research] = ('让机器使用要素', 'Aspects for machinery')
for index, pair in enumerate(guide): names['omniverse.page.machine_ports.' + str(index)] = pair
write(data / 'research_catalog/machinery.json', {'format': 1, 'categories': [{
    'key': 'THAUMICOMNIVERSE', 'icon': ns + 'textures/gui/research_category.png',
    'background': 'thaumcraft:textures/gui/gui_researchback.png', 'research': [{
        'key': research, 'category': 'THAUMICOMNIVERSE', 'displayColumn': 0, 'displayRow': 36,
        'complexity': 1, 'isSecondary': True, 'parents': ['VISPOWER'], 'pageCount': len(guide),
        'aspects': {'machina': 2, 'praecantatio': 2}, 'omniverse_requires': requires,
        'icon_item': {'item': ns + 'machine_vis_bridge'}}]}]})
write(data / 'research_pages/machinery.json', {'format': 1, 'entries': [{
    'key': research, 'omniverse_requires': requires,
    'pages': [{'type': 'TEXT', 'text': 'omniverse.page.machine_ports.' + str(index)} for index in range(len(guide))]}]})
for index, lang in enumerate(('zh_cn', 'en_us')):
    path = assets / 'lang' / (lang + '.json')
    translations = json.loads(path.read_text(encoding='utf-8'))
    translations.update({key: pair[index] for key, pair in names.items()})
    write(path, translations)
print('Generated65machinery blocks, face models, authored recipes, loot and translations')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
