package tbmodern.api;

import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

/** Experience is measured in points, never levels. Container implementations need no TB classes. */
public final class ExperienceFluids {
    private static final String CREDIT="thaumicbases:xpFluidRemainders";
    private static final TagKey<Fluid> EXPERIENCE=TagKey.create(Registries.FLUID,new ResourceLocation("forge","experience"));
    private static final Map<ResourceLocation,Integer> OVERRIDES=new HashMap<>();

    /** An integration may override the standard Forge experience-fluid ratio (20 mB/point). */
    public static void register(ResourceLocation fluid,int millibucketsPerPoint) {
        if(millibucketsPerPoint<=0)throw new IllegalArgumentException("Experience ratio must be positive");
        OVERRIDES.put(Objects.requireNonNull(fluid),millibucketsPerPoint);
    }
    public static int millibucketsPerPoint(FluidStack fluid) {
        if(fluid.isEmpty())return 0;
        Integer override=OVERRIDES.get(ForgeRegistries.FLUIDS.getKey(fluid.getFluid()));
        return override!=null?override:fluid.getFluid().is(EXPERIENCE)?20:0;
    }
    public static int extract(BlockEntity tile,int maximum) {
        int result=0;
        Set<IFluidHandler> visited=Collections.newSetFromMap(new IdentityHashMap<>());
        // Unsided access covers EIO; sided access also supports ordinary directional tanks.
        for(int side=-1;side<Direction.values().length && result<maximum;side++) {
            var capability=side<0?tile.getCapability(ForgeCapabilities.FLUID_HANDLER):tile.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.values()[side]);
            var handler=capability.orElse(null);
            if(handler!=null && visited.add(handler))result+=extract(handler,maximum-result,tile);
        }
        return result;
    }
    public static int extract(IFluidHandler handler,int maximum,BlockEntity owner) {
        if(maximum<=0)return 0;
        int total=0;
        var credits=owner.getPersistentData().getCompound(CREDIT);
        for(int tank=0;tank<handler.getTanks() && total<maximum;tank++) {
            // Several Forge tanks expose their live stack; drain may mutate it to EMPTY.
            var fluid=handler.getFluidInTank(tank).copy();int ratio=millibucketsPerPoint(fluid);
            if(ratio==0)continue;
            String id=Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(fluid.getFluid())).toString();
            int credit=Math.max(0,credits.getInt(id))%ratio;
            int budget=(int)Math.min(Integer.MAX_VALUE,(long)(maximum-total)*ratio-credit);
            var request=fluid.copy();request.setAmount(Math.min(budget,fluid.getAmount()));
            var simulated=handler.drain(request,IFluidHandler.FluidAction.SIMULATE);
            if(simulated.isEmpty() || !simulated.isFluidEqual(fluid))continue;
            // Leave ordinary sub-point fluid in the tank. Only retain a credit if an unusual
            // handler executes less than its simulation, so paid fluid is never counted twice.
            int points=(int)Math.min(maximum-total,((long)Math.min(simulated.getAmount(),request.getAmount())+credit)/ratio);
            if(points==0)continue;
            request.setAmount(points*ratio-credit);
            var drained=handler.drain(request,IFluidHandler.FluidAction.EXECUTE);
            if(drained.isEmpty() || !drained.isFluidEqual(fluid))continue;
            long paid=(long)credit+drained.getAmount();
            total+=(int)Math.min(maximum-total,paid/ratio);
            if(paid%ratio==0)credits.remove(id);else credits.putInt(id,(int)(paid%ratio));
            owner.getPersistentData().put(CREDIT,credits);owner.setChanged();
        }
        return total;
    }
    private ExperienceFluids() {}
}
