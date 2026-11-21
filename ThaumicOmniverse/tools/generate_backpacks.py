"""Authored backpack upgrade artwork and research; no third-party pixels copied."""
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
NS='thaumicomniverse'
DATA=ROOT/'data'/NS
ASSETS=ROOT/'assets'/NS
def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

item='focus_pouch_upgrade'
image=Image.new('RGBA',(16,16));d=ImageDraw.Draw(image)
# Warm metal upgrade frame, cloth pouch and jewel at native 16px scale.
d.rectangle((1,1,14,14),fill='#34292a',outline='#18171d')
d.rectangle((2,2,13,13),fill='#7a5540',outline='#c88e56')
d.line((3,3,12,3),fill='#f0bb75');d.line((3,12,12,12),fill='#452f2e')
for x,y in [(2,2),(13,2),(2,13),(13,13)]:d.point((x,y),fill='#e8c28d')
d.polygon([(5,5),(10,5),(12,10),(10,12),(5,12),(3,10)],fill='#67417f',outline='#251c37')
d.line((5,5,10,5),fill='#d2b15c');d.line((6,6,6,10),fill='#9465aa')
d.polygon([(8,6),(10,8),(8,10),(6,8)],fill='#66d2d0');d.point((8,7),fill='#d6ffff')
texture=ASSETS/'textures/item'/(item+'.png');texture.parent.mkdir(parents=True,exist_ok=True);image.save(texture)
write(ASSETS/'models/item'/(item+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+item}})
key=NS+':FOCUS_POUCH_UPGRADE'
center='sophisticatedbackpacks:upgrade_base'
components=['thaumcraft:focus_pouch','thaumcraft:thaumium_ingot','minecraft:ender_pearl','thaumcraft:thaumium_ingot']
aspects={'vacuos':16,'permutatio':12,'praecantatio':10}
write(DATA/'recipes/collab'/(item+'.json'),{'type':'thaumcraft:infusion','research':key,'instability':2,
    'center':{'ingredient':{'item':center}},'components':[{'ingredient':{'item':x}} for x in components],
    'aspects':aspects,'result':{'item':NS+':'+item},'conditions':[{'type':'forge:mod_loaded','modid':'sophisticatedbackpacks'}]})
write(DATA/'research_catalog/backpacks.json',{'format':1,'categories':[{'key':'THAUMICOMNIVERSE',
    'icon':NS+':textures/gui/research_category.png','background':'thaumcraft:textures/gui/gui_researchback.png',
    'research':[{'key':key,'category':'THAUMICOMNIVERSE','displayColumn':0,'displayRow':16,'complexity':2,
        'isConcealed':True,'isSecondary':True,'parents':['FOCUSPOUCH'],'pageCount':2,
        'aspects':{'vacuos':8,'permutatio':5,'praecantatio':5},'icon_item':{'item':NS+':'+item},
        'omniverse_requires':['sophisticatedbackpacks']}]}]})
