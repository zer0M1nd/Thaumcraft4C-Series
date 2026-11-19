package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public  class InfusionMatrixBlock extends BaseEntityBlock {
    public InfusionMatrixBlock() { super(Properties.of().strength(3,15).requiresCorrectToolForDrops().noOcclusion().lightLevel(state->10)); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new InfusionMatrixBlockEntity(pos,state); }
    @Override public net.minecraft.world.InteractionResult use(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        if(hand!=net.minecraft.world.InteractionHand.MAIN_HAND||!player.getItemInHand(hand).isEmpty()||!tc4revival.config.TCConfig.SERVER.revival.repeatInfusion.get())return net.minecraft.world.InteractionResult.PASS;
        if(level.isClientSide)return net.minecraft.world.InteractionResult.SUCCESS;
        if(player instanceof net.minecraft.server.level.ServerPlayer server&&level.getBlockEntity(pos) instanceof InfusionMatrixBlockEntity matrix&&matrix.repeat(server))return net.minecraft.world.InteractionResult.CONSUME;
        return net.minecraft.world.InteractionResult.CONSUME;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return createTickerHelper(type,TCArcane.INFUSION_MATRIX_ENTITY.get(),(world,pos,block,matrix)->matrix.tick());
    }
}
