import json,zipfile,itertools
from pathlib import Path
BASE=Path(__file__).resolve().parents[1];ROOT=BASE/'src/main/resources';MOD='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,v):
 if ('/item_aspects/' in p or '/item_bonus_aspects/' in p) and (ROOT / 'data/thaumichorizons/item_aspects/explicit.json').exists(): return
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(p)
clouds=['cloud','fire_cloud','thunder_cloud','acid_cloud','alloy_cloud','flesh_cloud','vis_cloud','glyph_cloud','spore_cloud','animus_cloud']
with zipfile.ZipFile(BASE/'references/ThaumicHorizons-1.8.21.jar') as jar:
 def external(new,old):
  assert old in jar.namelist(),old
  manifest['mapping'][new]=old
  if old+'.mcmeta' in jar.namelist():manifest['mapping'][new+'.mcmeta']=old+'.mcmeta'
 for i,name in enumerate(clouds+['leviathan']):
  texture=name.replace('_','')
  external(f'assets/{MOD}/textures/block/{name}.png',f'assets/{MOD}/textures/blocks/{texture}.png')
  model={'parent':'minecraft:block/cube_all','textures':{'all':f'{MOD}:block/{name}'}}
  if name!='leviathan':
   external(f'assets/{MOD}/textures/block/{name}_top.png',f'assets/{MOD}/textures/blocks/{texture}top.png')
   model={'parent':'minecraft:block/cube_bottom_top','render_type':'minecraft:translucent','textures':{'side':f'{MOD}:block/{name}','top':f'{MOD}:block/{name}_top','bottom':f'{MOD}:block/{name}_top'}}
  write(f'assets/{MOD}/models/block/{name}.json',model)
  write(f'assets/{MOD}/models/item/{name}.json',{'parent':f'{MOD}:block/{name}'})
  write(f'assets/{MOD}/blockstates/{name}.json',{'variants':{'':{'model':f'{MOD}:block/{name}'}}})
  write(f'data/{MOD}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
  for lang in manifest['languages'].values():lang[f'block.{MOD}.{name}']='tile.ThaumicHorizons_leviathan.name' if name=='leviathan' else f'tile.ThaumicHorizons_cloud.{i}.name'
 for name,texture in [('meat','meat'),('cooked_meat','meatcooked'),('meat_nugget','meatnugget')]:
  external(f'assets/{MOD}/textures/item/{name}.png',f'assets/{MOD}/textures/items/{texture}.png')
  write(f'assets/{MOD}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/{name}'}})
  for lang in manifest['languages'].values():lang[f'item.{MOD}.{name}']=f'item.{texture}TH.name'
 for name in ['rain','firerain']:
  path=f'assets/{MOD}/textures/environment/{name}.png';external(path,path)
entries=[];pages=[]
for key,y,count,items in [('planarClouds',6,3,clouds),('leviathan',10,2,['leviathan'])]:
 entries.append({'key':key,'category':'ThaumicHorizons','displayColumn':20,'displayRow':y,'complexity':1,'aspects':[],'parents':['pocketPlane'],'isLost':True,'isRound':True,'icon_item':{'item':f'{MOD}:{items[0]}'},'itemTriggers':[{'item':f'{MOD}:{name}'} for name in items],'pageCount':count+1})
 pages.append({'key':key,'pages':[{'type':'TEXT','text':f'{key}{i}'} for i in range(1,count+1)]+[{'type':'TEXT','text':f'{MOD}.cloud.availability'}]})
 for lang in manifest['languages'].values():
  for prefix in ['tc.research_name.','tc.research_text.']:lang[prefix+key]=prefix+key
  for i in range(1,count+1):lang[f'{key}{i}']=f'{key}{i}'
write(f'data/{MOD}/research_catalog/clouds.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':entries}]})
write(f'data/{MOD}/research_pages/clouds.json',{'format':1,'entries':pages})
write(f'data/{MOD}/item_aspects/clouds.json',{'format':1,'entries':[{'items':[f'{MOD}:{name}' for name in items],'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()]} for items,aspects in [(clouds,{'tempestas':2}),(['leviathan'],{'alienis':8,'mortuus':4}),(['meat'],{'corpus':4}),(['cooked_meat'],{'corpus':3,'ignis':1}),(['meat_nugget'],{'corpus':1})]]})
write(f'data/{MOD}/recipes/smelting/meat.json',{'type':'minecraft:smelting','ingredient':{'item':f'{MOD}:meat'},'result':f'{MOD}:cooked_meat','experience':1,'cookingtime':200})
write(f'data/{MOD}/recipes/infernal_bonus/meat.json',{'type':'thaumcraft:infernal_bonus','ingredient':{'item':f'{MOD}:meat'},'base_count':0,'result':{'item':f'{MOD}:meat_nugget'}})
for a,b in itertools.combinations(['chicken','pork','beef','fish'],2):
 write(f'data/{MOD}/recipes/triple_meat_{a}_{b}.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':i} for i in ['minecraft:sugar',f'{MOD}:meat_nugget',f'thaumcraft:{a}_nugget',f'thaumcraft:{b}_nugget']],'result':{'item':'thaumcraft:triple_meat_treat'}})
write(p,manifest)
for language,text in {'en_us':'Clouds and leviathan remains are available in Creative mode. Their natural generation methods were empty in the reference version; this port does not invent a survival recipe. Scan either block to discover its research after Pocket Planes. Cloud precipitation follows overworld rain, even in another dimension.','zh_cn':'位面云与利维坦残骸目前可从创造模式取得。参考版本的两种自然生成方法为空，本移植版不虚构生存配方。完成口袋位面前置后扫描对应方块可发现研究。位面云的降水跟随主世界雨态，在其他维度也适用。'}.items():
 path=f'assets/{MOD}/lang/{language}.json';data=read(path);data[f'{MOD}.cloud.availability']=text;data[f'entity.{MOD}.finite_lightning']='有限雷击' if language=='zh_cn' else 'Finite Lightning';write(path,data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
