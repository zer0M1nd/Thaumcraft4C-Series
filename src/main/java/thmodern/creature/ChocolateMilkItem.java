package thmodern.creature;

import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class ChocolateMilkItem extends MilkBucketItem {
    public ChocolateMilkItem(){super(new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET));}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){
        if(user instanceof ServerPlayer player){net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(player,stack);player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));}
        if(!level.isClientSide)user.curePotionEffects(new ItemStack(Items.MILK_BUCKET));
        if(!(user instanceof Player player)||!player.getAbilities().instabuild)stack.shrink(1);
        return stack.isEmpty()?new ItemStack(Items.BUCKET):stack;
    }
}
