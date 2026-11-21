package heartwork;

import heartwork.content.HeartworkVillage;
import heartwork.compat.blade.*;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("heartwork") @PrefixGameTestTemplate(false)
public class HeartworkWorldTests {
    @GameTest(template="empty") public static void villagePoolsConnectToRealStreets(GameTestHelper h) {
        var level=h.getLevel();var registry=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL);
        var generator=level.getChunkSource().getGenerator();var manager=level.getStructureManager();
        for(String biome:new String[]{"plains","desert","savanna","snowy","taiga"}) {
            var pool=registry.get(new ResourceLocation("minecraft:village/"+biome+"/houses"));
            h.assertTrue(pool.getMaxSize(manager)>=6,"Village pool height cache includes Heartwork houses");
            var entries=pool.getShuffledTemplates(net.minecraft.util.RandomSource.create(1));
            boolean blades=net.minecraftforge.fml.ModList.get().isLoaded("slashblade");
            h.assertTrue(entries.stream().filter(e -> e.toString().contains("heartwork:village/")).count()==(blades?4:2),"Natural pool retains configured house weights: "+biome);
            var found=new java.util.HashSet<String>();
            var start=registry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.TEMPLATE_POOL,new ResourceLocation("minecraft:village/"+biome+"/streets")));
            var base=h.absolutePos(new BlockPos(0,0,0));
            var pos=new BlockPos(base.getX(),generator.getFirstFreeHeight(base.getX(),base.getZ(),net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,level,level.getChunkSource().randomState()),base.getZ());
            // Vanilla villages grow houses inside street bounding volumes. The native
            // expansion hack supplies their vertical room; reversing this edge is invalid.
            for(int seed=0;seed<128&&found.size()<(blades?2:1);seed++) {
                var context=new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(level.registryAccess(),generator,generator.getBiomeSource(),
                        level.getChunkSource().randomState(),manager,seed,new net.minecraft.world.level.ChunkPos(pos),level,b -> true);
                var generated=net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement.addPieces(context,start,
                        java.util.Optional.empty(),2,pos,true,java.util.Optional.empty(),80).orElseThrow();
                for(var piece:generated.getPiecesBuilder().build().pieces())if(piece instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pooled) {
                    String id=pooled.getElement().toString();
                    if(id.contains("heartwork:village/"))found.add(id.contains("bladesmith")?"bladesmith":"heartworker");
                }
            }
            h.assertTrue(found.contains("heartworker")&&(!blades||found.contains("bladesmith")),"Real vanilla street generation selects and attaches each custom house: "+biome+" found="+found);
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void workSitesAssignProfessionAndRestock(GameTestHelper h) {
        var level=h.getLevel();
        for(boolean smith:new boolean[]{false,true}) {
            if(smith&&!net.minecraftforge.fml.ModList.get().isLoaded("slashblade"))continue;
            var pos=h.absolutePos(new BlockPos(smith?8:3,3,3));
            level.setBlockAndUpdate(pos,(smith?HeartworkVillage.ANVIL:HeartworkVillage.HEARTH).get().defaultBlockState());
            var villager=h.spawn(EntityType.VILLAGER,smith?8:3,3,4);villager.setNoAi(true);
            villager.setVillagerData(villager.getVillagerData().setProfession(net.minecraft.world.entity.npc.VillagerProfession.NONE));
            villager.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.POTENTIAL_JOB_SITE,net.minecraft.core.GlobalPos.of(level.dimension(),pos));
            h.assertTrue(net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite.create().tryStart(level,villager,level.getGameTime()),"Vanilla job-site assignment executes");
            h.assertTrue(villager.getVillagerData().getProfession()==(smith?HeartworkVillage.SMITH:HeartworkVillage.COOK).get(),"Actual POI resolves the custom profession");
            villager.setVillagerXp(1);
            var offer=villager.getOffers().get(0);offer.setToOutOfStock();h.assertTrue(offer.isOutOfStock(),"Trade is exhausted before work");
            class Work extends net.minecraft.world.entity.ai.behavior.WorkAtPoi {
                void atSite(){super.start(level,villager,level.getGameTime());}
            }
            new Work().atSite();
            h.assertTrue(!offer.isOutOfStock()&&offer.getUses()==0,"Vanilla work behavior replenishes the custom trade");
            var saved=new net.minecraft.nbt.CompoundTag();villager.addAdditionalSaveData(saved);
            var restored=EntityType.VILLAGER.create(level);restored.readAdditionalSaveData(saved);
            h.assertTrue(restored.getVillagerData().getProfession()==villager.getVillagerData().getProfession()&&restored.getVillagerXp()==1,"Profession and prior trade experience persist");
            villager.discard();restored.discard();
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void fluxCapabilityPersistsAndStopsAtEmpty(GameTestHelper h){
        if(!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        ItemStack blade=new ItemStack(Heartwork.item("rf_blade"));
        var energy=blade.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).orElseThrow(IllegalStateException::new);
        h.assertTrue(energy.receiveEnergy(50_000,true)==20_000&&energy.getEnergyStored()==0,"Simulation respects the original transfer limit without changing energy");
        energy.receiveEnergy(50_000,false);blade.getOrCreateTag().putBoolean("Empowered",true);
        blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(s -> s.setKillCount(37));
        var restored=ItemStack.of(blade.save(new net.minecraft.nbt.CompoundTag()));
        h.assertTrue(FluxBladeItem.energy(restored)==20_000&&restored.getCapability(ItemSlashBlade.BLADESTATE).orElseThrow(IllegalStateException::new).getKillCount()==37,"Both capabilities survive serialization");
        h.assertTrue(FluxBladeItem.extract(restored,30_000,false)==20_000&&!FluxBladeItem.empowered(restored),"An insufficient charge drains only the stored energy and switches off");h.succeed();
    }
    @GameTest(template="empty") public static void voidBladeRetainsWarpRepairAndWeakness(GameTestHelper h){
        if(!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        var blade=h.getLevel().registryAccess().registryOrThrow(SlashBladeDefinition.REGISTRY_KEY).get(new ResourceLocation("heartwork:smithblade_void")).getBlade();
        h.assertTrue(blade.getItem() instanceof HeartworkBladeItem,"Void definitions use their behavior-bearing item");
        var holder=h.spawn(EntityType.ZOMBIE,3,3,3);var victim=h.spawn(EntityType.COW,6,3,3);
        blade.setDamageValue(10);holder.tickCount=20;blade.getItem().inventoryTick(blade,h.getLevel(),holder,0,true);
        h.assertTrue(blade.getDamageValue()==9,"Original one durability repair every twenty ticks");
        blade.getItem().hurtEnemy(blade,victim,holder);
        h.assertTrue(victim.getEffect(net.minecraft.world.effect.MobEffects.WEAKNESS).getDuration()==60,"Original weakness duration");
        h.assertTrue(((thaumcraft.api.IWarpingGear)blade.getItem()).getWarp(blade,null)==2,"Original equipped warp");holder.discard();victim.discard();h.succeed();
    }
    @GameTest(template="empty") public static void legacyVillageTemplatesLoadAndPlace(GameTestHelper h){
        var manager=h.getLevel().getStructureManager();
        for(String biome:new String[]{"plains","desert","savanna","snowy","taiga"})for(String house:new String[]{"bladesmith","heartworker"}){
            var template=manager.get(new ResourceLocation("heartwork:village/"+biome+"/"+house)).orElseThrow();
            h.assertTrue(template.getSize().equals(new net.minecraft.core.Vec3i(10,6,8)),"Original house bounds plus entrance connection");
        }
        var template=manager.get(new ResourceLocation("heartwork:village/plains/heartworker")).orElseThrow();
        var origin=h.absolutePos(new BlockPos(0,2,0));
        h.assertTrue(template.placeInWorld(h.getLevel(),origin,origin,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setIgnoreEntities(true),h.getLevel().random,3),"Actual placement succeeds");
        h.assertTrue(h.getLevel().getBlockState(origin.offset(7,1,3)).is(HeartworkVillage.HEARTH.get()),"House has working modern job site");
        h.assertTrue(h.getLevel().getBlockEntity(origin.offset(5,1,6)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,"Original chest position is preserved");h.succeed();
    }
    @GameTest(template="empty") public static void bladesmithRejectsDifferentNamedBlades(GameTestHelper h){
        if(!net.minecraftforge.fml.ModList.get().isLoaded("slashblade")){h.succeed();return;}
        var trades=new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<java.util.List<net.minecraft.world.entity.npc.VillagerTrades.ItemListing>>();
        for(int i=1;i<=5;i++)trades.put(i,new java.util.ArrayList<>());
        var event=new net.minecraftforge.event.village.VillagerTradesEvent(trades,HeartworkVillage.SMITH.get());BladeTrades.add(event);
        var trader=h.spawn(EntityType.VILLAGER,4,3,4);var offer=trades.get(2).get(0).getOffer(trader,h.getLevel().random);
        h.assertTrue(offer.satisfiedBy(BladeTrades.blade(trader,"namedblade"),ItemStack.EMPTY),"Correct named blade can be sold");
        h.assertTrue(!offer.satisfiedBy(BladeTrades.blade(trader,"namedblade1"),ItemStack.EMPTY),"Same item class with different blade identity is rejected");
        h.assertTrue(trades.values().stream().mapToInt(java.util.List::size).sum()==9,"All original nine smith trades are available across career levels");trader.discard();h.succeed();
    }
}
