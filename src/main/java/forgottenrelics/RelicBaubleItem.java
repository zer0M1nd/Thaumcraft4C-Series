package forgottenrelics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import top.theillusivec4.curios.api.CuriosApi;

import vazkii.botania.common.item.equipment.bauble.BaubleItem;

/** Botania's base provides cosmetic attachment, phantom ink, Curios equipping and rendering. */
public class RelicBaubleItem extends BaubleItem
        implements thaumcraft.api.IVisDiscountGear,
                thaumcraft.api.IWarpingGear,
                thaumcraft.api.IRunicArmor {
    public final Relic relic;

    public RelicBaubleItem(Relic relic) {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
        this.relic = relic;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return relic.warp;
    }

    @Override
    public int getRunicCharge(ItemStack stack) {
        return 0;
    }

    @Override
    public int getVisDiscount(
            ItemStack stack, Player player, thaumcraft.api.aspects.Aspect aspect) {
        return relic == Relic.ARCANUM ? 35 : 0;
    }

    @Override
    public void onWornTick(ItemStack stack, LivingEntity entity) {
        if (entity instanceof ServerPlayer p)
            CuriosApi.getCuriosInventory(p)
                    .ifPresent(
                            inv ->
                                    inv.findCurios(candidate -> candidate == stack).stream()
                                            .findFirst()
                                            .ifPresent(
                                                    slot ->
                                                            RelicPassives.worn(
                                                                    relic,
                                                                    stack,
                                                                    p,
                                                                    slot.slotContext())));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            net.minecraft.world.level.Level level,
            java.util.List<net.minecraft.network.chat.Component> lines,
            net.minecraft.world.item.TooltipFlag flags) {
        super.appendHoverText(stack, level, lines, flags);
        lines.add(
                net.minecraft.network.chat.Component.translatable(
                                "tooltip.forgottenrelics." + relic.id)
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
        if (relic == Relic.OBLIVION_AMULET)
            lines.add(
                    net.minecraft.network.chat.Component.translatable(
                            "tooltip.forgottenrelics.stored_damage",
                            stack.getOrCreateTag().getFloat("damage")));
    }
}
