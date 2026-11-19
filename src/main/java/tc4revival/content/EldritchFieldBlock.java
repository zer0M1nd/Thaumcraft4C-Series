package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** The warden's replaceable, non-colliding field; walking trails also expire through legacy random ticks. */
public  class EldritchFieldBlock extends Block {
    public EldritchFieldBlock() {
        super(Properties.of().strength(100,50).noCollission().noOcclusion().replaceable().randomTicks().lightLevel(state->8).noLootTable());
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        float height=random.nextFloat()*.33f;
        tc4revival.client.EldritchSparkParticles.emit(level,pos.getX()+random.nextFloat(),pos.getY()+.1515f+height/2,pos.getZ()+random.nextFloat(),
                .33f+height,.3f-random.nextFloat()*.1f,0,.5f+random.nextFloat()*.2f,1);
        if (random.nextInt(50)==0) level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),TCSounds.JACOBS.get(),net.minecraft.sounds.SoundSource.BLOCKS,.5f,1+(random.nextFloat()-random.nextFloat())*.2f,false);
    }
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if (entity instanceof EldritchMob) return;
        var velocity=entity.getDeltaMovement(); entity.setDeltaMovement(velocity.multiply(.66,1,.66));
        if (!level.isClientSide) {
            if (level.random.nextInt(100)==0) entity.hurt(level.damageSources().wither(),1);
            if (entity instanceof Player player) player.causeFoodExhaustion(.05f);
            if (entity instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100,1,true,true));
        }
    }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) { level.removeBlock(pos,false); }
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) { level.removeBlock(pos,false); }
}
