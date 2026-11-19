package automagy.compat;

import java.lang.reflect.Method;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Optional bridge to Botania's public PetalApothecary API; no third-party classes are bundled. */
public final class BotaniaApothecary {
    private record Access(Class<?> type,Method get,Method set,Object empty,Object water) {}
    private static final class Lazy {
        static final Access API=load();
        static Access load(){
            if(!net.minecraftforge.fml.ModList.get().isLoaded("botania"))return null;
            try{
                var type=Class.forName("vazkii.botania.api.block.PetalApothecary");
                var get=type.getMethod("getFluid");var state=get.getReturnType();
                Object empty=null,water=null;
                for(Object value:state.getEnumConstants()){
                    if(((Enum<?>)value).name().equals("EMPTY"))empty=value;
                    if(((Enum<?>)value).name().equals("WATER"))water=value;
                }
                if(empty==null||water==null)throw new ReflectiveOperationException("Missing apothecary states");
                return new Access(type,get,type.getMethod("setFluid",state),empty,water);
            }catch(ReflectiveOperationException|LinkageError failure){
                com.mojang.logging.LogUtils.getLogger().warn("Automagy: Botania apothecary API unavailable",failure);return null;
            }
        }
    }
    public static IFluidHandler of(BlockEntity tile){
        var api=Lazy.API;
        if(api==null||!api.type().isInstance(tile))return null;
        return new IFluidHandler(){
            private boolean failed;
            public int getTanks(){return 0;}
            public FluidStack getFluidInTank(int tank){return FluidStack.EMPTY;}
            public int getTankCapacity(int tank){return 0;}
            public boolean isFluidValid(int tank,FluidStack fluid){return false;}
            public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.EMPTY;}
            public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
            public int fill(FluidStack fluid,FluidAction action){
                if(failed||fluid.isEmpty()||tile.isRemoved()||tile.getLevel()==null||tile.getLevel().isClientSide)return 0;
                try{
                    if(api.get().invoke(tile)!=api.empty())return 0;
                    // Legacy Automagy fills a bowl using the current siphon share, not a full bucket.
                    if(action.execute())api.set().invoke(tile,api.water());
                    return fluid.getAmount();
                }catch(ReflectiveOperationException failure){
                    failed=true;com.mojang.logging.LogUtils.getLogger().warn("Automagy: apothecary fill failed at {}",tile.getBlockPos(),failure);return 0;
                }
            }
        };
    }
    private BotaniaApothecary() {}
}
