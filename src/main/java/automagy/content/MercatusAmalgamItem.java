package automagy.content;

import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class MercatusAmalgamItem extends Item {
    public MercatusAmalgamItem(){super(new Properties());}
    @Override public int getUseDuration(ItemStack stack){return 32;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.DRINK;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(!level.isClientSide){entity.addEffect(new MobEffectInstance(MobEffects.POISON,100));entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION,150,1));}
        if(entity instanceof Player player&&player.getAbilities().instabuild)return stack;
        stack.shrink(1);ItemStack bottle=new ItemStack(Items.GLASS_BOTTLE);
        if(stack.isEmpty())return bottle;
        if(!level.isClientSide){if(entity instanceof Player player){if(!player.getInventory().add(bottle))player.drop(bottle,false);}else entity.spawnAtLocation(bottle);}
        return stack;
    }
}
