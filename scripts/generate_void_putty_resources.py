import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,v):
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(item,damage=0):return {'kind':'stack','stack':{'item':item,'count':1,'metadata':damage,'damageable':damage>0}}
item=f'{MOD}:void_putty';p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(p);manifest['mapping'][f'assets/{MOD}/textures/item/void_putty.png']='assets/thaumichorizons/textures/items/voidputty.png'
for lang in manifest['languages'].values():
 lang[f'item.{MOD}.void_putty']='item.voidPutty.name'
 for key in ['voidPutty1','tc.research_name.voidPutty','tc.research_text.voidPutty']:lang[key]=key
write(p,manifest)
write(f'assets/{MOD}/models/item/void_putty.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/void_putty'}})
write(f'data/{MOD}/recipes/void_putty_repair.json',{'type':f'{MOD}:void_putty_repair','category':'equipment'})
write(f'data/{MOD}/research_catalog/void_putty.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[{'key':'voidPutty','category':'ThaumicHorizons','displayColumn':16,'displayRow':9,'complexity':2,'aspects':[{'aspect':a,'amount':3} for a in ['permutatio','fabrico','metallum']],'parents':['planarRift'],'isConcealed':True,'icon_item':{'item':item},'pageCount':2}]}]})
write(f'data/{MOD}/research_pages/void_putty.json',{'format':1,'entries':[{'key':'voidPutty','pages':[{'type':'TEXT','text':'voidPutty1'},{'type':'NORMAL_CRAFTING','recipe':{'kind':'reference','class':'net.minecraft.item.crafting.ShapelessRecipes','keys':[item]}}]}]})
write(f'data/{MOD}/research_page_recipes/void_putty.json',{'format':1,'entries':[{'key':item,'class':'net.minecraft.item.crafting.ShapelessRecipes','data':{'kind':'crafting_shapeless','ingredients':{'kind':'sequence','elements':[stack('thaumcraft:elemental_sword',32767),stack(item)]},'output':stack('thaumcraft:elemental_sword')}}]})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
