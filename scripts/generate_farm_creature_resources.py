"""Cow and sheep transformations and their complete survival product recipes."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,data):
    if ('/item_aspects/' in path or '/item_bonus_aspects/' in path) and (ROOT / 'data/thaumichorizons/item_aspects/explicit.json').exists(): return
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def stack(item,count=1):return {'kind':'stack','stack':{'item':item,'count':count,'metadata':0,'damageable':False}}
def jar(entity):
    value=stack(f'{MOD}:creature_jar');value['stack']['nbt']=json.dumps({'Captured':{'id':entity,'Health':8.0}});return value
def text(key):return {'type':'TEXT','text':key}
def ref(key,kind='infusion'):
    return {'type':{'infusion':'INFUSION_CRAFTING','arcane':'ARCANE_CRAFTING','crafting':'NORMAL_CRAFTING'}[kind],'recipe':{'kind':'reference','class':{'infusion':'thaumcraft.api.crafting.InfusionRecipe','arcane':'thaumcraft.api.crafting.ShapelessArcaneRecipe','crafting':'net.minecraft.item.crafting.ShapedRecipes'}[kind],'keys':[f'{MOD}:{key}']}}
displays=[]
recipes=[
    ('chocolate_cow','cowInfusion','minecraft:cow',f'{MOD}:chocolate_cow',{'fames':4,'permutatio':2},['minecraft:cocoa_beans','minecraft:sugar']*2),
    ('mooshroom','cowInfusion','minecraft:cow','minecraft:mooshroom',{'herba':8,'bestia':4},['minecraft:brown_mushroom','minecraft:red_mushroom']*2),
    ('chromatic_sheep','sheepInfusion','minecraft:sheep',f'{MOD}:chromatic_sheep',{'sensus':8,'permutatio':4},['minecraft:red_dye','minecraft:lapis_lazuli','minecraft:yellow_dye']),
    ('self_shearing_sheep','sheepInfusion','minecraft:sheep',f'{MOD}:self_shearing_sheep',{'instrumentum':8,'pannus':4},['minecraft:shears','minecraft:comparator'])]
for key,research,source,target,aspects,ingredients in recipes:
    write(f'data/{MOD}/vat_infusions/{key}.json',{'research':research,'instability':2,'target':{'kind':'entity','id':source},'ingredients':[{'item':i} for i in ingredients],'aspects':aspects,'output':{'kind':'entity','id':target}})
    displays.append({'key':f'{MOD}:creature_{key}','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':research,'input':jar(source),'output':jar(target),'ingredients':{'kind':'sequence','elements':[stack(i) for i in ingredients]},'aspects':aspects,'instability':2}})
for key,base in [('chocolate_cow','cow'),('chromatic_sheep','sheep'),('self_shearing_sheep','sheep')]:
    write(f'data/{MOD}/loot_tables/entities/{key}.json',{'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:loot_table','name':f'minecraft:entities/{base}'}]}]})
write(f'data/{MOD}/recipes/chocolate_bar.json',{'type':'minecraft:crafting_shaped','pattern':['M'],'key':{'M':{'item':f'{MOD}:chocolate_milk'}},'result':{'item':f'{MOD}:chocolate_bar'}})
write(f'data/{MOD}/recipes/chocolate_block.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':f'{MOD}:chocolate_bar'}]*9,'result':{'item':f'{MOD}:chocolate'}})
write(f'data/{MOD}/recipes/chocolate_unpack.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':f'{MOD}:chocolate'}],'result':{'item':f'{MOD}:chocolate_bar','count':9}})
write(f'data/{MOD}/recipes/arcane/ice_cream.json',{'type':'thaumcraft:arcane','mode':'shapeless','research':'cowInfusion','ingredients':[{'item':f'{MOD}:chocolate_milk'},{'item':'minecraft:wheat'}],'vis':{'aqua':1},'result':{'item':f'{MOD}:ice_cream'}})
displays.extend([
    {'key':f'{MOD}:chocolate_bar','class':'net.minecraft.item.crafting.ShapedRecipes','data':{'kind':'crafting_shaped','width':1,'height':1,'mirrored':True,'ingredients':{'kind':'sequence','elements':[stack(f'{MOD}:chocolate_milk')]},'output':stack(f'{MOD}:chocolate_bar')}},
    {'key':f'{MOD}:ice_cream','class':'thaumcraft.api.crafting.ShapelessArcaneRecipe','data':{'kind':'arcane_shapeless','research':'cowInfusion','ingredients':{'kind':'sequence','elements':[stack(f'{MOD}:chocolate_milk'),stack('minecraft:wheat')]},'output':stack(f'{MOD}:ice_cream'),'aspects':{'aqua':1}}}])
cowPages=[text('cowInfusion1'),ref('creature_chocolate_cow'),ref('chocolate_bar','crafting'),ref('ice_cream','arcane'),text('cowInfusion2'),ref('creature_mooshroom')]
sheepPages=[text('sheepInfusion1'),ref('creature_chromatic_sheep'),ref('creature_self_shearing_sheep')]
research=[];pages=[]
for key,icon,x,aspects,content,secondary in [('cowInfusion',f'{MOD}:chocolate_milk',9,{'bestia':5,'limus':5,'permutatio':5},cowPages,False),('sheepInfusion','minecraft:white_wool',10,{'bestia':3,'pannus':3,'sensus':3},sheepPages,True)]:
    research.append({'key':key,'category':'ThaumicHorizons','displayColumn':x,'displayRow':-5,'complexity':1,'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()],'parents':['generalInfusion'],'parentsHidden':[],'siblings':[],'isSecondary':secondary,'isConcealed':True,'icon_item':{'item':icon},'pageCount':len(content)})
    pages.append({'key':key,'pages':content})
write(f'data/{MOD}/research_catalog/farm_creatures.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':research}]})
write(f'data/{MOD}/research_pages/farm_creatures.json',{'format':1,'entries':pages})
write(f'data/{MOD}/research_page_recipes/farm_creatures.json',{'format':1,'entries':displays})
write(f'assets/{MOD}/models/block/chocolate.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'{MOD}:blocks/chocolate'}})
write(f'assets/{MOD}/models/item/chocolate.json',{'parent':f'{MOD}:block/chocolate'})
write(f'assets/{MOD}/blockstates/chocolate.json',{'variants':{'':{'model':f'{MOD}:block/chocolate'}}})
write(f'data/{MOD}/loot_tables/blocks/chocolate.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:chocolate'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(f'data/{MOD}/item_aspects/chocolate.json',{'format':1,'entries':[{'items':[f'{MOD}:{key}'],'aspects':[{'aspect':aspect,'amount':amount} for aspect,amount in aspects.items()]} for key,aspects in [('chocolate_bar',{'fames':2,'victus':1}),('ice_cream',{'fames':3,'victus':1,'gelum':1}),('chocolate_milk',{'metallum':8,'vacuos':1,'fames':2,'victus':2})]]})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(path)
for key,texture,translation in [('chocolate_bar','chocolatebar','chocolate'),('chocolate_milk','bucket_chocolatemilk','chocolateMilk'),('ice_cream','icecream','icecream')]:
    write(f'assets/{MOD}/models/item/{key}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:items/{texture}'}})
    manifest['mapping'][f'assets/{MOD}/textures/items/{texture}.png']=f'assets/thaumichorizons/textures/items/{texture}.png'
    for language in manifest['languages'].values():language[f'item.{MOD}.{key}']=f'item.{translation}.name'
for asset in ['textures/blocks/chocolate.png','textures/entity/chocolatecow.png']:manifest['mapping'][f'assets/{MOD}/{asset}']=f'assets/thaumichorizons/{asset}'
for language in manifest['languages'].values():
    language[f'block.{MOD}.chocolate']='tile.ThaumicHorizons_chocolate.name'
    for modern,legacy in [('chocolate_cow','ChocolateCow'),('chromatic_sheep','ChromaticSheep'),('self_shearing_sheep','SelfShearingSheep')]:language[f'entity.{MOD}.{modern}']=f'entity.ThaumicHorizons.{legacy}.name'
    for key in ['cowInfusion','sheepInfusion']:
        for prefix in ['tc.research_name.','tc.research_text.']:language[prefix+key]=prefix+key
    for key in ['cowInfusion1','cowInfusion2','sheepInfusion1']:language[key]=key
write(path,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
