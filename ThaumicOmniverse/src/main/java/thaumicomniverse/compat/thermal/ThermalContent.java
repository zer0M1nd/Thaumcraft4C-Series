package thaumicomniverse.compat.thermal;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import cofh.thermal.lib.common.block.DynamoBlock;
import thaumicomniverse.ThaumicOmniverse;

public final class ThermalContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,"thaumicomniverse");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,"thaumicomniverse");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"thaumicomniverse");
    public static final RegistryObject<DynamoBlock> BLOCK=BLOCKS.register("essentia_dynamo",
        ()->new DynamoBlock(Block.Properties.copy(Blocks.IRON_BLOCK).noOcclusion(),EssentiaDynamo.class,()->ThermalContent.ENTITY.get()));
    public static final RegistryObject<BlockEntityType<EssentiaDynamo>> ENTITY=ENTITIES.register("essentia_dynamo",
        ()->BlockEntityType.Builder.of(EssentiaDynamo::new,BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<EssentiaDynamoMenu>> MENU=MENUS.register("essentia_dynamo",
        ()->net.minecraftforge.common.extensions.IForgeMenuType.create((id,inv,data)->new EssentiaDynamoMenu(id,inv.player.level(),data.readBlockPos(),inv,inv.player)));
    public static void register(IEventBus bus){
        BLOCKS.register(bus);ENTITIES.register(bus);MENUS.register(bus);
        ThaumicOmniverse.ITEMS.register("essentia_dynamo",()->new cofh.core.common.item.BlockItemCoFH(BLOCK.get(),new Item.Properties()));
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->bus.addListener(ThermalClient::setup));
    }
    private ThermalContent(){}
}
