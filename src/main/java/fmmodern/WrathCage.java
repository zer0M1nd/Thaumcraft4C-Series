package fmmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class WrathCage extends BaseEntityBlock {
    public WrathCage(){super(Properties.copy(Blocks.SPAWNER).strength(5,2000).noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new WrathCageEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Content.CAGE_ENTITY.get(),level.isClientSide?WrathCageEntity::clientTick:WrathCageEntity::tick);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof WrathCageEntity cage))return InteractionResult.PASS;
        var held=player.getItemInHand(hand);
        if(held.is(Content.FORK.get())){if(!level.isClientSide)cage.cycleMode();return InteractionResult.sidedSuccess(level.isClientSide);}
        if(!held.is(Content.MOB_CRYSTAL.get()))return InteractionResult.PASS;var id=SpecialItems.MobCrystal.mob(held);
        if(!CageMobs.allowed(id))return InteractionResult.PASS;
        if(!level.isClientSide){var previous=cage.crystal();cage.setMob(id);player.setItemInHand(hand,previous);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!old.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof WrathCageEntity cage){var crystal=cage.crystal();if(!crystal.isEmpty())popResource(level,pos,crystal);}
        super.onRemove(old,level,pos,next,moving);
    }
}
