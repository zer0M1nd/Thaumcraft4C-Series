"""Original author recipes for the two elemental EU harvesting tools."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,value):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def item(name):return {'item':name}
rows=[
 ('rockbreaker_drill','Drill of the Rockbreaker','drill_rockbreaker','drill.rockbreaker',0,-4,'thaumium_drill',{'potentia':3,'ignis':2,'perfodio':3},{'instrumentum':32,'potentia':48,'perfodio':32},['minecraft:flint_and_steel','minecraft:fire_charge','thaumcraft:elemental_pickaxe','thaumcraft:elemental_shovel','ic2:lapotron_crystal','ic2:reinforced_stone','ic2:iridium','ic2:overclocker_upgrade']),
 ('stream_chainsaw','Chainsaw of the Stream','chainsaw_stream','chainsaw.stream',-3,-4,'thaumium_chainsaw',{'instrumentum':3,'arbor':3,'aqua':6,'potentia':4},{'potentia':48,'arbor':48,'instrumentum':32,'aqua':32},['minecraft:water_bucket','thaumcraft:elemental_axe','thaumcraft:greatwood_log','ic2:lapotron_crystal','ic2:iridium','ic2:overclocker_upgrade'])]
catalog=[];pages=[];aliases={};display=[]
def stack(value):
 if isinstance(value,list):return dict(kind='sequence',elements=[stack(v) for v in value])
 value=value.get('ingredient',value);return dict(kind='stack',stack=dict(value,count=value.get('count',1)))
for id,name,texture,legacy,x,y,parent,research,aspects,components in rows:
 key='emt:'+id
 recipe=dict(type='thaumcraft:infusion',research=key,instability=6,aspects=aspects,center={'ingredient':item('emt:'+parent),'damage':32767},components=[{'ingredient':item(v),'damage':32767} for v in components],result=dict(item=key,nbt='{charge:10.0d}'))
 write('data/emt/recipes/'+id+'.json',recipe)
 write('assets/emt/models/item/'+id+'.json',dict(parent='minecraft:item/handheld',textures={'layer0':'emt:items/tools/'+texture}))
 catalog.append(dict(key=key,category='EMT',displayColumn=x,displayRow=y,pageCount=2,complexity=3,aspects=research,parents=['emt:'+parent],icon_item=item(key)))
 pages.append(dict(key=key,pages=[dict(type='TEXT',text='emt.text.'+name),dict(type='INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[key]))]))
 aliases['item.emt.'+id]='item.EMT.'+legacy+'.name';aliases['tc.research_name.'+key]='emt.rsname.'+name;aliases['tc.research_text.'+key]='emt.tag.'+name
 display.append({'key':key,'class':'modern','data':dict(kind='infusion',research=key,output=stack(recipe['result']),input=stack(recipe['center']),ingredients=stack(recipe['components']),aspects=aspects,instability=6)})
write('data/emt/research_catalog/advanced_tools.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=catalog)]))
write('data/emt/research_pages/advanced_tools.json',dict(format=1,entries=pages))
path=root/'data/emt/thaumcraft/original_resources.json';mapping=json.loads(path.read_text(encoding='utf8'))
for lang in ['en_us','zh_cn']:mapping['languages']['assets/emt/lang/'+lang+'.json'].update(aliases)
write('data/emt/thaumcraft/original_resources.json',mapping)
path=root/'data/emt/research_page_recipes/main.json';all_display=json.loads(path.read_text(encoding='utf8'));keys={entry['key'] for entry in display};all_display['entries']=[entry for entry in all_display['entries'] if entry['key'] not in keys]+display;write('data/emt/research_page_recipes/main.json',all_display)
for lang,text in {'en_us':{'message.emt.shears_on':'Shearing enabled.','message.emt.shears_off':'Shearing disabled.'},'zh_cn':{'message.emt.shears_on':'剪切模式已启用。','message.emt.shears_off':'剪切模式已关闭。'}}.items():
 path=root/('assets/emt/lang/'+lang+'.json');values=json.loads(path.read_text(encoding='utf8')) if path.exists() else {};values.update(text);write('assets/emt/lang/'+lang+'.json',values)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
