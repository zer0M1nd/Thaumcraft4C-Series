package thaumicfutures.expansion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.NitorBlock;

public class DyedNitor extends NitorBlock {
    public final DyeColor color;
    public DyedNitor(DyeColor color){this.color=color;}
    private void emit(Level level,BlockPos pos){DyedNitorParticles.emit(level,pos,color.getMapColor().col);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Tile(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?createTickerHelper(type,RestoredContent.NITOR_ENTITY.get(),(world,pos,block,tile)->emit(world,pos)):null;
    }
    public static class Tile extends BlockEntity {
        public Tile(BlockPos pos,BlockState state){super(RestoredContent.NITOR_ENTITY.get(),pos,state);}
    }
}
