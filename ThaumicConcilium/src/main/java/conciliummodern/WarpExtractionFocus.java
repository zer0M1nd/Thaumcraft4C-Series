package conciliummodern;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;

public final class WarpExtractionFocus extends ConciliumFocus implements thaumcraft.api.IWarpingGear {
    @Override public int getWarp(ItemStack stack,Player player){return 5;}
    @Override public String sortingPrefix(){return "CONCILIUM_WARP_EXTRACTION";}
    @Override public String depthTexture(){return "thaumicconcilium:item/warp_extraction_depth";}
    @Override public int color(ItemStack focus){return 0x5d016b;}
    @Override public Animation animation(ItemStack focus){return Animation.CHARGE;}
    @Override public boolean costPerTick(ItemStack focus){return true;}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return vis("aqua",50,"perditio",60,"aer",50);}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){
        return ranks(rank,rank==5?List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY):List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY,FocusUpgrade.ENLARGE),"mass_hysteria","self_flagellation");
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        if(!requireRing(player) || !(stack.getItem() instanceof CastingWandItem wand))return InteractionResultHolder.fail(stack);
        if(!player.isShiftKeyDown()){player.startUsingItem(hand);return InteractionResultHolder.consume(stack);}
        if(player instanceof ServerPlayer server && !(player instanceof net.minecraftforge.common.util.FakePlayer)){
            var charges=level.getEntitiesOfClass(WarpCharge.class,player.getBoundingBox().inflate(10),charge->charge.ownedBy(player) && !charge.forced());
            if(!charges.isEmpty())charges.forEach(WarpCharge::discard);
            else if(wand.consumeForPlayer(stack,player,costs(attached(stack)),false,false,false)){
                var charge=new WarpCharge(ConciliumEntities.WARP_CHARGE.get(),level);
                charge.initialize(server,Math.max(1,1+(int)wand.focusPotency(stack,player)),wand.focusUpgrade(stack,FocusUpgrade.ENLARGE),has(attached(stack),"mass_hysteria"),has(attached(stack),"self_flagellation"),false);
                charge.setPos(player.position());
                if(level.addFreshEntity(charge)){
                    wand.consumeForPlayer(stack,player,costs(attached(stack)),false,false,true);
                    level.playSound(null,player.blockPosition(),ConciliumSounds.BREATH.get(),net.minecraft.sounds.SoundSource.PLAYERS,.9f,.4f/(player.getRandom().nextFloat()*.4f+.8f));
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(player.level().isClientSide || !(stack.getItem() instanceof CastingWandItem wand))return;
        if(!ring(player)){player.stopUsingItem();return;}
        var entity=FocusTargeting.pointed(player,0,32,2,e->e instanceof WarpCharge);
        if(entity instanceof WarpCharge charge && !charge.forced() && charge.ownedBy(player)
                && wand.consumeForPlayer(stack,player,costs(attached(stack)),false,false,true)){
            var target=player.position().add(player.getLookAngle().scale(1.5)).add(0,2,0);
            var motion=target.subtract(charge.position().add(0,charge.getBbHeight()/2,0));
            if(motion.lengthSqr()>1)motion=motion.normalize();
            charge.setDeltaMovement(motion);charge.hurtMarked=true;
        }
    }
    @Override public void release(ItemStack stack,Level level,Player player,int remaining){
        if(level.isClientSide)return;
        for(var charge:level.getEntitiesOfClass(WarpCharge.class,player.getBoundingBox().inflate(10),c->c.ownedBy(player) && !c.forced())){
            charge.setDeltaMovement(player.getLookAngle().scale(2.5));charge.hurtMarked=true;
        }
    }
}
