package thaumicomniverse.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.aspects.IEssentiaStorage;

public class PortableEssentiaInterface extends WrenchableDirectionalBlock implements IBE<PortableEssentiaInterface.Tile> {
    public PortableEssentiaInterface(){super(Properties.copy(Blocks.IRON_BLOCK).strength(3).noOcclusion());}
    @Override public Class<Tile> getBlockEntityClass(){return Tile.class;}
    @Override public BlockEntityType<? extends Tile> getBlockEntityType(){return ArcaneCreateContent.INTERFACE_TILE.get();}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return com.simibubi.create.AllShapes.PORTABLE_STORAGE_INTERFACE.get(state.getValue(FACING));}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){if(level.getBlockEntity(pos) instanceof Tile tile)tile.neighbourChanged();}
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof Tile tile&&tile.canTransfer()?15:0;}
    public static class Tile extends PortableStorageInterfaceBlockEntity implements IEssentiaStorage,EssentiaTransport,EssentiaSource,RevealedAspectContainer {
        public Tile(BlockPos pos,BlockState state){super(ArcaneCreateContent.INTERFACE_TILE.get(),pos,state);}
        public boolean renderConnected(){return transferTimer>=4&&transferTimer<=getTransferTimeout()+4;}
        public float renderExtension(float partial){return (float)(Math.pow(connectionAnimation.getValue(partial),2)*distance/2);}
        @Override protected void invalidateCapability(){}
        private MountedArcaneStorage storage(){
            return ArcaneContraptions.enabled()&&canTransfer()&&connectedEntity instanceof AbstractContraptionEntity entity&&entity.getContraption()!=null?MountedArcaneStorage.of(entity.getContraption()):null;
        }
        @Override public AspectAmounts contents(){var storage=storage();return storage==null?new AspectAmounts():storage.contents();}
        @Override public AspectAmounts revealedAspects(){return contents();}
        @Override public int totalCapacity(){var storage=storage();return storage==null?0:storage.totalCapacity();}
        @Override public int capacity(Aspect a){var storage=storage();return storage==null?0:storage.capacity(a);}
        @Override public int insert(Aspect a,int n,boolean simulate){var storage=storage();int moved=storage==null?0:storage.insert(a,n,simulate);if(moved>0&&!simulate)onContentTransferred();return moved;}
        @Override public int extract(Aspect a,int n,boolean simulate){var storage=storage();int moved=storage==null?0:storage.extract(a,n,simulate);if(moved>0&&!simulate)onContentTransferred();return moved;}
        @Override public boolean contains(Aspect a,int n){return n>=0&&extract(a,n,true)==n;}
        @Override public boolean takeFromContainer(Aspect a,int n){return contains(a,n)&&extract(a,n,false)==n;}
        @Override public java.util.List<? extends EssentiaSource> availabilitySources(){var storage=storage();return storage==null?java.util.List.of():java.util.List.copyOf(storage.entries());}
        @Override public boolean connectable(Direction side){return side!=getBlockState().getValue(FACING);}
        @Override public boolean canInput(Direction side){return connectable(side);}
        @Override public boolean canOutput(Direction side){return connectable(side);}
        @Override public Aspect suctionType(Direction side){return null;}
        @Override public int suction(Direction side){return storage()!=null?64:0;}
        @Override public int minimumSuction(){return 0;}
        @Override public Aspect essentiaType(Direction side){return contents().aspects().stream().filter(a->contents().amount(a)>0).findFirst().orElse(null);}
        @Override public int essentiaAmount(Direction side){var a=essentiaType(side);return a==null?0:contents().amount(a);}
        @Override public int takeEssentia(Aspect a,int n,Direction side){return canOutput(side)?extract(a,n,false):0;}
        @Override public int addEssentia(Aspect a,int n,Direction side){return canInput(side)?insert(a,n,false):0;}
        @Override public void tick(){
            super.tick();if(level==null||level.isClientSide||level.getGameTime()%5!=0||storage()==null)return;
            for(var side:Direction.values()){
                if(!canInput(side))continue;var pos=worldPosition.relative(side);if(!level.hasChunkAt(pos))continue;
                if(!(level.getBlockEntity(pos) instanceof EssentiaTransport source)||!source.connectable(side.getOpposite())||!source.canOutput(side.getOpposite())||source.suction(side.getOpposite())>=64||source.minimumSuction()>64)continue;
                var a=source.essentiaType(side.getOpposite());if(a==null||insert(a,1,true)==0)continue;
                int taken=source.takeEssentia(a,1,side.getOpposite());if(taken>0){insert(a,taken,false);break;}
            }
        }
    }
}
