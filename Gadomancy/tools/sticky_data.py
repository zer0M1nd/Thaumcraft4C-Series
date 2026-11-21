from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def stack(item,nbt=None):
 s=dict(item=item,count=1)
 if nbt:s['nbt']=nbt
 return dict(kind='stack',stack=s)
key='gadomancy:sticky_jar';recipes=[]
for jar in ['essentia_jar','filled_essentia_jar','void_jar','filled_void_jar']:
 name='sticky_'+jar;output=dict(item='thaumcraft:'+jar,nbt='{isStickyJar:1b}')
 write('data/gadomancy/recipes/'+name+'.json',dict(type='gadomancy:sticky_jar',mode='shaped',research=key,pattern=['J','S'],key={'J':{'item':'thaumcraft:'+jar},'S':{'item':'minecraft:slime_ball'}},vis={'aqua':10,'terra':10},result=output))
 recipes.append(dict(key='gadomancy:'+name,**{'class':'modern'},data=dict(kind='arcane_shaped',research=key,aspects={'aqua':10,'terra':10},width=1,height=2,output=stack('thaumcraft:'+jar,output['nbt']),ingredients=dict(kind='sequence',elements=[stack('thaumcraft:'+jar),stack('minecraft:slime_ball')]))))
write('data/gadomancy/research_catalog/sticky.json',dict(format=1,categories=[dict(key='GADOMANCY',icon='gadomancy:textures/items/category_icon.png',background='gadomancy:textures/gui/gui_researchback.png',research=[dict(key=key,category='GADOMANCY',displayColumn=-5,displayRow=-5,pageCount=2,complexity=2,aspects={'limus':8,'terra':8},parents=['JARLABEL'],isConcealed=True,icon_item=dict(item='thaumcraft:essentia_jar',nbt='{isStickyJar:1b}'))])]))
write('data/gadomancy/research_pages/sticky.json',dict(format=1,entries=[dict(key=key,pages=[dict(type='TEXT',text='gadomancy.research_page.STICKYJAR.1'),dict(type='ARCANE_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[r['key'] for r in recipes]))])]))
write('data/gadomancy/research_page_recipes/sticky.json',dict(format=1,entries=recipes))
write('assets/gadomancy/blockstates/sticky_jar.json',dict(variants={'':{'model':'gadomancy:block/sticky_jar'}}))
write('assets/gadomancy/models/block/sticky_jar.json',dict(textures={'particle':'thaumcraft:block/jar_side'}))
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/gadomancy/lang/'+lang+'.json'].update({'tc.research_name.'+key:'tc.research_name.GADOMANCY.STICKYJAR','tc.research_text.'+key:'tc.research_text.GADOMANCY.STICKYJAR'})
write('data/gadomancy/thaumcraft/original_resources.json',data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
