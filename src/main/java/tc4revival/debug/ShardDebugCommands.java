package tc4revival.debug;

import com.mojang.brigadier.builder.ArgumentBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.content.ShardType;
import tc4revival.content.TCItems;
import tc4revival.research.ScanTargetResolver;

 class ShardDebugCommands {
    private ShardDebugCommands() {}
    static ArgumentBuilder<CommandSourceStack, ?> tree() {
        return Commands.literal("shards").then(Commands.literal("verify").executes(context -> {
            var source = context.getSource();
            for (var type : ShardType.values()) {
                var item = TCItems.shard(type);
                var stack = new ItemStack(item, 64);
                require(ForgeRegistries.ITEMS.getKey(item).toString().equals(type.id()), "Wrong shard registry ID");
                require(stack.getMaxStackSize() == 64 && stack.getMaxDamage() == 0 && stack.getRarity() == Rarity.COMMON, "Shard item properties differ");
                var identity = ItemStackAspects.identity(stack).orElseThrow();
                require(identity.item().equals("Thaumcraft:ItemShard") && identity.metadata() == type.metadata(), "Old shard metadata differs");
                require(ItemStackAspects.scanHash(stack).orElseThrow() == -165650046 + type.metadata(), "Old shard hash differs");
                require(stack.is(ItemTags.create(ResourceLocation.parse("thaumcraft:shards"))), "Missing complete TC shard category");
                require(stack.is(ItemTags.create(ResourceLocation.parse("forge:shards"))) == (type != ShardType.BALANCED), "Ore shard parent category differs");
                for (var category : ShardType.values()) {
                    if (category == ShardType.BALANCED) continue;
                    require(stack.is(ItemTags.create(ResourceLocation.parse("forge:shards/" + category.material()))) == (type == category), "Element shard tags were mixed");
                }
                var expected = new LinkedHashMap<String, Integer>();
                expected.put("praecantatio", 1);
                String primal = List.of("aer", "ignis", "aqua", "terra", "ordo", "perditio", "aer").get(type.metadata());
                expected.put(primal, 2);
                expected.put("vitreus", 1);
                if (type == ShardType.BALANCED) { expected.put("terra", 1); expected.put("ordo", 1); expected.put("perditio", 1); }
                var actual = new LinkedHashMap<String, Integer>();
                ItemStackAspects.resolve(stack).orElseThrow().view().forEach((aspect, quantity) -> actual.put(aspect.tag(), quantity));
                require(new ArrayList<>(expected.entrySet()).equals(new ArrayList<>(actual.entrySet())), "Shard final ordered aspects differ: " + type + " " + actual);
                var dropped = new ItemEntity(source.getLevel(), 0, 200, 0, stack);
                try {
                    var target = ScanTargetResolver.entity(dropped).orElseThrow();
                    require(target.identity().orElseThrow().equals(Integer.toString(-165650046 + type.metadata())), "Dropped shard identity differs");
                    require(target.aspects().view().equals(ItemStackAspects.resolve(stack).orElseThrow().view()), "Dropped shard aspects differ");
                    require(dropped.getItem().getCount() == 64, "Scanning consumed shard items");
                } finally { dropped.discard(); }
                source.sendSuccess(() -> Component.literal(type.id() + ": legacy metadata " + type.metadata() + "; hash "
                        + (-165650046 + type.metadata()) + "; aspects " + actual), false);
            }
            source.sendSuccess(() -> Component.literal("Shard checks passed: 7 registered items, legacy stack properties and identities, "
                    + "six exclusive Forge shard tags, full TC shard category, ordered final aspects and actual dropped-stack resolution."), false);
            return 1;
        }));
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
