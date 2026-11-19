package thaumicomniverse.compat.create;


import com.simibubi.create.content.kinetics.mechanicalArm.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.ArcanePedestalBlock;

public final class CreateCompat {
    private CreateCompat() {}
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        ArcaneAssembly.register(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(CreateWandControl::setup));
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->
            bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event)->event.enqueueWork(CreateDeviceRendering::setup)));
        thaumicomniverse.compat.create.airship.AirshipContent.register(bus);
        ArcaneCreateContent.register(bus);
        ArcaneDisplaySources.register(bus);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(MagicHand::tick);
        CreateArcaneNetwork.register();
        CreateFocusNetwork.register();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(PowerFocusSessions::tick);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(RelocationSessions::tick);
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->CreateFocusClient.register(bus));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(ArcaneContraptions::setup));
        for(String name:java.util.List.of("alchemical_mechanism","infused_mechanism")) {
            thaumicomniverse.ThaumicOmniverse.ITEMS.register(name,()->new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));
            thaumicomniverse.ThaumicOmniverse.ITEMS.register("incomplete_"+name,()->new com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem(new net.minecraft.world.item.Item.Properties()));
        }
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->ArcaneAssemblyTooltips.register(bus));
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->CreateArcaneClient::init);
        ArmInteractionPointType.register(new ArmInteractionPointType(net.minecraft.resources.ResourceLocation.parse("thaumicomniverse:arcane_pedestal")) {
            @Override public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
                return thaumicomniverse.RevivalFeatures.Group.CREATE.enabled() && state.getBlock() instanceof ArcanePedestalBlock;
            }
            @Override public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
                return new ArmInteractionPoint(this, level, pos, state) {};
            }
        });
    }
}
