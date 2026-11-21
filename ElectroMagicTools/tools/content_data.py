"""Own JSON definitions. Legacy PNG/lang contents remain exclusively in external JARs."""
from pathlib import Path
import json,zipfile
from legacy_language_aliases import complete_aliases
root=Path(__file__).resolve().parents[1]
def write(relative,value,project=root):
 p=project/'src/main/resources'/relative;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def item(x):return {'item':x}
def result(x,charged=False):return dict(item='emt:'+x,**({'nbt':'{charge:10.0d}'} if charged else {}))
entries=[('thaumium_drill','drill_thaumium','drill.thaumium','Thaumium Drill',-1,-2,{'potentia':5,'instrumentum':6,'perfodio':4}),('diamond_chainsaw','chainsaw_diamond','chainsaw.diamond','Diamond Chainsaw',5,-6,{}),('thaumium_chainsaw','chainsaw_thaumium','chainsaw.thaumium','Thaumium Chainsaw',-2,-2,{'potentia':5,'telum':6,'instrumentum':4}),('iron_omnitool','omnitool_iron','omnitool.iron','Iron Omnitool',-2,0,{}),('diamond_omnitool','omnitool_diamond','omnitool.diamond','Diamond Omnitool',-4,0,{'potentia':2,'instrumentum':3,'perfodio':2,'telum':2}),('thaumium_omnitool','omnitool_thaumium','omnitool.thaumium','Thaumium Omnitool',-6,0,{'potentia':5,'instrumentum':6,'perfodio':4,'telum':6}),('electric_scribing_tools',None,'scribingtools.electric','Electric Scribing Tools',-6,-5,{'potentia':5,'tenebrae':3,'fabrico':1})]
research=[dict(key='emt:foundation',category='EMT',displayColumn=0,displayRow=0,pageCount=1,isAutoUnlock=True,isRound=True,icon='emt:textures/misc/emt.png')]
pages=[dict(key='emt:foundation',pages=[dict(type='TEXT',text='emt.text.Electric Magic Tools')])]
aliases={'tc.research_name.emt:foundation':'emt.rsname.Electric Magic Tools','tc.research_text.emt:foundation':'emt.tag.Electric Magic Tools'}
for ident,texture,old,name,col,row,aspects in entries:
 write('assets/emt/models/item/'+ident+'.json',{'parent':'minecraft:item/handheld' if texture else 'minecraft:item/generated','textures':{'layer0':'emt:items/tools/'+texture if texture else 'thaumcraft:item/inkwell'}})
 aliases['item.emt.'+ident]='item.EMT.'+old+'.name'
 aliases['tc.research_name.emt:'+ident]='emt.rsname.'+name;aliases['tc.research_text.emt:'+ident]='emt.tag.'+name
 parents=['emt:foundation']
 if ident=='diamond_omnitool':parents=['emt:iron_omnitool']
 if ident=='thaumium_omnitool':parents=['emt:diamond_omnitool']
 research.append(dict(key='emt:'+ident,category='EMT',displayColumn=col,displayRow=row,pageCount=2,complexity=2 if ident=='thaumium_omnitool' else 1,aspects=aspects,parents=parents,isAutoUnlock=not aspects,isSecondary=bool(aspects) and ident!='thaumium_omnitool',isConcealed=bool(aspects),icon_item=item('emt:'+ident),parentsHidden=['INFUSION'] if ident in ('thaumium_drill','thaumium_chainsaw','thaumium_omnitool') else []))
 kind='INFUSION_CRAFTING' if ident in ('thaumium_drill','thaumium_chainsaw','thaumium_omnitool') else 'NORMAL_CRAFTING' if ident in ('iron_omnitool','diamond_chainsaw') else 'ARCANE_CRAFTING'
 pages.append(dict(key='emt:'+ident,pages=[dict(type='TEXT',text='emt.text.'+name),dict(type=kind,recipe={'kind':'reference','keys':['emt:'+ident],'class':'modern'})]))