write(DATA/'research_pages/backpacks.json',{'format':1,'entries':[{'key':key,'omniverse_requires':['sophisticatedbackpacks'],
    'pages':[{'type':'TEXT','text':'omniverse.page.focus_pouch_upgrade'},
        {'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[key]}}]}]})
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1}}
write(DATA/'research_page_recipes/backpacks.json',{'format':1,'entries':[{'key':key,
    'class':'thaumcraft.api.crafting.InfusionRecipe','omniverse_requires':['sophisticatedbackpacks'],
    'data':{'kind':'infusion','research':key,'instability':2,'input':stack(center),'output':stack(NS+':'+item),
        'ingredients':{'kind':'sequence','elements':[stack(x) for x in components]},
        'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()]}}]})
texts={
 'item.'+NS+'.'+item:('法杖核心手袋升级','Focus Pouch Upgrade'),
 'item.'+NS+'.'+item+'.tooltip':('从背包切换法杖核心，优先放回原格子。','Switch wand foci from this backpack, preferring their original slots.'),
 'tc.research_name.'+key:('便携核心库','Portable Focus Collection'),
 'tc.research_text.'+key:('熟悉的位置','A place for every focus'),
 'omniverse.page.focus_pouch_upgrade':(
   '装入升级后，随身携带的精妙背包可直接用于法杖核心切换。核心回收时优先返回它原来的格子，若该格不可用则使用其他空格。背包原有磁吸升级也能收取要素球：优先由玩家持有的魔杖吸收，或为背包中的魔杖充能。此功能遵从磁吸升级的经验拾取开关；放置的背包也可为内部魔杖充能。',
   'This upgrade lets a carried backpack supply the wand focus selector. Returning foci prefer their previous slots, falling back to another empty slot when needed. Existing magnet upgrades also collect vis orbs into your hotbar wands or the wands inside the backpack. Their XP pickup toggle controls this feature. Placed backpacks can charge their stored wands too.')}
for i,lang in enumerate(['zh_cn','en_us']):
 p=ASSETS/'lang'/(lang+'.json');data=json.loads(p.read_text(encoding='utf-8'));data.update({k:v[i] for k,v in texts.items()});write(p,data)
print('Generated focus pouch upgrade recipe, research and original artwork')

# Tank resources share the native-style upgrade frame but have an authored glass vessel.
item='essentia_tank_upgrade';key=NS+':ESSENTIA_TANK_UPGRADE'
image=Image.new('RGBA',(16,16));d=ImageDraw.Draw(image)
d.rectangle((1,1,14,14),fill='#34292a',outline='#18171d')
d.rectangle((2,2,13,13),fill='#7a5540',outline='#c88e56')
d.line((3,3,12,3),fill='#f0bb75')
for x,y in [(2,2),(13,2),(2,13),(13,13)]:d.point((x,y),fill='#e8c28d')
d.rectangle((5,4,10,12),fill='#183642',outline='#a9d7dc')
d.rectangle((6,8,9,11),fill='#816ac4');d.line((6,5,6,7),fill='#eeffff')
d.rectangle((5,4,10,5),fill='#d0a14d');d.line((6,4,9,4),fill='#f0d18c')
image.save(ASSETS/'textures/item'/(item+'.png'))
write(ASSETS/'models/item'/(item+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+item}})
components=['thaumcraft:essentia_jar','thaumcraft:essentia_tube','thaumicomniverse:alchemical_brass_ingot','thaumcraft:essentia_tube']
aspects={'vacuos':16,'aqua':12,'praecantatio':8}
write(DATA/'recipes/collab'/(item+'.json'),{'type':'thaumcraft:infusion','research':key,'instability':2,
    'center':{'ingredient':{'item':center}},'components':[{'ingredient':{'item':x}} for x in components],
    'aspects':aspects,'result':{'item':NS+':'+item},'conditions':[{'type':'forge:mod_loaded','modid':'sophisticatedbackpacks'}]})
p=DATA/'research_catalog/backpacks.json';data=json.loads(p.read_text(encoding='utf-8'))
data['categories'][0]['research'].append({'key':key,'category':'THAUMICOMNIVERSE','displayColumn':4,'displayRow':16,
    'complexity':2,'isConcealed':True,'isSecondary':True,'parents':['TUBES'],'pageCount':2,
    'aspects':{'vacuos':8,'aqua':5,'praecantatio':5},'icon_item':{'item':NS+':'+item},'omniverse_requires':['sophisticatedbackpacks']});write(p,data)
p=DATA/'research_pages/backpacks.json';data=json.loads(p.read_text(encoding='utf-8'))
data['entries'].append({'key':key,'omniverse_requires':['sophisticatedbackpacks'],'pages':[
    {'type':'TEXT','text':'omniverse.page.essentia_tank_upgrade'},
    {'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[key]}}]});write(p,data)
p=DATA/'research_page_recipes/backpacks.json';data=json.loads(p.read_text(encoding='utf-8'))
data['entries'].append({'key':key,'class':'thaumcraft.api.crafting.InfusionRecipe','omniverse_requires':['sophisticatedbackpacks'],
    'data':{'kind':'infusion','research':key,'instability':2,'input':stack(center),'output':stack(NS+':'+item),
    'ingredients':{'kind':'sequence','elements':[stack(x) for x in components]},
    'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()]}});write(p,data)
texts={
 'item.'+NS+'.'+item:('源质储罐升级','Essentia Tank Upgrade'),
 'item.'+NS+'.'+item+'.tooltip':('储存 64 点单一种类的源质，支持六面管道连接。','Stores 64 essentia of one aspect. Connect tubes on any face.'),
 'tc.research_name.'+key:('背包源质储罐','Backpack Essentia Tank'),
 'tc.research_text.'+key:('将罐子随身携带','A reservoir to carry'),
 'omniverse.page.essentia_tank_upgrade':(
  '源质储罐可存放 64 点单一种类的源质。放置背包后，任何一面都能连接源质管道，吸力与未贴标签的要素罐相同，满罐时停止吸取。便捷管道和源质瓶也可直接操作背包。升级拆下时保留内容；禁用升级会停止转移。打开储罐分页可查看当前要素和容量。',
  'The tank holds 64 essentia of one aspect. A placed backpack connects on all six faces with the suction of an unlabelled jar, stopping intake when full. Convenient tubes and phials also work. Contents stay in the upgrade when removed; disabling it stops transfers. Open its tab to see the current aspect and amount.')}
for i,lang in enumerate(['zh_cn','en_us']):
 p=ASSETS/'lang'/(lang+'.json');data=json.loads(p.read_text(encoding='utf-8'));data.update({k:v[i] for k,v in texts.items()});write(p,data)
print('Generated essentia tank upgrade recipe, research and original artwork')

# Pump: brass upgrade frame, opposed pipes and a violet essentia chamber.
item='essentia_pump_upgrade';key=NS+':ESSENTIA_PUMP_UPGRADE'
image=Image.new('RGBA',(16,16));d=ImageDraw.Draw(image)
d.rectangle((1,1,14,14),fill='#34292a',outline='#18171d')
d.rectangle((2,2,13,13),fill='#7a5540',outline='#c88e56')
d.line((3,3,12,3),fill='#f0bb75')
for x,y in [(2,2),(13,2),(2,13),(13,13)]:d.point((x,y),fill='#e8c28d')
d.rectangle((3,6,12,9),fill='#b48d42',outline='#392638')
d.rectangle((6,4,9,11),fill='#38234c',outline='#dbc27a')
d.rectangle((7,6,8,9),fill='#a375e0');d.point((7,6),fill='#e4c9ff')
d.line((3,7,5,7),fill='#eed78d');d.line((10,8,12,8),fill='#eed78d')
image.save(ASSETS/'textures/item'/(item+'.png'))
write(ASSETS/'models/item'/(item+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+item}})
components=['thaumcraft:essentia_tube','minecraft:piston','thaumcraft:essentia_phial','thaumicomniverse:alchemical_brass_ingot']
aspects={'motus':16,'aqua':12,'permutatio':8}
write(DATA/'recipes/collab'/(item+'.json'),{'type':'thaumcraft:infusion','research':key,'instability':2,
    'center':{'ingredient':{'item':center}},'components':[{'ingredient':{'item':x}} for x in components],
    'aspects':aspects,'result':{'item':NS+':'+item},'conditions':[{'type':'forge:mod_loaded','modid':'sophisticatedbackpacks'}]})
p=DATA/'research_catalog/backpacks.json';data=json.loads(p.read_text(encoding='utf-8'))
data['categories'][0]['research'].append({'key':key,'category':'THAUMICOMNIVERSE','displayColumn':6,'displayRow':16,
    'complexity':2,'isConcealed':True,'isSecondary':True,'parents':[NS+':ESSENTIA_TANK_UPGRADE'],'pageCount':2,
    'aspects':{'motus':8,'aqua':5,'permutatio':5},'icon_item':{'item':NS+':'+item},'omniverse_requires':['sophisticatedbackpacks']});write(p,data)
p=DATA/'research_pages/backpacks.json';data=json.loads(p.read_text(encoding='utf-8'))
data['entries'].append({'key':key,'omniverse_requires':['sophisticatedbackpacks'],'pages':[
    {'type':'TEXT','text':'omniverse.page.essentia_pump_upgrade'},
    {'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[key]}}]});write(p,data)
p=DATA/'research_page_recipes/backpacks.json';data=json.loads(p.read_text(encoding='utf-8'))
data['entries'].append({'key':key,'class':'thaumcraft.api.crafting.InfusionRecipe','omniverse_requires':['sophisticatedbackpacks'],
    'data':{'kind':'infusion','research':key,'instability':2,'input':stack(center),'output':stack(NS+':'+item),
    'ingredients':{'kind':'sequence','elements':[stack(x) for x in components]},
    'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()]}});write(p,data)
texts={
 'item.'+NS+'.'+item:('要素泵升级','Essentia Pump Upgrade'),
 'item.'+NS+'.'+item+'.tooltip':('为源质储罐输入或输出源质。','Moves essentia into or out of the essentia tank.'),
 'tc.research_name.'+key:('背包要素泵','Backpack Essentia Pump'),
 'tc.research_text.'+key:('让源质流动','Keep essentia flowing'),
 'omniverse.pump.pending':('暂存：%s','Pending: %s'),
 'omniverse.page.essentia_pump_upgrade':(
  '需要启用源质储罐升级。分页中可切换输入或输出，并分别开关手持容器、相邻储罐交互。每秒最多转移 8 点源质，源质瓶以完整的 8 点操作，要素罐遵守容量和标签限制。随身背包操作持有者的双手；放置的背包操作三格内玩家的手持容器。关闭相应开关可停止自动交互。异常转移未能退回的源质保存在升级中，并优先归还背包储罐。',
  'Requires an enabled essentia tank upgrade. Select input or output and toggle held-container and adjacent-storage interactions independently. Transfers up to 8 essentia per second; phials move in complete units of 8 and jars respect capacity and labels. Carried backpacks use the holder\'s hands; placed backpacks use held containers of players within three blocks. Disable the corresponding switch to stop automatic interaction. Unreturnable transfer remainders are retained in this upgrade and restored to its tank first.')}
for i,lang in enumerate(['zh_cn','en_us']):
 p=ASSETS/'lang'/(lang+'.json');data=json.loads(p.read_text(encoding='utf-8'));data.update({k:v[i] for k,v in texts.items()});write(p,data)
print('Generated essentia pump upgrade recipe, research and original artwork')

for item, title, english, column, parent, components, aspects, zh, en in [
 ('arcane_crafting_upgrade','奥术合成升级','Arcane Crafting Upgrade',0,'BASICTHAUMATURGY',
  ['thaumcraft:arcane_workbench','thaumcraft:thaumium_ingot','minecraft:crafting_table','thaumcraft:thaumium_ingot'],
  {'fabrico':16,'praecantatio':16,'cognitio':8},
  '提供九格奥术合成区和魔杖槽，同时支持普通合成。研究要求、vis 扣费、装备减免和容器剩余物规则与奥术工作台相同。合成区和魔杖保存在升级中，拆下升级不会丢失。界面显示减免后的实际消耗，vis 不足时数字变红。放置的背包上方可以安装工作台充能器，为合成区和背包内的魔杖供能。',
  'Provides a nine-slot arcane crafting grid and a wand slot, also supporting ordinary crafting. Research, vis payments, equipment discounts and crafting remainders follow the arcane workbench. Ingredients and the wand remain in the upgrade when removed. The tab shows discounted costs in red when vis is insufficient. A workbench charger above a placed backpack charges its crafting wand and stored wands.'),
 ('energized_node_upgrade','充能节点升级','Energized Node Upgrade',4,'VISPOWER',
  ['thaumcraft:node_converter','thaumcraft:node_stabilizer','thaumcraft:vis_relay','thaumcraft:thaumium_ingot'],
  {'auram':24,'praecantatio':16,'vacuos':16},
  '用未配置的升级右键收取一个充能节点。节点会从世界中移除；升级保留其永久供能速率，不会保留临时增强器带来的增益。装入背包后，每 tick 按各原始要素的 cv/t 预算为奥术合成槽和背包内的魔杖充能。此预算由所有魔杖共享，不会随魔杖数量倍增。升级提示会显示供能速率，拆下后保留节点，禁用时停止供能。',
  'Use an empty upgrade on an energized node to capture it. The node is removed from the world; its permanent supply rates are preserved without temporary amplifier bonuses. Installed upgrades share each primal aspect\'s cv/t budget across the arcane crafting wand and stored wands every tick. More wands do not multiply the budget. Supply rates appear in the tooltip. Removing the upgrade retains the node; disabling it stops charging.')]:
 key=NS+':'+item.upper()
 image=Image.new('RGBA',(16,16));d=ImageDraw.Draw(image)
 d.rectangle((1,1,14,14),fill='#34292a',outline='#18171d');d.rectangle((2,2,13,13),fill='#7a5540',outline='#c88e56')
 d.line((3,3,12,3),fill='#f0bb75')
 if item.startswith('arcane'):
  d.rectangle((4,4,11,11),fill='#4c325e',outline='#dfb965')
  for n in [6,9]:d.line((n,4,n,11),fill='#ae8cad');d.line((4,n,11,n),fill='#ae8cad')
  d.line((8,12,12,8),fill='#a07340',width=2);d.point((12,8),fill='#e7d37b')
 else:
  d.ellipse((4,4,11,11),fill='#535ebd',outline='#c39bef');d.ellipse((6,6,9,9),fill='#f1e5ff')
  for x,y,c in [(7,3,'#ffff7e'),(11,5,'#e64938'),(11,10,'#64ca4c'),(7,12,'#6fe3ed'),(3,10,'#dad8d4'),(3,5,'#78508c')]:d.point((x,y),fill=c)
 image.save(ASSETS/'textures/item'/(item+'.png'))
 write(ASSETS/'models/item'/(item+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+item}})
 write(DATA/'recipes/collab'/(item+'.json'),{'type':'thaumcraft:infusion','research':key,'instability':3,
  'center':{'ingredient':{'item':center}},'components':[{'ingredient':{'item':x}} for x in components],
  'aspects':aspects,'result':{'item':NS+':'+item},'conditions':[{'type':'forge:mod_loaded','modid':'sophisticatedbackpacks'}]})
 p=DATA/'research_catalog/backpacks.json';data=json.loads(p.read_text(encoding='utf-8'))
 data['categories'][0]['research'].append({'key':key,'category':'THAUMICOMNIVERSE','displayColumn':column,'displayRow':19,
  'complexity':2,'isConcealed':True,'isSecondary':True,'parents':[parent],'pageCount':2,
  'aspects':{a:max(1,n//2) for a,n in aspects.items()},'icon_item':{'item':NS+':'+item},'omniverse_requires':['sophisticatedbackpacks']});write(p,data)
 p=DATA/'research_pages/backpacks.json';data=json.loads(p.read_text(encoding='utf-8'))
 data['entries'].append({'key':key,'omniverse_requires':['sophisticatedbackpacks'],'pages':[
  {'type':'TEXT','text':'omniverse.page.'+item},
  {'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[key]}}]});write(p,data)
 p=DATA/'research_page_recipes/backpacks.json';data=json.loads(p.read_text(encoding='utf-8'))
 data['entries'].append({'key':key,'class':'thaumcraft.api.crafting.InfusionRecipe','omniverse_requires':['sophisticatedbackpacks'],
  'data':{'kind':'infusion','research':key,'instability':3,'input':stack(center),'output':stack(NS+':'+item),
  'ingredients':{'kind':'sequence','elements':[stack(x) for x in components]},
  'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()]}});write(p,data)
 texts={'item.'+NS+'.'+item:(title,english),'item.'+NS+'.'+item+'.tooltip':(title,english),
  'tc.research_name.'+key:(title,english),'tc.research_text.'+key:('随身的奥术技艺','Portable thaumaturgy'),
  'omniverse.page.'+item:(zh,en),'omniverse.node_upgrade.empty':('右键充能节点以收取，节点将从世界中移除。','Use on an energized node to capture and remove it from the world.')}
 for i,lang in enumerate(['zh_cn','en_us']):
  p=ASSETS/'lang'/(lang+'.json');data=json.loads(p.read_text(encoding='utf-8'));data.update({k:v[i] for k,v in texts.items()});write(p,data)
 print('Generated '+item+' resources')

# Preserve OK Backpack's original shaped arcane recipes (ModRecipes.thaumcraftRecipes).
for item,parent,amount,pattern,ingredients in [
 ('arcane_crafting_upgrade','BASICTHAUMATURGY',10,[' A ','IUI',' C '],
  {'A':'thaumcraft:arcane_workbench','I':'minecraft:iron_ingot','U':center,'C':'minecraft:chest'}),
 ('energized_node_upgrade','VISPOWER',50,[' T ','RUC',' S '],
  {'T':'thaumcraft:node_converter','R':'thaumcraft:vis_relay','U':center,
   'C':'thaumcraft:workbench_charger','S':'thaumcraft:advanced_node_stabilizer'})]:
 key=NS+':'+item.upper();vis={a:amount for a in ['aer','terra','ignis','aqua','ordo','perditio']}
 write(DATA/'recipes/collab'/(item+'.json'),{'type':'thaumcraft:arcane','research':parent,
  'pattern':pattern,'key':{k:{'item':v} for k,v in ingredients.items()},'vis':vis,
  'result':{'item':NS+':'+item},'conditions':[{'type':'forge:mod_loaded','modid':'sophisticatedbackpacks'}]})
 p=DATA/'research_pages/backpacks.json';data=json.loads(p.read_text(encoding='utf-8'))
 for entry in data['entries']:
  if entry['key']==key:entry['pages'][1]={'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.ShapedArcaneRecipe','keys':[key]}}
 write(p,data)

 p=DATA/'research_page_recipes/backpacks.json';data=json.loads(p.read_text(encoding='utf-8'))
 for entry in data['entries']:
  if entry['key']==key:
   entry['class']='thaumcraft.api.crafting.ShapedArcaneRecipe'
   entry['data']={'kind':'arcane_shaped','research':parent,'width':3,'height':3,'shaped':True,'mirrored':True,
    'output':stack(NS+':'+item),'aspects':[{'aspect':a,'amount':n} for a,n in vis.items()],
    'ingredients':{'kind':'sequence','elements':[stack(ingredients[c]) if c!=' ' else None for row in pattern for c in row]}}
 write(p,data)

texts={
 'omniverse.arcane.intoStorage.on':('合成产物优先放入背包','Crafted items go to the backpack first'),
 'omniverse.arcane.intoStorage.off':('合成产物优先放入玩家物品栏','Crafted items go to the player inventory first'),
 'omniverse.arcane.refill.on':('从背包补充消耗的合成材料','Refill consumed crafting ingredients from the backpack'),
 'omniverse.arcane.refill.off':('不自动补充合成材料','Do not refill crafting ingredients'),
 'omniverse.arcane.rotate':('旋转材料；按住 Shift 反向旋转','Rotate ingredients; hold Shift to reverse'),
 'omniverse.arcane.clear':('收回合成材料；无空间时保留','Return crafting ingredients; keep them if no room'),
 'key.thaumicomniverse.node_map':('切换已扫描节点显示','Toggle scanned aura nodes'),
 'omniverse.node_map.enabled':('显示已扫描的灵气节点','Show scanned aura nodes'),
 'omniverse.node_map.group':('已扫描的灵气节点','Scanned aura nodes'),
 'omniverse.node_map.live':('灵气节点：最近的服务端数据','Aura node: latest server observation'),
 'omniverse.node_map.offline':('灵气节点：未加载，恢复预估','Aura node: unloaded recovery forecast'),
 'omniverse.node_map.forecast':('基于上次记录的稳定器与永久节点状态。恢复的具体要素将在加载时随机决定。','Based on the last stabilizer and permanent node state. Individual recovered aspects are randomly chosen on load.')}
for i,lang in enumerate(['zh_cn','en_us']):
 p=ASSETS/'lang'/(lang+'.json');data=json.loads(p.read_text(encoding='utf-8'));data.update({k:v[i] for k,v in texts.items()});write(p,data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
