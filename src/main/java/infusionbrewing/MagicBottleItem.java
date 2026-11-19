package infusionbrewing;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.HitResult;

public class MagicBottleItem extends Item {
    public MagicBottleItem(){super(new Properties().stacksTo(8));}
    @Override public int getMaxStackSize(ItemStack stack){return BrewingConfig.stackSize();}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.ANY);
        if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(stack);
        var pos=hit.getBlockPos();int id=PotionFluids.id(level.getFluidState(pos));
        if(id==0||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),stack))return InteractionResultHolder.pass(stack);
        if(level.isClientSide)return InteractionResultHolder.success(stack);
        level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());var result=ItemUtils.createFilledResult(stack,player,PotionData.base(id).potion(false));
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.BOTTLE_FILL,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        level.gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.FLUID_PICKUP,pos);
        return InteractionResultHolder.consume(result);
    }
}
