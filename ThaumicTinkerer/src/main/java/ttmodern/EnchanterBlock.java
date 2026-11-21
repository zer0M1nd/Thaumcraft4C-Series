package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

public final class EnchanterBlock extends BaseEntityBlock {
    public EnchanterBlock(){super(BlockBehaviour.Properties.copy(Blocks.ENCHANTING_TABLE).strength(5,2000).noOcclusion());}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return Block.box(0,0,0,16,12,16);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EnchanterEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,Tinkerer.ENCHANTER_ENTITY.get(),(world,pos,s,entity)->entity.tick());}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.mayInteract(player,pos))return InteractionResult.FAIL;
        if(player instanceof net.minecraft.server.level.ServerPlayer server&&level.getBlockEntity(pos) instanceof EnchanterEntity entity){
            var options=EnchanterMenu.options(server);
            net.minecraftforge.network.NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inventory,p)->new EnchanterMenu(id,inventory,entity,options),net.minecraft.network.chat.Component.translatable("block.thaumic_tinkerer.osmotic_enchanter")),buffer->{buffer.writeBlockPos(pos);EnchanterMenu.writeOptions(buffer,options);});
        }return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!level.isClientSide&&!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof EnchanterEntity entity)for(int i=0;i<2;i++){var item=entity.items.extractItem(i,1,false);if(!item.isEmpty())Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),item);}super.onRemove(state,level,pos,next,moving);}
}
