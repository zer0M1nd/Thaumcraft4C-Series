package infusionbrewing;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.*;

@Mod(InfusionBrewing.ID)
public class InfusionBrewing {
    public static final String ID="infusionbrewing";
    public static ResourceLocation id(String path){return new ResourceLocation(ID,path);}
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,ID);
    public static final RegistryObject<RecipeSerializer<BrewingRecipe>> BREWING=RECIPES.register("brewing",BrewingRecipe.Serializer::new);
    public static final RegistryObject<Item> MAGIC_BOTTLE=ITEMS.register("magic_bottle",MagicBottleItem::new);
    public static final RegistryObject<Item> MAGIC_BUCKET=ITEMS.register("magic_bucket",()->new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> FOCUS=ITEMS.register("potion_focus",PotionFocusItem::new);
    public static final RegistryObject<Item> AMULET=ITEMS.register("potion_amulet",()->new PotionCurioItem("aer"));
    public static final RegistryObject<Item> RING=ITEMS.register("potion_ring",()->new PotionCurioItem("aqua"));
    public static final RegistryObject<Item> BELT=ITEMS.register("potion_belt",()->new PotionCurioItem("terra"));
    public static final RegistryObject<Block> SEMI_SOLID=BLOCKS.register("semi_solid_potion",SemiSolidPotionBlock::new);
    public static final RegistryObject<Item> SEMI_SOLID_ITEM=ITEMS.register("semi_solid_potion",()->new BlockItem(SEMI_SOLID.get(),new Item.Properties()));
    public static final java.util.List<RegistryObject<Item>> UPGRADES=new java.util.ArrayList<>();
    static { for(int i=0;i<8;i++){int code=i;UPGRADES.add(ITEMS.register("spawner_"+(i<4?"power_":"speed_")+(i%4+1),()->new SpawnerUpgradeItem(code)));} }
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,ID);
    public static final RegistryObject<CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.infusionbrewing")).icon(()->new ItemStack(MAGIC_BOTTLE.get())).displayItems((p,out)->{
        ITEMS.getEntries().forEach(entry->{if(!(entry.get() instanceof PotionCurioItem) && !(entry.get() instanceof PotionFocusItem))out.accept(entry.get());});
        for(int id=1;id<=23;id++){
            int amplifier=BrewingRules.noAmplifier(id)?0:3;
            var data=new PotionData(id,BrewingRules.instant(id)?1:19200,amplifier);
            out.accept(data.write(new ItemStack(FOCUS.get())));
            if(BrewingRules.support(id)&&!BrewingRules.instant(id))for(var curio:java.util.List.of(AMULET,RING,BELT))out.accept(data.write(new ItemStack(curio.get())));
            out.accept(PotionData.base(id).potion(false));
        }
    }).build());
    public InfusionBrewing(){
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        PotionFluids.register(bus);BLOCKS.register(bus);ITEMS.register(bus);RECIPES.register(bus);TABS.register(bus);
        BrewingConfig.register();bus.addListener(this::setup);
        net.minecraftforge.common.crafting.CraftingHelper.register(id("potion"),PotionIngredient.SERIALIZER);
        MinecraftForge.EVENT_BUS.addListener(SpawnerUpgradeItem::inspect);
    }
    private void setup(FMLCommonSetupEvent event){event.enqueueWork(BrewingDispensers::register);}
}
