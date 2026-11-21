"""Generate our JSON models/recipes and a manifest pointing into the user-supplied legacy jar.
No legacy image, sound, language or model is copied into the distribution.
"""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
MOD = 'thaumicenergistics'
def write(path, value):
    # Complete aspect definitions are maintained directly since TC4 0.4.56.
    if ('/item_aspects/' in path or '/item_bonus_aspects/' in path) and (RES / 'data/thaumicenergistics/item_aspects/explicit.json').exists(): return
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')

mapping = {}
en, zh = {}, {}
def texture(path):
    target = f'assets/{MOD}/textures/{path}.png'
    mapping[target] = target
    return f'{MOD}:{path}'
def page_item(value):
    if isinstance(value,list):return {'kind':'ore','names':[],'elements':[page_item(v) for v in value]}
    return {'kind':'stack','stack':value}
def item(name, tex, english, chinese):
    write(f'assets/{MOD}/models/item/{name}.json', {'parent':'minecraft:item/generated','textures':{'layer0':tex}})
    en[f'item.{MOD}.{name}'], zh[f'item.{MOD}.{name}'] = english, chinese
def block(name, tex, english, chinese):
    write(f'assets/{MOD}/blockstates/{name}.json', {'variants':{'':{'model':f'{MOD}:block/{name}'}}})
    write(f'assets/{MOD}/models/block/{name}.json', {'parent':'minecraft:block/cube_all','textures':{'all':tex}})
    write(f'assets/{MOD}/models/item/{name}.json', {'parent':f'{MOD}:block/{name}'})
    en[f'block.{MOD}.{name}'], zh[f'block.{MOD}.{name}'] = english, chinese
    write(f'data/{MOD}/loot_tables/blocks/{name}.json', {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
def shaped(name, pattern, keys, result, count=1):
    write(f'data/{MOD}/recipes/{name}.json', {'type':'minecraft:crafting_shaped','pattern':pattern,'key':{key:{'item':val} for key,val in keys.items()},'result':{'item':result,'count':count}})
tiers = ['1k','4k','16k','64k','256k','1024k','4096k','16384k','quantum','singularity','creative']
for tier in tiers:
    source = tier if tier in ['1k','4k','16k','64k','creative'] else '64k'
    item(f'essentia_cell_{tier}', texture(f'items/essentia.cell.{source}'), f'{tier} Essentia Storage Cell', f'{tier} 源质存储元件')
    if tier in ['creative','quantum','singularity']: continue
    item(f'storage_component_{tier}', texture(f'items/storage.component.{source}'), f'{tier} Essentia Storage Component', f'{tier} 源质存储组件')
    write(f'data/{MOD}/recipes/essentia_cell_{tier}.json', {'type':'minecraft:crafting_shapeless','ingredients':[{'item':f'{MOD}:storage_casing'},{'item':f'{MOD}:storage_component_{tier}'}],'result':{'item':f'{MOD}:essentia_cell_{tier}'}})
for name, old, english, chinese in [
    ('storage_casing','essentia.cell.casing','Essentia Cell Housing','源质元件外壳'),
    ('diffusion_core','material.diffusion.core','Diffusion Core','扩散核心'),
    ('coalescence_core','material.coalescence.core','Coalescence Core','凝聚核心'),
    ('knowledge_core','knowledge.core','Knowledge Core','知识核心')]:
    item(name, texture(f'items/{old}'), english, chinese)
item('arcane_pattern','ae2:item/blank_pattern','Arcane Pattern','奥术样板')
item('cell_microscope','minecraft:item/spyglass','Cell Microscope','存储元件显微镜')
item('essentia_ampoule','minecraft:item/glass_bottle','Essentia Ampoule','源质安瓿')
item('iron_gear',texture('items/material.iron.gear'),'Iron Gear','铁齿轮')
item('wrench_focus',texture('items/foci.aewrench'),'Wand Focus: AE Wrench','法杖核心：AE 扳手')
item('golem_wireless_backpack',texture('items/golem.wifi.backpack'),'Golem Wireless Backpack','傀儡无线背包')
for skin in ['Clay','Diamond','Flesh','Gold','Iron','Stone','Straw','Tallow','Thaum','Wood']:
    mapping[f'assets/{MOD}/textures/models/backpack_{skin.lower()}.png']=f'assets/{MOD}/textures/models/golemBackpack/{skin}.png'
for name, old, english, chinese in [
    ('essentia_provider','essentia.provider','Essentia Provider','源质供应器'),
    ('infusion_provider','infusion.provider','Infusion Provider','注魔供应器'),
    ('advanced_infusion_provider','infusion.provider','Advanced Infusion Provider','高级注魔供应器'),
    ('essentia_vibration_chamber','e.vibration.face.off','Essentia Vibration Chamber','源质振动室'),
    ('vis_interface','parts/vis.interface','Vis Interface','灵气接口'),
    ('creative_vis_interface','parts/vis.interface','Creative Vis Interface','创造灵气接口'),
    ('knowledge_inscriber','knowledge.inscriber.top','Knowledge Inscriber','知识刻写器'),
    ('iron_gearbox','gear.box.fallback','Iron Gearbox','铁齿轮箱'),
    ('thaumium_gearbox','golem.gear.box.fallback','Thaumium Gearbox','神秘齿轮箱'),
    ('arcane_terminal','parts/arcane.crafting.side','Arcane Crafting Terminal','奥术合成终端'),
    ('arcane_assembler','arcane.assembler.fallback','Arcane Assembler','奥术装配器'),
    ('distillation_encoder','distillation.encoder.face','Distillation Pattern Encoder','蒸馏样板编码器')]:
    block(name, texture(f'blocks/{old}'), english, chinese)

shaped('essentia_ampoule',[' G ','G G',' G '],{'G':'minecraft:glass'},f'{MOD}:essentia_ampoule')
write('data/minecraft/tags/blocks/mineable/pickaxe.json', {'replace':False, 'values':[
    f'{MOD}:{name}' for name in ['essentia_provider','infusion_provider','essentia_vibration_chamber',
    'vis_interface','creative_vis_interface','knowledge_inscriber','iron_gearbox','thaumium_gearbox',
    'arcane_terminal','arcane_assembler']]})
shaped('storage_casing',['GIG','I I','III'],{'G':'minecraft:glass','I':'thaumcraft:thaumium_ingot'},f'{MOD}:storage_casing')
shaped('diffusion_core',[' Q ',' C ',' R '],{'Q':'ae2:certus_quartz_crystal','C':'ae2:annihilation_core','R':'minecraft:redstone'},f'{MOD}:diffusion_core')
shaped('coalescence_core',[' Q ',' C ',' R '],{'Q':'ae2:certus_quartz_crystal','C':'ae2:formation_core','R':'minecraft:redstone'},f'{MOD}:coalescence_core')
for i,tier in enumerate(tiers[:8]):
    previous = 'ae2:logic_processor' if i == 0 else f'{MOD}:storage_component_{tiers[i-1]}'
    shaped(f'storage_component_{tier}',['RQR','QCQ','RQR'],{'R':'minecraft:redstone','Q':previous,'C':f'{MOD}:coalescence_core'},f'{MOD}:storage_component_{tier}')
for name, center in {'essentia_provider':'ae2:interface','infusion_provider':'thaumicenergistics:essentia_provider',
 'advanced_infusion_provider':'thaumicenergistics:infusion_provider','essentia_vibration_chamber':'ae2:vibration_chamber',
 'vis_interface':'ae2:fluix_crystal','knowledge_inscriber':'minecraft:enchanting_table',
 'iron_gearbox':'minecraft:iron_block','thaumium_gearbox':'thaumicenergistics:iron_gearbox',
 'arcane_terminal':'ae2:crafting_terminal','arcane_assembler':'ae2:molecular_assembler','distillation_encoder':'ae2:pattern_encoding_terminal'}.items():
    shaped(name,['ITI','CFC','ITI'],{'I':'thaumcraft:thaumium_ingot','T':'ae2:fluix_crystal','C':f'{MOD}:coalescence_core','F':center},f'{MOD}:{name}')
shaped('iron_gear',[' I ','INI',' I '],{'I':'minecraft:iron_ingot','N':'minecraft:iron_nugget'},f'{MOD}:iron_gear')
shaped('knowledge_core',['QGQ','GPG','QGQ'],{'Q':'ae2:certus_quartz_crystal','G':'minecraft:gold_ingot','P':'ae2:engineering_processor'},f'{MOD}:knowledge_core')
shaped('cell_microscope',[' Q ',' I ',' G '],{'Q':'ae2:certus_quartz_crystal','I':'minecraft:iron_ingot','G':'minecraft:glass'},f'{MOD}:cell_microscope')
shaped('wrench_focus',[' Q ','WCW',' Q '],{'Q':'ae2:certus_quartz_crystal','W':'ae2:certus_quartz_wrench','C':f'{MOD}:coalescence_core'},f'{MOD}:wrench_focus')
shaped('golem_wireless_backpack',['ILI','WCW','ILI'],{'I':'thaumcraft:thaumium_ingot','L':'minecraft:leather','W':'ae2:wireless_receiver','C':f'{MOD}:coalescence_core'},f'{MOD}:golem_wireless_backpack')

# Modern progression definitions remain editable JSON and preserve research-gated arcane crafting.
chapters=[
 ('START','Diffusion and Coalescence','扩散与凝聚','diffusion_core',None,
  'AE networks can store essentia as a native resource. Ordinary terminals, buses, monitors and level emitters support it. No separate essentia buses are needed.',
  'ME 网络将源质作为独立资源存储。普通终端、总线、存储监视器和电平发射器均可操作源质，不需要专用源质总线。'),
 ('STORAGE','Essentia Storage','源质存储','essentia_cell_1k','START',
  'Install essentia cells in ME drives. Configure partitions in the AE Cell Workbench. Use phials, jars or an ampoule at a terminal to transfer essentia. The microscope inspects a cell held in your other hand. An empty cell can be disassembled by sneaking and using it.',
  '将源质元件装入 ME 驱动器，在 AE 元件工作台设置分区。小瓶、罐子和安瓿可在终端中存取源质；显微镜检查另一只手中的存储元件。空元件可潜行右键拆分。'),
 ('AUTOMATION','Essentia Automation','源质自动化','infusion_provider','STORAGE',
  'Connect providers to a powered network. Essentia providers feed tubes; infusion providers supply nearby infusion consumers. The advanced provider requests missing essentia through AE crafting CPUs. Encode a distillation pattern using one input sample and a blank pattern, then install it in a pattern provider serving an alchemical furnace. Import the resulting essentia using ordinary import buses.',
  '供应器需要已供电的网络。源质供应器向管道供源；注魔供应器为附近注魔消费者供源；高级供应器可通过 AE 合成 CPU 请求缺失源质。在蒸馏编码器中放入输入样品和空白样板，编码后将样板装入连接炼金炉的样板供应器，再用普通输入总线收集源质。'),
 ('VIS','Vis and Power','灵气与能源','vis_interface','STORAGE',
  'Sneak-use toggles input/output. An output interface supplies nearby vis relays from the same ME network. A vis interface collects centivis from an energized node network and supplies arcane machines on the same ME grid. The creative interface provides unlimited primal vis. Vibration chambers burn Ignis or Potentia for AE power. Iron gearboxes turn adjacent crankable machines; thaumium gearboxes also accept use-core golems.',
  '潜行右键切换输入/输出。输出接口将同一 ME 网络的灵气提供给附近中继。灵气接口从充能节点网络取得灵气，为同一 ME 网络中的奥术设备供能；创造接口无限提供元始灵气。源质振动室燃烧火或能量源质产生 AE 能量。铁齿轮箱驱动相邻可转动机器；神秘齿轮箱还允许使用核心的傀儡操作。'),
 ('ARCANE','Arcane Automation','奥术自动化','arcane_assembler','VIS',
  'Put a 3x3 recipe in the terminal. A wand or network vis pays its cost. Refill replenishes non-empty ingredient slots from ME. Use a knowledge core on the inscriber to record your research. Install it in an assembler. Encode an arcane pattern in the terminal and put it into an adjacent AE pattern provider; the assembler validates research, inputs and vis before crafting. Finished goods return to its ME grid. Four armor slots reduce vis cost; up to four acceleration cards speed crafting at additional AE cost.',
  '在终端放入 3×3 配方，使用法杖或网络灵气支付消耗；补充按钮从 ME 补足非空材料格。手持知识核心右键刻写器记录研究，将其装入装配器。在终端编码奥术样板，装入相邻 AE 样板供应器；装配器验证研究、材料和灵气后执行合成，成品返回其 ME 网络。四个护甲位提供灵气减免，最多四张加速卡提高速度并增加每刻 AE 消耗。'),
 ('GOLEMS','Wireless Golems','无线傀儡','golem_wireless_backpack','AUTOMATION',
  'Link a backpack in an AE wireless access point, then equip a Gather, Fill, Liquid or Essentia golem. It still requires wireless coverage and network power. Gather stores collected items; Fill and Liquid draw from ME to supply home. Essentia golems import from a transport home or fill marked labeled jars from ME when placed on ordinary blocks. Sneak with an empty hand to remove the backpack. AE facades customize its appearance.',
  '在 AE 无线接入点绑定背包后，给收集、填充、流体或源质傀儡装备。工作需要无线覆盖和网络供电。收集核心将拾取物存入 ME；填充和流体核心从 ME 取货补充家中容器。源质核心以源质运输方块为家时向 ME 存储，否则从 ME 填充标记的带标签罐子。空手潜行右键取回背包；可使用 AE 立面改变背包外观。'),
 ('TOOLS','Network Tools','网络工具','wrench_focus','START',
  'The AE wrench focus invokes the real AE wrench while attached to a wand. Native AE facades can cover compatible Thaumcraft blocks. Connect each network machine with AE cable; channels and power apply normally.',
  'AE 扳手核心装在法杖上后调用 AE 扳手交互。原生 AE 立面支持适用的神秘时代方块。使用 AE 线缆连接网络设备，并提供所需的频道和电力。'),
 ('QUARTZ','Quartz Duplication','石英复制','diffusion_core','STORAGE',
  'Duplicate Certus Quartz in a crucible with four Vitreus and two Aqua. Nether Quartz also needs two Potentia.',
  '在坩埚中以四点水晶、两点水要素复制赛特斯石英；复制下界石英额外需要两点能量要素。')]
research_for={}
for name in ['diffusion_core','coalescence_core']:research_for[name]='START'
for tier in tiers[:8]:research_for[f'storage_component_{tier}']='STORAGE'
for name in ['essentia_provider','infusion_provider','advanced_infusion_provider','distillation_encoder']:research_for[name]='AUTOMATION'
for name in ['vis_interface','essentia_vibration_chamber','iron_gearbox','thaumium_gearbox']:research_for[name]='VIS'
for name in ['knowledge_core','knowledge_inscriber','arcane_assembler','arcane_terminal']:research_for[name]='ARCANE'
research_for.update(wrench_focus='TOOLS',golem_wireless_backpack='GOLEMS',iron_gear='VIS',cell_microscope='TOOLS')
for name, chapter in research_for.items():
    path=RES/f'data/{MOD}/recipes/{name}.json'
    recipe=json.loads(path.read_text(encoding='utf8'))
    recipe.update(type='thaumcraft:arcane',mode='shaped',research=f'{MOD}:{chapter}',vis={'ordo':3,'ignis':3})
    write(f'data/{MOD}/recipes/{name}.json',recipe)
for name,result,aspects in [('certus','ae2:certus_quartz_crystal',{'vitreus':4,'aqua':2}),('nether','minecraft:quartz',{'vitreus':4,'aqua':2,'potentia':2})]:
    write(f'data/{MOD}/recipes/duplicate_{name}.json',{'type':'thaumcraft:crucible','research':f'{MOD}:QUARTZ','catalyst':{'item':result},'aspects':aspects,'result':{'item':result,'count':2}})
from legacy_recipes import apply
apply(write,MOD,research_for)
component_path=RES/f'data/{MOD}/recipes/storage_component_1k.json'
component=json.loads(component_path.read_text(encoding='utf8'))
component['key']['Q']=[{'item':'ae2:certus_quartz_crystal'},{'item':'ae2:charged_certus_quartz_crystal'}]
write(f'data/{MOD}/recipes/storage_component_1k.json',component)
write('data/forge/tags/items/gears/iron.json',{'replace':False,'values':[f'{MOD}:iron_gear']})
write('data/forge/tags/items/gears.json',{'replace':False,'values':['#forge:gears/iron']})
from research_progression import expand
chapters,research_for,research_metadata=expand(RES,MOD,chapters,research_for,write)
nodes,pages,display_recipes=[],[],[]
for index,(key,english,chinese,icon,parent,text_en,text_zh) in enumerate(chapters):
    full=f'{MOD}:{key}'
    entry_pages=[{'type':'TEXT','text':f'{MOD}.page.{key}'}]
    for recipe_name,chapter in research_for.items():
        if chapter!=key:continue
        recipe=json.loads((RES/f'data/{MOD}/recipes/{recipe_name}.json').read_text(encoding='utf8'))
        if recipe['type']=='thaumcraft:arcane':
            ingredients=[]
            if recipe['mode']=='shaped':
                for row in recipe['pattern']:
                    for letter in row:
                        ingredients.append(None if letter==' ' else page_item(recipe['key'][letter]))
                width,height=len(recipe['pattern'][0]),len(recipe['pattern'])
            else:
                ingredients=[{'kind':'stack','stack':i} for i in recipe['ingredients']]
                width,height=3,(len(ingredients)+2)//3
                ingredients += [None]*(width*height-len(ingredients))
            entry_pages.append({'type':'ARCANE_CRAFTING','recipe':{'kind':'inline_arcane','research':full,'width':width,'height':height,'mirrored':True,'output':recipe['result'],'ingredients':{'kind':'sequence','elements':ingredients},'aspects':recipe['vis']}})
        else:
            kind=recipe['type'].split(':')[1]
            reference=f'{MOD}:{recipe_name}'
            cls='thaumcraft.api.crafting.InfusionRecipe' if kind=='infusion' else 'thaumcraft.api.crafting.CrucibleRecipe'
            stack=lambda v:{'kind':'stack','stack':v}
            data={'kind':kind,'research':full,'output':stack(recipe['result']),'aspects':recipe['aspects']}
            if kind=='infusion':
                data.update(input=stack(recipe['center']['ingredient']),ingredients={'kind':'sequence','elements':[stack(dict(c['ingredient'],**({'nbt':c['nbt']} if 'nbt' in c else {}))) for c in recipe['components']]},instability=recipe['instability'])
            else:data['catalyst']=stack(recipe['catalyst'])
            display_recipes.append({'key':reference,'class':cls,'data':data})
            entry_pages.append({'type':'INFUSION_CRAFTING' if kind=='infusion' else 'CRUCIBLE_CRAFTING','recipe':{'kind':'reference','keys':[reference],'class':cls},'recipeOutput':recipe['result']})
    for path in sorted((RES/f'data/{MOD}/recipes').glob('*.json')):
        recipe=json.loads(path.read_text(encoding='utf8'))
        recipe_name=path.stem
        if not (key=='STORAGE' and recipe['type'].startswith('minecraft:crafting') and (recipe_name.startswith('essentia_cell_') or recipe_name=='storage_casing') or key=='QUARTZ' and recipe_name.startswith('duplicate_')):continue
        cls='thaumcraft.api.crafting.CrucibleRecipe' if key=='QUARTZ' else 'net.minecraft.world.item.crafting.Recipe'
        reference=f'{MOD}:{recipe_name}'
        stack=lambda v:{'kind':'stack','stack':v}
        data={'kind':'crucible' if key=='QUARTZ' else 'crafting_shaped' if 'pattern' in recipe else 'crafting_shapeless','research':full,'output':stack(recipe['result'])}
        if key=='QUARTZ':data.update(catalyst=stack(recipe['catalyst']),aspects=recipe['aspects'])
        else:
            if 'pattern' in recipe:
                parts=[None if letter==' ' else stack(recipe['key'][letter]) for row in recipe['pattern'] for letter in row];data.update(width=len(recipe['pattern'][0]),height=len(recipe['pattern']))
            else:parts=[stack(ingredient) for ingredient in recipe['ingredients']]
            data['ingredients']={'kind':'sequence','elements':parts}
        display_recipes.append({'key':reference,'class':cls,'data':data})
        entry_pages.append({'type':'CRUCIBLE_CRAFTING' if key=='QUARTZ' else 'NORMAL_CRAFTING','recipe':{'kind':'reference','keys':[reference],'class':cls},'recipeOutput':recipe['result']})
    for page in entry_pages:
        output=page.get('recipeOutput',page.get('recipe',{}).get('output',{})).get('item','')
        if output.rsplit('_',1)[-1] == '256k':
            page['conditions']=[{'type':f'{MOD}:feature_enabled','feature':'cells_256k'}]
        if output.rsplit('_',1)[-1] in ('1024k','4096k','16384k','quantum','singularity'):
            page['conditions']=[{'type':f'{MOD}:feature_enabled','feature':'extended_cells'}]
    node={'key':full,'category':'THAUMICENERGISTICS','displayColumn':(index%3)*3,'displayRow':(index//3)*3,'pageCount':len(entry_pages),'icon_item':{'item':f'{MOD}:{icon}'},'complexity':1}
    if parent:node.update(parents=[f'{MOD}:{parent}'],isSecondary=True,aspects={'cognitio':2,'ordo':2})
    else:node['isAutoUnlock']=True
    node.update(research_metadata[key])
    nodes.append(node);pages.append({'key':full,'pages':entry_pages})
    en[f'tc.research_name.{full}']=english;zh[f'tc.research_name.{full}']=chinese
    en[f'tc.research_text.{full}']=english;zh[f'tc.research_text.{full}']=chinese
    en[f'{MOD}.page.{key}']=text_en;zh[f'{MOD}.page.{key}']=text_zh
en.update({'gui.thaumicenergistics.vis_input':'Vis interface: input from node network','gui.thaumicenergistics.vis_output':'Vis interface: output to node network'})
zh.update({'gui.thaumicenergistics.vis_input':'灵气接口：从节点网络输入','gui.thaumicenergistics.vis_output':'灵气接口：向节点网络输出'})
en['tc.research_category.THAUMICENERGISTICS']='Thaumic Energistics';zh['tc.research_category.THAUMICENERGISTICS']='神秘能源'
write(f'data/{MOD}/research_catalog/main.json',{'format':1,'categories':[{'key':'THAUMICENERGISTICS','icon':texture('research/tab.icon')+'.png','background':texture('research/Research.Background')+'.png','research':nodes}]})
# Resource locations in the book use complete texture paths, not model texture references.
catalog_path=RES/f'data/{MOD}/research_catalog/main.json'
catalog=json.loads(catalog_path.read_text(encoding='utf8'))
catalog['categories'][0]['icon']=f'{MOD}:textures/research/tab.icon.png'
mapping[f'assets/{MOD}/textures/research/background.png']=mapping.pop(f'assets/{MOD}/textures/research/Research.Background.png')
catalog['categories'][0]['background']=f'{MOD}:textures/research/background.png'
write(f'data/{MOD}/research_catalog/main.json',catalog)
write(f'data/{MOD}/research_pages/main.json',{'format':1,'entries':pages})
write(f'data/{MOD}/research_page_recipes/main.json',{'format':1,'entries':display_recipes})
write(f'data/{MOD}/item_aspects/main.json',{'format':1,'entries':[{'items':[f'{MOD}:{name}'],'aspects':[{'aspect':'machina','amount':4},{'aspect':'metallum','amount':2}]} for name in research_for]})

en.update({'gui.thaumicenergistics.essentia':'Essentia','gui.thaumicenergistics.container':'Essentia Container',
 'gui.thaumicenergistics.linked':'Linked to a wireless access point','gui.thaumicenergistics.unlinked':'Not linked',
 'gui.thaumicenergistics.backpack_help':'Equip on a compatible golem; sneak empty-handed to remove.',
 'gui.thaumicenergistics.craft':'Craft','gui.thaumicenergistics.encode_arcane':'Arcane','gui.thaumicenergistics.encode_distillation':'Distill','gui.thaumicenergistics.refill':'Refill',
 'gui.thaumicenergistics.action_failed':'Check ingredients, research, power, vis and output space.',
 'gui.thaumicenergistics.wand_short':'Wand','gui.thaumicenergistics.core_short':'Core','gui.thaumicenergistics.pattern_short':'Blank',
 'gui.thaumicenergistics.knowledge_count':'Recorded research: %s','gui.thaumicenergistics.inscribed':'Your research has been recorded in the core.',
 'gui.thaumicenergistics.cell_capacity':'Stored: %s / %s essentia','gui.thaumicenergistics.cell_types':'Types: %s / %s',
 'gui.thaumicenergistics.creative_cell':'Infinite supply; configure aspects in a Cell Workbench.',
 'gui.thaumicenergistics.machine_status':'%s | active: %s | fuel: %s | vis: %s centivis | energy: %s AE'})
zh.update({'gui.thaumicenergistics.essentia':'源质','gui.thaumicenergistics.container':'源质容器',
 'gui.thaumicenergistics.linked':'已绑定无线接入点','gui.thaumicenergistics.unlinked':'尚未绑定',
 'gui.thaumicenergistics.backpack_help':'装备到相应傀儡上；空手潜行右键取下。',
 'gui.thaumicenergistics.craft':'合成','gui.thaumicenergistics.encode_arcane':'奥术编码','gui.thaumicenergistics.encode_distillation':'蒸馏编码','gui.thaumicenergistics.refill':'补充',
 'gui.thaumicenergistics.action_failed':'请检查材料、研究、电力、灵气及输出空间。',
 'gui.thaumicenergistics.wand_short':'法杖','gui.thaumicenergistics.core_short':'核心','gui.thaumicenergistics.pattern_short':'样板',
 'gui.thaumicenergistics.knowledge_count':'已记录研究：%s','gui.thaumicenergistics.inscribed':'已将你的研究记录到知识核心。',
 'gui.thaumicenergistics.cell_capacity':'已存储：%s / %s 源质','gui.thaumicenergistics.cell_types':'类型：%s / %s',
 'gui.thaumicenergistics.creative_cell':'无限供应；可在元件工作台选择要素。',
 'gui.thaumicenergistics.machine_status':'%s | 已激活：%s | 燃料：%s | 灵气：%s 厘点 | 能量：%s AE'})
write(f'assets/{MOD}/lang/en_us.json',en)
write(f'assets/{MOD}/lang/zh_cn.json',zh)
write(f'data/{MOD}/thaumcraft/original_resources.json',{
 'name':'Thaumic Energistics 1.1.3.0 (MC 1.7.10)', 'legacy_modid':MOD,
 'sha256':'5657c17a6b8b6ad77169cbf7496d40b6ade7a846759278db162dfc61ee42a58f',
 'download':'https://www.curseforge.com/minecraft/mc-mods/thaumic-energistics/files/2563527',
 'mapping':mapping})

# Extra UI text for native terminal / pattern core / point-to-point vis links.
en.update({'gui.thaumicenergistics.pattern_count':'Stored recipes: %s / 21','gui.thaumicenergistics.inscribe_pattern':'Save / remove recipe','gui.thaumicenergistics.vis_link':'Vis interface source'})
zh.update({'gui.thaumicenergistics.pattern_count':'已记录配方：%s / 21','gui.thaumicenergistics.inscribe_pattern':'保存 / 移除配方','gui.thaumicenergistics.vis_link':'灵气接口源'})
write(f'assets/{MOD}/lang/en_us.json',en)
write(f'assets/{MOD}/lang/zh_cn.json',zh)

from machine_models import apply as machine_models
machine_models(write,texture,MOD,mapping)
manifest=json.loads((RES/f'data/{MOD}/thaumcraft/original_resources.json').read_text(encoding='utf8'))
manifest['mapping']=mapping
write(f'data/{MOD}/thaumcraft/original_resources.json',manifest)

from aspect_data import apply as aspect_data
aspect_data(write,MOD)

from derived_aspects import apply as derived_aspects
derived_aspects(RES,MOD,write)

# Vibration chamber uses a native, read-only menu.
for locale,extra in {"en_us":{"fuel":"Stored fuel: %s / 64", "remaining":"Remaining energy: %s AE", "generation":"%s AE/t  |  %s%% load"},"zh_cn":{"fuel":"储存燃料：%s / 64", "remaining":"剩余能量：%s AE", "generation":"%s AE/t  |  %s%% 负载"}}.items():
    path=RES/f"assets/{MOD}/lang/{locale}.json"
    data=json.loads(path.read_text(encoding="utf8"))
    data.update({f"gui.{MOD}.{key}":value for key,value in extra.items()})
    write(f"assets/{MOD}/lang/{locale}.json",data)

# The legacy facade whitelist, including glass with a block entity. Fixed recipes use native AE2 facade NBT.
facades=['amber_block','obsidian_tile','thaumium_block','tallow_block','arcane_stone','arcane_stone_bricks','eldritch_stone','eldritch_rock','eldritch_crust','greatwood_planks','silverwood_planks','taint_crust','flesh_block','warded_glass']
write('data/ae2/tags/blocks/whitelisted/facades.json',{'replace':False,'values':['thaumcraft:'+name for name in facades]})
for name in facades:
    write(f'data/{MOD}/recipes/facade_{name}.json',{'type':'minecraft:crafting_shaped','pattern':[' A ','ABA',' A '],'key':{'A':{'item':'ae2:cable_anchor'},'B':{'item':'thaumcraft:'+name}},'result':{'item':'ae2:facade','count':4,'nbt':{'item':'thaumcraft:'+name}}})

# Equivalent crafting switches; native AE buses/wireless terminals retain AE2 configuration.
feature_groups={'arcane_assembler':['arcane_assembler','knowledge_core','knowledge_inscriber'], 'arcane_terminal':['arcane_terminal'], 'distillation_encoder':['distillation_encoder'],'essentia_provider':['essentia_provider'],'vibration_chamber':['essentia_vibration_chamber'],'golem_backpack':['golem_wireless_backpack'],'infusion_provider':['infusion_provider','advanced_infusion_provider'],'vis_interface':['vis_interface'],'gearbox':['iron_gear','iron_gearbox','thaumium_gearbox'],'quartz_duplication':['duplicate_certus','duplicate_nether'],'wrench_focus':['wrench_focus']}
for recipe_file in (RES/f'data/{MOD}/recipes').glob('*.json'):
    name=recipe_file.stem
    feature=next((key for key,names in feature_groups.items() if name in names),None)
    if name.startswith(('storage_component_','essentia_cell_')) or name=='storage_casing':feature='essentia_cells'
    if name.startswith('facade_'):feature='thaumcraft_facades'
    if feature:
        data=json.loads(recipe_file.read_text(encoding='utf8'))
        data['conditions']=[{'type':f'{MOD}:feature_enabled','feature':feature}]
        tier=name.removesuffix('_shaped').rsplit('_',1)[-1]
        added='cells_256k' if tier=='256k' else 'extended_cells' if tier in ('1024k','4096k','16384k','quantum','singularity') else name if name in ('wireless_arcane_terminal','vis_charging_card') else None
        if added:data['conditions'].append({'type':f'{MOD}:feature_enabled','feature':added})
        write(f'data/{MOD}/recipes/{name}.json',data)

# Cable parts retain the same translated names as their legacy block forms.
for locale in ['en_us','zh_cn']:
    path=RES/f'assets/{MOD}/lang/{locale}.json'
    data=json.loads(path.read_text(encoding='utf-8'))
    for name in ['vis_interface','creative_vis_interface','arcane_terminal']:
        data[f'item.{MOD}.{name}']=data[f'block.{MOD}.{name}']
    write(f'assets/{MOD}/lang/{locale}.json',data)

# Keep the reviewed modern replacements after rebuilding the legacy-derived resources.
import report0920
report0920.resources()
report0920.retire_and_translate()

# Final art layer, including the AE2 style document and provenance manifest.
import modern_resources
modern_resources.apply()
import report0921
report0921.apply()

# The legacy knowledge-inscriber layout remains the visual baseline.
path=RES/f'data/{MOD}/thaumcraft/original_resources.json'
manifest=json.loads(path.read_text(encoding='utf8'))
gui=f'assets/{MOD}/textures/gui/knowledge.inscriber.png'
manifest['mapping'][gui]=gui
write(f'data/{MOD}/thaumcraft/original_resources.json',manifest)
for locale,text in [('en_us','Clear crafting grid'),('zh_cn','清空合成网格')]:
 path=RES/f'assets/{MOD}/lang/{locale}.json';lang=json.loads(path.read_text(encoding='utf8'));lang['gui.thaumicenergistics.clear_grid']=text;write(f'assets/{MOD}/lang/{locale}.json',lang)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
