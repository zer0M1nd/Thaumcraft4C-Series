package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class WardedBlock extends BaseEntityBlock {
    public static final IntegerProperty LIGHT = IntegerProperty.create("light", 0, 15);
    public WardedBlock() {
        super(Properties.of().strength(-1, 999).noOcclusion().dynamicShape().noLootTable().lightLevel(state -> state.getValue(LIGHT))
                .isRedstoneConductor((state, level, pos) -> original(level, pos).isRedstoneConductor(level, pos))
                .isSuffocating((state, level, pos) -> original(level, pos).isSuffocating(level,pos))
                .isViewBlocking((state, level, pos) -> original(level, pos).isViewBlocking(level,pos))
                .isValidSpawn((state, level, pos, type) -> false).pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(LIGHT, 0));
    }
    private static BlockState original(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof WardedBlockEntity ward ? ward.original() : Blocks.STONE.defaultBlockState();
    }
    /** Used only at legacy proxy hooks that became fixed vanilla tag lookups. */
    public static BlockState unwrap(BlockState state,BlockGetter level,BlockPos pos) {
        return state.getBlock() instanceof WardedBlock ? original(level,pos) : state;
    }
    public static BlockState queryOriginal(BlockGetter level,BlockPos pos) { return unwrap(level.getBlockState(pos),level,pos); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LIGHT); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WardedBlockEntity(pos, state); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return original(level, pos).getShape(level, pos, context); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return original(level, pos).getCollisionShape(level, pos, context); }
    @Override public VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos) { return original(level,pos).getBlockSupportShape(level,pos); }
    @Override public VoxelShape getVisualShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return original(level,pos).getVisualShape(level,pos,context); }
    @Override public float getShadeBrightness(BlockState state,BlockGetter level,BlockPos pos) { return original(level,pos).getShadeBrightness(level,pos); }
    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return original(level, pos).getLightBlock(level, pos); }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) { var original = original(level, pos); original.getBlock().animateTick(original, level, pos, random); }
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) { var original = original(level, pos); original.getBlock().stepOn(level, pos, original, entity); }
    @Override public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) { var original = original(level, pos); original.getBlock().entityInside(original, level, pos, entity); }
    @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) { var original = original(level, pos); original.getBlock().fallOn(level, original, pos, entity, distance); }
    @Override public void attack(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player) { var original=original(level,pos); original.getBlock().attack(original,level,pos,player); }
    @Override public boolean isLadder(BlockState state,net.minecraft.world.level.LevelReader level,BlockPos pos,net.minecraft.world.entity.LivingEntity entity) { return original(level,pos).isLadder(level,pos,entity); }
    @Override public boolean canHarvestBlock(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.entity.player.Player player) { return true; }
    @Override public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) { return false; }
    @Override public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {}
    @Override public float getFriction(BlockState state,net.minecraft.world.level.LevelReader level,BlockPos pos,Entity entity) { return original(level,pos).getFriction(level,pos,entity); }
    @Override public net.minecraft.world.level.block.SoundType getSoundType(BlockState state,net.minecraft.world.level.LevelReader level,BlockPos pos,Entity entity) { return original(level,pos).getSoundType(level,pos,entity); }
    @Override public float getEnchantPowerBonus(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) { return original(level, pos).getEnchantPowerBonus(level, pos); }
    @Override public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) { return original(level, pos).isFertile(level, pos); }
    @Override public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction face, net.minecraftforge.common.IPlantable plant) { return original(level, pos).canSustainPlant(level, pos, face, plant); }
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult hit, BlockGetter level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        return original(level, pos).getCloneItemStack(hit, level, pos, player);
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientBlockExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientBlockExtensions() {
            @Override public boolean addHitEffects(BlockState state, Level level, net.minecraft.world.phys.HitResult hit, net.minecraft.client.particle.ParticleEngine engine) {
                if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) tc4revival.client.WardEffects.hit(level, blockHit);
                return true;
            }
        });
    }
}
