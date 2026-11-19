package tc4revival.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

/** Optional legacy silver content; never adds a duplicate silver material. */
public final class SilverCaps {
    public static final TagKey<Item> INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.parse("forge:ingots/silver"));
    public static final TagKey<Item> NUGGETS = TagKey.create(Registries.ITEM, ResourceLocation.parse("forge:nuggets/silver"));
    public static boolean available() {
        var tags = ForgeRegistries.ITEMS.tags();
        return tags != null && (!tags.getTag(INGOTS).isEmpty() || !tags.getTag(NUGGETS).isEmpty());
    }
    public static boolean visible(Item item) {
        var id = ForgeRegistries.ITEMS.getKey(item);
        return id == null || !id.getNamespace().equals("thaumcraft")
                || !java.util.Set.of("wand_cap_silver", "wand_cap_silver_inert").contains(id.getPath()) || available();
    }
    private SilverCaps() {}
}
