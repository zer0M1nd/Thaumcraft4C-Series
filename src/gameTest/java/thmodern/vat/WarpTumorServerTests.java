package thmodern.vat;
import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge.WarpKind;
import thmodern.Horizons;

@GameTestHolder(Horizons.ID) @PrefixGameTestTemplate(false)
public final class WarpTumorServerTests {
    @GameTest(template="empty")
    public static void tumorStoresInPriorityOrderAndReturnsExactlyOnce(GameTestHelper test){
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"TumorTest"));var knowledge=KnowledgeSavedData.get(player.server);int capacity=WarpTumor.CAPACITY.get();
        knowledge.update(player.getUUID(),player.getRandom()::nextInt,k->{k.setWarp(WarpKind.PERMANENT,capacity+5);k.setWarp(WarpKind.STICKY,7);k.setWarp(WarpKind.TEMPORARY,9);return null;});
        SelfInfusions.replace(player,Set.of("warped_tumor"));var first=knowledge.snapshot(player.getUUID()).orElseThrow();
        test.assertTrue(first.warp(WarpKind.PERMANENT)==5&&first.warp(WarpKind.STICKY)==7&&first.warp(WarpKind.TEMPORARY)==9,"Permanent warp fills capacity before sticky or temporary");
        WarpTumor.absorb(player);test.assertTrue(knowledge.snapshot(player.getUUID()).orElseThrow().warp(WarpKind.PERMANENT)==5,"Repeated absorb cannot exceed capacity");
        SelfInfusions.replace(player,Set.of());WarpTumor.release(player);var restored=knowledge.snapshot(player.getUUID()).orElseThrow();
        test.assertTrue(restored.warp(WarpKind.PERMANENT)==capacity+5&&restored.warp(WarpKind.STICKY)==7&&restored.warp(WarpKind.TEMPORARY)==9,"Discarding a body restores every held point exactly once");test.succeed();
    }
    @GameTest(template="empty")
    public static void endReturnDoesNotLoseOrDuplicateStoredWarp(GameTestHelper test){
        var original=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"TumorEndTest"));var knowledge=KnowledgeSavedData.get(original.server);int amount=Math.min(12,WarpTumor.CAPACITY.get());
        knowledge.update(original.getUUID(),original.getRandom()::nextInt,k->{k.setWarp(WarpKind.STICKY,amount);return null;});SelfInfusions.replace(original,Set.of("warped_tumor"));
        var returned=new FakePlayer(test.getLevel(),original.getGameProfile());SelfInfusions.clonePlayer(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(returned,original,false));
        test.assertTrue(knowledge.snapshot(original.getUUID()).orElseThrow().warp(WarpKind.STICKY)==0,"Non-death replacement retains the liability without returning it");
        SelfInfusions.died(new net.minecraftforge.event.entity.living.LivingDeathEvent(returned,returned.damageSources().generic()));
        test.assertTrue(knowledge.snapshot(original.getUUID()).orElseThrow().warp(WarpKind.STICKY)==amount&&!SelfInfusions.has(returned,"warped_tumor"),"Actual death returns the copied liability and removes body powers");test.succeed();
    }
}
