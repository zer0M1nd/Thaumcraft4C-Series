package arcane_engineering;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import tc4revival.content.WandParts;

@Mod(ArcaneEngineering.ID)
public class ArcaneEngineering {
    public static final String ID = "arcane_engineering";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final RegistryObject<UpgradeableWandItem> WAND = ITEMS.register("wand_upgradeable", UpgradeableWandItem::new);
    public static final RegistryObject<Item> STEEL_CAP = ITEMS.register("wandcap_steel", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ELECTRUM_CAP = ITEMS.register("wandcap_electrum", () -> new Item(new Item.Properties()));
    public static final RegistryObject<WandUpgradeItem> CAPACITOR = ITEMS.register("wandupgrade_capacitor", () -> new WandUpgradeItem("capacitors"));
    public static final RegistryObject<WandUpgradeItem> CHARGER = ITEMS.register("wandupgrade_charger", () -> new WandUpgradeItem("chargers"));
    public static final RegistryObject<WandUpgradeItem> ENERGIZER = ITEMS.register("wandupgrade_energizer", () -> new WandUpgradeItem("energizers"));
    public static final RegistryObject<ArcaneDrillHeadItem> THAUMIUM_DRILL = ITEMS.register("drillhead_thaumium", () -> new ArcaneDrillHeadItem(false));
    public static final RegistryObject<ArcaneDrillHeadItem> VOID_DRILL = ITEMS.register("drillhead_void", () -> new ArcaneDrillHeadItem(true));
    public static final RegistryObject<ImpulseBootsItem> BOOTS = ITEMS.register("impulse_boots", ImpulseBootsItem::new);
    public static final RegistryObject<DestabilizerBlock> DESTABILIZER = BLOCKS.register("destabilizer", DestabilizerBlock::new);
    public static final RegistryObject<Item> DESTABILIZER_ITEM = ITEMS.register("destabilizer", () -> new DestabilizerBlock.Item(DESTABILIZER.get()));
    public static final RegistryObject<BlockEntityType<DestabilizerBlockEntity>> DESTABILIZER_TILE = TILES.register("destabilizer", () -> BlockEntityType.Builder.of(DestabilizerBlockEntity::new, DESTABILIZER.get()).build(null));
    public static final RegistryObject<UpgradeableWandRecipe.Serializer> WAND_RECIPE = RECIPES.register("upgradeable_wand", UpgradeableWandRecipe.Serializer::new);
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.arcane_engineering")).icon(() -> new ItemStack(DESTABILIZER_ITEM.get()))
        .displayItems((params, output) -> {
            for (var item : ITEMS.getEntries()) if (item.get() != WAND.get()) output.accept(item.get());
            for (String cap : new String[]{"iron", ID+":electrum", ID+":steel"})
                output.accept(WAND.get().create(WandParts.Rod.require(ID+":upgradeable"), WandParts.Cap.require(cap), false, true));
        }).build());

    public ArcaneEngineering() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus); BLOCKS.register(bus); TILES.register(bus); RECIPES.register(bus); TABS.register(bus);
        RevivalFeatures.register(); BootsNetwork.register();
        bus.addListener(this::creativeVariants);
    }
    private void creativeVariants(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event) {
        if(event.getTab()==tc4revival.content.TCCreativeTabs.THAUMCRAFT.get()) {
            var rod=WandParts.Rod.require(ID+":upgradeable");
            event.getEntries().remove(tc4revival.content.TCItems.CASTING_WAND.get().create(rod,WandParts.Cap.IRON,false,true));
            event.accept(WAND.get().create(rod,WandParts.Cap.IRON,false,true));
        }
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
}
