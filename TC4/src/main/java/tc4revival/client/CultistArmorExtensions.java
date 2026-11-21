package tc4revival.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import tc4revival.content.CultistEquipment.Style;

public  class CultistArmorExtensions implements IClientItemExtensions {
    private final java.util.function.Supplier<Style> styleSupplier;
    private CultistArmorModel inner,outer;
    public CultistArmorExtensions(java.util.function.Supplier<Style> style) { this.styleSupplier=style; }
    @Override public Model getGenericArmorModel(LivingEntity wearer,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original) {
        Style style=styleSupplier.get();
        if (style==Style.BOOTS) return original;
        boolean chest=slot==EquipmentSlot.CHEST;
        if (chest && outer==null) outer=new CultistArmorModel(style,true);
        if (!chest && inner==null) inner=new CultistArmorModel(style,false);
        var model=chest ? outer : inner; model.prepare(wearer,slot,original); return model;
    }
}
