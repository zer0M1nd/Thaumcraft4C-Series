from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
recipes=[];research=[];pages=[];aliases={}
for kind,center,motus,energy,components,column,complexity in [
 ('electric','thaumcraft:traveller_boots',32,16,['minecraft:diamond','ic2:electric_motor','ic2:coil','ic2:rubber_boots','ic2:advanced_re_battery'],2,1),
 ('nano','emt:electric_traveller_boots',48,25,['minecraft:diamond','ic2:nano_boots','ic2:energy_crystal'],4,2),
 ('quantum','emt:nano_traveller_boots',64,32,['minecraft:redstone_block','ic2:quantum_boots','ic2:lapotron_crystal','ic2:iridium'],6,3)]:
 id=kind+'_traveller_boots';key='emt:'+id;name=kind.title()+' Boots of the Traveller';parent={'electric':'foundation','nano':'electric_traveller_boots','quantum':'nano_traveller_boots'}[kind]
 recipe=dict(type='thaumcraft:infusion',research=key,instability=2,aspects={'potentia':energy,'tutamen':32,'motus':motus},center={'ingredient':{'item':center},'damage':32767},components=[{'ingredient':{'item':i},'damage':32767} for i in components],result={'item':key,'nbt':'{charge:10.0d}'})
 write('data/emt/recipes/'+id+'.json',recipe)
 write('assets/emt/models/item/'+id+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'emt:items/armor/boots_'+kind}})
 research.append(dict(key=key,category='EMT',displayColumn=column,displayRow=0,pageCount=2,complexity=complexity,aspects={'potentia':complexity+1,'tutamen':5,'motus':complexity+1},parents=['emt:'+parent],isConcealed=True,isSecondary=kind=='electric',icon_item={'item':key}))
 pages.append(dict(key=key,pages=[dict(type='TEXT',text='emt.text.'+name),dict(type='INFUSION_CRAFTING',recipe={'kind':'reference','class':'modern','keys':[key]})]))
 aliases['item.emt.'+id]='item.EMT.boots.traveller.'+kind+'.name';aliases['tc.research_name.'+key]='emt.rsname.'+name;aliases['tc.research_text.'+key]='emt.tag.'+name
write('data/emt/research_catalog/boots.json',dict(format=1,categories=[dict(key='EMT',icon='emt:textures/misc/emt.png',background='thaumcraft:textures/gui/gui_researchback.png',research=research)]))
write('data/emt/research_pages/boots.json',dict(format=1,entries=pages))
path=root/'data/emt/thaumcraft/original_resources.json';data=json.loads(path.read_text())
for lang in ['en_us','zh_cn']:data.setdefault('languages',{}).setdefault('assets/emt/lang/'+lang+'.json',{}).update(aliases)
write('data/emt/thaumcraft/original_resources.json',data)
exec((Path(__file__).parent/'energy_data.py').read_text(),{'__file__':str(Path(__file__).parent/'energy_data.py')})

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
