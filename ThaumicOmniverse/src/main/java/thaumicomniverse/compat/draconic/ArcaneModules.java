package thaumicomniverse.compat.draconic;

import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.api.capability.DECapabilities;
import com.brandon3055.draconicevolution.api.modules.Module;
import com.brandon3055.draconicevolution.api.modules.ModuleCategory;
import com.brandon3055.draconicevolution.api.modules.items.ModuleItem;
import com.brandon3055.draconicevolution.api.modules.lib.ModuleImpl;
import com.brandon3055.draconicevolution.api.modules.types.ModuleTypeImpl;
import com.brandon3055.draconicevolution.init.DEModules;
import com.brandon3055.draconicevolution.items.equipment.IModularArmor;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import tc4revival.content.TCAttributes;
import thaumicomniverse.ThaumicOmniverse;

/** Loaded only when DE is installed; uses DE's own equipped-armor selection, including Curios. */
public class ArcaneModules {
    public static final ModuleTypeImpl<ArcaneModuleData> ARCANE = new ModuleTypeImpl<>("thaumicomniverse_arcane", 2, 2, ModuleCategory.CHESTPIECE, ModuleCategory.ARMOR_CHEST);
    public static final DeferredRegister<Module<?>> MODULES = DeferredRegister.create(DEModules.MODULE_KEY, ThaumicOmniverse.MOD_ID);
    private static final UUID POTENCY = UUID.fromString("b6a4605f-d764-4c5e-b9bf-e54da17d2308");
    private static final UUID DISCOUNT = UUID.fromString("6e7bd563-fc41-46f1-90b1-a3a7d4cd7557");

    public static void register(IEventBus bus) {
        ARCANE.setMaxInstallable(1);
        for (var tier : TechLevel.values()) {
            int rank = tier.index + 1;
            String name = tier.name().toLowerCase(Locale.ROOT) + "_arcane";
            var module = MODULES.register(name, () -> new ModuleImpl<>(ARCANE, tier, ignored -> new ArcaneModuleData(rank * 3, rank * 5)));
            ThaumicOmniverse.ITEMS.register("item_" + name, () -> new ModuleItem<>(module::get));
        }
        DEModules.MODULE_PROVIDING_MODS.add(ThaumicOmniverse.MOD_ID);
        MODULES.register(bus);
        MinecraftForge.EVENT_BUS.addListener(ArcaneModules::tick);
    }

    private static void tick(LivingEvent.LivingTickEvent event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide) return;
        var stack = IModularArmor.getArmor(entity);
        var data = stack.getCapability(DECapabilities.MODULE_HOST_CAPABILITY).resolve()
                .map(host -> host.getModuleData(ARCANE, ArcaneModuleData.NONE)).orElse(ArcaneModuleData.NONE);
        if (!thaumicomniverse.RevivalFeatures.Group.DRACONIC.enabled()) data = ArcaneModuleData.NONE;
        update(entity.getAttribute(TCAttributes.POTENCY.get()), POTENCY, data.potency());
        update(entity.getAttribute(TCAttributes.VIS_COST.get()), DISCOUNT, -data.discount() / 100d);
    }

    private static void update(AttributeInstance attribute, UUID id, double amount) {
        if (attribute == null) return;
        var old = attribute.getModifier(id);
        if (old != null && old.getAmount() == amount) return;
        if (old != null) attribute.removeModifier(id);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(id, "Arcane module", amount, AttributeModifier.Operation.ADDITION));
    }
}
