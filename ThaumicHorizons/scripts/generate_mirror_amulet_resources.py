import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,value):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1,'metadata':0,'damageable':False}}
write(f'assets/{MOD}/models/item/mirror_amulet.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/amuletmirror'}})
write('data/curios/tags/items/necklace.json',{'replace':False,'values':[f'{MOD}:mirror_amulet']})
materials=['thaumcraft:mundane_amulet','minecraft:map','thaumcraft:item_mirror'];aspects={'mortuus':16,'perditio':32,'vacuos':32}
write(f'data/{MOD}/recipes/infusion/mirror_amulet.json',{'type':'thaumcraft:infusion','research':'mirrorAmulet','instability':6,'aspects':aspects,'center':{'ingredient':{'item':'thaumcraft:primordial_pearl'}},'components':[{'ingredient':{'item':i}} for i in materials],'result':{'item':f'{MOD}:mirror_amulet'}})
write(f'data/{MOD}/research_catalog/mirror_amulet.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[{'key':'mirrorAmulet','category':'ThaumicHorizons','displayColumn':17,'displayRow':4,'complexity':2,'aspects':[{'aspect':a,'amount':5} for a in ['mortuus','perditio','vacuos']],'parents':['cloneSelf'],'parentsHidden':['MIRROR'],'isConcealed':True,'icon_item':{'item':f'{MOD}:mirror_amulet'},'pageCount':2}]}]})
write(f'data/{MOD}/research_pages/mirror_amulet.json',{'format':1,'entries':[{'key':'mirrorAmulet','pages':[{'type':'TEXT','text':'mirrorAmulet1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:mirror_amulet']}}]}]})
write(f'data/{MOD}/research_page_recipes/mirror_amulet.json',{'format':1,'entries':[{'key':f'{MOD}:mirror_amulet','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'mirrorAmulet','instability':6,'aspects':aspects,'input':stack('thaumcraft:primordial_pearl'),'output':stack(f'{MOD}:mirror_amulet'),'ingredients':{'kind':'sequence','elements':[stack(i) for i in materials]}}}]})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(path);manifest['mapping'][f'assets/{MOD}/textures/item/amuletmirror.png']='assets/thaumichorizons/textures/items/amuletmirror.png'
for language in manifest['languages'].values():
    language[f'item.{MOD}.mirror_amulet']='item.amuletMirror.name'
    for key in ['mirrorAmulet1','tc.research_name.mirrorAmulet','tc.research_text.mirrorAmulet']:language[key]=key
write(path,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
