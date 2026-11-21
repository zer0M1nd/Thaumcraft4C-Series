"""Original-author hammer progression, with externally supplied item sprites."""
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
for id,tex,legacy in [('tainted_mjolnir','taintedthorhammer','hammer.broken'),('mjolnir','thorhammer','hammer'),('supercharged_mjolnir','electricthorhammer','hammer.electric')]:
 write('assets/emt/models/item/'+id+'.json',dict(parent='minecraft:item/handheld',textures={'layer0':'emt:items/hammer/'+tex}))
 aliases['item.emt.'+id]='item.EMT.'+legacy+'.name'
catalog.append(dict(key='emt:the_legend',category='EMT',displayColumn=5,displayRow=-5,pageCount=1,complexity=0,aspects={},isRound=True,isAutoUnlock=True,icon_item=item('emt:tainted_mjolnir')))
pages.append(dict(key='emt:the_legend',pages=[dict(type='TEXT',text='emt.text.The Legend')]))
aliases.update({'tc.research_name.emt:the_legend':'emt.rsname.The Legend','tc.research_text.emt:the_legend':'emt.tag.The Legend','entity.emt.archer':'entity.EMT.archer.name'})
rows=[('mjolnir','Mjolnir',-5,5,7,'tainted_mjolnir',{'telum':3,'tempestas':4,'alienis':3},{'telum':48,'tempestas':48,'alienis':32},['emt:lightning_summoner','emt:lightning_summoner','thaumcraft:elemental_sword','thaumcraft:nitor','thaumcraft:focus_shock','ic2:rubber']),('supercharged_mjolnir','Supercharged Mjolnir',-6,6,10,'mjolnir',{'telum':4,'potentia':6,'bestia':4},{'telum':48,'potentia':64,'bestia':32},['emt:lightning_summoner','emt:lightning_summoner','minecraft:tnt','thaumcraft:focus_hellbat','thaumcraft:elemental_sword','ic2:lapotron_crystal','ic2:iridium','ic2:iridium','ic2:nano_saber'])]
for id,name,x,y,instability,center,research_aspects,aspects,components in rows:
 key='emt:'+id;result=item(key)
 if id=='supercharged_mjolnir':result['nbt']='{charge:10.0d}'
 recipe=dict(type='thaumcraft:infusion',research=key,instability=instability,aspects=aspects,center={'ingredient':item('emt:'+center),'damage':32767},components=[{'ingredient':item(v),'damage':32767 if v in ['ic2:lapotron_crystal','ic2:nano_saber'] else 0} for v in components],result=result)
 write('data/emt/recipes/'+id+'.json',recipe)
 row=dict(key=key,category='EMT',displayColumn=x,displayRow=y,pageCount=2,complexity=3,aspects=research_aspects,parentsHidden=['INFUSION'],icon_item=item(key))
 if id=='mjolnir':row.update(isConcealed=True);row['parentsHidden'].append('emt:foundation')
 else:row['parents']=['emt:mjolnir']
 catalog.append(row);pages.append(dict(key=key,pages=[dict(type='TEXT',text='emt.text.'+name),dict(type='INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[key]))]))
 aliases['tc.research_name.'+key]='emt.rsname.'+name;aliases['tc.research_text.'+key]='emt.tag.'+name
 display.append(dict(key=key,**{'class':'modern'},data=dict(kind='infusion',research=key,output=stack(result),input=stack(recipe['center']),ingredients=stack(recipe['components']),aspects=aspects,instability=instability)))
write('assets/emt/models/item/archer_spawn_egg.json',dict(parent='minecraft:item/template_spawn_egg'))
write('data/emt/research_catalog/hammers.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=catalog)]))
write('data/emt/research_pages/hammers.json',dict(format=1,entries=pages))
path=root/'data/emt/thaumcraft/original_resources.json';mapping=json.loads(path.read_text(encoding='utf8'))
for lang in ['en_us','zh_cn']:mapping['languages']['assets/emt/lang/'+lang+'.json'].update(aliases)
write('data/emt/thaumcraft/original_resources.json',mapping)
path=root/'data/emt/research_page_recipes/main.json';data=json.loads(path.read_text(encoding='utf8'));keys={d['key'] for d in display};data['entries']=[d for d in data['entries'] if d['key'] not in keys]+display;write('data/emt/research_page_recipes/main.json',data)
for lang,name in [('en_us','Archer Spawn Egg'),('zh_cn','弓箭守卫刷怪蛋')]:
 path=root/('assets/emt/lang/'+lang+'.json');data=json.loads(path.read_text(encoding='utf8'));data['item.emt.archer_spawn_egg']=name;write('assets/emt/lang/'+lang+'.json',data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
