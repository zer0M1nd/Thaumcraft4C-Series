"""Authored receptacle geometry; all old art stays in the external resource jar."""
import json,zipfile
from pathlib import Path
BASE=Path(__file__).resolve().parents[1];ROOT=BASE/'src/main/resources';MOD='thaumichorizons'
def write(path,value):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def cube(x,y,z,w,h,d,u,v,tint=False):
 uv={'west':[u,v+d,u+d,v+d+h],'north':[u+d,v+d,u+d+w,v+d+h],'east':[u+d+w,v+d,u+2*d+w,v+d+h],'south':[u+2*d+w,v+d,u+2*d+2*w,v+d+h],'up':[u+d,v,u+d+w,v+d],'down':[u+d+w,v,u+d+2*w,v+d]}
 return {'from':[x,y,z],'to':[x+w,y+h,z+d],'faces':{face:{'texture':'#body','uv':[n/4 for n in values],**({'tintindex':0} if tint else {})} for face,values in uv.items()}}
parts=[]
for x in [0,12]:
 for y in [0,12]:
  for z in [0,12]:parts.append(cube(x,y,z,4,4,4,0,0))
for y in [0,12]:
 for z in [0,12]:parts.append(cube(4,y,z,8,4,4,0,8))
for x in [0,12]:
 for z in [0,12]:parts.append(cube(x,4,z,4,8,4,0,16))
for x in [0,12]:
 for y in [0,12]:parts.append(cube(x,y,4,4,4,8,0,28))
for filled in [False,True]:write(f'assets/{MOD}/models/block/keystone_slot'+('_filled' if filled else '')+'.json',{'parent':'minecraft:block/block','textures':{'body':f'{MOD}:models/receptacle','particle':f'{MOD}:models/receptacle'},'elements':parts+([cube(4,4,4,8,8,8,24,0,True)] if filled else [])})
write(f'assets/{MOD}/blockstates/keystone_slot.json',{'variants':{'has_key=false':{'model':f'{MOD}:block/keystone_slot'},'has_key=true':{'model':f'{MOD}:block/keystone_slot_filled'}}})
write(f'assets/{MOD}/models/item/keystone_slot.json',{'parent':f'{MOD}:block/keystone_slot'})
write(f'data/{MOD}/loot_tables/blocks/keystone_slot.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:keystone_slot'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(f'assets/{MOD}/models/block/gateway_portal.json',{'render_type':'minecraft:translucent','textures':{'portal':'minecraft:block/nether_portal','particle':'minecraft:block/nether_portal'},'elements':[{'from':[0,0,7.75],'to':[16,16,8.25],'faces':{'north':{'texture':'#portal','uv':[0,0,16,16]},'south':{'texture':'#portal','uv':[0,0,16,16]}}}]})
write(f'assets/{MOD}/blockstates/gateway_portal.json',{'variants':{'axis=x':{'model':f'{MOD}:block/gateway_portal'},'axis=z':{'model':f'{MOD}:block/gateway_portal','y':90}}})
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=json.loads((ROOT/p).read_text(encoding='utf-8'))
with zipfile.ZipFile(BASE/'references/ThaumicHorizons-1.8.21.jar') as jar:
 paths=['textures/models/receptacle.png']+[f'textures/blocks/gateway_{part}.png' for part in ['topright','topleft','bottomright','bottomleft','top2','top','right','left','bottom']]
 for path in paths:
  full=f'assets/{MOD}/{path}';assert full in jar.namelist();manifest['mapping'][full]=full
for lang in manifest['languages'].values():lang[f'block.{MOD}.keystone_slot']='tile.ThaumicHorizons_slot.name';lang[f'block.{MOD}.gateway_portal']='tile.ThaumicHorizons_gateway.name'
write(p,manifest)
