from pathlib import Path
import zipfile,json,hashlib,re
p=Path(__file__).resolve().parents[1];root=p.parent.parent
jar=root/'Addons/references/phase2/thaumicinfusion/ThaumicInfusion-4.32.jar'
def out(path,data):
 f=p/'src/generated/resources'/path;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
with zipfile.ZipFile(jar) as z:
 mapping={n.lower():n for n in z.namelist() if n.startswith('assets/') and not n.endswith('/')}
 aliases={'item.thaumicinfusion.focus_infusion':'item.FocusInfusion.name','item.thaumicinfusion.coordinate_paper':'item.CoordinatePaper.name'}
 for key in ['FOCUSINFUSION','ESSENTIABLOCKS']:
  for kind in ['name','text']:aliases[f'tc.research_{kind}.thaumicinfusion:{key}']=f'tc.research_{kind}.{key}'
 languages={}
 for n in list(mapping.values()):
  if not n.endswith('.lang'):continue
  target=n.lower().removesuffix('.lang')+'.json'; mapping[target]=n
  keys={line.split('=',1)[0]:line.split('=',1)[0] for line in z.read(n).decode('utf-8-sig').splitlines() if '=' in line and not line.startswith('#')}
  keys.update(aliases);languages[target]=keys
 # Keep reviewed aliases shared with the optional legacy Chinese fallback declaration.
 for source in json.loads((p/'src/main/resources/data/thaumicinfusion/thaumcraft/legacy_localization.json').read_text('utf-8'))['sources']:
  for keys in languages.values():
   for modern,legacy in source['keys'].items():
    if legacy in keys:keys.setdefault(modern,legacy)
 out('data/thaumicinfusion/thaumcraft/original_resources.json',{'name':'Thaumic Infusion 1.7.10-4.32 Hotfix','legacy_modid':'thaumicinfusion','sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'download':'https://www.curseforge.com/minecraft/mc-mods/thaumic-infusion/files/2275434','mapping':mapping,'languages':languages})
out('assets/thaumicinfusion/models/item/focus_infusion.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumicinfusion:items/focus_infusion','layer1':'thaumicinfusion:items/focus_infusion_orn'}})
out('assets/thaumicinfusion/models/item/coordinate_paper.json',{'parent':'minecraft:item/generated','textures':{'layer0':'thaumcraft:item/researchnotes'}})
# Keep reviewed translations as source data so regeneration cannot erase them.
locales={file.stem:json.loads(file.read_text('utf-8')) for file in sorted((p/'src/main/localization').glob('*.json'))}
for language,translations in locales.items():
 out(f'assets/thaumicinfusion/lang/{language}.json',translations)
effects=re.findall(r'add\("([a-z]+)",(\d+),(\d+),', (p/'src/main/java/timodern/InfusionEffects.java').read_text('utf-8'))
if not effects:raise ValueError('No infusion definitions found')
for language in ['en_us','zh_cn']:
 for aspect,cost,interval in effects:
  for prefix in ['ti.effect_detail.','ti.effect_info.']:
   if prefix+aspect.upper() not in locales[language]:raise ValueError(f'Missing {language}: {prefix}{aspect.upper()}')
aspect_arguments=json.loads((p/'src/main/book/aspect_arguments.json').read_text('utf-8'))
def translated(key):
 value={'translate':key}
 if key in aspect_arguments:value['with']=[{'aspect':tag} for tag in aspect_arguments[key]]
 return value
