package tc4revival.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.item.BlockScanTargets;
import tc4revival.aspect.recipe.RecipeAspectData.Key;
import tc4revival.network.CluePacket;
import tc4revival.network.TCNetwork;
import tc4revival.research.PlayerKnowledge;
import tc4revival.research.ResearchCatalogs;
import tc4revival.research.ResearchClues;
import tc4revival.research.ResearchDefinition;
import tc4revival.research.ResearchRules;

/** Real registry/tag/NBT/network adapters, tested without editing player knowledge or spawning test entities. */
public  class ResearchClueDebugCommands {
    private ResearchClueDebugCommands() {}
    public static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("adapters").executes(context -> check(context.getSource(), false))
                .then(Commands.literal("tagged").executes(context -> check(context.getSource(), true)));
    }
    private static int check(CommandSourceStack source, boolean expectTaggedNugget) {
        var catalog = ResearchCatalogs.catalog();
        var bowTrigger = catalog.require("BONEBOW").triggers().items().orElseThrow().get(0);
        var bow = new ItemStack(Items.BOW);
        bow.setDamageValue(42);
        var captured = ResearchClues.dropped(bow);
        require(captured.matches().test(bowTrigger), "Modern Damage tag blocked an ordinary damaged bow");
        bow.setHoverName(Component.literal("Named test bow"));
        require(!ResearchClues.dropped(bow).matches().test(bowTrigger), "Named bow ignored NBT");
        require(captured.matches().test(bowTrigger), "Observed item retained the mutable original stack");
        var bone = new ItemStack(Items.BONE, 64);
        bone.getOrCreateTag().putInt("x", 1);
        bone.getOrCreateTag().putInt("y", 2);
        var boneTrigger = new ResearchDefinition.Stack(new Key("minecraft:bone", 0), 1, false,
                Optional.of("{y:2,x:1}"), Optional.empty());
        require(ResearchClues.dropped(bone).matches().test(boneTrigger), "NBT compared text order or stack count");

        var woolTrigger = catalog.require("TINYHAT").triggers().items().orElseThrow().get(0);
        for (var dye : net.minecraft.world.item.DyeColor.values()) {
            var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("minecraft:" + dye.getName() + "_wool"));
            require(item != null && ResearchClues.dropped(new ItemStack(item)).matches().test(woolTrigger), "Flattened wool metadata mismatch: " + dye);
        }
        var grouped = ResearchClues.block(BlockScanTargets.target(Blocks.DAMAGED_ANVIL.defaultBlockState()).orElseThrow());
        var damaged = ResearchClues.dropped(new ItemStack(Items.DAMAGED_ANVIL));
        var anvilZero = new ResearchDefinition.Stack(new Key("minecraft:anvil", 0), 1, false, Optional.empty(), Optional.empty());
        var anvilTwo = new ResearchDefinition.Stack(new Key("minecraft:anvil", 2), 1, false, Optional.empty(), Optional.empty());
        require(grouped.matches().test(anvilZero) && !grouped.matches().test(anvilTwo), "Block clue did not use grouped metadata");
        require(damaged.matches().test(anvilTwo) && !damaged.matches().test(anvilZero), "Dropped clue discarded original metadata");
        var enderman = EntityType.ENDERMAN.create(source.getLevel());
        require(enderman != null && ResearchClues.entity(enderman).orElseThrow().equals(new ResearchRules.EntityClue("Enderman")), "Wrong entity clue identity");

        // A temporary validation datapack supplies the foreign material membership for the tagged run.
        var nugget = new ItemStack(Items.IRON_NUGGET);
        nugget.setHoverName(Component.literal("NBT must not block a material match"));
        var clues = ResearchCatalogs.rules().clueCandidates(new PlayerKnowledge(TCAspects.catalog()), ResearchClues.dropped(nugget), java.util.Map.of());
        require(clues.equals(expectTaggedNugget ? List.of("JARBRAIN") : List.of()), "Unexpected material-tag candidates: " + clues);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var packet = new CluePacket("THAUMIUM");
            TCNetwork.CHANNEL.encodeMessage(packet, buffer);
            require(buffer.readUnsignedByte() == 4, "Wrong registered clue discriminator");
            require(packet.equals(CluePacket.decode(buffer)), "Clue packet did not round trip");
            buffer.clear();
            packet.encode(buffer);
            buffer.writeByte(1);
            boolean rejected = false;
            try { CluePacket.decode(buffer); } catch (IllegalArgumentException expected) { rejected = true; }
            require(rejected, "Trailing clue packet bytes were accepted");
        } finally { buffer.release(); }
        for (String bad : List.of("", "@THAUMIUM", "two words", "x".repeat(129))) {
            boolean rejected = false;
            try { new CluePacket(bad); } catch (IllegalArgumentException expected) { rejected = true; }
            require(rejected, "Malformed clue identity accepted");
        }
        source.sendSuccess(() -> Component.literal("Research clue adapters PASS: bow Damage/NBT/snapshot, structural NBT/count, 16 wool colors,"
                + " grouped block vs dropped anvil, Enderman, materialTag=" + expectTaggedNugget + ", registered packet codec"), false);
        return 1;
    }
    private static void require(boolean valid, String detail) { if (!valid) throw new IllegalStateException(detail); }
}
