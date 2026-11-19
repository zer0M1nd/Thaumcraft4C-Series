from convert_legacy_data import *
import zipfile,hashlib
jar=ROOT/'Addons/references/phase2/taintedmagic/Tainted-Magic-1.7.10-8.1.1.jar' if 'ROOT' in globals() else P.parent.parent/'Addons/references/phase2/taintedmagic/Tainted-Magic-1.7.10-8.1.1.jar'
aliases={}
for rod in ["warpwood"]:aliases["item.Wand.taintedmagic:"+rod+".rod"]="item.Wand."+rod+".rod"
for cap in ["shadowmetal","cloth","crimsoncloth","shadowcloth"]:aliases["item.Wand.taintedmagic:"+cap+".cap"]="item.Wand."+cap+".cap"
for name,id in OWN.items():aliases[('block.' if name.startswith('Block') else 'item.')+'taintedmagic.'+id]=('tile.' if name.startswith('Block') else 'item.')+name+'.name'
for name,ids in META.items():
 for n,id in enumerate(ids):aliases['item.taintedmagic.'+id]='item.'+name+'.'+str(n)+'.name'
categories={'key':'TAINTEDMAGIC','icon':'taintedmagic:textures/misc/tab_tm.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[]};pages=[]
for match in re.finditer(r'new TMResearchItem\(',researchSource):
 body,end=balanced(researchSource,match.end()-1);args=split(body);key=json.loads(args[0]);tail=researchSource[end+1:researchSource.index('registerResearchItem()',end)];entry={'key':research(key),'category':'TAINTEDMAGIC','displayColumn':int(args[2]),'displayRow':int(args[3]),'complexity':int(args[5]),'warp':int(args[6]),'aspects':aspects(args[1])}
 if args[4].startswith('new ResourceLocation('):entry['icon_resource']=json.loads(split(balanced(args[4],args[4].index('('))[0])[0]).lower()
 else:entry['icon_item']=item(args[4])
 for method,field in [('setParents','parents'),('setParentsHidden','parentsHidden'),('setSiblings','siblings')]:
  m=re.search(r'\.'+method+r'\(',tail)
  if m:entry[field]=[research(json.loads(x)) for x in split(balanced(tail,m.end()-1)[0])]
 for method,field in [('setConcealed','concealed'),('setSecondary','secondary'),('setSpecial','special'),('setRound','round'),('setAutoUnlock','autoUnlock'),('setHidden','hidden'),('setVirtual','virtual'),('setLost','lost')]:
  if '.'+method+'(' in tail:entry['is'+field[0].upper()+field[1:]]=True
 ps=[];m=re.search(r'\.setPages\(',tail)
 if m:
  for page in split(balanced(tail,m.end()-1)[0]):
   if page.startswith('new ResearchPage('):ps.append({'type':'TEXT','text':'tm.text.'+key+'.'+json.loads(split(balanced(page,page.index('('))[0])[0])})
   else:
    kind=page[:page.index('(')];recipe=json.loads(balanced(page,page.index('('))[0]);ps.append({'type':{'recipePage':'NORMAL_CRAFTING','arcanePage':'ARCANE_CRAFTING','infusionPage':'INFUSION_CRAFTING','cruciblePage':'CRUCIBLE_CRAFTING'}[kind],'recipe':{'kind':'reference','keys':[RECIPES[recipe]],'class':'modern'}})
 if any(p['type']=='INFUSION_CRAFTING' for p in ps) and 'parentsHidden' not in entry:entry['parentsHidden']=['INFUSION']
 entry['pageCount']=len(ps);categories['research'].append(entry);pages.append({'key':research(key),'pages':ps})
 for kind,legacy in [('name','name'),('text','tag')]:aliases[f'tc.research_{kind}.{research(key)}']=f'tm.{legacy}.{key}'
# Legacy shadow tab stub follows the actual eldritch unlock; no additional paid research.
categories['research'].append({'key':'taintedmagic:TMELDRITCHMAJOR','mirrorOf':'ELDRITCHMAJOR','category':'TAINTEDMAGIC','displayColumn':7,'displayRow':-7,'complexity':0,'warp':0,'aspects':{},'icon_resource':'thaumcraft:textures/misc/r_eldritchmajor.png','parents':[],'isSpecial':True,'isRound':True,'isStub':True,'pageCount':0})
for k in ['name','text']:aliases['tc.research_'+k+'.taintedmagic:TMELDRITCHMAJOR']='tc.research_'+k+'.ELDRITCHMAJOR'
for entry in categories['research']:
 if 'taintedmagic:TMELDRITCHMAJOR' in entry.get('parents',[]):
  entry['parents']=['ELDRITCHMAJOR' if k=='taintedmagic:TMELDRITCHMAJOR' else k for k in entry['parents']]
  entry.setdefault('parentMirrors',{})['ELDRITCHMAJOR']='taintedmagic:TMELDRITCHMAJOR'
emit('data/taintedmagic/research_catalog/core.json',{'format':1,'categories':[categories]});emit('data/taintedmagic/research_pages/core.json',{'format':1,'entries':pages})
with zipfile.ZipFile(jar) as z:
 mapping={n.lower():n for n in z.namelist() if n.startswith('assets/') and not n.endswith('/')};languages={}
 for n in list(mapping.values()):
  if not n.endswith('.lang'):continue
  target=n.lower().removesuffix('.lang')+'.json';mapping[target]=n;keys={line.split('=',1)[0]:line.split('=',1)[0] for line in z.read(n).decode('utf-8-sig').splitlines() if '=' in line and not line.startswith('#')};keys.update(aliases);languages[target]=keys
 # Keep reviewed aliases shared with the optional legacy Chinese fallback declaration.
 for source in json.loads((P/'src/main/resources/data/taintedmagic/thaumcraft/legacy_localization.json').read_text('utf-8'))['sources']:
  for keys in languages.values():
   for modern,legacy in source['keys'].items():keys.setdefault(modern,legacy)
 emit('data/taintedmagic/thaumcraft/original_resources.json',{'name':'Tainted Magic 1.7.10-8.1.1','legacy_modid':'TaintedMagic','sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'download':'https://www.curseforge.com/minecraft/mc-mods/tainted-magic/files/3634109','mapping':mapping,'languages':languages})
 # Every texture comes from the original author JAR, including animation metadata.
 for name,id in OWN.items():
  if name.startswith('Block'):continue
  tex='taintedmagic:items/'+name.lower();layers={'layer0':tex};orn=tex+'_orn';depth=tex+'_depth'
  # Focus depth layers belong to the attached-wand renderer, never the item icon.
  if not id.startswith('focus_') and 'assets/'+depth.replace(':','/textures/')+'.png' in mapping:layers['layer1']=depth
  if 'assets/'+orn.replace(':','/textures/')+'.png' in mapping:layers['layer'+str(len(layers))]=orn
  if id=='gate_key':layers['layer1']=tex+'_overlay'
  emit('assets/taintedmagic/models/item/'+id+'.json',{'parent':'minecraft:item/handheld' if id in ['primal_blade','hollow_dagger','thaumic_disassembler'] or 'shadowmetal_' in id else 'minecraft:item/generated','textures':layers})
 for name,ids in META.items():
  for n,id in enumerate(ids):
   if name=='ItemKatana':emit('assets/taintedmagic/models/item/'+id+'.json',{'parent':'builtin/entity','gui_light':'front'});continue
   candidates=[name.lower()+str(n),name.lower()+'_'+str(n),name.lower()+'.'+str(n)]
   found=next((v for v in candidates if 'assets/taintedmagic/textures/items/'+v+'.png' in mapping),None)
   if not found:raise ValueError((name,n,[k for k in mapping if name.lower() in k]))
   emit('assets/taintedmagic/models/item/'+id+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'taintedmagic:items/'+found}})
emit('assets/taintedmagic/lang/en_us.json',{'itemGroup.taintedmagic':'Tainted Magic'});emit('assets/taintedmagic/lang/zh_cn.json',{'itemGroup.taintedmagic':'污秽魔法'})
for name in ['warpwood_planks','warpwood_leaves']:
 tex='taintedmagic:blocks/block'+name.replace('_','')+('_transparent' if name.endswith('leaves') else '')
 emit('assets/taintedmagic/models/block/'+name+'.json',{'parent':'minecraft:block/cube_all','textures':{'all':tex}})
 emit('assets/taintedmagic/blockstates/'+name+'.json',{'variants':{'':{'model':'taintedmagic:block/'+name}}})
for name in ['warpwood_sapling','nightshade_bush']:
 tex='taintedmagic:blocks/block'+name.replace('_','');emit('assets/taintedmagic/models/block/'+name+'.json',{'parent':'minecraft:block/cross','textures':{'cross':tex}});emit('assets/taintedmagic/blockstates/'+name+'.json',{'variants':{'':{'model':'taintedmagic:block/'+name}}})
 emit('assets/taintedmagic/models/item/'+name+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':tex}})
variants={}
for knot in [False,True]:
 model='warpwood_log'+('_knot' if knot else '');emit('assets/taintedmagic/models/block/'+model+'.json',{'parent':'minecraft:block/cube_column','textures':{'end':'taintedmagic:blocks/blockwarpwoodlog_top','side':'taintedmagic:blocks/blockwarpwoodlog'+('_knot' if knot else '_side')}})
 for axis in ['x','y','z']:
  value={'model':'taintedmagic:block/'+model};value.update({'x':90,'y':90} if axis=='x' else {'x':90} if axis=='z' else {});variants[f'axis={axis},knot={str(knot).lower()}']=value
emit('assets/taintedmagic/blockstates/warpwood_log.json',{'variants':variants})
for name in ['warpwood_log','warpwood_planks','warpwood_leaves']:emit('assets/taintedmagic/models/item/'+name+'.json',{'parent':'taintedmagic:block/'+name})
emit('assets/taintedmagic/blockstates/lumos.json',{'variants':{'':{'model':'minecraft:block/air'}}});emit('assets/taintedmagic/models/item/lumos.json',{'parent':'minecraft:item/generated','textures':{'layer0':'taintedmagic:items/itemfocuslumos'}})
for name in ['warpwood_planks','warpwood_sapling']:emit('data/taintedmagic/loot_tables/blocks/'+name+'.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'taintedmagic:'+name}]}]})
emit('data/taintedmagic/loot_tables/blocks/warpwood_leaves.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[{'type':'minecraft:item','name':'taintedmagic:warpwood_leaves','conditions':[{'condition':'minecraft:match_tool','predicate':{'items':['minecraft:shears']}}]},{'type':'minecraft:item','name':'taintedmagic:warpwood_sapling','conditions':[{'condition':'minecraft:random_chance','chance':.02}]}]}]}]})
emit('data/taintedmagic/loot_tables/blocks/nightshade_bush.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[{'type':'minecraft:item','name':'taintedmagic:nightshade_bush','conditions':[{'condition':'minecraft:match_tool','predicate':{'items':['minecraft:shears']}}]},{'type':'minecraft:item','name':'taintedmagic:nightshade_berries','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':3}}]}]}]}]})
for kind,name,values in [('blocks','logs',['warpwood_log']),('blocks','logs_that_burn',['warpwood_log']),('blocks','leaves',['warpwood_leaves']),('blocks','saplings',['warpwood_sapling']),('blocks','mineable/axe',['warpwood_log','warpwood_planks']),('items','logs',['warpwood_log']),('items','planks',['warpwood_planks'])]:emit(f'data/minecraft/tags/{kind}/{name}.json',{'replace':False,'values':['taintedmagic:'+i for i in values]})
for tag,values in [('ingots/shadowmetal',['shadowmetal_ingot']),('nuggets/shadowmetal',['shadowmetal_nugget'])]:emit('data/forge/tags/items/'+tag+'.json',{'replace':False,'values':['taintedmagic:'+x for x in values]})
for slot,itemName in [('head','warped_goggles'),('head','voidmetal_goggles'),('belt','voidwalker_sash'),('ring','lumos_ring')]:
 path=P/'src/generated/resources/data/curios/tags/items'/f'{slot}.json';data=json.loads(path.read_text()) if path.exists() else {'replace':False,'values':[]};value='taintedmagic:'+itemName
 if value not in data['values']:data['values'].append(value)
 emit('data/curios/tags/items/'+slot+'.json',data)
for name,source,result,count,pattern in [('shadowmetal_ingot','shadowmetal_nugget','shadowmetal_ingot',1,['AAA','AAA','AAA']),('shadowmetal_nugget','shadowmetal_ingot','shadowmetal_nugget',9,['A']),('warpwood_planks','warpwood_log','warpwood_planks',4,['A'])]:emit('data/taintedmagic/recipes/'+name+'.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{'A':{'item':'taintedmagic:'+source}},'result':{'item':'taintedmagic:'+result,'count':count}})
emit('data/taintedmagic/recipes/void_blood_application.json',{'type':'taintedmagic:void_blood','category':'misc'})
emit('data/taintedmagic/damage_type/nightshade.json',{'message_id':'nightshade','scaling':'never','exhaustion':.1})
upgrades=[]
for key,aspectlist in [('sanity',['cognitio','sano']),('antibody',['vitium','sano']),('corrosive',['vitium','venenum']),('persistent',['tutamen','motus','potentia']),('diffusion',['tenebrae','alienis','alienis','auram','auram','auram','auram'])]:upgrades.append({'key':'taintedmagic:'+key,'texture':'taintedmagic:textures/foci/icon'+key+'.png','name':'focus.upgrade.'+key+'.name','description':'focus.upgrade.'+key+'.text','aspects':aspectlist})
emit('data/taintedmagic/thaumcraft/focus_upgrades.json',upgrades)
emit('data/taintedmagic/thaumcraft/wand_rods.json',[{'tag':'taintedmagic:'+name,'item':'taintedmagic:wand_rod_'+name,'capacity':capacity,'craftCost':cost,'texture':'taintedmagic:textures/models/modelwand_rod_warpwood.png','staff':staff,'runes':staff,'glowing':staff,'research':'taintedmagic:ROD_'+name} for name,capacity,cost,staff in [('warpwood',250,16,False),('warpwood_staff',500,20,True)]])
emit('data/taintedmagic/thaumcraft/wand_caps.json',[{'tag':'taintedmagic:'+name,'item':'taintedmagic:wand_cap_'+name,'cost':cost,'craftCost':craft,'texture':'taintedmagic:textures/models/modelwand_cap_'+name.replace('crimsoncloth','crimson_cloth').replace('shadowcloth','shadow_cloth')+'.png','research':'taintedmagic:CAP_'+name} for name,cost,craft in [('shadowmetal',.65,12),('cloth',.97,2),('crimsoncloth',.93,3),('shadowcloth',.93,4)]])
print('Research',len(pages),'resource mapping',len(mapping))

emit("assets/taintedmagic/models/item/itemfocusmagemace_orn.json",{"parent":"minecraft:item/generated","textures":{"layer0":"taintedmagic:items/itemfocusmagemace_orn"}})

for damage in ["katana_fire","katana_magic"]:emit("data/taintedmagic/damage_type/"+damage+".json",{"message_id":"player","scaling":"never","exhaustion":.1})
emit("data/minecraft/tags/damage_type/is_fire.json",{"replace":False,"values":["taintedmagic:katana_fire"]})
emit("data/forge/tags/damage_type/is_magic.json",{"replace":False,"values":["taintedmagic:katana_magic"]})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
