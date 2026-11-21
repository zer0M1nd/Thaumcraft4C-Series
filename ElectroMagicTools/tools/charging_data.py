from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def ingredient(value):return {'ingredient':value if isinstance(value,dict) else {'item':value},'damage':32767}
entries=[
 ('wand_charger','Industrial Wand Charging Station',-4,2,3,{'potentia':4,'fabrico':2,'permutatio':3,'lucrum':5},'emt:potentia_generator',6,{'potentia':48,'fabrico':48,'permutatio':48,'lucrum':64},'thaumcraft:wand_pedestal',['ic2:replicator','ic2:iridium','minecraft:glowstone','thaumcraft:entropy_shard','thaumcraft:essentia_jar'],'tile.EMT.machine.wandrecharge.name'),
 ('charge_focus','Wand Focus: Charging',5,3,2,{'permutatio':4,'potentia':5,'machina':4},'emt:foundation',4,{'permutatio':25,'potentia':32,'machina':32},'ic2:advanced_re_battery',['ic2:generator','ic2:batbox','emt:thaumium_plate','emt:thaumium_plate',{'tag':'emt:all_shards'},{'tag':'emt:all_shards'}],'item.EMT.focus.charge.name'),
 ('wand_charge_focus','Wand Focus: Wand Charging',7,3,3,{'potentia':5,'auram':4,'permutatio':4,'lucrum':5},'emt:charge_focus',5,{'potentia':48,'auram':32,'permutatio':48,'lucrum':64},'emt:wand_charger',['emt:charge_focus','ic2:energy_pack','ic2:iridium','ic2:iridium'],'item.EMT.focus.chargeWand.name')]
research=[];pages=[];aliases={}
for id,name,col,row,complexity,aspects,parent,instability,cost,center,parts,old in entries:
 key='emt:'+id
 write('data/emt/recipes/'+id+'.json',dict(type='thaumcraft:infusion',research=key,instability=instability,aspects=cost,center=ingredient(center),components=[ingredient(x) for x in parts],result={'item':key}))
 research.append(dict(key=key,category='EMT',displayColumn=col,displayRow=row,pageCount=2,complexity=complexity,aspects=aspects,parents=[parent],isConcealed=id!='wand_charger',icon_item={'item':key}))
 pages.append(dict(key=key,pages=[dict(type='TEXT',text='emt.text.'+name),dict(type='INFUSION_CRAFTING',recipe=dict(kind='reference',**{'class':'modern'},keys=[key]))]))
 aliases['item.emt.'+id]=old;aliases['block.emt.'+id]=old;aliases['tc.research_name.'+key]='emt.rsname.'+name;aliases['tc.research_text.'+key]='emt.tag.'+name
write('data/emt/research_catalog/charging.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=research)]))
write('data/emt/research_pages/charging.json',dict(format=1,entries=pages))
write('data/emt/tags/items/all_shards.json',dict(replace=False,values=['thaumcraft:'+x+'_shard' for x in ['air','fire','water','earth','order','entropy','balanced']]))
for id,texture in [('charge_focus','charge'),('wand_charge_focus','chargewand')]:write('assets/emt/models/item/'+id+'.json',dict(parent='minecraft:item/generated',textures={'layer0':'emt:items/focus_'+texture}))
write('assets/emt/models/block/wand_charger.json',dict(parent='minecraft:block/orientable',textures={'top':'emt:blocks/machines/top','bottom':'emt:blocks/machines/side','side':'emt:blocks/machines/side','front':'emt:blocks/machines/wandcharger'}))
write('assets/emt/blockstates/wand_charger.json',{'variants':{'facing='+face:{'model':'emt:block/wand_charger','y':rot} for face,rot in [('north',0),('east',90),('south',180),('west',270)]}})
write('assets/emt/models/item/wand_charger.json',dict(parent='emt:block/wand_charger'))
write('data/emt/loot_tables/blocks/wand_charger.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'ic2:machine'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
path=root/'data/emt/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data['languages']['assets/emt/lang/'+lang+'.json'].update(aliases)
write('data/emt/thaumcraft/original_resources.json',data)
exec((Path(__file__).parent/'energy_data.py').read_text(),{'__file__':str(Path(__file__).parent/'energy_data.py')})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
