"""Pig infusion recipes and legacy artwork mappings."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,data):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1,'metadata':0,'damageable':False}}
def jar(entity):
    value=stack(f'{MOD}:creature_jar');value['stack']['nbt']=json.dumps({'Captured':{'id':entity,'Health':10.0}});return value
displays=[];pages=[]
for index,(key,danger,aspects,ingredients) in enumerate([
    ('ore_pig',4,{'terra':8,'fames':8,'metallum':16,'ignis':8},['minecraft:furnace','thaumcraft:fire_shard','thaumcraft:entropy_shard','thaumcraft:earth_shard']),
    ('taint_eater_pig',6,{'vitium':6,'sano':8,'herba':8,'fames':16},['thaumcraft:ethereal_bloom','thaumcraft:silverwood_log','thaumcraft:brain_jar','thaumcraft:silverwood_log'])],1):
    write(f'data/{MOD}/vat_infusions/{key}.json',{'research':'pigInfusion','instability':danger,'target':{'kind':'entity','id':'minecraft:pig'},'ingredients':[{'item':i} for i in ingredients],'aspects':aspects,'output':{'kind':'entity','id':f'{MOD}:{key}'}})
    displays.append({'key':f'{MOD}:creature_{key}','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'pigInfusion','input':jar('minecraft:pig'),'output':jar(f'{MOD}:{key}'),'ingredients':{'kind':'sequence','elements':[stack(i) for i in ingredients]},'aspects':aspects,'instability':danger}})
    pages.extend([{'type':'TEXT','text':f'pigInfusion{index}'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:creature_{key}']}}])
research={'key':'pigInfusion','category':'ThaumicHorizons','displayColumn':12,'displayRow':-5,'complexity':1,'aspects':[{'aspect':a,'amount':n} for a,n in {'bestia':5,'fames':5,'metallum':3,'vitium':3,'sano':3}.items()],'parents':['generalInfusion'],'parentsHidden':[],'siblings':[],'isConcealed':True,'icon_item':{'item':'minecraft:cobblestone'},'pageCount':len(pages)}
write(f'data/{MOD}/research_catalog/pigs.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[research]}]})
write(f'data/{MOD}/research_pages/pigs.json',{'format':1,'entries':[{'key':'pigInfusion','pages':pages}]})
write(f'data/{MOD}/research_page_recipes/pigs.json',{'format':1,'entries':displays})
write(f'data/{MOD}/tags/blocks/taint_edible.json',{'replace':False,'values':['thaumcraft:taint_fibres','thaumcraft:taint_crust','thaumcraft:taint_soil']})
write(f'data/{MOD}/loot_tables/entities/taint_eater_pig.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table','name':'minecraft:entities/pig'}]}]})
write(f'data/{MOD}/loot_tables/entities/ore_pig.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:stone','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':3}},{'function':'minecraft:looting_enchant','count':{'type':'minecraft:uniform','min':0,'max':1}}]}]}]})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=json.loads((ROOT/path).read_text(encoding='utf-8'))
for texture in ['oreboar','taintfeeder','taintfeeder_eyes']:manifest['mapping'][f'assets/{MOD}/textures/entity/{texture}.png']=f'assets/thaumichorizons/textures/entity/{texture}.png'
for language in manifest['languages'].values():
    for modern,legacy in [('ore_pig','OrePig'),('taint_eater_pig','TaintEaterPig')]:language[f'entity.{MOD}.{modern}']=f'entity.ThaumicHorizons.{legacy}.name'
    for key in ['tc.research_name.pigInfusion','tc.research_text.pigInfusion','pigInfusion1','pigInfusion2']:language[key]=key
write(path,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
