package heartwork;

import com.google.gson.*;
import heartwork.content.*;
import heartwork.recipe.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

@Mod(Heartwork.ID)
public class Heartwork {
    public static final String ID = "heartwork";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<StrawEntity>> STRAW = ENTITIES.register("slash_straw", () -> net.minecraft.world.entity.EntityType.Builder.of(StrawEntity::new, net.minecraft.world.entity.MobCategory.MISC)
            .sized(0.6F, 1.8F).clientTrackingRange(8).build("heartwork:slash_straw"));
    public static final Map<String, RegistryObject<Item>> ITEM_MAP = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Block>> BLOCK_MAP = new LinkedHashMap<>();
    public static final RegistryObject<RecipeSerializer<SwissRepairRecipe>> SWISS_REPAIR = RECIPES.register("swiss_repair", () -> new SimpleCraftingRecipeSerializer<>(SwissRepairRecipe::new));
    public static final RegistryObject<RecipeSerializer<SmeltingRecipe>> SMELTING = RECIPES.register("smelting", CountedSmeltingSerializer::new);
    public static final JsonObject CONTENT = loadContent();

    public Heartwork() {
        thaumcraft.api.resources.ExternalResourceConverters.register(new ResourceLocation(ID,"achievement_text"),HeartworkLanguages::convert);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        HeartworkVillage.register(bus);
        for (JsonElement entry : CONTENT.getAsJsonArray("blocks")) {
            JsonObject data = entry.getAsJsonObject();
            String id = data.get("id").getAsString();
            BLOCK_MAP.put(id, BLOCKS.register(id, () -> switch(data.get("kind").getAsString()) {
                case "crop" -> new HeartworkCrop(data.get("seed").getAsString(), data.get("produce").getAsString());
                case "cake" -> new HeartworkCake(id);
                default -> throw new IllegalArgumentException("Unknown Heartwork block: " + id);
            }));
        }
        for (JsonElement entry : CONTENT.getAsJsonArray("items")) {
            JsonObject data = entry.getAsJsonObject();
            String id = data.get("id").getAsString();
            ITEM_MAP.put(id, ITEMS.register(id, () -> ContentItems.create(data)));
        }
        TABS.register("smith", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.heartwork.smith"))
                .icon(() -> new ItemStack(item("hammer"))).displayItems((parameters, output) -> fillTab(output, false)).build());
        TABS.register("food", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.heartwork.food"))
                .icon(() -> new ItemStack(item("chocolate"))).displayItems((parameters, output) -> fillTab(output, true)).build());
        BLOCKS.register(bus); ITEMS.register(bus); TABS.register(bus); RECIPES.register(bus); ENTITIES.register(bus);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event) -> event.put(STRAW.get(), StrawEntity.attributes().build()));
        HeartworkLoot.REGISTER.register(bus);
        if (net.minecraftforge.fml.ModList.get().isLoaded("slashblade")) heartwork.compat.blade.BladeCompat.register(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
                thaumcraft.api.research.ResearchCategories.registerEnablementFilter(new ResourceLocation(ID,"slashblade"),
                        key -> !key.startsWith("heartwork:") || net.minecraftforge.fml.ModList.get().isLoaded("slashblade"))));
    }

    private static JsonObject loadContent() {
        try (InputStream input = Objects.requireNonNull(Heartwork.class.getResourceAsStream("/data/heartwork/content.json"));
             Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) { throw new UncheckedIOException(exception); }
    }

    private static void fillTab(CreativeModeTab.Output output, boolean food) {
        output.accept(food?HeartworkVillage.HEARTH.get():HeartworkVillage.ANVIL.get());
        if(!food&&net.minecraftforge.fml.ModList.get().isLoaded("slashblade")&&net.minecraftforge.fml.ModList.get().isLoaded("exparmory"))output.accept(item("wrapped_blade"));
        for (JsonElement entry : CONTENT.getAsJsonArray("items")) {
            JsonObject data = entry.getAsJsonObject();
            String id=data.get("id").getAsString(), kind=data.get("kind").getAsString();
            boolean edible = Set.of("food", "seed", "cake").contains(kind)
                    || id.matches("(cup.*|cocoa.*|coffee.*|mashed_tea.*|tea_leaf|flour|dough.*|iron_plate)");
            if (food == edible) output.accept(item(id));
        }
    }

    public static Item item(String name) {
        ResourceLocation id = name.contains(":") ? new ResourceLocation(name) : new ResourceLocation(ID, name);
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || item == Items.AIR) throw new IllegalArgumentException("Missing Heartwork ingredient " + id);
        return item;
    }
}
