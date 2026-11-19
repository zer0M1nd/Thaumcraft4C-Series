package tc4revival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import tc4revival.content.ThaumonomiconItem;
import tc4revival.research.*;
import tc4revival.config.TCConfig;

/** Locate a recipe's research without moving an inventory item or granting knowledge. */
public record BookRecipeRequest(String research) {
    public void encode(FriendlyByteBuf b) { b.writeUtf(research,128); }
    public static BookRecipeRequest decode(FriendlyByteBuf b) { return new BookRecipeRequest(b.readUtf(128)); }
    public void handle(ServerPlayer player) {
        if(player==null||!player.isAlive()||player.isSpectator()||ResearchCatalogs.catalog().find(research).isEmpty())return;
        int slot=-1;
        for(int i=0;i<player.getInventory().getContainerSize();i++)
            if(player.getInventory().getItem(i).getItem() instanceof ThaumonomiconItem){slot=i;break;}
        if(slot<0)return;
        player.closeContainer();
        int bookSlot=slot;
        var opening=ResearchBookSessions.LIVE.replace(player,()->new ServerResearchBook(player,bookSlot,
                s->s.getItem() instanceof ThaumonomiconItem,KnowledgeSavedData.get(player.server),
                ()->new ServerResearchBook.Settings(TCConfig.SERVER.researchDifficulty.get(),TCConfig.SERVER.allowCheatSheet.get(),TCConfig.SERVER.disableWarp.get())));
        KnowledgeSync.flushBeforeScreen(player);
        TCNetwork.send(player,new BookOpenPacket(opening.token(),research));
    }
}
