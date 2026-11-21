package wgmodern;
import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.block.entity.*;import net.minecraft.world.level.Level;
public class TemporaryLight extends BaseEntityBlock {
 public TemporaryLight(){super(Properties.copy(Blocks.AIR).noCollission().noOcclusion().noLootTable().lightLevel(s->15));}
 @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tile(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,Content.LIGHT_TILE.get(),(level,pos,state,tile)->{if(tile.remaining--<=0)level.removeBlock(pos,false);else tile.setChanged();});}
 public static class Tile extends BlockEntity {public int remaining=400;public Tile(BlockPos p,BlockState s){super(Content.LIGHT_TILE.get(),p,s);}@Override public void load(net.minecraft.nbt.CompoundTag t){super.load(t);remaining=t.getInt("Remaining");}@Override protected void saveAdditional(net.minecraft.nbt.CompoundTag t){super.saveAdditional(t);t.putInt("Remaining",remaining);}}
}
