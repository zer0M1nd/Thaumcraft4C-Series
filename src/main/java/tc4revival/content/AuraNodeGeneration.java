package tc4revival.content;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.AspectAmounts;
import tc4revival.aspect.AspectCatalog;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;

/** Shared node generation for natural nodes, silverwood, structures and primal-orb impacts. */
public  class AuraNodeGeneration {
    private AuraNodeGeneration() {}
    public record Environment(int water,int lava,int stone,int foliage) {}
    public record Result(AspectAmounts aspects,AuraNodeType type,AuraNodeModifier modifier) {
        public Result { aspects=aspects.copy(); }
        @Override public AspectAmounts aspects() { return aspects.copy(); }
    }
    /** Null biome tags are significant: MAGICAL can deliberately select the fallback pair. */
    public record BiomeProfile(int aura,List<String> aspectTags,boolean tainted) {
        public BiomeProfile {
            if (aura<4) throw new IllegalArgumentException("Biome aura must allow a positive small-node roll");
            aspectTags=java.util.Collections.unmodifiableList(new java.util.ArrayList<>(aspectTags));
        }
    }
    public static Result roll(AspectCatalog catalog,RandomSource random,BiomeProfile biome,Environment environment,
                              int rarity,boolean silverwood,boolean eerie,boolean small) {
        if (rarity<3) throw new IllegalArgumentException("Invalid special-node rarity");
        var primals=catalog.all().stream().filter(Aspect::isPrimal).toList();
        var compounds=catalog.all().stream().filter(aspect->!aspect.isPrimal()).toList();
        AuraNodeType type=AuraNodeType.NORMAL;
        if (silverwood) type=AuraNodeType.PURE;
        else if (eerie) type=AuraNodeType.DARK;
        else if (random.nextInt(rarity)==0) {
            int kind=random.nextInt(10);
            type=kind<3 ? AuraNodeType.DARK : kind<6 ? AuraNodeType.UNSTABLE : kind<9 ? AuraNodeType.PURE : AuraNodeType.HUNGRY;
        }
        AuraNodeModifier modifier=random.nextInt(rarity/2)==0 ? AuraNodeModifier.values()[random.nextInt(3)] : null;
        int aura=biome.aura();
        if (type!=AuraNodeType.PURE && biome.tainted()) {
            aura=(int)(aura*1.5f);
            if (random.nextBoolean()) { type=AuraNodeType.TAINTED; aura=(int)(aura*1.5f); }
        }
        if (silverwood || small) aura/=4;
        int half=Math.max(1,aura/2), value=random.nextInt(half)+half;
        String tag=biome.aspectTags().isEmpty() ? null : biome.aspectTags().get(random.nextInt(biome.aspectTags().size()));
        var weights=new AspectAmounts();
        if (tag!=null) weights.add(catalog.require(tag),2);
        else {
            weights.add(compounds.get(random.nextInt(compounds.size())),1);
            weights.add(primals.get(random.nextInt(primals.size())),1);
        }
        for (int i=0;i<3;i++) {
            if (!random.nextBoolean()) continue;
            var pool=random.nextInt(rarity)==0 ? compounds : primals;
            weights.merge(pool.get(random.nextInt(pool.size())),1);
        }
        switch (type) {
            case HUNGRY -> { weights.merge(catalog.require("fames"),2); if (random.nextBoolean()) weights.merge(catalog.require("lucrum"),1); }
            case PURE -> weights.merge(catalog.require(random.nextBoolean() ? "victus" : "ordo"),2);
            case DARK -> { for (String aspect : List.of("mortuus","exanimis","perditio","tenebrae")) if (random.nextBoolean()) weights.merge(catalog.require(aspect),1); }
            default -> {}
        }
        if (environment.water()>100) weights.merge(catalog.require("aqua"),1);
        if (environment.lava()>100) { weights.merge(catalog.require("ignis"),1); weights.merge(catalog.require("terra"),1); }
        if (environment.stone()>500) weights.merge(catalog.require("terra"),1);
        if (environment.foliage()>100) weights.merge(catalog.require("herba"),1);
        var sorted=weights.aspects().stream().sorted(Comparator.comparing(Aspect::tag)).toList();
        int[] shares=new int[sorted.size()]; float total=0;
        for (int i=0;i<shares.length;i++) {
            shares[i]=weights.amount(sorted.get(i))==2 ? 50+random.nextInt(25) : 25+random.nextInt(50); total+=shares[i];
        }
        for (int i=0;i<shares.length;i++) weights.merge(sorted.get(i),(int)(shares[i]/total*value));
        return thaumcraft.api.worldgen.NaturalGenerationRules.nodeResult(
            new thaumcraft.api.worldgen.NaturalGenerationRules.NodeContext(catalog,biome,environment,rarity,silverwood,eerie,small),new Result(weights,type,modifier));
    }
    public static Environment survey(WorldGenLevel level,BlockPos center) {
        int water=0,lava=0,stone=0,foliage=0;
        int minX=center.getX()-5,maxX=center.getX()+5,minZ=center.getZ()-5,maxZ=center.getZ()+5;
        int minY=Math.max(center.getY()-5,level.getMinBuildHeight()),maxY=Math.min(center.getY()+5,level.getMaxBuildHeight()-1);
        // An 11-cube touches at most four chunk columns. Resolve each once, without
        // requesting unavailable neighbours from WorldGenRegion / distant generation.
        for(int cx=minX>>4;cx<=maxX>>4;cx++)for(int cz=minZ>>4;cz<=maxZ>>4;cz++){
            if(!level.hasChunk(cx,cz))continue;
            var chunk=level.getChunk(cx,cz);
            int x0=Math.max(minX,cx<<4),x1=Math.min(maxX,(cx<<4)+15);
            int z0=Math.max(minZ,cz<<4),z1=Math.min(maxZ,(cz<<4)+15);
            for(int sy=minY>>4;sy<=maxY>>4;sy++){
                var section=chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
                if(section.hasOnlyAir())continue;
                int y0=Math.max(minY,sy<<4),y1=Math.min(maxY,(sy<<4)+15);
                for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++){
                    var state=section.getBlockState(x&15,y&15,z&15);
                    // Waterlogged blocks were not Material.water in 1.7.10.
                    if(state.is(Blocks.WATER))water++;
                    else if(state.is(Blocks.LAVA))lava++;
                    else if(state.is(Blocks.STONE))stone++;
                    if(state.is(BlockTags.LEAVES))foliage++;
                }
            }
        }
        return new Environment(water,lava,stone,foliage);
    }
    public static boolean createRandom(WorldGenLevel level,BlockPos pos,RandomSource random,
                                       boolean silverwood,boolean eerie,boolean small) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
        var biome=LegacyBiomeProfiles.get(level.getBiome(pos)).node();
        return place(level,pos,roll(TCAspects.catalog(),random,biome,survey(level,pos),
                TCConfig.SERVER.specialNodeRarity(),silverwood,eerie,small));
    }
    public static boolean place(WorldGenLevel level,BlockPos pos,Result result) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
        boolean water=TCConfig.SERVER.revival.underwaterNodes.get()&&level.getBlockState(pos).is(Blocks.WATER);
        if ((level.isEmptyBlock(pos)||water) && !level.setBlock(pos,TCArcane.AURA_NODE.get().defaultBlockState().setValue(AuraNodeBlock.WATERLOGGED,water),3)) return false;
        if (!(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node)) return false;
        node.configure(result.aspects(),result.type(),result.modifier(),"",false); return true;
    }
}
