package temodern.essentia;

import java.util.List;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

/** Identity is the data-pack tag, never an ordinal or mutable aspect metadata. */
public final class EssentiaKey extends AEKey {
    private final String tag;
    private EssentiaKey(String tag) { this.tag = tag; }
    public static EssentiaKey of(Aspect aspect) { return new EssentiaKey(aspect.tag()); }
    public static EssentiaKey parse(String tag) {
        return TCAspects.catalog().find(tag).map(EssentiaKey::of).orElse(null);
    }
    public String tag() { return tag; }
    public Aspect aspect() { return TCAspects.catalog().find(tag).orElse(null); }
    @Override public AEKeyType getType() { return EssentiaKeyType.INSTANCE; }
    @Override public AEKey dropSecondary() { return this; }
    @Override public CompoundTag toTag() { var nbt = new CompoundTag(); nbt.putString("aspect", tag); return nbt; }
    @Override public Object getPrimaryKey() { return tag; }
    @Override public ResourceLocation getId() { return new ResourceLocation(tag.contains(":") ? tag : "thaumcraft:" + tag); }
    @Override public void writeToPacket(FriendlyByteBuf buffer) { buffer.writeUtf(tag, 64); }
    @Override protected Component computeDisplayName() {
        var aspect = aspect();
        return Component.translatable("gui.thaumicenergistics.aspect_name",aspect == null ? tag : thaumcraft.api.aspects.AspectViews.display(aspect).displayName());
    }
    @Override public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
        if (amount > 0) drops.add(EssentiaAmpoule.filled(this, amount));
    }
    @Override public boolean equals(Object other) { return other instanceof EssentiaKey key && tag.equals(key.tag); }
    @Override public int hashCode() { return tag.hashCode(); }
}
