package tc4revival.content;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

public  class PrimalFocusItem extends WandFocusItem {
    @Override public int color(ItemStack focus) { return 10854849; }
    @Override public String sortingPrefix() { return "FP"; }
    @Override public String depthTexture() { return "focus_primal_depth"; }
    @Override public int cooldownMillis(ItemStack focus) { return 500; }
    @Override public Map<Aspect,Integer> costs(ItemStack focus) {
        var random=new Random(System.currentTimeMillis()/200L);
        var result=new LinkedHashMap<Aspect,Integer>(); var aspects=TCAspects.catalog();
        for (String tag:List.of("aqua","aer","terra","ignis","ordo","perditio")) result.put(aspects.require(tag),50+random.nextInt(5)*50);
        return result;
    }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank) {
        return rank==3 ? List.of(FocusUpgrade.FRUGAL,FocusUpgrade.SEEKER)
                : rank>=1 && rank<=5 ? List.of(FocusUpgrade.FRUGAL) : List.of();
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit) {
        var wand=(CastingWandItem)stack.getItem();
        // Construction precedes payment on both sides, as in the original focus.
        var orb=FocusProjectiles.fromHand(new PrimalOrbEntity(level,player,wand.focusUpgrade(stack,FocusUpgrade.SEEKER)>0),player,hand);
        if (!level.isClientSide && wand.consumeForPlayer(stack,player,costs(stack),false,false,true)) {
            level.addFreshEntity(orb); level.playSound(null,orb.blockPosition(),TCSounds.ICE.get(),net.minecraft.sounds.SoundSource.PLAYERS,.3f,.8f+level.random.nextFloat()*.1f);
        }
        player.swing(hand); return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
