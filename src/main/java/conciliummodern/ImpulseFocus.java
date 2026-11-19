package conciliummodern;

import java.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.*;
import tc4revival.content.*;

public final class ImpulseFocus extends WandFocusItem {
    @Override public String sortingPrefix() { return "CONCILIUM_IMPULSE"; }
    @Override public int color(ItemStack focus) { return SignalFocus.has(focus,"pneumo_strike")?0xff9999:0xff3333; }
    @Override public String depthTexture() { return "thaumicconcilium:item/impulse_depth"; }
    @Override public String depthTexture(ItemStack focus) {
        return SignalFocus.has(focus,"vacuum")?"thaumicconcilium:item/vacuum_impulse_depth":SignalFocus.has(focus,"pneumo_strike")?"thaumicconcilium:item/pneumo_depth":depthTexture();
    }
    @Override public String ornamentTexture() { return "thaumicconcilium:item/impulse_orn"; }
    @Override public String ornamentTexture(ItemStack focus) {
        return SignalFocus.has(focus,"vacuum")?"thaumicconcilium:item/impulse_orn_inversed":SignalFocus.has(focus,"pneumo_strike")?"thaumicconcilium:item/impulse_orn_diluted":ornamentTexture();
    }
    @Override public int cooldownMillis(ItemStack focus) { return 1000; }
    @Override public Map<Aspect,Integer> costs(ItemStack focus) {
        boolean vacuum=SignalFocus.has(focus,"vacuum"),pneumo=SignalFocus.has(focus,"pneumo_strike");var a=TCAspects.catalog();
        return Map.of(a.require("ignis"),vacuum?2000:pneumo?200:500,a.require("aer"),vacuum?2000:pneumo?750:500,a.require("perditio"),vacuum?3500:pneumo?300:600);
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank) {
        return switch(rank) {
            case 1 -> List.of(FocusUpgrade.FRUGAL,FocusUpgrade.EXTEND);
            case 2 -> List.of(FocusUpgrade.FRUGAL,FocusUpgrade.EXTEND,FocusUpgrade.POTENCY);
            case 3,4 -> List.of(FocusUpgrade.FRUGAL,FocusUpgrade.POTENCY);
            case 5 -> List.of(FocusUpgrade.FRUGAL,SignalFocus.upgrade("pneumo_strike"),SignalFocus.upgrade("vacuum"));default -> List.of();
        };
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!level.isClientSide && stack.getItem() instanceof CastingWandItem wand) {
            if(!wand.consumeForPlayer(stack,player,costs(attached(stack)),false,false,true))return InteractionResultHolder.fail(stack);
            var blast=new CompressedBlast(ConciliumEntities.COMPRESSED_BLAST.get(),level);
            blast.initialize(player,stack,ItemStack.EMPTY);level.addFreshEntity(blast);
        }
        player.swing(hand);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
