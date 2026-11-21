"""Last two original-author material recipes and their actual research pages."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,value):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def item(id,count=1):return dict(item=id,count=count)
uranium=dict(type='thaumcraft:arcane',mode='shapeless',research='emt:tiny_uranium',ingredients=[item('ic2:uranium_238')],vis={a:1 for a in ['aer','aqua','ignis','terra','ordo','perditio']},result=item('ic2:small_uranium_235',7))
plate=dict(type='emt:thaumium_plate',pattern=['X','Y','Z'],key={'X':item('ic2:forge_hammer'),'Y':item('thaumcraft:thaumium_ingot'),'Z':item('minecraft:obsidian')},result=item('emt:thaumium_plate'))
write('data/emt/recipes/tiny_uranium.json',uranium);write('data/emt/recipes/thaumium_plate.json',plate)
catalog=[];pages=[];aliases={}
for id,name,x,y,icon in [('tiny_uranium','Tiny Uranium',-5,-5,'ic2:uranium_238'),('thaumium_plate','Thaumium Plate',6,-6,'emt:thaumium_plate')]:
 key='emt:'+id;entry=dict(key=key,category='EMT',displayColumn=x,displayRow=y,pageCount=2,complexity=1 if id=='tiny_uranium' else 0,aspects={'venenum':4,'mortuus':3,'permutatio':3} if id=='tiny_uranium' else {},icon_item=item(icon))
 if id=='tiny_uranium':entry.update(isSecondary=True,isConcealed=True,parentsHidden=['emt:foundation'])
 else:entry.update(isRound=True,isAutoUnlock=True)
 catalog.append(entry);pages.append(dict(key=key,pages=[dict(type='TEXT',text='emt.text.'+name),dict(type='ARCANE_CRAFTING' if id=='tiny_uranium' else 'NORMAL_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[key]))]))
 aliases['tc.research_name.'+key]='emt.rsname.'+name;aliases['tc.research_text.'+key]='emt.tag.'+name
write('data/emt/research_catalog/legacy_acquisition.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=catalog)]))
write('data/emt/research_pages/legacy_acquisition.json',dict(format=1,entries=pages))
path=root/'data/emt/thaumcraft/original_resources.json';mapping=json.loads(path.read_text(encoding='utf8'))
for lang in ['en_us','zh_cn']:mapping['languages']['assets/emt/lang/'+lang+'.json'].update(aliases)
write('data/emt/thaumcraft/original_resources.json',mapping)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
