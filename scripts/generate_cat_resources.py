"""Legacy ocelot transformations cover both modern cats and modern ocelots."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,data):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def item(key):return {'item':key}
def stack(key):return {'kind':'stack','stack':{'item':key,'count':1,'metadata':0,'damageable':False}}
def jar(entity):
    value=stack(f'{MOD}:creature_jar');value['stack']['nbt']=json.dumps({'Captured':{'id':entity,'Health':10.0}});return value
write(f'data/{MOD}/tags/entity_types/infusable_cats.json',{'replace':False,'values':['minecraft:cat','minecraft:ocelot']})
rows=[('guardian_panther',2,{'bestia':8,'telum':8},[{'type':'forge:nbt','item':'minecraft:potion','nbt':{'Potion':'minecraft:strong_strength'}},item('minecraft:saddle')]),('familiar',4,{'vinculum':8,'auram':4,'potentia':16},[item('thaumcraft:balanced_shard'),item('minecraft:black_wool'),item('thaumcraft:balanced_shard'),item('minecraft:gold_nugget')]),('gravekeeper',6,{'ordo':16,'mortuus':8,'lux':24},[item('minecraft:bone'),item('minecraft:gold_block'),item('thaumcraft:amber'),item('thaumcraft:silverwood_log')])]
displays=[];pages=[]
for index,(key,danger,aspects,ingredients) in enumerate(rows,1):
    write(f'data/{MOD}/vat_infusions/{key}.json',{'research':'catInfusion','instability':danger,'target':{'kind':'tag','id':f'{MOD}:infusable_cats'},'ingredients':ingredients,'aspects':aspects,'output':{'kind':'entity','id':f'{MOD}:{key}'}})
    shown=[]
    for ingredient in ingredients:
        value=stack(ingredient['item'])
        if 'nbt' in ingredient:value['stack']['nbt']=json.dumps(ingredient['nbt'])
        shown.append(value)
    displays.append({'key':f'{MOD}:creature_{key}','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'catInfusion','input':{'kind':'sequence','elements':[jar('minecraft:cat'),jar('minecraft:ocelot')]},'output':jar(f'{MOD}:{key}'),'ingredients':{'kind':'sequence','elements':shown},'aspects':aspects,'instability':danger}})
    pages.extend([{'type':'TEXT','text':f'catInfusion{index}'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:creature_{key}']}}])
    write(f'data/{MOD}/loot_tables/entities/{key}.json',{'type':'minecraft:entity','pools':[]})
research={'key':'catInfusion','category':'ThaumicHorizons','displayColumn':13,'displayRow':-5,'complexity':1,'aspects':[{'aspect':a,'amount':n} for a,n in {'bestia':5,'praecantatio':3,'exanimis':3}.items()],'parents':['generalInfusion'],'parentsHidden':[],'siblings':[],'isConcealed':True,'icon_item':{'item':'minecraft:cod'},'pageCount':len(pages)}
write(f'data/{MOD}/research_catalog/cats.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[research]}]})
write(f'data/{MOD}/research_pages/cats.json',{'format':1,'entries':[{'key':'catInfusion','pages':pages}]})
write(f'data/{MOD}/research_page_recipes/cats.json',{'format':1,'entries':displays})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=json.loads((ROOT/path).read_text(encoding='utf-8'))
for texture in ['familiar','gravekeeper']:manifest['mapping'][f'assets/{MOD}/textures/entity/{texture}.png']=f'assets/thaumichorizons/textures/entity/{texture}.png'
for language in manifest['languages'].values():
    for modern,legacy in [('guardian_panther','GuardianPanther'),('familiar','Familiar'),('gravekeeper','Gravekeeper')]:language[f'entity.{MOD}.{modern}']=f'entity.ThaumicHorizons.{legacy}.name'
    for key in ['tc.research_name.catInfusion','tc.research_text.catInfusion','catInfusion1','catInfusion2','catInfusion3']:language[key]=key
write(path,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
