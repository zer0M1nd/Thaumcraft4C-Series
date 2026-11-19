package wgmodern;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.registries.RegistryObject;

/** The author's registered but research-hidden two-block mirror. It never teleports. */
public class WallMirror extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final RegistryObject<Block> BLOCK=Content.BLOCKS.register("wall_mirror",WallMirror::new);
    public static final RegistryObject<Item> ITEM=Content.ITEMS.register("wall_mirror",()->new MirrorItem(BLOCK.get()));
    public static final RegistryObject<BlockEntityType<Tile>> TILE=Content.TILES.register("wall_mirror",()->BlockEntityType.Builder.of(Tile::new,BLOCK.get()).build(null));
    public static void register(){thaumcraft.api.research.ResearchCategories.registerEnablement("witchinggadgets:WALLMIRROR",()->tc4revival.config.TCConfig.SERVER.allowMirrors.get());}

    public WallMirror(){super(Properties.of().strength(0,0).noOcclusion().isValidSpawn((s,l,p,t)->false));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(HALF,DoubleBlockHalf.LOWER));}
    @Override public String getDescriptionId(){return "tile.WG_WallMirror";}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,HALF);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var pos=c.getClickedPos();return pos.getY()+1<c.getLevel().getMaxBuildHeight()&&c.getLevel().isEmptyBlock(pos.above())?defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite()):null;}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity player,ItemStack stack){l.setBlock(p.above(),s.setValue(HALF,DoubleBlockHalf.UPPER),UPDATE_ALL);}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(s.getValue(FACING)){case SOUTH->box(0,0,0,16,16,1);case WEST->box(15,0,0,16,16,16);case EAST->box(0,0,0,1,16,16);default->box(0,0,15,16,16,16);};}
    @Override public VoxelShape getOcclusionShape(BlockState s,BlockGetter l,BlockPos p){return Shapes.empty();}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(!l.isClientSide&&!player.isShiftKeyDown()){var base=s.getValue(HALF)==DoubleBlockHalf.UPPER?p.below():p;if(l.getBlockEntity(base) instanceof Tile tile)tile.toggle();}return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public void neighborChanged(BlockState s,Level l,BlockPos p,Block other,BlockPos from,boolean moving){var partner=s.getValue(HALF)==DoubleBlockHalf.UPPER?p.below():p.above();if(l.isEmptyBlock(partner))l.removeBlock(p,false);}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())){var partner=s.getValue(HALF)==DoubleBlockHalf.UPPER?p.below():p.above();var state=l.getBlockState(partner);if(state.is(this)&&state.getValue(HALF)!=s.getValue(HALF)&&state.getValue(FACING)==s.getValue(FACING))l.removeBlock(partner,false);}super.onRemove(s,l,p,next,moving);}
    @Override public void playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){if(!l.isClientSide&&player.isCreative()&&s.getValue(HALF)==DoubleBlockHalf.LOWER)popResource(l,p,new ItemStack(ITEM.get()));super.playerWillDestroy(l,p,s,player);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tile(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return createTickerHelper(t,TILE.get(),(world,pos,state,tile)->tile.tick());}

    public static class MirrorItem extends BlockItem {
        public MirrorItem(Block block){super(block,new Item.Properties());}
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions(){private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer renderer;@Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new WallMirrorClient.ItemRenderer();return renderer;}});}
    }
    public static class Tile extends BlockEntity {
        public int activationAnimation,animation;
        public boolean active,activating,deactivating;
        public Tile(BlockPos p,BlockState s){super(TILE.get(),p,s);}
        public boolean dummy(){return getBlockState().getValue(HALF)==DoubleBlockHalf.UPPER;}
        public Direction facing(){return getBlockState().getValue(FACING);}
        public void toggle(){if(active&&!deactivating)deactivating=true;else if(!active&&!activating)activating=true;sync();}
        public void tick(){if(dummy())return;boolean oldActive=active,oldStarting=activating,oldStopping=deactivating;var step=WallMirrorRules.advance(activationAnimation,active,activating,deactivating);activationAnimation=step.frame();active=step.active();activating=step.activating();deactivating=step.deactivating();if(oldStarting||oldStopping)animation=0;if(oldActive!=active||oldStarting!=activating||oldStopping!=deactivating)sync();}
        public void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),UPDATE_CLIENTS);}
        @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putBoolean("isActive",active);tag.putBoolean("isDummy",dummy());tag.putInt("facing",facing().get3DDataValue());}
        @Override public void load(CompoundTag tag){super.load(tag);active=tag.getBoolean("isActive");activationAnimation=tag.getInt("activationAnimation");animation=tag.getInt("animation");activating=tag.getBoolean("activating");deactivating=tag.getBoolean("deactivating");}
        @Override public CompoundTag getUpdateTag(){var tag=saveWithoutMetadata();tag.putInt("activationAnimation",activationAnimation);tag.putInt("animation",animation);tag.putBoolean("activating",activating);tag.putBoolean("deactivating",deactivating);return tag;}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
        @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).expandTowards(0,1,0);}
        public AABB reflectedArea(){var f=facing();double minX=f.getAxis()==Direction.Axis.Z?-2:f==Direction.WEST?-8:-.5,maxX=f.getAxis()==Direction.Axis.Z?2:f==Direction.WEST?.5:8,minZ=f.getAxis()==Direction.Axis.X?-2:f==Direction.NORTH?-8:-.5,maxZ=f.getAxis()==Direction.Axis.X?2:f==Direction.NORTH?.5:8;return new AABB(minX,-1,minZ,maxX,2,maxZ).move(worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5);}
        public List<Player> reflectedPlayers(){return level==null?List.of():level.getEntitiesOfClass(Player.class,reflectedArea());}
    }
}
