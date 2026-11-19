from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def stack(item):
 if isinstance(item,str):item={'item':item}
 if 'ingredient' in item:item=item['ingredient']
 return dict(kind='stack',stack=dict(item,count=item.get('count',1)))
recipes={}
def infusion(key,center,ingredients,aspects,instability,result,typ='thaumcraft:infusion'):
 data=dict(type=typ,research='gadomancy:'+key,instability=instability,center={'ingredient':{'item':center},'damage':-1},components=[{'ingredient':{'item':item},'damage':-1} for item in ingredients],aspects=aspects,result=result)
 recipes[key]=data;write('data/gadomancy/recipes/'+key+'.json',data)
infusion('silverwood_golem','thaumcraft:silverwood_log',['thaumcraft:primal_charm']+['thaumcraft:salis_mundus','thaumcraft:knowledge_fragment','thaumcraft:quicksilver']*2,dict(motus=16,praecantatio=16,humanus=16,sensus=16),7,{'item':'gadomancy:silverwood_golem','nbt':'{AddonMaterial:"gadomancy:silverwood"}'})
infusion('golem_core_break','thaumcraft:golem_core_harvest',['thaumcraft:elemental_shovel','thaumcraft:elemental_pickaxe','thaumcraft:elemental_axe'],dict(perditio=16,instrumentum=24,machina=16),3,{'item':'gadomancy:golem_core_break'})
infusion('golem_core_bodyguard','thaumcraft:golem_core_guard',['thaumcraft:traveller_boots','minecraft:ender_pearl','thaumcraft:elemental_sword','minecraft:ender_pearl'],dict(instrumentum=28,machina=20,telum=10,tutamen=16),3,{'item':'gadomancy:golem_core_bodyguard'})
infusion('golem_runic_shield','gadomancy:silverwood_golem',['thaumcraft:runic_girdle','thaumcraft:salis_mundus','thaumcraft:salis_mundus'],dict(praecantatio=16,potentia=16,tutamen=8,auram=4),5,{'patch':'{AddonData:{"gadomancy:golem":{RunicShield:1b}}}'},'gadomancy:golem_runic_shield')
base={'parent':'minecraft:item/generated','textures':{'layer0':'gadomancy:items/golem_silverwood'}}
base['overrides']=[{'predicate':{'gadomancy:golem_configuration':v},'model':'gadomancy:item/silverwood_golem_'+str(v)} for v in [1,2,3]]
write('assets/gadomancy/models/item/silverwood_golem.json',base)
for value in [1,2,3]:
 layers={'layer0':'gadomancy:items/golem_silverwood','layer1':'thaumcraft:item/golem_over_adv' if value&1 else 'thaumcraft:item/blank','layer2':'thaumcraft:item/golem_over_core' if value&2 else 'thaumcraft:item/blank'}
 write(f'assets/gadomancy/models/item/silverwood_golem_{value}.json',{'parent':'minecraft:item/generated','textures':layers})
for core in ['break','bodyguard']:write('assets/gadomancy/models/item/golem_core_'+core+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'gadomancy:items/golem_core_'+core}})
definitions=[('silverwood_golem','GOLEMSILVERWOOD',1,-2,3,dict(motus=10,arbor=10,praecantatio=8,ordo=8,corpus=6,permutatio=5),['GOLEMTALLOW']),('golem_core_break','GOLEMCOREBREAK',3,-3,3,dict(instrumentum=8,perditio=8,machina=8),['COREHARVEST','ELEMENTALAXE','ELEMENTALPICK','ELEMENTALSHOVEL']),('golem_core_bodyguard','GOLEMCOREBODYGUARD',5,-3,2,dict(instrumentum=8,ordo=8,machina=10,tutamen=8,telum=8),['BOOTSTRAVELLER','ELEMENTALSWORD','COREGUARD']),('golem_runic_shield','GOLEMRUNICSHIELD',0,0,4,dict(auram=16,tutamen=6,praecantatio=4,motus=8),['gadomancy:silverwood_golem'])]
research=[];pages=[];display=[]
for key,old,x,y,difficulty,aspects,parents in definitions:
 icon={'item':'gadomancy:'+('silverwood_golem' if key=='golem_runic_shield' else key)}
 if key=='golem_runic_shield':icon['nbt']='{AddonData:{"gadomancy:golem":{RunicShield:1b}}}'
 research.append(dict(key='gadomancy:'+key,category='GADOMANCY',displayColumn=x,displayRow=y,pageCount=2,complexity=difficulty,aspects=aspects,parents=parents,isConcealed=True,icon_item=icon))
 recipe=recipes[key]
 inputs=['thaumcraft:golem_'+material for material in ['straw','wood','tallow','flesh','clay','stone','iron','thaumium']]+['gadomancy:silverwood_golem'] if key=='golem_runic_shield' else [recipe['center']['ingredient']['item']]
 keys=[]
 for index,item in enumerate(inputs):
  displaykey='gadomancy:'+key+('/'+str(index) if len(inputs)>1 else '')
  keys.append(displaykey);output={'item':item,'nbt':'{AddonData:{"gadomancy:golem":{RunicShield:1b}}}'} if key=='golem_runic_shield' else recipe['result']
  display.append(dict(key=displaykey,**{'class':'modern'},data=dict(kind='infusion',research='gadomancy:'+key,input=stack(item),output=stack(output),ingredients=dict(kind='sequence',elements=[stack(c) for c in recipe['components']]),aspects=recipe['aspects'],instability=recipe['instability'])))
 pages.append(dict(key='gadomancy:'+key,pages=[dict(type='TEXT',text='gadomancy.research_page.'+old+'.1'),dict(type='INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=keys))]))
write('data/gadomancy/research_catalog/golems.json',dict(format=1,categories=[dict(key='GADOMANCY',icon='gadomancy:textures/items/category_icon.png',background='gadomancy:textures/gui/gui_researchback.png',research=research)]))
write('data/gadomancy/research_pages/golems.json',dict(format=1,entries=pages));write('data/gadomancy/research_page_recipes/golems.json',dict(format=1,entries=display))
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:
 aliases=data['languages']['assets/gadomancy/lang/'+lang+'.json'];aliases['item.gadomancy.silverwood_golem']='item.ItemGolemPlacer.silverwood.name'
 for core in ['break','bodyguard']:aliases['item.gadomancy.golem_core_'+core]='gadomancy.golem.'+core+'core'
 for key,old,*_ in definitions:
  for kind in ['name','text']:aliases['tc.research_'+kind+'.gadomancy:'+key]='tc.research_'+kind+'.GADOMANCY.'+old
write('data/gadomancy/thaumcraft/original_resources.json',data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
