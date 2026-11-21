import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,v):
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
write(f'assets/{MOD}/blockstates/vortex.json',{'variants':{'':{'model':f'{MOD}:block/vortex'}}});write(f'assets/{MOD}/models/block/vortex.json',{'textures':{'particle':f'{MOD}:block/vortex'},'elements':[]})
for name in ['vortex','vortex_stable']:write(f'assets/{MOD}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:block/vortex'}})
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(p);manifest['mapping'][f'assets/{MOD}/textures/block/vortex.png']='assets/thaumichorizons/textures/blocks/vortex.png'
for lang in manifest['languages'].values():
 lang[f'block.{MOD}.vortex']='tile.ThaumicHorizons_vortex.0.name';lang[f'item.{MOD}.vortex_stable']='tile.ThaumicHorizons_vortex.1.name'
manifest['mapping'][f'assets/{MOD}/sounds/vortex.ogg']='assets/thaumichorizons/sounds/vortex.ogg'
write(p,manifest)
write(f'assets/{MOD}/sounds.json',{'vortex':{'sounds':[{'name':f'{MOD}:vortex','stream':True}]}})
