package tc4revival.content;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class WardingIsolationServerTests {
    private static ItemStack wand(FakePlayer player) {
        var wand=TCItems.CASTING_WAND.get().create(WandParts.Rod.GREATWOOD,WandParts.Cap.GOLD,false,true);WandFocusItem.attach(wand,new ItemStack(TCItems.WARDING_FOCUS.get()));player.setItemInHand(InteractionHand.MAIN_HAND,wand);return wand;
    }
    private static void cast(FakePlayer player,ItemStack wand,BlockPos pos) {
        TCItems.WARDING_FOCUS.get().cast(wand,player.level(),player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    @GameTest(template="wand_test_empty")
    public static void wardingDelayIsSharedByPositionButIsolatedAcrossDimensions(GameTestHelper test) {
        var overworld=test.getLevel();var nether=overworld.getServer().getLevel(Level.NETHER);test.assertTrue(nether!=null,"Nether loaded");
        var anchor=test.absolutePos(new BlockPos(4,0,4));var pos=new BlockPos(anchor.getX(),80,anchor.getZ());var otherPos=pos.east(2);
        var snapshots=new LinkedHashMap<ServerLevel,Map<BlockPos,BlockState>>();
        for(var level:List.of(overworld,nether)){var states=new LinkedHashMap<BlockPos,BlockState>();for(var p:List.of(pos,otherPos)){test.assertTrue(!level.isOutsideBuildHeight(p),"Shared coordinate is legal in both dimensions");level.getChunk(p);states.put(p,level.getBlockState(p));level.setBlock(p,Blocks.STONE.defaultBlockState(),2);}snapshots.put(level,states);}
        var owner=new FakePlayer(overworld,new GameProfile(UUID.randomUUID(),"WardClock"));var stranger=new FakePlayer(overworld,new GameProfile(UUID.randomUUID(),"WardClockOther"));var remote=new FakePlayer(nether,new GameProfile(UUID.randomUUID(),"WardClockNether"));
        var ownWand=wand(owner);var strangerWand=wand(stranger);var remoteWand=wand(remote);
        try {
            // Even an ineligible target starts the old position-wide delay, shared across players.
            overworld.setBlock(pos,Blocks.GLASS.defaultBlockState(),2);long started=System.currentTimeMillis();cast(stranger,strangerWand,pos);
            overworld.setBlock(pos,Blocks.STONE.defaultBlockState(),2);var unpaid=ownWand.copy();cast(owner,ownWand,pos);
            test.assertTrue(System.currentTimeMillis()-started<500,"Shared-position assertion executed within delay window");
            test.assertTrue(overworld.getBlockState(pos).is(Blocks.STONE)&&ItemStack.matches(unpaid,ownWand),"Another player cannot bypass an invalid target's position delay");
            cast(owner,ownWand,otherPos);cast(remote,remoteWand,pos);
            test.assertTrue(overworld.getBlockEntity(otherPos) instanceof WardedBlockEntity,"Different position remains independent");
            test.assertTrue(nether.getBlockEntity(pos) instanceof WardedBlockEntity,"Same coordinates in another dimension remain independent");
            tc4revival.TC4Revival.LOGGER.info("PASS wardingDelayIsSharedByPositionButIsolatedAcrossDimensions: invalid-target delay shared across players, separate position and Nether coordinates independent");test.succeed();
        } finally {snapshots.forEach((level,states)->states.forEach((p,state)->level.setBlock(p,state,2)));}
    }
    @GameTest(template="wand_test_empty",timeoutTicks=12000)
    public static void wardingResearchConfigFiltersCandidatesAndCompletionWithoutErasingKnowledge(GameTestHelper test) {
        var setting=tc4revival.config.TCConfig.SERVER.wardedStone;boolean old=setting.get();
        var knowledge=new tc4revival.research.PlayerKnowledge(tc4revival.aspect.TCAspects.catalog());var catalog=tc4revival.research.ResearchCatalogs.catalog();var entry=catalog.find("FOCUSWARDING").orElseThrow();
        entry.parents().forEach(knowledge::rememberResearch);entry.hiddenParents().forEach(knowledge::rememberResearch);
        entry.parentsAny().stream().findFirst().ifPresent(knowledge::rememberResearch);
        var server=test.getLevel().getServer();var packs=List.copyOf(server.getPackRepository().getSelectedIds());
        java.util.concurrent.CompletableFuture<Void> checks=java.util.concurrent.CompletableFuture.completedFuture(null);
        // ResearchPrerequisites publishes an immutable definition snapshot on resource reload.
        // Exercise that real lifecycle instead of expecting ConfigValue.set to mutate the snapshot.
        for(int stage=0;stage<5;stage++) {
            final int checkpoint=stage;final boolean enabled=stage%2==0;
            checks=checks.thenComposeAsync(unused->{setting.set(enabled);return server.reloadResources(packs);},server)
                    .thenRunAsync(()->{
                        var current=tc4revival.research.ResearchCatalogs.catalog().require("FOCUSWARDING");
                        var rules=tc4revival.research.ResearchCatalogs.rules();
                        if(checkpoint<3) {
                            test.assertTrue(tc4revival.research.ResearchCatalogs.enabled(current)==enabled&&rules.prerequisites(knowledge,"FOCUSWARDING")==enabled,"Research registration gate follows reloaded configuration");
                            test.assertTrue(rules.matchingCandidates(knowledge,"terra",1).contains("FOCUSWARDING")==enabled,"Disabled core cannot be selected for new research");
                            if(checkpoint==2)knowledge.rememberResearch("FOCUSWARDING");
                        } else {
                            test.assertTrue(rules.completed(knowledge,"FOCUSWARDING")==enabled,"Existing completion is unusable while disabled and returns when enabled");
                            test.assertTrue(knowledge.researchMarkers().contains("FOCUSWARDING"),"Configuration does not delete saved knowledge");
                        }
                    },server);
        }
        checks.whenComplete((unused,error)->server.execute(()->{
            setting.set(old);
            server.reloadResources(packs).whenComplete((restored,restoreError)->server.execute(()->{
                if(restoreError!=null){test.fail("Failed to restore warding configuration snapshot: "+restoreError);return;}
                if(error!=null){test.fail("Warding reload checkpoint failed: "+error);return;}
                tc4revival.TC4Revival.LOGGER.info("PASS wardingResearchConfigFiltersCandidatesAndCompletionWithoutErasingKnowledge: five actual reload checkpoints and restored configuration");test.succeed();
            }));
        }));
    }
}
