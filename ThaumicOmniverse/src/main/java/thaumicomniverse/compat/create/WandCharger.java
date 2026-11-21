package thaumicomniverse.compat.create;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import tc4revival.aspect.*;
import tc4revival.content.*;

/** A receiving leaf. It never offers its connection or buffer to other network consumers. */
public class WandCharger extends BaseEntityBlock implements com.simibubi.create.content.equipment.wrench.IWrenchable,com.simibubi.create.api.contraption.transformable.TransformableBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty ROTATED=net.minecraft.world.level.block.state.properties.BooleanProperty.create("rotated");
    public WandCharger(){super(Properties.copy(Blocks.OAK_PLANKS).strength(2.5f).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP).setValue(ROTATED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,ROTATED);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){
        var state=defaultBlockState().setValue(FACING,context.getClickedFace());
        return canSurvive(state,context.getLevel(),context.getClickedPos())?state:null;
    }
    public static Direction port(BlockState state){
        var face=state.getValue(FACING);
        if(state.getValue(ROTATED))return face.getAxis()==Direction.Axis.Y?Direction.NORTH:Direction.UP;
        return face.getAxis()==Direction.Axis.X?Direction.NORTH:Direction.EAST;
    }
    public static BlockState frame(BlockState state,Direction face,Direction port){
        var result=state.setValue(FACING,face).setValue(ROTATED,false);
        return result.setValue(ROTATED,port.getAxis()!=port(result).getAxis());
    }
    @Override public BlockState rotate(BlockState state,Rotation rotation){return frame(state,rotation.rotate(state.getValue(FACING)),rotation.rotate(port(state)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return frame(state,mirror.mirror(state.getValue(FACING)),mirror.mirror(port(state)));}
    @Override public BlockState transform(BlockState state,com.simibubi.create.content.contraptions.StructureTransform transform){
        return frame(state,ArcaneContraptions.direction(transform,state.getValue(FACING)),ArcaneContraptions.direction(transform,port(state)));
    }
    @Override public InteractionResult onWrenched(BlockState state,net.minecraft.world.item.context.UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();
        if(!level.isClientSide){level.setBlock(pos,state.cycle(ROTATED),3);if(level.getBlockEntity(pos) instanceof Tile tile)tile.refreshNetwork();}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    public static boolean connects(BlockState state,net.minecraft.world.phys.Vec3 direction){
        var nearest=Direction.getNearest(direction.x,direction.y,direction.z);
        return direction.lengthSqr()>1e-8&&nearest.getAxis()==port(state).getAxis();
    }
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return switch(state.getValue(FACING)){
        case UP->Block.box(2,0,2,14,12,14);case DOWN->Block.box(2,4,2,14,16,14);
        case NORTH->Block.box(2,2,4,14,14,16);case SOUTH->Block.box(2,2,0,14,14,12);
        case WEST->Block.box(4,2,2,16,14,14);case EAST->Block.box(0,2,2,12,14,14);
    };}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Tile(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,ArcaneCreateContent.CHARGER_TILE.get(),(l,p,s,t)->t.tick());}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(pos) instanceof Tile tile)||!ArcaneContraptions.enabled())return InteractionResult.PASS;
        var held=player.getItemInHand(hand);
        if(!held.isEmpty()&&!tile.inventory.isItemValid(0,held))return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(held.isEmpty()){
            var extracted=tile.inventory.extractItem(0,1,false);if(!extracted.isEmpty())player.setItemInHand(hand,extracted);
        }else player.setItemInHand(hand,tile.inventory.insertItem(0,held,false));
        return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof Tile tile)Block.popResource(level,pos,tile.inventory.getStackInSlot(0));
        super.onRemove(state,level,pos,next,moving);
    }
    public static class Tile extends VisNetworkNodeBlockEntity {
        private net.minecraft.world.phys.Vec3 beamParent;
        private CompoundTag repairVis=new CompoundTag(),beam=new CompoundTag();
        public net.minecraft.world.phys.Vec3 beamParent(){return beamParent;}
        public CompoundTag beam(){return beam;}
        public final ItemStackHandler inventory=new ItemStackHandler(1){
            @Override public int getSlotLimit(int slot){return 1;}
            @Override public boolean isItemValid(int slot,ItemStack stack){return VisCharging.accepts(stack)||VisItemRepair.needsRepair(stack);}
            @Override protected void onContentsChanged(int slot){changed();}
        };
        private LazyOptional<IItemHandler> items=LazyOptional.of(()->inventory);
        public Tile(BlockPos pos,BlockState state){super(ArcaneCreateContent.CHARGER_TILE.get(),pos,state);}
        @Override public int range(){return 8;}
        @Override public boolean source(){return false;}
        @Override public boolean relay(){return false;}
        @Override public boolean connectsTo(thaumcraft.api.visnet.VisEndpoint other){return connects(getBlockState(),other.point().subtract(point()));}
        public void tick(){
            if(level==null||level.isClientSide||!ArcaneContraptions.enabled())return;
            tickNetwork();boolean changed=VisCharging.charge(inventory.getStackInSlot(0),this::consumeVis);
            changed|=VisItemRepair.repair(inventory.getStackInSlot(0),level,null,repairVis,this::consumeVis);
            var support=worldPosition.relative(getBlockState().getValue(FACING).getOpposite());
            if(level.hasChunkAt(support)&&level.getBlockEntity(support) instanceof com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity deployer){
                var worker=deployer.getPlayer();
                if(worker!=null){
                    var held=worker.getMainHandItem();
                    boolean repaired=VisCharging.charge(held,this::consumeVis);
                    repaired|=VisItemRepair.repair(held,level,worker,repairVis,this::consumeVis);
                    if(repaired){deployer.notifyUpdate();changed=true;}
                }
            }
            if(changed)changed();else if(level.getGameTime()%40==0)setChanged();
        }
        @Override protected void onVisConsumed(Aspect aspect){
            if(level!=null&&level.getGameTime()>=beam.getLong("Until")){
                beam.putLong("Until",level.getGameTime()+5);beam.putInt("Color",aspect.color());changed();
            }
        }
        @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(){
            var box=new net.minecraft.world.phys.AABB(worldPosition).inflate(1);
            return beamParent==null?box:box.minmax(new net.minecraft.world.phys.AABB(beamParent,beamParent).inflate(.2));
        }
        public void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
        @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Inventory",inventory.serializeNBT());tag.put("RepairVis",repairVis);}
        @Override public void load(CompoundTag tag){super.load(tag);inventory.deserializeNBT(tag.getCompound("Inventory"));repairVis=tag.getCompound("RepairVis");beam=tag.getCompound("VisBeam");beamParent=readParent(tag);}
        @Override public CompoundTag getUpdateTag(){var tag=saveWithoutMetadata();writeParent(tag,networkParent());tag.put("VisBeam",beam.copy());return tag;}
        @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ITEM_HANDLER&&!isRemoved()?items.cast():super.getCapability(cap,side);}
        @Override public void invalidateCaps(){super.invalidateCaps();items.invalidate();}
        @Override public void reviveCaps(){super.reviveCaps();items=LazyOptional.of(()->inventory);}
    }
    public static void writeParent(CompoundTag tag,thaumcraft.api.visnet.VisEndpoint parent){
        tag.remove("VisParent");if(parent==null)return;var point=parent.point();var p=new CompoundTag();p.putDouble("X",point.x);p.putDouble("Y",point.y);p.putDouble("Z",point.z);tag.put("VisParent",p);
    }
    public static net.minecraft.world.phys.Vec3 readParent(CompoundTag tag){
        if(!tag.contains("VisParent"))return null;var p=tag.getCompound("VisParent");var v=new net.minecraft.world.phys.Vec3(p.getDouble("X"),p.getDouble("Y"),p.getDouble("Z"));
        return Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z)?v:null;
    }
}
