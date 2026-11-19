package tc4revival.content;

import java.util.*;
import java.util.function.Consumer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class NodeRemovalServerTests {
    @GameTest(template="wand_test_empty")
    public static void nodeHarvestRetainsSeparateEssencesAndThresholds(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"NodeHarvestAudit"));player.setPos(pos.getCenter());
        var rule=level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);boolean before=rule.get();
        var drops=new ArrayList<ItemStack>();
        Consumer<EntityJoinLevelEvent> capture=event->{
            if(event.getLevel()==level && event.getEntity() instanceof ItemEntity item && item.position().distanceToSqr(pos.getCenter())<4) {
                drops.add(item.getItem().copy());event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,capture);int cases=0;
        try {
            for(var mode:new GameType[]{GameType.SURVIVAL,GameType.CREATIVE})for(boolean enabled:new boolean[]{true,false}) {
                player.setGameMode(mode);rule.set(enabled,level.getServer());
                for(int amount:new int[]{0,4,5,9,10,20,640}) {
                    level.setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState(),18);
                    var values=new AspectAmounts();for(var aspect:TCAspects.catalog().primals()) values.add(aspect,amount);
                    ((AuraNodeBlockEntity)level.getBlockEntity(pos)).configure(values,AuraNodeType.NORMAL,null,"harvest-audit",false);
                    drops.clear();test.assertTrue(player.gameMode.destroyBlock(pos)&&level.isEmptyBlock(pos),"Actual node harvest removes the node: "+mode);
                    int perAspect=enabled&&amount>=5?amount/10+1:0;
                    test.assertTrue(drops.size()==perAspect*TCAspects.catalog().primals().size(),"Old count threshold and separate entities: "+amount+"/"+mode+"/"+enabled);
                    var seen=new HashMap<Aspect,Integer>();
                    for(var stack:drops) {
                        test.assertTrue(stack.is(TCItems.WISP_ESSENCE.get())&&stack.getCount()==1,"Each harvest drop is one essence, never a merged stack");
                        var stored=TCItems.WISP_ESSENCE.get().storedAspects(stack);
                        test.assertTrue(stored.size()==1&&stored.total()==2,"Each essence retains two units of exactly one aspect");
                        seen.merge(stored.aspects().get(0),1,Integer::sum);
                    }
                    for(var aspect:TCAspects.catalog().primals())test.assertTrue(seen.getOrDefault(aspect,0)==perAspect,"Independent amount for each aspect");
                    cases++;
                }
            }
        } finally {MinecraftForge.EVENT_BUS.unregister(capture);rule.set(before,level.getServer());}
        tc4revival.TC4Revival.LOGGER.info("PASS nodeHarvestRetainsSeparateEssencesAndThresholds: {} cases",cases);test.succeed();
    }

    @GameTest(template="wand_test_empty")
    public static void energizedNodeFailureScattersLegacyOverflowFlux(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,20,3));
        int cases=0;
        for(int failure=0;failure<3;failure++)for(int seed=0;seed<4;seed++) {
            for(var target:BlockPos.betweenClosed(pos.offset(-7,-7,-7),pos.offset(7,7,7)))level.setBlock(target,Blocks.AIR.defaultBlockState(),18);
            var solids=new ArrayList<BlockPos>();
            for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)if(((x+z)&1)==0) {
                var target=pos.offset(x,-2,z);solids.add(target);level.setBlock(target,Blocks.STONE.defaultBlockState(),18);
            }
            level.setBlock(pos.below(),TCArcane.NODE_STABILIZER.get().defaultBlockState(),18);
            level.setBlock(pos.above(),TCArcane.NODE_CONVERTER.get().defaultBlockState(),18);
            level.setBlock(pos,TCArcane.ENERGIZED_NODE.get().defaultBlockState(),18);
            // First verify the valid structure does not collapse on the same neighbor notification.
            level.updateNeighborsAt(pos.below(),TCArcane.NODE_STABILIZER.get());
            test.assertTrue(level.getBlockState(pos).is(TCArcane.ENERGIZED_NODE.get()),"Valid structure survives neighbor notification");
            if(failure==0)level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),18);
            if(failure==1)level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),18);
            if(failure==2)level.setBlock(pos.below().east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),18);
            level.random.setSeed(1200+seed);
            level.updateNeighborsAt(pos.below(),TCArcane.NODE_STABILIZER.get());
            test.assertTrue(!(level.getBlockEntity(pos) instanceof EnergizedNodeBlockEntity),"Missing stabilizer, missing converter, or powered stabilizer collapses the node");
            int gas=0,goo=0;
            for(var target:BlockPos.betweenClosed(pos.offset(-7,-7,-7),pos.offset(7,7,7))) {
                var state=level.getBlockState(target);
                if(state.is(TCArcane.FLUX_GAS.get())||state.is(TCArcane.FLUX_GOO.get())) {
                    test.assertTrue(state.getValue(FiniteFluidBlock.QUANTA)==9,"Old metadata eight is nine quanta, not ordinary full capacity");
                    test.assertTrue(state.is(TCArcane.FLUX_GOO.get())==(target.getY()<pos.getY()),"Goo strictly below the origin; gas at or above it");
                    if(state.is(TCArcane.FLUX_GAS.get()))gas++;else goo++;
                }
            }
            test.assertTrue(gas>0&&goo>0&&gas+goo<=50,"Fifty attempts yield both forms without treating attempts as guaranteed placements");
            for(var solid:solids)test.assertTrue(level.getBlockState(solid).is(Blocks.STONE),"Explosion preserves terrain and pollution only occupies air");
            cases++;
        }
        tc4revival.TC4Revival.LOGGER.info("PASS energizedNodeFailureScattersLegacyOverflowFlux: {} cases",cases);test.succeed();
    }
}
