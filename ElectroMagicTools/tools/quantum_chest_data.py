"""Author 1.2.6.2 chestplate/shield recipes and external-resource model references."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,value):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def item(name):return {'item':name}
recipes={
 'shield':dict(type='thaumcraft:arcane',mode='shaped',research='emt:shield',pattern=['XYX','X X','XYX'],key={'X':item('minecraft:glass'),'Y':item('minecraft:obsidian')},vis={'ordo':1,'perditio':1},result=dict(item='emt:shield',count=6)),
 'infused_quantum_chestplate':dict(type='thaumcraft:infusion',research='emt:infused_quantum_chestplate',instability=5,aspects={'volatus':48,'aer':32,'machina':48,'potentia':32,'tutamen':48},center={'ingredient':item('ic2:quantum_chestplate'),'damage':32767},components=[{'ingredient':item(v)} for v in ['minecraft:diamond','ic2:iridium','ic2:iridium','ic2:iridium','emt:shield','emt:shield']],result=dict(item='emt:infused_quantum_chestplate',nbt='{charge:10.0d}'))}
for name,value in recipes.items():write('data/emt/recipes/'+name+'.json',value)
write('assets/emt/blockstates/shield.json',{'variants':{'':{'model':'emt:block/shield'}}})
write('assets/emt/models/block/shield.json',{'parent':'minecraft:block/cube_all','render_type':'minecraft:cutout','textures':{'all':'emt:blocks/shield'}})
write('assets/emt/models/item/shield.json',{'parent':'emt:block/shield'})
write('data/emt/loot_tables/blocks/shield.json',{'type':'minecraft:block','pools':[]})
overrides=[]
for n,suffix in [(1,'jetpack'),(2,'wing_t'),(3,'wing_n'),(4,'wing_q')]:
 name='infused_quantum_chestplate_'+suffix
 write('assets/emt/models/item/'+name+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'emt:items/armor/armor_quantum_chest_'+suffix}})
 overrides.append({'predicate':{'emt:attachment':n},'model':'emt:item/'+name})
write('assets/emt/models/item/infused_quantum_chestplate.json',{'parent':'minecraft:item/generated','textures':{'layer0':'emt:items/armor/armor_quantum_chest'},'overrides':overrides})
entries=[];pages=[];aliases={}
for id,name,x,y,complexity,aspects,parent in [('shield','Shield Block',4,5,1,{'tutamen':3,'vinculum':2},'shield_focus'),('infused_quantum_chestplate','Infused Quantum Armor',0,10,4,{'volatus':5,'aer':5,'machina':8,'potentia':4},'quantum_wings')]:
 key='emt:'+id
 # TC4 ResearchItem clamps the requested complexity to [0,3] in its constructor.
 entries.append(dict(key=key,category='EMT',displayColumn=x,displayRow=y,pageCount=2,complexity=min(3,max(0,complexity)),aspects=aspects,parents=['emt:'+parent],isSecondary=id=='shield',isConcealed=True,icon_item=item(key)))
 pages.append(dict(key=key,pages=[dict(type='TEXT',text='emt.text.'+name),dict(type='ARCANE_CRAFTING' if id=='shield' else 'INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[key]))]))
 aliases['tc.research_name.'+key]='emt.rsname.'+name;aliases['tc.research_text.'+key]='emt.tag.'+name
aliases['block.emt.shield']='tile.EMT.shield.name';aliases['item.emt.infused_quantum_chestplate']='item.EMT.chestplates.infusedQuantum.name'
write('data/emt/research_catalog/quantum_chest.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=entries)]))
write('data/emt/research_pages/quantum_chest.json',dict(format=1,entries=pages))
path=root/'data/emt/thaumcraft/original_resources.json';mapping=json.loads(path.read_text(encoding='utf8'))
for lang in ['en_us','zh_cn']:mapping['languages']['assets/emt/lang/'+lang+'.json'].update(aliases)
write('data/emt/thaumcraft/original_resources.json',mapping)
texts={'en_us':{'message.emt.attachment_installed':'%s installed.','message.emt.attachment_removed':'%s removed.','message.emt.hover_on':'Hover Mode enabled.','message.emt.hover_off':'Hover Mode disabled.'},'zh_cn':{'message.emt.attachment_installed':'已装入%s。','message.emt.attachment_removed':'已卸下%s。','message.emt.hover_on':'悬浮模式已启用。','message.emt.hover_off':'悬浮模式已关闭。'}}
for lang,text in texts.items():
 path=root/('assets/emt/lang/'+lang+'.json');existing=json.loads(path.read_text(encoding='utf8')) if path.exists() else {};existing.update(text);write('assets/emt/lang/'+lang+'.json',existing)
def displaystack(value):
 if value is None:return dict(kind='sequence',elements=[])
 if isinstance(value,list):return dict(kind='sequence',elements=[displaystack(v) for v in value])
 value=value.get('ingredient',value);return dict(kind='stack',stack=dict(value,count=value.get('count',1)))
display=[]
for name,r in recipes.items():
 d={'output':displaystack(r['result']),'research':r['research']}
 if r['type']=='thaumcraft:infusion':d.update(kind='infusion',input=displaystack(r['center']),ingredients=displaystack(r['components']),aspects=r['aspects'],instability=r['instability'])
 else:d.update(kind='arcane_shaped',width=3,height=3,ingredients=displaystack([r['key'].get(c) for row in r['pattern'] for c in row]),aspects=r['vis'])
 display.append({'key':'emt:'+name,'class':'modern','data':d})
display_path=root/'data/emt/research_page_recipes/main.json'
all_display=json.loads(display_path.read_text(encoding='utf8')) if display_path.exists() else dict(format=1,entries=[])
keys={entry['key'] for entry in display}
all_display['entries']=[entry for entry in all_display['entries'] if entry['key'] not in keys]+display
write('data/emt/research_page_recipes/main.json',all_display)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
