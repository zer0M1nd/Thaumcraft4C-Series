package thaumicomniverse.compat.create;

import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import tc4revival.content.*;

public class VoidSandpaper extends SandPaperItem implements RepairableItem,WarpingGear {
    public VoidSandpaper(){super(new Properties().rarity(Rarity.UNCOMMON));}
    @Override public int getMaxDamage(ItemStack stack){return 64;}
    @Override public int warp(ItemStack stack,Player player){return 1;}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){super.inventoryTick(stack,level,holder,slot,selected);if(!level.isClientSide)VoidEquipment.repair(stack,holder);}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack ingredient){return ingredient.is(TCItems.material(MaterialType.VOID));}
}
