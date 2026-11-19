package thaumicfutures.expansion;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
public final class RestoredOreFeature extends OreFeature {
    public RestoredOreFeature(){super(OreConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<OreConfiguration> context){
        return thaumicfutures.FutureFeatures.Group.RESTORATION.enabled()
                &&context.level().getLevel().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)&&super.place(context);
    }
}
