package automagy.content;
import net.minecraft.world.item.*;
public final class WorkbenchUpgradeItem extends Item {
 private final int type;
 public WorkbenchUpgradeItem(int type){super(new Properties().rarity(Rarity.UNCOMMON));this.type=type;}
 @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.translatable("tooltip.automagy.workbench_upgrade."+type));}
}
