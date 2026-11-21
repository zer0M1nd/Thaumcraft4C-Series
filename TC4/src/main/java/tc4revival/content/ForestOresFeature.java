package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Forest decorator deposits, retaining the original heights and stone-only substrate. */
public  class ForestOresFeature extends Feature<NoneFeatureConfiguration> {
    private record Deposit(Block block,int count,int size,int height,boolean triangular) {}
    private static final Deposit[] DEPOSITS={
        new Deposit(Blocks.DIRT,20,32,256,false),new Deposit(Blocks.GRAVEL,10,32,256,false),
        new Deposit(Blocks.COAL_ORE,20,16,128,false),new Deposit(Blocks.IRON_ORE,20,8,64,false),
        new Deposit(Blocks.GOLD_ORE,2,8,32,false),new Deposit(Blocks.REDSTONE_ORE,8,7,16,false),
        new Deposit(Blocks.DIAMOND_ORE,1,7,16,false),new Deposit(Blocks.LAPIS_ORE,1,6,16,true)
    };
    public ForestOresFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var random=context.random(); var chunk=new ChunkPos(context.origin()); boolean placed=false;
        for (var deposit:DEPOSITS) for (int attempt=0;attempt<deposit.count();attempt++) {
            int x=chunk.getMinBlockX()+random.nextInt(16);
            int y=random.nextInt(deposit.height())+(deposit.triangular() ? random.nextInt(deposit.height()) : 0);
            int z=chunk.getMinBlockZ()+random.nextInt(16);
            placed|=vein(context.level(),random,new BlockPos(x,y,z),deposit.block(),deposit.size());
        }
        return placed;
    }

    static boolean vein(WorldGenLevel world,RandomSource random,BlockPos origin,Block output,int size) {
        return vein(world,random,origin,output,null,size);
    }
    /** TC ores opt in to deepslate independently of the legacy forest decorator. */
    static boolean vein(WorldGenLevel world,RandomSource random,BlockPos origin,Block output,Block deepslateOutput,int size) {
        float angle=random.nextFloat()*(float)Math.PI;
        double x0=(float)(origin.getX()+8)+Mth.sin(angle)*size/8.0F;
        double x1=(float)(origin.getX()+8)-Mth.sin(angle)*size/8.0F;
        double z0=(float)(origin.getZ()+8)+Mth.cos(angle)*size/8.0F;
        double z1=(float)(origin.getZ()+8)-Mth.cos(angle)*size/8.0F;
        double y0=origin.getY()+random.nextInt(3)-2,y1=origin.getY()+random.nextInt(3)-2;
        boolean placed=false;
        for (int step=0;step<=size;step++) {
            double cx=x0+(x1-x0)*step/size,cy=y0+(y1-y0)*step/size,cz=z0+(z1-z0)*step/size;
            double scale=random.nextDouble()*size/16.0D;
            double radius=((Mth.sin(step*(float)Math.PI/size)+1.0F)*scale+1.0D)/2.0D;
            for (int x=Mth.floor(cx-radius);x<=Mth.floor(cx+radius);x++) {
                double nx=(x+.5D-cx)/radius; if (nx*nx>=1) continue;
                for (int y=Mth.floor(cy-radius);y<=Mth.floor(cy+radius);y++) {
                    double ny=(y+.5D-cy)/radius; if (nx*nx+ny*ny>=1) continue;
                    for (int z=Mth.floor(cz-radius);z<=Mth.floor(cz+radius);z++) {
                        double nz=(z+.5D-cz)/radius; if (nx*nx+ny*ny+nz*nz>=1) continue;
                        var pos=new BlockPos(x,y,z);
                        if (!world.isOutsideBuildHeight(pos) && world.hasChunkAt(pos)) {
                            var substrate=world.getBlockState(pos);
                            Block replacement=substrate.is(Blocks.STONE)?output:deepslateOutput!=null&&substrate.is(Blocks.DEEPSLATE)?deepslateOutput:null;
                            if(replacement!=null)placed|=DecorationWrites.set(world,pos,replacement.defaultBlockState(),2);
                        }
                    }
                }
            }
        }
        return placed;
    }
}
