import json,zipfile
from pathlib import Path
BASE=Path(__file__).resolve().parents[1];ROOT=BASE/'src/main/resources';MOD='thaumichorizons'
def read(p):return json.loads((ROOT/p).read_text(encoding='utf-8'))
def write(p,v):
 p=ROOT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(p)
with zipfile.ZipFile(BASE/'references/ThaumicHorizons-1.8.21.jar') as jar:
 for name,texture,translation in [('node_cheat','nodecheat','item.nodeCheat.name'),('suicide_pill','syringeEmpty','item.suicidePill.name')]:
  old=f'assets/{MOD}/textures/items/{texture}.png';assert old in jar.namelist();manifest['mapping'][old]=old
  write(f'assets/{MOD}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:items/{texture}'}})
  for lang in manifest['languages'].values():lang[f'item.{MOD}.{name}']=translation
for name,old in [('chromatic_sheep','ChromaticSheep'),('self_shearing_sheep','SelfShearingSheep'),('guardian_panther','GuardianPanther'),('gravekeeper','Gravekeeper'),('familiar','Familiar'),('chocolate_cow','ChocolateCow'),('scholar_chicken','ScholarChicken'),('gold_chicken','GoldChicken'),('ore_pig','OrePig'),('taint_eater_pig','TaintEaterPig'),('endersteed','Endersteed'),('seawolf','Seawolf'),('nether_hound','NetherHound'),('lunar_wolf','LunarWolf'),('undead_horse','HorseZombie'),('skeletal_horse','HorseSkeleton'),('nightmare','NightmareTH'),('sheeder','Sheeder'),('soul','LostSoul')]:
 write(f'assets/{MOD}/models/item/{name}_spawn_egg.json',{'parent':'minecraft:item/template_spawn_egg'})
 for lang in manifest['languages'].values():lang[f'item.{MOD}.{name}_spawn_egg']=f'entity.ThaumicHorizons.{old}.name'
write(p,manifest)
write(f'data/{MOD}/damage_type/suicide.json',{'message_id':f'{MOD}.suicide','scaling':'never','exhaustion':0})
write('data/minecraft/tags/damage_type/bypasses_invulnerability.json',{'replace':False,'values':[f'{MOD}:suicide']})
for lang,values in {'en_us':{'node_cheat.tip':'Testing tool: transforms an aura node into a self-stabilized rift.','suicide_pill.tip':'Testing tool: deals fatal damage to your current body, including in Creative mode. The item is reusable.','death':'%1$s ended their current body in a test'},'zh_cn':{'node_cheat.tip':'测试工具：将灵气节点转化为自稳定裂隙。','suicide_pill.tip':'测试工具：对当前躯体造成致命伤害，创造模式也生效。物品可重复使用。','death':'%1$s 在测试中终止了当前躯体'}}.items():
 path=f'assets/{MOD}/lang/{lang}.json';data=read(path)
 for key,value in values.items():data[f'death.attack.{MOD}.suicide' if key=='death' else f'{MOD}.{key}']=value
 write(path,data)
