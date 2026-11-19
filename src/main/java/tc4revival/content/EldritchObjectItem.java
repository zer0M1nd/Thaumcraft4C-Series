package tc4revival.content;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Eye/tablet actions belong to the altar and lock, with server-owned consumption. */
public  class EldritchObjectItem extends Item {
    private final boolean tablet;
    public EldritchObjectItem(boolean tablet) { super(new Properties().stacksTo(1).rarity(tablet ? Rarity.RARE : Rarity.UNCOMMON)); this.tablet=tablet; }
    @Override public void appendHoverText(ItemStack stack,Level world,List<Component> lines,TooltipFlag flags) {
        lines.add(Component.translatable("item.ItemEldritchObject.text."+(tablet ? 4 : 1)).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
