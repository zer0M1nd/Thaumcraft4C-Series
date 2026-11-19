package tc4revival.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import tc4revival.aspect.TCAspects;
import tc4revival.research.PlayerKnowledge;
import tc4revival.research.ResearchCatalogs;
import tc4revival.research.ResearchHex;
import tc4revival.research.ResearchNote;
import tc4revival.research.ResearchNoteNbt;
import tc4revival.research.ResearchTableActions;

/** Checks table transactions against live registries without opening a fake table or spending real inventory. */
public  class ResearchTableDebugCommands {
    private ResearchTableDebugCommands() {}
    public static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("table_actions").executes(context -> check(context.getSource()))
                .then(Commands.literal("storage").executes(context -> storage(context.getSource())))
                .then(Commands.literal("world").executes(context -> ResearchTableWorldChecks.verify(context.getSource())))
                .then(Commands.literal("hoppers").executes(context -> ResearchTableHopperChecks.verify(context.getSource())))
                .then(Commands.literal("environment").executes(context -> ResearchTableEnvironmentChecks.start(context.getSource())))
                .then(Commands.literal("bonuses").executes(context -> bonuses(context.getSource())));
    }
    private static int storage(CommandSourceStack source) {
        var state = tc4revival.content.TCTables.TABLE.get().defaultBlockState()
                .setValue(tc4revival.content.TableBlock.PART, tc4revival.content.TableBlock.Part.MASTER);
        var table = new tc4revival.content.ResearchTableBlockEntity(net.minecraft.core.BlockPos.ZERO, state);
        var tools = new net.minecraft.world.item.ItemStack(tc4revival.content.TCItems.SCRIBING_TOOLS.get()); tools.setDamageValue(99);
        table.items.setStackInSlot(0, tools);
        var note = new ResearchNote("THAUMIUM", 0x551199, false, 2, Map.of(new ResearchHex(0, 0), ResearchNote.Cell.blank()));
        table.items.setStackInSlot(1, tc4revival.content.ResearchNotesItem.create(note));
        var saved = table.saveWithoutMetadata();
        saved.putInt("nextRecalc", 601);
        var bonuses = new net.minecraft.nbt.ListTag(); var bonus = new CompoundTag(); bonus.putString("tag", "aer"); bonuses.add(bonus);
        saved.put("bonusAspects", bonuses);
        var loaded = new tc4revival.content.ResearchTableBlockEntity(net.minecraft.core.BlockPos.ZERO, state); loaded.load(saved);
        require(tc4revival.content.ScribingToolsItem.ink(loaded.items.getStackInSlot(0)) == 1, "Ink persistence");
        require(ResearchNoteNbt.read(loaded.items.getStackInSlot(1).getTag(), TCAspects.catalog()).note().orElseThrow().equals(note), "Note persistence");
        require(loaded.bonuses().equals(Set.of("aer")) && loaded.saveWithoutMetadata().getInt("nextRecalc") == 601, "Bonus/counter persistence");
        var capability = loaded.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
        var handler = capability.orElseThrow(IllegalStateException::new);
        var discovery = new net.minecraft.world.item.ItemStack(tc4revival.content.TCItems.DISCOVERY.get());
        require(!handler.isItemValid(1, discovery), "Automation accepted completed research");
        require(handler.isItemValid(1, new net.minecraft.world.item.ItemStack(tc4revival.content.TCItems.UNKNOWN_RESEARCH_NOTES.get())), "Automation rejected unknown note");
        require(handler.extractItem(0, 1, true).getDamageValue() == 99 && !loaded.items.getStackInSlot(0).isEmpty(), "Extraction simulation changed inventory");
        require(!handler.extractItem(0, 1, false).isEmpty() && loaded.items.getStackInSlot(0).isEmpty(), "Extraction failed");
        loaded.invalidateCaps(); require(!capability.isPresent(), "Stale optional survived invalidation");
        loaded.reviveCaps(); require(loaded.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent(), "Capability did not revive");
        loaded.setBlockState(state.setValue(tc4revival.content.TableBlock.PART, tc4revival.content.TableBlock.Part.SINGLE));
        require(!loaded.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).isPresent()
                && handler.extractItem(1, 1, false).isEmpty(), "Stale automation can access dismantled table");
        for (int count : new int[] {64, 65, 127, 128, 255, 256, 257}) {
            var stack = tc4revival.content.ResearchNotesItem.create(new ResearchNote("THAUMIUM", 0x551199, true, 5,
                    Map.of(new tc4revival.research.ResearchHex(0, 0), new ResearchNote.Cell(1, "terra"))));
            stack.setCount(count); table.items.setStackInSlot(1, stack);
            var encoded = table.saveWithoutMetadata();
            var copy = new tc4revival.content.ResearchTableBlockEntity(net.minecraft.core.BlockPos.ZERO, state); copy.load(encoded);
            int expected = Math.max(0, (byte) count);
            require(copy.items.getStackInSlot(1).getCount() == expected, "Old byte Count boundary changed at " + count);
            if (expected > 0) require(java.util.Objects.equals(stack.getTag(), copy.items.getStackInSlot(1).getTag()), "Oversized note NBT changed");
        }
        source.sendSuccess(() -> Component.literal("Research table storage PASS; ink/note/bonus/counter NBT; automation filters, simulation, extraction, invalidation/revival, dismantled handle; old byte Count roundtrips 64/65/127/128/255/256/257; no world/player changes"), false);
        return 1;
    }
    private static int bonuses(CommandSourceStack source) {
        var aspects = TCAspects.catalog().all().stream().map(aspect -> aspect.tag()).toList();
        var visits = new int[1]; var draws = new java.util.ArrayList<Integer>();
        var world = new tc4revival.research.ResearchTableBonuses.Environment() {
            @Override public int tableY() { return 200; }
            @Override public int height() { return 256; }
            @Override public boolean darkCoveredNight() { return true; }
            @Override public tc4revival.research.ResearchTableBonuses.Source at(int x, int y, int z) {
                visits[0]++;
                require(x == -8 && y == -8 && z == -8, "Scan order");
                return tc4revival.research.ResearchTableBonuses.Source.bookshelf();
            }
        };
        var result = tc4revival.research.ResearchTableBonuses.recalculate(Set.of(), world, aspects,
                bound -> { draws.add(bound); return 0; });
        require(result.equals(Set.of("aer", "perditio")) && visits[0] == 1, "Environment result/stop");
        require(draws.equals(java.util.List.of(20, 20, 20, 20, 300, 48)), "Environment random sequence");
        int counter = 0;
        for (int tick = 1; tick < 602; tick++) {
            counter = tc4revival.research.ResearchTableBonuses.nextCounter(counter);
            require(counter == tick, "Early environmental tick");
        }
        require(tc4revival.research.ResearchTableBonuses.nextCounter(counter) == 0, "602 tick interval");
        source.sendSuccess(() -> Component.literal("Research table bonuses PASS; interval=602; night/altitude draws=4; bookshelf=300/48; ordered scan; existing bonus ends scan; no world/player changes"), false);
        return 1;
    }
    private static int check(CommandSourceStack source) {
        var aspects = TCAspects.catalog();
        var catalog = ResearchCatalogs.catalog();
        var actions = new ResearchTableActions(aspects, catalog, ResearchCatalogs::enabled);
        var empty = new ResearchTableActions.State(Optional.empty(), 0, 0, Set.of());
        int combinations = 0;
        for (var output : aspects.compounds()) for (int reverse = 0; reverse < 2; reverse++) {
            String a = output.components().get(reverse);
            String b = output.components().get(1 - reverse);
            var player = new PlayerKnowledge(aspects);
            player.setPool(aspects.require(a), (short) 1);
            player.setPool(aspects.require(b), (short) 1);
            var result = actions.combine(empty, player, a, b, 100).change().orElseThrow();
            require(result.knowledge().pool(output) == 3 && result.reward().orElseThrow().discovery(), "Combination discovery");
            require(!player.knows(output), "Mutated input knowledge");
            combinations++;
        }
        var player = new PlayerKnowledge(aspects);
        player.setPool(aspects.require("vitreus"), (short) 1);
        var middle = new ResearchHex(0, 0);
        var note = new ResearchNote("THAUMIUM", 123, false, 0, Map.of(
                middle, ResearchNote.Cell.blank(), new ResearchHex(-1, 0), new ResearchNote.Cell(1, "terra"),
                new ResearchHex(1, 0), new ResearchNote.Cell(1, "ordo")));
        var table = new ResearchTableActions.State(Optional.of(note), 1, 1, Set.of("vitreus"));
        var solved = actions.place(table, player, middle, "vitreus", () -> { throw new IllegalStateException("Unexpected random draw"); })
                .change().orElseThrow();
        require(solved.completedNow() && solved.table().ink() == 0 && solved.knowledge().pool(aspects.require("vitreus")) == 0,
                "Place cost or completion");
        require(solved.table().bonuses().contains("vitreus"), "Did not prefer pool");
        var tag = new CompoundTag();
        ResearchNoteNbt.write(tag, solved.table().note().orElseThrow());
        require(ResearchNoteNbt.read(tag, aspects).note().orElseThrow().complete(), "Completed note NBT");
        require(!actions.place(table, player, new ResearchHex(-1, 0), "vitreus", () -> 0).accepted(), "Anchor overwrite");
        require(!actions.duplicate(solved.table(), player, 1, 1).accepted(), "Missing duplication research accepted");
        var copier = player.copy();
        copier.rememberResearch("RESEARCHDUPE");
        catalog.require("THAUMIUM").aspects().forEach((key, amount) -> copier.setPool(aspects.require(key), amount.shortValue()));
        var copied = actions.duplicate(solved.table(), copier, 1, 1).change().orElseThrow();
        require(copied.paper() == 1 && copied.inkSac() == 1 && copied.table().noteCount() == 2
                && copied.table().note().orElseThrow().copies() == 1, "Copy inventory cost");
        for (String key : catalog.require("THAUMIUM").aspects().keySet()) require(copied.knowledge().pool(aspects.require(key)) == 0, "Copy pool cost");
        require(!actions.duplicate(copied.table(), copied.knowledge(), 1, 1).accepted(), "Unaffordable repeated copy");
        final int count = combinations;
        source.sendSuccess(() -> Component.literal("Research table actions PASS; combinations=" + count
                + "; pool before bonus; last ink; completed NBT; anchors protected; copy research/material/point costs"
                + "; real player and inventory unchanged"), false);
        return 1;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Research table actions FAIL: " + message);
    }
}
