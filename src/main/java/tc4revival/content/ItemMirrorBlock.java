package tc4revival.content;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public  class ItemMirrorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public ItemMirrorBlock() {
        super(Properties.of().strength(1,10).noOcclusion().noCollission().sound(new net.minecraftforge.common.util.ForgeSoundType(.5f,2,TCSounds.JAR,TCSounds.JAR,TCSounds.JAR,TCSounds.JAR,TCSounds.JAR)));
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getClickedFace()); }
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        var face=state.getValue(FACING); var support=pos.relative(face.getOpposite()); return level.getBlockState(support).isFaceSturdy(level,support,face);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving) { if (!level.isClientSide && !canSurvive(state,level,pos)) level.destroyBlock(pos,true); }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return switch(state.getValue(FACING)) {
            case DOWN->box(0,15,0,16,16,16); case UP->box(0,0,0,16,1,16);
            case NORTH->box(0,0,15,16,16,16); case SOUTH->box(0,0,0,16,16,1);
            case WEST->box(15,0,0,16,16,16); case EAST->box(0,0,0,1,16,16);
        };
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) { return InteractionResult.sidedSuccess(level.isClientSide); }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if (!level.isClientSide && entity instanceof ItemEntity item && item.isAlive() && item.getPortalCooldown()==0 && level.getBlockEntity(pos) instanceof ItemMirrorBlockEntity mirror) mirror.transport(item);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ItemMirrorBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) { return type==TCArcane.ITEM_MIRROR_ENTITY.get() ? (world,pos,block,entity)->((ItemMirrorBlockEntity)entity).tick() : null; }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if(!replacement.is(state.getBlock())&&level.getBlockEntity(pos) instanceof ItemMirrorBlockEntity host)
            host.extensions().beforeRemove(moving?thaumcraft.api.devices.DeviceExtensions.Removal.MOVED:thaumcraft.api.devices.DeviceExtensions.Removal.REPLACED);

        if (!state.is(replacement.getBlock()) && level instanceof net.minecraft.server.level.ServerLevel server && level.getBlockEntity(pos) instanceof ItemMirrorBlockEntity mirror) {
            if(!moving){mirror.dropQueuedItems();mirror.invalidateLink();thaumcraft.api.transport.MirrorEndpoints.get(server.getServer()).destroy(mirror);}
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
}
