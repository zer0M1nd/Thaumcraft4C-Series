package conciliummodern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import tc4revival.content.*;

public final class AssistantJarSupplyGoal extends Goal {
    private final GolemEntity golem;private BlockPos opened;private int ticks;
    public AssistantJarSupplyGoal(GolemEntity golem){this.golem=golem;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
    public static boolean emptyJar(ItemStack stack){return stack.is(TCItems.ESSENTIA_JAR.get())&&stack.getItem() instanceof StoredAspectItem item&&item.storedAspects(stack).isEmpty();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public boolean canUse(){return ConciliumGolems.available(ConciliumGolems.ASSISTANT)&&golem.carried().isEmpty()&&golem.nearHomeAndIdle()&&golem.tickCount%Math.max(1,golem.workDelay())==0&&take(false);}
    private boolean take(boolean commit){
        if(!(golem.level() instanceof ServerLevel level))return false;
        var inventory=GolemInventoryViews.single(level,golem.homeInventory(),golem.homeFacing());if(inventory==null)return false;
        for(int slot=0;slot<inventory.getSlots();slot++)if(emptyJar(inventory.getStackInSlot(slot))){
            var result=inventory.extractItem(slot,Math.min(golem.carrySpace(),inventory.getStackInSlot(slot).getMaxStackSize()),!commit);if(!emptyJar(result))continue;
            if(commit){golem.carried(result);opened=golem.homeInventory();golem.openInventory(opened);ticks=5;}return true;
        }return false;
    }
    @Override public void start(){ticks=0;take(true);}
    @Override public boolean canContinueToUse(){return golem.workEnabled()&&ticks>0;}
    @Override public void tick(){ticks--;}
    @Override public void stop(){if(opened!=null){golem.closeInventory(opened);opened=null;}}
}
