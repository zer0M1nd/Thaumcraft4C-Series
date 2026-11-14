package arcane_engineering;

import blusunrize.immersiveengineering.api.tool.IUpgradeableTool;
import blusunrize.immersiveengineering.api.tool.IUpgrade;
import blusunrize.immersiveengineering.common.gui.IESlot;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import java.util.function.Supplier;

public class UpgradeableWandItem extends CastingWandItem implements IUpgradeableTool {
    public int slots(ItemStack stack) { return cap(stack).tag.equals(ArcaneEngineering.ID+":steel") ? 4 : cap(stack).tag.equals(ArcaneEngineering.ID+":electrum") ? 3 : 2; }
    @Override public WandParts.Rod rod(ItemStack stack) { return WandParts.Rod.require(ArcaneEngineering.ID+":upgradeable"); }
    @Override public boolean isSceptre(ItemStack stack) { return false; }
    public static int upgradedCapacity(int count) { return (int)(3000 * Math.pow(1.5, Math.max(0, Math.min(4, count)))); }
    @Override public int capacity(ItemStack stack) { return upgradedCapacity(getUpgrades(stack).getInt("capacitors")); }
    @Override public float focusPotency(ItemStack stack, Player player) { return super.focusPotency(stack, player)+getUpgrades(stack).getInt("energizers"); }
    @Override public float consumptionModifier(ItemStack stack, Player player, Aspect aspect, boolean crafting) { return super.consumptionModifier(stack, player, aspect, crafting)+.1f*getUpgrades(stack).getInt("energizers"); }
    @Override public CompoundTag getUpgrades(ItemStack stack) {
        var modifications = new CompoundTag();
        stack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(inv -> {
            for (int i=0; i<Math.min(slots(stack),inv.getSlots()); i++) {
                var upgrade=inv.getStackInSlot(i);
                if (!upgrade.isEmpty() && upgrade.getItem() instanceof IUpgrade item && item.getUpgradeTypes(upgrade).contains("WAND") && item.canApplyUpgrades(stack, upgrade)) item.applyUpgrades(stack, upgrade, modifications);
            }
        });
        for (String key : new String[]{"capacitors","chargers","energizers"}) modifications.putInt(key, Math.max(0,Math.min(slots(stack),modifications.getInt(key))));
        return modifications;
    }
    @Override public void clearUpgrades(ItemStack stack) { if (stack.hasTag()) stack.getTag().remove("upgrades"); }
    @Override public void finishUpgradeRecalculation(ItemStack stack) {
        if (stack.hasTag()) for (var aspect : TCAspects.catalog().primals()) if (stack.getTag().getInt(aspect.tag())>capacity(stack)) stack.getTag().putInt(aspect.tag(), capacity(stack));
    }
    @Override public void recalculateUpgrades(ItemStack stack, Level level, Player player) {
        if (level != null && level.isClientSide) return;
        // IE shift-click mutates the stack returned by the slot before calling this hook.
        stack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(inv -> { if(inv instanceof WandInventory owned) owned.flush(); });
        stack.getOrCreateTag().put("upgrades", getUpgrades(stack)); finishUpgradeRecalculation(stack);
    }
    @Override public boolean canTakeFromWorkbench(ItemStack stack) { return true; }
    @Override public boolean canModify(ItemStack stack) { return true; }
    @Override public void removeFromWorkbench(Player player, ItemStack stack) { recalculateUpgrades(stack, player.level(), player); }
    @Override public Slot[] getWorkbenchSlots(AbstractContainerMenu menu, ItemStack stack, Level level, Supplier<Player> player, IItemHandler inventory) {
        Slot[] result = new Slot[slots(stack)];
        for (int i=0;i<result.length;i++) result[i] = new IESlot.Upgrades(menu, inventory, i, 80+i*20, 32, "WAND", stack, false, level, player);
        return result;
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        int chargers = Math.max(0, Math.min(4, getUpgrades(stack).getInt("chargers")));
        if (!level.isClientSide && chargers>0 && entity.tickCount % (80/chargers)==0)
            for (var aspect : TCAspects.catalog().primals()) if (vis(stack,aspect)<capacity(stack)) { addVis(stack,aspect,1,true,true); break; }
    }
    @Override public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) { return new InventoryCapability(stack); }
    public static class InventoryCapability implements ICapabilityProvider {
        private final LazyOptional<IItemHandler> inventory;
        public InventoryCapability(ItemStack owner) {
            inventory=LazyOptional.of(() -> new WandInventory(owner));
        }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) { return capability==ForgeCapabilities.ITEM_HANDLER ? inventory.cast() : LazyOptional.empty(); }
    }
    private static class WandInventory extends ItemStackHandler {
        private final ItemStack owner;
        private CompoundTag lastLoaded;
        WandInventory(ItemStack owner) { super(4); this.owner=owner; }
        private void read() {
            var saved=owner.hasTag()?owner.getTag().getCompound("ae_inventory"):new CompoundTag();
            if(lastLoaded!=null && lastLoaded.equals(saved))return;
            lastLoaded=saved.copy();var normalized=saved.copy();normalized.putInt("Size",4);
            super.deserializeNBT(normalized);
        }
        void flush() {
            read();var saved=super.serializeNBT();
            owner.getOrCreateTag().put("ae_inventory",saved);lastLoaded=saved.copy();
        }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack value) { return slot<((UpgradeableWandItem)owner.getItem()).slots(owner) && value.getItem() instanceof IUpgrade upgrade && upgrade.getUpgradeTypes(value).contains("WAND") && upgrade.canApplyUpgrades(owner,value); }
        @Override public ItemStack getStackInSlot(int slot) { read(); return super.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack value, boolean simulate) { read(); return super.insertItem(slot,value,simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { read(); return super.extractItem(slot,amount,simulate); }
        @Override public void setStackInSlot(int slot, ItemStack value) { read(); super.setStackInSlot(slot,value); }
        @Override protected void onContentsChanged(int slot) { flush();((UpgradeableWandItem)owner.getItem()).recalculateUpgrades(owner,null,null); }
    }
}