write('data/emt/research_catalog/tools.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=research)]))
write('data/emt/research_pages/tools.json',dict(format=1,entries=pages))
write('assets/emt/models/item/thaumium_plate.json',{'parent':'minecraft:item/generated','textures':{'layer0':'emt:items/materials/thaumiumplate'}})
aliases['item.emt.thaumium_plate']='item.EMT.thaumiumPlate.name'
for lang in ['en_us','zh_cn']:
 path=root/'src/main/resources/data/emt/thaumcraft/original_resources.json';data=json.loads(path.read_text(encoding='utf8'));data.setdefault('languages',{}).setdefault('assets/emt/lang/'+lang+'.json',{}).update(aliases)
 with zipfile.ZipFile(root.parent/'references/phase2/electromagictools/ElectroMagicTools-1.2.6.2.jar') as archive:complete_aliases(data,archive)
 path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
write('data/forge/tags/items/plates/thaumium.json',{'replace':False,'values':['emt:thaumium_plate']})
write('data/emt/recipes/thaumium_plate.json',{'type':'minecraft:crafting_shaped','pattern':['X','Y','Z'],'key':{'X':item('ic2:forge_hammer'),'Y':item('thaumcraft:thaumium_ingot'),'Z':item('minecraft:obsidian')},'result':result('thaumium_plate')})
write('data/emt/recipes/thaumium_plate_rolling.json',{'type':'ic2:metal_former_rolling','ingredient':item('thaumcraft:thaumium_ingot'),'result':result('thaumium_plate')})
write('data/emt/recipes/iron_omnitool.json',{'type':'minecraft:crafting_shaped','pattern':['X','Z','Y'],'key':{'X':item('ic2:chainsaw'),'Z':item('ic2:iron_plate'),'Y':item('ic2:drill')},'result':result('iron_omnitool',True)})
write('data/emt/recipes/diamond_chainsaw.json',{'type':'minecraft:crafting_shaped','pattern':[' X ','XYX'],'key':{'X':item('minecraft:diamond'),'Y':item('ic2:chainsaw')},'result':result('diamond_chainsaw',True)})
write('data/emt/recipes/electric_scribing_tools.json',{'type':'thaumcraft:arcane','mode':'shaped','research':'emt:electric_scribing_tools','pattern':['YXY','XZX','YXY'],'key':{'Y':item('ic2:circuit'),'X':item('thaumcraft:scribing_tools'),'Z':item('ic2:energy_crystal')},'vis':{'aer':16,'ordo':16,'perditio':16},'result':result('electric_scribing_tools',True)})
write('data/emt/recipes/diamond_omnitool.json',{'type':'thaumcraft:arcane','mode':'shapeless','research':'emt:diamond_omnitool','ingredients':[item('emt:diamond_chainsaw'),item('ic2:diamond_drill')],'vis':{'aer':15,'ordo':15},'result':result('diamond_omnitool',True)})
write('data/emt/recipes/diamond_omnitool_upgrade.json',{'type':'thaumcraft:arcane','mode':'shaped','research':'emt:diamond_omnitool','pattern':['XAX','XBX','XAX'],'key':{'X':item('minecraft:diamond'),'A':item('ic2:advanced_circuit'),'B':item('emt:iron_omnitool')},'vis':{'aer':15,'ordo':15},'result':result('diamond_omnitool')})
for ident,center,aspects,components in [
 ('thaumium_drill','ic2:diamond_drill',{'instrumentum':16,'perfodio':16,'potentia':16},['minecraft:diamond']*2+['emt:thaumium_plate']*3+['ic2:dense_iron_plate']),
 ('thaumium_chainsaw','emt:diamond_chainsaw',{'instrumentum':16,'telum':16,'potentia':16},['minecraft:diamond']*2+['emt:thaumium_plate']*3+['ic2:dense_iron_plate']),
 ('thaumium_omnitool','emt:thaumium_chainsaw',{'instrumentum':25,'telum':25,'potentia':25,'perfodio':25},['emt:thaumium_drill']+['emt:thaumium_plate']*3+['ic2:carbon_plate','ic2:obsidian_plate'])]:
 write('data/emt/recipes/'+ident+'.json',{'type':'thaumcraft:infusion','research':'emt:'+ident,'instability':6 if ident=='thaumium_omnitool' else 5,'aspects':aspects,'center':{'ingredient':item(center)},'components':[{'ingredient':item(x)} for x in components],'result':result(ident,True)})
gad=root.parent/'Gadomancy'
for ident in ['arcane_package','disguised_package']:
 write('assets/gadomancy/models/item/'+ident+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:item/lootbag'}},gad)
write('assets/gadomancy/models/item/arcane_packager.json',{'parent':'builtin/entity','gui_light':'front','display':{'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.625,.625,.625]},'ground':{'translation':[0,3,0],'scale':[.25,.25,.25]},'fixed':{'rotation':[0,180,0],'scale':[.5,.5,.5]},'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375,.375,.375]},'firstperson_righthand':{'rotation':[0,45,0],'scale':[.4,.4,.4]}}},gad)
write('assets/gadomancy/blockstates/arcane_packager.json',{'variants':{'':{'model':'gadomancy:block/arcane_packager'}}},gad)
write('assets/gadomancy/models/block/arcane_packager.json',{'textures':{'particle':'thaumcraft:block/stone'}} ,gad)
write('data/gadomancy/loot_tables/blocks/arcane_packager.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'gadomancy:arcane_packager'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]},gad)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
