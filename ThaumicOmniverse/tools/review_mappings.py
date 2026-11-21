"""Curated cross-version renames and metadata splits, saved as reviewable JSON."""
import json,pathlib,re,zipfile
import generate as g
ROOT=g.ROOT;BASE=g.BASE
maps=g.read(ROOT/'tools/mappings.json') if (ROOT/'tools/mappings.json').exists() else {}
rows=g.collect();invs=g.inventories()
VANILLA_IDS=set(g.read(ROOT/'evidence/vanilla-registry.json')['items'])
def add(id,meta,target,reason):
 legacy=id.split(':')[0]
 key=id+'@'+str(meta)
 if legacy not in invs:return
 targets=[target] if isinstance(target,str) else target
 available=set(invs[legacy]['items'])|set(invs[legacy]['blocks'])
 if any(t not in available for t in targets):return
 maps[key]={'targets':targets,'reason':reason}
def family(id,table,reason):
 for meta,target in table.items():add(id,meta,target,reason)
def allmeta(id,target,reason):
 for (old,meta) in rows[id.split(':')[0]]:
  if old==id:add(id,meta,target,reason)
def vanilla(id,meta,target,reason):
 # Validate exact native IDs against the independently started core-only profile.
 if target not in VANILLA_IDS:raise ValueError('Unknown vanilla target: '+target)
 maps[id+'@'+str(meta)]={'targets':[target],'kind':'item_aspects','reason':reason+'; file target-mod gate prevents assignment while counterpart is absent'}
family('Avaritia:Resource',{1:'avaritia:crystal_matrix_ingot',4:'avaritia:neutron_ingot',5:'avaritia:infinity_catalyst',6:'avaritia:infinity_ingot'},'legacy ItemResource.types; Re-Avaritia separates metadata resources')
allmeta('Avaritia:Singularity','avaritia:singularity','legacy singularities become one NBT item; item-wide JSON uses maximum union')
family('ProjectE:item.pe_covalence_dust',{0:'projecte:low_covalence_dust',1:'projecte:medium_covalence_dust',2:'projecte:high_covalence_dust'},'ProjectE covalence dust tiers: low, medium, high')
family('NuclearCraft:parts',{1:'nuclearcraft:iron_plate',6:'nuclearcraft:tin_plate',14:'nuclearcraft:lead_plate'},'legacy ItemPart switch returns iron/tin/lead Plating; modern same material plates')
family('flammpfeil.slashblade:proudsoul',{0:'slashblade:proudsoul',1:'slashblade:proudsoul_ingot',2:'slashblade:proudsoul_sphere',3:'slashblade:proudsoul_tiny',4:'slashblade:proudsoul_crystal',5:'slashblade:proudsoul_trapezohedron'},'legacy SlashBlade.init explicit soul material metadata; modern distinct item forms')
allmeta('EnderTanks:blockEnderTank','endertanks:ender_tank','EnderTanks same tank, modern snake_case registry ID')
allmeta('EnderTanks:itemEnderBucket','endertanks:ender_bucket','EnderTanks same bucket, modern snake_case registry ID')
family('Mekanism:OreBlock',{0:'mekanism:osmium_ore',2:'mekanism:tin_ore'},'legacy BlockOre metadata; copper became vanilla and is not reassigned globally')
family('Mekanism:Ingot',{1:'mekanism:ingot_osmium',2:'mekanism:ingot_bronze',4:'mekanism:ingot_steel',6:'mekanism:ingot_tin'},'ItemIngot.en_USNames; modern Mekanism registration names')
family('SilentGems:Gem',{i:'silentgems:'+n.lower() for i,n in enumerate(['Ruby','Garnet','Topaz','Heliodor','Peridot','Beryl','Aquamarine','Sapphire','Iolite','Amethyst','Morganite','Onyx'])},'legacy EnumGem metadata and names; same modern gems')
family('SilentGems:GemOre',{i:'silentgems:'+n.lower()+'_ore' for i,n in enumerate(['Ruby','Garnet','Topaz','Heliodor','Peridot','Beryl','Aquamarine','Sapphire','Iolite','Amethyst','Morganite','Onyx'])},'legacy EnumGem metadata; ordinary stone ore host')
parts={'pickaxeHead':'pick_head','hatchetHead':'small_axe_head','swordBlade':'small_blade','knifeBlade':'small_blade','largeSwordBlade':'broad_blade','broadAxeHead':'broad_axe_head','hammerHead':'hammer_head','toolRod':'tool_handle','toughRod':'tough_handle','binding':'tool_binding','toughBinding':'tough_binding','heavyPlate':'large_plate','BowLimbPart':'bow_limb','CrossbowLimbPart':'bow_limb','CrossbowBodyPart':'bow_grip','bowstring':'bowstring','arrowhead':'arrow_head','Crossbow':'crossbow','ShortBow':'bow','LongBow':'longbow','broadsword':'sword','lumberaxe':'broad_axe','hammer':'sledge_hammer','hatchet':'hand_axe','scythe':'scythe','cleaver':'cleaver','pickaxe':'pickaxe','mattock':'mattock','Shuriken':'shuriken','blankPattern':'pattern','CraftingStation':'crafting_station','ToolStationBlock':'tinker_station','ToolForgeBlock':'tinkers_anvil'}
for old,new in parts.items():allmeta('TConstruct:'+old,'tconstruct:'+new,'TiC1 to TiC3 corresponding tool/part; old material metadata becomes NBT, item-wide maximum union')
patterns=['ingot','tool_handle','pick_head',None,'small_axe_head','small_blade',None,None,None,'tool_binding',None,None,'small_blade',None,'tough_handle','tough_binding','large_plate','broad_axe_head',None,None,'broad_blade','hammer_head',None,None,None,'arrow_head']
for item in ['metalPattern','Cast']:
 for meta,part in enumerate(patterns):
  if part:family('TConstruct:'+item,{meta:'tconstruct:'+part+'_cast'},'legacy TinkerTools.patternTypes order; corresponding reusable gold cast')
