package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;

/** Compatibility facade for previously tagged potion loot. */
public  class LegacyPotionEffects {
    public static final String TAG="tc4_legacy_potion_effects";
    private LegacyPotionEffects() {}
    /** Legacy tags remain readable in existing items; modern potion identities determine effects. */
    public static List<MobEffectInstance> resolve(List<MobEffectInstance> ordinary,CompoundTag tag) { return ordinary; }
}
