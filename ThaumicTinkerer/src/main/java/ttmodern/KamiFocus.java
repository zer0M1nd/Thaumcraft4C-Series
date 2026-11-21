package ttmodern;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public abstract class KamiFocus extends TinkererFocus {
    protected KamiFocus(String name,String prefix,int tint){super(name,prefix,tint);}
    @Override public Rarity getRarity(ItemStack stack){return Rarity.EPIC;}
    @Override public String ornamentTexture(){return null;}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){if(!TinkererConfig.KAMI.get())return InteractionResultHolder.fail(stack);player.startUsingItem(hand);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
}
