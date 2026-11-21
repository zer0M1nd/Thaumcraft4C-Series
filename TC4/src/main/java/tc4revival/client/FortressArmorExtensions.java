package tc4revival.client;

import net.minecraft.client.model.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public  class FortressArmorExtensions implements IClientItemExtensions {
    private FortressArmorModel inner,outer;
    @Override public Model getGenericArmorModel(LivingEntity wearer,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original) {
        boolean chest=slot==EquipmentSlot.CHEST;
        if (chest && outer==null) outer=new FortressArmorModel(true);
        if (!chest && inner==null) inner=new FortressArmorModel(false);
        var model=chest?outer:inner; model.prepare(wearer,slot,original); return model;
    }
}
