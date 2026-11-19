package ttmodern;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.*;
import net.minecraftforge.items.*;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class InterfaceServerTests {
    private static final class Fixture extends FurnaceBlockEntity {
        final EnergyStorage energy=new EnergyStorage(1000);final FluidTank fluid=new FluidTank(1000);final ItemStackHandler items=new ItemStackHandler(1);
        final LazyOptional<net.minecraftforge.energy.IEnergyStorage> energyCap=LazyOptional.of(()->energy);
        final LazyOptional<IFluidHandler> fluidCap=LazyOptional.of(()->fluid);
        final LazyOptional<IItemHandler> itemCap=LazyOptional.of(()->items);
        Fixture(BlockPos pos,BlockState state){super(pos,state);}
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){
            if(side!=Direction.NORTH)return LazyOptional.empty();
            if(cap==ForgeCapabilities.ENERGY)return energyCap.cast();if(cap==ForgeCapabilities.FLUID_HANDLER)return fluidCap.cast();if(cap==ForgeCapabilities.ITEM_HANDLER)return itemCap.cast();return super.getCapability(cap,side);
        }
        @Override public void invalidateCaps(){super.invalidateCaps();energyCap.invalidate();fluidCap.invalidate();itemCap.invalidate();}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererInterfaceForwardsFacesAndInvalidatesCachedTargets(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(1,1,1));var target=pos.offset(4,0,0);
        level.setBlock(pos,Tinkerer.INTERFACE.get().defaultBlockState(),3);level.setBlock(target,Blocks.FURNACE.defaultBlockState(),3);
        var source=(InterfaceEntity)level.getBlockEntity(pos);var fixture=new Fixture(target,level.getBlockState(target));level.setBlockEntity(fixture);
        try {
            test.assertTrue(source.link(target),"Four block boundary allowed");test.assertTrue(!source.link(pos)&&!source.link(pos.offset(5,0,0)),"Self/outside range rejected");
            var items=source.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.NORTH).orElseThrow(AssertionError::new);
            test.assertTrue(items.insertItem(0,new ItemStack(Items.DIAMOND,3),true).isEmpty()&&fixture.items.getStackInSlot(0).isEmpty(),"Simulation preserves inventory");
            items.insertItem(0,new ItemStack(Items.DIAMOND,3),false);
            test.assertTrue(fixture.items.getStackInSlot(0).getCount()==3&&items.extractItem(0,1,false).getCount()==1,"Actual same-face transfer");
            var denied=source.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.SOUTH).orElseThrow(AssertionError::new);
            test.assertTrue(denied.getSlots()==0&&!denied.insertItem(0,new ItemStack(Items.GOLD_INGOT),false).isEmpty(),"Opposite face not substituted");
            var energy=source.getCapability(ForgeCapabilities.ENERGY,Direction.NORTH).orElseThrow(AssertionError::new);
            test.assertTrue(energy.receiveEnergy(250,true)==250&&fixture.energy.getEnergyStored()==0,"Energy simulation");energy.receiveEnergy(250,false);
            test.assertTrue(energy.extractEnergy(40,false)==40&&fixture.energy.getEnergyStored()==210,"Exact energy conservation");
            var fluid=source.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.NORTH).orElseThrow(AssertionError::new);
            test.assertTrue(fluid.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,500),IFluidHandler.FluidAction.SIMULATE)==500&&fixture.fluid.isEmpty(),"Fluid simulation");
            fluid.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,500),IFluidHandler.FluidAction.EXECUTE);
            test.assertTrue(fluid.drain(125,IFluidHandler.FluidAction.EXECUTE).getAmount()==125&&fixture.fluid.getFluidAmount()==375,"Exact fluid conservation");
            source.camouflage(Blocks.OAK_LOG.defaultBlockState());var saved=source.saveWithoutMetadata();var restored=new InterfaceEntity(pos,source.getBlockState());restored.load(saved);
            test.assertTrue(restored.targetPosition().equals(target)&&restored.camouflage().is(Blocks.OAK_LOG),"Binding and camouflage persist");
            level.removeBlock(target,false);
            test.assertTrue(items.getSlots()==0&&items.extractItem(0,1,false).isEmpty()&&energy.extractEnergy(1,false)==0&&fluid.drain(1,IFluidHandler.FluidAction.EXECUTE).isEmpty(),"Previously cached handles cannot access removed target");
            level.setBlock(target,Blocks.FURNACE.defaultBlockState(),3);var replacement=new Fixture(target,level.getBlockState(target));level.setBlockEntity(replacement);
            items.insertItem(0,new ItemStack(Items.EMERALD),false);
            test.assertTrue(replacement.items.getStackInSlot(0).is(Items.EMERALD)&&fixture.items.getStackInSlot(0).getCount()==2,"Cached proxy follows replacement, never old inventory");
            var optional=source.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.NORTH);level.removeBlock(pos,false);
            test.assertTrue(!optional.isPresent()&&items.getSlots()==0,"Source removal invalidates exported handle");test.succeed();
        }finally{level.removeBlock(pos,false);level.removeBlock(target,false);}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererInterfaceForwardsVanillaAndEssentia(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(1,1,1));var target=pos.offset(3,0,0);
        level.setBlock(pos,Tinkerer.INTERFACE.get().defaultBlockState(),3);level.setBlock(target,Blocks.FURNACE.defaultBlockState(),3);var source=(InterfaceEntity)level.getBlockEntity(pos);
        try {
            source.link(target);source.setItem(0,new ItemStack(Items.IRON_ORE,2));
            test.assertTrue(((FurnaceBlockEntity)level.getBlockEntity(target)).getItem(0).getCount()==2,"Vanilla container writes reach target");
            test.assertTrue(java.util.Arrays.equals(source.getSlotsForFace(Direction.UP),new int[]{0}),"Sided furnace slots preserved");
            test.assertTrue(source.removeItem(0,1).getCount()==1&&source.getItem(0).getCount()==1,"Vanilla extraction debits target");
            level.setBlock(target,tc4revival.content.TCArcane.ESSENTIA_JAR.get().defaultBlockState(),3);
            var jar=(tc4revival.content.EssentiaJarBlockEntity)level.getBlockEntity(target);var air=tc4revival.aspect.TCAspects.catalog().require("aer");jar.addToContainer(air,10);
            test.assertTrue(source.contains(air,10)&&source.takeFromContainer(air,2)&&jar.amount()==8,"Remote essentia reservoir conservation");
            test.assertTrue(source.connectable(Direction.UP)==jar.connectable(Direction.UP)&&source.suction(Direction.UP)==jar.suction(Direction.UP),"Transport properties delegate");
            test.assertTrue(source.takeEssentia(air,1,Direction.UP)==1&&jar.amount()==7&&source.revealedAspects().amount(air)==7,"Pipe and goggles expose actual contents");
            level.setBlock(target,Tinkerer.INTERFACE.get().defaultBlockState(),3);
            test.assertTrue(!source.link(target)&&source.getContainerSize()==0&&source.amount()==0,"Interfaces cannot proxy one another even after replacement");test.succeed();
        }finally{level.removeBlock(pos,false);level.removeBlock(target,false);}
    }
    @GameTest(template="wand_test_empty")
    public static void tinkererConnectorBindsAndKeepsDimensionBoundary(GameTestHelper test){
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(1,1,1));var target=pos.offset(4,0,0);
        level.setBlock(pos,Tinkerer.INTERFACE.get().defaultBlockState(),3);level.setBlock(target,Blocks.CHEST.defaultBlockState(),3);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"InterfaceLink"));
        var stack=new ItemStack(Tinkerer.CONNECTOR.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        java.util.function.Consumer<BlockPos> use=at->Tinkerer.CONNECTOR.get().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(at.getCenter(),Direction.UP,at,false)));
        try {
            use.accept(pos);test.assertTrue(stack.getOrCreateTag().contains("Source"),"First click selects interface");
            use.accept(pos.offset(5,0,0));test.assertTrue(stack.getOrCreateTag().contains("Source"),"Failed range check preserves selection");
            use.accept(target);var source=(InterfaceEntity)level.getBlockEntity(pos);
            test.assertTrue(target.equals(source.targetPosition())&&!stack.getOrCreateTag().contains("Source"),"Second click binds and clears tool");
            use.accept(pos);stack.getOrCreateTag().putString("Dimension","minecraft:the_nether");use.accept(target);
            test.assertTrue(!stack.getOrCreateTag().contains("Source")&&target.equals(source.targetPosition()),"Cross-dimension selection rejected without changing old link");
            var pages=tc4revival.research.ResearchDefinitions.server().pages().require("thaumic_tinkerer:INTERFACE");
            test.assertTrue(pages.size()==5,"All five reference pages loaded");
            for(String id:java.util.List.of("transvector_interface","connector"))test.assertTrue(level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":"+id)).isPresent(),"Arcane recipe loaded: "+id);
            test.succeed();
        }finally{level.removeBlock(pos,false);level.removeBlock(target,false);}
    }

}
