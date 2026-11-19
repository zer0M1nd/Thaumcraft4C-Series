from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
research='gadomancy:armor_disguise'
write('data/gadomancy/recipes/armor_disguise.json',dict(type=research,research=research,instability=0,center={'ingredient':{'type':'gadomancy:disguise_material'},'damage':-1},components=[{'ingredient':{'item':item}} for item in ['minecraft:slime_ball']*2+['thaumcraft:quicksilver']*2]+[{'ingredient':{'type':'gadomancy:disguise_material'},'damage':-1}],aspects={'limus':12,'tutamen':10,'praecantatio':8},result={'patch':'{gadomancy:{disguise:1b}}'}))
write('data/gadomancy/research_catalog/armor_disguise.json',dict(format=1,categories=[dict(key='GADOMANCY',icon='gadomancy:textures/items/category_icon.png',background='gadomancy:textures/gui/gui_researchback.png',research=[dict(key=research,category='GADOMANCY',displayColumn=-2,displayRow=0,pageCount=4,complexity=1,aspects={'limus':8,'praecantatio':4,'tutamen':4},parents=['INFUSIONENCHANTMENT'],isConcealed=True,icon_item={'item':'minecraft:iron_chestplate'})])]))
write('data/gadomancy/research_pages/armor_disguise.json',dict(format=1,entries=[dict(key=research,pages=[dict(type='TEXT',text='gadomancy.research_page.ARMORDISGUISE.1'),dict(type=research,data={'invisible':False}),dict(type=research,data={'invisible':True}),dict(type='TEXT',text='gadomancy.research_page.ARMORDISGUISE.4')])]))
path=root/'data/gadomancy/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:
 for kind in ['name','text']:data['languages']['assets/gadomancy/lang/'+lang+'.json']['tc.research_'+kind+'.'+research]='tc.research_'+kind+'.GADOMANCY.ARMORDISGUISE'
write('data/gadomancy/thaumcraft/original_resources.json',data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
