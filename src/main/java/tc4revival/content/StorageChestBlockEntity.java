package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class StorageChestBlockEntity extends ChestBlockEntity {
    public static final TagKey<Item> VOID_TOOLS = TagKey.create(Registries.ITEM, ResourceLocation.parse("thaumcraft:void_tools"));
    private CompoundTag repairCredit = new CompoundTag();

    public StorageChestBlockEntity(BlockPos pos, BlockState state) {
        super(TCArcane.STORAGE_CHEST_ENTITY.get(), pos, state);
        setItems(NonNullList.withSize(getContainerSize(), ItemStack.EMPTY));
    }

    @Override public int getContainerSize() { return ((StorageChestBlock)getBlockState().getBlock()).capacity(); }
    @Override protected Component getDefaultName() { return getBlockState().getBlock().getName(); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new StorageChestMenu(id, inventory, this); }
    @Override protected void signalOpenCount(Level level, BlockPos pos, BlockState state, int before, int after) {
        super.signalOpenCount(level, pos, state, before, after);
        if (state.getValue(StorageChestBlock.OPEN) != (after > 0))
            level.setBlock(pos, state.setValue(StorageChestBlock.OPEN, after > 0), 2);
    }

    static void tick(Level level, BlockPos pos, BlockState state, StorageChestBlockEntity chest) {
        long time = level.getGameTime();
        if (time % 20 != 0 || chest.lootTable != null) return;
        if (state.getValue(StorageChestBlock.OPEN) && ChestBlockEntity.getOpenCount(level, pos) == 0)
            level.setBlock(pos, state.setValue(StorageChestBlock.OPEN, false), 2);
        boolean voidChest = state.is(TCArcane.VOID_CHEST.get());
        boolean changed = false;
        for (var stack : chest.getItems()) {
            if (!stack.isDamaged()) continue;
            if (voidChest && stack.is(VOID_TOOLS)) {
                stack.setDamageValue(stack.getDamageValue() - 1);
                changed = true;
            }
            if (time % 40 == 0 && DeviceVisRepair.needsRepair(stack)) {
                // A partially supplied debit also has to survive a world save.
                changed |= DeviceVisRepair.repair(stack, level, null, chest.repairCredit, (aspect, amount) -> {
                    int supplied = VisNetwork.drain(level, pos, aspect, amount);
                    if (supplied > 0) chest.setChanged();
                    return supplied;
                });
            }
        }
        if (changed) chest.setChanged();
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("RepairCredit", repairCredit.copy());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        repairCredit = tag.getCompound("RepairCredit");
    }
}
