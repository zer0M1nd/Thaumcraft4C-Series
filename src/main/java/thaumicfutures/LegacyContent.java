package thaumicfutures;
/** Futures owns its registrations; LegacyMappings resolves the previous namespace on load. */
public final class LegacyContent {
    public static final String MOD_ID="thaumicfutures";
    public static final net.minecraftforge.registries.DeferredRegister<net.minecraft.world.item.Item> ITEMS=net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ITEMS,MOD_ID);
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> QUARTZ_SLIVER=ITEMS.register("quartz_sliver",()->new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
    public static final net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> BRASS=ITEMS.register("alchemical_brass_ingot",()->new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
}
