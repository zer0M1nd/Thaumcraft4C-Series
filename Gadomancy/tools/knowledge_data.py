from pathlib import Path
import json,zipfile
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def stack(item):return dict(kind='stack',stack=dict(item=item,count=1))
research='gadomancy:knowledge_book'
items={'S':'thaumcraft:scribing_tools','T':'thaumcraft:thaumonomicon','B':'minecraft:enchanted_book','O':'gadomancy:aura_core_ordo','M':'thaumcraft:mnemonic_matrix','G':'thaumcraft:goggles'}
vis={'ordo':70,'perditio':20,'aer':20,'ignis':30}
write('data/gadomancy/recipes/knowledge_book.json',dict(type='thaumcraft:arcane',mode='shaped',research=research,pattern=['STS','BOB','MGM'],key={key:dict(item=value) for key,value in items.items()},vis=vis,result=dict(item='gadomancy:knowledge_book')))
write('data/gadomancy/research_catalog/knowledge_book.json',dict(format=1,categories=[dict(key='GADOMANCY',icon='gadomancy:textures/items/category_icon.png',background='gadomancy:textures/gui/gui_researchback.png',research=[dict(key=research,category='GADOMANCY',displayColumn=-2,displayRow=8,pageCount=4,complexity=2,warp=1,aspects={'cognitio':12,'praecantatio':8,'motus':6,'ordo':10,'machina':8,'fabrico':8},parents=['gadomancy:aura_core','GOGGLES','THAUMATORIUM'],isHidden=True,itemTriggers=[dict(item='gadomancy:aura_core_ordo',count=1)],icon_item={'item':'gadomancy:knowledge_book'})])]))
write('data/gadomancy/research_pages/knowledge_book.json',dict(format=1,entries=[dict(key=research,pages=[dict(type='TEXT',text='gadomancy.research_page.KNOWLEDGE_BOOK.1'),dict(type='ARCANE_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[research])),dict(type='TEXT',text='gadomancy.research_page.KNOWLEDGE_BOOK.3'),dict(type='TEXT',text='gadomancy.research_page.KNOWLEDGE_BOOK.4')])]))
write('data/gadomancy/research_page_recipes/knowledge_book.json',dict(format=1,entries=[dict(key=research,**{'class':'modern'},data=dict(kind='arcane_shaped',research=research,aspects=vis,width=3,height=3,output=stack('gadomancy:knowledge_book'),ingredients=dict(kind='sequence',elements=[stack(items[key]) for key in 'STSBOBMGM']))) ]))
write('assets/gadomancy/blockstates/knowledge_book.json',{'variants':{'':{'model':'gadomancy:block/knowledge_book'}}})
write('assets/gadomancy/models/block/knowledge_book.json',{'textures':{'particle':'minecraft:block/white_wool'}})
write('assets/gadomancy/models/item/knowledge_book.json',{'parent':'builtin/entity','gui_light':'front','display':{'gui':{'rotation':[30,225,0],'scale':[.625,.625,.625]},'ground':{'translation':[0,3,0],'scale':[.25,.25,.25]},'fixed':{'rotation':[0,180,0],'scale':[.5,.5,.5]},'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375,.375,.375]},'firstperson_righthand':{'rotation':[0,45,0],'scale':[.4,.4,.4]}}})
write('data/gadomancy/loot_tables/blocks/knowledge_book.json',dict(type='minecraft:block',pools=[dict(rolls=1,entries=[dict(type='minecraft:item',name='gadomancy:knowledge_book')],conditions=[dict(condition='minecraft:survives_explosion')])]))
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/gadomancy/lang/'+lang+'.json'].update({'block.gadomancy.knowledge_book':'tile.BlockKnowledgeBook.name','tc.research_name.'+research:'tc.research_name.GADOMANCY.KNOWLEDGE_BOOK','tc.research_text.'+research:'tc.research_text.GADOMANCY.KNOWLEDGE_BOOK'})
write('data/gadomancy/thaumcraft/original_resources.json',data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
