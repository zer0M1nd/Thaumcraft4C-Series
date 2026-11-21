package conciliummodern;

import java.util.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;

public final class ScribingFocus extends ConciliumFocus {
    @Override public String sortingPrefix(){return "CONCILIUM_SCRIBING";}
    @Override public int color(ItemStack focus){return 0x111111;}
    @Override public String depthTexture(){return "thaumicconcilium:item/scribing_foci_depth";}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return vis("terra",50,"ignis",50,"aer",60);}
    @Override public boolean costPerTick(ItemStack focus){return true;}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return ranks(rank,List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY,FocusUpgrade.ENLARGE));}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack wand,Level level,Player player,InteractionHand hand,BlockHitResult hit){
        player.startUsingItem(hand);return InteractionResultHolder.consume(wand);
    }
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(remaining%10!=0 || !(player.level() instanceof ServerLevel level) || !(stack.getItem() instanceof CastingWandItem wand))return;
        var actor=thaumcraft.api.wands.WandActors.resolve(player);
        var target=FocusTargeting.pointed(actor,0,5+5*wand.focusUpgrade(stack,FocusUpgrade.ENLARGE),.1);
        if(!(target instanceof ServerPlayer recipient) || !wand.consumeForPlayer(stack,player,costs(attached(stack)),false,false,true))return;
        RunicShieldEvents.restore(recipient,2+wand.focusPotency(stack,player));
        level.playSound(null,actor.blockPosition(),TCSounds.WRITE.get(),SoundSource.PLAYERS,.5f,.4f/(level.random.nextFloat()*.4f+.8f));
        RuneFlow.send(actor,recipient);
    }
}
