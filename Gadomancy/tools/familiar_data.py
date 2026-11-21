"""Original 1.0.7.3 familiar recipes; GTNH's dynamic aspect binding without its pack recipe changes."""
from pathlib import Path
import json,re
repo=Path(__file__).resolve().parents[3];root=Path(__file__).resolve().parents[1]/'src/main/resources'
source=(repo/'Addons/references/phase2/Gadomancy/original-src/makeo/gadomancy/common/registration/RegisteredRecipes.java').read_text(encoding='utf8')
def write(path,value):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def item(id):return {'item':id}
aspects={a['tag']:a for a in json.loads((repo/'TC4/src/main/resources/data/thaumcraft/aspect_registry.json').read_text(encoding='utf8'))}
tags=dict(MOTION='motus',AIR='aer',AURA='auram',WEAPON='telum',FIRE='ignis',ENERGY='potentia',WATER='aqua',POISON='venenum',ELDRITCH='alienis',DEATH='mortuus',ENTROPY='perditio',TRAP='vinculum')
items=dict(wispyEssence='thaumcraft:wisp_essence',alumentum='thaumcraft:alumentum',amber='thaumcraft:amber',glowstone='minecraft:glowstone_dust',ghastTear='minecraft:ghast_tear',netherStar='minecraft:nether_star',airShard='thaumcraft:air_shard',quicksilver='thaumcraft:quicksilver',blazePowder='minecraft:blaze_powder',redstone='minecraft:redstone',magmaCream='minecraft:magma_cream',coalBlock='minecraft:coal_block',nitor='thaumcraft:nitor',fireShard='thaumcraft:fire_shard',spiderEye='minecraft:spider_eye',rottenFlesh='minecraft:rotten_flesh',fermentedSpiderEye='minecraft:fermented_spider_eye',enderPearl='minecraft:ender_pearl',sugarCane='minecraft:sugar_cane',pumpkinSeeds='minecraft:pumpkin_seeds',netherWarts='minecraft:nether_wart',deadBush='minecraft:dead_bush',string='minecraft:string',witherSkull='minecraft:wither_skeleton_skull',entropyShard='thaumcraft:entropy_shard',enderEye='minecraft:ender_eye',balancedShard='thaumcraft:amber',gunpowder='minecraft:gunpowder',primalCharm='thaumcraft:primal_charm',voidSeed='thaumcraft:void_seed',amberBlock='thaumcraft:amber_block',voidSword='thaumcraft:void_sword',ordoShard='thaumcraft:order_shard',elementalSword='thaumcraft:elemental_sword',primordialPearl='thaumcraft:primordial_pearl',salisMundis='thaumcraft:salis_mundus')
# 'balancedShard' in author source is resource metadata 6 (amber), NOT metadata 18; preserve that recipe.
recipes={};grouped={}
creation=dict(type='gadomancy:familiar_infusion',familiar_aspect='*',aspect_cost=10,research='gadomancy:ethereal_familiar',instability=4,aspects={'auram':12,'praecantatio':18},center={'ingredient':item('thaumcraft:runic_amulet')},components=[{'ingredient':item(i)} for i in ['thaumcraft:wisp_essence','thaumcraft:salis_mundus','thaumcraft:nitor']*2],result=item('gadomancy:ethereal_familiar'))
recipes['familiar_create']=creation;grouped['ethereal_familiar']=['familiar_create']
pattern=r'new EtherealFamiliarUpgradeRecipe\(modid \+ "\.(FAMILIAR_\w+)", (\d+), new AspectList\(\)(.*?), famIn, FamiliarAugment\.(\w+), (\d+), (.*?)\);'
rows=re.findall(pattern,source);assert len(rows)==19,len(rows)
for research,inst,costs,augment,prev,components in rows:
 augment={'DAMAGE_INCREASE':'damage','RANGE_INCREASE':'range','ATTACK_SPEED':'speed'}.get(augment,augment.lower());name=f'familiar_{augment}_{int(prev)+1}';research=research.lower();cost={};extra=0
 for key,qty in re.findall(r'\.add\((a|Aspect\.\w+), (\d+)\)',costs):
  if key=='a':extra=int(qty)
  else:cost[tags[key[7:]]]=cost.get(tags[key[7:]],0)+int(qty)
 recipes[name]=dict(type='gadomancy:familiar_infusion',familiar_aspect='*',aspect_cost=extra,augment=augment,previous_level=int(prev),research='gadomancy:'+research,instability=int(inst),aspects=cost,center={'ingredient':item('gadomancy:ethereal_familiar')},components=[{'ingredient':item(items[v.strip()])} for v in components.split(',')],result=item('gadomancy:ethereal_familiar'))
 grouped.setdefault(research,[]).append(name)
