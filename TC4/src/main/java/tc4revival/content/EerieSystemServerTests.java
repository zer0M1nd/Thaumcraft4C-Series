package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class EerieSystemServerTests {
    @GameTest(template="tree_test_empty")
    public static void nodeBiomeEffectsAndRestoration(GameTestHelper test){
        var level=test.getLevel();var origin=test.absolutePos(new BlockPos(8,4,8));
        var originals=new java.util.LinkedHashMap<BlockPos,net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>>();
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){var p=origin.offset(x,0,z);if(level.hasChunkAt(p))originals.put(p,level.getBiomeManager().getNoiseBiomeAtPosition(p).unwrapKey().orElseThrow());}
        boolean hard=tc4revival.config.TCConfig.SERVER.hardNodes.get();
        try{
            tc4revival.config.TCConfig.SERVER.hardNodes.set(false);
            long seed=37;var rng=net.minecraft.util.RandomSource.create(seed);var darkTarget=origin.offset(rng.nextInt(12)-rng.nextInt(12),0,rng.nextInt(12)-rng.nextInt(12));
            WorldBiomeChanges.changeColumn(level,darkTarget,Biomes.PLAINS);level.random.setSeed(seed);NodeBiomeEffects.tick(level,origin,AuraNodeType.DARK);
            test.assertTrue(level.getBiomeManager().getNoiseBiomeAtPosition(darkTarget).is(TCBiomes.EERIE),"Dark node changes a stored biome even when hard node encounters are disabled");
            test.assertTrue(EtherealBloomBlockEntity.cleanse(level,darkTarget,darkTarget),"Bloom now restores registered Eerie biome");
            rng=net.minecraft.util.RandomSource.create(seed);var taintTarget=origin.offset(rng.nextInt(8)-rng.nextInt(8),0,rng.nextInt(8)-rng.nextInt(8));
            level.random.setSeed(seed);NodeBiomeEffects.tick(level,origin,AuraNodeType.TAINTED);
            test.assertTrue(level.getBiomeManager().getNoiseBiomeAtPosition(taintTarget).is(TCBiomes.TAINTED_LAND),"Tainted node biome conversion is independent of hard node switch");
            test.assertTrue(PureNodeBiomeConversion.convert(level,origin,taintTarget)&&level.getBiomeManager().getNoiseBiomeAtPosition(taintTarget).is(TCBiomes.MAGICAL_FOREST),"Pure node cleans the same exact storage cell");
            test.assertTrue(!NodeBiomeEffects.darkDimensionAllowed(level.getServer().getLevel(net.minecraft.world.level.Level.NETHER)),"Dark nodes do not change Nether biomes");
            test.assertTrue(!NodeBiomeEffects.spawnGiant(level,origin),"No dark-node giant encounter without a nearby player");
        }finally{tc4revival.config.TCConfig.SERVER.hardNodes.set(hard);originals.forEach((p,b)->WorldBiomeChanges.changeColumn(level,p,b));}
        pass(test,"node-biomes-restoration");
    }
    @GameTest(template="wand_test_empty")
    public static void brainyAngerPersistenceAndCure(GameTestHelper test){
        var level=test.getLevel();var giant=TCEntities.GIANT_BRAINY_ZOMBIE.get().create(level);giant.moveTo(test.absolutePos(new BlockPos(2,3,2)),0,0);
        test.assertTrue(giant.getMaxHealth()==60&&giant.getAttributeValue(Attributes.ATTACK_DAMAGE)==7&&giant.getArmorValue()==5,"Giant base health damage and inherited armor");
        for(int i=0;i<15;i++)giant.hurt(level.damageSources().generic(),0);
        test.assertTrue(giant.anger()==2,"Even rejected damage increases anger, capped at two");giant.aiStep();
        test.assertTrue(Math.abs(giant.anger()-1.998)<.0001&&giant.getAttributeValue(Attributes.ATTACK_DAMAGE)>11.98,"Anger decays and updates damage");
        test.assertTrue(Math.abs(giant.getBbWidth()-.6f*giant.anger())<.001,"Physical size follows visible anger scale");
        giant.setVillager(true);var tag=new CompoundTag();giant.addAdditionalSaveData(tag);var loaded=TCEntities.GIANT_BRAINY_ZOMBIE.get().create(level);loaded.readAdditionalSaveData(tag);
        test.assertTrue(loaded.anger()==giant.anger()&&loaded.villager(),"Anger and villager form persist");
        var pos=test.absolutePos(new BlockPos(4,3,4));var zombie=TCEntities.BRAINY_ZOMBIE.get().create(level);zombie.moveTo(pos,0,0);zombie.setVillager(true);zombie.setBaby(true);zombie.startCure(1);level.addFreshEntity(zombie);zombie.tick();
        var villagers=level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new net.minecraft.world.phys.AABB(pos).inflate(2));
        test.assertTrue(zombie.isRemoved()&&villagers.size()==1&&villagers.get(0).isBaby(),"Curing replaces zombie with baby villager");villagers.forEach(Entity::discard);
        pass(test,"anger-save-cure");
    }
    @GameTest(template="wand_test_empty")
    public static void wispAspectsCombatAndLoot(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));var wisp=TCEntities.WISP.get().create(level);wisp.moveTo(pos,0,0);wisp.setAspectTag("aer");
        test.assertTrue(wisp.getMaxHealth()==22&&wisp.getAttributeValue(Attributes.ATTACK_DAMAGE)==3,"Wisp attributes");
        var aspects=tc4revival.aspect.entity.EntityAspects.resolve(wisp).orElseThrow();test.assertTrue(aspects.aspects().amount(TCAspects.catalog().require("aer"))==3&&aspects.legacyIdentity().equals("Thaumcraft.WispTypeaer"),"Primal type merges overlapping aspect and has qualified scan identity");
        var tag=new CompoundTag();wisp.addAdditionalSaveData(tag);var copy=TCEntities.WISP.get().create(level);copy.readAdditionalSaveData(tag);test.assertTrue(copy.aspectTag().equals("aer"),"Type persists");
        var target=EntityType.IRON_GOLEM.create(level);target.moveTo(pos.east(2),0,0);target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);float health=target.getHealth();
        for(int i=0;i<30;i++){target.invulnerableTime=0;wisp.zap(target);test.assertTrue(wisp.attackCounter>=-20&&wisp.attackCounter<=-1,"Zap resets old cooldown range");}
        test.assertTrue(target.getHealth()<health,"Stationary target can take zap damage");
        level.addFreshEntity(wisp);wisp.hurt(level.damageSources().genericKill(),1000);var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2));
        test.assertTrue(drops.stream().anyMatch(item->item.getItem().is(TCItems.WISP_ESSENCE.get())&&TCItems.WISP_ESSENCE.get().storedAspects(item.getItem()).amount(TCAspects.catalog().require("aer"))==2),"Death produces two-aspect wisp essence");drops.forEach(Entity::discard);
        wisp.waypoint=wisp.position();wisp.setNoAi(false);wisp.customServerAiStep();test.assertTrue(Double.isFinite(wisp.getDeltaMovement().lengthSqr()),"Coincident waypoint cannot create NaN velocity");
        pass(test,"wisp-aspects-combat-loot");
    }
    @GameTest(template="tree_test_empty")
    public static void eerieBiomeEcology(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(8,3,8));var original=level.getBiomeManager().getNoiseBiomeAtPosition(pos).unwrapKey().orElseThrow();
        try{
            // Surround fuzzy biome sampling as well as the literal cell for the natural spawn event.
            var biome=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(TCBiomes.EERIE).value();
            test.assertTrue(biome.getBaseTemperature()==.5f&&biome.getSkyColor()==0x222299&&biome.getWaterColor()==3035999,"Eerie climate and palette");
            test.assertTrue(biome.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream().anyMatch(row->row.type==EntityType.BAT&&row.getWeight().asInt()==3),"Legacy creature-category bat entry retained");
            WorldBiomeChanges.changeColumn(level,pos,TCBiomes.EERIE);
            var event=new net.minecraftforge.event.level.LevelEvent.PotentialSpawns(level,MobCategory.MONSTER,pos,biome.getMobSettings().getMobs(MobCategory.MONSTER));
            // Choose a position whose runtime biome query resolves to this stored column.
            if(!level.getBiome(pos).is(TCBiomes.EERIE)){
                for(int x=0;x<4;x++)for(int z=0;z<4;z++){var query=new BlockPos((pos.getX()>>2)*4+x,pos.getY(),(pos.getZ()>>2)*4+z);if(level.getBiome(query).is(TCBiomes.EERIE))event=new net.minecraftforge.event.level.LevelEvent.PotentialSpawns(level,MobCategory.MONSTER,query,biome.getMobSettings().getMobs(MobCategory.MONSTER));}
            }
            test.assertTrue(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(TCBiomes.EERIE).is(net.minecraftforge.common.Tags.Biomes.IS_SPOOKY),"Spooky tag integrates champion location rules");
            EerieSpawning.potentialSpawns(event);EerieSpawning.potentialSpawns(event);
            test.assertTrue(event.getSpawnerDataList().stream().filter(row->row.type==TCEntities.GIANT_BRAINY_ZOMBIE.get()&&row.getWeight().asInt()==8).count()==1,"Giant entry has old weight without duplicates");
            test.assertTrue(event.getSpawnerDataList().stream().anyMatch(row->row.type==TCEntities.WISP.get()&&row.getWeight().asInt()==3),"Wisp Eerie weight");
            net.minecraftforge.common.ForgeConfigSpec.BooleanValue flag=tc4revival.config.TCConfig.SERVER_SPEC.getValues().get("monster_spawning.spawn_wisps");boolean enabled=flag.get();
            try{flag.set(false);var disabled=new net.minecraftforge.event.level.LevelEvent.PotentialSpawns(level,MobCategory.MONSTER,event.getPos(),biome.getMobSettings().getMobs(MobCategory.MONSTER));EerieSpawning.potentialSpawns(disabled);
                test.assertTrue(disabled.getSpawnerDataList().stream().noneMatch(row->row.type==TCEntities.WISP.get()),"Live config switch removes future wisp spawning without resource regeneration");
            }finally{flag.set(enabled);}
        }finally{WorldBiomeChanges.changeColumn(level,pos,original);}pass(test,"biome-ecology");
    }
    private static void pass(GameTestHelper test,String name){com.mojang.logging.LogUtils.getLogger().info("Eerie system test passed: {}",name);test.succeed();}
}
