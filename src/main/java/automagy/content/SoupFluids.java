package automagy.content;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
/** Each fluid owns its source, flowing state, bucket and placeable world block. */
public final class SoupFluids {
 public static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES,"automagy");
 public static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(ForgeRegistries.FLUIDS,"automagy");
 public static final Soup MUSHROOM=new Soup("mushroom",0), VISHROOM=new Soup("vishroom",8);
 public static final RegistryObject<LiquidBlock> MILK_BLOCK=Content.BLOCKS.register("milk_fluid",()->new LiquidBlock(()->(FlowingFluid)net.minecraftforge.common.ForgeMod.MILK.get(),Block.Properties.copy(Blocks.WATER)){});
 public static final class Soup {
  public final String name;public final RegistryObject<FluidType> type;public final RegistryObject<ForgeFlowingFluid.Source> source;public final RegistryObject<ForgeFlowingFluid.Flowing> flowing;public final RegistryObject<LiquidBlock> block;public final RegistryObject<Item> bucket;
  Soup(String name,int light){this.name=name;
   type=TYPES.register(name,()->new FluidType(FluidType.Properties.create().density(1000).viscosity(1500).lightLevel(light).canConvertToSource(false).descriptionId("fluid.automagy."+name)){
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions(){public ResourceLocation getStillTexture(){return ResourceLocation.parse("automagy:block/"+name+"_fluid");}public ResourceLocation getFlowingTexture(){return getStillTexture();}});}
   });
   source=FLUIDS.register(name,()->new ForgeFlowingFluid.Source(properties()));flowing=FLUIDS.register("flowing_"+name,()->new ForgeFlowingFluid.Flowing(properties()));
   block=Content.BLOCKS.register(name+"_fluid",()->new SoupLiquidBlock(this,Block.Properties.copy(Blocks.WATER).lightLevel(s->light)));
   bucket=Content.ITEMS.register(name+"_bucket",()->new BucketItem(source,new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
  }
  ForgeFlowingFluid.Properties properties(){return new ForgeFlowingFluid.Properties(type,source,flowing).block(block).bucket(bucket).tickRate(7);}
 }
 public static void register(IEventBus bus){TYPES.register(bus);FLUIDS.register(bus);}
 private SoupFluids(){}
}
