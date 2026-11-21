package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class CopperProcessingServerTests {
    @GameTest(template="wand_test_empty")
    public static void allCopperStagesKeepWaxAndShapeAndUseExactAlchemyCosts(GameTestHelper test) {
        int shapes=0,recipes=0,shock=0;
        for(var pair:HoneycombItem.WAXABLES.get().entrySet()) {
            var bare=pair.getKey();var waxed=pair.getValue();
            if(!net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(bare).getNamespace().equals("minecraft"))continue;
            shapes++;
            for(var block:java.util.List.of(bare,waxed)) {
                var state=block.defaultBlockState();
                if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST);
                if(state.hasProperty(BlockStateProperties.WATERLOGGED))state=state.setValue(BlockStateProperties.WATERLOGGED,true);
                var actual=CopperProcessing.deoxidized(state);var previous=WeatheringCopper.getPrevious(bare);
                test.assertTrue(actual.isPresent()==previous.isPresent(),"Only oxidized copper has a previous stage");
                if(previous.isPresent()) {
                    var expected=block==bare?previous.get():HoneycombItem.WAXABLES.get().get(previous.get());
                    test.assertTrue(actual.get().equals(expected.withPropertiesOf(state)),"Shock keeps wax, orientation and waterlogging");shock++;
                }
            }
            var deWax=CrucibleRecipe.find(test.getLevel(),new CrucibleRecipe.Input(new ItemStack(waxed),new AspectAmounts().add(TCAspects.catalog().require("aqua"),1),k->k.equals("ENTROPICPROCESSING"))).orElseThrow();
            test.assertTrue(deWax.getResultItem(test.getLevel().registryAccess()).is(bare.asItem())&&deWax.costs().total()==1,"One aqua removes coating at every oxidation stage");recipes++;
            var next=WeatheringCopper.getNext(bare);
            if(next.isPresent())for(var block:java.util.List.of(bare,waxed)) {
                var input=new CrucibleRecipe.Input(new ItemStack(block),new AspectAmounts().add(TCAspects.catalog().require("perditio"),1),k->k.equals("ENTROPICPROCESSING"));
                var recipe=CrucibleRecipe.find(test.getLevel(),input).orElseThrow();
                var expected=block==bare?next.get():HoneycombItem.WAXABLES.get().get(next.get());
                test.assertTrue(recipe.getResultItem(test.getLevel().registryAccess()).is(expected.asItem())&&recipe.costs().total()==1,"One perditio advances exactly one stage, preserving wax");
                test.assertTrue(!recipe.matches(new CrucibleRecipe.Input(new ItemStack(block),input.aspects(),k->false),test.getLevel()),"Knowledge is required");recipes++;
            }
        }
        test.assertTrue(shapes==16&&recipes==40&&shock==24,"Every vanilla copper shape and stage covered");test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void shockBeamDeoxidizesTheActuallyTargetedCopperAndPaysVis(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));
        level.setBlockAndUpdate(pos,Blocks.WAXED_OXIDIZED_CUT_COPPER.defaultBlockState());
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CopperShock"));
        player.setPos(pos.getX()+.5,pos.getY()-1.12,pos.getZ()-3);player.setYRot(0);player.setXRot(0);
        var wand=new ItemStack(TCItems.CASTING_WAND.get());wand.getOrCreateTag().putString("rod","silverwood");wand.getOrCreateTag().putString("cap","iron");wand.getOrCreateTag().putInt("aer",1000);
        var focus=new ItemStack(TCItems.SHOCK_FOCUS.get());wand.getOrCreateTag().put("focus",focus.save(new net.minecraft.nbt.CompoundTag()));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,wand);
        ((ShockFocusItem)focus.getItem()).usingTick(wand,player,100);
        test.assertTrue(level.getBlockState(pos).is(Blocks.WAXED_WEATHERED_CUT_COPPER)&&wand.getTag().getInt("aer")<1000,"Real beam target advances one stage and charges the wand");
        test.assertTrue(!CopperProcessing.shock(player,wand,new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false)),"Non-copper is unchanged");test.succeed();
    }
}
