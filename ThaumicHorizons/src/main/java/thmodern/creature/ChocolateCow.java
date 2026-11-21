package thmodern.creature;

import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import thmodern.Horizons;

public final class ChocolateCow extends Cow {
    public ChocolateCow(EntityType<? extends Cow> type,Level level){super(type,level);}
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);
        if(held.is(Items.BUCKET)){
            if(!level().isClientSide){player.playSound(SoundEvents.COW_MILK,1,1);player.setItemInHand(hand,ItemUtils.createFilledResult(held,player,new ItemStack(Horizons.CHOCOLATE_MILK.get())));}
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player,hand);
    }
}
