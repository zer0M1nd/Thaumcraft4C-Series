package ttmodern;

import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class InfusedPotionItem extends Item {
    private final String aspect;
    public InfusedPotionItem(String aspect) { super(new Properties().stacksTo(1));this.aspect=aspect; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack) { return 32; }
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand) { return ItemUtils.startUsingInstantly(world,player,hand); }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level world,LivingEntity entity) {
        if(!world.isClientSide)entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(ElementalEffects.EFFECTS.get(aspect).get(),3600));
        if(entity instanceof net.minecraft.server.level.ServerPlayer player)net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(player,stack);
        if(entity instanceof Player player)player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        if(!(entity instanceof Player player)||!player.getAbilities().instabuild) {
            stack.shrink(1);if(stack.isEmpty())return new ItemStack(Items.GLASS_BOTTLE);
            if(entity instanceof Player player&&!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE)))player.drop(new ItemStack(Items.GLASS_BOTTLE),false);
        }
        return stack;
    }
    @Override public void appendHoverText(ItemStack stack,Level world,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags) { lines.add(net.minecraft.network.chat.Component.translatable(ElementalEffects.EFFECTS.get(aspect).get().getDescriptionId()).append(" (3:00)")); }
}
