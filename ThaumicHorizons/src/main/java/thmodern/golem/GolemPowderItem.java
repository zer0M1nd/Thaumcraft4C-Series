package thmodern.golem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class GolemPowderItem extends Item {
    public GolemPowderItem(){super(new Properties());}
    @Override public InteractionResult useOn(UseOnContext context){
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(!(context.getPlayer() instanceof ServerPlayer player)||!player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        var stack=context.getItemInHand();
        boolean created=AnimatedGolems.animate(player,context.getClickedPos(),1200,false,false,new AnimatedGolems.Payment(){
            private boolean spent;
            @Override public boolean commit(){if(player.getItemInHand(context.getHand())!=stack||stack.isEmpty())return false;if(!player.getAbilities().instabuild){stack.shrink(1);spent=true;}return true;}
            @Override public void refund(){if(spent)stack.grow(1);}
        });
        return created?InteractionResult.CONSUME:InteractionResult.FAIL;
    }
}
