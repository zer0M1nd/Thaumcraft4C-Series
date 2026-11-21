"""Content previously gated on base APIs, available starting at 0.4.12."""
import json,zipfile
from pathlib import Path
BASE=Path(__file__).resolve().parents[1];ROOT=BASE/'src/main/resources';M='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,v):
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1,'metadata':0,'damageable':False}}
rod={'tag':M+':disposable','capacity':250,'craftCost':99,'glowing':True,'rechargeable':False,'texture':M+':textures/models/crystal_rod.png','research':'crystalWand'}
cap={'tag':M+':disposable','cost':0.9,'craftCost':99,'texture':M+':textures/models/crystal_cap.png','research':'crystalWand'}
write(f'data/{M}/thaumcraft/wand_rods.json',[rod]);write(f'data/{M}/thaumcraft/wand_caps.json',[cap])
write(f'assets/{M}/models/item/disposable_wand.json',{'parent':'builtin/entity','textures':{'particle':M+':items/crystalwand'}})
write(f'assets/{M}/models/item/crystal_wand.json',{'parent':'minecraft:item/handheld','textures':{'layer0':M+':items/crystalwand'}})
write(f'assets/{M}/models/item/transduction_amplifier.json',{'parent':'builtin/entity','textures':{'particle':'minecraft:block/iron_block'}})
write(f'assets/{M}/models/block/transduction_amplifier.json',{'textures':{'particle':'minecraft:block/iron_block'},'elements':[]})
write(f'assets/{M}/blockstates/transduction_amplifier.json',{'variants':{'facing='+f:{'model':M+':block/transduction_amplifier'} for f in ['north','south','east','west']}})
write(f'data/{M}/loot_tables/blocks/transduction_amplifier.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':M+':transduction_amplifier'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
catalog=[];pages=[];display=[]
for key,item,x,y,complexity,costs,parents,pattern,ingredients,vis,texts in [
 ('transductionAmplifier','transduction_amplifier',12,6,3,{'auram':4,'potentia':4,'vacuos':3},['planarTheory'],[' C ','ATA','RNR'],{'C':M+':planar_conduit','A':'thaumcraft:alumentum','T':'thaumcraft:node_converter','R':'minecraft:redstone_block','N':'thaumcraft:thaumium_ingot'},{'aer':10,'ignis':10,'ordo':10},2),
 ('crystalWand','crystal_wand',15,10,2,{'praecantatio':4,'vitreus':4,'potentia':4},['planarRift'],['  B',' C ','B  '],{'B':'thaumcraft:balanced_shard','C':'thaumcraft:mixed_crystal_cluster'},{a:25 for a in ['aer','terra','ignis','aqua','ordo','perditio']},1)]:
  output=M+':'+item;recipe=M+':'+key
  write(f'data/{M}/recipes/arcane/{item}.json',{'type':'thaumcraft:arcane','research':key,'pattern':pattern,'key':{k:{'item':v} for k,v in ingredients.items()},'vis':vis,'result':{'item':output}})
  content=[{'type':'TEXT','text':key+'1'},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.ShapedArcaneRecipe','keys':[recipe]}}]+[{'type':'TEXT','text':key+str(i)} for i in range(2,texts+1)]
  catalog.append({'key':key,'category':'ThaumicHorizons','displayColumn':x,'displayRow':y,'complexity':complexity,'aspects':[{'aspect':a,'amount':n} for a,n in costs.items()],'parents':parents,'isConcealed':True,'icon_item':{'item':output},'pageCount':len(content)})
  pages.append({'key':key,'pages':content})
  display.append({'key':recipe,'class':'thaumcraft.api.crafting.ShapedArcaneRecipe','data':{'kind':'arcane_shaped','research':key,'aspects':vis,'width':3,'height':3,'mirrored':True,'output':stack(output),'ingredients':{'kind':'sequence','elements':[stack(ingredients[c]) if c!=' ' else None for row in pattern for c in row]}}})
write(f'data/{M}/research_catalog/api_completion.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':M+':textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':catalog}]})
write(f'data/{M}/research_pages/api_completion.json',{'format':1,'entries':pages})
write(f'data/{M}/research_page_recipes/api_completion.json',{'format':1,'entries':display})
p=f'data/{M}/thaumcraft/original_resources.json';manifest=read(p)
with zipfile.ZipFile(BASE/'references/ThaumicHorizons-1.8.21.jar') as jar:
 for path in ['textures/items/crystalwand.png','textures/models/crystalRod.png','textures/models/crystalCap.png','textures/models/crystalCap.png.mcmeta']:
  path='assets/'+M+'/'+path;assert path in jar.namelist();manifest['mapping'][path.replace('crystalRod','crystal_rod').replace('crystalCap','crystal_cap')]=path
for lang in manifest['languages'].values():
 lang[f'block.{M}.transduction_amplifier']='tile.ThaumicHorizons_transductionAmplifier.name'
 lang[f'item.{M}.crystal_wand']='item.crystalWand.name'
 for part in ['rod','cap']:lang[f'item.Wand.{M}:disposable.{part}']=f'item.Wand.disposable.{part}'
 for row in catalog:
  key=row['key']
  for prefix in ['tc.research_name.','tc.research_text.']:lang[prefix+key]=prefix+key
 for row in pages:
  for page in row['pages']:
   if page['type']=='TEXT':lang[page['text']]=page['text']
write(p,manifest)
for lang,values in {
 'zh_cn':{'preview':'拖动旋转；滚轮或下方箭头选择层。蓝色表示水源。','all_layers':'全部四层','layer':'第 %s 层'},
 'en_us':{'preview':'Drag to rotate. Scroll or click arrows for layers. Blue marks water sources.','all_layers':'All four layers','layer':'Layer %s'}
}.items():
 p=f'assets/{M}/lang/{lang}.json';data=read(p);data.pop(M+'.rift.availability',None)
 data.update({M+'.vat.'+k:v for k,v in values.items()});write(p,data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
