package thaumicomniverse.compat.thermal;
import cofh.core.common.inventory.BlockEntityCoFHMenu;
import cofh.lib.common.inventory.wrapper.InvWrapperCoFH;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.level.Level;

public class EssentiaDynamoMenu extends BlockEntityCoFHMenu {
    public final EssentiaDynamo tile;
    public EssentiaDynamoMenu(int id,Level level,BlockPos pos,Inventory inventory,Player player){
        super(ThermalContent.MENU.get(),id,level,pos,inventory,player);
        tile=(EssentiaDynamo)level.getBlockEntity(pos);
        bindAugmentSlots(new InvWrapperCoFH(tile.getItemInv()),0,tile.augSize());bindPlayerInventory(inventory);
    }
}
