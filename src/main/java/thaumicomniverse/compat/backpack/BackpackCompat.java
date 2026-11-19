package thaumicomniverse.compat.backpack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import thaumcraft.api.wands.FocusStorageRegistry;

public final class BackpackCompat {
    public static void register(DeferredRegister<Item> items) {
        items.register("arcane_crafting_upgrade", ArcaneCraftingUpgradeItem::new);
        items.register("energized_node_upgrade", EnergizedNodeUpgradeItem::new);
        items.register("focus_pouch_upgrade", FocusPouchUpgradeItem::new);
        items.register("essentia_tank_upgrade", EssentiaTankUpgradeItem::new);
        items.register("essentia_pump_upgrade", EssentiaPumpUpgradeItem::new);
    }
    public static void setup() {
        thaumcraft.api.devices.VisChargingTargets.register("thaumicomniverse:backpack", 0, context -> {
            if (!(context.target() instanceof net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity backpack)) return java.util.Optional.empty();
            return java.util.Optional.of(payment -> BackpackVisCharging.charge(backpack.getBackpackWrapper(), payment::pay, 5));
        });
        net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry.register(
                ResourceLocation.parse("thaumicomniverse:arcane_crafting_upgrade"), ArcaneCraftingContainer.TYPE);
        net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry.register(
                ResourceLocation.fromNamespaceAndPath("thaumicomniverse", "essentia_pump_upgrade"), EssentiaPumpContainer.TYPE);
        net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry.register(
                ResourceLocation.fromNamespaceAndPath("thaumicomniverse", "essentia_tank_upgrade"), EssentiaTankContainer.TYPE);
        thaumcraft.api.aspects.EssentiaStorageApi.register(ResourceLocation.fromNamespaceAndPath("thaumicomniverse", "backpack"),
                (entity, face) -> entity instanceof net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity backpack
                        ? EssentiaTankUpgradeItem.find(backpack.getBackpackWrapper()) : null);
        FocusStorageRegistry.register(ResourceLocation.fromNamespaceAndPath("thaumicomniverse", "backpack"), stack ->
                stack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).resolve().flatMap(backpack ->
                        backpack.getUpgradeHandler().getTypeWrappers(FocusPouchUpgradeItem.TYPE).stream()
                                .filter(FocusPouchUpgradeItem.Wrapper::isEnabled).findFirst()).orElse(null));
    }
}
