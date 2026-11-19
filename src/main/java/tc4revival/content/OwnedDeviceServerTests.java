package tc4revival.content;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class OwnedDeviceServerTests {
    private OwnedDeviceServerTests() {}
    @GameTest(template="wand_test_empty")
    public static void keyDelegationAndOwnerOnlyRemoval(GameTestHelper test) {
        var access=new OwnedDeviceAccess(); access.setOwner("Owner");
        test.assertTrue(access.mayBindKey("Owner",true) && access.mayBindKey("Owner",false),"The owner may bind both key types");
        test.assertTrue(access.grant("Guest",false) && access.mayUse("Guest") && !access.mayConfigure("Guest")
                && !access.mayBindKey("Guest",false),"A normal key grants use without delegation");
        test.assertTrue(access.grant("Guest",true) && access.mayConfigure("Guest") && access.mayBindKey("Guest",false)
                && !access.mayBindKey("Guest",true) && !access.ownedBy("Guest"),"A master grant allows normal-key binding but never ownership or master-key delegation");
        test.assertTrue(!access.grant("Guest",true) && !access.grant("Guest",false) && !access.grant("Owner",true),"Duplicate and redundant key use must not append entries");
        test.assertTrue(!access.mayUse("guest"),"Old player-name comparisons are case sensitive");
        var data=new CompoundTag(); access.save(data); var restored=new OwnedDeviceAccess(); restored.load(data);
        test.assertTrue(restored.owner().equals("Owner") && restored.entries().equals(java.util.List.of("0Guest","1Guest")),"Persist both old permission entries when a normal grant is upgraded");
        test.succeed(); com.mojang.logging.LogUtils.getLogger().info("Owned device test passed: keyDelegationAndOwnerOnlyRemoval");
    }
    @GameTest(template="wand_test_empty")
    public static void placedGlassOwnershipAndWandRecovery(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new net.minecraft.core.BlockPos(3,1,3)); var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WardTest"));
        boolean previous=tc4revival.config.TCConfig.SERVER.wardedStone.get();
        try {
            tc4revival.config.TCConfig.SERVER.wardedStone.set(true);
            level.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),18);
            var glass=new net.minecraft.world.item.ItemStack(TCItems.WARDED_GLASS.get());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,glass);
            var placement=new net.minecraft.world.item.context.BlockPlaceContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,glass,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos.below()),net.minecraft.core.Direction.UP,pos.below(),false));
            test.assertTrue(((net.minecraft.world.item.BlockItem)glass.getItem()).place(placement).consumesAction(),"Place real warded glass through the item path");
            var entity=(OwnedDeviceBlockEntity)level.getBlockEntity(pos);
            test.assertTrue(entity.access().ownedBy(player.getGameProfile().getName()),"Placement records the placer name");
            var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.WOOD,WandParts.Cap.IRON,false,false);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
            var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));
            entity.access().setOwner("DifferentOwner"); entity.access().grant(player.getGameProfile().getName(),true); entity.changed();
            test.assertTrue(TCItems.CASTING_WAND.get().onItemUseFirst(wand,context)==net.minecraft.world.InteractionResult.PASS
                    && level.getBlockState(pos).is(TCArcane.WARDED_GLASS.get()),"A master grant is not permission to recover another owner's glass");
            entity.access().setOwner(player.getGameProfile().getName()); entity.changed();
            test.assertTrue(TCItems.CASTING_WAND.get().onItemUseFirst(wand,context).consumesAction() && level.isEmptyBlock(pos),"The owner can recover with an uncharged wand");
            long drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1)).stream()
                    .filter(item->item.getItem().is(TCItems.WARDED_GLASS.get())).mapToInt(item->item.getItem().getCount()).sum();
            test.assertTrue(drops==1,"Recovery returns exactly one glass item");
            level.setBlock(pos,TCArcane.WARDED_GLASS.get().defaultBlockState(),18);
            ((OwnedDeviceBlockEntity)level.getBlockEntity(pos)).access().setOwner("DifferentOwner");
            tc4revival.config.TCConfig.SERVER.wardedStone.set(false);
            test.assertTrue(TCItems.CASTING_WAND.get().onItemUseFirst(wand,context).consumesAction() && level.isEmptyBlock(pos),"Disabling ward protection allows another player to recover the glass");
            test.succeed(); com.mojang.logging.LogUtils.getLogger().info("Owned device test passed: placedGlassOwnershipAndWandRecovery");
        } finally { tc4revival.config.TCConfig.SERVER.wardedStone.set(previous); }
    }
    @GameTest(template="wand_test_empty")
    public static void plateEntityDetectionPowerAndConfiguration(GameTestHelper test) {
        var level=test.getLevel(); var pos=test.absolutePos(new net.minecraft.core.BlockPos(3,1,3)); var block=TCArcane.ARCANE_PRESSURE_PLATE.get();
        level.setBlock(pos,block.defaultBlockState(),18);
        var entity=(OwnedDeviceBlockEntity)level.getBlockEntity(pos); var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WardTest"));
        entity.access().setOwner("AnotherOwner");
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
        block.use(level.getBlockState(pos),level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        test.assertTrue(level.getBlockState(pos).getValue(ArcanePressurePlateBlock.MODE)==0,"Unauthorized users cannot cycle the setting");
        entity.access().grant(player.getGameProfile().getName(),true);
        block.use(level.getBlockState(pos),level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        test.assertTrue(level.getBlockState(pos).getValue(ArcanePressurePlateBlock.MODE)==1,"Master-authorized users may configure the plate");
        var item=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.05,pos.getZ()+.5,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        level.addFreshEntity(item); block.entityInside(level.getBlockState(pos),level,pos,item);
        var pressed=level.getBlockState(pos);
        test.assertTrue(pressed.getValue(ArcanePressurePlateBlock.POWERED),"Mode one still detects nonplayer entities");
        for (var face:net.minecraft.core.Direction.values()) test.assertTrue(block.getSignal(pressed,level,pos,face)==15
                && block.getDirectSignal(pressed,level,pos,face)==(face==net.minecraft.core.Direction.UP ? 15 : 0),"Weak power is omnidirectional while direct power goes downward");
        block.use(pressed,level,pos,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        block.tick(level.getBlockState(pos),level,pos,level.random);
        test.assertTrue(level.getBlockState(pos).getValue(ArcanePressurePlateBlock.MODE)==2 && !level.getBlockState(pos).getValue(ArcanePressurePlateBlock.POWERED),
                "The next scheduled check releases a mode-two plate when only an item is present");
        item.discard(); test.succeed(); com.mojang.logging.LogUtils.getLogger().info("Owned device test passed: plateEntityDetectionPowerAndConfiguration");
    }
    @GameTest(template="wand_test_empty")
    public static void pressurePlateModesRespectPlayerAndEntityRules(GameTestHelper test) {
        var access=new OwnedDeviceAccess(); access.setOwner("Owner"); access.grant("Guest",false);
        test.assertTrue(access.triggersPlate(0,null,false) && access.triggersPlate(0,"Stranger",false),"Mode zero accepts entities and players");
        test.assertTrue(access.triggersPlate(1,null,false) && access.triggersPlate(1,"Stranger",false)
                && !access.triggersPlate(1,"Owner",false) && !access.triggersPlate(1,"Guest",false),"Mode one excludes authorized players but still detects nonplayers");
        test.assertTrue(!access.triggersPlate(2,null,false) && !access.triggersPlate(2,"Stranger",false)
                && access.triggersPlate(2,"Guest",false),"Mode two detects only authorized players");
        for (int mode=0;mode<=2;mode++) test.assertTrue(!access.triggersPlate(mode,"Owner",true),"Entity pressure-plate immunity applies in every mode");
        test.assertTrue(!access.triggersPlate(7,"Owner",false),"A damaged mode must fail closed instead of dereferencing a missing entity list");
        test.succeed(); com.mojang.logging.LogUtils.getLogger().info("Owned device test passed: pressurePlateModesRespectPlayerAndEntityRules");
    }
    @GameTest(template="wand_test_empty")
    public static void legacyDoorPlateMatchingAndMalformedEntry(GameTestHelper test) {
        var door=new OwnedDeviceAccess(); door.setOwner("DoorOwner"); door.grant("Delegate",false);
        var plate=new OwnedDeviceAccess(); plate.setOwner("Delegate");
        test.assertTrue(door.matchesPressurePlate(plate),"A plate owned by an authorized door user may operate that door");
        plate.setOwner("Unrelated"); plate.grant("DoorOwner",false);
        test.assertTrue(door.matchesPressurePlate(plate),"Common authorized names match despite permission prefixes");
        var saved=new CompoundTag(); door.save(saved);
        var malformed=new CompoundTag(); malformed.putString("name",""); saved.getList("access",net.minecraft.nbt.Tag.TAG_COMPOUND).add(malformed);
        door.load(saved);
        test.assertTrue(door.matchesPressurePlate(plate),"An empty damaged entry neither crashes nor revokes valid permission");
        test.succeed(); com.mojang.logging.LogUtils.getLogger().info("Owned device test passed: legacyDoorPlateMatchingAndMalformedEntry");
    }
}
