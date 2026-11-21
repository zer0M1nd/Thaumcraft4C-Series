package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class OwnedDevicePearlServerTests {
    @GameTest(template="wand_test_empty")
    public static void ownedPearlDeviceKindsAndPermissions(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,2,3));
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PearlWardTest"));
        boolean previous=tc4revival.config.TCConfig.SERVER.wardedStone.get();
        try {
            for(var block:java.util.List.of(TCArcane.WARDED_GLASS.get(),TCArcane.ARCANE_DOOR.get(),TCArcane.ARCANE_PRESSURE_PLATE.get())) {
                level.setBlock(pos,block.defaultBlockState(),18);
                var owned=(OwnedDeviceBlockEntity)level.getBlockEntity(pos);
                for(int mode=0;mode<4;mode++) {
                    owned.access().setOwner(mode==0?player.getGameProfile().getName():"SomeoneElse");
                    if(mode==1)owned.access().grant(player.getGameProfile().getName(),true);
                    tc4revival.config.TCConfig.SERVER.wardedStone.set(mode!=3);
                    var pearl=new ThrownEnderpearl(level,player);pearl.setPos(Vec3.atCenterOf(pos.above(2)));
                    test.assertTrue(!level.addFreshEntity(pearl) && pearl.isRemoved(),"All owned device kinds reject pearl even for owner/delegate or disabled break protection");
                    test.assertTrue(level.getEntity(pearl.getUUID())==null,"Rejected pearl never enters entity manager");
                }
                var ownerless=new ThrownEnderpearl(net.minecraft.world.entity.EntityType.ENDER_PEARL,level);ownerless.setPos(Vec3.atCenterOf(pos.above(2)));
                test.assertTrue(!level.addFreshEntity(ownerless) && ownerless.isRemoved(),"Pearls without a player owner are also removed");
                var snowball=new Snowball(level,player);snowball.setPos(Vec3.atCenterOf(pos.above(2)));
                test.assertTrue(level.addFreshEntity(snowball) && !snowball.isRemoved(),"Other projectiles remain unaffected");snowball.discard();
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
            }
        } finally { level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);tc4revival.config.TCConfig.SERVER.wardedStone.set(previous); }
        passed(test,"device-types-and-permissions");
    }
    @GameTest(template="wand_test_empty")
    public static void ownedPearlCubeBoundsAndEntryTiming(GameTestHelper test) {
        var level=test.getLevel();
        // Negative fractions distinguish floor from truncation; a cube corner distinguishes a sphere.
        var origin=new BlockPos(-24200,test.absolutePos(new BlockPos(3,2,3)).getY(),-8200);
        var corner=origin.offset(5,5,5);var outside=origin.offset(6,5,5);
        level.setBlock(corner,TCArcane.WARDED_GLASS.get().defaultBlockState(),18);
        try {
            var pearl=new ThrownEnderpearl(net.minecraft.world.entity.EntityType.ENDER_PEARL,level);
            pearl.setPos(origin.getX()+.8,origin.getY()+.8,origin.getZ()+.8);
            test.assertTrue(!level.addFreshEntity(pearl) && pearl.isRemoved(),"Inclusive five-block cube uses floored spawn coordinates");
            level.setBlock(corner,Blocks.AIR.defaultBlockState(),18);level.setBlock(outside,TCArcane.WARDED_GLASS.get().defaultBlockState(),18);
            var allowed=new ThrownEnderpearl(net.minecraft.world.entity.EntityType.ENDER_PEARL,level);allowed.setPos(Vec3.atCenterOf(origin));
            test.assertTrue(level.addFreshEntity(allowed) && !allowed.isRemoved(),"Six-block axis distance lies outside ward");
            allowed.setPos(Vec3.atCenterOf(outside.above()));
            test.assertTrue(!allowed.isRemoved(),"Moving an already-added pearl near ward is not a new join");allowed.discard();
            level.setBlock(outside,Blocks.AIR.defaultBlockState(),18);
            level.setBlock(corner,TCArcane.WARDED.get().defaultBlockState(),18);
            var focusPearl=new ThrownEnderpearl(net.minecraft.world.entity.EntityType.ENDER_PEARL,level);focusPearl.setPos(Vec3.atCenterOf(origin));
            test.assertTrue(level.addFreshEntity(focusPearl) && !focusPearl.isRemoved(),"Warding focus block is not old TileOwned");focusPearl.discard();
            level.setBlock(corner,Blocks.CHEST.defaultBlockState(),18);
            var ordinary=new ThrownEnderpearl(net.minecraft.world.entity.EntityType.ENDER_PEARL,level);ordinary.setPos(Vec3.atCenterOf(origin));
            test.assertTrue(level.addFreshEntity(ordinary) && !ordinary.isRemoved(),"Ordinary block entities do not ward pearls");ordinary.discard();
        } finally { level.setBlock(corner,Blocks.AIR.defaultBlockState(),18);level.setBlock(outside,Blocks.AIR.defaultBlockState(),18); }
        passed(test,"cube-bounds-and-join-timing");
    }
    private static void passed(GameTestHelper test,String name) { tc4revival.TC4Revival.LOGGER.info("Owned pearl server test PASS: {}",name);test.succeed(); }
}

