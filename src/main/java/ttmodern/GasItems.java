package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class GasItems {
    private GasItems() {}
    public static final class Bottle extends Item {
        private final java.util.function.Supplier<GasBlock> gas;
        public Bottle(java.util.function.Supplier<GasBlock> gas){super(new Properties());this.gas=gas;}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
            var stack=player.getItemInHand(hand);var pos=player.blockPosition().above();
            if(!level.mayInteract(player,pos)||!level.getBlockState(pos).isAir())return InteractionResultHolder.fail(stack);
            if(!level.isClientSide&&level.setBlock(pos,gas.get().defaultBlockState().setValue(GasBlock.POWER,4),3)) {
                if(!player.getAbilities().instabuild)stack.shrink(1);
                level.playSound(null,player.blockPosition(),SoundEvents.ITEM_PICKUP,SoundSource.PLAYERS,.5f,.4f/(level.random.nextFloat()*.4f+.8f));
            }
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
    }
    public static final class Remover extends Item {
        public Remover(){super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
            var stack=player.getItemInHand(hand);if(!player.isShiftKeyDown())return InteractionResultHolder.pass(stack);
            if(!level.isClientSide) {
                var origin=player.blockPosition();
                for(var pos:BlockPos.betweenClosed(origin.offset(-3,-3,-3),origin.offset(2,2,2)))
                    if(level.hasChunkAt(pos)&&level.mayInteract(player,pos)&&level.getBlockState(pos).getBlock() instanceof GasBlock)level.removeBlock(pos,false);
                level.playSound(null,origin,tc4revival.content.TCSounds.WAND.get(),SoundSource.PLAYERS,.2f,1);
            }
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
    }
    public static final class BrightNitor extends Item implements top.theillusivec4.curios.api.type.capability.ICurioItem {
        @Override public void curioTick(top.theillusivec4.curios.api.SlotContext context,ItemStack stack) { if(!context.cosmetic())inventoryTick(stack,context.entity().level(),context.entity(),context.index(),false); }
        public BrightNitor(){super(new Properties().stacksTo(1));}
        @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected) {
            PersonalLight.place(level,entity,entity.blockPosition().above(),0);
        }
    }
}
