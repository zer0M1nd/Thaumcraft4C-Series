"""Original 1.0.7.3 node/maze research, recipes and ordered multiblock pages."""
from pathlib import Path
import json
p=Path(__file__).resolve().parents[1];root=p/'src/main/resources'
def write(path,data):
 f=root/path;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def stack(v):
 if v is None:return {'kind':'sequence','elements':[]}
 if isinstance(v,list):return {'kind':'sequence','elements':[stack(s) for s in v]}
 if isinstance(v,str):v={'item':v}
 if 'ingredient' in v:v=v['ingredient']
 return {'kind':'stack','stack':dict(v,count=v.get('count',1))}
def ref(key):return {'kind':'reference','class':'modern','keys':['gadomancy:'+key]}
def text(key,n):return {'type':'TEXT','text':f'gadomancy.research_page.{key.upper()}.{n}'}
recipes={}
def infusion(name,research,instability,aspects,center,components):recipes[name]=dict(type='thaumcraft:infusion',research='gadomancy:'+research,instability=instability,aspects=aspects,center={'ingredient':{'item':'thaumcraft:'+center}},components=[{'ingredient':{'item':'thaumcraft:'+s}} for s in components],result={'item':'gadomancy:'+name})
infusion('node_manipulator','node_manipulator',10,{'auram':42,'alienis':22,'praecantatio':38,'machina':28,'tenebrae':14},'wand_pedestal',['node_stabilizer','mixed_crystal_cluster','void_seed','eldritch_stone','nitor','primal_charm','warding_paving_stone','mixed_crystal_cluster','void_seed','eldritch_stone','nitor','primal_charm'])
infusion('randomization_focus','node_manipulator',7,{'alienis':18,'praecantatio':18,'machina':20,'tenebrae':28,'ordo':30},'wand_pedestal_focus',['primal_charm','void_ingot','mixed_crystal_cluster','void_ingot']*2)
infusion('portal_focus','e_portal_creator',7,{'alienis':22,'vacuos':38,'machina':30,'tenebrae':28,'permutatio':38},'wand_pedestal_focus',['eldritch_eye','entropy_crystal_cluster','void_ingot','entropy_crystal_cluster']*2)
recipes['ancient_stone']=dict(type='thaumcraft:arcane',mode='shaped',research='gadomancy:ancient_stones',pattern=['SSS','SES','SSS'],key={'S':{'item':'thaumcraft:arcane_stone'},'E':{'item':'thaumcraft:eldritch_eye'}},vis={'perditio':16,'terra':8},result={'item':'thaumcraft:eldritch_stone','count':4},conditions=[{'type':'gadomancy:ancient_stones'}])
recipes['ancient_stone_pedestal']=dict(type='thaumcraft:crucible',research='gadomancy:ancient_stones',catalyst={'item':'thaumcraft:arcane_stone_bricks'},aspects={'alienis':8,'permutatio':8,'perditio':12},result={'item':'thaumcraft:eldritch_pedestal_stone'},conditions=[{'type':'gadomancy:ancient_stones'}])
display=[]
for name,data in recipes.items():
 write('data/gadomancy/recipes/'+name+'.json',data)
 if data['type']=='thaumcraft:arcane':view=dict(kind='arcane_shaped',research=data['research'],width=3,height=3,aspects=data['vis'],output=stack(data['result']),ingredients=stack([data['key'].get(c) for row in data['pattern'] for c in row]))
 elif data['type']=='thaumcraft:crucible':view=dict(kind='crucible',research=data['research'],aspects=data['aspects'],output=stack(data['result']),input=stack(data['catalyst']))
 else:view=dict(kind='infusion',research=data['research'],instability=data['instability'],aspects=data['aspects'],output=stack(data['result']),input=stack(data['center']),ingredients=stack(data['components']))
 display.append({'key':'gadomancy:'+name,'class':'modern','data':view})
for name,width in [('node_manipulator',3),('e_portal_creator',7)]:
 mid=width//2;blocks=[]
 for y in [2,1,0]:
  for z in range(width):
   for x in range(width):
    a,b=x-mid,z-mid;value=None
    if y==2 and a==b==0:value='thaumcraft:aura_node'
    elif y==1:
     if a==b==0:value='gadomancy:randomization_focus' if width==3 else 'gadomancy:portal_focus'
     elif abs(a)==abs(b)==1:value='thaumcraft:eldritch_stone'
    elif y==0:
     if a==b==0:value='gadomancy:node_manipulator'
     elif abs(a)==abs(b)==1:value='thaumcraft:eldritch_pedestal_stone'
     elif width==7 and ((abs(a)==3 and b==0)or(abs(b)==3 and a==0)):value='gadomancy:ancient_pedestal'
    blocks.append(value)
 structure={'kind':'sequence','elements':[{'kind':'aspects','aspects':{a:100 if width==3 else 150 for a in ['aer','ignis','aqua','terra','ordo','perditio']}}]+[{'kind':'literal','value':v}for v in [width,3,width]]+[stack(blocks)]}
 display.append({'key':'gadomancy:activate_'+name,'class':'modern','data':{'kind':'compound','structure':structure}})
