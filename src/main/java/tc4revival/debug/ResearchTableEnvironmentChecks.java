package tc4revival.debug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import tc4revival.TC4Revival;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;
import tc4revival.research.ResearchTableBonuses;

/** Natural block-entity ticks, with a mid-cycle NBT reload; no world RNG replacement or player records. */
public  class ResearchTableEnvironmentChecks {
    private static final Map<MinecraftServer, Fixture> RUNNING = new HashMap<>();
    private ResearchTableEnvironmentChecks() {}
    static int start(CommandSourceStack source) {
        require(!RUNNING.containsKey(source.getServer()), "Research table environment check already running");
        var fixture = new Fixture(source);
        fixture.checkEmpty();
        try {
            fixture.prepare(); RUNNING.put(source.getServer(), fixture);
            source.sendSuccess(() -> Component.literal("Research table environment START; await 602 natural server ticks, NBT replacement at tick301; temporary fixture at 8,100,8"), false);
        } catch (RuntimeException failure) { fixture.close(); throw failure; }
        return 1;
    }
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var fixture = RUNNING.get(event.getServer()); if (fixture == null) return;
        try {
            if (!fixture.advance()) return;
            fixture.source.sendSuccess(() -> Component.literal("Research table environment PASS; 602 natural ticks, no early refresh; real block entity replaced from NBT at301; bonus set preserved; extension never ticks; world source/radius/Y boundaries and dimension heights checked; fixture cleaned"), false);
        } catch (RuntimeException failure) {
            TC4Revival.LOGGER.error("Research table environment check failed", failure);
            fixture.source.sendFailure(Component.literal("Research table environment FAIL: " + failure.getMessage()));
        }
        fixture.close(); RUNNING.remove(event.getServer());
    }
    public static void stopping(ServerStoppingEvent event) {
        var fixture = RUNNING.remove(event.getServer()); if (fixture != null) fixture.close();
    }
    private static  class Fixture {
        private final CommandSourceStack source;
        private final ServerLevel level;
        private final BlockPos owner = new BlockPos(8, 100, 8), mate = owner.east();
        private final List<BlockPos> positions = List.of(owner, mate, owner.above(), owner.offset(8, 8, 8), owner.offset(9, 0, 0),
                new BlockPos(8, 0, 8), new BlockPos(8, 1, 8));
        private int elapsed;
        private int warmup;
        private boolean began;
        Fixture(CommandSourceStack source) { this.source = source; level = source.getServer().overworld(); }
        void checkEmpty() {
            require(level.players().isEmpty(), "Run environment fixture with overworld players offline");
            // ServerLevel skips all block-entity ticking after 300 empty ticks without forced chunks,
            // even when a spawn chunk reports BLOCK_TICKING and its entity data is already loaded.
            require(level.getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(owner)),
                    "Offline environment fixture requires temporary forceload 8 8; remove only that new ticket after testing");
            require(level.getChunkAt(owner).getFullStatus().isOrAfter(net.minecraft.server.level.FullChunkStatus.BLOCK_TICKING)
                            && level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(owner)),
                    "Fixture chunk is not ready for block entity ticking; temporarily forceload 8 8, wait for activation, and remove only that new ticket after testing");
            for (var pos : positions) require(level.hasChunkAt(pos) && level.getBlockState(pos).isAir()
                    && level.getBlockEntity(pos) == null, "Environment fixture must be loaded and empty: " + pos);
        }
        void prepare() {
            // Exercise the actual world adapter, with deterministic draws only in the isolated rule invocation.
            var environment = new ResearchTableEnvironment(level, owner);
            var order = TCAspects.catalog().all().stream().map(a -> a.tag()).toList();
            require(environment.height() == level.dimensionType().logicalHeight(), "Overworld height adapter");
            var nether = source.getServer().getLevel(Level.NETHER);
            require(nether != null && new ResearchTableEnvironment(nether, owner).height() == 128, "Default nether logical height");
            level.setBlockAndUpdate(owner.above(), Blocks.DIRT.defaultBlockState());
            require(environment.at(0, 1, 0).equals(ResearchTableBonuses.Source.primal("terra", 20)), "Real dirt bonus");
            level.setBlockAndUpdate(owner.above(), Blocks.ROOTED_DIRT.defaultBlockState());
            require(environment.at(0, 1, 0).equals(ResearchTableBonuses.Source.NONE), "New dirt must not gain old bonus");
            level.setBlockAndUpdate(owner.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(owner.offset(8, 8, 8), Blocks.DIRT.defaultBlockState());
            require(scan(environment, order).contains("terra"), "Inclusive eight-block scan boundary");
            level.setBlockAndUpdate(owner.offset(8, 8, 8), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(owner.offset(9, 0, 0), Blocks.DIRT.defaultBlockState());
            require(!scan(environment, order).contains("terra"), "Nine-block source incorrectly included");
            level.setBlockAndUpdate(owner.offset(9, 0, 0), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(8, 0, 8), Blocks.DIRT.defaultBlockState());
            var low = new ResearchTableEnvironment(level, new BlockPos(8, 1, 8));
            require(!scan(low, order).contains("terra"), "Absolute Y0 must be excluded");
            level.setBlockAndUpdate(new BlockPos(8, 1, 8), Blocks.DIRT.defaultBlockState());
            require(scan(low, order).contains("terra"), "Absolute Y1 must be included");
            require(!scan(new ResearchTableEnvironment(level, new BlockPos(8, -8, 8)), order).contains("terra"), "Negative expansion changed old Y boundary");
            level.setBlockAndUpdate(new BlockPos(8, 0, 8), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(8, 1, 8), Blocks.AIR.defaultBlockState());
            var state = TCTables.TABLE.get().defaultBlockState();
            level.setBlock(owner, state.setValue(TableBlock.PART, TableBlock.Part.MASTER).setValue(TableBlock.FACING, Direction.EAST), 2);
            level.setBlock(mate, state.setValue(TableBlock.PART, TableBlock.Part.EXTENSION).setValue(TableBlock.FACING, Direction.WEST), 2);
            // Preexisting bonuses must survive the natural refresh and the mid-cycle reload.
            var tag = table().saveWithoutMetadata(); var bonus = new net.minecraft.nbt.ListTag();
            for (String aspect : order) { var row = new net.minecraft.nbt.CompoundTag(); row.putString("tag", aspect); bonus.add(row); }
            tag.put("bonusAspects", bonus); tag.putInt("nextRecalc", 0); table().load(tag);
        }
        private Set<String> scan(ResearchTableEnvironment environment, List<String> order) {
            return ResearchTableBonuses.recalculate(Set.of(), environment, order, bound -> 0);
        }
        private ResearchTableBlockEntity table() {
            require(level.getBlockEntity(owner) instanceof ResearchTableBlockEntity, "Owner replaced during fixture");
            return (ResearchTableBlockEntity) level.getBlockEntity(owner);
        }
        boolean advance() {
            require(level.players().stream().noneMatch(p -> p.getBoundingBox().intersects(new AABB(owner).inflate(20))), "Player entered environment fixture");
            var table = table(); var saved = table.saveWithoutMetadata(); int count = saved.getInt("nextRecalc");
            // Commands and newly installed tickers can cross the pending-ticker queue boundary.
            // Start the strict 602-tick assertion on the first actual tick, with bounded setup time.
            if (!began && count == 0) { require(++warmup <= 100, "Table ticker did not start within100 server ticks"); return false; }
            began = true; elapsed++;
            require(count == (elapsed == 602 ? 0 : elapsed), "Natural tick counter mismatch at " + elapsed + ": " + count);
            require(table.bonuses().size() == TCAspects.catalog().all().size(), "Existing bonuses changed");
            var extension = (ResearchTableBlockEntity) level.getBlockEntity(mate);
            require(extension != null && extension.saveWithoutMetadata().getInt("nextRecalc") == 0 && extension.bonuses().isEmpty(), "Extension collected bonuses");
            if (elapsed == 301) {
                var replacement = new ResearchTableBlockEntity(owner, level.getBlockState(owner)); replacement.load(saved);
                level.removeBlockEntity(owner); level.setBlockEntity(replacement);
                require(table() == replacement && replacement.saveWithoutMetadata().getInt("nextRecalc") == 301, "Mid-cycle reload failed");
            }
            return elapsed == 602;
        }
        void close() {
            for (var pos : positions) {
                if (level.getBlockEntity(pos) instanceof ResearchTableBlockEntity table) {
                    table.items.setStackInSlot(0, net.minecraft.world.item.ItemStack.EMPTY);
                    table.items.setStackInSlot(1, net.minecraft.world.item.ItemStack.EMPTY);
                }
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
