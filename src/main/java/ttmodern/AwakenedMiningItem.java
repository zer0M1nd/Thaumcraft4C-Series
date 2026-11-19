package ttmodern;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.*;

public final class AwakenedMiningItem extends DiggerItem implements AwakenedMiningTool {
    private final String kind;
    public AwakenedMiningItem(String kind){super(kind.equals("axe")?3:1,kind.equals("pick")||kind.equals("pickaxe")?-2.8f:-3f,IchorTools.TIER,kind.equals("axe")?BlockTags.MINEABLE_WITH_AXE:kind.equals("shovel")?BlockTags.MINEABLE_WITH_SHOVEL:BlockTags.MINEABLE_WITH_PICKAXE,new Properties().stacksTo(1).rarity(Rarity.EPIC));this.kind=kind;}
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){return kind.equals("pick")?BedrockTravel.usePick(context):super.useOn(context);}
    public String kind(){return kind;}
    @Override public boolean isEnchantable(ItemStack stack){return stack.getCount()==1;}
    public boolean effective(BlockState state){return state.is(net.minecraft.tags.BlockTags.create(net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":awakened_"+kind+"_mineable")));}
    @Override public float getDestroySpeed(ItemStack stack,BlockState state){return effective(state)?Math.max(10,kind.equals("axe")?Items.SHEARS.getDestroySpeed(stack,state):10):super.getDestroySpeed(stack,state);}
    @Override public boolean isCorrectToolForDrops(BlockState state){return effective(state)&&TierSortingRegistry.isCorrectTierForDrops(IchorTools.TIER,state);}
    @Override public boolean canPerformAction(ItemStack stack,ToolAction action){return action==(kind.equals("axe")?ToolActions.AXE_DIG:kind.equals("shovel")?ToolActions.SHOVEL_DIG:ToolActions.PICKAXE_DIG);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return AwakenedModes.use(level,player,hand,kind);}
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){lines.add(AwakenedModes.label(stack,kind).withStyle(net.minecraft.ChatFormatting.DARK_AQUA));}
}
