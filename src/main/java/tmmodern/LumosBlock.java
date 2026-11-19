package tmmodern;
import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.*;import net.minecraft.world.level.*;import net.minecraft.core.*;import net.minecraft.world.phys.shapes.*;import net.minecraft.world.level.block.entity.*;
public class LumosBlock extends BaseEntityBlock {
    public LumosBlock(){super(Properties.copy(Blocks.LIGHT).lightLevel(s->15).noCollission().noOcclusion().strength(0).noLootTable());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(6,6,6,10,10,10);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new Tile(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?createTickerHelper(type,TaintedMagic.LUMOS_TILE.get(),Tile::tick):null;}
    public static class Tile extends BlockEntity {
        public Tile(BlockPos p,BlockState s){super(TaintedMagic.LUMOS_TILE.get(),p,s);}
        public static void tick(Level l,BlockPos p,BlockState s,Tile t){if(l.random.nextInt(15)==0)TMEffects.localSparkles(l,net.minecraft.world.phys.Vec3.atCenterOf(p),1,6,1.75f,-.5f);}
    }
}
