package thaumicomniverse.compat.gregtech;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

/** Lay out both inputs together before GT measures and binds the recipe template. */
public final class EssentiaFuelRecipeUI extends GTRecipeTypeUI {
    public EssentiaFuelRecipeUI(GTRecipeType type) { super(type); }

    @Override protected WidgetGroup addInventorySlotGroup(boolean outputs, boolean steam, boolean highPressure) {
        if (outputs) return super.addInventorySlotGroup(true, steam, highPressure);
        var group = new WidgetGroup(0, 0, 44, 26);
        var aspect = EssentiaFuelCapability.CAP.createWidget();
        aspect.setSelfPosition(4, 4);
        aspect.setId(EssentiaFuelCapability.CAP.slotName(IO.IN, 0));
        aspect.setBackground(getOverlaysForSlot(false, EssentiaFuelCapability.CAP, true, steam, highPressure));
        group.addWidget(aspect);
        var fluid = FluidRecipeCapability.CAP.createWidget();
        fluid.setSelfPosition(22, 4);
        fluid.setId(FluidRecipeCapability.CAP.slotName(IO.IN, 0));
        fluid.setBackground(getOverlaysForSlot(false, FluidRecipeCapability.CAP, true, steam, highPressure));
        group.addWidget(fluid);
        return group;
    }
}
