package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/** Home extraction is simulated for scheduling and committed only by start. */
public class GolemHomeTakeGoal extends Goal {
    private final GolemWorkHost host;
    private final boolean sorting;
    private int chestTicks;
    private BlockPos opened;
    public GolemHomeTakeGoal(GolemWorkHost host,boolean sorting){this.host=host;this.sorting=sorting;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public boolean canUse(){return host.carried().isEmpty() && host.nearHomeAndIdle() && host.mob().tickCount%Math.max(1,host.workDelay())==0 && take(false);}
    private boolean take(boolean execute) {
        if(!(host.mob().level() instanceof ServerLevel level) || !host.carried().isEmpty())return false;
        var pos=host.homeInventory();var inventory=GolemInventoryViews.single(level,pos,host.homeFacing());
        if(inventory==null)return false;
        var needed=host.transport().needed();
        if(needed!=null) {
            for(var stack:needed) {
                if(!host.transport().validTarget(stack))continue;
                var result=GolemItemTransfer.extract(inventory,stack,host.carrySpace(),host.configuration(),execute);
                if(!result.isEmpty()){if(execute)accept(result,pos);return true;}
            }
        } else if(!sorting) {
            for(int attempt=0;attempt<inventory.getSlots();attempt++) {
                var candidate=host.transport().first(inventory,false);if(candidate.isEmpty())return false;
                if(!host.transport().validTarget(candidate))continue;
                var result=host.transport().first(inventory,execute);
                if(!result.isEmpty()){if(execute)accept(result,pos);return true;}
            }
        }
        return false;
    }
    private void accept(ItemStack stack,BlockPos pos){host.carried(stack);host.openInventory(pos);opened=pos;chestTicks=5;}
    @Override public void start(){chestTicks=0;take(true);}
    @Override public boolean canContinueToUse(){return host.workEnabled() && chestTicks>0;}
    @Override public void tick(){chestTicks--;}
    @Override public void stop(){if(opened!=null){host.closeInventory(opened);opened=null;}}
}
