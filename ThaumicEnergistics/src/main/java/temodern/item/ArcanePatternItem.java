package temodern.item;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.crafting.pattern.EncodedPatternItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import temodern.crafting.ArcanePattern;

/** Use AE2's ordinary input/output tooltip, output preview and pattern clearing behavior. */
public class ArcanePatternItem extends EncodedPatternItem {
    public ArcanePatternItem() { super(new Properties()); }

    @Override public IPatternDetails decode(ItemStack stack, Level level, boolean tryRecovery) {
        return level == null ? null : new ArcanePattern.Decoder().decodePattern(stack, level, tryRecovery);
    }

    @Override public IPatternDetails decode(AEItemKey key, Level level) {
        return decode(key.toStack(), level, false);
    }
}
