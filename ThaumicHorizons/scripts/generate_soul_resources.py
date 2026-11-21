"""Soul contents share the existing jar block and retain external animation assets."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,value):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(path);manifest['mapping'][f'assets/{MOD}/textures/misc/soul.png']=f'assets/thaumichorizons/textures/misc/soul.png';write(path,manifest)
for language,name,jar in [('zh_cn','灵魂','装有灵魂的罐子'),('en_us','Soul','Soul in a Jar')]:
    path=f'assets/{MOD}/lang/{language}.json';data=read(path);data.update({f'entity.{MOD}.soul':name,f'{MOD}.jar.soul':jar});write(path,data)
write(f'data/{MOD}/loot_tables/entities/soul.json',{'type':'minecraft:entity','pools':[]})
