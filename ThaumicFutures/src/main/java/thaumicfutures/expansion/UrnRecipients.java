package thaumicfutures.expansion;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.ModList;

/** Respect each exposed fluid input; some devices accept only sides or whole buckets. */
public final class UrnRecipients {
    private static final Direction[] FACES={Direction.UP,Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST,null};
    private UrnRecipients() {}
    public static int fill(BlockEntity target,int amount,boolean execute) {
        if(target==null||target.isRemoved()||target instanceof EverfullUrn.Tile||amount<=0)return 0;
        for(var side:FACES){
            var handler=target.getCapability(ForgeCapabilities.FLUID_HANDLER,side).resolve().orElse(null);
            if(handler==null)continue;
            int accepted=Math.max(0,Math.min(amount,handler.fill(new FluidStack(Fluids.WATER,amount),FluidAction.SIMULATE)));
            if(accepted<=0)continue;
            return execute?Math.max(0,Math.min(accepted,handler.fill(new FluidStack(Fluids.WATER,accepted),FluidAction.EXECUTE))):accepted;
        }
        return ModList.get().isLoaded("botania")?UrnBotania.fill(target,amount,execute):0;
    }
}
