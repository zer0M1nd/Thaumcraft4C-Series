package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.*;
/** Entity-selective invisible collision. Periodic orphan cleanup retains the old delay. */
public  class WardingBarrierBlock extends BaseEntityBlock {
    public static final TagKey<Block> LEGACY_COSMETIC=BlockTags.create(ResourceLocation.parse("thaumcraft:legacy_cosmetic_solid"));
    public WardingBarrierBlock() { super(Properties.of().replaceable().noOcclusion().dynamicShape().noLootTable().isSuffocating((s,w,p)->false).isViewBlocking((s,w,p)->false).isRedstoneConductor((s,w,p)->false).isValidSpawn((s,w,p,t)->false)); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        if(!(context instanceof EntityCollisionContext entityContext)||!(entityContext.getEntity() instanceof LivingEntity living)||living instanceof Player)return Shapes.empty();
        var source=level.getBlockState(pos.below()).is(LEGACY_COSMETIC)?pos.below():pos.below(2);
        return level instanceof Level world&&world.hasNeighborSignal(source)?Shapes.empty():Shapes.block();
    }
    @Override public boolean isPathfindable(BlockState state,BlockGetter level,BlockPos pos,PathComputationType type) {
        for(int below=1;below<=2;below++)if(level.getBlockEntity(pos.below(below)) instanceof WardingPavingBlockEntity)return level instanceof Level world&&world.hasNeighborSignal(pos.below(below));
        return true;
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new WardingBarrierBlockEntity(pos,state); }
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type) { return level.isClientSide?null:createTickerHelper(type,TCArcane.WARDING_BARRIER_ENTITY.get(),(world,pos,current,tile)->tile.tick()); }
}
