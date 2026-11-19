import json,zipfile
from pathlib import Path
BASE=Path(__file__).resolve().parents[1];ROOT=BASE/'src/main/resources';MOD='thaumichorizons'
def write(path,value):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=json.loads((ROOT/p).read_text(encoding='utf-8'))
texture=f'assets/{MOD}/textures/items/keystone.png'
with zipfile.ZipFile(BASE/'references/ThaumicHorizons-1.8.21.jar') as jar:assert texture in jar.namelist()
manifest['mapping'][f'assets/{MOD}/textures/item/keystone.png']=texture
for lang in manifest['languages'].values():
 lang[f'item.{MOD}.keystone']='item.keystoneBlank.name';lang[f'item.{MOD}.keystone_bound']='item.keystoneTH.name'
write(p,manifest)
write(f'assets/{MOD}/models/item/keystone.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/keystone'}})
# The gateway research pages are generated with the slot/gateway implementation.
for key,research,center,components,cost in [
 ('keystone','pocketPlane',f'{MOD}:planar_conduit',['minecraft:quartz','thaumcraft:balanced_shard']*4,{'iter':32,'vacuos':32,'permutatio':16}),
 ('keystone_alternate','planarKeystone','minecraft:diamond',['thaumcraft:thaumium_ingot','thaumcraft:balanced_shard']*2,{'iter':24,'vacuos':24,'alienis':12})]:
 write(f'data/{MOD}/recipes/infusion/{key}.json',{'type':'thaumcraft:infusion','research':research,'instability':6,'aspects':cost,'center':{'ingredient':{'item':center}},'components':[{'ingredient':{'item':i}} for i in components],'result':{'item':f'{MOD}:keystone'}})
