package tc4revival.content;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.item.*;
import net.minecraft.core.BlockPos;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class PechServerTests {
    private static void pass(GameTestHelper test,String name) { test.succeed(); com.mojang.logging.LogUtils.getLogger().info("Pech system test passed: {}",name); }
    @GameTest(template="wand_test_empty")
    public static void pechPickupPackPersistenceAndProfessions(GameTestHelper test) {
        var pech=TCEntities.PECH.get().create(test.getLevel()); var emeralds=new ItemStack(Items.EMERALD,64);
        for (int i=0;i<64 && !pech.friendly();i++) emeralds=pech.pickup(emeralds);
        test.assertTrue(pech.friendly() && emeralds.getCount()<64,"Valued offerings consume singles and tame");
        pech.pack.setItem(8,new ItemStack(Items.DIAMOND,12)); pech.profession(2);
        var saved=new net.minecraft.nbt.CompoundTag(); pech.addAdditionalSaveData(saved);
        var loaded=TCEntities.PECH.get().create(test.getLevel()); loaded.readAdditionalSaveData(saved);
        test.assertTrue(loaded.profession()==2 && loaded.friendly() && loaded.pack.getItem(8).getCount()==12 && loaded.pack.getItem(0).isEmpty(),"All nine pack positions and state survive reload");
        var left=loaded.pickup(new ItemStack(Items.DIAMOND,60));
        test.assertTrue(left.isEmpty() && loaded.pack.getItem(8).getCount()==64 && loaded.pack.getItem(0).getCount()==8,"Partial merging preserves remainder in next empty slot");
        var aspects=tc4revival.aspect.entity.EntityAspects.resolve(loaded).orElseThrow();
        test.assertTrue(aspects.legacyIdentity().endsWith("PechType2") && aspects.aspects().amount(tc4revival.aspect.TCAspects.catalog().require("praecantatio"))==4,"Archer keeps its legacy scan identity and unusual four magic aspects");
        for (int i=0;i<5;i++) loaded.pack.setItem(i,new ItemStack(Items.STICK));
        test.assertTrue(!loaded.removeWhenFarAway(10000),"Five occupied slots prevent distance despawn");
        saved.putInt("PechType",99); loaded.readAdditionalSaveData(saved); test.assertTrue(loaded.profession()<=2,"Malformed profession cannot index beyond render/trade pools");
        pass(test,"pickup-pack-profession");
    }
    @GameTest(template="wand_test_empty")
    public static void pechTradeMenuAndFocusAcquisition(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,1,3)); var pech=TCEntities.PECH.get().create(level); pech.moveTo(pos.getX(),pos.getY(),pos.getZ()); pech.profession(1); pech.friendly(true); level.addFreshEntity(pech);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"PechTradeTest")); player.moveTo(pech.position());
        pech.customer=player; var menu=new PechMenu(7,player.getInventory(),pech); player.containerMenu=menu;
        menu.contents.setItem(0,new ItemStack(Items.GOLD_BLOCK,2)); test.assertTrue(PechEntity.value(menu.contents.getItem(0))>5,"Legacy greed value enables high-tier rewards");
        test.assertTrue(menu.clickMenuButton(player,0),"Real menu button trades with authenticated customer");
        test.assertTrue(menu.contents.getItem(0).getCount()==1,"Trade consumes exactly one offered item");
        int occupied=0; for (int i=1;i<5;i++) if (!menu.contents.getItem(i).isEmpty()) occupied++;
        test.assertTrue(occupied>0 && !menu.ready(),"Results fill output-only slots and block repeat requests");
        test.assertTrue(!menu.clickMenuButton(player,0) && menu.contents.getItem(0).getCount()==1,"Blocked replay does not consume another offering");
        pech.getRandom().setSeed(7331); boolean focus=false;
        for (int attempt=0;attempt<96 && !focus;attempt++) {
            for (int i=1;i<5;i++) menu.contents.setItem(i,ItemStack.EMPTY);
            pech.friendly(true); pech.customer=player; menu.contents.setItem(0,new ItemStack(Items.GOLD_BLOCK,2));
            test.assertTrue(menu.clickMenuButton(player,0),"Repeated legitimate offers follow the real menu path");
            for (int i=1;i<5;i++) if (menu.contents.getItem(i).is(TCItems.PECH_FOCUS.get())) focus=true;
        }
        test.assertTrue(focus,"The actual mage trade menu awards a usable Pech focus");
        menu.removed(player);
        test.assertTrue(pech.customer==null && menu.contents.isEmpty(),"Closing clears trade ownership and inventory");
        test.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(4)).stream().anyMatch(item->item.getPersistentData().getBoolean("PechDrop")),"Returned goods are marked against Pech repickup");
        pech.discard(); pass(test,"menu-and-focus");
    }
    @GameTest(template="wand_test_empty")
    public static void pechNaturalEquipmentCombatAndAnger(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,1,3)); boolean mage=false,archer=false,forager=false;
        for (int i=0;i<160 && !(mage && archer && forager);i++) {
            var pech=TCEntities.PECH.get().create(level); pech.getRandom().setSeed(i+9403); pech.moveTo(pos.getX(),pos.getY(),pos.getZ());
            pech.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),net.minecraft.world.entity.MobSpawnType.NATURAL,null,null);
            if (pech.profession()==1) { mage=true; test.assertTrue(WandFocusItem.attached(pech.getMainHandItem()).is(TCItems.PECH_FOCUS.get()),"Natural mage equipment carries the obtainable focus"); }
            else if (pech.profession()==2) { archer=true; test.assertTrue(pech.getMainHandItem().is(Items.BOW),"Natural archer carries a bow"); } else forager=true;
        }
        test.assertTrue(mage && archer && forager,"Natural equipment selects all three professions");
        var first=TCEntities.PECH.get().create(level); var second=TCEntities.PECH.get().create(level);
        first.moveTo(pos.getX(),pos.getY(),pos.getZ()); second.moveTo(pos.getX()+2,pos.getY(),pos.getZ()); level.addFreshEntity(first); level.addFreshEntity(second); first.friendly(true); second.friendly(true);
        var attacker=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"PechAngerTest"));
        first.hurt(level.damageSources().playerAttack(attacker),1);
        test.assertTrue(first.anger()>=400 && second.anger()>=400 && !first.friendly() && !second.friendly(),"Player damage angers nearby Pechs and clears friendliness");
        first.profession(1); first.performRangedAttack(second,1);
        test.assertTrue(!level.getEntitiesOfClass(PechBlastEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(4)).isEmpty(),"Mage combat launches the real focus projectile");
        first.discard(); second.discard(); pass(test,"equipment-combat-anger");
    }
    @GameTest(template="wand_test_empty")
    public static void pechPoolsAndCandleDependency(GameTestHelper test) {
        var definition=tc4revival.data.PechTradeProvider.definition(); var pools=PechTrades.decode(definition);
        test.assertTrue(pools.size()==3 && pools.get(2).stream().filter(row->row.value()==1).count()==16,"Archer keeps bean plus old fifteen candle variants");
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,1,3)); level.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),18);
        for (int i=0;i<16;i++) { var candle=TCArcane.candle(i).defaultBlockState(); test.assertTrue(candle.getLightEmission()==14 && candle.is(InfusionLayout.STABILIZERS),"Every tallow candle emits old light and stabilizes infusion"); }
        level.setBlock(pos,TCArcane.candle(9).defaultBlockState(),3); level.removeBlock(pos.below(),false); test.assertTrue(level.isEmptyBlock(pos),"Unsupported candle breaks");
        var broken=definition.deepCopy(); broken.getAsJsonArray("professions").get(0).getAsJsonArray().asList().removeIf(row->row.getAsJsonObject().get("value").getAsInt()==4);
        boolean rejected=false; try { PechTrades.decode(broken); } catch (IllegalArgumentException expected) { rejected=true; }
        test.assertTrue(rejected,"Missing reward tier is rejected instead of infinite-looping a server trade");
        pass(test,"trade-pools-candles");
    }
    @GameTest(template="wand_test_empty")
    public static void pechBiomeSpawnIntegration(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,1,3));
        WorldBiomeChanges.changeColumn(level,pos,TCBiomes.MAGICAL_FOREST);
        test.assertTrue(PechSpawning.biomeAllowed(level,pos),"Magical forest is an allowed Pech habitat");
        var pool=net.minecraft.util.random.WeightedRandomList.create(new net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityType.ZOMBIE,100,4,4));
        var event=new net.minecraftforge.event.level.LevelEvent.PotentialSpawns(level,net.minecraft.world.entity.MobCategory.MONSTER,pos,pool);
        PechSpawning.potentialSpawns(event); PechSpawning.potentialSpawns(event);
        var pechs=event.getSpawnerDataList().stream().filter(row->row.type==TCEntities.PECH.get()).toList();
        test.assertTrue(pechs.size()==1 && pechs.get(0).getWeight().asInt()==10 && pechs.get(0).minCount==1 && pechs.get(0).maxCount==1,"Natural spawn hook adds exactly one legacy-weight entry");
        WorldBiomeChanges.changeColumn(level,pos,TCBiomes.TAINTED_LAND);
        test.assertTrue(!PechSpawning.biomeAllowed(level,pos),"Tainted magical land is explicitly rejected");
        pass(test,"biome-spawn-hook");
    }

}
