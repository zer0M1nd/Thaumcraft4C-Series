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
elements=[cube([0,0,0],[16,1,16],0,23),cube([2,1,2],[14,12,14],0,0)]
for x in [0,14]:elements.append(cube([x,11,7],[x+2,12,9],48,0))
for z in [0,14]:elements.append(cube([7,11,z],[9,12,z+2],48,0))
for x in [0,15]:elements.append(cube([x,12,7],[x+1,16,9],48,16))
for z in [0,15]:elements.append(cube([7,12,z],[9,16,z+1],48,8))
for x in [1,11]:elements.append(cube([x,15,7],[x+4,16,9],48,48))
for z in [1,11]:elements.append(cube([7,15,z],[9,16,z+4],0,48))
write(f'assets/{MOD}/models/block/soul_beacon.json',{'render_type':'minecraft:translucent','textures':{'body':f'{MOD}:models/soulbeacon','particle':'minecraft:block/end_portal_frame_eye'},'elements':elements})
write(f'assets/{MOD}/models/item/soul_beacon.json',{'parent':f'{MOD}:block/soul_beacon'})
write(f'assets/{MOD}/blockstates/soul_beacon.json',{'variants':{'':{'model':f'{MOD}:block/soul_beacon'}}})
write(f'data/{MOD}/loot_tables/blocks/soul_beacon.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:soul_beacon'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
materials=['thaumcraft:essentia_jar','thaumcraft:void_ingot','minecraft:ender_eye','minecraft:beacon','minecraft:ender_eye','thaumcraft:void_ingot'];aspects={'spiritus':16,'vinculum':32,'iter':32,'mortuus':32}
write(f'data/{MOD}/recipes/infusion/soul_beacon.json',{'type':'thaumcraft:infusion','research':'cloneSelf','instability':8,'aspects':aspects,'center':{'ingredient':{'item':'thaumcraft:primordial_pearl'}},'components':[{'ingredient':{'item':i}} for i in materials],'result':{'item':f'{MOD}:soul_beacon'}})
research=[{'key':'nightmare','category':'ThaumicHorizons','displayColumn':13,'displayRow':4,'complexity':1,'aspects':[{'aspect':'spiritus','amount':1}],'parents':['cloneVillager'],'isConcealed':True,'isRound':True,'icon_resource':f'{MOD}:textures/misc/skull.png','pageCount':2},{'key':'cloneSelf','category':'ThaumicHorizons','displayColumn':15,'displayRow':4,'complexity':2,'aspects':[{'aspect':a,'amount':4} for a in ['spiritus','vinculum','humanus','sano']],'parents':['nightmare','PRIMPEARL'],'isConcealed':True,'warp':5,'icon_resource':f'{MOD}:textures/misc/steve.png','pageCount':3}]
write(f'data/{MOD}/research_catalog/soul_beacon.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':research}]})
write(f'data/{MOD}/research_pages/soul_beacon.json',{'format':1,'entries':[{'key':'nightmare','pages':[{'type':'TEXT','text':'nightmare1'},{'type':'TEXT','text':'nightmare2'}]},{'key':'cloneSelf','pages':[{'type':'TEXT','text':'cloneSelf1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:soul_beacon']}},{'type':'TEXT','text':'cloneSelf2'}]}]})
write(f'data/{MOD}/research_page_recipes/soul_beacon.json',{'format':1,'entries':[{'key':f'{MOD}:soul_beacon','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'cloneSelf','instability':8,'aspects':aspects,'input':stack('thaumcraft:primordial_pearl'),'output':stack(f'{MOD}:soul_beacon'),'ingredients':{'kind':'sequence','elements':[stack(i) for i in materials]}}}]})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(path)
for texture in ['models/soulbeacon','misc/skull','misc/steve']:manifest['mapping'][f'assets/{MOD}/textures/{texture}.png']=f'assets/thaumichorizons/textures/{texture}.png'
for language in manifest['languages'].values():
    language[f'block.{MOD}.soul_beacon']='tile.ThaumicHorizons_soulBeacon.name';language[f'{MOD}.setBeacon']=f'{MOD}.setBeacon'
    for key in ['nightmare','cloneSelf']:
        for text in [f'{key}1',f'{key}2',f'tc.research_name.{key}',f'tc.research_text.{key}']:language[text]=text
write(path,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
