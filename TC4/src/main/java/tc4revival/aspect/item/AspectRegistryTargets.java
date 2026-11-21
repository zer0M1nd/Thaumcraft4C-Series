package tc4revival.aspect.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Shared registry checks for server reloads and the synchronized client snapshot. */
public class AspectRegistryTargets {
    public static ItemTagLookup itemTags() {
        return new ItemTagLookup() {
            @Override public java.util.Collection<String> tags(String id) {
                var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));
                return item==null?java.util.List.of():item.builtInRegistryHolder().tags().map(tag->tag.location().toString()).toList();
            }
            @Override public boolean test(String id,String tag) { return itemTag(id,tag); }
        };
    }
    public static boolean itemTag(String id,String tag) {
        var item=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id));
        return item!=null&&item.builtInRegistryHolder().is(net.minecraft.tags.ItemTags.create(ResourceLocation.parse(tag)));
    }
    public static boolean item(String id) {
        return !id.equals("minecraft:air") && ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse(id));
    }
    public static boolean block(String id) {
        return ForgeRegistries.BLOCKS.containsKey(ResourceLocation.parse(id));
    }
    public static boolean editor(String kind,String id) {
        return switch(kind) {
            case "item" -> item(id);
            case "block" -> block(id);
            case "entity" -> ForgeRegistries.ENTITY_TYPES.containsKey(ResourceLocation.parse(id));
            default -> true; // Empty or currently absent tags simply match nothing.
        };
    }
}
