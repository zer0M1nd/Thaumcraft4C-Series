package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GolemTaskSwitchTest {
    static final class Delivery extends Goal {
        int starts,stops,ticks;
        Delivery(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return true;}
        @Override public boolean canContinueToUse(){return true;}
        @Override public void start(){starts++;}
        @Override public void stop(){stops++;}
        @Override public void tick(){ticks++;}
    }
    @Test void successiveJobsReleaseRunningLocksBeforeReplacement(){
        var selector=new GoalSelector(()->InactiveProfiler.INSTANCE);
        selector.setNewGoalRate(1);
        var first=new Delivery();selector.addGoal(1,first);selector.tick();
        assertEquals(1,first.starts);assertTrue(first.ticks>0);
        GolemTaskSet.stopAndRemoveGoals(selector);
        assertEquals(1,first.stops);assertTrue(selector.getAvailableGoals().isEmpty());
        var second=new Delivery();selector.addGoal(1,second);selector.tick();
        assertEquals(1,second.starts,"The replacement must acquire the same-priority MOVE and LOOK locks");
        assertTrue(second.ticks>0);
        GolemTaskSet.stopAndRemoveGoals(selector);GolemTaskSet.stopAndRemoveGoals(selector);
        assertEquals(1,second.stops,"Cleanup must stop a running job exactly once");
        var third=new Delivery();selector.addGoal(1,third);selector.tick();
        assertEquals(1,third.starts);assertEquals(1,first.starts);
    }
}
