package tc4revival.content;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

public  class GolemLiquidSources {
    private GolemLiquidSources() {}
    public static IFluidHandler at(ServerLevel level,BlockPos pos,Direction face) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return null;
        var tile=level.getBlockEntity(pos);
        return tile==null?null:tile.getCapability(ForgeCapabilities.FLUID_HANDLER,face).resolve().orElse(null);
    }
    public static List<Fluid> needed(GolemWorkHost host) {
        if (!(host.mob().level() instanceof ServerLevel level)) return List.of();
        var home=at(level,host.homeInventory(),host.homeFacing()); if (home==null) return List.of();
        var result=new ArrayList<Fluid>();
        for (var fluid:List.of(Fluids.WATER,Fluids.LAVA)) {
            if (!host.fluid().isEmpty() && host.fluid().getFluid()!=fluid) continue;
            var probe=new FluidStack(fluid,Integer.MAX_VALUE); boolean accepted=false;
            for (int tank=0;tank<home.getTanks();tank++) if (home.isFluidValid(tank,probe)) { accepted=true; break; }
            if (!accepted || home.fill(new FluidStack(fluid,1),IFluidHandler.FluidAction.SIMULATE)<=0) continue;
            if (!host.filters().isEmpty()) {
                boolean matches=false;
                for (int slot=0;slot<host.filters().size();slot++) {
                    var sample=FluidUtil.getFluidContained(host.filters().sample(slot));
                    if (sample.isPresent() && sample.get().isFluidEqual(probe)) { matches=true; break; }
                }
                if (!matches) continue;
            }
            result.add(fluid);
        }
        return result;
    }
    public static boolean connected(ServerLevel level,BlockPos pos,Fluid fluid) {
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !(level.getBlockState(pos).getBlock() instanceof LiquidBlock)) return false;
        return level.getFluidState(pos).getType().isSame(fluid);
    }
    public static boolean source(ServerLevel level,BlockPos pos,Fluid fluid) { return connected(level,pos,fluid) && level.getFluidState(pos).isSource(); }
    public static boolean takeSource(ServerLevel level,BlockPos pos,Fluid fluid) {
        // Level.removeBlock preserves the fluid state; pumping must replace the source with air.
        return source(level,pos,fluid) && level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
    }
    public static List<GolemMarker> handlers(GolemWorkHost host,Fluid fluid,boolean adjacent) {
        if (!(host.mob().level() instanceof ServerLevel level)) return List.of();
        var result=new ArrayList<GolemMarker>();
        for (var marker:host.markers().inDimension(level.dimension().location())) {
            if (adjacent && !GolemWorkRange.adjacent(host,marker.pos())) continue;
            var handler=at(level,marker.pos(),marker.face());
            if (handler!=null && !handler.drain(new FluidStack(fluid,1),IFluidHandler.FluidAction.SIMULATE).isEmpty()) result.add(marker);
        }
        return result;
    }
    public static List<BlockPos> adjacentBlocks(GolemWorkHost host) {
        if (!(host.mob().level() instanceof ServerLevel level)) return List.of();
        var result=new ArrayList<BlockPos>();
        for (var marker:host.markers().inDimension(level.dimension().location())) {
            if (!level.hasChunkAt(marker.pos()) || GolemInventoryViews.exists(level,marker.pos())
                    || !GolemWorkRange.adjacent(host,marker.pos())) continue;
            result.add(marker.pos());
        }
        return result;
    }
    public static BlockPos find(GolemWorkHost host,Fluid fluid) {
        if (!(host.mob().level() instanceof ServerLevel level)) return null;
        double range=host.configuration().range(),best=Double.MAX_VALUE; BlockPos target=null;
        for (var marker:handlers(host,fluid,false)) {
            double distance=host.mob().position().distanceToSqr(Vec3.atCenterOf(marker.pos()));
            if (distance<=range*range && distance<best) { best=distance; target=marker.pos(); }
        }
        if (target!=null) return target;
        for (var marker:host.markers().inDimension(level.dimension().location())) {
            if (!source(level,marker.pos(),fluid)) continue;
            double distance=host.mob().position().distanceToSqr(Vec3.atCenterOf(marker.pos()));
            if (distance<=range*range && distance<best) { best=distance; target=marker.pos(); }
        }
        return target;
    }
}
