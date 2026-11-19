from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
key='emt:portable_node';old='Portable Node'
write('data/emt/recipes/portable_node.json',dict(type='thaumcraft:crucible',research=key,catalyst={'item':'thaumcraft:node_jar'},aspects={'praecantatio':32,'auram':32,'lucrum':32},result={'item':key}))
write('data/emt/research_catalog/portable_node.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=[dict(key=key,category='EMT',displayColumn=2,displayRow=5,pageCount=2,complexity=3,aspects={'praecantatio':5,'auram':5,'lucrum':5},parents=['emt:foundation'],isConcealed=True,icon_item={'item':key})])]))
write('data/emt/research_pages/portable_node.json',dict(format=1,entries=[dict(key=key,pages=[dict(type='TEXT',text='emt.text.'+old),dict(type='CRUCIBLE_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[key]))])]))
write('assets/emt/models/item/portable_node.json',dict(parent='minecraft:item/generated',textures={'layer0':'emt:blocks/portablenode'}))
write('assets/emt/models/block/portable_node.json',dict(textures={'particle':'emt:blocks/portablenode'}))
write('assets/emt/blockstates/portable_node.json',{'variants':{'':{'model':'emt:block/portable_node'}}})
path=root/'data/emt/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/emt/lang/'+lang+'.json'].update({'block.emt.portable_node':'tile.EMT.portablenode.name','tc.research_name.'+key:'emt.rsname.'+old,'tc.research_text.'+key:'emt.tag.'+old})
write('data/emt/thaumcraft/original_resources.json',data)
exec((Path(__file__).parent/'energy_data.py').read_text(),{'__file__':str(Path(__file__).parent/'energy_data.py')})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
