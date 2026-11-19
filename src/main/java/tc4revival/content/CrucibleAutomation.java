package tc4revival.content;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import tc4revival.aspect.*;
import tc4revival.aspect.Aspect;
import thaumcraft.api.aspects.*;

/** Hidden input/output slots; accepted ingredients follow the ordinary hot crucible transaction. */
public final class CrucibleAutomation extends ItemStackHandler {
    private final CrucibleBlockEntity owner;
    public CrucibleAutomation(CrucibleBlockEntity owner) { super(2); this.owner = owner; }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0; }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return slot == 0 ? super.insertItem(slot, stack, simulate) : stack;
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slot == 1 ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }
    @Override protected void onContentsChanged(int slot) { owner.synchronize(); }
    public void tick() {
        if (getStackInSlot(0).isEmpty() || !getStackInSlot(1).isEmpty()) return;
        var result = owner.processAutomation(getStackInSlot(0).copyWithCount(1));
        if (result.isEmpty()) return;
        setStackInSlot(0, getStackInSlot(0).copyWithCount(getStackInSlot(0).getCount() - 1));
        setStackInSlot(1, result.get());
    }
    public void drop() {
        if (owner.getLevel() == null || owner.getLevel().isClientSide) return;
        var pos = owner.getBlockPos();
        for (int i = 0; i < getSlots(); i++) {
            var stack = getStackInSlot(i); setStackInSlot(i, ItemStack.EMPTY);
            if (!stack.isEmpty()) net.minecraft.world.Containers.dropItemStack(owner.getLevel(), pos.getX()+.5, pos.getY()+1, pos.getZ()+.5, stack);
        }
    }
    public IEssentiaStorage essentiaInput() {
        return new IEssentiaStorage() {
            public AspectAmounts contents() { return owner.contents().aspects(); }
            public int capacity(Aspect aspect) { return aspect == null || owner.isRemoved() ? 0 : Integer.MAX_VALUE; }
            public int insert(Aspect aspect, int amount, boolean simulate) {
                if (amount < 0) throw new IllegalArgumentException("Negative essentia");
                int accepted = aspect == null ? 0 : Math.max(0, Math.min(amount, capacity(aspect) - owner.contents().aspects().amount(aspect)));
                if (accepted > 0 && !simulate) {
                    if (!owner.contents().dissolve(new AspectAmounts().add(aspect, accepted))) return 0;
                    owner.synchronize();
                }
                return accepted;
            }
            public int extract(Aspect aspect, int amount, boolean simulate) { return 0; }
        };
    }
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft", bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
            event.enqueueWork(() -> EssentiaStorageApi.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:crucible_input"),
                    (entity, side) -> entity instanceof CrucibleBlockEntity crucible ? crucible.automation().essentiaInput() : null));
        }
    }
}
