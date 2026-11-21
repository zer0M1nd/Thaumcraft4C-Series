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
elements=[cube([3,-1,3],[13,13,13],0,0),cube([0,0,0],[16,1,16],0,39),cube([2,1,2],[14,4,14],0,24),cube([2,13,2],[14,14,14],0,24)]
write(f'assets/{MOD}/models/block/inspiratron.json',{'render_type':'minecraft:translucent','textures':{'body':f'{MOD}:models/inspiratron','particle':f'{MOD}:models/inspiratron'},'elements':elements})
write(f'assets/{MOD}/models/item/inspiratron.json',{'parent':f'{MOD}:block/inspiratron'})
write(f'assets/{MOD}/blockstates/inspiratron.json',{'variants':{'':{'model':f'{MOD}:block/inspiratron'}}})
write(f'data/{MOD}/loot_tables/blocks/inspiratron.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:inspiratron'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
materials=['thaumcraft:scribing_tools','thaumcraft:thaumium_ingot','minecraft:golden_apple','thaumcraft:thaumium_ingot'];aspects={'sano':8,'cognitio':16,'ordo':8,'spiritus':8}
write(f'data/{MOD}/recipes/infusion/inspiratron.json',{'type':'thaumcraft:infusion','research':'soulKnowledge','instability':5,'aspects':aspects,'center':{'ingredient':{'item':'thaumcraft:brain_jar'}},'components':[{'ingredient':{'item':i}} for i in materials],'result':{'item':f'{MOD}:inspiratron'}})
write(f'data/{MOD}/research_catalog/inspiratron.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[{'key':'soulKnowledge','category':'ThaumicHorizons','displayColumn':7,'displayRow':6,'complexity':2,'aspects':[{'aspect':a,'amount':4} for a in ['spiritus','sano','cognitio']],'parents':['soulExtractor'],'isConcealed':True,'warp':2,'icon_item':{'item':f'{MOD}:inspiratron'},'pageCount':3}]}]})
write(f'data/{MOD}/research_pages/inspiratron.json',{'format':1,'entries':[{'key':'soulKnowledge','pages':[{'type':'TEXT','text':'soulKnowledge1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:inspiratron']}},{'type':'TEXT','text':'soulKnowledge2'}]}]})
write(f'data/{MOD}/research_page_recipes/inspiratron.json',{'format':1,'entries':[{'key':f'{MOD}:inspiratron','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'soulKnowledge','instability':5,'aspects':aspects,'input':stack('thaumcraft:brain_jar'),'output':stack(f'{MOD}:inspiratron'),'ingredients':{'kind':'sequence','elements':[stack(i) for i in materials]}}}]})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(path)
for texture in ['models/inspiratron','models/brain','models/jarbrine','models/jarbrine2','gui/guisieve','gui/guiinspiratron']:
    manifest['mapping'][f'assets/{MOD}/textures/{texture}.png']=f'assets/thaumichorizons/textures/{texture}.png'
for language in manifest['languages'].values():
    language[f'block.{MOD}.inspiratron']='tile.ThaumicHorizons_inspiratron.name'
    for key in ['soulKnowledge1','soulKnowledge2','tc.research_name.soulKnowledge','tc.research_text.soulKnowledge']:language[key]=key
write(path,manifest)
for language,values in [('zh_cn',['灵魂装置','灵魂筛分器','灵感装置','灵魂组装机','灵魂：%s / 16','认知能量：%s']),('en_us',['Soul device','Soul Sieve','Inspiratron','Soulforge','Souls: %s / 16','Cognitive energy: %s'])]:
    path=f'assets/{MOD}/lang/{language}.json';data=read(path)
    for key,value in zip(['container.thaumichorizons.soul_device']+[f'{MOD}.soul_device.{key}' for key in ['extractor','inspiratron','forge','souls','energy']],values):data[key]=value
    write(path,data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
