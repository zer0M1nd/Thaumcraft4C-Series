package wgmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;

public class SpinningWheel extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public SpinningWheel(){super(Properties.copy(Blocks.OAK_PLANKS).strength(2.5f,6).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(FACING).getAxis()==Direction.Axis.X?Block.box(0,0,4,16,17,12):Block.box(4,0,0,12,17,16);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tile(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,Content.SPINNING_TILE.get(),Tile::tick);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer server&&l.getBlockEntity(p) instanceof Tile tile)NetworkHooks.openScreen(server,tile,p);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(s.getBlock()!=next.getBlock()&&l.getBlockEntity(p) instanceof Tile tile)Containers.dropContents(l,p,tile);super.onRemove(s,l,p,next,moving);}
    public static class Tile extends SidedInventoryEntity implements MenuProvider {
        private NonNullList<ItemStack> items=NonNullList.withSize(6,ItemStack.EMPTY);public int progress,duration=120;
        public Tile(BlockPos p,BlockState s){super(Content.SPINNING_TILE.get(),p,s);}
        public static void tick(Level l,BlockPos p,BlockState s,Tile tile){var found=l.getRecipeManager().getRecipeFor(Content.SPINNING.get(),tile,l);if(found.isEmpty()){if(tile.progress!=0){tile.progress=0;tile.changed();}return;}var recipe=found.get();var result=recipe.getResultItem(l.registryAccess());var output=tile.items.get(5);tile.duration=recipe.duration();if(!output.isEmpty()&&(!ItemStack.isSameItemSameTags(output,result)||output.getCount()+result.getCount()>output.getMaxStackSize())){if(tile.progress!=0){tile.progress=0;tile.changed();}return;}if(tile.progress>=tile.duration){if(output.isEmpty())tile.items.set(5,result.copy());else output.grow(result.getCount());for(int i=0;i<5;i++)tile.items.get(i).shrink(1);tile.progress=0;tile.changed();}else{tile.progress++;tile.setChanged();}}
        private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
        @Override public void load(CompoundTag nbt){super.load(nbt);items=NonNullList.withSize(6,ItemStack.EMPTY);ContainerHelper.loadAllItems(nbt,items);progress=Math.max(0,nbt.getInt("progress"));}
        @Override protected void saveAdditional(CompoundTag nbt){super.saveAdditional(nbt);ContainerHelper.saveAllItems(nbt,items);nbt.putInt("progress",progress);}
        @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
        @Override public int getContainerSize(){return 6;}@Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
        @Override public ItemStack getItem(int i){return items.get(i);}@Override public ItemStack removeItem(int i,int n){var out=ContainerHelper.removeItem(items,i,n);changed();return out;}@Override public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
        @Override public void setItem(int i,ItemStack stack){items.set(i,stack);stack.setCount(Math.min(stack.getCount(),stack.getMaxStackSize()));changed();}
        @Override public boolean stillValid(Player p){return level!=null&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
        @Override public void clearContent(){items.clear();changed();}
        @Override public int[] getSlotsForFace(Direction d){return d==null?new int[]{0,1,2,3,4,5}:d==Direction.DOWN?new int[]{5}:new int[]{0,1,2,3,4};}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot>=0&&slot<5;}
        @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction d){return slot<5;}
        @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction d){return slot==5;}
        @Override public Component getDisplayName(){return Component.translatable("tile.WG_WoodenDevice.spinningWheel");}
        @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new SpinningMenu(id,inv,worldPosition);}
    }
}
