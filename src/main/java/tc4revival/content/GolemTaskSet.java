package tc4revival.content;

import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;

/** Complete core-to-task mapping; the owning entity invokes this after configuring its statistics. */
public  class GolemTaskSet {
    private GolemTaskSet() {}
    static void stopAndRemoveGoals(GoalSelector selector) {
        // removeAllGoals only removes entries; it leaves running goals holding MOVE/LOOK locks.
        var previous=selector.getAvailableGoals().stream().map(WrappedGoal::getGoal).toList();
        previous.forEach(selector::removeGoal);
    }
    public static void install(GolemWorkHost host,GoalSelector work,GoalSelector targets) {
        stopAndRemoveGoals(work);
        stopAndRemoveGoals(targets);
        host.mob().setTarget(null);
        host.mob().getNavigation().stop();
        if(host instanceof GolemEntity moving && moving.mobileBindings()){
            stopAndRemoveGoals(targets);moving.setTarget(null);
            work.addGoal(0,new GolemMobileContainerGoal(moving));return;
        }
        if(host.configuration().customTask()!=null && host instanceof GolemEntity entity){
            stopAndRemoveGoals(targets);thaumcraft.api.golems.GolemApi.task(host.configuration().customTask()).ifPresent(task->task.install(entity,work,targets));return;
        }
        var core=host.configuration().core(); if (core==null) return;
        if (!host.configuration().properties().sinks()) work.addGoal(0,new FloatGoal(host.mob()));
        work.addGoal(0,new GolemAvoidCreeperGoal(host));
        switch (core) {
            case FILL -> {
                work.addGoal(1,new GolemHomeReplaceGoal(host)); work.addGoal(2,new GolemHomePlaceGoal(host));
                work.addGoal(3,new GolemHomeDropGoal(host)); work.addGoal(4,new GolemFillTakeGoal(host)); work.addGoal(5,new GolemFillTravelGoal(host));
            }
            case EMPTY -> {
                work.addGoal(1,new GolemHomeReplaceGoal(host)); work.addGoal(2,new GolemEmptyPlaceGoal(host));
                work.addGoal(3,new GolemEmptyDropGoal(host)); work.addGoal(4,new GolemEmptyTravelGoal(host)); work.addGoal(5,new GolemHomeTakeGoal(host,false));
            }
            case GATHER -> {
                work.addGoal(1,new GolemHomeReplaceGoal(host)); work.addGoal(2,new GolemHomePlaceGoal(host)); work.addGoal(3,new GolemGatherGoal(host));
            }
            case HARVEST -> work.addGoal(3,new GolemHarvestGoal(host));
            case GUARD, BUTCHER -> {
                if (host.configuration().decorated('R')) work.addGoal(3,new GolemDartGoal(host));
                work.addGoal(4,new GolemMeleeGoal(host));
                if (core==GolemCoreType.GUARD) {
                    targets.addGoal(1,new HurtByTargetGoal(host.mob())); targets.addGoal(2,new GolemTargetGoal(host,false));
                } else targets.addGoal(1,new GolemTargetGoal(host,true));
            }
            case LIQUID -> {
                work.addGoal(2,new GolemLiquidEmptyGoal(host)); work.addGoal(3,new GolemLiquidGatherGoal(host)); work.addGoal(4,new GolemLiquidTravelGoal(host));
            }
            case ESSENTIA -> {
                work.addGoal(2,new GolemEssentiaEmptyGoal(host)); work.addGoal(3,new GolemEssentiaGatherGoal(host)); work.addGoal(4,new GolemEssentiaTravelGoal(host));
            }
            case LUMBER -> work.addGoal(3,new GolemLumberGoal(host));
            case USE -> {
                work.addGoal(1,new GolemHomeReplaceGoal(host)); work.addGoal(1,new GolemUseItemGoal(host)); work.addGoal(5,new GolemHomeTakeGoal(host,false));
            }
            case SORTING -> {
                work.addGoal(1,new GolemHomeReplaceGoal(host)); work.addGoal(2,new GolemEmptyPlaceGoal(host,true));
                work.addGoal(4,new GolemEmptyTravelGoal(host,true)); work.addGoal(5,new GolemHomeTakeGoal(host,true));
            }
            case FISH -> work.addGoal(3,new GolemFishGoal(host));
        }
        work.addGoal(6,new GolemOpenDoorGoal(host)); work.addGoal(7,new GolemReturnHomeGoal(host));
        work.addGoal(8,new LookAtPlayerGoal(host.mob(),Player.class,6)); work.addGoal(9,new RandomLookAroundGoal(host.mob()));
    }
}