write('data/gadomancy/research_page_recipes/nodes.json',{'format':1,'entries':display})
entries=[];pages=[]
def research(key,x,y,complexity,aspects,parents,icon,body,**flags):
 item=dict(key='gadomancy:'+key,category='GADOMANCY',displayColumn=x,displayRow=y,pageCount=len(body),complexity=min(3,complexity),aspects=aspects,parents=parents,**flags)
 item['icon_resource'if icon.startswith('texture:')else'icon_item']=icon.removeprefix('texture:')if icon.startswith('texture:')else{'item':icon}
 entries.append(item);pages.append({'key':'gadomancy:'+key,'pages':body})
research('node_manipulator',-9,-4,2,{'alienis':8,'auram':6,'tenebrae':4,'praecantatio':8,'lucrum':4,'vacuos':10,'machina':6,'permutatio':4},['VOIDMETAL','WANDPEDFOC','PAVEWARD','WANDPED'],'gadomancy:node_manipulator',[text('NODE_MANIPULATOR',1),{'type':'INFUSION_CRAFTING','recipe':ref('node_manipulator')},text('NODE_MANIPULATOR',3),{'type':'INFUSION_CRAFTING','recipe':ref('randomization_focus')},{'type':'COMPOUND_CRAFTING','recipe':ref('activate_node_manipulator')},text('NODE_MANIPULATOR',6)],isConcealed=True,isSpecial=True,warp=4)
research('e_portal_creator',-10,0,2,{'alienis':6,'tenebrae':10,'iter':8,'auram':10,'vacuos':6,'machina':4},['gadomancy:node_manipulator','OCULUS'],'gadomancy:additional_eldritch_portal',[text('E_PORTAL_CREATOR',1),{'type':'ARCANE_CRAFTING','recipe':ref('ancient_pedestal')},text('E_PORTAL_CREATOR',3),text('E_PORTAL_CREATOR',4),{'type':'INFUSION_CRAFTING','recipe':ref('portal_focus')},text('E_PORTAL_CREATOR',6),{'type':'COMPOUND_CRAFTING','recipe':ref('activate_e_portal_creator')},text('E_PORTAL_CREATOR',8)],isConcealed=True,isSpecial=True,warp=4)
research('growing',-8,-1,5,{},['gadomancy:node_manipulator'],'texture:thaumcraft:textures/misc/r_nodes.png',[text('GROWING',1),text('GROWING',2)],isLost=True,isConcealed=True,isSpecial=True)
research('growing_aggression',-5,0,6,{},['gadomancy:growing'],'thaumcraft:aura_node',[text('GROWING_AGGRESSION',1)],isLost=True)
research('growing_growth',-7,2,6,{},['gadomancy:growing'],'texture:gadomancy:textures/misc/r_node_star.png',[text('GROWING_GROWTH',1),text('GROWING_GROWTH',2)],isLost=True)
research('growing_attack',-6,1,6,{},['gadomancy:growing'],'texture:gadomancy:textures/misc/r_node_dark.png',[text('GROWING_ATTACK',1)],isLost=True)
research('growing_growthclue',-4,3,2,{'auram':10,'praecantatio':15,'alienis':5,'vacuos':12,'potentia':12,'perditio':8,'mortuus':3,'lucrum':10,'fames':10,'machina':12,'tenebrae':12},['gadomancy:growing_aggression','gadomancy:growing_growth','gadomancy:growing_attack'],'thaumcraft:aura_node',[text('GROWING_GROWTHCLUE',1),text('GROWING_GROWTHCLUE',2)],isSpecial=True,isRound=True,isConcealed=True,warp=3)
research('ancient_stones',-12,1,3,{'alienis':10,'terra':8,'permutatio':6,'perditio':8},['OCULUS'],'thaumcraft:eldritch_stone',[text('ANCIENT_STONES',1),{'type':'ARCANE_CRAFTING','recipe':ref('ancient_stone')},{'type':'CRUCIBLE_CRAFTING','recipe':ref('ancient_stone_pedestal')}],isConcealed=True)
write('data/gadomancy/research_catalog/nodes.json',{'format':1,'categories':[{'key':'GADOMANCY','icon':'gadomancy:textures/items/category_icon.png','background':'gadomancy:textures/gui/gui_researchback.png','research':entries}]})
write('data/gadomancy/research_pages/nodes.json',{'format':1,'entries':pages})
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for aliases in data['languages'].values():
 for key,old in {'node_manipulator':'tile.BlockNodeManipulator.5.name','randomization_focus':'tile.BlockStoneMachine.0.name','portal_focus':'tile.BlockStoneMachine.3.name'}.items():aliases['block.gadomancy.'+key]=old
 for row in entries:
  key=row['key'].split(':',1)[1]
  for kind in ['name','text']:aliases[f'tc.research_{kind}.gadomancy:{key}']=f'tc.research_{kind}.GADOMANCY.{key.upper()}'
write('data/gadomancy/thaumcraft/original_resources.json',data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
