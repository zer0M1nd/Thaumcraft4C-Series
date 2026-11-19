package thaumcraft.api;

public  class ItemApi {
    private ItemApi() {}
    /** Registry IDs replace legacy string fields and metadata. */
    public static net.minecraft.world.item.ItemStack getItem(String id) {
        var key=net.minecraft.resources.ResourceLocation.parse(id.contains(":")?id:"thaumcraft:"+id);
        if(!net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(key))return net.minecraft.world.item.ItemStack.EMPTY;
        return new net.minecraft.world.item.ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(key));
    }
    public static net.minecraft.world.item.ItemStack getBlock(String id) { return getItem(id); }
}
