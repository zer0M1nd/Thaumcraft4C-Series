from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
id='ethereal_processor';key='emt:'+id;old='Etheral Processor'
write('data/emt/recipes/'+id+'.json',dict(type='thaumcraft:infusion',research=key,instability=3,aspects={'machina':16,'praecantatio':16,'fabrico':16},center={'ingredient':{'item':'ic2:macerator'}},components=[{'ingredient':{'item':item}} for item in ['ic2:electric_furnace','thaumcraft:alchemical_construct','minecraft:end_stone','minecraft:end_stone','minecraft:iron_block','minecraft:iron_block','emt:thaumium_plate','emt:thaumium_plate']],result={'item':key}))
write('data/emt/research_catalog/processor.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=[dict(key=key,category='EMT',displayColumn=-1,displayRow=2,pageCount=2,complexity=2,aspects={'machina':3,'praecantatio':4,'fabrico':5},parents=['emt:foundation'],isConcealed=True,icon_item={'item':key})])]))
write('data/emt/research_pages/processor.json',dict(format=1,entries=[dict(key=key,pages=[dict(type='TEXT',text='emt.text.'+old),dict(type='INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[key]))])]))
for active in [False,True]:write('assets/emt/models/block/'+id+('_active' if active else '')+'.json',dict(parent='minecraft:block/orientable',textures={'top':'emt:blocks/machines/etherealmaceratortop','bottom':'emt:blocks/machines/etherealmacerator','side':'emt:blocks/machines/etherealmacerator','front':'emt:blocks/machines/etherealmaceratorfront'+('active' if active else '')}))
write('assets/emt/blockstates/'+id+'.json',{'variants':{'facing='+face+',lit='+str(active).lower():{'model':'emt:block/'+id+('_active' if active else ''),'y':rot} for face,rot in [('north',0),('east',90),('south',180),('west',270)] for active in [False,True]}})
write('assets/emt/models/item/'+id+'.json',dict(parent='emt:block/'+id))
write('data/emt/loot_tables/blocks/'+id+'.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'ic2:machine'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write('assets/emt/models/item/legacy_tin_nugget.json',dict(parent='minecraft:item/generated',textures={'layer0':'emt:items/legacy_tin_nugget'}))
write('data/forge/tags/items/nuggets/tin.json',dict(replace=False,values=['emt:legacy_tin_nugget']))
write('data/forge/tags/items/nuggets.json',dict(replace=False,values=['emt:legacy_tin_nugget']))
write('data/emt/recipes/legacy_tin_nuggets.json',dict(type='minecraft:crafting_shapeless',ingredients=[{'tag':'forge:ingots/tin'}],result={'item':'emt:legacy_tin_nugget','count':9}))
write('data/emt/recipes/legacy_tin_ingot.json',dict(type='minecraft:crafting_shaped',pattern=['###','###','###'],key={'#':{'tag':'forge:nuggets/tin'}},result={'item':'ic2:tin_ingot'}))
path=root/'data/emt/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/emt/lang/'+lang+'.json'].update({'block.emt.'+id:'tile.EMT.machine.etheralprocessor.name','tc.research_name.'+key:'emt.rsname.'+old,'tc.research_text.'+key:'emt.tag.'+old})
write('data/emt/thaumcraft/original_resources.json',data)
exec((Path(__file__).parent/'energy_data.py').read_text(),{'__file__':str(Path(__file__).parent/'energy_data.py')})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
