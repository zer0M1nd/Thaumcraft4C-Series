"""Human effigy foundation; legacy textures remain in the player's external pack."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'; MOD='thaumichorizons'
def write(path,value):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1,'metadata':0,'damageable':False}}
components=['minecraft:rotten_flesh','minecraft:bone']*4
cost={'fabrico':8,'humanus':8}
write(f'assets/{MOD}/models/item/corpse_effigy.json',{'parent': 'builtin/entity', 'gui_light': 'front', 'textures': {'particle': 'thaumcraft:item/brain'}, 'display': {'gui': {'rotation': [10, -25, 0]}, 'ground': {'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]}, 'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 2, 1], 'scale': [0.65, 0.65, 0.65]}, 'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 2, 1], 'scale': [0.65, 0.65, 0.65]}, 'firstperson_righthand': {'rotation': [0, -20, 0], 'translation': [0, 1, 0]}, 'firstperson_lefthand': {'rotation': [0, 20, 0], 'translation': [0, 1, 0]}}})
write(f'data/{MOD}/recipes/infusion/corpse_effigy.json',{'type':'thaumcraft:infusion','research':'cloneVillager','instability':3,'aspects':cost,'center':{'ingredient':{'item':'thaumcraft:zombie_brain'}},'components':[{'ingredient':{'item':i}} for i in components],'result':{'item':f'{MOD}:corpse_effigy'}})
write(f'data/{MOD}/research_catalog/self.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[{'key':'cloneVillager','category':'ThaumicHorizons','displayColumn':11,'displayRow':4,'complexity':3,'aspects':[{'aspect':k,'amount':8} for k in ['spiritus','humanus','sano']],'parents':['incarnationVat','soulAssembler'],'siblings':['nightmare'],'isConcealed':True,'warp':3,'icon_resource':f'{MOD}:textures/misc/villager.png','pageCount':3}]}]})
write(f'data/{MOD}/research_pages/self.json',{'format':1,'entries':[{'key':'cloneVillager','pages':[{'type':'TEXT','text':'cloneVillager1'},{'type':'INFUSION_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.InfusionRecipe','keys':[f'{MOD}:corpse_effigy']}},{'type':'TEXT','text':'cloneVillager2'}]}]})
write(f'data/{MOD}/research_page_recipes/self.json',{'format':1,'entries':[{'key':f'{MOD}:corpse_effigy','class':'thaumcraft.api.crafting.InfusionRecipe','data':{'kind':'infusion','research':'cloneVillager','instability':3,'aspects':cost,'input':stack('thaumcraft:zombie_brain'),'output':stack(f'{MOD}:corpse_effigy'),'ingredients':{'kind':'sequence','elements':[stack(i) for i in components]}}}]})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=read(path)
manifest['mapping'][f'assets/{MOD}/textures/misc/villager.png']='assets/thaumichorizons/textures/misc/villager.png'
for skin in ['corpseeffigy','corpseeffigyrevived']:
    manifest['mapping'][f'assets/{MOD}/textures/models/{skin}.png']=f'assets/thaumichorizons/textures/models/{skin}.png'
for language in manifest['languages'].values():
    language[f'item.{MOD}.corpse_effigy']='item.corpseEffigy.name'
    for key in ['cloneVillager1','cloneVillager2','tc.research_name.cloneVillager','tc.research_text.cloneVillager']:language[key]=key
write(path,manifest)
for language,growing,ready in [('zh_cn','躯壳培养：%s tick','躯壳已就绪'),('en_us','Effigy incubation: %s ticks','Effigy ready')]:
    path=f'assets/{MOD}/lang/{language}.json';data=read(path);data.update({f'{MOD}.vat.effigy_growing':growing,f'{MOD}.vat.effigy_ready':ready});write(path,data)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
