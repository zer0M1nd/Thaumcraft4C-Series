package gadomancymodern.automation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public class RemoteJarBlock extends StickyJarBlock {
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new RemoteJarEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,gadomancymodern.Gadomancy.REMOTE_JAR_BE.get(),(world,pos,block,jar)->jar.tick());}
}
