package temodern.essentia;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class EssentiaKeyType extends AEKeyType {
    public static final EssentiaKeyType INSTANCE = new EssentiaKeyType();
    private EssentiaKeyType() { super(new ResourceLocation("thaumicenergistics", "essentia"), EssentiaKey.class,
            Component.translatable("gui.thaumicenergistics.essentia")); }
    @Override public AEKey readFromPacket(FriendlyByteBuf input) { return EssentiaKey.parse(input.readUtf(64)); }
    @Override public AEKey loadKeyFromTag(CompoundTag tag) { return EssentiaKey.parse(tag.getString("aspect")); }
    @Override public int getAmountPerByte() { return 8; }
    @Override public int getAmountPerOperation() { return 1; }
}
