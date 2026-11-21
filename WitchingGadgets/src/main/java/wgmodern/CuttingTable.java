package wgmodern;
import net.minecraft.core.*;import net.minecraft.nbt.*;import net.minecraft.network.chat.Component;import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;import net.minecraft.server.level.ServerPlayer;import net.minecraft.world.*;import net.minecraft.world.entity.player.*;import net.minecraft.world.inventory.*;import net.minecraft.world.item.*;import net.minecraft.world.item.context.BlockPlaceContext;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.entity.*;import net.minecraft.world.level.block.state.*;import net.minecraft.world.phys.BlockHitResult;import net.minecraft.world.phys.shapes.*;import net.minecraftforge.network.NetworkHooks;
public class CuttingTable extends BaseEntityBlock {
 public CuttingTable(){super(Properties.copy(Blocks.OAK_PLANKS).strength(2.5f,6).noOcclusion());registerDefaultState(stateDefinition.any().setValue(SpinningWheel.FACING,Direction.NORTH));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(SpinningWheel.FACING);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(SpinningWheel.FACING,c.getHorizontalDirection().getOpposite());}
 @Override public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(0,0,0,16,14,16);}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tile(p,s);}
 @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer server&&l.getBlockEntity(p) instanceof Tile tile)NetworkHooks.openScreen(server,tile,p);return InteractionResult.sidedSuccess(l.isClientSide);}
 @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(s.getBlock()!=next.getBlock()&&l.getBlockEntity(p) instanceof Tile t)for(int i=0;i<4;i++)Containers.dropItemStack(l,p.getX(),p.getY(),p.getZ(),t.items.get(i));super.onRemove(s,l,p,next,moving);}
 public static class Tile extends SidedInventoryEntity implements MenuProvider {
  public NonNullList<ItemStack> items=NonNullList.withSize(5,ItemStack.EMPTY);public int cut;
  public Tile(BlockPos p,BlockState s){super(Content.CUTTING_TILE.get(),p,s);}
  public static String aspect(ItemStack stack){if(!(stack.getItem() instanceof tc4revival.content.WispEssenceItem||stack.getItem() instanceof tc4revival.content.EssentiaPhialItem))return "";var a=((tc4revival.content.StoredAspectItem)stack.getItem()).storedAspects(stack);return a.isEmpty()?"":a.aspects().get(0).tag();}
  public String aspect(){for(int i=1;i<4;i++){var a=aspect(items.get(i));if(!a.isEmpty())return a;}return "";}
  public boolean accepts(ItemStack s){String a=aspect(s);return GemRules.PRIMALS.contains(a)&&(aspect().isEmpty()||aspect().equals(a));}
  @Override public int[] getSlotsForFace(Direction face){return new int[]{0,1,2,3};}
  @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0||slot>0&&slot<4&&accepts(stack);}
  @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face){return canPlaceItem(slot,stack);}
  @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face){return slot>=0&&slot<4;}
  public ItemStack result(){var definition=GemAffinities.find(items.get(0),level);String a=aspect();if(definition==null||!GemRules.PRIMALS.contains(a))return ItemStack.EMPTY;int filled=0;for(int i=1;i<4;i++)if(!items.get(i).isEmpty()){if(!a.equals(aspect(items.get(i))))return ItemStack.EMPTY;filled++;}var out=InfusedGem.create(a,cut);int potency=GemRules.potency(filled,cut==1,definition.affinities().contains(a));int brittle=GemRules.brittle(filled,definition.affinities().contains(a),definition.aversions().contains(a));if(potency>0)out.enchant(WGMagic.ENCHANTS.get("gemstone_potency").get(),potency);if(brittle>0)out.enchant(WGMagic.ENCHANTS.get("gemstone_brittle").get(),brittle);return out;}
  public void consume(){for(int i=0;i<4;i++)items.get(i).shrink(1);changed();}
  public void changed(){items.set(4,result());setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
  @Override public void load(CompoundTag t){super.load(t);items=NonNullList.withSize(5,ItemStack.EMPTY);ContainerHelper.loadAllItems(t,items);cut=Math.floorMod(t.getInt("GemCut"),2);items.set(4,result());}
  @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);var input=NonNullList.withSize(4,ItemStack.EMPTY);for(int i=0;i<4;i++)input.set(i,items.get(i));ContainerHelper.saveAllItems(t,input);t.putInt("GemCut",cut);}
  @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}@Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
  @Override public int getContainerSize(){return 5;}@Override public boolean isEmpty(){return items.subList(0,4).stream().allMatch(ItemStack::isEmpty);}@Override public ItemStack getItem(int i){return items.get(i);}
  @Override public ItemStack removeItem(int i,int n){var out=ContainerHelper.removeItem(items,i,n);if(i<4)changed();return out;}@Override public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
  @Override public void setItem(int i,ItemStack s){items.set(i,s);if(i<4)changed();}@Override public void clearContent(){items.clear();changed();}
  @Override public boolean stillValid(Player p){return level!=null&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(Vec3Center(worldPosition))<=64;}
  private static net.minecraft.world.phys.Vec3 Vec3Center(BlockPos p){return net.minecraft.world.phys.Vec3.atCenterOf(p);}
  @Override public Component getDisplayName(){return Component.translatable("tile.WG_WoodenDevice.cuttingTable");}@Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new CuttingMenu(id,inv,worldPosition);}
 }
}
