package tc4revival.content;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Modern food properties and use animation, with the old completed-meal behavior. */
public class LegacyFoodItem extends Item {
    private final int duration;

    public LegacyFoodItem(Properties properties, int duration) {
        super(properties);
        this.duration = duration;
    }

    @Override public int getUseDuration(ItemStack stack) { return duration; }

    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        if (!(eater instanceof Player player)) return super.finishUsingItem(stack, level, eater);
        var food = stack.getFoodProperties(eater);
        if (food == null) return stack;
        // Capture properties and advancement input before the last item becomes an empty stack.
        var consumed = stack.copy();
        stack.shrink(1);
        player.getFoodData().eat(food.getNutrition(), food.getSaturationModifier());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP,
                SoundSource.PLAYERS, 0.5f, level.random.nextFloat() * 0.1f + 0.9f);
        if (!level.isClientSide) {
            for (var effect : food.getEffects()) {
                if (level.random.nextFloat() < effect.getSecond())
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect.getFirst()));
            }
            // Retain modern advancement, statistics and vibration integration without an extra completion chew.
            player.awardStat(Stats.ITEM_USED.get(this));
            if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, consumed);
            player.gameEvent(GameEvent.EAT);
        }
        return stack;
    }
}
