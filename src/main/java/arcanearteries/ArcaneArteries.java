package arcanearteries;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import thaumcraft.api.research.ResearchCategories;
import wayoftime.bloodmagic.block.enums.BloodRuneType;
import wayoftime.bloodmagic.common.block.BlockBloodRune;
import arcanearteries.SacrificeValues.School;

@Mod(ArcaneArteries.ID)
public final class ArcaneArteries {
    public static final String ID = "arcanearteries";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final Map<String, RegistryObject<Item>> CONTENT = new LinkedHashMap<>();
    public static final RegistryObject<Item> THAUMIC_KNIFE = knife("thaumic_knife", School.THAUMIC, false);
    public static final RegistryObject<Item> BOTANICAL_KNIFE = knife("botanical_knife", School.BOTANICAL, false);
    public static final RegistryObject<Item> EVIL_KNIFE = knife("evil_knife", School.EVIL, false);
    public static final RegistryObject<Item> CUSTOM_KNIFE = knife("config_dagger", School.CUSTOM, false);
    public static final RegistryObject<Item> THAUMIC_SACRIFICE = knife("thaumic_sacrifice", School.THAUMIC, true);
    public static final RegistryObject<Item> BOTANICAL_SACRIFICE = knife("botanical_sacrifice", School.BOTANICAL, true);
    public static final RegistryObject<Item> EVIL_SACRIFICE = knife("evil_sacrifice", School.EVIL, true);
    public static final RegistryObject<Item> CUSTOM_SACRIFICE = knife("config_sacrifice", School.CUSTOM, true);
    public static final RegistryObject<Item> THAUMIC_SLATE = simple("thaumic_slate");
    public static final RegistryObject<Item> MANA_SLATE = simple("mana_slate");
    public static final RegistryObject<Block> THAUMIC_RUNE = rune("thaumic_rune");
    public static final RegistryObject<Block> MANA_RUNE = rune("mana_rune");
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, ID);
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.arcanearteries"))
            .icon(() -> THAUMIC_KNIFE.get().getDefaultInstance())
            .displayItems((parameters, output) -> CONTENT.forEach((name, item) -> { if (visible(name)) output.accept(item.get()); })).build());

    public ArcaneArteries() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ArteriesConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ArteriesConfig.CLIENT_SPEC);
        ITEMS.register(bus); BLOCKS.register(bus); TABS.register(bus);
        CraftingHelper.register(new FeatureCondition.Serializer());
        bus.addListener(this::setup);
        bus.addListener(ConfigSync::setup);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new ConfigSync());
    }
    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var bloodMagic = wayoftime.bloodmagic.api.IBloodMagicAPI.INSTANCE.get();
            bloodMagic.registerAltarComponent(THAUMIC_RUNE.get().defaultBlockState(), "bloodrune");
            bloodMagic.registerAltarComponent(MANA_RUNE.get().defaultBlockState(), "bloodrune");
            for (String key : new String[]{"DAGGER", "DAGGERH", "SACRIFICEDAGGER", "SACRIFICEDAGGERH", "SLATE", "RUNE"})
                ResearchCategories.registerEnablement(ID + ":" + key, () -> researchEnabled(key));
        });
    }
    public static ResourceLocation id(String path) { return new ResourceLocation(ID, path); }
    private static RegistryObject<Item> simple(String name) {
        RegistryObject<Item> item = ITEMS.register(name, () -> new Item(new Item.Properties())); CONTENT.put(name, item); return item;
    }
    private static RegistryObject<Item> knife(String name, School school, boolean mob) {
        RegistryObject<Item> item = ITEMS.register(name, () -> mob ? new MobSacrificeKnife(school) : new SelfSacrificeKnife(school));
        CONTENT.put(name, item); return item;
    }
    private static RegistryObject<Block> rune(String name) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new BlockBloodRune(BloodRuneType.BLANK));
        CONTENT.put(name, ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()))); return block;
    }
    public static boolean available(School school, boolean mob) {
        if (!(mob ? ArteriesConfig.value(ArteriesConfig.SACRIFICE) : ArteriesConfig.value(ArteriesConfig.SELF))) return false;
        return switch (school) {
            case THAUMIC -> ArteriesConfig.value(ArteriesConfig.THAUMIC);
            case BOTANICAL -> ArteriesConfig.value(ArteriesConfig.BOTANICAL) && ModList.get().isLoaded("botania");
            case EVIL -> ArteriesConfig.value(ArteriesConfig.EVIL) && ModList.get().isLoaded("evilcraft");
            case CUSTOM -> mob ? ArteriesConfig.value(ArteriesConfig.CUSTOM_MOB) : ArteriesConfig.value(ArteriesConfig.CUSTOM_SELF);
        };
    }
    public static boolean researchEnabled(String key) {
        return switch (key) {
            case "DAGGER", "DAGGERH" -> available(School.THAUMIC, false) && ArteriesConfig.value(ArteriesConfig.HARD_CRAFTING) == key.endsWith("H");
            case "SACRIFICEDAGGER", "SACRIFICEDAGGERH" -> available(School.THAUMIC, true) && ArteriesConfig.value(ArteriesConfig.HARD_CRAFTING) == key.endsWith("H");
            case "SLATE", "RUNE" -> ArteriesConfig.value(ArteriesConfig.SLATES);
            default -> false;
        };
    }
    private static boolean visible(String name) {
        Item item = CONTENT.get(name).get();
        if (item instanceof SelfSacrificeKnife knife) return available(knife.school, false);
        if (item instanceof MobSacrificeKnife knife) return available(knife.school, true);
        return ArteriesConfig.value(ArteriesConfig.SLATES) && (!name.startsWith("mana_") || ModList.get().isLoaded("botania"));
    }
}
