package ttmodern;

import net.minecraft.world.item.ItemStack;

/** Public modern Tinkers API adapter, kept optional and independent of the base mod. */
public final class TinkersRestoration {
    private static Class<?> itemType;private static java.lang.reflect.Method from,repair;private static boolean initialized,warned;
    private static void init(){if(initialized)return;initialized=true;if(!net.minecraftforge.fml.ModList.get().isLoaded("tconstruct"))return;try{var item=Class.forName("slimeknights.tconstruct.library.tools.item.ModifiableItem");var tool=Class.forName("slimeknights.tconstruct.library.tools.nbt.ToolStack");var view=Class.forName("slimeknights.tconstruct.library.tools.nbt.IToolStackView");from=tool.getMethod("from",ItemStack.class);repair=Class.forName("slimeknights.tconstruct.library.tools.helper.ToolDamageUtil").getMethod("repair",view,int.class);itemType=item;}catch(ReflectiveOperationException error){warn(error);}}
    private static void warn(Exception error){if(!warned){warned=true;com.mojang.logging.LogUtils.getLogger().warn("Tinkers restoration public API unavailable",error);}}
    public static boolean accepts(ItemStack stack){init();return TinkererConfig.REPAIR_TINKERS.get()&&itemType!=null&&itemType.isInstance(stack.getItem())&&!(stack.hasTag()&&(stack.getTag().contains("Energy")||stack.getTag().contains("charge")));}
    public static ItemStack repairedCopy(ItemStack original,int amount){var copy=original.copy();try{if(accepts(original))repair.invoke(null,from.invoke(null,copy),amount);else copy.setDamageValue(Math.max(0,copy.getDamageValue()-amount));return copy.getDamageValue()<original.getDamageValue()?copy:ItemStack.EMPTY;}catch(ReflectiveOperationException error){warn(error);return ItemStack.EMPTY;}}
    private TinkersRestoration(){}
}