for name,recipe in recipes.items():write('data/gadomancy/recipes/'+name+'.json',recipe)
write('assets/gadomancy/models/item/ethereal_familiar.json',{'parent':'builtin/entity','textures':{'particle':'gadomancy:items/category_icon'}})
write('data/curios/tags/items/necklace.json',{'replace':False,'values':['gadomancy:ethereal_familiar']})
research=[('ethereal_familiar',5,1,{'praecantatio':8,'ignis':6,'auram':4},['RUNICARMOR'],None),('familiar_shock',4,-1,{'tempestas':4,'telum':4,'aer':8},['gadomancy:ethereal_familiar'],'lightning'),('familiar_fire',6,-1,{'ignis':8,'telum':4,'praecantatio':4},['gadomancy:ethereal_familiar'],'fire'),('familiar_poison',3,2,{'venenum':8,'telum':4,'perditio':4},['gadomancy:ethereal_familiar'],'poison'),('familiar_weakness',2,0,{'vinculum':8,'telum':8,'vitium':6},['gadomancy:ethereal_familiar'],'enervation'),('familiar_damage',8,2,{'telum':8,'potentia':8,'praecantatio':4},['gadomancy:ethereal_familiar','VOIDMETAL'],'damage'),('familiar_range',6,4,{'praecantatio':8,'alienis':8,'ordo':4},['gadomancy:ethereal_familiar'],'range'),('familiar_speed',8,5,{'motus':10,'praecantatio':4,'auram':4},['gadomancy:familiar_damage','gadomancy:familiar_range','PRIMPEARL'],'speed')]
catalog=[];pages=[];display=[];aliases={'item.gadomancy.ethereal_familiar':'item.ItemEtherealFamiliar.name'}
def stack(v):
 if isinstance(v,list):return dict(kind='sequence',elements=[stack(e) for e in v])
 if 'ingredient' in v:v=dict(v['ingredient'],**({'nbt':v['nbt']} if 'nbt' in v else {}))
 return dict(kind='stack',stack=dict(v,count=v.get('count',1)))
for id,x,y,cost,parents,texture in research:
 key='gadomancy:'+id;keys=[]
 for name in grouped[id]:
  alternatives=[]
  for a in aspects:
   r=json.loads(json.dumps(recipes[name]));r['aspects'][a]=r['aspects'].get(a,0)+r['aspect_cost'];r['center']['nbt']='{aspect:"'+a+'"}' if id!='ethereal_familiar' else '{}'
   for comp in r['components']:
    if comp['ingredient']['item']=='thaumcraft:wisp_essence':comp['nbt']='{Aspects:[{key:"'+a+'",amount:2}]}'
   nbt='{aspect:"'+a+'"';prior=r.get('previous_level',0)
   if prior:r['center']['nbt']='{aspect:"'+a+'",augments:{'+r['augment']+':'+str(prior)+'}}'
   if 'augment' in r:nbt+=',augments:{'+r['augment']+':'+str(prior+1)+'}'
   r['result']['nbt']=nbt+'}';recipekey='gadomancy:'+name+'/'+a;alternatives.append(recipekey)
   display.append(dict(key=recipekey,**{'class':'modern'},data=dict(kind='infusion',research=key,aspects=r['aspects'],instability=r['instability'],input=stack(r['center']),output=stack(r['result']),ingredients=stack(r['components']))))
  keys.append(dict(type='INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=alternatives)))
 pageList=[dict(type='TEXT',text='gadomancy.research_page.'+id.upper()+'.1')]+keys
 if id=='ethereal_familiar':pageList.append(dict(type='TEXT',text='gadomancy.research_page.ETHEREAL_FAMILIAR.3'))
 r=dict(key=key,category='GADOMANCY',displayColumn=x,displayRow=y,pageCount=len(pageList),complexity=2,aspects=cost,parents=parents,isConcealed=True)
 if texture:r['icon_resource']='gadomancy:textures/misc/fam_'+texture+'.png'
 else:r.update(isSpecial=True,icon_item=dict(item='gadomancy:ethereal_familiar',nbt='{aspect:"praecantatio"}'))
 if id in ['familiar_damage','familiar_range']:r['parentsAny']=['gadomancy:familiar_'+element for element in ['fire','shock','poison','weakness']]
 catalog.append(r);pages.append(dict(key=key,pages=pageList))
 for typ in ['name','text']:aliases[f'tc.research_{typ}.{key}']=f'tc.research_{typ}.GADOMANCY.{id.upper()}'
write('data/gadomancy/research_catalog/familiar.json',dict(format=1,categories=[dict(key='GADOMANCY',icon='gadomancy:textures/items/category_icon.png',background='gadomancy:textures/gui/gui_researchback.png',research=catalog)]))
write('data/gadomancy/research_pages/familiar.json',dict(format=1,entries=pages))
write('data/gadomancy/research_page_recipes/familiar.json',dict(format=1,entries=display))
path=root/'data/gadomancy/thaumcraft/original_resources.json';mapping=json.loads(path.read_text(encoding='utf8'))
for lang in ['en_us','zh_cn']:mapping['languages']['assets/gadomancy/lang/'+lang+'.json'].update(aliases)
write('data/gadomancy/thaumcraft/original_resources.json',mapping)
print(f'{len(recipes)} templates, {len(display)} aspect variants, {len(catalog)} research entries')

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
