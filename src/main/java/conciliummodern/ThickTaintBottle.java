package conciliummodern;

import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class ThickTaintBottle extends Item implements thaumcraft.api.IWarpingGear {
    public ThickTaintBottle() { super(new Properties()); }
    @Override public int getWarp(ItemStack stack,Player player) { return 2; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(!level.isClientSide) {
            var bottle=new ThickTaintProjectile(ConciliumEntities.THICK_TAINT.get(),level);
            bottle.setOwner(player);bottle.setPos(player.getX(),player.getEyeY()-.1,player.getZ());
            bottle.shootFromRotation(player,player.getXRot(),player.getYRot(),-20,.5f,1);
            if(!level.addFreshEntity(bottle))return InteractionResultHolder.fail(stack);
            if(!player.getAbilities().instabuild)stack.shrink(1);
            level.playSound(null,player.blockPosition(),SoundEvents.ARROW_SHOOT,SoundSource.PLAYERS,.5f,.4f/(level.random.nextFloat()*.4f+.8f));
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
