import json,zipfile
from pathlib import Path
BASE=Path(__file__).resolve().parents[1];ROOT=BASE/'src/main/resources';MOD='thaumichorizons'
def write(path,value):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=f'data/{MOD}/thaumcraft/original_resources.json';manifest=json.loads((ROOT/p).read_text(encoding='utf-8'))
with zipfile.ZipFile(BASE/'references/ThaumicHorizons-1.8.21.jar') as jar:
 for kind in ['meat','mercurial','volt','med']:
  texture=f'assets/{MOD}/textures/entity/{kind}slime.png';assert texture in jar.namelist();manifest['mapping'][texture]=texture
  for lang in manifest['languages'].values():
   lang[f'entity.{MOD}.{kind}_slime']=f'entity.ThaumicHorizons.{kind.title()}Slime.name'
   lang[f'item.{MOD}.{kind}_slime_spawn_egg']=f'entity.ThaumicHorizons.{kind.title()}Slime.name'
  write(f'assets/{MOD}/models/item/{kind}_slime_spawn_egg.json',{'parent':'minecraft:item/template_spawn_egg'})
write(p,manifest)
