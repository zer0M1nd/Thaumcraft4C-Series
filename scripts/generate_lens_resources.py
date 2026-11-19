import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,value):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1,'metadata':0,'damageable':False}}
rows=[
 ('fire','Fire',2,-2,2,{'sensus':3,'lux':3,'potentia':2},{'lux':20,'potentia':12,'sensus':20},['minecraft:golden_carrot','thaumcraft:fire_shard']*2,['GOGGLES','INFUSION']),
 ('water','Water',1,-4,1,{'sensus':4,'permutatio':2},{'sensus':24,'permutatio':12,'aqua':12},['thaumcraft:arcane_ear','thaumcraft:water_shard','minecraft:note_block','thaumcraft:water_shard'],['lensFire','ARCANEEAR']),
 ('earth','Earth',3,-4,1,{'sensus':4,'vacuos':2,'lux':2},{'sensus':20,'vacuos':12,'lux':12},['minecraft:glowstone_dust','thaumcraft:earth_shard','minecraft:ender_eye','thaumcraft:earth_shard'],['lensFire']),
 ('air','Air',-1,-4,1,{'sensus':4,'auram':2,'victus':2,'exanimis':2},{'sensus':18,'auram':4,'victus':12,'exanimis':12},['minecraft:bone','thaumcraft:air_shard','minecraft:egg','thaumcraft:air_shard'],['lensFire']),
 ('order_entropy','OrderEntropy',5,-4,1,{'sensus':4,'cognitio':4,'praecantatio':2},{'sensus':24,'cognitio':12,'praecantatio':12},['thaumcraft:scribing_tools','thaumcraft:entropy_shard','minecraft:book','thaumcraft:order_shard'],['lensFire'])]
research=[];pages=[];display=[];path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(path)
for texture in ['ripple','vortex']:manifest['mapping'][f'assets/{MOD}/textures/fx/{texture}.png']=f'assets/thaumichorizons/textures/fx/{texture}.png'
for key,old,x,y,complexity,points,costs,ingredients,parents in rows:
    item=f'{MOD}:lens_{key}';r='lens'+old
    texture='lens'+old.lower();write(f'assets/{MOD}/models/item/lens_{key}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/{texture}'}})
    manifest['mapping'][f'assets/{MOD}/textures/item/{texture}.png']=f'assets/thaumichorizons/textures/items/{texture}.png'
    write(f'data/{MOD}/recipes/infusion/lens_{key}.json',{'type':'thaumcraft:infusion','research':r,'instability':1,'aspects':costs,'center':{'ingredient':{'item':'minecraft:glass_pane'}},'components':[{'ingredient':{'item':i}} for i in ingredients],'result':{'item':item}})
    research.append({'key':r,'category':'ThaumicHorizons','displayColumn':x,'displayRow':y,'complexity':complexity,'aspects':[{'aspect':a,'amount':n} for a,n in points.items()],'parents':parents,'isConcealed':key!='fire','icon_item':{'item':item},'pageCount':2})
    pages.append({'key':r,'pages':[{'type':'TEXT','text':r+'1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[item]}}]})
    display.append({'key':item,'class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':r,'instability':1,'aspects':costs,'input':stack('minecraft:glass_pane'),'output':stack(item),'ingredients':{'kind':'sequence','elements':[stack(i) for i in ingredients]}}})
    for language in manifest['languages'].values():
        language[f'item.{MOD}.lens_{key}']=f'item.Lens{old}.name'
        for name in [r+'1','tc.research_name.'+r,'tc.research_text.'+r]:language[name]=name
write(f'assets/{MOD}/models/item/lens_case.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/lenscase'}})
manifest['mapping'][f'assets/{MOD}/textures/item/lenscase.png']='assets/thaumichorizons/textures/items/lenscase.png'
write('data/curios/tags/items/belt.json',{'replace':False,'values':[f'{MOD}:lens_case']})
costs={'terra':10,'ordo':10,'perditio':10};item=f'{MOD}:lens_case'
write(f'data/{MOD}/recipes/arcane/lens_case.json',{'type':'thaumcraft:arcane','research':'lensCase','pattern':['LGL','LBL','LLL'],'key':{'L':{'item':'minecraft:leather'},'G':{'item':'minecraft:iron_ingot'},'B':{'item':'thaumcraft:mundane_belt'}},'vis':costs,'result':{'item':item}})
research.append({'key':'lensCase','category':'ThaumicHorizons','displayColumn':2,'displayRow':-6,'complexity':1,'aspects':[{'aspect':a,'amount':n} for a,n in {'vitreus':2,'pannus':4,'vacuos':2}.items()],'parents':['lensAir','lensWater','lensEarth','lensOrderEntropy','FOCUSPOUCH'],'isSecondary':True,'isConcealed':True,'icon_item':{'item':item},'pageCount':2})
pages.append({'key':'lensCase','pages':[{'type':'TEXT','text':'lensCase1'},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.ShapedArcaneRecipe','keys':[item]}}]})
display.append({'key':item,'class':'thaumcraft.api.crafting.ShapedArcaneRecipe','data':{'kind':'arcane_shaped','research':'lensCase','aspects':costs,'width':3,'height':3,'mirrored':True,'output':stack(item),'ingredients':{'kind':'sequence','elements':[stack(i) for i in ['minecraft:leather','minecraft:iron_ingot','minecraft:leather','minecraft:leather','thaumcraft:mundane_belt','minecraft:leather','minecraft:leather','minecraft:leather','minecraft:leather']]}}})
for language in manifest['languages'].values():
    language[f'item.{MOD}.lens_case']='item.lensCase.name'
    for name in ['lensCase1','tc.research_name.lensCase','tc.research_text.lensCase']:language[name]=name
write(path,manifest)
write(f'data/{MOD}/research_catalog/lenses.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':research}]})
write(f'data/{MOD}/research_pages/lenses.json',{'format':1,'entries':pages});write(f'data/{MOD}/research_page_recipes/lenses.json',{'format':1,'entries':display})
for language,values in [('zh_cn',['选择镜片','卸下镜片','没有可用镜片']),('en_us',['Choose lens','Remove lens','No available lenses'])]:
    path=f'assets/{MOD}/lang/{language}.json';data=read(path)
    for key,value in zip(['key.thaumichorizons.lenses',f'{MOD}.lens.remove',f'{MOD}.lens.none'],values):data[key]=value
    write(path,data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
