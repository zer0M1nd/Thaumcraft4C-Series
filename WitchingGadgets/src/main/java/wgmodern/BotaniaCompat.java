package wgmodern;
import java.util.List;import net.minecraft.world.item.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.network.chat.Component;import net.minecraft.world.level.Level;import vazkii.botania.api.item.CosmeticAttachable;
/** Loaded only when Botania is installed; keeps cosmetic data on the actual cloak/accessory. */
public class BotaniaCompat {
 public static Item cloak(String kind,boolean kama){return new Cloak(kind,kama);}public static Item gear(String kind){return new Gear(kind);}
 static ItemStack read(ItemStack s){return s.hasTag()?ItemStack.of(s.getTag().getCompound("cosmeticItem")):ItemStack.EMPTY;}
 static void write(ItemStack s,ItemStack cosmetic){if(cosmetic.isEmpty())s.getOrCreateTag().remove("cosmeticItem");else s.getOrCreateTag().put("cosmeticItem",cosmetic.copyWithCount(1).save(new CompoundTag()));}
 static void tooltip(ItemStack s,List<Component> lines){var c=read(s);if(!c.isEmpty())lines.add(Component.translatable("botaniamisc.hasCosmetic",c.getHoverName()));}
 public static class Cloak extends CloakItem implements CosmeticAttachable {public Cloak(String kind,boolean kama){super(kind,kama);}public ItemStack getCosmeticItem(ItemStack s){return read(s);}public void setCosmeticItem(ItemStack s,ItemStack c){write(s,c);}@Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){super.appendHoverText(s,l,lines,f);tooltip(s,lines);}}
 public static class Gear extends WGAccessories.Gear implements CosmeticAttachable {public Gear(String kind){super(kind);}public ItemStack getCosmeticItem(ItemStack s){return read(s);}public void setCosmeticItem(ItemStack s,ItemStack c){write(s,c);}@Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){super.appendHoverText(s,l,lines,f);tooltip(s,lines);}}
}
