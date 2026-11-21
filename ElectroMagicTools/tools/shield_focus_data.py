from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def ingredient(id):return {'ingredient':{'item':id},'damage':32767}
key='emt:shield_focus'
write('data/emt/recipes/shield_focus.json',dict(type='thaumcraft:infusion',research=key,instability=4,aspects={'aer':16,'tutamen':20,'vitreus':16,'vinculum':20,'victus':16},center=ingredient('thaumcraft:focus_portable_hole'),components=[ingredient(id) for id in ['ic2:reinforced_stone','ic2:reinforced_glass','ic2:reinforced_stone','ic2:reinforced_glass','minecraft:diamond_block','minecraft:obsidian','minecraft:obsidian']],result={'item':key}))
write('assets/emt/models/item/shield_focus.json',dict(parent='minecraft:item/generated',textures={'layer0':'emt:items/focus_shield'}))
write('data/emt/research_catalog/shield_focus.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=[dict(key=key,category='EMT',displayColumn=4,displayRow=3,pageCount=2,complexity=2,aspects={'tutamen':5,'aer':2,'vitreus':3,'vinculum':2},parents=['emt:foundation'],hiddenParents=['INFUSION'],isConcealed=True,icon_item={'item':key})])]))
write('data/emt/research_pages/shield_focus.json',dict(format=1,entries=[dict(key=key,pages=[dict(type='TEXT',text='emt.text.Shield Focus'),dict(type='INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[key]))])]))
path=root/'data/emt/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/emt/lang/'+lang+'.json'].update({'item.emt.shield_focus':'item.EMT.focus.shield.name','tc.research_name.'+key:'emt.rsname.Shield Focus','tc.research_text.'+key:'emt.tag.Shield Focus'})
write('data/emt/thaumcraft/original_resources.json',data)
exec((Path(__file__).parent/'energy_data.py').read_text(),{'__file__':str(Path(__file__).parent/'energy_data.py')})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
