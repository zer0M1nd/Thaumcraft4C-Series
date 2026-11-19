package automagy.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class ScribePointerBlock extends BaseEntityBlock {
    public ScribePointerBlock(){super(Properties.of().strength(1.5f).noOcclusion());}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return canSurvive(defaultBlockState(),context.getLevel(),context.getClickedPos())?defaultBlockState():null;}
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return level.getBlockState(pos.below()).is(Content.SCRIBE.get());}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){if(!level.isClientSide&&level.hasChunkAt(pos.below())&&!canSurvive(state,level,pos))level.destroyBlock(pos,true);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var held=player.getItemInHand(hand);if(!player.mayBuild()||!held.isEmpty()&&!(held.getItem() instanceof automagy.api.LocationLink))return InteractionResult.PASS;
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof ScribePointerBlockEntity pointer){
            if(held.isEmpty()){var eye=pointer.remove();if(!player.getInventory().add(eye))player.drop(eye,false);}
            else if(pointer.insert(held)&&!player.getAbilities().instabuild)held.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof ScribePointerBlockEntity pointer)pointer.drop();super.onRemove(state,level,pos,next,moving);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(1,0,1,15,6,15);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ScribePointerBlockEntity(pos,state);}
}
