package automagy.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Vanilla menu buttons carry bounded operations, never client-supplied block positions or NBT. */
public final class HourglassMenu extends AbstractContainerMenu {
    private final HourglassBlockEntity tile;
    private final ContainerData data;
    public HourglassMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof HourglassBlockEntity be ? be : null);
    }
    public HourglassMenu(int id, Inventory inventory, HourglassBlockEntity tile) {
        super(Content.HOURGLASS_MENU.get(), id);
        this.tile = tile;
        this.data = tile == null || inventory.player.level().isClientSide ? new SimpleContainerData(6) : new ContainerData() {
            public int get(int index) { return switch (index) {
                case 0 -> tile.timer.target() / 20; case 1 -> tile.timer.signalMode(); case 2 -> tile.timer.flipMode();
                case 3 -> tile.timer.inputMode(); case 4 -> tile.timer.repeat() ? 1 : 0; case 5 -> tile.timer.remainingSeconds(); default -> 0;
            }; }
            public void set(int index, int value) {}
            public int getCount() { return 6; }
        };
        addDataSlots(data);
        // Original container includes the player's inventory below the settings.
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, 8 + column * 18, 162));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, 9 + column + row * 9, 8 + column * 18, 104 + row * 18));
    }
    public int value(int field) { return data.get(field); }
    @Override public boolean stillValid(Player player) {
        return tile != null && !tile.isRemoved() && tile.getLevel() == player.level() && player.mayBuild()
                && player.level().getBlockEntity(tile.getBlockPos()) == tile
                && tile.getBlockState().is(Content.MAGIC_HOURGLASS.get())
                && player.distanceToSqr(tile.getBlockPos().getCenter()) <= 64;
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        var timer = tile.timer;
        if(button>=10&&button<=17){
            int index=(button-10)/2;int[] increments={60,600,1,10};int sign=(button&1)==0?1:-1;
            timer.setSeconds(Math.floorMod(timer.target()/20-1+increments[index]*sign,3600)+1);tile.changed();broadcastChanges();return true;
        }
        if(button>=20&&button<=25){
            int operation=(button-20)/2,step=(button&1)==0?1:-1;
            int signal=timer.signalMode(),sand=timer.flipMode(),input=timer.inputMode();
            if(operation==0){int[] order={0,1,4,3,2};for(int i=0;i<order.length;i++)if(order[i]==signal){signal=order[Math.floorMod(i+step,5)];break;}}
            else if(operation==1)sand=Math.floorMod(sand+step,3);else input=Math.floorMod(input+step,4);
            timer.configure(signal,sand,input,timer.repeat());tile.changed();broadcastChanges();return true;
        }
        switch (button) {
            case 0 -> timer.setSeconds(timer.target()/20 - 1);
            case 1 -> timer.setSeconds(timer.target()/20 + 1);
            case 2 -> timer.setSeconds(timer.target()/20 - 10);
            case 3 -> timer.setSeconds(timer.target()/20 + 10);
            case 4 -> timer.configure((timer.signalMode()+1)%5, timer.flipMode(), timer.inputMode(), timer.repeat());
            case 5 -> timer.configure(timer.signalMode(), (timer.flipMode()+1)%3, timer.inputMode(), timer.repeat());
            case 6 -> timer.configure(timer.signalMode(), timer.flipMode(), (timer.inputMode()+1)%4, timer.repeat());
            case 7 -> timer.configure(timer.signalMode(), timer.flipMode(), timer.inputMode(), !timer.repeat());
            case 8 -> tile.flip();
            default -> { return false; }
        }
        tile.changed(); broadcastChanges(); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
