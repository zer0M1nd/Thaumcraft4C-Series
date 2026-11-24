package temodern.crafting;

/** Common transaction host for standalone machines and cable-mounted terminals. */
public interface WorkHost extends appeng.api.networking.security.IActionHost, appeng.api.storage.ITerminalHost, appeng.api.inventories.ISegmentedInventory {
    @Override default appeng.api.inventories.InternalInventory getSubInventory(net.minecraft.resources.ResourceLocation id){
        return appeng.parts.reporting.CraftingTerminalPart.INV_CRAFTING.equals(id)?workstation().craftingMatrix:null;
    }
    net.minecraft.world.level.Level workLevel();
    net.minecraft.core.BlockPos workPosition();
    void markWorkChanged();
    String kind();
    appeng.api.networking.IManagedGridNode node();
    appeng.api.networking.security.IActionSource action();
    boolean consumeNetworkVis(java.util.Map<tc4revival.aspect.Aspect,Integer> costs,boolean simulate);
    Workstation workstation();
    boolean removed();
    appeng.menu.locator.MenuLocator locator();
    default boolean active(){var node=getActionableNode();return node!=null&&node.isActive();}
    default appeng.api.networking.IGrid grid(){var node=getActionableNode();return node==null?null:node.getGrid();}
    default boolean inventoryWands(){return false;}
}
