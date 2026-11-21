package temodern.golem;

import java.util.function.BooleanSupplier;
import appeng.api.config.Actionable;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import tc4revival.content.*;
import temodern.essentia.EssentiaKey;

/** Keeps physical cargo on the golem whenever wireless power, capacity or range disappears. */
public final class WirelessWork extends Goal {
    private final GolemEntity golem;
    private int nextAttempt;
    private WirelessWork(GolemEntity golem){this.golem=golem;}
    public static void install(GolemEntity golem,GoalSelector work,GoalSelector targets){
        work.addGoal(0,new GolemAvoidCreeperGoal(golem));work.addGoal(1,new WirelessWork(golem));
        var core=golem.configuration().core();
        if(core==GolemCoreType.GATHER)work.addGoal(2,new GolemGatherGoal(golem));
        if(core==GolemCoreType.FILL){work.addGoal(2,new GolemHomePlaceGoal(golem));work.addGoal(3,new GolemHomeDropGoal(golem));}
        if(core==GolemCoreType.LIQUID)work.addGoal(2,new LiquidDelivery(golem));
        if(core==GolemCoreType.ESSENTIA){
            work.addGoal(2,new Conditional(new GolemEssentiaGatherGoal(golem),()->emptying(golem)));
            work.addGoal(2,new EssentiaDelivery(golem));
        }
        work.addGoal(5,new GolemOpenDoorGoal(golem));work.addGoal(6,new GolemReturnHomeGoal(golem));
        work.addGoal(7,new LookAtPlayerGoal(golem,Player.class,6));work.addGoal(8,new RandomLookAroundGoal(golem));
    }
    private static boolean emptying(GolemEntity golem){
        var level=golem.level();var home=golem.homeInventory();if(!level.hasChunkAt(home))return true;
        if(level.getBlockEntity(home) instanceof EssentiaTransport)return true;
        for(int y=1;y<=5;y++)if(level.hasChunkAt(home.above(y))&&level.getBlockEntity(home.above(y)) instanceof AlembicBlockEntity)return true;
        return false;
    }
    public static IWirelessAccessPoint access(GolemEntity golem){
        if(!(golem.level() instanceof ServerLevel level))return null;
        var backpack=ItemStack.of(WirelessBackpack.data(golem));if(!backpack.hasTag())return null;
        var tag=backpack.getTag();if(!tag.getString("dimension").equals(level.dimension().location().toString()))return null;
        var pos=BlockPos.of(tag.getLong("accessPoint"));if(!level.hasChunkAt(pos))return null;
        if(!(level.getBlockEntity(pos) instanceof IWirelessAccessPoint linked)||linked.getGrid()==null)return null;
        // All access points on the linked grid can supply coverage. Never load a chunk to find one.
        for(var gridNode:linked.getGrid().getNodes())if(gridNode.getOwner() instanceof IWirelessAccessPoint candidate&&candidate.isActive()
                &&candidate.getLocation().getLevel()==level&&golem.distanceToSqr(candidate.getLocation().getPos().getX()+.5,candidate.getLocation().getPos().getY()+.5,candidate.getLocation().getPos().getZ()+.5)<=candidate.getRange()*candidate.getRange())return candidate;
        return null;
    }
    @Override public boolean canUse(){
        if(golem.tickCount<nextAttempt)return false;nextAttempt=golem.tickCount+20;boolean connected=access(golem)!=null;
        var data=WirelessBackpack.data(golem);if(data.getBoolean("connected")!=connected){data.putBoolean("connected",connected);thaumcraft.api.golems.GolemApi.data(golem,WirelessBackpack.ID,data);}
        return golem.workEnabled()&&connected;
    }
    @Override public boolean canContinueToUse(){return false;}
    @Override public void start(){ transfer(golem); }
    public static void transfer(GolemEntity golem){
        var ap=access(golem);if(ap==null||ap.getGrid()==null)return;var grid=ap.getGrid();var source=IActionSource.ofMachine(ap);
        var core=golem.configuration().core();
        var rate=WirelessRates.of(golem.configuration().upgrades(4),golem.configuration().advanced());
        if(core==GolemCoreType.GATHER){
            var held=golem.carried();if(held.isEmpty())return;
            long inserted=StorageHelper.poweredInsert(grid.getEnergyService(),grid.getStorageService().getInventory(),AEItemKey.of(held),Math.min(held.getCount(),rate.items()),source);
            if(inserted>0){held.shrink((int)inserted);golem.carried(held);golem.action();}
        }else if(core==GolemCoreType.FILL&&golem.carried().isEmpty()){
            for(var wanted:golem.missingItems()){
                for(var entry:grid.getStorageService().getInventory().getAvailableStacks()) {
                    if(!(entry.getKey() instanceof AEItemKey key) || !GolemItemMatching.matches(key.toStack(),wanted,golem.configuration()))continue;
                    long taken=StorageHelper.poweredExtraction(grid.getEnergyService(),grid.getStorageService().getInventory(),key,Math.min(rate.items(),Math.min(wanted.getCount(),golem.configuration().carryLimit())),source);
                    if(taken>0){golem.carried(key.toStack((int)taken));golem.action();return;}
                }
            }
        }else if(core==GolemCoreType.LIQUID&&golem.fluid().getAmount()<1000){
            var needed=neededFluids(golem);if(needed.size()!=1)return;
            var fluid=needed.get(0);int request=Math.min(rate.fluid(),1000-golem.fluid().getAmount());
            var power=grid.getEnergyService();
            if(power.extractAEPower(request/100d,Actionable.SIMULATE,appeng.api.config.PowerMultiplier.CONFIG)+1e-6<request/100d)return;
            long taken=grid.getStorageService().getInventory().extract(AEFluidKey.of(fluid),request,Actionable.MODULATE,source);
            if(taken>0){power.extractAEPower(taken/100d,Actionable.MODULATE,appeng.api.config.PowerMultiplier.CONFIG);golem.fluid(new FluidStack(fluid,golem.fluid().getAmount()+(int)taken));golem.action();}
        }else if(core==GolemCoreType.ESSENTIA){
            if(emptying(golem)){
                if(golem.essentia()==null||golem.essentiaAmount()==0)return;
                long inserted=grid.getStorageService().getInventory().insert(EssentiaKey.of(golem.essentia()),Math.min(rate.essentia(),golem.essentiaAmount()),Actionable.MODULATE,source);
                if(inserted>0){int left=golem.essentiaAmount()-(int)inserted;golem.essentia(left==0?null:golem.essentia(),left);golem.action();}
            }else if(golem.essentiaAmount()==0){
                for(var target:essentiaTargets(golem)) {
                    var container=thaumcraft.api.aspects.EssentiaStorageApi.at((ServerLevel)golem.level(),target.getKey(),target.getValue());
                    for(var entry:grid.getStorageService().getInventory().getAvailableStacks()) {
                        if(!(entry.getKey() instanceof EssentiaKey key)||key.aspect()==null)continue;
                        int fit=container.insert(key.aspect(),rate.essentia(),true);
                        if(fit<=0)continue;
                        long taken=grid.getStorageService().getInventory().extract(key,fit,Actionable.MODULATE,source);
                        if(taken>0){golem.essentia(key.aspect(),(int)taken);golem.action();return;}
                    }
                }

            }
        }
    }
    private static java.util.List<java.util.Map.Entry<BlockPos,Direction>> essentiaTargets(GolemEntity golem){
                var targets=new java.util.LinkedHashMap<BlockPos,Direction>();
                var queue=new java.util.ArrayDeque<BlockPos>();
                for(var marker:golem.markers().inDimension(golem.level().dimension().location())){
                    if(!golem.level().hasChunkAt(marker.pos())||golem.home().distSqr(marker.pos())>golem.configuration().range()*golem.configuration().range())continue;
                    targets.put(marker.pos(),marker.face());
                    if(golem.level().getBlockEntity(marker.pos()) instanceof EssentiaJarBlockEntity)queue.add(marker.pos());
                }
                var seen=new java.util.HashSet<BlockPos>();
                while(!queue.isEmpty()&&seen.size()<4096){
                    var pos=queue.remove();if(!seen.add(pos)||golem.home().distSqr(pos)>golem.configuration().range()*golem.configuration().range()||!golem.level().hasChunkAt(pos)||!(golem.level().getBlockEntity(pos) instanceof EssentiaJarBlockEntity))continue;
                    targets.put(pos,Direction.UP);for(var side:Direction.values())queue.add(pos.relative(side));
                }
                var ordered=new java.util.ArrayList<>(targets.entrySet());
                ordered.sort(java.util.Comparator.<java.util.Map.Entry<BlockPos,Direction>>comparingInt(e->golem.level().getBlockEntity(e.getKey()) instanceof EssentiaJarBlockEntity jar&&jar.voidJar()?1:0)
                    .thenComparingDouble(e->golem.distanceToSqr(e.getKey().getCenter())));
        return ordered;
    }
    private static final class LiquidDelivery extends Goal {
        private final GolemEntity golem;
        LiquidDelivery(GolemEntity golem){this.golem=golem;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
        public boolean canUse(){return golem.workEnabled()&&!golem.fluid().isEmpty()&&golem.nearHomeAndIdle()&&neededFluids(golem).contains(golem.fluid().getFluid());}
        public boolean canContinueToUse(){return false;}
        public void start(){
            var handler=GolemLiquidSources.at((ServerLevel)golem.level(),golem.homeInventory(),golem.homeFacing());if(handler==null)return;
            var held=golem.fluid().copy();int accepted=handler.fill(held,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            held.shrink(accepted);golem.fluid(held);golem.watchingFluid(false);if(accepted>0)golem.action();
        }
    }
    private static final class EssentiaDelivery extends Goal {
        private final GolemEntity golem;private java.util.Map.Entry<BlockPos,Direction> target;private int remaining;
        EssentiaDelivery(GolemEntity golem){this.golem=golem;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
        public boolean canUse(){
            if(!golem.workEnabled()||emptying(golem)||golem.essentia()==null||golem.essentiaAmount()<=0)return false;
            for(var candidate:essentiaTargets(golem))if(thaumcraft.api.aspects.EssentiaStorageApi.at((ServerLevel)golem.level(),candidate.getKey(),candidate.getValue()).insert(golem.essentia(),golem.essentiaAmount(),true)>0){target=candidate;return true;}
            return false;
        }
        public boolean canContinueToUse(){return remaining>0&&golem.workEnabled()&&golem.essentiaAmount()>0;}
        public boolean requiresUpdateEveryTick(){return true;}
        public void start(){remaining=200;golem.getNavigation().moveTo(target.getKey().getX()+.5,target.getKey().getY(),target.getKey().getZ()+.5,golem.navigationSpeed());}
        public void tick(){
            if(--remaining<=0||golem.essentia()==null)return;
            if(golem.distanceToSqr(target.getKey().getCenter())>4)return;
            var storage=thaumcraft.api.aspects.EssentiaStorageApi.at((ServerLevel)golem.level(),target.getKey(),target.getValue());
            int accepted=storage.insert(golem.essentia(),golem.essentiaAmount(),false),left=golem.essentiaAmount()-accepted;
            golem.essentia(left==0?null:golem.essentia(),left);remaining=0;if(accepted>0)golem.action();
        }
        public void stop(){golem.getNavigation().stop();target=null;}
    }
    private static java.util.List<net.minecraft.world.level.material.Fluid> neededFluids(GolemEntity golem){
        var home=GolemLiquidSources.at((ServerLevel)golem.level(),golem.homeInventory(),golem.homeFacing());
        if(home==null)return java.util.List.of();var result=new java.util.ArrayList<net.minecraft.world.level.material.Fluid>();
        for(var fluid:net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValues()){
            if(fluid==net.minecraft.world.level.material.Fluids.EMPTY||fluid instanceof net.minecraft.world.level.material.FlowingFluid flowing&&flowing.getSource()!=fluid)continue;
            if(!golem.fluid().isEmpty()&&golem.fluid().getFluid()!=fluid)continue;
            var probe=new FluidStack(fluid,1000);boolean accepted=false;
            for(int tank=0;tank<home.getTanks();tank++)if(home.isFluidValid(tank,probe)){accepted=true;break;}
            if(!accepted||home.fill(probe,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)==0)continue;
            if(!golem.filters().isEmpty()){
                boolean matches=false;for(int slot=0;slot<golem.filters().size();slot++)if(net.minecraftforge.fluids.FluidUtil.getFluidContained(golem.filters().sample(slot)).filter(s->s.isFluidEqual(probe)).isPresent()){matches=true;break;}
                if(!matches)continue;
            }
            result.add(fluid);
        }
        return result;
    }
    private static final class Conditional extends Goal {
        private final Goal delegate;private final BooleanSupplier enabled;
        Conditional(Goal delegate,BooleanSupplier enabled){this.delegate=delegate;this.enabled=enabled;setFlags(delegate.getFlags());}
        @Override public boolean canUse(){return enabled.getAsBoolean()&&delegate.canUse();}
        @Override public boolean canContinueToUse(){return enabled.getAsBoolean()&&delegate.canContinueToUse();}
        @Override public boolean requiresUpdateEveryTick(){return delegate.requiresUpdateEveryTick();}
        @Override public boolean isInterruptable(){return delegate.isInterruptable();}
        @Override public void start(){delegate.start();}@Override public void tick(){delegate.tick();}@Override public void stop(){delegate.stop();}
    }
}
