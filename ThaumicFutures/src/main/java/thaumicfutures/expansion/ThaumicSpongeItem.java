package thaumicfutures.expansion;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import tc4revival.content.CrucibleBlockEntity;

public final class ThaumicSpongeItem extends BlockItem {
    public ThaumicSpongeItem(Block block){super(block,new Properties());}
    @Override public InteractionResult useOn(UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();var player=context.getPlayer();
        if(thaumicfutures.FutureFeatures.Group.RESTORATION.enabled()
                &&level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible){
            if(player==null||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
            if(crucible.contents().water()==0&&crucible.contents().aspects().isEmpty())return InteractionResult.PASS;
            if(!level.isClientSide){
                // Empty the contents without invoking CrucibleBlockEntity.empty(), which releases flux.
                crucible.contents().empty();crucible.synchronize();
                level.blockEvent(pos,level.getBlockState(pos).getBlock(),2,5);
                {
                    var held=context.getItemInHand();held.shrink(1);
                    var wet=new ItemStack(RestoredContent.WET_SPONGE_ITEM.get());
                    if(held.isEmpty())player.setItemInHand(context.getHand(),wet);
                    else if(!player.getInventory().add(wet))player.drop(wet,false);
                }
                level.playSound(null,pos,net.minecraft.sounds.SoundEvents.GENERIC_SPLASH,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useOn(context);
    }
}
