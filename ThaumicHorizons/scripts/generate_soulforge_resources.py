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
    return {'from':start,'to':end,'faces':{face:{'texture':'#body','uv':[x/4 for x in rect]} for face,rect in uv.items()}}
elements=[cube([0,0,0],[16,8,16],0,0),cube([5.5,8,5.5],[10.5,10,10.5],0,24),cube([3,10,3],[13,16,13],0,48)]+[cube([x,8,z],[x+1,12,z+1],0,31) for x in [0,15] for z in [0,15]]
write(f'assets/{MOD}/models/block/soulforge.json',{'render_type':'minecraft:translucent','textures':{'body':f'{MOD}:models/soulforge','particle':f'{MOD}:models/soulforge'},'elements':elements})
write(f'assets/{MOD}/models/item/soulforge.json',{'parent':f'{MOD}:block/soulforge'})
write(f'assets/{MOD}/blockstates/soulforge.json',{'variants':{'':{'model':f'{MOD}:block/soulforge'}}})
write(f'data/{MOD}/loot_tables/blocks/soulforge.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:soulforge'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
materials=['thaumcraft:brain_jar','minecraft:golden_apple','thaumcraft:vis_filter']*2;aspects={'spiritus':16,'fabrico':16,'permutatio':16,'cognitio':32,'sano':16}
write(f'data/{MOD}/recipes/infusion/soulforge.json',{'type':'thaumcraft:infusion','research':'soulAssembler','instability':6,'aspects':aspects,'center':{'ingredient':{'item':'thaumcraft:essentia_jar'}},'components':[{'ingredient':{'item':i}} for i in materials],'result':{'item':f'{MOD}:soulforge'}})
write(f'data/{MOD}/research_catalog/soulforge.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[{'key':'soulAssembler','category':'ThaumicHorizons','displayColumn':9,'displayRow':6,'complexity':3,'aspects':[{'aspect':a,'amount':4} for a in ['spiritus','sano','cognitio']],'parents':['soulKnowledge'],'isConcealed':True,'warp':2,'icon_item':{'item':f'{MOD}:soulforge'},'pageCount':4}]}]})
write(f'data/{MOD}/research_pages/soulforge.json',{'format':1,'entries':[{'key':'soulAssembler','pages':[{'type':'TEXT','text':'soulAssembler1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:soulforge']}},{'type':'TEXT','text':'soulAssembler2'},{'type':'TEXT','text':'soulAssembler3'}]}]})
write(f'data/{MOD}/research_page_recipes/soulforge.json',{'format':1,'entries':[{'key':f'{MOD}:soulforge','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'soulAssembler','instability':6,'aspects':aspects,'input':stack('thaumcraft:essentia_jar'),'output':stack(f'{MOD}:soulforge'),'ingredients':{'kind':'sequence','elements':[stack(i) for i in materials]}}}]})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(path);manifest['mapping'][f'assets/{MOD}/textures/models/soulforge.png']=f'assets/thaumichorizons/textures/models/soulforge.png'
for language in manifest['languages'].values():
    language[f'block.{MOD}.soulforge']='tile.ThaumicHorizons_soulforge.name'
    for key in ['soulAssembler1','soulAssembler2','soulAssembler3','tc.research_name.soulAssembler','tc.research_text.soulAssembler']:language[key]=key
write(path,manifest)
for language,status in [('zh_cn','灵魂：%s/16；碎片：%s/9600；认知储备：%s'),('en_us','Souls: %s/16; bits: %s/9600; cognition reserve: %s')]:
    path=f'assets/{MOD}/lang/{language}.json';data=read(path);data[f'{MOD}.soulforge.status']=status;write(path,data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
