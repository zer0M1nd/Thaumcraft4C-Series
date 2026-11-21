package ttmodern;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

public class GasBlock extends Block {
    public static final IntegerProperty POWER=IntegerProperty.create("power",0,4);
    private final boolean shadow,temporary;
    public GasBlock(boolean shadow,boolean temporary) {
        super(BlockBehaviour.Properties.of().air().noCollission().noOcclusion().replaceable().noLootTable().randomTicks()
                .lightLevel(state->shadow?0:temporary&&state.getValue(POWER)==1?15:12));
        this.shadow=shadow;this.temporary=temporary;registerDefaultState(stateDefinition.any().setValue(POWER,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(POWER);}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.entity.Entity entity){return false;}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    // noCollission only removes physical collisions; the outline is also used by block picking.
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return net.minecraft.world.phys.shapes.Shapes.empty();}
    @Override public int getLightBlock(BlockState state,BlockGetter level,BlockPos pos){return shadow?15:0;}
    @Override public boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos pos){return !shadow;}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){if(!level.isClientSide)level.scheduleTick(pos,this,temporary?delay(level):10);}
    private int delay(Level level){return level.dimension()==Level.NETHER?60:20;}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){if(random.nextFloat()<(temporary?.03f:.0075f))gasEffect(level,pos);}
    private void gasEffect(Level level,BlockPos pos){if(shadow)thaumcraft.api.fx.LegacyEffects.emit(level,"dark_wisp",pos.getCenter(),net.minecraft.world.phys.Vec3.ZERO,0x101010,.125f,40,1,0,-.02f);else thaumcraft.api.fx.LegacyEffects.sparkle(level,pos.getCenter(),temporary?0xee3333:0xcccc33,1,1,0);}
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){tick(state,level,pos,random);}
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(temporary) {
            int radius=state.getValue(POWER)==1?6:1;
            boolean nearby=level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,new AABB(pos.getX()-radius,pos.getY()-radius,pos.getZ()-radius,pos.getX()+radius,pos.getY()+radius,pos.getZ()+radius)).stream().anyMatch(PersonalLight::supportedBy);
            if(!nearby)level.removeBlock(pos,false);else level.scheduleTick(pos,this,delay(level));return;
        }
        int power=state.getValue(POWER);if(power==0)return;
        // Reset the source before spreading, so scheduled and random ticks cannot multiply a source.
        level.setBlock(pos,state.setValue(POWER,0),2);
        for(Direction direction:Direction.values()) {
            var next=pos.relative(direction);if(level.isOutsideBuildHeight(next)||!level.hasChunkAt(next))continue;
            var target=level.getBlockState(next);
            if(target.isAir()&&!target.is(this))level.setBlock(next,defaultBlockState().setValue(POWER,power-1),3);
        }
        gasEffect(level,pos);
    }
}
