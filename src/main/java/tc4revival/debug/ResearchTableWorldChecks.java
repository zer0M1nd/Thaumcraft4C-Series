package tc4revival.debug;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import tc4revival.content.*;
import tc4revival.research.*;

/** Actual server blocks and callbacks in a checked-empty fixture; never substitutes for client or network acceptance. */
 class ResearchTableWorldChecks {
    private ResearchTableWorldChecks() {}
    static int verify(CommandSourceStack source) {
        recipes(source);
        var level = source.getServer().overworld();
        var center = new BlockPos(8, 100, 8);
        var positions = new java.util.ArrayList<BlockPos>(); positions.add(center);
        for (var direction : TableBlock.SEARCH_ORDER) positions.add(center.relative(direction));
        var area = new AABB(center).inflate(4);
        require(level.players().stream().noneMatch(player -> player.getBoundingBox().intersects(area.inflate(12))), "Player near table test area");
        for (var pos : positions) require(level.hasChunkAt(pos) && level.getBlockState(pos).isAir() && level.getBlockEntity(pos) == null, "Fixture area is not loaded and empty: " + pos);
        var priorDrops = level.getEntitiesOfClass(ItemEntity.class, area).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
        var player = new FakePlayer(level, new GameProfile(UUID.nameUUIDFromBytes("tc4revival:table-world-test".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "TC4TableProbe"));
        player.setPos(8.5, 100, 10.5);
        var block = TCTables.TABLE.get();
        try {
            for (var direction : TableBlock.SEARCH_ORDER) {
                var mate = center.relative(direction);
                level.setBlockAndUpdate(center, block.defaultBlockState()); level.setBlockAndUpdate(mate, block.defaultBlockState());
                player.getAbilities().instabuild = false;
                var tool = new ItemStack(TCItems.SCRIBING_TOOLS.get()); tool.setDamageValue(17); player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                var hit = new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false);
                var result = tool.getItem().onItemUseFirst(tool, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
                require(result.consumesAction() && tool.isEmpty(), "Survival formation did not consume tool: " + direction);
                require(level.getBlockState(center).getValue(TableBlock.PART) == TableBlock.Part.MASTER
                        && level.getBlockState(center).getValue(TableBlock.FACING) == direction, "Owner orientation: " + direction);
                require(level.getBlockState(mate).getValue(TableBlock.PART) == TableBlock.Part.EXTENSION
                        && level.getBlockState(mate).getValue(TableBlock.FACING) == direction.getOpposite(), "Extension orientation: " + direction);
                var table = (ResearchTableBlockEntity) level.getBlockEntity(center);
                require(table.items.getStackInSlot(0).getDamageValue() == 17 && table.valid(player), "Owner ink/reach");
                var saved = table.saveWithFullMetadata(); table.items.setStackInSlot(0, ItemStack.EMPTY); table.load(saved);
                require(table.items.getStackInSlot(0).getDamageValue() == 17, "Placed table NBT reload");
                var note = new ResearchNote("THAUMIUM", 0x551199, true, 3, Map.of(new ResearchHex(0, 0), new ResearchNote.Cell(1, "terra")));
                var notes = ResearchNotesItem.create(note); notes.setCount(128); table.items.setStackInSlot(1, notes);
                var menu = new ResearchTableMenu(1, player.getInventory(), table); player.containerMenu = menu;
                require(!menu.quickMoveStack(player, 1).isEmpty() && player.getInventory().getItem(8).getCount() == 128, "Old oversized move to player");
                require(!menu.quickMoveStack(player, 37).isEmpty() && table.items.getStackInSlot(1).getCount() == 64, "Old table insertion clamp");
                player.setPos(40, 100, 40);
                require(!menu.stillValid(player) && !table.act(player, ResearchTableBlockEntity.Action.DUPLICATE, 0, 0, null, null), "Out of reach transaction accepted");
                player.setPos(8.5, 100, 10.5); player.containerMenu = player.inventoryMenu;
                require(!table.act(player, ResearchTableBlockEntity.Action.COMBINE, 0, 0, "terra", "ordo"), "Closed menu transaction accepted");
                var drops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(mate), level, mate, level.getBlockEntity(mate));
                require(drops.size() == 1 && drops.get(0).is(TCItems.TABLE.get()) && drops.get(0).getCount() == 1, "Extension loot must be plain table");
                var beforeBreak = dropIds(level, area);
                level.destroyBlock(mate, true);
                checkDrops(level, area, beforeBreak, 17, 64, notes.getTag());
                require(level.getBlockState(center).getValue(TableBlock.PART) == TableBlock.Part.SINGLE && table.items.getStackInSlot(0).isEmpty()
                        && table.items.getStackInSlot(1).isEmpty() && !menu.stillValid(player), "Breaking extension did not dismantle/drop contents");
                level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
                player.getInventory().clearContent();
            }
            // With four neighbors present the first match must be north, and creative retains the held tool.
            for (var pos : positions) level.setBlockAndUpdate(pos, block.defaultBlockState());
            player.getAbilities().instabuild = true; var tool = new ItemStack(TCItems.SCRIBING_TOOLS.get()); player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            tool.getItem().onItemUseFirst(tool, new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false)));
            require(!tool.isEmpty() && level.getBlockState(center).getValue(TableBlock.FACING) == Direction.NORTH, "Creative tool/search order");
            var captured = (ResearchTableBlockEntity) level.getBlockEntity(center);
            var notes = ResearchNotesItem.create(new ResearchNote("THAUMIUM", 0x551199, true, 3,
                    Map.of(new ResearchHex(0, 0), new ResearchNote.Cell(1, "terra"))));
            notes.setCount(7); captured.items.setStackInSlot(1, notes);
            var beforeBreak = dropIds(level, area);
            level.destroyBlock(center, true);
            checkDrops(level, area, beforeBreak, 0, 7, notes.getTag());
            require(level.getBlockState(center.north()).getValue(TableBlock.PART) == TableBlock.Part.SINGLE && captured.items.getStackInSlot(0).isEmpty(), "Breaking owner did not dismantle extension");
            // Preserve old activation search even for an inconsistent multi-owner layout.
            for (var pos : positions) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            var extensionState = block.defaultBlockState().setValue(TableBlock.PART, TableBlock.Part.EXTENSION).setValue(TableBlock.FACING, Direction.EAST);
            level.setBlock(center, extensionState, 2);
            level.setBlock(center.east(), block.defaultBlockState().setValue(TableBlock.PART, TableBlock.Part.MASTER).setValue(TableBlock.FACING, Direction.SOUTH), 2);
            level.setBlock(center.north(), block.defaultBlockState().setValue(TableBlock.PART, TableBlock.Part.MASTER).setValue(TableBlock.FACING, Direction.WEST), 2);
            require(TableBlock.paired(level, center, extensionState), "Old nonreciprocal pairing was rejected");
            require(center.north().equals(TableBlock.interactionOwner(level, center, extensionState)), "Extension must search north before its declared east owner");
            level.setBlock(center.north(), Blocks.AIR.defaultBlockState(), 2);
            require(center.east().equals(TableBlock.interactionOwner(level, center, extensionState)), "Extension owner search fallback");
            level.setBlock(center.east(), Blocks.AIR.defaultBlockState(), 2);
            require(TableBlock.interactionOwner(level, center, extensionState) == null, "Missing owner not handled");
            block.neighborChanged(extensionState, level, center, Blocks.AIR, center.east(), false);
            require(level.getBlockState(center).getValue(TableBlock.PART) == TableBlock.Part.SINGLE, "Orphan extension did not revert");
        } finally {
            for (var pos : positions) {
                if (level.getBlockEntity(pos) instanceof ResearchTableBlockEntity table) { table.items.setStackInSlot(0, ItemStack.EMPTY); table.items.setStackInSlot(1, ItemStack.EMPTY); }
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            for (var entity : level.getEntitiesOfClass(ItemEntity.class, area)) if (!priorDrops.contains(entity.getUUID())) entity.discard();
        }
        source.sendSuccess(() -> Component.literal("Research table world PASS; recipes 36 wood pairs + ink sac/black dye + refill; four formation orientations; survival/creative costs; NBT; oversized Shift move 128/64; reach/closed-menu rejection; both dismantle directions; actual drop totals and NBT: table=1, tools=1, discovery=64 (extension) or 7 (owner); old multi-owner search/nonreciprocal pair/orphan behavior; fixture cleaned, real player knowledge/inventory unchanged"), false);
        return 1;
    }
    private static Set<UUID> dropIds(net.minecraft.server.level.ServerLevel level, AABB area) {
        return level.getEntitiesOfClass(ItemEntity.class, area).stream().map(ItemEntity::getUUID).collect(java.util.stream.Collectors.toSet());
    }
    private static void checkDrops(net.minecraft.server.level.ServerLevel level, AABB area, Set<UUID> before,
            int damage, int notes, net.minecraft.nbt.CompoundTag noteTag) {
        int tables = 0, tools = 0, discoveries = 0;
        for (var entity : level.getEntitiesOfClass(ItemEntity.class, area)) if (!before.contains(entity.getUUID())) {
            var stack = entity.getItem();
            if (stack.is(TCItems.TABLE.get())) tables += stack.getCount();
            else if (stack.is(TCItems.SCRIBING_TOOLS.get())) {
                tools += stack.getCount(); require(stack.getDamageValue() == damage, "Dropped ink damage changed");
            } else if (stack.is(TCItems.DISCOVERY.get())) {
                discoveries += stack.getCount(); require(java.util.Objects.equals(stack.getTag(), noteTag), "Dropped discovery NBT changed");
            } else throw new IllegalStateException("Unexpected table drop: " + stack);
        }
        require(tables == 1 && tools == 1 && discoveries == notes,
                "Drop totals: table=" + tables + ", tools=" + tools + ", discoveries=" + discoveries);
    }
    private static void recipes(CommandSourceStack source) {
        var level = source.getServer().overworld();
        var menu = new AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 3, 3);
        var planks = List.of(Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS);
        var slabs = List.of(Items.OAK_SLAB, Items.SPRUCE_SLAB, Items.BIRCH_SLAB, Items.JUNGLE_SLAB, Items.ACACIA_SLAB, Items.DARK_OAK_SLAB);
        for (var plank : planks) for (var slab : slabs) {
            grid.clearContent(); for (int slot : new int[] {0, 1, 2}) grid.setItem(slot, new ItemStack(slab));
            grid.setItem(3, new ItemStack(plank)); grid.setItem(5, new ItemStack(plank));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            require(recipe.assemble(grid, level.registryAccess()).is(TCItems.TABLE.get()), "Table recipe");
        }
        for (var pigment : List.of(Items.INK_SAC, Items.BLACK_DYE)) for (int damage : new int[] {-1, 0, 99, 100}) {
            grid.clearContent(); grid.setItem(0, new ItemStack(pigment));
            if (damage < 0) { grid.setItem(3, new ItemStack(Items.GLASS_BOTTLE)); grid.setItem(5, new ItemStack(Items.FEATHER)); }
            else { var tool = new ItemStack(TCItems.SCRIBING_TOOLS.get()); tool.setDamageValue(damage); tool.getOrCreateTag().putString("test_marker", "discard"); grid.setItem(1, tool); }
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            var output = recipe.assemble(grid, level.registryAccess());
            require(output.is(TCItems.SCRIBING_TOOLS.get()) && ScribingToolsItem.ink(output) == 100 && (!output.hasTag() || !output.getTag().contains("test_marker")), "Ink refill/output NBT");
        }
        grid.setItem(0, new ItemStack(Items.RED_DYE));
        require(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(), "Red dye accepted as ink");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
