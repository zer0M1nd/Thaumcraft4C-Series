package ttmodern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;

public final class TravelBlocks {
    private TravelBlocks() {}
    static void boost(Level world,Entity entity) {
        if(!world.isClientSide&&entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,40,1));
            living.addEffect(new MobEffectInstance(MobEffects.JUMP,40,0));
        }
    }
    public static final class TravelSlab extends SlabBlock {
        public TravelSlab() { super(Properties.copy(Blocks.STONE_SLAB).strength(.8f,10)); }
        @Override public void stepOn(Level world,BlockPos pos,BlockState state,Entity entity) { boost(world,entity);super.stepOn(world,pos,state,entity); }
    }
    public static final class TravelStairs extends StairBlock {
        public TravelStairs() { super(()->tc4revival.content.TCArcane.TRAVEL_PAVING.get().defaultBlockState(),Properties.copy(Blocks.STONE_STAIRS).strength(2,10)); }
        @Override public void stepOn(Level world,BlockPos pos,BlockState state,Entity entity) { boost(world,entity);super.stepOn(world,pos,state,entity); }
    }
    public static final class WardSlab extends SlabBlock {
        public WardSlab() { super(Properties.copy(Blocks.STONE_SLAB).strength(.8f,10).isValidSpawn((s,w,p,e)->false)); }
        @Override public void stepOn(Level world,BlockPos pos,BlockState state,Entity entity) { boost(world,entity);super.stepOn(world,pos,state,entity); }
        @Override public void onPlace(BlockState state,Level world,BlockPos pos,BlockState previous,boolean moving) { super.onPlace(state,world,pos,previous,moving);world.scheduleTick(pos,this,5); }
        @Override public void tick(BlockState state,ServerLevel world,BlockPos pos,RandomSource random) {
            if(!world.hasNeighborSignal(pos)) {
                for(var living:world.getEntitiesOfClass(LivingEntity.class,new AABB(pos.getX(),pos.getY()-1,pos.getZ(),pos.getX()+1,pos.getY()+3,pos.getZ()+1).inflate(.1))) {
                    if(living instanceof Player||living.onGround())continue;
                    double yaw=Math.toRadians(living.getYRot());living.push(Math.sin(yaw)*.2,-.1,-Math.cos(yaw)*.2);
                }
            }
            for(int height=1;height<=2;height++) {
                var target=pos.above(height);var old=world.getBlockState(target);
                if(world.hasChunkAt(target)&&old.isAir()&&!old.is(tc4revival.content.TCArcane.LAMP_AIR.get()))world.setBlock(target,Tinkerer.WARD_BARRIER.get().defaultBlockState(),3);
            }
            world.scheduleTick(pos,this,5);
        }
    }
    /** Addon-owned barrier so base paving code never needs to recognize addon concrete block classes. */
    public static final class WardBarrier extends Block {
        public WardBarrier() { super(Properties.of().replaceable().noOcclusion().noLootTable().dynamicShape().isSuffocating((s,w,p)->false).isViewBlocking((s,w,p)->false).isRedstoneConductor((s,w,p)->false)); }
        private BlockPos source(BlockGetter world,BlockPos pos) {
            for(int i=1;i<=2;i++){var below=pos.below(i);if(world.getBlockState(below).is(Tinkerer.WARD_SLAB.get()))return below;}return null;
        }
        private boolean active(BlockGetter world,BlockPos pos) { var source=source(world,pos);return source!=null&&(!(world instanceof Level level)||!level.hasNeighborSignal(source)); }
        @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
        @Override public VoxelShape getShape(BlockState s,BlockGetter w,BlockPos p,CollisionContext c) { return Shapes.empty(); }
        @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter w,BlockPos p,CollisionContext c) {
            return c instanceof EntityCollisionContext e&&e.getEntity() instanceof LivingEntity&&!(e.getEntity() instanceof Player)&&active(w,p)?Shapes.block():Shapes.empty();
        }
        @Override public boolean isPathfindable(BlockState s,BlockGetter w,BlockPos p,PathComputationType type) { return !active(w,p); }
        @Override public void onPlace(BlockState s,Level w,BlockPos p,BlockState old,boolean moving) { w.scheduleTick(p,this,20); }
        @Override public void tick(BlockState s,ServerLevel w,BlockPos p,RandomSource random) { if(source(w,p)==null)w.removeBlock(p,false);else w.scheduleTick(p,this,20); }
    }
}
