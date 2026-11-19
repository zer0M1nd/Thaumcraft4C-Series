package tc4revival.content;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public  class TaintBottleItem extends Item {
    public TaintBottleItem() { super(new Properties().stacksTo(8)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(player,player.getX(),player.getY(),player.getZ(),net.minecraft.sounds.SoundEvents.ARROW_SHOOT,net.minecraft.sounds.SoundSource.PLAYERS,.5f,.4f/(level.random.nextFloat()*.4f+.8f));
        if (!level.isClientSide) level.addFreshEntity(new TaintBottleEntity(level,player));
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
