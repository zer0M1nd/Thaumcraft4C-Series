package tc4revival.content;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
public  class NaturalAuraFeature extends Feature<NoneFeatureConfiguration> {
    public NaturalAuraFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        boolean oreClaimed=thaumcraft.api.worldgen.NaturalGenerationRules.generateOres(context);
        var level=context.level();var dimension=level.getLevel().dimension();
        var chunk=new net.minecraft.world.level.ChunkPos(context.origin());
        var random=net.minecraft.util.RandomSource.create(context.random().nextLong());
        // Ores must complete during FEATURES, including chunks generated without a nearby player.
        // The aura queue requires fully loaded neighbours and may remain pending indefinitely.
        if(!oreClaimed&&!dimension.equals(net.minecraft.world.level.Level.NETHER)&&!dimension.equals(net.minecraft.world.level.Level.END)
                &&!dimension.equals(TCEldritch.OUTER_LANDS)
                &&level.getBiome(context.origin()).is(net.minecraft.tags.BiomeTags.IS_OVERWORLD))NaturalOreGeneration.generate(level,chunk,random,true);
        NaturalAuraQueue.enqueue(level.getLevel(),chunk,random.nextLong());return true;
    }
}
