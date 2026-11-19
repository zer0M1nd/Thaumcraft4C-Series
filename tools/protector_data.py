from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def stack(item,potion=None):
 s=dict(item=item,count=1)
 if potion:s['nbt']='{Potion:"minecraft:'+potion+'"}'
 return dict(kind='stack',stack=s)
research='gadomancy:block_protector'; recipes=[]
for i,(left,right) in enumerate([('long_weakness','long_weakness'),('weakness','weakness'),('long_weakness','weakness'),('weakness','long_weakness')]):
 name='block_protector_'+str(i);ingredients={'N':'thaumcraft:nitor','P':'thaumcraft:essentia_tube','J':'thaumcraft:essentia_jar','G':'thaumcraft:greatwood_planks','R':'minecraft:redstone'}
 key={k:dict(item=v) for k,v in ingredients.items()}
 for symbol,potion in [('W',left),('V',right)]:key[symbol]=dict(type='forge:partial_nbt',item='minecraft:potion',nbt={'Potion':'minecraft:'+potion})
 write('data/gadomancy/recipes/'+name+'.json',dict(type='thaumcraft:arcane',mode='shaped',research=research,pattern=['WNV','PJP','GRG'],key=key,vis={'ordo':120,'terra':120},result={'item':'gadomancy:block_protector'}))
 slots=[]
 for symbol in 'WNVPJPGRG':slots.append(stack('minecraft:potion',left if symbol=='W' else right) if symbol in 'WV' else stack(ingredients[symbol]))
 recipes.append(dict(key='gadomancy:'+name,**{'class':'modern'},data=dict(kind='arcane_shaped',research=research,aspects={'ordo':120,'terra':120},width=3,height=3,output=stack('gadomancy:block_protector'),ingredients=dict(kind='sequence',elements=slots))))
write('data/gadomancy/research_catalog/protector.json',dict(format=1,categories=[dict(key='GADOMANCY',icon='gadomancy:textures/items/category_icon.png',background='gadomancy:textures/gui/gui_researchback.png',research=[dict(key=research,category='GADOMANCY',displayColumn=-6,displayRow=-2,pageCount=4,complexity=2,aspects={'ordo':10,'tutamen':8,'terra':12,'lux':10,'auram':8},parents=['gadomancy:sticky_jar'],isConcealed=True,icon_item={'item':'gadomancy:block_protector'})])]))
write('data/gadomancy/research_pages/protector.json',dict(format=1,entries=[dict(key=research,pages=[dict(type='TEXT',text='gadomancy.research_page.BLOCK_PROTECTOR.1'),dict(type='ARCANE_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[r['key'] for r in recipes])),dict(type='TEXT',text='gadomancy.research_page.BLOCK_PROTECTOR.3'),dict(type='TEXT',text='gadomancy.research_page.BLOCK_PROTECTOR.4')])]))
write('data/gadomancy/research_page_recipes/protector.json',dict(format=1,entries=recipes))
write('assets/gadomancy/blockstates/block_protector.json',{'variants':{'':{'model':'gadomancy:block/block_protector'}}})
write('assets/gadomancy/models/block/block_protector.json',{'textures':{'particle':'thaumcraft:block/pipe_1'}})
write('assets/gadomancy/models/item/block_protector.json',json.loads((root/'assets/gadomancy/models/item/arcane_dropper.json').read_text()))
write('data/gadomancy/loot_tables/blocks/block_protector.json',dict(type='minecraft:block',pools=[dict(rolls=1,entries=[dict(type='minecraft:item',name='gadomancy:block_protector')],conditions=[dict(condition='minecraft:survives_explosion')])]))
write('data/minecraft/tags/blocks/mineable/pickaxe.json',{'replace':False,'values':['gadomancy:block_protector','gadomancy:arcane_dropper']})
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/gadomancy/lang/'+lang+'.json'].update({'block.gadomancy.block_protector':'tile.BlockStoneMachine.2.name','tc.research_name.'+research:'tc.research_name.GADOMANCY.BLOCK_PROTECTOR','tc.research_text.'+research:'tc.research_text.GADOMANCY.BLOCK_PROTECTOR'})
write('data/gadomancy/thaumcraft/original_resources.json',data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
