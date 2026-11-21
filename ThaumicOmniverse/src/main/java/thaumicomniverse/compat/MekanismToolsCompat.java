package thaumicomniverse.compat;

import mekanism.common.config.BaseMekanismConfig;
import mekanism.tools.common.item.ItemMekanismPaxel;
import mekanism.tools.common.material.MaterialCreator;
import mekanism.tools.common.material.impl.BronzeMaterialDefaults;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.DeferredRegister;
import tc4revival.content.*;

public final class MekanismToolsCompat {
    private MekanismToolsCompat() {}
    public static void register(DeferredRegister<Item> items, IEventBus bus) {
        MaterialConfig config = new MaterialConfig();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, config.spec, "thaumicomniverse-tools.toml");
        bus.addListener((ModConfigEvent event) -> {
            if (event.getConfig().getSpec() == config.spec) config.clearCache(event instanceof ModConfigEvent.Unloading);
        });
        items.register("thaumium_paxel", () -> new ThaumicPaxel(config.thaumium));
        items.register("void_paxel", () -> new VoidPaxel(config.voidmetal));
    }

    private static final class MaterialConfig extends BaseMekanismConfig {
        final ForgeConfigSpec spec;
        final MaterialCreator thaumium, voidmetal;
        MaterialConfig() {
            ForgeConfigSpec.Builder builder = new MaterialConfigBuilder();
            thaumium = new MaterialCreator(this, builder, new MaterialDefaults("thaumium", ThaumiumToolTier.INSTANCE));
            voidmetal = new MaterialCreator(this, builder, new MaterialDefaults("void", VoidToolTier.INSTANCE));
            spec = builder.build();
        }
        @Override public String getFileName() { return "thaumicomniverse-tools"; }
        @Override public ForgeConfigSpec getConfigSpec() { return spec; }
        @Override public ModConfig.Type getConfigType() { return ModConfig.Type.SERVER; }
    }
    /** Mekanism's material API supplies its own paxel actions/attributes and configurable durability. */
    private static final class MaterialDefaults extends BronzeMaterialDefaults {
        private final String name;
        private final Tier tier;
        MaterialDefaults(String name, Tier tier) { this.name = name; this.tier = tier; }
        @Override public String getRegistryPrefix() { return name; }
        @Override public String getConfigCommentName() { return name; }
        @Override public int getUses() { return tier.getUses(); }
        @Override public float getSpeed() { return tier.getSpeed(); }
        @Override public int getLevel() { return tier.getLevel(); }
        @Override public float getAttackDamageBonus() { return tier.getAttackDamageBonus(); }
        @Override public int getCommonEnchantability() { return tier.getEnchantmentValue(); }
        @Override public Ingredient getCommonRepairMaterial() { return tier.getRepairIngredient(); }
        @Override public TagKey<Block> getTag() { return BlockTags.NEEDS_DIAMOND_TOOL; }
        @Override public float getAxeDamage() { return 3; }
        @Override public float getPaxelAtkSpeed() { return -2.4f; }
    }
    public static class ThaumicPaxel extends ItemMekanismPaxel implements thaumcraft.api.IRepairable {
        public ThaumicPaxel(MaterialCreator material) { super(material, new Item.Properties()); }
    }
    public static final class VoidPaxel extends ThaumicPaxel implements WarpingGear {
        public VoidPaxel(MaterialCreator material) { super(material); }
        @Override public int warp(ItemStack stack, Player player) { return thaumicomniverse.RevivalFeatures.Group.MEKANISM.enabled() ? 1 : 0; }
        @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
            super.inventoryTick(stack, level, holder, slot, selected);
            if (thaumicomniverse.RevivalFeatures.Group.MEKANISM.enabled() && !level.isClientSide) VoidEquipment.repair(stack, holder);
        }
        @Override public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
            if (thaumicomniverse.RevivalFeatures.Group.MEKANISM.enabled()) VoidEquipment.weaken(target, player, 80);
            return super.onLeftClickEntity(stack, player, target);
        }
    }
}
