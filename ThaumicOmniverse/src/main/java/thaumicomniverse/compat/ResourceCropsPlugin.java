package thaumicomniverse.compat;

import com.blakebr0.mysticalagriculture.api.IMysticalAgriculturePlugin;
import com.blakebr0.mysticalagriculture.api.MysticalAgriculturePlugin;
import com.blakebr0.mysticalagriculture.api.crop.*;
import com.blakebr0.mysticalagriculture.api.lib.LazyIngredient;
import com.blakebr0.mysticalagriculture.api.registry.ICropRegistry;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

@MysticalAgriculturePlugin
public final class ResourceCropsPlugin implements IMysticalAgriculturePlugin {
    @Override public void configure(com.blakebr0.mysticalagriculture.api.lib.PluginConfig config) {
        config.setModId("thaumicomniverse");
    }
    @Override public void onPostRegisterCrops(ICropRegistry registry) {
        // All plugins have registered their tiers by this phase, regardless of discovery order.
        if (!ModList.get().isLoaded("mysticalagradditions")) return;
        var crop = registry.getCropByName("omniverse_primordial_pearl");
        var tier = registry.getTierById(ResourceLocation.parse("mysticalagradditions:6"));
        if (crop != null && tier != null) crop.setTier(tier);
    }
    @Override public void onRegisterCrops(ICropRegistry registry) {
        try (var reader = new InputStreamReader(java.util.Objects.requireNonNull(getClass().getResourceAsStream(
                "/thaumicomniverse/renewables.json")), StandardCharsets.UTF_8)) {
            for (var entry : JsonParser.parseReader(reader).getAsJsonArray()) {
                var row = entry.getAsJsonObject();
                if (!ModList.get().isLoaded(row.get("mod").getAsString())) continue;
                String name = "omniverse_" + row.get("name").getAsString();
                if (registry.getCropByName(name) != null) {
                    throw new IllegalStateException("Resource crop name already registered: " + name);
                }
                CropTier tier = switch (row.get("tier").getAsInt()) {
                    case 2 -> CropTier.TWO;
                    case 3 -> CropTier.THREE;
                    case 4 -> CropTier.FOUR;
                    case 5 -> CropTier.FIVE;
                    default -> throw new IllegalArgumentException("Unsupported resource crop tier");
                };
                var texture = new CropTextures(CropTextures.FLOWER_INGOT_BLANK, CropTextures.ESSENCE_GEM_BLANK);
                Crop crop = new Crop(ResourceLocation.fromNamespaceAndPath("thaumicomniverse", name), tier,
                        CropType.RESOURCE, texture, row.get("color").getAsInt(), LazyIngredient.item(row.get("item").getAsString())) {
                    @Override public boolean isEnabled() {
                        return super.isEnabled() && (!thaumicomniverse.RevivalFeatures.CONFIG.loaded() || thaumicomniverse.RevivalFeatures.Group.CROPS.enabled());
                    }
                };
                if (tier == CropTier.FIVE) crop.setBaseSecondaryChance(0);
                if (row.has("fragment")) crop.setHasEffect(true);
                registry.register(crop);
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read Omniverse crop definitions", exception);
        }
    }
}
