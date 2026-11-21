package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;

/** Two successive crowns with separate branch bases and four-column trunks. */
public  class GreatwoodGrowth {
    private final WorldGenLevel level;
    private final RandomSource random;
    private int limit,height;
    private BlockPos base;
    private record Branch(BlockPos leaves,int joinY) {}
    private GreatwoodGrowth(WorldGenLevel level,RandomSource source,BlockPos base) {
        this.level=level; this.random=new net.minecraft.world.level.levelgen.LegacyRandomSource(source.nextLong()); this.base=base; limit=11+random.nextInt(11);
    }
    public static boolean grow(WorldGenLevel level,BlockPos origin,RandomSource random) { return grow(level,origin,random,false); }
    public static boolean grow(WorldGenLevel level,BlockPos origin,RandomSource random,boolean spiders) { return new GreatwoodGrowth(level,random,origin).grow(origin,random,spiders); }
    public static boolean natural(WorldGenLevel level,BlockPos origin,RandomSource random) { return grow(level,origin,random,random.nextInt(8)==0); }
    /** Addons can choose a taller crown while retaining the original greatwood shape. */
    public static boolean natural(WorldGenLevel level,BlockPos origin,RandomSource random,int crownLimit) {
        var tree=new GreatwoodGrowth(level,random,origin);tree.limit=Math.max(6,crownLimit);
        return tree.grow(origin,random,random.nextInt(8)==0);
    }
    private boolean grow(BlockPos origin,RandomSource decorationRandom,boolean spiders) {
        // Generation never pulls ungenerated chunks into a server tick.
        if (origin.getY()<=level.getMinBuildHeight() || spiders && origin.getY()-2<level.getMinBuildHeight() || origin.getY()+limit*2+5>=level.getMaxBuildHeight()
                || !level.hasChunksAt(origin.offset(-16,spiders ? -2 : -1,-16),origin.offset(16,limit*2+5,16))) return false;
        if(!DecorationWrites.allowed(level,origin.offset(-16,0,-16))||!DecorationWrites.allowed(level,origin.offset(16,0,16)))return false;
        boolean found=false;
        search: for (int dx=-1;dx<=1;dx++) for (int dz=-1;dz<=1;dz++) {
            boolean valid=true;
            for (int x=0;x<2 && valid;x++) for (int z=0;z<2;z++) if (!validColumn(origin.offset(dx+x,0,dz+z))) { valid=false; break; }
            if (valid) { base=origin.offset(dx,0,dz); found=true; break search; }
        }
        if (!found) return false;
        crown(1.2);
        base=origin.above(height);
        crown(1.66);
        if (spiders) spiderNest(origin,decorationRandom);
        return true;
    }
    private void spiderNest(BlockPos origin,RandomSource decorationRandom) {
        var spawnerPos=origin.below(); DecorationWrites.set(level,spawnerPos,Blocks.SPAWNER.defaultBlockState(),3);
        if (!(level.getBlockEntity(spawnerPos) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner)) return;
        spawner.setEntityId(net.minecraft.world.entity.EntityType.CAVE_SPIDER,random); spawner.setChanged();
        for (int attempt=0;attempt<50;attempt++) {
            var target=origin.offset(-7+decorationRandom.nextInt(14),decorationRandom.nextInt(10),-7+decorationRandom.nextInt(14));
            if (!level.isEmptyBlock(target)) continue;
            boolean touching=false;
            for (var direction:Direction.values()) {
                var block=level.getBlockState(target.relative(direction)).getBlock();
                if (block instanceof MagicalLeavesBlock || block instanceof MagicalLogBlock) { touching=true; break; }
            }
            if (touching) DecorationWrites.set(level,target,Blocks.COBWEB.defaultBlockState(),3);
        }
        var chestPos=origin.below(2); DecorationWrites.set(level,chestPos,Blocks.CHEST.defaultBlockState(),3);
        if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)
            chest.setLootTable(net.minecraft.resources.ResourceLocation.parse("thaumcraft:chests/dungeon"),random.nextLong());
    }
    private boolean validColumn(BlockPos bottom) {
        var soil=level.getBlockState(bottom.below());
        if (!soil.canSustainPlant(level,bottom.below(),Direction.UP,(net.minecraftforge.common.IPlantable)Blocks.OAK_SAPLING)) return false;
        int obstruction=obstruction(bottom,bottom.above(limit-1));
        if (obstruction<0) return true;
        if (obstruction<6) return false;
        limit=obstruction; return true;
    }
    private void crown(double width) {
        height=Math.min(limit-1,(int)(limit*.618));
        int attempts=Math.max(1,(int)(1.382+Math.pow(.9*limit/13,2)));
        int relative=limit-4, y=base.getY()+relative, trunkTop=base.getY()+height;
        var branches=new ArrayList<Branch>(); branches.add(new Branch(new BlockPos(base.getX(),y--,base.getZ()),trunkTop));
        for (;relative>=0;relative--,y--) {
            float radius=layerRadius(relative); if (radius<0) continue;
            for (int attempt=0;attempt<attempts;attempt++) {
                double distance=width*radius*(random.nextFloat()+.328),angle=random.nextFloat()*2*Math.PI;
                var leaves=new BlockPos(Mth.floor(base.getX()+.5+distance*Math.sin(angle)),y,Mth.floor(base.getZ()+.5+distance*Math.cos(angle)));
                if (obstruction(leaves,leaves.above(4))>=0) continue;
                double reach=Math.sqrt(Math.pow(base.getX()-leaves.getX(),2)+Math.pow(base.getZ()-leaves.getZ(),2))*.38;
                int join=Math.min(trunkTop,(int)(y-reach));
                if (obstruction(new BlockPos(base.getX(),join,base.getZ()),leaves)<0) branches.add(new Branch(leaves,join));
            }
        }
        for (var branch:branches) for (int layer=0;layer<4;layer++) leafDisk(branch.leaves().above(layer),layer==0 || layer==3 ? 2 : 3);
        for (var branch:branches) if (branch.joinY()-base.getY()>=limit*.2) logs(new BlockPos(base.getX(),branch.joinY(),base.getZ()),branch.leaves());
        for (var offset:List.of(new BlockPos(0,0,0),new BlockPos(1,0,0),new BlockPos(1,0,1),new BlockPos(0,0,1))) logs(base.offset(offset),base.offset(offset).above(height));
    }
    private float layerRadius(int relative) {
        if (relative<limit*.3) return -1;
        float half=limit/2f, delta=half-relative;
        return (Math.abs(delta)>=half ? 0 : (float)Math.sqrt(half*half-delta*delta))*.5f;
    }
    private void leafDisk(BlockPos center,int radius) {
        int extent=(int)(radius+.618);
        for (int x=-extent;x<=extent;x++) for (int z=-extent;z<=extent;z++) {
            if (Math.pow(Math.abs(x)+.5,2)+Math.pow(Math.abs(z)+.5,2)>radius*radius) continue;
            var pos=center.offset(x,0,z); var state=level.getBlockState(pos);
            if (state.isAir() || state.getBlock() instanceof MagicalLeavesBlock) DecorationWrites.set(level,pos,TCArcane.GREATWOOD_LEAVES.get().defaultBlockState(),3);
        }
    }
    private static int[] coordinates(BlockPos pos) { return new int[]{pos.getX(),pos.getY(),pos.getZ()}; }
    static List<BlockPos> line(BlockPos start,BlockPos end,boolean round) {
        int[] from=coordinates(start),to=coordinates(end),delta=new int[3]; int axis=0;
        for (int i=0;i<3;i++) { delta[i]=to[i]-from[i]; if (Math.abs(delta[i])>Math.abs(delta[axis])) axis=i; }
        var result=new ArrayList<BlockPos>(); int length=Math.abs(delta[axis]); if (length==0) return result;
        int sign=Integer.signum(delta[axis]);
        for (int step=0;step<=length;step++) {
            int[] point=new int[3];
            for (int i=0;i<3;i++) point[i]=i==axis ? from[i]+step*sign : Mth.floor(from[i]+(double)(step*sign)*delta[i]/delta[axis]+(round ? .5 : 0));
            result.add(new BlockPos(point[0],point[1],point[2]));
        }
        return result;
    }
    private int obstruction(BlockPos start,BlockPos end) {
        var points=line(start,end,false);
        for (int index=0;index<points.size();index++) {
            var state=level.getBlockState(points.get(index));
            if (!state.isAir() && !(state.getBlock() instanceof MagicalLeavesBlock)) return index;
        }
        return -1;
    }
    private void logs(BlockPos start,BlockPos end) {
        for (var pos:line(start,end,true)) {
            int x=Math.abs(pos.getX()-start.getX()),z=Math.abs(pos.getZ()-start.getZ());
            var axis=x==0 && z==0 ? Direction.Axis.Y : x>=z ? Direction.Axis.X : Direction.Axis.Z;
            DecorationWrites.set(level,pos,TCArcane.GREATWOOD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis),3);
        }
    }
}
