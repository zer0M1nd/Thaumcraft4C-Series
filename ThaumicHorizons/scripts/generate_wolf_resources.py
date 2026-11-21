"""Three wolf variants, with legacy potion metadata translated to named potion NBT."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,data):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def item(key):return {'item':key}
def stack(key):return {'kind':'stack','stack':{'item':key,'count':1,'metadata':0,'damageable':False}}
def jar(entity):
    value=stack(f'{MOD}:creature_jar');value['stack']['nbt']=json.dumps({'Captured':{'id':entity,'Health':8.0}});return value
fish={'tag':'forge:raw_fishes'}
rows=[('seawolf',2,{'aqua':8,'aer':8,'permutatio':4,'motus':8},[fish,item('thaumcraft:water_shard'),fish,{'type':'forge:nbt','item':'minecraft:potion','nbt':{'Potion':'minecraft:long_water_breathing'}}]),('lunar_wolf',4,{'lux':8,'vacuos':8,'praecantatio':16,'permutatio':4},[item('thaumcraft:salis_mundus'),item('thaumcraft:mirrored_glass'),item('thaumcraft:salis_mundus')]),('nether_hound',6,{'ignis':24,'telum':8,'bestia':8},[item('minecraft:lava_bucket'),item('minecraft:blaze_rod'),item('thaumcraft:fire_shard'),item('minecraft:blaze_rod')])]
pages=[];displays=[]
for index,(key,danger,aspects,ingredients) in enumerate(rows,1):
    write(f'data/{MOD}/vat_infusions/{key}.json',{'research':'dogInfusion','instability':danger,'target':{'kind':'entity','id':'minecraft:wolf'},'ingredients':ingredients,'aspects':aspects,'output':{'kind':'entity','id':f'{MOD}:{key}'}})
    shown=[]
    for ingredient in ingredients:
        value=stack(ingredient['item']) if 'item' in ingredient else {'kind':'sequence','elements':[stack('minecraft:'+fish) for fish in ['cod','salmon','tropical_fish','pufferfish']]}
        if 'nbt' in ingredient:value['stack']['nbt']=json.dumps(ingredient['nbt'])
        shown.append(value)
    displays.append({'key':f'{MOD}:creature_{key}','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'dogInfusion','input':jar('minecraft:wolf'),'output':jar(f'{MOD}:{key}'),'ingredients':{'kind':'sequence','elements':shown},'aspects':aspects,'instability':danger}})
    pages.extend([{'type':'TEXT','text':f'dogInfusion{index}'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:creature_{key}']}}])
    write(f'data/{MOD}/loot_tables/entities/{key}.json',{'type':'minecraft:entity','pools':[]})
research={'key':'dogInfusion','category':'ThaumicHorizons','displayColumn':14,'displayRow':-5,'complexity':1,'aspects':[{'aspect':a,'amount':n} for a,n in {'bestia':5,'lux':3,'ignis':3,'aqua':3,'praecantatio':3}.items()],'parents':['generalInfusion'],'parentsHidden':[],'siblings':[],'isConcealed':True,'icon_item':{'item':'minecraft:bone'},'pageCount':len(pages)}
write(f'data/{MOD}/research_catalog/wolves.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[research]}]})
write(f'data/{MOD}/research_pages/wolves.json',{'format':1,'entries':[{'key':'dogInfusion','pages':pages}]})
write(f'data/{MOD}/research_page_recipes/wolves.json',{'format':1,'entries':displays})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=json.loads((ROOT/path).read_text(encoding='utf-8'))
for texture in ['seawolf','lunarwolf','netherhound']:manifest['mapping'][f'assets/{MOD}/textures/entity/{texture}.png']=f'assets/thaumichorizons/textures/entity/{texture}.png'
for language in manifest['languages'].values():
    for modern,legacy in [('seawolf','Seawolf'),('lunar_wolf','LunarWolf'),('nether_hound','NetherHound')]:language[f'entity.{MOD}.{modern}']=f'entity.ThaumicHorizons.{legacy}.name'
    for key in ['tc.research_name.dogInfusion','tc.research_text.dogInfusion','dogInfusion1','dogInfusion2','dogInfusion3']:language[key]=key
write(path,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
