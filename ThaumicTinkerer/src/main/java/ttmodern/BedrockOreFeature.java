package ttmodern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.block.Blocks;

/** Data-defined ore-dictionary replacement; missing optional ore tags are ignored. */
public final class BedrockOreFeature extends Feature<BedrockOreFeature.Configuration> {
    static volatile java.util.Set<ResourceLocation> clientOres=java.util.Set.of();
    public record Ore(ResourceLocation tag,int weight){static final Codec<Ore> CODEC=RecordCodecBuilder.create(instance->instance.group(ResourceLocation.CODEC.fieldOf("tag").forGetter(Ore::tag),Codec.intRange(1,1000000).fieldOf("weight").forGetter(Ore::weight)).apply(instance,Ore::new));}
    public record Configuration(java.util.List<Ore> ores) implements FeatureConfiguration {static final Codec<Configuration> CODEC=RecordCodecBuilder.create(instance->instance.group(Ore.CODEC.listOf().fieldOf("ores").forGetter(Configuration::ores)).apply(instance,Configuration::new));}
    public BedrockOreFeature(){super(Configuration.CODEC);}
    static net.minecraft.world.level.block.Block oreBlock(net.minecraft.core.RegistryAccess access,Ore ore){
        if(TinkererConfig.BEDROCK_BLACKLIST.get().contains(ore.tag().toString()))return null;
        var registry=access.registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
        var tag=registry.getTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,ore.tag()));
        return tag.flatMap(values->values.stream().map(net.minecraft.core.Holder::value)
                .filter(block->block.asItem()!=net.minecraft.world.item.Items.AIR&&!block.defaultBlockState().hasBlockEntity())
                .min(java.util.Comparator.<net.minecraft.world.level.block.Block>comparingInt(block->registry.getKey(block).getPath().startsWith("deepslate_")?1:0)
                .thenComparingInt(block->registry.getKey(block).getNamespace().equals("minecraft")?0:1).thenComparing(block->registry.getKey(block).toString()))).orElse(null);
    }
    static boolean generatedOre(net.minecraft.world.level.Level level,net.minecraft.world.level.block.state.BlockState state){
        if(level.isClientSide)return clientOres.contains(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()));
        var configured=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
                .get(ResourceLocation.parse(Tinkerer.ID+":bedrock_ores"));
        if(configured==null||!(configured.config() instanceof Configuration config))return false;
        return config.ores().stream().anyMatch(ore->state.getBlock()==oreBlock(level.registryAccess(),ore));
    }
    static java.util.List<ResourceLocation> generatedOres(net.minecraft.core.RegistryAccess access){
        var configured=access.registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE).get(ResourceLocation.parse(Tinkerer.ID+":bedrock_ores"));
        if(configured==null||!(configured.config() instanceof Configuration config))return java.util.List.of();
        return config.ores().stream().map(ore->oreBlock(access,ore)).filter(java.util.Objects::nonNull).map(net.minecraftforge.registries.ForgeRegistries.BLOCKS::getKey).distinct().toList();
    }
    @Override public boolean place(FeaturePlaceContext<Configuration> context){
        if(!TinkererConfig.KAMI.get()||!TinkererConfig.BEDROCK_ENABLED.get())return false;
        record Candidate(net.minecraft.world.level.block.state.BlockState state,int weight){}var candidates=new java.util.ArrayList<Candidate>();long total=0;
        var registry=context.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK);
        for(var ore:context.config().ores()){
            var block=oreBlock(context.level().registryAccess(),ore);if(block==null)continue;
            candidates.add(new Candidate(block.defaultBlockState(),ore.weight()));total+=ore.weight();
        }if(candidates.isEmpty()||total>Integer.MAX_VALUE)return false;
        var random=context.random();var origin=context.origin();boolean placed=false;
        for(int column=0;column<TinkererConfig.BEDROCK_DENSITY.get();column++){
            int x=(origin.getX()&~15)+random.nextInt(16),z=(origin.getZ()&~15)+random.nextInt(16),choice=random.nextInt((int)total);var selected=candidates.get(0);for(var candidate:candidates){choice-=candidate.weight();if(choice<0){selected=candidate;break;}}
            for(int attempt=0;attempt<200;attempt++){int y=random.nextInt(245)+6,size=random.nextInt(20);if(size==0)continue;placed|=placeVein(context.level(),random,new net.minecraft.core.BlockPos(x,y,z),selected.state(),size);}
        }return placed;
    }

    // Legacy OreClusterGenerator uses WorldGenMinable with bedrock as its only replacement.
    // Keep this dimension's deposits independent of mods filtering vanilla OreConfiguration.
    static boolean placeVein(net.minecraft.world.level.WorldGenLevel level,net.minecraft.util.RandomSource random,
            net.minecraft.core.BlockPos origin,net.minecraft.world.level.block.state.BlockState ore,int size){
        if(size<=0)return false;
        double angle=random.nextFloat()*Math.PI;
        double x1=origin.getX()+8+Math.sin(angle)*size/8, x2=origin.getX()+8-Math.sin(angle)*size/8;
        double z1=origin.getZ()+8+Math.cos(angle)*size/8, z2=origin.getZ()+8-Math.cos(angle)*size/8;
        double y1=origin.getY()+random.nextInt(3)-2, y2=origin.getY()+random.nextInt(3)-2;
        boolean placed=false;
        var pos=new net.minecraft.core.BlockPos.MutableBlockPos();
        for(int step=0;step<=size;step++){
            double fraction=(double)step/size;
            double cx=x1+(x2-x1)*fraction,cy=y1+(y2-y1)*fraction,cz=z1+(z2-z1)*fraction;
            double radius=((Math.sin(Math.PI*fraction)+1)*random.nextDouble()*size/16+1)/2;
            for(int x=net.minecraft.util.Mth.floor(cx-radius);x<=net.minecraft.util.Mth.floor(cx+radius);x++)
                for(int y=net.minecraft.util.Mth.floor(cy-radius);y<=net.minecraft.util.Mth.floor(cy+radius);y++)
                    for(int z=net.minecraft.util.Mth.floor(cz-radius);z<=net.minecraft.util.Mth.floor(cz+radius);z++){
                        double dx=(x+.5-cx)/radius,dy=(y+.5-cy)/radius,dz=(z+.5-cz)/radius;
                        if(dx*dx+dy*dy+dz*dz>=1)continue;
                        pos.set(x,y,z);
                        if(!level.isOutsideBuildHeight(pos)&&level.ensureCanWrite(pos)&&level.getBlockState(pos).is(Blocks.BEDROCK))
                            placed|=level.setBlock(pos,ore,2);
                    }
        }
        return placed;
    }
}
