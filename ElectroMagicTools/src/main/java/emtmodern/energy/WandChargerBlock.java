package emtmodern.energy;
import emtmodern.ElectroMagicTools;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
public class WandChargerBlock extends BaseEntityBlock implements ic2.api.tile.IWrenchAble {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public WandChargerBlock(){super(Properties.of().strength(4).sound(SoundType.METAL));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new WandChargerEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,ElectroMagicTools.WAND_CHARGER_BE.get(),WandChargerEntity::tick);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer server&&level.getBlockEntity(pos) instanceof WandChargerEntity machine)NetworkHooks.openScreen(server,machine,pos);return InteractionResult.sidedSuccess(level.isClientSide);}
    @Override public void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving){if(!old.is(next.getBlock())&&level.getBlockEntity(pos) instanceof WandChargerEntity machine)Containers.dropContents(level,pos,machine);super.onRemove(old,level,pos,next,moving);}
    @Override public Direction getFacing(Level level,BlockPos pos){return level.getBlockState(pos).getValue(FACING);}
    @Override public boolean setFacing(Level level,BlockPos pos,Direction facing,Player player){return false;}
    @Override public boolean wrenchCanRemove(Level level,BlockPos pos,Player player){return true;}
    @Override public java.util.List<ItemStack> getWrenchDrops(Level level,BlockPos pos,BlockState state,BlockEntity entity,Player player,int fortune){return java.util.List.of(new ItemStack(level.random.nextFloat()<.8?asItem():net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.parse("ic2:machine"))));}
}
