package thaumicomniverse.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import tc4revival.content.ThaumiumToolTier;
import tc4revival.content.VoidToolTier;
import tc4revival.content.VoidEquipment;
import tc4revival.content.WarpingGear;
import vectorwing.farmersdelight.common.item.KnifeItem;

public final class FarmersDelightCompat {
    private FarmersDelightCompat() {}
    public static void register(DeferredRegister<Item> items) {
        items.register("thaumium_knife", () -> new ThaumicKnife(ThaumiumToolTier.INSTANCE));
        items.register("void_knife", VoidKnife::new);
    }
    public static class ThaumicKnife extends KnifeItem implements thaumcraft.api.IRepairable {
        public ThaumicKnife(Tier tier) { super(tier, .5f, -2, new Item.Properties()); }
    }
    public static final class VoidKnife extends ThaumicKnife implements WarpingGear {
        public VoidKnife() { super(VoidToolTier.INSTANCE); }
        @Override public int warp(ItemStack stack, Player player) { return thaumicomniverse.RevivalFeatures.Group.FARMER.enabled() ? 1 : 0; }
        @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
            if (thaumicomniverse.RevivalFeatures.Group.FARMER.enabled() && !level.isClientSide) VoidEquipment.repair(stack, holder);
        }
        @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            if (thaumicomniverse.RevivalFeatures.Group.FARMER.enabled()) VoidEquipment.weaken(target, attacker, 60);
            return super.hurtEnemy(stack, target, attacker);
        }
    }
}
