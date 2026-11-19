package thmodern.blood;

import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;

public final class InjectionItem extends Item {
    private final boolean phial;
    public InjectionItem(boolean phial){super(new Properties().stacksTo(1));this.phial=phial;}
    @Override public int getUseDuration(ItemStack stack){return 8;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.BOW;}
    public static void apply(ItemStack stack,LivingEntity target,Entity source){
        if(target.level().isClientSide)return;
        for(var effect:PotionUtils.getMobEffects(stack)){
            if(effect.getEffect().isInstantenous())effect.getEffect().applyInstantenousEffect(source,source,target,effect.getAmplifier(),1);
            else target.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect),source);
        }
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(!level.isClientSide){apply(stack,entity,entity);if(!(entity instanceof Player player&&player.getAbilities().instabuild))stack.shrink(1);}
        return stack;
    }
    @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        if(!target.level().isClientSide){apply(stack,target,attacker);stack.shrink(1);}return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!phial){player.startUsingItem(hand);return InteractionResultHolder.consume(stack);}
        if(!level.isClientSide){
            var projectile=new net.minecraft.world.entity.projectile.ThrownPotion(level,player);projectile.setItem(stack.copyWithCount(1));
            projectile.shootFromRotation(player,player.getXRot(),player.getYRot(),0,.75f,1);
            if(!level.addFreshEntity(projectile))return InteractionResultHolder.fail(stack);
            if(!player.getAbilities().instabuild)stack.shrink(1);
            level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.SPLASH_POTION_THROW,net.minecraft.sounds.SoundSource.PLAYERS,.5f,.4f/(level.random.nextFloat()*.4f+.8f));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){PotionUtils.addPotionTooltip(stack,lines,1);}
}
