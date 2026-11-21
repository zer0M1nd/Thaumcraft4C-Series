from pathlib import Path
import json
base=Path(__file__).resolve().parents[1];root=base/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def stack(item):return dict(kind='stack',stack=dict(item=item,count=1))
key='gadomancy:infusion_claw'
items=['thaumcraft:focus_primal','thaumcraft:arcane_stone','thaumcraft:golem_core_use','thaumcraft:void_ingot','thaumcraft:arcane_stone','thaumcraft:primal_charm','thaumcraft:arcane_stone','thaumcraft:zombie_brain','thaumcraft:void_ingot','thaumcraft:arcane_stone']
aspects=dict(alienis=25,machina=20,praecantatio=16,ordo=20,tenebrae=12)
write('data/gadomancy/recipes/infusion_claw.json',dict(type='thaumcraft:infusion',research=key,instability=10,center=dict(ingredient=dict(item='thaumcraft:wand_pedestal')),components=[dict(ingredient=dict(item=item)) for item in items],aspects=aspects,result=dict(item=key)))
write('data/gadomancy/research_catalog/infusion_claw.json',dict(format=1,categories=[dict(key='GADOMANCY',icon='gadomancy:textures/items/category_icon.png',background='gadomancy:textures/gui/gui_researchback.png',research=[dict(key=key,category='GADOMANCY',displayColumn=-6,displayRow=-7,pageCount=3,complexity=5,aspects=dict(alienis=8,machina=8,praecantatio=8,ordo=8,tenebrae=4),parents=['FOCUSPRIMAL','VOIDMETAL','WANDPED','COREUSE'],isConcealed=True,icon_item={'item':key})])]))
write('data/gadomancy/research_pages/infusion_claw.json',dict(format=1,entries=[dict(key=key,pages=[dict(type='TEXT',text='gadomancy.research_page.INFUSIONCLAW.1'),dict(type='INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[key])),dict(type='TEXT',text='gadomancy.research_page.INFUSIONCLAW.2')])]))
write('data/gadomancy/research_page_recipes/infusion_claw.json',dict(format=1,entries=[dict(key=key,**{'class':'modern'},data=dict(kind='infusion',research=key,input=stack('thaumcraft:wand_pedestal'),output=stack(key),ingredients=dict(kind='sequence',elements=[stack(item) for item in items]),aspects=aspects,instability=10))]))
write('assets/gadomancy/blockstates/infusion_claw.json',{'variants':{'':{'model':'gadomancy:block/infusion_claw'}}})
write('assets/gadomancy/models/block/infusion_claw.json',{'textures':{'particle':'thaumcraft:block/pedestal_top'}})
write('assets/gadomancy/models/item/infusion_claw.json',json.loads((root/'assets/gadomancy/models/item/arcane_packager.json').read_text()))
write('data/gadomancy/loot_tables/blocks/infusion_claw.json',dict(type='minecraft:block',pools=[dict(rolls=1,entries=[dict(type='minecraft:item',name=key)],conditions=[dict(condition='minecraft:survives_explosion')])]))
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/gadomancy/lang/'+lang+'.json'].update({'block.gadomancy.infusion_claw':'tile.BlockInfusionClaw.name','tc.research_name.'+key:'tc.research_name.GADOMANCY.INFUSIONCLAW','tc.research_text.'+key:'tc.research_text.GADOMANCY.INFUSIONCLAW'})
write('data/gadomancy/thaumcraft/original_resources.json',data)
path=root/'data/minecraft/tags/blocks/mineable/pickaxe.json';tag=json.loads(path.read_text());tag['values']=sorted(set(tag['values']+[key]));write('data/minecraft/tags/blocks/mineable/pickaxe.json',tag)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
