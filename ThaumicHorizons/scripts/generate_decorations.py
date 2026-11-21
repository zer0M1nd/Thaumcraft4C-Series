import json,zipfile
from pathlib import Path
BASE=Path(__file__).resolve().parents[1];ROOT=BASE/'src/main/resources';MOD='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,v):
 if ('/item_aspects/' in p or '/item_bonus_aspects/' in p) and (ROOT / 'data/thaumichorizons/item_aspects/explicit.json').exists(): return
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(p)
colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
def drops(name):write(f'data/{MOD}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
with zipfile.ZipFile(BASE/'references/ThaumicHorizons-1.8.21.jar') as jar:
 def external(new,old):
  assert old in jar.namelist(),old
  manifest['mapping'][new]=old
  if old+'.mcmeta' in jar.namelist():manifest['mapping'][new+'.mcmeta']=old+'.mcmeta'
 for i,color in enumerate(colors+['deep']):
  name='deep_crystal' if color=='deep' else f'crystal_{color}'
  suffix=''.join(part.title() for part in color.split('_'))
  external(f'assets/{MOD}/textures/block/{name}.png',f'assets/{MOD}/textures/blocks/blockCrystal{suffix}.png')
  write(f'assets/{MOD}/models/block/{name}.json',{'parent':'minecraft:block/cube_all','render_type':'minecraft:translucent','textures':{'all':f'{MOD}:block/{name}'}})
  write(f'assets/{MOD}/models/item/{name}.json',{'parent':f'{MOD}:block/{name}'})
  write(f'assets/{MOD}/blockstates/{name}.json',{'variants':{'':{'model':f'{MOD}:block/{name}'}}});drops(name)
  for lang in manifest['languages'].values():lang[f'block.{MOD}.{name}']='tile.ThaumicHorizons_deepcrystal.name' if color=='deep' else f'tile.ThaumicHorizons_crystal.{i}.name'
 external(f'assets/{MOD}/models/spike.obj',f'assets/{MOD}/textures/models/spike.obj')
 for kind,old,particle in [('metal','spike','minecraft:block/iron_block'),('wooden','spikeWood','thaumcraft:block/greatwoodtop'),('tooth','spikeTooth',f'{MOD}:block/bone')]:
  name=f'{kind}_spike';texture=f'assets/{MOD}/textures/models/{kind}spike.png';external(texture,texture)
  write(f'assets/{MOD}/models/block/{name}.json',{'textures':{'particle':particle},'elements':[]})
  write(f'assets/{MOD}/models/item/{name}.json',{'parent':'minecraft:builtin/entity','textures':{'particle':particle},'display':{'ground':{'scale':[.5,.5,.5]},'fixed':{'rotation':[0,180,0],'scale':[.6,.6,.6]},'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.4,.4,.4]},'firstperson_righthand':{'rotation':[0,45,0],'scale':[.65,.65,.65]}}})
  write(f'assets/{MOD}/blockstates/{name}.json',{'variants':{f'facing={face}':{'model':f'{MOD}:block/{name}'} for face in ['up','down','north','south','east','west']}});drops(name)
  for lang in manifest['languages'].values():lang[f'block.{MOD}.{name}']=f'tile.ThaumicHorizons_{old}.name'
write(p,manifest)
entries=[([f'crystal_{c}' for c in colors],{'vitreus':2}),(['deep_crystal'],{'vitreus':2,'lux':2,'permutatio':1}),(['metal_spike'],{'telum':3,'metallum':3}),(['wooden_spike'],{'telum':3,'arbor':3}),(['tooth_spike'],{'telum':3,'bestia':3}),(['void'],{'vacuos':4,'vinculum':4}),(['dust'],{'perditio':2}),(['brain'],{'cognitio':4,'corpus':4}),(['eyes'],{'sensus':2,'bestia':1}),(['bone'],{'mortuus':2}),(['flesh'],{'corpus':2}),(['vortex','vortex_stable'],{'auram':16,'vacuos':16})]
write(f'data/{MOD}/item_aspects/plane_environment.json',{'format':1,'entries':[{'items':[f'{MOD}:{name}' for name in names],'aspects':[{'aspect':a,'amount':n} for a,n in aspects.items()]} for names,aspects in entries]})
