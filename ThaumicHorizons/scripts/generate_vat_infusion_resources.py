"""First implemented creature augmentations; additional outcomes are added with their behavior, not as inert recipes."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources';MOD='thaumichorizons'
def write(path,data):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def item(key):return {'item':key}
recipes=[
    ('quick_limbs',3,{'motus':16,'machina':8,'volatus':4},[{'type':'forge:nbt','item':'minecraft:potion','nbt':{'Potion':'minecraft:strong_swiftness'}},item('thaumcraft:quicksilver'),item('thaumcraft:quicksilver')]),
    ('thaumic_claws',3,{'telum':12,'metallum':8,'praecantatio':8},[item('thaumcraft:thaumium_sword')]*2),
    ('awake_blood',4,{'sano':8,'victus':16,'cognitio':4},[item('minecraft:glistering_melon_slice'),item(f'{MOD}:human_blood'),item('minecraft:golden_apple'),item(f'{MOD}:human_blood')]),
    ('diamond_skin',4,{'tutamen':16,'vitreus':16,'bestia':8},[item('minecraft:leather'),item('minecraft:diamond'),item('minecraft:diamond')]),
    ('ender_heart',6,{'alienis':16,'iter':16,'corpus':16},[item('minecraft:ender_pearl'),item('minecraft:egg'),item('minecraft:egg')]),
    ('shock_skin',5,{'potentia':16,'telum':12,'tempestas':4},[item('minecraft:quartz'),item('thaumcraft:air_shard'),item('minecraft:quartz'),item('minecraft:redstone')]),
    ('instilled_loyalty',6,{'cognitio':4,'bestia':3,'permutatio':3},[item('minecraft:saddle'),item('thaumcraft:zombie_brain'),item('minecraft:golden_apple')]),
    ('runic_hide',7,{'tutamen':16,'praecantatio':32,'potentia':16,'corpus':8},[item('minecraft:diamond'),item('thaumcraft:amber'),item('thaumcraft:enchanted_fabric'),item('thaumcraft:nitor'),item('thaumcraft:scribing_tools')]),
    ('eldritch_fangs',8,{'alienis':8,'telum':16,'metallum':16,'venenum':32},[item('thaumcraft:void_ingot'),item('thaumcraft:vishroom'),item('thaumcraft:void_ingot'),item('thaumcraft:vishroom')]),
    ('portability',6,{'vacuos':16,'vinculum':16},[item('thaumcraft:essentia_jar'),item('minecraft:ender_pearl')])]
for key,instability,aspects,ingredients in recipes:
    research={'instilled_loyalty':'instilledLoyalty','runic_hide':'runicCreature','eldritch_fangs':'eldritchFangs','portability':'portability'}.get(key,'infusionVat')
    write(f'data/{MOD}/vat_infusions/{key}.json',{'research':research,'instability':instability,'target':{'kind':'living'},'ingredients':ingredients,'aspects':aspects,'output':{'kind':'augmentation','id':key}})
write(f'data/{MOD}/tags/entity_types/infusion_blacklist.json',{'replace':False,'values':['minecraft:ghast','minecraft:enderman','minecraft:witch','minecraft:blaze','minecraft:magma_cube',{'id':'thaumcraft:pech','required':False},{'id':'thaumcraft:wisp','required':False},{'id':'thaumcraft:golem','required':False}]})
components=['thaumcraft:salis_mundus',f'{MOD}:human_blood','thaumcraft:salis_mundus',f'{MOD}:human_blood']
write(f'data/{MOD}/recipes/infusion/modified_matrix.json',{'type':'thaumcraft:infusion','research':'infusionVat','instability':10,'aspects':{'victus':32,'fabrico':32,'praecantatio':32,'bestia':32},'center':{'ingredient':{'item':'thaumcraft:infusion_matrix'}},'components':[{'ingredient':item(key)} for key in components],'result':item(f'{MOD}:modified_matrix')})
faces={side:{'texture':'#stone'} for side in ['up','down','north','south','east','west']}
elements=[{'from':[2+x*7,2+y*7,2+z*7],'to':[7+x*7,7+y*7,7+z*7],'faces':faces} for x in range(2) for y in range(2) for z in range(2)]
write(f'assets/{MOD}/models/block/modified_matrix.json',{'textures':{'stone':'thaumcraft:block/arcane_stone','particle':'thaumcraft:block/arcane_stone'},'elements':elements})
write(f'assets/{MOD}/models/item/modified_matrix.json',{'parent':'builtin/entity','textures':{'particle':'thaumcraft:block/arcane_stone'}})
write(f'assets/{MOD}/blockstates/modified_matrix.json',{'variants':{'':{'model':f'{MOD}:block/modified_matrix'}}})
write(f'data/{MOD}/loot_tables/blocks/modified_matrix.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:modified_matrix'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
path=f'data/{MOD}/thaumcraft/original_resources.json';manifest=json.loads((ROOT/path).read_text(encoding='utf-8'))
for lang in manifest['languages'].values():lang[f'block.{MOD}.modified_matrix']='tile.ThaumicHorizons_modMatrix.name'
write(path,manifest)
