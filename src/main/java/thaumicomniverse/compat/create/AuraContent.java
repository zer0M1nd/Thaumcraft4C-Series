package thaumicomniverse.compat.create;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.*;
import net.minecraftforge.registries.*;
import thaumicomniverse.ThaumicOmniverse;

/** Aura materials exist independently; optional integrations supply processing and collection. */
public class AuraContent {
    private static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES,"thaumicomniverse");
    private static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(ForgeRegistries.FLUIDS,"thaumicomniverse");
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"thaumicomniverse");
    public static final RegistryObject<FluidType> TYPE=TYPES.register("aura",()->new FluidType(FluidType.Properties.create().density(1000).viscosity(1000).lightLevel(12)) {
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer){
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions(){
                @Override public ResourceLocation getStillTexture(){return ResourceLocation.parse("thaumicomniverse:fluid/aura_still");}
                @Override public ResourceLocation getFlowingTexture(){return ResourceLocation.parse("thaumicomniverse:fluid/aura_flow");}
            });
        }
    });
    public static final RegistryObject<ForgeFlowingFluid.Source> SOURCE=FLUIDS.register("aura",()->new ForgeFlowingFluid.Source(properties()));
    public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING=FLUIDS.register("flowing_aura",()->new ForgeFlowingFluid.Flowing(properties()));
    public static final RegistryObject<LiquidBlock> LIQUID=BLOCKS.register("aura",()->new LiquidBlock(SOURCE,BlockBehaviour.Properties.copy(Blocks.WATER).lightLevel(state->12)));
    public static final RegistryObject<Block> SOLID_BLOCK=BLOCKS.register("solid_aura_block",()->new Block(BlockBehaviour.Properties.of().strength(3).sound(SoundType.AMETHYST).lightLevel(state->10)));
    public static final RegistryObject<SolidAuraItem> SOLID=ThaumicOmniverse.ITEMS.register("solid_aura",SolidAuraItem::new);
    public static final RegistryObject<Item> BUCKET=ThaumicOmniverse.ITEMS.register("aura_bucket",()->new BucketItem(SOURCE,new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    private static ForgeFlowingFluid.Properties properties(){return new ForgeFlowingFluid.Properties(TYPE,SOURCE,FLOWING).bucket(BUCKET).block(LIQUID).slopeFindDistance(3).tickRate(10);}
    public static void register(IEventBus bus){
        ThaumicOmniverse.ITEMS.register("solid_aura_block",()->new BlockItem(SOLID_BLOCK.get(),new Item.Properties()));
        TYPES.register(bus);FLUIDS.register(bus);BLOCKS.register(bus);
    }
}
