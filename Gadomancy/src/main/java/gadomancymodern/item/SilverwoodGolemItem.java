package gadomancymodern.item;
import gadomancymodern.golem.GadGolems;
import net.minecraft.world.item.ItemStack;
public class SilverwoodGolemItem extends tc4revival.content.GolemPlacerItem {
    public SilverwoodGolemItem(){super(new Properties(),tc4revival.content.GolemMaterial.WOOD,gadomancymodern.Gadomancy.SILVERWOOD_ENTITY);}
    @Override public ItemStack getDefaultInstance(){var stack=super.getDefaultInstance();stack.getOrCreateTag().putString("AddonMaterial",GadGolems.SILVERWOOD.toString());return stack;}
    @Override public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack,net.minecraft.world.item.context.UseOnContext context){if(!context.getLevel().isClientSide)stack.getOrCreateTag().putString("AddonMaterial",GadGolems.SILVERWOOD.toString());return super.onItemUseFirst(stack,context);}
}
