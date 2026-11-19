package tc4revival.debug;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.ArgumentBuilder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import tc4revival.TC4Revival;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.content.ShardType;
import tc4revival.content.TCItems;
import tc4revival.research.KnowledgeNbt;
import tc4revival.research.KnowledgeSavedData;
import tc4revival.research.ScanTargetSelector;
import tc4revival.research.ThaumometerUse;

/** Retained actual-world recipe and item-callback fixture. This does not replace physical client testing. */
public  class ThaumometerDebugCommands {
    private static final UUID ID = UUID.nameUUIDFromBytes("tc4revival:validation:thaumometer:v1".getBytes(StandardCharsets.UTF_8));
    private static final Map<MinecraftServer, Fixture> RUNNING = new HashMap<>();
    private ThaumometerDebugCommands() {}
    static ArgumentBuilder<CommandSourceStack, ?> tree() {
        return Commands.literal("thaumometer").then(Commands.literal("verify").executes(context -> {
            var source = context.getSource();
            require(!RUNNING.containsKey(source.getServer()), "Thaumometer fixture already running");
            verifyRecipes(source);
            var fixture = new Fixture(source);
            try { fixture.prepare(); RUNNING.put(source.getServer(), fixture); }
            catch (RuntimeException failure) { fixture.close(); throw failure; }
            source.sendSuccess(() -> Component.literal("Thaumometer recipe checks passed: all 49 shard pairs, material rejection, stack/rarity/hash. Starting 4 timed item-use scenarios: " + ID), false);
            return 1;
        }));
    }
    private static void verifyRecipes(CommandSourceStack source) {
        var tool = new ItemStack(TCItems.THAUMOMETER.get());
        require(tool.getMaxStackSize() == 1 && !tool.isDamageableItem() && tool.getRarity() == Rarity.UNCOMMON, "Tool properties differ");
        require(ItemStackAspects.scanHash(tool).orElseThrow() == 1287147729, "Thaumometer scan hash differs");
        var menu = new AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 3, 3);
        grid.setItem(3, new ItemStack(Items.GOLD_INGOT));
        grid.setItem(4, new ItemStack(Items.GLASS));
        grid.setItem(5, new ItemStack(Items.GOLD_INGOT));
        var level = source.getServer().overworld();
        for (var top : ShardType.values()) for (var bottom : ShardType.values()) {
            grid.setItem(1, new ItemStack(TCItems.shard(top)));
            grid.setItem(7, new ItemStack(TCItems.shard(bottom)));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            var result = recipe.assemble(grid, level.registryAccess());
            require(result.is(TCItems.THAUMOMETER.get()) && result.getCount() == 1, "Shard recipe pair differs: " + top + "/" + bottom);
        }
        for (var invalid : new net.minecraft.world.item.Item[]{Items.GOLD_NUGGET, Items.IRON_INGOT}) {
            grid.setItem(3, new ItemStack(invalid));
            require(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(), "Incorrect gold replacement accepted");
        }
        grid.setItem(3, new ItemStack(Items.GOLD_INGOT));
        grid.setItem(4, new ItemStack(Items.GLASS_PANE));
        require(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(), "Glass pane accepted as glass block");
    }
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var fixture = RUNNING.get(event.getServer());
        if (fixture == null) return;
        try {
            if (!fixture.advance()) return;
            fixture.source.sendSuccess(() -> Component.literal("Thaumometer item-use checks passed: main-hand 20-tick completion, early release, stack replacement, off-hand completion; exactly two scans; no early rewards."), false);
        } catch (RuntimeException failure) {
            TC4Revival.LOGGER.error("Thaumometer item-use fixture failed", failure);
            fixture.source.sendFailure(Component.literal("Thaumometer fixture failed: " + failure.getMessage()));
        }
        fixture.close(); RUNNING.remove(event.getServer());
    }
    public static void stopping(ServerStoppingEvent event) {
        var fixture = RUNNING.remove(event.getServer());
        if (fixture != null) fixture.close();
    }
    public static void stopped(ServerStoppedEvent event) { RUNNING.remove(event.getServer()); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static  class Fixture {
        private final CommandSourceStack source;
        private final BlockPos target = new BlockPos(8, 201, 5);
        private FakePlayer player;
        private boolean placed;
        private int scenario = -1;
        private long started;
        private CompoundTag before;
        private ItemStack stack;
        Fixture(CommandSourceStack source) { this.source = source; }
        void prepare() {
            var level = source.getServer().overworld();
            require(level.getBlockState(target).isAir() && level.getBlockEntity(target) == null, "Thaumometer test position is occupied");
            player = FakePlayerFactory.get(level, new GameProfile(ID, "TC4ToolFixture"));
            player.setHealth(player.getMaxHealth());
            player.setPos(8.5, 200, 8.5);
            player.setYRot(180); player.setXRot(0);
            KnowledgeSavedData.get(source.getServer()).update(ID, bound -> 0, knowledge -> { knowledge.wipe(); knowledge.setWarpCounter(0); return true; });
        }
        CompoundTag knowledge() { return KnowledgeNbt.write(KnowledgeSavedData.get(source.getServer()).snapshot(ID).orElseThrow()); }
        boolean advance() {
            var level = source.getServer().overworld();
            if (scenario == -1) { start(0); return false; }
            int elapsed = (int) (level.getGameTime() - started);
            if (elapsed == 10 && scenario == 1) player.releaseUsingItem();
            if (elapsed == 10 && scenario == 2) player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TCItems.THAUMOMETER.get()));
            stack.getItem().onUseTick(level, player, stack, 25 - elapsed);
            if (elapsed < 20 || scenario == 1 || scenario == 2) require(before.equals(knowledge()), "Early or interrupted use awarded knowledge in scenario " + scenario + " at " + elapsed);
            if (elapsed < 22) return false;
            if (scenario == 0 || scenario == 3) {
                require(!before.equals(knowledge()), "Completed use did not award knowledge in scenario " + scenario);
                require(!player.isUsingItem(), "Completed use did not stop item use");
            }
            if (scenario == 3) {
                var actual = KnowledgeSavedData.get(source.getServer()).snapshot(ID).orElseThrow();
                require(actual.scannedKeys(tc4revival.research.PlayerKnowledge.ScanKind.OBJECT).size() == 2, "Unexpected scan count");
                return true;
            }
            start(scenario + 1); return false;
        }
        void start(int next) {
            player.stopUsingItem(); ThaumometerUse.cancel(player);
            scenario = next;
            var level = source.getServer().overworld();
            var block = switch (next) { case 0 -> Blocks.GLASS; case 1 -> Blocks.IRON_BLOCK; default -> Blocks.STONE; };
            level.setBlockAndUpdate(target, block.defaultBlockState()); placed = true;
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            var hand = next == 3 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            stack = new ItemStack(TCItems.THAUMOMETER.get());
            player.setItemInHand(hand, stack);
            var selected = ScanTargetSelector.resolve(player).orElseThrow(() -> new IllegalStateException("Fixture cannot aim at target"));
            if (next == 0 || next == 3) {
                var preview = KnowledgeSavedData.get(source.getServer()).snapshot(ID).orElseThrow();
                var result = selected.complete(preview, tc4revival.aspect.TCAspects.catalog(),
                        tc4revival.research.PlayerKnowledge.ScanMode.THAUMOMETER, 100);
                require(result.succeeded(), "Fixture completion target has unmet prerequisites: " + result.status());
            }
            before = knowledge(); started = level.getGameTime();
            stack.getItem().use(level, player, hand);
        }
        void close() {
            if (player != null) {
                ThaumometerUse.cancel(player); player.stopUsingItem();
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            }
            if (placed) source.getServer().overworld().setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        }
    }
}
