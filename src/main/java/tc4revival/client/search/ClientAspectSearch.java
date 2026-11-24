package tc4revival.client.search;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import tc4revival.TC4Revival;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;
import tc4revival.client.ClientItemAspects;

/** Client-thread snapshots. Changing the server data or the active language invalidates both viewers. */
public final class ClientAspectSearch {
    private static int version = -1;
    private static Language language;
    private static long epoch;
    private static AspectNames names;
    private static final Cache<String, Predicate<AspectExpression.Values>> queries =
            CacheBuilder.newBuilder().maximumSize(64).build();
    private static final Cache<String, AspectSearchQuery> searches =
            CacheBuilder.newBuilder().maximumSize(64).build();
    // Many JEI subtypes differ only in NBT that the aspect resolver does not read.
    private static final Cache<CompoundTag, AspectExpression.Values> recentProfiles =
            CacheBuilder.newBuilder().maximumSize(256).build();

    public static long epoch() {
        if (version != ClientItemAspects.version() || language != Language.getInstance()) {
            version = ClientItemAspects.version();
            language = Language.getInstance();
            names = null;
            queries.invalidateAll();
            searches.invalidateAll();
            recentProfiles.invalidateAll();
            epoch++;
        }
        return epoch;
    }

    public static AspectSearchQuery parseSearch(String input) {
        epoch();
        if (!ClientItemAspects.ready()) return AspectSearchQuery.INVALID;
        var cached = searches.getIfPresent(input);
        if (cached != null) return cached;
        var result = AspectSearchQuery.compile(input, ClientAspectSearch::query);
        searches.put(input, result);
        return result;
    }

    public static Predicate<AspectExpression.Values> query(String input) {
        epoch();
        if (!ClientItemAspects.ready()) return AspectExpression.INVALID;
        var cached = queries.getIfPresent(input);
        if (cached != null) return cached;
        var result = AspectExpression.compile(input, term -> {
            if (names == null) names = loadNames();
            return names.find(term);
        });
        queries.put(input, result);
        return result;
    }

    public static AspectExpression.Values values(ItemStack stack) {
        if (stack.isEmpty() || !ClientItemAspects.ready()) return null;
        var key = ClientItemAspects.key(stack);
        var values = recentProfiles.getIfPresent(key);
        if (values != null) return values;
        values = ClientItemAspects.resolve(stack, key).map(r -> AspectExpression.Values.of(r.amounts())).orElse(null);
        if (values != null) recentProfiles.put(key, values);
        return values;
    }

    private static AspectNames loadNames() {
        var aspects = TCAspects.catalog().all();
        var dictionary = JecPronunciation.Dictionary.open();
        Function<Aspect, String> translated = a -> language.getOrDefault(a.descriptionKey(), "");
        Function<Aspect, String> chinese = a -> "";
        if (dictionary != null) {
            chinese = Minecraft.getInstance().getLanguageManager().getSelected().equals("zh_cn")
                    ? translated : chineseNames(aspects);
        }
        return new AspectNames(aspects, translated, chinese, dictionary == null ? s -> null : dictionary::spell);
    }

    private static Function<Aspect, String> chineseNames(List<Aspect> aspects) {
        var keys = new HashSet<String>();
        aspects.forEach(aspect -> keys.add(aspect.descriptionKey()));
        var chinese = new HashMap<String, String>();
        var resources = Minecraft.getInstance().getResourceManager();
        for (String namespace : resources.getNamespaces()) {
            for (var resource : resources.getResourceStack(ResourceLocation.fromNamespaceAndPath(namespace, "lang/zh_cn.json"))) {
                try (var stream = resource.open()) {
                    Language.loadFromJson(stream, (key, value) -> {
                        if (keys.contains(key)) chinese.put(key, value);
                    });
                } catch (IOException | com.google.gson.JsonParseException error) {
                    TC4Revival.LOGGER.warn("Could not read Chinese aspect names from {}", resource.sourcePackId(), error);
                }
            }
        }
        return a -> chinese.getOrDefault(a.descriptionKey(), "");
    }

    private ClientAspectSearch() {}
}