for (id,meta),row in rows['TConstruct'].items():
 if id.startswith('TConstruct:fluid.molten.'):
  material=id.split('.')[-1];material={'pigiron':'pig_iron','alubrass':'aluminum_brass','shiny':'platinum'}.get(material,material)
  add(id,meta,'tconstruct:molten_'+material,'same molten material; technical fluid block has no item')
for old,new in [('liquid.blood','blood'),('liquid.glue','glue'),('liquid.slime','earth_slime'),('fluid.ender','ender')]:allmeta('TConstruct:'+old,'tconstruct:'+new,'same technical fluid block')
eio={0:'electrical_steel',1:'energetic_alloy',2:'vibrant_alloy',3:'redstone_alloy',4:'conductive_iron',5:'pulsating_alloy',6:'dark_steel',7:'soularium'}
for meta,metal in eio.items():
 family('EnderIO:itemAlloy',{meta:'enderio:'+metal+'_ingot'},'legacy Alloy ordinal; phasedGold/phasedIron renamed vibrant/pulsating alloy')
 family('EnderIO:blockIngotStorage',{meta:'enderio:'+metal+'_block'},'legacy Alloy ordinal, storage block')
family('EnderIO:itemMaterial',{0:'enderio:silicon',1:'enderio:conduit_binder',2:'enderio:conduit_binder_composite',3:'enderio:pulsating_alloy_nugget',4:'enderio:vibrant_alloy_nugget',5:'enderio:pulsating_crystal',6:'enderio:vibrant_crystal',7:'enderio:dark_steel_grinding_ball'},'legacy Material enum ordinal')
family('EnderIO:itemBasicCapacitor',{0:'enderio:basic_capacitor',1:'enderio:double_layer_capacitor',2:'enderio:octadic_capacitor'},'legacy capacitor tier metadata')
family('EnderIO:blockCapBank',{0:'enderio:basic_capacitor_bank',1:'enderio:capacitor_bank',2:'enderio:vibrant_capacitor_bank'},'legacy capacitor bank tier metadata')
eio_renames={'blockAttractor':'attractor_obelisk','blockCombustionGenerator':'combustion_generator','blockExperienceObelisk':'experience_obelisk','blockFarmStation':'farming_station','blockKillerJoe':'killer_joe','blockPowerMonitor':'power_monitor','blockReinforcedObsidian':'reinforced_obsidian_block','blockReservoir':'reservoir','blockTransceiver':'dimensional_transceiver','blockWeatherObelisk':'weather_obelisk','blockWirelessCharger':'wireless_charger','blockZombieGenerator':'stirling_generator','blockDarkIronBars':'dark_steel_bars','itemExistingItemFilter':'existing_item_filter','itemSoulVessel':'empty_soul_vial','itemFusedQuartzFrame':'fused_quartz','blockDarkSteelAnvil':'dark_steel_anvil','itemModItemFilter':'mod_item_filter'}
eio_renames.pop('blockZombieGenerator') # zombie generator is not the unrelated Stirling generator
eio_renames.pop('itemFusedQuartzFrame') # a frame is not fused quartz itself
family('EnderIO:blockTank',{0:'enderio:fluid_tank',1:'enderio:pressurized_fluid_tank'},'legacy tank tier metadata; modern ordinary/pressurized tanks')
skulls={0:'zombie_electrode',2:'frank_n_zombie',3:'ender_resonator',4:'sentient_ender',5:'skeletal_contractor',6:'guardian_diode'}
for (id,meta) in rows['EnderIO']:
 if id=='EnderIO:itemFrankenSkull':
  target='enderio:'+skulls[int(meta)] if meta.isdigit() and int(meta) in skulls else None
  maps[id+'@'+meta]={'targets':[target] if target in invs['EnderIO']['items'] else [],'reason':'legacy FrankenSkull ordinal; same named modern electrode/controller, unresolved controller excluded'}
 elif id=='EnderIO:itemGliderWing' and meta!='0':maps[id+'@'+meta]={'targets':[],'reason':'legacy metadata 1 is assembled plural GliderWings, not a single modern glider_wing'}
for old,new in eio_renames.items():allmeta('EnderIO:'+old,'enderio:'+new,'same Ender IO machine/component, modern descriptive registry name')
for old,new in [('itemEnderFood','enderios'),('blockDarkSteelPressurePlate','dark_steel_pressure_plate')]:allmeta('EnderIO:'+old,'enderio:'+new,'legacy named Ender IO item')
# AE2 preserves the material/part enum semantics while exposing separate IDs.
ae_materials={0:'certus_quartz_crystal',1:'charged_certus_quartz_crystal',2:'certus_quartz_dust',3:'nether_quartz_dust',4:'flour',5:'silicon',6:'matter_ball',7:'fluix_crystal',8:'fluix_dust',9:'fluix_pearl',13:'calculation_processor_press',14:'engineering_processor_press',15:'logic_processor_press',16:'printed_calculation_processor',17:'printed_engineering_processor',18:'printed_logic_processor',19:'silicon_press',20:'printed_silicon',22:'logic_processor',23:'calculation_processor',24:'engineering_processor',25:'basic_card',26:'redstone_card',27:'capacity_card',28:'advanced_card',29:'fuzzy_card',30:'speed_card',31:'inverter_card',32:'spatial_cell_component_2',33:'spatial_cell_component_16',34:'spatial_cell_component_128',35:'cell_component_1k',36:'cell_component_4k',37:'cell_component_16k',38:'cell_component_64k',39:'item_cell_housing',40:'wooden_gear',41:'wireless_receiver',42:'wireless_booster',43:'formation_core',44:'annihilation_core',45:'sky_dust',46:'ender_dust',47:'singularity',48:'quantum_entangled_singularity',49:'iron_dust',51:'gold_dust',52:'blank_pattern',53:'crafting_card'}
family('appliedenergistics2:item.ItemMultiMaterial',{m:'ae2:'+s for m,s in ae_materials.items()},'legacy MaterialType explicit damageValue; AE2 separate registry IDs')
ae_parts={80:'toggle_bus',100:'inverted_toggle_bus',120:'cable_anchor',140:'quartz_fiber',160:'monitor',180:'semi_dark_monitor',200:'dark_monitor',220:'storage_bus',240:'import_bus',260:'export_bus',280:'level_emitter',300:'annihilation_plane',301:'identity_annihilation_plane',320:'formation_plane',340:'pattern_encoding_terminal',360:'crafting_terminal',380:'terminal',400:'storage_monitor',420:'conversion_monitor',440:'interface',460:'me_p2p_tunnel',461:'redstone_p2p_tunnel',462:'item_p2p_tunnel',463:'fluid_p2p_tunnel',466:'fe_p2p_tunnel',467:'light_p2p_tunnel',480:'pattern_access_terminal'}
family('appliedenergistics2:item.ItemMultiPart',{m:'ae2:'+s for m,s in ae_parts.items()},'legacy PartType baseDamage; corresponding AE2 part')
# Preserve individual cable colors. 16 is the legacy transparent color.
colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black','fluix']
for base,kind in [(0,'glass'),(20,'covered'),(40,'smart'),(60,'smart_dense')]:
 family('appliedenergistics2:item.ItemMultiPart',{base+i:'ae2:'+color+'_'+kind+'_cable' for i,color in enumerate(colors)},'legacy PartType baseDamage + AEColor index; same cable type and color')
