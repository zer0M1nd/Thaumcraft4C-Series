package tc4revival.content;

import net.minecraft.world.level.block.SoundType;

/** Optional block variants; registration stays stable across world configuration changes. */
public class DeepslateOreBlock extends LegacyOreBlock {
    public DeepslateOreBlock(int metadata) {
        super(metadata,Properties.of().strength(2.25f,5).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops());
    }
    public static String name(int metadata){return "deepslate_"+LegacyOreNames.path(metadata);}
    public static boolean enabled(){return !tc4revival.config.TCConfig.SERVER_SPEC.isLoaded() || tc4revival.config.TCConfig.SERVER.revival.expandedOres.get();}
}
