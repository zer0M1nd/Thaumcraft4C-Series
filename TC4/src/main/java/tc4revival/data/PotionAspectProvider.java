package tc4revival.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.TC4Revival;
import tc4revival.aspect.item.PotionAspectData;
import tc4revival.aspect.item.PotionAspectJson;
import tc4revival.aspect.item.PotionAspectRules;

public  class PotionAspectProvider implements DataProvider {
    private final PackOutput output;
    public PotionAspectProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        try {
            var raw = JsonParser.parseString(Files.readString(Path.of(System.getProperty("tc4revival.legacyBaseline")).resolve("potion-aspects.json"))).getAsJsonArray();
            var definitions = PotionAspectJson.definitions(raw);
            var defaults = new LinkedHashMap<PotionAspectData.Variant, Integer>();
            var represented = new LinkedHashMap<Integer, PotionAspectData.Variant>();
            definitions.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                int metadata = entry.getKey();
                if (metadata == 32767) return;
                var stack = new CompoundTag();
                stack.putString("id", "minecraft:potion");
                stack.putShort("Damage", (short) metadata);
                stack.putByte("Count", (byte) 1);
                // ItemPotionFix only visits the nested tag when it exists; later water fallback loses Damage.
                stack.put("tag", new CompoundTag());
                var fixed = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, stack),
                        99, SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                String itemId = fixed.get("id").asString("");
                String potionId = fixed.get("tag").get("Potion").asString("");
                if (!itemId.equals("minecraft:potion") && !itemId.equals("minecraft:splash_potion")) return;
                var id = ResourceLocation.tryParse(potionId);
                if (id == null || !ForgeRegistries.POTIONS.containsKey(id)) return;
                var effects = ForgeRegistries.POTIONS.getValue(id).getEffects();
                var old = entry.getValue().effects();
                if (effects.size() != old.size()) return;
                for (int i = 0; i < effects.size(); i++) {
                    if (!ForgeRegistries.MOB_EFFECTS.getKey(effects.get(i).getEffect()).toString().equals(PotionAspectRules.modernEffect(old.get(i).id()))
                            || effects.get(i).getAmplifier() != old.get(i).amplifier()) return;
                }
                var variant = new PotionAspectData.Variant(itemId, potionId);
                represented.put(metadata, variant);
                boolean plain = Set.of(0, 16, 32, 64, 16384, 16400, 16416, 16448).contains(metadata);
                boolean regular = !old.isEmpty() && ((metadata >= 8192 && metadata < 8320) || (metadata >= 16384 && metadata < 16512))
                        && (metadata & 16) == 0 && (metadata & 96) != 96;
                if (plain || regular) defaults.putIfAbsent(variant, metadata);
            });
            var data = new PotionAspectData(definitions, defaults, represented);
            if (defaults.size() < 40) throw new IllegalStateException("Incomplete standard legacy potion conversion");
            TC4Revival.LOGGER.info("Generated {} old potion definitions, {} standard variants and {} represented metadata values",
                    definitions.size(), defaults.size(), represented.size());
            return DataProvider.saveStable(cache, PotionAspectJson.encode(data), output.getOutputFolder(PackOutput.Target.DATA_PACK)
                    .resolve("thaumcraft/potion_aspects/legacy.json"));
        } catch (IOException failure) { return CompletableFuture.failedFuture(failure); }
    }
    @Override public String getName() { return "TC4 legacy potion identities and effect inputs"; }
}
