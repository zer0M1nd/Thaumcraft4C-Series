import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,value):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1,'metadata':0,'damageable':False}}
def cube(start,end,u,v):
    w,h,d=[end[i]-start[i] for i in range(3)]
    uv={'west':[u,v+d,u+d,v+d+h],'north':[u+d,v+d,u+d+w,v+d+h],'east':[u+d+w,v+d,u+2*d+w,v+d+h],'south':[u+2*d+w,v+d,u+2*d+2*w,v+d+h],'up':[u+d,v,u+d+w,v+d],'down':[u+d+w,v,u+d+2*w,v+d]}
    return {'from':start,'to':end,'faces':{face:{'texture':'#body','uv':[x/(8 if i%2==0 else 4) for i,x in enumerate(rect)]} for face,rect in uv.items()}}
elements=[cube([0,10,15],[16,16,16],0,0),cube([0,10,0],[16,16,1],0,9),cube([15,10,1],[16,16,15],0,44),cube([0,10,1],[1,16,15],0,18),cube([1,10,1],[15,11,15],48,0),cube([4,4,4],[12,10,12],32,32),cube([6,0,6],[10,4,10],32,18)]
write(f'assets/{MOD}/models/block/soul_extractor.json',{'textures':{'body':f'{MOD}:models/soulsieve','particle':f'{MOD}:models/soulsieve'},'elements':elements})
write(f'assets/{MOD}/models/item/soul_extractor.json',{'parent':f'{MOD}:block/soul_extractor'})
write(f'assets/{MOD}/blockstates/soul_extractor.json',{'variants':{'':{'model':f'{MOD}:block/soul_extractor'}}})
write(f'data/{MOD}/loot_tables/blocks/soul_extractor.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:soul_extractor'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
materials=['thaumcraft:amber','thaumcraft:enchanted_fabric','minecraft:piston']*2;aspects={'spiritus':16,'machina':8,'permutatio':8}
write(f'data/{MOD}/recipes/infusion/soul_extractor.json',{'type':'thaumcraft:infusion','research':'soulExtractor','instability':4,'aspects':aspects,'center':{'ingredient':{'item':'minecraft:hopper'}},'components':[{'ingredient':{'item':i}} for i in materials],'result':{'item':f'{MOD}:soul_extractor'}})
write(f'data/{MOD}/research_catalog/soul_extractor.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[{'key':'soulExtractor','category':'ThaumicHorizons','displayColumn':5,'displayRow':6,'complexity':1,'aspects':[{'aspect':a,'amount':4} for a in ['spiritus','vinculum','terra','permutatio']],'parents':['JARBRAIN','ENCHFABRIC'],'isHidden':True,'itemTriggers':[{'item':'minecraft:soul_sand','count':1,'metadata':0,'damageable':False}],'warp':3,'icon_item':{'item':f'{MOD}:soul_extractor'},'pageCount':3}]}]})
write(f'data/{MOD}/research_pages/soul_extractor.json',{'format':1,'entries':[{'key':'soulExtractor','pages':[{'type':'TEXT','text':'soulExtractor1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:soul_extractor']}},{'type':'TEXT','text':'soulExtractor2'}]}]})
write(f'data/{MOD}/research_page_recipes/soul_extractor.json',{'format':1,'entries':[{'key':f'{MOD}:soul_extractor','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'soulExtractor','instability':4,'aspects':aspects,'input':stack('minecraft:hopper'),'output':stack(f'{MOD}:soul_extractor'),'ingredients':{'kind':'sequence','elements':[stack(i) for i in materials]}}}]})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(path);manifest['mapping'][f'assets/{MOD}/textures/models/soulsieve.png']=f'assets/thaumichorizons/textures/models/soulsieve.png'
for language in manifest['languages'].values():
    language[f'block.{MOD}.soul_extractor']='tile.ThaumicHorizons_soulSieve.name'
    for key in ['soulExtractor1','soulExtractor2','tc.research_name.soulExtractor','tc.research_text.soulExtractor']:language[key]=key
write(path,manifest)
for language,status in [('zh_cn','筛分剩余：%s/1200；灵魂沙：%s'),('en_us','Sifting remaining: %s/1200; soul sand: %s')]:
    path=f'assets/{MOD}/lang/{language}.json';data=read(path);data[f'{MOD}.extractor.status']=status;write(path,data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
