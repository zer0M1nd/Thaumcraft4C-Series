package tc4revival.data;

/** Runtime key adaptation only. Original text is supplied by the player's JAR. */
public  class LegacyLanguageTranslations {
    private static final java.util.Map<String,String> EXTRA_ALIASES=java.util.Map.ofEntries(
            java.util.Map.entry("block.thaumcraft.silverwood_node_log","tile.blockMagicalLog.silverwood.name"),
            java.util.Map.entry("block.thaumcraft.warding_barrier","tile.blockAiry.4.name"),
            java.util.Map.entry("block.thaumcraft.lamp_air","tile.blockAiry.3.name"),
            java.util.Map.entry("block.thaumcraft.shock_field","tile.blockAiry.10.name"),
            java.util.Map.entry("block.thaumcraft.eldritch_field","tile.blockAiry.11.name"),
            java.util.Map.entry("block.thaumcraft.eldritch_door_barrier","tile.blockAiry.12.name"),
            java.util.Map.entry("block.thaumcraft.eldritch_portal","tile.blockPortalEldritch.name"),
            java.util.Map.entry("block.thaumcraft.portable_hole","tile.blockHole.name"),
            java.util.Map.entry("block.thaumcraft.warded","tile.blockSecure.name"),
            java.util.Map.entry("container.arcaneworkbench","tile.blockTable.15.name"));
    private LegacyLanguageTranslations() {}
    public static void append(java.util.List<String> lines, boolean chinese,
            java.util.function.BiConsumer<String,String> sink) {
                var legacyLines=new java.util.LinkedHashMap<String,String>();
                for (String line:lines) {
                    int separator=line.indexOf('='); if (separator>0) legacyLines.put(line.substring(0,separator),line);
                }
                for (String line : legacyLines.values()) {
                    int equals = line.indexOf('=');
                    if (equals <= 0) continue;
                    String key = line.substring(0, equals);
                    EXTRA_ALIASES.forEach((modern,legacy)->{if(legacy.equals(key))sink.accept(modern,line.substring(equals+1));});
                    if(key.equals("item.warping")) sink.accept("attribute.name.thaumcraft.warp",line.substring(equals+1));
                    if(key.equals("item.runic.charge")) sink.accept("attribute.name.thaumcraft.runic_shield",line.substring(equals+1));
                    if (key.equals("item.ItemPrimalCrusher.name")) sink.accept("item.thaumcraft.primal_crusher",line.substring(equals+1));
                    if (key.equals("item.ItemBowBone.name")) sink.accept("item.thaumcraft.bone_bow",line.substring(equals+1));
                    for (int element=0;element<6;element++) if (key.equals("item.ItemPrimalArrow."+element+".name"))
                        sink.accept("item.thaumcraft.primal_arrow_"+tc4revival.content.ShardType.values()[element].material(),line.substring(equals+1));
                    for (String arrow:java.util.List.of("airarrow","firearrow","orderarrow")) if (key.startsWith("death.attack."+arrow)) sink.accept(key,line.substring(equals+1));
                    if (key.equals("tile.blockCosmeticSolid.4.name")) sink.accept("block.thaumcraft.thaumium_block",line.substring(equals+1));
                    if (key.equals("tile.blockCosmeticSolid.9.name")) sink.accept("block.thaumcraft.golem_fetter",line.substring(equals+1));
                    for (var identity:GolemResources.identities()) {
                        String oldName=identity.legacy().equals("GolemBell")?"item.GolemBell.name":identity.legacy().equals("ItemGolemCore")?"item.ItemGolemCore.name":"item."+identity.legacy()+"."+identity.metadata()+".name";
                        if (key.equals(oldName)) sink.accept("item.thaumcraft."+identity.path(),line.substring(equals+1));
                    }
                    if (key.equals("entity.Thaumcraft.Golem.name")) sink.accept("entity.thaumcraft.golem",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.Dart.name")) sink.accept("entity.thaumcraft.golem_dart",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.GolemBobber.name")) sink.accept("entity.thaumcraft.golem_bobber",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.TravelingTrunk.name")) sink.accept("entity.thaumcraft.traveling_trunk",line.substring(equals+1));
                    if (key.equals("item.TrunkSpawner.name")) sink.accept("item.thaumcraft.traveling_trunk",line.substring(equals+1));
                    if (key.equals("tile.blockChestHungry.name")) sink.accept("block.thaumcraft.hungry_chest",line.substring(equals+1));
                    if (key.startsWith("entity.trunk.") || key.equals("item.TrunkSpawner.text.1")) sink.accept(key,line.substring(equals+1));
                    if (key.startsWith("golemblurb.") || key.startsWith("golemthreat.") || key.startsWith("item.ItemGolemCore.") || key.startsWith("item.ItemGolemUpgrade.") || key.startsWith("item.ItemGolemDecoration.") || key.equals("tc.adv") || key.equals("tc.markedloc")) sink.accept(key,line.substring(equals+1));
                    if (key.equals("item.ItemEldritchObject.1.name")) sink.accept("item.thaumcraft.crimson_rites",line.substring(equals+1));
                    if (key.equals("item.ItemSwordCrimson.name")) sink.accept("item.thaumcraft.crimson_sword",line.substring(equals+1));
                    for (var armor:tc4revival.content.CultistEquipment.Armor.values())
                        if (key.equals("item."+armor.legacy+".name")) sink.accept("item.thaumcraft."+armor.path,line.substring(equals+1));
                    if (key.equals("item.ItemEldritchObject.0.name")) sink.accept("item.thaumcraft.eldritch_eye",line.substring(equals+1));
                    for (var cleansing:CleansingResources.IDENTITIES)
                        if (key.equals((cleansing[1].startsWith("block") ? "tile." : "item.")+cleansing[1]+(cleansing[1].equals("blockCosmeticSolid") ? ".5" : "")+".name"))
                            sink.accept((cleansing[1].startsWith("block") ? "block.thaumcraft." : "item.thaumcraft.")+cleansing[0],line.substring(equals+1));
                    for (var armor:FortressResources.all())
                        if (key.equals("item."+(armor.length>4?armor[4]:armor[1])+".name")) sink.accept("item.thaumcraft."+armor[0],line.substring(equals+1));
                    for (var device:UtilityDeviceResources.IDENTITIES)
                        if (key.equals("tile."+device[1]+(device[2].equals("0")?"":"."+device[2])+".name")) sink.accept("block.thaumcraft."+device[0],line.substring(equals+1));
                    for (var lamp:ArcaneLampResources.IDENTITIES)
                        if (key.equals("tile."+lamp[1]+"."+lamp[2]+".name")) sink.accept("block.thaumcraft."+lamp[0],line.substring(equals+1));
                    for (var bore:ArcaneBoreResources.IDENTITIES)
                        if (key.equals("tile."+bore[1]+"."+bore[2]+".name")) sink.accept("block.thaumcraft."+bore[0],line.substring(equals+1));
                    if (key.equals("item.ItemPickaxeElemental.name")) sink.accept("item.thaumcraft.elemental_pickaxe",line.substring(equals+1));
                    if (key.equals("item.ItemHoeElemental.name")) sink.accept("item.thaumcraft.elemental_hoe",line.substring(equals+1));
                    if (key.equals("item.ItemAxeElemental.name")) sink.accept("item.thaumcraft.elemental_axe",line.substring(equals+1));
                    if (key.equals("item.ItemShovelElemental.name")) sink.accept("item.thaumcraft.elemental_shovel",line.substring(equals+1));
                    if (key.equals("item.ItemSwordElemental.name")) sink.accept("item.thaumcraft.elemental_sword",line.substring(equals+1));
                    if (key.startsWith("item.HelmetFortress.mask.")) sink.accept(key,line.substring(equals+1));
                    if (key.equals("item.ItemEldritchObject.2.name")) sink.accept("item.thaumcraft.runed_tablet",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.CultistPortal.name")) sink.accept("entity.thaumcraft.cultist_portal",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.Taintacle.name")) sink.accept("entity.thaumcraft.taintacle",line.substring(equals+1));
                    for(var egg:tc4revival.content.LegacyMobEggs.entries())if(!java.util.Set.of("eldritch_warden","eldritch_golem","cultist_leader").contains(egg.path())&&key.equals("entity.Thaumcraft."+egg.legacyName()+".name"))
                        sink.accept("item.thaumcraft."+egg.path()+"_spawn_egg",line.substring(equals+1).strip()+(chinese?"刷怪蛋":" Spawn Egg"));
                    if (key.equals("entity.Thaumcraft.TaintacleTiny.name")) sink.accept("entity.thaumcraft.taintacle_small",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.TaintacleGiant.name")) sink.accept("entity.thaumcraft.taintacle_giant",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.CultistKnight.name")) sink.accept("entity.thaumcraft.cultist_knight",line.substring(equals+1).replace("%d","%s"));
                    if (key.equals("entity.Thaumcraft.CultistCleric.name")) sink.accept("entity.thaumcraft.cultist_cleric",line.substring(equals+1).replace("%d","%s"));
                    if (key.equals("entity.Thaumcraft.CultistLeader.name")) sink.accept("entity.thaumcraft.cultist_leader",line.substring(equals+1).replace("%d","%s"));
                    if (key.equals("entity.Thaumcraft.BrainyZombie.name")) {sink.accept("entity.thaumcraft.brainy_zombie",line.substring(equals+1));sink.accept("item.thaumcraft.brainy_zombie_spawn_egg",line.substring(equals+1)+(chinese?"刷怪蛋":" Spawn Egg"));}
                    if (key.equals("entity.Thaumcraft.GiantBrainyZombie.name")) {sink.accept("entity.thaumcraft.giant_brainy_zombie",line.substring(equals+1));sink.accept("item.thaumcraft.giant_brainy_zombie_spawn_egg",line.substring(equals+1)+(chinese?"刷怪蛋":" Spawn Egg"));}
                    if (key.equals("entity.Thaumcraft.Wisp.name")) {sink.accept("entity.thaumcraft.wisp",line.substring(equals+1));sink.accept("item.thaumcraft.wisp_spawn_egg",line.substring(equals+1)+(chinese?"刷怪蛋":" Spawn Egg"));}
                    if (key.equals("entity.Thaumcraft.InhabitedZombie.name")) sink.accept("entity.thaumcraft.inhabited_zombie",line.substring(equals+1).replace("%d","%s"));
                    if (key.equals("entity.Thaumcraft.EldritchGolem.name")) sink.accept("entity.thaumcraft.eldritch_golem",line.substring(equals+1));
                    if (key.startsWith("champion.mod.")) sink.accept(key,line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.EldritchWarden.name")) sink.accept("entity.thaumcraft.eldritch_warden",line.substring(equals+1).replace("%d","%s"));
                    if (key.equals("entity.Thaumcraft.EldritchGuardian.name")) sink.accept("entity.thaumcraft.eldritch_guardian",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.EldritchOrb.name")) sink.accept("entity.thaumcraft.eldritch_orb",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.EldritchCrab.name")) sink.accept("entity.thaumcraft.eldritch_crab",line.substring(equals+1));
                    String[] eldritchDevices={"eldritch_altar","eldritch_obelisk","eldritch_obelisk_segment","eldritch_cap","eldritch_glowing_stone","eldritch_glyphed_stone","eldritch_carved_stone","eldritch_door_frame","eldritch_lock","eldritch_crab_spawner","eldritch_rune_trap"};
                    for (int index=0;index<eldritchDevices.length;index++) if (key.equals("tile.blockEldritch."+index+".name")) sink.accept("block.thaumcraft."+eldritchDevices[index],line.substring(equals+1));
                    for(int ore=0;ore<8;ore++)if(key.equals("tile.blockCustomOre."+ore+".name"))sink.accept("block.thaumcraft."+tc4revival.content.LegacyOreNames.path(ore),line.substring(equals+1));
                    if (key.equals("tile.blockCosmeticSolid.0.name")) { sink.accept("block.thaumcraft.obsidian_totem",line.substring(equals+1));sink.accept("block.thaumcraft.obsidian_node_totem",line.substring(equals+1)); }
                    if (key.equals("tile.blockCosmeticSolid.1.name")) sink.accept("block.thaumcraft.obsidian_tile",line.substring(equals+1));
                    if (key.equals("tile.blockCrystal.7.name")) sink.accept("block.thaumcraft.eldritch_crystal",line.substring(equals+1));
                    if (key.equals("item.ItemEldritchObject.4.name")) sink.accept("item.thaumcraft.obelisk_placer",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.MindSpider.name")) sink.accept("entity.thaumcraft.mind_spider",line.substring(equals+1));
                    String[] eldritchStoneNames={"eldritch_stone","eldritch_rock","eldritch_stone_nonspawning","eldritch_crust","eldritch_pedestal_stone"};
                    for (int index=0;index<eldritchStoneNames.length;index++) if (key.equals("tile.blockCosmeticSolid."+(11+index)+".name"))
                        sink.accept("block.thaumcraft."+eldritchStoneNames[index],line.substring(equals+1));
                    if (key.equals("tile.blockStairsEldritch.name")) sink.accept("block.thaumcraft.eldritch_stairs",line.substring(equals+1));
                    if (key.equals("tile.blockCosmeticSlabStone.eldritch.name")) sink.accept("block.thaumcraft.eldritch_slab",line.substring(equals+1));
                    for (boolean crate:new boolean[]{false,true}) if (key.equals(crate ? "tile.blockLootCrate.name" : "tile.blockLootUrn.name"))
                        for (int rarity=0;rarity<3;rarity++) sink.accept("block.thaumcraft.loot_"+(crate ? "crate_" : "urn_")+rarity,line.substring(equals+1));
                    for (var kind:tc4revival.content.MagicalPlantBlock.Kind.values()) if (key.equals("tile.blockCustomPlant."+kind.metadata+".name")) sink.accept("block.thaumcraft."+kind.path,line.substring(equals+1));
                    for (int wood=0;wood<2;wood++) {
                        String name=wood==0 ? "greatwood" : "silverwood";
                        if (key.equals("tile.blockWoodenDevice."+(wood+6)+".name")) sink.accept("block.thaumcraft."+name+"_planks",line.substring(equals+1));
                        if (key.equals("tile.blockCosmeticSlabWood."+name+".name")) sink.accept("block.thaumcraft."+name+"_slab",line.substring(equals+1));
                        if (key.equals("tile.blockStairs"+(wood==0 ? "Greatwood" : "Silverwood")+".name")) sink.accept("block.thaumcraft."+name+"_stairs",line.substring(equals+1));
                        if (key.equals("tile.blockMagicalLog."+name+".name")) sink.accept("block.thaumcraft."+name+"_log",line.substring(equals+1));
                        if (key.equals("tile.blockMagicalLeaves."+name+".name")) sink.accept("block.thaumcraft."+name+"_leaves",line.substring(equals+1));
                        if (key.equals("tile.blockCustomPlant."+wood+".name")) sink.accept("block.thaumcraft."+name+"_sapling",line.substring(equals+1));
                    }
                    for (var type:tc4revival.content.RunicBaubleType.values()) if (key.equals("item."+type.legacy()+"."+type.metadata()+".name")) sink.accept("item.thaumcraft."+type.path(),line.substring(equals+1));
                    for (var type:tc4revival.content.ShardType.values()) if (key.equals("tile.blockCrystal."+type.metadata()+".name")) sink.accept("block.thaumcraft."+type.crystalClusterPath(),line.substring(equals+1));
                    if (key.equals("item.runic.charge")) sink.accept(key,line.substring(equals+1));
                    if (key.equals("item.ItemResonator.name")) sink.accept("item.thaumcraft.essentia_resonator",line.substring(equals+1));
                    if (key.startsWith("tc.resonator")) sink.accept(key,line.substring(equals+1));
                    if (key.equals("potion.visexhaust")) sink.accept("effect.thaumcraft.vis_exhaust", line.substring(equals + 1));
                    if (key.equals("tile.blockMetalDevice.0.name")) sink.accept("block.thaumcraft.crucible",line.substring(equals+1));
                    if (key.equals("tile.blockMetalDevice.1.name")) sink.accept("block.thaumcraft.alembic",line.substring(equals+1));
                    if (key.equals("tile.blockEssentiaReservoir.name")) sink.accept("block.thaumcraft.essentia_reservoir",line.substring(equals+1));
                    if (key.equals("tile.blockMirror.0.name")) sink.accept("block.thaumcraft.item_mirror",line.substring(equals+1));
                    if (key.equals("item.HandMirror.name")) sink.accept("item.thaumcraft.hand_mirror",line.substring(equals+1));
                    if (key.startsWith("tc.handmirror")) sink.accept(key,line.substring(equals+1));
                    if (key.equals("tile.blockMirror.6.name")) sink.accept("block.thaumcraft.essentia_mirror",line.substring(equals+1));
                    if (key.equals("tile.blockArcaneFurnace.name")) sink.accept("block.thaumcraft.infernal_furnace",line.substring(equals+1));
                    if (key.equals("tile.blockMetalDevice.9.name")) sink.accept("block.thaumcraft.alchemical_construct",line.substring(equals+1));
                    if (key.equals("tile.blockCosmeticOpaque.2.name")) sink.accept("block.thaumcraft.warded_glass",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.Pech.name")) { sink.accept("entity.thaumcraft.pech",line.substring(equals+1)); sink.accept("item.thaumcraft.pech_spawn_egg",line.substring(equals+1)+(chinese ? "刷怪蛋" : " Spawn Egg")); }
                    if (key.equals("entity.Thaumcraft.Pech.1.name")) sink.accept("entity.thaumcraft.pech.1",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.Pech.2.name")) sink.accept("entity.thaumcraft.pech.2",line.substring(equals+1));
                    for (int color=0;color<16;color++) if (key.equals("tile.blockCandle."+color+".name")) sink.accept("block.thaumcraft.tallow_candle_"+color,line.substring(equals+1));
                    if (key.equals("item.ItemArcaneDoor.name")) { sink.accept("block.thaumcraft.arcane_door",line.substring(equals+1)); sink.accept("item.thaumcraft.arcane_door",line.substring(equals+1)); }
                    if (key.equals("item.ArcaneDoorKey.0.name")) sink.accept("item.thaumcraft.iron_key",line.substring(equals+1));
                    if (key.equals("item.ArcaneDoorKey.1.name")) sink.accept("item.thaumcraft.gold_key",line.substring(equals+1));
                    if (key.startsWith("tc.key")) sink.accept(key,line.substring(equals+1));
                    if (key.equals("tile.blockWoodenDevice.2.name")) sink.accept("block.thaumcraft.arcane_pressure_plate",line.substring(equals+1));
                    if (key.equals("tile.blockMetalDevice.3.name")) sink.accept("block.thaumcraft.advanced_alchemical_construct",line.substring(equals+1));
                    if (key.equals("tile.blockAlchemyFurnace.name")) sink.accept("block.thaumcraft.advanced_alchemy_furnace",line.substring(equals+1));
                    if (key.equals("tile.blockMetalDevice.10.name")) sink.accept("block.thaumcraft.thaumatorium",line.substring(equals+1));
                    if (key.equals("tile.blockMetalDevice.12.name")) sink.accept("block.thaumcraft.mnemonic_matrix",line.substring(equals+1));
                    var tubeNames=java.util.Map.of(0,"essentia_tube",1,"essentia_valve",3,"essentia_filter",4,"essentia_buffer",5,"essentia_restrictor",6,"essentia_oneway");
                    for (var tubeName:tubeNames.entrySet()) if (key.equals("tile.blockTube."+tubeName.getKey()+".name")) sink.accept("block.thaumcraft."+tubeName.getValue(),line.substring(equals+1));
                    if (key.equals("tile.blockStoneDevice.0.name")) sink.accept("block.thaumcraft.alchemy_furnace",line.substring(equals+1));
                    if (key.equals("container.alchemyfurnace") || key.startsWith("tile.alembic.msg.")) sink.accept(key,line.substring(equals+1));
                    if (key.equals("tile.blockWoodenDevice.0.name")) sink.accept("block.thaumcraft.bellows",line.substring(equals+1));
                    if (key.equals("tile.blockJar.1.name")) sink.accept("block.thaumcraft.brain_jar",line.substring(equals+1));
                    if (key.equals("tile.blockJar.2.name")) sink.accept("block.thaumcraft.node_jar",line.substring(equals+1));
                    if (key.equals("tile.blockJar.0.name")) sink.accept("block.thaumcraft.essentia_jar",line.substring(equals+1));
                    if (key.equals("tile.blockJar.3.name")) sink.accept("block.thaumcraft.void_jar",line.substring(equals+1));
                    if (key.equals("item.ItemEssence.0.name")) sink.accept("item.thaumcraft.empty_phial",line.substring(equals+1));
                    if (key.equals("item.ItemEssence.1.name")) sink.accept("item.thaumcraft.essentia_phial",line.substring(equals+1));
                    if (key.equals("item.ItemResource.13.name")) sink.accept("item.thaumcraft.jar_label",line.substring(equals+1));
                    if (key.equals("item.ItemResource.0.name")) sink.accept("item.thaumcraft.alumentum",line.substring(equals+1));
                    if (key.equals("item.BlockJarFilledItem.name")) sink.accept("item.thaumcraft.filled_essentia_jar",line.substring(equals+1));
                    if (key.equals("item.BlockJarFilledItem.void.name")) sink.accept("item.thaumcraft.filled_void_jar",line.substring(equals+1));
                    if (key.equals("tile.blockStoneDevice.1.name")) sink.accept("block.thaumcraft.arcane_pedestal",line.substring(equals+1));
                    if (key.equals("tile.blockStoneDevice.2.name")) sink.accept("block.thaumcraft.infusion_matrix",line.substring(equals+1));
                    if (key.equals("tile.blockStoneDevice.3.name")) sink.accept("block.thaumcraft.infusion_pillar",line.substring(equals+1));
                    if (key.equals("tile.blockStoneDevice.4.name")) sink.accept("block.thaumcraft.infusion_pillar_top",line.substring(equals+1));
                    if (key.equals("container.pedestal")) sink.accept(key,line.substring(equals+1));
                    if (key.equals("tile.blockTaint.0.name")) sink.accept("block.thaumcraft.taint_crust",line.substring(equals+1));
                    if (key.equals("tile.blockTaint.1.name")) sink.accept("block.thaumcraft.taint_soil",line.substring(equals+1));
                    if (key.equals("tile.blockTaint.2.name")) sink.accept("block.thaumcraft.flesh_block",line.substring(equals+1));
                    if (key.equals("tile.blockTaintFibres.0.name")) sink.accept("block.thaumcraft.taint_fibres",line.substring(equals+1));
                    for (int variant=1;variant<=4;variant++) if (key.equals("tile.blockTaintFibres."+variant+".name"))
                        sink.accept("item.thaumcraft."+new String[]{"taint_grass","taint_plant","taint_spore_stalk","taint_spore_stalk_mature"}[variant-1],line.substring(equals+1));
                    if (key.equals("tile.blockFluxGas.name")) sink.accept("block.thaumcraft.flux_gas",line.substring(equals+1));
                    if (key.equals("tile.blockFluidDeath.name")) sink.accept("block.thaumcraft.liquid_death",line.substring(equals+1));
                    if (key.equals("item.ItemBucketPure.name")) sink.accept("item.thaumcraft.purifying_water_bucket",line.substring(equals+1));
                    if (key.equals("item.ItemBucketDeath.name")) sink.accept("item.thaumcraft.liquid_death_bucket",line.substring(equals+1));
                    if (key.equals("item.ItemBottleTaint.name")) sink.accept("item.thaumcraft.taint_bottle",line.substring(equals+1));
                    if (key.equals("tile.blockCustomPlant.4.name")) sink.accept("block.thaumcraft.ethereal_bloom",line.substring(equals+1));
                    if (key.equals("tile.blockTube.2.name")) sink.accept("block.thaumcraft.alchemy_centrifuge",line.substring(equals+1));
                    if (key.equals("tile.blockTube.7.name")) sink.accept("block.thaumcraft.essentia_crystallizer",line.substring(equals+1));
                    if (key.equals("item.ItemCrystalEssence.name")) sink.accept("item.thaumcraft.essence_crystal",line.substring(equals+1));
                    if (key.startsWith("death.attack.dissolve")) sink.accept(key,line.substring(equals+1));
                    if (key.equals("tile.blockFluxGoo.name")) sink.accept("block.thaumcraft.flux_goo",line.substring(equals+1));
                    if (key.equals("potion.fluxtaint")) sink.accept("effect.thaumcraft.flux_taint", line.substring(equals + 1));
                    if (key.equals("tc.taint_item_poison") || key.startsWith("death.attack.taint")) sink.accept(key, line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.ThaumSlime.name")) sink.accept("entity.thaumcraft.thaumic_slime", line.substring(equals+1));
                    for(var row:new String[][]{{"TaintSwarmer","taint_hive"},{"TaintSwarm","taint_swarm"}})if(key.equals("entity.Thaumcraft."+row[0]+".name")) {
                        sink.accept("entity.thaumcraft."+row[1],line.substring(equals+1));
                        sink.accept("item.thaumcraft."+row[1]+"_spawn_egg",line.substring(equals+1)+(chinese?"刷怪蛋":" Spawn Egg"));
                    }
                    for(var kind:tc4revival.content.TaintedAnimalEntity.Kind.values())if(key.equals("entity.Thaumcraft."+kind.legacy+".name")) {
                        sink.accept("entity.thaumcraft."+kind.id(),line.substring(equals+1));
                        sink.accept("item.thaumcraft."+kind.id()+"_spawn_egg",line.substring(equals+1)+(chinese?"刷怪蛋":" Spawn Egg"));
                    }
                    if (key.equals("entity.Thaumcraft.TaintSpider.name")) sink.accept("entity.thaumcraft.taint_spider", line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.TaintSpore.name")) sink.accept("entity.thaumcraft.taint_spore", line.substring(equals+1));
                    if (key.equals("item.ItemResource.11.name")) sink.accept("item.thaumcraft.tainted_goo", line.substring(equals+1));
                    if (key.equals("item.ItemResource.12.name")) sink.accept("item.thaumcraft.tainted_tendril", line.substring(equals+1));
                    if (key.equals("potion.infvisexhaust")) sink.accept("effect.thaumcraft.infectious_vis_exhaust", line.substring(equals + 1));
                    for (var effect:java.util.Map.of("unhunger","unnatural_hunger","warpward","warp_ward","deathgaze","death_gaze","blurred","blurred_vision","sunscorned","sun_scorned","thaumarhia","thaumarhia").entrySet())
                        if (key.equals("potion."+effect.getKey())) sink.accept("effect.thaumcraft."+effect.getValue(),line.substring(equals+1));
                    if (key.equals("enchantment.repair")) sink.accept("enchantment.thaumcraft.repair", line.substring(equals + 1));
                    if (key.equals("enchantment.haste")) sink.accept("enchantment.thaumcraft.haste", line.substring(equals + 1));
                    if (key.equals("item.ItemGoggles.name")) sink.accept("item.thaumcraft.goggles", line.substring(equals + 1));
                    if (key.equals("item.ItemChestplateRobe.name")) sink.accept("item.thaumcraft.robe_chest", line.substring(equals + 1));
                    if (key.equals("item.ItemLeggingsRobe.name")) sink.accept("item.thaumcraft.robe_legs", line.substring(equals + 1));
                    if (key.equals("item.ItemBootsRobe.name")) sink.accept("item.thaumcraft.robe_boots", line.substring(equals + 1));
                    if (key.equals("tc.visdiscount")) sink.accept(key, line.substring(equals + 1));
                    var arcaneBlocks = java.util.Map.of("tile.blockMetalDevice.14.name", "vis_relay", "tile.blockMetalDevice.2.name", "workbench_charger",
                            "tile.blockStoneDevice.9.name", "node_stabilizer", "tile.blockStoneDevice.10.name", "advanced_node_stabilizer", "tile.blockStoneDevice.11.name", "node_converter",
                            "tile.blockStoneDevice.13.name", "focal_manipulator", "tile.blockCosmeticSolid.6.name", "arcane_stone",
                            "tile.blockCosmeticSolid.7.name", "arcane_stone_bricks", "tile.blockStairsArcaneStone.name", "arcane_stone_stairs",
                            "tile.blockCosmeticSlabStone.arcane.name", "arcane_stone_slab");
                    if (arcaneBlocks.containsKey(key)) sink.accept("block.thaumcraft." + arcaneBlocks.get(key), line.substring(equals + 1));
                    if (key.equals("tile.blockStoneDevice.5.name")) sink.accept("block.thaumcraft.wand_pedestal", line.substring(equals + 1));
                    if (key.equals("tile.blockStoneDevice.8.name")) sink.accept("block.thaumcraft.wand_pedestal_focus", line.substring(equals + 1));
                    if (key.startsWith("wandtable.text")) sink.accept(key, line.substring(equals + 1));
                    if (key.startsWith("focus.upgrade.")) sink.accept(key, line.substring(equals + 1));
                    if (key.startsWith("nodetype.") || key.startsWith("nodemod.")) sink.accept(key,line.substring(equals+1));
                    if (key.equals("item.FocusPouch.name")) sink.accept("item.thaumcraft.focus_pouch", line.substring(equals + 1));
                    if (key.equals("item.FocusShock.name")) sink.accept("item.thaumcraft.focus_shock", line.substring(equals + 1));
                    if (key.equals("item.FocusFire.name")) sink.accept("item.thaumcraft.focus_fire", line.substring(equals + 1));
                    if (key.equals("item.FocusFrost.name")) sink.accept("item.thaumcraft.focus_frost", line.substring(equals + 1));
                    if (key.equals("item.FocusExcavation.name")) sink.accept("item.thaumcraft.focus_excavation", line.substring(equals + 1));
                    if (key.equals("item.FocusPech.name")) sink.accept("item.thaumcraft.focus_pech", line.substring(equals + 1));
                    if (key.equals("item.FocusPortableHole.name")) sink.accept("item.thaumcraft.focus_portable_hole", line.substring(equals + 1));
                    if (key.equals("item.FocusTrade.name")) sink.accept("item.thaumcraft.focus_trade", line.substring(equals + 1));
                    if (key.equals("item.FocusWarding.name")) sink.accept("item.thaumcraft.focus_warding", line.substring(equals + 1));
                    if (key.equals("item.FocusHellbat.name")) sink.accept("item.thaumcraft.focus_hellbat", line.substring(equals + 1));
                    if (key.equals("item.FocusPrimal.name")) sink.accept("item.thaumcraft.focus_primal",line.substring(equals+1));
                    if (key.equals("entity.Thaumcraft.Firebat.name")) sink.accept("entity.thaumcraft.fire_bat", line.substring(equals + 1));
                    if (key.equals("item.ItemResource.15.name")) sink.accept("item.thaumcraft.primal_charm", line.substring(equals + 1));
                    if (key.equals("item.ItemWispEssence.name")) sink.accept("item.thaumcraft.wisp_essence", line.substring(equals + 1));
                    if (key.equals("item.ItemResource.1.name")) sink.accept("block.thaumcraft.nitor", line.substring(equals + 1));
                    if (key.equals("item.ItemAmuletVis.0.name")) sink.accept("item.thaumcraft.lesser_vis_amulet", line.substring(equals + 1));
                    if (key.equals("item.ItemAmuletVis.1.name")) sink.accept("item.thaumcraft.vis_amulet", line.substring(equals + 1));
                    if (key.equals("item.ItemAmuletVis.text")) sink.accept(key, line.substring(equals + 1));
                    if (key.startsWith("tc.primalcharm.")) sink.accept(key, line.substring(equals + 1));
                    if (key.equals("container.arcaneworkbench")) sink.accept(key, line.substring(equals + 1));
                    if (key.equals("item.ItemSanityChecker.name")) sink.accept("item.thaumcraft.sanity_checker",line.substring(equals+1));
                    if (key.equals("item.ItemCompassStone.name")) sink.accept("item.thaumcraft.sinister_stone",line.substring(equals+1));
                    if (key.equals("tile.blockCosmeticSolid.2.name")) sink.accept("block.thaumcraft.travel_paving_stone",line.substring(equals+1));
                    if (key.equals("tile.blockCosmeticSolid.3.name")) sink.accept("block.thaumcraft.warding_paving_stone",line.substring(equals+1));
                    if (key.startsWith("text.spa.mix.")) sink.accept(key,line.substring(equals+1));
                    if (key.equals("tile.blockMetalDevice.5.name")) sink.accept("block.thaumcraft.metal_grate", line.substring(equals+1));
                    if (key.equals("tile.blockStoneDevice.12.name")) sink.accept("block.thaumcraft.arcane_spa", line.substring(equals+1));
                    if (key.equals("tile.blockStoneDevice.14.name")) sink.accept("block.thaumcraft.flux_scrubber", line.substring(equals + 1));
                    if (key.equals("tile.blockTable.14.name")) sink.accept("block.thaumcraft.deconstruction_table", line.substring(equals + 1));
                    if (key.equals("tile.blockTable.15.name")) sink.accept("block.thaumcraft.arcane_workbench", line.substring(equals + 1));
                    for (var component : tc4revival.content.WandComponent.all())
                        if (key.equals("item." + component.legacyItem() + "." + component.metadata() + ".name"))
                            sink.accept("item.thaumcraft." + component.path(), line.substring(equals + 1));
                    if (key.startsWith("item.Wand.") || key.equals("item.capacity.text") || key.startsWith("tc.vis.") || key.startsWith("item.Focus.cost"))
                        sink.accept(key, line.substring(equals + 1).replace("%CAP", "%1$s").replace("%ROD", "%2$s").replace("%OBJ", "%3$s"));
                    if (key.equals("item.ItemZombieBrain.name")) sink.accept("item.thaumcraft.zombie_brain", line.substring(equals + 1));
                    for (var type : tc4revival.content.ThaumiumToolType.values()) if (key.equals("item." + type.legacyName() + ".name"))
                        sink.accept("item.thaumcraft." + type.path(), line.substring(equals + 1));
                    for (var type : tc4revival.content.ThaumiumArmorType.values()) if (key.equals("item." + type.legacyName() + ".name"))
                        sink.accept("item.thaumcraft." + type.path(), line.substring(equals + 1));
                    if (key.equals("tc.discount")) sink.accept(key, line.substring(equals + 1));
                    if (key.equals("enchantment.special.sapless")) sink.accept(key, line.substring(equals + 1));
                    if (key.equals("item.warping")) sink.accept(key, line.substring(equals + 1));
                    if (key.equals("tc.lootbag")) sink.accept(key,line.substring(equals+1));
                    for (var kind:tc4revival.content.LootBagItem.Kind.values()) if (key.equals("item.ItemLootBag."+kind.metadata+".name"))
                        sink.accept("item.thaumcraft."+kind.path,line.substring(equals+1));
                    if (key.equals("item.ItemEldritchObject.3.name")) sink.accept("item.thaumcraft.primordial_pearl", line.substring(equals + 1));
                    if (key.equals("item.ItemManaBean.name")) { sink.accept("item.thaumcraft.mana_bean",line.substring(equals+1)); sink.accept("block.thaumcraft.mana_pod",line.substring(equals+1)); }
                    if (key.startsWith("item.ItemEldritchObject.text.") || key.startsWith("tc.boss.")
                            || key.startsWith("warp.text.") || key.equals("tc.removewarpsticky") || key.equals("tc.break.fly")) sink.accept(key, line.substring(equals + 1));
                    for (var type : tc4revival.content.VoidEquipment.Tool.values()) if (key.equals("item." + type.legacyName + ".name"))
                        sink.accept("item.thaumcraft." + type.path, line.substring(equals + 1));
                    for (var type : tc4revival.content.VoidEquipment.Armor.values()) if (key.equals("item." + type.legacyName + ".name"))
                        sink.accept("item.thaumcraft." + type.path, line.substring(equals + 1));
                    if (key.equals("item.ItemBaubleBlanks.3.name")) for (var type : tc4revival.content.ElementalRingType.values())
                        sink.accept("item.thaumcraft." + type.path(), line.substring(equals + 1).replace("%TYPE", type.definition().create().displayName()));
                    for (var type : tc4revival.content.MundaneBaubleType.values()) if (key.equals("item.ItemBaubleBlanks." + type.metadata() + ".name"))
                        sink.accept("item.thaumcraft." + type.path(), line.substring(equals + 1));
                    for (var type : tc4revival.content.FoodType.values()) if (key.equals("item." + type.legacyName() + ".name"))
                        sink.accept("item.thaumcraft." + type.path(), line.substring(equals + 1));
                    if (key.equals("tc.addwarptemp")) sink.accept(key, line.substring(equals + 1));
                    for (var type : tc4revival.content.ClusterType.values()) if (key.equals("item.ItemNugget." + type.metadata() + ".name"))
                        sink.accept("item.thaumcraft." + type.path(), line.substring(equals + 1));
                    for (var type : tc4revival.content.NuggetType.values()) if (key.equals("item.ItemNugget." + type.metadata() + ".name"))
                        sink.accept("item.thaumcraft." + type.path(), line.substring(equals + 1));
                    for (var type : tc4revival.content.MaterialType.values()) if (type != tc4revival.content.MaterialType.GOLD_COIN && key.equals("item.ItemResource." + type.metadata() + ".name"))
                        sink.accept("item.thaumcraft." + type.path(), line.substring(equals + 1));
                    if (key.equals("item.ItemInkwell.name")) sink.accept("item.thaumcraft.scribing_tools", line.substring(equals + 1));
                    if (key.equals("itemGroup.thaumcraft")) sink.accept(key, line.substring(equals + 1));
                    if (key.equals("item.ItemThaumonomicon.name")) {
                        sink.accept("item.thaumcraft.thaumonomicon", line.substring(equals + 1));
                        sink.accept("item.thaumcraft.cheat_thaumonomicon", line.substring(equals + 1));
                    }
                    if (key.equals("tile.blockTable.0.name")) sink.accept("block.thaumcraft.table", line.substring(equals + 1));
                    if (key.equals("tile.blockTable.research.name")) {
                        sink.accept("block.thaumcraft.research_table", line.substring(equals + 1));
                        sink.accept("container.thaumcraft.research_table", line.substring(equals + 1));
                    }
                    if (key.equals("item.ItemResource.9.name")) sink.accept("item.thaumcraft.knowledge_fragment", line.substring(equals + 1));
                    if (key.startsWith("tc.aspect.") || key.startsWith("tc.research_name.") || key.startsWith("tc.research_text.")
                            || key.startsWith("tc.research_page.") || key.startsWith("tc.research_category.")
                            || key.startsWith("recipe.type.") || key.equals("recipe.clickthrough")
                            || key.equals("tc.inst") || key.startsWith("tc.inst.")
                            || key.equals("enchantment.haste") || key.equals("enchantment.repair")
                            || java.util.Set.of("tc.research.hasnote", "tc.research.getprim", "tc.research.shortprim",
                                    "tc.research.purchase", "tc.research.short", "tc.researchmissing", "tc.research.popup").contains(key)
                            || key.startsWith("item.researchnotes.") || key.startsWith("tc.forbidden")
                            || key.startsWith("tile.researchtable.noink.") || key.equals("tc.research.copy")
                            || java.util.Set.of("item.discovery.name", "tc.researcherror", "tc.addwarp", "tc.addwarpsticky",
                            "tc.discoveryerror", "tc.unknownobject", "tc.addaspectpool", "tc.addaspectdiscovery", "tc.addclue").contains(key)) {
                        sink.accept(key, key.equals("tc.forbidden") ? line.substring(equals + 1).replace("%n", "%s") : line.substring(equals + 1));
                    }
                }
                // Boss entity labels are random-name format strings, not generic item names.
                sink.accept("item.thaumcraft.eldritch_warden_spawn_egg",chinese?"邪术监守刷怪蛋":"Eldritch Warden Spawn Egg");
                sink.accept("item.thaumcraft.eldritch_golem_spawn_egg",chinese?"邪术傀儡刷怪蛋":"Eldritch Construct Spawn Egg");
                sink.accept("item.thaumcraft.cultist_leader_spawn_egg",chinese?"血腥主教刷怪蛋":"Crimson Praetor Spawn Egg");
    }
}
