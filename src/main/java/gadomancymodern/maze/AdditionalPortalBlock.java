package gadomancymodern.maze;
import gadomancymodern.Gadomancy;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public class AdditionalPortalBlock extends tc4revival.content.EldritchPortalBlock {
    @Override public String getDescriptionId(){return tc4revival.content.TCEldritch.PORTAL.get().getDescriptionId();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new AdditionalPortalEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,Gadomancy.PORTAL_BE.get(),(world,pos,block,tile)->tile.tick());}
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){return state;}
}
