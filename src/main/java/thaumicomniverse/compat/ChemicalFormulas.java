package thaumicomniverse.compat;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

public class ChemicalFormulas {
    private static final Map<ResourceLocation, String> FORMULAS = load();
    private static Map<ResourceLocation, String> load() {
        var result = new HashMap<ResourceLocation, String>();
        try (var stream = ChemicalFormulas.class.getResourceAsStream("/data/thaumicomniverse/chemical_formulas/thaumcraft.json")) {
            if (stream != null) try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("formulas").entrySet()
                        .forEach(entry -> result.put(ResourceLocation.parse(entry.getKey()), entry.getValue().getAsString()));
            }
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        return Map.copyOf(result);
    }
    public static String formula(ResourceLocation item) { return FORMULAS.get(item); }
}
