package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
import tc4revival.aspect.item.ItemStackAspects;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public  class CrucibleDissolutionServerTests {
    @GameTest(template="wand_test_empty")
    public static void crucibleCollisionAndFluidCapabilityPreserveLegacyBoundaries(GameTestHelper test) {
        var level=test.getLevel();var pos=test.absolutePos(new BlockPos(2,2,2));
        level.setBlock(pos,TCArcane.CRUCIBLE.get().defaultBlockState(),2);
        var tile=(CrucibleBlockEntity)level.getBlockEntity(pos);var state=tile.getBlockState();
        var base=ItemStackAspects.resolve(new ItemStack(Items.DIRT)).orElseThrow();
        test.assertTrue(!base.isEmpty(),"Old ordinary material has dissolvable aspects");
        try {
            for(int amount:new int[]{1,2,3,4,63,64}) for(int heat:new int[]{150,151}) for(int water:new int[]{0,1}) {
                reset(tile,heat,water);var input=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(Items.DIRT,amount));
                state.getBlock().entityInside(state,level,pos,input);
                int consumed=heat==151&&water==1?amount:0;
                test.assertTrue(input.getItem().getCount()==amount-consumed && input.isRemoved()==(consumed==amount),"Collision consumes every eligible item exactly once");
                test.assertTrue(tile.contents().water()==water,"Dissolving items does not charge recipe water");
                for(var entry:base.view().entrySet())test.assertTrue(tile.contents().aspects().amount(entry.getKey())==entry.getValue()*consumed,"Whole per-item aspects are conserved");
                input.discard();
            }
            reset(tile,151,1000);
            var unsupported=AspectFreeTestItems.create();unsupported.setCount(5);
            var unknown=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,unsupported);
            test.assertTrue(ItemStackAspects.resolve(unknown.getItem()).orElseGet(tc4revival.aspect.AspectAmounts::new).isEmpty(),"Runtime fixture has no dissolvable aspects");
            state.getBlock().entityInside(state,level,pos,unknown);
            test.assertTrue(unknown.getItem().getCount()==5 && !unknown.isRemoved() && unknown.getDeltaMovement().y==.35 && tile.contents().aspects().isEmpty(),"Unsupported material is ejected intact");unknown.discard();
            reset(tile,0,0);
            for(var side:Direction.values())test.assertTrue(tile.getCapability(ForgeCapabilities.FLUID_HANDLER,side).isPresent(),"Fluid capability exposed on all six faces");
            var capability=tile.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new IllegalStateException("Missing capability"));
            test.assertTrue(capability.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE)==0,"Reject nonwater");
            test.assertTrue(capability.fill(new FluidStack(Fluids.WATER,2000),FluidAction.SIMULATE)==1000 && tile.contents().water()==0,"Fill simulation does not mutate");
            test.assertTrue(capability.fill(new FluidStack(Fluids.WATER,2000),FluidAction.EXECUTE)==1000,"Capacity caps real fill");
            capability.getFluidInTank(0).setAmount(1);test.assertTrue(tile.contents().water()==1000,"Returned fluid is detached");
            test.assertTrue(capability.drain(600,FluidAction.SIMULATE).getAmount()==600 && tile.contents().water()==1000,"Drain simulation");
            test.assertTrue(capability.drain(new FluidStack(Fluids.LAVA,100),FluidAction.EXECUTE).isEmpty(),"Wrong fluid drain rejected");
            test.assertTrue(capability.drain(600,FluidAction.EXECUTE).getAmount()==600 && tile.contents().water()==400,"Actual drain shares contents");
            var handle=tile.getCapability(ForgeCapabilities.FLUID_HANDLER);tile.invalidateCaps();test.assertTrue(!handle.isPresent(),"Old handle invalidated");tile.reviveCaps();
            test.assertTrue(tile.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new IllegalStateException("Missing revived capability")).getFluidInTank(0).getAmount()==400,"Revived capability retains contents");
        } finally {reset(tile,0,0);level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);}
        tc4revival.TC4Revival.LOGGER.info("PASS crucibleCollisionAndFluidCapabilityPreserveLegacyBoundaries: 24 collision cases, unsupported ejection and actual fluid capability simulation/lifecycle");test.succeed();
    }
    private static void reset(CrucibleBlockEntity tile,int heat,int water) {
        var tag=new CompoundTag();tag.putShort("Heat",(short)heat);tag.putString("FluidName","water");tag.putInt("Amount",water);tile.load(tag);
    }
}
