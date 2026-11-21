package tc4revival.content;

import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tc4revival.research.BrainConsumption;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.network.TCNetwork;
import tc4revival.network.WarpGainPacket;

public  class ZombieBrainItem extends LegacyFoodItem {
    public ZombieBrainItem() {
        super(new Properties().food(new FoodProperties.Builder().nutrition(4).saturationMod(0.2f).meat()
                .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8f).build()), 32);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        return finishUsingItem(stack, level, eater, ZombieBrainItem::persistWarp);
    }
    /** Runs the registered item's eating path against an isolated warp store when validating on the server. */
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater,
                                    BiConsumer<ServerPlayer, BrainConsumption.Gain> warp) {
        java.util.Objects.requireNonNull(warp);
        if (!level.isClientSide && eater instanceof ServerPlayer player) warp.accept(player, BrainConsumption.roll(level.random));
        return super.finishUsingItem(stack, level, eater);
    }
    private static void persistWarp(ServerPlayer player, BrainConsumption.Gain gain) {
        KnowledgeSavedData.get(player.getServer()).update(player.getUUID(), player.getRandom()::nextInt, knowledge -> {
            BrainConsumption.apply(knowledge, gain); return null;
        });
        TCNetwork.send(player, new WarpGainPacket(gain.kind(), gain.amount()));
    }
}
