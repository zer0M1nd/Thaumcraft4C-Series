package tc4revival.content;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public  class TCFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, "thaumcraft");
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, "thaumcraft");
    public static final RegistryObject<FluidType> GAS_TYPE = TYPES.register("flux_gas", () -> new FluxType(-4, 2500, "fluxgas"));
    public static final RegistryObject<FiniteFluxFluid> GAS = FLUIDS.register("flux_gas", () -> new FiniteFluxFluid(GAS_TYPE, TCArcane.FLUX_GAS));
    public static final RegistryObject<FluidType> GOO_TYPE = TYPES.register("flux_goo", () -> new FluxType(8,6000,"fluxgoo"));
    public static final RegistryObject<FiniteFluxFluid> GOO = FLUIDS.register("flux_goo", () -> new FiniteFluxFluid(GOO_TYPE,TCArcane.FLUX_GOO));
    public static final RegistryObject<FluidType> PURE_TYPE=TYPES.register("purifying_water",()->new FluidType(FluidType.Properties.create()
            .descriptionId("block.thaumcraft.purifying_water").rarity(net.minecraft.world.item.Rarity.RARE)
            .density(1000).viscosity(1000).lightLevel(10).canConvertToSource(false).canSwim(true).canDrown(true).canExtinguish(true).canHydrate(true)) {
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions() {
                @Override public net.minecraft.resources.ResourceLocation getStillTexture() { return net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/fluidpure"); }
                @Override public net.minecraft.resources.ResourceLocation getFlowingTexture() { return getStillTexture(); }
            });
        }
    });
    public static final RegistryObject<net.minecraftforge.fluids.ForgeFlowingFluid.Source> PURE=FLUIDS.register("purifying_water",()->new net.minecraftforge.fluids.ForgeFlowingFluid.Source(pureProperties()));
    public static final RegistryObject<net.minecraftforge.fluids.ForgeFlowingFluid.Flowing> PURE_FLOWING=FLUIDS.register("flowing_purifying_water",()->new net.minecraftforge.fluids.ForgeFlowingFluid.Flowing(pureProperties()));
    private static net.minecraftforge.fluids.ForgeFlowingFluid.Properties pureProperties() {
        return new net.minecraftforge.fluids.ForgeFlowingFluid.Properties(PURE_TYPE,PURE,PURE_FLOWING).bucket(TCItems.PURE_WATER_BUCKET).block(TCCleansing.PURE_WATER).tickRate(5).slopeFindDistance(4).levelDecreasePerBlock(1);
    }
    public static final RegistryObject<FluidType> DEATH_TYPE=TYPES.register("liquid_death",()->new FluidType(FluidType.Properties.create()
            .descriptionId("block.thaumcraft.liquid_death").density(1000).viscosity(1500).lightLevel(8).rarity(net.minecraft.world.item.Rarity.RARE)
            .canConvertToSource(false).canSwim(true).canDrown(true).canExtinguish(true).canHydrate(true)) {
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions() {
                @Override public net.minecraft.resources.ResourceLocation getStillTexture() { return net.minecraft.resources.ResourceLocation.parse("thaumcraft:block/fluiddeath"); }
                @Override public net.minecraft.resources.ResourceLocation getFlowingTexture() { return getStillTexture(); }
                @Override public int getTintColor(net.minecraft.world.level.material.FluidState state,net.minecraft.world.level.BlockAndTintGetter level,net.minecraft.core.BlockPos pos) { return 0x00ffffff; }
            });
        }
    });
    public static final RegistryObject<FiniteFluxFluid> DEATH=FLUIDS.register("liquid_death",()->new FiniteFluxFluid(DEATH_TYPE,TCArcane.LIQUID_DEATH));
    private TCFluids() {}
    public static void register(IEventBus bus) { TYPES.register(bus); FLUIDS.register(bus); }

    private static  class FluxType extends FluidType {
        private final String texture;
        FluxType(int density, int viscosity, String texture) {
            // MaterialTaint was not water: do not introduce swimming, buoyancy, hydration or drowning.
            super(Properties.create().density(density).viscosity(viscosity).lightLevel(7)
                    .canPushEntity(false).canSwim(false).canDrown(false).canExtinguish(false).canConvertToSource(false).canHydrate(false));
            this.texture = texture;
        }
        @Override public void setItemMovement(net.minecraft.world.entity.item.ItemEntity entity) {}
        @Override public int getLightLevel(net.minecraft.world.level.material.FluidState state, net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos) {
            return (state.getAmount() - 1) * 7 / 8;
        }
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
            // initializeClient is called by FluidType's constructor, before this subclass's fields are initialized.
            consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions() {
                @Override public net.minecraft.resources.ResourceLocation getStillTexture() { return new net.minecraft.resources.ResourceLocation("thaumcraft", "block/" + texture); }
                @Override public net.minecraft.resources.ResourceLocation getFlowingTexture() { return getStillTexture(); }
                // The chunk model owns finite, inverted geometry. Suppress vanilla's bottom-filled fluid mesh.
                @Override public int getTintColor(net.minecraft.world.level.material.FluidState state, net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos) { return 0x00ffffff; }
            });
        }
    }
}
