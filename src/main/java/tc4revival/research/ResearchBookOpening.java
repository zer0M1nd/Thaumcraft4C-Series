package tc4revival.research;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import tc4revival.config.TCConfig;
import tc4revival.network.BookOpenPacket;
import tc4revival.network.TCNetwork;

/** The item supplies book identities; all gameplay settings and knowledge come from the server. */
public  class ResearchBookOpening {
    private ResearchBookOpening() {}
    public static ServerResearchBook open(ServerPlayer player, InteractionHand hand,
            Predicate<ItemStack> book, Predicate<ItemStack> cheat) {
        thaumcraft.api.research.ResearchBookLifecycle.observe(thaumcraft.api.research.ResearchBookLifecycle.Stage.SERVER_ATTEMPT,player.getUUID(),null,"entered");
        ServerResearchBook opening=null;
        try {
            opening = ResearchBookSessions.LIVE.replace(player, () -> new ServerResearchBook(player, hand, book, cheat,
                    KnowledgeSavedData.get(player.getServer()), () -> new ServerResearchBook.Settings(
                            TCConfig.SERVER.researchDifficulty.get(), TCConfig.SERVER.allowCheatSheet.get(), TCConfig.SERVER.disableWarp.get())));
            thaumcraft.api.research.ResearchBookLifecycle.observe(thaumcraft.api.research.ResearchBookLifecycle.Stage.SESSION_CREATED,player.getUUID(),opening.token(),"session created");
            KnowledgeSync.flushBeforeScreen(player);
            thaumcraft.api.research.ResearchBookLifecycle.observe(thaumcraft.api.research.ResearchBookLifecycle.Stage.SYNC_SENT,player.getUUID(),opening.token(),"knowledge synchronized");
            TCNetwork.send(player, new BookOpenPacket(opening.token()));
        } catch (RuntimeException failure) {
            thaumcraft.api.research.ResearchBookLifecycle.observe(thaumcraft.api.research.ResearchBookLifecycle.Stage.FAILED,player.getUUID(),opening==null?null:opening.token(),failure.toString());
            if(opening!=null)ResearchBookSessions.LIVE.close(player, opening.token());
            throw failure;
        }
        return opening;
    }
}
