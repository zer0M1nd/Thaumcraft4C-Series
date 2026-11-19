package tc4revival.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeHooks;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

public  class ExcavationFocusItem extends WandFocusItem {
    public static final net.minecraft.tags.TagKey<Block> FAST_MATERIAL=BlockTags.create(net.minecraft.resources.ResourceLocation.parse("thaumcraft:excavation_fast_material"));
    // Client and integrated-server players have equal network IDs, but independent work.
    private final Map<Player, Progress> progress = new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static  class Progress { BlockPos target; float work; long nextSound; }
    private boolean has(ItemStack focus, FocusUpgrade upgrade) { return upgradeLevel(focus, upgrade.id) > 0; }
    @Override public String sortingPrefix() { return "BE"; }
    @Override public int color(ItemStack focus) { return 409606; }
    @Override public Map<Aspect, Integer> costs(ItemStack focus) {
        var catalog = TCAspects.catalog();
        if (has(focus, FocusUpgrade.SILK_TOUCH) || has(focus, FocusUpgrade.DOWSING)) {
            return has(focus, FocusUpgrade.SILK_TOUCH)
                    ? Map.of(catalog.require("aer"), 1, catalog.require("ignis"), 1, catalog.require("aqua"), 1,
                            catalog.require("terra"), 16, catalog.require("ordo"), 1, catalog.require("perditio"), 1)
                    : Map.of(catalog.require("ignis"), 2, catalog.require("ordo"), 2, catalog.require("terra"), 15);
        }
        return Map.of(catalog.require("terra"), 15);
    }
    @Override public boolean costPerTick(ItemStack focus) { return true; }
    @Override public Animation animation(ItemStack focus) { return Animation.CHARGE; }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank) {
        return switch (rank) {
            case 1 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.TREASURE);
            case 2, 4 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.ENLARGE);
            case 3 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.TREASURE, FocusUpgrade.DOWSING);
            case 5 -> List.of(FocusUpgrade.FRUGAL, FocusUpgrade.POTENCY, FocusUpgrade.TREASURE, FocusUpgrade.SILK_TOUCH);
            default -> List.of();
        };
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        player.startUsingItem(hand); return InteractionResultHolder.consume(stack);
    }
    public static BlockHitResult target(Player player) {
        var eye = player.getEyePosition();
        return player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(10)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }
    @Override public void usingTick(ItemStack stack, Player player, int remaining) {
        var wand = (CastingWandItem) stack.getItem();
        if (!wand.consumeForPlayer(stack, player, costs(attached(stack)), false, false, false)) { player.stopUsingItem(); return; }
        var level = player.level(); var hit = target(player);
        if (level.isClientSide) tc4revival.client.ContinuousFocusBeam.update(player, hit.getType() == HitResult.Type.BLOCK ? hit.getLocation()
                : player.getEyePosition().add(player.getLookAngle().scale(10)), 2, 65382, hit.getType() == HitResult.Type.BLOCK ? 2 : 0, false);
        var state = progress.computeIfAbsent(player, ignored -> new Progress());
        if (hit.getType() != HitResult.Type.BLOCK || !level.mayInteract(player, hit.getBlockPos())) { reset(player, state); state.nextSound = 0; return; }
        var position = hit.getBlockPos(); var block = level.getBlockState(position);
        long now = level.getGameTime();
        if (!level.isClientSide && state.nextSound < now) {
            level.playSound(null, position, TCSounds.RUMBLE.get(), SoundSource.PLAYERS, .3f, 1); state.nextSound = now + 24;
        }
        float hardness = block.getDestroySpeed(level, position);
        if (hardness < 0) return;
        if (!position.equals(state.target)) { reset(player, state); state.target = position.immutable(); return; }
        float potency = wand.focusPotency(stack, player);
        float speed = block.is(FAST_MATERIAL) ? .25f + potency * .25f : .05f + potency * .1f;
        speed=(float)thaumcraft.api.wands.SpellParameters.scale("excavation.speed",speed);
        if (block.is(Blocks.OBSIDIAN)) speed *= 3;
        if (level.isClientSide && state.work > 0 && !block.isAir()) level.destroyBlockProgress(player.getId(), position, hardness == 0 ? 9 : (int) (state.work / hardness * 9));
        if (state.work < hardness) { state.work += speed; return; }
        if (player instanceof ServerPlayer server && wand.consumeForPlayer(stack, player, costs(attached(stack)), false, false, true)) {
            if (excavate(server, stack, position, block)) {
                for (int i = 0; i < wand.focusUpgrade(stack, FocusUpgrade.ENLARGE); i++) {
                    if (!wand.consumeForPlayer(stack, player, costs(attached(stack)), false, false, false)) continue;
                    var directions = new ArrayList<>(List.of(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
                    for (int j = directions.size() - 1; j > 0; j--) java.util.Collections.swap(directions, j, level.random.nextInt(j + 1));
                    for (var direction : directions) {
                        var neighbour = position.relative(direction);
                        if (level.hasChunkAt(neighbour) && level.getBlockState(neighbour).equals(block) && excavate(server, stack, neighbour, block)) {
                            wand.consumeForPlayer(stack, player, costs(attached(stack)), false, false, true); break;
                        }
                    }
                }
            }
            reset(player, state);
        } else if (level.isClientSide) state.work = 0;
    }
    private boolean excavate(ServerPlayer player, ItemStack wand, BlockPos position, BlockState expected) {
        var level = player.serverLevel();
        if (!level.hasChunkAt(position) || !level.getBlockState(position).equals(expected) || !level.mayInteract(player, position)
                || player.blockActionRestricted(level, position, player.gameMode.getGameModeForPlayer())) return false;
        if (ForgeHooks.onBlockBreakEvent(level, player.gameMode.getGameModeForPlayer(), player, position) < 0) return false;
        var focus = attached(wand); int fortune = upgradeLevel(focus, FocusUpgrade.TREASURE.id);
        boolean silk = has(focus, FocusUpgrade.SILK_TOUCH);
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (silk) tool.enchant(Enchantments.SILK_TOUCH, 1);
        if (fortune > 0) tool.enchant(Enchantments.BLOCK_FORTUNE, fortune);
        var drops = Block.getDrops(expected, level, position, level.getBlockEntity(position), player, tool);
        boolean dowsing = !silk && has(focus, FocusUpgrade.DOWSING);
        for (var drop : drops) {
            var result = dowsing ? SpecialMining.apply(drop, fortune, level.random) : drop;
            if (result.getItem() != drop.getItem()) level.playSound(null, position, net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, .2f, .7f + level.random.nextFloat() * .2f);
            Block.popResource(level, position, result);
        }
        if (!silk) expected.getBlock().popExperience(level, position, expected.getExpDrop(level, level.random, position, fortune, 0));
        level.removeBlock(position, false); level.levelEvent(2001, position, Block.getId(expected)); return true;
    }
    private void reset(Player player, Progress state) {
        if (state.target != null && player.level().isClientSide) player.level().destroyBlockProgress(player.getId(), state.target, -1);
        state.target = null; state.work = 0;
    }
    @Override public void release(ItemStack wand, Level level, Player player, int remaining) {
        var state = progress.remove(player); if (state != null) reset(player, state);
        if (level.isClientSide) tc4revival.client.ContinuousFocusBeam.release(player);
    }
}
