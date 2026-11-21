package automagy.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public final class ExperienceStoneBlock extends BaseEntityBlock {
 public ExperienceStoneBlock(){super(Properties.of().strength(5,2000).sound(SoundType.STONE));}
 @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ExperienceStoneBlockEntity(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Content.EXPERIENCE_STONE_TILE.get(),ExperienceStoneBlockEntity::tick);}
 @Override public boolean hasAnalogOutputSignal(BlockState s){return true;}
 @Override public int getAnalogOutputSignal(BlockState s,Level l,BlockPos p){return l.getBlockEntity(p) instanceof ExperienceStoneBlockEntity t?t.comparator():0;}
 @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof ExperienceStoneBlockEntity tile){int xp=tile.quickDrain();int remaining=xp-Math.max(1,xp/5);if(remaining>0&&level instanceof net.minecraft.server.level.ServerLevel server)net.minecraft.world.entity.ExperienceOrb.award(server,pos.getCenter(),remaining);}super.onRemove(state,level,pos,next,moving);}
}
