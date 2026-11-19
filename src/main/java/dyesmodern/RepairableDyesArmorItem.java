package dyesmodern;

import net.minecraft.world.item.ArmorMaterial;
import tc4revival.content.RepairableItem;

/** Runic armor intentionally does not implement the Repair-enchantment marker. */
public class RepairableDyesArmorItem extends DyesArmorItem implements RepairableItem {
    public RepairableDyesArmorItem(String family,String legacyKey,ArmorMaterial material,Type type){super(family,legacyKey,material,type);}
}
