package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public  class TaintSolidBlock extends Block {
    public enum Kind { CRUST,SOIL,FLESH }
    private final Kind kind;
    public TaintSolidBlock(Kind kind) {
        super(Properties.of().strength(kind==Kind.CRUST ? 1.75f : kind==Kind.SOIL ? 1.5f : .2f,10).randomTicks()
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                .sound(new net.minecraftforge.common.util.ForgeSoundType(.5f,.8f,TCSounds.GORE,TCSounds.GORE,TCSounds.GORE,TCSounds.GORE,TCSounds.GORE)));
        this.kind=kind;
    }
    public Kind kind() { return kind; }
    static void corrupt(ServerLevel level,BlockPos pos) {
        var old=level.getBlockState(pos); int adjacent=TaintFibreBlock.adjacentTaint(level,pos);
        if (level.getBlockEntity(pos)!=null) return;
        if (adjacent>=2 && (old.is(BlockTags.LOGS) || old.is(Blocks.PUMPKIN) || old.is(Blocks.CARVED_PUMPKIN)
                || old.is(Blocks.JACK_O_LANTERN) || old.is(Blocks.MELON) || old.is(Blocks.CACTUS))) {
            level.setBlock(pos,TCArcane.TAINT_CRUST.get().defaultBlockState(),3); level.blockEvent(pos,TCArcane.TAINT_CRUST.get(),1,0);
        }
        if (adjacent>=3 && (old.is(Blocks.SAND) || old.is(Blocks.RED_SAND) || old.is(Blocks.GRAVEL) || old.is(Blocks.DIRT)
                || old.is(Blocks.COARSE_DIRT) || old.is(Blocks.PODZOL) || old.is(Blocks.GRASS_BLOCK) || old.is(Blocks.MYCELIUM) || old.is(Blocks.CLAY))) {
            level.setBlock(pos,TCArcane.TAINT_SOIL.get().defaultBlockState(),3); level.blockEvent(pos,TCArcane.TAINT_SOIL.get(),1,0);
        }
    }
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity) {
        if (kind!=Kind.FLESH) TaintFibreBlock.poison(level,entity,100,20);
        super.stepOn(level,pos,state,entity);
    }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if (kind==Kind.FLESH) return;
        TaintFibreBlock.spreadBiome(level,pos,random,this);
        if (kind==Kind.CRUST) {
            if (fall(level,pos,pos)) return;
            if (level.isEmptyBlock(pos.above())) {
                Direction side=Direction.values()[2+random.nextInt(4)]; boolean collapse=true;
                for (int i=0;i<4;i++) if (!level.isEmptyBlock(pos.relative(side).below(i)) || !(level.getBlockState(pos.below(i)).getBlock() instanceof TaintSolidBlock)) { collapse=false; break; }
                if (collapse && fall(level,pos,pos.relative(side))) return;
            }
        }
        var target=pos.offset(random.nextInt(3)-1,random.nextInt(5)-3,random.nextInt(3)-1);
        if (!level.hasChunkAt(target) || level.isOutsideBuildHeight(target)) return;
        if (TCBiomes.isTainted(level,target)) {
            TaintFibreBlock.spread(level,target);
            if (kind==Kind.CRUST) {
                if(tc4revival.config.TCConfig.SERVER.spawns(tc4revival.config.TCConfig.SpawnKind.TAINT_SPORES)
                        && level.isEmptyBlock(pos.above()) && random.nextInt(200)==0) {
                    growHive(level,pos);return;
                }
                boolean buried=level.getBlockState(pos.above()).getBlock() instanceof TaintSolidBlock;
                for (Direction side:Direction.Plane.HORIZONTAL) buried&=level.getBlockState(pos.relative(side)).getBlock() instanceof TaintSolidBlock;
                if (buried) level.setBlock(pos,TCArcane.FLUX_GOO.get().defaultBlockState().setValue(FiniteFluidBlock.QUANTA,9),3);
            }
        } else if (kind==Kind.CRUST && random.nextInt(20)==0) level.setBlock(pos,TCArcane.FLUX_GOO.get().defaultBlockState().setValue(FiniteFluidBlock.QUANTA,9),3);
        else if (kind==Kind.SOIL && random.nextInt(10)==0) level.setBlock(pos,Blocks.DIRT.defaultBlockState(),3);
    }
    static boolean growHive(ServerLevel level,BlockPos pos) {
        if(!level.getEntitiesOfClass(TaintHiveEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(16)).isEmpty())return false;
        var hive=TCEntities.TAINT_HIVE.get().create(level);if(hive==null)return false;
        hive.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        var previous=level.getBlockState(pos);
        if(!level.removeBlock(pos,false))return false;
        if(!level.addFreshEntity(hive)) { level.setBlock(pos,previous,3);return false; }
        level.playSound(null,hive,TCSounds.ROOTS.get(),net.minecraft.sounds.SoundSource.HOSTILE,.1f,.9f+level.random.nextFloat()*.2f);
        return true;
    }
    static boolean canFallThrough(ServerLevel level,BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return true;
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) for (int y=-1;y<=1;y++) {
            var at=pos.offset(x,y,z); if (!level.hasChunkAt(at)) return false;
            if (level.getBlockState(at).is(BlockTags.LOGS)) return false;
        }
        var state=level.getBlockState(pos);
        if (state.getBlock() instanceof FluxGooBlock && state.getValue(FiniteFluidBlock.QUANTA)>=5) return false;
        return state.isAir() || state.is(BlockTags.FIRE) || state.getBlock() instanceof TaintFibreBlock || state.canBeReplaced()
                || state.is(Blocks.WATER) || state.is(Blocks.LAVA);
    }
    private boolean fall(ServerLevel level,BlockPos source,BlockPos target) {
        if (target.getY()<level.getMinBuildHeight() || !canFallThrough(level,target.below())) return false;
        if (level.hasChunksAt(target.offset(-32,-32,-32),target.offset(32,32,32))) {
            var falling=new FallingTaintEntity(TCEntities.FALLING_TAINT.get(),level);
            falling.begin(source,target,level.getBlockState(source)); return level.addFreshEntity(falling);
        }
        // The fallback lands in the loaded column without forcing neighboring chunks.
        var original=level.getBlockState(source); level.removeBlock(source,false); var landing=target;
        while (landing.getY()>level.getMinBuildHeight() && canFallThrough(level,landing.below())) landing=landing.below();
        if (landing.getY()>=level.getMinBuildHeight()) level.setBlock(landing,original,3);
        return true;
    }
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int parameter) {
        if (id!=1) return super.triggerEvent(state,level,pos,id,parameter);
        if (level.isClientSide) level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),TCSounds.ROOTS.get(),net.minecraft.sounds.SoundSource.BLOCKS,.1f,.9f+level.random.nextFloat()*.2f,false);
        return true;
    }
}
