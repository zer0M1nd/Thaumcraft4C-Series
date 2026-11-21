import json,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,v):
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(i):return {'kind':'stack','stack':{'item':i,'count':1,'metadata':0,'damageable':False}}
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(p)
with zipfile.ZipFile(Path(__file__).resolve().parents[1]/'references/ThaumicHorizons-1.8.21.jar') as jar:
 names={n.lower():n for n in jar.namelist()}
 for side in ['top','bottom','side']:
  target=f'assets/{MOD}/textures/block/alchemite_{side}.png';manifest['mapping'][target]=names[f'assets/thaumichorizons/textures/blocks/alchemite_{side}.png']
write(f'assets/{MOD}/models/block/alchemite.json',{'parent':'minecraft:block/cube_bottom_top','textures':{s:f'{MOD}:block/alchemite_{s}' for s in ['top','bottom','side']}})
write(f'assets/{MOD}/blockstates/alchemite.json',{'variants':{'':{'model':f'{MOD}:block/alchemite'}}});write(f'assets/{MOD}/models/item/alchemite.json',{'parent':f'{MOD}:block/alchemite'})
item=f'{MOD}:alchemite';cost={'potentia':4,'vitreus':4,'permutatio':4}
write(f'data/{MOD}/loot_tables/blocks/alchemite.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':item}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(f'data/{MOD}/recipes/crucible/alchemite.json',{'type':'thaumcraft:crucible','research':'alchemite','aspects':cost,'catalyst':{'item':'minecraft:tnt'},'result':{'item':item}})
write(f'data/{MOD}/research_catalog/alchemite.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[{'key':'alchemite','category':'ThaumicHorizons','displayColumn':3,'displayRow':6,'complexity':2,'aspects':[{'aspect':a,'amount':n} for a,n in {'ignis':2,'perditio':2,'potentia':4,'vitreus':4,'permutatio':2}.items()],'parentsHidden':['ESSENTIACRYSTAL'],'icon_item':{'item':item},'pageCount':2}]}]})
write(f'data/{MOD}/research_pages/alchemite.json',{'format':1,'entries':[{'key':'alchemite','pages':[{'type':'TEXT','text':'alchemite1'},{'type':'CRUCIBLE_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.CrucibleRecipe','keys':[item]}}]}]})
write(f'data/{MOD}/research_page_recipes/alchemite.json',{'format':1,'entries':[{'key':item,'class':'thaumcraft.api.crafting.CrucibleRecipe','data':{'kind':'crucible','research':'alchemite','aspects':cost,'catalyst':stack('minecraft:tnt'),'output':stack(item)}}]})
for lang in manifest['languages'].values():
 lang[f'block.{MOD}.alchemite']='tile.ThaumicHorizons_alchemite.name'
 for key in ['alchemite1','tc.research_name.alchemite','tc.research_text.alchemite']:lang[key]=key
write(p,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
