package conciliummodern;

import java.util.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;

public final class PositiveBurstFocus extends WandFocusItem {
    @Override public String sortingPrefix() { return "CONCILIUM_POSITIVE_BURST"; }
    @Override public String depthTexture() { return "thaumicconcilium:item/positive_burst_depth"; }
    @Override public int color(ItemStack focus) { return 10854849; }
    @Override public int cooldownMillis(ItemStack focus) { return 500; }
    @Override public Map<Aspect,Integer> costs(ItemStack focus) {
        var random=new Random(System.currentTimeMillis()/200);var a=TCAspects.catalog();
        int vitamin=upgradeLevel(focus,SignalFocus.upgrade("vitaminize").id),fulfillment=upgradeLevel(focus,SignalFocus.upgrade("fulfillment").id);
        return Map.of(a.require("aqua"),300+random.nextInt(5+2*vitamin)*50,a.require("terra"),300+random.nextInt(5+2*fulfillment)*50,
                a.require("ordo"),400+random.nextInt(5+2*vitamin)*50);
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank) {
        if(rank<1 || rank>5)return List.of();
        return rank==5?List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY,SignalFocus.upgrade("vitaminize"),SignalFocus.upgrade("fulfillment")):List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY);
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!level.isClientSide && stack.getItem() instanceof CastingWandItem wand) {
            var focus=attached(stack);
            if(!wand.consumeForPlayer(stack,player,costs(focus),false,false,true))return InteractionResultHolder.fail(stack);
            int power=Math.max(0,(int)wand.focusPotency(stack,player));int count=4+player.getRandom().nextInt(5+power);
            for(int i=0;i<count;i++) {
                var orb=new PositiveBurstOrb(ConciliumEntities.POSITIVE_ORB.get(),level);orb.initialize(player,focus,power,hand);level.addFreshEntity(orb);
            }
            level.playSound(null,player.blockPosition(),TCSounds.ICE.get(),SoundSource.PLAYERS,.3f,.8f+player.getRandom().nextFloat()*.1f);
        }
        player.swing(hand);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
