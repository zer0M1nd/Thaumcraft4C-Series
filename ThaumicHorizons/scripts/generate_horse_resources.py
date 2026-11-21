"""Horse transformations and the nightmare's separate research and transformation gate."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,data):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(key):return {'kind':'stack','stack':{'item':key,'count':1,'metadata':0,'damageable':False}}
def jar(entity):
    value=stack(f'{MOD}:creature_jar');value['stack']['nbt']=json.dumps({'Captured':{'id':entity,'Health':20.0}});return value
rows=[('endersteed',6,{'alienis':16,'iter':16,'vacuos':24},['minecraft:ender_pearl','minecraft:egg','minecraft:ender_pearl','thaumcraft:salis_mundus'],'horse'),('undead_horse',2,{'mortuus':8,'exanimis':8},['minecraft:rotten_flesh']*4,'zombie_horse'),('skeletal_horse',2,{'mortuus':8,'exanimis':8},['minecraft:bone']*4,'skeleton_horse')]
pages=[];displays=[]
for index,(key,danger,aspects,ingredients,loot) in enumerate(rows):
    write(f'data/{MOD}/vat_infusions/{key}.json',{'research':'horseInfusion','instability':danger,'target':{'kind':'entity','id':'minecraft:horse'},'ingredients':[{'item':i} for i in ingredients],'aspects':aspects,'output':{'kind':'entity','id':f'{MOD}:{key}'}})
    displays.append({'key':f'{MOD}:creature_{key}','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'horseInfusion','input':jar('minecraft:horse'),'output':jar(f'{MOD}:{key}'),'ingredients':{'kind':'sequence','elements':[stack(i) for i in ingredients]},'aspects':aspects,'instability':danger}})
    if index<2:pages.append({'type':'TEXT','text':f'horseInfusion{index+2}'})
    pages.append({'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:creature_{key}']}})
    write(f'data/{MOD}/loot_tables/entities/{key}.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table','name':f'minecraft:entities/{loot}'}]}]})
research={'key':'horseInfusion','category':'ThaumicHorizons','displayColumn':15,'displayRow':-5,'complexity':2,'aspects':[{'aspect':a,'amount':n} for a,n in {'bestia':4,'iter':5,'alienis':4}.items()],'parents':['generalInfusion'],'parentsHidden':[],'siblings':[],'isConcealed':True,'icon_item':{'item':'minecraft:saddle'},'pageCount':len(pages)}
write(f'data/{MOD}/research_catalog/horses.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[research]}]})
write(f'data/{MOD}/research_pages/horses.json',{'format':1,'entries':[{'key':'horseInfusion','pages':pages}]})
write(f'data/{MOD}/research_page_recipes/horses.json',{'format':1,'entries':displays})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=json.loads((ROOT/path).read_text(encoding='utf-8'))
for suffix in ['', 'iron','gold','diamond']:manifest['mapping'][f'assets/{MOD}/textures/entity/endersteed{suffix}.png']=f'assets/thaumichorizons/textures/entity/endersteed{suffix}.png'
for language in manifest['languages'].values():
    for modern,legacy in [('endersteed','Endersteed'),('undead_horse','HorseZombie'),('skeletal_horse','HorseSkeleton')]:language[f'entity.{MOD}.{modern}']=f'entity.ThaumicHorizons.{legacy}.name'
    for key in ['tc.research_name.horseInfusion','tc.research_text.horseInfusion','horseInfusion2','horseInfusion3']:language[key]=key
write(path,manifest)
# Nightmare keeps its own research gate and transforms an endersteed, not an ordinary horse.
ingredients=['minecraft:nether_star','minecraft:obsidian','thaumcraft:fire_shard','thaumcraft:levitator','thaumcraft:fire_shard','minecraft:obsidian'];aspects={'ignis':32,'iter':16,'vacuos':16}
write(f'data/{MOD}/vat_infusions/nightmare.json',{'research':'nightmareInfusion','instability':8,'target':{'kind':'entity','id':f'{MOD}:endersteed'},'ingredients':[{'item':i} for i in ingredients],'aspects':aspects,'output':{'kind':'entity','id':f'{MOD}:nightmare'}})
recipe={'key':f'{MOD}:creature_nightmare','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'nightmareInfusion','input':jar(f'{MOD}:endersteed'),'output':jar(f'{MOD}:nightmare'),'ingredients':{'kind':'sequence','elements':[stack(i) for i in ingredients]},'aspects':aspects,'instability':8}}
pages=[{'type':'TEXT','text':'nightmareInfusion1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:creature_nightmare']}},{'type':'TEXT','text':'nightmareInfusion2'}]
research={'key':'nightmareInfusion','category':'ThaumicHorizons','displayColumn':17,'displayRow':-7,'complexity':2,'aspects':[{'aspect':a,'amount':n} for a,n in {'ignis':4,'vacuos':3,'iter':3}.items()],'parents':['horseInfusion','LEVITATOR'],'parentsHidden':[],'siblings':[],'isConcealed':True,'icon_item':{'item':'minecraft:nether_star'},'pageCount':3}
write(f'data/{MOD}/research_catalog/nightmare.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[research]}]})
write(f'data/{MOD}/research_pages/nightmare.json',{'format':1,'entries':[{'key':'nightmareInfusion','pages':pages}]})
write(f'data/{MOD}/research_page_recipes/nightmare.json',{'format':1,'entries':[recipe]})
write(f'data/{MOD}/loot_tables/entities/nightmare.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table','name':'minecraft:entities/horse'}]}]})
for suffix in ['', 'iron','gold','diamond']:manifest['mapping'][f'assets/{MOD}/textures/entity/nightmare{suffix}.png']=f'assets/thaumichorizons/textures/entity/nightmare{suffix}.png'
for language in manifest['languages'].values():
    language[f'entity.{MOD}.nightmare']='entity.ThaumicHorizons.NightmareTH.name'
    for key in ['tc.research_name.nightmareInfusion','tc.research_text.nightmareInfusion','nightmareInfusion1','nightmareInfusion2']:language[key]=key
write(path,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
