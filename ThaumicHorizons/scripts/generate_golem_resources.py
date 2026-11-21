"""Golem definitions; legacy artwork is referenced, not copied into this repository's assets."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
MOD='thaumichorizons'
def write(path,data):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
stats={
 'grass':(10,0,.38,False,2,1,75,0,750), 'dirt':(10,0,.38,False,1,1,75,0,500),
 'wood':(20,6,.35,False,2,4,75,1,750), 'plant':(10,0,.4,False,2,1,75,0,750),
 'metal':(35,15,.31,True,2,32,125,4,1250), 'cloth':(10,0,.4,False,2,1,75,0,750),
 'sand':(10,0,.4,False,1,1,75,0,500), 'redstone':(20,9,.35,True,2,8,45,2,1000),
 'tnt':(10,0,.38,False,1,1,75,0,750), 'ice':(20,6,.35,False,2,4,75,1,750),
 'cactus':(20,6,.35,False,2,4,75,1,1000), 'clay':(25,9,.33,True,1,8,100,2,750),
 'cake':(20,6,.35,False,2,4,25,1,750), 'web':(15,6,.35,False,2,4,40,1,1000),
 'void':(25,15,.4,True,2,8,20,3,0),'rock':(30,12,.32,True,2,16,100,3,1000),
}
blocks={'grass':['grass_block','mycelium','podzol'], 'dirt':['dirt','coarse_dirt','rooted_dirt','mud'],
 'wood':['pumpkin','carved_pumpkin','jack_o_lantern','melon','bookshelf','crafting_table','note_block','brown_mushroom_block','red_mushroom_block','mushroom_stem'],
 'cloth':['sponge','wet_sponge'], 'sand':['sand','red_sand','gravel','suspicious_sand','suspicious_gravel'],
 'redstone':['redstone_lamp','redstone_block'], 'tnt':['tnt'], 'ice':['ice','packed_ice','blue_ice','snow_block'],
 'cactus':['cactus'],'cake':['cake'],'web':['cobweb']}
tags={'wood':['minecraft:logs','minecraft:planks'], 'plant':['minecraft:leaves'], 'cloth':['minecraft:wool','minecraft:wool_carpets'],
 'metal':['forge:storage_blocks/iron','forge:storage_blocks/gold','forge:storage_blocks/copper','forge:storage_blocks/netherite','forge:storage_blocks/thaumium','forge:storage_blocks/void','minecraft:anvil']}
fields=['health','armor','speed','fire_resistant','upgrades','carry','regeneration_delay','strength','vis_cost']
profiles=[dict(id=name,**dict(zip(fields,values)),sinks=name in ['rock','metal','redstone'],blocks=['minecraft:'+n for n in blocks.get(name,[])],tags=tags.get(name,[])) for name,values in stats.items()]
write(f'data/{MOD}/golem_profiles.json',profiles)
write(f'data/{MOD}/tags/blocks/animation_whitelist.json',{'replace':False,'values':['minecraft:cake','minecraft:cactus','minecraft:glass','minecraft:packed_ice','minecraft:ice','minecraft:cobweb','thaumcraft:amber_block','thaumcraft:greatwood_planks','thaumcraft:silverwood_planks']})
for name,texture in [('golem_powder','golempowder'),('focus_animation','focus_animation'),('golem_placer','golem'),('golem_bell','newbell')]:
    write(f'assets/{MOD}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:items/{texture}'}})
write(f'data/{MOD}/recipes/arcane/golem_bell.json',{'type':'thaumcraft:arcane','research':'GOLEMBELL','pattern':['QQ ','QQ ','  S'],'key':{'Q':{'item':'minecraft:quartz'},'S':{'tag':'forge:rods/wooden'}},'vis':{'ordo':5},'result':{'item':f'{MOD}:golem_bell'}})
def stack(item):return {'kind':'stack','stack':{'item':item,'count':1,'metadata':0,'damageable':False}}
bell={'key':'alternateGolemBell','category':'ThaumicHorizons','displayColumn':-2,'displayRow':0,'complexity':1,'aspects':[{'aspect':a,'amount':n} for a,n in {'ordo':2,'potentia':2,'vitreus':3}.items()],'siblings':['GOLEMBELL'],'isConcealed':True,'icon_item':{'item':f'{MOD}:golem_bell'},'pageCount':2}
write(f'data/{MOD}/research_catalog/golems.json',{'format':1,'categories':[{'key':'ThaumicHorizons','icon':f'{MOD}:textures/misc/vat.png','background':'thaumcraft:textures/gui/gui_researchback.png','research':[bell]}]})
write(f'data/{MOD}/research_pages/golems.json',{'format':1,'entries':[{'key':'alternateGolemBell','pages':[{'type':'TEXT','text':'alternateGolemBell1'},{'type':'ARCANE_CRAFTING','recipe':{'kind':'reference','class':'thaumcraft.api.crafting.ShapedArcaneRecipe','keys':[f'{MOD}:alternateGolemBell']}}]}]})
write(f'data/{MOD}/research_page_recipes/golems.json',{'format':1,'entries':[{'key':f'{MOD}:alternateGolemBell','class':'thaumcraft.api.crafting.ShapedArcaneRecipe','data':{'kind':'arcane_shaped','research':'GOLEMBELL','aspects':{'ordo':5},'width':3,'height':3,'mirrored':True,'output':stack(f'{MOD}:golem_bell'),'ingredients':{'kind':'sequence','elements':[stack(i) if i else None for i in ['minecraft:quartz','minecraft:quartz',None,'minecraft:quartz','minecraft:quartz',None,None,None,'minecraft:stick']]}}}]})
write(f'data/{MOD}/thaumcraft/focus_upgrades.json',[{'key':f'{MOD}:{name}','aspects':['telum'],'texture':f'{MOD}:textures/foci/{name}.png','name':f'focus.upgrade.{name}.name','description':f'focus.upgrade.{name}.text','foci':[f'{MOD}:focus_animation'],'ranks':[rank]} for name,rank in [('berserk',3),('detonation',5)]])
write(f'data/{MOD}/recipes/crucible/golem_powder.json',{'type':'thaumcraft:crucible','research':'golemPowder','aspects':{'humanus':4,'spiritus':4,'motus':4},'catalyst':{'item':'thaumcraft:salis_mundus'},'result':{'item':f'{MOD}:golem_powder'}})
manifest_path=f'data/{MOD}/thaumcraft/original_resources.json'
manifest=json.loads((ROOT/manifest_path).read_text(encoding='utf-8'))
for path in ['textures/items/golempowder.png','textures/items/focus_animation.png','textures/items/golem.png','textures/items/newbell.png','textures/models/golem_void.png','textures/foci/berserk.png','textures/foci/detonation.png']:
    manifest['mapping'][f'assets/{MOD}/{path}']=f'assets/thaumichorizons/{path}'
for keys in manifest['languages'].values():
    keys[f'item.{MOD}.golem_powder']='item.golemPowder.name';keys[f'item.{MOD}.focus_animation']='item.focusAnimation.name'
    keys[f'item.{MOD}.golem_placer']='item.golemPlacer.name';keys[f'item.{MOD}.golem_bell']='item.GolemBellTH.name'
    for key in ['alternateGolemBell1','tc.research_name.alternateGolemBell','tc.research_text.alternateGolemBell']+[f'thaumichorizons.golemWarning{i}' for i in range(1,4)]:keys[key]=key
    for name in ['berserk','detonation']:
        for suffix in ['name','text']:
            key=f'focus.upgrade.{name}.{suffix}';keys[key]=key
write(manifest_path,manifest)
for language,text in [('en_us','%s Golem'),('zh_cn','%s傀儡')]:
    write(f'assets/{MOD}/lang/{language}.json',{f'entity.{MOD}.block_golem':text,f'entity.{MOD}.void_golem':'虚空傀儡' if language=='zh_cn' else 'Voidling'})

# Complete the animation acquisition chain and the non-destructive advanced construct infusion.
def read(path):return json.loads((ROOT/path).read_text(encoding='utf-8'))
components=['thaumcraft:golem_core_blank',f'{MOD}:human_blood','thaumcraft:golem_upgrade_air','thaumcraft:golem_upgrade_fire','thaumcraft:golem_upgrade_order','thaumcraft:golem_core_blank',f'{MOD}:human_blood','thaumcraft:golem_upgrade_earth','thaumcraft:golem_upgrade_water','thaumcraft:golem_upgrade_entropy']
focus={'type':'thaumcraft:infusion','research':'focusAnimation','instability':10,'aspects':{'humanus':64,'motus':64,'spiritus':64,'fabrico':64},'center':{'ingredient':{'item':'thaumcraft:primordial_pearl'}},'components':[{'ingredient':{'item':i}} for i in components],'result':{'item':f'{MOD}:focus_animation'}}
write(f'data/{MOD}/recipes/infusion/focus_animation.json',focus)
advanced={'type':'thaumcraft:infusion','research':'focusAnimation','instability':3,'aspects':{'cognitio':8,'sensus':8,'victus':8},'center':{'ingredient':{'item':f'{MOD}:golem_placer'},'damage':32767},'components':[{'ingredient':{'item':i}} for i in ['minecraft:redstone','minecraft:glowstone_dust','minecraft:gunpowder','thaumcraft:essentia_jar','thaumcraft:zombie_brain']],'result':{'patch':'{advanced:1b}'}}
write(f'data/{MOD}/recipes/infusion/advanced_golem.json',advanced)
path=f'data/{MOD}/research_catalog/golems.json';catalog=read(path)
for key,y,points,parents,item,count in [('focusAnimation',-1,{'humanus':5,'motus':5,'spiritus':5,'fabrico':3},['PRIMPEARL','golemPowder'],'focus_animation',4),('golemPowder',1,{'humanus':3,'spiritus':3,'motus':3,'fabrico':3},['GOLEMTHAUMIUM','ADVANCEDGOLEM','incarnationVat'],'golem_powder',4)]:
 catalog['categories'][0]['research'].append({'key':key,'category':'ThaumicHorizons','displayColumn':12,'displayRow':y,'complexity':3,'aspects':[{'aspect':a,'amount':n} for a,n in points.items()],'parents':parents,'isConcealed':True,'icon_item':{'item':f'{MOD}:{item}'},'pageCount':count})
write(path,catalog)
def page(kind,key,clazz):return {'type':kind,'recipe':{'kind':'reference','class':clazz,'keys':[f'{MOD}:{key}']}}
infusion_class='thaumcraft.api.crafting.InfusionRecipe';crucible_class='thaumcraft.api.crafting.CrucibleRecipe'
path=f'data/{MOD}/research_pages/golems.json';pages=read(path)
pages['entries'] += [{'key':'focusAnimation','pages':[{'type':'TEXT','text':'focusAnimation1'},page('INFUSION_CRAFTING','focus_animation',infusion_class),{'type':'TEXT','text':'focusAnimation2'},page('INFUSION_CRAFTING','advanced_golem',infusion_class)]},{'key':'golemPowder','pages':[{'type':'TEXT','text':'golemPowder1'},page('CRUCIBLE_CRAFTING','golem_powder',crucible_class),{'type':'TEXT','text':'golemPowder2'},{'type':'TEXT','text':'golemPowder3'}]}]
write(path,pages)
path=f'data/{MOD}/research_page_recipes/golems.json';display=read(path)
for key,recipe in [('focus_animation',focus),('advanced_golem',advanced)]:
 output=stack(f'{MOD}:focus_animation') if key=='focus_animation' else {'kind':'sequence','elements':[{'kind':'literal','value':'advanced'},{'kind':'nbt','type':'net.minecraft.nbt.NBTTagByte','value':'1b'}]}
 display['entries'].append({'key':f'{MOD}:{key}','class':infusion_class,'data':{'kind':'infusion','research':'focusAnimation','instability':recipe['instability'],'aspects':recipe['aspects'],'input':stack(recipe['center']['ingredient']['item']),'output':output,'ingredients':{'kind':'sequence','elements':[stack(i['ingredient']['item']) for i in recipe['components']]}}})
display['entries'].append({'key':f'{MOD}:golem_powder','class':crucible_class,'data':{'kind':'crucible','research':'golemPowder','aspects':{'humanus':4,'spiritus':4,'motus':4},'catalyst':stack('thaumcraft:salis_mundus'),'output':stack(f'{MOD}:golem_powder')}})
write(path,display)
manifest=read(manifest_path)
for lang in manifest['languages'].values():
 for key,count in [('focusAnimation',2),('golemPowder',3)]:
  for prefix in ['tc.research_name.','tc.research_text.']:lang[prefix+key]=prefix+key
  for i in range(1,count+1):lang[f'{key}{i}']=f'{key}{i}'
write(manifest_path,manifest)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
