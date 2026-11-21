package technomancy;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.*;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.*;
import net.minecraftforge.registries.*;

public final class TechFluids {
    private static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES,Technomancy.ID);
    private static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(ForgeRegistries.FLUIDS,Technomancy.ID);
    public static final RegistryObject<FluidType> TYPE=TYPES.register("mana_condensate",()->new FluidType(FluidType.Properties.create().density(1000).viscosity(1000)) {
        @Override public void initializeClient(java.util.function.Consumer<IClientFluidTypeExtensions> consumer){consumer.accept(new IClientFluidTypeExtensions(){
            public ResourceLocation getStillTexture(){return new ResourceLocation("minecraft","block/water_still");}
            public ResourceLocation getFlowingTexture(){return new ResourceLocation("minecraft","block/water_flow");}
            public int getTintColor(){return 0xff00c6ff;}
        });}
    });
    public static final RegistryObject<FlowingFluid> SOURCE=FLUIDS.register("mana_condensate",()->new ForgeFlowingFluid.Source(properties()));
    public static final RegistryObject<FlowingFluid> FLOWING=FLUIDS.register("flowing_mana_condensate",()->new ForgeFlowingFluid.Flowing(properties()));
    public static final RegistryObject<LiquidBlock> BLOCK=TechRegistry.BLOCKS.register("mana_condensate",()->new LiquidBlock(SOURCE,net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(Blocks.WATER)));
    public static final RegistryObject<Item> BUCKET=TechRegistry.ITEMS.register("mana_condensate_bucket",()->new BucketItem(SOURCE,new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    private static ForgeFlowingFluid.Properties properties(){return new ForgeFlowingFluid.Properties(TYPE,SOURCE,FLOWING).block(BLOCK).bucket(BUCKET);}
    public static void register(IEventBus bus){TYPES.register(bus);FLUIDS.register(bus);}
    private TechFluids(){}
}
