package tc4revival.debug;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Map;
import java.util.Random;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import tc4revival.aspect.TCAspects;
import tc4revival.research.ResearchCatalogs;
import tc4revival.research.ResearchHex;
import tc4revival.research.ResearchNote;
import tc4revival.research.ResearchNoteNbt;
import tc4revival.research.ResearchNotes;

/** Exercises the loaded server catalog and real NBT implementation without modifying players or world RNG. */
public  class ResearchNoteDebugCommands {
    private ResearchNoteDebugCommands() {}
    public static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("notes").executes(context -> check(context.getSource()))
                .then(Commands.literal("extra").requires(source -> source.hasPermission(2))
                    .then(Commands.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,32))
                        .then(Commands.argument("y", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,32))
                            .executes(context -> {
                                int x=com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context,"x");
                                int y=com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context,"y");
                                if (ResearchCatalogs.catalog().all().stream().anyMatch(r -> !r.mirror()
                                        && r.noteSeeds().inner().size()+r.noteSeeds().negative().size()+x+y>37)) {
                                    context.getSource().sendFailure(Component.literal("数量过多：额外要素加研究原有内侧/负片要素不能超过 37（最大笔记内圈容量）。"));
                                    return 0;
                                }
                                tc4revival.research.ResearchNoteTestSeeds.set(x,y);
                                context.getSource().sendSuccess(() -> Component.literal("研究笔记测试：之后新生成的笔记额外添加 "+x+" 个随机负片、"+y+" 个随机内侧初始要素。0 0 关闭；服务器关闭后重置。"),false);
                                return 1;
                            }))))
                .then(Commands.literal("fixture").requires(source->source.hasPermission(2)).executes(context->{
                    var source=context.getSource();var player=source.getPlayerOrException();
                    var base=ResearchCatalogs.catalog().require("THAUMIUM");
                    var custom=new tc4revival.research.ResearchDefinition(base.key(),base.category(),Map.of("terra",1,"ordo",1),
                            base.parents(),base.hiddenParents(),base.siblings(),base.flags(),3,base.warp(),base.display(),base.triggers(),base.parentsAny(),
                            new tc4revival.research.ResearchDefinition.NoteSeeds(java.util.List.of("aer","ignis"),java.util.List.of("aqua","perditio")));
                    var note=new ResearchNotes(TCAspects.catalog()).generate(custom,new Random(42)::nextInt, tc4revival.config.TCConfig.SERVER.randomizedResearchNotes.get()).orElseThrow();
                    var stack=new net.minecraft.world.item.ItemStack(tc4revival.content.TCItems.RESEARCH_NOTES.get());
                    ResearchNoteNbt.write(stack.getOrCreateTag(),note);
                    if(!player.getInventory().add(stack))player.drop(stack,false);
                    source.sendSuccess(()->Component.literal("Granted test-only inner/negative note; research definitions unchanged."),false);
                    return 1;
                }));
    }
    private static int check(CommandSourceStack source) {
        var engine = new ResearchNotes(TCAspects.catalog());
        int generated = 0;
        for (var research : ResearchCatalogs.catalog().all()) {
            for (long seed : new long[] {0, 1, 42, 987654321}) {
                var note = engine.generate(research, new Random(seed)::nextInt, tc4revival.config.TCConfig.SERVER.randomizedResearchNotes.get());
                if (note.isEmpty()) continue;
                generated++;
                var value = note.orElseThrow();
                int radius = research.complexity() + 1;
                int holes = research.complexity() > 1 ? research.complexity() * 2 : 0;
                require(value.grid().size() == 1 + 3 * radius * (radius + 1) - holes, "Incorrect board size");
                require(value.grid().values().stream().filter(ResearchNote.Cell::anchor).count() == research.aspects().size() + research.noteSeeds().inner().size(), "Lost anchor");
                require(value.grid().values().stream().filter(ResearchNote.Cell::negative).count() == research.noteSeeds().negative().size(), "Lost negative seed");
                var tag = new CompoundTag();
                tag.putString("unrelated", "preserve");
                ResearchNoteNbt.write(tag, value);
                require(ResearchNoteNbt.read(tag, TCAspects.catalog()).note().orElseThrow().equals(value), "NBT round trip");
                require(tag.getString("unrelated").equals("preserve"), "Lost unrelated NBT");
                tag.getList("hexgrid", 10).getCompound(0).putString("aspect", "invalid_test_aspect");
                require(ResearchNoteNbt.read(tag, TCAspects.catalog()).note().isEmpty(), "Accepted damaged note");
            }
        }
        var connection = new ResearchNote("THAUMIUM", 123, false, 2, Map.of(
                new ResearchHex(-1, 0), new ResearchNote.Cell(1, "terra"),
                new ResearchHex(0, 0), new ResearchNote.Cell(2, "vitreus"),
                new ResearchHex(1, 0), new ResearchNote.Cell(1, "ordo"),
                new ResearchHex(3, 0), new ResearchNote.Cell(2, "ignis")));
        require(engine.complete(connection, aspect -> !aspect.equals("vitreus")).isEmpty(), "Undiscovered connector accepted");
        var completed = engine.complete(connection, aspect -> true).orElseThrow();
        require(completed.complete() && completed.grid().size() == 3 && completed.copies() == 2, "Completion pruning");
        require(!connection.complete() && connection.grid().size() == 4, "Modified input note");
        final int count = generated;
        source.sendSuccess(() -> Component.literal("Research notes PASS; generated=" + count
                + "; NBT round trips; invalid data rejected; known direct links; pruning; real player knowledge unchanged"), false);
        return 1;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Research notes FAIL: " + message);
    }
}
