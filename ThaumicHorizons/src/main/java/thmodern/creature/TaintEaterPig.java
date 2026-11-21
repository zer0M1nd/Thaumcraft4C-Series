package thmodern.creature;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import tc4revival.content.*;

public final class TaintEaterPig extends Pig {
    private int eatingCooldown;
    public TaintEaterPig(EntityType<? extends Pig> type,Level level){super(type,level);goalSelector.addGoal(5,new EatTaintGoal(this));}
    @Override public void aiStep(){
        if(!level().isClientSide){if(eatingCooldown>0)eatingCooldown--;removeEffect(TCEffects.FLUX_TAINT.get());removeEffect(TCEffects.VIS_EXHAUST.get());removeEffect(TCEffects.INFECTIOUS_VIS_EXHAUST.get());}
        super.aiStep();
    }
    public boolean edible(BlockPos pos){
        if(!level().hasChunkAt(pos)||!level().getWorldBorder().isWithinBounds(pos)||level().getBlockEntity(pos)!=null)return false;
        var state=level().getBlockState(pos);
        return state.is(BlockTags.create(ResourceLocation.parse("thaumichorizons:taint_edible")))
            ||state.is(Blocks.GRASS_BLOCK)&&level() instanceof ServerLevel server&&EtherealBloomBlockEntity.storedBiome(server,pos).is(TCBiomes.TAINTED_LAND);
    }
    public boolean eatTaint(BlockPos pos){
        if(!(level() instanceof ServerLevel server)||eatingCooldown>0||distanceToSqr(pos.getCenter())>4||!edible(pos)||!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(server,this))return false;
        var state=server.getBlockState(pos);boolean grass=state.is(Blocks.GRASS_BLOCK);
        if(!server.setBlock(pos,grass?Blocks.DIRT.defaultBlockState():Blocks.AIR.defaultBlockState(),3))return false;
        server.levelEvent(2001,pos,net.minecraft.world.level.block.Block.getId(state));
        WorldBiomeChanges.changeColumn(server,pos,Biomes.PLAINS);heal(1);eatingCooldown=grass?10:20;
        playSound(net.minecraft.sounds.SoundEvents.PLAYER_BURP,.2f,((random.nextFloat()-random.nextFloat())*.7f+1)*2);return true;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("EatingCooldown",eatingCooldown);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);eatingCooldown=Math.max(0,Math.min(20,tag.getInt("EatingCooldown")));}
    private static final class EatTaintGoal extends Goal {
        private final TaintEaterPig pig;private BlockPos target;private int remaining;
        EatTaintGoal(TaintEaterPig pig){this.pig=pig;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){
            if(pig.eatingCooldown>0)return false;target=null;var origin=pig.blockPosition();
            for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){var pos=origin.offset(x,y,z);if(pig.edible(pos)){target=pos;return true;}}
            for(int i=0;i<30;i++){var pos=origin.offset(pig.random.nextInt(17)-8,pig.random.nextInt(5)-2,pig.random.nextInt(17)-8);if(pig.edible(pos)&&pig.level().isEmptyBlock(pos.above())){target=pos;return true;}}
            return false;
        }
        @Override public void start(){remaining=500;move();}
        private void move(){pig.getNavigation().moveTo(target.getX()+.5,target.getY()+1,target.getZ()+.5,1);}
        @Override public boolean canContinueToUse(){return remaining>0&&pig.eatingCooldown==0&&target!=null&&pig.edible(target);}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){remaining--;pig.getLookControl().setLookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5,30,30);if(pig.distanceToSqr(target.getCenter())<=4)pig.eatTaint(target);else if(remaining%10==0)move();}
        @Override public void stop(){target=null;remaining=0;pig.getNavigation().stop();}
    }
}
