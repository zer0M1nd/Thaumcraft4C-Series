"""Complete prerequisite chain and handbook recipe displays for blood infusion."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,data):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(item,count=1):return {'kind':'stack','stack':{'item':item,'count':count,'metadata':0,'damageable':False}}
def sequence(items):return {'kind':'sequence','elements':items}
displays=[];research=[];pages=[]
def crucible(key,name,catalyst,result,count,aspects):
    write(f'data/{MOD}/recipes/crucible/{name}.json',{'type':'thaumcraft:crucible','research':key,'aspects':aspects,'catalyst':{'item':catalyst},'result':{'item':result,'count':count}})
    displays.append({'key':f'{MOD}:{name}','class':'thaumcraft.api.crafting.CrucibleRecipe','data':{'kind':'crucible','research':key,'aspects':aspects,'catalyst':stack(catalyst),'output':stack(result,count)}});return name
def text(key):return {'type':'TEXT','text':key}
def ref(kind,keys):return {'type':{'crucible':'CRUCIBLE_CRAFTING','infusion':'INFUSION_CRAFTING','arcane':'ARCANE_CRAFTING'}[kind],'recipe':{'kind':'reference','class':{'crucible':'thaumcraft.api.crafting.CrucibleRecipe','infusion':'thaumcraft.api.crafting.InfusionRecipe','arcane':('thaumcraft.api.crafting.ShapelessArcaneRecipe' if keys==['blood_phial'] else 'thaumcraft.api.crafting.ShapedArcaneRecipe')}[kind],'keys':[f'{MOD}:{k}' for k in keys]}}
def entry(key,item,x,y,complexity,aspects,content,parents=(),hidden=(),secondary=False,concealed=False,warp=0):
    research.append({'key':key,'category':'ThaumicHorizons','displayColumn':x,'displayRow':y,'complexity':complexity,'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()],'parents':list(parents),'parentsHidden':list(hidden),'isSecondary':secondary,'isConcealed':concealed,'warp':warp,'icon_item':{'item':item},'pageCount':len(content)})
    pages.append({'key':key,'pages':content})
farm=[]
for output,catalyst,count,cost in [('wheat','wheat_seeds',2,2),('carrot','carrot',2,2),('potato','potato',2,2),('melon','melon_seeds',1,3),('pumpkin','pumpkin_seeds',1,3)]:farm.append(crucible('alcheponics','alcheponics_'+output,'minecraft:'+catalyst,'minecraft:'+output,count,{'lux':cost,'terra':cost,'aqua':cost}))
entry('alcheponics','minecraft:wheat',0,0,1,{'messis':5,'herba':5,'lux':5},[text('alcheponics1'),ref('crucible',farm[:1]),text('alcheponics2')]+[ref('crucible',[r]) for r in farm[1:]])
crucible('alchIncubation','incubated_egg','minecraft:egg',f'{MOD}:incubated_egg',1,{'ignis':2,'victus':2})
entry('alchIncubation',f'{MOD}:incubated_egg',2,0,1,{'bestia':4,'victus':2,'ignis':2},[text('alchIncubation1'),ref('crucible',['incubated_egg'])],['alcheponics'],secondary=True)
meats=[crucible('meatGrowth','meat_growth_'+meat,'minecraft:'+meat,'minecraft:'+meat,2,{'victus':3}) for meat in ['porkchop','beef','chicken']]
entry('meatGrowth','minecraft:beef',4,0,2,{'corpus':6,'victus':4,'bestia':4,'fabrico':4},[text('meatGrowth1'),ref('crucible',meats)],['alchIncubation'])
leathers=[crucible('fleshLeather','leather_'+meat,'minecraft:'+meat,'minecraft:leather',1,dict(fabrico=1,pannus=1,**({'ordo':1} if meat=='rotten_flesh' else {}))) for meat in ['beef','porkchop','rotten_flesh']]
entry('fleshLeather','minecraft:leather',6,-1,1,{'corpus':4,'pannus':6,'fabrico':4},[text('fleshLeather1'),ref('crucible',leathers)],['meatGrowth'],secondary=True)
# Crafting and display data are derived from the same authored recipe definitions.
for folder,name in [('arcane','empty_syringe'),('infusion','blood_infuser'),('arcane','blood_phial')]:
    recipe=json.loads((ROOT/f'data/{MOD}/recipes/{folder}/{name}.json').read_text(encoding='utf-8'))
    result=recipe['result'];data={'kind':'infusion' if folder=='infusion' else ('arcane_shapeless' if recipe.get('mode')=='shapeless' else 'arcane_shaped'),'research':recipe['research'],'output':stack(result['item'],result.get('count',1)),'aspects':recipe.get('aspects',recipe.get('vis',{}))}
    if folder=='infusion':data.update(input=stack(recipe['center']['ingredient']['item']),ingredients=sequence([stack(c['ingredient']['item']) for c in recipe['components']]),instability=recipe['instability'])
    elif recipe.get('mode')=='shapeless':data.update(ingredients=sequence([stack(c['item']) for c in recipe['ingredients']]))
    else:data.update(width=len(recipe['pattern'][0]),height=len(recipe['pattern']),mirrored=True,ingredients=sequence([stack(recipe['key'][c]['item']) if c!=' ' else None for row in recipe['pattern'] for c in row]))
    displays.append({'key':f'{MOD}:{name}','class':'thaumcraft.api.crafting.InfusionRecipe' if folder=='infusion' else 'thaumcraft.api.crafting.ShapelessArcaneRecipe' if recipe.get('mode')=='shapeless' else 'thaumcraft.api.crafting.ShapedArcaneRecipe','data':data})
entry('essentiaInjection',f'{MOD}:human_blood',6,1,3,{'victus':4,'humanus':2,'permutatio':2,'praecantatio':2},[text('essentiaInjection1'),ref('arcane',['empty_syringe']),text('essentiaInjection2'),ref('infusion',['blood_infuser']),text('essentiaInjection3'),text('essentiaInjection4')],['meatGrowth'],['THAUMATORIUM','INFUSION'],warp=1)
entry('blastPhial',f'{MOD}:blood_phial',6,3,1,{'victus':2,'ignis':2,'perditio':2},[text('blastPhial1'),ref('arcane',['blood_phial'])],['essentiaInjection'],secondary=True,concealed=True)
write(f'data/{MOD}/research_catalog/blood.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':research}]})
write(f'data/{MOD}/research_pages/blood.json',{'format':1,'entries':pages})
write(f'data/{MOD}/research_page_recipes/blood.json',{'format':1,'entries':displays})
write(f'assets/{MOD}/models/item/incubated_egg.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:items/eggincubated'}})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=json.loads((ROOT/path).read_text(encoding='utf-8'));manifest['mapping'][f'assets/{MOD}/textures/items/eggincubated.png']='assets/thaumichorizons/textures/items/eggincubated.png'
for translations in manifest['languages'].values():
    translations[f'item.{MOD}.incubated_egg']='item.eggIncubated.name'
    for row in research:
        for prefix in ['tc.research_name.','tc.research_text.']:translations[prefix+row['key']]=prefix+row['key']
    for row in pages:
        for page in row['pages']:
            if 'text' in page:translations[page['text']]=page['text']
write(path,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