aspects=['aqua','humanus','aer','volatus','ignis','praecantatio','exanimis','corpus','bestia','venenum','terra','alienis','iter','metallum','limus','pannus','messis']
family('ThaumicTinkerer:mobAspect',{i:'thaumic_tinkerer:soul_'+a for i,a in enumerate(aspects)},'legacy NumericAspectHelper.init order; port MobEssenceItem.ASPECTS order')
family('ThaumicTinkerer:kamiResource',{6:'thaumic_tinkerer:nether_shard',7:'thaumic_tinkerer:ender_shard'},'Kami dimensional shards retain their Nether/End identity')
wg='WitchingGadgets:item.WG_Material'
family(wg,{i:'witchinggadgets:'+n for i,n in enumerate(['thread_simple','thread_gold','thread_thaumium','cloth_space','cloth_golden','cloth_bewitched','wolf_pelt','calculator','cutting_tools','photo_plate','developed_photo','guiding_string','powerless_pearl','gemstone_dust'])},'legacy ItemMaterials.subNames; port Content.LEGACY_MATERIALS and named registrations')
family('WitchingGadgets:item.WG_MagicFood',{0:'witchinggadgets:sweetwart',2:'witchinggadgets:brainjerky'},'legacy ItemMagicFood.subNames; port MagicFood IDs')
allmeta('WitchingGadgets:WG_RoseVine','witchinggadgets:rose_vines','same RoseVines block, renamed plural registry ID')
family('WitchingGadgets:WG_StoneDevice',{0:'witchinggadgets:ethereal_wall',1:'witchinggadgets:magic_tile_lock',2:'witchinggadgets:obsidian_plate',3:'witchinggadgets:obsidian_decoration',4:'witchinggadgets:obsidian_decoration',5:'witchinggadgets:obsidian_decoration',6:'witchinggadgets:sarcophagus'},'original 1.1.10 BlockWGStoneDevice.subNames; decorations now block states')
family('WitchingGadgets:WG_WoodenDevice',{1:'witchinggadgets:snow_generator',3:'witchinggadgets:cutting_table'},'original 1.1.10 BlockWGWoodenDevice.subNames')
# FramedBlocks is a functional successor; only shared camouflage block types map.
for old,new in {'blockCarpentersButton':'framed_button','blockCarpentersGate':'framed_fence_gate','blockCarpentersHatch':'framed_trapdoor','blockCarpentersLadder':'framed_ladder','blockCarpentersLever':'framed_lever','blockCarpentersPressurePlate':'framed_pressure_plate','blockCarpentersTorch':'framed_torch','itemCarpentersDoor':'framed_door','itemCarpentersHammer':'framed_hammer'}.items():allmeta('CarpentersBlocks:'+old,'framedblocks:'+new,'shared camouflage block/tool type; functional counterpart rather than source lineage')
for old,new in {'blockAlloyGlass':'reinforced_glass','blockFenceIron':'iron_fence','blockIronScaffold':'iron_scaffold','blockRubLeaves':'rubber_leaves','blockRubSapling':'rubber_sapling','blockRubWood':'rubber_wood','blockRubber':'rubber_sheet','blockScaffold':'scaffold'}.items():allmeta('IC2:'+old,'ic2:'+new,'IC2 BlocksItems/modern Refactored same block, separate descriptive ID')
family('IC2:blockElectric',{0:'ic2:batbox',1:'ic2:mfe',2:'ic2:mfsu',7:'ic2:cesu'},'legacy BlockElectric.getTeClass metadata')
family('IC2:blockGenerator',{0:'ic2:generator',2:'ic2:water_generator',3:'ic2:solar_generator',4:'ic2:wind_generator'},'legacy BlockGenerator.getTeClass metadata')
family('IC2:blockMachine',{1:'ic2:iron_furnace',2:'ic2:electric_furnace',3:'ic2:macerator',4:'ic2:extractor',5:'ic2:compressor'},'legacy BlockMachine.getTeClass metadata')
# Railcraft explicitly numbers BRICK, FITTED, BLOCK, ORNATE, ETCHED, COBBLE.
for (id,meta),row in rows['Railcraft'].items():
 if ':brick.' not in id:continue
 theme=id.split('.')[-1];theme={'bleachedbone':'bleached_bone'}.get(theme,theme)
 types=[theme+'_bricks','polished_'+theme+'_stone',theme+'_stone','chiseled_'+theme+'_stone','etched_'+theme+'_stone',theme+'_cobblestone']
 available=set(invs['Railcraft']['items'])|set(invs['Railcraft']['blocks'])
 targets=[('railcraft:'+name) for name in types if 'railcraft:'+name in available] if meta in ('32767','-1') else ['railcraft:'+types[int(meta)]] if int(meta)<6 else []
 if targets:add(id,meta,targets,'legacy BrickVariant ordinal; same Railcraft stone theme and style')
