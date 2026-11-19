package infusionbrewing;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.registries.*;

public final class PotionFluids {
    private static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES,InfusionBrewing.ID);
    private static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(ForgeRegistries.FLUIDS,InfusionBrewing.ID);
    public static final java.util.List<Family> ALL=new java.util.ArrayList<>();
    public static final class Family {
        public final int id;
        public final RegistryObject<FluidType> type;
        public final RegistryObject<ForgeFlowingFluid.Source> source;
        public final RegistryObject<ForgeFlowingFluid.Flowing> flowing;
        public final RegistryObject<Block> block;
        public final RegistryObject<Item> bucket;
        Family(int id){
            this.id=id;String name=PotionData.NAMES[id-1];
            type=TYPES.register(name,()->new FluidType(FluidType.Properties.create().density(800).viscosity(1000).canConvertToSource(true)){
                @Override public boolean canConvertToSource(FluidState state,LevelReader level,BlockPos pos){return BrewingConfig.SPEC.isLoaded()?BrewingConfig.INFINITE.get():true;}
                @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> c){
                    c.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions(){
                        @Override public ResourceLocation getStillTexture(){return InfusionBrewing.id("block/potion"+id+"_still");}
                        @Override public ResourceLocation getFlowingTexture(){return InfusionBrewing.id("block/potion"+id+"_flow");}
                    });
                }
            });
            source=FLUIDS.register(name,()->new ForgeFlowingFluid.Source(properties()));
            flowing=FLUIDS.register("flowing_"+name,()->new ForgeFlowingFluid.Flowing(properties()));
            block=InfusionBrewing.BLOCKS.register(name,()->new PotionLiquidBlock(this));
            bucket=InfusionBrewing.ITEMS.register(name+"_bucket",()->new BucketItem(source,new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
        }
        private ForgeFlowingFluid.Properties properties(){return new ForgeFlowingFluid.Properties(type,source,flowing).bucket(bucket).block(()->(LiquidBlock)block.get()).tickRate(5).slopeFindDistance(4).levelDecreasePerBlock(1);}
    }
    static{for(int i=1;i<=23;i++)ALL.add(new Family(i));}
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus){TYPES.register(bus);FLUIDS.register(bus);}
    public static Family family(int id){return ALL.get(id-1);}
    public static int id(FluidState fluid){for(var family:ALL)if(fluid.is(family.source.get())||fluid.is(family.flowing.get()))return family.id;return 0;}
}
