"""Both original sheeder conversions, preserving any-colour wool ingredients."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,data):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1,'metadata':0,'damageable':False}}
def jar(entity):
    value=stack(f'{MOD}:creature_jar');value['stack']['nbt']=json.dumps({'Captured':{'id':entity,'Health':8.0}});return value
displays=[];pages=[{'type':'TEXT','text':'sheederInfusion1'}]
for source,aspects,ingredients in [('sheep',{'pannus':32,'permutatio':8,'venenum':8},[{'item':'minecraft:cobweb'},{'item':'minecraft:spider_eye'}]*2),('spider',{'pannus':32,'permutatio':8,'cognitio':16},[{'item':'thaumcraft:zombie_brain'}]+[{'tag':'minecraft:wool'}]*3)]:
    key='sheeder_from_'+source
    write(f'data/{MOD}/vat_infusions/{key}.json',{'research':'sheederInfusion','instability':5,'target':{'kind':'entity','id':'minecraft:'+source},'ingredients':ingredients,'aspects':aspects,'output':{'kind':'entity','id':f'{MOD}:sheeder'}})
    # A sequence of wool stacks displays every valid colour in the recipe slot.
    colours=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
    shown=[stack(i['item']) if 'item' in i else {'kind':'sequence','elements':[stack(f'minecraft:{colour}_wool') for colour in colours]} for i in ingredients]
    displays.append({'key':f'{MOD}:{key}','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'sheederInfusion','input':jar('minecraft:'+source),'output':jar(f'{MOD}:sheeder'),'ingredients':{'kind':'sequence','elements':shown},'aspects':aspects,'instability':5}})
    pages.append({'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:{key}']}})
research={'key':'sheederInfusion','category':'ThaumicHorizons','displayColumn':10,'displayRow':-7,'complexity':2,'aspects':[{'aspect':a,'amount':n} for a,n in {'pannus':4,'bestia':3,'permutatio':3}.items()],'parents':['sheepInfusion'],'parentsHidden':[],'siblings':[],'isConcealed':True,'icon_item':{'item':'minecraft:string'},'pageCount':len(pages)}
write(f'data/{MOD}/research_catalog/sheeder.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[research]}]})
write(f'data/{MOD}/research_pages/sheeder.json',{'format':1,'entries':[{'key':'sheederInfusion','pages':pages}]})
write(f'data/{MOD}/research_page_recipes/sheeder.json',{'format':1,'entries':displays})
write(f'data/{MOD}/loot_tables/entities/sheeder.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:string'}],'conditions':[{'condition':'minecraft:entity_properties','entity':'this','predicate':{'nbt':'{Sheared:0b}'}}]}]})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=json.loads((ROOT/path).read_text(encoding='utf-8'))
for texture in ['sheeder','sheederoverlay']:manifest['mapping'][f'assets/{MOD}/textures/entity/{texture}.png']=f'assets/thaumichorizons/textures/entity/{texture}.png'
for language in manifest['languages'].values():
    language[f'entity.{MOD}.sheeder']='entity.ThaumicHorizons.Sheeder.name'
    for key in ['tc.research_name.sheederInfusion','tc.research_text.sheederInfusion','sheederInfusion1']:language[key]=key
write(path,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
