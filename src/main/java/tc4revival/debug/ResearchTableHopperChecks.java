package tc4revival.debug;

import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import tc4revival.content.*;
import tc4revival.research.*;

/** Real placed hoppers, invoking their normal server tick with controlled cooldowns. */
 class ResearchTableHopperChecks {
    private ResearchTableHopperChecks() {}
    static int verify(CommandSourceStack source) {
        var level = source.getServer().overworld();
        var owner = new BlockPos(8, 100, 8); var mate = owner.east();
        var positions = List.of(owner, mate, owner.above(), owner.below(), mate.above(), mate.below());
        var area = new AABB(owner).inflate(4);
        require(level.players().stream().noneMatch(p -> p.getBoundingBox().intersects(area.inflate(12))), "Player near hopper fixture");
        for (var pos : positions) require(level.hasChunkAt(pos) && level.getBlockState(pos).isAir()
                && level.getBlockEntity(pos) == null, "Hopper fixture is not loaded and empty: " + pos);
        var priorDrops = level.getEntitiesOfClass(ItemEntity.class, area).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
        try {
            // Install both halves without neighbor callbacks until the pair is complete.
            var tableState = TCTables.TABLE.get().defaultBlockState();
            level.setBlock(owner, tableState.setValue(TableBlock.PART, TableBlock.Part.MASTER).setValue(TableBlock.FACING, Direction.EAST), 2);
            level.setBlock(mate, tableState.setValue(TableBlock.PART, TableBlock.Part.EXTENSION).setValue(TableBlock.FACING, Direction.WEST), 2);
            for (var pos : List.of(owner.above(), owner.below(), mate.above(), mate.below()))
                level.setBlockAndUpdate(pos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
            var table = (ResearchTableBlockEntity) level.getBlockEntity(owner);
            var feed = (HopperBlockEntity) level.getBlockEntity(owner.above());
            var drain = (HopperBlockEntity) level.getBlockEntity(owner.below());
            var tools = new ItemStack(TCItems.SCRIBING_TOOLS.get()); tools.setDamageValue(99);
            tools.getOrCreateTag().putString("hopper_probe", "retain");
            feed.setItem(0, tools.copy()); tick(feed);
            require(feed.isEmpty() && ItemStack.matches(table.items.getStackInSlot(0), tools), "Hopper did not insert tools with NBT");
            var note = ResearchNotesItem.create(new ResearchNote("THAUMIUM", 0x551199, false, 2,
                    Map.of(new ResearchHex(0, 0), ResearchNote.Cell.blank())));
            var batch = note.copy(); batch.setCount(6); feed.setItem(0, batch); tick(feed);
            require(feed.getItem(0).getCount() == 5 && ItemStack.matches(table.items.getStackInSlot(1), note), "Hopper note insertion/NBT/count");
            feed.clearContent();
            var completed = ResearchNotesItem.create(new ResearchNote("THAUMIUM", 0x551199, true, 2,
                    Map.of(new ResearchHex(0, 0), new ResearchNote.Cell(1, "terra"))));
            table.items.setStackInSlot(1, ItemStack.EMPTY);
            feed.setItem(0, completed.copy()); feed.setItem(1, new ItemStack(Items.STONE, 3)); tick(feed);
            require(feed.getItem(0).getCount() == 1 && feed.getItem(1).getCount() == 3 && table.items.getStackInSlot(1).isEmpty(),
                    "Hopper accepted discovery or unrelated item");
            feed.clearContent();
            table.items.setStackInSlot(1, completed.copy()); tick(drain); tick(drain);
            require(table.items.getStackInSlot(0).isEmpty() && table.items.getStackInSlot(1).isEmpty()
                    && ItemStack.matches(drain.getItem(0), tools) && ItemStack.matches(drain.getItem(1), completed),
                    "Hopper extraction lost tools/discovery or NBT");
            var extensionFeed = (HopperBlockEntity) level.getBlockEntity(mate.above());
            var extensionDrain = (HopperBlockEntity) level.getBlockEntity(mate.below());
            table.items.setStackInSlot(0, tools.copy()); extensionFeed.setItem(0, note.copy());
            tick(extensionFeed); tick(extensionDrain);
            require(ItemStack.matches(extensionFeed.getItem(0), note) && extensionDrain.isEmpty()
                    && ItemStack.matches(table.items.getStackInSlot(0), tools) && table.items.getStackInSlot(1).isEmpty(),
                    "Extension incorrectly exposed owner inventory to hoppers");
        } finally {
            // Clear test-owned inventories before removal, preserving all pre-existing item entities.
            for (var pos : positions) {
                if (level.getBlockEntity(pos) instanceof ResearchTableBlockEntity table) {
                    table.items.setStackInSlot(0, ItemStack.EMPTY); table.items.setStackInSlot(1, ItemStack.EMPTY);
                }
                if (level.getBlockEntity(pos) instanceof HopperBlockEntity hopper) hopper.clearContent();
            }
            for (var pos : positions) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, area)) if (!priorDrops.contains(entity.getUUID())) entity.discard();
        }
        source.sendSuccess(() -> Component.literal("Research table hoppers PASS; real placed hopper ticks with reset cooldowns; tool/note insertion, one-item transfer, damage/custom NBT preserved; discovery/stone rejected; tool/discovery extraction; extension inaccessible; fixture cleaned"), false);
        return 1;
    }
    private static void tick(HopperBlockEntity hopper) {
        hopper.setCooldown(0);
        HopperBlockEntity.pushItemsTick(hopper.getLevel(), hopper.getBlockPos(), hopper.getBlockState(), hopper);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
