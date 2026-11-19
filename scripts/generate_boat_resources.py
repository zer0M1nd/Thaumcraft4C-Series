import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,v):
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(i):return {'kind':'stack','stack':{'item':i,'count':1,'metadata':0,'damageable':False}} if i else None
research=[];pages=[];display=[];p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(p)
for name,x,points,cost,pattern,key,parents in [('greatwood',5,{'aqua':1,'iter':1,'fabrico':1},{'ordo':5,'terra':5,'aqua':5},['G G','GGG'],{'G':'thaumcraft:greatwood_log'},[]),('thaumium',7,{'ignis':2,'metallum':2,'praecantatio':2},{'ignis':10,'aqua':10,'ordo':10,'terra':10},['STS','TBT','STS'],{'S':'thaumcraft:water_shard','T':'thaumcraft:thaumium_ingot','B':f'{MOD}:boat_greatwood'},['greatwoodBoat','THAUMIUM'])]:
 item=f'{MOD}:boat_{name}';r=name+'Boat'
 write(f'assets/{MOD}/models/item/boat_{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/boat_{name}'}})
 manifest['mapping'][f'assets/{MOD}/textures/item/boat_{name}.png']=f'assets/thaumichorizons/textures/items/boat{name}.png';manifest['mapping'][f'assets/{MOD}/textures/entity/boat{name}.png']=f'assets/thaumichorizons/textures/entity/boat{name}.png'
 write(f'data/{MOD}/recipes/arcane/boat_{name}.json',{'type':'thaumcraft:arcane','research':r,'pattern':pattern,'key':{k:{'item':v} for k,v in key.items()},'vis':cost,'result':{'item':item}})
 research.append({'key':r,'category':'ThaumicHorizons','displayColumn':x,'displayRow':8,'complexity':1,'aspects':[{'aspect':a,'amount':n} for a,n in points.items()],'parents':parents,'isSecondary':name=='greatwood','icon_item':{'item':item},'pageCount':2})
 pages.append({'key':r,'pages':[{'type':'TEXT','text':r+'1'},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.ShapedArcaneRecipe','keys':[item]}}]})
 display.append({'key':item,'class':'thaumcraft.api.crafting.ShapedArcaneRecipe','data':{'kind':'arcane_shaped','research':r,'aspects':cost,'width':3,'height':len(pattern),'mirrored':True,'output':stack(item),'ingredients':{'kind':'sequence','elements':[stack(key.get(k)) for row in pattern for k in row]}}})
 for lang in manifest['languages'].values():
  lang[f'item.{MOD}.boat_{name}']='item.boat'+name.title()+'.name'
  for text in [r+'1','tc.research_name.'+r,'tc.research_text.'+r]:lang[text]=text
write(p,manifest)
write(f'data/{MOD}/research_catalog/boats.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':research}]});write(f'data/{MOD}/research_pages/boats.json',{'format':1,'entries':pages});write(f'data/{MOD}/research_page_recipes/boats.json',{'format':1,'entries':display})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
