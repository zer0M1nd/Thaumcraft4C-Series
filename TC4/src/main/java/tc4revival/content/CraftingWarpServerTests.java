package tc4revival.content;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.gametest.*;
import tc4revival.config.TCConfig;
import tc4revival.research.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class CraftingWarpServerTests {
    @GameTest(template="wand_test_empty")
    public static void craftingWarpEventCoverageAndExclusions(GameTestHelper test) {
        var player=new FakePlayer(test.getLevel(),new GameProfile(UUID.randomUUID(),"CraftWarpTest"));
        player.setGameMode(GameType.SURVIVAL);
        var store=KnowledgeSavedData.get(player.server); store.ensure(player.getUUID(),bound->0);
        boolean disabled=TCConfig.SERVER.disableWarp.get();
        try {
            TCConfig.SERVER.disableWarp.set(false);
            store.update(player.getUUID(),bound->0,knowledge->{knowledge.changeWarp(PlayerKnowledge.WarpKind.PERMANENT,3);knowledge.changeWarp(PlayerKnowledge.WarpKind.TEMPORARY,2);return null;});
            var outputs=List.of(TCItems.HELLBAT_FOCUS.get(),TCItems.SINISTER_STONE.get(),TCItems.BRAIN_JAR.get(),
                    TCItems.LIQUID_DEATH_BUCKET.get(),TCItems.TAINT_BOTTLE.get(),TCItems.golem(GolemMaterial.FLESH),
                    TCItems.PRIMAL_FOCUS.get(),TCItems.wandComponent("staff_rod_primal"));
            int expected=0;
            for(var item:outputs) {
                var stack=new ItemStack(item);stack.getOrCreateTag().putString("custom","retained");
                var before=stack.copy();fire(player,stack);expected++;
                var knowledge=store.snapshot(player.getUUID()).orElseThrow();
                test.assertTrue(knowledge.warp(PlayerKnowledge.WarpKind.STICKY)==expected && knowledge.warpCounter()==expected+5,"Each registered crafting event gains one sticky warp and resets the event counter");
                test.assertTrue(ItemStack.matches(stack,before),"Warp handling does not mutate the crafted item");
            }
            player.setGameMode(GameType.CREATIVE);
            fire(player,new ItemStack(TCItems.TAINT_BOTTLE.get(),16));expected++;
            test.assertTrue(store.snapshot(player.getUUID()).orElseThrow().warp(PlayerKnowledge.WarpKind.STICKY)==expected,"Creative and multi-output crafting still award exactly once per event");
            long revision=store.revision(player.getUUID());
            var active=new ItemStack(TCItems.SINISTER_STONE.get());active.getOrCreateTag().putBoolean("LegacyActive",true);
            for(var stack:List.of(active,new ItemStack(TCItems.golem(GolemMaterial.WOOD)),new ItemStack(TCItems.wandComponent("wand_rod_silverwood")),new ItemStack(Items.DIAMOND),ItemStack.EMPTY))fire(player,stack);
            test.assertTrue(store.revision(player.getUUID())==revision,"Other metadata, materials and ordinary outputs do not grant crafting warp");
            TCConfig.SERVER.disableWarp.set(true);fire(player,new ItemStack(TCItems.HELLBAT_FOCUS.get()));
            test.assertTrue(store.revision(player.getUUID())==revision,"Disable-warp configuration suppresses the actual subscribed crafting callback");
            var knowledge=store.snapshot(player.getUUID()).orElseThrow();
            test.assertTrue(knowledge.warp(PlayerKnowledge.WarpKind.PERMANENT)==3 && knowledge.warp(PlayerKnowledge.WarpKind.TEMPORARY)==2,"Crafting leaves other warp categories unchanged");
        } finally {TCConfig.SERVER.disableWarp.set(disabled);}
        tc4revival.TC4Revival.LOGGER.info("Crafting warp PASS: eight-items-event-registration-metadata-count-creative-config-counter");test.succeed();
    }
    private static void fire(FakePlayer player,ItemStack stack) {
        ForgeEventFactory.firePlayerCraftingEvent(player,stack,new SimpleContainer(9));
    }
}
