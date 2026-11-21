package tbmodern;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.content.TCEffects;

public final class RosehipSyrup extends Item {
    public RosehipSyrup() { super(new Properties().stacksTo(16)); }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack) { return 32; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity) {
        if(level.isClientSide)return stack;
        Set<MobEffect> remove=Set.of(MobEffects.BLINDNESS,MobEffects.CONFUSION,MobEffects.DIG_SLOWDOWN,MobEffects.HUNGER,
            MobEffects.MOVEMENT_SLOWDOWN,MobEffects.POISON,MobEffects.WEAKNESS,MobEffects.WITHER);
        Set<MobEffect> reduce=Set.of(TCEffects.BLURRED_VISION.get(),TCEffects.INFECTIOUS_VIS_EXHAUST.get(),TCEffects.THAUMARHIA.get(),
            TCEffects.UNNATURAL_HUNGER.get(),TCEffects.VIS_EXHAUST.get(),TCEffects.FLUX_TAINT.get());
        for(var effect:new ArrayList<>(entity.getActiveEffects())) {
            if(remove.contains(effect.getEffect()))entity.removeEffect(effect.getEffect());
            else if(reduce.contains(effect.getEffect())) {
                entity.removeEffect(effect.getEffect());
                if(effect.getAmplifier()>0) {
                    var replacement=new MobEffectInstance(effect.getEffect(),effect.getDuration(),effect.getAmplifier()-1,effect.isAmbient(),effect.isVisible(),effect.showIcon());
                    replacement.setCurativeItems(effect.getCurativeItems().stream().map(ItemStack::copy).toList());
                    entity.addEffect(replacement);
                }
            }
        }
        if(entity instanceof Player player && player.getAbilities().instabuild)return stack;
        stack.shrink(1);
        var empty=new ItemStack(tc4revival.content.TCItems.EMPTY_PHIAL.get());
        if(stack.isEmpty())return empty;
        if(entity instanceof Player player) { if(!player.addItem(empty))player.drop(empty,false); }
        else entity.spawnAtLocation(empty);
        return stack;
    }
}
