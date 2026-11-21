package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class TrunkStorageServerTests {
    @GameTest(template="wand_test_empty")
    public static void trunkStorageMenusAndDeathAcrossAllUpgrades(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3)); var area=new AABB(pos).inflate(3);
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"TrunkStorage"));player.setPos(pos.getCenter());
        boolean mobLoot=level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
        try {
            for (int upgrade=-1;upgrade<6;upgrade++) {
                var trunk=TCEntities.TRAVELING_TRUNK.get().create(level);
                trunk.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0); trunk.upgrade(upgrade); trunk.ownerName("TrunkStorage");
                player.getInventory().clearContent();
                for (int slot=0;slot<36;slot++) {
                    var stack=new ItemStack(Items.DIAMOND,slot+1); stack.getOrCreateTag().putInt("test_slot",slot);
                    trunk.inventory.setItem(slot,stack);
                }
                var original=trunk.saveInventory(); var saved=new CompoundTag(); trunk.saveWithoutId(saved);
                var restored=TCEntities.TRAVELING_TRUNK.get().create(level); restored.load(saved);
                test.assertTrue(original.equals(restored.saveInventory()) && restored.rows()==(upgrade==1?4:3),
                        "All 36 slots persist independently of visible row count for upgrade "+upgrade);
                restored.inventory.getItem(35).shrink(1);
                test.assertTrue(trunk.inventory.getItem(35).getCount()==36,"Restored inventory has independent stacks"); restored.discard();
                var menu=new TravelingTrunkMenu(TCArcane.TRUNK_MENU.get(),1,player.getInventory(),trunk); player.containerMenu=menu;
                int exposed=upgrade==1?36:27;
                test.assertTrue(menu.slots.size()==exposed+36 && trunk.open(),"Menu exposes exact upgrade capacity");
                var last=menu.quickMoveStack(player,exposed-1);
                test.assertTrue(last.getCount()==exposed && trunk.inventory.getItem(exposed-1).isEmpty(),"Last exposed slot shifts into player inventory");
                int returning=-1;
                for (int slot=exposed;slot<menu.slots.size();slot++) if (menu.getSlot(slot).hasItem()) { returning=slot; break; }
                test.assertTrue(returning>=0,"Transferred stack is accessible in player slots");
                menu.quickMoveStack(player,returning);
                test.assertTrue(original.equals(trunk.saveInventory()),"Round trip preserves every hidden and visible slot");
                menu.removed(player); player.containerMenu=player.inventoryMenu;
                test.assertTrue(!trunk.open(),"Closing menu clears open state");
                level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(upgrade%2==0,level.getServer());
                boolean killed=trunk.hurt(level.damageSources().genericKill(),10000);
                if (upgrade==3) {
                    test.assertTrue(!killed && trunk.isAlive() && original.equals(trunk.saveInventory()),"Water upgrade preserves damage immunity and all contents");
                } else {
                    test.assertTrue(killed && !trunk.isAlive() && trunk.inventory.isEmpty(),"Actual lethal damage empties all 36 slots");
                    var drops=level.getEntitiesOfClass(ItemEntity.class,area);
                    test.assertTrue(drops.size()==36,"Death drops each stored stack once regardless of doMobLoot");
                    var slots=new java.util.HashSet<Integer>();
                    for (var drop:drops) {
                        var stack=drop.getItem(); int slot=stack.getOrCreateTag().getInt("test_slot");
                        test.assertTrue(stack.is(Items.DIAMOND) && slot>=0 && slot<36 && stack.getCount()==slot+1 && slots.add(slot),
                                "Every dropped slot preserves exact item/count/NBT without duplication");
                        drop.discard();
                    }
                }
                trunk.discard();
            }
            tc4revival.TC4Revival.LOGGER.info("PASS trunkStorageMenusAndDeathAcrossAllUpgrades"); test.succeed();
        } finally {
            player.containerMenu=player.inventoryMenu;
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(mobLoot,level.getServer());
            for (var item:level.getEntitiesOfClass(ItemEntity.class,area)) item.discard();
        }
    }
    @GameTest(template="wand_test_empty")
    public static void entropyTrunkCollectsOnlyVisibleSlotsAndPreservesRemainders(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new BlockPos(3,3,3));
        var trunk=TCEntities.TRAVELING_TRUNK.get().create(level); trunk.upgrade(5);
        trunk.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0); trunk.setNoAi(true); trunk.setNoGravity(true);
        var stack=new ItemStack(Items.DIAMOND,5); stack.getOrCreateTag().putString("test","entropy");
        var item=new ItemEntity(level,trunk.getX(),trunk.getY(),trunk.getZ(),stack.copy()); item.setNoGravity(true);
        var distant=new ItemEntity(level,trunk.getX()+2,trunk.getY(),trunk.getZ(),new ItemStack(Items.EMERALD)); distant.setNoGravity(true);
        // ItemEntity constructors give drops random initial motion; measure attraction from rest.
        distant.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        try {
            for (int slot=0;slot<27;slot++) trunk.inventory.setItem(slot,new ItemStack(Items.COBBLESTONE,64));
            var partial=stack.copy(); partial.setCount(62); trunk.inventory.setItem(26,partial);
            test.assertTrue(level.addFreshEntity(item) && level.addFreshEntity(distant),"Pickup fixtures spawn in loaded chunks");
            trunk.aiStep();
            test.assertTrue(trunk.inventory.getItem(26).getCount()==64 && item.isAlive() && item.getItem().getCount()==3,
                    "Only available matching capacity is consumed from the live drop");
            for (int slot=27;slot<36;slot++) test.assertTrue(trunk.inventory.getItem(slot).isEmpty(),"Entropy upgrade never inserts into hidden row");
            test.assertTrue(distant.getDeltaMovement().x<0 && distant.getItem().is(Items.EMERALD),"Outer collection radius attracts without consuming");
            trunk.aiStep();
            test.assertTrue(item.getItem().getCount()==3,"Full visible inventory leaves remainder intact");
            trunk.inventory.setItem(0,ItemStack.EMPTY); trunk.aiStep();
            var collected=trunk.inventory.getItem(0);
            test.assertTrue(item.isRemoved() && collected.getCount()==3 && ItemStack.isSameItemSameTags(collected,stack),
                    "Freed visible slot consumes exactly the remaining tagged stack");
            test.assertTrue(stack.getCount()==5,"Source fixture stack was never aliased or mutated");
            tc4revival.TC4Revival.LOGGER.info("PASS entropyTrunkCollectsOnlyVisibleSlotsAndPreservesRemainders"); test.succeed();
        } finally { trunk.discard(); item.discard(); distant.discard(); }
    }
}
