package tc4revival.content;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public  class FocusPouchItem extends Item implements top.theillusivec4.curios.api.type.capability.ICurioItem {
    public FocusPouchItem() { super(new Properties().stacksTo(1)); }
    @Override public Rarity getRarity(ItemStack stack) { return Rarity.RARE; }
    @Override public boolean isFoil(ItemStack stack) { return false; }
    @Override public boolean canEquipFromUse(top.theillusivec4.curios.api.SlotContext context, ItemStack stack) { return false; }
    public static NonNullList<ItemStack> contents(ItemStack pouch) {
        var result = NonNullList.withSize(18, ItemStack.EMPTY);
        if (!pouch.hasTag()) return result;
        var entries = pouch.getTag().getList("Inventory", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i); int slot = entry.getByte("Slot") & 255;
            if (slot < result.size()) result.set(slot, ItemStack.of(entry));
        }
        return result;
    }
    public static void save(ItemStack pouch, java.util.List<ItemStack> contents) {
        if (contents.size() != 18) throw new IllegalArgumentException("Focus pouch needs 18 slots");
        var entries = new ListTag();
        for (int slot = 0; slot < contents.size(); slot++) if (!contents.get(slot).isEmpty()) {
            var entry = contents.get(slot).save(new CompoundTag()); entry.putByte("Slot", (byte) slot); entries.add(entry);
        }
        pouch.getOrCreateTag().put("Inventory", entries);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) net.minecraftforge.network.NetworkHooks.openScreen(server,
                new SimpleMenuProvider((id, inventory, owner) -> new FocusPouchMenu(id, inventory, hand),
                        net.minecraft.network.chat.Component.translatable("item.thaumcraft.focus_pouch")), buffer -> buffer.writeEnum(hand));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
