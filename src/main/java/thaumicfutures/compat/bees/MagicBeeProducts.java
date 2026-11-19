package thaumicfutures.compat.bees;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Products missing from Mo'Bees, registered only with both modern bee mods present. */
public final class MagicBeeProducts {
    public static final String[] PRIMALS = {"air", "fire", "water", "earth", "order", "chaos"};
    private static final Map<String, RegistryObject<Item>> ITEMS = new LinkedHashMap<>();

    private MagicBeeProducts() {}

    public static void register(DeferredRegister<Item> registry) {
        ThaumicBeeTools.register(registry);
        for (String primal : PRIMALS) {
            add(registry, "bee_comb_" + primal);
            add(registry, "bee_propolis_" + primal);
            add(registry, "bee_dust_" + primal);
        }
        for (String name : new String[]{"bee_comb_intellect", "bee_comb_temporal", "bee_comb_soul",
                "bee_wax_magic", "bee_wax_soul", "bee_propolis_unstable", "bee_pollen_unusual",
                "bee_pollen_phased", "bee_drop_intellect"}) add(registry, name);
        for (String name : new String[]{"life", "armor", "unstable", "death", "time", "oblivion"})
            add(registry, "bee_essence_" + name);
        add(registry, "bee_dimensional_singularity");
        add(registry, "bee_drop_enchanted");
        add(registry, "bee_lore_fragment");
        add(registry, "bee_comb_legacy_forgotten");
        add(registry, "bee_wax_amnesic");
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> LegacyHiveColors.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus()));
        for (String name : new String[]{"mundane", "molten", "occult", "otherworldly", "transmuted",
                "furtive", "airy", "firey", "watery", "earthy"}) add(registry, "bee_comb_legacy_" + name);
    }

    private static void add(DeferredRegister<Item> registry, String name) {
        ITEMS.put(name, registry.register(name, () -> new Item(new Item.Properties())));
    }

    public static ItemStack stack(String name) {
        RegistryObject<Item> item = ITEMS.get(name);
        if (item == null) throw new IllegalArgumentException("Unknown magic bee product: " + name);
        return new ItemStack(item.get());
    }
}
