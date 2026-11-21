package tc4revival.content;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;
import tc4revival.config.TCConfig;

public  class PortableHoleFocusItem extends WandFocusItem {
    public static final TagKey<Block> BLACKLIST = TagKey.create(Registries.BLOCK, ResourceLocation.parse("thaumcraft:portable_hole_blacklist"));
    @Override public String sortingPrefix() { return "BPH"; }
    @Override public String depthTexture() { return "focus_portablehole_depth"; }
    @Override public int color(ItemStack focus) { return 594985; }
    @Override public Map<Aspect, Integer> costs(ItemStack focus) { return Map.of(TCAspects.catalog().require("perditio"), 10, TCAspects.catalog().require("aer"), 10); }
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus, int rank) {
        return rank >= 1 && rank <= 5 ? List.of(FocusUpgrade.FRUGAL, FocusUpgrade.ENLARGE, FocusUpgrade.EXTEND) : List.of();
    }
    static boolean stops(Level level, BlockPos pos, BlockState state) {
        return level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || state.isAir() || state.is(Blocks.BEDROCK) || state.is(TCArcane.PORTABLE_HOLE.get())
                || state.is(BLACKLIST) || state.getDestroySpeed(level, pos) < 0
                || TCConfig.SERVER.portableHoleBlacklist.get().contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        if (level.dimension().location().equals(ResourceLocation.parse("thaumcraft:outer_lands"))) {
            if (!level.isClientSide) level.playSound(null, hit.getBlockPos(), TCSounds.WAND_FAIL.get(), SoundSource.PLAYERS, 1, 1);
            player.swing(hand); return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player instanceof ServerPlayer server) {
            var wand = (CastingWandItem) stack.getItem(); int distance = 0;
            var direction = hit.getDirection().getOpposite(); var pos = hit.getBlockPos();
            int maximum = thaumcraft.api.wands.SpellParameters.bounded("portable_hole.depth",33,512) + wand.focusUpgrade(stack, FocusUpgrade.ENLARGE) * 8;
            while (distance < maximum && level.hasChunkAt(pos) && !stops(level, pos, level.getBlockState(pos))) { distance++; pos = pos.relative(direction); }
            int cost = 10 * Math.max(1, distance);
            if (wand.consumeForPlayer(stack, player, Map.of(TCAspects.catalog().require("aer"), cost, TCAspects.catalog().require("perditio"), cost), false, false, true))
                PortableHoleBlockEntity.open(server, hit.getBlockPos(), hit.getDirection().get3DDataValue(), distance + 1,
                        thaumcraft.api.wands.SpellParameters.bounded("portable_hole.duration",120 + 60 * wand.focusUpgrade(stack, FocusUpgrade.EXTEND),12000));
            level.playSound(null, hit.getBlockPos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1, 1);
        }
        player.swing(hand); return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
