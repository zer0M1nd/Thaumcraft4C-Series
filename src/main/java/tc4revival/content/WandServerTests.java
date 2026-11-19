package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Written now; run only after the complete wand-system milestone permits server validation. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public  class WandServerTests {
    private WandServerTests() {}
    @GameTest(template = "wand_test_empty")
    public static void wardedBeaconBaseUsesOriginalMaterial(GameTestHelper test) throws ReflectiveOperationException {
        var center=new BlockPos(3,3,3); var pos=test.absolutePos(center); var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FixturePlayer"));
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) test.setBlock(center.offset(x,-1,z),Blocks.IRON_BLOCK);
        test.assertTrue(WardedBlockEntity.protect(player,pos.below()),"Iron base must accept warding");
        var calculation=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(net.minecraft.world.level.block.entity.BeaconBlockEntity.class,
                "m_155092_",net.minecraft.world.level.Level.class,int.class,int.class,int.class);
        int tiers=(Integer)calculation.invoke(null,test.getLevel(),pos.getX(),pos.getY(),pos.getZ());
        test.assertTrue(tiers==1,"Actual vanilla beacon calculation must count warded iron");
        test.setBlock(center.below(),Blocks.STONE);
        test.assertTrue(WardedBlockEntity.protect(player,pos.below()),"Stone must accept warding");
        test.assertTrue((Integer)calculation.invoke(null,test.getLevel(),pos.getX(),pos.getY(),pos.getZ())==0,
                "A ward over stone must not be treated as a beacon base"); test.succeed();
    }
    @GameTest(template = "wand_test_empty",timeoutTicks = 40)
    public static void wardedLogsSupportLeavesUntilRemoved(GameTestHelper test) {
        var log=new BlockPos(3,2,3); var leaf=log.east(); var level=test.getLevel();
        test.setBlock(log,Blocks.OAK_LOG); test.assertTrue(WardedBlockEntity.protect(new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FixturePlayer")),test.absolutePos(log)),"Log must accept warding");
        test.setBlock(leaf,Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true));
        var position=test.absolutePos(leaf);
        level.scheduleTick(position,Blocks.OAK_LEAVES,1);
        test.runAfterDelay(5,()->{
            test.assertTrue(test.getBlockState(leaf).getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)==1,"Leaf distance tick must see the saved log");
            test.setBlock(log,Blocks.STONE);
        });
        test.runAfterDelay(10,()->{
            test.assertTrue(test.getBlockState(leaf).getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)==7,"Neighbor updates must remove support when the saved log is gone"); test.succeed();
        });
    }
    @GameTest(template = "wand_test_empty")
    public static void nodeJarReleasePreservesIdentityAndCurrentCharge(GameTestHelper test) {
        var location=new BlockPos(3,2,3); var pos=test.absolutePos(location); var player=new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FixturePlayer"));
        var air=tc4revival.aspect.TCAspects.catalog().require("aer");
        test.setBlock(location,TCArcane.NODE_JAR.get()); var jar=(NodeJarBlockEntity)test.getBlockEntity(location);
        jar.configure(new NodeJarData(new tc4revival.aspect.AspectAmounts().add(air,30),AuraNodeType.PURE,AuraNodeModifier.PALE,"transport-check"));
        test.assertTrue(jar.takeFromContainer(air,5),"Sealed node charge can be removed through its container contract");
        jar.load(jar.saveWithoutMetadata());
        test.assertTrue(jar.base().amount(air)==30 && jar.aspects().amount(air)==25,"Save/load must not replace capacity with remaining charge");
        test.assertTrue(NodeJarData.read(jar.asItem().getTag()).aspects().amount(air)==25,"Portable form must carry remaining charge only");
        var scanned=tc4revival.research.ScanTargetResolver.block(test.getLevel(),pos).orElseThrow();
        var wand=new ItemStack(TCItems.CASTING_WAND.get()); player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        test.assertTrue(jar.onWandUseFirst(wand,context).consumesAction(),"Wand must release the node");
        var node=(AuraNodeBlockEntity)test.getBlockEntity(location);
        test.assertTrue(node.aspects().amount(air)==25 && node.base().amount(air)==30 && node.nodeType()==AuraNodeType.PURE && node.nodeModifier()==AuraNodeModifier.PALE,
                "Released node retains current charge and original permanent capacity separately");
        var released=tc4revival.research.ScanTargetResolver.block(test.getLevel(),pos).orElseThrow();
        test.assertTrue(scanned.identity().equals(released.identity()) && scanned.continuity().equals(released.continuity())
                && scanned.aspects().view().equals(released.aspects().view()),"Jar and released node must resolve the same scan result"); test.succeed();
    }
    @GameTest(template = "wand_test_empty", timeoutTicks = 40)
    public static void holeRestoresFullStateAfterSavedCountdown(GameTestHelper test) {
        var relative = new BlockPos(3, 2, 3); var position = test.absolutePos(relative);
        var original = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        test.setBlock(relative, original); var player = new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FixturePlayer"));
        test.assertTrue(PortableHoleBlockEntity.open(player, position, -1, 1, 10), "Solid block must open");
        var hole = (PortableHoleBlockEntity) test.getBlockEntity(relative);
        var saved = hole.saveWithoutMetadata(); saved.putInt("countdown", 8); hole.load(saved);
        test.runAfterDelay(3, () -> {
            test.assertTrue(test.getBlockState(relative).equals(original), "Restored state must retain the original log axis"); test.succeed();
        });
    }
    @GameTest(template = "wand_test_empty")
    public static void holeAndWardCannotDestroyContainerContents(GameTestHelper test) {
        var relative = new BlockPos(3, 2, 3); var pos = test.absolutePos(relative);
        test.setBlock(relative, Blocks.CHEST); var chest = (ChestBlockEntity) test.getBlockEntity(relative);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 17)); var player = new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FixturePlayer"));
        test.assertTrue(!PortableHoleBlockEntity.open(player, pos, -1, 1, 10), "Hole must reject block entities");
        test.assertTrue(!WardedBlockEntity.protect(player, pos), "Ward must reject block entities");
        test.assertTrue(test.getBlockEntity(relative) == chest && chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 17,
                "Rejected operations must leave the same container and contents intact"); test.succeed();
    }
    @GameTest(template = "wand_test_empty")
    public static void wardSaveRoundTripPreservesOwnerAndBlockProperties(GameTestHelper test) {
        var relative = new BlockPos(3, 2, 3); var pos = test.absolutePos(relative); var player = new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FixturePlayer"));
        var original = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z); test.setBlock(relative, original);
        test.assertTrue(WardedBlockEntity.protect(player, pos), "Conductor without a block entity must accept warding");
        var ward = (WardedBlockEntity) test.getBlockEntity(relative); var saved = ward.saveWithoutMetadata();
        var restored = new WardedBlockEntity(pos, ward.getBlockState()); restored.load(saved);
        test.assertTrue(restored.ownedBy(player) && restored.original().equals(original), "Owner UUID and all original state properties must survive save/load");
        saved.putUUID("Owner", java.util.UUID.randomUUID()); restored.load(saved);
        test.assertTrue(!restored.ownedBy(player), "A different UUID must not acquire ownership"); test.succeed();
    }
    @GameTest(template = "wand_test_empty")
    public static void stationaryFrostImpactStaysFiniteAndHonoursPostDecrement(GameTestHelper test) {
        var shard = new FrostShardEntity(TCEntities.FROST_SHARD.get(), test.getLevel());
        var pos = test.absolutePos(new BlockPos(3, 2, 3)); shard.setPos(Vec3.atCenterOf(pos)); shard.setDeltaMovement(Vec3.ZERO);
        var hit = new BlockHitResult(shard.position(), Direction.UP, pos, false);
        for (int i = 0; i < 3; i++) {
            shard.onHit(hit); test.assertTrue(!shard.isRemoved(), "Ordinary shard survives its first three impacts");
            test.assertTrue(Double.isFinite(shard.getX()) && Double.isFinite(shard.getY()) && Double.isFinite(shard.getZ()), "Stationary bounce must not introduce NaN");
        }
        shard.onHit(hit); test.assertTrue(shard.isRemoved(), "Fourth impact breaks the ordinary shard");
        tc4revival.TC4Revival.LOGGER.info("PASS stationaryFrostImpactStaysFiniteAndHonoursPostDecrement");test.succeed();
    }
    @GameTest(template = "wand_test_empty")
    public static void fragileFrostAppliesSlowAndClearsDamageImmunity(GameTestHelper test) {
        var player = new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FrostImpact"));
        var cow = EntityType.COW.create(test.getLevel());
        cow.setPos(Vec3.atCenterOf(test.absolutePos(new BlockPos(3, 2, 3))));
        var shard = new FrostShardEntity(test.getLevel(), player, 8, 1, 2, true, false); shard.setPos(cow.position());
        shard.onHit(new EntityHitResult(cow));
        test.assertTrue(cow.getHealth()<cow.getMaxHealth(),"A real frost impact damages the living target");
        test.assertTrue(shard.isRemoved() && cow.invulnerableTime == 0, "Scatter shard vanishes and allows following pellets to deal damage");
        var slow = cow.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        test.assertTrue(slow != null && slow.getDuration() == 200 && slow.getAmplifier() == 1, "Frost level two supplies ten seconds of slowness II");
        tc4revival.TC4Revival.LOGGER.info("PASS fragileFrostAppliesSlowAndClearsDamageImmunity");test.succeed();
    }
    @GameTest(template = "wand_test_empty")
    public static void devilBatKeepsLegacyHealthAndRejectsFireDamage(GameTestHelper test) {
        var player = new net.minecraftforge.common.util.FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FixturePlayer")); var cow = EntityType.COW.create(test.getLevel());
        var bat = new FireBatEntity(TCEntities.FIRE_BAT.get(), test.getLevel()); bat.summon(player, cow, 2, false, true, true);
        test.assertTrue(bat.getMaxHealth() == 5 && bat.getHealth() == 5, "Devil upgrade retains legacy five-health behaviour");
        test.assertTrue(bat.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == 5, "Devil damage includes potency");
        test.assertTrue(!bat.hurt(test.getLevel().damageSources().inFire(), 100) && bat.getHealth() == 5, "Fire must not damage the bat");
        var saved = new net.minecraft.nbt.CompoundTag(); bat.saveWithoutId(saved);
        var restored = new FireBatEntity(TCEntities.FIRE_BAT.get(), test.getLevel()); restored.load(saved);
        test.assertTrue(restored.summoned() && restored.devil() && restored.vampire() && !restored.hanging(), "Variant flags survive reload"); test.succeed();
    }
}