family('Automagy:blockBookshelfEnchanted',{0:'automagy:greatwood_bookshelf',1:'automagy:silverwood_bookshelf'},'legacy bookshelf metadata; modern Content separate wood registrations')
allmeta('Automagy:blockMirrorMultiDest','automagy:multi_dest_mirror','modern Content.MULTI_DEST_MIRROR')
for old,new in [('Milk','milk'),('Mushroom','mushroom'),('Vishroom','vishroom')]:
 # Technical fluids are registered in Java and have no item model/blockstate assets.
 for (id,meta) in rows['Automagy']:
  if id=='Automagy:blockFluid'+old:maps[id+'@'+meta]={'targets':['automagy:'+new+'_fluid'],'reason':'modern SoupFluids source LiquidBlock registration; no block item'}
for (id,meta) in rows['thaumicbases']:
 old=id.split(':')[1]
 if old in ('genLeaves','goldenOakSapling') and meta.isdigit():
  tree=['golden_oak','peaceful','nether','ender'][int(meta)&3];add(id,meta,'thaumicbases:'+tree+('_leaves' if old=='genLeaves' else '_sapling'),'legacy BlockTBLeaves/sapling type lower two metadata bits; modern named tree blocks')
 elif old=='genLogs':
  trees=['peaceful','nether','ender'];targets=['thaumicbases:'+t+'_log' for t in trees] if meta in ('32767','-1') else ['thaumicbases:'+trees[int(meta)&3]+'_log'] if meta.isdigit() and (int(meta)&3)<3 else []
  if targets:add(id,meta,targets,'legacy BlockTBLog type bits and axis bits; same modern tree log')
family('MagicBees:wax',{0:'mobees:arcane_wax',1:'mobees:soul_wax'},'legacy WaxType MAGIC/SOUL; Mo Bees corresponding wax')
for (id,meta) in rows['MagicBees']:
 if id=='MagicBees:comb' and meta in ('32767','-1'):
  targets=sorted(t for t in invs['MagicBees']['items'] if t.startswith('mobees:bee_comb_'))
  add(id,meta,targets,'legacy wildcard comb definition; successor bee comb family, excludes comb blocks/crates')
# Native NBT families: never merge distinct materials/species into an item-wide value.
vanilla('Mekanism:Ingot',5,'minecraft:copper_ingot','legacy ItemIngot.en_USNames index 5 Copper; modern Mekanism uses vanilla copper')
vanilla('Mekanism:OreBlock',1,'minecraft:copper_ore','legacy BlockOre CopperOre index 1; modern ordinary copper ore')
vanilla('Forestry:ingotCopper',0,'minecraft:copper_ingot','legacy Forestry copper ingot; successor uses vanilla copper')
vanilla('NuclearCraft:material',0,'minecraft:copper_ingot','legacy ItemMaterial.copperIngot; successor uses vanilla copper')
vanilla('NuclearCraft:blockOre',0,'minecraft:copper_ore','legacy ItemBlockOre.copperOre; successor uses vanilla ordinary ore')
vanilla('ThermalFoundation:material',64,'minecraft:copper_ingot','legacy TFItems ingotCopper metadata; successor uses vanilla copper')
vanilla('ThermalFoundation:material',8,'minecraft:iron_nugget','legacy TFItems nuggetIron metadata; successor uses vanilla iron nugget')
vanilla('Botania:manaResource',10,'minecraft:prismarine_shard','legacy LibItemNames prismarineShard; native vanilla replacement')
for (id,meta) in rows['Botania']:
 if id=='Botania:seaLamp':vanilla(id,meta,'minecraft:sea_lantern','legacy Botania sea lantern backport; vanilla replacement')
 elif id=='Botania:prismarine':
  types={0:'prismarine',1:'prismarine_bricks',2:'dark_prismarine'}
  if meta.isdigit() and int(meta) in types:vanilla(id,meta,'minecraft:'+types[int(meta)],'legacy prismarine backport variant; same vanilla masonry')
vanilla('BiomesOPlenty:bamboo',0,'minecraft:bamboo','legacy bamboo plant; native vanilla replacement')
vanilla('BiomesOPlenty:flowers2',1,'minecraft:lily_of_the_valley','legacy ItemBlockFlower2 lilyofthevalley; native vanilla same flower')
vanilla('BiomesOPlenty:flowers',15,'minecraft:dandelion','legacy ItemBlockFlower dandelion; native vanilla same flower')
vanilla('TConstruct:materials',9,'minecraft:copper_ingot','legacy TinkerTools.materialStrings ingotCopper; native vanilla replacement')
vanilla('TConstruct:materials',19,'minecraft:iron_nugget','legacy TinkerTools.materialStrings nuggetIron; native vanilla replacement')
if 'Railcraft' in invs:
 available=set(invs['Railcraft']['items']);rail_source=ROOT/'evidence/legacy-source/mods/railcraft/common/blocks'
 aliases={'TURBINE':'steam_turbine','COKE_OVEN':'coke_oven_bricks','BLAST_FURNACE':'blast_furnace_bricks','TANK_WATER':'water_tank_siding','ROLLING_MACHINE':'powered_rolling_machine','BOILER_TANK_LOW_PRESSURE':'low_pressure_steam_boiler_tank','BOILER_TANK_HIGH_PRESSURE':'high_pressure_steam_boiler_tank','BOILER_FIREBOX_SOLID':'solid_fueled_firebox','BOILER_FIREBOX_FLUID':'fluid_fueled_firebox','COKE_BLOCK':'coal_coke_block','INFERNAL_BRICK':'infernal_bricks','SANDY_BRICK':'sandy_bricks'}
 for old,file in [('machine.alpha','machine/alpha/EnumMachineAlpha.java'),('machine.beta','machine/beta/EnumMachineBeta.java'),('cube','aesthetics/cube/EnumCube.java'),('detector','detector/EnumDetector.java'),('ore','ore/EnumOre.java')]:
  names=re.findall(r'^\s+([A-Z][A-Z_0-9]+)\(', (rail_source/file).read_text(encoding='utf-8'),re.M)
  resolved={}
  for meta,name in enumerate(names):
   path=name.lower()
   if old=='detector':path+='_detector'
   if old=='ore':path+='_ore'
   path=aliases.get(name,path);targets=['railcraft:'+path] if 'railcraft:'+path in available else []
   if name.startswith('TANK_') and old=='machine.beta':
    _,metal,part=name.lower().split('_');targets=sorted(t for t in available if t.startswith('railcraft:') and t.endswith('_'+metal+'_tank_'+part))
   if targets:resolved[meta]=targets;add('Railcraft:'+old,meta,targets,'legacy '+file+' ordinal/getItem metadata; corresponding native machine/detector/material form, tank colors all covered by legacy generic registration')
  for (id,meta) in rows['Railcraft']:
   if id=='Railcraft:'+old and meta in ('32767','-1') and resolved:add(id,meta,sorted({t for targets in resolved.values() for t in targets}),'legacy wildcard covers every verified corresponding native subtype of this family')
