"""Implemented transformed creatures only; legacy images are resolved from the user's jar."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,data):
    if ('/item_aspects/' in path or '/item_bonus_aspects/' in path) and (ROOT / 'data/thaumichorizons/item_aspects/explicit.json').exists(): return
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def stack(item):
    if item=='minecraft:ink_sac':return {'kind':'ore','names':['forge:dyes/black'],'elements':[stack('minecraft:black_dye')]}
    return {'kind':'stack','stack':{'item':item,'count':1,'metadata':0,'damageable':False}}

def ingredient(item):return {'tag':'forge:dyes/black'} if item=='minecraft:ink_sac' else {'item':item}
def jar(entity):
    value=stack(f'{MOD}:creature_jar');value['stack']['nbt']=json.dumps({'Captured':{'id':entity,'Health':4.0}});return value
recipes=[('scholar_chicken',2,{'tenebrae':4,'limus':4,'permutatio':2},['minecraft:ink_sac','minecraft:shears']),('gold_chicken',6,{'lucrum':8,'metallum':8,'fabrico':16},['minecraft:golden_apple','thaumcraft:alchemical_construct','minecraft:golden_apple','thaumcraft:crucible'])]
displays=[];pages=[]
for index,(key,instability,aspects,ingredients) in enumerate(recipes,1):
    write(f'data/{MOD}/vat_infusions/{key}.json',{'research':'chickenInfusion','instability':instability,'target':{'kind':'entity','id':'minecraft:chicken'},'ingredients':[ingredient(i) for i in ingredients],'aspects':aspects,'output':{'kind':'entity','id':f'{MOD}:{key}'}})
    displays.append({'key':f'{MOD}:creature_{key}','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'chickenInfusion','input':jar('minecraft:chicken'),'output':jar(f'{MOD}:{key}'),'ingredients':{'kind':'sequence','elements':[stack(i) for i in ingredients]},'aspects':aspects,'instability':instability}})
    pages.extend([{'type':'TEXT','text':f'chickenInfusion{index}'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:creature_{key}']}}])
    write(f'data/{MOD}/loot_tables/entities/{key}.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table','name':'minecraft:entities/chicken'}]}]})
pages.append({'type':'TEXT','text':f'{MOD}.gold_egg_smelting'})
research={'key':'chickenInfusion','category':'ThaumicHorizons','displayColumn':11,'displayRow':-5,'complexity':1,'aspects':[{'aspect':a,'amount':n} for a,n in {'bestia':5,'limus':5,'lucrum':3,'tenebrae':3}.items()],'parents':['generalInfusion'],'parentsHidden':[],'siblings':[],'isSecondary':True,'isConcealed':True,'icon_item':{'item':f'{MOD}:ink_egg'},'pageCount':len(pages)}
write(f'data/{MOD}/research_catalog/creatures.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[research]}]})
write(f'data/{MOD}/research_pages/creatures.json',{'format':1,'entries':[{'key':'chickenInfusion','pages':pages}]})
write(f'data/{MOD}/research_page_recipes/creatures.json',{'format':1,'entries':displays})
write(f'data/{MOD}/recipes/gold_egg_smelting.json',{'type':'minecraft:smelting','ingredient':{'item':f'{MOD}:gold_egg'},'result':'minecraft:gold_nugget','experience':1.0,'cookingtime':200})
write('data/forge/tags/items/dyes/black.json',{'replace':False,'values':[f'{MOD}:ink_egg']})
write('data/forge/tags/items/dyes.json',{'replace':False,'values':['#forge:dyes/black']})
write(f'data/{MOD}/item_aspects/creature_products.json',{'format':1,'entries':[{'items':[f'{MOD}:{key}'],'aspects':[{'aspect':aspect,'amount':amount} for aspect,amount in aspects.items()]} for key,aspects in [('ink_egg',{'limus':2,'tenebrae':2}),('gold_egg',{'limus':2,'lucrum':1})]]})
manifestPath=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(manifestPath)
for key,texture,translation in [('ink_egg','tenebrousegg','inkEgg'),('gold_egg','goldegg','goldEgg')]:
    write(f'assets/{MOD}/models/item/{key}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:items/{texture}'}})
    manifest['mapping'][f'assets/{MOD}/textures/items/{texture}.png']=f'assets/thaumichorizons/textures/items/{texture}.png'
    for language in manifest['languages'].values():language[f'item.{MOD}.{key}']=f'item.{translation}.name'
for key,texture,translation in [('scholar_chicken','scholarchicken','ScholarChicken'),('gold_chicken','goldchicken','GoldChicken')]:
    manifest['mapping'][f'assets/{MOD}/textures/entity/{texture}.png']=f'assets/thaumichorizons/textures/entity/{texture}.png'
    for language in manifest['languages'].values():language[f'entity.{MOD}.{key}']=f'entity.ThaumicHorizons.{translation}.name'
for language in manifest['languages'].values():
    for key in ['tc.research_name.chickenInfusion','tc.research_text.chickenInfusion','chickenInfusion1','chickenInfusion2']:language[key]=key
write(manifestPath,manifest)
for lang,text in [('en_us','Smelt a golden egg in a furnace to obtain one gold nugget (1 experience).'),('zh_cn','在熔炉中熔炼一个金蛋可获得一个金粒和 1 点经验。')]:
    path=f'assets/{MOD}/lang/{lang}.json';value=read(path);value[f'{MOD}.gold_egg_smelting']=text;write(path,value)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
