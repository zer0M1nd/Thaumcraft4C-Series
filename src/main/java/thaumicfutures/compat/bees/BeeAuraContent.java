package thaumicfutures.compat.bees;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class BeeAuraContent {
    static final int[] DURATIONS = {850, 400, 300};
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "thaumicfutures");
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "thaumicfutures");
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "thaumicfutures");
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<MagicApiaryMenu>> APIARY_MENU = MENUS.register("magic_apiary",
            () -> net.minecraftforge.common.extensions.IForgeMenuType.create(MagicApiaryMenu::fromNetwork));
    public static final RegistryObject<Block> APIARY = BLOCKS.register("magic_apiary", () -> new Machine(true));
    public static final RegistryObject<Block> PROVIDER = BLOCKS.register("vis_aura_provider", () -> new Machine(false));
    public static final RegistryObject<BlockEntityType<MagicApiary>> APIARY_TILE = TILES.register("magic_apiary",
            () -> BlockEntityType.Builder.of(MagicApiary::new, APIARY.get()).build(null));
    public static final RegistryObject<BlockEntityType<VisAuraProvider>> PROVIDER_TILE = TILES.register("vis_aura_provider",
            () -> BlockEntityType.Builder.of(VisAuraProvider::new, PROVIDER.get()).build(null));
    private BeeAuraContent() {}
    public static void register(DeferredRegister<Item> items, IEventBus bus) {
        items.register("magic_apiary", () -> new BlockItem(APIARY.get(), new Item.Properties()));
        items.register("vis_aura_provider", () -> new BlockItem(PROVIDER.get(), new Item.Properties()));
        BLOCKS.register(bus);
        TILES.register(bus);
        MENUS.register(bus);
    }
    private static final class Machine extends BaseEntityBlock {
        private final boolean apiary;
        Machine(boolean apiary) {
            super(Properties.of().strength(apiary ? 1 : .5f, 1.5f).sound(SoundType.WOOD)
                    .lightLevel(state -> apiary ? 0 : 8).noOcclusion());
            this.apiary = apiary;
        }
        @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return apiary ? new MagicApiary(pos, state) : new VisAuraProvider(pos, state);
        }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (apiary) return createTickerHelper(type, APIARY_TILE.get(), (world, pos, current, tile) -> {
                if (world.isClientSide) tile.clientTick(world, pos, current); else tile.serverTick(world, pos, current);
            });
            return level.isClientSide ? null : createTickerHelper(type, PROVIDER_TILE.get(), (world, pos, current, tile) -> tile.tick(world));
        }
        @Override public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!apiary) return InteractionResult.PASS;
            if (player instanceof ServerPlayer server && world.getBlockEntity(pos) instanceof MagicApiary tile) tile.openGui(server, hand, pos);
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        @Override public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(world, pos, state, placer, stack);
            if (placer instanceof Player player && world.getBlockEntity(pos) instanceof MagicApiary tile) {
                tile.getOwnerHandler().setOwner(player.getGameProfile());
                tile.setChanged();
            }
        }
        @Override public void onRemove(BlockState state, Level world, BlockPos pos, BlockState replacement, boolean moving) {
            if (!state.is(replacement.getBlock()) && world.getBlockEntity(pos) instanceof MagicApiary tile) {
                Containers.dropContents(world, pos, tile);
                world.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, world, pos, replacement, moving);
        }
    }
}
