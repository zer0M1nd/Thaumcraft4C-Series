package ttmodern;

import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class AwakenedModes {
    public static int mode(ItemStack stack){int value=stack.hasTag()?stack.getTag().getInt("ToolMode"):0;return value>=0&&value<=2?value:0;}
    public static net.minecraft.network.chat.MutableComponent label(ItemStack stack,String kind){return Component.translatable("ttmisc.mode."+kind+"."+mode(stack));}
    public static InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand,String kind){
        var stack=player.getItemInHand(hand);if(!player.isShiftKeyDown())return InteractionResultHolder.pass(stack);
        if(!level.isClientSide){stack.getOrCreateTag().putInt("ToolMode",(mode(stack)+1)%3);player.displayClientMessage(label(stack,kind),true);}
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    private AwakenedModes(){}
}
