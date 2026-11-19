"""Own JSON models/data only. Legacy pixels/translations remain in the player-supplied jar."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
MOD = 'forbidden_magic'
def write(path, data):
    # Complete aspect definitions are maintained directly since TC4 0.4.56.
    if ('/item_aspects/' in path or '/item_bonus_aspects/' in path) and (ROOT / 'data/forbidden_magic/item_aspects/explicit.json').exists(): return
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

manifest_path = ROOT / f'data/{MOD}/thaumcraft/original_resources.json'
manifest = json.loads(manifest_path.read_text(encoding='utf-8')) if manifest_path.exists() else {
    'name': 'Forbidden Magic 1.7.10-0.575', 'legacy_modid': 'ForbiddenMagic',
    'sha256': 'd6b239a05a630cfb3b6de01fe14a8674ec14ef8fb34ea75d1ad45b79565d8636',
    'download': 'https://www.curseforge.com/minecraft/mc-mods/forbidden-magic/files/2303822',
    'mapping': {}, 'languages': {}}
mapping = manifest['mapping']
def texture(kind, name, old):
    mapping[f'assets/{MOD}/textures/{kind}/{name}.png'] = f'assets/forbidden/textures/{old}.png'
    return f'{MOD}:{kind}/{name}'
def translate(key, legacy):
    for lang, old in [('en_us','en_US'), ('zh_cn','zh_CN')]:
        target=f'assets/{MOD}/lang/{lang}.json'
        mapping[target]=f'assets/forbidden/lang/{old}.lang'
        manifest['languages'].setdefault(target,{})[key]=legacy
def item(name, old, legacy, handheld=False):
    tex=texture('item',name,'items/'+old)
    write(f'assets/{MOD}/models/item/{name}.json',{'parent':'minecraft:item/'+('handheld' if handheld else 'generated'),'textures':{'layer0':tex}})
    translate(f'item.{MOD}.{name}',legacy)
def tag(namespace, path, values):
    write(f'data/{namespace}/tags/items/{path}.json',{'replace':False,'values':values})
def shapeless(name, inputs, output, count=1):
    write(f'data/{MOD}/recipes/{name}.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':i} for i in inputs],'result':{'item':output,'count':count}})

for sin in ['wrath','envy','taint','pride','lust','sloth','greed']:
    item(sin+'_shard',sin+'shard',f'item.NetherShard.{sin}.name')
for name,old,legacy in [
    ('gluttony_shard','gluttonyshard','GluttonyShard'),('emerald_nugget','emerald_nugget','FMResource.0'),
    ('black_dye','dye_powder_black','FMResource.1'),('taint_coal','taint_charcoal','TaintCoal'),
    ('taint_fruit','taint_fruit','TaintFruit'),('mob_crystal','emptycrystal','MobCrystal'),
    ('crystalwell','crystalwell','Crystalwell'),('primewell','primewell','Primewell'),
    ('ring_nutrition','ring_nutrition','RingNutrition'),('arcane_cake','cake','ArcaneCake')]:item(name,old,'item.'+legacy+'.name')
for name,old,legacy in [('skull_axe','skullaxe','SkullAxe'),('diabolist_fork','fork','DiabolistFork'),('riding_crop','crop','RidingCrop')]:item(name,old,'item.'+legacy+'.name',True)
translate(f'item.{MOD}.mob_crystal_empty','item.MobCrystalEmpty.name')
texture('item','mob_crystal_filled','items/mobcrystal')
write(f'assets/{MOD}/models/item/mob_crystal_filled.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/mob_crystal_filled'}})
write(f'assets/{MOD}/models/item/mob_crystal.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/mob_crystal'},'overrides':[{'predicate':{f'{MOD}:filled':1},'model':f'{MOD}:item/mob_crystal_filled'}]})
aspects=[]
for name,color,parts,blend in [('luxuria',0xffc1ce,['corpus','fames'],1),('infernus',0xff0000,['ignis','praecantatio'],771),('superbia',0x9639ff,['volatus','vacuos'],1),('gula',0xd59c46,['fames','vacuos'],1),('invidia',0x00ba00,['sensus','fames'],1),('desidia',0x6e6e6e,['vinculum','spiritus'],771),('ira',0x870404,['telum','ignis'],771)]:
    texture('aspects',name,'aspects/'+name)
    aspects.append({'tag':f'{MOD}:{name}','color':color,'components':parts,'chatColor':'','blendFactor':blend,'texture':f'{MOD}:textures/aspects/{name}.png'})
write(f'data/{MOD}/thaumcraft/aspects.json',aspects)
for name in ['cluster','wrath','greedy','consuming','educational','corrupting','voidtouched','impact']:translate(f'enchantment.{MOD}.{name}','enchantment.'+name)
tag('forge','nuggets/emerald',[f'{MOD}:emerald_nugget']);tag('forge','nuggets',[f'{MOD}:emerald_nugget'])
tag('forge','dyes/black',[f'{MOD}:black_dye']);tag('forge','dyes',[f'{MOD}:black_dye'])
tag('curios','ring',[f'{MOD}:ring_nutrition'])
shapeless('emerald_from_nuggets',[f'{MOD}:emerald_nugget']*9,'minecraft:emerald')
shapeless('emerald_nuggets',['minecraft:emerald'],f'{MOD}:emerald_nugget',9)
shapeless('ink_flower_dye',[f'{MOD}:ink_flower'],f'{MOD}:black_dye',2)
shapeless('nether_star_block',['minecraft:nether_star']*9,f'{MOD}:nether_star_block')
shapeless('nether_stars',[f'{MOD}:nether_star_block'],'minecraft:nether_star',9)
texture('block','nether_star_block','blocks/starblock')
write(f'assets/{MOD}/blockstates/nether_star_block.json',{'variants':{'':{'model':f'{MOD}:block/nether_star_block'}}})
write(f'assets/{MOD}/models/block/nether_star_block.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'{MOD}:block/nether_star_block'}})
write(f'assets/{MOD}/models/item/nether_star_block.json',{'parent':f'{MOD}:block/nether_star_block'})
translate(f'block.{MOD}.nether_star_block','tile.StarBlock.name')
for name,old in [('ink_flower','flower_black'),('umbral_bush_bottom','umbral_rose_bottom'),('umbral_bush_top','umbral_rose_top')]:
    tex=texture('block',name,'blocks/'+old)
    write(f'assets/{MOD}/models/block/{name}.json',{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':tex}})
write(f'assets/{MOD}/blockstates/ink_flower.json',{'variants':{'':{'model':f'{MOD}:block/ink_flower'}}})
write(f'assets/{MOD}/blockstates/umbral_bush.json',{'variants':{'half=lower':{'model':f'{MOD}:block/umbral_bush_bottom'},'half=upper':{'model':f'{MOD}:block/umbral_bush_top'}}})
for name,tex,old in [('ink_flower','ink_flower','InkFlower'),('umbral_bush','umbral_bush_top','blackRoseBush')]:
    write(f'assets/{MOD}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:block/{tex}'}})
    translate(f'block.{MOD}.{name}',f'tile.{old}.name')
for face in ['top','bottom','side','inner']:texture('block','cake_'+face,'blocks/cake_'+face)
variants={}
for bites in range(12):
    name=f'arcane_cake_{bites}'
    variants[f'bites={bites}']={'model':f'{MOD}:block/{name}'}
    faces={d:{'texture':'#'+('top' if d=='up' else 'bottom' if d=='down' else 'inner' if d=='west' and bites else 'side')} for d in ['up','down','east','west','north','south']}
    write(f'assets/{MOD}/models/block/{name}.json',{'textures':{**{f:f'{MOD}:block/cake_{f}' for f in ['top','bottom','side','inner']},'particle':f'{MOD}:block/cake_side'},'elements':[{'from':[1+bites*14/12,0,1],'to':[15,8,15],'faces':faces}]})
write(f'assets/{MOD}/blockstates/arcane_cake.json',{'variants':variants})
for block in ['ink_flower','nether_star_block','umbral_bush']:
    entry={'type':'minecraft:item','name':f'{MOD}:{block}'}
    if block=='umbral_bush':entry['conditions']=[{'condition':'minecraft:block_state_property','block':f'{MOD}:{block}','properties':{'half':'lower'}}]
    write(f'data/{MOD}/loot_tables/blocks/{block}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[entry],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(f'data/{MOD}/loot_tables/blocks/arcane_cake.json',{'type':'minecraft:block','pools':[]})
translate('itemGroup.forbidden_magic','itemGroup.forbidden')
for name,old,legacy in [('taint_pickaxe','taintpickaxe','TaintPickaxe'),('taint_shovel','taintshovel','TaintShovel')]:item(name,old,'item.'+legacy+'.name',True)
for kind,old,legacy in [('pickaxe','pick','Pickaxe'),('shovel','shovel','Shovel'),('axe','axe','Axe'),('sword','sword','Sword')]:
    name='morph_'+kind
    base=texture('item',name,'items/chameleon'+old);eye=texture('item',name+'_eye','items/eye'+old)
    write(f'assets/{MOD}/models/item/{name}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':base,'layer1':eye}})
    translate(f'item.{MOD}.{name}','item.Morph'+legacy+'.name')
tag(MOD,'consuming_trash',['minecraft:dirt','minecraft:coarse_dirt','minecraft:sand','minecraft:red_sand','minecraft:gravel','minecraft:cobblestone','minecraft:netherrack'])
write('data/forge/loot_modifiers/global_loot_modifiers.json',{'replace':False,'entries':[f'{MOD}:mining']})
write(f'data/{MOD}/loot_modifiers/mining.json',{'type':f'{MOD}:mining','conditions':[]})
for name,old,legacy in [('taint_planks','taint_planks','TaintPlank'),('taint_stone','taint_stone_smooth','TaintStone_Smooth'),('taint_bricks','taint_stone_brick','TaintStone_Brick'),('taint_leaves','taint_leaves','TaintLeaves')]:
    texture('block',name,'blocks/'+old)
    model={'parent':'minecraft:block/cube_all','textures':{'all':f'{MOD}:block/{name}'}}
    if name=='taint_leaves':model['render_type']='minecraft:cutout_mipped'
    write(f'assets/{MOD}/models/block/{name}.json',model)
    write(f'assets/{MOD}/models/item/{name}.json',{'parent':f'{MOD}:block/{name}'})
    write(f'assets/{MOD}/blockstates/{name}.json',{'variants':{'':{'model':f'{MOD}:block/{name}'}}})
    translate(f'block.{MOD}.{name}',f'tile.{legacy}.name')
texture('block','taint_log_side','blocks/taint_log_side');texture('block','taint_log_end','blocks/taint_log_bottom')
write(f'assets/{MOD}/models/block/taint_log.json',{'parent':'minecraft:block/cube_column','textures':{'side':f'{MOD}:block/taint_log_side','end':f'{MOD}:block/taint_log_end'}})
write(f'assets/{MOD}/models/item/taint_log.json',{'parent':f'{MOD}:block/taint_log'})
write(f'assets/{MOD}/blockstates/taint_log.json',{'variants':{'axis=y':{'model':f'{MOD}:block/taint_log'},'axis=x':{'model':f'{MOD}:block/taint_log','x':90,'y':90},'axis=z':{'model':f'{MOD}:block/taint_log','x':90}}})
texture('block','taint_sapling','blocks/sapling_taint')
write(f'assets/{MOD}/models/block/taint_sapling.json',{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':f'{MOD}:block/taint_sapling'}})
write(f'assets/{MOD}/models/item/taint_sapling.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:block/taint_sapling'}})
write(f'assets/{MOD}/blockstates/taint_sapling.json',{'variants':{'':{'model':f'{MOD}:block/taint_sapling'}}})
for name,old in [('taint_log','TaintLog'),('taint_sapling','TaintSapling')]:translate(f'block.{MOD}.{name}',f'tile.{old}.name')
for namespace,kind,values in [('minecraft','logs',['taint_log']),('minecraft','planks',['taint_planks']),('minecraft','leaves',['taint_leaves']),('minecraft','saplings',['taint_sapling']),('minecraft','mineable/axe',['taint_log','taint_planks']),('minecraft','mineable/pickaxe',['taint_stone','taint_bricks','nether_star_block'])]:
    write(f'data/{namespace}/tags/blocks/{kind}.json',{'replace':False,'values':[f'{MOD}:{v}' for v in values]})
    if '/' not in kind:tag(namespace,kind,[f'{MOD}:{v}' for v in values])
for name in ['taint_log','taint_planks','taint_stone','taint_bricks','taint_sapling']:
    write(f'data/{MOD}/loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
silk={'condition':'minecraft:match_tool','predicate':{'enchantments':[{'enchantment':'minecraft:silk_touch','levels':{'min':1}}]}}
shears={'condition':'minecraft:match_tool','predicate':{'items':['minecraft:shears']}}
silk_or_shears={'condition':'minecraft:any_of','terms':[silk,shears]}
no_silk={'condition':'minecraft:inverted','term':silk_or_shears}
write(f'data/{MOD}/loot_tables/blocks/taint_leaves.json',{'type':'minecraft:block','pools':[
    {'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[{'type':'minecraft:item','name':f'{MOD}:taint_leaves','conditions':[silk_or_shears]},{'type':'minecraft:item','name':f'{MOD}:taint_sapling','conditions':[{'condition':'minecraft:table_bonus','enchantment':'minecraft:fortune','chances':[1/60,1/58,1/56,1/52,1/44]}]}]}],'conditions':[{'condition':'minecraft:survives_explosion'}]},
    {'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:taint_fruit'}],'conditions':[no_silk,{'condition':'minecraft:random_chance','chance':.05},{'condition':'minecraft:survives_explosion'}]}]})
shapeless('taint_planks',[f'{MOD}:taint_log'],f'{MOD}:taint_planks',4)
write(f'data/{MOD}/recipes/taint_bricks.json',{'type':'minecraft:crafting_shaped','pattern':['SS','SS'],'key':{'S':{'item':f'{MOD}:taint_stone'}},'result':{'item':f'{MOD}:taint_bricks','count':4}})
write(f'data/{MOD}/recipes/taint_coal.json',{'type':'minecraft:smelting','ingredient':{'item':f'{MOD}:taint_log'},'result':f'{MOD}:taint_coal','experience':.15,'cookingtime':200})
mob_aspects={}
for aspect,mobs in {
    'corpus':['zombie'],'mortuus':['skeleton','wither_skeleton'],'ignis':['creeper','blaze','magma_cube'],
    'bestia':['horse','pig','cow','ocelot','wolf'],'pannus':['sheep','spider'],'herba':['mooshroom'],
    'volatus':['chicken','bat'],'sensus':['squid'],'limus':['slime'],'forbidden_magic:infernus':['ghast'],
    'lucrum':['zombified_piglin','silverfish','villager'],'alienis':['enderman'],'venenum':['cave_spider'],'praecantatio':['witch']
}.items():
    for mob in mobs:mob_aspects['minecraft:'+mob]=aspect
for aspect,mobs in {'ignis':['fire_bat'],'auram':['wisp'],'vitium':['thaumic_slime','taint_spider','taint_swarm','tainted_pig','tainted_sheep','tainted_cow','tainted_chicken','tainted_villager'],'cognitio':['brainy_zombie','giant_brainy_zombie'],'alienis':['cultist_knight','cultist_cleric','eldritch_crab','inhabited_zombie','eldritch_guardian'],'lucrum':['pech']}.items():
    for mob in mobs:mob_aspects['thaumcraft:'+mob]=aspect
write(f'data/{MOD}/forbidden_magic/cage_mobs/default.json',mob_aspects)
texture('block','wrath_cage','blocks/spirit_box')
write(f'assets/{MOD}/models/block/wrath_cage.json',{'parent':'minecraft:block/cube_all','render_type':'minecraft:cutout','textures':{'all':f'{MOD}:block/wrath_cage'}})
write(f'assets/{MOD}/models/item/wrath_cage.json',{'parent':f'{MOD}:block/wrath_cage'})
write(f'assets/{MOD}/blockstates/wrath_cage.json',{'variants':{'':{'model':f'{MOD}:block/wrath_cage'}}})
write(f'data/{MOD}/loot_tables/blocks/wrath_cage.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{MOD}:wrath_cage'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write('data/minecraft/tags/blocks/beacon_base_blocks.json',{'replace':False,'values':[f'{MOD}:nether_star_block']})
translate(f'block.{MOD}.wrath_cage','tile.WrathCage.name')
rods=[]
for rod,capacity,staff in [('tainted',150,False),('infernal',150,False),('profane',50,False),('profaned',50,False),('neutronium',9001,False),('neutronium_staff',9002,True)]:
    image='neutronium' if rod.startswith('neutronium') else rod
    texture('models','wand_rod_'+image,'models/wand_rod_'+image)
    row={'tag':f'{MOD}:{rod}','capacity':capacity,'craftCost':1000 if rod.startswith('neutronium') else 12,'texture':f'{MOD}:textures/models/wand_rod_{image}.png','staff':staff,'research':f'{MOD}:ROD_{rod}'}
    if rod in ['tainted','infernal','profane']:
        row['item']=f'{MOD}:wand_rod_{rod}'
        item('wand_rod_'+rod,'wand_rod_'+rod,f'item.WandCores.{rod}.name')
    else:row['research']=''
    if rod.startswith('neutronium'):row['glowing']=True
    rods.append(row)
write(f'data/{MOD}/thaumcraft/wand_rods.json',rods)
upgrades=[]
for key,aspect in [('hellfire',f'{MOD}:infernus'),('pandemonium','tenebrae')]:
    texture('misc',key,'misc/'+key)
    upgrades.append({'key':f'{MOD}:{key}','aspects':[aspect],'texture':f'{MOD}:textures/misc/{key}.png','name':f'forbidden.upgrade.{key}.name','description':f'forbidden.upgrade.{key}.text'})
    for suffix in ['name','text']:translate(f'forbidden.upgrade.{key}.{suffix}',f'forbidden.upgrade.{key}.{suffix}')
write(f'data/{MOD}/thaumcraft/focus_upgrades.json',upgrades)
item('focus_blink','focus_blink','item.BlinkFocus.name')
texture('item','focus_blink_orn','items/focus_blink_orn')
write(f'assets/{MOD}/models/item/focus_blink.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/focus_blink_orn','layer1':f'{MOD}:item/focus_blink'}})
write(f'assets/{MOD}/models/item/focus_blink_orn.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{MOD}:item/focus_blink_orn'}})
item('pain_collar','collar','item.SubCollar.name')
tag('curios','necklace',[f'{MOD}:pain_collar'])
for key in ['message.collar.placescollar','message.collar.youplacecollar','message.collar.alreadywearing','tooltip.collar.owner']:translate(key,key)
texture('entity','heretic','models/heretic')
write(f'assets/{MOD}/models/item/heretic_spawn_egg.json',{'parent':'minecraft:item/template_spawn_egg'})
write(f'assets/{MOD}/lang/en_us.json',{'entity.forbidden_magic.heretic':'Heretic','entity.minecraft.villager.forbidden_magic.heretic':'Heretic','item.forbidden_magic.heretic_spawn_egg':'Heretic Spawn Egg','tooltip.forbidden_magic.contract':'Contract: %s'})
write(f'assets/{MOD}/lang/zh_cn.json',{'entity.forbidden_magic.heretic':'异端','entity.minecraft.villager.forbidden_magic.heretic':'异端','item.forbidden_magic.heretic_spawn_egg':'异端刷怪蛋','tooltip.forbidden_magic.contract':'契约余额：%s'})
write(f'data/{MOD}/thaumcraft/original_resources.json',manifest)
