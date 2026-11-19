package tc4revival.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public  class FluxGooBlock extends FiniteFluidBlock {
    public FluxGooBlock() { super(Properties.of().noCollission().noOcclusion().noLootTable().randomTicks()
            .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY).lightLevel(state -> (state.getValue(QUANTA)-1)*7/8),8,6000); }
    @Override public FluidState getFluidState(BlockState state) { return TCFluids.GOO.get().defaultFluidState().setValue(QUANTA,state.getValue(QUANTA)); }
    @Override public boolean canBeReplaced(BlockState state,BlockPlaceContext context) { return state.getValue(QUANTA)<=2; }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Shapes.empty(); }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        int metadata=state.getValue(QUANTA)-1;
        if (entity instanceof ThaumicSlimeEntity slime) {
            if (!level.isClientSide && slime.slimeSize()<metadata && level.random.nextBoolean()) {
                slime.setSlimeSize(slime.slimeSize()+1);
                if (metadata>1) level.setBlock(pos,state.setValue(QUANTA,metadata),3); else level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            }
        } else {
            double friction=1-state.getValue(QUANTA)/8.0; entity.setDeltaMovement(entity.getDeltaMovement().multiply(friction,1,friction));
            if (!level.isClientSide && entity instanceof LivingEntity living) {
                var effect=new MobEffectInstance(TCEffects.VIS_EXHAUST.get(),600,metadata/3,true,true); effect.setCurativeItems(List.of()); living.addEffect(effect);
            }
        }
    }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        flow(level,pos,random);
        var remaining=level.getBlockState(pos);
        int metadata=remaining.is(this) ? remaining.getValue(QUANTA)-1 : 0;
        boolean open=level.isEmptyBlock(pos.above());
        if (metadata>=2 && metadata<6 && open && random.nextInt(25)==0) spawnSlime(level,pos,1);
        else if (metadata>=6 && open) {
            if (random.nextInt(25)==0) spawnSlime(level,pos,2);
            else if (tc4revival.config.TCConfig.SERVER.taintFromFlux.get() && random.nextInt(50)==0) {
                WorldBiomeChanges.changeColumn(level,pos,TCBiomes.TAINTED_LAND);
                var fibre=TCArcane.TAINT_FIBRES.get();
                level.setBlock(pos,TaintFibreBlock.faces(fibre.defaultBlockState(),level,pos),3); level.blockEvent(pos,fibre,1,0);
            }
        } else if (random.nextInt(30)==0) {
            // A port must not delete an unrelated block displaced into this cell by finite-fluid exchange.
            if (!remaining.is(this)) return;
            if (metadata==0) level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            else {
                level.setBlock(pos,remaining.setValue(QUANTA,metadata),3);
                if (random.nextBoolean() && open) level.setBlock(pos.above(),TCArcane.FLUX_GAS.get().defaultBlockState(),3);
            }
        }
    }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) { tick(state,level,pos,random); }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) { tc4revival.client.CrucibleEffects.goo(level,pos,state.getValue(QUANTA)-1); }
    private void spawnSlime(ServerLevel level,BlockPos pos,int size) {
        var slime=TCEntities.THAUMIC_SLIME.get().create(level); if (slime==null) return;
        slime.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0); slime.setSlimeSize(size);
        if (level.addFreshEntity(slime)) { level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3); slime.playSound(TCSounds.GORE.get(),1,1); }
    }
}
