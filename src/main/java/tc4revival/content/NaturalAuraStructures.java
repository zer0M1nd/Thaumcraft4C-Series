package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import tc4revival.config.TCConfig;
/** Shared legacy order: wild aura, mutually exclusive surface ruins, then optional totem. */
public  class NaturalAuraStructures {
    private NaturalAuraStructures() {}
    static boolean allowsWildNodes(net.minecraft.resources.ResourceKey<Level> dimension,ChunkPos chunk) {
        return (TCConfig.SERVER.revival.dimensionNodes.get() || dimension.equals(Level.OVERWORLD) || dimension.equals(Level.NETHER))
                && TCConfig.SERVER.nodeMultiplier(dimension.location())>0;
    }
    public static boolean wildNode(ServerLevel level,ChunkPos chunk,RandomSource random,boolean auraGenerated) {
        if(!allowsWildNodes(level.dimension(),chunk)||!TCConfig.SERVER.generates(TCConfig.WorldFeature.AURA_NODES)||auraGenerated)return false;
        int rarity=TCConfig.SERVER.nodeRarity.get();
        double multiplier=thaumcraft.api.worldgen.NaturalGenerationRules.nodeChance(new thaumcraft.api.worldgen.NaturalGenerationRules.ChanceContext(level,chunk,auraGenerated),TCConfig.SERVER.nodeMultiplier(level.dimension().location()));
        boolean surfaceRoll=tc4revival.config.NodeDimensionRules.roll(random,rarity,multiplier);
        int z=chunk.getMinBlockZ()+random.nextInt(16),x=chunk.getMinBlockX()+random.nextInt(16),y=5;
        boolean ocean=level.dimension().equals(Level.OVERWORLD)&&level.getBiome(new BlockPos(x,level.getSeaLevel(),z)).is(net.minecraft.tags.BiomeTags.IS_OCEAN);
        boolean modernSurface=!level.dimension().equals(Level.OVERWORLD)&&!level.dimension().equals(Level.NETHER);
        // Water is occupied in the old getFirstUncoveredY scan. Keep that surface
        // attempt; underwater nodes are a separate Modern Port addition below.
        if(surfaceRoll) {
            // Floating islands/custom vertical ranges must place above actual terrain, not at Y=6 in the void.
            if(modernSurface) {
                int surface=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
                if(surface<=level.getMinBuildHeight()||surface>=level.getMaxBuildHeight())return false;
                y=surface-1;
            }
            if(ocean)y=Math.max(y,level.getSeaLevel()-1);
            while(y+1<level.getMaxBuildHeight()&&!level.isEmptyBlock(new BlockPos(x,y+1,z)))y++;
            if(y+1<level.getMaxBuildHeight()) {
                if(level.isEmptyBlock(new BlockPos(x,y+1,z)))y++;
                int offset=random.nextInt(4);var target=new BlockPos(x,y+offset,z);var state=level.getBlockState(target);
                if(state.isAir()||state.canBeReplaced())y+=offset;
                if(!level.isOutsideBuildHeight(new BlockPos(x,y,z)) && AuraNodeGeneration.createRandom(level,new BlockPos(x,y,z),random,false,false,false))return true;
            }
        }
        // Same configured 1/rarity chance, only after no surface node was placed.
        // Thus ocean probability is p + (1-p)*p, and never two nodes per chunk.
        if(ocean&&TCConfig.SERVER.revival.underwaterNodes.get()&&tc4revival.config.NodeDimensionRules.roll(random,rarity,multiplier)) {
            int floor=level.getHeight(Heightmap.Types.OCEAN_FLOOR,x,z),surface=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
            if(surface-floor>=3) {
                var submerged=new BlockPos(x,floor+random.nextInt(surface-floor),z);
                if(level.getBlockState(submerged).is(net.minecraft.world.level.block.Blocks.WATER))
                    return AuraNodeGeneration.createRandom(level,submerged,random,false,false,false);
            }
        }
        return false;
    }
    private static boolean totemRoll(RandomSource random,int rarity) { long bound=(long)rarity*10;return bound<=Integer.MAX_VALUE?random.nextInt((int)bound)==0:random.nextDouble()*bound<1; }
    public static void generate(ServerLevel level,ChunkPos chunk,RandomSource random,boolean fresh) {
        // Additional dimensions receive only the ordinary per-chunk wild-node roll.
        // Do not introduce temple nodes, ore retrogen or the modern ocean bonus there.
        if(!level.dimension().equals(Level.OVERWORLD)&&!level.dimension().equals(Level.NETHER)) {
            if(fresh||TCConfig.SERVER.regenerates(TCConfig.WorldFeature.AURA_NODES))wildNode(level,chunk,random,false);
            return;
        }
        if(!fresh&&!level.dimension().equals(Level.NETHER))NaturalOreGeneration.generate(level,chunk,random,false);
        boolean aura=false;
        if(allowsWildNodes(level.dimension(),chunk)&&TCConfig.SERVER.generates(TCConfig.WorldFeature.AURA_NODES)&&(fresh||TCConfig.SERVER.regenerates(TCConfig.WorldFeature.AURA_NODES))){
            if(!level.dimension().equals(Level.NETHER))aura=TempleAura.generate(level,chunk,random);
            aura=wildNode(level,chunk,random,aura);
        }
        // Surface ruins and totems now own registered structure starts and pieces.
        // Retrogen is intentionally not used to insert unlocatable copies into saved chunks.
    }
}
