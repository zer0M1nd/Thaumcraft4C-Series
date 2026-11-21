package advancedthaumaturgy;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import tc4revival.content.WandParts;
import thaumcraft.api.crafting.ArcaneResultTransforms;

@Mod(AdvancedThaumaturgy.ID)
public class AdvancedThaumaturgy {
    public static final String ID = "advancedthaumaturgy";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> MOB_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<SentryFireball>> SENTRY_FIREBALL = MOB_TYPES.register("sentry_fireball", () -> net.minecraft.world.entity.EntityType.Builder.<SentryFireball>of(SentryFireball::new,net.minecraft.world.entity.MobCategory.MISC).sized(.2f,.2f).clientTrackingRange(8).updateInterval(2).build(ID+":sentry_fireball"));
    public static final RegistryObject<Item> MERCURIAL_ROD = ITEMS.register("mercurial_rod", () -> new Item(new Item.Properties()) {
        @Override public boolean isFoil(ItemStack stack) { return true; }
    });
    public static final RegistryObject<MercurialWandItem> MERCURIAL_WAND = ITEMS.register("mercurial_wand", MercurialWandItem::new);
    public static final RegistryObject<Item> ENDSTONE_CHUNK = ITEMS.register("infused_endstone_chunk", () -> new Item(new Item.Properties()));
    public static final RegistryObject<VoidCageFocusItem> VOID_CAGE = ITEMS.register("focus_void_cage", VoidCageFocusItem::new);
    public static final Map<String, RegistryObject<CrystalItem>> CRYSTALS = new LinkedHashMap<>();
    static {
        crystal("inert", 0xffffff, CrystalItem.Upgrade.NONE);
        crystal("recharge", 0xff0000, CrystalItem.Upgrade.RECHARGE);
        crystal("compound", 0x00ffff, CrystalItem.Upgrade.COMPOUND);
        crystal("multiplier", 0x0000ff, CrystalItem.Upgrade.MULTIPLIER);
        crystal("stabilizer", 0xff00ff, CrystalItem.Upgrade.STABILIZER);
        crystal("discount", 0x00ffff, CrystalItem.Upgrade.DISCOUNT);
        crystal("potency", 0x00ff00, CrystalItem.Upgrade.POTENCY);
        crystal("flux", 0x800080, CrystalItem.Upgrade.NONE);
        crystal("watchful", 0x0000ff, CrystalItem.Upgrade.NONE);
        crystal("healing", 0xff2f34, CrystalItem.Upgrade.NONE);
        crystal("lightning", 0xffff7e, CrystalItem.Upgrade.NONE);
        crystal("fire", 0xff5a01, CrystalItem.Upgrade.NONE);
        crystal("frost", 0xe1ffff, CrystalItem.Upgrade.NONE);
    }
    private static void crystal(String name, int color, CrystalItem.Upgrade upgrade) { CRYSTALS.put(name, ITEMS.register("crystal_" + name, () -> new CrystalItem(color, upgrade))); }
    public static final RegistryObject<NodeModifierBlock> NODE_MODIFIER = BLOCKS.register("node_modifier", NodeModifierBlock::new);
    public static final RegistryObject<WandbenchBlock> WANDBENCH = BLOCKS.register("wandbench", WandbenchBlock::new);
    public static final RegistryObject<EtherealJarBlock> ETHEREAL_JAR = BLOCKS.register("ethereal_jar", EtherealJarBlock::new);
    public static final RegistryObject<CreativeNodeBlock> CREATIVE_NODE = BLOCKS.register("creative_node", CreativeNodeBlock::new);
    public static final RegistryObject<AltarDeployerBlock> ALTAR_DEPLOYER = BLOCKS.register("altar_deployer", AltarDeployerBlock::new);
    public static final RegistryObject<CrystalHolderBlock> CRYSTAL_HOLDER = BLOCKS.register("crystal_holder", CrystalHolderBlock::new);
    public static final RegistryObject<Block> MICROLITH_MULTIBLOCK = BLOCKS.register("microlith_multiblock", () -> new Block(Block.Properties.copy(net.minecraft.world.level.block.Blocks.OBSIDIAN)));
    public static final RegistryObject<Block> MICROLITH_MODEL_MULTIBLOCK = BLOCKS.register("microlith_model_multiblock", () -> new Block(Block.Properties.copy(net.minecraft.world.level.block.Blocks.OBSIDIAN)));
    public static final Map<MachineBlock.Kind, RegistryObject<MachineBlock>> MACHINES = new EnumMap<>(MachineBlock.Kind.class);
    static {
        for (var kind : MachineBlock.Kind.values()) MACHINES.put(kind, BLOCKS.register(kind.id, () -> new MachineBlock(kind)));
        ITEMS.register("ethereal_jar", () -> new EtherealJarBlock.JarItem(ETHEREAL_JAR.get()));
        for (var block : List.of(NODE_MODIFIER, WANDBENCH, CREATIVE_NODE, ALTAR_DEPLOYER)) ITEMS.register(block.getId().getPath(), () -> new RenderedBlockItem(block.get()));
        MACHINES.forEach((kind, block) -> ITEMS.register(kind.id, () -> new RenderedBlockItem(block.get())));
        for(var block:List.of(CRYSTAL_HOLDER,MICROLITH_MULTIBLOCK,MICROLITH_MODEL_MULTIBLOCK))ITEMS.register(block.getId().getPath(),()->new BlockItem(block.get(),new Item.Properties()));
    }
    public static final RegistryObject<BlockEntityType<MachineEntity>> MACHINE_ENTITY = ENTITIES.register("machine", () -> BlockEntityType.Builder.of(MachineEntity::new, MACHINES.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static final RegistryObject<BlockEntityType<NodeModifierEntity>> NODE_ENTITY = ENTITIES.register("node_modifier", () -> BlockEntityType.Builder.of(NodeModifierEntity::new, NODE_MODIFIER.get()).build(null));
    public static final RegistryObject<BlockEntityType<WandbenchEntity>> BENCH_ENTITY = ENTITIES.register("wandbench", () -> BlockEntityType.Builder.of(WandbenchEntity::new, WANDBENCH.get()).build(null));
    public static final RegistryObject<BlockEntityType<EtherealJarEntity>> JAR_ENTITY = ENTITIES.register("ethereal_jar", () -> BlockEntityType.Builder.of(EtherealJarEntity::new, ETHEREAL_JAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<CreativeNodeEntity>> CREATIVE_ENTITY = ENTITIES.register("creative_node", () -> BlockEntityType.Builder.of(CreativeNodeEntity::new, CREATIVE_NODE.get()).build(null));
    public static final RegistryObject<MenuType<NodeModifierMenu>> NODE_MENU = MENUS.register("node_modifier", () -> IForgeMenuType.create(NodeModifierMenu::new));
    public static final RegistryObject<MenuType<WandbenchMenu>> BENCH_MENU = MENUS.register("wandbench", () -> IForgeMenuType.create(WandbenchMenu::new));
    public static final RegistryObject<RecipeSerializer<WandUpgradeRecipe>> WAND_UPGRADE = RECIPES.register("wand_upgrade", WandUpgradeRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<MercurialAssemblyRecipe>> WAND_ASSEMBLY = RECIPES.register("mercurial_assembly", MercurialAssemblyRecipe.Serializer::new);
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.advancedthaumaturgy"))
        .icon(() -> new ItemStack(MERCURIAL_ROD.get())).displayItems((parameters, output) -> {
            ITEMS.getEntries().forEach(entry -> { if (entry.get() != MERCURIAL_WAND.get() && ATConfig.showItem(entry.getId().getPath())) output.accept(entry.get()); });
            if(ATConfig.enabled("mercurial_wand")){
                output.accept(MERCURIAL_WAND.get().create(WandParts.Rod.require(MercurialWandItem.ROD), WandParts.Cap.THAUMIUM, false, false));
                output.accept(MERCURIAL_WAND.get().create(WandParts.Rod.require(MercurialWandItem.ROD), WandParts.Cap.THAUMIUM, false, true));
            }
        }).build());
    public AdvancedThaumaturgy() {
        ATConfig.register();
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus); BLOCKS.register(bus); ENTITIES.register(bus); MENUS.register(bus); RECIPES.register(bus); TABS.register(bus); MOB_TYPES.register(bus);
        bus.addListener(this::setup);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(StabilizerTracker.class);
        ArcaneResultTransforms.register(id("mercurial_wand"), (inputs, w, h, registries, base, config) -> {
            if (!ATConfig.enabled("mercurial_wand") || !WandParts.rod(base.getTag()).tag.equals(MercurialWandItem.ROD)) return ItemStack.EMPTY;
            var result = new ItemStack(MERCURIAL_WAND.get()); result.setTag(base.getTag().copy()); return result;
        });
    }
    private void setup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) { event.enqueueWork(() -> {
        if(net.minecraftforge.fml.ModList.get().isLoaded("computercraft")) advancedthaumaturgy.compat.ComputerCraftCompat.register();
        if(net.minecraftforge.fml.ModList.get().isLoaded("opencomputers")) advancedthaumaturgy.compat.OpenComputersCompat.register();
        ATConfig.FEATURES.keySet().forEach(feature->thaumcraft.api.config.FeatureRecipes.register(id("feature_"+feature),recipe->recipe.getNamespace().equals(ID)&&ATConfig.featureFor(recipe.getPath()).equals(feature),()->ATConfig.enabled(feature)));
        Map<String,String> features=new LinkedHashMap<>();
        features.put("MERCURIALROD","mercurial_core");features.put("MERCURIALWAND","mercurial_wand");features.put("NODEMODIFIER","node_modifier");features.put("FERTILIZER","fertilizer");features.put("ETHEREALJAR","ethereal_jar");features.put("ESSENTIAENGINE","engine");features.put("VOIDCAGE","focus_void_cage");features.put("WANDBENCH","wandbench");
        for(String key:List.of("ARCANECRYSTAL","UPGRADERECHARGE","UPGRADECOMPOUND","UPGRADEDRAIN","UPGRADESTABILIZER","UPGRADEDISCOUNT","UPGRADEPOTENCY"))features.put(key,"wand_upgrades");
        for(String key:List.of("MINILITHBASE","MINILITHFLUX","MINILITHWATCHFUL","MINILITHHEAL","MINILITHZAP","MINILITHFIRE","MINILITHFROST"))features.put(key,"microlith");
        features.forEach((key,feature)->thaumcraft.api.research.ResearchCategories.registerEnablement(ID+":"+key,()->ATConfig.enabled(feature)));
        thaumcraft.api.aspects.ItemAspectProviders.register(ID+":legacy_ingredients",20,query->{
            var stack=query.stack();String aspect=stack.is(Items.EGG)&&ATConfig.enabled("exchange_in_eggs")?"permutatio":stack.is(Items.BONE)&&ATConfig.enabled("undead_in_bones")?"exanimis":null;
            if(aspect==null)return thaumcraft.api.aspects.ItemAspectProviders.Resolution.pass();
            var value=tc4revival.aspect.item.ItemStackAspects.resolve(stack,query.access(),query.scanning()).orElseGet(tc4revival.aspect.AspectAmounts::new);
            var a=tc4revival.aspect.TCAspects.catalog().require(aspect);if(value.amount(a)==0)value.add(a,1);
            return thaumcraft.api.aspects.ItemAspectProviders.Resolution.handled(java.util.Optional.of(value));
        });
        thaumcraft.api.wands.WandAttributes.registerCapacity(ID, MercurialWandItem.ROD, () -> Math.min(1000, Math.max(500, Arrays.stream(WandParts.Rod.values()).filter(r -> !r.tag.equals(MercurialWandItem.ROD)).mapToInt(r -> r.capacity).max().orElse(500))));
        net.minecraftforge.common.world.ForgeChunkManager.setForcedChunkLoadingCallback(ID, (level, tickets) -> {
            tickets.getBlockTickets().forEach((pos, helper) -> {
                if (!ATConfig.enabled("microlith") || !(level.getBlockEntity(pos) instanceof MachineEntity machine) || machine.kind() != MachineBlock.Kind.WATCHFUL || !machine.active()) tickets.removeAllTickets(pos);
            });
        });
    }); }
    public static ResourceLocation id(String path) { return new ResourceLocation(ID, path); }
}
