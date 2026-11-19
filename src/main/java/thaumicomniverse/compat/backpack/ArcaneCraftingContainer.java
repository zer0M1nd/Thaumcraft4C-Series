package thaumicomniverse.compat.backpack;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.*;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;

/** Embeds the core workbench's slots and transaction rules in the native backpack tab. */
public final class ArcaneCraftingContainer extends UpgradeContainerBase<ArcaneCraftingUpgradeItem.Wrapper, ArcaneCraftingContainer>
        implements ICraftingContainer {
    public static final UpgradeContainerType<ArcaneCraftingUpgradeItem.Wrapper, ArcaneCraftingContainer> TYPE =
            new UpgradeContainerType<>(ArcaneCraftingContainer::new);
    private final ArcaneWorkbenchMenu workbench;
    private ItemStack shiftOutput = ItemStack.EMPTY;
    private List<ItemStack> shiftPlayer = List.of(), shiftStorage = List.of();
    public ArcaneCraftingContainer(Player player, int id, ArcaneCraftingUpgradeItem.Wrapper wrapper,
                                   UpgradeContainerType<ArcaneCraftingUpgradeItem.Wrapper, ArcaneCraftingContainer> type) {
        super(player, id, wrapper, type);
        var table = new ArcaneWorkbenchBlockEntity(BlockPos.ZERO, TCArcane.BLOCK.get().defaultBlockState()) {
            @Override public ItemStack getItem(int slot) { return slot == 9 ? ItemStack.EMPTY : getUpgradeWrapper().inventory().getStackInSlot(slot == 10 ? 9 : slot); }
            @Override public void setItem(int slot, ItemStack stack) {
                if (slot != 9) getUpgradeWrapper().inventory().setStackInSlot(slot == 10 ? 9 : slot, stack);
            }
            @Override public ItemStack removeItem(int slot, int count) {
                return slot == 9 ? ItemStack.EMPTY : getUpgradeWrapper().inventory().extractItem(slot == 10 ? 9 : slot, count, false);
            }
            @Override public ItemStack removeItemNoUpdate(int slot) { return removeItem(slot, Integer.MAX_VALUE); }
            @Override public void clearContent() { for (int i = 0; i < 10; i++) getUpgradeWrapper().inventory().setStackInSlot(i, ItemStack.EMPTY); }
            @Override public boolean isEmpty() {
                for (int i = 0; i < 10; i++) if (!getUpgradeWrapper().inventory().getStackInSlot(i).isEmpty()) return false;
                return true;
            }
            @Override public boolean stillValid(Player user) { return user == player; }
            @Override public void setChanged() { getUpgradeWrapper().persist(); }
        };
        workbench = new ArcaneWorkbenchMenu(-1, player.getInventory(), table) {
            @Override public boolean stillValid(Player user) { return user == player && getUpgradeWrapper().isEnabled(); }
        };
        // Keep the core's wand/grid slots; only the output needs the host menu's partial-take handling.
        var output = workbench.slots.get(0);
        slots.add(new Slot(output.container, output.getContainerSlot(), -100, -100) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player user) { return thaumicomniverse.RevivalFeatures.Group.BACKPACKS.enabled() && output.mayPickup(user); }
            @Override public ItemStack remove(int amount) { return output.remove(amount); }
            @Override public void onTake(Player user, ItemStack taken) {
                if (!player.level().isClientSide) {
                    var before = getRecipeSlots().stream().map(slot -> slot.getItem().copy()).toList();
                    var remainder = getItem().copy();
                    set(ItemStack.EMPTY);
                    output.onTake(user, taken);
                    if (getUpgradeWrapper().setting("refill")) refill(before);
                    if (!remainder.isEmpty()) {
                        if (!user.getInventory().add(remainder)) user.drop(remainder, false);
                    }
                }
            }
        });
        for (int i = 1; i < 11; i++) {
            var slot = workbench.slots.get(i);
            SlotPosition.move(slot, -100, -100); slots.add(slot);
        }
    }
    public void refresh() { workbench.refresh(); }
    public Map<Aspect, Integer> visCost() { return workbench.visCost(); }
    public ItemStack wand() { return workbench.wand(); }
    public ItemStack preview() { return workbench.preview(); }
    public boolean setting(String key) { return getUpgradeWrapper().setting(key); }
    public void setting(String key, boolean value) { getUpgradeWrapper().setting(key, value); sendBooleanToServer(key, value); }
    @Override public void onInit() { refresh(); }
    @Override public void handleMessage(CompoundTag data) {
        for (var key : List.of("intoStorage", "refill")) if (data.contains(key, 1)) setting(key, data.getBoolean(key));
        if (data.contains("rotate", 1)) rotate(data.getBoolean("rotate"));
        if (data.contains("clear", 1)) clearGrid();
    }
    public void rotate(boolean clockwise) {
        if (player.level().isClientSide) { sendDataToServer(() -> { var tag = new CompoundTag(); tag.putBoolean("rotate", clockwise); return tag; }); return; }
        var snapshot = getRecipeSlots().stream().map(slot -> slot.getItem().copy()).toList();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
            getRecipeSlots().get(clockwise ? col * 3 + 2 - row : (2 - col) * 3 + row).set(snapshot.get(row * 3 + col));
        refresh();
    }
    public void clearGrid() {
        if (player.level().isClientSide) { sendDataToServer(() -> { var tag = new CompoundTag(); tag.putBoolean("clear", true); return tag; }); return; }
        for (var slot : getRecipeSlots()) {
            var stack = slot.getItem().copy();
            if (setting("intoStorage")) stack = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(getUpgradeWrapper().storage(), stack, false);
            if (!stack.isEmpty()) player.getInventory().add(stack);
            slot.set(stack);
        }
        refresh();
    }
    private void refill(List<ItemStack> before) {
        var inventory = getUpgradeWrapper().storage();
        for (int cell = 0; cell < 9; cell++) {
            var old = before.get(cell); var slot = getRecipeSlots().get(cell); var current = slot.getItem();
            if (old.isEmpty() || !current.isEmpty() && !ItemStack.isSameItemSameTags(old, current)
                    || current.getCount() >= old.getCount()) continue;
            for (int source = 0; source < inventory.getSlots(); source++) {
                if (!ItemStack.isSameItemSameTags(old, inventory.getStackInSlot(source))) continue;
                var extracted = inventory.extractItem(source, 1, false);
                if (extracted.isEmpty()) continue;
                if (current.isEmpty()) slot.set(extracted);
                else { var next = current.copy(); next.grow(1); slot.set(next); }
                break;
            }
        }
    }
    @Override public boolean mergeIntoStorageFirst(Slot slot) { return setting("intoStorage"); }
    @Override public boolean allowsPickupAll(Slot slot) { return slot != slots.get(0); }
    @Override public ItemStack getSlotStackToTransfer(Slot slot) {
        refresh();
        if (slot == slots.get(0)) {
            if (!slot.mayPickup(player)) return ItemStack.EMPTY;
            shiftOutput = slot.getItem().copy();
            shiftPlayer = java.util.stream.IntStream.range(0, player.getInventory().getContainerSize())
                    .mapToObj(i -> player.getInventory().getItem(i).copy()).toList();
            var storage = getUpgradeWrapper().storage();
            shiftStorage = java.util.stream.IntStream.range(0, storage.getSlots()).mapToObj(i -> storage.getStackInSlot(i).copy()).toList();
        }
        return slot.getItem();
    }
    @Override public void onTakeFromSlot(Slot slot, Player user, ItemStack remaining) {
        if (slot == slots.get(0)) {
            // Storage transfers replace the slot with a remainder but pass the original
            // transfer stack to this callback. Read the committed slot in both routes.
            int count = shiftOutput.getCount() - slot.getItem().getCount();
            if (count > 0) {
                // The host has already transferred the result. Re-enter the core
                // result slot's removal transaction so vis is authorized exactly
                // once, just as for a normal click. A rejected payment rolls the
                // host transfer back rather than granting a free result.
                slot.set(shiftOutput.copy());
                var taken = slot.remove(count);
                if (taken.isEmpty()) {
                    for (int i = 0; i < shiftPlayer.size(); i++) player.getInventory().setItem(i, shiftPlayer.get(i));
                    for (int i = 0; i < shiftStorage.size(); i++) getUpgradeWrapper().storage().setStackInSlot(i, shiftStorage.get(i));
                    refresh();
                } else slot.onTake(user, taken);
            }
            shiftOutput = ItemStack.EMPTY; shiftPlayer = List.of(); shiftStorage = List.of();
        } else super.onTakeFromSlot(slot, user, remaining);
    }
    @Override public List<Slot> getRecipeSlots() { return slots.subList(2, 11); }
    @Override public Container getCraftMatrix() { return slots.get(2).container; }
    @Override public void setRecipeUsed(ResourceLocation recipeId) { refresh(); }
    @Override public RecipeType<?> getRecipeType() { return TCArcane.RECIPE_TYPE.get(); }
    @Override public boolean shouldRefillCraftingGrid() { return setting("refill"); }
}
