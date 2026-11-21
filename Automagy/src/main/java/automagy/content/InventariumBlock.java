package automagy.content;

import automagy.logic.InventariumStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class InventariumBlock extends BaseEntityBlock {
    public InventariumBlock(){super(Properties.of().strength(2.5f).noOcclusion());}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        return InventariumStructure.validate(new InventariumEnvironment(context.getLevel(),context.getClickedPos()),true)==InventariumStructure.Validity.VALID?defaultBlockState():null;
    }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(!level.isClientSide&&!old.is(this)){
            if(InventariumStructure.validate(new InventariumEnvironment(level,pos),true)==InventariumStructure.Validity.VALID)level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),3);
            else {level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);Block.popResource(level,pos,new net.minecraft.world.item.ItemStack(this));}
        }
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof InventariumBlockEntity core)core.breakLinks();super.onRemove(state,level,pos,next,moving);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(4,5.12,4,12,14.4,12);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new InventariumBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Content.INVENTARIUM_TILE.get(),InventariumBlockEntity::tick);}
}
