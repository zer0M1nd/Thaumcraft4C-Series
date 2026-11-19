package thaumicfutures.compat.railcraft;

import mods.railcraft.world.item.CrowbarItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.content.*;

public class ArcaneCrowbar extends CrowbarItem implements WarpingGear {
    private final boolean voidMetal;
    public ArcaneCrowbar(boolean voidMetal){super(2,-2.4f,voidMetal?VoidToolTier.INSTANCE:ThaumiumToolTier.INSTANCE,new Item.Properties());this.voidMetal=voidMetal;}
    @Override public int warp(ItemStack stack,Player wearer){return voidMetal?1:0;}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){super.inventoryTick(stack,level,entity,slot,selected);if(voidMetal&&!level.isClientSide)VoidEquipment.repair(stack,entity);}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack repair){return repair.is(TCItems.material(voidMetal?MaterialType.VOID:MaterialType.THAUMIUM));}
}
