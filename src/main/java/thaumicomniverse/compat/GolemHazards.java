package thaumicomniverse.compat;

import java.util.Set;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.api.golems.GolemPathHazards;

/** Exact implementation families reviewed against the installed ATM9 jars; no optional class loading. */
public final class GolemHazards {
    private static final Set<String> CONTACT_TYPES=Set.of(
            "slimeknights.tconstruct.fluids.block.BurningLiquidBlock",
            "slimeknights.tconstruct.plugin.jsonthings.block.FlexBurningLiquidBlock",
            "com.mcwfences.kikoz.objects.WiredFence",
            "com.mcwfences.kikoz.objects.StoneWiredFence",
            "twilightforest.block.SliderBlock");
    private GolemHazards() {}
    public static void setup() {
        for(var block:ForgeRegistries.BLOCKS)for(Class<?> type=block.getClass();type!=null;type=type.getSuperclass()) {
            if(CONTACT_TYPES.contains(type.getName())) { GolemPathHazards.register(block);break; }
        }
    }
}
