package tc4revival.content;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandler;
import thaumcraft.api.golems.*;

/** Container-only work while any bound frame is mounted. Targets are re-resolved before each commit. */
public class GolemMobileContainerGoal extends Goal {
    private final GolemEntity golem;
    private long nextAction;
    private record Endpoint(GolemAnchor anchor,byte color,GolemLocations.Target target){}
    public GolemMobileContainerGoal(GolemEntity golem){this.golem=golem;setFlags(EnumSet.allOf(Flag.class));}
    @Override public boolean canUse(){return golem.mobileBindings()&&golem.state().workEnabled();}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void stop(){golem.getNavigation().stop();}
    private Endpoint home(ServerLevel level){var anchor=golem.state().homeAnchor();var target=GolemLocations.resolve(level,anchor);return target==null?null:new Endpoint(anchor,(byte)-1,target);}
    private List<Endpoint> marked(ServerLevel level){
        var result=new ArrayList<Endpoint>();
        for(var marker:golem.markers().inDimension(level.dimension().location())){
            var target=GolemLocations.resolve(level,marker.anchor());
            if(target!=null&&target.center().distanceToSqr(golem.position())<=Math.pow(golem.configuration().range(),2)
                &&!marker.anchor().id().equals(golem.state().homeAnchor().id()))result.add(new Endpoint(marker.anchor(),marker.color(),target));
        }
        result.sort(Comparator.comparingDouble(e->e.target.center().distanceToSqr(golem.position())));return result;
    }
    @Override public void tick(){
        if(!(golem.level() instanceof ServerLevel level)||level.getGameTime()<nextAction)return;
        var home=home(level);if(home==null){golem.getNavigation().stop();return;}
        var marked=marked(level);var core=golem.configuration().core();if(core==null)return;
        switch(core){
            case FILL,EMPTY,SORTING->items(level,home,marked,core);
            case LIQUID->liquids(level,home,marked);
            case ESSENTIA->essentia(level,home,marked);
            default->golem.getNavigation().stop();
        }
    }
    private boolean approach(Endpoint endpoint){
        var at=endpoint.target.center();var face=endpoint.target.face();
        var goal=at.add(net.minecraft.world.phys.Vec3.atLowerCornerOf(face.getNormal()).scale(.75));
        golem.getLookControl().setLookAt(at.x,at.y,at.z);
        double reach=2+tc4revival.config.TCConfig.SERVER.golemInteractionBonus.get();
        if(golem.position().distanceToSqr(at)>reach*reach){
            // The default moveTo overload accepts a path ending one block short. For a
            // container face that can leave the golem just outside interaction range.
            var path=golem.getNavigation().createPath(net.minecraft.core.BlockPos.containing(goal),0);
            if(path!=null)golem.getNavigation().moveTo(path,golem.navigationSpeed());
            return false;
        }
        golem.getNavigation().stop();return true;
    }
    private void acted(ServerLevel level){nextAction=level.getGameTime()+Math.max(5,golem.workDelay());golem.action();}
    private int count(IItemHandler inventory,ItemStack sample){
        int count=0;if(inventory!=null)for(int i=0;i<inventory.getSlots();i++)if(GolemItemMatching.matches(inventory.getStackInSlot(i),sample,golem.configuration()))count+=inventory.getStackInSlot(i).getCount();return count;
    }
    private boolean color(Endpoint marked,ItemStack item){
        if(marked.color==-1)return !golem.filters().matchingColors(item,golem.configuration()).isEmpty();
        return golem.filters().matchingColors(item,golem.configuration()).stream().anyMatch(c->c==-1||c==marked.color);
    }
    private int wanted(Endpoint home,ItemStack item,GolemCoreType core){
        if(core!=GolemCoreType.FILL)return golem.configuration().carryLimit();
        if(golem.filters().isEmpty())return 0;
        int desired=golem.filters().amountNeeded(item,golem.configuration().upgrades(5)>0);
        return desired<=0?0:golem.configuration().toggle(0)?golem.configuration().carryLimit():Math.max(0,desired-count(home.target.items(),item));
    }
    private Endpoint destination(Endpoint home,List<Endpoint> marked,ItemStack item,GolemCoreType core){
        if(core==GolemCoreType.FILL)return GolemItemTransfer.insert(home.target.items(),item,false).getCount()<item.getCount()?home:null;
        for(var target:marked)if(color(target,item)&&(core!=GolemCoreType.SORTING||count(target.target.items(),item)>0)
            &&GolemItemTransfer.insert(target.target.items(),item,false).getCount()<item.getCount())return target;
        return null;
    }
    private void items(ServerLevel level,Endpoint home,List<Endpoint> marked,GolemCoreType core){
        if(!golem.carried().isEmpty()){
            var destination=destination(home,marked,golem.carried(),core);
            if(destination==null){golem.getNavigation().stop();return;}
            if(approach(destination)){golem.carried(GolemItemTransfer.insert(destination.target.items(),golem.carried(),true));acted(level);}return;
        }
        var sources=core==GolemCoreType.FILL?marked:List.of(home);
        for(var source:sources){
            var inventory=source.target.items();if(inventory==null)continue;
            for(int slot=0;slot<inventory.getSlots();slot++){
                var sample=inventory.getStackInSlot(slot);if(sample.isEmpty())continue;
                if(core==GolemCoreType.FILL&&!color(source,sample)||core!=GolemCoreType.FILL&&core!=GolemCoreType.SORTING&&golem.filters().matchingColors(sample,golem.configuration()).isEmpty())continue;
                int limit=Math.min(golem.configuration().carryLimit(),wanted(home,sample,core));if(limit<=0)continue;
                var extract=inventory.extractItem(slot,limit,true);if(extract.isEmpty())continue;
                var destination=destination(home,marked,extract,core);if(destination==null)continue;
                int accepted=extract.getCount()-GolemItemTransfer.insert(destination.target.items(),extract,false).getCount();if(accepted<=0)continue;
                if(approach(source)){golem.carried(inventory.extractItem(slot,accepted,false));acted(level);}return;
            }
        }golem.getNavigation().stop();
    }
    private boolean fluidAllowed(FluidStack fluid){
        if(golem.filters().isEmpty())return true;
        for(int i=0;i<golem.filters().size();i++)if(FluidUtil.getFluidContained(golem.filters().sample(i)).map(f->f.isFluidEqual(fluid)).orElse(false))return true;
        return false;
    }
    private void liquids(ServerLevel level,Endpoint home,List<Endpoint> marked){
        var sink=home.target.fluids();if(sink==null)return;
        if(!golem.fluid().isEmpty()){
            if(sink.fill(golem.fluid(),FluidAction.SIMULATE)>0&&approach(home)){var rest=golem.fluid();rest.shrink(sink.fill(rest,FluidAction.EXECUTE));golem.fluid(rest);acted(level);}return;
        }
        for(var source:marked){var inventory=source.target.fluids();if(inventory==null)continue;
            for(int tank=0;tank<inventory.getTanks();tank++){
                var probe=inventory.getFluidInTank(tank).copy();if(probe.isEmpty()||!fluidAllowed(probe))continue;
                probe.setAmount(Math.min(probe.getAmount(),golem.configuration().fluidCarryLimit()));
                probe=inventory.drain(probe,FluidAction.SIMULATE);if(probe.isEmpty())continue;
                probe.setAmount(sink.fill(probe,FluidAction.SIMULATE));if(probe.isEmpty())continue;
                if(approach(source)){golem.fluid(inventory.drain(probe,FluidAction.EXECUTE));acted(level);}return;
            }
        }golem.getNavigation().stop();
    }
    private void essentia(ServerLevel level,Endpoint home,List<Endpoint> marked){
        if(golem.essentia()!=null&&golem.essentiaAmount()>0){
            for(var destination:marked){var sink=destination.target.essentia();if(sink==null||sink.insert(golem.essentia(),golem.essentiaAmount(),true)<=0)continue;
                if(approach(destination)){int rest=golem.essentiaAmount()-sink.insert(golem.essentia(),golem.essentiaAmount(),false);golem.essentia(rest==0?null:golem.essentia(),rest);acted(level);}return;
            }return;
        }
        var source=home.target.essentia();if(source==null)return;
        for(var aspect:source.contents().aspects()){
            int amount=source.extract(aspect,golem.configuration().carryLimit(),true);if(amount<=0)continue;
            for(var destination:marked){var sink=destination.target.essentia();if(sink==null)continue;
                int accepted=sink.insert(aspect,amount,true);if(accepted<=0)continue;
                if(approach(home)){int taken=source.extract(aspect,accepted,false);golem.essentia(taken==0?null:aspect,taken);acted(level);}return;
            }
        }golem.getNavigation().stop();
    }
}
