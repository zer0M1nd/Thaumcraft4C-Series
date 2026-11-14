package forgottenrelics;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.*;

import java.util.*;

/**
 * One stone and at most one sacrificial sample. Registry IDs survive registration-order changes.
 */
public class OblivionRecipe extends CustomRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ForgottenRelics.ID);
    public static final RegistryObject<SimpleCraftingRecipeSerializer<OblivionRecipe>> TYPE =
            SERIALIZERS.register(
                    "oblivion_binding",
                    () -> new SimpleCraftingRecipeSerializer<>(OblivionRecipe::new));

    public OblivionRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    public static ListTag filters(ItemStack stone) {
        return stone.getOrCreateTag().getList("filters", Tag.TAG_STRING);
    }

    public static String identity(ItemStack stack) {
        String id = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        // Potion metadata from 1.7.10 became Potion NBT in modern Minecraft. Keep base potion
        // variants distinct.
        return stack.getItem() instanceof PotionItem
                ? id
                        + "|"
                        + ForgeRegistries.POTIONS.getKey(
                                net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack))
                : id;
    }

    private ItemStack result(CraftingContainer grid) {
        ItemStack stone = ItemStack.EMPTY, sample = ItemStack.EMPTY;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            var s = grid.getItem(i);
            if (s.isEmpty()) continue;
            if (Relics.is(s, Relic.OBLIVION_STONE)) {
                if (!stone.isEmpty()) return ItemStack.EMPTY;
                stone = s;
            } else {
                if (!sample.isEmpty()) return ItemStack.EMPTY;
                sample = s;
            }
        }
        if (stone.isEmpty()) return ItemStack.EMPTY;
        if (sample.isEmpty()) {
            var cleared = stone.copyWithCount(1);
            cleared.getOrCreateTag().remove("filters");
            return cleared;
        }
        var list = filters(stone).copy();
        String id = identity(sample);
        if (list.size() >= Math.min(4096, RelicsConfig.integer("oblivionStoneHardCap")))
            return ItemStack.EMPTY;
        for (int i = 0; i < list.size(); i++)
            if (list.getString(i).equals(id)) return ItemStack.EMPTY;
        list.add(StringTag.valueOf(id));
        var result = stone.copyWithCount(1);
        result.getOrCreateTag().put("filters", list);
        return result;
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        return !result(grid).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        return result(grid);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
        var remaining = NonNullList.withSize(grid.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < grid.getContainerSize(); i++)
            if (grid.getItem(i).hasCraftingRemainingItem())
                remaining.set(i, grid.getItem(i).getCraftingRemainingItem());
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TYPE.get();
    }

    public static void tick(ItemStack stone, ServerPlayer player) {
        if (player.tickCount % 10 != 0 || stone.getOrCreateTag().getBoolean("disabled")) return;
        var list = filters(stone);
        var inventory = player.getInventory().items;
        int mode = Math.floorMod(stone.getOrCreateTag().getInt("mode"), 3);
        boolean full = inventory.stream().noneMatch(ItemStack::isEmpty);
        boolean changed = false;
        if (mode == 2 && !full) return;
        for (int f = 0; f < Math.min(list.size(), 4096); f++) {
            String id = list.getString(f);
            int largest = -1, smallest = -1;
            for (int i = 0; i < inventory.size(); i++) {
                var s = inventory.get(i);
                if (s.isEmpty() || Relics.is(s, Relic.OBLIVION_STONE) || !identity(s).equals(id))
                    continue;
                if (largest < 0 || s.getCount() > inventory.get(largest).getCount()) largest = i;
                if (smallest < 0 || s.getCount() < inventory.get(smallest).getCount()) smallest = i;
            }
            if (mode == 2 && smallest >= 0) {
                inventory.set(smallest, ItemStack.EMPTY);
                changed = true;
                break;
            }
            for (int i = 0; i < inventory.size(); i++) {
                var s = inventory.get(i);
                if (!s.isEmpty()
                        && !Relics.is(s, Relic.OBLIVION_STONE)
                        && identity(s).equals(id)
                        && (mode == 0 || i != largest)) {
                    inventory.set(i, ItemStack.EMPTY);
                    changed = true;
                }
            }
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    public static void tooltip(ItemStack stack, List<Component> text) {
        text.add(
                Component.translatable(
                        "tooltip.forgottenrelics.void_mode",
                        Component.translatable(
                                "tooltip.forgottenrelics.void_mode."
                                        + Math.floorMod(stack.getOrCreateTag().getInt("mode"), 3)),
                        Component.translatable(
                                stack.getOrCreateTag().getBoolean("disabled")
                                        ? "tooltip.forgottenrelics.off"
                                        : "tooltip.forgottenrelics.on")));
        var list = filters(stack);
        int cap = Math.min(list.size(), Math.min(64, RelicsConfig.integer("oblivionStoneSoftCap")));
        for (int i = 0; i < cap; i++) {
            var parts = list.getString(i).split("\\|", 2);
            var id = ResourceLocation.tryParse(parts[0]);
            var item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
            if (item == null) {
                text.add(Component.literal(list.getString(i)));
                continue;
            }
            var sample = new ItemStack(item);
            if (parts.length == 2) {
                var potion = ResourceLocation.tryParse(parts[1]);
                if (potion != null && ForgeRegistries.POTIONS.containsKey(potion))
                    net.minecraft.world.item.alchemy.PotionUtils.setPotion(
                            sample, ForgeRegistries.POTIONS.getValue(potion));
            }
            text.add(sample.getHoverName());
        }
        if (cap < list.size())
            text.add(
                    Component.translatable(
                            "tooltip.forgottenrelics.more_filters", list.size() - cap));
    }
}
