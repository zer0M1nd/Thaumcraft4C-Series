"""Modern syringe geometry and injector progression; original artwork remains external."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,data):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def cube(x,y,z,w,h,d,u,v,tinted=False):
    uv={'up':[u+d,v,u+d+w,v+d],'down':[u+d+w,v,u+d+2*w,v+d],'west':[u,v+d,u+d,v+d+h],'north':[u+d,v+d,u+d+w,v+d+h],'east':[u+d+w,v+d,u+2*d+w,v+d+h],'south':[u+2*d+w,v+d,u+2*d+2*w,v+d+h]}
    return {'from':[x+8,30.5-y-h,z+7.5],'to':[x+w+8,30.5-y,z+d+7.5],'faces':{side:dict(texture='#body',uv=[n/(4 if i%2==0 else 2) for i,n in enumerate(rect)],**({'tintindex':0} if tinted else {})) for side,rect in uv.items()}}
geometry=[cube(-3,21,-1,9,3,3,0,0,True),cube(-7,22,0,4,1,1,25,0),cube(6,22,0,1,1,1,0,8),cube(7,21,-1,1,3,3,0,12)]
model={'textures':{'body':f'{MOD}:models/syringe','particle':f'{MOD}:models/syringe'},'render_type':'minecraft:translucent','elements':geometry,'display':{'gui':{'rotation':[20,0,-35],'scale':[.9,.9,.9]},'ground':{'scale':[.6,.6,.6]},'firstperson_righthand':{'rotation':[0,0,-66],'translation':[0,2,1]},'thirdperson_righthand':{'rotation':[0,0,-66],'translation':[0,2,0]}}}
for item in ['empty_syringe','human_blood','blood_sample','injection']:write(f'assets/{MOD}/models/item/{item}.json',model)
write(f'assets/{MOD}/models/item/injector.json',{'parent':'builtin/entity','textures':{'particle':f'{MOD}:models/injector'},'display':{'firstperson_righthand':{'rotation':[0,180,0],'translation':[0,0,-2]},'thirdperson_righthand':{'rotation':[0,180,0],'translation':[0,2,0]}}})
cost={'telum':8,'machina':8,'volatus':8};components=['thaumcraft:greatwood_planks','minecraft:feather','thaumcraft:thaumium_ingot','thaumcraft:air_shard']
write(f'data/{MOD}/recipes/infusion/injector.json',{'type':'thaumcraft:infusion','research':'injector','instability':2,'aspects':cost,'center':{'ingredient':{'item':'thaumcraft:bone_bow'}},'components':[{'ingredient':{'item':i}} for i in components],'result':{'item':f'{MOD}:injector'}})
entry={'key':'injector','category':'ThaumicHorizons','displayColumn':8,'displayRow':3,'complexity':1,'aspects':[{'aspect':key,'amount':3} for key in cost],'parents':['blastPhial','BONEBOW'],'isConcealed':True,'icon_item':{'item':f'{MOD}:injector'},'pageCount':2}
write(f'data/{MOD}/research_catalog/injector.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[entry]}]})
write(f'data/{MOD}/research_pages/injector.json',{'format':1,'entries':[{'key':'injector','pages':[{'type':'TEXT','text':'injector1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:injector']}}]}]})
def stack(item):return {'kind':'stack','stack':{'item':item,'metadata':0,'count':1,'damageable':False}}
write(f'data/{MOD}/research_page_recipes/injector.json',{'format':1,'entries':[{'key':f'{MOD}:injector','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'injector','input':stack('thaumcraft:bone_bow'),'output':stack(f'{MOD}:injector'),'ingredients':{'kind':'sequence','elements':[stack(i) for i in components]},'aspects':cost,'instability':2}}]})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(path)
for asset in ['textures/models/injector.png','textures/gui/guiinjector.png']:manifest['mapping'][f'assets/{MOD}/{asset}']=f'assets/thaumichorizons/{asset}'
for translations in manifest['languages'].values():
    translations[f'item.{MOD}.injector']='item.injector.name'
    for key in ['tc.research_name.injector','tc.research_text.injector','injector1']:translations[key]=key
write(path,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
