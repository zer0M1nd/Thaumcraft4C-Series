import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,v):
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(i):return {'kind':'stack','stack':{'item':i,'count':1,'metadata':0,'damageable':False}}
item=f'{MOD}:focus_disintegration';research='focusDisintegration';cost={'perditio':32,'praecantatio':24,'vitreus':32,'permutatio':16};ingredients=['thaumcraft:liquid_death_bucket','thaumcraft:primal_charm','thaumcraft:liquid_death_bucket','thaumcraft:alumentum']
write(f'assets/{MOD}/models/item/focus_disintegration.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/focus_disintegration'}})
write(f'data/{MOD}/recipes/infusion/focus_disintegration.json',{'type':'thaumcraft:infusion','research':research,'instability':7,'aspects':cost,'center':{'ingredient':{'item':'thaumcraft:focus_excavation'}},'components':[{'ingredient':{'item':i}} for i in ingredients],'result':{'item':item}})
write(f'data/{MOD}/research_catalog/disintegration.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[{'key':research,'category':'ThaumicHorizons','displayColumn':3,'displayRow':8,'complexity':2,'aspects':[{'aspect':a,'amount':n} for a,n in {'praecantatio':3,'vitreus':2,'perditio':3}.items()],'parents':['focusLiquefaction','alchemite','LIQUIDDEATH'],'isConcealed':True,'icon_item':{'item':item},'pageCount':2}]}]})
write(f'data/{MOD}/research_pages/disintegration.json',{'format':1,'entries':[{'key':research,'pages':[{'type':'TEXT','text':research+'1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[item]}}]}]})
write(f'data/{MOD}/research_page_recipes/disintegration.json',{'format':1,'entries':[{'key':item,'class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':research,'instability':7,'aspects':cost,'input':stack('thaumcraft:focus_excavation'),'output':stack(item),'ingredients':{'kind':'sequence','elements':[stack(i) for i in ingredients]}}}]})
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(p);manifest['mapping'][f'assets/{MOD}/textures/item/focus_disintegration.png']='assets/thaumichorizons/textures/items/focus_disintegration.png'
u=f'data/{MOD}/thaumcraft/focus_upgrades.json';upgrades=[v for v in read(u) if v['key'] not in [f'{MOD}:enervation']]
for key,aspect,ranks in [('enervation','mortuus',[5])]:
 upgrades.append({'key':f'{MOD}:{key}','aspects':[aspect],'texture':f'{MOD}:textures/foci/{key}.png','name':f'focus.upgrade.{key}.name','description':f'focus.upgrade.{key}.text','foci':[item],'ranks':ranks});manifest['mapping'][f'assets/{MOD}/textures/foci/{key}.png']=f'assets/thaumichorizons/textures/foci/{key}.png'
 for lang in manifest['languages'].values():
  for suffix in ['name','text']:lang[f'focus.upgrade.{key}.{suffix}']=f'focus.upgrade.{key}.{suffix}'
for lang in manifest['languages'].values():
 lang[f'item.{MOD}.focus_disintegration']='item.focusDisintegration.name'
 for key in [research+'1','tc.research_name.'+research,'tc.research_text.'+research]:lang[key]=key
write(p,manifest);write(u,upgrades)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
