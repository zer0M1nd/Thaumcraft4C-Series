package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import tc4revival.config.TCConfig;
public  class NaturalOreGeneration {
    private NaturalOreGeneration() {}
    private static boolean enabled(TCConfig.WorldFeature feature,boolean fresh){return TCConfig.SERVER.generates(feature)&&(fresh||TCConfig.SERVER.regenerates(feature));}
    private static void single(WorldGenLevel level,BlockPos pos,int metadata,int flags){if(!level.isOutsideBuildHeight(pos)&&level.getBlockState(pos).is(Blocks.STONE))DecorationWrites.set(level,pos,TCArcane.ore(metadata).defaultBlockState(),flags);}
    public static void generate(WorldGenLevel level,ChunkPos chunk,RandomSource random,boolean fresh){
        if(TCConfig.SERVER.revival.expandedOres.get()){expanded(level,chunk,random,fresh);return;}
        int bx=chunk.getMinBlockX(),bz=chunk.getMinBlockZ();
        if(enabled(TCConfig.WorldFeature.CINNABAR_ORE,fresh))for(int i=0;i<18;i++){int x=bx+random.nextInt(16),y=random.nextInt(51),z=bz+random.nextInt(16);single(level,new BlockPos(x,y,z),0,2);}
        if(enabled(TCConfig.WorldFeature.AMBER_ORE,fresh))for(int i=0;i<20;i++){int x=bx+random.nextInt(16),z=bz+random.nextInt(16),y=LegacySurfaceHeight.at(level,x,z)-random.nextInt(25);single(level,new BlockPos(x,y,z),7,2);}
        if(enabled(TCConfig.WorldFeature.INFUSED_STONE,fresh))for(int i=0,count=infusedAttempts(random);i<count;i++){
            int x=bx+random.nextInt(16),z=bz+random.nextInt(16),y=random.nextInt(Math.max(5,LegacySurfaceHeight.at(level,x,z)-5)),metadata=1+random.nextInt(6);
            if(random.nextInt(3)==0){var tags=LegacyBiomeProfiles.get(level.getBiome(new BlockPos(x,0,z))).node().aspectTags();String tag=tags.isEmpty()?null:tags.get(random.nextInt(tags.size()));if(tag==null)metadata=1+random.nextInt(6);else metadata=switch(tag){case "aer"->1;case "ignis"->2;case "aqua"->3;case "terra"->4;case "ordo"->5;case "perditio"->6;default->metadata;};}
            ForestOresFeature.vein(level,random,new BlockPos(x,y,z),TCArcane.ore(metadata),6);
        }
    }
    private static int infusedAttempts(RandomSource random){
        double expected=8*TCConfig.SERVER.infusedStoneDensityMultiplier.get();int count=(int)Math.floor(expected);
        // At the default multiplier retain the old RNG sequence as well as the count.
        return count+(expected>count&&random.nextDouble()<expected-count?1:0);
    }
    static net.minecraft.world.level.block.state.BlockState replacement(net.minecraft.world.level.block.state.BlockState substrate,int metadata,boolean expanded){
        if(substrate.is(Blocks.STONE))return TCArcane.ore(metadata).defaultBlockState();
        return expanded&&substrate.is(Blocks.DEEPSLATE)?TCArcane.deepslateOre(metadata).defaultBlockState():null;
    }
    private static void expanded(WorldGenLevel level,ChunkPos chunk,RandomSource random,boolean fresh){
        boolean cinnabar=enabled(TCConfig.WorldFeature.CINNABAR_ORE,fresh),amber=enabled(TCConfig.WorldFeature.AMBER_ORE,fresh),infused=enabled(TCConfig.WorldFeature.INFUSED_STONE,fresh)&&TCConfig.SERVER.infusedStoneDensityMultiplier.get()>0;
        if(!cinnabar&&!amber&&!infused)return;
        int bx=chunk.getMinBlockX(),bz=chunk.getMinBlockZ(),minimum=level.getMinBuildHeight(),maximum=level.getMaxBuildHeight();
        int[] surfaces=new int[256];
        // One height lookup per column, shared by amber and infused ore distributions.
        if(amber||infused)for(int column=0;column<256;column++)surfaces[column]=LegacySurfaceHeight.at(level,bx+(column&15),bz+(column>>4));
        if(cinnabar)expandedDeposits(level,chunk,random,minimum,maximum,surfaces,0,18);
        if(amber)expandedDeposits(level,chunk,random,minimum,maximum,surfaces,7,40);
        if(infused)expandedDeposits(level,chunk,random,minimum,maximum,surfaces,1,8);
    }
    private static void expandedDeposits(WorldGenLevel level,ChunkPos chunk,RandomSource random,int minimum,int maximum,int[] surfaces,int type,int attempts){
        var bands=new ExpandedOreDistribution.Band[256];
        for(int column=0;column<256;column++)bands[column]=type==0?ExpandedOreDistribution.cinnabar(minimum,maximum):type==7?ExpandedOreDistribution.amber(minimum,maximum,surfaces[column]):ExpandedOreDistribution.infused(minimum,maximum,surfaces[column]);
        var distribution=new ExpandedOreDistribution(bands,attempts,type==1?TCConfig.SERVER.infusedStoneDensityMultiplier.get():1);
        int count=distribution.attempts(random.nextDouble());
        for(int i=0;i<count;i++){
            int column=distribution.column(random.nextDouble());var band=distribution.band(column);
            int x=chunk.getMinBlockX()+(column&15),z=chunk.getMinBlockZ()+(column>>4),y=band.minimum()+random.nextInt(band.span());
            var pos=new BlockPos(x,y,z);
            if(type==0||type==7){var state=replacement(level.getBlockState(pos),type,true);if(state!=null)DecorationWrites.set(level,pos,state,2);continue;}
            int metadata=1+random.nextInt(6);
            if(random.nextInt(3)==0){var tags=LegacyBiomeProfiles.get(level.getBiome(new BlockPos(x,0,z))).node().aspectTags();String tag=tags.isEmpty()?null:tags.get(random.nextInt(tags.size()));if(tag==null)metadata=1+random.nextInt(6);else metadata=switch(tag){case "aer"->1;case "ignis"->2;case "aqua"->3;case "terra"->4;case "ordo"->5;case "perditio"->6;default->metadata;};}
            ForestOresFeature.vein(level,random,pos,TCArcane.ore(metadata),TCArcane.deepslateOre(metadata),6);
        }
    }
}
