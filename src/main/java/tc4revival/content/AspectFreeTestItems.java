package tc4revival.content;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.item.ItemStackAspects;

/** Runtime test fixture: modern crafting inference may assign aspects to former empty examples. */
final class AspectFreeTestItems {
    private AspectFreeTestItems() {}

    static ItemStack create() {
        var candidates=java.util.stream.Stream.concat(
                java.util.stream.Stream.of(Items.STRUCTURE_VOID,Items.BARRIER,Items.LIGHT,Items.DEBUG_STICK),
                ForgeRegistries.ITEMS.getEntries().stream()
                        .filter(e->e.getKey().location().getNamespace().equals("minecraft"))
                        .sorted(java.util.Comparator.comparing(e->e.getKey().location().toString()))
                        .map(java.util.Map.Entry::getValue));
        var stack=candidates.distinct().map(ItemStack::new).filter(s->!s.isEmpty())
                .filter(s->ItemStackAspects.resolve(s).orElseGet(AspectAmounts::new).isEmpty())
                .findFirst().orElseThrow(()->new IllegalStateException("No aspect-free vanilla item available for behavior test"));
        tc4revival.TC4Revival.LOGGER.info("ASPECT_FREE_TEST_FIXTURE {}",ForgeRegistries.ITEMS.getKey(stack.getItem()));
        return stack;
    }
}