out('data/thaumicinfusion/research_text_replacements/localization.json',json.loads((p/'src/main/book/replacements.json').read_text('utf-8')))
for name,tex in [('essentia_block','essentiablock'),('essentia_bricks','bricks_essentiablock'),('chiseled_essentia','squarebrick_essentiablock')]:
 out(f'assets/thaumicinfusion/blockstates/{name}.json',{'variants':{'':{'model':f'thaumicinfusion:block/{name}'}}})
 out(f'assets/thaumicinfusion/models/block/{name}.json',{'parent':'minecraft:block/block','textures':{'all':f'thaumicinfusion:blocks/{tex}','particle':'#all'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':{face:{'texture':'#all','tintindex':0,'cullface':face} for face in ['north','south','east','west','up','down']}}]})
 out(f'assets/thaumicinfusion/models/item/{name}.json',{'parent':f'thaumicinfusion:block/{name}'})
 out(f'data/thaumicinfusion/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'thaumicinfusion:{name}','functions':[{'function':'minecraft:copy_nbt','source':'block_entity','ops':[{'source':'aspect','target':'BlockEntityTag.aspect','op':'replace'}]}]}]}]})
out('data/minecraft/tags/blocks/mineable/pickaxe.json',{'replace':False,'values':['thaumicinfusion:essentia_block','thaumicinfusion:essentia_bricks','thaumicinfusion:chiseled_essentia']})
out('data/thaumicinfusion/recipes/focus_infusion.json',{'type':'thaumcraft:infusion','research':'thaumicinfusion:FOCUSINFUSION','instability':4,'aspects':{'terra':25,'tutamen':25,'ordo':25,'cognitio':10},'center':{'ingredient':{'item':'thaumcraft:focus_warding'}},'components':[{'ingredient':{'item':i}} for i in ['thaumcraft:quicksilver','thaumcraft:earth_shard','minecraft:prismarine_crystals','thaumcraft:order_shard']*2],'result':{'item':'thaumicinfusion:focus_infusion'}})
# Legacy Items.field_151128_bU is nether quartz; resolve against 1.7 mappings rather than newer item order.
f=p/'src/generated/resources/data/thaumicinfusion/recipes/focus_infusion.json';s=f.read_text();f.write_text(s.replace('minecraft:prismarine_crystals','minecraft:quartz'),encoding='utf-8')
category={'key':'THAUMICINFUSION','icon':'thaumicinfusion:textures/research/r_ti.png','background':'thaumicinfusion:textures/research/r_tibg.png','research':[]};pages=[]
for key,x,item,aspects,texts in [('FOCUSINFUSION',-2,'focus_infusion',{'terra':6,'tutamen':3,'ordo':3,'cognitio':3},3),('ESSENTIABLOCKS',2,'essentia_block',{'ordo':3,'praecantatio':3},1)]:
 research={'key':'thaumicinfusion:'+key,'category':'THAUMICINFUSION','displayColumn':x,'displayRow':0,'complexity':2,'aspects':aspects,'warp':0,'icon_item':{'item':'thaumicinfusion:'+item,'count':1}}
 category['research'].append(research)
 # Original prose stays intact; the core applies independent inline corrections,
 # then converts aspect references to the native book components.
 ps=[{'type':'TEXT','text':{'translate':f'tc.research_page.{key}.{i}'},'import_legacy':True} for i in range(1,texts+1)]
 if key=='FOCUSINFUSION':ps.insert(1,{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':['thaumicinfusion:focus_infusion'],'class':'modern'}})
 if key=='ESSENTIABLOCKS':ps.append({'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','keys':['thaumicinfusion:essentia_block','thaumicinfusion:essentia_bricks','thaumicinfusion:chiseled_essentia'],'class':'modern'}})
 for number,page in enumerate(ps,1):
  page['id']=f'thaumicinfusion:{key}/page/{number}';page['page_break']=True
 if key=='FOCUSINFUSION':
  for section in ['usage','effects','linking']:
   ps.append({'id':f'thaumicinfusion:FOCUSINFUSION/{section}','type':'TEXT','page_break':True,'text':translated('ti.book.'+section)})
  for aspect,cost,interval in effects:
   prefix=f'thaumicinfusion:FOCUSINFUSION/effect/{aspect}'
   # Optional aspect references must never be decoded without their provider.
   condition={'conditions':[{'type':'forge:mod_loaded','modid':'forbidden_magic'}]} if aspect=='infernus' else {}
   ps.append({'id':prefix,'type':'TEXT','page_break':True,**condition,'text':{'text':'','extra':[
    {'aspect':'forbidden_magic:infernus' if aspect=='infernus' else aspect},
    {'text':'\n\n'}, {'translate':'ti.book.cost','with':[int(cost)]}, {'text':'\n\n'},
   ]}})
   ps.append({'id':prefix+'/original','type':'TEXT','page_break':False,'import_legacy':True,**condition,'text':{'translate':'ti.effect_info.'+aspect.upper()}})
   ps.append({'id':prefix+'/details','type':'TEXT','page_break':False,**condition,'text':{'text':'','extra':[
    {'translate':'ti.book.details','italic':True},translated('ti.effect_detail.'+aspect.upper())
   ]}})
 if key=='ESSENTIABLOCKS':
  ps.insert(1,{'id':'thaumicinfusion:ESSENTIABLOCKS/details','type':'TEXT','page_break':False,'text':translated('ti.book.essentia_details')})
 research['pageCount']=len(ps)
 pages.append({'key':'thaumicinfusion:'+key,'pages':ps})
out('data/thaumicinfusion/research_catalog/core.json',{'format':1,'categories':[category]})
out('data/thaumicinfusion/research_pages/core.json',{'format':1,'entries':pages})
out('thaumicinfusion.mixins.json',{'required':True,'minVersion':'0.8','package':'timodern.mixin','compatibilityLevel':'JAVA_17','mixins':['InfusedStateMixin','InfusedBlockMixin','InfusedFallingMixin'],'client':['InfusedRendererMixin'],'injectors':{'defaultRequire':1}})

for stage,(item,source) in enumerate([('essentia_block','thaumcraft:essentia_phial'),('essentia_bricks','thaumicinfusion:essentia_block'),('chiseled_essentia','thaumicinfusion:essentia_bricks')]):
 out(f'data/thaumicinfusion/recipes/{item}.json',{'type':'thaumicinfusion:essentia','stage':stage,'research':'thaumicinfusion:ESSENTIABLOCKS','mode':'shaped','pattern':['PP','PP'],'key':{'P':{'item':source}},'vis':{'perditio':4},'result':{'item':f'thaumicinfusion:{item}','nbt':{'BlockEntityTag':{'aspect':'aer'}}}})

def stack(item,count=1,nbt=None):
 d={'item':item,'count':count}
 if nbt:d['nbt']=nbt
 return {'kind':'stack','stack':d}
entries=[]
for file in sorted((p/'src/generated/resources/data/thaumicinfusion/recipes').glob('*.json')):
 r=json.loads(file.read_text(encoding='utf-8'));d={'research':r['research'],'output':stack(r['result']['item'],1),'aspects':r.get('aspects',r.get('vis',{}))}
 if r['type']=='thaumcraft:infusion':d.update(kind='infusion',input=stack(r['center']['ingredient']['item']),instability=r['instability'],ingredients={'kind':'sequence','elements':[stack(i['ingredient']['item']) for i in r['components']]})
 else:d.update(kind='arcane_shaped',width=2,height=2,mirrored=True,ingredients={'kind':'sequence','elements':[stack(r['key']['P']['item'])]*4})
 entries.append({'key':'thaumicinfusion:'+file.stem,'class':'modern','data':d})
out('data/thaumicinfusion/research_page_recipes/core.json',{'format':1,'entries':entries})

out("assets/thaumicinfusion/models/item/focus_infusion_orn.json",{"parent":"minecraft:item/generated","textures":{"layer0":"thaumicinfusion:items/focus_infusion_orn"}})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
