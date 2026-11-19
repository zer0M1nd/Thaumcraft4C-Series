package thaumicfutures.compat.machinery;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class MachineryContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "thaumicfutures");
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "thaumicfutures");
    private static final Map<String, RegistryObject<Block>> PORTS = new LinkedHashMap<>();
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "thaumicfutures");
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<AspectPortMenu>> PORT_MENU = MENUS.register("machine_aspect_port",
            () -> net.minecraftforge.common.extensions.IForgeMenuType.create(AspectPortMenu::fromNetwork));
    public static final RegistryObject<BlockEntityType<AspectPort>> PORT_TILE = TILES.register("machine_aspect_port",
            () -> BlockEntityType.Builder.of(MachineryContent::createTile, PORTS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    private MachineryContent() {}
    public static void register(DeferredRegister<Item> items, IEventBus bus) {
        for (AspectPort.Kind kind : AspectPort.Kind.values()) {
            for (int tier = 1; tier <= (kind == AspectPort.Kind.BRIDGE ? 1 : 16); tier++) {
                int value = tier;
                String name = kind == AspectPort.Kind.BRIDGE ? "machine_vis_bridge"
                        : "machine_" + kind.name().toLowerCase(Locale.ROOT) + "_" + tier;
                RegistryObject<Block> block = BLOCKS.register(name, () -> new PortBlock(kind, value));
                PORTS.put(name, block);
                items.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
            }
        }
        BLOCKS.register(bus);
        TILES.register(bus);
        MENUS.register(bus);
    }
    private static AspectPort createTile(BlockPos pos, BlockState state) {
        return ((PortBlock) state.getBlock()).kind.essentia() ? new AspectPort.Essentia(pos, state) : new AspectPort(pos, state);
    }
    public static final class PortBlock extends BaseEntityBlock {
        public static final net.minecraft.world.level.block.state.properties.BooleanProperty[] FACES =
                java.util.Arrays.stream(net.minecraft.core.Direction.values()).map(direction ->
                        net.minecraft.world.level.block.state.properties.BooleanProperty.create(direction.getName()))
                        .toArray(net.minecraft.world.level.block.state.properties.BooleanProperty[]::new);
        final AspectPort.Kind kind;
        final int tier;
        PortBlock(AspectPort.Kind kind, int tier) {
            super(Properties.of().strength(3.5f).sound(SoundType.METAL));
            this.kind = kind;
            this.tier = tier;
            BlockState initial = stateDefinition.any();
            for (var face : FACES) initial = initial.setValue(face, kind != AspectPort.Kind.BRIDGE);
            registerDefaultState(initial);
        }
        @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACES);
        }
        @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return createTile(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : createTickerHelper(type, PORT_TILE.get(), (world, pos, current, tile) -> tile.tick());
        }
        @Override public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!(world.getBlockEntity(pos) instanceof AspectPort port)) return InteractionResult.PASS;
            if (!world.isClientSide && !world.mayInteract(player, pos)) return InteractionResult.FAIL;
            if (!world.isClientSide && player.isShiftKeyDown()) {
                if (kind == AspectPort.Kind.BRIDGE) port.selectAbsorption(hit.getDirection());
                else port.toggleSide(hit.getDirection());
            } else if (player instanceof net.minecraft.server.level.ServerPlayer server) {
                world.sendBlockUpdated(pos, state, state, 2);
                net.minecraftforge.network.NetworkHooks.openScreen(server,
                        new net.minecraft.world.SimpleMenuProvider((id, inventory, viewer) -> new AspectPortMenu(id, port),
                                net.minecraft.network.chat.Component.translatable(getDescriptionId())), pos);
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
    }
}
