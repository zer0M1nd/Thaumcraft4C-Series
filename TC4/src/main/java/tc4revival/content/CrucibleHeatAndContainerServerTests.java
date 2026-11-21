package tc4revival.content;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class CrucibleHeatAndContainerServerTests {
    @GameTest(template="wand_test_empty")
    public static void thaumatoriumAndCrucibleShareModernHeatSources(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,4,2));
        level.setBlock(pos,TCArcane.THAUMATORIUM.get().defaultBlockState(),2);
        var tile=(ThaumatoriumBlockEntity)level.getBlockEntity(pos);
        for(var source:java.util.List.of(Blocks.CAMPFIRE,Blocks.SOUL_CAMPFIRE))for(boolean lit:new boolean[]{false,true}) {
            var state=source.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,lit);
            level.setBlock(pos.below(2),state,2);
            test.assertTrue(tile.heated()==lit,"Thaumatorium heat follows campfire light state: "+source+" "+lit);
            test.assertTrue(CrucibleHeat.heated(state)==lit,"Crucible matches thaumatorium");
        }
        for(var source:java.util.List.of(Blocks.MAGMA_BLOCK,Blocks.LAVA,Blocks.FIRE,Blocks.SOUL_FIRE,TCArcane.NITOR.get())) {
            level.setBlock(pos.below(3),source==Blocks.SOUL_FIRE?Blocks.SOUL_SOIL.defaultBlockState():Blocks.NETHERRACK.defaultBlockState(),2);
            level.setBlock(pos.below(2),source.defaultBlockState(),2);
            test.assertTrue(tile.heated(),"Thaumatorium supports "+source);
        }
        level.setBlock(pos.below(2),Blocks.STONE.defaultBlockState(),2);
        test.assertTrue(!tile.heated(),"Cold blocks cannot run thaumatorium recipes");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void crucibleWorldHeatAndContainersMatchLegacyRules(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        level.setBlock(pos,TCArcane.CRUCIBLE.get().defaultBlockState(),2);
        var tile=(CrucibleBlockEntity)level.getBlockEntity(pos);
        var sides=new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST};int cases=0;
        try {
            level.setBlock(pos.below(2),Blocks.STONE.defaultBlockState(),2);
            for(var source:java.util.List.of(Blocks.FIRE,Blocks.LAVA,Blocks.MAGMA_BLOCK,Blocks.CAMPFIRE,TCArcane.NITOR.get(),Blocks.STONE)) for(int bellows=0;bellows<=4;bellows++) {
                level.setBlock(pos.below(),source.defaultBlockState(),2);
                for(int i=0;i<4;i++)level.setBlock(pos.relative(sides[i]),i<bellows?TCArcane.BELLOWS.get().defaultBlockState().setValue(BellowsBlock.FACING,Direction.UP):Blocks.AIR.defaultBlockState(),2);
                // Redstone powers the side bellows; old crucible ignores orientation and power.
                for(var side:sides)level.setBlock(pos.relative(side).below(),Blocks.REDSTONE_BLOCK.defaultBlockState(),2);
                test.assertTrue(level.getBlockState(pos.below()).is(source),"Heat-source fixture survives neighbor shape updates");
                for(int water:new int[]{0,1000}) for(int heat:new int[]{0,150,199,200}) {
                    reset(tile,heat,water);tile.tick();
                    int wanted=water>0&&source!=Blocks.STONE?(heat<200?heat+1+2*bellows:heat):Math.max(0,heat-1);
                    test.assertTrue(tile.contents().heat()==wanted,"World heat source/bellows/water boundary: "+source+" "+bellows+" "+heat+" "+water);
                    test.assertTrue(tile.contents().boiling()==(wanted>150),"Exact boiling threshold");cases++;
                }
            }
            for(var side:sides){level.setBlock(pos.relative(side),Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.relative(side).below(),Blocks.AIR.defaultBlockState(),2);}
            level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),2);
            for(boolean creative:new boolean[]{false,true}) for(boolean bottle:new boolean[]{false,true}) for(int initial:new int[]{0,480,999,1000}) {
                reset(tile,0,initial);
                var player=new FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CrucibleFill"));
                player.setGameMode(creative?GameType.CREATIVE:GameType.SURVIVAL);
                var held=bottle?PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.WATER):new ItemStack(Items.WATER_BUCKET);
                player.setItemInHand(InteractionHand.MAIN_HAND,held);
                tile.getBlockState().getBlock().use(tile.getBlockState(),level,pos,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
                int volume=bottle?250:1000;boolean accepted=bottle?initial+volume<=1000:initial<1000;
                test.assertTrue(tile.contents().water()==(accepted?Math.min(1000,initial+volume):initial),"Buckets top up like cauldrons; bottles require room for their full fluid volume");
                var after=player.getMainHandItem();
                test.assertTrue(after.is(accepted&&!creative?(bottle?Items.GLASS_BOTTLE:Items.BUCKET):(bottle?Items.POTION:Items.WATER_BUCKET)),"Survival returns its empty container; creative and rejected fills preserve the held item");
            }
        } finally {
            reset(tile,0,0);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),2);
            level.setBlock(pos.below(2),Blocks.AIR.defaultBlockState(),2);
            for(var side:sides){level.setBlock(pos.relative(side),Blocks.AIR.defaultBlockState(),2);level.setBlock(pos.relative(side).below(),Blocks.AIR.defaultBlockState(),2);}
        }
        tc4revival.TC4Revival.LOGGER.info("PASS crucibleWorldHeatAndContainersMatchLegacyRules: {} heat cases and 16 actual bottle/bucket interactions",cases);test.succeed();
    }
    private static void reset(CrucibleBlockEntity tile,int heat,int water){var tag=new CompoundTag();tag.putShort("Heat",(short)heat);tag.putString("FluidName","water");tag.putInt("Amount",water);tile.load(tag);}
}
