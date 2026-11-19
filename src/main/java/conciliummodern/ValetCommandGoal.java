package conciliummodern;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.*;
import tc4revival.content.GolemEntity;

/** A spoken command consumes one pending request and activates the first matching marked face. */
public final class ValetCommandGoal extends Goal {
    private final GolemEntity golem;private BlockPos target;private Direction face;private BlockState expected;private int ticks,next;
    public ValetCommandGoal(GolemEntity golem){this.golem=golem;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
    public static FakePlayer actor(GolemEntity golem){
        var identity=java.util.UUID.nameUUIDFromBytes(("ConciliumGolem:"+golem.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return FakePlayerFactory.get((ServerLevel)golem.level(),new GameProfile(identity,"FakeThaumcraftGolem"));
    }
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public boolean canUse(){
        if(!golem.workEnabled()||!ConciliumGolems.available(ConciliumGolems.VALET)||golem.tickCount<next||!golem.getNavigation().isDone())return false;
        next=golem.tickCount+3;var data=ConciliumGolems.data(golem);if(!data.getBoolean("Pending"))return false;
        String command=data.getString("Command");data.putBoolean("Pending",false);ConciliumGolems.data(golem,data);
        var level=golem.level();
        for(int slot=0;slot<golem.filters().size();slot++){
            var paper=golem.filters().sample(slot);if(!paper.is(Items.PAPER)||!paper.getHoverName().getString().equals(command))continue;
            byte color=golem.filters().color(slot);
            for(var marker:golem.markers().inDimension(level.dimension().location())){
                if(color!=-1&&marker.color()!=color||!level.hasChunkAt(marker.pos())||!golem.isWithinRestriction(marker.pos())||level.isEmptyBlock(marker.pos())!=golem.configuration().toggle(0))continue;
                var adjacent=marker.pos().relative(marker.face());if(!level.hasChunkAt(adjacent)||!level.isEmptyBlock(adjacent))continue;
                target=marker.pos();face=marker.face();expected=level.getBlockState(target);return true;
            }
        }
        return false;
    }
    @Override public void start(){ticks=200;golem.getNavigation().moveTo(target.getX()+.5,target.getY()+.5,target.getZ()+.5,golem.navigationSpeed());}
    @Override public boolean canContinueToUse(){return golem.workEnabled()&&ticks>0&&target!=null&&golem.level().hasChunkAt(target)&&golem.level().getBlockState(target).equals(expected);}
    @Override public void tick(){
        ticks--;golem.getLookControl().setLookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5,30,30);
        if(golem.position().distanceToSqr(Vec3.atCenterOf(target))<=4)click();else if(golem.getNavigation().isDone())ticks=0;
    }
    private void click(){
        ticks=0;if(!(golem.level() instanceof ServerLevel level))return;
        var clicked=level.isEmptyBlock(target)?target.relative(face.getOpposite()):target;var player=actor(golem);
        if(!level.hasChunkAt(clicked)||!level.getWorldBorder().isWithinBounds(clicked)||!level.mayInteract(player,clicked))return;
        player.moveTo(golem.getX(),golem.getY(),golem.getZ(),golem.getYRot(),golem.getXRot());player.getInventory().selected=0;
        player.setItemInHand(InteractionHand.MAIN_HAND,golem.carried().copy());player.setShiftKeyDown(golem.configuration().toggle(2));
        try(var interaction=thaumcraft.api.golems.GolemInteraction.enter(golem)){
            if(golem.configuration().toggle(1)){golem.leftArm();player.gameMode.handleBlockBreakAction(clicked,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,face,level.getMaxBuildHeight(),0);}
            else{
                var result=player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(clicked),face,clicked,false));
                if(result.consumesAction())golem.rightArm();
            }
        }catch(RuntimeException exception){com.mojang.logging.LogUtils.getLogger().warn("Valet interaction failed at {}",clicked,exception);}
        finally{
            golem.carried(player.getMainHandItem().copy());player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            for(int slot=1;slot<player.getInventory().getContainerSize();slot++){
                var extra=player.getInventory().getItem(slot);if(extra.isEmpty())continue;
                if(golem.carried().isEmpty())golem.carried(extra.copy());else golem.spawnAtLocation(extra.copy());
                player.getInventory().setItem(slot,ItemStack.EMPTY);
            }
            player.setShiftKeyDown(false);
        }
    }
    @Override public void stop(){target=null;ticks=0;golem.getNavigation().stop();}
}
