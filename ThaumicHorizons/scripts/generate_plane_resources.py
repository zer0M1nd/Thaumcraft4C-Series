"""Environment resources: authored models/loot, external legacy textures only."""
import json, zipfile
from pathlib import Path
BASE=Path(__file__).resolve().parents[1]; ROOT=BASE/'src/main/resources'; MOD='thaumichorizons'
def write(path,value):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=json.loads((ROOT/p).read_text(encoding='utf-8'))
with zipfile.ZipFile(BASE/'references/ThaumicHorizons-1.8.21.jar') as jar:
 names=set(jar.namelist())
 for block,texture in {'dust':'dust','bone':'bone','brain':'brain','eyes':'eyecluster','void':'void','flesh':None}.items():
  if texture:
   old=f'assets/{MOD}/textures/blocks/{texture}.png';assert old in names
   manifest['mapping'][f'assets/{MOD}/textures/block/{block}.png']=old
   if old+'.mcmeta' in names:manifest['mapping'][f'assets/{MOD}/textures/block/{block}.png.mcmeta']=old+'.mcmeta'
  write(f'assets/{MOD}/models/block/{block}.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'{MOD}:block/{block}' if texture else 'thaumcraft:block/fleshblock'}})
  write(f'assets/{MOD}/models/item/{block}.json',{'parent':f'{MOD}:block/{block}'})
  write(f'assets/{MOD}/blockstates/{block}.json',{'variants':{'':{'model':f'{MOD}:block/{block}'}}})
  for lang in manifest['languages'].values():lang[f'block.{MOD}.{block}']='tile.ThaumicHorizons_'+('eye' if block=='eyes' else block)+'.name'
  if block=='void':continue
  drop={'bone':'minecraft:bone','flesh':'minecraft:bone','brain':'thaumcraft:zombie_brain','eyes':'minecraft:spider_eye'}.get(block,f'{MOD}:{block}')
  functions=[]
  if block!='dust':
   low,high=(2,3) if block=='brain' else (3,5)
   functions=[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':low,'max':high}}, {'function':'minecraft:apply_bonus','enchantment':'minecraft:fortune','formula':'minecraft:uniform_bonus_count','parameters':{'bonusMultiplier':1}}]
  write(f'data/{MOD}/loot_tables/blocks/{block}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':drop,'functions':functions}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(p,manifest)
