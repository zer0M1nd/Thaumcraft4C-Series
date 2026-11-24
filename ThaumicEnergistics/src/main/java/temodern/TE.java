package temodern;

import java.util.LinkedHashMap;
import java.util.Map;
import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.storage.StorageCells;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import temodern.essentia.*;
import temodern.storage.*;
import temodern.machine.*;

@Mod(TE.ID)
public final class TE {
    public static final String ID = "thaumicenergistics";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final RegistryObject<Item> AMPOULE = ITEMS.register("essentia_ampoule", EssentiaAmpoule::new);
    public static final Map<String, RegistryObject<Item>> CELLS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> MATERIALS = new LinkedHashMap<>();
    static {
        for (var tier : CellTier.ALL) CELLS.put(tier.id(), ITEMS.register("essentia_cell_" + tier.id(), () -> new EssentiaCell(tier)));
        for (String name : new String[]{"storage_casing", "diffusion_core", "coalescence_core", "iron_gear"})
            MATERIALS.put(name, ITEMS.register(name, () -> new Item(new Item.Properties())));
        MATERIALS.put("encoded_arcane_pattern", ITEMS.register("encoded_arcane_pattern", temodern.item.ArcanePatternItem::new));
        MATERIALS.put("wireless_arcane_terminal",ITEMS.register("wireless_arcane_terminal",() -> net.minecraftforge.fml.ModList.get().isLoaded("ae2wtlib")
                ? temodern.compat.WTLibTerminal.create() : new temodern.item.WirelessArcaneTerminalItem()));
        MATERIALS.put("vis_charging_card",ITEMS.register("vis_charging_card",()->appeng.api.upgrades.Upgrades.createUpgradeCardItem(new Item.Properties())));
        MATERIALS.put("knowledge_core", ITEMS.register("knowledge_core", temodern.item.KnowledgeCore::new));
        MATERIALS.put("cell_microscope", ITEMS.register("cell_microscope", temodern.item.CellMicroscope::new));
        MATERIALS.put("wrench_focus", ITEMS.register("wrench_focus", temodern.item.WrenchFocus::new));
        MATERIALS.put("golem_wireless_backpack", ITEMS.register("golem_wireless_backpack", temodern.golem.WirelessBackpack::new));
        for (var tier : CellTier.ALL) if (!tier.creative() && !tier.id().equals("quantum") && !tier.id().equals("singularity")) {
            String name = "storage_component_" + tier.id();
            MATERIALS.put(name, ITEMS.register(name, () -> new Item(new Item.Properties())));
        }
        TABS.register("main", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.thaumicenergistics"))
                .icon(() -> new ItemStack(CELLS.get("1k").get()))
                .displayItems((parameters, output) -> ITEMS.getEntries().stream().filter(entry->RecipeConfig.itemEnabled(entry.getId().getPath())).forEach(entry -> output.accept(entry.get()))).build());
    }
    public TE() {
        RevivalFeatures.register();
        temodern.crafting.GhostRecipePacket.register();
        temodern.crafting.TerminalRecipePacket.register();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                new thaumcraft.api.config.MigratingConfigSpec(RecipeConfig.SPEC).fromCommon("thaumicenergistics-common.toml"), "thaumicenergistics-server.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,ClientConfig.SPEC);
        net.minecraftforge.common.crafting.CraftingHelper.register(RecipeConfig.SERIALIZER);
        appeng.api.parts.PartModels.registerModels(temodern.crafting.ArcaneTerminalPart.BASE,temodern.crafting.ArcaneTerminalPart.FACE,temodern.crafting.ArcaneTerminalPart.FACE_OFF);
        appeng.api.parts.PartModels.registerModels(temodern.machine.VisInterfacePart.MODELS);
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        Machines.register(bus); ITEMS.register(bus); TABS.register(bus);
        temodern.crafting.WorkMenu.MENUS.register(bus);
        bus.addListener(this::registerKeys);
        if (net.minecraftforge.fml.ModList.get().isLoaded("ae2wtlib"))
            bus.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, this::registerWirelessModule);
        bus.addListener(this::setup);
    }
    private void registerKeys(RegisterEvent event) {
        if (event.getRegistryKey().location().equals(new ResourceLocation("ae2", "keytypes"))) AEKeyTypes.register(EssentiaKeyType.INSTANCE);
    }
    private void registerWirelessModule(RegisterEvent event) {
        var registry = event.getRegistryKey();
        if ((registry.equals(net.minecraft.core.registries.Registries.ITEM)
                || registry.equals(net.minecraft.core.registries.Registries.MENU))
                && MATERIALS.get("wireless_arcane_terminal").isPresent()
                && temodern.crafting.WorkMenu.TERMINAL.isPresent())
            temodern.compat.WTLibTerminal.register();
    }
    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var wireless=MATERIALS.get("wireless_arcane_terminal").get();
            appeng.api.features.GridLinkables.register(wireless,appeng.items.tools.powered.WirelessTerminalItem.LINKABLE_HANDLER);
            appeng.api.upgrades.Upgrades.add(MATERIALS.get("vis_charging_card").get(),wireless,1);
            appeng.api.upgrades.Upgrades.add(appeng.core.definitions.AEItems.ENERGY_CARD,wireless,2);
            if (net.minecraftforge.fml.ModList.get().isLoaded("ae2wtlib")) temodern.compat.WTLibTerminal.registerUpgrade();
            var quantum=temodern.crafting.WirelessQuantumLink.card();
            if(quantum!=null)appeng.api.upgrades.Upgrades.add(quantum,wireless,1);
            appeng.api.upgrades.Upgrades.add(appeng.core.definitions.AEItems.SPEED_CARD,Machines.ALL.get("arcane_assembler").get(),5);
            appeng.menu.MenuOpener.addOpener(temodern.crafting.WorkMenu.TERMINAL.get(),(player,locator,returning)->{
                var host=locator.locate(player,temodern.crafting.WorkHost.class);
                if(host==null||!(player instanceof net.minecraft.server.level.ServerPlayer server))return false;
                if (host instanceof temodern.crafting.PortableArcaneHost && !RevivalFeatures.wireless()) return false;
                temodern.crafting.ArcaneTerminalMenu.open(server,host,locator);return true;
            });
            EssentiaBus.register();
            thaumcraft.api.golems.GolemEssentiaProviders.register(new ResourceLocation(ID,"me_interface"),temodern.golem.InterfaceEssentia::find);
            ContainerItemStrategy.register(EssentiaKeyType.INSTANCE, EssentiaKey.class, new ContainerStrategy());
            StorageCells.addCellHandler(new EssentiaCell.Handler());
            appeng.api.crafting.PatternDetailsHelper.registerDecoder(new temodern.crafting.ArcanePattern.Decoder());
            temodern.golem.WirelessBackpack.register();
            // A network provider must never be re-exported into that same network via a storage bus.
            thaumcraft.api.aspects.EssentiaStorageApi.register(new ResourceLocation(ID, "network_machines"),
                    (entity, side) -> entity instanceof NetworkMachine machine && !machine.kind().equals("essentia_vibration_chamber")
                            ? thaumcraft.api.aspects.EssentiaStorageApi.empty() : null);
        });
    }
}
