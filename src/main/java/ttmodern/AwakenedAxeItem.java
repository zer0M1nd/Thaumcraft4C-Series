package ttmodern;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.*;

/** Native axe identity permits ordinary axe enchantments while retaining awakened actions. */
public final class AwakenedAxeItem extends AxeItem implements AwakenedMiningTool {
    public AwakenedAxeItem() { super(IchorTools.TIER,3,-3,new Properties().stacksTo(1).rarity(Rarity.EPIC)); }
    @Override public String kind() { return "axe"; }
    @Override public boolean isEnchantable(ItemStack stack) { return stack.getCount()==1; }
    @Override public InteractionResult useOn(UseOnContext context) { return InteractionResult.PASS; }
    @Override public boolean canPerformAction(ItemStack stack,ToolAction action) { return action==ToolActions.AXE_DIG; }
    @Override public float getDestroySpeed(ItemStack stack,BlockState state) { return effective(state)?Math.max(10,Items.SHEARS.getDestroySpeed(stack,state)):super.getDestroySpeed(stack,state); }
    @Override public boolean isCorrectToolForDrops(BlockState state) { return effective(state)&&TierSortingRegistry.isCorrectTierForDrops(IchorTools.TIER,state); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) { return AwakenedModes.use(level,player,hand,kind()); }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags) { lines.add(AwakenedModes.label(stack,kind()).withStyle(net.minecraft.ChatFormatting.DARK_AQUA)); }
}
