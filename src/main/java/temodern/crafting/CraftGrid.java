package temodern.crafting;

import java.util.List;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

public final class CraftGrid extends SimpleContainer implements CraftingContainer {
    public CraftGrid() { super(9); }
    @Override public int getWidth() { return 3; }
    @Override public int getHeight() { return 3; }
    @Override public List<ItemStack> getItems() { return java.util.stream.IntStream.range(0,9).mapToObj(this::getItem).toList(); }
    @Override public void fillStackedContents(StackedContents contents) { getItems().forEach(contents::accountSimpleStack); }
}
