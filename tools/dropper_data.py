from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1}}
name='arcane_dropper';key='gadomancy:arcane_dropper';result={'item':key}
ingredients={'S':{'item':'thaumcraft:arcane_stone_bricks'},'D':{'item':'minecraft:dropper'},'B':{'item':'thaumcraft:bellows'},'A':{'item':'thaumcraft:alchemical_construct'}}
write('data/gadomancy/recipes/arcane_dropper.json',dict(type='thaumcraft:arcane',mode='shaped',research=key,pattern=['SDS','BAB','SSS'],key=ingredients,vis={'ordo':14,'aer':14,'terra':10},result=result))
write('data/gadomancy/research_catalog/dropper.json',{'format':1,'categories':[{'key':'GADOMANCY','icon':'gadomancy:textures/items/category_icon.png','background':'gadomancy:textures/gui/gui_researchback.png','research':[dict(key=key,category='GADOMANCY',displayColumn=-1,displayRow=-3,pageCount=2,complexity=2,aspects={'ordo':4,'aer':6,'terra':5,'sensus':5,'machina':7},parents=['DISTILESSENTIA','BELLOWS'],isConcealed=True,icon_item=result)]}]})
write('data/gadomancy/research_pages/dropper.json',{'format':1,'entries':[{'key':key,'pages':[{'type':'TEXT','text':'gadomancy.research_page.ARCANEDROPPER.1'},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','class':'modern','keys':[key]}}]}]})
write('data/gadomancy/research_page_recipes/dropper.json',{'format':1,'entries':[{'key':key,'class':'modern','data':{'kind':'arcane_shaped','research':key,'aspects':{'ordo':14,'aer':14,'terra':10},'width':3,'height':3,'output':stack(key),'ingredients':{'kind':'sequence','elements':[stack(ingredients[c]['item']) for c in 'SDSBABSSS']}}}]})
write('assets/gadomancy/blockstates/arcane_dropper.json',{'variants':{'':{'model':'gadomancy:block/arcane_dropper'}}})
write('assets/gadomancy/models/block/arcane_dropper.json',{'textures':{'particle':'thaumcraft:block/pedestal_top'}})
model=json.loads((root/'assets/gadomancy/models/item/arcane_packager.json').read_text());write('assets/gadomancy/models/item/arcane_dropper.json',model)
write('data/gadomancy/loot_tables/blocks/arcane_dropper.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':key}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write('data/minecraft/tags/blocks/mineable/pickaxe.json',{'replace':False,'values':['gadomancy:arcane_dropper','gadomancy:arcane_packager']})
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:
 data.setdefault('languages',{}).setdefault('assets/gadomancy/lang/'+lang+'.json',{}).update({'block.gadomancy.arcane_dropper':'tile.BlockArcaneDropper.name','block.gadomancy.arcane_packager':'tile.BlockStoneMachine.4.name','tc.research_name.gadomancy:arcane_dropper':'tc.research_name.GADOMANCY.ARCANEDROPPER','tc.research_text.gadomancy:arcane_dropper':'tc.research_text.GADOMANCY.ARCANEDROPPER'})
write('data/gadomancy/thaumcraft/original_resources.json',data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
