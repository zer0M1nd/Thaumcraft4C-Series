"""Generate owned block/model/recipe/research JSON from original-author rules."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def item(name):return {'item':name}
def result(name):return item('emt:'+name)
def drop(name):write('data/emt/loot_tables/blocks/'+name+'.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'emt:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
def recipe(name,value):write('data/emt/recipes/'+name+'.json',value)
catalog=[];pages=[];aliases={}
def research(key,name,x,y,aspects,parents,crafts,complexity=1,hidden=None):
 catalog.append(dict(key='emt:'+key,category='EMT',displayColumn=x,displayRow=y,pageCount=1+len(crafts),complexity=complexity,aspects=aspects,parents=parents,parentsHidden=hidden or [],isSecondary=complexity==1,isConcealed=True,icon_item=item('emt:'+key)))
 pages.append(dict(key='emt:'+key,pages=[dict(type='TEXT',text='emt.text.'+name)]+[dict(type=kind,recipe=dict(kind='reference',keys=['emt:'+r],**{'class':'modern'})) for r,kind in crafts]))
 aliases['tc.research_name.emt:'+key]='emt.rsname.'+name;aliases['tc.research_text.emt:'+key]='emt.tag.'+name
primals=dict(water='aqua',dark='perditio',order='ordo',fire='ignis',air='aer',earth='terra')
coords=dict(water=(-7,-7),dark=(-8,-6),order=(-6,-8),fire=(-5,-7),air=(-4,-6),earth=(-6,-7))
for kind in ['compressed',*primals]:
 for tier,prefix in enumerate(['','double_','triple_']):
  name=prefix+kind+'_solar';side='side' if kind=='compressed' else kind+'/'+kind+'side';top=['solartop','doublesolartop','triplesolartop'][tier]
  write('assets/emt/models/block/'+name+'.json',{'parent':'minecraft:block/cube_bottom_top','textures':{'top':'emt:blocks/solars/'+top,'bottom':'emt:blocks/solars/bottom','side':'emt:blocks/solars/'+side}})
  write('assets/emt/blockstates/'+name+'.json',{'variants':{'':{'model':'emt:block/'+name}}})
  write('assets/emt/models/item/'+name+'.json',{'parent':'emt:block/'+name});drop(name)
  aliases['block.emt.'+name]='tile.EMT.solar'+('2' if kind=='earth' or kind=='air' and tier>0 else '')+'.'+prefix.replace('_','')+kind+'.name'
  if kind=='compressed':
   if tier<2:recipe(name,dict(type='thaumcraft:arcane',mode='shaped',research='emt:compressed_solar',pattern=['XXX']*3,key={'X':item('ic2:solar_generator' if tier==0 else 'emt:compressed_solar')},vis={a:8 for a in ['aer','aqua','ignis','terra','ordo','perditio']},result=result(name)))
   else:recipe(name,dict(type='thaumcraft:infusion',research='emt:compressed_solar',instability=2,aspects={a:8 for a in ['aer','aqua','ignis','terra','ordo','perditio']},center={'ingredient':item('emt:double_compressed_solar')},components=[{'ingredient':item('emt:double_compressed_solar')}]*8,result=result(name)))
  else:recipe(name,dict(type='thaumcraft:crucible',research='emt:'+kind+'_solar',catalyst=item('emt:'+prefix+'compressed_solar'),aspects={primals[kind]:16,'praecantatio':16,'permutatio':16},result=result(name)))
 if kind=='compressed':research('compressed_solar','Compressed Solars',-4,-5,{'potentia':4,'lux':3,'metallum':2},[],[('compressed_solar','ARCANE_CRAFTING'),('double_compressed_solar','ARCANE_CRAFTING'),('triple_compressed_solar','INFUSION_CRAFTING')],2,['emt:potentia_generator'])
 else:research(kind+'_solar',('Entropy' if kind=='dark' else kind.title())+' Infused Solar Panels',*coords[kind],{primals[kind]:4,'praecantatio':3,'potentia':4},['emt:compressed_solar'],[(p+kind+'_solar','CRUCIBLE_CRAFTING') for p in ['','double_','triple_']])

for aspect,x,y in [('potentia',-2,3),('ignis',-3,4),('auram',-1,4),('arbor',-2,5),('aer',-2,6)]:
 name=aspect+'_generator'
 textures={'top':'emt:blocks/essentiagenerator/top','bottom':'emt:blocks/essentiagenerator/top','side':'emt:blocks/essentiagenerator/side','front':'emt:blocks/essentiagenerator/'+aspect+'front'}
 write('assets/emt/models/block/'+name+'.json',{'parent':'minecraft:block/orientable','textures':textures})
 write('assets/emt/blockstates/'+name+'.json',{'variants':{'facing='+d:{'model':'emt:block/'+name,'y':angle} for d,angle in [('north',0),('east',90),('south',180),('west',270)]}})
 write('assets/emt/models/item/'+name+'.json',{'parent':'emt:block/'+name});drop(name)
 aliases['block.emt.'+name]='tile.EMT.essentia.'+aspect+'.name'
 if aspect=='potentia':
  recipe(name,dict(type='thaumcraft:infusion',research='emt:'+name,instability=6,aspects={a:32 for a in ['potentia','praecantatio','metallum','permutatio']},center={'ingredient':item('ic2:semifluid_generator')},components=[{'ingredient':item(i)} for i in ['minecraft:diamond','thaumcraft:focus_trade','minecraft:hopper','thaumcraft:essentia_jar','ic2:mv_transformer','ic2:advanced_machine','ic2:ore_washing_plant','ic2:scrap']],result=result(name)))
  research(name,aspect.title()+' Generator',x,y,{'potentia':4,'permutatio':4,'metallum':3},['emt:foundation'],[(name,'INFUSION_CRAFTING')],3)
 else:
  recipe(name,dict(type='thaumcraft:crucible',research='emt:'+name,catalyst=item('emt:potentia_generator'),aspects={aspect:8 if aspect=='aer' else 16,'permutatio':16 if aspect=='aer' else 8},result=result(name)))
  research(name,aspect.title()+' Generator',x,y,{aspect:3,'permutatio':4},['emt:potentia_generator'],[(name,'CRUCIBLE_CRAFTING')])
write('data/emt/research_catalog/energy.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=catalog)]))
write('data/emt/research_pages/energy.json',dict(format=1,entries=pages))
path=root/'data/emt/thaumcraft/original_resources.json';data=json.loads(path.read_text(encoding='utf8'))
for lang in ['en_us','zh_cn']:data['languages']['assets/emt/lang/'+lang+'.json'].update(aliases)
write('data/emt/thaumcraft/original_resources.json',data)
write('data/minecraft/tags/blocks/mineable/pickaxe.json',{'replace':False,'values':['emt:'+p.stem for p in (root/'assets/emt/blockstates').glob('*.json') if p.stem not in ('portable_node','electric_cloud','shield')]})

# The book keeps explicit display recipes, generated from the same definitions to avoid drift.
def stack(value):
 if value is None:return dict(kind='sequence',elements=[])
 if isinstance(value,list):return dict(kind='sequence',elements=[stack(v) for v in value])
 if 'ingredient' in value:value=value['ingredient']
 if isinstance(value,list):return dict(kind='sequence',elements=[stack(v) for v in value])
 if 'item' in value:return dict(kind='stack',stack=dict(value,count=value.get('count',1)))
 if 'tag' in value:return dict(kind='ore',names=[value['tag']],elements=[])
 raise ValueError(value)
display=[]
for path in (root/'data/emt/recipes').glob('*.json'):
 r=json.loads(path.read_text());typ=r['type']
 if typ not in ['thaumcraft:infusion','thaumcraft:crucible','thaumcraft:arcane','minecraft:crafting_shaped','minecraft:crafting_shapeless','emt:thaumium_plate']:continue
 d={'output':stack(r['result']),'research':r.get('research','')}
 if typ=='thaumcraft:infusion':d.update(kind='infusion',input=stack(r['center']),ingredients=stack(r['components']),aspects=r['aspects'],instability=r['instability'])
 elif typ=='thaumcraft:crucible':d.update(kind='crucible',catalyst=stack(r['catalyst']),aspects=r['aspects'])
 elif typ in ['thaumcraft:arcane','minecraft:crafting_shaped','minecraft:crafting_shapeless','emt:thaumium_plate']:
  mode=r.get('mode','shaped' if 'pattern' in r else 'shapeless');kind=('arcane_' if typ=='thaumcraft:arcane' else 'crafting_')+mode
  d.update(kind=kind,aspects=r.get('vis',{}))
  if mode=='shaped':d.update(width=len(r['pattern'][0]),height=len(r['pattern']),ingredients=stack([r['key'].get(c) for row in r['pattern'] for c in row]))
  else:d.update(ingredients=stack(r['ingredients']))
 else:continue
 display.append({'key':'emt:'+path.stem,'class':'modern','data':d})
write('data/emt/research_page_recipes/main.json',dict(format=1,entries=display))

# EMTResearchItem.setPages adds INFUSION whenever a research has an infusion page.
infusion_research=set()
for path in (root/'data/emt/research_pages').glob('*.json'):
 for entry in json.loads(path.read_text(encoding='utf8')).get('entries',[]):
  if any(page.get('type')=='INFUSION_CRAFTING' for page in entry.get('pages',[])):infusion_research.add(entry['key'])
for path in (root/'data/emt/research_catalog').glob('*.json'):
 data=json.loads(path.read_text(encoding='utf8'));changed=False
 for category in data.get('categories',[]):
  for entry in category.get('research',[]):
   if 'hiddenParents' in entry:entry.setdefault('parentsHidden',[]).extend(entry.pop('hiddenParents'));changed=True
   if entry['key'] in infusion_research and 'INFUSION' not in entry.get('parentsHidden',[]):entry.setdefault('parentsHidden',[]).append('INFUSION');changed=True
 if changed:write(path.relative_to(root),data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
