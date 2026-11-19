from pathlib import Path
import json, hashlib
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'src/main/resources'
NS='thaumic_tinkerer'
def write(path,value):
 # Complete aspect definitions are maintained directly since TC4 0.4.56.
 if ('/item_aspects/' in path or '/item_bonus_aspects/' in path) and (OUT / 'data/thaumic_tinkerer/item_aspects/explicit.json').exists(): return
 p=OUT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def model(name,parent,textures):write(f'assets/{NS}/models/block/{name}.json',{'parent':'minecraft:block/'+parent,'textures':{k:NS+':block/'+v for k,v in textures.items()}})
def item(item,count=1):return {'item':item,'count':count}
def shape(name,pattern,keys,out,count=1):write(f'data/{NS}/recipes/{name}.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':keys,'result':item(NS+':'+out,count)})
blocknames=['dark_quartz_block','chiseled_dark_quartz','dark_quartz_pillar','dark_quartz_slab','dark_quartz_stairs','restorer']
texturemap={
 'dark_quartz_side':'darkQuartz0','dark_quartz_top':'darkQuartz1','dark_quartz_bottom':'darkQuartz1',
 'chiseled_side':'chiseledDarkQuartz0','chiseled_top':'chiseledDarkQuartz1','pillar_side':'pillarDarkQuartz0','pillar_top':'pillarDarkQuartz1'}
model('dark_quartz_block','cube_bottom_top',{'side':'dark_quartz_side','top':'dark_quartz_top','bottom':'dark_quartz_bottom'})
model('chiseled_dark_quartz','cube_column',{'side':'chiseled_side','end':'chiseled_top'})
model('dark_quartz_pillar','cube_column',{'side':'pillar_side','end':'pillar_top'})
model('dark_quartz_pillar_horizontal','cube_column_horizontal',{'side':'pillar_side','end':'pillar_top'})
for suffix,parent in [('', 'slab'),('_top','slab_top')]:model('dark_quartz_slab'+suffix,parent,{'side':'dark_quartz_side','top':'dark_quartz_top','bottom':'dark_quartz_bottom'})
for suffix,parent in [('', 'stairs'),('_inner','inner_stairs'),('_outer','outer_stairs')]:model('dark_quartz_stairs'+suffix,parent,{'side':'dark_quartz_side','top':'dark_quartz_top','bottom':'dark_quartz_bottom'})
for name in blocknames:
 write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':block/'+name})
 write(f'data/{NS}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 if name in ['dark_quartz_block','chiseled_dark_quartz']:write(f'assets/{NS}/blockstates/{name}.json',{'variants':{'':{'model':NS+':block/'+name}}})
write(f'assets/{NS}/blockstates/dark_quartz_pillar.json',{'variants':{'axis=y':{'model':NS+':block/dark_quartz_pillar'},'axis=x':{'model':NS+':block/dark_quartz_pillar_horizontal','x':90,'y':90},'axis=z':{'model':NS+':block/dark_quartz_pillar_horizontal','x':90}}})
write(f'assets/{NS}/blockstates/dark_quartz_slab.json',{'variants':{'type='+t:{'model':NS+':block/'+m} for t,m in [('bottom','dark_quartz_slab'),('top','dark_quartz_slab_top'),('double','dark_quartz_block')]}})
write(f'data/{NS}/loot_tables/blocks/dark_quartz_slab.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':dark_quartz_slab','functions':[{'function':'minecraft:set_count','count':2,'conditions':[{'condition':'minecraft:block_state_property','block':NS+':dark_quartz_slab','properties':{'type':'double'}}]},{'function':'minecraft:explosion_decay'}]}]}]})
variants={}
for facing,angle in [('east',0),('south',90),('west',180),('north',270)]:
 for half in ['bottom','top']:
  for form in ['straight','inner_left','inner_right','outer_left','outer_right']:
   suffix='' if form=='straight' else '_'+form.split('_')[0]
   y=angle+(-90 if form.endswith('left') else 0)+(90 if half=='top' and form!='straight' else 0)
   variants[f'facing={facing},half={half},shape={form}']={'model':NS+':block/dark_quartz_stairs'+suffix,'x':180 if half=='top' else 0,'y':y%360,'uvlock':True}
write(f'assets/{NS}/blockstates/dark_quartz_stairs.json',{'variants':variants})
write(f'assets/{NS}/models/item/dark_quartz.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/dark_quartz'}})
shape('dark_quartz',['Q Q',' C ','Q Q'],{'Q':{'item':'minecraft:quartz'},'C':{'tag':'minecraft:coals'}},'dark_quartz',4)
shape('dark_quartz_block',['QQ','QQ'],{'Q':{'item':NS+':dark_quartz'}},'dark_quartz_block')
shape('dark_quartz_pillar',['Q','Q'],{'Q':{'item':NS+':dark_quartz_block'}},'dark_quartz_pillar',2)
shape('chiseled_dark_quartz',['Q','Q'],{'Q':{'item':NS+':dark_quartz_slab'}},'chiseled_dark_quartz')
shape('dark_quartz_slab',['QQQ'],{'Q':{'item':NS+':dark_quartz_block'}},'dark_quartz_slab',6)
shape('dark_quartz_stairs',['Q  ','QQ ','QQQ'],{'Q':{'item':NS+':dark_quartz_block'}},'dark_quartz_stairs',4)
write('data/minecraft/tags/blocks/mineable/pickaxe.json',{'replace':False,'values':[NS+':'+n for n in blocknames]})
for tag,values in [('slabs',['dark_quartz_slab']),('stairs',['dark_quartz_stairs'])]:
 for kind in ['blocks','items']:write(f'data/minecraft/tags/{kind}/{tag}.json',{'replace':False,'values':[NS+':'+v for v in values]})
