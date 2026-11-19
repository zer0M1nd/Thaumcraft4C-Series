"""Blood infusion resources. Artwork and translated legacy prose remain external."""
import json
import re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
MOD='thaumichorizons'

def modern_resource_paths(value):
    if isinstance(value,str) and re.fullmatch(r'thaumichorizons:(?:textures|block|item|items|models|gui|misc)/[A-Za-z0-9/._-]+',value):return value.lower()
    if isinstance(value,list):return [modern_resource_paths(v) for v in value]
    if isinstance(value,dict):return {k:modern_resource_paths(v) for k,v in value.items()}
    return value
def write(path,data):
    if path.endswith('/original_resources.json'):
        data={**data,'mapping':{k.lower():v for k,v in data['mapping'].items()}}
    else:data=modern_resource_paths(data)
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
items={'empty_syringe':('syringeEmpty','syringeEmpty'),'human_blood':('syringeBlood','syringeBlood.0'),'blood_phial':('phialBlood','syringeBlood.1'),'blood_sample':('syringeBlood','syringeSample'),'injection':('phialBlood','injection.0'),'injection_phial':('phialBlood','injection.1')}
for item,(texture,key) in items.items():write(f'assets/{MOD}/models/item/{item}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:items/{texture}'}})
def cube(x,y,z,w,h,d,u,v):
    # Authored modern model coordinates and the six faces of the legacy model-box UV net.
    faces={'up':[u+d,v,u+d+w,v+d],'down':[u+d+w,v,u+d+2*w,v+d],'west':[u,v+d,u+d,v+d+h],'north':[u+d,v+d,u+d+w,v+d+h],'east':[u+d+w,v+d,u+2*d+w,v+d+h],'south':[u+2*d+w,v+d,u+2*d+2*w,v+d+h]}
    return {'from':[x+8,24-y-h,z+8],'to':[x+w+8,24-y,z+d+8],'faces':{side:{'texture':'#body','uv':[n/4 for n in uv]} for side,uv in faces.items()}}
parts=[cube(-8,14,-8,16,10,16,0,0),cube(-3,8,-3,6,6,6,0,32)]
for x,y,z,w,h,d in [(-1,9,3,2,2,3),(-1,9,-6,2,2,3),(3,9,-1,3,2,2),(-6,9,-1,3,2,2)]:parts.append(cube(x,y,z,w,h,d,0,48))
for x,z in [(-1,4),(-1,-6),(4,-1),(-6,-1)]:parts.append(cube(x,11,z,2,3,2,0,54))
write(f'assets/{MOD}/models/block/blood_infuser.json',{'textures':{'body':f'{MOD}:models/bloodinfuser','particle':f'{MOD}:models/bloodinfuser'},'elements':parts})
write(f'assets/{MOD}/blockstates/blood_infuser.json',{'variants':{'':{'model':f'{MOD}:block/blood_infuser'}}})
write(f'assets/{MOD}/models/item/blood_infuser.json',{'parent':f'{MOD}:block/blood_infuser'})
write(f'data/{MOD}/loot_tables/blocks/blood_infuser.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:blood_infuser'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(f'data/{MOD}/recipes/arcane/empty_syringe.json',{'type':'thaumcraft:arcane','research':'essentiaInjection','pattern':['P','N'],'key':{'P':{'item':'thaumcraft:empty_phial'},'N':{'item':'minecraft:iron_nugget'}},'vis':{'aqua':1,'ordo':1},'result':{'item':f'{MOD}:empty_syringe'}})
write(f'data/{MOD}/recipes/arcane/blood_phial.json',{'type':'thaumcraft:arcane','mode':'shapeless','research':'blastPhial','ingredients':[{'item':i} for i in ['thaumcraft:empty_phial']*4+['minecraft:gunpowder',f'{MOD}:human_blood']],'vis':{'ignis':1,'aqua':1,'perditio':1},'result':{'item':f'{MOD}:blood_phial','count':4}})
write(f'data/{MOD}/recipes/infusion/blood_infuser.json',{'type':'thaumcraft:infusion','research':'essentiaInjection','instability':4,'aspects':{'humanus':8,'permutatio':8,'victus':16,'praecantatio':16},'center':{'ingredient':{'item':'thaumcraft:crucible'}},'components':[{'ingredient':{'item':i}} for i in ['minecraft:comparator','thaumcraft:alchemical_construct','thaumcraft:essentia_filter','thaumcraft:alchemical_construct']],'result':{'item':f'{MOD}:blood_infuser'}})
manifest_path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=json.loads((ROOT/manifest_path).read_text(encoding='utf-8'))
for path in ['textures/gui/guibloodinfuser.png','textures/models/bloodinfuser.png','textures/models/syringe.png','textures/misc/potions.png']+[f'textures/items/{name}.png' for name in ['syringeEmpty','syringeBlood','phialBlood']]:manifest['mapping'][f'assets/{MOD}/{path}']=f'assets/thaumichorizons/{path}'
for translations in manifest['languages'].values():
    for item,(texture,key) in items.items():translations[f'item.{MOD}.{item}']=f'item.{key}.name'
    translations[f'block.{MOD}.blood_infuser']='tile.ThaumicHorizons_bloodInfuser.name'
    translations['container.bloodInfuser']='tile.ThaumicHorizons_bloodInfuser.name'
    for modern,old in [('vis_regeneration','visregen'),('vacuum','vacuum'),('shock','shock'),('synthesis','synthesis')]:translations[f'effect.{MOD}.{modern}']='potion.'+old
write(manifest_path,manifest)
for language,values in {'en_us':{'once':'Infuse once','continuous':'Infuse continuously','clone_cost':'Essentia required to clone: Victus ×4'},'zh_cn':{'once':'注魔一次','continuous':'连续注魔','clone_cost':'克隆所需要素：生命 ×4'}}.items():
    path=f'assets/{MOD}/lang/{language}.json';existing=json.loads((ROOT/path).read_text(encoding='utf-8'))
    existing.update({f'{MOD}.infuser.once':values['once'],f'{MOD}.infuser.continuous':values['continuous'],f'{MOD}.sample.clone_cost':values['clone_cost']});write(path,existing)
