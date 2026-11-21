package gadomancymodern.golem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraftforge.items.IItemHandler;
import tc4revival.content.*;

/** Marker-face reach, mixed block/tool samples and original tool-exchange behavior. */
public class BreakGoal extends Goal {
    protected final GolemEntity golem;
    protected final net.minecraftforge.common.util.FakePlayer player;
    protected GolemMarker marker;
    protected boolean toolValid;
    protected final java.util.Map<GolemMarker,Integer> blacklist=new java.util.HashMap<>();
    protected int pathFailures,counter,clicks,stage=-1;
    protected long started;
    protected net.minecraft.world.level.block.state.BlockState digging;
    public BreakGoal(GolemEntity golem){this.golem=golem;player=golem.level() instanceof ServerLevel server?new net.minecraftforge.common.util.FakePlayer(server,new com.mojang.authlib.GameProfile(java.util.UUID.fromString("d36a4fa5-c70e-4358-8418-9ed683687d0a"),"[gadomancy:golem]")):null;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
    public boolean canUse(){if(!golem.workEnabled()||golem.tickCount%golem.workDelay()>0||player==null)return false;marker=golem.markers().inDimension(golem.level().dimension().location()).stream().sorted(java.util.Comparator.comparingDouble(this::distance)).filter(this::validMarker).findFirst().orElse(null);toolValid=validTool(golem.carried());return toolValid?marker!=null:homeRange();}
    public boolean canContinueToUse(){return golem.workEnabled()&&(toolValid?marker!=null&&!golem.level().isEmptyBlock(marker.pos()):homeRange());}
    public boolean requiresUpdateEveryTick(){return true;}
    public void stop(){cancel();marker=null;pathFailures=0;}
    public void tick(){if(toolValid){if(distance(marker)<1){if(golem.carried().isEmpty())golem.action();else golem.rightArm();if(clicks++%(7-Math.min(6,golem.configuration().strength()))==0)leftClick();golem.getLookControl().setLookAt(marker.pos().getX()+.5,marker.pos().getY()+.5,marker.pos().getZ()+.5,30,30);counter=0;}else{if(counter==20){counter=0;var destination=marker.pos().relative(marker.face());if(!golem.getNavigation().moveTo(destination.getX()+.5,destination.getY()+.5,destination.getZ()+.5,golem.navigationSpeed())){if(pathFailures>10){blacklist.put(marker,golem.tickCount);stop();return;}pathFailures++;}}counter++;clicks=0;}}else if(golem.tickCount%golem.workDelay()>0&&homeRange()){var inventory=GolemItemTransfer.at((ServerLevel)golem.level(),golem.home().relative(golem.homeFacing().getOpposite()),golem.homeFacing());if(inventory!=null)switchTool(inventory);}}
    protected double distance(GolemMarker marker){return golem.distanceToSqr(marker.pos().getX()+.5+.5*marker.face().getStepX(),marker.pos().getY()+.5+.5*marker.face().getStepY(),marker.pos().getZ()+.5+.5*marker.face().getStepZ());}
    protected boolean homeRange(){return golem.distanceToSqr(golem.home().getX()+.5,golem.home().getY()+.5,golem.home().getZ()+.5)<3;}
    public boolean validMarker(GolemMarker marker){
        if(marker==null||!golem.level().hasChunkAt(marker.pos()))return false;var expiry=blacklist.get(marker);if(expiry!=null){if(expiry+800>=golem.tickCount)return false;blacklist.remove(marker);}if(golem.home().distSqr(marker.pos())>golem.configuration().range()*golem.configuration().range())return false;var state=golem.level().getBlockState(marker.pos());if(state.isAir())return false;var offered=new ItemStack(state.getBlock());boolean empty=true;
        for(int slot=0;slot<golem.filters().size();slot++){var filter=golem.filters().sample(slot);if(!(filter.getItem() instanceof BlockItem))continue;empty=false;if((marker.color()==-1||marker.color()==golem.filters().color(slot))&&GolemItemMatching.matches(offered,filter,golem.configuration()))return true;}return empty;
    }
    public boolean validTool(ItemStack tool){
        if(tool.getItem() instanceof BlockItem)return false;boolean empty=true;for(int slot=0;slot<golem.filters().size();slot++){var filter=golem.filters().sample(slot);if(filter.isEmpty()||filter.getItem() instanceof BlockItem||marker!=null&&golem.filters().color(slot)!=-1&&marker.color()!=-1&&golem.filters().color(slot)!=marker.color())continue;empty=false;if(!tool.isEmpty()&&GolemItemMatching.matches(tool,filter,golem.configuration()))return true;}return empty&&tool.isEmpty();
    }
    public void switchTool(IItemHandler inventory){
        for(int slot=0;slot<inventory.getSlots();slot++){
            var stack=inventory.getStackInSlot(slot);var current=golem.carried();if(!current.isEmpty()){if(!GolemItemTransfer.insert(inventory,current,false).isEmpty())return;var rest=GolemItemTransfer.insert(inventory,current,true);golem.carried(rest);if(!rest.isEmpty())return;if(validTool(ItemStack.EMPTY)){toolValid=true;return;}}
            if(stack.isEmpty()||!validTool(stack))continue;var taken=inventory.extractItem(slot,1,false);if(taken.isEmpty())continue;golem.carried(taken);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,taken);toolValid=true;break;
        }
    }
    public void leftClick(){
        if(marker==null)return;var level=(ServerLevel)golem.level();var pos=marker.pos();var state=level.getBlockState(pos);if(state.isAir())return;player.moveTo(golem.position());player.setOnGround(golem.onGround());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,golem.carried());player.removeAllEffects();for(var effect:golem.getActiveEffects())player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect));
        if(digging!=state){cancel();digging=state;started=level.getGameTime();var event=net.minecraftforge.common.ForgeHooks.onLeftClickBlock(player,pos,marker.face(),net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);if(event.isCanceled()||event.getUseItem()==net.minecraftforge.eventbus.api.Event.Result.DENY){digging=null;return;}if(event.getUseBlock()!=net.minecraftforge.eventbus.api.Event.Result.DENY)state.attack(level,pos,player);}
        float progress=state.getDestroyProgress(player,level,pos)*(level.getGameTime()-started+1);int next=(int)(progress*10);if(next!=stage){level.destroyBlockProgress(golem.getId(),pos,next);stage=next;}
        if(progress>=.9f){player.gameMode.destroyBlock(pos);golem.carried(player.getMainHandItem());cancel();toolValid=validTool(golem.carried());}
    }
    protected void cancel(){if(marker!=null&&golem.level() instanceof ServerLevel level)level.destroyBlockProgress(golem.getId(),marker.pos(),-1);digging=null;stage=-1;}
}
