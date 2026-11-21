package tc4revival.client;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.TC4Revival;
import tc4revival.config.TCConfig;
import tc4revival.research.sync.KnowledgeDiagnostics;

@Mod.EventBusSubscriber(modid = TC4Revival.MOD_ID, value = Dist.CLIENT)
public  class ClientDebugCommands {
    private ClientDebugCommands() { }
    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tcclient").then(Commands.literal("knowledge").executes(context -> {
            var state = ClientKnowledge.displaySnapshot();
            context.getSource().sendSuccess(() -> Component.literal(state.map(knowledge -> "Client knowledge: "
                    + KnowledgeDiagnostics.describe(knowledge)).orElse("Client knowledge: awaiting synchronization")), false);
            state.ifPresent(knowledge -> context.getSource().sendSuccess(() -> Component.literal("Pools: " + knowledge.knownAspects()
                    .entrySet().stream().map(entry -> entry.getKey().tag() + "=" + knowledge.poolAmount(entry.getKey())).toList()), false));
            return state.isPresent() ? 1 : 0;
        })).then(Commands.literal("book_text_check").executes(context -> {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            int count = 0; int maximumHeight = 0;
            var tall = new java.util.ArrayList<String>();
            for (var entry : tc4revival.research.ResearchPages.bundled().entries().entrySet()) {
                for (int pageIndex = 0; pageIndex < entry.getValue().size(); pageIndex++) {
                    var page = entry.getValue().get(pageIndex);
                    if (page.text().isEmpty()) continue;
                    String key = page.text().get();
                    String text = net.minecraft.client.resources.language.I18n.exists(key)
                            ? net.minecraft.client.resources.language.I18n.get(key) : key;
                    var layout = ResearchBookText.layout(minecraft.font, text, 139);
                    maximumHeight = Math.max(maximumHeight, layout.height());
                    if (layout.height() > 181) tall.add(entry.getKey() + ":" + pageIndex + "=" + layout.height());
                    for (var element : layout.elements()) if (element instanceof ResearchBookText.Illustration illustration) {
                        var location = net.minecraft.resources.ResourceLocation.parse(illustration.picture().texture());
                        if (minecraft.getResourceManager().getResource(location).isEmpty()) throw new IllegalStateException("Missing book image " + location);
                    }
                    count++;
                }
            }
            String result = "Native book text layout checked: " + count + " text pages; max height=" + maximumHeight
                    + "; pages above 181px=" + tall + "; layout/resource check only, visual rendering remains to be verified";
            TC4Revival.LOGGER.info(result);
            context.getSource().sendSuccess(() -> Component.literal(result), false);
            return count;
        })).then(Commands.literal("config").executes(context -> {
            var config = TCConfig.SERVER;
            context.getSource().sendSuccess(() -> Component.literal("Client received server config: research="
                    + config.researchDifficulty.get() + "; aspect_cap=" + config.aspectPoolSoftCap.get()
                    + "; mirrors=" + config.allowMirrors.get()), false);
            return 1;
        })).then(Commands.literal("use").then(Commands.argument("ticks", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 40))
                .executes(context -> {
                    int ticks = ClientUseTest.schedule(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ticks"));
                    context.getSource().sendSuccess(() -> Component.literal("Use binding test queued for " + ticks + " ticks after chat closes"), false);
                    return ticks;
                }))));
    }
}
