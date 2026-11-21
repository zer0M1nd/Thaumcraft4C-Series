package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.item.LegacyItemMappings;
import tc4revival.research.ResearchCatalogs;
import tc4revival.research.ResearchDefinition;
import tc4revival.research.ResearchProgression;

/** Display-only completion event; player knowledge continues through the revisioned stream. */
public record ResearchCompletionPacket(String key, boolean virtual, int column, int row, String texture,
        ItemStack icon, int permanentWarp, int stickyWarp) {
    public ResearchCompletionPacket {
        if (!key.matches("[A-Za-z0-9_:./-]{1,128}") || texture.length() > 256 || permanentWarp < 0 || stickyWarp < 0) {
            throw new IllegalArgumentException("Invalid research completion");
        }
        if (!texture.isEmpty()) ResourceLocation.parse(texture);
        icon = icon.copy();
    }
    @Override public ItemStack icon() { return icon.copy(); }
    public static ResearchCompletionPacket from(ResearchProgression.Completion completion) {
        var research = ResearchCatalogs.catalog().require(completion.key());
        ItemStack icon = ItemStack.EMPTY;
        if (research.display().iconItem().isPresent()) {
            var source = research.display().iconItem().orElseThrow();
            var id = LegacyItemMappings.identity().items().entrySet().stream().filter(entry -> entry.getValue().equals(source.key()))
                    .map(java.util.Map.Entry::getKey).sorted().findFirst();
            var fallback=ResourceLocation.tryParse(source.key().item().toLowerCase(java.util.Locale.ROOT));
            if(id.isEmpty()&&fallback!=null&&ForgeRegistries.ITEMS.containsKey(fallback))id=java.util.Optional.of(fallback.toString());
            if (id.isPresent()) {
                icon = new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(id.orElseThrow())), source.count());
                if (source.nbt().isPresent()) icon.setTag(tc4revival.research.ResearchClues.readNbt(source.nbt()));
            }
        }
        return new ResearchCompletionPacket(research.key(), research.has(ResearchDefinition.Flag.VIRTUAL),
                research.display().column(), research.display().row(), research.display().iconResource().orElse(""), icon,
                completion.permanentWarp(), completion.stickyWarp());
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(key, 128);
        buffer.writeBoolean(virtual);
        buffer.writeInt(column);
        buffer.writeInt(row);
        buffer.writeUtf(texture, 256);
        buffer.writeItem(icon);
        buffer.writeInt(permanentWarp);
        buffer.writeInt(stickyWarp);
    }
    public static ResearchCompletionPacket decode(FriendlyByteBuf buffer) {
        var packet = new ResearchCompletionPacket(buffer.readUtf(128), buffer.readBoolean(), buffer.readInt(), buffer.readInt(),
                buffer.readUtf(256), buffer.readItem(), buffer.readInt(), buffer.readInt());
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing completion packet data");
        return packet;
    }
}
