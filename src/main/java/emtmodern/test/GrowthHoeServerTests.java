package emtmodern.test;

import emtmodern.ElectroMagicTools;
import ic2.api.item.ElectricItem;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("emt") @PrefixGameTestTemplate(false)
public class GrowthHoeServerTests {
    public static UseOnContext context(net.minecraft.world.entity.player.Player player,BlockPos pos){return new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    @GameTest(templateNamespace="emt",template="test_empty")
    public static void industrialBridgeAndGrowthHoeExactEnergyBranches(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,3,3));var player=GogglesServerTests.player(test);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(Vec3.atCenterOf(pos.west(2)));
        var bridge=new ItemStack(ElectroMagicTools.CONTENT.get("industrial_electric_hoe").get());player.setItemInHand(InteractionHand.MAIN_HAND,bridge);level.setBlockAndUpdate(pos,Blocks.MYCELIUM.defaultBlockState());level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());ElectricItem.manager.charge(bridge,50,Integer.MAX_VALUE,true,false);
        test.assertTrue(bridge.useOn(context(player,pos)).consumesAction()&&level.getBlockState(pos).is(Blocks.FARMLAND)&&ElectricItem.manager.getCharge(bridge)==0,"Restored IC2 hoe tills one mycelium for 50 EU");
        var hoe=new ItemStack(ElectroMagicTools.CONTENT.get("electric_growth_hoe").get());player.setItemInHand(InteractionHand.MAIN_HAND,hoe);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){level.setBlockAndUpdate(pos.offset(x,0,z),Blocks.DIRT.defaultBlockState());level.setBlockAndUpdate(pos.offset(x,1,z),Blocks.AIR.defaultBlockState());}
        ElectricItem.manager.charge(hoe,50,Integer.MAX_VALUE,true,false);test.assertTrue(hoe.useOn(context(player,pos)).consumesAction(),"Partial charged area till succeeds");int farmland=0;for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(level.getBlockState(pos.offset(x,0,z)).is(Blocks.FARMLAND))farmland++;
        test.assertTrue(farmland==2&&ElectricItem.manager.getCharge(hoe)==0,"3x3 stops after exactly two 25 EU cells");
        level.setBlockAndUpdate(pos,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7));level.setBlockAndUpdate(pos.above(),Blocks.WHEAT.defaultBlockState());ElectricItem.manager.charge(hoe,250,Integer.MAX_VALUE,true,false);
        test.assertTrue(hoe.useOn(context(player,pos.above())).consumesAction()&&level.getBlockState(pos.above()).getValue(CropBlock.AGE)>0&&ElectricItem.manager.getCharge(hoe)==0,"One real bonemeal growth spends exactly 250 EU");
        level.setBlockAndUpdate(pos,Blocks.DIRT.defaultBlockState());level.setBlock(pos.above(),tc4revival.content.TCArcane.SILVERWOOD_SAPLING.get().defaultBlockState(),2);level.setBlock(pos.above(2),Blocks.BEDROCK.defaultBlockState(),2);
        ElectricItem.manager.charge(hoe,249,Integer.MAX_VALUE,true,false);test.assertTrue(!hoe.useOn(context(player,pos.above())).consumesAction(),"Magical sapling needs 250 EU available");ElectricItem.manager.charge(hoe,1,Integer.MAX_VALUE,true,false);
        test.assertTrue(hoe.useOn(context(player,pos.above())).consumesAction()&&ElectricItem.manager.getCharge(hoe)==250&&level.getBlockState(pos.above()).is(tc4revival.content.TCArcane.SILVERWOOD_SAPLING.get()),"Original magical tree branch retains EU and restores obstructed sapling");
        test.assertTrue(ElectricItem.manager.discharge(hoe,100,1,true,true,true)==100,"Growth hoe retains original external power supply flag");
        var recipe=level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("emt","electric_growth_hoe")).orElseThrow();test.assertTrue(recipe.getResultItem(level.registryAccess()).is(hoe.getItem()),"Original infusion chain loads with local industrial electric hoe bridge");test.succeed();
    }
}
