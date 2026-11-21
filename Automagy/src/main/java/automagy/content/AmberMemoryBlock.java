package automagy.content;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class AmberMemoryBlock extends Block {
    public static final BooleanProperty ACTIVE=BooleanProperty.create("active");
    public AmberMemoryBlock(){super(Properties.of().strength(1.5f,5).sound(SoundType.METAL));registerDefaultState(stateDefinition.any().setValue(ACTIVE,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ACTIVE);}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){super.onPlace(state,level,pos,old,moving);if(!level.isClientSide&&!old.is(this))level.scheduleTick(pos,this,1);}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){if(!level.isClientSide)level.scheduleTick(pos,this,1);}
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        boolean amber=false;for(Direction direction:Direction.values()){var neighbor=pos.relative(direction);if(level.hasChunkAt(neighbor)&&level.getBlockState(neighbor).is(InventariumEnvironment.AMBER)){amber=true;break;}}
        boolean active=amber&&InventariumBlockEntity.memoryInUse(level,pos);
        if(active!=state.getValue(ACTIVE))level.setBlock(pos,state.setValue(ACTIVE,active),3);
        level.scheduleTick(pos,this,80);
    }
}
