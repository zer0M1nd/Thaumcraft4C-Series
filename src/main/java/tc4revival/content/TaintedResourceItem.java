package tc4revival.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public  class TaintedResourceItem extends Item {
    public TaintedResourceItem() { super(new Properties()); }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || living.isInvertedHealAndHarm()
                || living.hasEffect(TCEffects.FLUX_TAINT.get()) || level.random.nextInt(4321)>stack.getCount()) return;
        living.addEffect(new MobEffectInstance(TCEffects.FLUX_TAINT.get(),120,0,false,true));
        if (entity instanceof Player player) {
            player.displayClientMessage(Component.translatable("tc.taint_item_poison", stack.getHoverName().copy()
                    .withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC)),false);
            if(!player.getAbilities().instabuild)stack.shrink(1);
        }
    }
}
