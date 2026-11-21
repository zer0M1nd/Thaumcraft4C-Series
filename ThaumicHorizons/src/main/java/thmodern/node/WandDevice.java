package thmodern.node;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import tc4revival.content.*;

public interface WandDevice extends WandInteractable {
    BlockPos getBlockPos();
    @Override default InteractionResult onWandUseFirst(ItemStack stack,UseOnContext context){return InteractionResult.PASS;}
    @Override default Optional<ItemStack> onWandUse(Level level,ItemStack stack,Player player){
        if(!(stack.getItem() instanceof CastingWandItem wand))return Optional.empty();
        wand.setObjectInUse(stack,level,getBlockPos());
        player.startUsingItem(player.getMainHandItem()==stack?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);
        return Optional.of(stack);
    }
    default boolean lookingAt(Player player){
        var hit=player.pick(player.getBlockReach(),1,true);
        boolean valid=hit instanceof BlockHitResult block&&hit.getType()==HitResult.Type.BLOCK&&block.getBlockPos().equals(getBlockPos());
        if(!valid)player.stopUsingItem(); return valid;
    }
    @Override default void onWandRelease(ItemStack stack,Level level,Player player,int remainingTicks){}
}
