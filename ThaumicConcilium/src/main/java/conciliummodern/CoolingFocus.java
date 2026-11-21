package conciliummodern;

import java.util.List;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** Original 1.1.1 cooling/refreeze modes, with bounded modern potency intervals. */
public final class CoolingFocus extends WandFocusItem {
    private static FocusUpgrade refreeze() { return FocusUpgrade.require("thaumicconcilium:refreeze"); }
    private boolean refrozen(ItemStack focus) { return upgradeLevel(focus,refreeze().id)>0; }
    @Override public String sortingPrefix() { return "CONCILIUM_COOLING"; }
    @Override public String depthTexture() { return "thaumicconcilium:item/cooling_depth"; }
    @Override public String depthTexture(ItemStack focus) { return refrozen(focus)?"thaumicconcilium:item/refreeze_depth":depthTexture(); }
    @Override public int color(ItemStack focus) { return 0xeeeeee; }
    @Override public Animation animation(ItemStack focus) { return Animation.CHARGE; }
    @Override public boolean costPerTick(ItemStack focus) { return true; }
    @Override public Map<Aspect,Integer> costs(ItemStack focus) {
        var aspects=TCAspects.catalog();
        return refrozen(focus)?Map.of(aspects.require("aqua"),100,aspects.require("aer"),150):Map.of(aspects.require("aqua"),3);
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank) {
        if(rank<1 || rank>5)return List.of();
        return rank==5?List.of(FocusUpgrade.POTENCY,refreeze()):List.of(FocusUpgrade.POTENCY);
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand,Level level,Player player,InteractionHand hand,BlockHitResult hit) {
        player.startUsingItem(hand);return InteractionResultHolder.consume(wand);
    }
    @Override public void usingTick(ItemStack stack,Player player,int remaining) {
        if(player.level().isClientSide || !(stack.getItem() instanceof CastingWandItem wand))return;
        var focus=attached(stack);boolean refreeze=refrozen(focus);
        int interval=Math.max(1,(refreeze?10:7)-(int)wand.focusPotency(stack,player));
        if(Math.floorMod(remaining,interval)!=0)return;
        if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,true)){player.stopUsingItem();return;}
        if(refreeze) {
            var shard=new FrostShardEntity(player.level(),player,1.5f,1,0,true,false);
            positionForHand(shard,player);player.level().addFreshEntity(shard);
            player.level().playSound(null,player.blockPosition(),TCSounds.ICE.get(),SoundSource.PLAYERS,.4f,1+player.getRandom().nextFloat()*.1f);
        } else {
            var snow=new Snowball(player.level(),player);
            snow.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.5f,1);
            positionForHand(snow,player);player.level().addFreshEntity(snow);
            player.level().playSound(null,player.blockPosition(),SoundEvents.SNOWBALL_THROW,SoundSource.PLAYERS,.5f,.4f/(player.getRandom().nextFloat()*.4f+.8f));
        }
    }
    private static void positionForHand(net.minecraft.world.entity.Entity projectile,Player player) {
        var arm=player.getUsedItemHand()==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite();
        double side=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?1:-1;
        double yaw=Math.toRadians(player.getYRot());
        projectile.setPos(player.getX()-Math.cos(yaw)*.16*side,player.getEyeY()-.1,player.getZ()-Math.sin(yaw)*.16*side);
    }
}
