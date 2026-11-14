package forgottenrelics;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import thaumcraft.api.IWarpingGear;

import java.util.List;

public class RelicItem extends Item implements IWarpingGear {
    public final Relic relic;

    public RelicItem(Relic type) {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
        relic = type;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return relic.warp;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return switch (relic) {
            case DIMENSIONAL_MIRROR -> 80;
            case WEATHER_STONE -> 60;
            case OVERTHROWER -> 150;
            case VOID_GRIMOIRE -> 100;
            default -> 72000;
        };
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        return RelicUse.begin(relic, level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof ServerPlayer player) RelicUse.tick(relic, stack, player, remaining);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        RelicUse.clear(user);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        RelicUse.clear(entity);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayer p) RelicPassives.inventory(relic, stack, p);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return relic == Relic.XP_TOME && stack.getOrCreateTag().getBoolean("active")
                || relic == Relic.DIMENSIONAL_MIRROR
                        && stack.hasTag()
                        && stack.getTag().contains("destination");
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(
                Component.translatable("tooltip.forgottenrelics." + relic.id)
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
        if (relic == Relic.XP_TOME) {
            var n = stack.getOrCreateTag();
            lines.add(
                    Component.translatable(
                            "tooltip.forgottenrelics.xp",
                            n.getLong("experience"),
                            n.getBoolean("active")
                                    ? Component.translatable(
                                            n.getBoolean("extract")
                                                    ? "tooltip.forgottenrelics.extract"
                                                    : "tooltip.forgottenrelics.absorb")
                                    : Component.translatable("tooltip.forgottenrelics.off")));
        }
        if (relic == Relic.FATE_TOME)
            lines.add(
                    Component.translatable(
                            "tooltip.forgottenrelics.cooldown",
                            stack.getOrCreateTag().getInt("fate_cooldown") / 20));
        if (relic == Relic.OBLIVION_AMULET)
            lines.add(
                    Component.translatable(
                            "tooltip.forgottenrelics.stored_damage",
                            stack.getOrCreateTag().getFloat("damage")));
        if (relic == Relic.OBLIVION_STONE) OblivionRecipe.tooltip(stack, lines);
        if (relic == Relic.DIMENSIONAL_MIRROR
                && stack.hasTag()
                && stack.getTag().contains("destination")) {
            var n = stack.getTag().getCompound("destination");
            lines.add(
                    Component.literal(
                            n.getString("dimension")
                                    + " "
                                    + n.getInt("x")
                                    + ", "
                                    + n.getInt("y")
                                    + ", "
                                    + n.getInt("z")));
        }
    }
}
