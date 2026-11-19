package thaumicfutures.compat.bees;

import forestry.apiculture.gui.ContainerBeeHelper;
import forestry.core.gui.ContainerTile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DataSlot;

public final class MagicApiaryMenu extends ContainerTile<MagicApiary> {
    public int progress, boosts;
    public MagicApiaryMenu(int id, Inventory inventory, MagicApiary apiary) {
        super(id, BeeAuraContent.APIARY_MENU.get(), inventory, apiary, 8, 110);
        ContainerBeeHelper.addSlots(this, apiary, true);
        apiary.getBeekeepingLogic().clearCachedValues();
        addDataSlot(new DataSlot() {
            @Override public int get() { return apiary.getBeekeepingLogic().getBeeProgressPercent(); }
            @Override public void set(int value) { progress = value; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return apiary.boostFlags(); }
            @Override public void set(int value) { boosts = value; }
        });
    }
    public static MagicApiaryMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        var tile = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        if (!(tile instanceof MagicApiary apiary)) throw new IllegalStateException("Missing magic apiary for menu");
        return new MagicApiaryMenu(id, inventory, apiary);
    }
}
