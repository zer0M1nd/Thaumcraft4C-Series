package thmodern.vat;

import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import thmodern.Horizons;
import thmodern.node.NodeDeviceBlock;

public final class VatMatrixBlock extends NodeDeviceBlock<VatMatrixEntity> {
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    public VatMatrixBlock(){super(Properties.of().strength(2,2).sound(SoundType.STONE),()->Horizons.VAT_MATRIX_ENTITY.get(),VatMatrixEntity::tick);}
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return level.hasChunkAt(pos.below())&&level.getBlockEntity(pos.below()) instanceof VatEntity;}
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos other){return side==Direction.DOWN&&!canSurvive(state,level,pos)?Blocks.AIR.defaultBlockState():super.updateShape(state,side,neighbor,level,pos,other);}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos.below()) instanceof VatEntity vat&&vat.mode()==VatEntity.INFUSING)vat.destroySubject();
        super.onRemove(state,level,pos,next,moving);
    }
}
