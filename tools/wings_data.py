from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def item(name):return {'item':name}
def result(name):return item('emt:'+name)
def shaped(name,pattern,key,arcane=None):
 r=dict(type='minecraft:crafting_shaped',pattern=pattern,key={k:({'tag':v[1:]} if v.startswith('#') else item(v)) for k,v in key.items()},result=result(name))
 if arcane:r.update(type='thaumcraft:arcane',mode='shaped',research='emt:thaumium_wings',vis={a:48 for a in ['aer','ignis','aqua','terra','ordo','perditio']})
 write('data/emt/recipes/'+name+'.json',r)
shaped('duct_tape',['XXX','YYY','XXX'],{'X':'emt:glue','Y':'minecraft:paper'})
shaped('feather_mesh',['XYX','YXY','XYX'],{'X':'minecraft:feather','Y':'emt:duct_tape'})
shaped('feather_wing_component',['XYZ','XYZ','XYZ'],{'X':'emt:feather_mesh','Y':'emt:glue','Z':'emt:cardboard'})
shaped('feather_wings',['XX'],{'X':'emt:feather_wing_component'})
shaped('cardboard',['XXX','YYY','XXX'],{'X':'minecraft:paper','Y':'#minecraft:logs'})
shaped('rubber_ball',['XX','XX'],{'X':'ic2:rubber'})
write('data/emt/recipes/glue.json',dict(type='minecraft:smelting',ingredient=item('emt:rubber_ball'),result='emt:glue',experience=0,cookingtime=200))
# Vanilla furnace output count is represented with the Forge stack extension.
write('data/emt/recipes/glue.json',dict(type='minecraft:smelting',ingredient=item('emt:rubber_ball'),result={'item':'emt:glue','count':2},experience=0,cookingtime=200))
shaped('tainted_feather',[' X ','XYX',' X '],{'X':'thaumcraft:tainted_goo','Y':'minecraft:feather'})
shaped('thaumium_wing_component',['XYZ','XYZ','XYZ'],{'X':'emt:tainted_feather','Y':'emt:thaumium_plate','Z':'ic2:iron_plate'},True)
shaped('thaumium_wings',['XX'],{'X':'emt:thaumium_wing_component'},True)
for kind,center,instability,components in [('nano','thaumium',4,['ic2:carbon_plate']*3+['ic2:nano_chestplate','thaumcraft:nitor']),('quantum','nano',6,['ic2:iridium']*3+['ic2:quantum_chestplate','thaumcraft:nitor'])]:
 aspects={'volatus':48,'aer':32,'machina':48,'potentia':32}
 if kind=='quantum':aspects['tutamen']=48
 write('data/emt/recipes/'+kind+'_wings.json',dict(type='thaumcraft:infusion',research='emt:'+kind+'_wings',instability=instability,aspects=aspects,center={'ingredient':item('emt:'+center+'_wings'),'damage':32767},components=[{'ingredient':item(i),'damage':32767} for i in components],result=dict(item='emt:'+kind+'_wings',nbt='{charge:10.0d}')))
aliases={}
for id,texture,legacy in [('feather_mesh','feathermesh','featherMesh'),('glue','glue','glue'),('duct_tape','ducttape','ductTape'),('rubber_ball','rubberball','rubberBall'),('cardboard','cardboard','cardboard'),('feather_wing_component','cardboardsheet','featherGluedCardboardWing'),('tainted_feather','taintedfeather','taintedFeather'),('thaumium_wing_component','thaumiumwing','thaumiumWing')]:
 write('assets/emt/models/item/'+id+'.json',dict(parent='minecraft:item/generated',textures={'layer0':'emt:items/materials/'+texture}));aliases['item.emt.'+id]='item.EMT.'+legacy+'.name'
research=[];pages=[]
for kind,name,row,complexity,parent,recipes in [('feather','Feather Wings',2,0,'foundation',['cardboard','feather_wing_component','feather_wings']),('thaumium','Thaumium Reinforced Wings',4,0,'feather_wings',['thaumium_wing_component','thaumium_wings']),('nano','Nanosuit Wings',6,2,'thaumium_wings',['nano_wings']),('quantum','Quantum Wings',8,3,'nano_wings',['quantum_wings'])]:
 key='emt:'+kind+'_wings';aspects={} if kind=='feather' else {'volatus':5,'aer':5,'machina':8}
 if kind in ['nano','quantum']:aspects['potentia']=4
 page_entries=[] if kind=='feather' else [dict(type='TEXT',text='emt.text.'+name)]
 for recipe in recipes:page_entries.append(dict(type='NORMAL_CRAFTING' if kind=='feather' else 'ARCANE_CRAFTING' if kind=='thaumium' else 'INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=['emt:'+recipe])))
 research.append(dict(key=key,category='EMT',displayColumn=0,displayRow=row,pageCount=len(page_entries),complexity=complexity,aspects=aspects,parents=['emt:'+parent],isConcealed=kind!='feather',isAutoUnlock=kind=='feather',isRound=kind=='feather',isSecondary=kind=='thaumium',icon_item=item(key)))
 pages.append(dict(key=key,pages=page_entries));write('assets/emt/models/item/'+kind+'_wings.json',dict(parent='minecraft:item/generated',textures={'layer0':'emt:items/armor/wing_'+kind}))
 aliases['item.emt.'+kind+'_wings']='item.EMT.wing.'+kind+'.name';aliases['tc.research_name.'+key]='emt.rsname.'+name;aliases['tc.research_text.'+key]='emt.tag.'+name
write('data/emt/research_catalog/wings.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=research)]))
write('data/emt/research_pages/wings.json',dict(format=1,entries=pages))
path=root/'data/emt/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/emt/lang/'+lang+'.json'].update(aliases)
write('data/emt/thaumcraft/original_resources.json',data)
exec((Path(__file__).parent/'energy_data.py').read_text(),{'__file__':str(Path(__file__).parent/'energy_data.py')})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
