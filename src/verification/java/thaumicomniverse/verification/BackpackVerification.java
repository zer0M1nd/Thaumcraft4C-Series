package thaumicomniverse.verification;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import thaumicomniverse.compat.backpack.*;
import static thaumicomniverse.verification.ServerVerification.*;

final class BackpackVerification {
    static void run() {
        var backpack = new ItemStack(ForgeRegistries.ITEMS.getValue(id("sophisticatedbackpacks:backpack")));
        var storage = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElseThrow(() -> new AssertionError("Missing backpack wrapper"));
        check("backpack tank capacity, simulation and aspect isolation", () -> {
            var upgrade = new ItemStack(ForgeRegistries.ITEMS.getValue(id("essentia_tank_upgrade")));
            var tank = new EssentiaTankUpgradeItem.Wrapper(storage, upgrade, ignored -> {});
            var air = tc4revival.aspect.TCAspects.catalog().require("aer"); var fire = tc4revival.aspect.TCAspects.catalog().require("ignis");
            int capacity=2*storage.getNumberOfSlotRows()*8;
            require(tank.tankCapacity()==capacity,"Two occupied columns must hold 8 essentia per slot without stack upgrades");
            require(tank.insert(air, capacity+36, true) == capacity && tank.amount() == 0, "Simulation changed tank");
            require(tank.insert(air, capacity+36, false) == capacity && tank.insert(fire, 1, false) == 0, "Aspect mixing or overflow");
            require(tank.extract(fire, 10, false) == 0 && tank.extract(air, capacity-4, false) == capacity-4, "Wrong drain");
            var restored = new EssentiaTankUpgradeItem.Wrapper(storage, upgrade.copy(), ignored -> {});
            require(restored.amount() == 4 && restored.aspect().equals(air), "Tank persistence");
        });
        check("focus pouch returns to original slot", () -> {
            var upgrade = new ItemStack(ForgeRegistries.ITEMS.getValue(id("focus_pouch_upgrade")));
            var pouch = new FocusPouchUpgradeItem.Wrapper(storage, upgrade, ignored -> {});
            var focus = ForgeRegistries.ITEMS.getValues().stream().filter(item -> item instanceof tc4revival.content.WandFocusItem).findFirst().orElseThrow();
            var stack = new ItemStack(focus); pouch.set(5, stack); pouch.set(5, ItemStack.EMPTY);
            require(pouch.preferredSlot(stack) == 5 && !pouch.accepts(0, stack), "Original home not preferred");
            storage.getInventoryHandler().setStackInSlot(5, new ItemStack(net.minecraft.world.item.Items.STONE));
            require(pouch.preferredSlot(stack) == -1 && pouch.accepts(0, stack), "Occupied home blocks fallback");
        });
        check("Omniverse arcane upgrade grid persistence and wand filter", () -> {
            var upgrade = new ItemStack(ForgeRegistries.ITEMS.getValue(id("arcane_crafting_upgrade")));
            var grid = new ArcaneCraftingUpgradeItem.Wrapper(storage, upgrade, ignored -> {});
            grid.inventory().setStackInSlot(0, new ItemStack(net.minecraft.world.item.Items.STICK, 7));
            require(!grid.inventory().isItemValid(9, new ItemStack(net.minecraft.world.item.Items.STONE)), "Wand slot accepted stone");
            var restored = new ArcaneCraftingUpgradeItem.Wrapper(storage, upgrade.copy(), ignored -> {});
            require(restored.inventory().getStackInSlot(0).getCount() == 7, "Arcane grid NBT");
        });
    }
}
