package tc4revival.debug;

import java.util.ArrayList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.phys.AABB;
import tc4revival.content.*;

/** Accelerated vanilla furnace ticks in a checked-empty server fixture, restored even on failure. */
 class SmeltingDebugChecks {
    private SmeltingDebugChecks() {}
    private record Case(Item input, Item output, int count, float experience, String recipe) {}
    static int run(CommandSourceStack source) {
        try { return verify(source); }
        catch (RuntimeException failure) {
            tc4revival.TC4Revival.LOGGER.error("Smelting validation failed", failure);
            source.sendFailure(Component.literal("smelting FAILED: " + failure.getMessage())); return 0;
        }
    }
    private static int verify(CommandSourceStack source) {
        var cases = new ArrayList<Case>();
        for (var type : ClusterType.values()) {
            var stack = new ItemStack(TCItems.cluster(type));
            var identity = tc4revival.aspect.item.ItemStackAspects.identity(stack).orElseThrow();
            require(identity.item().equals("Thaumcraft:ItemNugget") && identity.metadata() == type.metadata(), "Cluster identity");
            require(stack.is(net.minecraft.tags.ItemTags.create(ResourceLocation.parse(type.tag()))), "Cluster tag");
            cases.add(new Case(stack.getItem(), type.output(), 2, 2, type.path() + "_smelting"));
        }
        cases.add(new Case(TCItems.shard(ShardType.BALANCED), TCItems.material(MaterialType.SALIS_MUNDUS), 1, 1, "salis_mundus_smelting"));
        cases.add(new Case(TCItems.material(MaterialType.GOLD_COIN), Items.GOLD_NUGGET, 1, 0, "gold_coin_smelting"));
        var level = source.getServer().overworld();
        var pos = new BlockPos(8, 104, 8);
        require(level.hasChunkAt(pos) && level.getBlockState(pos).isAir() && level.getBlockEntity(pos) == null, "Furnace fixture occupied or unloaded");
        require(level.players().stream().noneMatch(player -> player.getBoundingBox().intersects(new AABB(pos).inflate(16))), "Player near furnace fixture");
        try {
            for (var test : cases) {
                level.setBlock(pos, Blocks.FURNACE.defaultBlockState(), 3);
                var furnace = (FurnaceBlockEntity) level.getBlockEntity(pos);
                furnace.setItem(0, new ItemStack(test.input, 2)); furnace.setItem(1, new ItemStack(Items.COAL));
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, furnace, level).orElseThrow();
                require(recipe.getId().equals(ResourceLocation.parse("thaumcraft:" + test.recipe)) && recipe.getCookingTime() == 200
                        && recipe.getExperience() == test.experience, "Cooking time or experience conversion");
                tick(level, pos, furnace, 100);
                require(furnace.getItem(2).isEmpty() && furnace.getItem(0).getCount() == 2, "Premature smelting");
                var saved = furnace.saveWithFullMetadata();
                var restored = new FurnaceBlockEntity(pos, level.getBlockState(pos)); restored.load(saved);
                level.setBlockEntity(restored); furnace = restored;
                tick(level, pos, furnace, 99);
                require(furnace.getItem(2).isEmpty(), "Cook progress lost during NBT reload");
                tick(level, pos, furnace, 1);
                require(furnace.getItem(2).is(test.output) && furnace.getItem(2).getCount() == test.count && furnace.getItem(0).getCount() == 1,
                        "First smelting output");
                tick(level, pos, furnace, 200);
                require(furnace.getItem(2).getCount() == 2 * test.count && furnace.getItem(0).isEmpty(), "Second smelting did not add full result count");
                require(furnace.saveWithoutMetadata().getCompound("RecipesUsed").getInt(recipe.getId().toString()) == 2, "Experience recipe accounting");
                // A full result slot must not consume input; removing exactly enough room resumes processing.
                furnace.setItem(0, new ItemStack(test.input)); furnace.setItem(2, new ItemStack(test.output, 65 - test.count));
                tick(level, pos, furnace, 200);
                require(furnace.getItem(0).getCount() == 1 && furnace.getItem(2).getCount() == 65 - test.count, "Overflow consumed input");
                furnace.setItem(2, new ItemStack(test.output, 64 - test.count));
                tick(level, pos, furnace, 200);
                require(furnace.getItem(0).isEmpty() && furnace.getItem(2).getCount() == 64, "Exact remaining output space rejected");
                furnace.clearContent(); level.removeBlockEntity(pos); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        } finally {
            if (level.getBlockEntity(pos) instanceof FurnaceBlockEntity furnace) furnace.clearContent();
            level.removeBlockEntity(pos); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        source.sendSuccess(() -> Component.literal("smelting PASS: " + cases.size() + " loaded recipes; actual furnace ticks, double outputs, 200-tick cooking, NBT resume, experience accounting and output capacity; fixture restored"), false);
        return 1;
    }
    private static void tick(net.minecraft.server.level.ServerLevel level, BlockPos pos, FurnaceBlockEntity furnace, int count) {
        for (int i = 0; i < count; i++) AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), furnace);
    }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
