import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,v):
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def stack(i):return {'kind':'stack','stack':{'item':i,'count':1,'metadata':0,'damageable':False}}
def cube(x,y,z,w,h,d,u,v):
 uv={'west':[u,v+d,u+d,v+d+h],'north':[u+d,v+d,u+d+w,v+d+h],'east':[u+d+w,v+d,u+2*d+w,v+d+h],'south':[u+2*d+w,v+d,u+2*d+2*w,v+d+h],'up':[u+d,v,u+d+w,v+d],'down':[u+d+w,v,u+d+2*w,v+d]}
 return {'from':[x+8,24-y-h,z+8],'to':[x+8+w,24-y,z+8+d],'faces':{f:{'texture':'#body','uv':[a/4 for a in values]} for f,values in uv.items()}}
elements=[cube(-8,20,-8,16,4,16,0,0),cube(-1,8,-1,2,12,2,0,20)]
for half,y,u,v in [(6,18,0,34),(4,15,0,47),(2,12,0,56)]:
 for z in [-half,half-1]:elements.append(cube(-half,y,z,half*2,1,1,u,v))
 for x in [-half,half-1]:elements.append(cube(x,y,-half+1,1,1,half*2-2,u,v+2))
item=f'{MOD}:vortex_stabilizer';r='vortexStabilizer'
write(f'assets/{MOD}/models/block/vortex_stabilizer.json',{'textures':{'body':f'{MOD}:models/attenuator','particle':'minecraft:block/iron_block'},'elements':elements})
rot={'up':{},'down':{'x':180},'north':{'x':90},'south':{'x':90,'y':180},'east':{'x':90,'y':90},'west':{'x':90,'y':270}}
write(f'assets/{MOD}/blockstates/vortex_stabilizer.json',{'variants':{'facing='+key:{'model':f'{MOD}:block/vortex_stabilizer',**value} for key,value in rot.items()}})
write(f'assets/{MOD}/models/item/vortex_stabilizer.json',{'parent':f'{MOD}:block/vortex_stabilizer'})
write(f'data/{MOD}/loot_tables/blocks/vortex_stabilizer.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':item}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
cost={'auram':8,'vinculum':32,'fames':32,'potentia':16};ingredients=[f'{MOD}:planar_conduit','thaumcraft:thaumium_ingot','thaumcraft:amber','thaumcraft:thaumium_ingot']
write(f'data/{MOD}/recipes/infusion/vortex_stabilizer.json',{'type':'thaumcraft:infusion','research':r,'instability':6,'aspects':cost,'center':{'ingredient':{'item':'thaumcraft:node_stabilizer'}},'components':[{'ingredient':{'item':i}} for i in ingredients],'result':{'item':item}})
write(f'data/{MOD}/research_catalog/vortex_stabilizer.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[{'key':r,'category':'ThaumicHorizons','displayColumn':12,'displayRow':10,'complexity':2,'aspects':[{'aspect':a,'amount':3} for a in ['auram','fames','vinculum']],'parents':['planarTheory'],'isConcealed':True,'icon_item':{'item':item},'pageCount':2}]}]})
write(f'data/{MOD}/research_pages/vortex_stabilizer.json',{'format':1,'entries':[{'key':r,'pages':[{'type':'TEXT','text':r+'1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[item]}}]}]})
write(f'data/{MOD}/research_page_recipes/vortex_stabilizer.json',{'format':1,'entries':[{'key':item,'class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':r,'instability':6,'aspects':cost,'input':stack('thaumcraft:node_stabilizer'),'output':stack(item),'ingredients':{'kind':'sequence','elements':[stack(i) for i in ingredients]}}}]})
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(p);manifest['mapping'][f'assets/{MOD}/textures/models/attenuator.png']='assets/thaumichorizons/textures/models/attenuator.png'
for lang in manifest['languages'].values():
 lang[f'block.{MOD}.vortex_stabilizer']='tile.ThaumicHorizons_vortexStabilizer.name'
 for key in [r+'1','tc.research_name.'+r,'tc.research_text.'+r]:lang[key]=key
write(p,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
