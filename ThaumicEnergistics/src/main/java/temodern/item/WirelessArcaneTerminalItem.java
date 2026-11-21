package temodern.item;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import temodern.crafting.*;

/** Uses AE2 access-point binding, energy storage, upgrades and native terminal menus. */
public class WirelessArcaneTerminalItem extends WirelessTerminalItem {
    public WirelessArcaneTerminalItem(){super(appeng.core.AEConfig.instance().getWirelessTerminalBattery(),new Properties().stacksTo(1));}
    @Override public net.minecraft.world.inventory.MenuType<?> getMenuType(){return WorkMenu.TERMINAL.get();}
    @Override public appeng.api.upgrades.IUpgradeInventory getUpgrades(ItemStack stack){
        return appeng.api.upgrades.UpgradeInventories.forItem(stack,4,(item,upgrades)->
                setAEMaxPowerMultiplier(item,1+appeng.api.upgrades.Upgrades.getEnergyCardMultiplier(upgrades)));
    }
    @Override public appeng.api.networking.IGrid getLinkedGrid(ItemStack stack,net.minecraft.world.level.Level level,Player messages){
        var linked=super.getLinkedGrid(stack,level,null);
        if(linked!=null)return linked;
        var bridge=WirelessQuantumLink.node(stack,level,getUpgrades(stack));
        if(bridge!=null)return bridge.getGrid();
        return super.getLinkedGrid(stack,level,messages);
    }
    @Override public ItemMenuHost getMenuHost(Player player,int slot,ItemStack stack,BlockPos pos){
        if(!temodern.RevivalFeatures.wireless())return null;
        return new WirelessArcaneHost(player,slot,stack,(p,sm)->openFromInventory(p,slot,true));
    }
    @Override public void inventoryTick(ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected){
        super.inventoryTick(stack,level,entity,slot,selected);
        if(!temodern.RevivalFeatures.charging()||level.isClientSide||level.getGameTime()%5!=0||!(entity instanceof net.minecraft.server.level.ServerPlayer player)
                ||!getUpgrades(stack).isInstalled(temodern.TE.MATERIALS.get("vis_charging_card").get()))return;
        if(player.containerMenu instanceof ArcaneTerminalMenu menu&&menu.usesItem(stack))return;
        if(!hasPower(player,.5,stack))return;
        var host=new WirelessArcaneHost(player,slot,stack,(p,sm)->{});
        // A closed terminal still pays for the five ticks of wireless access used to charge its wand.
        double cost=host.inAccessPointRange()?2.5:112.5;
        if(host.active()&&hasPower(player,cost,stack)){usePower(player,cost,stack);host.charge();}
    }
}
