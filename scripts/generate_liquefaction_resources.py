import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,v):
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(i):return {'kind':'stack','stack':{'item':i,'count':1,'metadata':0,'damageable':False}}
item=f'{MOD}:focus_liquefaction';research='focusLiquefaction';cost={'ignis':12,'metallum':8,'permutatio':4};ingredients=['thaumcraft:focus_fire','thaumcraft:salis_mundus','thaumcraft:focus_excavation']
write(f'assets/{MOD}/models/item/focus_liquefaction.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/focus_liquefaction'}})
write(f'data/{MOD}/recipes/infusion/focus_liquefaction.json',{'type':'thaumcraft:infusion','research':research,'instability':3,'aspects':cost,'center':{'ingredient':{'item':'minecraft:diamond'}},'components':[{'ingredient':{'item':i}} for i in ingredients],'result':{'item':item}})
write(f'data/{MOD}/research_catalog/liquefaction.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[{'key':research,'category':'ThaumicHorizons','displayColumn':1,'displayRow':8,'complexity':2,'aspects':[{'aspect':a,'amount':n} for a,n in {'ignis':3,'metallum':3,'permutatio':2}.items()],'parents':['focusIllumination','FOCUSEXCAVATION'],'isConcealed':True,'icon_item':{'item':item},'pageCount':2}]}]})
write(f'data/{MOD}/research_pages/liquefaction.json',{'format':1,'entries':[{'key':research,'pages':[{'type':'TEXT','text':research+'1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[item]}}]}]})
write(f'data/{MOD}/research_page_recipes/liquefaction.json',{'format':1,'entries':[{'key':item,'class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':research,'instability':3,'aspects':cost,'input':stack('minecraft:diamond'),'output':stack(item),'ingredients':{'kind':'sequence','elements':[stack(i) for i in ingredients]}}}]})
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(p);manifest['mapping'][f'assets/{MOD}/textures/item/focus_liquefaction.png']='assets/thaumichorizons/textures/items/focus_liquefaction.png'
u=f'data/{MOD}/thaumcraft/focus_upgrades.json';upgrades=[v for v in read(u) if v['key'] not in [f'{MOD}:nuggets',f'{MOD}:purity']]
for key,aspect,ranks in [('nuggets','metallum',[1,2,4]),('purity','ordo',[5])]:
 upgrades.append({'key':f'{MOD}:{key}','aspects':[aspect],'texture':f'{MOD}:textures/foci/{key}.png','name':f'focus.upgrade.{key}.name','description':f'focus.upgrade.{key}.text','foci':[item],'ranks':ranks});manifest['mapping'][f'assets/{MOD}/textures/foci/{key}.png']=f'assets/thaumichorizons/textures/foci/{key}.png'
 for lang in manifest['languages'].values():
  for suffix in ['name','text']:lang[f'focus.upgrade.{key}.{suffix}']=f'focus.upgrade.{key}.{suffix}'
for lang in manifest['languages'].values():
 lang[f'item.{MOD}.focus_liquefaction']='item.focusLiquefaction.name'
 for key in [research+'1','tc.research_name.'+research,'tc.research_text.'+research]:lang[key]=key
write(p,manifest);write(u,upgrades)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
