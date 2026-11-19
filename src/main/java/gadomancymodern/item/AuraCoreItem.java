package gadomancymodern.item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public class AuraCoreItem extends Item {
    public final String aspect;
    public AuraCoreItem(String aspect){super(new Properties().stacksTo(1).rarity(Rarity.RARE));this.aspect=aspect;}
    @Override public String getDescriptionId(){return "item.ItemAuraCore.name";}
    @Override public Component getName(ItemStack stack){return Component.translatable(getDescriptionId()).withStyle(net.minecraft.ChatFormatting.GOLD);}
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flags){
        var text=aspect.equals("blank")?Component.translatable("gadomancy.auracore.blank"):Component.literal(thaumcraft.api.aspects.AspectViews.display(tc4revival.aspect.TCAspects.catalog().require(aspect)).displayName());
        if(aspect.equals("terra")||aspect.equals("perditio"))text.append(" ").append(Component.translatable("gadomancy.auracore.unused"));lines.add(text.withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    public int color(){return switch(aspect){case "aer"->0xFFFF7E;case "ignis"->16727041;case "aqua"->37119;case "terra"->40960;case "ordo"->0xEECCFF;case "perditio"->0x555577;default->0xFFFFFF;};}
}
