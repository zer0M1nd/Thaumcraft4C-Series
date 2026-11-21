package tc4revival.content;

import java.util.EnumSet;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

/** Eligibility checks only simulate; item extraction happens once when the task starts. */
public class GolemFillTakeGoal extends Goal {
    private final GolemWorkHost host;
    private BlockPos selected,opened;
    private Direction side;
    private int chestTicks;
    public GolemFillTakeGoal(GolemWorkHost host) {this.host=host;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean requiresUpdateEveryTick() {return true;}
    private int amount(){return host.configuration().toggle(0)?host.carrySpace():Math.min(host.watched().getCount(),host.carrySpace());}
    @Override public boolean canUse() {
        selected=null;
        if(!host.workEnabled() || !(host.mob().level() instanceof ServerLevel level) || !host.carried().isEmpty()
                || host.watched().isEmpty() || host.filters().isEmpty())return false;
        for(var pos:GolemMarkedInventories.positions(host,true)) {
            if(GolemInventoryViews.same(level,pos,host.homeInventory()))continue;
            for(byte color:host.filters().matchingColors(host.watched(),host.configuration()))
                for(var face:host.markers().faces(pos,level.dimension().location(),color))
                    if(!GolemItemTransfer.extract(GolemInventoryViews.single(level,pos,face),host.watched(),amount(),host.configuration(),false).isEmpty()) {
                        selected=pos;side=face;return true;
                    }
        }
        return false;
    }
    @Override public void start() {
        chestTicks=0;
        if(!(host.mob().level() instanceof ServerLevel level) || selected==null || !host.carried().isEmpty() || !GolemWorkRange.reaches(host,selected,4))return;
        var result=GolemItemTransfer.extract(GolemInventoryViews.single(level,selected,side),host.watched(),amount(),host.configuration(),true);
        if(result.isEmpty())return;
        host.mob().getNavigation().stop();host.carried(result);host.watched(ItemStack.EMPTY);
        opened=selected;host.openInventory(opened);chestTicks=5;
    }
    @Override public boolean canContinueToUse(){return host.workEnabled() && chestTicks>0;}
    @Override public void tick(){chestTicks--;}
    @Override public void stop(){if(opened!=null){host.closeInventory(opened);opened=null;}}
}