allmeta('totemic:redCedarPlank','totemic:cedar_planks','same cedar wood plank; modern registry drops red qualifier and uses plural')
allmeta('SaltMod:saltOre','salt:rock_salt_ore','same natural rock salt ore in the Salt successor')
allmeta('SaltMod:saltPinch','salt:salt','same salt ingredient, native Salt item')
if 'MineFactoryReloaded' in invs:
 machine_names={'Planter':'plant_sower','Fisher':'marine_fisher','Harvester':'plant_gatherer','Rancher':'animal_rancher','Fertilizer':'plant_fertilizer','ItemCollector':'item_collector','BlockBreaker':'block_breaker','Breeder':'animal_feeder','Grinder':'mob_crusher','AutoEnchanter':'enchantment_factory','Chronotyper':'animal_baby_separator','AutoSpawner':'mob_duplicator','BioReactor':'bioreactor','BioFuelGenerator':'biofuel_generator','Slaughterhouse':'mob_slaughter_factory','EnchantmentRouter':'enchantment_sorter','LaserDrill':'ore_laser_base','LaserDrillPrecharger':'laser_drill','AutoAnvil':'enchantment_applicator','BlockPlacer':'block_placer','Composter':'sewage_composter'}
 source=(ROOT/'evidence/legacy-source/powercrystals/minefactoryreloaded/setup/Machine.java').read_text(encoding='utf-8')
 for bank,meta,name in re.findall(r'new Machine\((\d+), (\d+), "([^"]+)"',source):
  if name in machine_names:add('MineFactoryReloaded:machine.'+bank,meta,'industrialforegoing:'+machine_names[name],'legacy Machine explicit bank/metadata; corresponding Industrial Foregoing machine function')
 for old,new in [('plastic.raw','plastic'),('plastic.sheet','plastic'),('safarinet.reusable','mob_imprisonment_tool'),('laserfocus','laser_lens')]:allmeta('MineFactoryReloaded:'+old,'industrialforegoing:'+new,'shared successor component/tool; native modern registration')
 for old,new in [('biofuel','biofuel'),('mobessence','essence'),('meat','meat'),('pinkslime','pink_slime'),('sewage','sewage'),('sludge','sludge')]:
  target='industrialforegoing:'+new
  if target in invs['MineFactoryReloaded']['blocks']:
   for (id,meta) in rows['MineFactoryReloaded']:
    if id=='MineFactoryReloaded:'+old+'.still':maps[id+'@'+meta]={'targets':[target],'kind':'block_aspects','reason':'same successor technical liquid block; fluids have no ordinary block item'}
  allmeta('MineFactoryReloaded:bucket.'+old,'industrialforegoing:'+new+'_bucket','same successor liquid bucket')
family('WitchingGadgets:item.WG_Baubles',{4:'witchinggadgets:title_crimson_cult'},'legacy ItemMagicalBaubles.subNames index 4; port WGAccessories.TYPES and native item registration')
family('ForbiddenMagic:NetherShard',{i:'forbidden_magic:'+sin+'_shard' for i,sin in enumerate(['wrath','envy','taint','pride','lust','sloth','greed'])},'legacy ItemDeadlyShard.vices ordinal; port Content.SINS named items')
family('ForbiddenMagic:TaintBlock',{0:'forbidden_magic:taint_stone',1:'forbidden_magic:taint_bricks'},'legacy ItemBlockStoneTainted smooth/brick metadata')
family('gadomancy:ItemAuraCore',{i:'gadomancy:aura_core_'+n for i,n in enumerate(['blank','aer','ignis','aqua','terra','ordo','perditio'])},'legacy AuraCoreType ordinal; modern AuraCoreItem aspect registration')
family('gadomancy:BlockAuraPylon',{0:'gadomancy:aura_pylon',1:'gadomancy:aura_pylon_cap'},'legacy pylon base/peak metadata; modern separate registrations')
family('gadomancy:ItemArcanePackage',{0:'gadomancy:arcane_package',1:'gadomancy:disguised_package'},'legacy ArcanePackage normal/disguised subtype; modern separate items')
family('thaumicbases:knoseFragment',{i:'thaumicbases:knose_fragment_'+n for i,n in enumerate(['air','fire','water','earth','order','entropy','mixed','tainted'])},'legacy ItemKnoseFragment names/order; modern Content crystal fragment kinds')
family('thaumicbases:resource',{5:'thaumicbases:aurelia_petal',6:'thaumicbases:briar_seedbag',7:'thaumicbases:tobacco_leaves'},'legacy TBResource.names indices; modern corresponding named materials')
allmeta('thaumicbases:fociExperience','thaumicbases:focus_experience','same experience focus, modern snake_case ID')
allmeta('thaumicbases:plaxSeed','thaumicbases:plax_seeds','same Plax seeds, modern plant definition')
family('thaumicbases:tobaccoPowder',{0:'thaumicbases:tobacco_pile'},'legacy TBTobacco.names index 0; modern named pile dose')
family('thaumicenergistics:storage.essentia',{i:'thaumicenergistics:essentia_cell_'+n for i,n in {0:'1k',1:'4k',2:'16k',3:'64k',4:'creative',5:'256k',6:'1024k',7:'4096k',8:'16384k',9:'quantum',10:'singularity'}.items()},'legacy EnumEssentiaStorageTypes explicit indices across both list versions; same modern cell capacity/type')
horizons={'barChocolate':'chocolate_bar','bucketChocolate':'chocolate_milk','boneTH':'bone','brainTH':'brain','dustTH':'dust','eyeTH':'eyes','fleshTH':'flesh','meatCookedTH':'cooked_meat','meatNuggetTH':'meat_nugget','crystalDeep':'deep_crystal','spikeTH':'metal_spike','spikeToothTH':'tooth_spike','spikeWoodTH':'wooden_spike','voidTH':'void','vortexTH':'vortex','cloudTH':'cloud','syringeBloodSample':'blood_sample','leviathanTH':'leviathan','portalTH':'gateway_portal','vatSolid':'vat'}
for old,new in horizons.items():allmeta('ThaumicHorizons:'+old,'thaumichorizons:'+new,'same Horizons content, legacy TH suffix removed or native descriptive registration name')
family('ThaumicHorizons:crystalTH',{0:'thaumichorizons:crystal_white'},'legacy crystal color 0 white; port PlanarDecorations dye-color registrations')
for old,new in [('syringeBlood',['human_blood','blood_phial']),('syringeInjection',['injection','injection_phial'])]:
 allmeta('ThaumicHorizons:'+old,['thaumichorizons:'+n for n in new],'legacy wildcard syringe item covers both metadata vessel variants; port separates vessels')
