package tc4revival.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tc4revival.TC4Revival;
import tc4revival.config.TCConfig;

/** Curated variants first, followed by registered items omitted from the display list. */
public  class TCCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TC4Revival.MOD_ID);
    public static final RegistryObject<CreativeModeTab> THAUMCRAFT = TABS.register("thaumcraft", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thaumcraft"))
            .icon(() -> TCItems.CASTING_WAND.get().getDefaultInstance())
            .displayItems((parameters, target) -> {
                var shown=new java.util.HashSet<net.minecraft.world.item.Item>();
                CreativeModeTab.Output output=new CreativeModeTab.Output() {
                    @Override public void accept(net.minecraft.world.item.ItemStack stack,CreativeModeTab.TabVisibility visibility) {
                        if (!stack.isEmpty() && SilverCaps.visible(stack.getItem())) { shown.add(stack.getItem()); target.accept(stack,visibility); }
                    }
                };
                var wand = TCItems.CASTING_WAND.get();
                output.accept(wand.create(WandParts.Rod.WOOD, WandParts.Cap.IRON, false, true));
                output.accept(wand.create(WandParts.Rod.GREATWOOD, WandParts.Cap.GOLD, false, true));
                output.accept(wand.create(WandParts.Rod.SILVERWOOD, WandParts.Cap.THAUMIUM, false, true));
                for(var rod:WandParts.Rod.values())if(rod.tag.contains(":"))output.accept(wand.create(rod,WandParts.Cap.IRON,false,true));
                for(var cap:WandParts.Cap.values())if(cap.tag.contains(":"))output.accept(wand.create(WandParts.Rod.WOOD,cap,false,true));
                var sceptre = wand.create(WandParts.Rod.SILVERWOOD, WandParts.Cap.THAUMIUM, true, true);
                sceptre.getOrCreateTag().putInt("tc4_metadata", 128); // The old creative sceptre used a distinct damage value.
                output.accept(sceptre);
                for (var component : WandComponent.all()) output.accept(TCItems.wandComponent(component.path()));
                output.accept(TCItems.THAUMONOMICON.get());
                if (TCConfig.SERVER_SPEC.isLoaded() && TCConfig.SERVER.allowCheatSheet.get())
                    output.accept(TCItems.CHEAT_THAUMONOMICON.get());
                output.accept(TCItems.THAUMOMETER.get());
                output.accept(TCItems.SCRIBING_TOOLS.get());
                output.accept(TCItems.TABLE.get());
                output.accept(TCItems.ARCANE_WORKBENCH.get());
                output.accept(TCItems.FOCAL_MANIPULATOR.get());
                output.accept(TCItems.VIS_RELAY.get()); output.accept(TCItems.WORKBENCH_CHARGER.get());
                output.accept(TCItems.WAND_PEDESTAL.get()); output.accept(TCItems.WAND_PEDESTAL_FOCUS.get());
                output.accept(TCItems.NODE_STABILIZER.get()); output.accept(TCItems.ADVANCED_NODE_STABILIZER.get()); output.accept(TCItems.NODE_CONVERTER.get());
                output.accept(TCItems.AURA_NODE.get());
                output.accept(TCItems.ARCANE_STONE.get()); output.accept(TCItems.ARCANE_STONE_BRICKS.get());
                output.accept(TCItems.ARCANE_STONE_SLAB.get()); output.accept(TCItems.ARCANE_STONE_STAIRS.get());
                for (var type : ShardType.values()) output.accept(TCItems.shard(type));
                for (var type : MaterialType.values()) if (type.creativeVisible()) output.accept(TCItems.material(type));
                output.accept(TCItems.KNOWLEDGE_FRAGMENT.get());
                output.accept(TCItems.PRIMAL_CHARM.get());
                output.accept(TCItems.PRIMORDIAL_PEARL.get());
                output.accept(TCItems.MANA_BEAN.get());
                for (var kind:LootBagItem.Kind.values()) output.accept(TCItems.lootBag(kind));
                output.accept(TCItems.TAINTED_GOO.get()); output.accept(TCItems.TAINTED_TENDRIL.get());
                output.accept(TCItems.NITOR.get());
                output.accept(TCItems.THAUMIUM_CHEST.get()); output.accept(TCItems.VOID_CHEST.get());
                output.accept(TCItems.CRUCIBLE.get());
                output.accept(TCItems.BELLOWS.get());
                output.accept(TCItems.ARCANE_PEDESTAL.get());
                output.accept(TCItems.INFUSION_MATRIX.get());
                output.accept(TCItems.ALUMENTUM.get());
                output.accept(TCItems.ALCHEMY_FURNACE.get()); output.accept(TCItems.ALEMBIC.get());
                output.accept(TCItems.ESSENTIA_RESERVOIR.get());
                output.accept(TCItems.ESSENTIA_MIRROR.get());
                output.accept(TCItems.WARDED_GLASS.get());
                output.accept(TCItems.ARCANE_PRESSURE_PLATE.get());
                output.accept(TCItems.ALCHEMICAL_CONSTRUCT.get()); output.accept(TCItems.ADVANCED_ALCHEMICAL_CONSTRUCT.get()); output.accept(TCItems.MNEMONIC_MATRIX.get()); output.accept(TCItems.CHAOS_MATRIX.get());
                TCArcane.essentiaTubes().forEach(output::accept);
                output.accept(TCItems.ESSENTIA_RESONATOR.get());
                output.accept(TCItems.ESSENTIA_JAR.get()); output.accept(TCItems.VOID_JAR.get()); output.accept(TCItems.JAR_LABEL.get()); output.accept(TCItems.EMPTY_PHIAL.get());
                for (var aspect:tc4revival.aspect.TCAspects.catalog().all()) output.accept(EssentiaPhialItem.filled(aspect));
                output.accept(TCItems.TAINT_FIBRES.get()); output.accept(TCItems.TAINT_CRUST.get());
                output.accept(TCItems.TAINT_GRASS.get()); output.accept(TCItems.TAINT_PLANT.get()); output.accept(TCItems.TAINT_SPORE_STALK.get());
                output.accept(TCItems.TAINT_SOIL.get()); output.accept(TCItems.FLESH_BLOCK.get());
                output.accept(TCItems.LESSER_VIS_AMULET.get()); output.accept(TCItems.VIS_AMULET.get());
                for (var aspect : tc4revival.aspect.TCAspects.catalog().all()) output.accept(TCItems.WISP_ESSENCE.get().of(aspect));
                output.accept(TCItems.FOCUS_POUCH.get());
                output.accept(TCItems.SHOCK_FOCUS.get());
                output.accept(TCItems.FIRE_FOCUS.get());
                output.accept(TCItems.FROST_FOCUS.get());
                output.accept(TCItems.EXCAVATION_FOCUS.get());
                output.accept(TCItems.PECH_FOCUS.get());
                output.accept(TCItems.PORTABLE_HOLE_FOCUS.get());
                output.accept(TCItems.TRADE_FOCUS.get());
                output.accept(TCItems.WARDING_FOCUS.get());
                output.accept(TCItems.HELLBAT_FOCUS.get());
                output.accept(TCItems.PRIMAL_FOCUS.get());
                output.accept(TCItems.GOGGLES.get());
                output.accept(TCItems.ROBE_CHEST.get()); output.accept(TCItems.ROBE_LEGS.get()); output.accept(TCItems.ROBE_BOOTS.get());
                output.accept(TCItems.ZOMBIE_BRAIN.get());
                // Preserve ItemNugget's interleaved order while reusing Minecraft's iron nugget.
                output.accept(net.minecraft.world.item.Items.IRON_NUGGET);
                output.accept(TCItems.nugget(NuggetType.QUICKSILVER));
                output.accept(TCItems.cluster(ClusterType.CINNABAR));
                output.accept(TCItems.nugget(NuggetType.THAUMIUM));
                output.accept(TCItems.nugget(NuggetType.VOID));
                output.accept(TCItems.cluster(ClusterType.IRON));
                output.accept(TCItems.cluster(ClusterType.GOLD));
                output.accept(TCItems.nugget(NuggetType.COPPER));
                output.accept(TCItems.cluster(ClusterType.COPPER));
                for (var type : FoodType.values()) output.accept(TCItems.food(type));
                for (var type : MundaneBaubleType.values()) output.accept(TCItems.bauble(type));
                for (var type:RunicBaubleType.values()) output.accept(TCItems.runicBauble(type));
                for (var type:ShardType.values()) output.accept(TCItems.crystal(type));
                output.accept(TCItems.GREATWOOD_LOG.get()); output.accept(TCItems.SILVERWOOD_LOG.get());
                output.accept(TCItems.GREATWOOD_LEAVES.get()); output.accept(TCItems.SILVERWOOD_LEAVES.get());
                output.accept(TCItems.GREATWOOD_SAPLING.get()); output.accept(TCItems.SILVERWOOD_SAPLING.get());
                for (String name:TCArcane.woodworkNames()) output.accept(TCItems.woodwork(name));
                for (var kind:MagicalPlantBlock.Kind.values()) output.accept(TCItems.plant(kind));
                for (var type : ThaumiumToolType.values()) output.accept(TCItems.tool(type));
                for (var type : ThaumiumArmorType.values()) output.accept(TCItems.armor(type));
                for (var type : VoidEquipment.Tool.values()) output.accept(TCItems.voidTool(type));
                for (var type : VoidEquipment.Armor.values()) output.accept(TCItems.voidArmor(type));
                output.accept(CrimsonBanner.item());
                // Keep test access complete when a feature registers an item but omits its tab entry.
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getEntries().stream()
                        .filter(entry->entry.getKey().location().getNamespace().equals(TC4Revival.MOD_ID))
                        .sorted(java.util.Comparator.comparing(entry->entry.getKey().location().toString()))
                        .map(java.util.Map.Entry::getValue).filter(item->!(item instanceof DeepslateOreItem)||DeepslateOreBlock.enabled()).filter(item->!shown.contains(item)).forEach(output::accept);
            }).build());
    private TCCreativeTabs() {}
    public static void register(IEventBus bus) { TABS.register(bus); }
}
