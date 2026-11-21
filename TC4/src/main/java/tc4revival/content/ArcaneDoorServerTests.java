package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class ArcaneDoorServerTests {
    private static UseOnContext context(net.minecraft.world.entity.player.Player player,BlockPos pos) {
        return new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    private static BlockPos place(GameTestHelper test,net.minecraft.world.entity.player.Player player) {
        var pos=test.absolutePos(new BlockPos(3,2,3)); var level=test.getLevel();
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),18);
        player.setYRot(0); player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.ARCANE_DOOR.get()));
        test.assertTrue(TCItems.ARCANE_DOOR.get().useOn(context(player,pos.below())).consumesAction(),"Real item places door");
        test.assertTrue(level.getBlockEntity(pos) instanceof OwnedDeviceBlockEntity && level.getBlockEntity(pos.above()) instanceof OwnedDeviceBlockEntity,"Both halves have persistent ownership");
        return pos;
    }
    @GameTest(template="wand_test_empty")
    public static void doorKeysBindUpperDelegateAndRecoverOnce(GameTestHelper test) {
        var level=test.getLevel(); var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WardTest")); var pos=place(test,player);
        var lower=(OwnedDeviceBlockEntity)level.getBlockEntity(pos); var upper=(OwnedDeviceBlockEntity)level.getBlockEntity(pos.above());
        String name=player.getGameProfile().getName();
        test.assertTrue(lower.access().ownedBy(name) && upper.access().ownedBy(name),"Both owners are the placer");
        var key=new ItemStack(TCItems.GOLD_KEY.get(),2); player.setItemInHand(InteractionHand.MAIN_HAND,key);
        key.getItem().onItemUseFirst(key,context(player,pos.above()));
        ItemStack bound=ItemStack.EMPTY;
        for (var stack:player.getInventory().items) if (stack.is(TCItems.GOLD_KEY.get()) && stack.hasTag()) bound=stack.copy();
        test.assertTrue(!bound.isEmpty() && ArcaneKeyItem.matches(bound,level,pos,true) && !ArcaneKeyItem.matches(bound,level,pos.above(),true),"Upper interaction binds the lower position");
        var wrong=bound.copy(); wrong.getOrCreateTag().putString("dimension","minecraft:the_nether");
        test.assertTrue(!ArcaneKeyItem.matches(wrong,level,pos,true) && !ArcaneKeyItem.matches(bound,level,pos,false),"Dimension and device type are part of the binding");
        lower.access().setOwner("OtherOwner"); upper.access().setOwner("OtherOwner");
        player.setItemInHand(InteractionHand.MAIN_HAND,bound);
        bound.getItem().onItemUseFirst(bound,context(player,pos.above()));
        test.assertTrue(lower.access().mayConfigure(name) && upper.access().mayConfigure(name) && !lower.access().ownedBy(name),"Master key delegates both halves without transferring ownership");
        var saved=lower.saveWithFullMetadata(); lower.load(saved);
        test.assertTrue(lower.access().mayBindKey(name,false) && !lower.access().mayBindKey(name,true),"Reload preserves normal-key delegation only");
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,false);
        player.setItemInHand(InteractionHand.MAIN_HAND,wand);
        boolean previous=tc4revival.config.TCConfig.SERVER.wardedStone.get();
        try {
            tc4revival.config.TCConfig.SERVER.wardedStone.set(true);
            test.assertTrue(OwnedDeviceRecovery.recover(context(player,pos.above()))==net.minecraft.world.InteractionResult.PASS,"Master authority does not allow recovery");
            lower.access().setOwner(name); upper.access().setOwner(name);
            test.assertTrue(OwnedDeviceRecovery.recover(context(player,pos.above())).consumesAction() && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()),"Owner recovers both halves through upper half");
            int count=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(1)).stream()
                    .filter(entity->entity.getItem().is(TCItems.ARCANE_DOOR.get())).mapToInt(entity->entity.getItem().getCount()).sum();
            test.assertTrue(count==1,"A two-block door returns exactly one item");
        } finally { tc4revival.config.TCConfig.SERVER.wardedStone.set(previous); }
        test.succeed(); com.mojang.logging.LogUtils.getLogger().info("Owned device test passed: doorKeysBindUpperDelegateAndRecoverOnce");
    }
    @GameTest(template="wand_test_empty")
    public static void doorUsesPermissionsAndOnlyMatchingPressurePlate(GameTestHelper test) {
        var level=test.getLevel(); var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WardTest")); var pos=place(test,player); var block=TCArcane.ARCANE_DOOR.get();
        var owned=(OwnedDeviceBlockEntity)level.getBlockEntity(pos); owned.access().setOwner("Owner");
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false);
        block.use(level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit);
        test.assertTrue(!level.getBlockState(pos).getValue(ArcaneDoorBlock.OPEN),"Unauthorized click fails");
        owned.access().grant(player.getGameProfile().getName(),false);
        block.use(level.getBlockState(pos.above()),level,pos.above(),player,InteractionHand.MAIN_HAND,hit);
        test.assertTrue(level.getBlockState(pos).getValue(ArcaneDoorBlock.OPEN) && level.getBlockState(pos.above()).getValue(ArcaneDoorBlock.OPEN),"Authorized upper click opens both halves");
        ArcaneDoorBlock.changeOpen(level,pos,false);
        level.setBlock(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        block.neighborChanged(level.getBlockState(pos),level,pos,Blocks.REDSTONE_BLOCK,pos.east(),false);
        test.assertTrue(!level.getBlockState(pos).getValue(ArcaneDoorBlock.OPEN),"Ordinary redstone cannot open the door");
        var platePos=pos.north(); var plate=TCArcane.ARCANE_PRESSURE_PLATE.get();
        level.setBlock(platePos,plate.defaultBlockState().setValue(ArcanePressurePlateBlock.POWERED,true),18);
        var access=((OwnedDeviceBlockEntity)level.getBlockEntity(platePos)).access(); access.setOwner("Stranger");
        block.neighborChanged(level.getBlockState(pos),level,pos,plate,platePos,false);
        test.assertTrue(!level.getBlockState(pos).getValue(ArcaneDoorBlock.OPEN),"An unrelated plate cannot open the door");
        access.setOwner("Owner"); block.neighborChanged(level.getBlockState(pos),level,pos,plate,platePos,false);
        test.assertTrue(level.getBlockState(pos).getValue(ArcaneDoorBlock.OPEN),"Matching pressed plate opens door");
        level.setBlock(platePos,plate.defaultBlockState(),18); block.neighborChanged(level.getBlockState(pos),level,pos,plate,platePos,false);
        test.assertTrue(!level.getBlockState(pos).getValue(ArcaneDoorBlock.OPEN),"Matching idle plate closes door");
        level.removeBlock(pos.below(),false);
        test.assertTrue(level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()),"Both door halves are removed when support is lost");
        test.succeed(); com.mojang.logging.LogUtils.getLogger().info("Owned device test passed: doorUsesPermissionsAndOnlyMatchingPressurePlate");
    }
}
