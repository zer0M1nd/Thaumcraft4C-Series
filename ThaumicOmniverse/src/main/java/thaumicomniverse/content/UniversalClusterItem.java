package thaumicomniverse.content;

import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** One registry item carries the ore identity; GT materials use their native TagPrefix instead. */
public class UniversalClusterItem extends Item {
    public UniversalClusterItem(){super(new Properties());}
    public static ItemStack of(String material,ItemStack product) {
        var stack=new ItemStack(thaumicomniverse.ThaumicOmniverse.ORE_CLUSTER.get());
        stack.getOrCreateTag().putString("Material",material);
        stack.getOrCreateTag().putString("Product",ForgeRegistries.ITEMS.getKey(product.getItem()).toString());return stack;
    }
    @Override public Component getName(ItemStack stack) {
        var id=stack.hasTag()?ResourceLocation.tryParse(stack.getTag().getString("Product")):null;
        var item=id==null?null:ForgeRegistries.ITEMS.getValue(id);
        if(item==null)return super.getName(stack);
        String name=item.getDescription().getString().replace("锭","").replaceAll("(?i)\\s+ingots?$","");
        return Component.translatable("item.thaumicomniverse.ore_cluster.material",name);
    }
}
