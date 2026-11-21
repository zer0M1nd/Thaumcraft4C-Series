from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def ing(id):return {'ingredient':{'item':id},'damage':32767}
entries=[('explosion_focus','Explosion Focus','explosion',2,{'ignis':3,'mortuus':2,'telum':3},6,{'ignis':30,'motus':16,'telum':30,'mortuus':16},'thaumcraft:focus_hellbat',['ic2:mining_laser','minecraft:arrow','minecraft:gunpowder','minecraft:firework_star','thaumcraft:thaumium_ingot']),('energy_ball_focus','Energy Ball Focus','energyBall',1,{'potentia':10,'praecantatio':5,'victus':6},4,{'potentia':10,'praecantatio':5,'victus':6},'thaumcraft:focus_shock',['minecraft:tnt']+['ic2:advanced_re_battery']*3)]
research=[];pages=[];aliases={}
for id,name,texture,x,aspects,instability,cost,center,parts in entries:
 key='emt:'+id
 # Author and GTNH have a mismatched, otherwise nonexistent recipe research key.
 requirement='emt:legacy_wand_focus_energy_ball' if id=='energy_ball_focus' else key
 write('data/emt/recipes/'+id+'.json',dict(type='thaumcraft:infusion',research=requirement,instability=instability,aspects=cost,center=ing(center),components=[ing(v) for v in parts],result={'item':key}))
 write('assets/emt/models/item/'+id+'.json',dict(parent='minecraft:item/generated',textures={'layer0':'emt:items/focus_'+texture.lower()}))
 research.append(dict(key=key,category='EMT',displayColumn=x,displayRow=3,pageCount=2,complexity=2,aspects=aspects,parents=['emt:foundation'],parentsHidden=['INFUSION'],isConcealed=True,icon_item={'item':key}))
 pages.append(dict(key=key,pages=[dict(type='TEXT',text='emt.text.'+name),dict(type='INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[key]))]))
 aliases['item.emt.'+id]='item.EMT.focus.'+texture+'.name';aliases['tc.research_name.'+key]='emt.rsname.'+name;aliases['tc.research_text.'+key]='emt.tag.'+name
key='emt:christmas_focus';name='Kris-tmas Focus'
write('assets/emt/models/item/christmas_focus.json',dict(parent='minecraft:item/generated',textures={'layer0':'emt:items/focus_christmas'}))
write('data/emt/recipes/christmas_focus.json',dict(type='thaumcraft:arcane',mode='shaped',research=key,pattern=['XYX','YZY','XYX'],key={'X':{'item':'minecraft:snow_block'},'Y':{'item':'minecraft:pumpkin'},'Z':{'item':'thaumcraft:focus_frost'}},vis={'ordo':20,'aer':20},result={'item':key}))
research.append(dict(key=key,category='EMT',displayColumn=3,displayRow=3,pageCount=2,complexity=1,aspects={'gelum':2,'bestia':5,'victus':6,'mortuus':2},parents=['emt:foundation'],isSecondary=True,isConcealed=True,icon_item={'item':key}))
pages.append(dict(key=key,pages=[dict(type='TEXT',text='emt.text.'+name),dict(type='ARCANE_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[key]))]))
aliases['item.emt.christmas_focus']='item.EMT.focus.christmas.name';aliases['tc.research_name.'+key]='emt.rsname.'+name;aliases['tc.research_text.'+key]='emt.tag.'+name
write('data/emt/research_catalog/attack_focus.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=research)]))
write('data/emt/research_pages/attack_focus.json',dict(format=1,entries=pages))
path=root/'data/emt/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/emt/lang/'+lang+'.json'].update(aliases)
write('data/emt/thaumcraft/original_resources.json',data)
exec((Path(__file__).parent/'energy_data.py').read_text(),{'__file__':str(Path(__file__).parent/'energy_data.py')})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