# Research has the complete original quartz recipe pages. No placeholder prerequisite research.
key=NS+':DARK_QUARTZ'
write(f'data/{NS}/research_catalog/main.json',{'format':1,'categories':[{'key':'TT_CATEGORY','icon':NS+':textures/misc/r_enchanting.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[{'key':key,'category':'TT_CATEGORY','displayColumn':-2,'displayRow':2,'pageCount':7,'icon_item':item(NS+':dark_quartz'),'isAutoUnlock':True,'isStub':True,'isRound':True}]}]})
pages=[{'type':'TEXT','text':'tc.research_page.'+key+'.0'}];display=[]
def recipeitem(ident,count=1):return {'kind':'stack','stack':item(ident,count)}
for name in ['dark_quartz','dark_quartz_block','dark_quartz_slab','dark_quartz_pillar','chiseled_dark_quartz','dark_quartz_stairs']:
 recipe=json.loads((OUT/f'data/{NS}/recipes/{name}.json').read_text(encoding='utf-8'));ref=NS+':'+name
 ingredients=[]
 for row in recipe['pattern']:
  for symbol in row:
   if symbol==' ':ingredients.append({'kind':'sequence','elements':[]});continue
   ingredient=recipe['key'][symbol]
   ingredients.append(recipeitem(ingredient.get('item','minecraft:coal')))
 display.append({'key':ref,'class':'modern','data':{'kind':'crafting_shaped','output':recipeitem(recipe['result']['item'],recipe['result']['count']),'width':len(recipe['pattern'][0]),'height':len(recipe['pattern']),'ingredients':{'kind':'sequence','elements':ingredients}}})
 pages.append({'type':'NORMAL_CRAFTING','recipe':{'kind':'reference','keys':[ref],'class':'modern'}})
write(f'data/{NS}/research_pages/main.json',{'format':1,'entries':[{'key':key,'pages':pages}]})
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display})
# Only maps and translation keys are distributed; original bytes remain in the supplied JAR.
mapping={f'assets/{NS}/textures/block/{target}.png':f'assets/ttinkerer/textures/blocks/{source}.png' for target,source in texturemap.items()}
mapping[f'assets/{NS}/textures/item/dark_quartz.png']='assets/ttinkerer/textures/items/darkQuartzItem.png'
mapping[f'assets/{NS}/textures/block/restorer.png']='assets/ttinkerer/textures/model/repairer.png'
keys={'item.'+NS+'.dark_quartz':'item.darkQuartzItem.name','tc.research_category.TT_CATEGORY':'tc.research_category.TT_CATEGORY'}
for target,old in zip(blocknames,['darkQuartz','darkQuartzChiseled','darkQuartzPillar','darkQuartzSlab','darkQuartzStairs','repairer']):keys[f'block.{NS}.{target}']='tile.'+old+'.name'
for field,old in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{key}']='ttresearch.'+old+'.DARK_QUARTZ'
keys['tc.research_page.'+key+'.0']='ttresearch.page.DARK_QUARTZ.0'
languages={}
for new,old in [('en_us','en_US'),('zh_cn','zh_CN')]:
 target=f'assets/{NS}/lang/{new}.json';mapping[target]=f'assets/ttinkerer/lang/{old}.lang';languages[target]=keys
reference=ROOT.parent/'references/ThaumicTinkerer-2.5-1.7.10-164.jar'
write(f'data/{NS}/thaumcraft/original_resources.json',{'name':'Thaumic Tinkerer 2.5-164','legacy_modid':'ThaumicTinkerer','sha256':hashlib.sha256(reference.read_bytes()).hexdigest(),'download':'https://www.curseforge.com/minecraft/mc-mods/thaumic-tinkerer/files/2232793','mapping':mapping,'languages':languages})
write('pack.mcmeta',{'pack':{'description':'Thaumic Tinkerer Modern Port definitions','pack_format':15}})
meta=OUT/'META-INF/mods.toml';meta.parent.mkdir(parents=True,exist_ok=True);meta.write_text('''modLoader="javafml"
loaderVersion="[47,)"
license="CC-BY-NC-SA-3.0"
[[mods]]
modId="thaumic_tinkerer"
version="${version}"
displayName="Thaumic Tinkerer"
[[dependencies.thaumic_tinkerer]]
modId="thaumcraft"
mandatory=true
versionRange="[0.4.114,)"
ordering="AFTER"
side="BOTH"
[[dependencies.thaumic_tinkerer]]
modId="enderstorage"
mandatory=false
versionRange="[2.11.0.188,2.12)"
ordering="AFTER"
side="BOTH"
[[dependencies.thaumic_tinkerer]]
modId="computercraft"
mandatory=false
versionRange="[1.120.2,)"
ordering="AFTER"
side="BOTH"
[[dependencies.thaumic_tinkerer]]
modId="jei"
mandatory=false
versionRange="[15.20.0.106,16)"
ordering="AFTER"
side="CLIENT"
''',encoding='utf-8')
# Generate the restorer's rectangular frame and glass from explicit geometry, with the old texture atlas.
# These coordinates are model definitions; the texture itself is still provided at runtime.
elements=[]
def box(x,y,z,w,h,d,u,v):
 uv={'west':[u,v+d,u+d,v+d+h],'east':[u+d+w,v+d,u+d+w+d,v+d+h],
     'up':[u+d,v,u+d+w,v+d],'down':[u+d+w,v,u+d+w+w,v+d],
     'north':[u+d,v+d,u+d+w,v+d+h],'south':[u+d+w+d,v+d,u+d+w+d+w,v+d+h]}
 elements.append({'from':[x,y,z],'to':[x+w,y+h,z+d],'faces':{side:{'texture':'#all','uv':[n/4 for n in value]} for side,value in uv.items()}})
box(0,0,0,16,1,16,0,0)
for x,z in [(0,0),(14,0),(0,14),(14,14)]:box(x,1,z,2,14,2,0,17)
for z in [0,14]:box(0,15,z,16,1,2,11,19)
for x in [0,14]:box(x,15,2,2,1,12,11,23)
for y in [6,9]:box(5.5,y,0,5,1,1,48,30)
for x in [6,9]:box(x,6.5,0,1,3,1,48,24)
# Double-sided glass panels use the transparent region of the supplied atlas.
for a,b in [([2,15.5,2],[14,15.5,14]),([.5,1,2],[.5,15,14]),([15.5,1,2],[15.5,15,14]),([2,1,.5],[14,15,.5]),([2,1,15.5],[14,15,15.5])]:
 faces=['up','down'] if a[1]==b[1] else ['east','west'] if a[0]==b[0] else ['north','south']
 elements.append({'from':a,'to':b,'faces':{side:{'texture':'#all','uv':[.25,9.5,3.25,13]} for side in faces}})
write(f'assets/{NS}/models/block/restorer.json',{'parent':'minecraft:block/block','render_type':'minecraft:translucent','textures':{'all':NS+':block/restorer','particle':NS+':block/restorer'},'elements':elements})
write(f'assets/{NS}/blockstates/restorer.json',{'variants':{'facing='+face:{'model':NS+':block/restorer','y':y} for face,y in [('north',0),('east',90),('south',180),('west',270)]}})
# Gas/light/funnel/restorer progression. Keep the original research graph and costs.
extra=[
 ('GASEOUS_LIGHT','gaseous_light',0,-3,1,{'lux':2,'aer':1},['NITOR'],[],False,False),
 ('GASEOUS_SHADOW','gaseous_shadow',-1,-5,2,{'tenebrae':2,'aer':1,'motus':4},[NS+':GASEOUS_LIGHT'],[],True,False),
 ('BRIGHT_NITOR','bright_nitor',1,-5,2,{'lux':2,'ignis':1,'potentia':1,'aer':1},[NS+':GASEOUS_LIGHT'],[],True,True),
 ('GAS_REMOVER','gas_remover',-2,-7,0,{'tenebrae':2,'lux':2},[NS+':GASEOUS_SHADOW'],[],False,False),
 ('FUNNEL','essentia_funnel',0,-7,1,{'instrumentum':1,'iter':2},[NS+':BRIGHT_NITOR'],['DISTILESSENTIA'],True,True),
 ('REPAIRER','restorer',-1,-9,3,{'instrumentum':2,'fabrico':1,'ordo':1,'praecantatio':1},[NS+':FUNNEL'],['THAUMIUM','ENCHFABRIC'],False,True)]
recipes={}
for research,name,catalyst,costs in [
 ('GASEOUS_LIGHT','gaseous_light','thaumcraft:empty_phial',{'lux':16,'aer':10,'motus':8}),
 ('GASEOUS_SHADOW','gaseous_shadow','thaumcraft:empty_phial',{'tenebrae':16,'aer':10,'motus':8}),
 ('BRIGHT_NITOR','bright_nitor','thaumcraft:nitor',{'potentia':25,'lux':25,'aer':10,'ignis':10})]:
 recipe={'type':'thaumcraft:crucible','research':NS+':'+research,'legacy_order':10000+len(recipes),'aspects':costs,'catalyst':{'item':catalyst},'result':item(NS+':'+name)}
 recipes[research]=recipe
 recipespec={'kind':'crucible','research':NS+':'+research,'catalyst':recipeitem(catalyst),'output':recipeitem(NS+':'+name),'aspects':costs}
 display.append({'key':NS+':'+name,'class':'modern','data':recipespec})
 write(f'data/{NS}/recipes/{name}.json',recipe)
for research,name,pattern,ingredients,costs in [
 ('GAS_REMOVER','gas_remover',['DDD','T G','QQQ'],{'D':NS+':dark_quartz','T':NS+':gaseous_shadow','G':NS+':gaseous_light','Q':'minecraft:quartz'},{'aer':2,'ordo':2}),
 ('FUNNEL','essentia_funnel',['STS'],{'S':'minecraft:stone','T':'thaumcraft:thaumium_ingot'},{'ordo':1,'perditio':1})]:
 recipe={'type':'thaumcraft:arcane','research':NS+':'+research,'pattern':pattern,'key':{k:{'item':v} for k,v in ingredients.items()},'vis':costs,'result':item(NS+':'+name)}
 recipes[research]=recipe;write(f'data/{NS}/recipes/{name}.json',recipe)
 display.append({'key':NS+':'+name,'class':'modern','data':{'kind':'arcane_shaped','research':NS+':'+research,'width':len(pattern[0]),'height':len(pattern),'aspects':costs,'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(ingredients[c]) if c!=' ' else {'kind':'sequence','elements':[]} for row in pattern for c in row]}}})
parts=['minecraft:iron_ingot','minecraft:gold_ingot','minecraft:diamond','minecraft:cobblestone','minecraft:oak_planks','minecraft:leather','thaumcraft:enchanted_fabric','thaumcraft:thaumium_ingot']
costs={'instrumentum':15,'fabrico':20,'ordo':10,'praecantatio':15}
recipes['REPAIRER']={'type':'thaumcraft:infusion','research':NS+':REPAIRER','legacy_order':10010,'instability':8,'aspects':costs,'center':{'ingredient':{'item':'thaumcraft:thaumium_block'}},'components':[{'ingredient':{'item':p}} for p in parts],'result':item(NS+':restorer')}
write(f'data/{NS}/recipes/restorer.json',recipes['REPAIRER'])
display.append({'key':NS+':restorer','class':'modern','data':{'kind':'infusion','research':NS+':REPAIRER','instability':8,'aspects':costs,'input':recipeitem('thaumcraft:thaumium_block'),'output':recipeitem(NS+':restorer'),'ingredients':{'kind':'sequence','elements':[recipeitem(p) for p in parts]}}})
catalog=json.loads((OUT/f'data/{NS}/research_catalog/main.json').read_text(encoding='utf-8'))
pagefile=json.loads((OUT/f'data/{NS}/research_pages/main.json').read_text(encoding='utf-8'))
for research,name,col,row,complexity,aspects,parents,hidden,secondary,concealed in extra:
 key=NS+':'+research
 catalog['categories'][0]['research'].append({'key':key,'category':'TT_CATEGORY','displayColumn':col,'displayRow':row,'complexity':complexity,'aspects':aspects,'parents':parents,'parentsHidden':hidden,'isSecondary':secondary,'isConcealed':concealed,'isRound':research=='GAS_REMOVER','pageCount':2,'icon_item':item(NS+':'+name)})
 kind=recipes[research]['type'].split(':')[1].upper()+'_CRAFTING'
 pagefile['entries'].append({'key':key,'pages':[{'type':'TEXT','text':'tc.research_page.'+key+'.0'},{'type':kind,'recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]})
 for field,old in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{key}']='ttresearch.'+old+'.'+research
 keys['tc.research_page.'+key+'.0']='ttresearch.page.'+research+'.0'
write(f'data/{NS}/research_catalog/main.json',catalog)
write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display})
for name,old in [('gaseous_light','gaseousLightItem'),('gaseous_shadow','gaseousShadowItem'),('gas_remover','gasRemover'),('bright_nitor','brightNitor')]:
 write(f'assets/{NS}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name}})
 mapping[f'assets/{NS}/textures/item/{name}.png']='assets/ttinkerer/textures/items/'+old+'.png'
 keys[f'item.{NS}.{name}']='item.'+old+'.name'
keys[f'block.{NS}.essentia_funnel']='tile.funnel.name'
for face in ['0','1']:mapping[f'assets/{NS}/textures/block/funnel{face}.png']=f'assets/ttinkerer/textures/blocks/funnel{face}.png'
write(f'assets/{NS}/models/block/essentia_funnel.json',{'parent':'minecraft:block/block','textures':{'top':NS+':block/funnel0','side':NS+':block/funnel1','particle':NS+':block/funnel1'},'elements':[{'from':[0,0,0],'to':[16,2,16],'faces':{face:{'texture':'#top' if face in ['up','down'] else '#side'} for face in ['up','down','north','south','east','west']}}]})
write(f'assets/{NS}/models/item/essentia_funnel.json',{'parent':NS+':block/essentia_funnel'})
write(f'assets/{NS}/blockstates/essentia_funnel.json',{'variants':{'':{'model':NS+':block/essentia_funnel'}}})
write(f'data/{NS}/loot_tables/blocks/essentia_funnel.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':essentia_funnel'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
manifest=json.loads((OUT/f'data/{NS}/thaumcraft/original_resources.json').read_text(encoding='utf-8'));manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Server-derived legacy recipe aspects are reproducible data, not bundled original resources.
derived=json.loads((Path(__file__).parent/'derived-aspects.json').read_text(encoding='utf-8'))
write(f'data/{NS}/item_aspects/acquisition.json',derived)

# Transvector interface and its two-stage connector.
interface_recipes=[
 ('transvector_interface',['BRB','LEL','BRB'],{'B':'thaumcraft:arcane_stone','R':'minecraft:redstone','L':'minecraft:lapis_lazuli','E':'minecraft:ender_pearl'},{'ordo':12,'perditio':16}),
 ('connector',[' I ',' WI','S  '],{'I':'minecraft:iron_ingot','W':'minecraft:stick','S':'thaumcraft:order_shard'},{'ordo':2})]
for name,pattern,ingredients,costs in interface_recipes:
 write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:arcane','research':NS+':INTERFACE','pattern':pattern,'key':{k:{'item':v} for k,v in ingredients.items()},'vis':costs,'result':item(NS+':'+name)})
 display.append({'key':NS+':'+name,'class':'modern','data':{'kind':'arcane_shaped','research':NS+':INTERFACE','width':3,'height':3,'aspects':costs,'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(ingredients[c]) if c!=' ' else {'kind':'sequence','elements':[]} for row in pattern for c in row]}}})
catalog['categories'][0]['research'].append({'key':NS+':INTERFACE','category':'TT_CATEGORY','displayColumn':-4,'displayRow':2,'complexity':1,'aspects':{'perditio':4,'ordo':4},'parents':[NS+':DARK_QUARTZ'],'pageCount':5,'icon_item':item(NS+':transvector_interface')})
pagefile['entries'].append({'key':NS+':INTERFACE','pages':[{'type':'TEXT','text':'tc.research_page.'+NS+':INTERFACE.0'},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','keys':[NS+':transvector_interface'],'class':'modern'}},{'type':'TEXT','text':'tc.research_page.'+NS+':INTERFACE.1'},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','keys':[NS+':connector'],'class':'modern'}},{'type':'TEXT','text':'tc.research_page.'+NS+':INTERFACE.2'}]})
write(f'data/{NS}/research_catalog/main.json',catalog)
write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display})
model('transvector_interface','cube_all',{'all':'transvector_interface'})
write(f'assets/{NS}/models/block/interface_camouflaged.json',{'textures':{'particle':NS+':block/transvector_interface'},'elements':[]})
write(f'assets/{NS}/blockstates/transvector_interface.json',{'variants':{'camouflaged=false':{'model':NS+':block/transvector_interface'},'camouflaged=true':{'model':NS+':block/interface_camouflaged'}}})
write(f'assets/{NS}/models/item/transvector_interface.json',{'parent':NS+':block/transvector_interface'})
write(f'assets/{NS}/models/item/connector.json',{'parent':'minecraft:item/handheld','textures':{'layer0':NS+':item/connector'}})
write(f'data/{NS}/loot_tables/blocks/transvector_interface.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':transvector_interface'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(f'data/minecraft/tags/blocks/mineable/pickaxe.json',{'replace':False,'values':[NS+':transvector_interface',NS+':restorer',NS+':essentia_funnel']+ [NS+':'+name for name in blocknames if 'quartz' in name]})
mapping[f'assets/{NS}/textures/block/transvector_interface.png']='assets/ttinkerer/textures/blocks/interface.png'
mapping[f'assets/{NS}/textures/item/connector.png']='assets/ttinkerer/textures/items/connector.png'
keys['block.'+NS+'.transvector_interface']='tile.interface.name'
keys['item.'+NS+'.connector']='item.connector.name'
for field,old in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:INTERFACE']='ttresearch.'+old+'.INTERFACE'
for page in range(3):keys[f'tc.research_page.{NS}:INTERFACE.{page}']=f'ttresearch.page.INTERFACE.{page}'
for message in ['set','notinterf','notpresent','interffail','toofar','complete']:keys['ttmisc.connector.'+message]='ttmisc.connector.'+message
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Mirror-dependent displacement uses the runtime research enablement API.
condition=[]
name='transvector_dislocator';costs={'terra':5,'perditio':5};ingredients={'M':'thaumcraft:mirrored_glass','I':NS+':transvector_interface','C':'minecraft:comparator'};pattern=[' M ',' I ',' C ']
write(f'data/{NS}/recipes/{name}.json',{'conditions':condition,'type':'thaumcraft:arcane','research':NS+':DISLOCATOR','pattern':pattern,'key':{k:{'item':v} for k,v in ingredients.items()},'vis':costs,'result':item(NS+':'+name)})
category=dict(catalog['categories'][0]);category['research']=[{'key':NS+':DISLOCATOR','category':'TT_CATEGORY','displayColumn':-6,'displayRow':1,'complexity':3,'aspects':{'iter':2,'machina':1,'alienis':1},'parents':[NS+':INTERFACE'],'parentsHidden':['MIRROR'],'isSecondary':True,'isConcealed':True,'pageCount':2,'icon_item':item(NS+':'+name)}]
write(f'data/{NS}/research_catalog/dislocator.json',{'format':1,'conditions':condition,'categories':[category]})
write(f'data/{NS}/research_pages/dislocator.json',{'format':1,'conditions':condition,'entries':[{'key':NS+':DISLOCATOR','pages':[{'type':'TEXT','text':'tc.research_page.'+NS+':DISLOCATOR.0'},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]}]})
write(f'data/{NS}/research_page_recipes/dislocator.json',{'format':1,'conditions':condition,'entries':[{'key':NS+':'+name,'class':'modern','data':{'kind':'arcane_shaped','research':NS+':DISLOCATOR','width':3,'height':3,'aspects':costs,'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(ingredients[c]) if c!=' ' else {'kind':'sequence','elements':[]} for row in pattern for c in row]}}}]})
write(f'assets/{NS}/models/block/{name}.json',{'parent':'minecraft:block/orientable','textures':{'front':NS+':block/dislocator1','side':NS+':block/dislocator0','top':NS+':block/dislocator0'}})
variants={}
for face,x,y in [('north',0,0),('south',0,180),('west',0,270),('east',0,90),('up',270,0),('down',90,0)]:
 variants['camouflaged=false,facing='+face]={'model':NS+':block/'+name,'x':x,'y':y}
variants['camouflaged=true']={'model':NS+':block/interface_camouflaged'}
write(f'assets/{NS}/blockstates/{name}.json',{'variants':variants})
write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':block/'+name})
write(f'data/{NS}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for i in range(2):mapping[f'assets/{NS}/textures/block/dislocator{i}.png']=f'assets/ttinkerer/textures/blocks/dislocator{i}.png'
keys['block.'+NS+'.'+name]='tile.dislocator.name'
for field,old in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:DISLOCATOR']='ttresearch.'+old+'.DISLOCATOR'
keys[f'tc.research_page.{NS}:DISLOCATOR.0']='ttresearch.page.DISLOCATOR.0'
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)
pickaxe=json.loads((OUT/'data/minecraft/tags/blocks/mineable/pickaxe.json').read_text(encoding='utf-8'));pickaxe['values'].append(NS+':'+name)
write('data/minecraft/tags/blocks/mineable/pickaxe.json',pickaxe)

# Early focus chain: excavation -> smelting -> flight -> telekinesis.
focus_entries=[
 ('FOCUS_SMELT','focus_smelting','focusSmelt',-2,-2,{'ignis':2,'potentia':1,'praecantatio':1},['FOCUSEXCAVATION'],False),
 ('FOCUS_FLIGHT','focus_flight','focusFlight',-3,-4,{'motus':1,'praecantatio':1,'aer':2},[NS+':FOCUS_SMELT'],False),
 ('FOCUS_TELEKINESIS','focus_telekinesis','focusTelekinesis',-4,-6,{'alienis':2,'praecantatio':1,'motus':1},[NS+':FOCUS_FLIGHT'],True)]
focus_display=[]
for key,name,old,col,row,aspects,parents,secondary in focus_entries:
 catalog['categories'][0]['research'].append({'key':NS+':'+key,'category':'TT_CATEGORY','displayColumn':col,'displayRow':row,'complexity':2,'aspects':aspects,'parents':parents,'isConcealed':True,'isSecondary':secondary,'pageCount':2,'icon_item':item(NS+':'+name)})
 pagefile['entries'].append({'key':NS+':'+key,'pages':[{'type':'TEXT','text':'tc.research_page.'+NS+':'+key+'.0'},{'type':('ARCANE' if key=='FOCUS_SMELT' else 'INFUSION')+'_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]})
 for suffix in ['', '_orn']:
  write(f'assets/{NS}/models/item/{name}{suffix}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name+suffix}})
  mapping[f'assets/{NS}/textures/item/{name}{suffix}.png']='assets/ttinkerer/textures/items/'+old+('Orn' if suffix else '')+'.png'
 keys['item.'+NS+'.'+name]='item.'+old+'.name'
 for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:'+key]='ttresearch.'+legacy+'.'+key
 keys[f'tc.research_page.{NS}:'+key+'.0']='ttresearch.page.'+key+'.0'
 if key=='FOCUS_SMELT':
  ingredients={'F':'thaumcraft:focus_fire','N':'thaumcraft:nitor','E':'thaumcraft:focus_excavation'};costs={'ignis':10,'ordo':5,'perditio':6}
  write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:arcane','research':NS+':'+key,'pattern':['FNE'],'key':{k:{'item':v} for k,v in ingredients.items()},'vis':costs,'result':item(NS+':'+name)})
  data={'kind':'arcane_shaped','research':NS+':'+key,'width':3,'height':1,'aspects':costs,'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(ingredients[c]) for c in 'FNE']}}
 else:
  flight=key=='FOCUS_FLIGHT';costs={'aer':15,'motus':20,'iter':10} if flight else {'motus':10,'aer':20,'perditio':20,'cognitio':10}
  parts=['minecraft:quartz']*4+(['minecraft:feather']*2 if flight else ['minecraft:iron_ingot','minecraft:gold_ingot'])+['thaumcraft:air_shard']
  instability=3 if flight else 5
  write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:infusion','research':NS+':'+key,'instability':instability,'aspects':costs,'center':{'ingredient':{'item':'minecraft:ender_pearl'}},'components':[{'ingredient':{'item':p}} for p in parts],'result':item(NS+':'+name)})
  data={'kind':'infusion','research':NS+':'+key,'instability':instability,'aspects':costs,'input':recipeitem('minecraft:ender_pearl'),'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(p) for p in parts]}}
 focus_display.append({'key':NS+':'+name,'class':'modern','data':data})
write(f'data/{NS}/research_catalog/main.json',catalog)
write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Kinetic/corporeal attractors and the soul mould share a five-page research.
magnet_display=[]
for name,metal in [('item_magnet','minecraft:iron_ingot'),('mob_magnet','minecraft:copper_ingot')]:
 pattern=[' I ','SIs','WFW'];ingredients={'I':metal,'S':'thaumcraft:air_shard','s':'thaumcraft:earth_shard','W':'thaumcraft:greatwood_log','F':NS+':focus_telekinesis'};costs={'aer':20,'ordo':5,'terra':15,'perditio':5}
 write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:arcane','research':NS+':MAGNETS','pattern':pattern,'key':{k:{'item':v} for k,v in ingredients.items()},'vis':costs,'result':item(NS+':'+name)})
 magnet_display.append({'key':NS+':'+name,'class':'modern','data':{'kind':'arcane_shaped','research':NS+':MAGNETS','width':3,'height':3,'aspects':costs,'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(ingredients[c]) if c!=' ' else {'kind':'sequence','elements':[]} for row in pattern for c in row]}}})
 # Rebuild the old 64x64 model atlas geometry in block coordinates.
 elements=[];box(1,0,1,14,2,14,0,0);box(6,2,6,4,13,4,0,16)
 for a,b,faces,uv in [([5,2,5],[5,16,11],['east','west'],[28,19,34,33]),([11,2,5],[11,16,11],['east','west'],[28,33,34,47]),([5,2,5],[11,16,5],['north','south'],[40,19,46,33]),([5,2,11],[11,16,11],['north','south'],[40,33,46,47]),([5,16,5],[11,16,11],['up','down'],[34,49,40,55])]:
  elements.append({'from':a,'to':b,'faces':{face:{'texture':'#all','uv':[n/4 for n in uv]} for face in faces}})
 for mode in ['pull','push']:
  write(f'assets/{NS}/models/block/{name}_{mode}.json',{'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':{'all':NS+':block/'+name+'_'+mode,'particle':'minecraft:block/spruce_planks'},'elements':elements})
  old=('mobMagnet' if name=='mob_magnet' else 'magnet')+('S' if mode=='pull' else 'N')
  mapping[f'assets/{NS}/textures/block/{name}_{mode}.png']='assets/ttinkerer/textures/model/'+old+'.png'
 write(f'assets/{NS}/blockstates/{name}.json',{'variants':{'pulling=true':{'model':NS+':block/'+name+'_pull'},'pulling=false':{'model':NS+':block/'+name+'_push'}}})
 write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':block/'+name+'_pull'})
 write(f'data/{NS}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 keys['block.'+NS+'.'+name]='tile.'+('mobMagnet' if name=='mob_magnet' else 'magnet')+'.name'
costs={'bestia':4,'cognitio':8,'sensus':8}
write(f'data/{NS}/recipes/soul_mould.json',{'type':'thaumcraft:crucible','research':NS+':MAGNETS','legacy_order':10030,'catalyst':{'item':'minecraft:ender_pearl'},'aspects':costs,'result':item(NS+':soul_mould')})
magnet_display.append({'key':NS+':soul_mould','class':'modern','data':{'kind':'crucible','research':NS+':MAGNETS','aspects':costs,'catalyst':recipeitem('minecraft:ender_pearl'),'output':recipeitem(NS+':soul_mould')}})
catalog['categories'][0]['research'].append({'key':NS+':MAGNETS','category':'TT_CATEGORY','displayColumn':-6,'displayRow':3,'complexity':3,'aspects':{'machina':2,'motus':1,'sensus':1},'parents':[NS+':INTERFACE'],'parentsHidden':[NS+':FOCUS_TELEKINESIS'],'isConcealed':True,'pageCount':5,'icon_item':item(NS+':item_magnet')})
pagefile['entries'].append({'key':NS+':MAGNETS','pages':[{'type':'TEXT','text':f'tc.research_page.{NS}:MAGNETS.{i}'} for i in range(2)]+[{'type':('CRUCIBLE' if name=='soul_mould' else 'ARCANE')+'_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}} for name in ['item_magnet','mob_magnet','soul_mould']]})
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display})
write(f'assets/{NS}/models/item/soul_mould.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/soul_mould'}})
mapping[f'assets/{NS}/textures/item/soul_mould.png']='assets/ttinkerer/textures/items/soulMould.png'
mapping[f'assets/{NS}/textures/gui/mob_magnet.png']='assets/ttinkerer/textures/gui/mobMagnet.png'
keys['item.'+NS+'.soul_mould']='item.soulMould.name'
for name in ['adult','child','none','all','soulMould.nonAssigned','soulMould.pattern']:keys['ttmisc.'+name]='ttmisc.'+name
for field,old in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:MAGNETS']='ttresearch.'+old+'.MAGNETS'
for i in range(2):keys[f'tc.research_page.{NS}:MAGNETS.{i}']=f'ttresearch.page.MAGNETS.{i}'
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Tool Dynamism Tablet: native fake-player automation with the original research/costs.
name='dynamism_tablet';research='ANIMATION_TABLET';costs={'aer':25,'ordo':15,'ignis':10};pattern=['GIG','ICI'];ingredients={'G':'minecraft:gold_ingot','I':'minecraft:iron_ingot','C':'thaumcraft:golem_core_blank'}
write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:arcane','research':NS+':'+research,'pattern':pattern,'key':{k:{'item':v} for k,v in ingredients.items()},'vis':costs,'result':item(NS+':'+name)})
tablet_display={'key':NS+':'+name,'class':'modern','data':{'kind':'arcane_shaped','research':NS+':'+research,'width':3,'height':2,'aspects':costs,'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(ingredients[c]) for row in pattern for c in row]}}}
catalog['categories'][0]['research'].append({'key':NS+':'+research,'category':'TT_CATEGORY','displayColumn':-8,'displayRow':2,'complexity':3,'warp':1,'aspects':{'machina':2,'metallum':1,'motus':1,'potentia':1},'parents':[NS+':MAGNETS'],'pageCount':2,'icon_item':item(NS+':'+name)})
pagefile['entries'].append({'key':NS+':'+research,'pages':[{'type':'TEXT','text':f'tc.research_page.{NS}:{research}.0'},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]})
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]})
textures={'particle':NS+':block/tablet2',**{str(i):NS+':block/tablet'+str(i) for i in range(3)}}
write(f'assets/{NS}/models/block/{name}.json',{'parent':'minecraft:block/block','textures':textures,'elements':[{'from':[0,0,0],'to':[16,2,16],'faces':{face:{'texture':'#'+('1' if face=='up' else '0' if face=='down' else '2')} for face in ['up','down','north','south','east','west']}}]})
write(f'assets/{NS}/blockstates/{name}.json',{'variants':{'facing='+face:{'model':NS+':block/'+name,'y':angle} for face,angle in [('north',0),('east',90),('south',180),('west',270)]}})
write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':block/'+name})
write(f'data/{NS}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for i in range(3):mapping[f'assets/{NS}/textures/block/tablet{i}.png']=f'assets/ttinkerer/textures/blocks/animationTablet{i}.png'
for suffix in ['atCenter','atLeft','atRight','atFacingIndent']:mapping[f'assets/{NS}/textures/misc/{suffix.lower()}.png']=f'assets/ttinkerer/textures/misc/{suffix}.png'
mapping[f'assets/{NS}/textures/gui/{name}.png']='assets/ttinkerer/textures/gui/animationTablet.png'
keys['block.'+NS+'.'+name]='tile.animationTablet.name'
for key in ['leftClick','rightClick','redstoneControl','animationTablet.notRotatable']:keys['ttmisc.'+key]='ttmisc.'+key
for field,old in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:{research}']='ttresearch.'+old+'.'+research
keys[f'tc.research_page.{NS}:{research}.0']=f'ttresearch.page.{research}.0'
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)
pickaxe['values'].append(NS+':'+name);write('data/minecraft/tags/blocks/mineable/pickaxe.json',pickaxe)

# Remaining non-KAMI focus progression. Research follows the runtime mirror toggle.
late_foci=[
 ('FOCUS_DEFLECT','focus_deflection','focusDeflect',-4,-3,3,0,{'motus':2,'aer':1,'ordo':1,'mortuus':1},'FOCUS_SMELT',True,1),
 ('FOCUS_ENDER_CHEST','focus_ender_chest','focusEnderChest',-6,-2,2,1,{'alienis':2,'vacuos':1,'praecantatio':1},'FOCUS_DEFLECT',False,1),
 ('FOCUS_DISLOCATION','focus_dislocation','focusDislocation',-5,-5,2,0,{'alienis':2,'praecantatio':1,'permutatio':1},'FOCUS_FLIGHT',True,2),
 ('FOCUS_HEAL','focus_healing','focusHeal',-6,-4,2,0,{'sano':2,'spiritus':1,'praecantatio':1},'FOCUS_DEFLECT',True,1)]
late_display=[]
for key,name,old,col,row,complexity,warp,aspects,parent,secondary,textcount in late_foci:
 catalog['categories'][0]['research'].append({'key':NS+':'+key,'category':'TT_CATEGORY','displayColumn':col,'displayRow':row,'complexity':complexity,'warp':warp,'aspects':aspects,'parents':[NS+':'+parent],'isConcealed':True,'isSecondary':secondary,'pageCount':textcount+1,'icon_item':item(NS+':'+name)})
 pagefile['entries'].append({'key':NS+':'+key,'pages':[{'type':'TEXT','text':f'tc.research_page.{NS}:{key}.{i}'} for i in range(textcount)]+[{'type':('ARCANE' if key=='FOCUS_ENDER_CHEST' else 'INFUSION')+'_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]})
 for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:{key}']='ttresearch.'+legacy+'.'+key
 for i in range(textcount):keys[f'tc.research_page.{NS}:{key}.{i}']=f'ttresearch.page.{key}.{i}'
 keys['item.'+NS+'.'+name]='item.'+old+'.name'
 suffixes=['']+(['_orn'] if key=='FOCUS_DISLOCATION' else ['_depth'] if key in ['FOCUS_HEAL','FOCUS_ENDER_CHEST'] else [])
 for suffix in suffixes:
  mapping[f'assets/{NS}/textures/item/{name}{suffix}.png']=f'assets/ttinkerer/textures/items/{old}'+({'_orn':'Orn','_depth':'Depth'}.get(suffix,''))+'.png'
  write(f'assets/{NS}/models/item/{name}{suffix}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name+suffix}})
 if key=='FOCUS_ENDER_CHEST':
  costs={'ordo':10,'perditio':10};ingredients={'M':'thaumcraft:item_mirror','E':'minecraft:ender_eye','P':'thaumcraft:focus_portable_hole'}
  write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:arcane','research':NS+':'+key,'pattern':['M','E','P'],'key':{k:{'item':v} for k,v in ingredients.items()},'vis':costs,'result':item(NS+':'+name)})
  data={'kind':'arcane_shaped','research':NS+':'+key,'width':1,'height':3,'aspects':costs,'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(ingredients[c]) for c in 'MEP']}}
 else:
  if key=='FOCUS_DEFLECT':
   costs={'aer':15,'tutamen':5,'ordo':20};center=NS+':focus_flight';parts=['thaumcraft:mirrored_glass']*2+['thaumcraft:obsidian_totem','thaumcraft:order_shard'];instability=5
  elif key=='FOCUS_DISLOCATION':
   costs={'alienis':20,'tenebrae':10,'vacuos':25,'praecantatio':20,'vitium':5};center='minecraft:ender_pearl';parts=['minecraft:quartz']*4+['thaumcraft:amber']*3+['minecraft:diamond'];instability=8
  else:
   costs={'sano':10,'spiritus':10,'victus':15};center='thaumcraft:focus_pech';parts=['minecraft:golden_carrot']+['minecraft:gold_nugget']*3;instability=4
  write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:infusion','research':NS+':'+key,'instability':instability,'aspects':costs,'center':{'ingredient':{'item':center}},'components':[{'ingredient':{'item':p}} for p in parts],'result':item(NS+':'+name)})
  data={'kind':'infusion','research':NS+':'+key,'instability':instability,'aspects':costs,'input':recipeitem(center),'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(p) for p in parts]}}
 late_display.append({'key':NS+':'+name,'class':'modern','data':data})
write(f'assets/{NS}/textures/item/focus_healing_depth.png.mcmeta',{'animation':{'frametime':1}})
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Vanilla classes that did not implement 1.7.10 IProjectile remain unaffected.
write(f'data/{NS}/tags/entity_types/deflection_immune.json',{'replace':False,'values':['minecraft:experience_bottle','minecraft:fireball','minecraft:small_fireball','minecraft:dragon_fireball','minecraft:wither_skull','minecraft:firework_rocket','minecraft:fishing_bobber']})

# Osmotic enchantment definitions, translated from the registered legacy table.
import re
legacyroot=Path(__file__).resolve().parents[2]/'references/ThaumicTinkerer-GTNH/src/main/java/thaumic/tinkerer'
source=(legacyroot/'common/enchantment/core/EnchantmentManager.java').read_text(encoding='utf-8')
vanilla={'fireProtection':'fire_protection','featherFalling':'feather_falling','blastProtection':'blast_protection','projectileProtection':'projectile_protection','aquaAffinity':'aqua_affinity','baneOfArthropods':'bane_of_arthropods','fireAspect':'fire_aspect','silkTouch':'silk_touch','field_151369_A':'lure','field_151370_z':'luck_of_the_sea'}
modern={'ascentBoost':'ascent_boost','slowFall':'slow_fall','autoSmelt':'flaming_touch','desintegrate':'disintegrate','quickDraw':'quick_draw','vampirism':'vampirism','focusedStrike':'focused_strike','dispersedStrikes':'dispersed_strike','finalStrike':'final_strike','valiance':'valiance','shatter':'shatter','shockwave':'shockwave','tunnel':'tunnel','pounce':'pounce'}
research_names={'ascentBoost':'ASCENT_BOOST','slowFall':'SLOW_FALL','autoSmelt':'AUTO_SMELT','desintegrate':'DESINTEGRATE','quickDraw':'QUICK_DRAW','vampirism':'VAMPIRISM','focusedStrike':'FOCUSED','dispersedStrikes':'DISPERSED','finalStrike':'FINAL','valiance':'VALIANCE','shatter':'SHATTER','shockwave':'SHOCKWAVE','tunnel':'TUNNEL','pounce':'POUNCE'}
legacy_resources=dict(re.findall(r'String (ENCHANT_\w+) = PREFIX_ENCHANTS \+ "([^";]+)";', (legacyroot/'client/lib/LibResources.java').read_text()))
aspect_names={'EARTH':'terra','ENTROPY':'perditio','FIRE':'ignis','WATER':'aqua','AIR':'aer','ORDER':'ordo','BEAST':'bestia'}
enchantments={}
for owner,old,icon,body in re.findall(r'registerExponentialCostData\(\s*(Enchantment|Config|ModEnchantments)\.(\w+),\s*LibResources\.(\w+),\s*(?:true|false),([\s\S]*?)\);',source):
 ident=('minecraft:'+vanilla.get(old,old)) if owner=='Enchantment' else ('thaumcraft:'+{'enchHaste':'haste','enchRepair':'repair'}[old]) if owner=='Config' else NS+':'+modern[old]
 costs={aspect_names[tag]:int(amount) for tag,amount in re.findall(r'\.add\(Aspect\.(\w+), (\d+)\)',body)}
 texture=legacy_resources[icon];newtexture=texture.removesuffix('.png')
 mapping[f'assets/{NS}/textures/enchantments/{texture.lower()}']='assets/ttinkerer/textures/enchants/'+texture
 enchantments[ident]={'enchantment':ident,'research':NS+':TTENCH_'+research_names[old] if owner=='ModEnchantments' else '', 'aspects':costs,'icon':NS+':textures/enchantments/'+texture.lower()}
 if owner=='ModEnchantments':
  oldname='dispersedStrike' if old=='dispersedStrikes' else old
  keys['enchantment.'+NS+'.'+modern[old]]='enchantment.ttinkerer:'+oldname
write(f'data/{NS}/osmotic_enchantments/main.json',{'format':1,'entries':list(enchantments.values())})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Spell cloth and the complete osmotic enchanter research chain.
cloth_cost={'praecantatio':10,'perditio':6,'permutatio':4}
write(f'data/{NS}/recipes/spell_cloth.json',{'type':'thaumcraft:crucible','research':NS+':SPELL_CLOTH','legacy_order':10040,'catalyst':{'item':'thaumcraft:enchanted_fabric'},'aspects':cloth_cost,'result':item(NS+':spell_cloth')})
write(f'data/{NS}/recipes/spell_cloth_disenchant.json',{'type':NS+':spell_cloth','category':'misc'})
write(f'data/{NS}/tags/items/spell_cloth_immune.json',{'replace':False,'values':[]})
write(f'assets/{NS}/models/item/spell_cloth.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/spell_cloth'}})
mapping[f'assets/{NS}/textures/item/spell_cloth.png']='assets/ttinkerer/textures/items/spellCloth.png'
keys['item.'+NS+'.spell_cloth']='item.spellCloth.name'
name='osmotic_enchanter';costs={'praecantatio':50,'potentia':20,'alienis':20,'vacuos':20,'cognitio':10};parts=['thaumcraft:obsidian_totem']*5+['thaumcraft:thaumium_ingot']*2+[NS+':spell_cloth']
write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:infusion','research':NS+':ENCHANTER','instability':15,'aspects':costs,'center':{'ingredient':{'item':'minecraft:enchanting_table'}},'components':[{'ingredient':{'item':p}} for p in parts],'result':item(NS+':'+name)})
new_display=[{'key':NS+':spell_cloth','class':'modern','data':{'kind':'crucible','research':NS+':SPELL_CLOTH','aspects':cloth_cost,'catalyst':recipeitem('thaumcraft:enchanted_fabric'),'output':recipeitem(NS+':spell_cloth')}},{'key':NS+':'+name,'class':'modern','data':{'kind':'infusion','research':NS+':ENCHANTER','instability':15,'aspects':costs,'input':recipeitem('minecraft:enchanting_table'),'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(p) for p in parts]}}}]
for key,name,col,row,complexity,aspects,parent,hidden,count,kind in [('SPELL_CLOTH','spell_cloth',3,2,2,{'praecantatio':2,'pannus':1},[],['ENCHFABRIC'],1,'CRUCIBLE'),('ENCHANTER','osmotic_enchanter',5,4,3,{'praecantatio':2,'auram':1,'alienis':1,'tenebrae':1,'cognitio':1},[NS+':SPELL_CLOTH'],[],3,'INFUSION')]:
 catalog['categories'][0]['research'].append({'key':NS+':'+key,'category':'TT_CATEGORY','displayColumn':col,'displayRow':row,'complexity':complexity,'aspects':aspects,'parents':parent,'parentsHidden':hidden,'pageCount':count+1,'icon_item':item(NS+':'+name)})
 pagefile['entries'].append({'key':NS+':'+key,'pages':[{'type':'TEXT','text':f'tc.research_page.{NS}:{key}.{i}'} for i in range(count)]+[{'type':kind+'_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]})
 for field,old in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:{key}']='ttresearch.'+old+'.'+key
 for i in range(count):keys[f'tc.research_page.{NS}:{key}.{i}']=f'ttresearch.page.{key}.{i}'
name='osmotic_enchanter'
write(f'assets/{NS}/models/block/{name}.json',{'parent':'minecraft:block/block','textures':{'particle':NS+':block/enchanter2',**{str(i):NS+':block/enchanter'+str(i) for i in range(3)}},'elements':[{'from':[0,0,0],'to':[16,12,16],'faces':{face:{'texture':'#'+('1' if face=='up' else '0' if face=='down' else '2')} for face in ['up','down','north','south','east','west']}}]})
write(f'assets/{NS}/blockstates/{name}.json',{'variants':{'':{'model':NS+':block/'+name}}});write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':block/'+name})
write(f'data/{NS}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for i in range(3):mapping[f'assets/{NS}/textures/block/enchanter{i}.png']=f'assets/ttinkerer/textures/blocks/enchanter{i}.png'
mapping[f'assets/{NS}/textures/gui/{name}.png']='assets/ttinkerer/textures/gui/enchanter.png'
keys['block.'+NS+'.'+name]='tile.enchanter.name'
pickaxe['values'].append(NS+':'+name);write('data/minecraft/tags/blocks/mineable/pickaxe.json',pickaxe)
# Extract the fourteen original research placements, aspects and prerequisites.
research_src=(legacyroot/'common/research/ResearchHelper.java').read_text(encoding='utf-8')
research_constants=dict(re.findall(r'String (KEY_\w+) = "([^";]+)";', (legacyroot/'common/lib/LibResearch.java').read_text()))
aspect_fields={'AIR':'aer','MOTION':'motus','MAGIC':'praecantatio','FIRE':'ignis','ENTROPY':'perditio','VOID':'vacuos','SENSES':'sensus','WEAPON':'telum','HUNGER':'fames','ORDER':'ordo','ARMOR':'tutamen','EARTH':'terra','TOOL':'instrumentum','HEAL':'sano'}
for symbol,aspecttext,col,row,complexity,icon,parenttext in re.findall(r'new TTResearchItem\(\s*LibResearch\.(KEY_ENCHANT_\w+),\s*new AspectList\(\)([\s\S]*?),\s*(-?\d+),\s*(-?\d+),\s*(\d+),\s*new ResourceLocation\(LibResources\.(ENCHANT_\w+)\)\)\s*\.setParents\(([\s\S]*?)\);',research_src):
 key=research_constants[symbol];parents=[NS+':'+research_constants[p] for p in re.findall(r'LibResearch\.(KEY_\w+)',parenttext)];aspects={aspect_fields[k]:int(v) for k,v in re.findall(r'\.add\(Aspect\.(\w+), (\d+)\)',aspecttext)}
 catalog['categories'][0]['research'].append({'key':NS+':'+key,'category':'TT_CATEGORY','displayColumn':int(col),'displayRow':int(row),'complexity':int(complexity),'aspects':aspects,'parents':parents,'isSecondary':True,'pageCount':1,'icon_resource':NS+':textures/enchantments/'+legacy_resources[icon].lower()})
 pagefile['entries'].append({'key':NS+':'+key,'pages':[{'type':'TEXT','text':f'tc.research_page.{NS}:{key}.0'}]})
 for field,old in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:{key}']='ttresearch.'+old+'.'+key
 keys[f'tc.research_page.{NS}:{key}.0']=f'ttresearch.page.{key}.0'
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display})
for language,text in [('en_us','Click to cycle level; after maximum, click to remove.'),('zh_cn','点击提升等级，最高级后再次点击取消。')]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {};lang['gui.thaumic_tinkerer.enchanter.cycle']=text;write(f'assets/{NS}/lang/{language}.json',lang)
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Worn talismans use the modern Curios necklace slot and server-owned state.
talisman_display=[]
for name,old,key,col,row,complexity,research_aspects,parents,concealed,instability,costs,center,parts in [
 ('xp_talisman','xpTalisman','XP_TALISMAN',4,-1,2,{'lucrum':1,'praecantatio':1,'humanus':1},['JARBRAIN',NS+':SPELL_CLOTH'],True,6,{'lucrum':20,'permutatio':10,'bestia':10,'machina':5},'minecraft:gold_ingot',['minecraft:quartz']*2+['thaumcraft:zombie_brain','minecraft:diamond']),
 ('cleansing_talisman','cleansingTalisman','CLEANSING_TALISMAN',-3,4,3,{'sano':2,'ordo':1,'venenum':1},[NS+':DARK_QUARTZ'],False,5,{'sano':10,'instrumentum':10,'humanus':20,'victus':10},'minecraft:ender_pearl',[NS+':dark_quartz']*4+['minecraft:ghast_tear','thaumcraft:alumentum'])]:
 write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:infusion','research':NS+':'+key,'instability':instability,'aspects':costs,'center':{'ingredient':{'item':center}},'components':[{'ingredient':{'item':p}} for p in parts],'result':item(NS+':'+name)})
 talisman_display.append({'key':NS+':'+name,'class':'modern','data':{'kind':'infusion','research':NS+':'+key,'instability':instability,'aspects':costs,'input':recipeitem(center),'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(p) for p in parts]}}})
 catalog['categories'][0]['research'].append({'key':NS+':'+key,'category':'TT_CATEGORY','displayColumn':col,'displayRow':row,'complexity':complexity,'aspects':research_aspects,'parents':parents,'isConcealed':concealed,'isSecondary':True,'pageCount':2,'icon_item':item(NS+':'+name)})
 pagefile['entries'].append({'key':NS+':'+key,'pages':[{'type':'TEXT','text':f'tc.research_page.{NS}:{key}.0'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]})
 keys['item.'+NS+'.'+name]='item.'+old+'.name'
 for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:{key}']='ttresearch.'+legacy+'.'+key
 keys[f'tc.research_page.{NS}:{key}.0']=f'ttresearch.page.{key}.0'
 for enabled in [False,True]:
  suffix='_enabled' if enabled else '';mapping[f'assets/{NS}/textures/item/{name}{suffix}.png']=f'assets/ttinkerer/textures/items/{old}{1 if enabled else 0}.png'
  modeldata={'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name+suffix}}
  if not enabled:modeldata['overrides']=[{'predicate':{NS+':enabled':1},'model':NS+':item/'+name+'_enabled'}]
  write(f'assets/{NS}/models/item/{name}{suffix}.json',modeldata)
write('data/curios/tags/items/necklace.json',{'replace':False,'values':[NS+':xp_talisman',NS+':cleansing_talisman']})
for key in ['full','absorbing','notAbsorbing','active','inactive']:keys['ttmisc.'+key]='ttmisc.'+key
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Cursed spirits blade, soul aspects and the Tablet of Necromancy.
soul_aspects=['aqua','humanus','aer','volatus','ignis','praecantatio','exanimis','corpus','bestia','venenum','terra','alienis','iter','metallum','limus','pannus','messis']
soul_display=[]
raw_rules=[]
for aspect in soul_aspects:
 raw='soul_'+aspect;condensed='condensed_soul_'+aspect;infused='infused_soul_'+aspect
 for grade,name in enumerate([raw,condensed,infused]):
  write(f'assets/{NS}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name}})
  mapping[f'assets/{NS}/textures/item/{name}.png']=f'assets/ttinkerer/textures/items/{aspect}'+('_condensed' if grade==1 else '')+'.png'
  keys['item.'+NS+'.'+name]='item.mobAspect'+('.condensed' if grade==1 else '.infused' if grade==2 else '')+'.name'
 raw_rules.append({'items':[NS+':'+raw],'aspects':[{'aspect':aspect,'amount':8}]})
 shape(condensed,['XXX']*3,{'X':{'item':NS+':'+raw}},condensed)
 soul_display.append({'key':NS+':'+condensed,'class':'modern','data':{'kind':'crafting_shaped','output':recipeitem(NS+':'+condensed),'width':3,'height':3,'ingredients':{'kind':'sequence','elements':[recipeitem(NS+':'+raw)]*9}}})
 write(f'data/{NS}/recipes/{infused}.json',{'type':'thaumcraft:infusion','research':NS+':SUMMON','instability':4,'aspects':{aspect:10},'center':{'ingredient':{'item':NS+':'+condensed}},'components':[{'ingredient':{'item':NS+':'+condensed}}]*8,'result':item(NS+':'+infused)})
 soul_display.append({'key':NS+':'+infused,'class':'modern','data':{'kind':'infusion','research':NS+':SUMMON','instability':4,'aspects':{aspect:10},'input':recipeitem(NS+':'+condensed),'output':recipeitem(NS+':'+infused),'ingredients':{'kind':'sequence','elements':[recipeitem(NS+':'+condensed)]*8}}})
write(f'data/{NS}/item_aspects/soul_essences.json',{'format':1,'entries':raw_rules})
name='blood_sword';costs={'fames':20,'tenebrae':5,'spiritus':10,'humanus':6};center='thaumcraft:thaumium_sword';parts=['minecraft:rotten_flesh','minecraft:porkchop','minecraft:beef','minecraft:bone','minecraft:diamond','minecraft:ghast_tear']
write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:infusion','research':NS+':BLOOD_SWORD','instability':6,'aspects':costs,'center':{'ingredient':{'item':center}},'components':[{'ingredient':{'item':part}} for part in parts],'result':item(NS+':'+name)})
soul_display.append({'key':NS+':'+name,'class':'modern','data':{'kind':'infusion','research':NS+':BLOOD_SWORD','instability':6,'aspects':costs,'input':recipeitem(center),'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(p) for p in parts]}}})
for suffix,index in [('',0),('_enabled',1)]:
 mapping[f'assets/{NS}/textures/item/{name}{suffix}.png']=f'assets/ttinkerer/textures/items/bloodSword{index}.png'
 modeldata={'parent':'minecraft:item/handheld','textures':{'layer0':NS+':item/'+name+suffix}}
 if not suffix:modeldata['overrides']=[{'predicate':{NS+':enabled':1},'model':NS+':item/'+name+'_enabled'}]
 write(f'assets/{NS}/models/item/{name}{suffix}.json',modeldata)
keys['item.'+NS+'.blood_sword']='item.bloodSword.name'
for mode in ['activateEssentiaHarvest','deactivateEssentiaHarvest']:keys['ttmisc.bloodSword.'+mode]='ttmisc.bloodSword.'+mode
name='summoning_tablet';costs={'ordo':50,'perditio':50};ingredients={'W':'thaumcraft:obsidian_tile','S':'minecraft:stone'}
write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:arcane','research':NS+':SUMMON','pattern':['WWW','SSS'],'key':{k:{'item':v} for k,v in ingredients.items()},'vis':costs,'result':item(NS+':'+name)})
soul_display.append({'key':NS+':'+name,'class':'modern','data':{'kind':'arcane_shaped','research':NS+':SUMMON','width':3,'height':2,'aspects':costs,'output':recipeitem(NS+':'+name),'ingredients':{'kind':'sequence','elements':[recipeitem(ingredients[c]) for c in 'WWWSSS']}}})
write(f'assets/{NS}/models/block/{name}.json',{'parent':'minecraft:block/block','textures':{'particle':NS+':block/summoning_side','top':NS+':block/summoning_top','side':NS+':block/summoning_side','bottom':'minecraft:block/obsidian'},'elements':[{'from':[0,0,0],'to':[16,2,16],'faces':{f:{'texture':'#'+('top' if f=='up' else 'bottom' if f=='down' else 'side')} for f in ['up','down','north','south','east','west']}}]})
write(f'assets/{NS}/blockstates/{name}.json',{'variants':{'':{'model':NS+':block/'+name}}});write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':block/'+name})
write(f'data/{NS}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for label,index in [('top',1),('side',2)]:mapping[f'assets/{NS}/textures/block/summoning_{label}.png']=f'assets/ttinkerer/textures/blocks/spawner{index}.png'
keys['block.'+NS+'.'+name]='tile.spawner.name';pickaxe['values'].append(NS+':'+name);write('data/minecraft/tags/blocks/mineable/pickaxe.json',pickaxe)
for key,name,x,y,warp,aspects,parent in [('BLOOD_SWORD','blood_sword',-4,6,1,{'fames':2,'telum':1,'corpus':1,'spiritus':1},'CLEANSING_TALISMAN'),('SUMMON','summoning_tablet',-5,8,3,{'telum':1,'bestia':3,'praecantatio':3},'BLOOD_SWORD')]:
 pages=[{'type':'TEXT','text':f'tc.research_page.{NS}:{key}.0'}]
 if key=='BLOOD_SWORD':pages.append({'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':blood_sword'],'class':'modern'}})
 else:
  pages.extend([{'type':kind+'_CRAFTING','recipe':{'kind':'reference','keys':refs,'class':'modern'}} for kind,refs in [('ARCANE',[NS+':summoning_tablet']),('NORMAL',[NS+':condensed_soul_'+a for a in soul_aspects]),('INFUSION',[NS+':infused_soul_'+a for a in soul_aspects])]])
 pages.append({'type':'TEXT','text':f'tc.research_page.{NS}:{key}.1'})
 catalog['categories'][0]['research'].append({'key':NS+':'+key,'category':'TT_CATEGORY','displayColumn':x,'displayRow':y,'complexity':3,'warp':warp,'aspects':aspects,'parents':[NS+':'+parent],'isSecondary':key=='BLOOD_SWORD','pageCount':len(pages),'icon_item':item(NS+':'+name)})
 pagefile['entries'].append({'key':NS+':'+key,'pages':pages})
 for field,old in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:{key}']='ttresearch.'+old+'.'+key
 for i in range(2):keys[f'tc.research_page.{NS}:{key}.{i}']=f'ttresearch.page.{key}.{i}'
rows=[('chicken','messis volatus bestia'),('pig','bestia terra iter'),('mooshroom','bestia terra messis'),('cow','bestia terra bestia'),('sheep','terra pannus bestia'),('ocelot','bestia terra alienis'),('wolf','bestia bestia bestia'),('horse','bestia bestia iter'),('snow_golem','aqua aqua humanus'),('iron_golem','metallum metallum humanus'),('villager','humanus humanus humanus'),('squid','aqua aqua aqua'),('bat','aer aer volatus'),('blaze','ignis ignis ignis'),('zombified_piglin','exanimis corpus ignis'),('magma_cube','ignis limus limus'),('ghast','ignis volatus volatus'),('thaumcraft:fire_bat','volatus ignis praecantatio'),('thaumcraft:brainy_zombie','praecantatio exanimis corpus'),('zombie','corpus corpus exanimis'),('skeleton','exanimis humanus exanimis'),('wither_skeleton','exanimis venenum ignis'),('creeper','praecantatio bestia alienis'),('witch','praecantatio exanimis alienis'),('cave_spider','bestia venenum venenum'),('spider','bestia exanimis exanimis'),('slime','limus limus bestia'),('silverfish','metallum metallum terra'),('enderman','alienis alienis humanus'),('thaumcraft:wisp','aer praecantatio praecantatio')]
write(f'data/{NS}/mob_essences/main.json',{'format':1,'entries':[{'entity':e if ':' in e else 'minecraft:'+e,'aspects':a.split()} for e,a in rows]})
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Knowledge tools, directional platforms and paving variants.
utility_display=[]
def utility_recipe(name,kind,research,pattern=None,ingredients=None,costs=None,result_count=1,center=None,parts=None,instability=0):
 ref=NS+':'+name;costs=costs or {};ingredients=ingredients or {}
 if kind=='infusion':
  write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:infusion','research':research,'instability':instability,'aspects':costs,'center':{'ingredient':{'item':center}},'components':[{'ingredient':{'item':p}} for p in parts],'result':item(ref,result_count)})
  data={'kind':'infusion','research':research,'instability':instability,'aspects':costs,'input':recipeitem(center),'output':recipeitem(ref,result_count),'ingredients':{'kind':'sequence','elements':[recipeitem(p) for p in parts]}}
 else:
  recipe={'type':'thaumcraft:arcane' if kind=='arcane' else 'minecraft:crafting_shaped','pattern':pattern,'key':{k:{'item':v} for k,v in ingredients.items()},'result':item(ref,result_count)}
  if kind=='arcane':recipe.update(research=research,vis=costs)
  if kind!='custom':write(f'data/{NS}/recipes/{name}.json',recipe)
  data={'kind':'arcane_shaped' if kind=='arcane' else 'crafting_shaped','width':len(pattern[0]),'height':len(pattern),'output':recipeitem(ref,result_count),'ingredients':{'kind':'sequence','elements':[recipeitem(ingredients[c]) if c!=' ' else {'kind':'sequence','elements':[]} for row in pattern for c in row]}}
  if kind=='arcane':data.update(research=research,aspects=costs)
 utility_display.append({'key':ref,'class':'modern','data':data})
utility_recipe('revealing_helm','arcane',NS+':REVEALING_HELM',['GH'],{'G':'thaumcraft:goggles','H':'thaumcraft:thaumium_helmet'},{a:5 for a in ['aer','aqua','terra','ignis','ordo','perditio']})
utility_recipe('infused_inkwell','infusion',NS+':INFUSED_INKWELL',costs={'vacuos':8,'tenebrae':8},center='thaumcraft:scribing_tools',parts=['thaumcraft:air_shard','thaumcraft:essentia_jar','thaumcraft:quicksilver'],instability=2)
shape('refill_infused_inkwell',['III','IBI','III'],{'I':{'tag':'forge:dyes/black'},'B':{'item':NS+':infused_inkwell'}},'infused_inkwell')
utility_recipe('share_book','custom','',[' S ','PTP',' P '],{'S':'thaumcraft:scribing_tools','T':'thaumcraft:thaumonomicon','P':'minecraft:paper'})
write(f'data/{NS}/recipes/share_book.json',{'type':NS+':share_book','category':'misc'})
utility_recipe('platform','arcane',NS+':PLATFORM',[' S ','G G'],{'S':'thaumcraft:silverwood_planks','G':'thaumcraft:greatwood_planks'},{'aer':2,'perditio':4},2)
for name,old in [('infused_inkwell','infusedInkwell'),('revealing_helm','revealingHelm'),('share_book','shareBook')]:
 write(f'assets/{NS}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name}})
 mapping[f'assets/{NS}/textures/item/{name}.png']=f'assets/ttinkerer/textures/items/{old}.png';keys[f'item.{NS}.{name}']='item.'+old+'.name'
mapping[f'assets/{NS}/textures/models/revealing.png']='assets/ttinkerer/textures/model/revealing.png'
for key in ['write','sync','noAssign','playerName']:keys['ttmisc.shareTome.'+key]='ttmisc.shareTome.'+key
for language,text in [('en_us','Vis discount: %s%%'),('zh_cn','灵力消耗减免：%s%%')]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang['ttmodern.vis_discount']=text;write(f'assets/{NS}/lang/{language}.json',lang)
for name,base,old in [('travel_slab','travel','travelSlab'),('ward_slab','warding','wardSlab'),('travel_stairs','travel','travelStairs')]:
 texture='thaumcraft:blocks/paving_stone_'+base
 if name.endswith('slab'):
  for suffix,parent in [('', 'slab'),('_top','slab_top'),('_double','cube_all')]:write(f'assets/{NS}/models/block/{name}{suffix}.json',{'parent':'minecraft:block/'+parent,'textures':{k:texture for k in (['all'] if suffix=='_double' else ['side','top','bottom'])}})
  write(f'assets/{NS}/blockstates/{name}.json',{'variants':{'type='+t:{'model':NS+':block/'+name+suffix} for t,suffix in [('bottom',''),('top','_top'),('double','_double')]}})
  loot=json.loads((OUT/f'data/{NS}/loot_tables/blocks/dark_quartz_slab.json').read_text());loot=json.loads(json.dumps(loot).replace('dark_quartz_slab',name))
  utility_recipe(name,'crafting','',['QQQ'],{'Q':'thaumcraft:'+base+'_paving_stone'},result_count=6)
 else:
  for suffix,parent in [('', 'stairs'),('_inner','inner_stairs'),('_outer','outer_stairs')]:write(f'assets/{NS}/models/block/{name}{suffix}.json',{'parent':'minecraft:block/'+parent,'textures':{k:texture for k in ['side','top','bottom']}})
  write(f'assets/{NS}/blockstates/{name}.json',json.loads((OUT/f'assets/{NS}/blockstates/dark_quartz_stairs.json').read_text().replace('dark_quartz_stairs',name)))
  loot={'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]}
  utility_recipe(name,'crafting','',['  Q',' QQ','QQQ'],{'Q':'thaumcraft:travel_paving_stone'},result_count=4)
 write(f'data/{NS}/loot_tables/blocks/{name}.json',loot);write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':block/'+name});keys[f'block.{NS}.{name}']='tile.'+old+'.name';pickaxe['values'].append(NS+':'+name)
for tag,values in [('slabs',['dark_quartz_slab','travel_slab','ward_slab']),('stairs',['dark_quartz_stairs','travel_stairs'])]:
 for kind in ['blocks','items']:write(f'data/minecraft/tags/{kind}/{tag}.json',{'replace':False,'values':[NS+':'+v for v in values]})
write('data/minecraft/tags/blocks/mineable/pickaxe.json',pickaxe)
name='platform';mapping[f'assets/{NS}/textures/block/platform.png']='assets/ttinkerer/textures/blocks/platform.png';keys[f'block.{NS}.platform']='tile.platform.name'
write(f'assets/{NS}/models/block/platform.json',{'parent':'minecraft:block/cube_all','textures':{'all':NS+':block/platform'}})
write(f'assets/{NS}/models/block/platform_covered.json',{'textures':{'particle':NS+':block/platform'},'elements':[]})
write(f'assets/{NS}/blockstates/platform.json',{'variants':{'camouflaged=false':{'model':NS+':block/platform'},'camouflaged=true':{'model':NS+':block/platform_covered'}}})
write(f'assets/{NS}/models/item/platform.json',{'parent':NS+':block/platform'})
write(f'data/{NS}/loot_tables/blocks/platform.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':platform'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write('data/minecraft/tags/blocks/mineable/axe.json',{'replace':False,'values':[NS+':platform']})
for key,name,x,y,complexity,aspects,parents,flags,kind in [
 ('REVEALING_HELM','revealing_helm',0,0,1,{'auram':2,'tutamen':1},['GOGGLES'],{'parentsHidden':['THAUMIUM']},'ARCANE'),
 ('SHARE_TOME','share_book',0,-1,0,{},[],{'isAutoUnlock':True,'isStub':True,'isRound':True},'NORMAL'),
 ('INFUSED_INKWELL','infused_inkwell',-3,0,1,{'vacuos':1,'tenebrae':1},['RESEARCH'],{'isSecondary':True},'INFUSION'),
 ('PLATFORM','platform',-2,6,3,{'sensus':2,'arbor':1,'motus':1},[NS+':CLEANSING_TALISMAN'],{'isSecondary':True,'isConcealed':True},'ARCANE')]:
 catalog['categories'][0]['research'].append({'key':NS+':'+key,'category':'TT_CATEGORY','displayColumn':x,'displayRow':y,'complexity':complexity,'aspects':aspects,'parents':parents,'pageCount':2,'icon_item':item(NS+':'+name),**flags})
 pagefile['entries'].append({'key':NS+':'+key,'pages':[{'type':'TEXT','text':f'tc.research_page.{NS}:{key}.0'},{'type':kind+'_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]})
 if key!='INFUSED_INKWELL':
  for field,old in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:{key}']='ttresearch.'+old+'.'+key
  keys[f'tc.research_page.{NS}:{key}.0']=f'ttresearch.page.{key}.0'
# The reference class has no research registration; supply an accessible research node for its gated infusion.
for language,title,lore,description in [
 ('en_us','Infused Scribing Tools','Eight times the ink','An infused inkwell holds 800 units of ink. Refill it by surrounding it with eight ink sacs in a crafting grid.'),
 ('zh_cn','灌注笔与墨','八倍墨水','灌注墨水瓶可容纳 800 单位墨水。将它放在合成网格中央，周围放入八个墨囊即可重新灌满。')]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang.update({f'tc.research_name.{NS}:INFUSED_INKWELL':title,f'tc.research_text.{NS}:INFUSED_INKWELL':lore,f'tc.research_page.{NS}:INFUSED_INKWELL.0':description});write(f'assets/{NS}/lang/{language}.json',lang)
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Six imbued fires. Transformation inputs remain limited to legacy content unless data packs add rules.
fire_display=[]
wood=['oak','spruce','birch','jungle','acacia','dark_oak']
transformations={
 'aer':[(w+'_log','sand') for w in wood]+[(w+'_leaves','sandstone') for w in wood]+[('ice','glass'),('water','cake'),('dirt','sand'),('grass_block','sand')],
 'ignis':[('grass_block','netherrack'),('dirt','netherrack'),('sand','soul_sand'),('red_sand','soul_sand'),('gravel','soul_sand'),('clay','glowstone')]+[(o+'_ore','nether_quartz_ore') for o in ['coal','iron','diamond','emerald','gold','lapis','redstone']]+[('water','lava')]+[(c,'nether_wart') for c in ['wheat','potatoes','carrots']]+[(c,'brown_mushroom') for c in ['poppy','blue_orchid','allium','azure_bluet','red_tulip','orange_tulip','white_tulip','pink_tulip','oxeye_daisy']]+[('dandelion','dandelion')],
 'terra':[('sand','dirt'),('red_sand','dirt'),('gravel','clay'),('nether_bricks','oak_planks'),('nether_brick_fence','oak_fence'),('nether_brick_stairs','oak_stairs'),('cactus','oak_log'),('snow','grass'),('stone','dirt'),('spawner','iron_block'),('cobblestone','dirt'),('glass','dirt')]+[(w+'_'+suffix,'dirt') for w in wood for suffix in ['log','leaves','planks']],
 'aqua':[('sand','ice'),('red_sand','ice'),('netherrack','snow_block'),('soul_sand','ice'),('glowstone','ice'),('lava','obsidian')],
 'ordo':[], 'perditio':[(NS+':fire_'+a,'fire') for a in ['aer','ignis','aqua','terra','ordo']]}
for index,(aspect,old,x,y,shard) in enumerate([('aer','fireAir',3,-7,'air'),('ignis','fireFire',4,-4,'fire'),('aqua','fireWater',2,-2,'water'),('terra','fireEarth',4,-6,'earth'),('ordo','fireOrder',3,-3,'order'),('perditio','fireChaos',2,-8,'entropy')]):
 name='fire_'+aspect;key='FIRE_'+aspect.upper();costs={'ignis':10,'aer':5} if aspect=='ignis' else {'ignis':5,'praecantatio':5,aspect:5}
 write(f'data/{NS}/imbued_fire/{aspect}.json',{'format':1,'aspect':aspect,'entries':[{'from':a if ':' in a else 'minecraft:'+a,'to':b if ':' in b else 'minecraft:'+b,**({'cake':True} if b=='cake' else {})} for a,b in transformations[aspect]]})
 write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:crucible','research':NS+':'+key,'legacy_order':10100+index,'aspects':costs,'catalyst':{'item':'thaumcraft:'+shard+'_shard'},'result':item(NS+':'+name)})
 fire_display.append({'key':NS+':'+name,'class':'modern','data':{'kind':'crucible','research':NS+':'+key,'aspects':costs,'output':recipeitem(NS+':'+name),'catalyst':recipeitem('thaumcraft:'+shard+'_shard')}})
 for layer in [0,1]:
  for suffix in ['.png','.png.mcmeta']:mapping[f'assets/{NS}/textures/block/{name}_{layer}'+suffix]=f'assets/ttinkerer/textures/blocks/{old}_layer_{layer}'+suffix
 mapping[f'assets/{NS}/textures/item/{name}.png']=f'assets/ttinkerer/textures/blocks/{aspect}Fire.png'
 write(f'assets/{NS}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name}})
 write(f'assets/{NS}/models/block/{name}.json',{'parent':'minecraft:block/template_fire_floor','render_type':'minecraft:cutout','textures':{'fire':NS+':block/'+name+'_0','particle':NS+':block/'+name+'_0'}})
 write(f'assets/{NS}/blockstates/{name}.json',{'variants':{'':{'model':NS+':block/'+name}}})
 keys[f'block.{NS}.{name}']='tile.'+old+'.name'
 aspects={'ignis':10} if aspect=='ignis' else {'ignis':5,aspect:5}
 catalog['categories'][0]['research'].append({'key':NS+':'+key,'category':'TT_CATEGORY','displayColumn':x,'displayRow':y,'complexity':3 if aspect=='perditio' else 2,'aspects':aspects,'parents':[NS+':BRIGHT_NITOR'],'isSecondary':True,'isConcealed':True,'pageCount':2,'icon_item':item(NS+':'+name)})
 pagefile['entries'].append({'key':NS+':'+key,'pages':[{'type':'TEXT','text':f'tc.research_page.{NS}:{key}.0'},{'type':'CRUCIBLE_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]})
 for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:{key}']='ttresearch.'+legacy+'.'+key
 keys[f'tc.research_page.{NS}:{key}.0']=f'ttresearch.page.{key}.0'
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Infused agriculture: one NBT seed supports all current and addon aspects.
crop_display=[];primal_seeds=['aer','ignis','aqua','terra','ordo','perditio'];potions=['aer','ignis','terra','aqua'];crop_research=NS+':INFUSED_POTIONS'
for index,aspect in enumerate(primal_seeds+['generic']):
 mapping[f'assets/{NS}/textures/item/seed_{aspect}.png']=f'assets/ttinkerer/textures/items/seed_{aspect}.png'
 write(f'assets/{NS}/models/item/seed_{aspect}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/seed_'+aspect}})
 for stage in range(4):
  mapping[f'assets/{NS}/textures/block/crop_{aspect}_{stage}.png']=f'assets/ttinkerer/textures/blocks/crop_{aspect}_{stage}.png'
  write(f'assets/{NS}/models/block/crop_{aspect}_{stage}.json',{'parent':'minecraft:block/crop','render_type':'minecraft:cutout','textures':{'crop':NS+':block/crop_'+aspect+'_'+str(stage)}})
write(f'assets/{NS}/models/item/infused_seeds.json',{'parent':NS+':item/seed_aer','overrides':[{'predicate':{NS+':aspect':i},'model':NS+':item/seed_'+a} for i,a in enumerate(primal_seeds+['generic'])]})
write(f'assets/{NS}/blockstates/infused_crop.json',{'variants':{f'age={age},form={form}':{'model':NS+':block/crop_'+aspect+'_'+str(3 if age==7 else min(age//2,2))} for form,aspect in enumerate(primal_seeds+['generic']) for age in range(8)}})
write(f'assets/{NS}/models/block/infused_farmland.json',{'parent':'minecraft:block/farmland','textures':{'dirt':'minecraft:block/dirt','top':NS+':block/infused_farmland'}})
write(f'assets/{NS}/blockstates/infused_farmland.json',{'variants':{'':{'model':NS+':block/infused_farmland'}}});mapping[f'assets/{NS}/textures/block/infused_farmland.png']='assets/ttinkerer/textures/blocks/infusedFarmland.png'
write(f'data/{NS}/loot_tables/blocks/infused_farmland.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:dirt'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for aspect,shard in zip(primal_seeds,['air','fire','water','earth','order','entropy']):
 name='infused_seed_'+aspect;costs={'messis':32,'meto':32};out={**item(NS+':infused_seeds'),'nbt':'{Aspect:"'+aspect+'"}'};parts=['thaumcraft:'+shard+'_shard']*4
 write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:infusion','research':crop_research,'instability':5,'aspects':costs,'center':{'ingredient':{'item':'minecraft:wheat_seeds'}},'components':[{'ingredient':{'item':p}} for p in parts],'result':out})
 crop_display.append({'key':NS+':'+name,'class':'modern','data':{'kind':'infusion','research':crop_research,'instability':5,'aspects':costs,'input':recipeitem('minecraft:wheat_seeds'),'output':{'kind':'stack','stack':out},'ingredients':{'kind':'sequence','elements':[recipeitem(p) for p in parts]}}})
for index,aspect in enumerate(potions):
 mapping[f'assets/{NS}/textures/mob_effect/imbued_{aspect}.png']=f'assets/ttinkerer/textures/items/potion_{aspect}.png'
 name='infused_potion_'+aspect;costs={'auram':5,aspect:5};grain=NS+':infused_grain_'+aspect
 write(f'data/{NS}/recipes/{name}.json',{'type':'thaumcraft:crucible','research':crop_research,'legacy_order':10200+index,'aspects':costs,'catalyst':{'item':grain},'result':item(NS+':'+name)})
 crop_display.append({'key':NS+':'+name,'class':'modern','data':{'kind':'crucible','research':crop_research,'aspects':costs,'catalyst':recipeitem(grain),'output':recipeitem(NS+':'+name)}})
 for prefix,old in [('infused_potion_','potion_'),('infused_grain_','fruit_')]:
  mapping[f'assets/{NS}/textures/item/{prefix}{aspect}.png']=f'assets/ttinkerer/textures/items/{old}{aspect}.png'
  write(f'assets/{NS}/models/item/{prefix}{aspect}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+prefix+aspect}})
 keys[f'item.{NS}.infused_grain_{aspect}']='item.infusedGrain.name';keys[f'item.{NS}.infused_potion_{aspect}']='item.infusedPotion.'+aspect.title()+'.name'
keys[f'item.{NS}.infused_seeds']='item.infusedSeeds.name'
pages=[{'type':'TEXT','text':f'tc.research_page.{crop_research}.{i}'} for i in range(6)]+[{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':infused_seed_'+a for a in primal_seeds],'class':'modern'}}]+[{'type':'CRUCIBLE_CRAFTING','recipe':{'kind':'reference','keys':[NS+':infused_potion_'+a],'class':'modern'}} for a in potions]
catalog['categories'][0]['research'].append({'key':crop_research,'category':'TT_CATEGORY','displayColumn':7,'displayRow':-5,'complexity':2,'aspects':{'aqua':5,'perditio':5},'parents':[NS+':FIRE_'+a.upper() for a in primal_seeds],'parentsHidden':['INFUSION'],'isConcealed':True,'pageCount':len(pages),'icon_item':{**item(NS+':infused_seeds'),'nbt':'{Aspect:"ignis"}'}})
pagefile['entries'].append({'key':crop_research,'pages':pages})
for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{crop_research}']='ttresearch.'+legacy+'.INFUSED_POTIONS'
for i in range(6):keys[f'tc.research_page.{crop_research}.{i}']=f'ttresearch.page.INFUSED_POTIONS.{i}'
for language,names in [('en_us',['Air Imbued','Fire Imbued','Earth Imbued','Water Imbued']),('zh_cn',['风之灌注','火之灌注','地之灌注','水之灌注'])]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang.update({f'effect.{NS}.imbued_{a}':n for a,n in zip(potions,names)});lang[f'item.{NS}.infused_inkwell']='Infused Scribing Tools' if language=='en_us' else '灌注笔与墨';write(f'assets/{NS}/lang/{language}.json',lang)
keys.pop(f'item.{NS}.infused_inkwell',None)
# Vanilla loot tables provide weighted crop rewards and normal data-pack replacement.
loot={}
def crop_reward(aspect,ident,count=1,weight=1,nbt=None,tag=False):
 entry={'type':'minecraft:tag' if tag else 'minecraft:item','name':ident,'weight':weight}
 if tag:entry['expand']=True
 functions=[]
 if count!=1:functions.append({'function':'minecraft:set_count','count':count})
 if nbt:functions.append({'function':'minecraft:set_nbt','tag':nbt})
 if functions:entry['functions']=functions
 loot.setdefault(aspect,[]).append(entry)
for a in potions:crop_reward(a,NS+':infused_grain_'+a)
fixed={
 'ordo':[('glass',16)],'perditio':[('sand',16)],'alienis':[('ender_pearl',4,10),('ender_eye',4,5),('thaumcraft:void_seed',1)],
 'cognitio':[('paper',64,15),('book',32,10),('bookshelf',16,5),('thaumcraft:knowledge_fragment',4,5)],'corpus':[('thaumcraft:zombie_brain',16)],'exanimis':[('rotten_flesh',32),('bone',24)],
 'fabrico':[(NS+':dark_quartz_block',32),('thaumcraft:alchemy_furnace',16),('crafting_table',4)],'fames':[('nether_wart',16)],'gelum':[('ice',2),('snowball',64),('snow_block',32),('snow',8)],
 'tutamen':[('diamond_'+a,1) for a in ['boots','leggings','chestplate','helmet']],'perfodio':[('thaumcraft:thaumium_pickaxe',1)],'meto':[('thaumcraft:thaumium_hoe',1)],'telum':[('thaumcraft:thaumium_sword',1)],'instrumentum':[('thaumcraft:thaumium_shovel',1),('thaumcraft:thaumium_axe',1)],
 'lucrum':[('gold_ingot',4),('emerald',2)],'lux':[('glowstone_dust',16,5),('thaumcraft:nitor',4),(NS+':bright_nitor',1)],'machina':[('piston',8),('hopper',4),('dropper',8),('dispenser',8)],'messis':[('wheat',32)],'metallum':[('iron_ingot',4,100)],'mortuus':[('bone',32)],'motus':[('rail',1,10),('activator_rail',1)],'pannus':[('white_wool',16,30),('string',15,10)],'permutatio':[('copper_ingot',4),('thaumcraft:cinnabar_ore',4)],'potentia':[('thaumcraft:alumentum',12)],
 'praecantatio':[('thaumcraft:'+s+'_shard',1) for s in ['air','fire','water','earth','order','entropy','balanced']],'sano':[('golden_apple',1),('cake',1)],'spiritus':[('soul_sand',16,2)],'tenebrae':[('obsidian',10)],'vacuos':[('bucket',1),('bowl',4),('thaumcraft:void_ingot',2)],'venenum':[('thaumcraft:quicksilver',16),('spider_eye',8)],'victus':[('egg',8)],'vinculum':[('cobweb',4),('thaumcraft:amber',16)],'iter':[('thaumcraft:travel_paving_stone',4)],'vitium':[('thaumcraft:tainted_goo',4)],'vitreus':[('diamond',1),('quartz',8),('glass',32,4)]}
for aspect,rewards in fixed.items():
 for entry in rewards:
  ident,count,*weight=entry;crop_reward(aspect,ident if ':' in ident else 'minecraft:'+ident,count,weight[0] if weight else 1)
for aspect,tag,count in [('arbor','minecraft:logs',1),('herba','minecraft:saplings',1),('limus','forge:slimeballs',4),('messis','forge:seeds',1),('vinculum','minecraft:wooden_trapdoors',1)]:crop_reward(aspect,tag,count,tag=True)
crop_reward('herba','minecraft:grass');crop_reward('sano','minecraft:potion',nbt='{Potion:"minecraft:strong_healing"}')
for core in ['blank','gather','fill','empty','harvest','guard','butcher','liquid','sorting','lumber','use','essentia']:crop_reward('humanus','thaumcraft:golem_core_'+core)
colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
for color in colors:
 crop_reward('pannus','minecraft:'+color+'_wool',4)
 for suffix,count in [('dye',1),('stained_glass',1),('stained_glass_pane',2)]:crop_reward('sensus','minecraft:'+color+'_'+suffix,count)
# Use the frozen dependency's aspect catalog, never invoke the base build.
import zipfile
with zipfile.ZipFile(ROOT/'libs/thaumcraft-0.4.11.jar') as frozen:
 all_aspects=[row['tag'] for row in json.loads(frozen.read('data/thaumcraft/aspect_registry.json'))]
for aspect in all_aspects:crop_reward('auram','thaumcraft:wisp_essence',nbt='{Aspects:[{key:"'+aspect+'",amount:2}]}')
for aspect in set(all_aspects)|set(loot):write(f'data/{NS}/loot_tables/crop_loot/{aspect.replace(":","/")}.json',{'type':'minecraft:chest','pools':[{'rolls':1,'entries':loot[aspect]}] if loot.get(aspect) else []})
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Levitational locomotive and its paired relays.
utility_recipe('mobilizer','infusion',NS+':LEVITATOR_LOCOMOTIVE',costs={'motus':15,'ordo':20,'praecantatio':15},center='thaumcraft:levitator',parts=['minecraft:iron_ingot','minecraft:feather','minecraft:iron_ingot','thaumcraft:obsidian_tile'],instability=4)
utility_recipe('mobilizer_relay','arcane',NS+':LEVITATOR_LOCOMOTIVE',['WFW','SIs','WFW'],{'W':'thaumcraft:greatwood_log','F':'minecraft:glass','S':'thaumcraft:air_shard','I':'minecraft:iron_ingot','s':'thaumcraft:earth_shard'},{'aer':20,'ordo':5,'terra':15})
for name,old in [('mobilizer','Levitational Locomotive'),('mobilizer_relay','Levitational Locomotive Relay')]:
 for side,index in [('bottom',0),('top',1),('side',2)]:mapping[f'assets/{NS}/textures/block/{name}_{side}.png']=f'assets/ttinkerer/textures/blocks/{old}{index}.png'
 write(f'assets/{NS}/models/block/{name}.json',{'parent':'minecraft:block/cube_bottom_top','textures':{side:NS+':block/'+name+'_'+side for side in ['bottom','top','side']}})
 write(f'assets/{NS}/blockstates/{name}.json',{'variants':{'':{'model':NS+':block/'+name}}});write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':block/'+name})
 write(f'data/{NS}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 pickaxe['values'].append(NS+':'+name);keys[f'block.{NS}.{name}']='tile.'+old+'.name'
write('data/minecraft/tags/blocks/mineable/pickaxe.json',pickaxe)
write(f'data/{NS}/tags/blocks/mobilizer_immovable.json',{'replace':False,'values':['minecraft:bedrock','minecraft:barrier','minecraft:end_portal','minecraft:end_portal_frame','minecraft:nether_portal','minecraft:piston_head','minecraft:moving_piston','thaumcraft:portable_hole']})
key='LEVITATOR_LOCOMOTIVE'
catalog['categories'][0]['research'].append({'key':NS+':'+key,'category':'TT_CATEGORY','displayColumn':-7,'displayRow':5,'complexity':3,'aspects':{'motus':2,'ordo':2},'parents':[NS+':MAGNETS'],'isSecondary':True,'pageCount':3,'icon_item':item(NS+':mobilizer')})
pagefile['entries'].append({'key':NS+':'+key,'pages':[{'type':'TEXT','text':f'tc.research_page.{NS}:{key}.0'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':mobilizer'],'class':'modern'}},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','keys':[NS+':mobilizer_relay'],'class':'modern'}}]})
for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:{key}']='ttresearch.'+legacy+'.'+key
keys[f'tc.research_page.{NS}:{key}.0']=f'ttresearch.page.{key}.0'
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# KAMI materials and open-registry wand components. No base classes are patched.
kami_materials={'ichor':'ichor','ichor_cloth':'ichorCloth','ichorium':'ichorium','ichor_nugget':'ichorNugget','ichor_cap':'ichorCap','ichorcloth_rod':'ichorclothRod','nether_shard':'netherShard','ender_shard':'enderShard'}
for name,old in kami_materials.items():
 mapping[f'assets/{NS}/textures/item/{name}.png']=f'assets/ttinkerer/textures/items/{old}.png'
 write(f'assets/{NS}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name}})
 keys[f'item.{NS}.{name}']='item.'+old+'.name'
for name,old in [('ichor_cap','capIchor'),('ichorcloth_rod','rodIchorcloth')]:mapping[f'assets/{NS}/textures/model/{name}.png']=f'assets/ttinkerer/textures/model/{old}.png'
write(f'data/{NS}/thaumcraft/wand_rods.json',[{'tag':NS+':ichorcloth','capacity':1000,'craftCost':10,'glowing':True,'item':NS+':ichorcloth_rod','research':NS+':ROD_ICHORCLOTH','texture':NS+':textures/model/ichorcloth_rod.png'}])
write(f'data/{NS}/thaumcraft/wand_caps.json',[{'tag':NS+':ichor','cost':0.7,'craftCost':10,'item':NS+':ichor_cap','research':NS+':CAP_ICHOR','texture':NS+':textures/model/ichor_cap.png'}])
write(f'data/{NS}/recipes/ichor_nugget.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':NS+':ichorium'}],'result':item(NS+':ichor_nugget',9)})
for tag,name in [('ingots/ichorium','ichorium'),('nuggets/ichorium','ichor_nugget')]:write(f'data/forge/tags/items/{tag}.json',{'replace':False,'values':[NS+':'+name]})
allvis=lambda amount:{a:amount for a in primal_seeds}
utility_recipe('ichor_cloth','arcane',NS+':ICHOR_CLOTH',['CCC','III','DDD'],{'C':'thaumcraft:enchanted_fabric','I':NS+':ichor','D':'minecraft:diamond'},allvis(125),3)
utility_recipe('ichorium','arcane',NS+':ICHORIUM',[' T ','IDI',' I '],{'T':'thaumcraft:thaumium_ingot','I':NS+':ichor','D':'minecraft:diamond'},allvis(100))
utility_recipe('ichor_cap','arcane',NS+':CAP_ICHOR',['ICI',' M ','ICI'],{'I':NS+':ichor','C':'thaumcraft:wand_cap_thaumium','M':NS+':ichorium'},allvis(100),2)
utility_recipe('ichor','infusion',NS+':ICHOR',costs={'humanus':32,'lux':32,'spiritus':64},result_count=8,center='minecraft:nether_star',parts=['minecraft:diamond',NS+':ender_shard','minecraft:ender_eye',NS+':nether_shard'],instability=7)
utility_recipe('ichorcloth_rod','infusion',NS+':ROD_ICHORCLOTH',costs={'praecantatio':100,'lux':32,'instrumentum':32},center='thaumcraft:wand_rod_silverwood',parts=[NS+':ichor',NS+':ichor_cloth','thaumcraft:salis_mundus','minecraft:ghast_tear','thaumcraft:salis_mundus',NS+':ichor_cloth'],instability=9)
# Runtime API supplies enabled prerequisites after datapacks and server config load.
kami_requirements=[]
kami_keys=[]
for key,name,x,y,costs,parents,hidden,warp,kind in [
 ('DIMENSION_SHARDS','ender_shard',7,8,{},[],[],0,None),
 ('ICHOR','ichor',9,8,{'humanus':1,'lux':2,'spiritus':1,'vitium':1},[],[],5,'INFUSION_CRAFTING'),
 ('ICHOR_CLOTH','ichor_cloth',11,7,{'pannus':2,'lux':1,'fabrico':1,'sensus':1},['ICHOR'],[],0,'ARCANE_CRAFTING'),
 ('ICHORIUM','ichorium',11,9,{'metallum':2,'lux':1,'fabrico':1,'instrumentum':1},['ICHOR'],['ICHOR_CLOTH'],0,'ARCANE_CRAFTING'),
 ('CAP_ICHOR','ichor_cap',11,11,{'instrumentum':2,'metallum':1,'lux':1,'praecantatio':1},['ICHORIUM'],[],2,'ARCANE_CRAFTING'),
 ('ROD_ICHORCLOTH','ichorcloth_rod',14,2,{'instrumentum':2,'pannus':1,'lux':1,'praecantatio':1},['ICHOR_CLOTH'],['CAP_ICHOR'],1,'INFUSION_CRAFTING')]:
 full=NS+':'+key;kami_keys.append(full);pages=[{'type':'TEXT','text':f'tc.research_page.{full}.0'}]
 if kind:pages.append({'type':kind,'recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}})
 row={'key':full,'category':'TT_CATEGORY','displayColumn':x,'displayRow':y,'complexity':3 if kind else 0,'aspects':costs,'parents':[NS+':'+p for p in parents],'parentsHidden':list(dict.fromkeys(([NS+':'+p for p in hidden]+kami_requirements) if kind else [])),'warp':warp,'isConcealed':True,'pageCount':len(pages),'icon_item':item(NS+':'+name)}
 if not kind:row.update(isAutoUnlock=True,isStub=True,isRound=True)
 catalog['categories'][0]['research'].append(row);pagefile['entries'].append({'key':full,'pages':pages})
 for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.'+key
 keys[f'tc.research_page.{full}.0']='ttresearch.page.'+key+'.0'
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Standard Ichorium tools, before the awakened variants.
write(f'data/{NS}/tags/blocks/needs_ichorium_tool.json',{'replace':False,'values':[]})
for name,old,pattern,aspect in [('ichor_pick','ichorPick',['III',' R ',' R '],'ignis'),('ichor_shovel','ichorShovel',[' I ',' R ',' R '],'terra'),('ichor_axe','ichorAxe',['II ','IR ',' R '],'aqua'),('ichor_sword','ichorSword',[' I ',' I ',' R '],'aer')]:
 mapping[f'assets/{NS}/textures/item/{name}.png']=f'assets/ttinkerer/textures/items/{old}.png'
 write(f'assets/{NS}/models/item/{name}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':NS+':item/'+name}});keys[f'item.{NS}.{name}']='item.'+old+'.name'
 utility_recipe(name,'arcane',NS+':ICHOR_TOOLS',pattern,{'I':NS+':ichorium','R':'thaumcraft:wand_rod_silverwood'},{aspect:75})
full=NS+':ICHOR_TOOLS';kami_keys.append(full)
catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':13,'displayRow':12,'complexity':3,'aspects':{'instrumentum':2,'telum':1,'metallum':1,'fabrico':1},'parents':[NS+':ICHORIUM'],'parentsHidden':list(dict.fromkeys(kami_requirements+[NS+':ROD_ICHORCLOTH'])),'warp':2,'isConcealed':True,'pageCount':5,'icon_item':item(NS+':ichor_pick')})
pagefile['entries'].append({'key':full,'pages':[{'type':'TEXT','text':f'tc.research_page.{full}.0'}]+[{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','keys':[NS+':ichor_'+tool],'class':'modern'}} for tool in ['pick','shovel','axe','sword']]})
for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.ICHOR_TOOLS'
keys[f'tc.research_page.{full}.0']='ttresearch.page.ICHOR_TOOLS.0'
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Ichorcloth armor: original cloth appearance is supplied only by the external jar.
for index in [1,2]:mapping[f'assets/{NS}/textures/model/ichor{index}.png']=f'assets/ttinkerer/textures/model/ichor{index}.png'
for name,old,pattern,aspect in [('helm','Helm',['CCC','C C'],'aqua'),('chest','Chest',['C C','CCC','CCC'],'aer'),('legs','Legs',['CCC','C C','C C'],'ignis'),('boots','Boots',['C C','C C'],'terra')]:
 ident='ichorcloth_'+name;mapping[f'assets/{NS}/textures/item/{ident}.png']=f'assets/ttinkerer/textures/items/ichorcloth{old}.png'
 write(f'assets/{NS}/models/item/{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+ident}});keys[f'item.{NS}.{ident}']='item.ichorcloth'+old+'.name'
 utility_recipe(ident,'arcane',NS+':ICHORCLOTH_ARMOR',pattern,{'C':NS+':ichor_cloth'},{aspect:75})
full=NS+':ICHORCLOTH_ARMOR';kami_keys.append(full)
catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':17,'displayRow':5,'complexity':3,'aspects':{'tutamen':2,'pannus':1,'lux':1,'fabrico':1},'parents':[NS+':ICHOR_CLOTH'],'parentsHidden':kami_requirements,'warp':4,'isConcealed':True,'pageCount':5,'icon_item':item(NS+':ichorcloth_helm')})
pagefile['entries'].append({'key':full,'pages':[{'type':'TEXT','text':f'tc.research_page.{full}.0'}]+[{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','keys':[NS+':ichorcloth_'+piece],'class':'modern'}} for piece in ['helm','chest','legs','boots']]})
for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.ICHORCLOTH_ARMOR'
keys[f'tc.research_page.{full}.0']='ttresearch.page.ICHORCLOTH_ARMOR.0'
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Awakened tool forms, mining tags, and infusion acquisition.
for kind,values in {
 'shovel':['#minecraft:mineable/shovel'],
 'pick':['#minecraft:mineable/pickaxe','#minecraft:mineable/shovel','#minecraft:ice','#forge:glass','#forge:glass_panes','minecraft:piston','minecraft:sticky_piston'],
 'axe':['#minecraft:mineable/axe','#minecraft:leaves','#minecraft:flowers','#minecraft:saplings','#minecraft:corals','minecraft:grass','minecraft:tall_grass','minecraft:fern','minecraft:large_fern','minecraft:vine']}.items():write(f'data/{NS}/tags/blocks/awakened_{kind}_mineable.json',{'replace':False,'values':values})
mapping[f'assets/{NS}/textures/gui/soul_hearts.png']='assets/ttinkerer/textures/gui/soulHearts.png'
for kind,x,y,research_aspects,costs in [
 ('pick',13,15,{'ignis':2,'instrumentum':1,'perfodio':1,'terra':1},{'ignis':50,'perfodio':64,'metallum':32,'terra':32,'meto':32,'lucrum':16,'sensus':16}),
 ('shovel',15,15,{'terra':3,'instrumentum':1,'perfodio':1},{'terra':82,'perfodio':64,'instrumentum':32,'meto':32,'vinculum':16,'sensus':16}),
 ('axe',16,14,{'aqua':2,'instrumentum':1,'arbor':1,'messis':1},{'aqua':50,'perfodio':64,'instrumentum':32,'arbor':32,'meto':32,'messis':16,'sensus':16}),
 ('sword',16,12,{'aer':2,'telum':1,'spiritus':1,'fames':1},{'aer':50,'fames':64,'spiritus':32,'telum':32,'potentia':32,'ordo':16,'vitreus':16})]:
 name='awakened_ichor_'+kind;old='ichor'+kind.title()+'Gem';full=NS+':ICHOR_'+kind.upper()+'_GEM';kami_keys.append(full)
 for mode in range(3):
  target=f'assets/{NS}/textures/item/{name}_{mode}.png';mapping[target]=f'assets/ttinkerer/textures/items/{old}{mode}.png'
  write(f'assets/{NS}/models/item/{name}_{mode}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':NS+':item/'+name+'_'+str(mode)}})
 write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':item/'+name+'_0','overrides':[{'predicate':{NS+':tool_mode':mode},'model':NS+':item/'+name+'_'+str(mode)} for mode in [1,2]]})
 keys[f'item.{NS}.{name}']='item.'+old+'.name'
 for mode in range(3):keys[f'ttmisc.mode.{kind}.{mode}']=f'ttmisc.mode.{kind}.{mode}'
 focus='thaumcraft:focus_frost' if kind=='sword' else 'thaumcraft:focus_excavation';elemental='thaumcraft:elemental_'+('pickaxe' if kind=='pick' else kind)
 parts=[NS+':ichorium',NS+':ichor',elemental,focus,'minecraft:cactus' if kind=='sword' else 'minecraft:tnt','thaumcraft:cinnabar_cluster','thaumcraft:iron_cluster','thaumcraft:gold_cluster','minecraft:diamond',focus,elemental,NS+':ichor_cloth']
 utility_recipe(name,'infusion',full,costs=costs,center=NS+':ichor_'+kind,parts=parts,instability=15)
 pages=[{'type':'TEXT','text':f'tc.research_page.{full}.0'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]
 if kind in ['pick','sword']:pages.append({'type':'TEXT','text':f'tc.research_page.{full}.1'})
 catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':x,'displayRow':y,'complexity':3,'aspects':research_aspects,'parents':[NS+':ICHOR_TOOLS'],'parentsHidden':kami_requirements,'isConcealed':True,'pageCount':len(pages),'icon_item':item(NS+':'+name)})
 pagefile['entries'].append({'key':full,'pages':pages})
 for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.ICHOR_'+kind.upper()+'_GEM'
 for index in range(2 if kind in ['pick','sword'] else 1):keys[f'tc.research_page.{full}.{index}']=f'ttresearch.page.ICHOR_{kind.upper()}_GEM.{index}'
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Upgraded Ichorcloth armor and its original infusion chains.
for index in [1,2]:mapping[f'assets/{NS}/textures/model/ichor_gem_{index}.png']=f'assets/ttinkerer/textures/model/ichorGem{index}.png'
armor_rows=[
 ('helm',18,3,{'aqua':2,'sano':1,'fames':1,'auram':1},{'aqua':50,'tutamen':32,'fames':32,'auram':32,'lux':64,'corpus':16,'cognitio':16},['thaumcraft:thaumonomicon','thaumcraft:focus_primal','minecraft:golden_helmet','minecraft:potion','thaumcraft:goggles','minecraft:ghast_tear','minecraft:cod','minecraft:cake','minecraft:ender_eye'],'night_vision'),
 ('chest',17,7,{'aer':2,'motus':1,'volatus':1,'alienis':1},{'aer':50,'tutamen':32,'volatus':32,'ordo':32,'lux':64,'alienis':16,'sensus':16},['thaumcraft:focus_primal','thaumcraft:thaumonomicon','minecraft:golden_chestplate',NS+':focus_flight','thaumcraft:hover_harness',NS+':focus_deflection','minecraft:feather','minecraft:ghast_tear','minecraft:arrow'],None),
 ('legs',17,9,{'ignis':2,'sano':1,'lucrum':1,'potentia':1},{'ignis':50,'tutamen':32,'sano':32,'potentia':32,'lux':64,'lucrum':16,'alienis':16},['thaumcraft:focus_primal','thaumcraft:thaumonomicon','minecraft:golden_leggings','minecraft:potion',NS+':focus_smelting',NS+':bright_nitor','minecraft:lava_bucket','minecraft:fire_charge','minecraft:blaze_rod'],'fire_resistance'),
 ('boots',15,10,{'terra':2,'iter':1,'perfodio':1,'herba':1},{'terra':50,'tutamen':32,'perfodio':32,'motus':32,'lux':64,'herba':16,'iter':16},['thaumcraft:thaumonomicon','thaumcraft:focus_primal','minecraft:golden_boots','minecraft:grass_block','thaumcraft:arcane_bore','thaumcraft:growth_lamp','minecraft:wheat_seeds','minecraft:white_wool','minecraft:lead'],None)]
for kind,x,y,research_aspects,costs,extra,potion in armor_rows:
 name='awakened_ichorcloth_'+kind;full=NS+':ICHORCLOTH_'+kind.upper()+'_GEM';kami_keys.append(full);old='ichorcloth'+kind.title()+'Gem'
 mapping[f'assets/{NS}/textures/item/{name}.png']=f'assets/ttinkerer/textures/items/{old}.png';keys[f'item.{NS}.{name}']='item.'+old+'.name'
 write(f'assets/{NS}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name}})
 parts=['minecraft:diamond',NS+':ichor',NS+':ichor']+extra
 utility_recipe(name,'infusion',full,costs=costs,center=NS+':ichorcloth_'+kind,parts=parts,instability=13)
 if potion:
  path=OUT/f'data/{NS}/recipes/{name}.json';recipe=json.loads(path.read_text(encoding='utf-8'));index=parts.index('minecraft:potion');nbt='{Potion:"minecraft:'+potion+'"}'
  recipe['components'][index]['ingredient']={'type':'forge:partial_nbt','item':'minecraft:potion','nbt':nbt};write(f'data/{NS}/recipes/{name}.json',recipe)
  utility_display[-1]['data']['ingredients']['elements'][index]['stack']['nbt']=nbt
 pages=[{'type':'TEXT','text':f'tc.research_page.{full}.0'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]
 if kind=='legs':pages.append({'type':'TEXT','text':f'tc.research_page.{full}.1'})
 catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':x,'displayRow':y,'complexity':3,'aspects':research_aspects,'parents':[NS+':ICHORCLOTH_ARMOR'],'parentsHidden':kami_requirements,'isConcealed':True,'pageCount':len(pages),'icon_item':item(NS+':'+name)})
 pagefile['entries'].append({'key':full,'pages':pages})
 for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.ICHORCLOTH_'+kind.upper()+'_GEM'
 for index in range(2 if kind=='legs' else 1):keys[f'tc.research_page.{full}.{index}']=f'ttresearch.page.ICHORCLOTH_{kind.upper()}_GEM.{index}'
for key in ['ttmisc.awakenedArmor0','ttmisc.awakenedArmor1','ttmisc.toggleArmor','ttmisc.keyCategory']:keys[key]=key
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# KAMI utility items.
for name,old,key,x,y,costs,parent,hidden,instability,aspects,center,parts in [
 ('cat_amulet','catAmulet','CAT_AMULET',13,10,{'cognitio':2,'ordo':1,'tenebrae':1,'mortuus':1},'ICHORIUM',[],8,{'tenebrae':16,'ordo':32,'cognitio':16},'minecraft:quartz_block',[NS+':ichor','minecraft:gold_ingot','minecraft:gold_ingot','minecraft:cocoa_beans','minecraft:jungle_leaves','minecraft:cod']),
 ('protoclay','protoclay','PROTOCLAY',12,17,{'instrumentum':2,'perfodio':1,'humanus':1,'machina':1},'ICHOR_PICK_GEM',['ICHOR_SHOVEL_GEM'],4,{'perfodio':16,'instrumentum':16},'minecraft:clay_ball',['minecraft:dirt','minecraft:stone','minecraft:oak_log',NS+':ender_shard'])]:
 full=NS+':'+key;kami_keys.append(full);mapping[f'assets/{NS}/textures/item/{name}.png']=f'assets/ttinkerer/textures/items/{old}.png';keys[f'item.{NS}.{name}']='item.'+old+'.name'
 write(f'assets/{NS}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name}})
 utility_recipe(name,'infusion',full,costs=aspects,center=center,parts=parts,instability=instability)
 catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':x,'displayRow':y,'complexity':3,'aspects':costs,'parents':[NS+':'+parent],'parentsHidden':list(dict.fromkeys(kami_requirements+[NS+':'+k for k in hidden])),'isConcealed':True,'pageCount':2,'icon_item':item(NS+':'+name)})
 pagefile['entries'].append({'key':full,'pages':[{'type':'TEXT','text':f'tc.research_page.{full}.0'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]})
 for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.'+key
 keys[f'tc.research_page.{full}.0']='ttresearch.page.'+key+'.0'
write('data/curios/tags/items/necklace.json',{'replace':False,'values':[NS+':xp_talisman',NS+':cleansing_talisman',NS+':cat_amulet']})
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# The 117-slot bottomless pouch. Focus selection integration requires a base API.
name='ichor_pouch';full=NS+':ICHOR_POUCH';kami_keys.append(full)
mapping[f'assets/{NS}/textures/item/{name}.png']='assets/ttinkerer/textures/items/ichorPouch.png';mapping[f'assets/{NS}/textures/gui/{name}.png']='assets/ttinkerer/textures/gui/ichorPouch.png';keys[f'item.{NS}.{name}']='item.ichorPouch.name'
write(f'assets/{NS}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name}})
write('data/curios/tags/items/belt.json',{'replace':False,'values':[NS+':ichor_pouch']})
write(f'data/{NS}/tags/items/pouch_blacklist.json',{'replace':False,'values':[NS+':ichor_pouch','thaumcraft:focus_pouch']})
utility_recipe(name,'infusion',full,costs={'vacuos':64,'humanus':32,'pannus':32,'alienis':32,'aer':64},center='thaumcraft:focus_pouch',parts=[NS+':ichor_cloth','thaumcraft:focus_portable_hole','minecraft:diamond',NS+':ichor_cloth','thaumcraft:hungry_chest','thaumcraft:void_jar'],instability=9)
recipe=json.loads((OUT/f'data/{NS}/recipes/{name}.json').read_text(encoding='utf-8'));recipe['center']['ingredient']={'type':NS+':empty_focus_pouch'};write(f'data/{NS}/recipes/{name}.json',recipe)
catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':13,'displayRow':6,'complexity':3,'aspects':{'vacuos':2,'pannus':1,'alienis':1,'humanus':1},'parents':[NS+':ICHOR_CLOTH'],'parentsHidden':kami_requirements,'isConcealed':True,'pageCount':3,'icon_item':item(NS+':'+name)})
pagefile['entries'].append({'key':full,'pages':[{'type':'TEXT','text':f'tc.research_page.{full}.0'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}},{'type':'TEXT','text':'ttmodern.pouch.upgrade_note'}]})
for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.ICHOR_POUCH'
keys[f'tc.research_page.{full}.0']='ttresearch.page.ICHOR_POUCH.0'
for language,note in [('en_us','Empty the focus pouch before infusion. This prevents its contents being lost during the upgrade. The bottomless pouch holds 117 stacks and cannot contain another pouch.'),('zh_cn','注魔前请先清空焦点袋，避免升级时遗失袋内物品。无底袋可存放 117 叠物品，不可套装其他袋子。')]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang['ttmodern.pouch.upgrade_note']=note;write(f'assets/{NS}/lang/{language}.json',lang)
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Block talisman, one bound block type with a bounded integer reserve.
name='block_talisman';full=NS+':BLOCK_TALISMAN';kami_keys.append(full)
for mode in [0,1]:
 mapping[f'assets/{NS}/textures/item/{name}_{mode}.png']=f'assets/ttinkerer/textures/items/blockTalisman{mode}.png'
 write(f'assets/{NS}/models/item/{name}_{mode}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name+'_'+str(mode)}})
write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':item/'+name+'_0','overrides':[{'predicate':{NS+':enabled':1},'model':NS+':item/'+name+'_1'}]})
keys[f'item.{NS}.{name}']='item.blockTalisman.name'
write('data/curios/tags/items/ring.json',{'replace':False,'values':[NS+':block_talisman']})
utility_recipe(name,'infusion',full,costs={'vacuos':65,'tenebrae':32,'praecantatio':50,'alienis':32},center='thaumcraft:focus_portable_hole',parts=[NS+':ichor','minecraft:ender_chest','minecraft:diamond',NS+':ichor','thaumcraft:tainted_goo','thaumcraft:void_jar'],instability=9)
catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':14,'displayRow':17,'complexity':3,'aspects':{'vacuos':2,'tenebrae':1,'alienis':1,'praecantatio':1},'parents':[NS+':ICHOR_PICK_GEM',NS+':ICHOR_SHOVEL_GEM'],'parentsHidden':kami_requirements,'isConcealed':True,'pageCount':2,'icon_item':item(NS+':'+name)})
pagefile['entries'].append({'key':full,'pages':[{'type':'TEXT','text':f'tc.research_page.{full}.0'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]})
for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.BLOCK_TALISMAN'
keys[f'tc.research_page.{full}.0']='ttresearch.page.BLOCK_TALISMAN.0'
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Warp gate and its paired sky pearls; original art remains in the user-provided jar.
name='warp_gate';full=NS+':WARP_GATE';kami_keys.append(full)
for i in range(3):mapping[f'assets/{NS}/textures/block/warp_gate_{i}.png']=f'assets/ttinkerer/textures/blocks/warpGate{i}.png'
mapping[f'assets/{NS}/textures/gui/warp_gate.png']='assets/ttinkerer/textures/gui/warpGate.png'
mapping[f'assets/{NS}/textures/item/sky_pearl.png']='assets/ttinkerer/textures/items/skyPearl.png'
keys[f'block.{NS}.warp_gate']='tile.warpGate.name';keys[f'item.{NS}.sky_pearl']='item.skyPearl.name'
model(name,'cube_bottom_top',{'side':'warp_gate_1','bottom':'warp_gate_1','top':'warp_gate_0'})
write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':block/'+name})
write(f'assets/{NS}/models/item/sky_pearl.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/sky_pearl'}})
write(f'assets/{NS}/blockstates/{name}.json',{'variants':{'':{'model':NS+':block/'+name}}})
write(f'data/{NS}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
path=OUT/'data/minecraft/tags/blocks/mineable/pickaxe.json';tag=json.loads(path.read_text(encoding='utf-8'));tag['values'].append(NS+':'+name);write('data/minecraft/tags/blocks/mineable/pickaxe.json',tag)
utility_recipe(name,'infusion',full,costs={'iter':64,'alienis':50,'volatus':50},center='thaumcraft:travel_paving_stone',parts=[NS+':ichor',NS+':ender_shard',NS+':transvector_dislocator',NS+':nether_shard','minecraft:diamond','minecraft:feather'],instability=8)
utility_recipe('sky_pearl','infusion',full,costs={'iter':32,'alienis':32,'volatus':32,'aer':16},center='minecraft:ender_pearl',parts=[NS+':ichor',NS+':ender_shard','minecraft:lapis_block','minecraft:diamond'],instability=6,result_count=2)
catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':19,'displayRow':6,'complexity':3,'aspects':{'iter':2,'alienis':1,'volatus':1,'machina':1},'parents':[NS+':ICHORCLOTH_CHEST_GEM'],'parentsHidden':list(dict.fromkeys(kami_requirements+[NS+':ICHORCLOTH_BOOTS_GEM'])),'isConcealed':True,'pageCount':4,'icon_item':item(NS+':'+name)})
pagefile['entries'].append({'key':full,'pages':[{'type':'TEXT','text':f'tc.research_page.{full}.0'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':warp_gate'],'class':'modern'}},{'type':'TEXT','text':f'tc.research_page.{full}.1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':sky_pearl'],'class':'modern'}}]})
for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.WARP_GATE'
for index in range(2):keys[f'tc.research_page.{full}.{index}']=f'ttresearch.page.WARP_GATE.{index}'
for language,words in [('en_us',{'locked':'Locked','unlocked':'Unlocked','map_help':'Drag / scroll: map  |  0–9: focus  |  Space: home'}),('zh_cn',{'locked':'已锁定','unlocked':'未锁定','map_help':'拖动/滚轮：地图 | 数字：定位 | 空格：当前位置'})]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang.update({'ttmodern.gate.'+key:value for key,value in words.items()});write(f'assets/{NS}/lang/{language}.json',lang)
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Placement mirror upgrade accepts an empty reserve only.
name='placement_mirror';full=NS+':PLACEMENT_MIRROR';kami_keys.append(full)
for mode in [0,1]:mapping[f'assets/{NS}/textures/item/{name}_{mode}.png']=f'assets/ttinkerer/textures/items/placementMirror{mode}.png'
write(f'assets/{NS}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name+'_0','layer1':NS+':item/'+name+'_1'}})
keys[f'item.{NS}.{name}']='item.placementMirror.name';keys['ttmisc.differentDim']='ttmisc.differentDim'
utility_recipe(name,'infusion',full,costs={'fabrico':65,'vitreus':32,'praecantatio':50,'cognitio':32},center=NS+':block_talisman',parts=[NS+':ichor','minecraft:dropper','minecraft:diamond','minecraft:glass','minecraft:blaze_powder',NS+':ichor'],instability=12)
recipe=json.loads((OUT/f'data/{NS}/recipes/{name}.json').read_text(encoding='utf-8'));recipe['center']['ingredient']={'type':NS+':empty_block_talisman'};write(f'data/{NS}/recipes/{name}.json',recipe)
catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':17,'displayRow':16,'complexity':3,'aspects':{'fabrico':2,'vitreus':1,'alienis':1,'cognitio':1},'parents':[NS+':BLOCK_TALISMAN'],'parentsHidden':kami_requirements,'isConcealed':True,'pageCount':3,'icon_item':item(NS+':'+name)})
pagefile['entries'].append({'key':full,'pages':[{'type':'TEXT','text':f'tc.research_page.{full}.0'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}},{'type':'TEXT','text':'ttmodern.mirror.upgrade_note'}]})
for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.PLACEMENT_MIRROR'
keys[f'tc.research_page.{full}.0']='ttresearch.page.PLACEMENT_MIRROR.0'
for language,note in [('en_us','Empty the block talisman before infusion. Sneak-use a solid block to bind its appearance; sneak-use in air to select 3, 5, 7, 9 or 11 blocks per side. Build using plain blocks from your inventory or matching block talismans, including worn rings. Protected placements are skipped without charge.'),('zh_cn','注魔前请先清空方块护符。潜行右键实心方块绑定，潜行对空气使用切换 3、5、7、9、11 格边长。建造消耗背包中的普通方块或同类方块护符储量，也支持已装备的戒指。受保护的放置会跳过，不消耗材料。')]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang['ttmodern.mirror.upgrade_note']=note;write(f'assets/{NS}/lang/{language}.json',lang)
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# The final three KAMI foci.
for name,old,key,x,y,research_aspects,parent,hidden,costs,center,parts,instability in [
 ('focus_xp_drain','focusXPDrain','FOCUS_XP_DRAIN',12,3,{'cognitio':2,'praecantatio':1,'auram':1,'humanus':1},'ROD_ICHORCLOTH',[],{'cognitio':65,'vitium':16,'praecantatio':50,'auram':32},'minecraft:ender_pearl',[NS+':ichor','minecraft:experience_bottle','minecraft:diamond',NS+':xp_talisman','minecraft:enchanting_table',NS+':ichor'],12),
 ('focus_recall','focusRecall','FOCUS_RECALL',20,8,{'iter':2,'alienis':1,'volatus':1,'praecantatio':1},'WARP_GATE',['ROD_ICHORCLOTH'],{'iter':100,'alienis':64,'praecantatio':50},NS+':sky_pearl',[NS+':ichor',NS+':ichor','minecraft:ender_pearl','minecraft:diamond','thaumcraft:item_mirror','thaumcraft:focus_portable_hole'],10),
 ('focus_shadowbeam','focusShadowbeam','FOCUS_SHADOWBEAM',14,4,{'tenebrae':2,'praecantatio':1,'alienis':1,'vitium':1},'ROD_ICHORCLOTH',[],{'tenebrae':65,'alienis':32,'praecantatio':50,'telum':32},'thaumcraft:focus_shock',[NS+':ichor','minecraft:arrow','minecraft:diamond','thaumcraft:focus_excavation',NS+':focus_deflection',NS+':ichor'],12)]:
 full=NS+':'+key;kami_keys.append(full)
 suffixes=['']+(['_orn'] if key=='FOCUS_SHADOWBEAM' else ['_depth'] if key=='FOCUS_RECALL' else [])
 for suffix in suffixes:
  mapping[f'assets/{NS}/textures/item/{name}{suffix}.png']='assets/ttinkerer/textures/items/'+old+{'':'','_orn':'Orn','_depth':'Depth'}[suffix]+'.png'
  write(f'assets/{NS}/models/item/{name}{suffix}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name+suffix}})
 keys[f'item.{NS}.{name}']='item.'+old+'.name'
 utility_recipe(name,'infusion',full,costs=costs,center=center,parts=parts,instability=instability)
 if key=='FOCUS_XP_DRAIN':
  recipe=json.loads((OUT/f'data/{NS}/recipes/{name}.json').read_text(encoding='utf-8'));recipe['components'][3]['ingredient']={'type':NS+':empty_xp_talisman'};write(f'data/{NS}/recipes/{name}.json',recipe)
 pages=[{'type':'TEXT','text':f'tc.research_page.{full}.0'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]
 if key=='FOCUS_XP_DRAIN':pages.append({'type':'TEXT','text':'ttmodern.xp_focus.upgrade_note'})
 catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':x,'displayRow':y,'complexity':3,'aspects':research_aspects,'parents':[NS+':'+parent],'parentsHidden':list(dict.fromkeys(kami_requirements+[NS+':'+k for k in hidden])),'isConcealed':True,'pageCount':len(pages),'icon_item':item(NS+':'+name)})
 pagefile['entries'].append({'key':full,'pages':pages})
 for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.'+key
 keys[f'tc.research_page.{full}.0']='ttresearch.page.'+key+'.0'
for key in ['ttmisc.experience','ttmisc.noDest','ttmisc.noTeleport']:keys[key]=key
for language,note in [('en_us','Empty the experience talisman before infusion. This prevents stored experience being destroyed by the upgrade. The focus spends actual experience points; a nearly full wand needs proportionally fewer points.'),('zh_cn','注魔前请清空经验护符，避免升级损失储存的经验。焦点消耗实际经验点；法杖接近充满时会按实际补充量减少扣费。')]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang['ttmodern.xp_focus.upgrade_note']=note;write(f'assets/{NS}/lang/{language}.json',lang)
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Bedrock mining world: the original 256 layers, with modern data-driven weighted ores.
write(f'data/{NS}/dimension_type/bedrock.json',{'ultrawarm':False,'natural':False,'coordinate_scale':1.0,'has_skylight':True,'has_ceiling':False,'ambient_light':1.0,'piglin_safe':False,'bed_works':False,'respawn_anchor_works':False,'has_raids':False,'min_y':0,'height':256,'logical_height':256,'infiniburn':'#minecraft:infiniburn_overworld','effects':NS+':bedrock','monster_spawn_block_light_limit':0,'monster_spawn_light_level':0})
write(f'data/{NS}/dimension/bedrock.json',{'type':NS+':bedrock','generator':{'type':'minecraft:flat','settings':{'biome':NS+':bedrock','lakes':False,'features':True,'layers':[{'height':256,'block':'minecraft:bedrock'}],'structure_overrides':[]}}})
features=[[] for i in range(11)];features[6]=[NS+':bedrock_ores']
write(f'data/{NS}/worldgen/biome/bedrock.json',{'has_precipitation':True,'temperature':.8,'downfall':.4,'effects':{'fog_color':1579032,'water_color':4159204,'water_fog_color':329011,'sky_color':7907327},'spawners':{'monster':[{'type':'minecraft:'+mob,'weight':weight,'minCount':minimum,'maxCount':maximum} for mob,weight,minimum,maximum in [('spider',100,4,4),('zombie',95,4,4),('zombie_villager',5,1,1),('skeleton',100,4,4),('creeper',100,4,4),('slime',100,4,4),('enderman',10,1,4),('witch',5,1,1)]],'ambient':[{'type':'minecraft:bat','weight':10,'minCount':8,'maxCount':8}],'water_creature':[{'type':'minecraft:squid','weight':10,'minCount':1,'maxCount':4}]},'spawn_costs':{},'carvers':{},'features':features})
weights={'aluminum':617,'amber':161,'apatite':269,'topaz':238,'silicon':234,'ilmenite':270,'cinnabar':172,'coal':2648,'copper':603,'diamond':67,'emerald':48,'tanzanite':61,'gold':164,'infused_air':94,'infused_earth':35,'infused_entropy':53,'infused_fire':42,'infused_order':31,'infused_water':27,'iron':1503,'lapis':57,'lead':335,'peridot':79,'redstone':364,'ruby':57,'sapphire':70,'malachite':416,'desh':105,'tin':507,'uranium':112,'quartz':809,'charged_certus_quartz':117,'cheese':1024}
write(f'data/{NS}/worldgen/configured_feature/bedrock_ores.json',{'type':NS+':bedrock_ores','config':{'ores':[{'tag':'forge:ores/'+key,'weight':weight} for key,weight in weights.items()]}})
write(f'data/{NS}/worldgen/placed_feature/bedrock_ores.json',{'feature':NS+':bedrock_ores','placement':[]})
mapping[f'assets/{NS}/textures/block/bedrock_portal.png']='assets/ttinkerer/textures/blocks/portal.png'
mapping[f'assets/{NS}/textures/block/bedrock_portal.png.mcmeta']='assets/ttinkerer/textures/blocks/portal.png.mcmeta'
write(f'assets/{NS}/blockstates/bedrock_portal.json',{'variants':{'':{'model':NS+':block/bedrock_portal'}}})
write(f'assets/{NS}/models/block/bedrock_portal.json',{'parent':'minecraft:block/cube_all','render_type':'minecraft:translucent','textures':{'all':NS+':block/bedrock_portal'}})
manifest['mapping']=mapping
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Optional computer peripheral foundation and aspect analyzer.
full=NS+':PERIPHERALS'
catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':-1,'displayRow':0,'complexity':0,'aspects':{},'isAutoUnlock':True,'isRound':True,'pageCount':2,'icon_item':item('minecraft:redstone')})
pagefile['entries'].append({'key':full,'pages':[{'type':'TEXT','text':f'tc.research_page.{full}.0'},{'type':'TEXT','text':'ttmodern.peripherals.analyzer'}]})
for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.PERIPHERALS'
keys[f'tc.research_page.{full}.0']='ttresearch.page.PERIPHERALS.0'
name='aspect_analyzer';full=NS+':ASPECT_ANALYZER'
for i in range(5):mapping[f'assets/{NS}/textures/block/{name}_{i}.png']=f'assets/ttinkerer/textures/blocks/aspectAnalyzer{i}.png'
mapping[f'assets/{NS}/textures/gui/{name}.png']='assets/ttinkerer/textures/gui/aspectAnalyzer.png'
write(f'assets/{NS}/models/block/{name}.json',{'parent':'minecraft:block/cube','textures':{face:NS+':block/'+name+'_'+str(i) for face,i in [('down',0),('up',0),('north',1),('south',2),('west',3),('east',4),('particle',0)]}})
write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':block/'+name})
write(f'assets/{NS}/blockstates/{name}.json',{'variants':{'':{'model':NS+':block/'+name}}})
write(f'data/{NS}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
path=OUT/'data/minecraft/tags/blocks/mineable/axe.json';tag=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'replace':False,'values':[]}
if NS+':'+name not in tag['values']:tag['values'].append(NS+':'+name)
write('data/minecraft/tags/blocks/mineable/axe.json',tag)
utility_recipe(name,'arcane',full,['TWT','WMW','TWT'],{'T':'thaumcraft:thaumium_ingot','W':'thaumcraft:greatwood_planks','M':'thaumcraft:thaumometer'},{'ordo':1,'perditio':1})
path=OUT/f'data/{NS}/recipes/{name}.json';recipe=json.loads(path.read_text(encoding='utf-8'));recipe['conditions']=[{'type':'forge:mod_loaded','modid':'computercraft'}];write(f'data/{NS}/recipes/{name}.json',recipe)
catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':0,'displayRow':1,'complexity':2,'aspects':{'machina':2,'sensus':1,'cognitio':1},'parents':[NS+':PERIPHERALS'],'parentsHidden':['GOGGLES','THAUMIUM'],'isConcealed':True,'isRound':True,'pageCount':2,'icon_item':item(NS+':'+name)})
pagefile['entries'].append({'key':full,'pages':[{'type':'TEXT','text':f'tc.research_page.{full}.0'},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}}]})
keys[f'block.{NS}.{name}']='tile.aspectAnalyzer.name'
for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.ASPECT_ANALYZER'
keys[f'tc.research_page.{full}.0']='ttresearch.page.ASPECT_ANALYZER.0'
for language,note in [('en_us','CC: Tweaked recognizes the analyzer as tt_aspectanalyzer. Its methods are hasItem(), itemHasAspects(), getAspects() and getAspectCount(). The aspect list uses indices starting at 1; the count table maps aspect tags to quantities per item. Reading never consumes the sample. Its GUI shows aspects only after you have scanned the item.'),('zh_cn','CC: Tweaked 将分析仪识别为 tt_aspectanalyzer。可调用 hasItem()、itemHasAspects()、getAspects() 和 getAspectCount()。要素列表从 1 开始编号，数量表以要素 ID 为键，返回单件物品的要素数量。读取不会消耗样品；界面仅显示已经扫描过的物品要素。')]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang['ttmodern.peripherals.analyzer']=note;write(f'assets/{NS}/lang/{language}.json',lang)
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Golem connector, using the frozen base's public state and marker API.
name='golem_connector';full=NS+':GOLEM_CONNECTOR'
mapping[f'assets/{NS}/textures/block/{name}.png']='assets/ttinkerer/textures/blocks/golemConnector.png'
write(f'assets/{NS}/models/block/{name}.json',{'parent':'minecraft:block/cube_all','textures':{'all':NS+':block/'+name}})
write(f'assets/{NS}/models/item/{name}.json',{'parent':NS+':block/'+name})
write(f'assets/{NS}/blockstates/{name}.json',{'variants':{'camouflaged=false':{'model':NS+':block/'+name},'camouflaged=true':{'model':NS+':block/interface_camouflaged'}}})
write(f'data/{NS}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':NS+':'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
path=OUT/'data/minecraft/tags/blocks/mineable/axe.json';tag=json.loads(path.read_text(encoding='utf-8'))
if NS+':'+name not in tag['values']:tag['values'].append(NS+':'+name)
write('data/minecraft/tags/blocks/mineable/axe.json',tag)
utility_recipe(name,'arcane',full,['WFW','sIs','WFW'],{'W':'thaumcraft:greatwood_log','F':'minecraft:redstone_block','s':'minecraft:ender_pearl','I':'thaumcraft:golem_bell'},{'aer':20,'ordo':5,'perditio':15})
path=OUT/f'data/{NS}/recipes/{name}.json';recipe=json.loads(path.read_text(encoding='utf-8'));recipe['conditions']=[{'type':'forge:mod_loaded','modid':'computercraft'}];write(f'data/{NS}/recipes/{name}.json',recipe)
pages=[{'type':'TEXT','text':f'tc.research_page.{full}.0'},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','keys':[NS+':'+name],'class':'modern'}},{'type':'TEXT','text':f'tc.research_page.{full}.1'},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','keys':[NS+':connector'],'class':'modern'}},{'type':'TEXT','text':f'tc.research_page.{full}.2'},{'type':'TEXT','text':'ttmodern.peripherals.golem'}]
catalog['categories'][0]['research'].append({'key':full,'category':'TT_CATEGORY','displayColumn':1,'displayRow':0,'complexity':0,'aspects':{'ordo':1,'iter':2,'instrumentum':1},'parents':[NS+':PERIPHERALS'],'parentsHidden':['GOLEMBELL'],'isConcealed':True,'isRound':True,'pageCount':len(pages),'icon_item':item(NS+':'+name)})
pagefile['entries'].append({'key':full,'pages':pages})
keys[f'block.{NS}.{name}']='tile.golemConnector.name'
for field,legacy in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{full}']='ttresearch.'+legacy+'.GOLEM_CONNECTOR'
for page in range(3):keys[f'tc.research_page.{full}.{page}']=f'ttresearch.page.GOLEM_CONNECTOR.{page}'
for language,note,messages in [
 ('en_us','Use the connector tool on this block, then sneak-use it on your golem. The golem must stay within 30 blocks along each axis. CC: Tweaked peripheral type: tt_golemconnector. Home and marker changes are immediate. Marker indices start at 1 and dim is a dimension resource ID such as minecraft:overworld. All replacements are validated before changes are applied.',{'notinterf':'Select a golem connector first.','notpresent':'The selected connector is unavailable.','complete':'Golem connected.','toofar':'Cannot bind: golem is outside the 30-block range, belongs to someone else, or the position is protected.'}),
 ('zh_cn','用连接工具右键此方块，再潜行右键自己的傀儡。傀儡须处于每个坐标轴 30 格范围内。CC: Tweaked 外设类型为 tt_golemconnector。家位置和标记的更改即时生效；标记编号统一从 1 开始，dim 使用 minecraft:overworld 等维度资源 ID。批量替换先验证所有数据，失败不会修改原有标记。',{'notinterf':'请先选择傀儡连接器。','notpresent':'选定的连接器已不可用。','complete':'已连接傀儡。','toofar':'无法绑定：傀儡超出 30 格范围、属于其他玩家，或位置受保护。'})]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang['ttmodern.peripherals.golem']=note
 lang.update({'ttmisc.golemconnector.'+key:value for key,value in messages.items()});write(f'assets/{NS}/lang/{language}.json',lang)
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)
write(f'data/{NS}/research_page_recipes/main.json',{'format':1,'entries':display+focus_display+magnet_display+[tablet_display]+late_display+new_display+talisman_display+soul_display+utility_display+fire_display+crop_display})
manifest['mapping']=mapping
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Stable registration with data-driven creative visibility when KAMI is disabled.
kami_items={NS+':'+name for name in kami_materials}
for recipe_path in (OUT/f'data/{NS}/recipes').glob('*.json'):
 recipe=json.loads(recipe_path.read_text(encoding='utf-8'))
 if recipe.get('research') in kami_keys:
  result=recipe.get('result',{})
  if isinstance(result,dict) and isinstance(result.get('item'),str) and result['item'].startswith(NS+':'):kami_items.add(result['item'])
write(f'data/{NS}/tags/items/kami.json',{'replace':False,'values':sorted(kami_items)})

# Modern recipe-viewer explanations are original text, not bundled legacy images.
for language,words in [
 ('en_us',{'summoned_entity':'Living creature (not a spawn egg)','summoning_rules':'3 pedestals; consumes one each. All infused: reusable, 60 s cooldown.','extraction':'Soul extraction','extraction_rules':'Kill with the activated cursed blade. Replaces ordinary loot with these souls.'}),
 ('zh_cn',{'summoned_entity':'生物本体（不是刷怪蛋）','summoning_rules':'三个祭坛各放一份。普通组合会消耗；全为注魔精魂时可重复使用，冷却 60 秒。','extraction':'精魂提取','extraction_rules':'用已激活的诅咒之刃击杀，以这些精魂替换普通掉落。'})]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang.update({'ttmodern.jei.'+key:value for key,value in words.items()});write(f'assets/{NS}/lang/{language}.json',lang)

for language,words in [
 ('en_us',{'crop_drops':'Infused crop harvest','crop_chance':'Per roll: %s%%','crop_rules':'Mature crop; base harvest roll. Ordo traits can add rolls. Seeds are separate.','crop_unknown':'This data pack uses conditional or complex loot. Exact preview unavailable.'}),
 ('zh_cn',{'crop_drops':'注魔作物收获','crop_chance':'每次抽取概率：%s%%','crop_rules':'成熟作物的基础抽取；秩序性状可增加抽取次数。种子另行掉落。','crop_unknown':'数据包使用条件或复杂掉落规则，无法精确预览。'})]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang.update({'ttmodern.jei.'+key:value for key,value in words.items()});write(f'assets/{NS}/lang/{language}.json',lang)

for language,words in [
 ('en_us',{'reset':'Focus reset to your vanilla ender chest.','bound':'Focus bound to EnderStorage frequency.','unavailable':'Bound EnderStorage is unavailable. Sneak-use obsidian to reset the focus.'}),
 ('zh_cn',{'reset':'焦点已恢复为自己的原版末影箱。','bound':'焦点已绑定 EnderStorage 频率。','unavailable':'绑定的 EnderStorage 不可用。潜行对黑曜石施法可恢复原版末影箱。'})]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang.update({'ttmodern.enderfocus.'+key:value for key,value in words.items()});write(f'assets/{NS}/lang/{language}.json',lang)

# Modern Botania magic missiles are destroyed after deflection, as in the legacy addon.
write(f"data/{NS}/tags/entity_types/deflection_discard.json",{"replace":False,"values":[{"id":"botania:magic_missile","required":False}]})

write(f"data/{NS}/computercraft/turtle_upgrades/fume.json",{"type":NS+":fume"})

for language,name in [("en_us","Fume-dissipating"),("zh_cn","烟雾消散")]:
 path=OUT/f"assets/{NS}/lang/{language}.json";lang=json.loads(path.read_text(encoding="utf-8"));lang["ttcomputer.dissipator"]=name;write(f"assets/{NS}/lang/{language}.json",lang)

# Data-pack replacement for the old ComputerCraft class-name exclusion list.
write("data/thaumic_tinkerer/tags/blocks/peripheral_disabled.json", {"replace": False, "values": []})

# Waila's machine status through modern Jade; localized legacy strings remain external.
for key in ['nothing','currentTool','leftClick','rightClick','redstone','autonomous','connected','repairing','finishedRepairing','allowIncoming','disallowIncoming','pullingMode','pushingMode','pullingAdult','pullingChild','pushingAdult','pushingChild','pullingAdultType','pullingChildType','pushingAdultType','pushingChildType']:
 keys['ttwaila.'+key]='ttwaila.'+key
for language in manifest['languages']:manifest['languages'][language]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)
for language,label in [('en_us','Thaumic Tinkerer machine status'),('zh_cn','神秘工匠机器状态')]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang['config.jade.plugin_thaumic_tinkerer.machine_status']=label;write(f'assets/{NS}/lang/{language}.json',lang)

# Optional Jade provider; all runtime state sync is performed by Jade's public API.
mods_path=OUT/'META-INF/mods.toml'
mods_path.write_text(mods_path.read_text(encoding='utf-8')+'\n[[dependencies.thaumic_tinkerer]]\nmodId="jade"\nmandatory=false\nversionRange="[11.13.1,12)"\nordering="AFTER"\nside="BOTH"\n',encoding='utf-8')

# GregTech Modern's public PPE tag replaces the legacy optional IHazardProtector interface.
write('data/gtceu/tags/items/ppe_armor.json',{'replace':False,'values':[NS+':awakened_ichorcloth_'+part for part in ['helm','chest','legs','boots']]})

# Restorer's active/off rune overlay is supplied by the player's legacy archive.
for name,legacy in [('repair','repair'),('repair_off','repairOff')]:mapping[f'assets/{NS}/textures/misc/{name}.png']=f'assets/ttinkerer/textures/misc/{legacy}.png'
manifest['mapping']=mapping
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# Also eligible for a pack-provided dedicated pouch Curios slot, as with Baubles Expanded.
write('data/curios/tags/items/focus_pouch.json',{'replace':False,'values':[NS+':ichor_pouch']})

# GTNH renamed the research ID while the player's supported legacy resource jar retains LEVITATOR.
for field,old in [('research_name','name'),('research_text','lore')]:keys[f'tc.{field}.{NS}:LEVITATOR_LOCOMOTIVE']='ttresearch.'+old+'.LEVITATOR'
keys[f'tc.research_page.{NS}:LEVITATOR_LOCOMOTIVE.0']='ttresearch.page.LEVITATOR.0'
# These three old blocks did not ship matching language entries; provide new descriptive names.
for name in ['travel_slab','ward_slab','travel_stairs']:keys.pop(f'block.{NS}.{name}',None)
for language,names in [('en_us',{'travel_slab':'Paving Stone of Travel Slab','ward_slab':'Paving Stone of Warding Slab','travel_stairs':'Paving Stone of Travel Stairs'}),('zh_cn',{'travel_slab':'旅行之石台阶','ward_slab':'守卫之石台阶','travel_stairs':'旅行之石楼梯'})]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang.update({f'block.{NS}.{name}':text for name,text in names.items()});write(f'assets/{NS}/lang/{language}.json',lang)
for target in manifest['languages']:manifest['languages'][target]=keys
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)

# 0921_2 original-author category and wand localization; universal Curios compatibility.
mapping[f'assets/{NS}/textures/misc/r_enchanting.png']='assets/ttinkerer/textures/misc/r_enchanting.png'
keys[f'item.Wand.{NS}:ichor.cap']='item.Wand.ICHOR.cap'
keys[f'item.Wand.{NS}:ichorcloth.rod']='item.Wand.ICHORCLOTH.rod'
for target in manifest['languages']:manifest['languages'][target]=keys
manifest['mapping']=mapping
write(f'data/{NS}/thaumcraft/original_resources.json',manifest)
write('data/curios/tags/items/curio.json',{'replace':False,'values':[NS+':bright_nitor',NS+':protoclay']})
for language,title in [('en_us','Thaumic Tinkerer'),('zh_cn','神秘工匠')]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang['itemGroup.thaumic_tinkerer']=title;write(f'assets/{NS}/lang/{language}.json',lang)

for language, messages in [('en_us', {'select':'Select an enchantment first.', 'pillars':'Requires six pillars: 2–12 obsidian totems starting at altar height, topped with Nitor.', 'wand':'Insert a wand to supply vis.'}), ('zh_cn', {'select':'请先选择附魔。', 'pillars':'需要六根柱子：与附魔台同一高度起，2–12 块黑曜石图腾，上放闪耀之光。', 'wand':'请放入法杖以提供魔力。'})]:
 path=OUT/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'));lang.update({f'ttmodern.enchanter.{key}':value for key,value in messages.items()});write(f'assets/{NS}/lang/{language}.json',lang)

# Modern infusion alternative for every Tinkerer enchantment.
enchant_materials={
 'ascent_boost':['minecraft:rabbit_foot','minecraft:feather'], 'slow_fall':['minecraft:phantom_membrane','minecraft:feather'],
 'flaming_touch':['minecraft:blaze_rod','minecraft:iron_pickaxe'], 'disintegrate':['minecraft:flint','minecraft:obsidian'],
 'quick_draw':['minecraft:bow','minecraft:sugar'], 'vampirism':['minecraft:fermented_spider_eye','minecraft:ghast_tear'],
 'focused_strike':['minecraft:iron_sword','minecraft:quartz'], 'dispersed_strike':['minecraft:iron_sword','minecraft:slime_ball'],
 'final_strike':['minecraft:iron_sword','minecraft:ender_pearl'], 'valiance':['minecraft:iron_sword','minecraft:golden_apple'],
 'shatter':['minecraft:diamond_pickaxe','minecraft:flint'], 'tunnel':['minecraft:iron_pickaxe','minecraft:piston'],
 'pounce':['minecraft:rabbit_foot','minecraft:iron_leggings'], 'shockwave':['minecraft:tnt','minecraft:iron_boots']}
catalog=json.loads((OUT/f'data/{NS}/research_catalog/main.json').read_text(encoding='utf-8'))
pagefile=json.loads((OUT/f'data/{NS}/research_pages/main.json').read_text(encoding='utf-8'))
enchants=json.loads((OUT/f'data/{NS}/osmotic_enchantments/main.json').read_text(encoding='utf-8'))['entries']
for entry in enchants:
 if not entry['enchantment'].startswith(NS+':'):continue
 name=entry['enchantment'].split(':')[1];key=entry['research'];rid=NS+':enchantment/'+name
 costs={a:max(4,n//2) for a,n in entry['aspects'].items()};costs['praecantatio']=12
 write(f'data/{NS}/recipes/enchantment/{name}.json',{'type':'thaumcraft:infusion','research':key,'enchantment':entry['enchantment'],'instability':4,'base_xp':5,'aspects':costs,'components':[{'ingredient':{'item':item}} for item in enchant_materials[name]+['thaumcraft:salis_mundus']]})
 page=next(row for row in pagefile['entries'] if row['key']==key)
 if not any(rid in p.get('recipe',{}).get('keys',[]) for p in page['pages']):page['pages'].append({'type':'INFUSION_ENCHANTMENT','recipe':{'kind':'reference','class':'modern','keys':[rid]}})
 for category in catalog['categories']:
  for research in category['research']:
   if research['key']==key:research['pageCount']=len(page['pages'])
write(f'data/{NS}/research_catalog/main.json',catalog);write(f'data/{NS}/research_pages/main.json',pagefile)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])

# These two paragraphs discuss the Soul Aspect item, not the Spiritus aspect.
# Keep the original translated prose without the legacy automatic glossary.
_summon_path=OUT/f'data/{NS}/research_pages/main.json'
_summon_pages=json.loads(_summon_path.read_text(encoding='utf-8'))
for _entry in _summon_pages['entries']:
 if _entry['key']==NS+':SUMMON':
  for _page in _entry['pages']:
   if _page.get('text',{}).get('translate') in {f'tc.research_page.{NS}:SUMMON.0',f'tc.research_page.{NS}:SUMMON.1'}:
    _page['import_legacy']=False
write(f'data/{NS}/research_pages/main.json',_summon_pages)
