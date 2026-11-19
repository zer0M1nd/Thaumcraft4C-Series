package thaumicfutures.compat.machinery;

import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class AspectPortMenu extends AbstractContainerMenu {
    public final AspectPort port;
    public AspectPortMenu(int id, AspectPort port) { super(MachineryContent.PORT_MENU.get(), id); this.port = port; }
    public static AspectPortMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        var tile = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        if (!(tile instanceof AspectPort port)) throw new IllegalStateException("Missing machine port for menu");
        return new AspectPortMenu(id, port);
    }
    @Override public boolean stillValid(Player player) {
        return !port.isRemoved() && player.level() == port.getLevel()
                && player.level().getBlockEntity(port.getBlockPos()) == port
                && player.distanceToSqr(port.getBlockPos().getX() + .5, port.getBlockPos().getY() + .5, port.getBlockPos().getZ() + .5) <= 64;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || !stillValid(player) || button < 0 || button >= 6
                || !player.level().mayInteract(player, port.getBlockPos())) return false;
        Direction face = Direction.values()[button];
        if (port.kind() == AspectPort.Kind.BRIDGE) port.selectAbsorption(face); else port.toggleSide(face);
        return true;
    }
}
