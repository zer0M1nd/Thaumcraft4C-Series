from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def item(name):return {'item':name}
def result(kind):return {'item':'emt:'+kind+'_goggles','nbt':'{charge:10.0d}'}
recipes={}
recipes['electric_goggles']=dict(type='thaumcraft:arcane',mode='shaped',research='emt:electric_goggles',pattern=[' Y ','AZA','BXB'],key={key:item(value) for key,value in {'Y':'minecraft:diamond_helmet','A':'ic2:re_battery','Z':'thaumcraft:goggles','B':'minecraft:repeater','X':'ic2:circuit'}.items()},vis={'aer':20,'ordo':20,'aqua':20},result=result('electric'))
for kind,center,instability,aspects,components in [
 ('nano','emt:electric_goggles',5,{'tutamen':16,'sensus':16,'potentia':16},['minecraft:diamond','minecraft:gold_ingot','ic2:nano_helmet','emt:thaumium_plate','emt:thaumium_plate','ic2:carbon_plate','ic2:circuit']),
 ('quantum','emt:nano_goggles',6,{'tutamen':25,'sensus':25,'potentia':25},['minecraft:diamond','minecraft:milk_bucket','ic2:quantum_helmet','emt:thaumium_plate','emt:thaumium_plate','ic2:iridium','ic2:advanced_circuit']),
 ('solar','emt:quantum_goggles',5,{'potentia':32,'aer':32,'lux':25},['emt:double_compressed_solar','ic2:glass_fibre_cable','ic2:lapotron_crystal','thaumcraft:order_shard'])]:
 recipes[kind+'_goggles']=dict(type='thaumcraft:infusion',research='emt:'+kind+'_goggles',instability=instability,aspects=aspects,center={'ingredient':item(center),'damage':32767},components=[{'ingredient':item(i),'damage':32767} for i in components],result=result(kind))
research=[];pages=[];aliases={}
for kind,name,col,row,complexity,aspects,parent in [
 ('electric','Electric Goggles',1,-2,1,{'tutamen':8,'potentia':5,'sensus':6},'foundation'),
 ('nano','Nanosuit Goggles of Revealing',2,-2,2,{'potentia':5,'sensus':8,'tutamen':6},'electric_goggles'),
 ('quantum','Quantum Goggles of Revealing',3,-2,3,{'tutamen':8,'potentia':4,'sensus':6},'nano_goggles'),
 ('solar','Solar Helmet of Revealing',-2,-5,1,{'potentia':5,'aer':2,'lux':4},'compressed_solar')]:
 key='emt:'+kind+'_goggles';research.append(dict(key=key,category='EMT',displayColumn=col,displayRow=row,pageCount=2,complexity=complexity,aspects=aspects,parents=['emt:'+parent],parentsHidden=['emt:quantum_goggles'] if kind=='solar' else [],isConcealed=kind!='solar',isSecondary=kind in ['electric','solar'],icon_item=item(key)))
 pages.append(dict(key=key,pages=[dict(type='TEXT',text='emt.text.'+name),dict(type='ARCANE_CRAFTING' if kind=='electric' else 'INFUSION_CRAFTING',recipe={'kind':'reference','class':'modern','keys':[key]})]))
 write('assets/emt/models/item/'+kind+'_goggles.json',{'parent':'minecraft:item/generated','textures':{'layer0':'emt:items/armor/goggles_'+kind}})
 aliases['item.emt.'+kind+'_goggles']='item.EMT.goggles.'+kind+'.name';aliases['tc.research_name.'+key]='emt.rsname.'+name;aliases['tc.research_text.'+key]='emt.tag.'+name
for name,recipe in recipes.items():write('data/emt/recipes/'+name+'.json',recipe)
write('data/emt/research_catalog/goggles.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=research)]))
write('data/emt/research_pages/goggles.json',dict(format=1,entries=pages))
path=root/'data/emt/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data.setdefault('languages',{}).setdefault('assets/emt/lang/'+lang+'.json',{}).update(aliases)
write('data/emt/thaumcraft/original_resources.json',data)
# Existing generator also rebuilds display definitions from every actual recipe, avoiding divergent book costs.
exec((Path(__file__).parent/'energy_data.py').read_text(),{'__file__':str(Path(__file__).parent/'energy_data.py')})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
