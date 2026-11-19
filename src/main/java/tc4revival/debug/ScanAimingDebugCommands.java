package tc4revival.debug;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.ArgumentBuilder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.research.PlayerKnowledge;
import tc4revival.research.PlayerKnowledge.ScanMode;
import tc4revival.research.ScanTarget;
import tc4revival.research.ScanTargetSelector;

 class ScanAimingDebugCommands {
    private static final UUID FIXTURE = UUID.nameUUIDFromBytes("tc4revival:validation:scan_aiming:v1".getBytes(StandardCharsets.UTF_8));
    private ScanAimingDebugCommands() {}
    static ArgumentBuilder<CommandSourceStack, ?> tree() {
        return Commands.literal("scan_aiming").then(Commands.literal("verify").executes(context -> verify(context.getSource())));
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static String hash(net.minecraft.world.item.Item item) { return Integer.toString(ItemStackAspects.scanHash(new ItemStack(item)).orElseThrow()); }

    private static int verify(CommandSourceStack source) {
        var level = source.getServer().overworld();
        var positions = new ArrayList<BlockPos>();
        for (int x = 0; x <= 12; x++) for (int y = 200; y <= 202; y++) {
            var position = new BlockPos(x, y, 0);
            require(level.hasChunkAt(position) && level.getBlockState(position).isAir(), "Aiming fixture requires loaded air: " + position);
            positions.add(position);
        }
        var entities = new ArrayList<Entity>();
        var player = FakePlayerFactory.get(level, new GameProfile(FIXTURE, "TC4AimFixture"));
        player.setHealth(player.getMaxHealth());
        player.setPos(0.5, 200, 0.5);
        player.setYRot(-90);
        player.setYHeadRot(90); // Deliberate mismatch: aiming must use the authoritative yaw, not head rendering.
        player.setXRot(0);
        var block = new BlockPos(3, 201, 0);
        var distant = new BlockPos(6, 201, 0);
        var boundary = new BlockPos(5, 201, 0);
        try {
            level.setBlock(block, Blocks.GLASS.defaultBlockState(), 2);
            require(ScanTargetSelector.resolve(player).orElseThrow().identity().orElseThrow().equals(hash(Items.GLASS)), "Nearby glass not selected");
            level.setBlock(block, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(distant, Blocks.GLASS.defaultBlockState(), 2);
            require(ScanTargetSelector.resolve(player).isEmpty(), "Block beyond five blocks selected");
            level.setBlock(distant, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(boundary, Blocks.GLASS.defaultBlockState(), 2);
            require(ScanTargetSelector.resolve(player).isPresent(), "Block within five-block range missed");
            player.setPos(0, 200, 0.5);
            require(ScanTargetSelector.resolve(player).isPresent(), "Exact five-block endpoint missed");
            player.setPos(-0.001, 200, 0.5);
            require(ScanTargetSelector.resolve(player).isEmpty(), "Block just outside five-block range selected");
            player.setPos(0.5, 200.35, 0.5);
            level.setBlock(boundary, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(block, Blocks.WATER.defaultBlockState(), 2);
            require(ScanTargetSelector.resolve(player).orElseThrow().continuity().equals(new ScanTarget.Block("minecraft:water", 0)), "Source water full-height bounds missed");
            level.setBlock(block, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1), 2);
            require(ScanTargetSelector.resolve(player).isEmpty(), "Flowing water was targeted");
            level.setBlock(block, Blocks.LAVA.defaultBlockState(), 2);
            require(ScanTargetSelector.resolve(player).orElseThrow().continuity().equals(new ScanTarget.Block("minecraft:lava", 0)), "Source lava missed");
            level.setBlock(block, Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 1), 2);
            require(ScanTargetSelector.resolve(player).isEmpty(), "Flowing lava was targeted");

            player.setPos(0.5, 200, 0.5);
            var dropped = new ItemEntity(level, 5.5, 201.5, 0.5, new ItemStack(Items.IRON_NUGGET));
            dropped.setNoGravity(true);
            dropped.setDeltaMovement(Vec3.ZERO);
            entities.add(dropped);
            require(level.addFreshEntity(dropped), "Could not create aiming item entity");
            level.setBlock(block, Blocks.WATER.defaultBlockState(), 2);
            require(ScanTargetSelector.pointedEntity(player).orElseThrow() == dropped, "Water incorrectly occluded entity LOS");
            require(ScanTargetSelector.resolve(player).orElseThrow().identity().orElseThrow().equals(hash(Items.IRON_NUGGET)), "Entity did not take precedence over water");
            level.setBlock(block, Blocks.STONE.defaultBlockState(), 2);
            require(ScanTargetSelector.pointedEntity(player).isEmpty(), "Entity behind solid wall selected");
            require(ScanTargetSelector.resolve(player).orElseThrow().identity().orElseThrow().equals(hash(Items.STONE)), "Occluding block not selected");
            level.setBlock(block.below(), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(block, Blocks.TORCH.defaultBlockState(), 2);
            require(ScanTargetSelector.pointedEntity(player).orElseThrow() == dropped, "Noncolliding torch occluded entity LOS");
            level.setBlock(block, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(block.below(), Blocks.AIR.defaultBlockState(), 2);
            dropped.setPos(8.5, 201.5, 0.5);
            require(ScanTargetSelector.pointedEntity(player).orElseThrow() == dropped, "Entity beyond block reach but within entity reach missed");
            dropped.setPos(12, 201.5, 0.5);
            require(ScanTargetSelector.pointedEntity(player).isEmpty(), "Entity beyond ray selected");
            dropped.setPos(8.5, 201.5, 1);
            require(ScanTargetSelector.pointedEntity(player).isEmpty(), "Broad phase was incorrectly expanded by pick radius");
            dropped.setPos(3.5, 201.5, 0.5);
            var farther = new ItemEntity(level, 8.5, 201.5, 0.5, new ItemStack(Items.SAND));
            entities.add(farther);
            require(level.addFreshEntity(farther), "Could not create second aiming entity");
            require(ScanTargetSelector.pointedEntity(player).orElseThrow() == dropped, "Nearer entity did not win");
            level.setBlock(boundary, Blocks.GLASS.defaultBlockState(), 2);
            var selected = ScanTargetSelector.resolve(player).orElseThrow();
            var knowledge = new PlayerKnowledge(TCAspects.catalog());
            knowledge.rememberScan(selected.kind(), ScanMode.THAUMOMETER.key(selected.identity().orElseThrow()));
            require(!selected.canTrack(knowledge, ScanMode.THAUMOMETER), "Repeated entity was not rejected");
            require(ScanTargetSelector.resolve(player).orElseThrow().continuity().equals(selected.continuity()), "Rejected entity incorrectly fell through to block behind it");
            dropped.discard();
            farther.discard();
            level.setBlock(boundary, Blocks.AIR.defaultBlockState(), 2);
            var pig = EntityType.PIG.create(level);
            entities.add(pig);
            pig.setNoAi(true);
            pig.setPos(0.8, 200, 0.5);
            require(level.addFreshEntity(pig), "Could not create near-range entity");
            require(ScanTargetSelector.pointedEntity(player).isEmpty(), "Entity within half-block minimum selected");
            source.sendSuccess(() -> Component.literal("Scan aiming checks passed: 5/10-block ranges and exact endpoint; source/full-height/flowing fluids; "
                    + "entity precedence; solid/torch/water LOS; noncollidable dropped items; unexpanded broad phase; nearest ordering; no repeated-target fallback; half-block minimum."), false);
            return 1;
        } catch (RuntimeException failure) {
            tc4revival.TC4Revival.LOGGER.error("Scan aiming fixture failed", failure);
            source.sendFailure(Component.literal("Scan aiming fixture failed: " + failure.getMessage()));
            return 0;
        } finally {
            entities.forEach(Entity::discard);
            for (var position : positions) level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
        }
    }
}
