package wgmodern;
import java.util.*;import net.minecraft.core.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.world.*;import net.minecraft.world.entity.*;import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.*;import net.minecraft.world.item.context.BlockPlaceContext;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.entity.*;import net.minecraft.world.level.block.state.*;import net.minecraft.world.level.block.state.properties.*;import net.minecraft.world.phys.*;import net.minecraft.world.phys.shapes.*;import net.minecraftforge.common.capabilities.*;import net.minecraftforge.common.util.LazyOptional;import net.minecraftforge.items.*;
/** Creative-only three-part loot tomb. Opening ejects all six inventory slots once. */
public class Sarcophagus extends BaseEntityBlock {
 public static final IntegerProperty PART=IntegerProperty.create("part",0,2);public static final BooleanProperty OPEN=BooleanProperty.create("open");private static final Set<Tile> BREAKING=Collections.newSetFromMap(new IdentityHashMap<>());
 public Sarcophagus(){super(Properties.copy(Blocks.STONE).strength(4,6).isValidSpawn((state,level,pos,type)->false).noOcclusion());registerDefaultState(stateDefinition.any().setValue(PART,1).setValue(OPEN,false).setValue(SpinningWheel.FACING,Direction.NORTH));}
 @Override public String getDescriptionId(){return "tile.WG_StoneDevice.sarcophagus";}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(PART,OPEN,SpinningWheel.FACING);}
 public static Direction along(BlockState s){return s.getValue(SpinningWheel.FACING).getAxis()==Direction.Axis.Z?Direction.EAST:Direction.SOUTH;}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=defaultBlockState().setValue(SpinningWheel.FACING,c.getHorizontalDirection().getOpposite());for(int n:List.of(-1,1)){var p=c.getClickedPos().relative(along(s),n);if(!c.getLevel().getBlockState(p).canBeReplaced(c)||c.getPlayer()!=null&&(!c.getLevel().mayInteract(c.getPlayer(),p)||!c.getPlayer().mayUseItemAt(p,c.getClickedFace(),c.getItemInHand())))return null;}return s;}
 @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){if(!l.isClientSide)for(int n:List.of(-1,1))l.setBlock(p.relative(along(s),n),s.setValue(PART,n+1),3);}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tile(p,s);}@Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 public static BlockPos centre(BlockPos p,BlockState s){return p.relative(along(s),1-s.getValue(PART));}
 @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.block();}
 @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(!l.isClientSide){var centre=centre(p,s);if(l.getBlockEntity(centre) instanceof Tile t&&!t.getBlockState().getValue(OPEN)){for(int i=-1;i<=1;i++){var q=centre.relative(along(s),i);if(l.getBlockState(q).is(this))l.setBlock(q,l.getBlockState(q).setValue(OPEN,true),3);}t.spill(.75);}}return InteractionResult.sidedSuccess(l.isClientSide);}
 @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&!l.isClientSide){var centre=centre(p,s);if(l.getBlockEntity(centre) instanceof Tile t&&BREAKING.add(t)){try{t.spill(.1);for(int n=-1;n<=1;n++){var q=centre.relative(along(s),n);if(!q.equals(p)&&l.getBlockState(q).is(this)&&centre(q,l.getBlockState(q)).equals(centre))l.removeBlock(q,false);}}finally{BREAKING.remove(t);}}}super.onRemove(s,l,p,next,moving);}
 public static class Tile extends BlockEntity {
  public final ItemStackHandler items=new ItemStackHandler(6){@Override protected void onContentsChanged(int slot){setChanged();}};
  private LazyOptional<IItemHandler> cap=LazyOptional.empty();private Tile capabilityMaster;private boolean capabilitiesValid=true;
  public Tile(BlockPos p,BlockState s){super(Content.SARCOPHAGUS_TILE.get(),p,s);}
  public Tile master(){var p=centre(worldPosition,getBlockState());return level!=null&&level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof Tile t?t:null;}
  public void spill(double height){for(int i=0;i<6;i++){var stack=items.getStackInSlot(i);if(!stack.isEmpty())Containers.dropItemStack(level,worldPosition.getX()+level.random.nextFloat()*.8+.1,worldPosition.getY()+level.random.nextFloat()*.8+height,worldPosition.getZ()+level.random.nextFloat()*.8+.1,stack.copy());items.setStackInSlot(i,ItemStack.EMPTY);}}
  @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.put("Inventory",items.serializeNBT());}@Override public void load(CompoundTag t){super.load(t);items.deserializeNBT(t.getCompound("Inventory"));}
  @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side){
   if(capability!=ForgeCapabilities.ITEM_HANDLER)return super.getCapability(capability,side);
   var m=master();if(isRemoved()||!capabilitiesValid||m==null||m.isRemoved()||!m.capabilitiesValid)return LazyOptional.empty();
   if(capabilityMaster!=m||!cap.isPresent()){
    cap.invalidate();capabilityMaster=m;cap=LazyOptional.of(()->m.items);
    // Each part owns its handle, and also loses it when the shared inventory disappears.
    if(m!=this){var delegated=cap;m.getCapability(ForgeCapabilities.ITEM_HANDLER).addListener(ignored->delegated.invalidate());}
   }
   return cap.cast();
  }
  @Override public void invalidateCaps(){super.invalidateCaps();capabilitiesValid=false;cap.invalidate();capabilityMaster=null;}
  @Override public void reviveCaps(){super.reviveCaps();capabilitiesValid=true;}
  @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(3);}
 }
 public static class Decoration extends Block {
  public static final IntegerProperty VARIANT=IntegerProperty.create("variant",0,2);public Decoration(){super(Properties.copy(Blocks.STONE).strength(4,6).isValidSpawn((state,level,pos,type)->false).noOcclusion());registerDefaultState(stateDefinition.any().setValue(VARIANT,0));}@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){b.add(VARIANT);}@Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(VARIANT,c.getLevel().random.nextInt(3));}@Override public String getDescriptionId(){return "tile.WG_StoneDevice.obsidianDecoration0";}
 }
}
