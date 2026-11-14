package forgottenrelics;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

import java.util.*;

@Mod(ForgottenRelics.ID)
public final class ForgottenRelics {
    public static final String ID = "forgottenrelics";
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    private static final EnumMap<Relic, RegistryObject<Item>> RELICS = new EnumMap<>(Relic.class);

    static {
        for (var relic : Relic.values())
            RELICS.put(
                    relic,
                    ITEMS.register(
                            relic.id,
                            () ->
                                    switch (relic) {
                                        case TERROR_CROWN -> new TerrorCrownItem();
                                        case PARADOX -> new ParadoxItem();
                                        default ->
                                                relic.slot.isEmpty()
                                                        ? new RelicItem(relic)
                                                        : new RelicBaubleItem(relic);
                                    }));
        TABS.register(
                "relics",
                () ->
                        CreativeModeTab.builder()
                                .title(Component.translatable("itemGroup.forgottenrelics"))
                                .icon(() -> new ItemStack(item(Relic.APOTHEOSIS)))
                                .displayItems(
                                        (parameters, output) -> {
                                            for (var relic : Relic.values())
                                                if (relic != Relic.GHASTLY_SKULL)
                                                    output.accept(item(relic));
                                        })
                                .build());
    }

    public static Item item(Relic relic) {
        return RELICS.get(relic).get();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(ID, path);
    }

    public ForgottenRelics() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        TABS.register(bus);
        RelicProjectiles.TYPES.register(bus);
        OblivionRecipe.SERIALIZERS.register(bus);
        RelicSounds.SOUNDS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RelicsConfig.SPEC);
        RelicsNetwork.register();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                ForgottenKnowledge::registerReload);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                ForgottenKnowledge::advancement);
        bus.addListener(
                (net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                        event.enqueueWork(
                                () -> {
                                    thaumcraft.api.research.ResearchCategories.registerEnablement(
                                            ID + ":FalseJustice",
                                            () -> RelicsConfig.enabled("falseJusticeEnabled"));
                                    thaumcraft.api.research.ResearchCategories.registerEnablement(
                                            ID + ":VoidGrimoire",
                                            () -> RelicsConfig.enabled("voidGrimoireEnabled"));
                                }));
    }
}
