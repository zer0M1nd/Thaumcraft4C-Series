package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.shapes.*;

public final class SummoningTabletBlock extends BaseEntityBlock {
    public SummoningTabletBlock(){super(BlockBehaviour.Properties.of().strength(3,50).sound(SoundType.METAL).noOcclusion());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Block.box(0,0,0,16,2,16);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SummoningTabletEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Tinkerer.SUMMONING_TABLET_ENTITY.get(),(l,p,s,be)->be.tick());}
}
