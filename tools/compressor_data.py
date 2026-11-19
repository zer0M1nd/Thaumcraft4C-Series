from pathlib import Path
import json
p=Path(__file__).resolve().parents[1];root=p/'src/main/resources'
def write(path,data):
 target=root/path;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def stack(v):
 if v is None:return {'kind':'sequence','elements':[]}
 if isinstance(v,list):return {'kind':'sequence','elements':[stack(s) for s in v]}
 if isinstance(v,str):v={'item':v}
 if 'ingredient' in v:v=v['ingredient']
 return {'kind':'stack','stack':dict(v,count=v.get('count',1))}
def ref(key):return {'kind':'reference','class':'modern','keys':['gadomancy:'+key]}
def text(key,n):return {'type':'TEXT','text':f'gadomancy.research_page.{key}.{n}'}
recipes={}
recipes['darkness_element']=dict(type='gadomancy:aura_core',research='gadomancy:essentia_compressor',instability=6,aspects={'vacuos':22,'tenebrae':18,'alienis':20,'auram':10},center={'ingredient':{'item':'thaumcraft:void_seed'}},components=[{'ingredient':{'item':'thaumcraft:'+item}} for item in ['alumentum','void_ingot','void_ingot','salis_mundus']*2],result={'item':'gadomancy:darkness_element'})
recipes['essentia_compressor']=dict(type='thaumcraft:arcane',mode='shaped',research='gadomancy:essentia_compressor',pattern=['LRL','TET','LCL'],key={k:{'item':v} for k,v in {'L':'thaumcraft:greatwood_log','R':'thaumcraft:order_crystal_cluster','T':'thaumcraft:essentia_filter','E':'gadomancy:darkness_element','C':'gadomancy:aura_core_aqua'}.items()},vis={'aqua':190,'ordo':150,'perditio':120,'ignis':100,'aer':160,'terra':60},result={'item':'gadomancy:essentia_compressor','count':3})
recipes['ancient_pedestal']=dict(type='thaumcraft:arcane',mode='shaped',research='gadomancy:e_portal_creator',pattern=['SPS',' S ','SPS'],key={'S':{'item':'thaumcraft:eldritch_stone'},'P':{'item':'thaumcraft:eldritch_pedestal_stone'}},vis={'perditio':25,'ordo':25},result={'item':'gadomancy:ancient_pedestal'})
display=[]
for name,data in recipes.items():
 write('data/gadomancy/recipes/'+name+'.json',data)
 if data['type']=='thaumcraft:arcane':view=dict(kind='arcane_shaped',research=data['research'],width=3,height=3,aspects=data['vis'],output=stack(data['result']),ingredients=stack([data['key'].get(c) for row in data['pattern'] for c in row]))
 else:view=dict(kind='infusion',research=data['research'],instability=data['instability'],aspects=data['aspects'],output=stack(data['result']),input=stack(data['center']),ingredients=stack(data['components']))
 display.append({'key':'gadomancy:'+name,'class':'modern','data':view})
structure={'kind':'sequence','elements':[{'kind':'aspects','aspects':{'aqua':140,'terra':80,'aer':150,'ordo':20,'perditio':140}}]+[{'kind':'literal','value':v} for v in [1,3,1]]+[stack(['gadomancy:essentia_compressor']*3)]}
display.append({'key':'gadomancy:activate_compressor','class':'modern','data':{'kind':'compound','structure':structure}});write('data/gadomancy/research_page_recipes/compressor.json',{'format':1,'entries':display})
research=dict(key='gadomancy:essentia_compressor',category='GADOMANCY',displayColumn=-4,displayRow=7,pageCount=9,complexity=2,aspects={'vacuos':12,'aqua':10,'perditio':12,'praecantatio':6,'machina':12},parents=['gadomancy:aura_core','ESSENTIARESERVOIR'],isHidden=True,itemTriggers=[{'item':'gadomancy:aura_core_aqua'}],icon_item={'item':'gadomancy:essentia_compressor'},warp=3)
write('data/gadomancy/research_catalog/compressor.json',{'format':1,'categories':[{'key':'GADOMANCY','icon':'gadomancy:textures/items/category_icon.png','background':'gadomancy:textures/gui/gui_researchback.png','research':[research]}]})
pages=[text('ESSENTIA_COMPRESSOR',i) for i in range(1,10)];pages[2]={'type':'INFUSION_CRAFTING','recipe':ref('darkness_element')};pages[4]={'type':'ARCANE_CRAFTING','recipe':ref('essentia_compressor')};pages[6]={'type':'COMPOUND_CRAFTING','recipe':ref('activate_compressor')};write('data/gadomancy/research_pages/compressor.json',{'format':1,'entries':[{'key':'gadomancy:essentia_compressor','pages':pages}]})
for name in ['essentia_compressor','ancient_pedestal']:
 write('assets/gadomancy/blockstates/'+name+'.json',{'variants':{'':{'model':'gadomancy:block/'+name}}})
 write('data/gadomancy/loot_tables/blocks/'+name+'.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'gadomancy:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write('assets/gadomancy/models/block/essentia_compressor.json',{'textures':{'particle':'gadomancy:blocks/block_packed_compressor'}})
write('assets/gadomancy/models/item/essentia_compressor.json',json.loads((root/'assets/gadomancy/models/item/arcane_dropper.json').read_text()))
write('assets/gadomancy/models/block/ancient_pedestal.json',{'parent':'thaumcraft:block/arcane_pedestal','textures':{'side':'gadomancy:blocks/ancient_pedestal_side','top':'gadomancy:blocks/manipulator_bot','bottom':'gadomancy:blocks/manipulator_bot','particle':'gadomancy:blocks/ancient_pedestal_side'}})
write('assets/gadomancy/models/item/ancient_pedestal.json',{'parent':'gadomancy:block/ancient_pedestal'})
write('assets/gadomancy/models/item/darkness_element.json',{'parent':'minecraft:item/generated','textures':{'layer0':'gadomancy:items/element'}})
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for language in data['languages'].values():
 language['block.gadomancy.essentia_compressor']='tile.BlockEssentiaCompressor.name';language['block.gadomancy.ancient_pedestal']='tile.BlockStoneMachine.1.name';language['item.gadomancy.darkness_element']='item.ItemElement.0.name'
 for kind in ['name','text']:language[f'tc.research_{kind}.gadomancy:essentia_compressor']=f'tc.research_{kind}.GADOMANCY.ESSENTIA_COMPRESSOR'
write('data/gadomancy/thaumcraft/original_resources.json',data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
