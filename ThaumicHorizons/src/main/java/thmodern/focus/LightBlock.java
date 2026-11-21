package thmodern.focus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;
public final class LightBlock extends Block {
    public static final IntegerProperty SHADE=IntegerProperty.create("shade",0,15);
    private final boolean solar;
    public LightBlock(boolean solar){super(Properties.of().strength(0).noCollission().noOcclusion().lightLevel(s->14).noLootTable().sound(SoundType.EMPTY));this.solar=solar;registerDefaultState(stateDefinition.any().setValue(SHADE,11));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(SHADE);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return Block.box(4.8,4.8,4.8,11.2,11.2,11.2);}
    @Override public void entityInside(BlockState state,Level world,BlockPos pos,net.minecraft.world.entity.Entity entity){if(!world.isClientSide&&solar&&entity instanceof net.minecraft.world.entity.LivingEntity living&&living.getMobType()==net.minecraft.world.entity.MobType.UNDEAD)living.setSecondsOnFire(5);}
    @Override public void animateTick(BlockState state,Level world,BlockPos pos,net.minecraft.util.RandomSource random){int color=IlluminationFocusItem.rgb(state.getValue(SHADE));var rgb=new org.joml.Vector3f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f);thaumcraft.api.fx.LegacyEffects.emit(world,state.getValue(SHADE)==0?"dark_glow":"glow",pos.getCenter(),net.minecraft.world.phys.Vec3.ZERO,color,.5f,40,1,0,0);}
}
