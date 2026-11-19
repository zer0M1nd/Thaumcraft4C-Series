package fmmodern;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;

public final class Content {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ForbiddenMagic.ID);
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,ForbiddenMagic.ID);
    public static final RegistryObject<net.minecraft.world.effect.MobEffect> BLOOD_SEAL=EFFECTS.register("blood_seal",()->new net.minecraft.world.effect.MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL,0xac1919){@Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientMobEffectExtensions> consumer){consumer.accept(new fmmodern.client.BloodSealIcon());}});
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ForbiddenMagic.ID);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ForbiddenMagic.ID);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> MOBS = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ForbiddenMagic.ID);
    public static final DeferredRegister<net.minecraft.world.entity.npc.VillagerProfession> PROFESSIONS = DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, ForbiddenMagic.ID);
    public static final DeferredRegister<net.minecraft.world.entity.ai.village.poi.PoiType> POIS=DeferredRegister.create(ForgeRegistries.POI_TYPES,ForbiddenMagic.ID);
    public static final RegistryObject<net.minecraft.world.entity.ai.village.poi.PoiType> HERETIC_POI=POIS.register("heretic",()->new net.minecraft.world.entity.ai.village.poi.PoiType(com.google.common.collect.ImmutableSet.copyOf(Content.CAGE.get().getStateDefinition().getPossibleStates()),1,1));
    public static final RegistryObject<net.minecraft.world.entity.npc.VillagerProfession> HERETIC_PROFESSION = PROFESSIONS.register("heretic",()->new net.minecraft.world.entity.npc.VillagerProfession("heretic",p->p.is(net.minecraft.resources.ResourceLocation.parse("forbidden_magic:heretic")),p->p.is(net.minecraft.resources.ResourceLocation.parse("forbidden_magic:heretic")),com.google.common.collect.ImmutableSet.of(),com.google.common.collect.ImmutableSet.of(),net.minecraft.sounds.SoundEvents.VILLAGER_WORK_CLERIC));
    public static final RegistryObject<net.minecraft.world.entity.EntityType<Heretic>> HERETIC = MOBS.register("heretic",()->net.minecraft.world.entity.EntityType.Builder.<Heretic>of(Heretic::new,net.minecraft.world.entity.MobCategory.MISC).sized(.6f,1.95f).clientTrackingRange(10).build("forbidden_magic:heretic"));
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, ForbiddenMagic.ID);
    public static final Map<String, RegistryObject<Item>> ALL = new LinkedHashMap<>();
    public static final String[] SINS = {"wrath", "envy", "taint", "pride", "lust", "sloth", "greed"};
    static { for (String sin : SINS) item(sin + "_shard", () -> new Item(new Item.Properties())); }
    public static final RegistryObject<Item> GLUTTONY = item("gluttony_shard", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationMod(.1f).build())));
    public static final RegistryObject<Item> EMERALD_NUGGET = item("emerald_nugget", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DYE = item("black_dye", () -> new DyeItem(DyeColor.BLACK, new Item.Properties()));
    public static final RegistryObject<Item> TAINT_COAL = item("taint_coal", () -> new Item(new Item.Properties()) {
        @Override public int getBurnTime(ItemStack stack,net.minecraft.world.item.crafting.RecipeType<?> type){return 1+java.util.concurrent.ThreadLocalRandom.current().nextInt(2400);}
    });
    public static final RegistryObject<Item> TAINT_FRUIT = item("taint_fruit", SpecialItems.TaintFruit::new);
    public static final RegistryObject<Item> MOB_CRYSTAL = item("mob_crystal", SpecialItems.MobCrystal::new);
    public static final RegistryObject<Item> FORK = item("diabolist_fork", () -> new SpecialItems.RitualWeapon(true));
    public static final RegistryObject<Item> SKULL_AXE = item("skull_axe", () -> new SpecialItems.RitualWeapon(false));
    public static final RegistryObject<Item> CROP = item("riding_crop", SpecialItems.RidingCrop::new);
    public static final RegistryObject<Item> MORPH_PICK = item("morph_pickaxe", MorphTools.Pick::new);
    public static final RegistryObject<Item> MORPH_SPADE = item("morph_shovel", MorphTools.Spade::new);
    public static final RegistryObject<Item> MORPH_AXE = item("morph_axe", MorphTools.Axe::new);
    public static final RegistryObject<Item> MORPH_SWORD = item("morph_sword", MorphTools.Sword::new);
    public static final RegistryObject<Item> TAINT_PICK = item("taint_pickaxe", TaintTools.Pick::new);
    public static final RegistryObject<Item> TAINT_SPADE = item("taint_shovel", TaintTools.Spade::new);
    public static final RegistryObject<Item> BLINK = item("focus_blink", BlinkFocus::new);
    static {for(String rod:new String[]{"tainted","infernal","profane"})item("wand_rod_"+rod,()->new Item(new Item.Properties()));}
    public static final RegistryObject<Item> CRYSTALWELL = item("crystalwell", () -> new SpecialItems.Inkwell(false));
    public static final RegistryObject<Item> PRIMEWELL = item("primewell", () -> new SpecialItems.Inkwell(true));
    public static final RegistryObject<Item> RING = item("ring_nutrition", () -> IntegrationContent.botania()?fmmodern.compat.BotanyAccessories.ring():new SpecialItems.NutritionRing());
    public static final RegistryObject<Item> COLLAR = item("pain_collar", ()->IntegrationContent.botania()?fmmodern.compat.BotanyAccessories.collar():new PainCollar());
    public static final RegistryObject<Item> HERETIC_EGG = item("heretic_spawn_egg",()->new net.minecraftforge.common.ForgeSpawnEggItem(HERETIC,0x341b36,0x9d535a,new Item.Properties()));
    public static final RegistryObject<Block> CAKE = block("arcane_cake", ArcaneCake::new);
    public static final RegistryObject<Block> INK_FLOWER = block("ink_flower", () -> new FlowerBlock(net.minecraft.world.effect.MobEffects.BLINDNESS, 8, BlockBehaviour.Properties.copy(Blocks.DANDELION)));
    public static final RegistryObject<Block> UMBRAL_BUSH = block("umbral_bush", UmbralBush::new);
    public static final RegistryObject<Block> STAR_BLOCK = block("nether_star_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
    public static final RegistryObject<Block> TAINT_LOG = block("taint_log", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG).sound(SoundType.SLIME_BLOCK)));
    public static final RegistryObject<Block> TAINT_PLANKS = block("taint_planks", () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2,5)));
    public static final RegistryObject<Block> TAINT_STONE = block("taint_stone", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2,10)));
    public static final RegistryObject<Block> TAINT_BRICKS = block("taint_bricks", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS).strength(2,10)));
    public static final RegistryObject<Block> TAINT_LEAVES = block("taint_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES).sound(SoundType.SLIME_BLOCK)));
    public static final RegistryObject<Block> TAINT_SAPLING = block("taint_sapling", TaintedTree::new);
    public static final RegistryObject<Block> CAGE = block("wrath_cage", WrathCage::new);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<WrathCageEntity>> CAGE_ENTITY = ENTITIES.register("wrath_cage",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(WrathCageEntity::new,CAGE.get()).build(null));
    static { TABS.register("forbidden_magic", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.forbidden_magic"))
            .icon(() -> new ItemStack(item("wrath_shard"))).displayItems((p, output) -> {ALL.forEach((name,item) -> {if(IntegrationContent.creativeVisible(name))output.accept(item.get());});CageMobs.crystals().forEach(output::accept);DarkWands.creative(output::accept);}).build()); }
    public static Item item(String id) { return ALL.get(id).get(); }
    public static RegistryObject<Item> item(String id, Supplier<Item> factory) { var result = ITEMS.register(id, factory); ALL.put(id, result); return result; }
    public static RegistryObject<Block> block(String id, Supplier<Block> factory) { var result = BLOCKS.register(id, factory); item(id, () -> new BlockItem(result.get(), new Item.Properties())); return result; }
    private Content() {}
}

