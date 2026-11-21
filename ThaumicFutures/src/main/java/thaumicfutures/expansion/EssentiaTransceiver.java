package thaumicfutures.expansion;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.*;
import tc4revival.aspect.*;
import tc4revival.aspect.Aspect;
import tc4revival.content.*;
import thaumcraft.api.aspects.*;

/** Wireless endpoint with a real one-unit transit buffer; failed deliveries never destroy essentia. */
public class EssentiaTransceiver extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public final boolean toJars;
    public EssentiaTransceiver(boolean toJars){super(Properties.copy(Blocks.IRON_BLOCK).strength(2).noOcclusion());this.toJars=toJars;registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getClickedFace());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(3,3,3,13,13,13);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Tile(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,RestoredContent.TRANSCEIVER_ENTITY.get(),(world,pos,block,tile)->tile.tick());
    }
    public static class Tile extends BlockEntity implements EssentiaTransport,RevealedAspectContainer {
        private Aspect buffer;
        private List<BlockPos> jars=List.of();private long rescan;
        public Tile(BlockPos pos,BlockState state){super(RestoredContent.TRANSCEIVER_ENTITY.get(),pos,state);}
        public Direction front(){return getBlockState().getValue(FACING);}
        private boolean toJars(){return ((EssentiaTransceiver)getBlockState().getBlock()).toJars;}
        private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
        private void scan(){
            if(level.getGameTime()<rescan)return;rescan=level.getGameTime()+100;
            var found=new ArrayList<BlockPos>();var server=(ServerLevel)level;
            for(int x=(worldPosition.getX()-16)>>4;x<=(worldPosition.getX()+16)>>4;x++)for(int z=(worldPosition.getZ()-16)>>4;z<=(worldPosition.getZ()+16)>>4;z++){
                var chunk=server.getChunkSource().getChunkNow(x,z);if(chunk==null)continue;
                for(var entry:chunk.getBlockEntities().entrySet()){
                    var delta=entry.getKey().subtract(worldPosition);
                    if(Math.abs(delta.getX())>16||Math.abs(delta.getY())>16||Math.abs(delta.getZ())>16
                            ||delta.getX()*front().getStepX()+delta.getY()*front().getStepY()+delta.getZ()*front().getStepZ()<0)continue;
                    if(entry.getValue() instanceof EssentiaSource&&EssentiaStorageApi.supports(entry.getValue(),null))found.add(entry.getKey().immutable());
                }
            }
            found.sort(Comparator.comparingDouble(p->p.distSqr(worldPosition)));jars=List.copyOf(found);
        }
        public void tick(){
            if(!(level instanceof ServerLevel server)||level.getGameTime()%5!=0||!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled())return;
            var behind=worldPosition.relative(front().getOpposite());if(!level.hasChunkAt(behind))return;
            var pipe=level.getBlockEntity(behind) instanceof EssentiaTransport transport?transport:null;
            if(toJars()){
                scan();
                if(buffer==null&&pipe!=null&&pipe.canOutput(front())&&pipe.suction(front())<128&&pipe.minimumSuction()<=128){
                    var aspect=pipe.essentiaType(front());
                    if(aspect!=null&&jars.stream().anyMatch(p->EssentiaStorageApi.atInventory(server,p).insert(aspect,1,true)>0)
                            &&pipe.takeEssentia(aspect,1,front())==1){buffer=aspect;changed();}
                }
                if(buffer!=null)for(var pos:jars)if(EssentiaStorageApi.atInventory(server,pos).insert(buffer,1,false)>0){pulse(pos);buffer=null;changed();break;}
            }else if(pipe!=null&&pipe.canInput(front())&&pipe.suction(front())>0){
                var wanted=pipe.suctionType(front());
                if(buffer==null&&wanted!=null){
                    var source=EssentiaSources.drain(this,wanted,front(),16,false);
                    if(source.isPresent()){buffer=wanted;pulse(source.get());changed();}
                }
                if(buffer!=null&&pipe.addEssentia(buffer,1,front())>0){buffer=null;changed();}
            }
        }
        private void pulse(BlockPos pos) {
            if (level instanceof ServerLevel server && buffer != null)
                tc4revival.network.EssentiaSourcePacket.send(server, toJars() ? pos : worldPosition, toJars() ? worldPosition : pos, buffer.color());
        }
        @Override public AspectAmounts revealedAspects(){return buffer==null?new AspectAmounts():new AspectAmounts().add(buffer,1);}
        @Override public boolean connectable(Direction side){return side==front().getOpposite();}
        @Override public boolean canInput(Direction side){return toJars()&&connectable(side);}
        @Override public boolean canOutput(Direction side){return !toJars()&&connectable(side);}
        @Override public Aspect suctionType(Direction side){return buffer;}
        @Override public int suction(Direction side){return toJars()&&buffer==null&&connectable(side)?128:0;}
        @Override public int minimumSuction(){return 0;}
        @Override public Aspect essentiaType(Direction side){return buffer;}
        @Override public int essentiaAmount(Direction side){return buffer==null?0:1;}
        @Override public int addEssentia(Aspect aspect,int amount,Direction side){
            if(!thaumicfutures.FutureFeatures.Group.RESTORATION.enabled()||aspect==null||amount<=0||buffer!=null||!canInput(side))return 0;
            buffer=aspect;changed();return 1;
        }
        @Override public int takeEssentia(Aspect aspect,int amount,Direction side){
            if(amount<=0||buffer==null||!buffer.equals(aspect)||!canOutput(side))return 0;
            buffer=null;changed();return 1;
        }
        @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(buffer!=null)tag.putString("Buffer",buffer.tag());}
        @Override public void load(CompoundTag tag){super.load(tag);buffer=TCAspects.catalog().find(tag.getString("Buffer")).orElse(null);}
        @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
        @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    }
}
