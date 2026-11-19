package tc4revival.content;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public  class TCEnchantments {
    private static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, "thaumcraft");
    public static final RegistryObject<LegacyRepairEnchantment> REPAIR = ENCHANTMENTS.register("repair", LegacyRepairEnchantment::new);
    public static final RegistryObject<LegacyHasteEnchantment> HASTE = ENCHANTMENTS.register("haste", LegacyHasteEnchantment::new);
    private TCEnchantments() {}
    public static void register(IEventBus bus) { ENCHANTMENTS.register(bus); }
}
