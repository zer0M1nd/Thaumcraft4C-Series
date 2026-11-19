package tc4revival.content;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;
import tc4revival.research.*;

/** Server lifecycle and persistence, using injected observations; ray targeting is audited separately. */
@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class ScanSessionServerTests {
    @GameTest(template="wand_test_empty",timeoutTicks=60)
    public static void scanSessionUsesFinalObservationAndCommitsOnlyOnce(GameTestHelper test) {
        var player=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ScanSession"));
        var other=new FakePlayer(test.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ScanOther"));
        var data=KnowledgeSavedData.get(player.server); var air=TCAspects.catalog().require("aer");
        data.update(player.getUUID(),bound->0,k->{k.setPool(air,(short)0);return null;});
        data.update(other.getUUID(),bound->0,k->{k.setPool(air,(short)0);return null;});
        var continuity=new ScanTarget.Entity(UUID.randomUUID());
        var observed=new AtomicReference<>(new ScanTarget(continuity,PlayerKnowledge.ScanKind.OBJECT,Optional.of("scan-session-start"),new AspectAmounts().add(air,1)));
        var session=new ServerScanSession(player,()->Optional.ofNullable(observed.get()));
        var cancelled=new ServerScanSession(other,()->Optional.ofNullable(observed.get()));
        long[] revision={0};
        test.runAtTickTime(1,()->{
            test.assertTrue(session.begin()==ScanProgress.State.TRACKING && cancelled.begin()==ScanProgress.State.TRACKING,"Independent server sessions begin");
            revision[0]=data.revision(player.getUUID());
        });
        for(int elapsed=1;elapsed<=20;elapsed++) {
            final int tick=elapsed;
            test.runAtTickTime(elapsed+1,()->{
                if(tick==10) {
                    observed.set(new ScanTarget(continuity,PlayerKnowledge.ScanKind.OBJECT,Optional.of("scan-session-final"),new AspectAmounts().add(air,7)));
                    cancelled.cancel();
                }
                var update=session.tick();
                if(tick<20) {
                    test.assertTrue(update.state()==ScanProgress.State.TRACKING && update.result().isEmpty(),"Only twenty consecutive world ticks complete");
                    for(int repeat=0;repeat<10;repeat++) test.assertTrue(session.tick().state()==ScanProgress.State.TRACKING,"Duplicate calls cannot accelerate scanning");
                    test.assertTrue(data.revision(player.getUUID())==revision[0] && data.snapshot(player.getUUID()).orElseThrow().pool(air)==0,"No interim knowledge writes");
                } else {
                    test.assertTrue(update.state()==ScanProgress.State.COMPLETED && update.result().orElseThrow().succeeded(),"Final tick commits successful scan");
                    var knowledge=data.snapshot(player.getUUID()).orElseThrow();
                    test.assertTrue(knowledge.pool(air)==7 && knowledge.scanned(PlayerKnowledge.ScanKind.OBJECT,"scan-session-final") && !knowledge.scanned(PlayerKnowledge.ScanKind.OBJECT,"scan-session-start"),"Reward uses final observed identity and amount");
                    test.assertTrue(data.revision(player.getUUID())==revision[0]+1 && session.tick().state()==ScanProgress.State.IDLE && session.begin()==ScanProgress.State.IDLE,"Duplicate completion/restart cannot reward same scan");
                    var restored=KnowledgeSavedData.load(data.save(new net.minecraft.nbt.CompoundTag()),TCAspects.catalog()).snapshot(player.getUUID()).orElseThrow();
                    test.assertTrue(restored.pool(air)==7 && restored.scanned(PlayerKnowledge.ScanKind.OBJECT,"scan-session-final"),"Saved-data roundtrip preserves reward and scan identity");
                    test.assertTrue(cancelled.tick().state()==ScanProgress.State.IDLE && data.snapshot(other.getUUID()).orElseThrow().pool(air)==0,"Cancelled second player receives no reward");
                    tc4revival.TC4Revival.LOGGER.info("PASS scanSessionUsesFinalObservationAndCommitsOnlyOnce: 20 real ticks, duplicate calls, final state, cancellation and knowledge save/load");
                    test.succeed();
                }
                if(tick<10)cancelled.tick();
            });
        }
    }
}
