package gadomancymodern.test;
/** Exercise the real config suppliers without writing a watched file during a synchronous test. */
public class TemporaryCoreConfig implements AutoCloseable {
    protected final com.electronwill.nightconfig.core.CommentedConfig original;
    public TemporaryCoreConfig(){var config=net.minecraftforge.fml.config.ConfigTracker.INSTANCE.configSets().get(net.minecraftforge.fml.config.ModConfig.Type.SERVER).stream().filter(c->c.getSpec()==tc4revival.config.TCConfig.SERVER_SPEC).findFirst().orElseThrow();original=config.getConfigData();tc4revival.config.TCConfig.SERVER_SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.copy(original));}
    @Override public void close(){tc4revival.config.TCConfig.SERVER_SPEC.setConfig(original);}
}
