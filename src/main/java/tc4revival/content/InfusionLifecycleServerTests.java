package tc4revival.content;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class InfusionLifecycleServerTests {
    @GameTest(template="wand_test_empty")
    public static void matrixStructureLossPausesAndWandRestoresSavedTransaction(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(3,6,3));
        for(var at:BlockPos.betweenClosed(pos.offset(-5,-3,-5),pos.offset(5,1,5)))level.setBlock(at,Blocks.AIR.defaultBlockState(),18);
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}) {
            level.setBlock(pos.offset(x,-2,z),TCArcane.INFUSION_PILLAR.get().defaultBlockState(),18);
            level.setBlock(pos.offset(x,-1,z),TCArcane.INFUSION_PILLAR_TOP.get().defaultBlockState(),18);
        }
        level.setBlock(pos.below(2),TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);
        level.setBlock(pos.offset(3,-2,0),TCArcane.ARCANE_PEDESTAL.get().defaultBlockState(),18);
        level.setBlock(pos.offset(4,-2,0),TCArcane.ESSENTIA_JAR.get().defaultBlockState(),18);
        level.setBlock(pos,TCArcane.INFUSION_MATRIX.get().defaultBlockState(),18);
        var matrix=(InfusionMatrixBlockEntity)level.getBlockEntity(pos);var center=(ArcanePedestalBlockEntity)level.getBlockEntity(pos.below(2));
        var component=(ArcanePedestalBlockEntity)level.getBlockEntity(pos.offset(3,-2,0));var jar=(EssentiaJarBlockEntity)level.getBlockEntity(pos.offset(4,-2,0));
        var air=TCAspects.catalog().require("aer");center.setItem(0,new ItemStack(Items.IRON_INGOT));component.setItem(0,new ItemStack(Items.DIAMOND));jar.addToContainer(air,4);
        var process=new InfusionProcess();var ingredient=new InfusionIngredient(com.google.gson.JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:diamond\"}}").getAsJsonObject());
        process.start(new InfusionRecipe.Plan(List.of(ingredient),new AspectAmounts().add(air,4),0,0,false,InfusionOutput.item(new ItemStack(Items.GOLD_INGOT))),center.getItem(0),"lifecycle",-1000);
        var saved=new net.minecraft.nbt.CompoundTag();saved.putBoolean("Active",true);saved.put("Process",process.save());matrix.load(saved);EssentiaSources.refresh(matrix);
        for(int tick=0;tick<20;tick++)matrix.tick();
        test.assertTrue(jar.amount()==2&&matrix.crafting()&&matrix.active(),"Real matrix pays two units before structural loss");
        var lower=pos.offset(-1,-2,-1);level.setBlock(lower,Blocks.AIR.defaultBlockState(),18);
        for(int tick=0;tick<20;tick++)matrix.tick();
        test.assertTrue(!matrix.active()&&matrix.crafting()&&jar.amount()==1&&component.getItem(0).is(Items.DIAMOND),"Old periodic structure check permits tick thirty then pauses at forty without cancelling transaction");
        saved=matrix.saveWithoutMetadata();var replacement=new InfusionMatrixBlockEntity(pos,matrix.getBlockState());replacement.load(saved);level.removeBlockEntity(pos);level.setBlockEntity(replacement);matrix=replacement;
        level.setBlock(lower,TCArcane.INFUSION_PILLAR.get().defaultBlockState(),18);
        for(int tick=0;tick<100;tick++)matrix.tick();
        test.assertTrue(matrix.validLocation()&&!matrix.active()&&matrix.crafting()&&jar.amount()==1,"Repair and reload alone do not automatically reactivate old suspended craft");
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"InfusionLifecycle"));player.moveTo(pos.getCenter());
        var wand=new ItemStack(TCItems.CASTING_WAND.get());player.setItemInHand(InteractionHand.MAIN_HAND,wand);
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
        test.assertTrue(matrix.onWandUseFirst(wand,context)==InteractionResult.SUCCESS&&matrix.active(),"Wand reactivates valid suspended matrix without replacing recipe");
        for(int tick=0;tick<200&&matrix.crafting();tick++)matrix.tick();
        test.assertTrue(!matrix.crafting()&&jar.amount()==0&&component.isEmpty()&&center.getItem(0).is(Items.GOLD_INGOT),"Restored matrix spends only remaining costs and publishes saved ordinary output");
        // A broken upper pillar also invalidates the modern complete altar structure.
        level.setBlock(pos.offset(1,-1,1),Blocks.AIR.defaultBlockState(),18);
        test.assertTrue(!matrix.validLocation(),"Missing upper pillar prevents an incomplete altar from being used");
        tc4revival.TC4Revival.LOGGER.info("PASS matrixStructureLossPausesAndWandRestoresSavedTransaction");test.succeed();
    }
}
