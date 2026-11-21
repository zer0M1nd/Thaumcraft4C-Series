package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class TravelingTrunkDimensionServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=200)
    public static void trunkFollowsAcrossDimensionWithInventory(GameTestHelper test) {
        var source=test.getLevel();var destination=source.getServer().getLevel(Level.NETHER);
        var pos=new BlockPos(24700,160,8000);var chunk=new net.minecraft.world.level.ChunkPos(pos);
        boolean forced=destination.getForcedChunks().contains(chunk.toLong());
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TrunkDimension");
        var owner=new net.minecraftforge.common.util.FakePlayer(destination,profile);owner.setPos(Vec3.atCenterOf(pos));
        var trunk=TCEntities.TRAVELING_TRUNK.get().create(source);trunk.setPos(Vec3.atCenterOf(test.absolutePos(new BlockPos(3,2,3))));
        trunk.ownerName(profile.getName());trunk.upgrade(1);trunk.anger(123);trunk.setNoAi(true);
        trunk.inventory.setItem(0,new ItemStack(Items.DIAMOND,13));trunk.inventory.setItem(35,new ItemStack(Items.GOLD_INGOT,17));
        source.addFreshEntity(trunk);var id=trunk.getUUID();TravelingTrunkFollowers.track(trunk);
        Runnable cleanup=()->{
            trunk.discard();var moved=destination.getEntity(id);if(moved!=null)moved.discard();owner.discard();
            if(!forced)destination.setChunkForced(chunk.x,chunk.z,false);
        };
        try {
            destination.addNewPlayer(owner);destination.setChunkForced(chunk.x,chunk.z,true);
            test.startSequence().thenWaitUntil(()->test.assertTrue(destination.isPositionEntityTicking(pos),"Destination entity section becomes active"))
                    .thenExecute(()->{
                        try {
                            MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(owner,source.dimension(),destination.dimension()));
                            test.assertTrue(trunk.isRemoved() && source.getEntity(id)==null,"Source trunk is removed during actual dimension transfer");
                            var moved=(TravelingTrunkEntity)destination.getEntity(id);
                            test.assertTrue(moved!=null && moved.ownerName().equals(profile.getName()),"Destination has one trunk with the same UUID and owner");
                            test.assertTrue(moved.position().distanceTo(owner.position().add(0,.25,0))<.001,"Follower arrives at owner plus quarter-block Y");
                            test.assertTrue(moved.upgrade()==1 && moved.rows()==4 && moved.anger()==0 && moved.inventory.getItem(0).getCount()==13 && moved.inventory.getItem(35).getCount()==17,"Transfer preserves upgrade and every inventory row; transient anger resets");
                            test.assertTrue(moved.isOnPortalCooldown(),"Transfer keeps legacy portal cooldown");
                            // FakePlayer has no Netty channel; the global login bus also runs Forge network negotiation.
                            TravelingTrunkFollowers.login(new PlayerEvent.PlayerLoggedInEvent(owner));
                            test.assertTrue(destination.getEntity(id)==moved,"Repeated lifecycle event cannot duplicate transported inventory");
                            tc4revival.TC4Revival.LOGGER.info("Trunk dimension PASS: actual-transfer-and-inventory");
                        } finally { cleanup.run(); }
                    }).thenSucceed();
        } catch(RuntimeException failure) { cleanup.run();throw failure; }
    }
    @GameTest(template="wand_test_empty")
    public static void trunkTransferGuardsAndLifecycle(GameTestHelper test) {
        var source=test.getLevel();var destination=source.getServer().getLevel(Level.NETHER);
        var owner=new net.minecraftforge.common.util.FakePlayer(destination,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TrunkGuards"));
        destination.addNewPlayer(owner);
        var trunks=new java.util.ArrayList<TravelingTrunkEntity>();
        try {
            for(int mode=0;mode<3;mode++) {
                var trunk=TCEntities.TRAVELING_TRUNK.get().create(source);trunk.setPos(Vec3.atCenterOf(test.absolutePos(new BlockPos(3,2,3))));
                trunk.ownerName(owner.getGameProfile().getName());trunk.setNoAi(true);trunk.inventory.setItem(35,new ItemStack(Items.EMERALD,7));source.addFreshEntity(trunk);trunks.add(trunk);
                if(mode==0)trunk.stay(true);
                if(mode==1)trunk.setPortalCooldown();
                if(mode!=2)TravelingTrunkFollowers.track(trunk);
            }
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(owner,false));
            for(var trunk:trunks)test.assertTrue(!trunk.isRemoved() && trunk.level()==source && trunk.inventory.getItem(35).getCount()==7,"Stay, cooldown and unobserved owner links block transfer without losing inventory");
            test.assertTrue(trunks.get(0).isOnPortalCooldown(),"Legacy lifecycle still sets cooldown before a stay refusal");
            var absent=TCEntities.TRAVELING_TRUNK.get().create(source);absent.ownerName("AbsentTrunkOwner");
            test.assertTrue(absent.changeDimension(destination)==null && !absent.isRemoved(),"Missing destination owner leaves source intact");
            test.assertTrue(trunks.get(2).changeDimension(source)==null,"Same-dimension transfer is ignored");
            tc4revival.TC4Revival.LOGGER.info("Trunk dimension PASS: guards-and-lifecycle");test.succeed();
        } finally { trunks.forEach(Entity::discard);owner.discard(); }
    }
}
