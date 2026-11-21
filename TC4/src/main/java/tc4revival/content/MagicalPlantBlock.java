package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The three ordinary luminous plants; ethereal bloom has a separate cleansing tile. */
public  class MagicalPlantBlock extends BushBlock {
    public enum Kind {
        SHIMMERLEAF(2,"shimmerleaf"), CINDERPEARL(3,"cinderpearl"), VISHROOM(5,"vishroom");
        public final int metadata; public final String path;
        Kind(int metadata,String path) { this.metadata=metadata; this.path=path; }
        public String texture() { return this==VISHROOM ? "manashroom" : path; }
    }
    private final Kind kind;
    public MagicalPlantBlock(Kind kind) { super(Properties.of().noCollission().instabreak().sound(SoundType.GRASS).randomTicks().lightLevel(state->8)); this.kind=kind; }
    public Kind kind() { return kind; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return box(1.6,0,1.6,14.4,12.8,14.4); }
    @Override protected boolean mayPlaceOn(BlockState soil,BlockGetter level,BlockPos pos) {
        return kind==Kind.CINDERPEARL ? soil.is(Blocks.SAND) || soil.is(Blocks.RED_SAND) || soil.is(net.minecraft.tags.BlockTags.TERRACOTTA) : super.mayPlaceOn(soil,level,pos);
    }
    @Override public void randomTick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,RandomSource random) {
        if (!state.canSurvive(level,pos)) level.destroyBlock(pos,true);
    }
    @Override public net.minecraftforge.common.PlantType getPlantType(BlockGetter level,BlockPos pos) { return kind==Kind.CINDERPEARL ? net.minecraftforge.common.PlantType.DESERT : net.minecraftforge.common.PlantType.PLAINS; }
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction side) { return 100; }
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction side) { return 60; }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.Entity entity) {
        if (kind==Kind.VISHROOM && !level.isClientSide && entity instanceof net.minecraft.world.entity.LivingEntity living)
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION,200,0));
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if (kind==Kind.CINDERPEARL) {
            if (!random.nextBoolean()) return;
            double x=pos.getX()+.5+(random.nextFloat()-random.nextFloat())*.1,y=pos.getY()+.6+(random.nextFloat()-random.nextFloat())*.1,z=pos.getZ()+.5+(random.nextFloat()-random.nextFloat())*.1;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,x,y,z,0,0,0);
            level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,x,y,z,0,0,0);
        } else if (random.nextInt(3)==0) tc4revival.client.PrimalOrbEffects.plant(level,pos,kind==Kind.VISHROOM);
    }
}
