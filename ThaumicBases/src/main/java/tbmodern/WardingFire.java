package tbmodern;

import java.util.EnumSet;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.api.aspects.*;

public final class WardingFire extends BlockEntity {
    private int burn,logs;
    public WardingFire(BlockPos pos,BlockState state) { super(Content.WARDING_FIRE.get(),pos,state); }
    private boolean brazier() { return ((Device)getBlockState().getBlock()).brazier; }
    public boolean addFuel(ItemStack stack) {
        if(stack.isEmpty() || brazier())return false;
        if(logs<=0 && stack.is(ItemTags.LOGS)) {
            logs=stack.is(Content.item("thaumcraft:greatwood_log"))?6400:1600;changed();return true;
        }
        if(logs>0 && burn<=0) {
            int duration=net.minecraftforge.common.ForgeHooks.getBurnTime(stack,null);
            if(duration>0) { burn=duration;changed();return true; }
        }
        return false;
    }
    public static void tick(Level level,BlockPos pos,BlockState state,WardingFire fire) {
        if(fire.brazier()) {
            if(fire.burn>0)fire.burn--;
            else if(AspectSourceHelper.drainEssentia(fire,Aspect.FIRE,null,6))fire.burn=1600;
        } else if(fire.burn>0) { fire.burn--;if(fire.logs>0)fire.logs--; }
        int expected=fire.burn>0 && (fire.brazier() || fire.logs>0)?2:fire.logs>0?1:0;
        if(state.getValue(Device.STATE)!=expected)level.setBlock(pos,state.setValue(Device.STATE,expected),3);
        fire.setChanged();
        if(expected!=2 || level.getGameTime()%10!=0)return;
        for(var mob:level.getEntitiesOfClass(PathfinderMob.class,new AABB(pos).inflate(12,6,12),entity->entity instanceof Enemy)) {
            Avoid goal=null;
            for(var existing:mob.goalSelector.getAvailableGoals())if(existing.getGoal() instanceof Avoid candidate) { goal=candidate;break; }
            if(goal==null) { goal=new Avoid(mob);mob.goalSelector.addGoal(1,goal); }
            goal.scare(pos,level.getGameTime()+100);
        }
    }
    private void changed() { setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag);tag.putInt("Burn",burn);tag.putInt("Logs",logs); }
    @Override public void load(CompoundTag tag) { super.load(tag);burn=Math.max(0,tag.getInt("Burn"));logs=Math.max(0,tag.getInt("Logs")); }
    private static final class Avoid extends Goal {
        private final PathfinderMob mob;
        private BlockPos origin=BlockPos.ZERO;
        private long expiry;
        private Path path;
        Avoid(PathfinderMob mob) { this.mob=mob;setFlags(EnumSet.of(Flag.MOVE)); }
        void scare(BlockPos pos,long expiry) { this.origin=pos.immutable();this.expiry=expiry; }
        @Override public boolean canUse() {
            if(mob.level().getGameTime()>=expiry)return false;
            Vec3 target=DefaultRandomPos.getPosAway(mob,16,10,Vec3.atCenterOf(origin));
            if(target==null)return false;
            path=mob.getNavigation().createPath(target.x,target.y,target.z,0);return path!=null && path.canReach();
        }
        @Override public boolean canContinueToUse() { return mob.level().getGameTime()<expiry && !mob.getNavigation().isDone(); }
        @Override public void start() { mob.getNavigation().moveTo(path,2); }
        @Override public void tick() { mob.getNavigation().setSpeedModifier(mob.distanceToSqr(Vec3.atCenterOf(origin))<49?2:4); }
        @Override public void stop() { path=null; }
    }
    public static final class Device extends BaseEntityBlock {
        public static final IntegerProperty STATE=IntegerProperty.create("state",0,2);
        private final boolean brazier;
        public Device(boolean brazier) { super(Properties.of().strength(1).noOcclusion().lightLevel(s->s.getValue(STATE)==2?15:0));this.brazier=brazier;registerDefaultState(stateDefinition.any().setValue(STATE,0)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(STATE); }
        @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Block.box(1,0,1,15,brazier?16:8,15); }
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new WardingFire(pos,state); }
        @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
            return level.isClientSide?null:createTickerHelper(type,Content.WARDING_FIRE.get(),WardingFire::tick);
        }
        @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
            if(brazier)return InteractionResult.PASS;
            if(level.isClientSide)return InteractionResult.SUCCESS;
            if(level.getBlockEntity(pos) instanceof WardingFire fire && fire.addFuel(player.getItemInHand(hand))) {
                var stack=player.getItemInHand(hand);ItemStack remainder=stack.hasCraftingRemainingItem()?stack.getCraftingRemainingItem():ItemStack.EMPTY;
                stack.shrink(1);
                if(!remainder.isEmpty()) { if(stack.isEmpty())player.setItemInHand(hand,remainder);else if(!player.addItem(remainder))player.drop(remainder,false); }
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
        @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
            if(state.getValue(STATE)!=2)return;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,pos.getX()+.5,pos.getY()+(brazier?1:.3),pos.getZ()+.5,0,.04,0);
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+(brazier?1.1:.4),pos.getZ()+.5,0,.03,0);
            if(random.nextInt(20)==0)level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),net.minecraft.sounds.SoundEvents.CAMPFIRE_CRACKLE,net.minecraft.sounds.SoundSource.BLOCKS,.1F,.8F,false);
        }
    }
}
