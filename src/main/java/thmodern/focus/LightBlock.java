package thmodern.focus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;
public final class LightBlock extends Block implements EntityBlock {
    public static final IntegerProperty SHADE=IntegerProperty.create("shade",0,15);
    private final boolean solar;
    public LightBlock(boolean solar){super(Properties.of().strength(0).noCollission().noOcclusion().lightLevel(s->14).noLootTable().sound(SoundType.EMPTY));this.solar=solar;registerDefaultState(stateDefinition.any().setValue(SHADE,11));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(SHADE);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return Block.box(4.8,4.8,4.8,11.2,11.2,11.2);}
    @Override public void entityInside(BlockState state,Level world,BlockPos pos,net.minecraft.world.entity.Entity entity){if(!world.isClientSide&&solar&&entity instanceof net.minecraft.world.entity.LivingEntity living&&living.getMobType()==net.minecraft.world.entity.MobType.UNDEAD)living.setSecondsOnFire(5);}
    @Override public Orb newBlockEntity(BlockPos pos,BlockState state){return new Orb(pos,state);}

    public static final class Orb extends net.minecraft.world.level.block.entity.BlockEntity {
        public Orb(BlockPos pos,BlockState state){super(thmodern.Horizons.LIGHT_ENTITY.get(),pos,state);}
    }
}
