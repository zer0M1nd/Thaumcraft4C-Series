package tc4revival.aspect.item;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.recipe.RecipeAspectData.Key;

public  class PotionAspects {
    public static final String LEGACY_METADATA_TAG = "tc4_legacy_potion_metadata";
    private static volatile PotionAspectData data;
    private PotionAspects() {}
    public static void addReloadListener(AddReloadListenerEvent event) { event.addListener(new Reload()); }
    public static void stopped(ServerStoppedEvent event) { data = null; }
    public static PotionAspectData data() {
        var current = data;
        if (current == null) throw new IllegalStateException("Potion aspect definitions have not loaded");
        return current;
    }
    public static AspectAmounts decorate(Key key, AspectAmounts amounts) {
        if (!key.item().equals("minecraft:potion")) return amounts;
        return new PotionAspectRules(data().definitions(), TCAspects.catalog()).decorate(key, amounts);
    }
    public static Optional<Key> key(ItemStack stack) {
        if((!stack.is(Items.POTION)&&!stack.is(Items.SPLASH_POTION))||!stack.hasTag())return Optional.empty();
        return key(stack,data());
    }
    public static Optional<Key> key(ItemStack stack,PotionAspectData current) {
        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION)) return Optional.empty();
        if (!stack.hasTag()) return Optional.empty();
        var potion = ResourceLocation.tryParse(stack.getTag().getString("Potion"));
        if (potion == null) return Optional.empty();
        var variant = new PotionAspectData.Variant(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString(), potion.toString());
        Integer preserved = stack.getTag().contains(LEGACY_METADATA_TAG, Tag.TAG_INT) ? stack.getTag().getInt(LEGACY_METADATA_TAG) : null;
        Integer metadata = current.metadata(variant, preserved);
        return metadata == null ? Optional.empty() : Optional.of(new Key("minecraft:potion", metadata));
    }
    private static  class Reload extends SimpleJsonResourceReloadListener {
        Reload() { super(new Gson(), "potion_aspects"); }
        @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            var id = new ResourceLocation(TC4Revival.MOD_ID, "legacy");
            if (files.size() != 1 || !files.containsKey(id)) throw new IllegalArgumentException("Expected one complete legacy potion definition");
            var candidate = PotionAspectJson.decode(files.get(id));
            for (var entry : candidate.represented().entrySet()) {
                var variant = entry.getValue();
                if (!variant.item().equals("minecraft:potion") && !variant.item().equals("minecraft:splash_potion")) throw new IllegalArgumentException("Unsupported potion item");
                if (!ForgeRegistries.POTIONS.containsKey(new ResourceLocation(variant.potion()))) throw new IllegalArgumentException("Unknown potion type");
                if (candidate.definitions().get(entry.getKey()).splash() != variant.item().equals("minecraft:splash_potion")) throw new IllegalArgumentException("Potion splash identity mismatch");
            }
            data = candidate;
            tc4revival.aspect.sync.ServerAspectData.capture("potion_aspects",files);
            TC4Revival.LOGGER.info("Loaded {} legacy potion definitions and {} standard modern potion variants", candidate.definitions().size(), candidate.defaults().size());
        }
    }
}
