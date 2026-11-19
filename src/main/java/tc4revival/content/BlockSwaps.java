package tc4revival.content;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One successful trade per world tick; queued work rechecks its owner, source, inventory and focus. */
@Mod.EventBusSubscriber(modid = "thaumcraft")
public  class BlockSwaps {
    private static final Map<ServerLevel, Queue> QUEUES = new WeakHashMap<>();
    private record Entry(UUID player, int slot, BlockPos pos, BlockState source, ItemStack target, int life, thaumcraft.api.wands.WandCasting.Deferred cast) {}
    private record Key(UUID player, BlockPos pos) {}
    private static  class Queue {
        final ArrayDeque<Key> pending = new ArrayDeque<>();
        final Map<Key, Entry> scheduled = new java.util.HashMap<>();
        void add(Entry entry) {
            var key = new Key(entry.player, entry.pos);
            var previous = scheduled.get(key);
            if (previous != null) {
                // A predicted right-click sends the swing packet first. Its single-block request
                // must not swallow the following cast (or a stronger propagation route).
                if (previous.slot == entry.slot && previous.source.equals(entry.source)
                        && ItemStack.isSameItemSameTags(previous.target, entry.target) && entry.life > previous.life)
                    scheduled.put(key, entry);
                return;
            }
            if (pending.size() < 16384) { scheduled.put(key, entry); pending.add(key); }
        }
        Entry next() { var key = pending.poll(); return key == null ? null : scheduled.remove(key); }
    }
    private BlockSwaps() {}
    public static boolean exposed(Level level, BlockPos pos) {
        for (var direction : Direction.values()) {
            var next = pos.relative(direction);
            if (level.hasChunkAt(next) && !level.getBlockState(next).isSolidRender(level, next)) return true;
        }
        return false;
    }
    public static void enqueue(ServerPlayer player, InteractionHand hand, BlockPos pos, ItemStack target, int life) {
        var level = player.serverLevel(); if (!level.hasChunkAt(pos) || !(target.getItem() instanceof BlockItem blockItem)) return;
        var state = level.getBlockState(pos);
        if (!eligible(level, pos, state) || state.getBlock() == blockItem.getBlock()) return;
        int slot = hand == InteractionHand.OFF_HAND ? 40 : player.getInventory().selected;
        var single = target.copy(); single.setCount(1);
        QUEUES.computeIfAbsent(level, key -> new Queue()).add(new Entry(player.getUUID(), slot, pos.immutable(), state, single, life, thaumcraft.api.wands.WandCasting.defer(player,player.getItemInHand(hand),slot)));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TCSounds.WAND.get(), SoundSource.PLAYERS, .25f, 1);
    }
    private static boolean eligible(Level level, BlockPos pos, BlockState state) {
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return !state.isAir() && !state.hasBlockEntity() && level.getBlockEntity(pos) == null && state.getDestroySpeed(level, pos) >= 0
                && !(id.getNamespace().equals("thaumcraft") && id.getPath().startsWith("taint"));
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) { if (event.getLevel() instanceof ServerLevel level) QUEUES.remove(level); }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        process(level,id->level.getServer().getPlayerList().getPlayer(id));
    }
    /** Same queue execution with an explicit player lookup for isolated server validation. */
    static void process(ServerLevel level,java.util.function.Function<UUID,ServerPlayer> players) {
        var queue = QUEUES.get(level); if (queue == null) return;
        for (int skipped = 0; skipped < 4096; skipped++) {
            var entry = queue.next(); if (entry == null) { QUEUES.remove(level); return; }
            var player=players.apply(entry.player);
            if(player==null||player.getInventory().getItem(entry.slot)!=entry.cast.source())continue;
            try(var cast=thaumcraft.api.wands.WandCasting.open(player,entry.cast.source(),thaumcraft.api.wands.WandCasting.Action.REPLAY)){
                if(entry.cast.matches(player,cast.context())&&swap(level,queue,entry,player))return;
            }
        }
    }
    private static boolean swap(ServerLevel level, Queue queue, Entry entry,ServerPlayer player) {
        if (player == null || player.level() != level || !player.isAlive() || player.isSpectator() || !level.hasChunkAt(entry.pos)) return false;
        var wandStack = player.getInventory().getItem(entry.slot);
        if (!(wandStack.getItem() instanceof CastingWandItem wand)) return false;
        var focusStack = WandFocusItem.attached(wandStack);
        if (!(focusStack.getItem() instanceof TradeFocusItem focus)) return false;
        var state = level.getBlockState(entry.pos);
        if (!state.equals(entry.source) || !eligible(level, entry.pos, state) || !level.mayInteract(player, entry.pos)
                || player.blockActionRestricted(level, entry.pos, player.gameMode.getGameModeForPlayer())) return false;
        var hand = entry.slot == 40 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        var hit = new BlockHitResult(Vec3.atCenterOf(entry.pos), Direction.UP, entry.pos, false);
        if (net.minecraftforge.common.ForgeHooks.onRightClickBlock(player,hand,entry.pos,hit).isCanceled()) return false;
        int materialSlot = -1;
        for (int i = 0; i < 36; i++) if (player.getInventory().getItem(i).is(entry.target.getItem()) && !player.getInventory().getItem(i).isEmpty()) { materialSlot = i; break; }
        if (materialSlot < 0 && !player.isCreative() || !wand.consumeForPlayer(wandStack, player, focus.costs(focusStack), false, false, false)) return false;
        if (!(entry.target.getItem() instanceof BlockItem target)) return false;
        var context = new BlockPlaceContext(player, hand, entry.target, hit) {
            @Override public BlockPos getClickedPos() { return entry.pos; }
            @Override public boolean replacingClickedOnBlock() { return true; }
        };
        var replacement = target.getBlock().getStateForPlacement(context);
        if (replacement == null || replacement.hasBlockEntity() || replacement.equals(state)) return false;
        if (net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level, player.gameMode.getGameModeForPlayer(), player, entry.pos) < 0) return false;
        int fortune = focus.upgradeLevel(focusStack, FocusUpgrade.TREASURE.id); boolean silk = focus.upgradeLevel(focusStack, FocusUpgrade.SILK_TOUCH.id) > 0;
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (fortune > 0) tool.enchant(Enchantments.BLOCK_FORTUNE, fortune);
        if (silk) tool.enchant(Enchantments.SILK_TOUCH, 1);
        var drops = player.isCreative() ? java.util.List.<ItemStack>of() : Block.getDrops(state, level, entry.pos, null, player, tool);
        // A rejected replacement must not consume material or return the old drops.
        if (!level.setBlockAndUpdate(entry.pos, replacement)) return false;
        if(!player.isCreative()&&!wand.consumeForPlayer(wandStack,player,focus.costs(focusStack),false,false,true)){
            level.setBlockAndUpdate(entry.pos,state);return false;
        }
        target.getBlock().setPlacedBy(level, entry.pos, replacement, player, entry.target);
        if (!player.isCreative()) {
            player.getInventory().getItem(materialSlot).shrink(1);
            for (var drop : drops) if (!player.getInventory().add(drop)) Block.popResource(level, entry.pos, drop);
            player.getInventory().setChanged();
        }
        level.levelEvent(2001, entry.pos, Block.getId(state));
        tc4revival.network.FocusEffectPacket.blockSparkle(level, entry.pos, 0xc0c0ff);
        if (entry.life > 0) for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            var next = entry.pos.offset(x, y, z);
            if (level.hasChunkAt(next) && level.getBlockState(next).equals(entry.source) && exposed(level, next))
                queue.add(new Entry(entry.player, entry.slot, next, entry.source, entry.target, entry.life - 1,entry.cast));
        }
        return true;
    }
}
