"""Author-version Curios rings, external resource aliases and infusion acquisition chains."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,value):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def item(name):return {'item':name}
def stack(value):
 if isinstance(value,list):return dict(kind='sequence',elements=[stack(v) for v in value])
 value=value.get('ingredient',value);return dict(kind='stack',stack=dict(value,count=value.get('count',1)))
catalog=[];pages=[];aliases={};display=[]
for kind,secondary,aspect,y,ingredient,texture in [('inventory',False,'vitreus',-5,'thaumium_drill','inventoryeumaker'),('armor',True,'tutamen',-7,'electric_traveller_boots','armoreumaker')]:
 id=kind+'_charging_ring';key='emt:'+id;name=kind.title()+' Charging Ring'
 aspects={'potentia':64,aspect:32,'praecantatio':48}
 recipe=dict(type='thaumcraft:infusion',research=key,instability=6,aspects=aspects,center={'ingredient':item('thaumcraft:mundane_ring'),'damage':0},components=[{'ingredient':item('emt:'+ingredient),'damage':32767}]+[{'ingredient':item('ic2:'+i)} for i in ['generator','geo_generator','solar_generator','wind_generator','water_generator','nuclear_reactor']],result=item(key))
 write('data/emt/recipes/'+id+'.json',recipe)
 write('assets/emt/models/item/'+id+'.json',dict(parent='minecraft:item/generated',textures={'layer0':'emt:items/'+texture}))
 row=dict(key=key,category='EMT',displayColumn=0,displayRow=y,pageCount=2,complexity=3,aspects={'potentia':4,aspect:4,'praecantatio':4},isConcealed=True,isSecondary=secondary,parentsHidden=['INFUSION'],icon_item=item(key))
 if secondary:row['parents']=['emt:inventory_charging_ring']
 else:row['parentsHidden'].append('emt:foundation')
 catalog.append(row)
 pages.append(dict(key=key,pages=[dict(type='TEXT',text='emt.text.'+name),dict(type='INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[key]))]))
 display.append(dict(key=key,**{'class':'modern'},data=dict(kind='infusion',research=key,output=stack(recipe['result']),input=stack(recipe['center']),ingredients=stack(recipe['components']),aspects=aspects,instability=6)))
 aliases['item.emt.'+id]='item.EMT.bauble.euMaker.'+kind+'.name';aliases['tc.research_name.'+key]='emt.rsname.'+name;aliases['tc.research_text.'+key]='emt.tag.'+name
write('assets/emt/models/item/one_ring.json',dict(parent='minecraft:item/generated',textures={'layer0':'emt:items/onering'}))
write('data/curios/tags/items/ring.json',dict(replace=False,values=['emt:inventory_charging_ring','emt:armor_charging_ring','emt:one_ring']))
catalog.append(dict(key='emt:one_ring',category='EMT',displayColumn=6,displayRow=-7,pageCount=1,complexity=0,aspects={},isRound=True,isAutoUnlock=True,icon_item=item('emt:one_ring')))
pages.append(dict(key='emt:one_ring',pages=[dict(type='TEXT',text='emt.text.The One Ring')]))
aliases.update({'item.emt.one_ring':'item.EMT.bauble.oneRing.name','tc.research_name.emt:one_ring':'emt.rsname.The One Ring','tc.research_text.emt:one_ring':'emt.tag.The One Ring'})
write('data/emt/research_catalog/rings.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=catalog)]))
write('data/emt/research_pages/rings.json',dict(format=1,entries=pages))
path=root/'data/emt/thaumcraft/original_resources.json';mapping=json.loads(path.read_text(encoding='utf8'))
for lang in ['en_us','zh_cn']:mapping['languages']['assets/emt/lang/'+lang+'.json'].update(aliases)
write('data/emt/thaumcraft/original_resources.json',mapping)
path=root/'data/emt/research_page_recipes/main.json';values=json.loads(path.read_text(encoding='utf8'));keys={e['key'] for e in display};values['entries']=[e for e in values['entries'] if e['key'] not in keys]+display;write('data/emt/research_page_recipes/main.json',values)
for lang,warning in [('en_us','You have worn the Ring. Your soul has now been forever tainted. Beware of wearing the ring. The tainting will only increase, and strange things will start happening.'),('zh_cn','你已戴上魔戒，灵魂从此染上污秽。小心：腐化只会不断加深，诡异之事即将发生。')]:
 path=root/('assets/emt/lang/'+lang+'.json');values=json.loads(path.read_text(encoding='utf8')) if path.exists() else {};values['message.emt.one_ring.warning']=warning;write('assets/emt/lang/'+lang+'.json',values)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
