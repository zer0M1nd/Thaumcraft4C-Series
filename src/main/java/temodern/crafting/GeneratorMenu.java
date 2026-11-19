package temodern.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import temodern.machine.NetworkMachine;

/** Read-only fuel and load instrumentation, synchronized by vanilla menu data. */
public final class GeneratorMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    public final ContainerData data;
    public GeneratorMenu(int id,Inventory inventory,FriendlyByteBuf buffer){this(id,inventory,buffer.readBlockPos(),new SimpleContainerData(6));}
    public GeneratorMenu(int id,Inventory inventory,NetworkMachine machine){this(id,inventory,machine.getBlockPos(),new ContainerData(){
        public int getCount(){return 6;}
        public int get(int index){return switch(index){case 0->machine.fuelAmount();case 1->machine.fuelKind();case 2->machine.energyRemaining();case 3->machine.fuelEnergy();case 4->machine.productionRate();case 5->machine.operatingSpeed();default->0;};}
        public void set(int index,int value){}
    });}
    private GeneratorMenu(int id,Inventory inventory,BlockPos pos,ContainerData data){super(WorkMenu.GENERATOR.get(),id);this.pos=pos;this.data=data;addDataSlots(data);}
    @Override public boolean stillValid(Player player){return player.distanceToSqr(pos.getCenter())<=64&&player.level().getBlockEntity(pos) instanceof NetworkMachine machine&&machine.kind().equals("essentia_vibration_chamber");}
    @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
}
