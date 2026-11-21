package tc4revival.research;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Reviewed generation-time item migration for book displays; never creates or grants inventory items. */
public  class ResearchDisplayItems {
    public record Identity(String item, int metadata, Optional<String> nbt) {
        public static Identity of(ResearchDefinition.Stack stack) { return new Identity(stack.key().item(), stack.key().metadata(), stack.nbt()); }
        public static Identity decode(JsonObject row) {
            return new Identity(row.get("item").getAsString(), row.get("metadata").getAsInt(), ResearchPage.string(row, "nbt"));
        }
    }
    public record Variant(String item, Optional<String> tag) {}
    public record Entry(Identity identity, List<Variant> variants, boolean cycleDamage, boolean cycleCount, Optional<String> missing) {
        public Entry {
            variants = List.copyOf(variants);
            if (variants.isEmpty() != missing.isPresent() || cycleDamage && variants.size() != 1)
                throw new IllegalArgumentException("Invalid book display migration result");
        }
    }
    private final Map<Identity, Entry> entries;
    private ResearchDisplayItems(Map<Identity, Entry> entries) { this.entries = Map.copyOf(entries); }
    public Map<Identity, Entry> entries() { return entries; }
    public Optional<Entry> find(ResearchDefinition.Stack stack) { return Optional.ofNullable(entries.get(Identity.of(stack))); }
    /** Each call produces an independent stack; old wildcard cycling constructs a single-item display. */
    public Optional<ItemStack> resolve(ResearchDefinition.Stack original, long milliseconds) {
        var entry = find(original).orElse(null);
        if(entry==null) {
            var id=ResourceLocation.tryParse(original.key().item());
            if(id==null||!ForgeRegistries.ITEMS.containsKey(id))return Optional.empty();
            var direct=new ItemStack(ForgeRegistries.ITEMS.getValue(id),original.count());
            original.nbt().ifPresent(text->{try{direct.setTag(TagParser.parseTag(text));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalArgumentException("Invalid addon display NBT",e);}});
            return direct.isEmpty()?Optional.empty():Optional.of(direct);
        }
        if (entry.missing().isPresent()) return Optional.empty();
        var variant = entry.variants().get((int) Math.floorMod(milliseconds / 1000, entry.variants().size()));
        var id = ResourceLocation.parse(variant.item());
        if (!ForgeRegistries.ITEMS.containsKey(id)) throw new IllegalStateException("Missing registered display item " + id);
        var stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id), entry.cycleCount() ? 1 : original.count());
        if (stack.isEmpty()) throw new IllegalStateException("Book display resolved to air " + id);
        variant.tag().ifPresent(text -> {
            try { stack.setTag(TagParser.parseTag(text)); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException failure) { throw new IllegalArgumentException("Invalid modern display tag", failure); }
        });
        if (entry.cycleDamage()) {
            if (stack.getMaxDamage() < 1) throw new IllegalStateException("Non-damageable display cycle " + id);
            stack.setDamageValue((int) Math.floorMod(milliseconds / 10, stack.getMaxDamage()));
        }
        return Optional.of(stack);
    }
    public static ResearchDisplayItems decode(JsonObject root) {
        if (root.get("format").getAsInt() != 1) throw new IllegalArgumentException("Unsupported display item format");
        var entries = new LinkedHashMap<Identity, Entry>();
        for (var element : root.getAsJsonArray("entries")) {
            var row = element.getAsJsonObject(); var identity = Identity.decode(row.getAsJsonObject("legacy"));
            var variants = new java.util.ArrayList<Variant>();
            for (var value : row.getAsJsonArray("variants")) {
                var variant = value.getAsJsonObject();
                variants.add(new Variant(variant.get("item").getAsString(), ResearchPage.string(variant, "tag")));
            }
            var entry = new Entry(identity, variants, row.get("cycle_damage").getAsBoolean(), row.get("cycle_count").getAsBoolean(), ResearchPage.string(row, "missing"));
            if (entries.putIfAbsent(identity, entry) != null) throw new IllegalArgumentException("Duplicate book display identity");
        }
        return new ResearchDisplayItems(entries);
    }
    public static ResearchDisplayItems bundled() { return Bundled.VALUE; }
    private static  class Bundled {
        private static final ResearchDisplayItems VALUE = read();
        private static ResearchDisplayItems read() {
            try (var reader = new java.io.InputStreamReader(java.util.Objects.requireNonNull(ResearchDisplayItems.class
                    .getResourceAsStream("/data/thaumcraft/research_display_items/legacy.json")), java.nio.charset.StandardCharsets.UTF_8)) {
                return decode(com.google.gson.JsonParser.parseReader(reader).getAsJsonObject());
            } catch (java.io.IOException failure) { throw new IllegalStateException("Missing book display migration", failure); }
        }
    }
}
