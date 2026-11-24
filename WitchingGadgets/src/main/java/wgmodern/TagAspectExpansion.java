package wgmodern;

import com.google.gson.*;
import java.util.*;
import java.util.function.Function;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.core.Holder;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.common.Mod;
import java.util.function.Supplier;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

/** Expand modern tag registrations during reload or against the client's synchronized tags. */
@Mod.EventBusSubscriber(modid="witchinggadgets")
public class TagAspectExpansion {
    private static volatile Map<ResourceLocation, Collection<Holder<Item>>> serverItemTags;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void reload(net.minecraftforge.event.AddReloadListenerEvent event) {
        var context = event.getConditionContext();
        event.addListener(loadItemTags(() -> context.getAllTags(Registries.ITEM)));
    }

    static ResourceManagerReloadListener loadItemTags(Supplier<Map<ResourceLocation, Collection<Holder<Item>>>> next) {
        // Vanilla loads tags before addon listeners apply. Until then an in-flight calculation needs the previous snapshot.
        return resources -> serverItemTags = next.get();
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        serverItemTags = null;
    }
    public static void expand(JsonElement value) {
        if(!(value instanceof JsonObject root)||!root.has("witchinggadgets:tag_aspects"))return;
        var staged = serverItemTags;
        expand(value, id -> {
            var tag = ResourceLocation.parse(id);
            var ids=new TreeSet<String>();
            if(staged!=null) {
                for(var holder:staged.getOrDefault(tag, List.of()))holder.unwrapKey().ifPresent(k->ids.add(k.location().toString()));
            } else {
                // Local calculation starts after vanilla tag synchronization, including datapack tags.
                var tags=ForgeRegistries.ITEMS.tags();
                if(tags!=null)for(var item:tags.getTag(TagKey.create(Registries.ITEM, tag)))ids.add(ForgeRegistries.ITEMS.getKey(item).toString());
            }
            return ids;
        });
    }
    static void expand(JsonElement value,Function<String,Collection<String>> members) {
        if(!(value instanceof JsonObject root)||!root.has("witchinggadgets:tag_aspects"))return;
        var entries=root.getAsJsonArray("entries");
        for(var entry:root.getAsJsonArray("witchinggadgets:tag_aspects")) {
            var row=entry.getAsJsonObject();
            var ids=new TreeSet<>(members.apply(row.get("tag").getAsString()));
            if(ids.isEmpty())continue;
            var expanded=new JsonObject();var items=new JsonArray();ids.forEach(items::add);
            expanded.add("items",items);expanded.add("aspects",row.get("aspects").deepCopy());entries.add(expanded);
        }
        root.remove("witchinggadgets:tag_aspects");
    }
}
