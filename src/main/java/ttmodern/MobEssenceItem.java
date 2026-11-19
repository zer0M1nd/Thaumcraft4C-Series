package ttmodern;

import java.util.List;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** Separate registry IDs replace old metadata arithmetic, preserving the three grades. */
public final class MobEssenceItem extends Item {
    public static final List<String> ASPECTS=List.of("aqua","humanus","aer","volatus","ignis","praecantatio","exanimis","corpus","bestia","venenum","terra","alienis","iter","metallum","limus","pannus","messis");
    private final String aspect;private final int grade;
    public MobEssenceItem(String aspect,int grade){super(new Properties().stacksTo(16));if(!ASPECTS.contains(aspect)||grade<0||grade>2)throw new IllegalArgumentException("Unknown essence");this.aspect=aspect;this.grade=grade;}
    public String aspect(){return aspect;}public int grade(){return grade;}
    @Override public boolean isFoil(ItemStack stack){return grade==2;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){lines.add(Component.literal(thaumcraft.api.aspects.AspectViews.display(tc4revival.aspect.TCAspects.catalog().require(aspect)).displayName()));}
    public static ItemStack stack(String aspect,int grade){return new ItemStack(Tinkerer.ESSENCES.get(aspect+"/"+grade).get());}
}
