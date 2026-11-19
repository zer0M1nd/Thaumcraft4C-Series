package timodern;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;

@Mod(ThaumicInfusion.ID)
public class ThaumicInfusion {
    public static final String ID="thaumicinfusion";
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ID);
    public static final DeferredRegister<BlockEntityType<?>> TILES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,ID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,ID);
    public static final RegistryObject<EssentiaRecipe.Serializer> ESSENTIA_RECIPE=RECIPES.register("essentia",EssentiaRecipe.Serializer::new);
    public static final RegistryObject<InfusingFocus> FOCUS=ITEMS.register("focus_infusion",InfusingFocus::new);
    public static final RegistryObject<Item> PAPER=ITEMS.register("coordinate_paper",()->new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Block> ESSENTIA=BLOCKS.register("essentia_block",EssentiaBlock::new);
    public static final RegistryObject<Block> BRICKS=BLOCKS.register("essentia_bricks",EssentiaBlock::new);
    public static final RegistryObject<Block> CHISELED=BLOCKS.register("chiseled_essentia",EssentiaBlock::new);
    public static final RegistryObject<BlockEntityType<EssentiaBlock.Tile>> ESSENTIA_TILE=TILES.register("essentia",()->BlockEntityType.Builder.of(EssentiaBlock.Tile::new,ESSENTIA.get(),BRICKS.get(),CHISELED.get()).build(null));
    static {for(var block:java.util.List.of(ESSENTIA,BRICKS,CHISELED))ITEMS.register(block.getId().getPath(),()->new BlockItem(block.get(),new Item.Properties()){public net.minecraft.network.chat.Component getName(ItemStack stack){var a=tc4revival.aspect.TCAspects.catalog().find(EssentiaBlock.aspect(stack));return a.isEmpty()?super.getName(stack):net.minecraft.network.chat.Component.translatable("block.thaumicinfusion."+block.getId().getPath()+".aspect",thaumcraft.api.aspects.AspectViews.display(a.get()).name());}});}
    public static final RegistryObject<CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder().title(net.minecraft.network.chat.Component.translatable("itemGroup.thaumicinfusion")).icon(()->new ItemStack(FOCUS.get())).displayItems((p,o)->{
        o.accept(FOCUS.get());o.accept(PAPER.get());for(var aspect:tc4revival.aspect.TCAspects.catalog().all())for(var block:java.util.List.of(ESSENTIA,BRICKS,CHISELED))o.accept(EssentiaBlock.stack(block.get(),aspect.tag()));
    }).build());
    public ThaumicInfusion(){
        RevivalFeatures.register();var bus=FMLJavaModLoadingContext.get().getModEventBus();ITEMS.register(bus);BLOCKS.register(bus);TILES.register(bus);TABS.register(bus);RECIPES.register(bus);net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,InfusionConfig.SPEC);InfusionNetwork.init();MinecraftForge.EVENT_BUS.register(new InfusionRuntime());MinecraftForge.EVENT_BUS.register(new InfusionCommands());}
}
