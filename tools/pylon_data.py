from pathlib import Path
import json
p=Path(__file__).resolve().parents[1];root=p/'src/main/resources'
def write(path,data):
 target=root/path;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def stack(v):
 if v is None:return {'kind':'sequence','elements':[]}
 if isinstance(v,list):return {'kind':'sequence','elements':[stack(s) for s in v]}
 if isinstance(v,str):v={'item':v}
 return {'kind':'stack','stack':dict(v,count=v.get('count',1))}
def ref(key):return {'kind':'reference','class':'modern','keys':['gadomancy:'+key]}
def text(key,n):return {'type':'TEXT','text':f'gadomancy.research_page.{key}.{n}'}
recipes={}
for name,pattern,key,vis in [('aura_pylon',['TST','S S','TJT'],{'T':'thaumcraft:essentia_filter','S':'thaumcraft:silverwood_log','J':'thaumcraft:essentia_jar'},{'ordo':80,'aqua':50,'ignis':50,'aer':50}),('aura_pylon_cap',[' N ','ACA',' P '],{'N':'thaumcraft:nitor','A':'thaumcraft:alumentum','C':'gadomancy:aura_core_ignis','P':'gadomancy:aura_pylon'},{'ordo':90,'ignis':120,'aer':70})]:
 data=dict(type='thaumcraft:arcane',mode='shaped',research='gadomancy:aura_pylon',pattern=pattern,key={c:{'item':i} for c,i in key.items()},vis=vis,result={'item':'gadomancy:'+name});recipes[name]=data;write('data/gadomancy/recipes/'+name+'.json',data)
 write('assets/gadomancy/blockstates/'+name+'.json',{'variants':{'':{'model':'gadomancy:block/'+name}}});write('assets/gadomancy/models/block/'+name+'.json',{'textures':{'particle':'minecraft:block/quartz_block_top'}})
 write('assets/gadomancy/models/item/'+name+'.json',json.loads((root/'assets/gadomancy/models/item/arcane_dropper.json').read_text()))
 write('data/gadomancy/loot_tables/blocks/'+name+'.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'gadomancy:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
research=[dict(key='gadomancy:aura_pylon',category='GADOMANCY',displayColumn=0,displayRow=9,pageCount=5,complexity=2,aspects={'ordo':12,'auram':20,'praecantatio':12,'machina':8},parents=['gadomancy:aura_core','TUBEFILTER'],isHidden=True,itemTriggers=[{'item':'gadomancy:aura_core_ignis'}],icon_item={'item':'gadomancy:aura_pylon_cap'}),dict(key='gadomancy:aura_effects',category='GADOMANCY',displayColumn=1,displayRow=10,pageCount=1,complexity=2,aspects={'auram':6,'cognitio':8,'praecantatio':4,'ordo':4},parents=['gadomancy:aura_pylon'],isConcealed=True,isSecondary=True,icon_resource='thaumcraft:textures/misc/r_aspects.png')]
write('data/gadomancy/research_catalog/pylon.json',{'format':1,'categories':[{'key':'GADOMANCY','icon':'gadomancy:textures/items/category_icon.png','background':'gadomancy:textures/gui/gui_researchback.png','research':research}]})
write('data/gadomancy/research_pages/pylon.json',{'format':1,'entries':[{'key':'gadomancy:aura_pylon','pages':[text('AURA_PYLON',1),{'type':'ARCANE_CRAFTING','recipe':ref('aura_pylon')},{'type':'ARCANE_CRAFTING','recipe':ref('aura_pylon_cap')},text('AURA_PYLON',4),{'type':'COMPOUND_CRAFTING','recipe':ref('activate_aura_pylon')}]},{'key':'gadomancy:aura_effects','pages':[text('AURA_EFFECTS',1)]}]})
display=[dict(key='gadomancy:'+name,**{'class':'modern'},data=dict(kind='arcane_shaped',research='gadomancy:aura_pylon',width=3,height=3,aspects=data['vis'],output=stack(data['result']),ingredients=stack([data['key'].get(c) for row in data['pattern'] for c in row]))) for name,data in recipes.items()]
structure={'kind':'sequence','elements':[{'kind':'aspects','aspects':dict.fromkeys(['aer','ignis','aqua','terra','ordo','perditio'],150)}]+[{'kind':'literal','value':v} for v in [1,4,1]]+[stack(['gadomancy:aura_pylon_cap']+['gadomancy:aura_pylon']*3)]}
display.append({'key':'gadomancy:activate_aura_pylon','class':'modern','data':{'kind':'compound','structure':structure}});write('data/gadomancy/research_page_recipes/pylon.json',{'format':1,'entries':display})
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for language in data['languages'].values():
 language['block.gadomancy.aura_pylon']='tile.BlockAuraPylon.0.name';language['block.gadomancy.aura_pylon_cap']='tile.BlockAuraPylon.1.name'
 for name in ['aura_pylon','aura_effects']:
  for kind in ['name','text']:language[f'tc.research_{kind}.gadomancy:{name}']=f'tc.research_{kind}.GADOMANCY.{name.upper()}'
write('data/gadomancy/thaumcraft/original_resources.json',data)
path=root/'gadomancy.mixins.json';data=json.loads(path.read_text());data['mixins']=list(dict.fromkeys(data['mixins']+['AuraResearchPagesMixin']));write('gadomancy.mixins.json',data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
