package tc4revival.debug;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.ArgumentBuilder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import tc4revival.TC4Revival;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.entity.EntityAspects;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.research.KnowledgeNbt;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.PlayerKnowledge;
import tc4revival.research.PlayerKnowledge.ScanKind;
import tc4revival.research.PlayerKnowledge.ScanMode;
import tc4revival.research.ScanProgress;
import tc4revival.research.ScanRules;
import tc4revival.research.ScanTarget;
import tc4revival.research.ScanTargetResolver;
import tc4revival.research.ScanTargetSelector;
import tc4revival.research.ServerScanSession;

/** Console-only verification uses real server ticks and a separate, named fake-player knowledge record. */
public  class ScanSessionDebugCommands {
    private static final UUID FIXTURE = UUID.nameUUIDFromBytes("tc4revival:validation:scan_session:v1".getBytes(StandardCharsets.UTF_8));
    private static final Map<MinecraftServer, Fixture> RUNNING = new HashMap<>();
    private ScanSessionDebugCommands() {}

    static ArgumentBuilder<CommandSourceStack, ?> tree() {
        return Commands.literal("scan_session")
                .then(Commands.literal("verify").executes(context -> {
                    var source = context.getSource();
                    if (RUNNING.containsKey(source.getServer())) throw new IllegalStateException("Scan fixture already running");
                    var fixture = new Fixture(source);
                    try {
                        fixture.prepare();
                        RUNNING.put(source.getServer(), fixture);
                        source.sendSuccess(() -> Component.literal("Scan session fixture started; using actual server ticks, no client: " + FIXTURE), false);
                    } catch (RuntimeException failure) {
                        fixture.close();
                        throw failure;
                    }
                    return 1;
                }))
                .then(Commands.literal("verify_saved").executes(context -> {
                    verifySaved(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal("Scan session saved knowledge verified: " + FIXTURE
                            + "; exactly the glass, iron nugget and sand object scans; no rejected or interrupted scan rewards."), false);
                    return 1;
                }));
    }

    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var fixture = RUNNING.get(event.getServer());
        if (fixture == null) return;
        try {
            if (!fixture.advance()) return;
            fixture.source.sendSuccess(() -> Component.literal("Scan session checks passed: 9 timed scenarios; same-tick replay, deferred rejections, "
                    + "block continuity, live dropped-stack change, changed entity, release, missed tick and death; grouped NBT and entity identities checked."), false);
        } catch (RuntimeException failure) {
            TC4Revival.LOGGER.error("Scan session fixture failed", failure);
            fixture.source.sendFailure(Component.literal("Scan session fixture failed: " + failure.getMessage()));
        }
        fixture.close();
        RUNNING.remove(event.getServer());
    }
    public static void stopped(ServerStoppedEvent event) { RUNNING.remove(event.getServer()); }
    public static void stopping(ServerStoppingEvent event) {
        var fixture = RUNNING.remove(event.getServer());
        if (fixture != null) fixture.close();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
    private static void verifySaved(MinecraftServer server) {
        var expected = new PlayerKnowledge(TCAspects.catalog());
        for (var item : new net.minecraft.world.item.Item[] { Items.GLASS, Items.IRON_NUGGET, Items.SAND }) {
            var stack = new ItemStack(item);
            require(ScanRules.apply(expected, TCAspects.catalog(), ScanKind.OBJECT,
                    Integer.toString(ItemStackAspects.scanHash(stack).orElseThrow()), ScanMode.THAUMOMETER,
                    ItemStackAspects.resolve(stack).orElseThrow(), 100).succeeded(), "Expected fixture target cannot scan");
        }
        var actual = KnowledgeSavedData.get(server).snapshot(FIXTURE).orElseThrow();
        require(KnowledgeNbt.write(expected).equals(KnowledgeNbt.write(actual)), "Persisted scan fixture differs: " + KnowledgeNbt.write(actual));
    }

    private static  class Fixture {
        private final CommandSourceStack source;
        private final ServerLevel level;
        private final BlockPos first = new BlockPos(0, 200, 0);
        private final BlockPos second = new BlockPos(1, 200, 0);
        private FakePlayer player;
        private ItemEntity dropped;
        private ItemEntity other;
        private ServerScanSession session;
        private boolean placed;
        private int scenario = -1;
        private int elapsed;
        private long started;
        private CompoundTag before;
        private Fixture(CommandSourceStack source) { this.source = source; level = source.getServer().overworld(); }

        private void prepare() {
            require(level.hasChunkAt(first) && level.hasChunkAt(second), "Fixture chunks must already be loaded");
            require(level.getBlockState(first).isAir() && level.getBlockState(second).isAir(), "Fixture positions 0/1 200 0 must be empty");
            placed = true;
            level.setBlockAndUpdate(first, Blocks.GLASS.defaultBlockState());
            level.setBlockAndUpdate(second, Blocks.GLASS.defaultBlockState());
            player = FakePlayerFactory.get(level, new GameProfile(FIXTURE, "TC4ScanFixture"));
            player.setHealth(player.getMaxHealth());
            player.setPos(0.5, 200, 3.5);
            dropped = new ItemEntity(level, 0.5, 204, 0.5, new ItemStack(Items.IRON_NUGGET));
            other = new ItemEntity(level, 1.5, 204, 0.5, new ItemStack(Items.STONE));
            dropped.setNoGravity(true);
            other.setNoGravity(true);
            dropped.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            other.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            require(level.addFreshEntity(dropped) && level.addFreshEntity(other), "Cannot create fixture entities");
            KnowledgeSavedData.get(source.getServer()).update(FIXTURE, bound -> 0, knowledge -> {
                knowledge.wipe();
                knowledge.setWarpCounter(0);
                return true;
            });
            verifyTargets();
        }

        private void verifyTargets() {
            var one = ScanTargetResolver.block(level, first).orElseThrow();
            var two = ScanTargetResolver.block(level, second).orElseThrow();
            require(one.continuity().equals(two.continuity()), "Glass coordinates must not enter continuity identity");
            require(ScanTargetResolver.block(level, new BlockPos(20_000_000, 200, 20_000_000)).isEmpty(), "Unloaded target accepted");
            require(!level.hasChunkAt(new BlockPos(20_000_000, 200, 20_000_000)), "Resolution loaded a distant chunk");
            var original = new ItemStack(Items.DAMAGED_ANVIL, 12);
            var enchantment = new CompoundTag();
            enchantment.putString("id", "minecraft:sharpness");
            enchantment.putShort("lvl", (short) 7);
            var enchantments = new ListTag();
            enchantments.add(enchantment);
            original.getOrCreateTag().put("Enchantments", enchantments);
            dropped.setItem(original);
            var observed = ScanTargetResolver.entity(dropped).orElseThrow();
            var representative = new ItemStack(Items.ANVIL);
            representative.setTag(original.getTag().copy());
            require(observed.aspects().view().equals(ItemStackAspects.resolve(representative).orElseThrow().view()), "Grouped anvil bonus/NBT differs");
            require(dropped.getItem().is(Items.DAMAGED_ANVIL) && dropped.getItem().getCount() == 12, "Scanner changed original stack");
            require(observed.kind() == ScanKind.OBJECT && observed.continuity().equals(new ScanTarget.Entity(dropped.getUUID())), "Dropped item classification differs");
            var pig = EntityType.PIG.create(level);
            try {
                var target = ScanTargetResolver.entity(pig).orElseThrow();
                require(target.kind() == ScanKind.ENTITY && target.identity().orElseThrow().equals(Integer.toString(EntityAspects.resolve(pig).orElseThrow().scanHash())), "Pig identity differs");
            } finally { pig.discard(); }
            dropped.setItem(new ItemStack(Items.ECHO_SHARD));
            var modern = ScanTargetResolver.entity(dropped).orElseThrow();
            require(modern.identity().isEmpty() && modern.aspects().isEmpty(), "New item received old aspects");
        }

        private boolean advance() {
            if (scenario < 0) { next(); return false; }
            elapsed = Math.toIntExact(level.getGameTime() - started);
            if (scenario == 2) aimAtGlass();
            if (scenario == 4 && elapsed == 10) dropped.setItem(new ItemStack(Items.SAND, 64));
            if (scenario == 6 && elapsed == 10) session.cancel();
            if (scenario == 7 && elapsed == 5) return false;
            if (scenario == 8 && elapsed == 10) player.setHealth(0);
            var update = session.tick();
            int end = scenario < 5 ? 20 : scenario == 7 ? 6 : 10;
            if (elapsed < end) {
                require(update.state() == ScanProgress.State.TRACKING && update.result().isEmpty(), "Early terminal state in scenario " + scenario + " at " + elapsed + ": " + update);
                require(before.equals(saved()), "Knowledge changed before completion");
                for (int i = 0; i < 30; i++) require(session.tick().state() == ScanProgress.State.TRACKING, "Same-tick replay advanced scan");
                return false;
            }
            if (scenario < 5) {
                var expected = scenario == 0 ? ScanRules.Status.MISSING_PARENT : scenario == 1 ? ScanRules.Status.NO_ASPECTS : ScanRules.Status.SUCCESS;
                require(update.state() == ScanProgress.State.COMPLETED && update.result().orElseThrow().status() == expected,
                        "Unexpected completion for scenario " + scenario + ": " + update);
                if (scenario < 2) require(before.equals(saved()), "Rejected scan changed knowledge");
            } else {
                var expected = scenario == 6 ? ScanProgress.State.IDLE : ScanProgress.State.INTERRUPTED;
                require(update.state() == expected && update.result().isEmpty(), "Interruption differed for scenario " + scenario + ": " + update);
                require(before.equals(saved()), "Interrupted scan rewarded knowledge");
            }
            var completed = saved();
            require(session.tick().state() == ScanProgress.State.IDLE && completed.equals(saved()), "Completion replay rewarded twice");
            TC4Revival.LOGGER.info("Scan session fixture scenario {} passed at {} ticks: {}", scenario, elapsed, update);
            if (scenario == 8) { verifySaved(source.getServer()); return true; }
            next();
            return false;
        }

        private void next() {
            scenario++;
            elapsed = 0;
            player.setHealth(player.getMaxHealth());
            if (scenario == 3) {
                var repeated = new ServerScanSession(player, () -> ScanTargetResolver.block(level, first));
                require(repeated.begin() == ScanProgress.State.IDLE, "Already-scanned glass started another scan");
            }
            dropped.setItem(new ItemStack(switch (scenario) {
                case 0, 3 -> Items.IRON_NUGGET;
                case 1, 4 -> Items.FILLED_MAP;
                default -> Items.STONE;
            }));
            Supplier<Optional<ScanTarget>> resolver = () -> ScanTargetResolver.entity(scenario == 5 && elapsed >= 10 ? other : dropped);
            if (scenario == 2) aimAtGlass();
            session = scenario == 2 ? new ServerScanSession(player) : new ServerScanSession(player, resolver);
            started = level.getGameTime();
            before = saved();
            require(session.begin() == ScanProgress.State.TRACKING, "Scenario cannot start: " + scenario);
            for (int i = 0; i < 30; i++) require(session.tick().state() == ScanProgress.State.TRACKING, "Start-tick replay advanced scan");
        }

        private CompoundTag saved() { return KnowledgeNbt.write(KnowledgeSavedData.get(source.getServer()).snapshot(FIXTURE).orElseThrow()); }
        private void aimAtGlass() {
            var position = (elapsed & 1) == 0 ? first : second;
            var direction = net.minecraft.world.phys.Vec3.atCenterOf(position).subtract(player.getEyePosition());
            player.setYRot((float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90);
            player.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z))));
            require(ScanTargetSelector.resolve(player).orElseThrow().continuity() instanceof ScanTarget.Block, "Fixture look direction missed glass");
        }
        private void close() {
            if (session != null) session.cancel();
            if (player != null) player.setHealth(player.getMaxHealth());
            if (dropped != null) dropped.discard();
            if (other != null) other.discard();
            if (placed) {
                level.setBlockAndUpdate(first, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
            }
        }
    }
}