for (id,meta) in rows['ThaumicHorizons']:
 if id=='ThaumicHorizons:focusIllumination' and meta in ('32767','-1'):
  targets=sorted(t for t in invs['ThaumicHorizons']['items'] if t.startswith('thaumichorizons:focus_illumination_'))
  add(id,meta,targets,'legacy wildcard illumination focus; all native modern color focus registrations')
if 'BiomesOPlenty' in invs:
 bop=ROOT/'evidence/legacy-source/biomesoplenty/common';table={}
 for group,source,var,kind in [('logs','blocks/BlockBOPLog.java','types','log'),('leaves','blocks/BlockBOPLeaves.java','leaves','leaves'),('colorizedLeaves','blocks/BlockBOPColorizedLeaves.java','leaves','leaves')]:
  names=g.family_names.array((bop/source).read_text(encoding='utf-8'),var)
  for i,name in enumerate(names):table[(group+str(i//4+1),i%4)]=(name,kind)
 for i,name in enumerate(g.family_names.array((bop/'itemblocks/ItemBlockPlank.java').read_text(encoding='utf-8'),'woodTypes')):table[('planks',i)]=(name.removesuffix('Plank'),'planks')
 available=set(invs['BiomesOPlenty']['items'])|set(invs['BiomesOPlenty']['blocks'])
 for (id,meta) in rows['BiomesOPlenty']:
  old=id.split(':')[1]
  if not any(group==old for group,index in table):continue
  variants=[(name,kind) for (group,index),(name,kind) in table.items() if group==old] if meta in ('32767','-1') else [table[(old,int(meta)&3 if old!='planks' else int(meta))]] if meta.isdigit() and (old,int(meta)&3 if old!='planks' else int(meta)) in table else []
  targets=sorted({target for name,kind in variants for target in available if g.norm(target.split(':')[1])==g.norm(name+'_'+kind)})
  maps[id+'@'+meta]={'targets':targets,'reason':'legacy BOP tree category ordinal and type/axis bits; exact same named modern tree form, unsupported trees excluded'}
botania_colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
for old,new in [('GlassBlock','clear_glass'),('GlassPane','clear_glass_pane'),('CastingChannel','seared_channel'),('woodPattern','pattern'),('Pattern','pattern')]:allmeta('TConstruct:'+old,'tconstruct:'+new,'same native TiC3 glass/channel or modern untyped part-builder pattern replacing shaped stencils')
for old,kind in [('GlassBlock.StainedClear','clear_stained_glass'),('GlassPaneClearStained','clear_stained_glass_pane')]:
 family('TConstruct:'+old,{i:'tconstruct:'+color+'_'+kind for i,color in enumerate(botania_colors)},'legacy GlassBlockConnectedMeta stained color ordinal; same native modern clear glass color')
for old,prefix in [('SearedBlock','seared'),('SearedBlockNether','scorched')]:
 family('TConstruct:'+old,{0:'tconstruct:'+prefix+'_table',1:'tconstruct:'+prefix+'_faucet',2:'tconstruct:'+prefix+'_basin'},'legacy SearedTableItemBlock Table/Faucet/Basin ordinals; corresponding smeltery/foundry cast hardware')
for old,prefix in [('Smeltery','seared'),('SmelteryNether','scorched')]:
 table={1:'drain',2:'bricks',4:'stone',5:'cobble',6:'paver',7:'cracked_bricks',8:'road',9:'fancy_bricks',10:'square_bricks'}
 family('TConstruct:'+old,{i:'tconstruct:'+prefix+'_'+name for i,name in table.items()},'legacy SmelteryItemBlock ordinal; corresponding drain or same decorative masonry style')
family('TConstruct:Smeltery',{0:'tconstruct:seared_smeltery_controller'},'legacy smeltery controller; same native modern controller')
family('TConstruct:fletching',{0:'tconstruct:fletching'},'legacy ordinary feather fletching; native modern feather material')
if 'TConstruct:fletching@0' in maps:maps['TConstruct:fletching@0']['nbt']={'Material':'tconstruct:feather'}
family('Botania:livingwood',{3:'botania:framed_livingwood',4:'botania:pattern_framed_livingwood'},'legacy framed livingwood styles; corresponding modern named blocks')
family('Botania:dreamwood',{3:'botania:framed_dreamwood',4:'botania:pattern_framed_dreamwood'},'legacy framed dreamwood styles; corresponding modern named blocks')
family('Botania:manaResource',{20:'botania:living_root'},'legacy LibItemNames.MANA_RESOURCE_NAMES living root; modern descriptive ID')
for old,new in [('distributor','mana_distributor'),('gaiaHead','gaia_head'),('gaiaHeadBlock','gaia_head'),('recordGaia1','record_gaia_1'),('recordGaia2','record_gaia_2'),('root','living_root')]:allmeta('Botania:'+old,'botania:'+new,'same Botania content; descriptive modern registry ID')
family('Botania:lens',{0:'botania:lens_normal'},'legacy ordinary unmodified mana lens; modern named normal lens')
allmeta('PneumaticCraft:plastic','pneumaticcraft:plastic','legacy colored plastic materials consolidated into native uncolored plastic; no modern color NBT variant')
for old,kind in [('flower','mystical_flower'),('petal','petal'),('mushroom','mushroom'),('buriedPetals','buried_petals')]:
 for (id,meta) in rows['Botania']:
  if id!='Botania:'+old:continue
  targets=['botania:'+c+'_'+kind for c in botania_colors] if meta in ('32767','-1') else ['botania:'+botania_colors[int(meta)]+'_'+kind] if meta.isdigit() and int(meta)<16 else []
  if targets:add(id,meta,targets,'legacy Botania color ordinal; corresponding named modern color variant')
for old,offset in [('doubleFlower1',0),('doubleFlower2',8)]:
 for (id,meta) in rows['Botania']:
  if id!='Botania:'+old:continue
  indices=range(offset,offset+8) if meta in ('32767','-1') else [offset+(int(meta)&7)] if meta.isdigit() and int(meta)<16 else []
  targets=['botania:'+botania_colors[i]+'_double_flower' for i in indices]
  if targets:add(id,meta,targets,'legacy BlockModDoubleFlower offset plus lower three color bits; upper-half state shares same modern item')
family('Botania:rune',{i:'botania:rune_'+n for i,n in enumerate(['water','fire','earth','air','spring','summer','autumn','winter','mana','lust','gluttony','greed','sloth','wrath','envy','pride'])},'legacy ItemRune metadata and localized rune identity; modern named runes')
family('Botania:quartz',{i:'botania:quartz_'+n for i,n in enumerate(['dark','mana','blaze','lavender','red','elven','sunny'])},'legacy ItemQuartz metadata and color type; modern named quartz items')
family('Forestry:pollen',{0:'forestry:pollen_cluster_normal',1:'forestry:pollen_cluster_crystalline'},'legacy EnumPollenCluster ordinal; modern separate cluster IDs')
family('Forestry:propolis',{i:'forestry:propolis_'+n for i,n in enumerate(['normal','sticky','pulsating','silky'])},'legacy EnumPropolis ordinal; modern separate propolis IDs')
family('Forestry:soil',{0:'forestry:humus',1:'forestry:bog_earth',2:'forestry:peat'},'legacy BlockSoil.SoilType ordinal; modern same soil')
poppets={0:'poppet',1:'earth_poppet',2:'water_poppet',3:'fire_poppet',4:'hunger_poppet',5:'tool_poppet',7:'voodoo_protection_poppet',8:'voodoo_poppet',11:'armour_poppet'}
for (id,meta) in rows['witchery']:
 if id=='witchery:poppet':
  target='enchanted:'+poppets[int(meta)] if meta.isdigit() and int(meta) in poppets else None
  maps[id+'@'+meta]={'targets':[target] if target in invs['witchery']['items'] else [],'reason':'legacy ItemPoppet.PoppetType explicit damageValue; modern protection-type item, unmatched effects excluded'}
for (id,meta) in rows['harvestcraft']:
 old=id.split(':')[1]
 if old.startswith('pam'):
  target='pamhc2trees:'+old.lower()
  if target in invs['harvestcraft']['blocks']:add(id,meta,target,'legacy BlockPamFruit is a tree fruit block, not its edible drop; HC2 corresponding fruit block')
if 'gregtech' in invs:
 available=set(invs['gregtech']['items']);materials={k[len('material.gtceu.'):] for k in invs['gregtech']['lang'] if k.startswith('material.gtceu.')}
 kinds={'Sword':'sword','Pickaxe':'pickaxe','Shovel':'shovel','Axe':'axe','Hoe':'hoe','Saw':'saw','Hammer':'hammer','Soft Mallet':'mallet','Wrench':'wrench','File':'file','Crowbar':'crowbar','Screwdriver':'screwdriver','Mortar':'mortar','Wire Cutter':'wire_cutter','Scoop':'scoop','Branch Cutter':'branch_cutter','Universal Spade':'spade','Knife':'knife','Butchery Knife':'butchery_knife','Sense':'scythe','Plow':'plow','Plunger':'plunger','Rolling Pin':'rolling_pin','Drill':'drill','Chainsaw':'chainsaw','Buzzsaw':'buzzsaw','JackHammer':'jackhammer','Soldering Iron':'soldering_iron'}
 for (id,meta),row in rows['gregtech'].items():
  for english in sorted(row['names']):
   match=re.fullmatch(r'(.+) \((LV|MV|HV|EV|IV|LuV|ZPM|UV)\)',english)
   name,tier=(match.group(1),match.group(2).lower()) if match else (english,None)
   if id=='gregtech:gt.metatool.01' and name in kinds:
    kind=kinds[name];targets=sorted(t for material in materials if (t:='gtceu:'+(tier+'_' if tier else '')+material+'_'+kind) in available)
    if targets:add(id,meta,targets,'legacy metatool kind/tier applies to every native modern material registration of the same kind/tier')
   elif 'metaitem' in id and tier and name in ['Electric Motor','Electric Pump','Electric Piston','Conveyor Module','Robot Arm','Field Generator','Emitter','Sensor']:
    add(id,meta,'gtceu:'+tier+'_'+name.lower().replace(' ','_'),'same GT component and voltage tier; legacy suffix tier becomes modern ID prefix')
with zipfile.ZipFile(next((ROOT/'evidence/modern-jars').glob('AgriCraft*.jar'))) as jar:
 plants={}
 for n in jar.namelist():
  match=re.search(r'(?:^|/)data/([^/]+)/agricraft/plants/([^/]+)\.json$',n)
  if match:plants[g.norm(match.group(2))]=match.group(1)+':'+match.group(2)
for (id,meta) in rows['AgriCraft']:
 old=id.split(':')[1]
 if not old.startswith('seed') or old=='seedAnalyzer':continue
 name=old[4:]
 renamed={'Daisy':'oxeye_daisy','Orchid':'blue_orchid','Sugarcane':'sugar_cane','ShroomBrown':'brown_mushroom','ShroomRed':'red_mushroom'}
 if name.startswith('Botania'):
  color=re.sub(r'([a-z])([A-Z])',r'\1_\2',name[7:]).lower();name=color+'_mystical_flower'
 else:name=renamed.get(name,name)
 plant=plants.get(g.norm(name))
 if plant:maps[id+'@'+meta]={'targets':['agricraft:seed'],'nbt':{'genes':{'species':{'dom':plant}}},'reason':'same native plant datapack ID; AgriGenome.genes and GeneSpecies.species.dom; recessive traits and stats excluded'}
singularities=['iron','gold','lapis_lazuli','redstone','quartz','copper','tin','lead','silver','nickel','enderium','clay']
with zipfile.ZipFile(next((ROOT/'evidence/modern-jars').glob('*Avaritia*.jar'))) as jar:
 available_singularities={pathlib.PurePosixPath(n).stem for n in jar.namelist() if n.startswith('data/avaritia/singularities/') and n.endswith('.json')}
for (id,meta) in rows['Avaritia']:
 if id!='Avaritia:Singularity':continue
 name=singularities[int(meta)] if meta.isdigit() and int(meta)<len(singularities) else None
 maps[id+'@'+meta]={'targets':['avaritia:singularity'] if name in available_singularities else [],'nbt':{'Id':'avaritia:'+name} if name in available_singularities else {},'reason':'legacy ItemSingularity.types; modern SingularityUtils.makeTag root Id and pinned singularity definitions'}
materials={0:'wood',1:'rock',2:'iron',3:'flint',4:'cactus',5:'bone',6:'obsidian',8:'earthslime',9:'paper',10:'cobalt',12:'manyullyn',13:'copper',14:'bronze',16:'steel',17:'skyslime',18:'pig_iron'}
part_stats={'pickaxeHead':('pick_head','head'),'hatchetHead':('small_axe_head','head'),'swordBlade':('small_blade','head'),'knifeBlade':('small_blade','head'),'largeSwordBlade':('broad_blade','head'),'broadAxeHead':('broad_axe_head','head'),'hammerHead':('hammer_head','head'),'toolRod':('tool_handle','handle'),'toughRod':('tough_handle','handle'),'binding':('tool_binding','binding'),'toughBinding':('tough_binding','binding'),'heavyPlate':('large_plate','head'),'BowLimbPart':('bow_limb','limb'),'CrossbowLimbPart':('bow_limb','limb'),'CrossbowBodyPart':('bow_grip','grip'),'arrowhead':('arrow_head','arrow_head')}
with zipfile.ZipFile(next((ROOT/'evidence/modern-jars').glob('TConstruct*.jar'))) as jar:
 material_stats={pathlib.PurePosixPath(n).stem:set(json.loads(jar.read(n))['stats']) for n in jar.namelist() if n.startswith('data/tconstruct/tinkering/materials/stats/') and n.endswith('.json')}
 material_variants={}
 for n in jar.namelist():
  if not n.startswith('data/tconstruct/recipes/') or not n.endswith('.json') or '/materials/' not in n:continue
  data=json.loads(jar.read(n))
  for key in ['material','output']:
   value=data.get(key)
   if isinstance(value,str) and value.startswith('tconstruct:') and '#' in value:material_variants.setdefault(value.split('#')[0],set()).add(value)
def material_selector(material):
 base='tconstruct:'+material;variants=material_variants.get(base,set())
 return {'$in':[base,*sorted(variants)]} if variants else base
for (id,meta) in rows['TConstruct']:
 old=id.split(':')[1]
 if old in part_stats:
  target,stat=part_stats[old];material=materials.get(int(meta)) if meta.isdigit() else None
  usable=material is not None and 'tconstruct:'+stat in material_stats.get(material,set())
  maps[id+'@'+meta]={'targets':['tconstruct:'+target] if usable else [],'nbt':{'Material':material_selector(material)} if usable else {},'reason':'legacy TinkerTools material ID; native IMaterialItem.Material; matching modern part stat and recipe-declared render variants' if usable else 'legacy material has no usable counterpart for this part in pinned TiC3 stats'}
 elif old=='bowstring':
  # Old bowstring IDs are string / enchanted fabric / flamestring, not tool materials.
  material='string' if meta=='0' else None
  maps[id+'@'+meta]={'targets':['tconstruct:bowstring'] if material else [],'nbt':{'Material':'tconstruct:string'} if material else {},'reason':'legacy TinkerWeaponry.addBowstringMaterial; only ordinary string has verified native counterpart'}
 elif old=='Cast':
  # WeaponryPattern has its own four-shape ordinal, unlike smeltery metalPattern.
  shape={1:'bow_limb',2:'bow_grip',3:'bow_limb'}.get(int(meta)) if meta.isdigit() else None
  target='tconstruct:'+shape+'_cast' if shape else None
  maps[id+'@'+meta]={'targets':[target] if target in invs['TConstruct']['items'] else [],'reason':'legacy WeaponryPattern.patternName ordinal; matching modern reusable cast shape'}
for entry in maps.values():
 if 'item-wide maximum union' in entry['reason']:
  entry['reason']='TiC1 to TiC3 corresponding tool/station; metadata is not a part material selector'
g.write(ROOT/'tools/mappings.json',dict(sorted(maps.items())))
print('Curated variant mappings',len(maps))
