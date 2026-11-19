from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
key='gadomancy:remote_jar'
write('data/gadomancy/recipes/remote_jar.json',dict(type='gadomancy:remote_jar',mode='shaped',research=key,pattern=['GJG','GMG'],key={'G':{'item':'thaumcraft:greatwood_log'},'J':{'item':'thaumcraft:essentia_jar'},'M':{'item':'thaumcraft:essentia_mirror'}},vis={'aqua':10,'terra':10,'ordo':10},result={'item':key}))
write('data/gadomancy/recipes/sticky_remote_jar.json',dict(type='gadomancy:sticky_jar',mode='shaped',research='gadomancy:sticky_jar',pattern=['J','S'],key={'J':{'item':key},'S':{'item':'minecraft:slime_ball'}},vis={'aqua':10,'terra':10},result={'item':key,'nbt':'{isStickyJar:1b}'}))
write('data/gadomancy/research_catalog/remote_jar.json',dict(format=1,categories=[dict(key='GADOMANCY',icon='gadomancy:textures/items/category_icon.png',background='gadomancy:textures/gui/gui_researchback.png',research=[dict(key=key,category='GADOMANCY',displayColumn=-4,displayRow=-3,pageCount=3,complexity=3,aspects={'aqua':4,'machina':8,'terra':4,'ordo':8},parents=['JARLABEL'],isConcealed=True,icon_item={'item':key})])]))
write('data/gadomancy/research_pages/remote_jar.json',dict(format=1,entries=[dict(key=key,pages=[dict(type='TEXT',text='gadomancy.research_page.REMOTEJAR.1'),dict(type='gadomancy:remote_jar_recipe'),dict(type='TEXT',text='gadomancy.research_page.REMOTEJAR.2')])]))
write('assets/gadomancy/blockstates/remote_jar.json',dict(variants={'':{'model':'gadomancy:block/remote_jar'}}))
write('assets/gadomancy/models/block/remote_jar.json',dict(parent='thaumcraft:block/essentia_jar',textures={'bottom':'gadomancy:blocks/transparent','particle':'gadomancy:blocks/jar_remote_side','side':'gadomancy:blocks/jar_remote_side','top':'gadomancy:blocks/jar_remote_top'}))
write('assets/gadomancy/models/item/remote_jar.json',dict(parent='thaumcraft:item/essentia_jar',textures={'particle':'gadomancy:blocks/jar_remote_side'}))
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/gadomancy/lang/'+lang+'.json'].update({'block.gadomancy.remote_jar':'tile.BlockRemoteJar.name','tc.research_name.'+key:'tc.research_name.GADOMANCY.REMOTEJAR','tc.research_text.'+key:'tc.research_text.GADOMANCY.REMOTEJAR'})
write('data/gadomancy/thaumcraft/original_resources.json',data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
