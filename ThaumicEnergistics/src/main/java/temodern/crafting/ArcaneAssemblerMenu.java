package temodern.crafting;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import temodern.machine.NetworkMachine;

/** Molecular assembler layout, with native AE2 upgrade and network-tool support. */
public class ArcaneAssemblerMenu extends appeng.menu.implementations.UpgradeableMenu<NetworkMachine> {
    public static final SlotSemantic KNOWLEDGE=SlotSemantics.register("thaumicenergistics_knowledge",false);
    public static final SlotSemantic WAND=SlotSemantics.register("thaumicenergistics_assembler_wand",false);
    @appeng.menu.guisync.GuiSync(200) public String vis="";
    public ArcaneAssemblerMenu(int id,Inventory player,net.minecraft.network.FriendlyByteBuf data){this(id,player,(NetworkMachine)player.player.level().getBlockEntity(data.readBlockPos()));}
    public ArcaneAssemblerMenu(int id,Inventory player,NetworkMachine host){super(WorkMenu.ASSEMBLER.get(),id,player,host);}
    @Override protected void setupConfig(){
        var container=getHost().workstation.inventory;
        var inventory=new appeng.api.inventories.InternalInventory(){
            public int size(){return container.getContainerSize();}
            public ItemStack getStackInSlot(int slot){return container.getItem(slot);}
            public void setItemDirect(int slot,ItemStack stack){container.setItem(slot,stack);}
            public void sendChangeNotification(int slot){container.setChanged();}
            public int getSlotLimit(int slot){return slot>=9&&slot<=11?1:64;}
            public boolean isItemValid(int slot,ItemStack stack){return slot<9||slot==9&&stack.getItem() instanceof tc4revival.content.CastingWandItem wand&&!wand.isStaff(stack)||slot==10&&AutomationPatterns.accepted(stack)
                    ||slot==11&&stack.getItem() instanceof temodern.item.ArcanePatternItem;}
        };
        for(int i=0;i<9;i++)addSlot(new AppEngSlot(inventory,i),SlotSemantics.MACHINE_CRAFTING_GRID);
        addSlot(new AppEngSlot(inventory,9),WAND);
        addSlot(new AppEngSlot(inventory,10),KNOWLEDGE);
        addSlot(new AppEngSlot(inventory,11),SlotSemantics.ENCODED_PATTERN);
        addSlot(new appeng.menu.slot.OutputSlot(inventory,12,null),SlotSemantics.MACHINE_OUTPUT);
    }
    @Override public void broadcastChanges(){
        if(isServerSide())vis=tc4revival.aspect.TCAspects.catalog().primals().stream().map(a->a.tag()+":"+getHost().storedVis(a)).collect(java.util.stream.Collectors.joining(";"));
        // Like AE2's MolecularAssemblerMenu, this device has no fuzzy/redstone settings.
        // UpgradeableMenu.broadcastChanges would unconditionally read those unsupported settings.
        getToolbox().tick();
        standardDetectAndSendChanges();
    }
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return !getHost().isRemoved()&&player.level()==getHost().getLevel()
            &&player.distanceToSqr(getHost().getBlockPos().getCenter())<=64&&super.stillValid(player);}
}
