import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,v):
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(i):return {'kind':'stack','stack':{'item':i,'count':1,'metadata':0,'damageable':False}}
colors='black red green brown blue purple cyan light_gray gray pink lime yellow light_blue magenta orange white'.split()
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(p)
for shade,color in enumerate(colors):
 name='focus_illumination_'+color
 write(f'assets/{MOD}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/{name}'}})
 manifest['mapping'][f'assets/{MOD}/textures/item/{name}.png']=f'assets/thaumichorizons/textures/items/focus_illumination.{shade}.png'
 for lang in manifest['languages'].values():lang[f'item.{MOD}.{name}']=f'item.focusIllumination.{shade}.name'
manifest['mapping'][f'assets/{MOD}/textures/foci/solar.png']='assets/thaumichorizons/textures/foci/solar.png'
for lang in manifest['languages'].values():
 for k in ['focus.upgrade.solar.name','focus.upgrade.solar.text','focusIllumination1','tc.research_name.focusIllumination','tc.research_text.focusIllumination']:lang[k]=k
write(p,manifest)
p=f'data/{MOD}/thaumcraft/focus_upgrades.json';upgrades=[u for u in read(p) if u['key']!=f'{MOD}:solar'];upgrades.append({'key':f'{MOD}:solar','aspects':['ordo','vacuos'],'texture':f'{MOD}:textures/foci/solar.png','name':'focus.upgrade.solar.name','description':'focus.upgrade.solar.text','foci':[f'{MOD}:focus_illumination_'+c for c in colors],'ranks':[3]});write(p,upgrades)
item=f'{MOD}:focus_illumination_yellow';cost={'lux':12,'potentia':8,'ignis':8}
write(f'data/{MOD}/recipes/crucible/focus_illumination.json',{'type':'thaumcraft:crucible','research':'focusIllumination','aspects':cost,'catalyst':{'item':'minecraft:glowstone'},'result':{'item':item}})
write(f'data/{MOD}/recipes/illumination_dye.json',{'type':f'{MOD}:illumination_dye','category':'misc'})
for name in ['light','solar_light']:
 write(f'assets/{MOD}/models/block/{name}.json',{'textures':{'particle':'minecraft:block/glowstone'},'elements':[]})
 write(f'assets/{MOD}/blockstates/{name}.json',{'variants':{'':{'model':f'{MOD}:block/{name}'}}})
write(f'data/{MOD}/research_catalog/illumination.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[{'key':'focusIllumination','category':'ThaumicHorizons','displayColumn':-1,'displayRow':6,'complexity':1,'aspects':[{'aspect':a,'amount':n} for a,n in {'lux':2,'potentia':2,'praecantatio':1}.items()],'parents':['FOCUSFIRE'],'isConcealed':True,'icon_item':{'item':item},'pageCount':2}]}]})
write(f'data/{MOD}/research_pages/illumination.json',{'format':1,'entries':[{'key':'focusIllumination','pages':[{'type':'TEXT','text':'focusIllumination1'},{'type':'CRUCIBLE_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.CrucibleRecipe','keys':[item]}}]}]})
write(f'data/{MOD}/research_page_recipes/illumination.json',{'format':1,'entries':[{'key':item,'class':'thaumcraft.api.crafting.CrucibleRecipe','data':{'kind':'crucible','research':'focusIllumination','aspects':cost,'catalyst':stack('minecraft:glowstone'),'output':stack(item)}}]})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
