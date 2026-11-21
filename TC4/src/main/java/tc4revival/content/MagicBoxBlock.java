package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public  class MagicBoxBlock extends BaseEntityBlock {
    public MagicBoxBlock() { super(Properties.of().strength(2.5f).sound(SoundType.WOOD).noOcclusion()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new MagicBoxBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide?null:createTickerHelper(type,TCArcane.MAGIC_BOX_ENTITY.get(),(world,pos,current,box)->box.tick());
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(player instanceof net.minecraft.server.level.ServerPlayer server&&level.getBlockEntity(pos) instanceof MagicBoxBlockEntity box)
            net.minecraftforge.network.NetworkHooks.openScreen(server,box,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int value) { super.triggerEvent(state,level,pos,id,value);var box=level.getBlockEntity(pos);return box!=null&&box.triggerEvent(id,value); }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!level.isClientSide&&!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof MagicBoxBlockEntity box) {
            box.dropLocalContents();
        }
        super.onRemove(state,level,pos,next,moving);
    }
}
