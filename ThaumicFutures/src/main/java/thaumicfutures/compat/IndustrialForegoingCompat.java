package thaumicfutures.compat;

import com.buuz135.industrial.IndustrialForegoing;
import com.buuz135.industrial.api.plant.PlantRecollectable;
import com.buuz135.industrial.registry.IFRegistries;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.ManaPodBlock;

public final class IndustrialForegoingCompat {
    private IndustrialForegoingCompat() {}
    public static void register(IEventBus bus) {
        var crops = DeferredRegister.create(IFRegistries.PLANT_RECOLLECTABLES_REGISTRY_KEY, "thaumicfutures");
        crops.register("magical_plants", MagicalPlants::new);
        crops.register(bus);
    }
    public static boolean allowed(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var player = IndustrialForegoing.getFakePlayer(level);
        return level.mayInteract(player, pos) && !MinecraftForge.EVENT_BUS.post(
                new BlockEvent.BreakEvent(level, pos, level.getBlockState(pos), player));
    }
    private static final class MagicalPlants extends PlantRecollectable {
        private MagicalPlants() { super("thaumicomniverse_magical_plants"); }
        @Override public boolean canBeHarvested(Level level, BlockPos pos, BlockState state) {
            if (!thaumicfutures.FutureFeatures.Group.INDUSTRIAL.enabled() || !level.hasChunkAt(pos)) return false;
            if (state.getBlock() instanceof ManaPodBlock) return state.getValue(ManaPodBlock.AGE) >= 5;
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            return id != null && id.getNamespace().equals("thaumcraft") && switch (id.getPath()) {
                case "shimmerleaf", "cinderpearl", "vishroom", "taint_fibres" -> true;
                default -> false;
            };
        }
        @Override public List<ItemStack> doHarvestOperation(Level level, BlockPos pos, BlockState state) {
            if (!(level instanceof ServerLevel server) || !canBeHarvested(level, pos, state)
                    || !level.getBlockState(pos).equals(state) || !allowed(server, pos)) return List.of();
            // Compute while the mana pod BE still exists so loot retains its aspect.
            var drops = Block.getDrops(state, server, pos, level.getBlockEntity(pos),
                    IndustrialForegoing.getFakePlayer(level), ItemStack.EMPTY);
            return level.removeBlock(pos, false) ? drops : List.of();
        }
        @Override public boolean shouldCheckNextPlant(Level level, BlockPos pos, BlockState state) { return true; }
    }
}
