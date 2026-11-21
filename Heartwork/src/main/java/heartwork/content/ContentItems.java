package heartwork.content;

import com.google.gson.JsonObject;
import heartwork.Heartwork;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeTier;

public final class ContentItems {
    private ContentItems() {}

    public static Item create(JsonObject data) {
        String id = data.get("id").getAsString();
        return switch(data.get("kind").getAsString()) {
            case "food" -> food(data);
            case "hammer" -> new Item(new Item.Properties().stacksTo(1)) {
                @Override public boolean hasCraftingRemainingItem(ItemStack stack) { return true; }
                @Override public ItemStack getCraftingRemainingItem(ItemStack stack) { return stack.copyWithCount(1); }
            };
            case "seed" -> new ItemNameBlockItem(data.get("block").getAsString().equals("minecraft:wheat") ? Blocks.WHEAT
                    : Heartwork.BLOCK_MAP.get(data.get("block").getAsString()).get(), new Item.Properties());
            case "cake" -> new BlockItem(Heartwork.BLOCK_MAP.get(id).get(), new Item.Properties().stacksTo(1));
            case "swiss" -> data.get("mode").getAsString().equals("researcher")
                    ? new SwissResearcherItem(data.get("uses").getAsInt())
                    : new SwissItem(data.get("uses").getAsInt(), data.get("mode").getAsString());
            case "tool" -> tool(data);
            case "guide" -> new Item(new Item.Properties()) {
                @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines, TooltipFlag flag) {
                    String key = id.equals("book1") ? "books" : "books_arthurs";
                    lines.add(Component.translatable(key + ".name")); lines.add(Component.translatable(key + ".name.ps"));
                }
            };
            case "straw" -> new StrawItem();
            default -> new Item(new Item.Properties());
        };
    }

    private static Item food(JsonObject data) {
        int nutrition = data.get("nutrition").getAsInt();
        FoodProperties.Builder food = new FoodProperties.Builder().nutrition(nutrition).saturationMod(nutrition - 1);
        if (data.get("meat").getAsBoolean()) food.meat();
        String remainder = data.has("remainder") ? data.get("remainder").getAsString() : null;
        return new Item(new Item.Properties().food(food.build())) {
            @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
                boolean creative = entity instanceof Player player && player.getAbilities().instabuild;
                ItemStack remaining = super.finishUsingItem(stack, level, entity);
                if (remainder == null || creative) return remaining;
                ItemStack container = new ItemStack(Heartwork.item(remainder));
                if (remaining.isEmpty()) return container;
                if (!level.isClientSide && entity instanceof Player player && !player.getInventory().add(container)) player.drop(container, false);
                return remaining;
            }
            @Override public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> lines, TooltipFlag flag) {
                lines.add(Component.translatable("THW.food.amount").append(": " + nutrition));
            }
        };
    }

    private static Item tool(JsonObject data) {
        int level = data.get("level").getAsInt();
        ForgeTier tier = new ForgeTier(level, data.get("uses").getAsInt(), data.get("speed").getAsFloat(),
                data.get("damage").getAsFloat(), data.get("enchantment").getAsInt(),
                level >= 3 ? net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL : net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL,
                () -> Ingredient.of(Heartwork.item(data.get("repair").getAsString())));
        Item.Properties properties = new Item.Properties();
        return switch(data.get("tool").getAsString()) {
            case "sword" -> new SwordItem(tier, 4, -2.4F, properties);
            case "pickaxe" -> new PickaxeItem(tier, 2, -2.8F, properties);
            case "axe" -> new AxeItem(tier, 3, -3.1F, properties);
            case "hoe" -> new HoeItem(tier, -data.get("damage").getAsInt(), -1, properties);
            case "shovel" -> new ShovelItem(tier, 1, -3, properties);
            default -> throw new IllegalArgumentException("Unknown tool " + data);
        };
    }
}
