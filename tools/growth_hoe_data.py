"""Preserve EMT's original infusion acquisition chain with the missing IC2 hoe restored locally."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,value):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def item(name):return {'item':name}
write('assets/emt/models/item/industrial_electric_hoe.json',dict(parent='minecraft:item/handheld',textures={'layer0':'ic2:item/tool/electric/electric_hoe'}))
write('assets/emt/models/item/electric_growth_hoe.json',dict(parent='minecraft:item/handheld',textures={'layer0':'emt:items/tools/hoe_growth'}))
write('data/emt/recipes/industrial_electric_hoe.json',dict(type='minecraft:crafting_shaped',pattern=['II',' I',' B'],key={'I':{'tag':'forge:plates/iron'},'B':item('ic2:small_power_unit')},result=item('emt:industrial_electric_hoe')))
recipe=dict(type='thaumcraft:infusion',research='emt:electric_growth_hoe',instability=4,aspects={'potentia':32,'herba':25,'messis':32,'praecantatio':48},center={'ingredient':item('thaumcraft:elemental_hoe'),'damage':0},components=[{'ingredient':item('ic2:lapotron_crystal'),'damage':32767},{'ingredient':item('emt:industrial_electric_hoe'),'damage':32767},{'ingredient':item('minecraft:bone_meal')},{'ingredient':item('ic2:electric_motor')},{'ingredient':item('ic2:coil')},{'ingredient':[item('minecraft:'+wood+'_sapling') for wood in ['oak','spruce','birch','jungle','acacia','dark_oak']]}],result=dict(item='emt:electric_growth_hoe',nbt='{charge:10.0d}'))
write('data/emt/recipes/electric_growth_hoe.json',recipe)
write('data/emt/research_catalog/growth_hoe.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=[dict(key='emt:electric_growth_hoe',category='EMT',displayColumn=-1,displayRow=-4,pageCount=2,complexity=2,aspects={'potentia':4,'herba':4,'messis':5,'praecantatio':4},parents=['emt:foundation'],isConcealed=True,icon_item=item('emt:electric_growth_hoe'))])]))
write('data/emt/research_pages/growth_hoe.json',dict(format=1,entries=[dict(key='emt:electric_growth_hoe',pages=[dict(type='TEXT',text='emt.text.Electric Hoe of Growth'),dict(type='INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=['emt:electric_growth_hoe']))])]))
aliases={'item.emt.electric_growth_hoe':'item.EMT.hoeofgrowth.electric.name','tc.research_name.emt:electric_growth_hoe':'emt.rsname.Electric Hoe of Growth','tc.research_text.emt:electric_growth_hoe':'emt.tag.Electric Hoe of Growth'}
path=root/'data/emt/thaumcraft/original_resources.json';mapping=json.loads(path.read_text(encoding='utf8'))
for lang in ['en_us','zh_cn']:mapping['languages']['assets/emt/lang/'+lang+'.json'].update(aliases)
write('data/emt/thaumcraft/original_resources.json',mapping)
for lang,name in [('en_us','Electric Hoe'),('zh_cn','电动锄')]:
 path=root/('assets/emt/lang/'+lang+'.json');values=json.loads(path.read_text(encoding='utf8')) if path.exists() else {};values['item.emt.industrial_electric_hoe']=name;write('assets/emt/lang/'+lang+'.json',values)
def stack(value):
 if isinstance(value,dict):value=value.get('ingredient',value)
 if isinstance(value,list):return dict(kind='sequence',elements=[stack(v) for v in value])
 if 'tag' in value:return dict(kind='ore',names=[value['tag']],elements=[])
 return dict(kind='stack',stack=dict(value,count=value.get('count',1)))
entry=dict(key='emt:electric_growth_hoe',**{'class':'modern'},data=dict(kind='infusion',research=recipe['research'],output=stack(recipe['result']),input=stack(recipe['center']),ingredients=stack(recipe['components']),aspects=recipe['aspects'],instability=4))
path=root/'data/emt/research_page_recipes/main.json';display=json.loads(path.read_text(encoding='utf8'));display['entries']=[e for e in display['entries'] if e['key']!=entry['key']]+[entry];write('data/emt/research_page_recipes/main.json',display)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
