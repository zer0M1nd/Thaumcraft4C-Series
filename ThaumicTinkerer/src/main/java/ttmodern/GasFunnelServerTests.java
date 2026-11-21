package ttmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;
import tc4revival.content.*;
import tc4revival.aspect.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class GasFunnelServerTests {
    @GameTest(template="wand_test_empty")
    public static void personalLightNeverInterceptsWorkstationPicking(GameTestHelper test) {
        var level=test.getLevel();var gasPos=test.absolutePos(new BlockPos(2,3,2));
        var target=gasPos.east(2);level.setBlockAndUpdate(target,Blocks.CRAFTING_TABLE.defaultBlockState());
        var player=test.makeMockPlayer();player.setPos(gasPos.getX()+.5,gasPos.getY()-1.12,gasPos.getZ()+.5);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Tinkerer.BRIGHT_NITOR.get()));
        try {
            for(var gas:java.util.List.of(Tinkerer.NITOR_GAS.get(),Tinkerer.LIGHT_GAS.get(),Tinkerer.SHADOW_GAS.get())) {
                level.setBlockAndUpdate(gasPos,gas.defaultBlockState());
                var state=level.getBlockState(gasPos);
                test.assertTrue(state.getShape(level,gasPos).isEmpty(),"Invisible gas has no selection box");
                for(var start:java.util.List.of(gasPos.getCenter(),gasPos.west().getCenter())) {
                    var hit=level.clip(new net.minecraft.world.level.ClipContext(start,target.getCenter(),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
                    test.assertTrue(hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&hit.getBlockPos().equals(target),"Ray from within or through gas reaches the actual workstation");
                    var result=level.getBlockState(target).use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                    test.assertTrue(result.consumesAction(),"Workstation still handles interaction while holding bright nitor");
                }
            }
        }finally{level.removeBlock(gasPos,false);level.removeBlock(target,false);player.closeContainer();}
        test.succeed();
    }
    @GameTest(template="wand_test_empty") public static void tinkererTurtleGasClearingBoundsAndProtection(GameTestHelper test){
        var world=test.getLevel();var origin=test.absolutePos(new net.minecraft.core.BlockPos(4,4,4));var low=origin.offset(-3,0,0);var high=origin.offset(2,0,0);var outside=origin.offset(3,0,0);var protectedPos=origin.above();var solid=origin.below();world.setBlockAndUpdate(low,Tinkerer.LIGHT_GAS.get().defaultBlockState());world.setBlockAndUpdate(high,Tinkerer.SHADOW_GAS.get().defaultBlockState());world.setBlockAndUpdate(outside,Tinkerer.LIGHT_GAS.get().defaultBlockState());world.setBlockAndUpdate(protectedPos,Tinkerer.NITOR_GAS.get().defaultBlockState());world.setBlockAndUpdate(solid,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var actor=net.minecraftforge.common.util.FakePlayerFactory.get(world,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"GasTurtleTest"));Object guard=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void GasFunnelServerTests_protect_1(net.minecraftforge.event.level.BlockEvent.BreakEvent event){if(event.getLevel()==world&&event.getPos().equals(protectedPos))event.setCanceled(true);}};net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(guard);
        try{GasClearing.clear(world,origin,actor);test.assertTrue(world.isEmptyBlock(low)&&world.isEmptyBlock(high),"Both inclusive legacy endpoints are cleared");test.assertTrue(world.getBlockState(outside).is(Tinkerer.LIGHT_GAS.get())&&world.getBlockState(protectedPos).is(Tinkerer.NITOR_GAS.get()),"Outside range and protected gas remain");test.assertTrue(world.getBlockState(solid).is(net.minecraft.world.level.block.Blocks.STONE),"Ordinary blocks never become turtle excavation targets");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(guard);for(var pos:java.util.List.of(low,high,outside,protectedPos,solid))world.removeBlock(pos,false);}test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererGasSpreadLightAndCleanup(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,4,2));
        level.setBlock(pos,Tinkerer.LIGHT_GAS.get().defaultBlockState().setValue(GasBlock.POWER,4),3);
        level.setBlock(pos.east(),Blocks.STONE.defaultBlockState(),3);
        Tinkerer.LIGHT_GAS.get().tick(level.getBlockState(pos),level,pos,level.random);
        test.assertTrue(level.getBlockState(pos.east()).is(Blocks.STONE),"Gas never overwrites solid blocks");
        test.assertTrue(level.getBlockState(pos.west()).is(Tinkerer.LIGHT_GAS.get())&&level.getBlockState(pos.west()).getValue(GasBlock.POWER)==3,"Gas propagates reduced distance");
        test.assertTrue(level.getBlockState(pos).getValue(GasBlock.POWER)==0&&level.getBlockState(pos).getLightEmission(level,pos)==12,"Source spent and light level preserved");
        var temp=pos.above(3);level.setBlock(temp,Tinkerer.NITOR_GAS.get().defaultBlockState(),3);Tinkerer.NITOR_GAS.get().tick(level.getBlockState(temp),level,temp,level.random);
        test.assertTrue(level.isEmptyBlock(temp),"Abandoned personal light removed");
        test.succeed();
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererFunnelMovesOneUnitAndHonorsFilter(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,4,2));var target=pos.below().east();
        level.setBlock(pos,Tinkerer.FUNNEL.get().defaultBlockState(),3);level.setBlock(pos.below(),Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.EAST),3);
        level.setBlock(target,TCArcane.ESSENTIA_JAR.get().defaultBlockState(),3);
        var funnel=(FunnelEntity)level.getBlockEntity(pos);var jar=(EssentiaJarBlockEntity)level.getBlockEntity(target);var air=TCAspects.catalog().require("aer");
        var source=new ItemStack(TCItems.FILLED_ESSENTIA_JAR.get());((StoredAspectItem)source.getItem()).setStoredAspects(source,new AspectAmounts().add(air,2));funnel.inventory.setStackInSlot(0,source);
        jar.label(TCAspects.catalog().require("ignis"),Direction.NORTH);funnel.tick();test.assertTrue(jar.revealedAspects().isEmpty(),"Mismatched filter is not bypassed");
        jar.removeLabel();funnel.tick();test.assertTrue(jar.revealedAspects().amount(air)==1&&((StoredAspectItem)source.getItem()).storedAspects(source).amount(air)==1,"One conserved unit per tick");
        test.assertTrue(funnel.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(IllegalStateException::new).extractItem(0,1,false).isEmpty(),"Underlying hopper cannot take the portable jar");
        funnel.tick();test.assertTrue(jar.revealedAspects().amount(air)==2&&funnel.inventory.getStackInSlot(0).is(TCItems.ESSENTIA_JAR.get()),"Emptied portable jar becomes reusable empty jar");
        for(String key:new String[]{"GASEOUS_LIGHT","GASEOUS_SHADOW","BRIGHT_NITOR","GAS_REMOVER","FUNNEL","REPAIRER"}) {
            test.assertTrue(tc4revival.research.ResearchCatalogs.catalog().find("thaumic_tinkerer:"+key).isPresent(),"Progression research exists: "+key);
        }
        test.assertTrue(level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("thaumic_tinkerer:restorer")).isPresent(),"Restorer infusion acquisition loaded");
        test.succeed();
    }
}
