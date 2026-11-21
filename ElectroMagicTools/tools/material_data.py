"""All original material processing and UU infusion recipes; no legacy binaries are copied."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,value):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def item(name,count=1):return {'item':name,'count':count}
def ingredient(name):return {'item':name}
def stack(value):
 if isinstance(value,list):return dict(kind='sequence',elements=[stack(v) for v in value])
 value=value.get('ingredient',value)
 if 'tag' in value:return dict(kind='ore',names=[value['tag']],elements=[])
 return dict(kind='stack',stack=dict(value,count=value.get('count',1)))
rows=[('uranium_cluster','clusteruranium','oreClusterUranium'),('crushed_amber','crushedamber','crushedOreAmber'),('purified_amber','purifiedamber','purifiedOreAmber'),('crushed_cinnabar','crushedcinnabar','crushedOreCinnabar'),('purified_cinnabar','purifiedcinnabar','purifiedOreCinnabar'),('lightning_summoner','lightningsummoner','lightningSummoner'),('uu_matter_drop','uumatterdrop','uumatterDrop')]
aliases={}
for id,texture,key in rows:
 write('assets/emt/models/item/'+id+'.json',dict(parent='minecraft:item/generated',textures={'layer0':'emt:items/materials/'+texture}))
 aliases['item.emt.'+id]='item.EMT.'+key+('' if id=='uranium_cluster' else '.name')
write('assets/emt/blockstates/amber_bricks.json',{'variants':{'':{'model':'emt:block/amber_bricks'}}})
write('assets/emt/models/block/amber_bricks.json',dict(parent='minecraft:block/cube_all',render_type='minecraft:translucent',textures={'all':'emt:blocks/amber_bricks'}))
write('assets/emt/models/item/amber_bricks.json',dict(parent='emt:block/amber_bricks'))
write('data/emt/loot_tables/blocks/amber_bricks.json',dict(type='minecraft:block',pools=[dict(rolls=1,entries=[dict(type='minecraft:item',name='emt:amber_bricks')],conditions=[{'condition':'minecraft:survives_explosion'}])]))
path=root/'data/minecraft/tags/blocks/mineable/pickaxe.json';pickaxe=json.loads(path.read_text(encoding='utf8')) if path.exists() else dict(replace=False,values=[])
if 'emt:amber_bricks' not in pickaxe['values']:pickaxe['values'].append('emt:amber_bricks')
write('data/minecraft/tags/blocks/mineable/pickaxe.json',pickaxe)
write('data/emt/recipes/amber_bricks.json',dict(type='minecraft:crafting_shaped',pattern=['##','##'],key={'#':ingredient('thaumcraft:amber_block')},result=item('emt:amber_bricks',4)))
write('data/emt/recipes/amber_bricks_unpack.json',dict(type='minecraft:crafting_shapeless',ingredients=[ingredient('emt:amber_bricks')],result=item('thaumcraft:amber',4)))
# Author's ore-washing typo gives cinnabar ONE small tin dust, while amber gets TWO copper dust.
for ore,byproduct,count,result in [('amber','copper',2,'amber'),('cinnabar','tin',1,'quicksilver')]:
 write('data/emt/recipes/macerate_'+ore+'.json',dict(type='ic2:macerator',ingredient=ingredient('thaumcraft:'+ore+'_ore'),result=item('emt:crushed_'+ore,2)))
 write('data/emt/recipes/wash_'+ore+'.json',dict(type='ic2:ore_washer',ingredient=ingredient('emt:crushed_'+ore),amount=1000,result=[item('emt:purified_'+ore),item('ic2:small_'+byproduct+'_dust',count),item('ic2:stone_dust')]))
 write('data/emt/recipes/centrifuge_'+ore+'.json',dict(type='ic2:centrifuge',ingredient=ingredient('emt:purified_'+ore),minHeat=1000,result=[item('ic2:small_'+byproduct+'_dust'),item('thaumcraft:'+result)]))
 for stage in ['crushed','purified']:
  write('data/emt/recipes/smelt_'+stage+'_'+ore+'.json',dict(type='minecraft:smelting',ingredient=ingredient('emt:'+stage+'_'+ore),result='thaumcraft:'+result,experience=0,cookingtime=200))
for metal in ['iron','gold','copper','tin','silver','lead']:
 write('data/emt/recipes/macerate_'+metal+'_cluster.json',dict(type='ic2:macerator',ingredient={'tag':'forge:clusters/'+metal},result=item('ic2:small_'+metal+'_dust',22)))
write('data/emt/recipes/roll_thaumium_plate.json',dict(type='ic2:metal_former_rolling',ingredient=ingredient('thaumcraft:thaumium_ingot'),result=item('emt:thaumium_plate')))
primal=['ignis','aqua','aer','terra','ordo','perditio'];key='emt:uu_matter_infusion';display=[];pages=[]
crucible=dict(type='thaumcraft:crucible',research=key,catalyst=ingredient('ic2:uu_matter_cell'),aspects={'vitreus':8,'gelum':8,'praecantatio':8},result=item('emt:uu_matter_drop'))
write('data/emt/recipes/uu_matter_drop.json',crucible)
display.append(dict(key='emt:uu_matter_drop',**{'class':'modern'},data=dict(kind='crucible',research=key,catalyst=stack(crucible['catalyst']),output=stack(crucible['result']),aspects=crucible['aspects'])))
pages=[dict(type='TEXT',text='emt.text.UU-Matter Infusion'),dict(type='CRUCIBLE_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=['emt:uu_matter_drop']))]
conversions=[
 ('charcoal_to_coal','minecraft:charcoal','minecraft:coal',16,2,8),('glowstone_dust_to_block','minecraft:glowstone_dust','minecraft:glowstone',1,1,4),
 ('stone_bricks_to_iron_ore','minecraft:stone_bricks','minecraft:iron_ore',16,2,8),('arcane_stone_to_copper_ore','thaumcraft:arcane_stone','ic2:crushed_copper',32,2,8),
 ('arcane_stone_bricks_to_tin_ore','thaumcraft:arcane_stone_bricks','ic2:crushed_tin',32,2,8),('amber_block_to_lead_ore','thaumcraft:amber_block','ic2:crushed_lead',32,2,8),
 ('amber_bricks_to_uranium_ore','emt:amber_bricks','ic2:uranium_ore',8,3,16),('shard_to_resin','#thaumcraft:shards','ic2:resin',21,3,8),
 ('shard_to_redstone','#thaumcraft:shards','minecraft:redstone',24,3,8),('shard_to_lapis','#thaumcraft:shards','minecraft:lapis_lazuli',8,3,8),
 ('iron_to_gold','minecraft:iron_ingot','minecraft:gold_ingot',2,3,4),('gold_to_diamond','minecraft:gold_ingot','minecraft:diamond',1,4,8),
 ('diamond_to_uranium','minecraft:diamond','ic2:uranium_238',2,8,16),('uranium_to_iridium','ic2:uranium_238','ic2:iridium_ore',2,12,32)]
for id,center,result,count,drops,cost in conversions:
 center={'tag':center[1:]} if center.startswith('#') else ingredient(center)
 aspects={aspect:cost for aspect in primal};recipe=dict(type='thaumcraft:infusion',research=key,instability=4,aspects=aspects,center={'ingredient':center},components=[{'ingredient':ingredient('emt:uu_matter_drop')} for _ in range(drops)],result=item(result,count))
 write('data/emt/recipes/uu_'+id+'.json',recipe);recipekey='emt:uu_'+id
 pages.append(dict(type='INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[recipekey])))
 display.append(dict(key=recipekey,**{'class':'modern'},data=dict(kind='infusion',research=key,instability=4,aspects=aspects,input=stack(recipe['center']),output=stack(recipe['result']),ingredients=stack(recipe['components']))))
catalog=[dict(key=key,category='EMT',displayColumn=5,displayRow=5,pageCount=len(pages),complexity=3,aspects={'alienis':4,'praecantatio':4,'fabrico':5},parentsHidden=['emt:foundation','INFUSION'],isConcealed=True,icon_item=ingredient('emt:uu_matter_drop'))]
pageentries=[dict(key=key,pages=pages)]
for id,name,x,y,icon in [('lightning_summoner','Lightning Summoner',6,-5,'emt:lightning_summoner'),('macerating_clusters','Macerating Native Ore Clusters',5,-7,'ic2:small_gold_dust')]:
 catalog.append(dict(key='emt:'+id,category='EMT',displayColumn=x,displayRow=y,pageCount=1,complexity=0,aspects={},isRound=True,isAutoUnlock=True,icon_item=ingredient(icon)))
 pageentries.append(dict(key='emt:'+id,pages=[dict(type='TEXT',text='emt.text.'+name)]));aliases['tc.research_name.emt:'+id]='emt.rsname.'+name;aliases['tc.research_text.emt:'+id]='emt.tag.'+name
aliases.update({'tc.research_name.'+key:'emt.rsname.UU-Matter Infusion','tc.research_text.'+key:'emt.tag.UU-Matter Infusion'})
write('data/emt/research_catalog/materials.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=catalog)]))
write('data/emt/research_pages/materials.json',dict(format=1,entries=pageentries))
path=root/'data/emt/thaumcraft/original_resources.json';mapping=json.loads(path.read_text(encoding='utf8'))
for lang in ['en_us','zh_cn']:mapping['languages']['assets/emt/lang/'+lang+'.json'].update(aliases)
write('data/emt/thaumcraft/original_resources.json',mapping)
path=root/'data/emt/research_page_recipes/main.json';values=json.loads(path.read_text(encoding='utf8'));keys={e['key'] for e in display};values['entries']=[e for e in values['entries'] if e['key'] not in keys]+display;write('data/emt/research_page_recipes/main.json',values)
for lang,name in [('en_us','Amber Bricks'),('zh_cn','琥珀砖')]:
 path=root/('assets/emt/lang/'+lang+'.json');values=json.loads(path.read_text(encoding='utf8')) if path.exists() else {};values['block.emt.amber_bricks']=name;write('assets/emt/lang/'+lang+'.json',values)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
