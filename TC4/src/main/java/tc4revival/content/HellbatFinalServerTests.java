package tc4revival.content;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class HellbatFinalServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=100)
    public static void naturalFireBatCombatHasOneDamageTwoSecondFireAndNoPlayerCredit(GameTestHelper test) {
        var level=test.getLevel();var at=Vec3.atCenterOf(test.absolutePos(new BlockPos(4,20,4)));var blasts=new ArrayList<net.minecraft.world.level.Explosion>();
        Consumer<ExplosionEvent.Start> capture=e->{if(e.getLevel()==level&&e.getExplosion().getDirectSourceEntity() instanceof FireBatEntity){blasts.add(e.getExplosion());e.setCanceled(true);}};MinecraftForge.EVENT_BUS.addListener(capture);
        int explosions=0,bites=0,fires=0;
        try {
            for(int sample=0;sample<64;sample++) {
                var cow=EntityType.COW.create(level);cow.moveTo(at.add(1,0,0));var bat=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);HellbatAuditServerTests.baselineDamage(bat);bat.moveTo(at);bat.setTarget(cow);bat.customServerAiStep();
                try {
                    test.assertTrue(!bat.hanging()&&!bat.summoned(),"Natural fixture wakes without summon flags");
                    long seed=sample*104729L;var oracle=net.minecraft.util.RandomSource.create(seed);boolean explode=oracle.nextInt(10)==0;boolean bite=!explode&&oracle.nextBoolean();
                    blasts.clear();level.random.setSeed(seed);bat.customServerAiStep();
                    test.assertTrue(LegacyCombatCredit.remaining(cow)==0,"Natural attacks never grant player kill credit");
                    if(explode){test.assertTrue(blasts.size()==1&&bat.isRemoved(),"Natural one-in-ten branch requests explosion");explosions++;}
                    else if(bite){test.assertTrue(cow.getHealth()==9&&!bat.isRemoved(),"Natural bite deals one damage");bites++;}
                    else {test.assertTrue(cow.getHealth()==10&&cow.getRemainingFireTicks()==40&&!bat.isRemoved(),"Natural fire branch lasts two seconds");fires++;}
                } finally {cow.discard();bat.discard();}
            }
            test.assertTrue(explosions>0&&bites>0&&fires>0,"All natural branches exercised");
            tc4revival.TC4Revival.LOGGER.info("PASS naturalFireBatCombatHasOneDamageTwoSecondFireAndNoPlayerCredit: 64 seeds, {}/{}/{} explosion/bite/fire; explosions intercepted",explosions,bites,fires);test.succeed();
        } finally {MinecraftForge.EVENT_BUS.unregister(capture);}
    }
    private static int flags(FireBatEntity bat) {return (bat.hanging()?1:0)|(bat.summoned()?2:0)|(bat.explosive()?4:0)|(bat.devil()?8:0)|(bat.vampire()?16:0);}
    @GameTest(template="wand_test_empty")
    public static void fireBatAllFlagsRoundTripActualMetadataPackets(GameTestHelper test) {
        var level=test.getLevel();int transfers=0;
        for(int bits=0;bits<32;bits++) {
            var source=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);var receiver=new FireBatEntity(TCEntities.FIRE_BAT.get(),level);
            try {
                for(int expected:new int[]{bits,31-bits}) {
                    var tag=new net.minecraft.nbt.CompoundTag();source.saveWithoutId(tag);tag.putByte("BatFlags",(byte)expected);source.load(tag);
                    var dirty=source.getEntityData().packDirty();
                    var packet=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(source.getId(),dirty==null?List.of():dirty);
                    var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                    try {
                        packet.write(buffer);var decoded=new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(buffer);
                        test.assertTrue(decoded.id()==source.getId()&&buffer.readableBytes()==0,"Actual packet ID and full consumption");
                        receiver.getEntityData().assignValues(decoded.packedItems());
                        test.assertTrue(flags(receiver)==expected,"Five flag bits survive packet and opposite transition: "+expected);transfers++;
                    } finally {buffer.release();}
                }
            } finally {source.discard();receiver.discard();}
        }
        tc4revival.TC4Revival.LOGGER.info("PASS fireBatAllFlagsRoundTripActualMetadataPackets: {} metadata transfers to server-side replica, not client rendering",transfers);test.succeed();
    }
}
