package thmodern.vat;
import tc4revival.content.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import tc4revival.aspect.Aspect;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge;

public final class MorphicFingersMenu extends AbstractContainerMenu {
    private final Player player;
    private final Container contents;

    private final ResultContainer result = new ResultContainer();
    private final Grid grid = new Grid();
    private boolean updating, crafting;
    private CraftingRecipe normal;
    private ArcaneRecipe arcane;
    private ItemStack preview = ItemStack.EMPTY;
    private Map<Aspect, Integer> visCost = Map.of();
    public MorphicFingersMenu(int id, Inventory inventory, FriendlyByteBuf buffer) { this(id,inventory); }
    public MorphicFingersMenu(int id, Inventory inventory) {
        super(thmodern.Horizons.FINGERS_MENU.get(),id);player=inventory.player;contents=new SimpleContainer(11);
        addSlot(new Slot(result, 0, 160, 64) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return !getItem().isEmpty() && stillValid(player); }
            @Override public void onTake(Player player, ItemStack stack) { finishCraft(stack); }
        });
        addSlot(new Slot(contents, 10, 160, 24) {
            @Override public boolean mayPlace(ItemStack stack) { return ArcaneWorkbenchBlockEntity.validWand(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) addSlot(new Slot(grid, col + row * 3, 40 + col * 24, 40 + row * 24));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, 9 + row * 9 + col, 16 + col * 18, 151 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 16 + col * 18, 209));
        refresh();
    }
    private Set<String> knowledge() {
        return player instanceof ServerPlayer server ? KnowledgeSavedData.get(server.getServer()).snapshot(player.getUUID()).map(PlayerKnowledge::researchMarkers).orElse(Set.of())
                : tc4revival.client.ClientKnowledge.snapshot().map(PlayerKnowledge::researchMarkers).orElse(Set.of());
    }
    public void refresh() {
        if (updating || crafting) return;
        updating = true;
        try {
            var level = player.level(); var known = knowledge();
            normal = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElse(null);
            arcane = level.getRecipeManager().getAllRecipesFor(TCArcane.RECIPE_TYPE.get()).stream()
                    .filter(recipe -> recipe.matches(grid, level) && recipe.unlocked(grid, known::contains)).findFirst().orElse(null);
            visCost = arcane == null ? Map.of() : arcane.visCost(grid);
            preview = arcane == null ? ItemStack.EMPTY : arcane.assemble(grid, level.registryAccess());
            // Only the server owns the takeable output; clients may calculate a grey preview.
            if (!level.isClientSide) {
                ItemStack next = normal != null ? normal.assemble(grid, level.registryAccess())
                        : canPay(false) ? preview.copy() : ItemStack.EMPTY;
                if (!ItemStack.matches(result.getItem(0), next)) result.setItem(0, next);
            }
        } finally { updating = false; }
    }
    public ItemStack wand() { return contents.getItem(10); }
    public ItemStack preview() { return preview.copy(); }
    public Map<Aspect, Integer> visCost() { return visCost; }
    private boolean canPay(boolean commit) {
        return !preview.isEmpty() && wand().getItem() instanceof CastingWandItem wand
                && wand.consumeForPlayer(wand(), player, visCost, true, true, commit);
    }
    private void finishCraft(ItemStack output) {
        if (player.level().isClientSide) return;
        crafting = true;
        try {
            // The old result slot also charged a matching arcane recipe when an ordinary recipe won priority.
            if (!visCost.isEmpty() && wand().getItem() instanceof CastingWandItem wand)
                wand.consumeForPlayer(wand(), player, visCost, true, true, true);
            net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(player, output, grid);
            output.onCraftedBy(player.level(), player, output.getCount());
            var remainders = normal == null ? arcane.getRemainingItems(grid) : normal.getRemainingItems(grid);
            for (int slot = 0; slot < 9; slot++) {
                // Old ItemCraftedEvent restored these catalysts before the result slot's debit.
                // Keep the original stack in its cell, including NBT, rather than moving it to inventory.
                if (output.is(TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get()) && slot == 4
                        || output.is(TCItems.JAR_LABEL.get()) && output.hasTag()
                        && grid.getItem(slot).getItem() instanceof EssentiaPhialItem) continue;
                if (!grid.getItem(slot).isEmpty()) grid.removeItem(slot, 1);
                var remainder = remainders.get(slot);
                if (remainder.isEmpty()) continue;
                if (remainder.isDamageableItem() && remainder.getDamageValue() > remainder.getMaxDamage()) {
                    net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, remainder, null); continue;
                }
                // Old container items try the player's inventory before an empty crafting cell.
                if (player.getInventory().add(remainder)) continue;
                if (grid.getItem(slot).isEmpty()) grid.setItem(slot, remainder); else player.drop(remainder, false);
            }
            contents.setChanged();
        } finally { crafting = false; }
        refresh();
    }
    @Override public void slotsChanged(Container changed) { refresh(); }
    @Override public void broadcastChanges() { refresh(); super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) { return player==this.player&&player.isAlive()&&!player.isSpectator()&&(player.level().isClientSide||SelfInfusions.has(player,"morphic_fingers")); }
    @Override public void removed(Player player) {
        super.removed(player);
        if(!player.level().isClientSide){crafting=true;try{result.clearContent();clearContainer(player,contents);}finally{crafting=false;}}
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player)) return;
        refresh(); // Recompute against shared inventory before using a viewer's potentially stale output.
        if (type == ClickType.THROW) button = 1;
        else if ((slot == 0 || slot == 1) && button > 0) button = 0;
        super.clicked(slot, button, type, player);
        refresh();
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.container != grid && slot.container != contents && slot.container != result && super.canTakeItemForPickAll(stack, slot); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        boolean moved;
        if (index == 0) moved = moveItemStackTo(stack, 11, 47, true);
        else if (index < 11) moved = moveItemStackTo(stack, 11, 47, false);
        else if (ArcaneWorkbenchBlockEntity.validWand(stack)) moved = moveItemStackTo(stack, 1, 2, false);
        else moved = index < 38 ? moveItemStackTo(stack, 38, 47, false) : moveItemStackTo(stack, 11, 38, false);
        if (!moved) return ItemStack.EMPTY;
        int amount = original.getCount() - stack.getCount();
        var leftoverOutput = index == 0 ? stack.copy() : ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        var taken = original.copy(); taken.setCount(amount); slot.onTake(player, taken);
        if (!leftoverOutput.isEmpty()) player.drop(leftoverOutput, false);
        return original;
    }
    private final class Grid implements CraftingContainer {
        @Override public int getWidth() { return 3; }
        @Override public int getHeight() { return 3; }
        @Override public int getContainerSize() { return 9; }
        @Override public List<ItemStack> getItems() { return java.util.stream.IntStream.range(0, 9).mapToObj(this::getItem).toList(); }
        @Override public boolean isEmpty() { return getItems().stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int slot) { return contents.getItem(slot); }
        @Override public ItemStack removeItem(int slot, int count) { var stack = contents.removeItem(slot, count); setChanged(); return stack; }
        @Override public ItemStack removeItemNoUpdate(int slot) { return contents.removeItemNoUpdate(slot); }
        @Override public void setItem(int slot, ItemStack stack) { contents.setItem(slot, stack); setChanged(); }
        @Override public void setChanged() { contents.setChanged(); slotsChanged(this); }
        @Override public boolean stillValid(Player player) { return contents.stillValid(player); }
        @Override public void clearContent() { for (int i = 0; i < 9; i++) setItem(i, ItemStack.EMPTY); }
        @Override public void fillStackedContents(StackedContents target) { getItems().forEach(target::accountSimpleStack); }
    }
}
