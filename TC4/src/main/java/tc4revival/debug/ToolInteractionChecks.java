package tc4revival.debug;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import tc4revival.content.*;

/** Batch integration regression for vanilla interactions that changed since 1.7.10. */
 class ToolInteractionChecks {
    private ToolInteractionChecks() {}
    static int run(CommandSourceStack source) {
        var level = source.getServer().overworld(); var pos = new BlockPos(8, 104, 8);
        if (!level.hasChunkAt(pos) || !level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())
                || !level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(16)).isEmpty()) {
            source.sendFailure(Component.literal("tools: isolated air fixture unavailable")); return 0;
        }
        var player = new FakePlayer(level, new GameProfile(UUID.fromString("06a20f02-c75d-4350-8dca-f585687db384"), "TC4ToolProbe"));
        player.getAbilities().instabuild = false;
        try {
            var pick = new ItemStack(TCItems.tool(ThaumiumToolType.PICKAXE));
            require(pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "Tier 3 failed obsidian harvest");
            require(!pick.isCorrectToolForDrops(Blocks.OAK_LOG.defaultBlockState()), "Pick accepted axe target");
            require(pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 7, "Pick mining speed");
            var hoe = new ItemStack(TCItems.tool(ThaumiumToolType.HOE));
            require(hoe.getDestroySpeed(Blocks.HAY_BLOCK.defaultBlockState()) == 1, "Modern hoe mining boost leaked");
            require(!hoe.isCorrectToolForDrops(Blocks.SCULK.defaultBlockState()), "Modern hoe harvest leaked");
            player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
            for (var block : new net.minecraft.world.level.block.Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL}) {
                level.setBlock(pos, block.defaultBlockState(), 3);
                int damage = hoe.getDamageValue();
                require(use(player, pos, Direction.UP).consumesAction() && level.getBlockState(pos).is(Blocks.FARMLAND)
                        && hoe.getDamageValue() == damage + 1, "Old dirt variant till/debit: " + block);
            }
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            int damage = hoe.getDamageValue();
            require(!use(player, pos, Direction.DOWN).consumesAction(), "Underside till accepted");
            level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
            require(!use(player, pos, Direction.UP).consumesAction() && hoe.getDamageValue() == damage, "Obstructed till consumed durability");
            level.removeBlock(pos.above(), false);
            for (boolean sneaking : new boolean[]{false, true}) {
                player.setShiftKeyDown(sneaking);
                for (var item : new net.minecraft.world.item.Item[]{TCItems.tool(ThaumiumToolType.AXE), TCItems.voidTool(VoidEquipment.Tool.AXE)}) {
                    transforms(player, pos, item, Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG);
                    transforms(player, pos, item, Blocks.EXPOSED_COPPER, Blocks.COPPER_BLOCK);
                    transforms(player, pos, item, Blocks.WAXED_COPPER_BLOCK, Blocks.COPPER_BLOCK);
                }
                for (var item : new net.minecraft.world.item.Item[]{TCItems.tool(ThaumiumToolType.SHOVEL), TCItems.voidTool(VoidEquipment.Tool.SHOVEL), TCItems.PRIMAL_CRUSHER.get()}) {
                    for (var block : new net.minecraft.world.level.block.Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.MYCELIUM, Blocks.ROOTED_DIRT})
                        transforms(player, pos, item, block, Blocks.DIRT_PATH);
                    var stack = new ItemStack(item); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    level.setBlock(pos, Blocks.CAMPFIRE.defaultBlockState(), 3);
                    require(use(player, pos, Direction.UP).consumesAction() && !level.getBlockState(pos).getValue(net.minecraft.world.level.block.CampfireBlock.LIT)
                            && stack.getDamageValue() == 1, "Campfire douse/debit: " + item);
                    level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                    require(!use(player, pos, Direction.DOWN).consumesAction(), "Underside flatten accepted: " + item);
                    level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
                    require(!use(player, pos, Direction.UP).consumesAction() && stack.getDamageValue() == 1, "Obstructed flatten/debit: " + item);
                    level.removeBlock(pos.above(), false);
                }
                for (var item : new net.minecraft.world.item.Item[]{TCItems.ELEMENTAL_AXE.get(), TCItems.ELEMENTAL_SHOVEL.get()}) {
                    var stack = new ItemStack(item); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                    for (var block : new net.minecraft.world.level.block.Block[]{Blocks.OAK_LOG, Blocks.EXPOSED_COPPER, Blocks.WAXED_COPPER_BLOCK, Blocks.GRASS_BLOCK, Blocks.CAMPFIRE}) {
                        level.setBlock(pos, block.defaultBlockState(), 3);
                        use(player, pos, Direction.UP);
                        require(level.getBlockState(pos).equals(block.defaultBlockState()) && stack.getDamageValue() == 0,
                                "Custom tool performed vanilla action: " + item);
                    }
                    require(!stack.canPerformAction(net.minecraftforge.common.ToolActions.AXE_STRIP)
                            && !stack.canPerformAction(net.minecraftforge.common.ToolActions.SHOVEL_FLATTEN), "Custom tool advertises vanilla action: " + item);
                }
            }
            source.sendSuccess(() -> Component.literal("tools PASS: tier/mining/hoe regression; thaumium/void axe strip/scrape/wax; thaumium/void/crusher path/douse/debit/rejections; custom elemental exclusions; ordinary and Shift; no real player/client test"), false);
            return 1;
        } catch (RuntimeException failure) {
            tc4revival.TC4Revival.LOGGER.error("Tool interaction regression failed", failure);
            source.sendFailure(Component.literal("tools FAILED: " + failure.getMessage())); return 0;
        } finally {
            level.removeBlock(pos.above(), false); level.removeBlock(pos, false); player.getInventory().clearContent();
        }
    }
    private static void transforms(FakePlayer player, BlockPos pos, net.minecraft.world.item.Item item,
                                   net.minecraft.world.level.block.Block before, net.minecraft.world.level.block.Block after) {
        var stack = new ItemStack(item); player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.level().setBlock(pos, before.defaultBlockState(), 3);
        require(use(player, pos, Direction.UP).consumesAction() && player.level().getBlockState(pos).is(after)
                && stack.getDamageValue() == 1, "Transform/debit: " + item + " / " + before);
    }
    private static net.minecraft.world.InteractionResult use(FakePlayer player, BlockPos pos, Direction face) {
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
        return player.getMainHandItem().getItem().useOn(context);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
