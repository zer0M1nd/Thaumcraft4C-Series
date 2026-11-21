package dyesmodern;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Optional consumers receive the actual registered original equipment, without a reverse dependency. */
public final class DyesEquipment {
    private DyesEquipment() {}
    public static ItemStack forConcilium(String role,EquipmentSlot slot) {
        String part=switch(slot){case HEAD->"Helmet";case CHEST->"Chest";case LEGS->"Legs";default->null;};
        if(part==null)return ItemStack.EMPTY;
        String family=switch(role){case "paladin"->slot==EquipmentSlot.HEAD?"Paladin":null;case "archer"->"Archer";case "ranger"->"Ranger";default->null;};
        return family==null?ItemStack.EMPTY:new ItemStack(DyesItems.get("itemCultist"+family+part));
    }
}
