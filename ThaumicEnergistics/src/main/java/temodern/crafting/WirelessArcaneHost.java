package temodern.crafting;

import appeng.helpers.WirelessTerminalMenuHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class WirelessArcaneHost extends WirelessTerminalMenuHost implements PortableArcaneHost,appeng.util.inv.InternalInventoryHost {
    private boolean loading=true;
    private final Workstation work;
    private final appeng.api.networking.IGrid targetGrid;
    private boolean accessPointRange;
    private final appeng.util.inv.AppEngInternalInventory singularity=new appeng.util.inv.AppEngInternalInventory(this,1);
    public WirelessArcaneHost(Player player,int slot,ItemStack stack,java.util.function.BiConsumer<Player,appeng.menu.ISubMenu> reopen){
        super(player,slot,stack,reopen);
        targetGrid=((appeng.items.tools.powered.WirelessTerminalItem)stack.getItem()).getLinkedGrid(stack,player.level(),null);
        work=new Workstation(this);work.load(stack.getOrCreateTag());singularity.readFromNBT(stack.getOrCreateTag(),"singularity");loading=false;
    }
    public appeng.api.inventories.InternalInventory singularity(){return singularity;}
    @Override public void onChangeInventory(appeng.api.inventories.InternalInventory inventory,int slot){
        saveChanges();
    }
    @Override public void saveChanges(){
        if(!loading&&!isClientSide()){singularity.writeToNBT(getItemStack().getOrCreateTag(),"singularity");getPlayer().getInventory().setChanged();}
    }
    private appeng.api.networking.IGridNode quantumNode(){
        var node=WirelessQuantumLink.node(getItemStack(),workLevel(),getUpgrades());
        // An open menu must never silently change the network whose inventory it exposes.
        return node!=null&&node.getGrid()==targetGrid?node:null;
    }
    public boolean inAccessPointRange(){return super.rangeCheck();}
    @Override public boolean rangeCheck(){accessPointRange=super.rangeCheck();return accessPointRange||quantumNode()!=null;}
    @Override public appeng.api.networking.IGridNode getActionableNode(){
        if(super.rangeCheck())return super.getActionableNode();
        return quantumNode();
    }
    @Override public appeng.api.storage.MEStorage getInventory(){
        var node=getActionableNode();return node==null?null:node.getGrid().getStorageService().getInventory();
    }
    @Override protected void setPowerDrainPerTick(double power){super.setPowerDrainPerTick(accessPointRange?power:22.5);}
    public void markWorkChanged(){if(!loading&&!isClientSide()){work.save(getItemStack().getOrCreateTag());getPlayer().getInventory().setChanged();}}
    public Workstation workstation(){return work;}
    public boolean removed(){return !ensureItemStillInSlot()||!isClientSide()&&!active();}
    public appeng.menu.locator.MenuLocator locator(){return appeng.menu.locator.MenuLocators.forInventorySlot(getSlot());}
    @Override public boolean onBroadcastChanges(net.minecraft.world.inventory.AbstractContainerMenu menu){
        if(!super.onBroadcastChanges(menu)||!ensureItemStillInSlot())return false;
        if(!isClientSide()&&workLevel().getGameTime()%5==0)charge();return true;
    }
}
