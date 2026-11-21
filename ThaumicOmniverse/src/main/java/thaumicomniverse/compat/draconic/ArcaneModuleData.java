package thaumicomniverse.compat.draconic;

import com.brandon3055.draconicevolution.api.modules.data.ModuleData;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleContext;
import java.util.Map;
import net.minecraft.network.chat.Component;

/** Only the strongest installed tier applies, including to stacks imported from edited saves. */
public record ArcaneModuleData(int discount, int potency) implements ModuleData<ArcaneModuleData> {
    public static final ArcaneModuleData NONE = new ArcaneModuleData(0, 0);

    @Override public ArcaneModuleData combine(ArcaneModuleData other) {
        return new ArcaneModuleData(Math.max(discount, other.discount), Math.max(potency, other.potency));
    }

    @Override public void addInformation(Map<Component, Component> information, ModuleContext context) {
        information.put(Component.translatable("omniverse.module.vis_discount"), Component.literal(discount + "%"));
        information.put(Component.translatable("attribute.name.thaumcraft.potency"), Component.literal("+" + potency));
    }
}
