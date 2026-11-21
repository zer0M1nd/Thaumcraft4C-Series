package tc4revival.content;

import com.google.gson.*;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Uniform 1..(base + fortune), distinct from vanilla's multiplicative ore bonus. */
public record LegacyOreCount(int base) implements NumberProvider {
    private static final DeferredRegister<LootNumberProviderType> TYPES = DeferredRegister.create(Registries.LOOT_NUMBER_PROVIDER_TYPE, "thaumcraft");
    private static final RegistryObject<LootNumberProviderType> TYPE = TYPES.register("legacy_ore_count", () -> new LootNumberProviderType(new Serializer()));
    public static void register(IEventBus bus) { TYPES.register(bus); }
    public LegacyOreCount {
        if (base < 1 || base > 2) throw new IllegalArgumentException("Legacy ore base must be 1 or 2");
    }
    @Override public int getInt(LootContext context) {
        ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
        int fortune = tool == null ? 0 : Math.max(0, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool));
        // TC4's block used the world random stream for this draw.
        return 1 + context.getLevel().random.nextInt(base + fortune);
    }
    @Override public float getFloat(LootContext context) { return getInt(context); }
    @Override public LootNumberProviderType getType() { return TYPE.get(); }
    @Override public Set<LootContextParam<?>> getReferencedContextParams() { return Set.of(LootContextParams.TOOL); }
    public static  class Serializer implements net.minecraft.world.level.storage.loot.Serializer<LegacyOreCount> {
        @Override public void serialize(JsonObject json, LegacyOreCount value, JsonSerializationContext context) { json.addProperty("base", value.base()); }
        @Override public LegacyOreCount deserialize(JsonObject json, JsonDeserializationContext context) { return new LegacyOreCount(GsonHelper.getAsInt(json, "base")); }
    }
}
