from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
key='emt:electric_cloud'
write('assets/emt/models/item/electric_cloud.json',{'parent':'minecraft:item/generated','textures':{'layer0':'emt:blocks/electric_cloud'}})
write('assets/emt/blockstates/electric_cloud.json',{'variants':{'':{'model':'emt:block/electric_cloud'}}})
write('assets/emt/models/block/electric_cloud.json',{'textures':{'particle':'emt:blocks/empty'}})
write('data/emt/loot_tables/blocks/electric_cloud.json',dict(type='minecraft:block',pools=[dict(rolls=1,entries=[dict(type='minecraft:item',name=key)],conditions=[dict(condition='minecraft:survives_explosion')])]))
write('data/emt/research_catalog/cloud.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=[dict(key=key,category='EMT',displayColumn=4,displayRow=-7,pageCount=2,complexity=0,aspects={},isRound=True,isAutoUnlock=True,icon_item={'item':key})])]))
write('data/emt/research_pages/cloud.json',dict(format=1,entries=[dict(key=key,pages=[dict(type='TEXT',text='emt.text.Electric Cloud.1'),dict(type='TEXT',text='emt.text.Electric Cloud.2')])]))
path=root/'data/emt/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/emt/lang/'+lang+'.json'].update({'block.emt.electric_cloud':'tile.EMT.electricCloud.name','tc.research_name.'+key:'emt.rsname.Electric Cloud','tc.research_text.'+key:'emt.tag.Electric Cloud'})
write('data/emt/thaumcraft/original_resources.json',data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
