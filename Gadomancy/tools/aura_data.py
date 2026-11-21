from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def stack(v):
 if v is None:return dict(kind='sequence',elements=[])
 if isinstance(v,list):return dict(kind='sequence',elements=[stack(e) for e in v])
 if isinstance(v,str):v={'item':v}
 if 'ingredient' in v:v=v['ingredient']
 return dict(kind='stack',stack=dict(v,count=v.get('count',1)))
def ref(keys):return dict(kind='reference',**{'class':'modern'},keys=keys)
def text(key,page):return dict(type='TEXT',text=f'gadomancy.research_page.{key}.{page}')
def page(typ,*keys):return dict(type=typ,recipe=ref(list(keys)))
primals=['aer','ignis','aqua','terra','ordo','perditio']
recipes={}
def recipe(name,data):recipes[name]=data;write(f'data/gadomancy/recipes/{name}.json',data)
for a in ['blank']+primals:
 write(f'assets/gadomancy/models/item/aura_core_{a}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'gadomancy:items/core_core'+('_blank' if a=='blank' else ''),'layer1':'gadomancy:items/core_border'}})
 if a=='blank':continue
 recipe('wash_aura_core_'+a,dict(type='thaumcraft:crucible',research='gadomancy:clean_aura_core',catalyst={'item':'gadomancy:aura_core_'+a},aspects={'praecantatio':12,'aqua':12,'sano':18},result={'item':'gadomancy:aura_core_blank'}))
 if a in ['terra','perditio']:recipe('undo_aura_core_'+a,dict(type='minecraft:crafting_shapeless',ingredients=[{'item':'gadomancy:aura_core_'+a}],result={'item':'gadomancy:aura_core_blank'}))
recipe('aura_core',dict(type='gadomancy:aura_core',research='gadomancy:aura_core',instability=6,center={'ingredient':{'item':'thaumcraft:primal_charm'}},components=[{'ingredient':{'item':item}} for item in ['thaumcraft:balanced_shard','thaumcraft:thaumium_ingot','thaumcraft:thaumium_ingot']*4],aspects={'auram':9,'praecantatio':19,'alienis':17,'vacuos':34},result={'item':'gadomancy:aura_core_blank'}))
recipe('arcane_packager',dict(type='gadomancy:packager',mode='shaped',research='gadomancy:arcane_packager',pattern=['PSP','GCG','JTJ'],key={k:{'item':v} for k,v in {'P':'thaumcraft:greatwood_planks','S':'minecraft:piston','G':'thaumcraft:warded_glass','C':'gadomancy:aura_core_aer','J':'thaumcraft:essentia_jar','T':'thaumcraft:arcane_workbench'}.items()},vis={'aer':100,'ordo':60,'perditio':50},result={'item':'gadomancy:arcane_packager'}))
research=[dict(key='gadomancy:aura_core',displayColumn=-1,displayRow=6,pageCount=4,complexity=2,aspects=dict(auram=22,praecantatio=10,**dict.fromkeys(primals,8)),parents=['INFUSION','THAUMIUM'],isConcealed=True,isSpecial=True,icon_item={'item':'gadomancy:aura_core_blank'}),dict(key='gadomancy:clean_aura_core',displayColumn=1,displayRow=5,pageCount=3,complexity=0,aspects={'sano':8,'auram':6,'praecantatio':8,'aqua':10},parents=['gadomancy:aura_core'],isConcealed=True,isSecondary=True,icon_item={'item':'gadomancy:aura_core_blank'}),dict(key='gadomancy:arcane_packager',displayColumn=2,displayRow=7,pageCount=4,complexity=2,aspects={'aer':12,'machina':16,'vacuos':10,'praecantatio':10},parents=['gadomancy:aura_core','WARDEDARCANA'],isHidden=True,itemTriggers=[{'item':'gadomancy:aura_core_aer'}],icon_item={'item':'gadomancy:arcane_packager'})]
for r in research:r['category']='GADOMANCY'
write('data/gadomancy/research_catalog/aura_core.json',{'format':1,'categories':[{'key':'GADOMANCY','icon':'gadomancy:textures/items/category_icon.png','background':'gadomancy:textures/gui/gui_researchback.png','research':research}]})
entries=[{'key':'gadomancy:aura_core','pages':[text('AURA_CORE',1),page('INFUSION_CRAFTING','gadomancy:aura_core'),text('AURA_CORE',3),page('COMPOUND_CRAFTING',*[f'gadomancy:activate_aura_core_{a}' for a in primals+['balanced']])]}, {'key':'gadomancy:clean_aura_core','pages':[text('CLEAN_AURA_CORE',1),page('CRUCIBLE_CRAFTING',*[f'gadomancy:wash_aura_core_{a}' for a in primals]),page('NORMAL_CRAFTING','gadomancy:undo_aura_core_terra','gadomancy:undo_aura_core_perditio')]},{'key':'gadomancy:arcane_packager','pages':[text('ARCANE_PACKAGER',1),dict(type='gadomancy:packager_recipe',data={}),text('ARCANE_PACKAGER',3),text('ARCANE_PACKAGER',4)]}]
write('data/gadomancy/research_pages/aura_core.json',dict(format=1,entries=entries))
display=[]
for name,r in recipes.items():
 typ=r['type'];d={'output':stack(r['result']),'research':r.get('research','')}
 if typ=='gadomancy:aura_core':d.update(kind='infusion',input=stack(r['center']),ingredients=stack(r['components']),aspects=r['aspects'],instability=r['instability'])
 elif typ=='thaumcraft:crucible':d.update(kind='crucible',catalyst=stack(r['catalyst']),aspects=r['aspects'])
 elif typ=='gadomancy:packager':d.update(kind='arcane_shaped',width=3,height=3,aspects=r['vis'],ingredients=stack([r['key'].get(c) for row in r['pattern'] for c in row]))
 else:d.update(kind='crafting_shapeless',ingredients=stack(r['ingredients']))
 display.append(dict(key='gadomancy:'+name,**{'class':'modern'},data=d))
for a,cluster in zip(primals+['balanced'],['air','fire','water','earth','order','entropy','mixed']):
 blocks=[None]*18;blocks[4]={'item':'thaumcraft:casting_wand','nbt':'{rod:"greatwood",cap:"gold"}'};blocks[13]={'item':'thaumcraft:'+cluster+'_crystal_cluster'};blocks[14]={'item':'gadomancy:aura_core_blank'}
 costs={a:84} if a!='balanced' else dict.fromkeys(primals,14)
 structure={'kind':'sequence','elements':[{'kind':'aspects','aspects':costs}]+[{'kind':'literal','value':v} for v in [3,2,3]]+[stack(blocks)]}
 display.append(dict(key='gadomancy:activate_aura_core_'+a,**{'class':'modern'},data=dict(kind='compound',structure=structure)))
write('data/gadomancy/research_page_recipes/aura_core.json',dict(format=1,entries=display))
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:
 aliases=data.setdefault('languages',{}).setdefault('assets/gadomancy/lang/'+lang+'.json',{})
 for key in ['aura_core','clean_aura_core','arcane_packager']:
  for typ in ['name','text']:aliases[f'tc.research_{typ}.gadomancy:{key}']=f'tc.research_{typ}.GADOMANCY.{key.upper()}'
write('data/gadomancy/thaumcraft/original_resources.json',data)

# Observed aura effects use the shared data-pack append protocol, not a page-list mixin.
import re
_effects_source=(root.parent/'java/gadomancymodern/aura/StandardAuraEffects.java').read_text(encoding='utf8')
_effects=sorted(set(re.findall(r'(?:AuraEffectRegistry\.register|potion)\("([a-z_]+)"',_effects_source)))
_rows=[dict(id=a,value=a,when={'research':'gadomancy:aura/'+a},conditions=[{'type':'forge:not','value':
    {'type':'thaumcraft:config','modid':'gadomancy','scope':'common','key':'blacklistAuraEffects','operator':'contains','value':a}}]) for a in _effects]
_addition={'format':1,'entries':[{'id':'gadomancy:observed_aura_effects','research':'gadomancy:aura_effects','position':'end','priority':0,
    'components':[{'id':'gadomancy:observed_aura_effects/list','type':'gadomancy:aura_effect_list','data':{},'group':{'size':4,'field':'aspects','entries':_rows}}]}]}
_target=root.parents[1]/'generated/resources/data/gadomancy/research_page_additions/aura_effects.json'
_target.parent.mkdir(parents=True,exist_ok=True);_target.write_text(json.dumps(_addition,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
