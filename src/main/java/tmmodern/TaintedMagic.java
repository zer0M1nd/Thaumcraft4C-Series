package tmmodern;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.entity.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TaintedMagic.ID)
public class TaintedMagic {
    public static final String ID="taintedmagic";
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,ID);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ID);
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<LumosBlock.Tile>> LUMOS_TILE=TILES.register("lumos",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(LumosBlock.Tile::new,block("lumos")).build(null));
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,ID);
    public static final Map<String,RegistryObject<Item>> CONTENT=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Block>> WOOD=new LinkedHashMap<>();
    public static Item item(String id){return CONTENT.get(id).get();}
    public static Block block(String id){return WOOD.get(id).get();}
    public static RegistryObject<Item> add(String id,Supplier<? extends Item> factory){RegistryObject<Item> value=ITEMS.register(id,()->factory.get());CONTENT.put(id,value);return value;}
    public static RegistryObject<Block> addBlock(String id,Supplier<? extends Block> factory){RegistryObject<Block> value=BLOCKS.register(id,()->factory.get());WOOD.put(id,value);add(id,()->new BlockItem(value.get(),new Item.Properties()));return value;}
    public static final RegistryObject<EntityType<DarkMatterProjectile>> DARK=ENTITIES.register("dark_matter",()->EntityType.Builder.<DarkMatterProjectile>of(DarkMatterProjectile::new,MobCategory.MISC).sized(.5f,.5f).clientTrackingRange(64).updateInterval(1).build("taintedmagic:dark_matter"));
    public static final RegistryObject<EntityType<DarkMatterProjectile>> DIFFUSION=ENTITIES.register("diffusion",()->EntityType.Builder.<DarkMatterProjectile>of(DarkMatterProjectile::new,MobCategory.MISC).sized(.1f,.1f).clientTrackingRange(64).updateInterval(1).build("taintedmagic:diffusion"));
    public static final RegistryObject<EntityType<HomingShardProjectile>> SHARD=ENTITIES.register("homing_shard",()->EntityType.Builder.<HomingShardProjectile>of(HomingShardProjectile::new,MobCategory.MISC).sized(.2f,.2f).clientTrackingRange(64).updateInterval(1).build("taintedmagic:homing_shard"));
    public static final RegistryObject<net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<VoidBloodRecipe>> VOID_RECIPE=RECIPES.register("void_blood",()->new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(VoidBloodRecipe::new));
    static {
        for(String id:List.of("shadowmetal_ingot","shadow_imbued_cloth","crimson_stained_cloth","warped_unbalanced_shard","tainted_unbalanced_shard","shard_of_creation","thaumic_alloy_plate","crimson_alloy_plate","shadowmetal_nugget","primordial_nodule","primordial_mote","fragment_of_creation","wand_rod_warpwood","wand_rod_warpwood_staff","wand_cap_shadowmetal","wand_cap_cloth","wand_cap_crimsoncloth","wand_cap_shadowcloth"))add(id,()->new Item(new Item.Properties().rarity(id.contains("creation")?Rarity.EPIC:Rarity.UNCOMMON)));
        for(String id:List.of("void_blood","crimson_blood","magic_funguar","nightshade_berries","salis_tempestas","salis_aevus","warp_fertilizer","gate_key","flyte_charm"))add(id,()->new UtilityItem(id));
        for(String id:List.of("taint_swarm","dark_matter","mage_mace","shockwave","vis_shard","lumos"))add("focus_"+id,()->new MagicFocus(id));
        for(String id:List.of("void","shadow"))for(var type:List.of(ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS))add(id+"_fortress_"+type.getName(),()->new TMArmor.Fortress(type,id.equals("shadow")));
        add("warped_goggles",()->new TMArmor.Goggles(false));add("voidmetal_goggles",()->new TMArmor.Goggles(true));add("voidwalker_boots",TMArmor.Boots::new);
        add("voidwalker_sash",()->new TMCurio(false));add("lumos_ring",()->new TMCurio(true));
        add("shadowmetal_pick",TMTools.Pick::new);add("shadowmetal_axe",TMTools.Axe::new);add("shadowmetal_spade",TMTools.Spade::new);add("shadowmetal_hoe",TMTools.Hoe::new);add("shadowmetal_sword",()->new TMTools.Blade(false));add("primal_blade",()->new TMTools.Blade(true));add("hollow_dagger",TMTools.HollowDagger::new);add("thaumic_disassembler",DisassemblerItem::new);
        for(int n=0;n<3;n++){int material=n;add(List.of("thaumium_katana","voidmetal_katana","shadowmetal_katana").get(n),()->new KatanaItem(material));}
        addBlock("warpwood_log",Warpwood.Log::new);addBlock("warpwood_planks",()->new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2,5)));addBlock("warpwood_leaves",Warpwood.Leaves::new);addBlock("warpwood_sapling",Warpwood.Sapling::new);addBlock("nightshade_bush",Warpwood.Nightshade::new);addBlock("lumos",LumosBlock::new);
        for(String id:List.of("shockwave","shard"))SOUNDS.register(id,()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.parse("taintedmagic:"+id)));
        TABS.register("main",()->CreativeModeTab.builder().title(net.minecraft.network.chat.Component.translatable("itemGroup.taintedmagic")).icon(()->new ItemStack(item("shard_of_creation"))).displayItems((p,out)->CONTENT.forEach((id,value)->{if(!id.equals("lumos"))out.accept(value.get());})).build());
    }
    public TaintedMagic(){
        RevivalFeatures.register();var bus=FMLJavaModLoadingContext.get().getModEventBus();ITEMS.register(bus);BLOCKS.register(bus);TILES.register(bus);ENTITIES.register(bus);RECIPES.register(bus);SOUNDS.register(bus);TABS.register(bus);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new TMRuntime());net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,TMConfig.SPEC);bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e)->e.enqueueWork(TMRuntime::setup));}
}
